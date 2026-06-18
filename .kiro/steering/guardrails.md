# Guardrails

## Architettura e codice

- Privilegiare la backward compatibility: ogni aggiunta deve integrarsi senza rompere funzionalità, API o contratti esistenti. Se una modifica richiede breaking change, segnalarlo e proporre un percorso di migrazione
- Seguire il pattern: Controller (interface) → impl → Service (interface) → impl → Repository
- DTO per input/output REST, Entity per persistenza
- Thymeleaf templates in `resources/templates/`
- Usare Lombok per ridurre boilerplate
- Rispettare le convenzioni già presenti nel codice

## Branching e organizzazione

- Un branch dedicato per ogni Use Case (es. `feature/UC-S-003`)
- `.kiro/specs/` contiene una sottodirectory per UC con requisiti e specifiche di implementazione
- Prima di procedere all'implementazione, aggiornare sempre i file `design.md` e `tasks.md` dello UC in lavorazione con le scelte definitive
- Consultare il design document per dipendenze tra UC

## Giornale di bordo

- Mantenere aggiornato `GiornaleDiBordo.md` nella root: progressi, appunti, decisioni
- A conclusione di ciascun Use Case, chiedere all'utente cosa annotare

## Verifica Use Case

- Al termine di ogni UC, eseguire una batteria di test per validarne completamento e correttezza
- Privilegiare test automatizzati (unit, integration) dove possibile
- Verificare sempre il fix anche sull'applicazione target (Docker) prima di considerare lo UC completato

## Approccio ai test — NON DISTRUTTIVO

- Usare SEMPRE soluzioni non distruttive: test in-memory, mock, DB di test isolato, transazioni con rollback, copie temporanee, snapshot
- Ingegnarsi per trovare alternative creative e non invasive — esplorare OGNI strada possibile
- Il percorso distruttivo è l'ULTIMA SPIAGGIA: solo quando ogni alternativa è stata esplorata e dimostrata non praticabile
- In quel caso, e solo in quel caso, chiedere conferma esplicita spiegando perché non esistono alternative

## Comunicazione e conferme

- Prima di procedere con la prossima fase (analisi, creazione file, implementazione, test, commit), dichiarare esplicitamente cosa si sta per fare: quali directory/file verranno creati, modificati o eliminati, e in quale percorso.
- Attendere conferma dell'utente prima di eseguire.

## Creazione file di spec per UC

- I file di spec vanno creati **uno alla volta**, in questo ordine: `analysis.md` → `requirements.md` → `design.md` → `tasks.md`
- Dopo la creazione di ciascun file, mostrare il contenuto completo e attendere esplicita conferma dell'utente prima di procedere al file successivo
- Non procedere al file successivo finché l'utente non approva quello corrente

## Operazioni che richiedono conferma esplicita

- Modifica o rimozione di credenziali di default (password admin, secret, chiavi) senza chiedere
- Eliminazione di file o directory
- DROP, TRUNCATE, DELETE massivi su database
- Migrazioni o script SQL che alterino struttura del DB
- `docker compose down -v` (rimozione volumi dati)
- `git reset --hard`, `git clean -f`, force push, branch -D
- Qualsiasi operazione irreversibile su codice o dati
