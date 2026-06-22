export function useApi() {
  const config = useRuntimeConfig()
  const auth = useAuthStore()

  function authHeaders(): Record<string, string> {
    return auth.token ? { Authorization: `Bearer ${auth.token}` } : {}
  }

  // Thin wrapper kept for compatibility. The global $fetch interceptor in
  // plugins/fetch.client.ts handles token refresh and 401/403 redirects for
  // all $fetch calls, including this one.
  async function apiFetch<T>(path: string, opts: RequestInit & { body?: unknown } = {}): Promise<T> {
    return $fetch<T>(path, {
      baseURL: config.public.apiBase as string,
      headers: authHeaders(),
      ...opts,
    } as Parameters<typeof $fetch>[1])
  }

  return { apiFetch, authHeaders }
}
