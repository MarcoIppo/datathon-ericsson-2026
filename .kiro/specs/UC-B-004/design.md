# UC-B-004 — Design

## Contesto

Il bean `PasswordEncoder` è definito in `SecurityConfig` ma `AuthServiceImpl` e `CustomAuthenticationManager` istanziano ciascuna un proprio `new BCryptPasswordEncoder()` come campo finale, bypassando l'injection Spring.

## Soluzione

### Approccio: constructor injection del bean esistente

**CustomAuthenticationManager** — aggiungere `PasswordEncoder` al costruttore (già ha un costruttore con `UserProfileRepository`):
```java
public CustomAuthenticationManager(UserProfileRepository userProfileRepository, PasswordEncoder passwordEncoder) {
    this.userProfileRepository = userProfileRepository;
    this.passwordEncoder = passwordEncoder;
}
```

**AuthServiceImpl** — aggiungere `PasswordEncoder` al costruttore `@Autowired` esistente, rimuovere il campo con `new BCryptPasswordEncoder()`:
```java
@Autowired
public AuthServiceImpl(..., PasswordEncoder passwordEncoder) {
    ...
    this.passwordEncoder = passwordEncoder;
}
```

### Impatto

| Componente | Modifica |
|---|---|
| `AuthServiceImpl.java` | Rimozione `new BCryptPasswordEncoder()`, aggiunta parametro costruttore |
| `CustomAuthenticationManager.java` | Rimozione `new BCryptPasswordEncoder()`, aggiunta parametro costruttore |

### Nessun impatto su
- `SecurityConfig` — il bean resta invariato
- Schema DB — nessuna modifica
- Comportamento runtime — stesso algoritmo, stessa istanza, stessi risultati

### Test di regressione
Test source-level che verifica l'assenza di `new BCryptPasswordEncoder()` nel codice sorgente (esclusa `SecurityConfig`).
