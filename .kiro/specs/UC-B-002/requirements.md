# UC-B-002 — Fix @EnableAutoConfiguration su Entity

## Evidenza del Bug

### Dove si manifesta
- **File:** `model/entity/RefreshToken.java`, riga 11 — `@EnableAutoConfiguration`
- **File:** `model/entity/Role.java`, riga 19 — `@EnableAutoConfiguration`

### Perché è un bug
`@EnableAutoConfiguration` è un'annotazione di Spring Boot che attiva il meccanismo di auto-configurazione del contesto applicativo. È progettata per essere usata **esclusivamente** su classi `@Configuration` o sulla main class `@SpringBootApplication` (che già la include internamente).

Posizionarla su un'entity JPA è un errore semantico con effetti collaterali:
1. **Duplicazione dell'auto-configuration trigger** — Spring rileva più punti di attivazione, potenzialmente caricando bean e configurazioni duplicate
2. **Inquinamento del component scan** — le entity vengono scansionate e l'annotazione può alterare l'ordine di inizializzazione dei bean
3. **Violazione del principio di separazione** — un'entity è un POJO di persistenza, non deve influenzare la configurazione applicativa

### Come riprodurre
1. Attivare il logging di debug Spring: `logging.level.org.springframework.boot.autoconfigure=DEBUG`
2. Osservare nel report di auto-configuration che le entity `RefreshToken` e `Role` compaiono come trigger di auto-configuration
3. Confrontare con il comportamento atteso: solo `EricssonDatathonProjectApplication` dovrebbe attivare l'auto-configuration

### Impatto osservabile
- In contesti di test (`@DataJpaTest`, `@WebMvcTest`), la presenza di `@EnableAutoConfiguration` sulle entity può causare il caricamento di bean non necessari, rallentando i test e introducendo dipendenze spurie
- In produzione il danno è limitato perché `@SpringBootApplication` già attiva l'auto-configuration, ma il codice resta scorretto e fuorviante

---

## Requirements (Fix)

### REQ-1: Rimozione annotazione
Rimuovere `@EnableAutoConfiguration` da `RefreshToken.java` e `Role.java`, incluso il relativo import `org.springframework.boot.autoconfigure.EnableAutoConfiguration`.

### REQ-2: Nessuna sostituzione necessaria
Non è richiesta alcuna annotazione sostitutiva. Le entity devono avere solo annotazioni JPA (`@Entity`, `@Table`, `@EntityListeners`) e Lombok.

### REQ-3: Test di regressione
Scrivere un test che verifica tramite reflection che nessuna classe nel package `model.entity` sia annotata con `@EnableAutoConfiguration`.

### REQ-4: Nessuna regressione funzionale
La rimozione non deve alterare il comportamento dell'applicazione: il context Spring deve avviarsi correttamente e le entity devono continuare a funzionare normalmente.
