# Changelog

## 0.2.0 — "Costrutti degli Abissi" (2026-10-30)
Poteri di **acqua solida** e la **Forgia del Corpo**.
- **Forgia del Corpo** (tasto **H**): interfaccia con anteprima 3D dal vivo e schema del corpo.
  Clicca un arto e scegli **Recidi**, **Forgia d'Acqua** o **Rigenera**.
  - Braccio d'acqua: +3 danno, +1 portata, scavo +40%, i pugni scatenano onde d'urto.
  - Gamba d'acqua: +15% velocità, salto e gradini più alti, cadute attutite.
  - Due gambe d'acqua: **cammini sull'acqua**.
  - Arti recisi: penalità (e niente braccio in prima persona!).
  - Arti d'acqua visibili in terza persona, in prima persona e nell'anteprima, con animazione di formazione a spirale.
- **4 nuovi poteri-costrutto per Poseidone** (ora 10 poteri + ultimate):
  - **Titano degli Abissi**: golem d'acqua alto 7 blocchi che combatte per te per 45 s.
    Emerge dal terreno, cammina, tira pugni a martello, fa schianti ad area. **Cavalcabile**: tasto destro per salire, R per pugno/schianto.
  - **Il Leviatano**: serpente marino d'acqua lungo ~30 blocchi con fauci, corna e pinne che caccia i nemici.
  - **Pioggia di Lance**: 10 lance di cristallo d'acqua in formazione alle tue spalle, lanciate una dopo l'altra.
  - **Cupola delle Maree**: sfera d'acqua che riflette frecce e palle di fuoco e respinge i nemici.
- Le tue ali diventano semi-trasparenti in terza persona da dietro, per vedere meglio.
- Nuovi test automatici: Titano (lealtà, cavalcatura, comando) e Forgia del Corpo (attributi).

## 0.1.0 — "Il Signore dei Mari" (2026-10-30)
Prima versione.
- **Sistema Divino**: forma divina (G), energia divina, 7 poteri per dio con ricariche, HUD laterale, titoli cinematografici, scossa della telecamera, sincronizzazione in multiplayer, configurazione.
- **Poseidone**: ali gigantesche d'acqua animate, corona, aureola, occhi luminosi, correnti orbitanti.
  - Poteri: Tridente delle Maree, Getto Abissale, Passo della Marea, Prigione d'Acqua, Vortice degli Abissi, Tsunami.
  - Ultimate: L'Ira di Poseidone.
  - Passivo: Marea Eterna.
- **Conchiglia di Poseidone**: oggetto craftabile per diventare Poseidone in sopravvivenza.
- Comandi `/gods grant|revoke|refill|list|who`.
- Test automatici (GameTest) per tutti i poteri.
