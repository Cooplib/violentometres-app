# La recette F-Droid

`fr.cooplib.util.yml` est la recette proposée à F-Droid, dans le dépôt
`gitlab.com/fdroid/fdroiddata`, sous `metadata/fr.cooplib.util.yml`.
Elle est gardée ici pour suivre le code ; F-Droid ne lit que sa copie
chez lui.

**Elle doit rester dans la forme que produit `fdroid rewritemeta`** :
l'intégration continue de fdroiddata refuse toute autre mise en page, et
cette forme retire les commentaires. D'où ce fichier-ci pour les
explications. Pour la vérifier : `fdroid lint fr.cooplib.util` puis
`fdroid rewritemeta fr.cooplib.util` dans une copie de fdroiddata, ou
dans un dossier qui a son `config/categories.yml` et son
`config/antiFeatures.yml`.

Ce qui n'est pas évident :

- **Reproductible.** F-Droid recompile chaque étiquette, compare son APK
  à celui de la release GitHub (`Binaries`), et publie le nôtre, signé
  par Cooplib, s'ils sont identiques. Une seule signature pour F-Droid,
  le site et Play. `AllowedAPKSigningKeys` est l'empreinte SHA-256 du
  certificat. Le job `reproductible` de la CI vérifie de notre côté que
  deux constructions donnent le même APK.
- **`gradleprops: avecAndroid=true`.** Sans lui, `:app` n'est inclus que
  si le SDK est trouvé, et en silence ; avec, son absence fait échouer
  (`settings.gradle.kts`).
- **Pas de `scandelete` pour `app/debug.keystore`** : le scanner de
  F-Droid ne signale pas cette clé de mise au point, et il refuse une
  ligne qui ne sert à rien (« Unused scandelete path », premier essai du
  11 octobre 2026).
- **`AutoName: Calculatrice`** : F-Droid le déduit du manifeste, dont
  l'étiquette est celle du leurre, et sa vérification refuse un autre
  nom. La fiche, elle, affiche le titre de `fastlane/`.
- **Fins de ligne Unix, et l'espace après `Binaries:`** : leur
  `rewritemeta` coupe la longue adresse sur la ligne suivante et laisse
  cet espace. Un fichier collé dans l'éditeur web de GitLab depuis
  Windows est arrivé avec des fins de ligne `\r\n`, et toute la recette
  a été refusée : le déposer par « Replace » (envoi du fichier), pas en
  le collant.
- **`prebuild` relève le plafond de mémoire de Gradle** (4 Go, 1 Go de
  Metaspace), le temps de la compilation chez F-Droid. Le
  `gradle.properties` du projet est réglé pour WSL (768 Mo) : assez pour
  le noyau, pas pour R8 et le lint de diffusion, qui y tombent en
  « OutOfMemoryError: Metaspace » (deuxième essai du 11 octobre 2026).
  Notre CI fait de même par `~/.gradle/gradle.properties`. Aucun effet
  sur l'APK produit. `prebuild` s'exécute dans `app/`, d'où le `../`.
- **Les étiquettes.** `NonFreeAssets` : le contenu est sous CC BY-NC-SA,
  que F-Droid ne tient pas pour libre. `NonFreeNet` : à retirer si le
  code de l'API est publié sous licence libre.
- **Les catégories**, dans la liste de F-Droid : rien pour les
  violences. « Emergency Action » écartée : elle promettrait une alerte
  d'urgence que l'application ne fait pas.
- **Le titre, les descriptions, les images** ne sont pas dans la
  recette : F-Droid les lit dans `fastlane/metadata/android/`, au
  commit de la version qu'il construit.
- **Les mises à jour** sont automatiques : `UpdateCheckMode: Tags`
  surveille les étiquettes `vX.Y.Z`, et `AutoUpdateMode` ajoute la
  version à la recette.
