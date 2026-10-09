"""
L'icône des fiches de magasin (512 × 512), dessinée depuis la géométrie
du V : la même que celle de l'icône du vrai nom
(app/src/main/res/drawable/vraie_motif.xml). Les deux se changent
ensemble.

    python3 outils/icone.py

Le V, choisi le 9 octobre 2026 plutôt que les quatre barres penchées du
site : à 48 pixels, la taille d'une liste de magasin, les barres fines
se brouillent, et le V se lit encore. Ses bandes vont du vert en haut au
rouge en bas, les niveaux d'un violentomètre dans leur ordre.

Dessiné en Python plutôt qu'en SVG : ImageMagick, le seul convertisseur
présent ici, rend mal les chemins de découpe. Dessiné quatre fois plus
grand puis réduit, pour adoucir les bords.
"""

from pathlib import Path

from PIL import Image, ImageDraw

# Le V, sur une grille de 100 (relevée sur le dessin de Cooplib). Centré
# sur x = 49,5.
V = [(8, 13), (31, 13), (49.5, 56), (68, 13), (91, 13), (60, 87), (39, 87)]

# Les bandes, de haut en bas : les couleurs de l'échelle du site.
BANDES = [(13, "#4caf50"), (31.5, "#fbc02d"), (50, "#f57c00"), (68.5, "#d32f2f")]
BAS = 87

COTE = 512
LOUPE = 4
# La part du côté qu'occupe le V : Play arrondit les coins et ajoute une
# ombre, il faut de l'air autour.
PART = 0.62


def dessiner(sortie: Path):
    grand = COTE * LOUPE
    echelle = grand * PART / (91 - 8)
    dx = grand / 2 - 49.5 * echelle
    dy = grand / 2 - (13 + BAS) / 2 * echelle

    def point(x, y):
        return (dx + x * echelle, dy + y * echelle)

    masque = Image.new("L", (grand, grand), 0)
    ImageDraw.Draw(masque).polygon([point(x, y) for x, y in V], fill=255)

    couleurs = Image.new("RGB", (grand, grand), "white")
    d = ImageDraw.Draw(couleurs)
    for i, (haut, couleur) in enumerate(BANDES):
        bas = BANDES[i + 1][0] if i + 1 < len(BANDES) else BAS
        d.rectangle([point(0, haut), point(100, bas)], fill=couleur)

    icone = Image.new("RGB", (grand, grand), "white")
    icone.paste(couleurs, mask=masque)
    icone.resize((COTE, COTE), Image.LANCZOS).save(sortie, optimize=True)


if __name__ == "__main__":
    sortie = Path(__file__).resolve().parent.parent / "fastlane/metadata/android/fr-FR/images/icon.png"
    sortie.parent.mkdir(parents=True, exist_ok=True)
    dessiner(sortie)
    print(sortie)
