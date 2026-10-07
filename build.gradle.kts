// Déclarés ici sans être appliqués : chaque module les applique, et
// Gradle ne charge le plugin qu'une fois.
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
}
