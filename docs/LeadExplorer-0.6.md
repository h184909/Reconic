# Reconic 0.6 – Lead Explorer

## Hvorfor 0.6

Tilbakemeldingen fra reell salgsbruk var at DNS-, DMARC-, SPF-, leverandør-,
ansatt- og bransjedata er brukbare, men at en lang CSV-rad ikke er en god
måte å evaluere leads på.

0.6 gjør derfor resultatet til et arbeidsverktøy.

## Ingen ny datainnhenting ved filtrering

Markedssøket fungerer som før:

1. Brønnøysund
2. domene
3. DNS
4. teknologi- og leverandørsignaler
5. passiv public intelligence
6. opportunity-score

Etter dette skjer all Lead Explorer-filtrering lokalt i nettleseren.
Endring av et filter kjører ikke Brønnøysund eller DNS på nytt.

## Filtre

Lead Explorer kan filtrere på:

- fritekstsøk
- minimum/maksimum ansatte
- kommune
- bransje
- e-postplattform
- leverandørsignal
- leverandørrolle
- DMARC
- SPF
- DNSSEC
- domenetillit
- opportunity-prioritet
- krever domene
- krever MX
- kun mulig MSP-signal

## Quick preset

`Forslag: e-postpolicy` setter et bevisst konservativt arbeidsutvalg:

- funnet domene
- høy domenetillit
- aktiv MX
- DMARC mangler eller `p=none`
- SPF mangler, softfail eller flere SPF-poster

Dette er bare et filterforslag. Det endrer ikke opportunity-scoren.

## Leverandørbildet

Toppen av Lead Explorer viser de vanligste observerte leverandørsignalene i
det filtrerte utvalget. Et klikk på en leverandørchip setter leverandørfilteret.

Signalene er fortsatt evidens, ikke bekreftede kundeforhold.

## Mer kompakt resultatvisning

Hovedtabellen er redusert til seks kolonner:

- Bedrift
- Kontakt / domene
- E-postoppsett
- Leverandørsignal
- Opportunity
- Analyse

Telefon og registrert e-post er flyttet frem slik at listen er mer egnet som
grunnlag for cold calls.

Detaljvisningen beholder rå DNS, scoreforklaring, usikkerhet, passive
infrastruktursignaler og alle leverandørbevis.

## Sortering og sider

Sortering kan gjøres på:

- opportunity-score
- ansatte
- datatillit
- bedrift A–Å

Visning kan begrenses til 25, 50 eller 100 leads per side, eller alle.

## Filtrert CSV

`Eksporter filtrert liste` lager en liten lead-CSV direkte i nettleseren med
det filtrerte utvalget.

Den erstatter ikke `Full CSV`, som fortsatt inneholder alle rådata og
valideringsfelt fra backend.

## Bevisst avgrensning

0.6 legger ikke til:

- regnskapsdata
- database
- historikk
- varsler
- aktiv sikkerhetsskanning

Neste store datalag bør være økonomi/lønnsomhet, fordi det ble identifisert
som en sentral del av den faktiske salgsprosessen.
