# Reconic 0.5.2.1 – DNSSEC hardening

## Hvorfor denne patchen finnes

I 0.5.2 brukte Reconic JDK/JNDI DNS-oppslag også for `DS`-poster.
I praksis returnerte JNDI:

```text
Unknown resource record type 'DS'
```

Dermed ble DNSSEC `Ukjent` på alle analyserte domener og `publicLookupWarnings`
ble fylt med støy.

## DNSSEC i 0.5.2.1

DS-oppslag går nå gjennom Google Public DNS sitt DNS-over-HTTPS JSON-endepunkt.

Reconic spør kun etter:

```text
<domene> DS
```

og tolker:

- DNS-svar med én eller flere RR type 43 (`DS`) → `Funnet`
- gyldig NOERROR-svar uten DS → `Ikke funnet`
- NXDOMAIN → `Ikke funnet`
- HTTP-feil, timeout eller andre DNS-returkoder → `Ukjent`

`AD`-flagget beholdes som ekstra evidence når resolveren markerer svaret som
DNSSEC-validert.

Dette er fortsatt et passivt offentlig DNS-oppslag. Reconic gjør ikke
portskanning eller kontakt med tjenester på kundens infrastruktur.

## Norid er tatt ut av lead-enrichment

0.5.2 hadde teknisk støtte for Norid RDAP, men den var avslått.

Etter kontroll av Norids bruksvilkår er automatisk Norid-berikelse nå
bevisst fjernet fra Reconics lead-workflow. Norids offentlige lookup-vilkår
forbyr kommersiell bruk av oppslagsdata, inkludert målrettet markedsføring.

Derfor:

- Reconic sender ingen Norid-oppslag
- Norid-konfigurasjonen er fjernet
- Norid-feltene er fjernet fra CSV-eksporten
- de gamle feltene i internmodellen står kun igjen midlertidig for
  bakoverkompatibilitet med 0.5.2

DNSSEC kommer nå fra DNS, ikke Norid.

## Duplicate-domain caching

Bedrifter i samme konsern kan dele domene. 0.5.2.1 analyserer hvert unike
domene én gang per søk og gjenbruker resultatet for alle kandidater med
samme domene.

Dette reduserer:

- unødvendige DNS-oppslag
- DoH-kall
- CT-kall dersom CT senere aktiveres
- total kjøretid og ekstern belastning

## Certificate Transparency

CT forblir opt-in:

```properties
reconic.public-intelligence.ct.enabled=false
```

Den påvirker fortsatt ikke opportunity-score.

## Scoring

Ingen av endringene i 0.5.2.1 endrer opportunity-score.

Målet med patchen er datakvalitet og korrekt innsamling før v0.6 Lead Explorer.
