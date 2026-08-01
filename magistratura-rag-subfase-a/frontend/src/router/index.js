import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { publica: true }
  },
  {
    path: '/registo',
    name: 'registo',
    component: () => import('@/views/RegistoView.vue'),
    meta: { publica: true }
  },
  {
    path: '/recuperar-password',
    name: 'recuperar-password',
    component: () => import('@/views/RecuperarPasswordView.vue'),
    meta: { publica: true }
  },
  {
    path: '/redefinir-password',
    name: 'redefinir-password',
    component: () => import('@/views/RedefinirPasswordView.vue'),
    meta: { publica: true }
  },
  {
    path: '/',
    component: () => import('@/layouts/AppLayout.vue'),
    meta: { requerAuth: true },
    children: [
      { path: '', redirect: '/dashboard' },
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: { nav: { label: 'Painel', iconKey: 'dashboard', order: 10 } }
      },
      {
        path: 'caminhada',
        name: 'caminhada',
        component: () => import('@/views/CaminhadaView.vue'),
        meta: { nav: { label: 'Experiências', iconKey: 'caminhada', order: 15 } }
      },
      {
        path: 'caminhada/:momentoId',
        name: 'experiencia-joao',
        component: () => import('@/views/ExperienciaJoaoView.vue')
      },
      {
        path: 'tutor',
        name: 'tutor',
        component: () => import('@/views/TutorView.vue'),
        meta: { nav: { label: 'Tutor IA', iconKey: 'tutor', order: 20 } }
      },
      {
        path: 'mapa',
        name: 'mapa',
        component: () => import('@/views/MapaJuridicoView.vue'),
        meta: { nav: { label: 'Mapa Jurídico', iconKey: 'mapa', order: 25 } }
      },
      {
        path: 'ferramentas',
        name: 'ferramentas',
        component: () => import('@/views/FerramentasView.vue'),
        meta: { nav: { label: 'Ferramentas IA', iconKey: 'ferramentas', order: 30 } }
      },
      {
        path: 'biblioteca',
        name: 'biblioteca',
        component: () => import('@/views/BibliotecaView.vue'),
        meta: { nav: { label: 'Biblioteca', iconKey: 'biblioteca', order: 40 } }
      },
      {
        path: 'biblioteca/artigos/:id',
        name: 'artigo',
        component: () => import('@/views/ArtigoView.vue')
      },
      {
        path: 'documentos',
        name: 'documentos',
        component: () => import('@/views/DocumentosView.vue'),
        meta: { nav: { label: 'Importar Documentos', iconKey: 'documentos', order: 50 } }
      },
      {
        path: 'flashcards',
        name: 'flashcards',
        component: () => import('@/views/FlashcardsView.vue'),
        meta: { nav: { label: 'Flashcards', iconKey: 'flashcards', order: 60 } }
      },
      {
        path: 'questoes',
        name: 'questoes',
        component: () => import('@/views/QuestoesView.vue'),
        meta: { nav: { label: 'Questões', iconKey: 'questoes', order: 70 } }
      },
      {
        path: 'estatisticas',
        name: 'estatisticas',
        component: () => import('@/views/EstatisticasView.vue'),
        meta: { nav: { label: 'Estatísticas', iconKey: 'estatisticas', order: 90 } }
      },
      {
        path: 'perfil',
        name: 'perfil',
        component: () => import('@/views/PerfilView.vue'),
        meta: { nav: { label: 'O meu perfil', iconKey: 'perfil', order: 100 } }
      },
      {
        path: 'definicoes',
        name: 'definicoes',
        component: () => import('@/views/DefinicoesView.vue'),
        meta: { nav: { label: 'Definições', iconKey: 'definicoes', order: 110 } }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/NotFoundView.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requerAuth && !auth.autenticado) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if ((to.name === 'login' || to.name === 'registo') && auth.autenticado) {
    return { name: 'dashboard' }
  }
  return true
})

export default router
