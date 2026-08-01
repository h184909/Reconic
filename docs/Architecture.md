# Architecture Direction

Spring Boot er foreløpig et lokalt skall rundt valideringsarbeidet. Leadmotoren skal holdes adskilt fra web- og controllerlaget.

Planlagte ansvarsområder:

- `generator`: innhenting og orkestrering
- `intelligence`: tekniske observasjoner
- `provider`: leverandørsignaturer
- `scoring`: forklarbar prioritering
- `monitoring`: tidsserier og endringsdeteksjon
- `controller`: lokal webvisning

Ingen database- eller produksjonsarkitektur er låst før generatoren har bevist verdi.
