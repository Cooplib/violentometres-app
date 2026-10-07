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

    /*
     * La signature de diffusion, lue dans l'environnement : la clé ne
     * passe JAMAIS par le dépôt, qui deviendra public avec F-Droid.
     * Sans elle, la version de diffusion sort non signée ; c'est ce que
     * fait la CI tant que les secrets ne sont pas posés, et c'est aussi
     * ce que F-Droid veut (il signe avec sa propre clé).
     *
     * PERDRE CETTE CLÉ, c'est ne plus pouvoir publier de mise à jour de
     * la même application : à garder en deux endroits, hors de cette
     * machine.
     */
    val cle = System.getenv("SIGNATURE_FICHIER")?.let(::file)?.takeIf { it.isFile }

    signingConfigs {
        /*
         * La clé de MISE AU POINT, commitée exprès : sans elle, chaque
         * machine de CI en fabrique une neuve, et Android refuse d'installer
         * une version par-dessus la précédente. Il fallait désinstaller,
         * donc perdre le code et les réglages, à chaque essai. Elle n'a rien
         * de secret (mots de passe « android », ceux de tout le monde) et
         * ne signe jamais une version de diffusion.
         */
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (cle != null) {
            create("diffusion") {
                storeFile = cle
                storePassword = System.getenv("SIGNATURE_MOT_DE_PASSE")
                keyAlias = System.getenv("SIGNATURE_ALIAS")
                keyPassword = System.getenv("SIGNATURE_MOT_DE_PASSE_CLE") ?: System.getenv("SIGNATURE_MOT_DE_PASSE")
            }
        }
    }

    buildTypes {
        release {
            // R8 : le code et les ressources qui ne servent pas partent. De
            // 12 Mo en mise au point à quelques Mo : sur un vieux téléphone
            // presque plein, une application lourde se désinstalle la
            // première, et sa taille se lit dans les réglages.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "regles-r8.pro")
            signingConfig = signingConfigs.findByName("diffusion")
        }
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
