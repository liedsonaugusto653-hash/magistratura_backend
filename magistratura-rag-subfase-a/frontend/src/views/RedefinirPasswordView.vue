<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import authService from '@/services/authService'
import BrandMark from '@/components/brand/BrandMark.vue'
import { BaseButton } from '@/components/ui'

const password = ref('')
const confirmar = ref('')
const token = ref('')
const carregando = ref(false)
const erro = ref('')
const sucesso = ref(false)

const route = useRoute()
const router = useRouter()

onMounted(() => {
  token.value = String(route.query.token || '')
  if (!token.value) {
    erro.value = 'Link inválido ou incompleto. Pede um novo link de recuperação.'
  }
})

async function submeter() {
  erro.value = ''
  if (!token.value) {
    erro.value = 'Token em falta.'
    return
  }
  if (password.value.length < 8) {
    erro.value = 'A palavra-passe deve ter pelo menos 8 caracteres.'
    return
  }
  if (password.value !== confirmar.value) {
    erro.value = 'As palavras-passe não coincidem.'
    return
  }
  carregando.value = true
  try {
    await authService.redefinirPassword(token.value, password.value)
    sucesso.value = true
    setTimeout(() => router.push({ name: 'login' }), 2000)
  } catch (e) {
    erro.value =
      e.response?.data?.mensagem ||
      e.response?.data?.error ||
      e.response?.data?.message ||
      'Não foi possível redefinir a palavra-passe. O link pode ter expirado.'
  } finally {
    carregando.value = false
  }
}
</script>

<template>
  <div class="login-screen">
    <div class="login-panel">
      <div class="login-brand">
        <div class="brand-mark"><BrandMark :size="56" /></div>
        <h1>Magistratura</h1>
        <p>Nova palavra-passe</p>
      </div>

      <form class="login-card" @submit.prevent="submeter">
        <h2>Redefinir palavra-passe</h2>
        <p class="subtitle">Escolhe uma nova palavra-passe para a tua conta.</p>

        <div class="field">
          <label for="password">Nova palavra-passe</label>
          <input id="password" v-model="password" type="password" required minlength="8" autocomplete="new-password" />
        </div>
        <div class="field">
          <label for="confirmar">Confirmar</label>
          <input id="confirmar" v-model="confirmar" type="password" required minlength="8" autocomplete="new-password" />
        </div>

        <p v-if="erro" class="erro">{{ erro }}</p>
        <p v-if="sucesso" class="sucesso">Palavra-passe atualizada. A redirecionar para o login…</p>

        <BaseButton
          type="submit"
          variant="primary"
          block
          :loading="carregando"
          :disabled="!token || sucesso"
          loading-text="A guardar…"
        >
          Guardar nova palavra-passe
        </BaseButton>

        <p class="links">
          <router-link :to="{ name: 'login' }">Voltar ao login</router-link>
        </p>
      </form>
    </div>
  </div>
</template>

<style scoped>
.login-screen {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(circle at 15% 20%, var(--color-primary-100), transparent 45%),
    radial-gradient(circle at 85% 80%, var(--color-accent-100), transparent 45%),
    var(--color-bg);
  padding: 2rem;
}
.login-panel { width: 100%; max-width: 420px; text-align: center; }
.login-brand { margin-bottom: 1.75rem; }
.brand-mark {
  width: 56px; height: 56px; border-radius: 18px; margin: 0 auto 1rem;
  display: flex; align-items: center; justify-content: center; color: #fff;
  background: linear-gradient(135deg, var(--color-secondary-500), var(--color-secondary-700));
  box-shadow: var(--shadow-md);
}
.login-brand h1 { font-size: 1.9rem; margin-bottom: 0.35rem; }
.login-brand p { color: var(--color-text-muted); font-size: 0.9rem; }
.login-card {
  background: var(--color-surface); border: 1px solid var(--color-border);
  border-radius: var(--radius-lg); padding: 2rem; box-shadow: var(--shadow-lg); text-align: left;
}
.login-card h2 { text-align: center; }
.subtitle { text-align: center; font-size: 0.85rem; margin-bottom: 1.5rem; color: var(--color-text-muted); }
.erro {
  background: #fbeae6; color: var(--color-danger); padding: 0.6rem 0.9rem;
  border-radius: var(--radius-sm); font-size: 0.84rem; margin-bottom: 1rem;
}
.sucesso {
  background: var(--color-accent-100); color: var(--color-accent-700);
  padding: 0.6rem 0.9rem; border-radius: var(--radius-sm); font-size: 0.84rem; margin-bottom: 1rem;
}
.links { text-align: center; margin-top: 1.25rem; font-size: 0.88rem; }
.links a { color: var(--color-secondary-600); font-weight: 600; }
</style>
