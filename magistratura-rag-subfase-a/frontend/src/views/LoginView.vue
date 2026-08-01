<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import BrandMark from '@/components/brand/BrandMark.vue'
import { BaseButton } from '@/components/ui'

const email = ref('')
const password = ref('')
const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

async function submeter() {
  const ok = await auth.login(email.value, password.value)
  if (ok) {
    router.push(route.query.redirect || '/dashboard')
  }
}
</script>

<template>
  <div class="login-screen">
    <div class="login-panel">
      <div class="login-brand">
        <div class="brand-mark-wrap"><BrandMark :size="64" /></div>
        <h1>Magistratura</h1>
        <p>Preparação de excelência para a Magistratura Judicial e o Ministério Público</p>
      </div>

      <form class="login-card" @submit.prevent="submeter">
        <h2>Entrar</h2>
        <p class="subtitle">Acede à tua conta para continuar a tua preparação</p>

        <div class="field">
          <label for="email">Email</label>
          <input
            id="email"
            v-model="email"
            type="email"
            placeholder="estudante@magistratura.local"
            required
            autocomplete="email"
          />
        </div>

        <div class="field">
          <label for="password">Palavra-passe</label>
          <input
            id="password"
            v-model="password"
            type="password"
            placeholder="••••••••"
            required
            autocomplete="current-password"
          />
        </div>

        <p v-if="auth.erro" class="erro">{{ auth.erro }}</p>

        <BaseButton
          type="submit"
          variant="primary"
          block
          :loading="auth.carregando"
          loading-text="A entrar…"
        >
          Entrar na plataforma
        </BaseButton>

        <p class="links">
          <router-link :to="{ name: 'recuperar-password' }">Esqueci a palavra-passe</router-link>
          ·
          <router-link :to="{ name: 'registo' }">Criar conta</router-link>
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

.login-panel {
  width: 100%;
  max-width: 420px;
  text-align: center;
}

.login-brand {
  margin-bottom: 1.75rem;
}

.brand-mark {
  width: 56px;
  height: 56px;
  border-radius: 18px;
  margin: 0 auto 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 1.6rem;
  color: #fff;
  background: linear-gradient(135deg, var(--color-secondary-500), var(--color-secondary-700));
  box-shadow: var(--shadow-md);
}

.login-brand h1 {
  font-size: 1.9rem;
  margin-bottom: 0.35rem;
}

.login-brand p {
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

.login-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 2rem;
  box-shadow: var(--shadow-lg);
  text-align: left;
}

.login-card h2 {
  text-align: center;
}

.subtitle {
  text-align: center;
  font-size: 0.85rem;
  margin-bottom: 1.5rem;
}

.erro {
  background: #fbeae6;
  color: var(--color-danger);
  padding: 0.6rem 0.9rem;
  border-radius: var(--radius-sm);
  font-size: 0.84rem;
  margin-bottom: 1rem;
}

.links {
  text-align: center;
  margin-top: 1.25rem;
  font-size: 0.88rem;
  color: var(--color-text-muted);
}

.links a {
  color: var(--color-secondary-600);
  font-weight: 600;
}

.brand-mark-wrap {
  display: flex;
  justify-content: center;
  margin-bottom: 0.75rem;
}
.brand-mark-wrap :deep(.brand-mark) {
  margin: 0 auto;
}
</style>
