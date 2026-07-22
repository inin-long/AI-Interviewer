<script setup>
defineProps({
  src: { type: String, required: true },
  alt: { type: String, default: '' },
  position: { type: String, default: 'center' },
})
</script>

<template>
  <figure class="screen-frame">
    <div class="screen-bar">
      <span></span><span></span><span></span>
      <strong>AI Interviewer</strong>
    </div>
    <img :src="src" :alt="alt" :style="{ objectPosition: position }" />
  </figure>
</template>
