# Generator 0.3

## Innhold

Versjon 0.3 bygger videre på commit `9864eca3397d96b856c43b9fd138a324ab0415f8`.

Den legger til:

- parallell DNS-berikelse for MX, SPF, DMARC og NS
- tydelig skille mellom manglende poster og tekniske oppslagsfeil
- DNS-statistikk og rå observasjoner i resultatsiden
- CSV-eksport av alle kandidater
- tilfeldig, stratifisert valideringsutvalg på 50 kandidater
- manuelle fasitkolonner i eksportene
- tester for DNS-parsing, delvise feil, rekkefølge, utvalg og CSV-format

## Fortsatt ikke implementert

- leverandøridentifisering
- M365-/Entra-analyse
- score eller «derfor ringer du»-tekst
- database eller historikk
- endringsvarsler
- brukerinnlogging og abonnement

## Validering før neste fase

1. Kjør alle tester.
2. Gjør et vanlig kandidatsøk.
3. Kontroller at DNS-status ikke forveksler «ikke funnet» med «feilet».
4. Eksporter valideringsutvalget.
5. Verifiser minst 30 høy-konfidens- og 20 middels-konfidens-domener.
6. Registrer feiltypene før domenefinneren eller leverandørlogikken utvides.
