# DNS enrichment 0.3

## Formål

Versjon 0.3 gjør faktabaserte DNS-oppslag for kandidatene som allerede har fått et domene i versjon 0.2.
Den skal samle observerbare tekniske signaler uten å gjøre salgs- eller sikkerhetskonklusjoner ennå.

## Oppslag

For hvert kandidatdomene hentes:

- `MX` på rotdomenet
- `TXT` på rotdomenet, filtrert til SPF-poster som starter med `v=spf1`
- `TXT` på `_dmarc.<domene>`, filtrert til en DMARC-post som starter med `v=DMARC1`
- `NS` på rotdomenet

DNS kjøres ikke for kandidater uten domene.

## Status

Hvert domene får én av fire statuser:

- `SUCCESS`: alle fire oppslagstyper kunne gjennomføres
- `PARTIAL`: minst ett oppslag lyktes og minst ett feilet teknisk
- `FAILED`: alle oppslagstypene feilet teknisk
- `SKIPPED`: kandidaten manglet domene

En vellykket DNS-forespørsel som returnerer null poster er **ikke** en feil. Derfor er «ikke funnet» forskjellig fra «oppslag feilet».

## Viktig tolkning

Disse observasjonene sier bare hva offentlig DNS viste på kjøretidspunktet.

- Manglende SPF eller DMARC skal ikke alene omtales som et sikkerhetsbrudd.
- Manglende MX kan bety at domenet ikke mottar e-post.
- En navneserver eller MX-vert identifiserer ikke automatisk virksomhetens IT-leverandør.
- Resultater fra domener med middels konfidens må fortsatt verifiseres manuelt.

## Ytelse og feil

Oppslag kjøres parallelt med maksimalt 24 samtidige domener. Hvert JNDI-oppslag har kort timeout og én retry.
En uventet feil på ett domene skal ikke stoppe hele kandidatsøket; feilen lagres på den aktuelle kandidaten.

## Neste bruk

DNS-dataene er grunnlaget for senere arbeid med:

1. leverandørsignaturer
2. Microsoft 365-signaler
3. historiske observasjoner
4. endringsdeteksjon
5. forklarbar scoring

Ingen av disse konklusjonene er implementert i 0.3.
