#!/bin/sh
# Enregistre de vraies réponses de l'API pour les tests du noyau.
#
# Des LECTURES seulement : rien ne se supprime sur ce site, une écriture
# de sonde y resterait pour toujours.
#
# Les tests vérifient qu'on sait lire ce que l'API envoie vraiment, pas
# ce qu'on croit qu'elle envoie : les deux formes du point, par exemple,
# n'ont pas les mêmes champs (celui d'un violentomètre n'a ni récits ni
# parcours). À relancer quand l'API change de forme.
set -eu

BASE="${BASE:-https://violentometres.fr/api}"
ICI="$(dirname "$0")/../noyau/src/test/resources/api"
mkdir -p "$ICI"

prendre() {
    curl -sSf "$BASE/$1" -o "$ICI/$2"
    echo "$2 : $(wc -c < "$ICI/$2") octets"
}

premier_id() {
    python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))[0]["id"])' "$ICI/$1"
}

prendre violentometers violentometres.json
prendre parcours parcours.json
prendre stories recits.json
prendre aides aides.json
prendre tests/orientation/tout point-orientation.json
prendre "history/recent?limit=30" historique.json

prendre "violentometers/$(premier_id violentometres.json)" violentometre.json
prendre "tests/violentometer/$(premier_id violentometres.json)" point-violentometre.json
prendre "parcours/$(premier_id parcours.json)" un-parcours.json
prendre "tests/parcours/$(premier_id parcours.json)" point-parcours.json
