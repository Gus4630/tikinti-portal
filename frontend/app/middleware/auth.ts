export default defineNuxtRouteMiddleware((to) => {
  if (to.path === '/login') return

  const auth = useAuthStore()
  if (!auth.isAuthenticated) {
    return navigateTo('/login')
  }
})
