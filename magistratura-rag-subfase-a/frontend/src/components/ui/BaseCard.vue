<script setup>
/**
 * tint: '' | primary | secondary | accent
 * interactive: hover + cursor
 * staticHover: desativa lift no hover (formulários)
 */
defineProps({
  tint: { type: String, default: '' },
  interactive: { type: Boolean, default: false },
  staticHover: { type: Boolean, default: false }
})
</script>

<template>
  <div
    class="card"
    :class="[
      tint ? 'card-tint-' + tint : '',
      { 'card-interactive': interactive, 'card-static': staticHover }
    ]"
  >
    <slot />
  </div>
</template>
