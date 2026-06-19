export function useApi() {
  const config = useRuntimeConfig()
  const auth = useAuthStore()

  function authHeaders(): Record<string, string> {
    return auth.token ? { Authorization: `Bearer ${auth.token}` } : {}
  }

  async function apiFetch<T>(path: string, opts: RequestInit & { body?: unknown } = {}): Promise<T> {
    try {
      return await $fetch<T>(path, {
        baseURL: config.public.apiBase as string,
        headers: authHeaders(),
        ...opts,
      } as Parameters<typeof $fetch>[1])
    } catch (e: unknown) {
      if ((e as { status?: number }).status === 401) {
        auth.logout()
        await navigateTo('/login')
      }
      throw e
    }
  }

  return { apiFetch, authHeaders }
}
