# Migração — Currículo Narrativo (Missão 1)

## O que foi alterado

### Criados
- `src/jornada/curriculo/missao-1.js` — missão, módulo, 7 competências, fases, estados cognitivos
- `src/jornada/curriculo/index.js` — helpers e anexação de metadados
- `docs/CURRICULO_NARRATIVO.md` — arquitectura pedagógica
- `docs/MIGRACAO_CURRICULO.md` — este relatório

### Reescritos
- `src/jornada/experiencias/joao.js` — **9 experiências em sequência lógica** (antes 6 episódios quase independentes)
  - E1 Porque existe o Direito?
  - E2 Quem cria as regras?
  - E3 O que é uma norma?
  - E4 Porque existem normas superiores?
  - E5 Como funciona a Constituição?
  - E6 Quem exerce os poderes do Estado?
  - E7 Como interpretar uma norma?
  - E8 Como aplicar uma norma?
  - E9 Pequeno caso prático

### Actualizados
- `src/jornada/experiencias/index.js` — agregação João + Ana
- `src/jornada/seed.js` — currículo no seed normalizado; `reflexaoParaMomento`; `progressoCompetencias`; ganchos pedagógicos
- `src/stores/jornada.js` — avanço para o **próximo da lista ordenada** (corrige salto entre módulos)
- `src/views/CaminhadaView.vue` — título da missão, mapa de competências, agrupamento por fase pedagógica
- `src/views/ExperienciaJoaoView.vue` — pergunta central, chips de competência/estado, saída esperada, gancho

### Preservados (sem alteração funcional)
- Ana e restantes personagens de conteúdo
- Backend `JornadaController` / progresso
- Biblioteca, Tutor, Flashcards, Questões, `resolver.js`

## Decisões arquitectónicas

1. **Compatibilidade**: cada experiência continua a ser um “momento” com `historia[]` + `cta`. O store e o normalizador de `cenas` mantêm-se.
2. **Currículo como camada**: metadados vivem em `curriculo/` e são anexados em `normalizarSeed` — não duplicam o motor de UI.
3. **IDs novos no João**: `joao-e1-…` a `joao-e9-…`. Progresso antigo com ids `joao-m1-a*` deixa de mapear (utilizadores recomeçam a sequência curricular — aceitável em fase pré-produção).
4. **Ana no módulo 2**: continua visível; não está ainda mapeada às competências da Missão 1.
5. **Sem backend LMS**: competências calculam-se no frontend a partir de `concluidos[]`.

## Possíveis melhorias futuras

- Mapear Ana a competências de um módulo “Método de estudo / concurso”
- Persistir `competenciasCompletas` no backend
- Pré-requisitos enforçados na UI (bloquear salto)
- Blocos `media` e `escolha`
- Missão 2 com casos mais densos e âncoras reais a artigos processados
