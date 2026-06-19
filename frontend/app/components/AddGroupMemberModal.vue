<script setup lang="ts">
const props = defineProps<{
  groupId: string
  existingMemberIds: string[]
}>()

const emit = defineEmits<{
  close: []
  saved: []
}>()

const config = useRuntimeConfig()
const auth = useAuthStore()

const { data: usersData } = await useAsyncData(`users-for-group-${props.groupId}`, () =>
  $fetch<Record<string, unknown>[]>('/api/v1/auth/users', {
    baseURL: config.public.apiBase,
    headers: { Authorization: `Bearer ${auth.token}` },
  }).catch(() => []),
)

const availableUsers = computed(() =>
  (usersData.value ?? []).filter(u => !props.existingMemberIds.includes(String(u.id))),
)

const selectedUserId = ref('')
const memberRole = ref('')
const loading = ref(false)
const error = ref('')

async function save() {
  if (!selectedUserId.value) { error.value = 'İstifadəçi seçin'; return }
  error.value = ''
  loading.value = true
  try {
    await $fetch(`/api/v1/groups/${props.groupId}/members`, {
      baseURL: config.public.apiBase,
      method: 'POST',
      headers: { Authorization: `Bearer ${auth.token}` },
      body: { userId: selectedUserId.value, memberRole: memberRole.value || undefined },
    })
    emit('saved')
  } catch {
    error.value = 'Xəta baş verdi. Yenidən cəhd edin.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div style="position:fixed;inset:0;z-index:50;display:flex;align-items:center;justify-content:center;padding:20px">
    <div style="position:absolute;inset:0;background:rgba(0,0,0,0.35)" @click="emit('close')" />
    <div style="position:relative;background:#fff;border-radius:12px;width:100%;max-width:420px;box-shadow:0 20px 60px rgba(0,0,0,0.18);overflow:hidden">
      <!-- Header -->
      <div style="display:flex;align-items:center;justify-content:space-between;padding:20px 24px;border-bottom:1px solid #F3F4F6">
        <h2 style="font-size:17px;font-weight:700;margin:0;letter-spacing:-0.01em">Üzv əlavə et</h2>
        <button style="background:none;border:none;cursor:pointer;padding:4px;color:#6B7280" @click="emit('close')">
          <UIcon name="i-lucide-x" style="width:20px;height:20px" />
        </button>
      </div>

      <!-- Body -->
      <div style="padding:24px;display:flex;flex-direction:column;gap:16px">
        <div v-if="error" style="background:#FEF2F2;border:1px solid #FCA5A5;border-radius:8px;padding:10px 14px;font-size:13.5px;color:#DC2626">
          {{ error }}
        </div>

        <div>
          <label style="display:block;font-size:13px;font-weight:500;color:#374151;margin-bottom:6px">İstifadəçi <span style="color:#EF4444">*</span></label>
          <select
            v-model="selectedUserId"
            style="width:100%;height:40px;border:1px solid #D1D5DB;border-radius:8px;padding:0 12px;font-size:14px;background:#fff;color:#0A0A0A;font-family:inherit;outline:none;box-sizing:border-box"
          >
            <option value="">— Seçin —</option>
            <option v-for="u in availableUsers" :key="String(u.id)" :value="u.id">
              {{ u.fullName || u.username }} ({{ u.email }})
            </option>
          </select>
        </div>

        <div>
          <label style="display:block;font-size:13px;font-weight:500;color:#374151;margin-bottom:6px">Vəzifə / Rol</label>
          <input
            v-model="memberRole"
            type="text"
            placeholder="məs. Layihə meneceri"
            style="width:100%;height:40px;border:1px solid #D1D5DB;border-radius:8px;padding:0 12px;font-size:14px;color:#0A0A0A;font-family:inherit;outline:none;box-sizing:border-box"
          >
        </div>
      </div>

      <!-- Footer -->
      <div style="display:flex;justify-content:flex-end;gap:10px;padding:16px 24px;border-top:1px solid #F3F4F6;background:#FAFBFC">
        <button
          style="height:38px;padding:0 18px;background:#fff;color:#374151;border:1px solid #D1D5DB;border-radius:8px;font-size:14px;font-weight:500;cursor:pointer;font-family:inherit"
          @click="emit('close')"
        >
          Ləğv et
        </button>
        <button
          :disabled="loading"
          style="height:38px;padding:0 20px;background:#3D5AF1;color:#fff;border:none;border-radius:8px;font-size:14px;font-weight:600;cursor:pointer;font-family:inherit;display:inline-flex;align-items:center;gap:8px"
          @click="save"
        >
          <UIcon v-if="loading" name="i-lucide-loader-2" style="width:15px;height:15px;animation:spin 1s linear infinite" />
          Əlavə et
        </button>
      </div>
    </div>
  </div>
</template>
