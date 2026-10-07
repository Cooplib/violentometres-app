rootProject.name = "violentometres-app"

pluginManagement {
    repositories {
        // Le dépôt de Google pour le plugin Android et AndroidX : des
        // bibliothèques libres (Apache 2.0), acceptées par F-Droid. Ce qui
        // est interdit, ce sont les services Google Play et Firebase.
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

// Le noyau ne dépend de rien d'Android : il se compile et se teste dans
// WSL avec un simple JDK. Le module :app viendra s'ajouter ici, inclus
// sous condition — voir les notes de conception, « Deux modules ».
include(":noyau")

// Fabrique le catalogue embarqué. Un outil de développement, JVM seule :
// il ne va pas dans l'APK.
include(":fabrique")

/*
 * Le module Android, seulement là où un SDK existe vraiment.
 *
 * Studio, sur Windows, écrit dans CE dossier un local.properties avec
 * `sdk.dir=D\:\\Android\\Sdk`. Lu tel quel depuis WSL, il ferait
 * construire l'Android avec un chemin qui n'existe pas ici, et :noyau:test
 * échouerait pour une raison qui ne le regarde pas. D'où : le chemin doit
 * exister pour le système qui lance Gradle.
 *
 * En CI, `-PavecAndroid=true` rend le module OBLIGATOIRE : un SDK absent
 * fait échouer la construction au lieu de l'escamoter en silence.
 */
fun sdkAndroid(): File? {
    val local = file("local.properties").takeIf { it.isFile }?.let { f ->
        java.util.Properties().apply { f.inputStream().use(::load) }.getProperty("sdk.dir")
    }
    return listOfNotNull(local, System.getenv("ANDROID_HOME"), System.getenv("ANDROID_SDK_ROOT"))
        .map(::File)
        .firstOrNull { it.isDirectory }
}

val avecAndroid = providers.gradleProperty("avecAndroid").orNull == "true"

if (sdkAndroid() != null) {
    include(":app")
} else if (avecAndroid) {
    throw GradleException("avecAndroid=true, mais aucun SDK Android trouvé (local.properties, ANDROID_HOME).")
}
