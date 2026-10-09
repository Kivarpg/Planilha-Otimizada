import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.nio.file.Files

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
}


abstract class RenameDebugApkTask : DefaultTask() {
    @get:InputFile
    abstract val sourceApk: RegularFileProperty

    @get:OutputFile
    abstract val targetApk: RegularFileProperty

    @TaskAction
    fun renameApk() {
        val source = sourceApk.get().asFile
        val target = targetApk.get().asFile

        check(source.isFile) { "APK debug não encontrado em ${source.absolutePath}" }
        target.parentFile.mkdirs()
        if (target.exists()) target.delete()
        Files.move(source.toPath(), target.toPath())
    }
}
// ktlint — formatação/lint automático de Kotlin. Configurado para NÃO
// falhar o build (ignoreFailures = true): o objetivo aqui é relatório e
// autocorreção via `./gradlew ktlintFormat`, não bloquear CI enquanto o
// código legado ainda não está 100% alinhado ao estilo padrão.
ktlint {
    version.set("1.3.1")
    ignoreFailures.set(true)
    filter {
        exclude("**/build/**")
    }
}

// O nome do APK é derivado do nome canônico do projeto (settings.gradle.kts).
// Há uma única fonte de verdade: rootProject.name == Exalted.<versão>.
// O build falha se o nome estiver fora desse formato, evitando regressões silenciosas.
val exaltedProjectName = rootProject.name
require(Regex("^Exalted\\.\\d+$").matches(exaltedProjectName)) {
    "rootProject.name deve seguir o formato Exalted.<versão>; valor atual: $exaltedProjectName"
}
val exaltedVersionNumber = exaltedProjectName.substringAfter("Exalted.").toInt()

// Padroniza o JDK utilizado pelo compilador Kotlin e pelas tarefas JVM.
// Mantém Java 17 alinhado ao GitHub Actions e às opções de compatibilidade Android.
kotlin {
    jvmToolchain(17)
}

android {
    namespace = "com.example"
    compileSdk = 37

    // Assinatura persistente opcional. No CI, o workflow materializa o keystore
    // a partir de GitHub Actions Secrets e fornece estas quatro variáveis.
    // Builds locais sem essas variáveis continuam usando a chave debug padrão.
    val persistentKeystorePath = System.getenv("EXALTED_KEYSTORE_PATH")
    val persistentKeystorePassword = System.getenv("EXALTED_KEYSTORE_PASSWORD")
    val persistentKeyAlias = System.getenv("EXALTED_KEY_ALIAS")
    val persistentKeyPassword = System.getenv("EXALTED_KEY_PASSWORD")
    val persistentSigningAvailable = listOf(
        persistentKeystorePath,
        persistentKeystorePassword,
        persistentKeyAlias,
        persistentKeyPassword,
    ).all { !it.isNullOrBlank() }

    signingConfigs {
        if (persistentSigningAvailable) {
            create("persistent") {
                storeFile = file(persistentKeystorePath!!)
                storePassword = persistentKeystorePassword
                keyAlias = persistentKeyAlias
                keyPassword = persistentKeyPassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    defaultConfig {
        applicationId = "com.aistudio.exalted3e.rwmaor"
        minSdk = 26
        targetSdk = 36
        versionCode = exaltedVersionNumber
        versionName = exaltedVersionNumber.toString()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // Segurança de release explícita: não depender apenas dos defaults do AGP.
            isDebuggable = false
            isJniDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Sem secrets, gerar release instalavel usando a chave debug.
            signingConfig = if (persistentSigningAvailable) {
                signingConfigs.getByName("persistent")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        debug {
            isMinifyEnabled = false
            if (persistentSigningAvailable) {
                signingConfig = signingConfigs.getByName("persistent")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        // Necessário desde o AGP 8+: a geração de BuildConfig (usada em
        // SheetRepository.kt pra checar BuildConfig.DEBUG antes de logar)
        // vem desativada por padrão a partir dessa versão — sem isso, a
        // classe simplesmente não existe e o import falha ao compilar.
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}


dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.foundation:foundation")
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.zxing.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.androidx.core)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
}

// Mantém o artefato do CI com o nome da versão, sem depender de renomeação manual.
// O renomeador é uma task tipada e usa apenas Providers/Properties de Gradle;
// isso evita capturar Project/Layout em uma ação do tipo doLast, que quebra a
// serialização do configuration cache do Gradle 9.8.0.
val renameDebugApk = tasks.register<RenameDebugApkTask>("renameDebugApk") {
    // Gradle 9 valida @InputFile antes de executar a task. A dependência explícita
    // garante que o produtor do app-debug.apk termine antes do consumidor.
    dependsOn("assembleDebug")
    sourceApk.set(layout.buildDirectory.file("outputs/apk/debug/app-debug.apk"))
    targetApk.set(layout.buildDirectory.file("outputs/apk/debug/$exaltedProjectName.apk"))
}

val renameReleaseApk = tasks.register<RenameDebugApkTask>("renameReleaseApk") {
    dependsOn("assembleRelease")
    sourceApk.set(layout.buildDirectory.file("outputs/apk/release/app-release.apk"))
    targetApk.set(layout.buildDirectory.file("outputs/apk/release/$exaltedProjectName.apk"))
}

// A suíte normal deve permanecer rápida. Auditorias estatísticas/stress são
// separadas sem consultar tasks Android antes de elas existirem.
val auditTestPatterns = listOf(
    "**/*AuditTest.class",
    "**/*StressTest.class",
)

tasks.withType<Test>().configureEach {
    testLogging {
        events("started", "passed", "skipped", "failed")
        showStandardStreams = true
    }

    // AGP registra testDebugUnitTest durante a configuração de variantes.
    // A filtragem é aplicada quando a task Test realmente existe.
    if (name == "testDebugUnitTest") {
        auditTestPatterns.forEach(::exclude)
    }
}

// Auditorias completas permanecem fora da suíte normal por padrão.
// Não criamos uma segunda task Test espelhando testDebugUnitTest: no Gradle 9.8/AGP 9
// configurar um TaskProvider a partir do callback configureEach viola o MutationGuard.
// Quando necessária, a auditoria pode ser executada explicitamente removendo o filtro
// ou por um job de CI dedicado, sem alterar a criação da task Android.
