# Contrato API — geração de material de estudo

## Decisão (Missão 6)

**Manter** a geração de flashcards e questões sob o controller de IA:

```
POST /api/ia/flashcards  → TutorService → FlashcardService.gerarViaIa → Knowledge Layer
POST /api/ia/questoes    → TutorService → QuestaoService.gerarViaIa  → Knowledge Layer
POST /api/simulados/gerar → SimuladoService.gerarViaIa              → Knowledge Layer
```

### Motivos

1. O frontend já consome `/api/ia/*` para geração (evidência: `TutorController`).
2. Controllers de estudo (`/api/flashcards`, `/api/questoes`) cobrem **consumo** (listar, rever, responder), não criação por IA.
3. Simulados têm endpoint de geração no próprio módulo porque **não existia** caminho em `/api/ia`.
4. Evita endpoints duplicados e contratos divergentes.

### Quando migrar para controllers de domínio

Só se o produto exigir roles distintas (ex.: só admin gera em `/api/flashcards/gerar`) ou o frontend deixar de usar `/api/ia`.

## Política de tokens (`StudyContextPolicy`)

| Uso | topK retrieval | max passagens no prompt |
|-----|----------------|-------------------------|
| Chat | 5 | 5 |
| Flashcards | 3 | 3 |
| Questões | 4 | 4 |
| Simulados | 8 | 6 |
| Truncagem por passagem | — | 3500 chars (`PromptBuilder`) |
