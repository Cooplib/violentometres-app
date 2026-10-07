/*
 * Tous les plugins déclarés ICI, sans être appliqués, et c'est
 * obligatoire : le plugin Kotlin et le plugin Android doivent vivre dans
 * le même chargeur de classes. Le premier essai déclarait Android dans
 * :app seulement, et la CI est tombée sur « Could not generate a
 * decorated class for type KotlinAndroidTarget » : les classes Kotlin
 * pour Android, chargées ici, ne voyaient pas celles d'Android.
 *
 * Le prix : le Gradle de WSL télécharge le plugin Android sans jamais
 * s'en servir (mesuré dans les notes de conception). Pas le SDK, qui reste absent.
 */
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
