# Porque o processamento de PDF fica preso

## O que a biblioteca mostra

A **Biblioteca** lista **diplomas com artigos**.  
Só depois do estado `PROCESSADO` / `PROCESSADO_COM_AVISOS` os artigos aparecem.

Se o documento ficar em `PROCESSANDO` / `OCR_EM_EXECUCAO` / `A iniciar processamento…` a 0%, **nada aparece na biblioteca**.

## Causas mais comuns (Windows)

### 1. Tesseract não instalado (scans)

A maioria dos PDFs oficiais são **imagem**. Sem Tesseract o OCR falha ou o job parece “parado”.

```powershell
# Verificar se existe:
tesseract --version
tesseract --list-langs
# Tem de incluir "por"
```

Instalar: https://github.com/UB-Mannheim/tesseract/wiki  
Marcar **Portuguese** no instalador.

Depois no `.env` ou ambiente:

```text
PIPELINE_OCR_ENABLED=true
PIPELINE_OCR_LANGUAGE=por
PIPELINE_OCR_DATAPATH=C:\Program Files\Tesseract-OCR\tessdata
```

Reiniciar o backend.

### 2. Não associou um diploma

`POST /processar` exige `diplomaId`. Sem diploma → erro; sem processar → sem artigos na biblioteca.

Fluxo correcto:
1. Criar diploma («Novo diploma»)
2. Upload do PDF
3. Processar **associado a esse diploma**

### 3. Job preso de tentativas anteriores

Na consola PostgreSQL:

```sql
SELECT id, nome_original, estado, mensagem_progresso, progresso_percentagem
FROM documentos
ORDER BY data_criacao DESC
LIMIT 20;
```

Resetar presos:

```sql
UPDATE documentos
SET estado = 'ERRO',
    mensagem_progresso = 'Interrompido manualmente — volte a processar',
    progresso_percentagem = 0
WHERE estado IN ('PROCESSANDO','ANALISANDO','EXTRAINDO_TEXTO','OCR_EM_EXECUCAO','ESTRUTURANDO');
```

Depois **Reprocessar** no UI (ou eliminar e voltar a importar).

### 4. PDF protegido

Logs com `PROTECTED` / `password` → exportar no Acrobat/Chrome com **Imprimir → Microsoft Print to PDF** e voltar a importar.

### 5. Ver o log real do backend

No PowerShell onde corre `mvn spring-boot:run`, ao clicar Processar deve aparecer:

```text
TX commitada — a disparar job pipeline documento=...
Job pipeline iniciado documento=...
Pipeline a iniciar documento=...
OCR a iniciar: ...   (se for scan)
```

- Se **não** aparecer `Job pipeline iniciado` → problema de `@Async` / commit.
- Se aparecer e parar em OCR → Tesseract/datapath/PDF enorme.
- Se aparecer `FALHA_EXTRACAO` → ler a mensagem (é orientativa).

## Checklist rápido

1. `tesseract --list-langs` contém `por`
2. Backend reiniciado após instalar Tesseract
3. Diploma criado e seleccionado no processar
4. Documentos `PROCESSANDO` resetados na BD
5. Testar com um PDF **com texto seleccionável** (não scan) — deve processar sem OCR

## Timeouts

`app.pipeline.timeout-minutes` (default 45) — se OCR ultrapassar, o documento passa a ERRO com mensagem clara em vez de ficar eterno.
