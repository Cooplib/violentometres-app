package fr.cooplib.util.ecrans

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.cooplib.util.leurre.Calculette
import fr.cooplib.util.leurre.Calculette.Touche
import fr.cooplib.util.leurre.CodeSecret

/*
 * L'écran du leurre. Gris et neutre, comme n'importe quelle
 * calculatrice : rien du violet de l'application, rien des couleurs de
 * l'échelle. Le calcul lui-même vit dans le noyau (Calculette), testé.
 *
 * Le code se tape comme un calcul : des chiffres, puis « = ». Reconnu,
 * l'écran se vide AVANT d'ouvrir l'application : le code ne reste pas
 * affiché pour la personne suivante. Pas reconnu, « = » donne le
 * nombre, comme toute calculatrice.
 */

private val Fond = Color(0xFF202124)
private val CouleurTouche = Color(0xFF3C4043)
private val CouleurFonction = Color(0xFF5F6368)
private val Texte = Color(0xFFF1F3F4)

// La calculatrice survit à une rotation : se vider en tournant le
// téléphone, c'est un comportement qu'aucune calculatrice n'a.
private val Sauvegarde: Saver<Calculette, Any> = listSaver(
    save = { listOf(it.saisie, it.calculee, it.resultat, it.erreur) },
    restore = { Calculette(it[0] ?: "", it[1], it[2], it[3]) },
)

@Composable
fun Calculatrice(empreinteDuCode: String?, deverrouiller: () -> Unit) {

    var c by rememberSaveable(stateSaver = Sauvegarde) { mutableStateOf(Calculette()) }

    fun appuyer(t: Touche) {
        if (t == Touche.Egal && empreinteDuCode != null && CodeSecret.verifie(c.saisie, empreinteDuCode)) {
            c = Calculette()
            deverrouiller()
            return
        }
        c = c.appuyer(t)
    }

    Column(
        Modifier.fillMaxSize().background(Fond).safeDrawingPadding().padding(12.dp),
        verticalArrangement = Arrangement.Bottom,
    ) {

        Text(
            c.calculee ?: "",
            Modifier.fillMaxWidth(),
            color = Texte.copy(alpha = 0.6f),
            fontSize = 24.sp,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.StartEllipsis,
        )

        Text(
            c.affichage,
            Modifier.fillMaxWidth().padding(bottom = 16.dp),
            color = Texte,
            fontSize = if (c.affichage.length > 12) 36.sp else 56.sp,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.StartEllipsis,
        )

        val lignes = listOf(
            listOf("C" to Touche.Effacer, "⌫" to Touche.Retour, "%" to Touche.Pourcent, "÷" to Touche.Operateur(Calculette.DIVISE)),
            listOf("7" to Touche.Chiffre('7'), "8" to Touche.Chiffre('8'), "9" to Touche.Chiffre('9'), "×" to Touche.Operateur(Calculette.FOIS)),
            listOf("4" to Touche.Chiffre('4'), "5" to Touche.Chiffre('5'), "6" to Touche.Chiffre('6'), "−" to Touche.Operateur(Calculette.MOINS)),
            listOf("1" to Touche.Chiffre('1'), "2" to Touche.Chiffre('2'), "3" to Touche.Chiffre('3'), "+" to Touche.Operateur(Calculette.PLUS)),
            listOf("0" to Touche.Chiffre('0'), "," to Touche.Virgule, "=" to Touche.Egal),
        )

        for (ligne in lignes) {
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((libelle, touche) in ligne) {
                    val fonction = touche !is Touche.Chiffre && touche != Touche.Virgule
                    Button(
                        onClick = { appuyer(touche) },
                        modifier = Modifier.weight(if (libelle == "0") 2f else 1f).padding(0.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (fonction) CouleurFonction else CouleurTouche, contentColor = Texte),
                    ) {
                        Text(libelle, fontSize = 26.sp, fontWeight = FontWeight.Normal, modifier = Modifier.padding(vertical = 10.dp))
                    }
                }
            }
        }
    }
}
