rootProject.name = "violentometres-app"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
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
