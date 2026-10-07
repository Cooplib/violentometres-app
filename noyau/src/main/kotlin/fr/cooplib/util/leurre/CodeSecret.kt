package fr.cooplib.util.leurre

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/*
 * Le code qui, tapé dans la calculatrice puis « = », ouvre la vraie
 * application.
 *
 * Des chiffres seulement, de 4 à 12 : il doit se taper comme un calcul
 * ordinaire. Gardé en empreinte salée, jamais en clair, pour qu'une
 * copie des données de l'application ne le donne pas tel quel. Ce
 * n'est pas un coffre : un code de quatre chiffres se retrouve en
 * essayant les dix mille. Il arrête un regard curieux, rien de plus,
 * et la page « ce qui est protégé » le dit.
 *
 * L'oublier ne coûte rien : rien de personnel n'est gardé par défaut,
 * on réinstalle. Donc pas de récupération, donc pas de porte dérobée.
 *
 * Peu d'itérations, exprès : l'empreinte se calcule à chaque « = » qui
 * pourrait être le code. Plus lente, elle ferait marquer un temps à la
 * calculatrice sur certains calculs et pas sur d'autres, et ça se
 * remarque. Sur un vieux téléphone, 10 000 tours restent sous le
 * dixième de seconde.
 */
object CodeSecret {

    private val FORME = Regex("^[0-9]{4,12}$")

    private const val TOURS = 10_000

    fun valide(code: String) = FORME.matches(code)

    // « 1$tours$sel$empreinte » : la forme peut changer sans perdre les
    // codes déjà posés.
    fun empreinte(code: String, sel: ByteArray = ByteArray(16).also(SecureRandom()::nextBytes)): String {
        require(valide(code)) { "un code est fait de 4 à 12 chiffres" }
        val b64 = Base64.getEncoder()
        return "1\$$TOURS\$${b64.encodeToString(sel)}\$${b64.encodeToString(deriver(code, sel, TOURS))}"
    }

    fun verifie(saisie: String, empreinte: String): Boolean {

        // Une addition n'est jamais le code : pas de calcul inutile.
        if (!valide(saisie)) return false

        val parts = empreinte.split('$')
        if (parts.size != 4 || parts[0] != "1") return false

        val tours = parts[1].toIntOrNull() ?: return false
        val b64 = Base64.getDecoder()
        val sel = runCatching { b64.decode(parts[2]) }.getOrNull() ?: return false
        val attendu = runCatching { b64.decode(parts[3]) }.getOrNull() ?: return false

        return MessageDigest.isEqual(deriver(saisie, sel, tours), attendu)
    }

    private fun deriver(code: String, sel: ByteArray, tours: Int): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(code.toCharArray(), sel, tours, 256))
            .encoded
}
