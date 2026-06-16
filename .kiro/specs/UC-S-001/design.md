# UC-S-001 — Design

## Contesto

`SecurityConstants.JWT_SECRET` è una costante `public static final String` con il secret in chiaro. `JwtUtility` la referenzia direttamente in 3 punti per firmare, parsare e validare token JWT.

## Soluzione

### Approccio: proprietà Spring + injection

1. Definire la proprietà `app.jwt.secret` in `application.properties` (letta da env var `JWT_SECRET`)
2. Iniettare con `@Value` in `JwtUtility`
3. Rimuovere la costante `JWT_SECRET` da `SecurityConstants`
4. Aggiungere validazione all'avvio (secret obbligatorio, lunghezza minima 64 char)

### Perché @Value in JwtUtility
- `JwtUtility` è già un `@Service` (dopo fix UC-B-006 → sarà una classe)
- Injection diretta, zero classi aggiuntive
- Pattern comune nei progetti Spring Boot

### Flusso dopo il fix

```
Startup
  → Spring legge JWT_SECRET da env / application.properties
  → Inject in JwtUtility.jwtSecret via @Value("${app.jwt.secret}")
  → @PostConstruct valida: non vuoto, >= 64 char → altrimenti throw IllegalStateException

Runtime (firma/validazione token)
  → JwtUtility usa this.jwtSecret invece di SecurityConstants.JWT_SECRET
```

### Exploit Demo

```
exploit/UC-S-001/
├── README.md              # Spiegazione exploit
├── forge_token.py         # Script Python che forgia un JWT ADMIN con il vecchio secret
└── test_exploit.sh        # Esegue lo script e prova a chiamare un endpoint protetto
```

### Impatto

| Componente | Modifica |
|---|---|
| `SecurityConstants.java` | Rimozione campo `JWT_SECRET` |
| `JwtUtility.java` | Aggiunta `@Value`, sostituzione riferimenti a `SecurityConstants.JWT_SECRET` con campo iniettato |
| `application.properties` | Aggiunta `app.jwt.secret=${JWT_SECRET}` |
| `application-docker.properties` | Aggiunta `app.jwt.secret=${JWT_SECRET}` |
| `.env` | Aggiunta `JWT_SECRET=<valore>` |
| `exploit/UC-S-001/` | Nuova directory con demo |

### Nessun impatto su
- Schema DB
- Frontend (il token viene solo ricevuto e inviato, non generato lato client)
- Endpoint esistenti (la firma cambia solo se si ruota il secret)

### Nota su backward compatibility
- Token generati con il vecchio secret diventano invalidi dopo il fix (il secret cambia). Questo è **intenzionale** per la sicurezza.
