# Google Play : ce qu'on répond, et pourquoi

Les réponses préparées pour la console Play, à reporter à la main. Tout
ce qui est affirmé ici doit rester vrai du code : si l'application
change ce qu'elle envoie ou ce qu'elle garde, ce fichier et la section
« application » des mentions légales du site changent avec.

## Le compte

- **Compte d'organisation**, au nom de l'organisation porteuse (une
  association), avec son numéro D-U-N-S. Pas de compte personnel : il
  lierait l'application à une personne, et imposerait un test fermé de
  12 testeurs pendant 14 jours.
- **Nom du développeur affiché : Cooplib.** Play affiche aussi, dans
  « À propos du développeur », le nom légal et l'adresse de
  l'organisation.

## La fiche

Tout est dans `fastlane/metadata/android/fr-FR/` : `title.txt`,
`short_description.txt`, `full_description.txt`, `images/icon.png`
(512 × 512), `images/featureGraphic.png` (1024 × 500). Pour les captures,
les maquettes de `python3 outils/maquettes.py` plutôt que les captures
brutes.

- Catégorie : **Éducation** (à défaut, Santé et remise en forme).
- Courriel de contact : contact@cooplib.fr. Site : https://violentometres.fr.
- **Politique de confidentialité : https://violentometres.fr/mentions**
  (section « L'application Android : confidentialité », lien aussi dans
  l'application, écran des conditions d'utilisation).

## La signature : « utiliser ma propre clé »

Sans ça, Google signe avec SA clé, et un APK de Play ne mettrait plus à
jour celui de F-Droid ou du site. Dans la console, à la création de la
première version : *Signature d'application par Google Play* → *Utiliser
une autre clé* → *Exporter et importer une clé depuis un keystore Java*.
Play fournit `pepk.jar` et une clé publique de chiffrement ; on lance,
là où est la clé :

```
java -jar pepk.jar --keystore=$HOME/cles/cooplib-util.jks --alias=util \
  --output=sortie.zip --include-cert --rsa-aes-encryption \
  --encryption-key-path=encryption_public_key.pem
```

et on importe `sortie.zip`. La même clé sert de clé d'importation :
l'App Bundle de la CI (`app-release.aab`, joint à chaque version
publiée) en est déjà signé.

## Accès à l'application (App access)

Toute l'application est derrière un code : il faut le dire, sinon le
relecteur reste sur la calculatrice et refuse. À coller tel quel :

> On first launch, the app walks through a short setup. Choose how it
> appears on the home screen (calculator or real name), then choose any
> 4 to 12 digit code: no account, nothing is sent. If you kept the
> calculator, type your code and then "=" to open the app; the
> calculator otherwise works normally. "Quitter vite" (top right) closes
> the app and turns it back into the calculator; reopen it with the same
> code.

## Notes pour les relecteurs (le déguisement)

> Violentomètres is a French-language tool for people experiencing
> violence (in a relationship, at work, in the family...). Its users may
> have their phone checked by the person who harms them. For their
> safety, the app can appear on the home screen as a working calculator,
> and the real app opens only after typing a code the user chose. This
> is offered on first launch and can be turned off in the settings; the
> app explains what this does and does not protect. It does not hide
> from the system app list, collects nothing, and has no ads, trackers
> or Google services. Similar disguised apps exist for the same purpose.
> Source code: https://github.com/Cooplib/violentometres-app

## Sécurité des données (Data safety)

- **Collecte ou partage de données : oui**, et seulement à l'initiative
  de la personne :
  - *Activité dans l'application → Autre contenu généré par
    l'utilisateur* : les témoignages publiés, les signalements. Collectés,
    **non partagés**, pour une **fonctionnalité de l'application**.
    Facultatif. Publics par nature (un témoignage publié s'affiche sur le
    site) : c'est une action de la personne, pas un partage avec un
    tiers ;
  - *Activité dans l'application → Interactions avec l'application* :
    les « J'aime ». Collectés, non partagés, fonctionnalité. Facultatif ;
  - *Identifiants de l'appareil ou autres* : l'identifiant tiré au
    hasard par l'application, envoyé avec un témoignage, un signalement ou un
    « J'aime ». Le serveur n'en garde que des empreintes. Collecté, non
    partagé, fonctionnalité.
- **Rien d'autre** : ni localisation, ni contacts, ni nom, ni courriel,
  ni réponses au point (elles ne quittent jamais le téléphone), ni
  adresse IP enregistrée, ni identifiant publicitaire.
- **Chiffrement en transit : oui** (HTTPS seulement).
- **Suppression des données** : il n'y a pas de compte. On peut demander
  le retrait d'un témoignage à contact@cooplib.fr (comme sur le site).

## Classification du contenu

Questionnaire IARC, catégorie « Référence, actualités ou éducation » :

- violence : **décrite par écrit** (situations, témoignages), jamais montrée
  en image ; pas de violence réaliste représentée ;
- sexualité : **évoquée par écrit** dans des situations de violences
  sexuelles, sans contenu explicite ;
- contenu généré par les utilisateurs : **oui** (les témoignages), modéré,
  avec signalement ;
- échanges entre utilisateurs, localisation, achats : **non**.

Attendu : 16 ans et plus. Ne pas minimiser : une classification trop
basse expose à un retrait.

## Public cible

**18 ans et plus**, éventuellement **16-17** : pas les enfants. Une
cible enfant ferait entrer l'application dans le programme « Familles »,
aux règles incompatibles avec des témoignages de violences.

## Autres déclarations

- Publicités : **non**. Achats dans l'application : **non**.
- Application gouvernementale, financière, d'actualités : **non**.
- Applications de santé : **non** (pas de suivi médical ni de données de
  santé).
- Contenu généré par les utilisateurs : **oui** ; on peut signaler
  chaque témoignage depuis l'application, les conditions d'utilisation
  interdisent de nommer une personne réelle, et les témoignages sont modérés
  sur le site.
