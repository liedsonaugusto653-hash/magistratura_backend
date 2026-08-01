# Relatório de auditoria — processamento de documentos bloqueado

**Projecto:** Magistratura (Java / Spring Boot)  
**Data:** 2026-07-28  
**Sintoma:** após `POST /api/documentos/{id}/processar` (200 OK), o documento fica em `PROCESSANDO`, mensagem `"A iniciar processamento…"`, progresso `0%`, sem chegar a `PROCESSADO` nem `ERRO`.

---

## 1. Fluxograma da execução

```
POST /api/documentos/{id}/processar
  → DocumentoController.processar
    → DocumentoService.processar [@Transactional]
         → estado=PROCESSANDO, mensagem="A iniciar…", %=0, save, COMMIT
         → afterCommit → DocumentoProcessamentoJob.executar [@Async]
              → DocumentoEstadoService.executarPipeline
                   → DocumentPipelineOrchestrator.executar → executarDesde
                        → DocumentValidatorStage
                        → PdfAnalysisStage
                        → PdfTextExtractorStage  ← ponto de bloqueio (antes do fix)
                             → DocumentoProgressoService.mensagem/actualizar [REQUIRES_NEW]
                        → MetadataExtractorStage
                        → StructureExtractorStage
                        → ArticlePersistenceStage
                        → KnowledgeIndexerStage
                        → KnowledgeGeneratorStage
```

## 2. Cadeia de chamadas e classes

| Ordem | Classe | Método |
|------:|--------|--------|
| 1 | DocumentoController | processar |
| 2 | DocumentoService | processar |
| 3 | DocumentoProcessamentoJob | executar (@Async pipelineExecutor) |
| 4 | DocumentoEstadoService | executarPipeline |
| 5 | DocumentPipelineOrchestrator | executar / executarDesde |
| 6 | PipelineStage (×8) | executar |
| 7 | DocumentoProgressoService | mensagem / actualizar |

Dependências: AsyncConfig, DocumentoProgressHub, PipelineEvents, PipelineAuditService, PdfAnalysisService, PdfExtractorService, OcrExtractorService, IncrementalChangeDetector, KnowledgeIndexer, PostgreSQL/JPA.

## 3. Causa raiz comprovada

### A — Deadlock lógico (principal)

1. `DocumentoEstadoService.executarPipeline` e `DocumentPipelineOrchestrator.executar*` corriam com `@Transactional`.
2. Qualquer `documentoRepository.save` nessa TX segurava **lock de linha** em `documentos` até ao commit final (OCR/PDFBox podiam demorar minutos).
3. `DocumentoProgressoService` abre TX `REQUIRES_NEW` e faz `UPDATE` na **mesma linha**.
4. A segunda conexão espera o lock; a thread do job espera o progresso → **hang infinito**.
5. O HTTP já tinha commitado `PROCESSANDO` / 0% / "A iniciar…" → sintoma exacto.

### B — Job antes do commit (variante)

Cópia em `backend/DocumentoService.java` (raiz do módulo, fora de `src/main/java`) disparava `@Async` **dentro** da `@Transactional`, antes do commit → race/lock com a linha ainda não commitada. A versão correcta usa `TransactionSynchronization.afterCommit`.

### C — Progresso invisível

Mesmo sem deadlock, TX única longa impedia que estados intermédios fossem visíveis via REST até ao fim.

## 4. O que NÃO era a causa

- Loops infinitos nas stages
- `Future.get()` / `join()` sem timeout
- OCR/IA/embeddings como primeiro bloqueio (o fluxo parava **antes**, no progresso)
- Pool `@Async` mal configurado (se o log "Job pipeline iniciado" aparecia)

## 5. Bugs e gravidade

| ID | Bug | Gravidade |
|----|-----|-----------|
| B1 | TX longa + progresso REQUIRES_NEW na mesma linha | Crítica |
| B2 | @Async antes do commit (cópia na raiz) | Crítica |
| B3 | Estados intermédios invisíveis (TX monolítica) | Alta |
| B4 | ERRO nunca gravado se thread presa no lock | Alta |
| B5 | Duplicados Java na raiz de `backend/` | Média |

## 6. Correcções aplicadas neste pacote

1. Remover `@Transactional` de `executarPipeline` / `reprocessarPipeline`.
2. Remover `@Transactional` de `executar` / `reprocessar` / `executarDesde` no orchestrator.
3. Manter `DocumentoService.processar` com `afterCommit` antes do job.
4. Manter job `@Async` **sem** `@Transactional`.
5. `DocumentoProgressoService`: `REQUIRES_NEW` + captura de timeout de lock.
6. Hikari `connection-init-sql`: `lock_timeout=8s`, `deadlock_timeout=1s`.
7. Scripts SQL de monitorização de locks.

## 7. Validação pós-aplicação

Logs esperados:

```
TX commitada — a disparar job pipeline documento=...
Job pipeline iniciado documento=...
Análise PDF ...
Texto pronto: ...
Job pipeline concluído documento=...
```

Estado final ∈ { PROCESSADO, PROCESSADO_COM_AVISOS, FALHA_EXTRACAO, ERRO }.
