# UC-B-004 — Tasks

## Task 1: Fix CustomAuthenticationManager
- Rimuovere `final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();`
- Aggiungere `private final PasswordEncoder passwordEncoder` come campo
- Aggiungere `PasswordEncoder passwordEncoder` al costruttore
- Rimuovere import di `BCryptPasswordEncoder`

## Task 2: Fix AuthServiceImpl
- Rimuovere `final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();`
- Aggiungere `private final PasswordEncoder passwordEncoder` come campo
- Aggiungere `PasswordEncoder passwordEncoder` al costruttore `@Autowired`
- Rimuovere import di `BCryptPasswordEncoder`

## Task 3: Test di regressione
- Creare test che scansiona i file sorgente e verifica che `new BCryptPasswordEncoder()` non appaia al di fuori di `SecurityConfig`

## Task 4: Verifica build + test

## Task 5: Aggiornamento GiornaleDiBordo
