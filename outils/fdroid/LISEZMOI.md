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
- **`scandelete: app/debug.keystore`.** La clé de mise au point est
  commitée exprès (`app/build.gradle.kts`) ; elle ne signe jamais la
  version de diffusion, et le scanner de F-Droid n'aime pas les clés.
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
