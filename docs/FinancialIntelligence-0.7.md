# Reconic 0.7 – Financial Intelligence

## Formål

Tilbakemeldingen fra salg var at en aktuell lead ikke bare må ha relevante
tekniske signaler. Selskapet må også ha økonomi til å kjøpe IT-tjenester.

0.7 legger derfor offentlig regnskapsinformasjon inn i samme Lead Explorer som
DNS-, leverandør- og markedsdata.

## Datakilde

Kilde er Brønnøysundregistrenes åpne Regnskapsregister-API:

```text
GET https://data.brreg.no/regnskapsregisteret/regnskap/{organisasjonsnummer}
```

Reconic bruker bare den åpne delen med nøkkeltall fra siste innsendte/godkjente
årsregnskap.

Ingen Proff.no-scraping brukes.

## Hentede felter

Når de finnes:

- regnskapsår / periodeslutt
- valuta
- driftsinntekter
- driftsresultat
- resultat før skatt
- årsresultat
- egenkapital
- eiendeler
- gjeld
- omløpsmidler
- kortsiktig gjeld
- revisjonsflagg

Reconic beregner i tillegg:

- driftsmargin
- egenkapitalandel
- likviditetsgrad
- omsetning per ansatt

## Begrensninger i Brreg-dataene

Manglende regnskap er **ikke** et negativt salgssignal.

Den åpne tjenesten gir nøkkeltall fra siste årsregnskap og dekker virksomheter
som følger ordinær oppstillingsplan. Banker/forsikring og konserntall kan mangle.

Derfor påvirker økonomidata **ikke opportunity-scoren i 0.7**.

## API-belastning

Som standard hentes regnskap bare for kandidater med funnet domene:

```properties
reconic.finance.only-with-domain=true
```

Det samsvarer med den faktiske salgsflyten: teknisk relevant kandidat først,
økonomisjekk etterpå.

Oppslag kjøres med maksimalt seks samtidige kall, og vellykkede / manglende
resultater caches i minnet gjennom appøkten.

## Underenheter

Underenheter har normalt ikke eget juridisk årsregnskap på samme måte som
hovedenheten. Dersom en kandidat er en underenhet, slår Reconic opp
organisasjonsnummeret til hovedenheten og merker tydelig at regnskapet tilhører
hovedenheten.

## Lead Explorer

Nye filtre:

- minimum omsetning i millioner NOK
- minimum driftsmargin
- positivt / negativt driftsresultat
- status for regnskapsdata
- krev regnskapsdata

Nye sorteringer:

- omsetning
- driftsmargin
- årsresultat

`Forslag: ringeklar` kombinerer:

- høy domenetillit
- aktiv MX
- DMARC mangler eller p=none
- SPF mangler / softfail / flere poster
- regnskapsdata finnes
- positivt driftsresultat

Det er et arbeidsfilter, ikke en ny score.

## Valuta

Regnskap kan leveres i andre valutaer enn NOK. Reconic viser alltid den
rapporterte valutaen.

Filteret `Min. omsetning (mill.)` brukes bare på regnskap rapportert i NOK,
fordi direkte sammenligning på tvers av valuta uten valutakonvertering ville
vært misvisende.

## Videre

Hvis dette viser seg nyttig, er naturlige senere steg:

- flere års historikk når en egnet datakilde/API gjør det praktisk
- vekstsignaler
- finansielle endringsvarsler
- eventuell kontrollert kalibrering av score basert på ekte salgsdata

0.7 legger bevisst ikke økonomi inn i opportunity-score ennå.
