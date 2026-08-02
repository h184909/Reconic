# Technology intelligence 0.4

## Formål

Versjon 0.4 oversetter rå DNS-observasjoner til forklarbare tekniske signaler. Analysen skal gjøre dataene lettere å bruke uten å late som at offentlig DNS alene gir et komplett bilde av en virksomhets IT-miljø.

## E-postplattform

Reconic gjenkjenner foreløpig:

- Microsoft 365
- Google Workspace
- annen plattform
- ingen MX
- ukjent ved teknisk oppslagsfeil

Direkte treff i MX gir høy konfidens. Treff bare i SPF gir middels konfidens fordi SPF kan beskrive utgående e-post bak en separat gateway.

## E-postgateway

Kjente gateway-signaturer i MX:

- Mimecast
- Proofpoint
- Cisco Email Security
- Telenor
- Altibox

Gateway og bakliggende plattform holdes adskilt. Et domene kan for eksempel bruke Proofpoint som innkommende gateway og Microsoft 365 som sannsynlig bakliggende plattform.

## DMARC

DMARC klassifiseres som:

- mangler
- overvåking (`p=none`)
- quarantine
- reject
- ugyldig eller ukjent
- ukjent ved teknisk oppslagsfeil

`p=none` og manglende DMARC er observerbare signaler, men er ikke alene bevis på dårlig sikkerhet eller et kjøpsbehov.

## SPF

SPF klassifiseres som:

- mangler
- flere SPF-poster
- hardfail (`-all`)
- softfail (`~all`)
- neutral (`?all`)
- tillater alle (`+all` eller `all`)
- funnet uten gjenkjent avslutning
- ukjent ved teknisk oppslagsfeil

Reconic trekker også ut kjente plattform- og leverandørspor fra SPF.

## Leverandørsignaturer

Første signatursett omfatter:

- Hjelseth
- Upheads
- ITsjefen
- Intility
- Netpower
- ECIT
- Telenor
- Altibox
- GlobalConnect
- Domeneshop
- one.com

Et treff i én kilde gir middels konfidens. Treff i minst to forskjellige kilder blant MX, SPF og NS gir høy konfidens.

Visningen bruker formuleringen «leverandørsignal», ikke «virksomhetens IT-leverandør». Signaturen kan gjelde DNS, e-post, sikkerhetsgateway eller en eldre teknisk relasjon.

## Bevis

Hvert leverandørsignal beholder råbeviset, for eksempel:

```text
NS: ns1.hjelseth.com
SPF: v=spf1 include:spf.hjelseth.com include:spf.protection.outlook.com -all
```

Dette gjør konklusjonene etterprøvbare og legger grunnlaget for en senere global signaturdatabase.

## Bevisste begrensninger

- Ingen samlet leadscore er implementert.
- Ingen leverandør konkluderes som definitiv.
- Signatursettet er foreløpig kodebasert, ikke lagret i database.
- Historikk og endringsdeteksjon er ikke implementert.
- DNS-data er et øyeblikksbilde og kan endres etter kjøringen.
