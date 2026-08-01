# Domain discovery 0.2

## Formål

Denne milepælen finner et sannsynlig virksomhetsdomene før Reconic gjør DNS-, M365- eller leverandøranalyse.
Et feil domene gjør alle senere observasjoner misvisende, derfor prioriteres presisjon fremfor maksimal dekning.

## Kilder og konfidens

1. **Registrert hjemmeside — høy konfidens**
   - URL normaliseres til registrerbart rotdomene.
   - `www`, protokoll, port, sti og query fjernes.
   - Sosiale medier, bedriftskataloger og delte hjemmesideplattformer avvises.

2. **Registrert e-post — middels konfidens**
   - Brukes bare når hjemmesiden mangler eller ikke kan brukes.
   - Gratis/private e-postleverandører avvises.
   - Resultatet merkes alltid for manuell verifisering fordi e-post kan tilhøre regnskapsfører, konsern eller ekstern partner.

3. **Ikke funnet**
   - Manglende eller ugyldige kilder gir ikke domene.
   - Dette skal aldri tolkes som et teknisk sikkerhetsproblem.

## Normalisering

Eksempler:

```text
https://www.example.no/kontakt  -> example.no
www.example.no                  -> example.no
portal.example.no               -> example.no
post@example.no                 -> example.no
```

Internasjonaliserte domenenavn lagres som ASCII/punycode for at senere DNS-oppslag skal være stabile.
En liten liste med flernivå-suffikser håndterer blant annet `company.co.uk`.

## Mål for validering

Før DNS-berikelse bør et tilfeldig utvalg manuelt kontrolleres:

- minst 50 kandidater
- minst 95 % presisjon blant domener med høy konfidens
- alle feil kategoriseres etter årsak
- dekning og presisjon må rapporteres separat

Bruk `test-data/domain-validation-template.csv` som fasitmal.

## Bevisste begrensninger

Denne versjonen søker ikke på nettet og gjetter ikke domener fra firmanavn. Den bruker bare registrerte felt fra Enhetsregisteret.
Mer avansert domenefinning kan legges til senere som en separat kilde med lavere konfidens og eksplisitt verifisering.
