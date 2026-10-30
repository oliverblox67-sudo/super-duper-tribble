# 📌 SEGNAPOSTO: The Gods

> Questo file serve a **riprendere lo sviluppo in una nuova chat**.
> Basta caricare lo zip `TheGods-<versione>-Segnaposto.zip` (o dare il link al repository)
> e incollare il **prompt di ripartenza** in fondo a questo file.

---

## Stato attuale

| | |
|---|---|
| **Versione** | 0.1.0, "Il Signore dei Mari" |
| **Minecraft / Loader** | 1.21.1 / NeoForge 21.1.250 (compatibile con 21.1.x), Java 21 |
| **Repository** | `oliverblox67-sudo/super-duper-tribble`, branch `claude/nice-cerf-6pwftr` |
| **Mod id / package** | `thegods` / `com.thegods` |
| **Dèi completati** | 🌊 Poseidone |
| **Test** | `./gradlew runGameTestServer`: 3/3 passano (tutti i 7 poteri lanciati su un server vero) |
| **Verifica grafica** | `./gradlew runShowcase` (con Xvfb in cloud) salva gli screenshot di ogni potere |

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
│   └── poseidon/
│       ├── PoseidonGod.java      # definizione: 7 poteri, attributi, passivo Marea Eterna, tick della forma
│       └── PoseidonPowers.java   # implementazione server dei poteri
├── entity/                       # "incantesimi" visibili (non salvati su disco)
│   ├── SpellEntity.java          # base: proprietario, dimensione sincronizzata, durata
│   ├── WaterTridentEntity / HydroBeamEntity / TsunamiEntity / MaelstromEntity / WaterPrisonEntity
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
    ├── ShowcaseDirector.java     # SOLO sviluppo (-Dthegods.showcase=true): demo automatica con screenshot
    ├── render/
    │   ├── WaterRender.java      # primitive: quad, prismi rastremati, tubi, sfere, glow additivo
    │   ├── PoseidonFormLayer.java# ali (braccio in 3 segmenti + 13 remiganti + 10 copritrici), correnti, corona, occhi, aureola
    │   └── *Renderer.java        # renderer procedurali per ogni incantesimo
    └── gui/GodHud.java, TitleCardOverlay.java
```

### Scelte tecniche importanti
- **Nessuna dipendenza esterna** (niente GeckoLib): modelli e animazioni sono **procedurali in codice** (`PoseStack` + vertici) e usano le texture animate dell'acqua vanilla. Così lo zip CurseForge contiene solo la nostra mod.
- Gli effetti luminosi usano `RenderType.lightning()` (additivo). Per questo `glowQuad` disegna entrambe le facce.
- Il volo divino viene tolto solo se l'ha concesso la mod (`GodData.grantedFlight`).
- I fulmini dei poteri sono **solo visivi** e il danno viene applicato a mano, quindi niente incendi e niente danno al lanciatore.
- Le entità-incantesimo hanno `noSave()`: se il chunk si scarica spariscono senza lasciare residui.
- Gli attributi divini sono `transient` e vengono riapplicati da `GodManager.refreshForm` (idempotente).

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
- [ ] **0.2.0**: ⚡ **Zeus** (ali di fulmine, Giudizio dall'Olimpo, tempesta orbitale, catene di fulmini)
- [ ] 💀 **Ade** (Signore dell'Oltretomba: anime, cani infernali, dimensione dell'Aldilà)
- [ ] ☀️ **Ra** o 🌑 un dio inventato (Vuoto / Tempo / Stelle)
- [ ] Versioni **boss** degli dèi con arene e loot
- [ ] Dimensione "Olimpo" / regni divini
- [ ] Suoni personalizzati (ora sono vanilla)
- [ ] Animazioni in prima persona (ora le ali si vedono solo in terza persona)

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
