<script setup lang="ts">
definePageMeta({ layout: 'default' })

const config = useRuntimeConfig()
const auth = useAuthStore()
const headers = computed(() => ({ Authorization: `Bearer ${auth.token}` }))

const { data, refresh } = await useAsyncData('groups', () =>
  $fetch<Record<string, unknown>[]>('/api/v1/groups', {
    baseURL: config.public.apiBase,
    headers: headers.value,
  }).catch(() => []),
)

const groups = computed(() => (data.value ?? []) as Record<string, unknown>[])

// Group create modal state
const newGroupOpen = ref(false)
const newGroupName = ref('')
const newGroupSaving = ref(false)

async function createGroup() {
  if (!newGroupName.value.trim()) return
  newGroupSaving.value = true
  try {
    await $fetch('/api/v1/groups', {
      baseURL: config.public.apiBase,
      method: 'POST',
      headers: headers.value,
      body: { name: newGroupName.value.trim() },
    })
    newGroupName.value = ''
    newGroupOpen.value = false
    await refresh()
  } finally {
    newGroupSaving.value = false
  }
}

// Member add modal state
const memberModalGroupId = ref<string | null>(null)
const memberModalExistingIds = ref<string[]>([])

function openAddMember(g: Record<string, unknown>) {
  memberModalGroupId.value = String(g.id)
  const members = (g.members as Record<string, unknown>[]) ?? []
  memberModalExistingIds.value = members.map(m => String(m.userId))
}

function onMemberAdded() {
  memberModalGroupId.value = null
  refresh()
}

async function removeMember(groupId: unknown, memberId: unknown) {
  await $fetch(`/api/v1/groups/${groupId}/members/${memberId}`, {
    baseURL: config.public.apiBase,
    method: 'DELETE',
    headers: headers.value,
  }).catch(() => {})
  await refresh()
}

function initials(name: string) {
  return name?.split(' ').map((w: string) => w[0]).join('').toUpperCase().slice(0, 2) ?? '??'
}
</script>

<template>
  <div class="animate-page">
    <div style="display:flex;align-items:flex-end;justify-content:space-between;gap:24px;margin-bottom:24px">
      <div>
        <div style="font-size:12px;font-weight:500;color:#9CA3AF;letter-spacing:0.04em;text-transform:uppercase;margin-bottom:6px">Təşkilat</div>
        <h1 style="font-size:26px;font-weight:700;letter-spacing:-0.02em;margin:0 0 4px">Qruplar</h1>
        <p style="font-size:14px;color:#6B7280;margin:0">{{ groups.length }} qrup</p>
      </div>
      <button
        style="display:inline-flex;align-items:center;gap:8px;height:40px;padding:0 18px;background:#3D5AF1;color:#fff;border:none;border-radius:8px;font-size:14px;font-weight:600;cursor:pointer"
        @click="newGroupOpen = true"
      >
        <UIcon name="i-lucide-plus" style="width:16px;height:16px" />
        Yeni qrup
      </button>
    </div>

    <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:18px">
      <div v-for="g in groups" :key="String(g.id)" class="tk-card" style="display:flex;flex-direction:column;overflow:hidden">
        <div style="padding:20px 20px 16px">
          <h3 style="font-size:16px;font-weight:600;margin:0 0 14px">{{ g.name }}</h3>

          <div style="font-size:12px;font-weight:600;color:#9CA3AF;text-transform:uppercase;letter-spacing:0.03em;margin-bottom:10px">Üzvlər</div>
          <div style="display:flex;flex-direction:column;gap:9px">
            <div
              v-for="m in (g.members as Record<string,unknown>[])"
              :key="String(m.memberId)"
              style="display:flex;align-items:center;gap:10px;group"
            >
              <div style="width:28px;height:28px;border-radius:9999px;background:#E0E9FF;color:#2234B0;display:flex;align-items:center;justify-content:center;font-size:10.5px;font-weight:700;flex:none">
                {{ initials(String(m.fullName || m.username || '?')) }}
              </div>
              <div style="flex:1;min-width:0">
                <span style="font-size:13px;font-weight:500">{{ m.fullName || m.username }}</span>
                <span v-if="m.memberRole" style="font-size:11.5px;color:#9CA3AF"> · {{ m.memberRole }}</span>
              </div>
              <button
                style="background:none;border:none;cursor:pointer;padding:3px;color:#D1D5DB;flex:none;display:inline-flex"
                title="Üzvü çıxar"
                @click.stop="removeMember(g.id, m.memberId)"
              >
                <UIcon name="i-lucide-x" style="width:14px;height:14px" />
              </button>
            </div>
            <div v-if="!(g.members as unknown[])?.length" style="font-size:13px;color:#9CA3AF">Üzv yoxdur</div>
          </div>
        </div>

        <div style="margin-top:auto;padding:14px 20px;background:#FAFBFC;border-top:1px solid #F3F4F6">
          <button
            style="display:inline-flex;align-items:center;gap:7px;width:100%;justify-content:center;height:36px;background:#fff;color:#3D5AF1;border:1px solid #C7D2FE;border-radius:8px;font-size:13.5px;font-weight:600;cursor:pointer;font-family:inherit"
            @click="openAddMember(g)"
          >
            <UIcon name="i-lucide-user-plus" style="width:15px;height:15px" />
            Üzv əlavə et
          </button>
        </div>
      </div>

      <div v-if="!groups.length" style="grid-column:1/-1;text-align:center;padding:60px;color:#9CA3AF;font-size:14px">
        Hələ ki qrup yoxdur
      </div>
    </div>

    <!-- New group modal -->
    <div v-if="newGroupOpen" style="position:fixed;inset:0;z-index:50;display:flex;align-items:center;justify-content:center;padding:20px">
      <div style="position:absolute;inset:0;background:rgba(0,0,0,0.35)" @click="newGroupOpen = false" />
      <div style="position:relative;background:#fff;border-radius:12px;width:100%;max-width:420px;box-shadow:0 20px 60px rgba(0,0,0,0.18);overflow:hidden">
        <div style="display:flex;align-items:center;justify-content:space-between;padding:20px 24px;border-bottom:1px solid #F3F4F6">
          <h2 style="font-size:17px;font-weight:700;margin:0">Yeni qrup</h2>
          <button style="background:none;border:none;cursor:pointer;padding:4px;color:#6B7280" @click="newGroupOpen = false">
            <UIcon name="i-lucide-x" style="width:20px;height:20px" />
          </button>
        </div>
        <div style="padding:24px">
          <label style="display:block;font-size:13px;font-weight:500;color:#374151;margin-bottom:6px">Qrup adı</label>
          <input
            v-model="newGroupName"
            type="text"
            placeholder="məs. Tikinti komandası"
            style="width:100%;height:40px;border:1px solid #D1D5DB;border-radius:8px;padding:0 12px;font-size:14px;font-family:inherit;outline:none;box-sizing:border-box"
            @keyup.enter="createGroup"
          >
        </div>
        <div style="display:flex;justify-content:flex-end;gap:10px;padding:16px 24px;border-top:1px solid #F3F4F6;background:#FAFBFC">
          <button
            style="height:38px;padding:0 18px;background:#fff;color:#374151;border:1px solid #D1D5DB;border-radius:8px;font-size:14px;font-weight:500;cursor:pointer;font-family:inherit"
            @click="newGroupOpen = false"
          >Ləğv et</button>
          <button
            :disabled="newGroupSaving || !newGroupName.trim()"
            style="height:38px;padding:0 20px;background:#3D5AF1;color:#fff;border:none;border-radius:8px;font-size:14px;font-weight:600;cursor:pointer;font-family:inherit;display:inline-flex;align-items:center;gap:8px"
            @click="createGroup"
          >
            <UIcon v-if="newGroupSaving" name="i-lucide-loader-2" style="width:15px;height:15px;animation:spin 1s linear infinite" />
            Yarat
          </button>
        </div>
      </div>
    </div>

    <!-- Add member modal -->
    <AddGroupMemberModal
      v-if="memberModalGroupId"
      :group-id="memberModalGroupId"
      :existing-member-ids="memberModalExistingIds"
      @close="memberModalGroupId = null"
      @saved="onMemberAdded"
    />
  </div>
</template>
