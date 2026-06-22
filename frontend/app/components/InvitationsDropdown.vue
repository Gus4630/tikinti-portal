<script setup lang="ts">
interface Invitation {
  id: string
  groupId: string
  groupName: string
  invitedByFullName: string | null
  invitedByUsername: string
  memberRole: string | null
  createdAt: string
}

const config = useRuntimeConfig()
const auth = useAuthStore()
const context = useContextStore()
const { resolveError } = useApiError()

const open = ref(false)
const invitations = ref<Invitation[]>([])
const loading = ref(false)
const actionLoading = ref<string | null>(null)

async function load() {
  if (!auth.token) return
  loading.value = true
  try {
    const res = await $fetch<Invitation[]>('/api/v1/auth/me/invitations', {
      baseURL: config.public.apiBase,
      headers: { Authorization: `Bearer ${auth.token}` },
    })
    invitations.value = res
  } catch {
    invitations.value = []
  } finally {
    loading.value = false
  }
}

async function respond(id: string, action: 'accept' | 'decline') {
  actionLoading.value = id
  try {
    await $fetch(`/api/v1/auth/me/invitations/${id}/${action}`, {
      baseURL: config.public.apiBase,
      method: 'POST',
      headers: { Authorization: `Bearer ${auth.token}` },
    })
    invitations.value = invitations.value.filter(i => i.id !== id)
    if (action === 'accept') {
      await context.loadGroups()
    }
  } catch (err: unknown) {
    const { message } = resolveError(err)
    console.error(message)
  } finally {
    actionLoading.value = null
  }
}

function toggle() {
  open.value = !open.value
  if (open.value) load()
}

onMounted(load)

function byName(inv: Invitation) {
  return inv.invitedByFullName || inv.invitedByUsername
}
</script>

<template>
  <div style="position:relative">
    <button
      style="position:relative;background:none;border:none;cursor:pointer;padding:4px;color:#6B7280;display:flex;align-items:center"
      @click="toggle"
    >
      <UIcon name="i-lucide-bell" style="width:20px;height:20px" />
      <span
        v-if="invitations.length > 0"
        style="position:absolute;top:-2px;right:-2px;min-width:16px;height:16px;border-radius:9999px;background:#EF4444;color:#fff;font-size:10px;font-weight:700;display:flex;align-items:center;justify-content:center;padding:0 3px;border:2px solid #fff"
      >{{ invitations.length }}</span>
      <span
        v-else
        style="position:absolute;top:-3px;right:-3px;width:8px;height:8px;border-radius:9999px;background:#F59E0B;border:2px solid #fff"
      />
    </button>

    <!-- Dropdown -->
    <div
      v-if="open"
      class="notif-dropdown"
      style="position:absolute;top:calc(100% + 10px);right:0;width:340px;background:#fff;border-radius:12px;box-shadow:0 8px 40px rgba(0,0,0,0.14);border:1px solid #E5E7EB;z-index:100;overflow:hidden"
    >
      <div style="padding:14px 16px;border-bottom:1px solid #F3F4F6;display:flex;align-items:center;justify-content:space-between">
        <span style="font-size:14px;font-weight:600;color:#111827">Dəvətlər</span>
        <button style="background:none;border:none;cursor:pointer;color:#9CA3AF;padding:2px" @click="open = false">
          <UIcon name="i-lucide-x" style="width:16px;height:16px" />
        </button>
      </div>

      <div v-if="loading" style="padding:32px;text-align:center;color:#9CA3AF">
        <UIcon name="i-lucide-loader-2" style="width:20px;height:20px;animation:spin 1s linear infinite" />
      </div>

      <div v-else-if="invitations.length === 0" style="padding:32px;text-align:center;color:#9CA3AF;font-size:13.5px">
        Gözləyən dəvət yoxdur
      </div>

      <div v-else style="max-height:360px;overflow-y:auto">
        <div
          v-for="inv in invitations"
          :key="inv.id"
          style="padding:14px 16px;border-bottom:1px solid #F9FAFB"
        >
          <div style="display:flex;align-items:flex-start;gap:10px;margin-bottom:10px">
            <div style="width:36px;height:36px;border-radius:8px;background:#E0E9FF;color:#3D5AF1;display:flex;align-items:center;justify-content:center;flex:none">
              <UIcon name="i-lucide-users" style="width:17px;height:17px" />
            </div>
            <div style="flex:1;min-width:0">
              <div style="font-size:13.5px;font-weight:600;color:#111827;margin-bottom:2px">{{ inv.groupName }}</div>
              <div style="font-size:12.5px;color:#6B7280">
                {{ byName(inv) }} tərəfindən dəvət
                <span v-if="inv.memberRole"> · {{ inv.memberRole }}</span>
              </div>
            </div>
          </div>
          <div style="display:flex;gap:8px">
            <button
              :disabled="actionLoading === inv.id"
              style="flex:1;height:32px;background:#3D5AF1;color:#fff;border:none;border-radius:7px;font-size:13px;font-weight:600;cursor:pointer;font-family:inherit;display:flex;align-items:center;justify-content:center;gap:6px"
              @click="respond(inv.id, 'accept')"
            >
              <UIcon v-if="actionLoading === inv.id" name="i-lucide-loader-2" style="width:13px;height:13px;animation:spin 1s linear infinite" />
              <UIcon v-else name="i-lucide-check" style="width:13px;height:13px" />
              Qəbul et
            </button>
            <button
              :disabled="actionLoading === inv.id"
              style="flex:1;height:32px;background:#fff;color:#374151;border:1px solid #D1D5DB;border-radius:7px;font-size:13px;font-weight:500;cursor:pointer;font-family:inherit"
              @click="respond(inv.id, 'decline')"
            >Rədd et</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Click-outside overlay -->
    <div v-if="open" style="position:fixed;inset:0;z-index:99" @click="open = false" />
  </div>
</template>
