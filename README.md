# Reconic

**Market intelligence for IT providers.**

Reconic skal ikke bli enda en generisk leadgenerator. Målet er å bygge en forklarbar motor som konsekvent finner bedriftene en erfaren MSP-selger bør kontakte før konkurrentene gjør det.

## Prosjektets viktigste spørsmål

> Hvorfor bør akkurat denne bedriften kontaktes nå?

En funksjon som ikke bidrar til å besvare dette spørsmålet, er ikke prioritert i valideringsfasen.

## Nåværende fase

Vi bygger **ikke SaaS-produktet ennå**. Først skal vi finne ut om kjernen er verdifull nok til at resten er verdt å bygge.

Prioritet nå:

1. finne riktige bedrifter
2. identifisere korrekt domene
3. samle verifiserbare tekniske signaler
4. skille fakta fra antakelser
5. rangere leads med forklarbar begrunnelse
6. validere resultatene mot ekte MSP-selgere

Ikke prioritert ennå:

- innlogging og brukeradministrasjon
- betaling og abonnement
- flerleietakerarkitektur
- CRM-integrasjoner
- produksjonsdrift
- avansert design

## Første suksesskriterium

Reconic må bestå en blindtest:

> Blant de 20 høyest rangerte bedriftene skal minst 15 vurderes som verdt å kontakte av en erfaren MSP-selger.

I tillegg skal vi måle:

- andel korrekte domener
- andel tekniske observasjoner med tydelig kilde
- nøyaktighet i mulig leverandøridentifisering
- datadekning og datakvalitet
- hvor ofte begrunnelsen faktisk er nyttig i salg

Hvis generatoren ikke gir bedre prioritering enn en enkel bedriftsliste, skal prosjektet stoppes eller endres før vi bygger resten.

## Grunnprinsipper

### Forklarbarhet

Ingen høy score uten en forståelig begrunnelse.

### Fakta før antakelser

Skriv «M365 ikke påvist», ikke «bruker ikke M365», når dataene ikke beviser det siste.

### Ukjent er ikke det samme som mangler

Et mislykket oppslag eller manglende domene skal aldri tolkes som et sikkerhetsproblem.

### Historikk fremfor overskriving

Langsiktig skal observasjoner lagres med tidspunkt. Endringer i MX, DMARC, DNS eller tenant-status er potensielle kjøpssignaler og kan ikke rekonstrueres senere dersom bare siste tilstand beholdes.

### Global kunnskap og private kundedata skal skilles

Globalt:

- virksomhets- og domeneobservasjoner
- leverandørsignaturer
- teknologifingeravtrykk
- historiske endringer

Privat per kunde:

- notater
- salgsstatus
- lagrede lister
- hvem som er kontaktet
- CRM-lenker

## Hva som kan bli vollgraven

Koden og de offentlige oppslagene kan kopieres. Det som kan bli vanskelig å kopiere er:

1. kvalitetssikret signaturdatabase for IT-leverandører
2. teknologifingeravtrykk med kjent presisjon
3. historiske infrastrukturendringer
4. dokumentert kunnskap om hvilke signaler som faktisk gir møter og kunder
5. tilbakemeldinger som forbedrer modellen over tid

## Leadmodell

En lead er ikke bare en bedrift. Den må ha:

- realistisk kommersiell match
- et observerbart behov eller kjøpssignal
- tilstrekkelig datakvalitet
- forklarbar begrunnelse
- høy nok prioritet til at en selger faktisk vil bruke tid på den

På sikt bør resultatet vise separate delscorer:

- **Business fit**
- **Technical opportunity**
- **Sales opportunity**
- **Data confidence**
- **Overall priority**

## Første valideringsløp

1. Kjør legacy-generatoren mot et fast marked.
2. Lagre baseline-resultatet.
3. La Lan-x vurdere 50–100 bedrifter uten å se generatorens score.
4. Merk hver bedrift som `Ring`, `Kanskje` eller `Ikke ring`.
5. Registrer feil domene, feil leverandør og misvisende tekniske konklusjoner.
6. Forbedre én del av motoren om gangen.
7. Kjør samme testsett på nytt og mål om resultatet faktisk blir bedre.

## Utviklingsrekkefølge

1. Baseline og testsett
2. Domenefinning og domenekonfidens
3. DNS/MX/SPF/DMARC-observasjoner
4. M365- og fødereringssignaler
5. Leverandørsignaturer
6. Forklarbar scoremodell
7. Historiske observasjoner og endringsdeteksjon
8. Enkel intern webvisning
9. SaaS-funksjoner først etter dokumentert verdi

## Prosjektstruktur

```text
Reconic/
├── docs/                  # Produktbeslutninger og valideringsplan
├── generator/
│   ├── legacy/            # Original generator, beholdes urørt
│   ├── input/             # Ikke-sensitive testinnganger
│   ├── output/            # Genererte resultater, ignoreres av Git
│   └── samples/           # Små anonymiserte eksempler
├── scripts/               # Hjelpescript
├── test-data/             # Testsett og menneskelig fasit
└── src/                   # Spring Boot-skallet og senere leadmotor
```

Se `docs/` for mer detaljer.

## Lokal kjøring

Krav: Java 21.

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

macOS/Linux:

```bash
./mvnw spring-boot:run
```

Åpne deretter `http://localhost:8080`.

## Status

Prosjektet er i **generator validation**. Ingen påstander om produktverdi skal gjøres før motoren er målt mot menneskelig vurdering og reelt salgsarbeid.
