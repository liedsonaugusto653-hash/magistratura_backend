# Corpo normativo vs. pré-texto (estudo)

## Problema de UX

O estudante quer estudar **artigos** (corpo da lei), não:

- capa e ficha técnica  
- **índice / sumário**  
- **preâmbulo**  
- **exposição de motivos**  
- considerandos, notas introdutórias  

Esses blocos, se materializados como “artigos”, poluem a biblioteca e o RAG.

## Solução

### 1. `PretextoJuridicoDetector` (novo)

Estima o **offset de início do corpo** com heurísticas determinísticas:

| Sinal | Exemplos |
|-------|----------|
| Cabeçalhos pré-textuais | Preâmbulo, Exposição de motivos, Índice, Sumário, Considerandos… |
| Marcadores de corpo | «A Assembleia Nacional decreta», Título I, Capítulo I… |
| 1.º Artigo 1.º “de verdade” | Seguido de texto normativo, **não** de `…… 12` (TOC) |

### 2. `EstruturaJuridicaParser` (reforçado)

- Ignora marcos `ARTIGO` **antes** do início do corpo (salvo falso positivo óbvio).  
- Mantém filtro de zona de índice/sumário.  
- `isBlocoIndice` + `pareceEntradaDeIndiceCurta` — TOC com ou sem pontos líderes (OCR).  
- `deduplicarPreferindoCorpo` — se o mesmo nº aparece no índice e no corpo, fica o corpo.

### Resultado para o estudante

Na biblioteca / RAG / Tutor entram sobretudo **artigos do articulado**, com capítulo/secção reais — não linhas do sumário nem parágrafos da exposição de motivos.

## Ficheiros

- `service/pdf/PretextoJuridicoDetector.java`  
- `service/pdf/EstruturaJuridicaParser.java`  
- testes em `EstruturaJuridicaParserTest.java`

## Limitações (conscientes)

- Diplomas **sem** “Artigo 1.º” numerado (ex.: só anexos) podem precisar de revisão manual.  
- Preâmbulos longos **sem** cabeçalho explícito e **sem** índice podem vazar um pouco até ao 1.º artigo de corpo (nesse caso o 1.º artigo real corta o pré-texto).  
- Anexos após o articulado continuam a ser parseados se tiverem “Artigo N” — desejável para estudo na maioria dos códigos.
