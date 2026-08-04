# Generator 0.5.1 – scorekalibrering

Versjon 0.5.1 kalibrerer den første opportunity-modellen etter gjennomgang av eksporten fra 0.5.
Målet er å hindre at usikre domener og passive nettsidedomener får en misvisende høy salgsprioritet.

## Manuelle domeneoverstyringer

Bekreftede feil kan legges i:

```text
src/main/resources/domain-overrides.csv
```

Format:

```text
organizationNumber;domain;reason
```

Overstyringen kjøres før DNS-, teknologi- og scoreanalysen. Hele analysen bruker dermed det korrigerte domenet.
Den automatiske kandidaten beholdes i forklaringen for etterprøvbarhet.

Følgende validerte rettelser følger med:

- Sandnes Havneterminal AS: `aktivepost.no` → `sandneshavneterminal.no`
- IXYS AS: `envirex.no` → `ixys.no`

## Prioritetsgrense for e-postbaserte domener

Et domene hentet fra registrert e-post beholder opportunity-scoren, men kan ikke få endelig `Høy` eller `Svært høy` prioritet før domenet er bekreftet.
Prioriteten vises midlertidig som `Middels`, sammen med en forklaring.

Dette skiller mellom:

- hvor interessant signalene ser ut
- hvor trygt det er å handle på dem

Manuelt bekreftede domener får høy domenekonfidens og omfattes ikke av grensen.

## Kalibrering ved manglende MX

Manglende SPF og DMARC vektes nå svakt når domenet ikke har MX.
Et passivt nettsidedomene uten observerbar e-postmottakelse skal ikke få en høy teknisk score bare fordi e-postpolicyene mangler.

Flere SPF-poster og `+all` beholdes som tydelige signaler fordi de er konkrete konfigurasjonsfunn.

## Konservativ leverandørvekt

- Ingen kjent leverandørsignatur: redusert fra 16 til 10 poeng.
- Bare DNS-/infrastrukturspor: redusert fra 12 til 8 poeng.

Fravær av offentlig signatur er fortsatt ikke bevis på at virksomheten mangler IT-leverandør.

## Delte domener

Reconic teller hvor mange kandidater i samme søkeresultat som deler domene.
Ved mer enn én kandidat vises:

- antall virksomheter som deler domenet
- usikkerhetsvarsel om mulig konsern eller felles IT-miljø
- egne CSV-felt

Dette hindrer at samme tekniske miljø fremstår som flere helt uavhengige funn uten kontekst.

## Salgsvalidering

CSV-eksporten har nye tomme fasitkolonner:

- `wouldContact`
- `priorityCorrect`
- `reasonsUseful`
- `manualPriority`
- `salesComment`

Disse skal fylles av en person som faktisk vurderer prospektene. Neste kalibrering bør baseres på denne fasiten, ikke bare på DNS-data.
