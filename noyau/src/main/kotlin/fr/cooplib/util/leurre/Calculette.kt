package fr.cooplib.util.leurre

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/*
 * Le leurre : une calculatrice, et une VRAIE.
 *
 * Une calculatrice qui ne calcule pas trahit l'application à la
 * première personne qui y touche. Celle-ci calcule en décimal exact
 * (0,1 + 0,2 donne 0,3, pas 0,30000000000000004, qu'aucune
 * calculatrice de téléphone n'affiche), respecte les priorités (2 + 3 × 4
 * donne 14), et se comporte comme celles qu'on connaît : un opérateur
 * tapé deux fois remplace le premier, un chiffre après « = » repart de
 * zéro, un opérateur continue sur le résultat.
 *
 * Le code se tape comme un usage normal : des chiffres puis « = ».
 * C'est l'écran qui le reconnaît (voir CodeSecret), pas ce fichier, qui
 * ne sait que calculer. Le motif est connu, et c'est accepté (décision
 * du 7 octobre 2026, notes de conception) : la protection ne repose pas sur le
 * secret du mécanisme, mais sur le fait qu'une calculatrice ressemble à
 * une calculatrice.
 *
 * Immuable : chaque appui rend un nouvel état.
 */
data class Calculette(
    // Ce qui est tapé, avec les signes affichés : « 12,5×3−4 ».
    val saisie: String = "",
    // Après « = » : l'expression calculée et son résultat.
    val calculee: String? = null,
    val resultat: String? = null,
    val erreur: String? = null,
) {

    // Ce que montre la grande ligne.
    val affichage: String get() = erreur ?: resultat ?: saisie.ifEmpty { "0" }

    fun appuyer(touche: Touche): Calculette = when (touche) {

        is Touche.Chiffre -> {
            // Après un résultat, un chiffre commence un nouveau calcul.
            val base = if (resultat != null || erreur != null) "" else saisie
            val nombre = dernierNombre(base)
            when {
                // Pas de « 007 » : un zéro seul se remplace.
                nombre == "0" -> Calculette(base.dropLast(1) + touche.chiffre)
                nombre == "−0" -> Calculette(base.dropLast(1) + touche.chiffre)
                else -> Calculette(base + touche.chiffre)
            }
        }

        Touche.Virgule -> {
            val base = if (resultat != null || erreur != null) "" else saisie
            val nombre = dernierNombre(base)
            when {
                ',' in nombre -> this
                nombre.isEmpty() || nombre == "−" -> Calculette(base + "0,")
                base.last() == '%' -> this
                else -> Calculette(base + ",")
            }
        }

        is Touche.Operateur -> {
            // Après un résultat, on continue dessus ; après une erreur, rien.
            val base = when {
                erreur != null -> ""
                resultat != null -> resultat
                else -> saisie.removeSuffix(",")
            }
            val signe = touche.signe
            when {
                // Seul le moins peut commencer un nombre.
                base.isEmpty() -> if (signe == MOINS) Calculette(MOINS.toString()) else Calculette()
                base == MOINS.toString() -> Calculette(base)
                // Un moins après × ou ÷ est le signe du nombre qui vient :
                // 2 × −3, comme sur les calculatrices de téléphone.
                signe == MOINS && (base.last() == FOIS || base.last() == DIVISE) -> Calculette(base + signe)
                // Après « × − », un autre opérateur remplace les deux.
                base.length >= 2 && base.last() == MOINS && base[base.length - 2] in OPERATEURS ->
                    Calculette(base.dropLast(2) + signe)
                // Deux opérateurs de suite : le second remplace le premier.
                base.last() in OPERATEURS -> Calculette(base.dropLast(1) + signe)
                else -> Calculette(base + signe)
            }
        }

        Touche.Pourcent -> {
            val base = if (resultat != null) resultat else saisie.removeSuffix(",")
            if (erreur == null && base.isNotEmpty() && (base.last().isDigit() || base.last() == '%')) Calculette(base + "%") else this
        }

        Touche.Retour -> when {
            resultat != null || erreur != null -> Calculette()
            else -> Calculette(saisie.dropLast(1))
        }

        Touche.Effacer -> Calculette()

        Touche.Egal -> {
            val expression = saisie.removeSuffix(",").trimEnd(*OPERATEURS)
            when {
                resultat != null || erreur != null || expression.isEmpty() || expression == MOINS.toString() -> this
                else -> try {
                    Calculette(calculee = expression, resultat = formater(Analyse(expression).evaluer()))
                } catch (_: ArithmeticException) {
                    Calculette(calculee = expression, erreur = DIVISION_PAR_ZERO)
                }
            }
        }
    }

    sealed interface Touche {
        data class Chiffre(val chiffre: Char) : Touche {
            init { require(chiffre in '0'..'9') }
        }
        data class Operateur(val signe: Char) : Touche {
            init { require(signe in OPERATEURS) }
        }
        data object Virgule : Touche
        data object Pourcent : Touche
        data object Retour : Touche
        data object Effacer : Touche
        data object Egal : Touche
    }

    companion object {

        const val PLUS = '+'
        const val MOINS = '−'
        const val FOIS = '×'
        const val DIVISE = '÷'

        val OPERATEURS = charArrayOf(PLUS, MOINS, FOIS, DIVISE)

        const val DIVISION_PAR_ZERO = "Impossible de diviser par 0"

        // Ce que les calculatrices de téléphone montrent, à peu près.
        private val PRECISION = MathContext(12, RoundingMode.HALF_EVEN)

        // Le nombre en cours de frappe, son signe compris : un moins en
        // tête, ou juste après un opérateur, lui appartient.
        private fun dernierNombre(saisie: String): String {
            val i = saisie.indices.lastOrNull { k ->
                saisie[k] in OPERATEURS && !(saisie[k] == MOINS && (k == 0 || saisie[k - 1] in OPERATEURS))
            }
            return if (i == null) saisie else saisie.substring(i + 1)
        }

        /*
         * Virgule décimale, à la française. Au-delà de mille milliards ou
         * en deçà d'un milliardième, la notation scientifique, comme les
         * calculatrices de téléphone.
         */
        internal fun formater(n: BigDecimal): String {
            val arrondi = n.round(PRECISION).stripTrailingZeros()
            val abs = arrondi.abs()
            val texte = if (abs.signum() != 0 && (abs >= BigDecimal("1e12") || abs < BigDecimal("1e-9"))) {
                arrondi.toString().replace("E+", "E")
            } else {
                arrondi.toPlainString()
            }
            return texte.replace('.', ',').replace('-', MOINS)
        }
    }

    /*
     * expression = terme (( + | − ) terme)*
     * terme      = facteur (( × | ÷ ) facteur)*
     * facteur    = −? nombre %*
     */
    private class Analyse(private val texte: String) {

        private var i = 0

        fun evaluer(): BigDecimal {
            val v = expression()
            check(i == texte.length) { "reste : ${texte.substring(i)}" }
            return v
        }

        private fun expression(): BigDecimal {
            var v = terme()
            while (i < texte.length && (texte[i] == PLUS || texte[i] == MOINS)) {
                val op = texte[i++]
                val d = terme()
                v = if (op == PLUS) v.add(d) else v.subtract(d)
            }
            return v
        }

        private fun terme(): BigDecimal {
            var v = facteur()
            while (i < texte.length && (texte[i] == FOIS || texte[i] == DIVISE)) {
                val op = texte[i++]
                val d = facteur()
                v = if (op == FOIS) v.multiply(d) else {
                    if (d.signum() == 0) throw ArithmeticException("division par zéro")
                    v.divide(d, MathContext.DECIMAL128)
                }
            }
            return v
        }

        private fun facteur(): BigDecimal {
            var negatif = false
            if (i < texte.length && texte[i] == MOINS) { negatif = true; i++ }
            val debut = i
            while (i < texte.length && (texte[i].isDigit() || texte[i] == ',')) i++
            var v = BigDecimal(texte.substring(debut, i).replace(',', '.'))
            while (i < texte.length && texte[i] == '%') { v = v.movePointLeft(2); i++ }
            return if (negatif) v.negate() else v
        }
    }
}
