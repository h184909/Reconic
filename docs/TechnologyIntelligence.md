# Technology intelligence 0.5

## Leverandørroller

Et leverandørspor sier nå både hvem som er observert og hvilken teknisk rolle signaturen vanligvis peker mot:

- `MSP_CANDIDATE`
- `DNS_PROVIDER`
- `CONNECTIVITY_PROVIDER`
- `EMAIL_PROVIDER`
- `EMAIL_SECURITY_PROVIDER`
- `OUTBOUND_EMAIL_PROVIDER`
- `UNKNOWN_TECHNICAL_PROVIDER`

Eksempel:

```text
Domeneshop
Rolle: DNS-/domeneleverandør
Bevis: NS
```

Dette skal ikke oversettes til at Domeneshop drifter virksomhetens IT.

## Foreløpig rollefordeling

- Hjelseth, Upheads, ITsjefen, Intility, Netpower og ECIT: mulig IT-/driftsleverandør
- Domeneshop og one.com: DNS-/domeneleverandør
- Telenor, Altibox og GlobalConnect: nett-/konnektivitetsleverandør

Rollene er hypoteser knyttet til signaturene og kan senere flyttes til en global signaturdatabase.

## SPF redirect

SPF med `redirect=<domene>` klassifiseres nå separat, og måldomenet lagres som `spfRedirectTarget`.

Reconic følger foreløpig ikke redirect-kjeden rekursivt. Det ville krevd nye DNS-oppslag, løkkedeteksjon og egne tidsavbrudd. I 0.5 vises derfor delegeringen eksplisitt uten å late som den endelige policyen allerede er analysert.

## Microsoft 365

Microsoft 365 beholdes som teknologi- og filtersignal. Det gir svært lite opportunity-poeng alene fordi valideringsutvalget viste at plattformen er svært vanlig.

## Tolkning

Alle signaler er offentlige tekniske observasjoner. De er ikke bekreftede kontraktsforhold, og de kan være historiske, delte mellom leverandører eller knyttet til bare én del av infrastrukturen.
