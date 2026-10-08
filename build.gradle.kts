// build.gradle.kts raiz — só declara os plugins (com apply false) pra
// deixar as versões disponíveis pro módulo :app, que os aplica de
// verdade via alias(libs.plugins.*) no build.gradle.kts dele.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ktlint) apply false
}
