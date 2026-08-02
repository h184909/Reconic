# Generator 0.4

## Innhold

Versjon 0.4 bygger videre på DNS- og CSV-versjonen 0.3 og legger til:

- gjenkjenning av Microsoft 365 og Google Workspace
- separat identifisering av e-postgateway
- forståelig DMARC-klassifisering
- SPF-klassifisering og kjente SPF-signaler
- leverandørsignaturer fra MX, SPF og NS
- bevis og konfidens for hvert leverandørsignal
- kompakt resultattabell med rådata i en detaljvisning
- nye teknologifelter i CSV-eksporten
- tester for plattform, gateway, DMARC, SPF, leverandørsignaturer og tekniske oppslagsfeil

## Nye resultatfelter

CSV-eksporten inneholder blant annet:

```text
emailPlatform
emailPlatformConfidence
emailGateway
emailGatewayConfidence
dmarcPosture
spfPosture
spfAllMechanism
spfSignals
providerSignals
providerEvidence
technologyEvidence
```

## Viktig tolkning

Reconic skiller mellom observasjon og konklusjon:

- «NS og SPF matcher Hjelseth» er et dokumentert signal.
- «Hjelseth drifter all IT for virksomheten» støttes ikke av DNS-data alene.

## Neste fase

Versjon 0.5 kan bygge en første forklarbar opportunity-score, men bare etter at v0.4-signalene er kontrollert mot ekte virksomheter og kjente leverandørforhold.
