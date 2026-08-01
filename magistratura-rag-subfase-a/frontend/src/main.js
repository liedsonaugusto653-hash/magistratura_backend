import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './assets/main.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')

/**
 * Pré-aquece rotas frequentes em idle — não atrasa o first paint.
 */
function aquecerRotas() {
  const warm = () => {
    import('@/views/DashboardView.vue')
    import('@/views/BibliotecaView.vue')
    import('@/views/TutorView.vue')
  }
  if ('requestIdleCallback' in window) {
    requestIdleCallback(warm, { timeout: 4000 })
  } else {
    setTimeout(warm, 2500)
  }
}

router.isReady().then(aquecerRotas)
