# UC-S-001 — Tasks

## Task 1: Esternalizzare il secret in application.properties
- Aggiungere `app.jwt.secret=${JWT_SECRET}` in `application.properties`
- Aggiungere `app.jwt.secret=${JWT_SECRET}` in `application-docker.properties`
- Aggiungere `JWT_SECRET=<valore-generato>` nel file `.env`

## Task 2: Modificare JwtUtility per usare il secret iniettato
- Aggiungere campo `private String jwtSecret` con `@Value("${app.jwt.secret}")`
- Sostituire i 3 riferimenti a `SecurityConstants.JWT_SECRET` con `this.jwtSecret`
- Aggiungere `@PostConstruct` che valida: secret non vuoto e >= 64 caratteri

## Task 3: Rimuovere JWT_SECRET da SecurityConstants
- Eliminare la riga `public static final String JWT_SECRET = ...`
- Verificare che non ci siano altri riferimenti rimasti

## Task 4: Creare Exploit Demo (before)
- Creare `exploit/UC-S-001/forge_token.py`: genera un JWT ADMIN usando il vecchio secret hardcoded
- Creare `exploit/UC-S-001/test_exploit.sh`: chiama un endpoint protetto con il token forgiato
- Creare `exploit/UC-S-001/README.md`: documenta il flusso dell'exploit

## Task 5: Test automatizzato
- Creare test che verifica che un token firmato con un secret arbitrario viene rifiutato
- Creare test che verifica che `SecurityConstants` non espone più un campo `JWT_SECRET`

## Task 6: Aggiornamento documentazione
- Aggiornare README con la variabile `JWT_SECRET` obbligatoria
- Aggiornare GiornaleDiBordo
