# Magistratura — UX premium + perfil + progresso OCR

## O que inclui
### Backend
- `V25` — preferências do utilizador + campos de progresso OCR
- `PATCH /api/auth/me` — actualizar nome/foto
- `POST /api/auth/me/password` — alterar palavra-passe
- `PUT /api/auth/me/preferencias` — JSON de definições
- Processamento de PDFs **assíncrono** + progresso página a página
- Resposta de documentos com `estadoRotulo`, `mensagemProgresso`, percentagem

### Frontend
- Menu: **O meu perfil** e **Definições** (funcionais)
- Barra de progresso no OCR (ex.: "A ler página 42 de 103")
- Sem jargão técnico (sem pipeline version, logs crus)
- Modal de eliminação, badges legíveis, focus-visible, cards sem hover falso
- Dashboard com atalhos de estudo
- BaseModal + BaseField no design system

## Aplicar
Sobrepor ficheiros na raiz do projecto, reiniciar backend (Flyway corre V25).
