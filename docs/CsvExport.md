# CSV export 0.3

## Eksporter alle

`/export/all.csv` eksporterer alle kandidatene fra det siste søket i nettleserøkten.
Filen inneholder:

- virksomhetsdata fra Enhetsregisteret
- funnet domene, kilde og konfidens
- MX, SPF, DMARC og NS
- tekniske DNS-feil
- tomme kolonner for manuell fasit

## Tilfeldig valideringsutvalg

`/export/validation.csv` lager et tilfeldig utvalg på inntil 50 kandidater som har domene.
Når datagrunnlaget er stort nok, velges:

- 30 kandidater med høy domenekonfidens
- 20 kandidater med middels domenekonfidens

Hvis en gruppe har for få kandidater, fylles resten fra andre kandidater med domene.
Kandidater uten domene tas ikke med i domenepresisjonsutvalget.

## Manuelle kolonner

De siste kolonnene er tomme:

- `manualDomain`
- `isCorrect`
- `comment`

Disse fylles manuelt før presisjon beregnes.

## Filformat

- UTF-8 med BOM for god støtte i Excel
- semikolon som skilletegn
- CRLF-linjeskift
- verdier med semikolon, anførselstegn eller linjeskift blir korrekt sitert

## Begrensning

Det siste resultatet lagres bare i den lokale HTTP-sessionen. Etter omstart av applikasjonen eller utløpt session må et nytt søk kjøres før eksport.
