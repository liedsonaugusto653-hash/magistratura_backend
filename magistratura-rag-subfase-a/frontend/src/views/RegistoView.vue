<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import BrandMark from '@/components/brand/BrandMark.vue'
import { BaseButton } from '@/components/ui'

const nome = ref('')
const email = ref('')
const password = ref('')
const confirmar = ref('')
const erroLocal = ref('')
const sucesso = ref(false)

const auth = useAuthStore()
const router = useRouter()

async function submeter() {
  erroLocal.value = ''
  sucesso.value = false

  if (password.value.length < 8) {
    erroLocal.value = 'A palavra-passe deve ter pelo menos 8 caracteres.'
    return
  }
  if (password.value !== confirmar.value) {
    erroLocal.value = 'As palavras-passe não coincidem.'
    return
  }

  const ok = await auth.registar(nome.value.trim(), email.value.trim(), password.value)
  if (ok) {
    sucesso.value = true
    // Após registo bem-sucedido, redireciona para login (ou dashboard se o backend devolver token)
    setTimeout(() => router.push({ name: 'login' }), 1500)
  }
}
</script>

<template>
  <div class="login-screen">
    <div class="login-panel">
      <div class="login-brand">
        <div class="brand-mark"><BrandMark :size="56" /></div>
        <h1>Magistratura</h1>
        <p>Cria a tua conta para começar a preparação</p>
      </div>

      <form class="login-card" @submit.prevent="submeter">
        <h2>Criar conta</h2>
        <p class="subtitle">Registo gratuito para estudantes</p>

        <div class="field">
          <label for="nome">Nome completo</label>
          <input id="nome" v-model="nome" type="text" required autocomplete="name" placeholder="Ana Silva" />
        </div>

        <div class="field">
          <label for="email">Email</label>
          <input id="email" v-model="email" type="email" required autocomplete="email" placeholder="estudante@magistratura.local" />
        </div>

        <div class="field">
          <label for="password">Palavra-passe</label>
          <input id="password" v-model="password" type="password" required minlength="8" autocomplete="new-password" placeholder="mínimo 8 caracteres" />
        </div>

        <div class="field">
          <label for="confirmar">Confirmar palavra-passe</label>
          <input id="confirmar" v-model="confirmar" type="password" required minlength="8" autocomplete="new-password" />
        </div>

        <p v-if="erroLocal || auth.erro" class="erro">{{ erroLocal || auth.erro }}</p>
        <p v-if="sucesso" class="sucesso">Conta criada. A redirecionar para o login…</p>

        <BaseButton type="submit" variant="primary" block :loading="auth.carregando" loading-text="A registar…">
          Criar conta
        </BaseButton>

        <p class="links">
          Já tens conta?
          <router-link :to="{ name: 'login' }">Entrar</router-link>
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
.subtitle { text-align: center; font-size: 0.85rem; margin-bottom: 1.5rem; }
.erro {
  background: #fbeae6; color: var(--color-danger); padding: 0.6rem 0.9rem;
  border-radius: var(--radius-sm); font-size: 0.84rem; margin-bottom: 1rem;
}
.sucesso {
  background: var(--color-accent-100); color: var(--color-accent-700);
  padding: 0.6rem 0.9rem; border-radius: var(--radius-sm); font-size: 0.84rem; margin-bottom: 1rem;
}
.links { text-align: center; margin-top: 1.25rem; font-size: 0.88rem; color: var(--color-text-muted); }
.links a { color: var(--color-secondary-600); font-weight: 600; }
</style>
