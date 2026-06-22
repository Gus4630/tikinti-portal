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
const { resolveError } = useApiError()

const { data: usersData } = await useAsyncData(`users-invite-${props.groupId}`, () =>
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
const sent = ref(false)

async function send() {
  if (!selectedUserId.value) { error.value = 'İstifadəçi seçin'; return }
  error.value = ''
  loading.value = true
  try {
    await $fetch(`/api/v1/groups/${props.groupId}/invitations`, {
      baseURL: config.public.apiBase,
      method: 'POST',
      headers: { Authorization: `Bearer ${auth.token}` },
      body: { userId: selectedUserId.value, memberRole: memberRole.value || undefined },
    })
    sent.value = true
  } catch (err: unknown) {
    const { message } = resolveError(err)
    error.value = message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div style="position:fixed;inset:0;z-index:50;display:flex;align-items:center;justify-content:center;padding:20px">
    <div style="position:absolute;inset:0;background:rgba(0,0,0,0.35)" @click="emit('close')" />
    <div style="position:relative;background:#fff;border-radius:12px;width:100%;max-width:420px;box-shadow:0 20px 60px rgba(0,0,0,0.18);overflow:hidden">
      <div style="display:flex;align-items:center;justify-content:space-between;padding:20px 24px;border-bottom:1px solid #F3F4F6">
        <h2 style="font-size:17px;font-weight:700;margin:0;letter-spacing:-0.01em">Dəvət göndər</h2>
        <button style="background:none;border:none;cursor:pointer;padding:4px;color:#6B7280" @click="emit('close')">
          <UIcon name="i-lucide-x" style="width:20px;height:20px" />
        </button>
      </div>

      <div style="padding:24px;display:flex;flex-direction:column;gap:16px">
        <!-- Success state -->
        <div v-if="sent" style="text-align:center;padding:20px 0">
          <div style="width:52px;height:52px;border-radius:9999px;background:#F0FDF4;border:1px solid #86EFAC;display:flex;align-items:center;justify-content:center;margin:0 auto 14px">
            <UIcon name="i-lucide-send" style="width:24px;height:24px;color:#16A34A" />
          </div>
          <div style="font-size:15px;font-weight:600;color:#111827;margin-bottom:6px">Dəvət göndərildi</div>
          <div style="font-size:13.5px;color:#6B7280">İstifadəçi dəvəti qəbul etdikdən sonra qrupa qoşulacaq.</div>
          <button
            style="margin-top:20px;height:38px;padding:0 20px;background:#3D5AF1;color:#fff;border:none;border-radius:8px;font-size:14px;font-weight:600;cursor:pointer;font-family:inherit"
            @click="emit('saved')"
          >Bağla</button>
        </div>

        <template v-else>
          <div v-if="error" style="background:#FEF2F2;border:1px solid #FCA5A5;border-radius:8px;padding:10px 14px;font-size:13.5px;color:#DC2626">
            {{ error }}
          </div>

          <div>
            <label style="display:block;font-size:13px;font-weight:500;color:#374151;margin-bottom:6px">
              İstifadəçi <span style="color:#EF4444">*</span>
            </label>
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

          <div style="background:#F0F9FF;border:1px solid #BAE6FD;border-radius:8px;padding:10px 12px;font-size:13px;color:#0369A1;display:flex;gap:8px;align-items:flex-start">
            <UIcon name="i-lucide-info" style="width:15px;height:15px;flex:none;margin-top:1px" />
            İstifadəçi dəvəti qəbul etdikdən sonra qrupa avtomatik əlavə ediləcək.
          </div>
        </template>
      </div>

      <div v-if="!sent" style="display:flex;justify-content:flex-end;gap:10px;padding:16px 24px;border-top:1px solid #F3F4F6;background:#FAFBFC">
        <button
          style="height:38px;padding:0 18px;background:#fff;color:#374151;border:1px solid #D1D5DB;border-radius:8px;font-size:14px;font-weight:500;cursor:pointer;font-family:inherit"
          @click="emit('close')"
        >Ləğv et</button>
        <button
          :disabled="loading"
          style="height:38px;padding:0 20px;background:#3D5AF1;color:#fff;border:none;border-radius:8px;font-size:14px;font-weight:600;cursor:pointer;font-family:inherit;display:inline-flex;align-items:center;gap:8px"
          @click="send"
        >
          <UIcon v-if="loading" name="i-lucide-loader-2" style="width:15px;height:15px;animation:spin 1s linear infinite" />
          <UIcon v-else name="i-lucide-send" style="width:15px;height:15px" />
          Dəvət göndər
        </button>
      </div>
    </div>
  </div>
</template>
