pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// Nome canônico da versão do projeto. O build do APK deriva o nome do artefato daqui.
// Isso evita que o nome do APK volte acidentalmente para app-debug.apk ou para
// uma versão anterior quando uma nova versão for empacotada.
rootProject.name = "Exalted.826"
include(":app")
