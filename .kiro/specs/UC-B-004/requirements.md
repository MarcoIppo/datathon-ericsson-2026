# UC-B-004 — Fix BCryptPasswordEncoder multipli

## Evidenza del Bug

### Dove si manifesta
- **`AuthServiceImpl.java`, riga 49:**
  ```java
  final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
  ```
- **`CustomAuthenticationManager.java`, riga 21:**
  ```java
  final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
  ```

### Il bean corretto esiste già
`SecurityConfig.java` definisce un `@Bean` centralizzato:
```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```
Questo bean è correttamente iniettato in `DataInitializer` e `UserProfileServiceImpl`, ma **ignorato** da `AuthServiceImpl` e `CustomAuthenticationManager`.

### Perché è un bug

1. **Violazione del pattern Dependency Injection** — creare `new BCryptPasswordEncoder()` localmente bypassa il container Spring, impedendo:
   - La sostituzione dell'encoder nei test (non mockabile)
   - La centralizzazione della configurazione (cambio algoritmo richiede modifica in 3 posti)

2. **Istanze duplicate** — 3 oggetti `BCryptPasswordEncoder` dove ne basta 1. Spreco di risorse e fonte di potenziale inconsistenza.

3. **Rischio di drift** — se il bean in `SecurityConfig` venisse cambiato (es. Argon2, rounds diversi), `AuthServiceImpl` e `CustomAuthenticationManager` continuerebbero a usare BCrypt con parametri di default, causando password incompatibili.

### Come riprodurre
1. Verificare che `AuthServiceImpl.passwordEncoder` non è lo stesso oggetto del bean Spring:
   - Aggiungere un breakpoint o log su `AuthServiceImpl.passwordEncoder.hashCode()`
   - Confrontare con `applicationContext.getBean(PasswordEncoder.class).hashCode()`
   - I valori sono diversi → istanze separate confermate

### Classi che usano correttamente il bean (via injection)
- `DataInitializer` ✅
- `UserProfileServiceImpl` ✅

### Classi che creano istanze proprie (bug)
- `AuthServiceImpl` ❌
- `CustomAuthenticationManager` ❌

---

## Requirements (Fix)

### REQ-1: Injection del PasswordEncoder in AuthServiceImpl
Rimuovere l'istanza locale `new BCryptPasswordEncoder()` da `AuthServiceImpl` e iniettare il bean `PasswordEncoder` tramite costruttore.

### REQ-2: Injection del PasswordEncoder in CustomAuthenticationManager
Rimuovere l'istanza locale `new BCryptPasswordEncoder()` da `CustomAuthenticationManager` e iniettare il bean `PasswordEncoder` tramite costruttore.

### REQ-3: Singola fonte di verità
Dopo il fix, l'unico punto di creazione del `PasswordEncoder` deve essere il `@Bean` in `SecurityConfig`. Nessun'altra classe deve istanziare direttamente un encoder.

### REQ-4: Test di regressione
Scrivere un test che verifica che nessuna classe nel progetto (esclusa `SecurityConfig`) istanzia direttamente `new BCryptPasswordEncoder()`.
