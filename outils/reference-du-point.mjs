/*
 * Joue des centaines de points avec le code DU SITE, et écrit ce qu'il
 * en sort : la référence que le portage Kotlin doit retrouver, action
 * après action (DerouleContreLeSiteTest).
 *
 *   node outils/reference-du-point.mjs <sortie.json>
 *
 * Lancé par Gradle avant les tests du noyau, sur le site cloné à côté :
 * la comparaison se fait toujours contre le site tel qu'il est, jamais
 * contre une copie figée qui aurait pu dériver sans que personne le
 * voie.
 *
 * Les parties sont tirées au hasard, mais d'une graine fixe : un écart
 * se rejoue à l'identique. Et seulement des actions qu'un écran du site
 * propose dans la phase où l'on est (pages/Test.jsx) : un enchaînement
 * impossible là-bas ne prouverait rien ici.
 */

import { readFileSync, writeFileSync, existsSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

const ICI = dirname(fileURLToPath(import.meta.url));
const SITE = resolve(ICI, "../../violentometres-frontend/src/utils");
const POOLS = resolve(ICI, "../noyau/src/test/resources/api");

if (!existsSync(`${SITE}/deroule.js`)) {
  console.error(`Le site n'est pas à côté : ${SITE}/deroule.js introuvable.`);
  console.error("Il faut ../violentometres-frontend, cloné à côté de ce dépôt.");
  process.exit(1);
}

const d = await import(pathToFileURL(`${SITE}/deroule.js`).href);
const { recommander } = await import(pathToFileURL(`${SITE}/recommandations.js`).href);

const sortie = process.argv[2];

if (!sortie) {
  console.error("Usage : node outils/reference-du-point.mjs <sortie.json>");
  process.exit(1);
}


// mulberry32 : court, sans dépendance, et le même partout.
function hasard(graine) {
  let a = graine >>> 0;
  return () => {
    a = (a + 0x6d2b79f5) >>> 0;
    let t = a;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

function tirer(alea, poids) {
  const total = poids.reduce((s, [, p]) => s + p, 0);
  let x = alea() * total;
  for (const [action, p] of poids) {
    x -= p;
    if (x < 0) {
      return action;
    }
  }
  return poids[poids.length - 1][0];
}


/*
 * Ce que chaque écran propose, avec un poids : surtout répondre, un
 * peu de tout le reste, pour que chaque transition soit jouée souvent.
 */
const ACTIONS = {
  intro: [["commencer", 1]],
  questions: [["oui", 30], ["non", 30], ["passer", 10], ["precedent", 8], ["pause", 3], ["terminer", 1]],
  sas: [["reprendreApresSas", 10], ["revoirLesPositifs", 2], ["pause", 2], ["terminer", 1]],
  positifs: [["revenirAuSas", 4], ["terminer", 1]],
  aide: [["apresAlerte", 10], ["pause", 2], ["terminer", 1]],
  alerte: [["apresAlerte", 3], ["terminer", 1]],
  pause: [["reprendre", 5], ["terminer", 1]]
};


function jouer(pool, etat, action, maintenant) {

  switch (action) {
    case "commencer": return d.commencer();
    case "oui":
    case "non":
    case "passer": return d.repondre(pool, etat, d.situationsCourantes(pool, etat)[etat.index], action, maintenant);
    case "precedent": return d.precedent(pool, etat) ?? {};
    case "pause": return d.pause(etat);
    case "reprendre": return d.reprendre(etat);
    case "terminer": return d.terminer(etat.answers, maintenant);
    case "reprendreApresSas": return d.reprendreApresSas();
    case "revoirLesPositifs": return d.revoirLesPositifs();
    case "revenirAuSas": return d.revenirAuSas();
    case "apresAlerte": return d.apresAlerte(pool, etat, maintenant);
    default: throw new Error(`Action inconnue : ${action}`);
  }

}


// L'état tel que le site le garde, sans `counted` (le compteur /done,
// que l'application n'envoie pas) ni `titre` (ajouté par le composant).
function photo(etat) {
  const { counted: _c, titre: _t, ...reste } = etat;
  return reste;
}

const ids = liste => liste.map(x => x.id);

// Ce que les écrans montrent, calculé depuis l'état : c'est ça qui doit
// être identique, autant que l'état lui-même.
function derive(pool, etat) {

  const reconnues = d.situationsReconnues(pool, etat.answers);

  return {
    question: d.situationsCourantes(pool, etat)[etat.index]?.id ?? null,
    niveauAtteint: d.niveauAtteint(reconnues),
    aidesDuResultat: ids(d.aidesDuResultat(pool, etat)),
    aidesDAlerte: etat.phase === "alerte" ? ids(d.aidesDAlerte(pool, etat)) : [],
    aidesReconnues: ids(d.aidesReconnues(pool, reconnues)),
    reconnuesParNiveau: d.reconnuesParNiveau(pool, reconnues).map(g => [g.niveau.position, ids(g.situations)]),
    formes: d.formesReconnues(reconnues).map(f => [f.cle, f.combien])
  };

}

function recommandations(pool, etat) {

  const r = recommander(pool, d.situationsReconnues(pool, etat.answers));

  const paire = liste => liste.map(x => [x.id, x.recoupements]);

  return {
    violentometres: paire(r.violentometres),
    parcours: paire(r.parcours),
    recits: paire(r.recits),
    mecanismes: paire(r.mecanismes)
  };

}


function partie(nomPool, pool, graine) {

  const alea = hasard(graine);

  const audience = alea() < 0.7 ? "personne" : "proche";

  let etat = { ...d.DEPART, ...d.choisirPublic(audience) };

  const pas = [];

  // Un plafond, au cas où « précédent » ferait tourner en rond.
  for (let n = 0; n < 400 && etat.phase !== "resultat"; n++) {

    const action = tirer(alea, ACTIONS[etat.phase]);

    const maintenant = 1_800_000_000_000 + graine * 1000 + n;

    etat = { ...etat, ...jouer(pool, etat, action, maintenant) };

    pas.push({ action, maintenant, etat: photo(etat), derive: derive(pool, etat) });

  }

  return { pool: nomPool, graine, audience, pas, recommandations: recommandations(pool, etat) };

}


const parties = [];

for (const [nom, combien] of [["point-orientation.json", 150], ["point-violentometre.json", 80]]) {

  const pool = JSON.parse(readFileSync(`${POOLS}/${nom}`, "utf8"));

  for (let graine = 1; graine <= combien; graine++) {
    parties.push(partie(nom, pool, graine));
  }

}

writeFileSync(sortie, JSON.stringify({ parties }));

const nbPas = parties.reduce((s, p) => s + p.pas.length, 0);

console.log(`${parties.length} parties, ${nbPas} actions → ${sortie}`);
