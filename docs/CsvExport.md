# CSV export 0.5

Begge eksportene inneholder virksomhetsdata, domene, rå DNS, tolkede teknologisignaler og opportunity-vurdering.

## Nye scorefelt

- `opportunityScore`
- `opportunityPriority`
- `marketFitScore`
- `technicalOpportunityScore`
- `providerLandscapeScore`
- `dataConfidenceScore`
- `reasonsToContact`
- `uncertaintyWarnings`
- `scoreEvidence`

## Nye teknologi- og rollefelt

- `spfRedirectTarget`
- `providerRoles`
- leverandørsammendraget inkluderer rolle, beviskilde og konfidens

## Nye manuelle valideringskolonner

- `manualEmailPlatform`
- `emailPlatformCorrect`
- `manualProviderRelationship`
- `providerSignalCorrect`
- `technologyComment`
- `manualDomain`
- `isCorrect`
- `comment`

## Filformat

- UTF-8 med BOM
- semikolon som skilletegn
- CRLF-linjeskift
- korrekt CSV-escaping av semikolon, anførselstegn og linjeskift

## Begrensning

Det siste søkeresultatet ligger bare i HTTP-sessionen. Et nytt søk må kjøres etter omstart eller utløpt session.
