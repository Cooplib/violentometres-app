# Les règles de kotlinx.serialization viennent avec la bibliothèque
# (META-INF/com.android.tools/r8) : rien à ajouter pour les modèles.
#
# Le catalogue embarqué est une ressource Java lue par son chemin
# absolu (Catalogue.RESSOURCE) : R8 ne touche pas aux ressources Java,
# même quand il renomme la classe qui les lit.

# Les noms de classes sont renommés, et c'est bienvenu : un nom de
# moins qui dise ce qu'est l'application. Mais les traces de plantage
# deviennent illisibles sans le fichier de correspondance
# (app/build/outputs/mapping/release/mapping.txt), gardé par la CI.
