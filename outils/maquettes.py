"""
Les images de la fiche Play : chaque capture dans un téléphone, sur le
violet de la charte, avec une phrase au-dessus.

    python3 outils/maquettes.py [sortie]

Les captures brutes (fastlane/…/phoneScreenshots/) restent ce qu'elles
sont : F-Droid les montre telles quelles, et ce sont elles qu'on refait
quand l'application change. Celles-ci en sont tirées, pour Play.

Le téléphone est DROIT, pas penché : à la taille d'une vignette de
magasin, une capture penchée devient illisible, et c'est ce qu'elle
montre qui doit convaincre.

Noto Sans plutôt que la police du site : le site prend celle du
système (Roboto sur Android), et Noto, libre, en est proche.
"""

import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

RACINE = Path(__file__).resolve().parent.parent
CAPTURES = RACINE / "fastlane/metadata/android/fr-FR/images/phoneScreenshots"

# La phrase de chaque capture, dans l'ordre des fichiers.
PHRASES = [
    "Ce que je vis, est-ce normal ?",
    "Faire le point, une question à la fois",
    "Voir où vous en êtes",
    "Un violentomètre, du vert au rouge",
    "Un numéro, d'un seul appui",
    "Des dizaines de violentomètres",
    "Lire ce que d'autres ont vécu",
    "Discrète : elle peut se cacher en calculatrice",
]

LARGEUR, HAUTEUR = 1080, 2160
VIOLET, VIOLET_FONCE = (0x5B, 0x3F, 0xA0), (0x47, 0x2F, 0x80)
POLICE = "/usr/share/fonts/truetype/noto/NotoSans-SemiBold.ttf"

# L'écran dans le téléphone, et le téléphone sur la page.
ECRAN_L = 800
ECRAN_H = ECRAN_L * 2
BORD = 22
HAUT_TELEPHONE = 470


def fond():
    # Un dégradé vertical du violet au violet foncé.
    img = Image.new("RGB", (LARGEUR, HAUTEUR))
    d = ImageDraw.Draw(img)
    for y in range(HAUTEUR):
        t = y / HAUTEUR
        d.line([(0, y), (LARGEUR, y)], fill=tuple(round(a + (b - a) * t) for a, b in zip(VIOLET, VIOLET_FONCE)))
    return img


def phrase(img, texte):
    d = ImageDraw.Draw(img)
    police = ImageFont.truetype(POLICE, 76)
    # Sur une ligne si elle tient ; sinon sur deux, coupée là où les deux
    # lignes sont le plus égales : un mot seul en bas fait bancal.
    mots = texte.split()
    if d.textlength(texte, font=police) <= LARGEUR - 160:
        lignes = [texte]
    else:
        coupe = min(range(1, len(mots)), key=lambda i: max(
            d.textlength(" ".join(mots[:i]), font=police), d.textlength(" ".join(mots[i:]), font=police)))
        lignes = [" ".join(mots[:coupe]), " ".join(mots[coupe:])]
    hauteur_ligne = 96
    y = (HAUT_TELEPHONE - hauteur_ligne * len(lignes)) // 2 - 10
    for l in lignes:
        d.text((LARGEUR // 2, y), l, font=police, fill="white", anchor="ma")
        y += hauteur_ligne


def telephone(img, capture):
    x = (LARGEUR - ECRAN_L) // 2 - BORD
    y = HAUT_TELEPHONE
    l, h = ECRAN_L + 2 * BORD, ECRAN_H + 2 * BORD
    rayon = 90

    # L'ombre, floue, un peu décalée vers le bas.
    ombre = Image.new("L", img.size, 0)
    ImageDraw.Draw(ombre).rounded_rectangle([x, y + 30, x + l, y + h + 30], rayon, fill=150)
    ombre = ombre.filter(ImageFilter.GaussianBlur(40))
    img.paste(Image.new("RGB", img.size, (0x24, 0x18, 0x48)), mask=ombre)

    d = ImageDraw.Draw(img)
    d.rounded_rectangle([x, y, x + l, y + h], rayon, fill=(0x1B, 0x1B, 0x1F))

    ecran = Image.open(capture).convert("RGB").resize((ECRAN_L, ECRAN_H), Image.LANCZOS)
    masque = Image.new("L", ecran.size, 0)
    ImageDraw.Draw(masque).rounded_rectangle([0, 0, ECRAN_L, ECRAN_H], rayon - BORD, fill=255)
    img.paste(ecran, (x + BORD, y + BORD), masque)


def maquette(capture, texte, sortie):
    img = fond()
    phrase(img, texte)
    telephone(img, capture)
    img.save(sortie, quality=90, optimize=True)


def banniere(sortie):
    """
    L'image de présentation (1024 × 500), exigée par Play, montrée en tête
    de fiche par F-Droid : le V, le titre, la description courte. Le V
    est redessiné depuis outils/icone.py, pour qu'il reste le même.
    """
    sys.path.insert(0, str(Path(__file__).resolve().parent))
    import icone

    l, h = 1024, 500
    img = Image.new("RGB", (l, h))
    d = ImageDraw.Draw(img)
    for x in range(l):
        t = x / l
        d.line([(x, 0), (x, h)], fill=tuple(round(a + (b - a) * t) for a, b in zip(VIOLET, VIOLET_FONCE)))

    # Le V sur un carré blanc arrondi, comme une icône posée.
    tmp = RACINE / "build/v.png"
    tmp.parent.mkdir(parents=True, exist_ok=True)
    icone.dessiner(tmp)
    v = Image.open(tmp).resize((260, 260), Image.LANCZOS)
    masque = Image.new("L", v.size, 0)
    ImageDraw.Draw(masque).rounded_rectangle([0, 0, 260, 260], 58, fill=255)
    img.paste(v, (80, (h - 260) // 2), masque)

    titre = ImageFont.truetype("/usr/share/fonts/truetype/noto/NotoSans-Bold.ttf", 64)
    sous = ImageFont.truetype("/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf", 32)
    x = 400
    d.text((x, 150), "Violentomètre", font=titre, fill="white")
    for i, ligne in enumerate(["Repérer les violences,", "faire le point,", "trouver de l'aide."]):
        d.text((x, 250 + i * 44), ligne, font=sous, fill=(0xEF, 0xEA, 0xF8))
    img.save(sortie, optimize=True)


if __name__ == "__main__":
    sortie = Path(sys.argv[1]) if len(sys.argv) > 1 else RACINE / "build/maquettes"
    sortie.mkdir(parents=True, exist_ok=True)
    for i, texte in enumerate(PHRASES, 1):
        maquette(CAPTURES / f"{i}.jpg", texte, sortie / f"{i}.jpg")
        print(sortie / f"{i}.jpg")
    banniere(CAPTURES.parent / "featureGraphic.png")
    print(CAPTURES.parent / "featureGraphic.png")
