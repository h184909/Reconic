# Reconic 0.8 – Lead Dossier

## Hva 0.8 løser

Lead Explorer gjør det mulig å finne en relevant ringeliste. Neste problem er
å slippe å lese en bred tabell eller hoppe mellom mange kilder før en cold call.

0.8 introduserer derfor et **Lead Dossier** per bedrift.

Klikk `Åpne dossier` på en rad. Reconic åpner da en samlet salgsvisning uten
nye nettverksoppslag.

## Dossieret inneholder

### Cold-call brief

- hvorfor Reconic mener leaden er verdt å undersøke
- eksplisitte usikkerhetspunkter
- opportunity og datatillit

### Kontakt

- telefon
- e-post
- domene
- adresse
- NACE/bransje

### Økonomi

- omsetning
- driftsresultat
- driftsmargin
- årsresultat
- egenkapital
- egenkapitalandel
- gjeld
- omsetning per ansatt
- regnskapsår
- kilde / hovedenhet

### E-post / observerbar konfigurasjon

- e-postplattform
- gateway
- DMARC
- SPF
- DNSSEC
- MTA-STS
- TLS-RPT
- autodiscover

Dette omtales bevisst som offentlig observerbar konfigurasjon, ikke som en full
sikkerhetsvurdering.

### Leverandørbildet

Alle provider-signaler vises med:

- mulig leverandør
- rolle
- konfidens
- kildetype
- konkret evidence

Reconic påstår fortsatt ikke at dette er et bekreftet kundeforhold.

### Domene / datakvalitet

- domenekilde
- domenetillit
- delt konserndomene
- manuell override/verifikasjonsbehov

### Rå observasjoner

Rå MX, SPF, DMARC, NS, DNS-status og public-intelligence evidence ligger
sammenfoldet nederst i dossieret.

## Kopier call brief

`Kopier call brief` lager en kort tekst som kan limes i interne notater før
en samtale. Den inkluderer:

- bedrift og org.nr.
- kontaktinfo
- økonomi
- viktigste IT-signaler
- primært leverandørsignal
- Reconic opportunity
- grunner til å undersøke

Dette lagres ikke i Reconic. Teksten kopieres bare til utklippstavlen.

## 0.7.1-kalibreringen er inkludert

`Forslag: ringeklar` er nå `Forslag: ringeklar 30M+`.

Presetet krever:

- funnet domene
- høy domenetillit
- aktiv MX
- DMARC mangler / p=none
- SPF mangler / softfail / flere poster
- regnskapsdata
- positivt driftsresultat
- minst **30 millioner NOK i omsetning**

30 millioner er foreløpig en praktisk standard, ikke en score-regel.
Brukeren kan fortsatt endre omsetningsfilteret fritt.

## Ingen database ennå

Dossieret bygges fra siste søkeresultat som allerede finnes i nettleseren.
Det gjør at 0.8 ikke krever:

- database
- brukerinnlogging
- lead-lagring
- CRM
- nye API-kall når dossieret åpnes

Dette holder valideringsfasen enkel.

## Scoring

Økonomi påvirker fortsatt ikke opportunity-score i 0.8.

Det er fortsatt bedre å la selgeren bruke økonomi som filter før vi har nok
reell feedback til å kalibrere scoren.
