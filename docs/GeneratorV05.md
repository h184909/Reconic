# Generator 0.5

Versjon 0.5 bygger videre på commit `1791af75ce836492cee5d89212aaa0f0ae2cdf4a`.

## Nytt

- forklarbar opportunity-score fra 0 til 100
- separat datatillit fra 0 til 100
- markedsmatch, teknisk mulighet og leverandørbilde som egne komponenter
- resultatlisten sorteres etter opportunity, datatillit og deretter ansatte
- rollebaserte leverandørsignaler
- Domeneshop/one.com klassifiseres som DNS-/domeneleverandører, ikke MSP-er
- Telenor, Altibox og GlobalConnect klassifiseres som nett-/konnektivitetsleverandører
- SPF `redirect=` oppdages og vises eksplisitt
- flere SPF-poster blir et tydelig, forklarbart signal
- Microsoft 365 alene er nesten ikke poenggivende
- CSV inneholder score, begrunnelser, usikkerhet, roller og teknologi-fasitkolonner

## Fortsatt ikke implementert

- maskinlært score
- database eller historikk
- endringsdeteksjon
- bekreftede leverandørkundeforhold
- CRM-status, brukerinnlogging eller betaling

## Neste validering

1. Kontroller de øverste 20–30 kandidatene manuelt.
2. La en erfaren MSP-selger rangere det samme utvalget uten å se Reconic-scoren.
3. Sammenlign topplister og begrunnelser.
4. Registrer hvilke poengregler som bommer.
5. Endre vektene først etter dokumenterte funn.
