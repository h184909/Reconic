# Forklarbar opportunity-score 0.5

## Formål

Scoren prioriterer hvilke virksomheter som er mest interessante å undersøke først. Den er en foreløpig **salgsrelevans-score**, ikke en sikkerhetskarakter og ikke en påstand om at virksomheten har et problem.

## To separate tall

Reconic viser alltid:

- `opportunityScore` – hvor interessant kandidaten ser ut basert på marked, tekniske observasjoner og leverandørbilde
- `dataConfidenceScore` – hvor godt datagrunnlaget støtter vurderingen

Et usikkert domene reduserer datatilliten, men endrer ikke automatisk markeds- eller opportunity-signalet.

## Opportunity-komponenter

### Markedsmatch – 0 til 35

Foreløpig vekting basert på:

- antall ansatte
- valgt bransjesegment

Modellen favoriserer foreløpig bedrifter i omtrent 25–120-ansatteområdet. Vektene er hypoteser og skal senere kalibreres mot faktisk salgserfaring.

### Teknisk mulighet – 0 til 45

Observerbare signaler kan gi poeng, blant annet:

- manglende DMARC
- DMARC i overvåkingsmodus (`p=none`)
- flere SPF-poster
- SPF som tillater alle (`+all`)
- manglende eller mykere SPF-policy
- kombinasjonen Microsoft 365 og svakere DMARC-håndheving

Microsoft 365 alene gir nesten ingen poeng fordi valideringsutvalget viste at dette er normaltilstanden for de fleste kandidatene.

### Leverandørbilde – 0 til 20

Leverandørsignaler er nå rollebaserte:

- mulig IT-/driftsleverandør
- DNS-/domeneleverandør
- nett-/konnektivitetsleverandør
- e-post-/gatewayrelaterte roller
- ukjent teknisk rolle

Domeneshop eller one.com via navneservere tolkes derfor ikke som en bekreftet MSP-relasjon. Fravær av en kjent MSP-signatur er heller ikke bevis på at virksomheten mangler leverandør.

## Prioritet

- 80–100: svært høy
- 65–79: høy
- 45–64: middels
- 0–44: lav

## Datatillit

Datatillit beregnes separat fra:

- domenekonfidens
- DNS-status
- e-postplattformens konfidens
- om SPF og DMARC kunne tolkes
- grunnleggende firmakompletthet som telefon, adresse og kontaktfelt

## Forklarbarhet

Hver kandidat lagrer:

- grunner til å undersøke kandidaten
- usikkerhetspunkter
- scorefordeling
- datatillit
- bevis som støtter vurderingen

## Viktig begrensning

Vektene er en første testbar hypotese. De skal ikke brukes som endelig kommersiell modell før resultatene er blindtestet av personer som faktisk selger IT-tjenester og sammenlignet med reelle salgsutfall.

## Kalibrering i 0.5.1

- E-postbaserte, ubekreftede domener kan ikke få endelig høyere prioritet enn `Middels`.
- Opportunity-scoren beholdes, slik at signalstyrken fortsatt kan sammenlignes.
- Manglende SPF og DMARC gir bare svak uttelling når domenet ikke har MX.
- Fravær av leverandørsignatur gir maksimalt 10 poeng, og bare infrastrukturspor gir 8 poeng.
- Delte domener merkes som mulig konsern-/fellesmiljø og vises som usikkerhet.
- Manuelt bekreftede domener fra `domain-overrides.csv` analyseres på nytt og behandles som høy konfidens.
