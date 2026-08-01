<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import authService from '@/services/authService'
import BrandMark from '@/components/brand/BrandMark.vue'
import { BaseButton } from '@/components/ui'

const email = ref('')
const carregando = ref(false)
const erro = ref('')
const sucesso = ref('')
const router = useRouter()

async function submeter() {
  erro.value = ''
  sucesso.value = ''
  if (!email.value.trim()) {
    erro.value = 'Indica o teu email.'
    return
  }
  carregando.value = true
  try {
    await authService.recuperarPassword(email.value.trim())
    sucesso.value =
      'Se existir uma conta com este email, receberás instruções para redefinir a palavra-passe.'
  } catch (e) {
    // Backend deve responder de forma opaca; se devolver mensagem, mostramos
    const msg =
      e.response?.data?.mensagem ||
      e.response?.data?.error ||
      e.response?.data?.message
    if (e.response?.status === 429) {
      erro.value = 'Demasiados pedidos. Tenta novamente dentro de um minuto.'
    } else if (msg) {
      erro.value = msg
    } else {
      // Mesmo em erro de rede, mensagem genérica (não revelar existência de email)
      sucesso.value =
        'Se existir uma conta com este email, receberás instruções para redefinir a palavra-passe.'
    }
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
        <p>Recuperação de palavra-passe</p>
      </div>

      <form class="login-card" @submit.prevent="submeter">
        <h2>Esqueci a palavra-passe</h2>
        <p class="subtitle">Indica o email da tua conta. Se existir, enviaremos um link de redefinição.</p>

        <div class="field">
          <label for="email">Email</label>
          <input
            id="email"
            v-model="email"
            type="email"
            required
            autocomplete="email"
            placeholder="estudante@magistratura.local"
          />
        </div>

        <p v-if="erro" class="erro">{{ erro }}</p>
        <p v-if="sucesso" class="sucesso">{{ sucesso }}</p>

        <BaseButton type="submit" variant="primary" block :loading="carregando" loading-text="A enviar…">
          Enviar link de recuperação
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
