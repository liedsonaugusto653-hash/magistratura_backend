# Fluxo antes vs depois

## Antes (bug)

```
[HTTP TX] PROCESSANDO + save + COMMIT
[Job TX LONGA] ─────────────────────────────────────────────► (nunca acaba)
    save(documento) → LOCK linha
    PdfTextExtractor → progresso REQUIRES_NEW → WAIT lock → HANG
[UI] vê sempre "A iniciar…" 0%
```

## Depois (fix)

```
[HTTP TX] PROCESSANDO + save + COMMIT
[afterCommit] dispara @Async
[Job SEM TX envolvente]
    save etapa → TX curta (Spring Data) → commit → libera linha
    progresso REQUIRES_NEW → UPDATE ok → UI vê %
    OCR / stages …
    estado final → PROCESSADO (ou erro)
```

## Rede de segurança

Se ainda houver contenção: `lock_timeout=8s` → erro `55P03` em vez de hang eterno.
