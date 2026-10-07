import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(project(":noyau"))
    implementation(libs.kotlinx.serialization.json)
}

/*
 * ./gradlew :fabrique:catalogue
 * ./gradlew :fabrique:catalogue -Padresse=http://localhost:8000
 *
 * Réécrit le catalogue embarqué depuis l'API. À lancer avant chaque
 * version, puis à commiter : le diff dit ce qui a changé dans le
 * contenu. Jamais lancé par la construction (voir Catalogue.kt).
 */
tasks.register<JavaExec>("catalogue") {
    group = "application"
    description = "Réécrit le catalogue embarqué depuis l'API."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "fr.cooplib.util.fabrique.FabriquerCatalogueKt"
    val sortie = rootProject.file("noyau/src/main/resources/fr/cooplib/util/donnees/catalogue.json")
    val adresse = providers.gradleProperty("adresse").orElse("https://violentometres.fr/api")
    args(sortie.absolutePath, adresse.get())
    // Toujours relancé : sa source est le réseau, que Gradle ne voit pas.
    outputs.upToDateWhen { false }
}
