# UC-S-005 — Tasks

## Task 1: Creare annotation `@NoXss` e validator
- Creare `model/validation/NoXss.java` — annotation custom
- Creare `model/validation/NoXssValidator.java` — implementa la regex `[<>"\\]|&[a-zA-Z]+;|&#`

## Task 2: Applicare `@NoXss` sui DTO/Entity
- `SignUpRequestDto.java`: aggiungere `@NoXss` su `firstName`, `lastName`
- `UserProfile.java`: aggiungere `@NoXss` su `username`, `firstName`, `lastName`, `email`, `phoneNumber`

## Task 3: Aggiungere `@Valid` nei controller web
- `UserProfileWebController.addProfile()`: aggiungere `@Valid` sul parametro `UserProfile`
- `UserProfileWebController.editProfile()`: aggiungere `@Valid` sul parametro `UserProfile`

## Task 4: Output escaping in profiles.html
- Aggiungere funzione `escapeHtml()` nel `<script>`
- Applicare `escapeHtml()` a tutti i campi interpolati nel template literal

## Task 5: Creare Exploit Demo
- Creare `exploit/UC-S-005/README.md` — documentazione exploit
- Creare `exploit/UC-S-005/xss_payload.sh` — script che registra un utente con payload XSS via API

## Task 6: Test automatizzati
- Test che verifica che input con `<script>` viene rifiutato (400)
- Test che verifica che nomi internazionali (`François`, `O'Brien`, `田中`) sono accettati
- Test che verifica che l'escaping HTML funziona nel template

## Task 7: Verifica build + test + app Docker

## Task 8: Aggiornamento GiornaleDiBordo
