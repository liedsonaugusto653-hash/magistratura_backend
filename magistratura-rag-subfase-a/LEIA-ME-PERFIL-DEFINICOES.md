# Perfil + Definições — persistência completa no backend

## Endpoints (já existiam; reforçados)

| Método | Rota | Função |
|--------|------|--------|
| GET | `/api/auth/me` | Dados + `preferenciasJson` |
| PATCH | `/api/auth/me` | Nome, email (JWT novo se o email mudar) |
| POST | `/api/auth/me/password` | Alterar palavra-passe |
| PUT | `/api/auth/me/preferencias` | Guardar preferências (merge JSON) |

## Preferências guardadas no servidor

```json
{
  "sidebarIniciaColapsada": false,
  "confirmarAntesDeEliminar": true,
  "mostrarDicas": true,
  "guiaNivel": "normal"
}
```

`guiaNivel`: `normal` | `minimo` | `desligado`

## O que mudou no frontend

- **Perfil**: editar nome/email + **alterar palavra-passe** (com validação)
- **Definições**: grava local **e** servidor; mostra erro se o servidor falhar; aplica sidebar compacta/expandida de imediato
- **Login / App**: hidrata preferências do servidor para localStorage + UI
- Schema normalizado em `prefsUi.js`

## Backend

- `AuthService.atualizarPreferencias` faz **merge** com o JSON existente e valida `guiaNivel`

## Ficheiros a copiar

```
backend/src/main/java/ao/magistratura/service/AuthService.java
backend/src/main/java/ao/magistratura/dto/auth/AtualizarPreferenciasRequest.java

frontend/src/utils/prefsUi.js
frontend/src/stores/ui.js
frontend/src/stores/auth.js
frontend/src/services/authService.js
frontend/src/views/PerfilView.vue
frontend/src/views/DefinicoesView.vue
frontend/src/App.vue
```

Reinicia backend + frontend. A migration `V26` (`preferencias_json`) já deve existir.

## Testar

1. Login → Definições → desligar confirmações + guia "Só essencial" → Guardar
2. Logout / login noutro browser (ou limpar localStorage de prefs) → preferências devem voltar do servidor
3. Perfil → mudar nome → Guardar
4. Perfil → alterar password (actual correcta)
5. Perfil → mudar email → deve continuar autenticado (token novo)
