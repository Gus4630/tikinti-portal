export default defineNuxtPlugin(async () => {
  const auth = useAuthStore()
  auth.hydrate()

  if (!auth.token) return

  const config = useRuntimeConfig()
  await auth.fetchUser(config.public.apiBase)

  const context = useContextStore()
  await context.hydrate()
})
