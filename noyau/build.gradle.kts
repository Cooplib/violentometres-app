import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

// Compilé par le JDK 21 de la machine, mais pour Java 17 : c'est ce
// qu'Android sait lire. Pas de `jvmToolchain(17)`, qui irait télécharger
// un second JDK qu'on n'a pas.
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
    // `api` : le décodeur du noyau (`decodage`) fait partie de ce qu'il
    // expose, et l'application s'en sert pour garder le catalogue.
    api(libs.kotlinx.serialization.json)
    testImplementation(kotlin("test"))
    testImplementation(libs.icu4j)
}

/*
 * La référence du point : des centaines de parties jouées par le code
 * DU SITE, que DerouleContreLeSiteTest rejoue avec le portage. Produite
 * à chaque fois depuis le site cloné à côté (../violentometres-frontend),
 * jamais commitée : une copie figée pourrait dériver du site sans que
 * personne le voie, et c'est exactement ce que ce test doit empêcher.
 *
 * Il faut donc Node, et le site à côté. Sans eux, la tâche échoue en le
 * disant : pas de test du point qui passe en silence faute de référence.
 */
val siteUtils = rootProject.file("../violentometres-frontend/src/utils")
val referenceDuPoint = layout.buildDirectory.file("reference-du-point/parties.json")

val produireReferenceDuPoint = tasks.register<Exec>("referenceDuPoint") {
    val script = rootProject.file("outils/reference-du-point.mjs")
    inputs.file(script)
    inputs.files(File(siteUtils, "deroule.js"), File(siteUtils, "recommandations.js"))
    inputs.dir("src/test/resources/api")
    outputs.file(referenceDuPoint)
    commandLine("node", script.absolutePath, referenceDuPoint.get().asFile.absolutePath)
}

tasks.test {
    useJUnitPlatform()
    dependsOn(produireReferenceDuPoint)
    inputs.file(referenceDuPoint)
    systemProperty("reference.du.point", referenceDuPoint.get().asFile.absolutePath)
}
