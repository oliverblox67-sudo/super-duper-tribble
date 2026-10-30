<p align="center"><img src="docs/logo.png" width="220" alt="The Gods"></p>

# The Gods

**Diventa una divinità.** Una mod per Minecraft con poteri giganteschi, forme divine animate ed effetti enormi.
Ogni dio ha una forma divina che ti trasforma (ali, corona, aura), 6 poteri e un'**ultimate** devastante.

| | |
|---|---|
| **Minecraft** | 1.21.1 |
| **Loader** | NeoForge 21.1.x |
| **Versione** | 0.1.0, *"Il Signore dei Mari"* |
| **Lato** | Client + Server (serve su entrambi) |

<p align="center">
  <img src="docs/screenshots/poseidone_forma.jpg" width="49%">
  <img src="docs/screenshots/ira_di_poseidone.jpg" width="49%">
  <img src="docs/screenshots/tsunami.jpg" width="32%">
  <img src="docs/screenshots/vortice.jpg" width="32%">
  <img src="docs/screenshots/prigione_acqua.jpg" width="32%">
</p>

*(screenshot presi in automatico dal gioco con `./gradlew runShowcase`)*

---

## 🌊 Poseidone, Signore dei Sette Mari

Quando ti manifesti ti spuntano due **ali gigantesche d'acqua viva**: 16 penne per ala, ognuna fatta di 5 segmenti che ondeggiano come correnti. Hai anche una **corona di spuntoni d'acqua**, un'**aureola luminosa** e gli **occhi che brillano**. Correnti marine ti orbitano attorno e gocce cadono dalle ali. Quando ti manifesti parte un titolo cinematografico e lo schermo trema.

### Forma divina (tasto **G**)
- **Volo** e niente danni da caduta
- +10 cuori, +8 armatura, +4 robustezza, +6 danno, resistenza al contraccolpo
- **Corporatura gigante** (+25%)
- Respirazione acquatica, Grazia del delfino, Forza del condotto
- Immune ai fulmini e all'annegamento, il fuoco si spegne su di te
- L'energia divina si ricarica più in fretta **in acqua o sotto la pioggia**

### Poteri (scegli con **V / B**, usa con **R**)
| Potere | Energia | Ricarica | Effetto |
|---|---|---|---|
| **Tridente delle Maree** | 12 | 1,5 s | Un tridente d'acqua lungo 5 blocchi che esplode in un'onda d'urto. Sotto la tempesta richiama il fulmine. |
| **Getto Abissale** | 22 | 8 s | Raggio d'acqua lungo 64 blocchi che segue il tuo sguardo per 2 secondi. |
| **Passo della Marea** | 8 | 2,5 s | Ti dissolvi in acqua e riappari 16 blocchi avanti, travolgendo chi incontri. |
| **Prigione d'Acqua** | 20 | 12 s | Il bersaglio resta intrappolato in una sfera d'acqua sospesa e annega. |
| **Vortice degli Abissi** | 35 | 18 s | Un maelstrom con tromba d'acqua alta 16 blocchi risucchia tutto, poi esplode in un geyser. |
| **Tsunami** | 40 | 15 s | Un muro d'oceano largo 26 e alto 9 blocchi travolge tutto davanti a te. |
| **⚡ L'Ira di Poseidone** | 100 | 90 s | **ULTIMATE.** Tempesta, **otto tsunami** in ogni direzione, fulmini sui nemici per 12 secondi. Le ali diventano ancora più grandi. |

**Passivo, Marea Eterna:** un colpo mortale ti dissolve nel mare invece di ucciderti. Torni a metà vita e respingi i nemici. Funziona una volta al minuto.

---

## 🎮 Come si gioca

**Diventare Poseidone**
- In sopravvivenza: crea la **Conchiglia di Poseidone** e usala col tasto destro.
  ```
  [Nautilo] [Cuore del mare] [Nautilo]
  [Nautilo] [   Tridente   ] [Nautilo]
  [Nautilo] [   Nautilo    ] [Nautilo]
  ```
- Con i comandi: `/gods grant @s poseidon`

**Tasti** (modificabili in *Opzioni → Controlli → The Gods*)
| Tasto | Azione |
|---|---|
| **G** | Assumi/lascia la forma divina |
| **R** | Usa il potere selezionato |
| **V** / **B** | Potere successivo / precedente |

**Comandi** (servono i permessi da operatore, tranne `list` e `who`)
- `/gods grant <giocatori> <dio>`: rende qualcuno un dio
- `/gods revoke <giocatori>`: toglie la divinità
- `/gods refill [giocatori]`: ricarica energia e poteri
- `/gods list`: elenca gli dèi · `/gods who`: chi è un dio online

**Configurazione:** `config/thegods-common.toml`, oppure *Mods → The Gods → Config* nel gioco. Puoi regolare danno, ricariche, energia, PvP, volo, forma gigante e meteo dell'ultimate.

---

## 📦 Installazione

Ogni versione è nella cartella [`releases/`](releases/) con due archivi:

1. **`TheGods-<versione>-CurseForge.zip`**: per giocare subito.
   App CurseForge → *Minecraft* → **Crea profilo personalizzato** → link **"Importa"** in alto → scegli lo zip.
   L'app scarica da sola Minecraft 1.21.1 e NeoForge, e la mod è già dentro.
2. **`TheGods-<versione>-Segnaposto.zip`**: il codice completo con il file [`SEGNAPOSTO.md`](SEGNAPOSTO.md). Serve a riprendere lo sviluppo in un'altra chat.

In alternativa metti `thegods-<versione>.jar` nella cartella `mods` di un'installazione NeoForge 1.21.1.

---

## 🛠️ Sviluppo

```bash
./gradlew build                 # compila -> build/libs/thegods-<versione>.jar
./gradlew runClient             # avvia il gioco con la mod
./gradlew runGameTestServer     # test automatici di tutti i poteri
./gradlew runShowcase           # entra in un mondo, usa ogni potere e salva screenshot in run/showcase/screenshots
./tools/package_release.sh      # crea i 2 zip della versione in releases/
python3 tools/generate_assets.py  # rigenera texture, logo e struttura dei test
```

L'architettura e come aggiungere nuovi dèi sono spiegati in [`SEGNAPOSTO.md`](SEGNAPOSTO.md).

---

*The Gods è un progetto originale. Le divinità si ispirano alle mitologie del mondo.*
