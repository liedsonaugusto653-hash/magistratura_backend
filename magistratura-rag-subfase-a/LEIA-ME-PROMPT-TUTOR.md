# System prompt do Tutor + data dinâmica

## Ficheiros
- `backend/src/main/resources/prompts/tutor.txt` — prompt profissional Magistratura Angola
- `backend/src/main/java/ao/magistratura/ia/PromptBuilder.java` — substitui `{{DATA_ATUAL}}` com a data real (fuso Africa/Luanda)

## Aplicar
Na raiz do projecto:
```bash
unzip -o magistratura-prompt-tutor.zip
```
Reinicia o backend. O template é cacheado em memória: **reinício obrigatório**.

## Efeito
O modelo deixa de poder afirmar com consistência que "está em 2023". A data do sistema é injectada em cada montagem do prompt de sistema do chat.
