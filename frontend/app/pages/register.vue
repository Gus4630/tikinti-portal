<script setup lang="ts">
definePageMeta({ layout: false })

const config = useRuntimeConfig()
const auth = useAuthStore()
const context = useContextStore()
const router = useRouter()
const { resolveError } = useApiError()

const form = reactive({ username: '', fullName: '', email: '', password: '', confirmPassword: '' })
const loading = ref(false)
const error = ref('')
const showPassword = ref(false)

interface PlatformStats {
  totalManagedExpense: number
  activeProjectCount: number
  supplierCount: number
}

const stats = ref<PlatformStats>({ totalManagedExpense: 0, activeProjectCount: 0, supplierCount: 0 })

onMounted(async () => {
  if (auth.isAuthenticated) { router.replace('/dashboard'); return }
  try {
    const data = await $fetch<PlatformStats>('/api/v1/public/stats', { baseURL: config.public.apiBase })
    stats.value = data
  } catch { /* non-critical — keep zeros */ }
})

function fmtExpense(n: number) {
  if (n >= 1_000_000) return '₼' + (n / 1_000_000).toFixed(1) + 'M'
  if (n >= 1_000) return '₼' + (n / 1_000).toFixed(0) + 'K'
  return '₼' + n.toLocaleString('az-AZ')
}

async function register() {
  error.value = ''
  if (form.password !== form.confirmPassword) {
    error.value = 'Şifrələr uyğun gəlmir.'
    return
  }
  loading.value = true
  try {
    const res = await $fetch<{ accessToken: string; refreshToken: string }>('/api/v1/auth/register', {
      baseURL: config.public.apiBase,
      method: 'POST',
      body: {
        username: form.username,
        fullName: form.fullName || undefined,
        email: form.email,
        password: form.password,
        deviceLabel: 'web',
      },
    })
    auth.setTokens({ accessToken: res.accessToken, refreshToken: res.refreshToken })
    await auth.fetchUser(config.public.apiBase)
    await context.hydrate()
    await router.replace('/dashboard')
  } catch (e: unknown) {
    const { message } = resolveError(e)
    error.value = message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-shell">
    <!-- Left panel -->
    <div class="auth-panel-left">
      <div style="position:absolute;right:-80px;top:-80px;width:320px;height:320px;border-radius:9999px;background:rgba(61,90,241,0.18)" />
      <div style="position:absolute;right:60px;bottom:-120px;width:260px;height:260px;border-radius:9999px;background:rgba(245,158,11,0.10)" />

      <!-- Logo -->
      <div style="display:flex;align-items:center;gap:11px;position:relative">
        <div style="width:34px;height:34px;border-radius:8px;background:#F59E0B;display:flex;align-items:center;justify-content:center;color:#0D1440;font-weight:700;font-size:13px;letter-spacing:-0.03em">SG</div>
        <span style="font-size:19px;font-weight:700;letter-spacing:-0.01em">SagaGroup</span>
      </div>

      <!-- Compact stats row — mobile only -->
      <div class="auth-mobile-stats">
        <div class="auth-mobile-stat">
          <div style="font-family:var(--font-mono);font-size:20px;font-weight:700;color:#F59E0B">{{ fmtExpense(stats.totalManagedExpense) }}</div>
          <div style="font-size:11px;color:#9098C9;margin-top:1px">idarə olunan xərc</div>
        </div>
        <div class="auth-mobile-stat-sep" />
        <div class="auth-mobile-stat">
          <div style="font-family:var(--font-mono);font-size:20px;font-weight:700;color:#fff">{{ stats.activeProjectCount }}</div>
          <div style="font-size:11px;color:#9098C9;margin-top:1px">aktiv layihə</div>
        </div>
        <div class="auth-mobile-stat-sep" />
        <div class="auth-mobile-stat">
          <div style="font-family:var(--font-mono);font-size:20px;font-weight:700;color:#fff">{{ stats.supplierCount }}</div>
          <div style="font-size:11px;color:#9098C9;margin-top:1px">təchizatçı</div>
        </div>
      </div>

      <!-- Body — hidden on mobile -->
      <div class="auth-left-body" style="position:relative">
        <h1 style="font-size:38px;line-height:1.15;font-weight:700;letter-spacing:-0.02em;margin:0 0 18px">
          SagaGroup ilə inşaat xərclərini bir mərkəzdən idarə edin.
        </h1>
        <p style="font-size:16px;line-height:1.6;color:#B7C0E8;margin:0;max-width:420px">
          Qaimə-fakturaların OCR ilə emalı, büdcə nəzarəti və təchizatçı hesablaşmaları — bütün layihələriniz üçün vahid portal.
        </p>
        <div style="display:flex;gap:30px;margin-top:38px">
          <div>
            <div style="font-family:var(--font-mono);font-size:26px;font-weight:600;color:#F59E0B">{{ fmtExpense(stats.totalManagedExpense) }}</div>
            <div style="font-size:13px;color:#9098C9;margin-top:2px">idarə olunan xərc</div>
          </div>
          <div>
            <div style="font-family:var(--font-mono);font-size:26px;font-weight:600">{{ stats.activeProjectCount }}</div>
            <div style="font-size:13px;color:#9098C9;margin-top:2px">aktiv layihə</div>
          </div>
          <div>
            <div style="font-family:var(--font-mono);font-size:26px;font-weight:600">{{ stats.supplierCount }}</div>
            <div style="font-size:13px;color:#9098C9;margin-top:2px">təchizatçı</div>
          </div>
        </div>
      </div>

      <div style="font-size:13px;color:#6B73A6;position:relative">© 2026 SagaGroup · Bütün hüquqlar qorunur</div>
    </div>

    <!-- Right panel -->
    <div class="auth-panel-right">
      <div class="auth-form-inner" style="width:100%;max-width:400px">
        <h2 style="font-size:24px;font-weight:700;letter-spacing:-0.01em;margin:0 0 6px">Hesab yaradın</h2>
        <p style="font-size:14px;color:#6B7280;margin:0 0 28px">Portala qoşulmaq üçün qeydiyyatdan keçin</p>

        <div v-if="error" style="background:#FEF2F2;border:1px solid #FCA5A5;border-radius:8px;padding:11px 14px;font-size:13.5px;color:#DC2626;margin-bottom:18px">
          {{ error }}
        </div>

        <form @submit.prevent="register">
          <!-- Username -->
          <label style="display:block;font-size:13px;font-weight:500;color:#374151;margin-bottom:7px">
            İstifadəçi adı <span style="color:#EF4444">*</span>
          </label>
          <div style="position:relative;margin-bottom:16px">
            <UIcon name="i-lucide-at-sign" style="position:absolute;left:13px;top:50%;transform:translateY(-50%);color:#9CA3AF;width:16px;height:16px;pointer-events:none" />
            <input
              v-model="form.username"
              type="text"
              autocomplete="username"
              placeholder="istifadeci_adi"
              required
              minlength="3"
              style="width:100%;height:48px;border:1px solid #D1D5DB;border-radius:8px;padding:0 14px 0 40px;color:#0A0A0A;outline:none;font-family:inherit"
            >
          </div>

          <!-- Full name -->
          <label style="display:block;font-size:13px;font-weight:500;color:#374151;margin-bottom:7px">
            Ad Soyad
            <span style="font-size:12px;color:#9CA3AF;font-weight:400">(istəyə bağlı)</span>
          </label>
          <div style="position:relative;margin-bottom:16px">
            <UIcon name="i-lucide-user" style="position:absolute;left:13px;top:50%;transform:translateY(-50%);color:#9CA3AF;width:16px;height:16px;pointer-events:none" />
            <input
              v-model="form.fullName"
              type="text"
              autocomplete="name"
              placeholder="Adınız Soyadınız"
              style="width:100%;height:48px;border:1px solid #D1D5DB;border-radius:8px;padding:0 14px 0 40px;color:#0A0A0A;outline:none;font-family:inherit"
            >
          </div>

          <!-- Email -->
          <label style="display:block;font-size:13px;font-weight:500;color:#374151;margin-bottom:7px">
            E-poçt <span style="color:#EF4444">*</span>
          </label>
          <div style="position:relative;margin-bottom:16px">
            <UIcon name="i-lucide-mail" style="position:absolute;left:13px;top:50%;transform:translateY(-50%);color:#9CA3AF;width:16px;height:16px;pointer-events:none" />
            <input
              v-model="form.email"
              type="email"
              autocomplete="email"
              placeholder="ad@domain.az"
              required
              style="width:100%;height:48px;border:1px solid #D1D5DB;border-radius:8px;padding:0 14px 0 40px;color:#0A0A0A;outline:none;font-family:inherit"
            >
          </div>

          <!-- Password -->
          <label style="display:block;font-size:13px;font-weight:500;color:#374151;margin-bottom:7px">
            Şifrə <span style="color:#EF4444">*</span>
          </label>
          <div style="position:relative;margin-bottom:16px">
            <UIcon name="i-lucide-lock" style="position:absolute;left:13px;top:50%;transform:translateY(-50%);color:#9CA3AF;width:16px;height:16px;pointer-events:none" />
            <input
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              autocomplete="new-password"
              placeholder="Ən azı 8 simvol"
              required
              minlength="8"
              style="width:100%;height:48px;border:1px solid #D1D5DB;border-radius:8px;padding:0 44px 0 40px;color:#0A0A0A;outline:none;font-family:inherit"
            >
            <button
              type="button"
              style="position:absolute;right:13px;top:50%;transform:translateY(-50%);background:none;border:none;cursor:pointer;color:#9CA3AF;padding:0;display:flex;align-items:center"
              @click="showPassword = !showPassword"
            >
              <UIcon :name="showPassword ? 'i-lucide-eye-off' : 'i-lucide-eye'" style="width:17px;height:17px" />
            </button>
          </div>

          <!-- Confirm password -->
          <label style="display:block;font-size:13px;font-weight:500;color:#374151;margin-bottom:7px">
            Şifrəni təsdiqlə <span style="color:#EF4444">*</span>
          </label>
          <div style="position:relative;margin-bottom:26px">
            <UIcon name="i-lucide-lock" style="position:absolute;left:13px;top:50%;transform:translateY(-50%);color:#9CA3AF;width:16px;height:16px;pointer-events:none" />
            <input
              v-model="form.confirmPassword"
              :type="showPassword ? 'text' : 'password'"
              autocomplete="new-password"
              placeholder="Şifrəni yenidən daxil edin"
              required
              style="width:100%;height:48px;border:1px solid #D1D5DB;border-radius:8px;padding:0 14px 0 40px;color:#0A0A0A;outline:none;font-family:inherit"
            >
          </div>

          <button
            type="submit"
            :disabled="loading"
            style="width:100%;height:50px;background:#3D5AF1;color:#fff;border:none;border-radius:8px;font-size:15px;font-weight:600;cursor:pointer;font-family:inherit;display:flex;align-items:center;justify-content:center;gap:8px"
            :style="{ opacity: loading ? '0.75' : '1' }"
          >
            <UIcon v-if="loading" name="i-lucide-loader-2" style="width:18px;height:18px;animation:spin 1s linear infinite" />
            {{ loading ? 'Gözləyin...' : 'Qeydiyyat' }}
          </button>
        </form>

        <p style="text-align:center;font-size:13px;color:#9CA3AF;margin:24px 0 0">
          Artıq hesabınız var?
          <NuxtLink to="/login" style="color:#3D5AF1;font-weight:500;text-decoration:none">Daxil olun</NuxtLink>
        </p>
      </div>
    </div>
  </div>
</template>
