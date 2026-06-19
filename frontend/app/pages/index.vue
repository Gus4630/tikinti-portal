<script setup lang="ts">
definePageMeta({ layout: false })

const auth = useAuthStore()
navigateTo(auth.isAuthenticated ? '/dashboard' : '/login', { replace: true })
</script>

<template>
  <div />
</template>
