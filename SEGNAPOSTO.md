# 📌 SEGNAPOSTO: The Gods

> Questo file serve a **riprendere lo sviluppo in una nuova chat**.
> Basta caricare lo zip `TheGods-<versione>-Segnaposto.zip` (o dare il link al repository)
> e incollare il **prompt di ripartenza** in fondo a questo file.

---

## Stato attuale

| | |
|---|---|
| **Versione** | 0.3.0, "Forme degli Abissi" |
| **Minecraft / Loader** | 1.21.1 / NeoForge 21.1.250 (compatibile con 21.1.x), Java 21 |
| **Repository** | `oliverblox67-sudo/super-duper-tribble`, branch `claude/nice-cerf-6pwftr` |
| **Mod id / package** | `thegods` / `com.thegods` |
| **Dèi completati** | 🌊 Poseidone (11 poteri + ultimate, Avatar con 5 mosse, Forgia del Corpo) |
| **Test** | `./gradlew runGameTestServer`: 6/6 passano (tutti i 12 poteri, Avatar, Titano, Forgia del Corpo, Marea Eterna, revoca) |
| **Verifica grafica** | `./gradlew runShowcase [-Pscene=avatar\|titan\|leviathan\|wings]` (con Xvfb in cloud) salva gli screenshot |

### Regole del progetto (decise con l'utente)
1. Il codice vive su GitHub.
2. **Ogni nuova espansione o update produce DUE zip** in `releases/v<versione>/` (con `./tools/package_release.sh`):
   - `TheGods-<v>-CurseForge.zip`: profilo da **importare nell'app CurseForge** per giocare subito (manifest + mod in `overrides/mods`).
   - `TheGods-<v>-Segnaposto.zip`: codice completo + questo file, per continuare in altre chat.
3. Si alza `mod_version` in `gradle.properties` e si aggiunge una voce in `CHANGELOG.md` a ogni versione.
4. Dèi **inventati** o **ispirati alle mitologie**, con poteri giganteschi, animazioni assurde e modelli ambiziosi.
5. L'utente parla italiano: testi, commenti e documenti sono in italiano (la lingua `en_us` resta per i giocatori inglesi).

---

## Architettura

```
src/main/java/com/thegods/
├── TheGods.java                  # @Mod comune: registri, rete, config
├── config/GodsConfig.java        # thegods-common.toml (danno, ricariche, PvP, volo, gigante, meteo)
├── god/
│   ├── God.java                  # classe base di un dio (poteri, modificatori, passivi, hook)
│   ├── Ability.java              # record: id, ricarica, costo, ultimate, Caster (logica server)
│   ├── DivineModifier.java       # attributo concesso in forma divina
│   ├── GodData.java              # attachment sul giocatore: dio, forma, energia, ricariche (+ campi animazione client)
│   ├── GodManager.java           # TUTTA la logica server: grant/revoke, manifest, useAbility, tick, sync, titoli, scosse
│   ├── Gods.java                 # REGISTRO del pantheon  <- qui si aggiunge ogni nuovo dio
│   ├── Limb.java / LimbState.java# arti (braccia/gambe) e stato: CARNE / RECISO / COSTRUTTO
│   ├── LimbManager.java          # Forgia del Corpo: recidi/forgia/rigenera, attributi per arto, camminata sull'acqua, pugni d'onda
│   ├── AvatarManager.java        # Avatar: crescita graduale (scala/attributi), durata, passi, pugni e atterraggi con onda d'urto
│   └── poseidon/
│       ├── PoseidonGod.java      # definizione: 12 poteri + 5 mosse dell'Avatar, attributi, passivo Marea Eterna
│       ├── PoseidonPowers.java   # implementazione server dei poteri
│       └── PoseidonAvatarMoves.java # trasformazione e mosse del titano (pugno, schianto, onda, ruggito, esci)
├── entity/                       # "incantesimi" visibili (non salvati su disco)
│   ├── SpellEntity.java          # base: proprietario, dimensione sincronizzata, durata
│   ├── WaterTridentEntity / HydroBeamEntity / TsunamiEntity / MaelstromEntity / WaterPrisonEntity
│   ├── WaterSpearEntity / TidalDomeEntity / LeviathanEntity (scia ricostruita sul client)
│   └── WaterGolemEntity.java     # Titano degli Abissi: PathfinderMob vero (IA, proprietario, cavalcabile, schianto)
├── network/                      # payload: Manifest, UseAbility (C→S), SyncGodData, TitleCard, Shake (S→C)
│                                 # ModNetwork.send(...) invia solo se il client ha la mod
├── command/GodsCommand.java      # /gods grant|revoke|refill|list|who
├── event/ServerEvents.java       # tick, login, respawn, tracking, caduta, morte (passivo), immunità
├── item/DivineRelicItem.java     # reliquia che concede un dio (es. Conchiglia di Poseidone)
├── registry/                     # ModAttachments, ModEntities, ModItems (+ tab creativa)
├── util/GodUtil.java             # bersagli validi, danno, raycast, particelle, fulmine divino (senza incendi)
├── gametest/GodGameTests.java    # test automatici
└── client/
    ├── TheGodsClient.java        # @Mod(dist=CLIENT): renderer, layer, tasti, HUD
    ├── ClientEvents.java         # tasti, animazione forma per tutti i giocatori, particelle ambiente, scossa telecamera
    ├── ClientConfig.java         # thegods-client.toml: shader dell'acqua sì/no, mantello sì/no
    ├── ShowcaseDirector.java     # SOLO sviluppo (-Dthegods.showcase=true): demo automatica con screenshot
    ├── ShowcaseScenes.java       # scene brevi della demo (-Pscene=avatar|titan|leviathan|wings)
    ├── mesh/                     # PIPELINE 3D
    │   ├── MeshLibrary.java      # carica assets/thegods/meshes/*.json (reload listener, anche F3+T)
    │   ├── GodMesh.java          # dati: ossa, parti, materiali (acqua/cristallo/scaglie/membrana/bagliore)
    │   ├── Skeleton.java         # posa delle ossa → matrici (rotazioni ZYX come le ModelPart)
    │   └── MeshDrawer.java       # skinning CPU a 2 ossa, tinta per vertice, emit() per geometria al volo
    ├── render/
    │   ├── GodRenderTypes.java   # RenderType con lo shader core "thegods:water_fx" (+ fallback vanilla)
    │   ├── TitanPose.java        # pose del titano: animazione autonoma o copia della posa del giocatore
    │   ├── AvatarRenderer.java   # Avatar: pilota dentro + titano skinnato + nucleo di luce + pugni in prima persona
    │   ├── LeviathanRenderer.java# spline Catmull-Rom + frame a rotazione minima + testa/pinne/coda mesh + barbigli + soffio
    │   ├── WingPhysics.java      # molle sulle penne;  WaterCape.java: mantello con stoffa Verlet
    │   ├── WaterRender.java      # primitive "vecchio stile" (texture acqua vanilla) per effetti semplici
    │   ├── PoseidonFormLayer.java# ali con penne-mesh (14 remiganti + 19 copritrici per ala), correnti, corona, occhi, aureola
    │   └── *Renderer.java        # renderer procedurali per ogni incantesimo
    └── gui/GodHud.java, TitleCardOverlay.java
```

### Modelli 3D: come funzionano (0.3.0)
Ricerca sulla mod di riferimento (*Ronova: Ruler of Death*, file della 1.12.3): **niente GeckoLib**. Usa:
- mesh JSON di centinaia di KB (vertici `v` + quad `q`, ossa con head/tail per il vestito, LOD per i corvi);
- shader core personalizzati (`fx`, `fx_add`, `fx_add_premul`);
- circa 200 texture animate `.mcmeta`;
- simulazione della stoffa.

Noi facciamo lo stesso, con strumenti nostri:
1. **Generatore** `tools/meshgen/build_meshes.py` (+ `meshlib.py`):
   - forme organiche con SDF (sfere, ellissoidi, capsule rastremate, unione morbida `smin`);
   - `marching cubes` (scikit-image), lisciatura di Taubin e decimazione (fast-simplification);
   - superfici parametriche per penne, pinne, corna e cristalli.

   Uscita: `assets/thegods/meshes/{titan,leviathan_head,leviathan_fins,feather}.json` (formato descritto in `meshlib.py`).
   ```bash
   python3 -m venv .meshenv && .meshenv/bin/pip install numpy scipy scikit-image trimesh fast-simplification
   .meshenv/bin/python tools/meshgen/build_meshes.py
   ```
2. **Skinning**: ogni vertice ha 2 ossa + peso (calcolati dalla distanza dai segmenti ossei). `Skeleton` usa rotazioni ZYX come le ModelPart.
   Spazio modello vanilla → spazio titano: X uguale, **Y e Z invertiti** (vedi `TitanPose.fromPlayer`).
3. **Shader** `assets/thegods/shaders/core/water_fx.{json,vsh,fsh}` (formato NEW_ENTITY, nessuna texture).
   - Il **materiale** viaggia nel canale overlay (`UV1.x`): 0 acqua, 1 cristallo, 2 scaglie, 3 membrana, 4 bagliore.
   - Usa `GameTime` per animare caustiche e flussi.
   - Se lo shader non si carica, o `fancyWaterShader=false`, `GodRenderTypes` torna a `entityTranslucent` + texture acqua vanilla.
4. **Avatar**:
   - il giocatore ha l'attributo SCALE (~3.5);
   - in `RenderPlayerEvent.Pre` disegniamo il pilota rimpicciolito di 1/scala nel petto (con un flag anti-rientro), poi il titano con la posa copiata dal `PlayerModel`;
   - `CalculateDetachedCameraDistanceEvent` avvicina la telecamera;
   - `RenderHandEvent` disegna i pugni.

### Scelte tecniche importanti
- **Nessuna dipendenza esterna** (niente GeckoLib): mesh e animazioni sono nostre (vedi sopra). Lo zip CurseForge contiene solo la nostra mod.
- Gli effetti luminosi usano `RenderType.lightning()` (additivo). Per questo `glowQuad` disegna entrambe le facce.
- Il volo divino viene tolto solo se l'ha concesso la mod (`GodData.grantedFlight`).
- I fulmini dei poteri sono **solo visivi** e il danno viene applicato a mano, quindi niente incendi e niente danno al lanciatore.
- Le entità-incantesimo hanno `noSave()`: se il chunk si scarica spariscono senza lasciare residui.
- Gli attributi divini sono `transient` e vengono riapplicati da `GodManager.refreshForm` (idempotente).
- Arti: gli attributi hanno un id per arto e per stato (`LimbManager.MODIFIERS`). Lo stato vive in `GodData.limbs` e sopravvive al cambio di dio.
  Gli arti non di carne vengono nascosti in `RenderPlayerEvent.Pre`; in prima persona `RenderArmEvent` disegna il costrutto.
- La camminata sull'acqua gira in `PlayerTickEvent.Pre` su **client e server**, perché il movimento del giocatore lo calcola il client.
- Il Titano: `getControllingPassenger` restituisce il proprietario, che lo guida (`tickRidden`/`getRiddenInput`). In groppa, `GodManager.useAbility` diventa `riderAttack`.
- Demo grafica: `ShowcaseDirector` (solo con `-Dthegods.showcase=true`) crea un mondo piatto, prova tutto e salva gli screenshot.

### Come aggiungere un nuovo dio (ricetta)
1. `god/<nome>/<Nome>God.java` estende `God`: colore, `abilities()` (6 poteri + 1 ultimate), `modifiers()`, passivo `tryEscapeDeath` o altri hook.
2. `god/<nome>/<Nome>Powers.java`: la logica server di ogni potere (un metodo `boolean cast(ServerPlayer, GodData)`).
3. Le nuove entità-incantesimo vanno in `entity/` (estendono `SpellEntity`) e si registrano in `ModEntities`. Il renderer va in `client/render/` e si registra in `TheGodsClient`.
4. Il layer della forma divina va in `client/render/<Nome>FormLayer.java` e si aggiunge in `TheGodsClient.addLayers` (controlla `data.is(Gods.<NOME>)`).
5. Registra il dio in `Gods.java` e la reliquia in `ModItems`, con texture, modello e ricetta.
6. Traduzioni in `assets/thegods/lang/it_it.json` e `en_us.json` (`god.thegods.<id>`, `.epithet`, `ability.thegods.<id>`, `.desc`).
7. Aggiungi un GameTest, lancia `runGameTestServer` e `runShowcase`, poi `./tools/package_release.sh`.

---

## Roadmap (idee concordate / proposte)
- [x] **0.1.0**: Sistema Divino + 🌊 **Poseidone**
- [x] **0.2.0**: costrutti d'acqua (Titano cavalcabile, Leviatano, Lance, Cupola) + **Forgia del Corpo** (recidi e sostituisci gli arti)
- [x] **0.3.0**: modelli 3D veri (pipeline mesh + shader), **Avatar degli Abissi** (diventi il titano), Leviatano 2.0, ali con penne-mesh e fisica, mantello con stoffa
- [ ] Prossimi miglioramenti grafici: tridente di Poseidone in mano (mesh), ali giganti anche per l'Avatar, LOD per le mesh lontane
- [ ] Idee per la Forgia: trasformare il braccio d'acqua in **lama/frusta/cannone** con un tasto, arti di altri elementi per gli altri dèi
- [ ] ⚡ **Zeus** (ali di fulmine, Giudizio dall'Olimpo, tempesta orbitale, catene di fulmini)
- [ ] 💀 **Ade** (Signore dell'Oltretomba: anime, cani infernali, dimensione dell'Aldilà)
- [ ] ☀️ **Ra** o 🌑 un dio inventato (Vuoto / Tempo / Stelle)
- [ ] Versioni **boss** degli dèi con arene e loot
- [ ] Dimensione "Olimpo" / regni divini
- [ ] Suoni personalizzati (ora sono vanilla)
- [ ] Ali visibili in prima persona (ora: solo i pugni dell'Avatar e gli arti d'acqua)

---

## Prompt di ripartenza (da incollare in una nuova chat)

```
Sto sviluppando la mod Minecraft "The Gods" (NeoForge 1.21.1, Java 21).
Il codice è nel repo GitHub oliverblox67-sudo/super-duper-tribble (branch claude/nice-cerf-6pwftr),
oppure nello zip Segnaposto che ti allego. Leggi prima SEGNAPOSTO.md: contiene stato, architettura,
regole del progetto e roadmap. Regola fissa: ogni nuova versione deve produrre i due zip
(CurseForge + Segnaposto) con ./tools/package_release.sh e aggiornare CHANGELOG.md e SEGNAPOSTO.md.
Prossimo obiettivo: <scrivi qui cosa vuoi, es. "aggiungi Zeus">.
```
