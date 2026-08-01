# Validation Plan

## Hypotese

Reconic kan rangere relevante bedrifter bedre enn tilfeldig utvalg eller enkel filtrering på geografi, bransje og ansatte.

## Første eksperiment

- Marked: Jæren
- Størrelse: 25–120 ansatte
- Segmenter: bygg/anlegg, industri, transport/logistikk, engros, advokat/regnskap og helse/omsorg
- Omfang: 50–100 manuelt vurderte virksomheter

## Blind vurdering

Den menneskelige vurdereren skal ikke se generatorens score før vurderingen er lagret.

Foreslåtte felt:

- `HumanVerdict`: Ring / Kanskje / Ikke ring
- `HumanReason`
- `DomainCorrect`: Ja / Nei / Usikker
- `ProviderCorrect`: Ja / Nei / Usikker / Ikke relevant
- `TechnicalClaimUseful`: Ja / Nei / Delvis
- `WouldContact`

## Første terskel

Minst 15 av topp 20 skal vurderes som verdt å kontakte.

Dette er en foreløpig terskel og kan skjerpes etter første test.
