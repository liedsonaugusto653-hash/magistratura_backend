<script setup>
import { onMounted, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'

const auth = useAuthStore()
const ui = useUiStore()


function aplicarTema(prefs) {
  const tema = prefs?.tema || 'claro'
  let dark = tema === 'escuro'
  if (tema === 'sistema' && typeof window !== 'undefined') {
    dark = window.matchMedia('(prefers-color-scheme: dark)').matches
  }
  document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light')
}

async function hidratar() {
  if (!auth.token) {
    ui.recarregarPrefsLocais()
    aplicarTema(ui.prefs)
    return
  }
  try {
    if (!auth.utilizador) await auth.carregarUtilizadorAtual()
    ui.hidratarPrefsDoServidor(auth.preferencias?.() || {})
    aplicarTema(ui.prefs)
  } catch {
    ui.recarregarPrefsLocais()
    aplicarTema(ui.prefs)
  }
}

onMounted(hidratar)
watch(
  () => auth.token,
  (t, p) => {
    if (t && t !== p) hidratar()
  }
)
</script>
<template>
  <router-view />
</template>
