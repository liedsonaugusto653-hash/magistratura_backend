# Alterações — TTS, Biblioteca e Guia

## 1. Leitor em voz alta (Tutor IA)
**Ficheiro:** `src/views/TutorView.vue`

- Importado o componente reutilizável `OuvirTexto` (já existia e é usado nos artigos).
- Botão **«Ouvir resposta»** em cada mensagem da IA (após terminar de escrever).
- Botão **«Ouvir fonte»** no painel de citação jurídica (extrato da fonte).
- Estilos `.mensagem-audio` e `.fonte-audio`.

O TTS nos **artigos** (texto oficial) já estava activo via `ArticleContent.vue` → `OuvirTexto`.

## 2. Biblioteca — dois botões de pesquisa
**Ficheiros:** `src/views/BibliotecaView.vue`, `src/components/ui/SearchInput.vue`

- Removido o segundo botão «Pesquisar» exterior (o `SearchInput` já inclui o seu próprio botão).
- Corrigido o evento: `@submit` → `@search` (era o que o componente emite de facto).
- Adicionada `hint` útil na barra de pesquisa.
- Ajustado o CSS para a barra preencher bem a toolbar (sem margem centrada desnecessária).

## 3. Guia mais comunicativo
**Ficheiro:** `src/guide/policy.js`

Textos reescritos com tom mais acolhedor e de mentor, por exemplo:
- Primeiro login: «Olá! Estou aqui para te acompanhar…»
- Regresso: «Que bom ver-te de volta!»
- Acerto: «Excelente! Acertaste…»
- Erro: «Não foi desta vez — faz parte do processo…»
- Pesquisa vazia, artigo aberto, capítulo concluído, idle, erros, etc.

## Como aplicar
Copia estes ficheiros para as respectivas pastas do teu frontend e faz deploy:

```
src/views/TutorView.vue
src/views/BibliotecaView.vue
src/guide/policy.js
src/components/ui/SearchInput.vue
```

Não há alterações de backend nem de dependências npm.
