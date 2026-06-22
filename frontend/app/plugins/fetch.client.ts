export default defineNuxtPlugin(() => {
  const auth = useAuthStore()
  const config = useRuntimeConfig()

  // Capture original $fetch before we override it.
  // Used directly for the refresh call so it never gets intercepted (no infinite loop).
  const originalFetch = globalThis.$fetch

  // Single shared promise for any in-flight refresh — prevents token stampede when
  // multiple concurrent requests all fail with 401/403 at the same time.
  let refreshing: Promise<boolean> | null = null

  async function attemptRefresh(): Promise<boolean> {
    if (refreshing) return refreshing

    refreshing = (async (): Promise<boolean> => {
      if (!auth.refreshToken) {
        auth.logout()
        await navigateTo('/login')
        return false
      }
      try {
        const tokens = await originalFetch<{
          accessToken: string
          refreshToken: string
          user?: { id: string; username: string; fullName: string | null; email: string; role: string }
        }>('/api/v1/auth/refresh', {
          baseURL: config.public.apiBase as string,
          method: 'POST',
          body: { refreshToken: auth.refreshToken },
        })
        auth.setTokens({ accessToken: tokens.accessToken, refreshToken: tokens.refreshToken })
        if (tokens.user) auth.setUser(tokens.user)
        return true
      } catch {
        auth.logout()
        await navigateTo('/login')
        return false
      }
    })()

    // Clear the semaphore once settled so future expiries can refresh again
    refreshing.finally(() => { refreshing = null })
    return refreshing
  }

  function toHeadersRecord(h: HeadersInit | undefined): Record<string, string> {
    if (!h) return {}
    if (h instanceof Headers) {
      const out: Record<string, string> = {}
      h.forEach((v, k) => { out[k] = v })
      return out
    }
    if (Array.isArray(h)) return Object.fromEntries(h)
    return { ...h as Record<string, string> }
  }

  async function interceptedFetch(url: unknown, opts: Record<string, unknown> = {}): Promise<unknown> {
    const urlStr = String(url)

    // Let login / refresh calls pass through untouched
    if (urlStr.includes('/auth/login') || urlStr.includes('/auth/refresh')) {
      return originalFetch(url as string, opts as Parameters<typeof originalFetch>[1])
    }

    // Always inject the current token — overrides whatever the caller put in
    // (important for the retry path where the old token would still be in opts.headers)
    const headers = toHeadersRecord(opts.headers as HeadersInit | undefined)
    if (auth.token) headers.Authorization = `Bearer ${auth.token}`

    try {
      return await originalFetch(url as string, { ...opts, headers } as Parameters<typeof originalFetch>[1])
    } catch (err: unknown) {
      const status = (err as { status?: number })?.status
      if ((status === 401 || status === 403) && !opts._authRetried) {
        const refreshed = await attemptRefresh()
        if (!refreshed) throw err // already redirected to /login
        // Retry once with the fresh token
        return originalFetch(url as string, {
          ...opts,
          _authRetried: true,
          headers: { ...headers, Authorization: `Bearer ${auth.token}` },
        } as Parameters<typeof originalFetch>[1])
      }
      throw err
    }
  }

  // Copy ofetch's static methods (.create, .raw, .native) onto our wrapper so
  // anything that calls $fetch.create() or $fetch.raw() still works.
  globalThis.$fetch = Object.assign(interceptedFetch, originalFetch) as typeof globalThis.$fetch
})
