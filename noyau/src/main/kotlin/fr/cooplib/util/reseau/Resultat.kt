package fr.cooplib.util.reseau

/*
 * Ce qu'une requête peut donner, sans exception à rattraper : l'écran
 * doit dire quelque chose de juste dans chaque cas, et un `when` qui
 * oublie un cas ne compile pas.
 */
sealed interface Resultat<out T> {

    data class Ok<T>(val valeur: T) : Resultat<T>

    /*
     * 429 : la cadence de l'API, 60 écritures par minute et par
     * identifiant. Ce n'est pas une panne, et il ne faut pas le dire
     * comme une panne : attendre, puis réessayer.
     */
    data class Limite(val attenteSecondes: Int) : Resultat<Nothing>

    // Le serveur a répondu non. `detail` est son message, en français,
    // quand il en donne un (« Le titre est obligatoire »).
    data class Refuse(val code: Int, val detail: String?) : Resultat<Nothing>

    // Pas de réseau, ou une réponse illisible. Hors ligne, l'application
    // marche quand même : c'est le catalogue embarqué.
    data class Injoignable(val cause: String) : Resultat<Nothing>
}
