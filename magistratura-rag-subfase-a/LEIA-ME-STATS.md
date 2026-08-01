# Correção: Dashboard vs Estatísticas

## Causa
- **Dashboard** lia a tabela legada `estatisticas` (quase nunca actualizada).
- **Estatísticas** calculava a partir de dados reais (respostas, flashcards, histórico, simulados).
- O frontend do dashboard pedia `percentagemSucessoQuestoes`, mas a API só enviava `percentagemSucesso`.

## Correcção
`DashboardService` passa a usar `EstatisticaService.obter()` (mesma fonte).
`DashboardResponse` inclui `percentagemSucesso` e `percentagemSucessoQuestoes` com o mesmo valor.

## Ficheiros
- `backend/.../DashboardService.java`
- `backend/.../DashboardResponse.java`
- `frontend/src/views/DashboardView.vue`
