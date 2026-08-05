# Currículo Narrativo — Arquitectura Pedagógica

## Objectivo

Transformar as experiências da plataforma de preparação à **Magistratura Angolana** num **currículo narrativo orientado por competências**. A história deixa de ser o fim; passa a ser o veículo da aprendizagem.

## Hierarquia

```
MISSÃO
  └── MÓDULO
        └── COMPETÊNCIA          ← o aluno consegue FAZER X
              └── EXPERIÊNCIA    ← narrativa + pergunta central
                    └── MOMENTO / blocos (historia[], cta, media futuro)
```

### Missão
Grande objectivo formativo. Ex.: *Compreender o Direito*.

### Módulo
Bloco temático dentro da missão. Ex.: *Primeiros Passos no Direito*.

### Competência
Capacidade observável (verbo de acção): *distinguir*, *identificar*, *aplicar*.  
Nunca apenas um “tema”.

### Experiência
Unidade narrativa com:
- `id`, `titulo`, `personagemId`
- `competenciaId`
- `perguntaCentral`
- `saidaEsperada`
- `ganchoProxima`
- `preRequisitos[]`
- `estadoCognitivo` (`ingenue` | `identifica` | `hipotese` | `argumenta` | `resolve`)
- `fasePedagogica` (`observar` | `compreender` | `interpretar` | `decidir`)
- `historia[]` (blocos `narrativa`, `dialogo`, futuro `media`)
- `cta` (Biblioteca / Tutor — contrato existente)

### Momento / blocos
Compatível com o motor actual (`NarrativaAudio`, `resolverCta`, progresso).

## Fases pedagógicas

| Fase | O aluno… |
|------|----------|
| **Observar** | Compreende; ainda não decide |
| **Compreender** | Normas, fontes, órgãos, Constituição |
| **Interpretar** | Pequenos casos; justifica; usa Biblioteca e Tutor |
| **Decidir** | Resolve; argumenta; recebe feedback |

## Progressão do protagonista (João)

| Estado | Linguagem típica |
|--------|------------------|
| `ingenue` | Perguntas simples |
| `identifica` | Nomeia conceitos |
| `hipotese` | “Isto parece contrariar…” |
| `argumenta` | Justifica com texto/contexto |
| `resolve` | Facto → norma → consequência |

## Ficheiros

| Caminho | Função |
|---------|--------|
| `src/jornada/curriculo/missao-1.js` | Missão, módulos, competências |
| `src/jornada/curriculo/index.js` | API do currículo + anexar metadados |
| `src/jornada/experiencias/joao.js` | 9 experiências da Missão 1 |
| `src/jornada/experiencias/ana.js` | Percursos complementares (inalterados na lógica) |
| `src/jornada/seed.js` | Normalização + `progressoCompetencias` |
| `src/views/CaminhadaView.vue` | Mapa de competências + fases |
| `src/views/ExperienciaJoaoView.vue` | Pergunta central, saída, gancho |

## Como adicionar uma nova experiência

1. Definir a **competência** (verbo de acção) em `curriculo/missao-1.js` (ou nova missão).
2. Criar o momento em `experiencias/<personagem>.js` com os campos pedagógicos.
3. Referenciar o `id` em `competencia.experienciaIds`.
4. Definir `preRequisitos` e `ganchoProxima` coerentes.
5. Garantir CTA para Biblioteca/Tutor quando fizer sentido.
6. Testar progresso e navegação na Caminhada.

## O que não muda

- API `/api/jornada/progresso`
- Biblioteca, Tutor IA, Flashcards, Questões
- `resolverCta` e âncoras
- Componentes Vue de narrativa/áudio

## Extensões futuras

- Blocos `tipo: 'media'` (vídeo/áudio/Lottie)
- Escolhas ramificadas (`tipo: 'escolha'`)
- Missão 2 (fontes e interpretação avançada)
- Persistência de competências no backend
