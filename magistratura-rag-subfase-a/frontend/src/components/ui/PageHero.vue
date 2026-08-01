<script setup>
/**
 * Cabeçalho de página — regra visual do sistema.
 * Todas as views autenticadas usam este bloco (eyebrow + título + lead + arte).
 */
import FeatureArt from '@/components/brand/FeatureArt.vue'
import BrandMark from '@/components/brand/BrandMark.vue'

defineProps({
  eyebrow: { type: String, default: '' },
  title: { type: String, required: true },
  lead: { type: String, default: '' },
  /** FeatureArt variant ou 'brand' para BrandMark */
  art: { type: String, default: 'default' },
  artSize: { type: [Number, String], default: 56 },
  compact: { type: Boolean, default: false }
})
</script>

<template>
  <header class="page-hero" :class="{ 'page-hero--compact': compact }">
    <div class="page-hero__copy">
      <span v-if="eyebrow" class="page-hero__eyebrow">{{ eyebrow }}</span>
      <h1 class="page-hero__title">{{ title }}</h1>
      <p v-if="lead || $slots.lead" class="page-hero__lead">
        <slot name="lead">{{ lead }}</slot>
      </p>
      <div v-if="$slots.actions" class="page-hero__actions">
        <slot name="actions" />
      </div>
    </div>
    <div class="page-hero__art" aria-hidden="true">
      <slot name="art">
        <BrandMark v-if="art === 'brand'" :size="Number(artSize) + 16" />
        <FeatureArt v-else :variant="art" :size="artSize" />
      </slot>
    </div>
  </header>
</template>

<style scoped>
.page-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1.15rem;
  margin-bottom: 1.25rem;
  padding: 1.1rem 1.2rem;
  border-radius: var(--radius-lg, 22px);
  background:
    radial-gradient(circle at 92% 18%, var(--color-primary-100), transparent 48%),
    radial-gradient(circle at 8% 85%, var(--color-secondary-100), transparent 42%),
    var(--color-surface);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);
}

.page-hero--compact {
  padding: 0.85rem 1rem;
  margin-bottom: 1rem;
}

.page-hero__copy {
  flex: 1;
  min-width: 0;
}

.page-hero__eyebrow {
  display: inline-block;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-secondary-600);
  margin-bottom: 0.3rem;
}

.page-hero__title {
  margin: 0 0 0.35rem;
  font-size: clamp(1.35rem, 2.8vw, 1.7rem);
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--color-text);
  line-height: 1.2;
}

.page-hero__lead {
  margin: 0;
  font-size: 0.92rem;
  line-height: 1.5;
  color: var(--color-text-muted);
  max-width: 38rem;
}

.page-hero__actions {
  margin-top: 0.75rem;
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.page-hero__art {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  border-radius: 18px;
  background: var(--color-secondary-50, color-mix(in srgb, var(--color-secondary-100) 70%, transparent));
  border: 1px solid var(--color-secondary-100, var(--color-border));
}

.page-hero--compact .page-hero__art {
  width: 56px;
  height: 56px;
  border-radius: 14px;
}

@media (max-width: 560px) {
  .page-hero {
    flex-direction: column-reverse;
    text-align: center;
    align-items: stretch;
  }
  .page-hero__art {
    margin: 0 auto;
  }
  .page-hero__lead {
    max-width: none;
  }
  .page-hero__actions {
    justify-content: center;
  }
}
</style>
