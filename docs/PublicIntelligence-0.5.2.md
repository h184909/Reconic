# Reconic 0.5.2 – public/passive intelligence

## Formål

0.5.2 utvider datagrunnlaget uten aktiv portskanning, sårbarhetsskanning eller andre inngrep mot virksomhetenes systemer.

Reconic beskriver observerbare signaler. Det skal fortsatt ikke påstås at et firma har en bestemt IT-leverandør eller et bestemt sikkerhetsnivå uten tilstrekkelig bevis.

## Aktive standardoppslag

For hvert funnet domene undersøkes:

- `_mta-sts.<domene>` TXT for MTA-STS
- `_smtp._tls.<domene>` TXT for TLS-RPT
- DS-poster for DNSSEC
- `autodiscover.<domene>` CNAME som ekstra e-postplattformsignal

Dette er passive DNS-oppslag.

## Norid RDAP

For `.no`-domener finnes en offisiell RDAP-tjeneste hos Norid som er laget for automatiserte oppslag.

Reconic har støtte for å hente:

- om domenet finnes i RDAP
- DNSSEC/delegationSigned når feltet er tilgjengelig
- registreringsdato
- sist endret-dato

Funksjonen er som standard deaktivert fordi offentlig tilgang kan være ratebegrenset. Aktiver eksplisitt i `application.properties`:

```properties
reconic.public-intelligence.norid.enabled=true
```

Reconic bruker bare oppslag av et allerede kjent domene. Versjonen prøver ikke å massehente Norids database eller omgå tilgangsbegrensninger.

## Certificate Transparency

Støtte for Certificate Transparency via `crt.sh` finnes som en opt-in valideringskilde.

Den kan finne offentlige sertifikatnavn under domenet, for eksempel:

- `vpn.example.no`
- `portal.example.no`
- `mail.example.no`

Dette er kun et navn observert i en offentlig sertifikatlogg. Det er ikke bevis på at tjenesten er aktiv, eksponert eller sårbar.

Aktiver eksplisitt:

```properties
reconic.public-intelligence.ct.enabled=true
```

CT er deaktivert som standard fordi `crt.sh` er en ekstern tjeneste som kan være treg eller ratebegrenset.

## Nye CSV-felt

- `mtaStsStatus`
- `mtaStsRecord`
- `tlsRptStatus`
- `tlsRptRecord`
- `dnssecStatus`
- `autodiscoverTarget`
- `noridStatus`
- `noridDnssec`
- `noridCreatedAt`
- `noridUpdatedAt`
- `certificateTransparencyStatus`
- `certificateNames`
- `publicLookupWarnings`
- `publicInfrastructureEvidence`

## Scoring

De nye signalene påvirker **ikke** opportunity-scoren i 0.5.2.

Dette er bevisst. Først samler vi inn data og ser om selgere faktisk bruker signalene. Deretter kan enkelte signaler eventuelt bli filter eller scorekomponent i en senere versjon.

## DMARC-oversikt

0.5.2 retter samtidig oversiktsstatistikken slik at kandidater uten funnet domene ikke telles som «DMARC mangler».

## Sikker avgrensning

Denne versjonen gjør ikke:

- portskanning
- banner grabbing
- innloggingstesting
- sårbarhetsskanning
- exploit-testing
- aktiv probing av tjenester funnet i Certificate Transparency

Det holder Reconic i sporet offentlig/passivt lead intelligence.
