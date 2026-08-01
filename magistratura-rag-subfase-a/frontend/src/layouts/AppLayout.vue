<script setup>
import { watch, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import SidebarNav from '@/components/SidebarNav.vue'
import GuideHost from '@/components/guide/GuideHost.vue'
import { emitGuideEvent, GuideEvent } from '@/guide/events'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const ui = useUiStore()

const ROTAS_COM_PAINEL_PROPRIO = ['tutor', 'ferramentas']
const LS_SESSION_WELCOME = 'magistratura.guide.sessionWelcome'

const ROTAS_DICA = {
  biblioteca: GuideEvent.ENTER_LIBRARY,
  documentos: null, // evita ruído; docs já têm feedback próprio
  questoes: null,
  flashcards: null,
  dashboard: null
}

let idleTimer = null
const IDLE_MS = 4 * 60 * 1000

function resetIdle() {
  if (idleTimer) clearTimeout(idleTimer)
  idleTimer = setTimeout(() => {
    emitGuideEvent(GuideEvent.IDLE_LONG)
  }, IDLE_MS)
}

function onActivity() {
  resetIdle()
}

function emitirBoasVindas() {
  try {
    if (sessionStorage.getItem(LS_SESSION_WELCOME) === '1') {
      // Já cumprimentou nesta sessão — dica suave de regresso só na 1.ª entrada pós-reload
      return
    }
    sessionStorage.setItem(LS_SESSION_WELCOME, '1')
  } catch {
    /* ignore */
  }

  const firstKey = 'magistratura.guide.everWelcomed'
  let first = false
  try {
    first = localStorage.getItem(firstKey) !== '1'
    if (first) localStorage.setItem(firstKey, '1')
  } catch {
    first = true
  }

  if (first) {
    emitGuideEvent(GuideEvent.FIRST_LOGIN)
  } else {
    emitGuideEvent(GuideEvent.RETURN_WITH_MEMORY, {
      hook: 'Bom regresso. Escolhe por onde queres continuar o estudo.',
      to: '/dashboard'
    })
  }
}

watch(
  () => route.name,
  (atual, anterior) => {
    const entrouEmRotaIA =
      ROTAS_COM_PAINEL_PROPRIO.includes(atual) && !ROTAS_COM_PAINEL_PROPRIO.includes(anterior)
    if (entrouEmRotaIA) {
      ui.autoColapsarParaIA()
    }

    // Dicas só em navegação real (não no mount — o listener do GuideHost ainda pode não existir)
    if (anterior != null) {
      if (atual === 'biblioteca' && anterior !== 'biblioteca') {
        emitGuideEvent(GuideEvent.ENTER_LIBRARY)
      }
      if (atual === 'artigo' && anterior !== 'artigo') {
        emitGuideEvent(GuideEvent.ARTICLE_OPENED)
      }
      resetIdle()
    }
  },
  { immediate: true }
)

onMounted(() => {
  // Pequeno atraso para o GuideHost registar o listener
  setTimeout(() => emitirBoasVindas(), 400)
  resetIdle()
  window.addEventListener('pointerdown', onActivity, { passive: true })
  window.addEventListener('keydown', onActivity, { passive: true })
})

onUnmounted(() => {
  if (idleTimer) clearTimeout(idleTimer)
  window.removeEventListener('pointerdown', onActivity)
  window.removeEventListener('keydown', onActivity)
})

async function sair() {
  await auth.logout()
  try {
    sessionStorage.removeItem(LS_SESSION_WELCOME)
  } catch {
    /* ignore */
  }
  router.push('/login')
}
</script>

<template>
  <div class="shell">
    <SidebarNav :nome="auth.nome" @sair="sair" />
    <main class="content" :class="{ 'content-colapsada': ui.sidebarColapsada }">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
    <GuideHost />
  </div>
</template>

<style scoped>
.shell {
  display: flex;
  min-height: 100vh;
  min-height: 100dvh;
}

.content {
  flex: 1;
  min-width: 0;
  margin-left: var(--sidebar-width);
  min-height: 100vh;
  min-height: 100dvh;
  transition: margin-left 0.22s ease;
}

.content-colapsada {
  margin-left: var(--sidebar-width-colapsada);
}

@media (max-width: 880px) {
  .content,
  .content-colapsada {
    margin-left: 0;
    width: 100%;
    padding-top: 56px;
  }
}

@media (max-width: 560px) {
  .content,
  .content-colapsada {
    padding-top: 52px;
  }
}
</style>
