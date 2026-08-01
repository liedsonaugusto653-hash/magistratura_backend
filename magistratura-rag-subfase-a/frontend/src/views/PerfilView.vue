<script setup>
import { ref, onMounted, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { PageHero } from '@/components/ui'

const auth = useAuthStore()
const aCarregar = ref(false)
const aGuardar = ref(false)
const aPassword = ref(false)
const mensagem = ref('')
const erro = ref('')
const mensagemPw = ref('')
const erroPw = ref('')

const form = ref({
  nome: '',
  email: ''
})

const formPw = ref({
  passwordAtual: '',
  novaPassword: '',
  confirmar: ''
})

const iniciais = computed(() => {
  const n = form.value.nome || auth.nome || '?'
  return n.trim().charAt(0).toUpperCase()
})

onMounted(async () => {
  aCarregar.value = true
  erro.value = ''
  try {
    await auth.carregarUtilizadorAtual()
    form.value.nome = auth.utilizador?.nome || auth.utilizador?.fullName || ''
    form.value.email = auth.utilizador?.email || ''
  } catch {
    erro.value = 'Não foi possível carregar o perfil.'
  } finally {
    aCarregar.value = false
  }
})

async function guardar() {
  aGuardar.value = true
  mensagem.value = ''
  erro.value = ''
  try {
    const nome = form.value.nome.trim()
    const email = form.value.email.trim().toLowerCase()
    if (!nome) {
      erro.value = 'O nome é obrigatório.'
      return
    }
    if (!email || !email.includes('@')) {
      erro.value = 'Indica um email válido.'
      return
    }
    await auth.atualizarPerfil({ nome, email })
    form.value.nome = auth.utilizador?.nome || nome
    form.value.email = auth.utilizador?.email || email
    mensagem.value = 'Perfil actualizado com sucesso.'
  } catch (e) {
    const d = e?.response?.data
    erro.value =
      d?.mensagem ||
      d?.message ||
      (e?.response?.status === 422 ? 'Este email já está em uso.' : null) ||
      'Não foi possível guardar o perfil.'
  } finally {
    aGuardar.value = false
  }
}

async function alterarPassword() {
  aPassword.value = true
  mensagemPw.value = ''
  erroPw.value = ''
  try {
    const { passwordAtual, novaPassword, confirmar } = formPw.value
    if (!passwordAtual) {
      erroPw.value = 'Indica a palavra-passe actual.'
      return
    }
    if (!novaPassword || novaPassword.length < 8) {
      erroPw.value = 'A nova palavra-passe deve ter pelo menos 8 caracteres.'
      return
    }
    if (novaPassword !== confirmar) {
      erroPw.value = 'A confirmação não coincide com a nova palavra-passe.'
      return
    }
    await auth.alterarPassword({ passwordAtual, novaPassword })
    formPw.value = { passwordAtual: '', novaPassword: '', confirmar: '' }
    mensagemPw.value = 'Palavra-passe alterada com sucesso.'
  } catch (e) {
    const d = e?.response?.data
    erroPw.value =
      d?.mensagem ||
      d?.message ||
      'Não foi possível alterar a palavra-passe. Verifica a actual.'
  } finally {
    aPassword.value = false
  }
}
</script>

<template>
  <div class="page perfil-page">
    <PageHero
      eyebrow="Conta"
      title="O meu perfil"
      lead="Dados da tua conta na plataforma Magistratura."
      art="perfil"
    />

    <div v-if="aCarregar" class="card center-state">A carregar…</div>

    <template v-else>
      <form class="card form-card" @submit.prevent="guardar">
        <div class="perfil-topo">
          <div class="avatar-lg">{{ iniciais }}</div>
          <div>
            <strong>{{ form.nome || 'Estudante' }}</strong>
            <p class="muted">{{ form.email || '—' }}</p>
          </div>
        </div>

        <div class="field">
          <label for="nome">Nome</label>
          <input
            id="nome"
            v-model="form.nome"
            type="text"
            required
            maxlength="150"
            autocomplete="name"
          />
        </div>

        <div class="field">
          <label for="email">Email</label>
          <input
            id="email"
            v-model="form.email"
            type="email"
            required
            maxlength="180"
            autocomplete="email"
          />
          <small class="muted">
            Podes alterar o email. A sessão é actualizada automaticamente (novo token JWT).
          </small>
        </div>

        <p v-if="erro" class="erro">{{ erro }}</p>
        <p v-if="mensagem" class="sucesso">{{ mensagem }}</p>

        <button class="btn btn-primary" type="submit" :disabled="aGuardar">
          {{ aGuardar ? 'A guardar…' : 'Guardar alterações' }}
        </button>
      </form>

      <form class="card form-card form-pw" @submit.prevent="alterarPassword">
        <h2 class="sec-title">Palavra-passe</h2>
        <p class="page-sub">Altera a palavra-passe da tua conta.</p>

        <div class="field">
          <label for="pw-atual">Palavra-passe actual</label>
          <input
            id="pw-atual"
            v-model="formPw.passwordAtual"
            type="password"
            required
            autocomplete="current-password"
          />
        </div>
        <div class="field">
          <label for="pw-nova">Nova palavra-passe</label>
          <input
            id="pw-nova"
            v-model="formPw.novaPassword"
            type="password"
            required
            minlength="8"
            maxlength="100"
            autocomplete="new-password"
          />
          <small class="muted">Mínimo 8 caracteres.</small>
        </div>
        <div class="field">
          <label for="pw-conf">Confirmar nova palavra-passe</label>
          <input
            id="pw-conf"
            v-model="formPw.confirmar"
            type="password"
            required
            minlength="8"
            autocomplete="new-password"
          />
        </div>

        <p v-if="erroPw" class="erro">{{ erroPw }}</p>
        <p v-if="mensagemPw" class="sucesso">{{ mensagemPw }}</p>

        <button class="btn btn-secondary" type="submit" :disabled="aPassword">
          {{ aPassword ? 'A alterar…' : 'Alterar palavra-passe' }}
        </button>
      </form>
    </template>
  </div>
</template>

<style scoped>
.perfil-page {
  max-width: 560px;
}
.page-header {
  margin-bottom: 1.25rem;
}
.page-header h1 {
  margin: 0 0 0.35rem;
  font-size: 1.45rem;
}
.page-sub {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.9rem;
}
.form-card {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1.35rem 1.4rem;
  margin-bottom: 1.25rem;
}
.form-pw .sec-title {
  margin: 0;
  font-size: 1.05rem;
}
.perfil-topo {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 0.5rem;
}
.avatar-lg {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--color-accent-300);
  color: var(--color-text);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 1.25rem;
  flex-shrink: 0;
}
.muted {
  color: var(--color-text-muted);
  font-size: 0.85rem;
  margin: 0.15rem 0 0;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}
.field label {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--color-secondary-700);
}
.field input {
  padding: 0.65rem 0.85rem;
  border-radius: var(--radius-sm);
  border: 1.5px solid var(--color-border);
  background: var(--color-surface-alt);
  font-family: inherit;
}
.erro {
  color: var(--color-danger);
  font-size: 0.88rem;
  margin: 0;
}
.sucesso {
  color: var(--color-secondary-700);
  font-size: 0.88rem;
  margin: 0;
}
.center-state {
  padding: 2rem;
  text-align: center;
  color: var(--color-text-muted);
}
.btn {
  font-family: inherit;
  font-weight: 600;
  border-radius: 999px;
  padding: 0.65rem 1.1rem;
  cursor: pointer;
  border: 1.5px solid transparent;
}
.btn:disabled {
  opacity: 0.7;
  cursor: wait;
}
.btn-primary {
  background: var(--color-primary-500, #c4785a);
  color: #fff;
  border-color: transparent;
}
.btn-secondary {
  background: var(--color-surface);
  color: var(--color-secondary-800);
  border-color: var(--color-border);
}
</style>
