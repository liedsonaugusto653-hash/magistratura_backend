# Correcção de compilação

## Causas
1. **QuestaoRequest / QuestaoCompletaResponse** — ficheiros DTO novos; o controller usava as classes sem `import`.
2. **`@Transactional` duplicado / método `gerarViaIa` partido** — ao inserir `eliminar`, a assinatura de `gerarViaIa` ficou sem anotação e, nalguns casos, com `@Transactional` a dobrar (erro *"not a repeatable annotation"*).
3. Os erros em cascata em **BibliotecaService** (`getId()`, `builder()`, etc.) são consequência dos erros acima (Lombok/compilação a falhar a meio) — o ficheiro em si está correcto.

## O que fazer
1. Descompactar este ZIP **por cima** do projecto (substituir ficheiros).
2. Confirmar que existem:
   - `backend/.../dto/questao/QuestaoRequest.java`
   - `backend/.../dto/questao/QuestaoCompletaResponse.java`
   - `backend/.../dto/flashcard/FlashcardRequest.java`
3. Na pasta `backend`:
   ```
   mvn clean compile
   mvn spring-boot:run
   ```
