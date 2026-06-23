<script setup lang="ts">
definePageMeta({ layout: 'default' })

const config = useRuntimeConfig()
const auth = useAuthStore()
const headers = computed(() => ({ Authorization: `Bearer ${auth.token}` }))
const tutorial = useTutorial()

function startTutorial() {
  tutorial.start()
}

// Profile
const profile = reactive({ fullName: '', email: '', username: '' })
const profileSaving = ref(false)
const profileSuccess = ref(false)

// Notifications (UI-only)
const notifs = reactive({ newInvoice: true, budgetExceeded: true, weeklyReport: false })

// Load current user profile
const { data: meData } = await useAsyncData('settings-me', () =>
  $fetch<{ fullName: string; email: string; username: string }>('/api/v1/auth/me', {
    baseURL: config.public.apiBase,
    headers: headers.value,
  }).catch(() => null),
)

watch(meData, (me) => {
  if (me) {
    profile.fullName = me.fullName ?? ''
    profile.email = me.email ?? ''
    profile.username = me.username ?? ''
  }
}, { immediate: true })

async function saveProfile() {
  profileSaving.value = true
  profileSuccess.value = false
  try {
    const updated = await $fetch<{ fullName: string; email: string; username: string }>('/api/v1/auth/me', {
      baseURL: config.public.apiBase,
      method: 'PUT',
      headers: headers.value,
      body: { fullName: profile.fullName },
    })
    if (auth.user) auth.setUser({ ...auth.user, fullName: updated.fullName })
    profileSuccess.value = true
    setTimeout(() => { profileSuccess.value = false }, 3000)
  } finally {
    profileSaving.value = false
  }
}


</script>

<template>
  <div class="animate-page">
    <div style="margin-bottom:24px">
      <div style="font-size:12px;font-weight:500;color:#9CA3AF;letter-spacing:0.04em;text-transform:uppercase;margin-bottom:6px">Hesab</div>
      <h1 style="font-size:26px;font-weight:700;letter-spacing:-0.02em;margin:0 0 4px">Tənzimləmələr</h1>
      <p style="font-size:14px;color:#6B7280;margin:0">Profil və sistem parametrləri</p>
    </div>

    <div style="display:grid;grid-template-columns:1fr 1fr;gap:18px;max-width:980px">
      <!-- Profile card -->
      <div class="tk-card" style="padding:22px">
        <h3 style="font-size:16px;font-weight:600;margin:0 0 18px">Profil məlumatları</h3>

        <div v-if="profileSuccess" style="background:#F0FDF4;border:1px solid #86EFAC;border-radius:8px;padding:10px 14px;font-size:13px;color:#16A34A;margin-bottom:14px">
          Dəyişikliklər yadda saxlanıldı
        </div>

        <label style="display:block;font-size:13px;font-weight:500;color:#374151;margin-bottom:6px">İstifadəçi adı</label>
        <input
          :value="profile.username"
          disabled
          style="width:100%;height:40px;border:1px solid #D1D5DB;border-radius:7px;padding:0 12px;font-size:14px;margin-bottom:14px;background:#F9FAFB;color:#9CA3AF;font-family:inherit;box-sizing:border-box"
        >

        <label style="display:block;font-size:13px;font-weight:500;color:#374151;margin-bottom:6px">Ad, soyad</label>
        <input
          v-model="profile.fullName"
          type="text"
          style="width:100%;height:40px;border:1px solid #D1D5DB;border-radius:7px;padding:0 12px;font-size:14px;margin-bottom:14px;font-family:inherit;outline:none;box-sizing:border-box"
        >

        <label style="display:block;font-size:13px;font-weight:500;color:#374151;margin-bottom:6px">E-poçt</label>
        <input
          :value="profile.email"
          disabled
          type="email"
          style="width:100%;height:40px;border:1px solid #D1D5DB;border-radius:7px;padding:0 12px;font-size:14px;margin-bottom:20px;background:#F9FAFB;color:#9CA3AF;font-family:inherit;box-sizing:border-box"
        >

        <button
          :disabled="profileSaving"
          style="height:40px;padding:0 20px;background:#3D5AF1;color:#fff;border:none;border-radius:8px;font-size:14px;font-weight:600;cursor:pointer;font-family:inherit;display:inline-flex;align-items:center;gap:8px"
          @click="saveProfile"
        >
          <UIcon v-if="profileSaving" name="i-lucide-loader-2" style="width:15px;height:15px;animation:spin 1s linear infinite" />
          Yadda saxla
        </button>
      </div>

      <!-- Notifications card -->
      <div class="tk-card" style="padding:22px">
        <h3 style="font-size:16px;font-weight:600;margin:0 0 18px">Bildirişlər</h3>

        <div style="display:flex;justify-content:space-between;align-items:center;padding:13px 0;border-bottom:1px solid #F3F4F6">
          <div>
            <div style="font-size:14px;font-weight:500">Yeni qaimə yükləndikdə</div>
            <div style="font-size:12.5px;color:#6B7280;margin-top:2px">E-poçt bildirişi</div>
          </div>
          <div
            :style="`width:40px;height:23px;border-radius:9999px;position:relative;cursor:pointer;transition:background .15s;background:${notifs.newInvoice ? '#3D5AF1' : '#D1D5DB'}`"
            @click="notifs.newInvoice = !notifs.newInvoice"
          >
            <span :style="`position:absolute;top:2.5px;${notifs.newInvoice ? 'right' : 'left'}:2.5px;width:18px;height:18px;background:#fff;border-radius:9999px;transition:left .15s,right .15s`" />
          </div>
        </div>

        <div style="display:flex;justify-content:space-between;align-items:center;padding:13px 0;border-bottom:1px solid #F3F4F6">
          <div>
            <div style="font-size:14px;font-weight:500">Büdcə aşıldıqda</div>
            <div style="font-size:12.5px;color:#6B7280;margin-top:2px">Anlıq xəbərdarlıq</div>
          </div>
          <div
            :style="`width:40px;height:23px;border-radius:9999px;position:relative;cursor:pointer;transition:background .15s;background:${notifs.budgetExceeded ? '#3D5AF1' : '#D1D5DB'}`"
            @click="notifs.budgetExceeded = !notifs.budgetExceeded"
          >
            <span :style="`position:absolute;top:2.5px;${notifs.budgetExceeded ? 'right' : 'left'}:2.5px;width:18px;height:18px;background:#fff;border-radius:9999px;transition:left .15s,right .15s`" />
          </div>
        </div>

        <div style="display:flex;justify-content:space-between;align-items:center;padding:13px 0">
          <div>
            <div style="font-size:14px;font-weight:500">Həftəlik hesabat</div>
            <div style="font-size:12.5px;color:#6B7280;margin-top:2px">Hər bazar ertəsi</div>
          </div>
          <div
            :style="`width:40px;height:23px;border-radius:9999px;position:relative;cursor:pointer;transition:background .15s;background:${notifs.weeklyReport ? '#3D5AF1' : '#D1D5DB'}`"
            @click="notifs.weeklyReport = !notifs.weeklyReport"
          >
            <span :style="`position:absolute;top:2.5px;${notifs.weeklyReport ? 'right' : 'left'}:2.5px;width:18px;height:18px;background:#fff;border-radius:9999px;transition:left .15s,right .15s`" />
          </div>
        </div>
      </div>

      <!-- Categories link card (full width) -->
      <div class="tk-card" style="grid-column:1 / -1;padding:18px 22px;display:flex;align-items:center;justify-content:space-between;gap:16px">
        <div style="display:flex;align-items:center;gap:12px">
          <div style="width:38px;height:38px;border-radius:9px;background:#EEF2FF;display:flex;align-items:center;justify-content:center;flex:none">
            <UIcon name="i-lucide-tag" style="width:18px;height:18px;color:#3D5AF1" />
          </div>
          <div>
            <div style="font-size:14px;font-weight:600;color:#0A0A0A">Xərc kateqoriyaları</div>
            <div style="font-size:13px;color:#6B7280;margin-top:1px">Çoxsəviyyəli kateqoriya ağacını idarə edin</div>
          </div>
        </div>
        <NuxtLink
          to="/categories"
          style="display:inline-flex;align-items:center;gap:6px;height:38px;padding:0 16px;background:#3D5AF1;color:#fff;border-radius:8px;font-size:13.5px;font-weight:600;text-decoration:none;flex:none"
        >
          Kateqoriyalara keç
          <UIcon name="i-lucide-arrow-right" style="width:14px;height:14px" />
        </NuxtLink>
      </div>

      <!-- Tutorial card (full width) -->
      <div class="tk-card" style="grid-column:1 / -1;padding:18px 22px;display:flex;align-items:center;justify-content:space-between;gap:16px;flex-wrap:wrap">
        <div style="display:flex;align-items:center;gap:12px">
          <div style="width:38px;height:38px;border-radius:9px;background:#FFF7ED;display:flex;align-items:center;justify-content:center;flex:none">
            <UIcon name="i-lucide-graduation-cap" style="width:18px;height:18px;color:#D97706" />
          </div>
          <div>
            <div style="font-size:14px;font-weight:600;color:#0A0A0A">Məhsul Turu</div>
            <div style="font-size:13px;color:#6B7280;margin-top:1px">Portalın əsas imkanlarını addım-addım keçin</div>
          </div>
        </div>
        <button
          style="display:inline-flex;align-items:center;gap:6px;height:38px;padding:0 16px;background:#F59E0B;color:#fff;border:none;border-radius:8px;font-size:13.5px;font-weight:600;cursor:pointer;font-family:inherit;flex:none"
          @click="startTutorial"
        >
          <UIcon name="i-lucide-play" style="width:14px;height:14px" />
          Turu Başlat
        </button>
      </div>
    </div>
  </div>
</template>
