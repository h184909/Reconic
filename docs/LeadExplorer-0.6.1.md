# Reconic 0.6.1 – Lead Explorer readability polish

0.6 fungerte funksjonelt, men Lead Explorer-panelet brukte mørke `rgba(...)`-bakgrunner
oppå et ellers lyst Reconic-design. Samtidig arvet inputfeltene mørk tekstfarge.
Det ga lav kontrast og gjorde særlig select/input-feltene vanskelige å lese.

0.6.1 endrer bare presentasjon:

- Lead Explorer-panelet følger nå Reconics lyse design.
- Filterfelt har hvit bakgrunn, mørk tekst og tydelig kant.
- Placeholder-tekst er lysere, men fortsatt lesbar.
- DMARC/SPF/Datakvalitet-boksene har hvit bakgrunn og tydelig tekst.
- Leverandørchips bruker Reconics grønne aksent i stedet for mørk grå.
- Tabellen har tydeligere sticky header, zebra-rader og hover.
- Fokusmarkering på inputs/selects er tydeligere.
- Ingen data, scoring, JavaScript-filtrering eller backend-logikk er endret.

Eksisterende 62 tester skal derfor fortsatt passere.
