export default defineNuxtRouteMiddleware((to) => {
  if (to.path === '/login' || to.path === '/register') return

  const auth = useAuthStore()
  if (!auth.isAuthenticated) {
    return navigateTo('/login')
  }
})
