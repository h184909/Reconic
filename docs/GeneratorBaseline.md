# Generator baseline 0.1

## Formål

Denne versjonen etablerer den første målbare grunnmuren for Reconic:

1. Motta markedskriterier lokalt i nettleseren.
2. Hente virksomheter fra Enhetsregisterets åpne API.
3. Filtrere på kommune, ansattintervall og relevante NACE-segmenter.
4. Vise rå kandidater uten DNS-berikelse, scoring eller spekulative konklusjoner.

## Viktig avgrensning

Dette er **ikke en leadscore**. Resultatene er kandidatbedrifter basert på offentlige virksomhetsdata.

Versjonen gjør foreløpig ikke:

- domenenormalisering eller domenekonfidens
- MX, SPF, DMARC eller NS-oppslag
- M365-/Entra-analyse
- leverandøridentifisering
- scoring eller «derfor ringer du»-begrunnelse
- lagring av resultater

## Hvorfor dette bygges først

Alle senere analyser blir meningsløse dersom kandidatgrunnlaget er feil. Baseline 0.1 skal gi oss et stabilt og repeterbart uttrekk som senere versjoner kan sammenlignes mot.

## Første manuelle test

Kjør:

- kommune: Sandnes
- ansatte: 25–120
- alle segmenter
- underenheter: av

Kontroller et utvalg manuelt mot Brønnøysundregistrene:

- korrekt organisasjonsnummer
- korrekt antall ansatte
- korrekt kommune
- korrekt NACE-kode og segment
- korrekt aktiv status
- korrekt hovedenhet/underenhet

## Neste planlagte arbeid

Neste milepæl er domenefinning med eksplisitt kilde og konfidens:

- registrert hjemmeside
- registrert e-postdomene
- avvisning av private/gratis e-postdomener
- normalisering av URL-er
- manuell fasit for domenenøyaktighet
