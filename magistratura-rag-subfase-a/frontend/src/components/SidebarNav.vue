<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useUiStore } from '@/stores/ui'
import { artForRoute } from '@/config/navArt'
import BrandMark from '@/components/brand/BrandMark.vue'
import FeatureArt from '@/components/brand/FeatureArt.vue'
import { LogOut, Menu, X, ChevronsLeft, ChevronsRight } from 'lucide-vue-next'

defineProps({ nome: { type: String, default: '' } })
defineEmits(['sair'])

const ui = useUiStore()
const { sidebarColapsada } = storeToRefs(ui)
const router = useRouter()

const aberta = ref(false)

/** Itens do menu = rotas com meta.nav (ordem definida no router). */
const itens = computed(() => {
  return router
    .getRoutes()
    .filter((r) => r.meta?.nav && r.name)
    .map((r) => ({
      to: r.path.startsWith('/') ? r.path : `/${r.path}`,
      name: r.name,
      label: r.meta.nav.label,
      order: r.meta.nav.order ?? 999,
      art: artForRoute(r.meta.nav.iconKey)
    }))
    .sort((a, b) => a.order - b.order)
})
</script>

<template>
  <button class="mobile-toggle" type="button" aria-label="Menu" @click="aberta = !aberta">
    <component :is="aberta ? X : Menu" :size="22" />
  </button>

  <aside class="sidebar" :class="{ 'is-open': aberta, 'is-colapsada': sidebarColapsada }">
    <div class="brand">
      <BrandMark :size="sidebarColapsada ? 36 : 42" class="brand-mark" />
      <div class="brand-texto">
        <strong>Magistratura</strong>
        <span>Plataforma de estudo</span>
      </div>
    </div>

    <nav class="nav" aria-label="Principal">
      <router-link
        v-for="item in itens"
        :key="item.name"
        :to="{ name: item.name }"
        class="nav-item"
        :title="sidebarColapsada ? item.label : null"
        @click="aberta = false"
      >
        <span class="nav-icon" aria-hidden="true">
          <FeatureArt :variant="item.art" :size="22" :animated="false" />
        </span>
        <span class="nav-label">{{ item.label }}</span>
      </router-link>
    </nav>

    <button
      class="colapso-btn"
      type="button"
      :title="sidebarColapsada ? 'Expandir menu' : 'Recolher menu'"
      @click="ui.alternarSidebar()"
    >
      <component :is="sidebarColapsada ? ChevronsRight : ChevronsLeft" :size="16" />
      <span class="nav-label">Recolher menu</span>
    </button>

    <div class="footer">
      <div class="user">
        <div class="avatar">{{ nome ? nome[0] : '?' }}</div>
        <div class="user-name nav-label">{{ nome || 'Estudante' }}</div>
      </div>
      <button class="logout" type="button" title="Terminar sessão" @click="$emit('sair')">
        <LogOut :size="17" />
        <span class="nav-label">Terminar sessão</span>
      </button>
    </div>
  </aside>

  <div v-if="aberta" class="overlay" @click="aberta = false" />
</template>

<style scoped>
.mobile-toggle {
  display: none;
  position: fixed;
  top: 0.85rem;
  left: 0.85rem;
  z-index: 40;
  width: 42px;
  height: 42px;
  border-radius: 12px;
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-text);
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: var(--shadow-sm);
}

.sidebar {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  width: var(--sidebar-width, 264px);
  z-index: 30;
  display: flex;
  flex-direction: column;
  padding: 1rem 0.75rem 0.85rem;
  background:
    radial-gradient(circle at 20% 0%, var(--color-primary-100), transparent 55%),
    radial-gradient(circle at 90% 100%, var(--color-secondary-100), transparent 50%),
    var(--color-surface);
  border-right: 1px solid var(--color-border);
  transition: width 0.22s ease, transform 0.22s ease;
}

.sidebar.is-colapsada {
  width: var(--sidebar-width-colapsada, 96px);
}

.brand {
  display: flex;
  align-items: center;
  gap: 0.7rem;
  padding: 0.35rem 0.5rem 1rem;
  border-bottom: 1px solid var(--color-border);
  margin-bottom: 0.65rem;
}

.brand-mark {
  flex-shrink: 0;
}

.brand-texto {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
  min-width: 0;
}

.brand-texto strong {
  font-size: 0.95rem;
  letter-spacing: -0.02em;
  color: var(--color-text);
}

.brand-texto span {
  font-size: 0.72rem;
  color: var(--color-text-muted);
}

.sidebar.is-colapsada .brand {
  justify-content: center;
  padding-left: 0;
  padding-right: 0;
}

.sidebar.is-colapsada .brand-texto,
.sidebar.is-colapsada .nav-label {
  display: none;
}

.nav {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  flex: 1;
  overflow-y: auto;
  padding: 0 0.15rem;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  padding: 0.48rem 0.55rem;
  border-radius: 12px;
  color: var(--color-text-soft, var(--color-text-muted));
  text-decoration: none;
  font-size: 0.86rem;
  font-weight: 550;
  border: 1px solid transparent;
  transition:
    background 0.15s ease,
    border-color 0.15s ease,
    color 0.15s ease;
}

.nav-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: var(--color-secondary-50, color-mix(in srgb, var(--color-secondary-100) 65%, transparent));
  border: 1px solid var(--color-border);
  flex-shrink: 0;
}

.nav-item:hover {
  background: var(--color-secondary-50, #f4f7ea);
  color: var(--color-secondary-700);
}

.nav-item.router-link-active {
  background: color-mix(in srgb, var(--color-secondary-100) 80%, var(--color-surface));
  border-color: var(--color-secondary-300, var(--color-border));
  color: var(--color-secondary-700);
}

.nav-item.router-link-active .nav-icon {
  border-color: var(--color-secondary-300, #9bb86a);
  background: var(--color-secondary-100);
}

.sidebar.is-colapsada .nav-item {
  justify-content: center;
  padding: 0.45rem;
}

.colapso-btn {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  margin: 0.35rem 0.15rem;
  padding: 0.45rem 0.55rem;
  border-radius: 10px;
  border: 1px solid transparent;
  background: transparent;
  color: var(--color-text-muted);
  font-family: inherit;
  font-size: 0.8rem;
  cursor: pointer;
}

.colapso-btn:hover {
  background: var(--color-surface-alt, #f7f4ef);
  color: var(--color-text);
}

.sidebar.is-colapsada .colapso-btn {
  justify-content: center;
}

.footer {
  border-top: 1px solid var(--color-border);
  padding-top: 0.75rem;
  margin-top: 0.35rem;
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.user {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  padding: 0 0.35rem;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 0.85rem;
  background: linear-gradient(145deg, var(--color-primary-100), var(--color-secondary-100));
  color: var(--color-secondary-700);
  border: 1px solid var(--color-border);
  flex-shrink: 0;
}

.user-name {
  font-size: 0.84rem;
  font-weight: 600;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.logout {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  padding: 0.45rem 0.55rem;
  border-radius: 10px;
  border: 1px solid transparent;
  background: transparent;
  color: var(--color-text-muted);
  font-family: inherit;
  font-size: 0.82rem;
  cursor: pointer;
}

.logout:hover {
  background: color-mix(in srgb, var(--color-danger) 8%, transparent);
  color: var(--color-danger);
}

.sidebar.is-colapsada .user {
  justify-content: center;
}

.sidebar.is-colapsada .logout {
  justify-content: center;
}

.overlay {
  display: none;
}

@media (max-width: 900px) {
  .mobile-toggle {
    display: flex;
  }

  .sidebar {
    transform: translateX(-105%);
    width: min(280px, 88vw);
    box-shadow: var(--shadow-lg);
  }

  .sidebar.is-open {
    transform: translateX(0);
  }

  .sidebar.is-colapsada {
    width: min(280px, 88vw);
  }

  .sidebar.is-colapsada .brand-texto,
  .sidebar.is-colapsada .nav-label {
    display: flex;
  }

  .sidebar.is-colapsada .nav-item,
  .sidebar.is-colapsada .colapso-btn,
  .sidebar.is-colapsada .logout {
    justify-content: flex-start;
  }

  .overlay {
    display: block;
    position: fixed;
    inset: 0;
    background: rgba(16, 22, 15, 0.35);
    z-index: 25;
  }

  .colapso-btn {
    display: none;
  }
}
</style>
