package fr.cooplib.util.reseau

import java.util.UUID

/*
 * L'identifiant anonyme envoyé en `X-Visitor-Id` à chaque écriture.
 *
 * C'est TOUTE l'identité d'un contributeur : rien n'est créé côté
 * serveur, qui n'en garde qu'une empreinte. Le perdre, c'est perdre le
 * lien avec ses récits déposés, et rien d'autre : pas de récupération à
 * prévoir, donc pas de porte dérobée.
 *
 * Le serveur refuse tout ce qui n'est pas fait de lettres, chiffres et
 * tirets, de 8 à 64 caractères. On le vérifie ici aussi : un identifiant
 * relu d'un stockage abîmé doit être écarté avant de partir, pas
 * découvert par un 422 au moment où quelqu'un envoie son récit.
 */
@JvmInline
value class IdentifiantVisiteur private constructor(val valeur: String) {

    override fun toString() = valeur

    companion object {

        private val FORME = Regex("^[A-Za-z0-9-]{8,64}$")

        fun estValide(texte: String) = FORME.matches(texte)

        // Un UUID aléatoire : 36 caractères, lettres, chiffres et tirets.
        fun nouveau() = IdentifiantVisiteur(UUID.randomUUID().toString())

        fun depuis(texte: String): IdentifiantVisiteur? =
            if (estValide(texte)) IdentifiantVisiteur(texte) else null
    }
}
