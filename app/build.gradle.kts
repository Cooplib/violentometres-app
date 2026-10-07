plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // Le brouillon d'un récit se garde en JSON (stockage/Brouillon.kt).
    alias(libs.plugins.kotlin.serialization)
}

android {
    /*
     * IRRÉVERSIBLE, tranché le 7 octobre 2026. Le changer après
     * publication, c'est publier une autre application et perdre les
     * installations. Il se lit dans Réglages → Applications et dans
     * l'adresse des fiches des magasins : muet, et n'imitant aucune
     * application existante.
     *
     * `namespace` prend la même valeur : un espace de noms parlant
     * ressortirait dans les noms de classes de l'APK. Ce qui ne
     * protège pas de qui décompile, et n'a pas à le faire (notes de conception).
     */
    namespace = "fr.cooplib.util"

    compileSdk = 37

    defaultConfig {
        applicationId = "fr.cooplib.util"
        // Android 8.0 : l'essentiel des téléphones en service, y compris
        // les vieux qu'on se passe.
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    // Aucune donnée d'usage envoyée à Google avec l'APK : les « dependency
    // metadata » chiffrées que Play lirait, et que F-Droid refuse.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation(project(":noyau"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
}
