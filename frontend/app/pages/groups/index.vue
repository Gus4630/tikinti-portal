<script setup lang="ts">
definePageMeta({ layout: 'default' })

interface PendingInvitation {
  id: string
  invitedUserId: string
  invitedUserUsername: string
  invitedUserFullName: string | null
  memberRole: string | null
}

const config = useRuntimeConfig()
const auth = useAuthStore()
const context = useContextStore()
const headers = computed(() => ({ Authorization: `Bearer ${auth.token}` }))

const { data, refresh } = await useAsyncData('groups', () =>
  $fetch<Record<string, unknown>[]>('/api/v1/groups', {
    baseURL: config.public.apiBase,
    headers: headers.value,
  }).catch(() => []),
)

const groups = computed(() => (data.value ?? []) as Record<string, unknown>[])

// Pending invitations per group (only loaded for groups where current user is owner)
const groupInvitations = ref<Record<string, PendingInvitation[]>>({})
const invitationsLoading = ref<Record<string, boolean>>({})

async function loadInvitations(groupId: string) {
  invitationsLoading.value[groupId] = true
  try {
    const res = await $fetch<PendingInvitation[]>(`/api/v1/groups/${groupId}/invitations`, {
      baseURL: config.public.apiBase,
      headers: headers.value,
    })
    groupInvitations.value[groupId] = res
  } catch {
    groupInvitations.value[groupId] = []
  } finally {
    invitationsLoading.value[groupId] = false
  }
}

async function loadAllInvitations() {
  const ownedGroups = groups.value.filter(g => isCurrentUserOwner(g))
  await Promise.allSettled(ownedGroups.map(g => loadInvitations(String(g.id))))
}

watch(groups, loadAllInvitations, { immediate: true })

const revokingId = ref<string | null>(null)

async function revokeInvitation(groupId: string, invitationId: string) {
  revokingId.value = invitationId
  try {
    await $fetch(`/api/v1/groups/${groupId}/invitations/${invitationId}`, {
      baseURL: config.public.apiBase,
      method: 'DELETE',
      headers: headers.value,
    })
    groupInvitations.value[groupId] = (groupInvitations.value[groupId] ?? []).filter(i => i.id !== invitationId)
  } catch {
    // silently fail; could show toast
  } finally {
    revokingId.value = null
  }
}

// Create group
const newGroupOpen = ref(false)
const newGroupName = ref('')
const newGroupSaving = ref(false)
const newGroupError = ref('')

async function createGroup() {
  if (!newGroupName.value.trim()) return
  newGroupError.value = ''
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
    await context.loadGroups()
  } catch {
    newGroupError.value = 'Qrup yaradılarkən xəta baş verdi.'
  } finally {
    newGroupSaving.value = false
  }
}

// Invite modal
const inviteModalGroupId = ref<string | null>(null)
const inviteModalExistingIds = ref<string[]>([])

function openInvite(g: Record<string, unknown>) {
  inviteModalGroupId.value = String(g.id)
  inviteModalExistingIds.value = ((g.members as Record<string, unknown>[]) ?? []).map(m => String(m.userId))
}

async function onInviteSaved(groupId: string) {
  inviteModalGroupId.value = null
  await loadInvitations(groupId)
}

// Remove member
async function removeMember(groupId: unknown, memberId: unknown) {
  await $fetch(`/api/v1/groups/${groupId}/members/${memberId}`, {
    baseURL: config.public.apiBase,
    method: 'DELETE',
    headers: headers.value,
  }).catch(() => {})
  await refresh()
}

// Helpers
function initials(name: string) {
  return String(name ?? '?').split(' ').map((w: string) => w[0]).join('').toUpperCase().slice(0, 2)
}
function members(g: Record<string, unknown>) {
  return (g.members as Record<string, unknown>[]) ?? []
}
function owner(g: Record<string, unknown>) {
  return members(g).find(m => String(m.memberRole).toUpperCase() === 'OWNER') ?? null
}
function regularMembers(g: Record<string, unknown>) {
  const o = owner(g)
  return o ? members(g).filter(m => m.memberId !== o.memberId) : members(g)
}
function isCurrentUserOwner(g: Record<string, unknown>) {
  const o = owner(g)
  return !!o && String(o.userId) === auth.user?.id
}
function inviteeName(inv: PendingInvitation) {
  return inv.invitedUserFullName || inv.invitedUserUsername
}
</script>

<template>
  <div class="animate-page">
    <div class="page-hdr">
      <div>
        <div style="font-size:12px;font-weight:500;color:#9CA3AF;letter-spacing:0.04em;text-transform:uppercase;margin-bottom:6px">Təşkilat</div>
        <h1 style="font-size:26px;font-weight:700;letter-spacing:-0.02em;margin:0 0 4px">Qruplar</h1>
        <p style="font-size:14px;color:#6B7280;margin:0">{{ groups.length }} qrup</p>
      </div>
      <div class="page-hdr-actions">
        <button
          style="display:inline-flex;align-items:center;gap:8px;height:44px;padding:0 20px;background:#3D5AF1;color:#fff;border:none;border-radius:8px;font-size:14px;font-weight:600;cursor:pointer;font-family:inherit"
          @click="newGroupOpen = true"
        >
          <UIcon name="i-lucide-plus" style="width:16px;height:16px" />
          Yeni qrup
        </button>
      </div>
    </div>

    <!-- Onboarding -->
    <div
      v-if="groups.length === 0"
      style="background:linear-gradient(135deg,#EEF2FF,#F0F9FF);border:1px solid #C7D2FE;border-radius:12px;padding:40px;text-align:center;margin-bottom:24px"
    >
      <div style="width:56px;height:56px;border-radius:12px;background:#3D5AF1;display:flex;align-items:center;justify-content:center;margin:0 auto 16px">
        <UIcon name="i-lucide-users" style="width:26px;height:26px;color:#fff" />
      </div>
      <h2 style="font-size:20px;font-weight:700;color:#111827;margin:0 0 8px">Hər şey qrupla başlayır</h2>
      <p style="font-size:14px;color:#6B7280;margin:0 0 24px;max-width:380px;margin-left:auto;margin-right:auto">
        Bir qrup yaradın, layihələrinizi ona bağlayın və komanda üzvlərini dəvət edin.
      </p>
      <button
        style="display:inline-flex;align-items:center;gap:8px;height:42px;padding:0 22px;background:#3D5AF1;color:#fff;border:none;border-radius:8px;font-size:14px;font-weight:600;cursor:pointer"
        @click="newGroupOpen = true"
      >
        <UIcon name="i-lucide-plus" style="width:16px;height:16px" />
        İlk qrupumu yarat
      </button>
    </div>

    <!-- Group cards -->
    <div class="rg-auto">
      <div v-for="g in groups" :key="String(g.id)" class="tk-card" style="display:flex;flex-direction:column;overflow:hidden">
        <div style="padding:20px 20px 16px">

          <!-- Header -->
          <div style="display:flex;align-items:flex-start;justify-content:space-between;margin-bottom:16px">
            <h3 style="font-size:16px;font-weight:600;margin:0;flex:1;min-width:0">{{ g.name }}</h3>
            <span style="font-size:11.5px;color:#9CA3AF;white-space:nowrap;margin-left:8px;margin-top:2px">{{ members(g).length }} üzv</span>
          </div>

          <!-- Owner -->
          <div v-if="owner(g)" style="background:#FFFBEB;border:1px solid #FDE68A;border-radius:8px;padding:10px 12px;margin-bottom:14px;display:flex;align-items:center;gap:10px">
            <div style="width:32px;height:32px;border-radius:9999px;background:#141F5E;color:#fff;display:flex;align-items:center;justify-content:center;font-size:11px;font-weight:700;flex:none">
              {{ initials(String(owner(g)!.fullName || owner(g)!.username || '?')) }}
            </div>
            <div style="flex:1;min-width:0">
              <div style="font-size:13px;font-weight:600;color:#111827;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">
                {{ owner(g)!.fullName || owner(g)!.username }}
              </div>
              <div style="font-size:11.5px;color:#D97706;display:flex;align-items:center;gap:4px;margin-top:1px">
                <UIcon name="i-lucide-star" style="width:11px;height:11px" />
                Qrup sahibi
              </div>
            </div>
          </div>

          <!-- Regular members -->
          <div v-if="regularMembers(g).length" style="margin-bottom:14px">
            <div style="font-size:11.5px;font-weight:600;color:#9CA3AF;text-transform:uppercase;letter-spacing:0.04em;margin-bottom:10px">Üzvlər</div>
            <div style="display:flex;flex-direction:column;gap:9px">
              <div
                v-for="m in regularMembers(g)"
                :key="String(m.memberId)"
                style="display:flex;align-items:center;gap:10px"
              >
                <div style="width:28px;height:28px;border-radius:9999px;background:#E0E9FF;color:#2234B0;display:flex;align-items:center;justify-content:center;font-size:10.5px;font-weight:700;flex:none">
                  {{ initials(String(m.fullName || m.username || '?')) }}
                </div>
                <div style="flex:1;min-width:0">
                  <span style="font-size:13px;font-weight:500">{{ m.fullName || m.username }}</span>
                  <span v-if="m.memberRole" style="font-size:11.5px;color:#9CA3AF"> · {{ m.memberRole }}</span>
                </div>
                <button
                  v-if="isCurrentUserOwner(g)"
                  style="background:none;border:none;cursor:pointer;padding:3px;color:#D1D5DB;flex:none;display:inline-flex"
                  title="Üzvü çıxar"
                  @click.stop="removeMember(g.id, m.memberId)"
                >
                  <UIcon name="i-lucide-x" style="width:14px;height:14px" />
                </button>
              </div>
            </div>
          </div>

          <!-- Pending invitations (owner-only view) -->
          <div v-if="isCurrentUserOwner(g)">
            <div v-if="invitationsLoading[String(g.id)]" style="display:flex;align-items:center;gap:6px;font-size:12.5px;color:#9CA3AF;padding:6px 0">
              <UIcon name="i-lucide-loader-2" style="width:13px;height:13px;animation:spin 1s linear infinite" />
              Dəvətlər yüklənir...
            </div>
            <template v-else-if="groupInvitations[String(g.id)]?.length">
              <div style="font-size:11.5px;font-weight:600;color:#9CA3AF;text-transform:uppercase;letter-spacing:0.04em;margin-bottom:10px">Gözləyən dəvətlər</div>
              <div style="display:flex;flex-direction:column;gap:8px">
                <div
                  v-for="inv in groupInvitations[String(g.id)]"
                  :key="inv.id"
                  style="display:flex;align-items:center;gap:10px;background:#FFF7ED;border:1px solid #FED7AA;border-radius:7px;padding:8px 10px"
                >
                  <div style="width:26px;height:26px;border-radius:9999px;background:#FDE68A;color:#92400E;display:flex;align-items:center;justify-content:center;font-size:10px;font-weight:700;flex:none">
                    {{ initials(inviteeName(inv)) }}
                  </div>
                  <div style="flex:1;min-width:0">
                    <div style="font-size:12.5px;font-weight:600;color:#111827;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">{{ inviteeName(inv) }}</div>
                    <div style="font-size:11px;color:#9CA3AF;display:flex;align-items:center;gap:4px;margin-top:1px">
                      <UIcon name="i-lucide-clock" style="width:10px;height:10px" />
                      Gözləyir{{ inv.memberRole ? ' · ' + inv.memberRole : '' }}
                    </div>
                  </div>
                  <button
                    :disabled="revokingId === inv.id"
                    style="display:inline-flex;align-items:center;gap:4px;height:26px;padding:0 9px;background:#fff;color:#DC2626;border:1px solid #FECACA;border-radius:5px;font-size:11.5px;font-weight:600;cursor:pointer;font-family:inherit;flex:none"
                    :style="{ opacity: revokingId === inv.id ? '0.5' : '1' }"
                    title="Dəvəti geri al"
                    @click="revokeInvitation(String(g.id), inv.id)"
                  >
                    <UIcon v-if="revokingId === inv.id" name="i-lucide-loader-2" style="width:11px;height:11px;animation:spin 1s linear infinite" />
                    <UIcon v-else name="i-lucide-x" style="width:11px;height:11px" />
                    Geri al
                  </button>
                </div>
              </div>
            </template>
          </div>
        </div>

        <!-- Footer: invite button (owner only) -->
        <div v-if="isCurrentUserOwner(g)" style="margin-top:auto;padding:14px 20px;background:#FAFBFC;border-top:1px solid #F3F4F6">
          <button
            style="display:inline-flex;align-items:center;gap:7px;width:100%;justify-content:center;height:36px;background:#fff;color:#3D5AF1;border:1px solid #C7D2FE;border-radius:8px;font-size:13.5px;font-weight:600;cursor:pointer;font-family:inherit"
            @click="openInvite(g)"
          >
            <UIcon name="i-lucide-send" style="width:15px;height:15px" />
            Dəvət göndər
          </button>
        </div>
      </div>
    </div>

    <!-- New group modal -->
    <div v-if="newGroupOpen" style="position:fixed;inset:0;z-index:50;display:flex;align-items:center;justify-content:center;padding:16px">
      <div style="position:absolute;inset:0;background:rgba(0,0,0,0.35)" @click="newGroupOpen = false" />
      <div style="position:relative;background:#fff;border-radius:12px;width:100%;max-width:420px;box-shadow:0 20px 60px rgba(0,0,0,0.18);overflow:hidden">
        <div style="display:flex;align-items:center;justify-content:space-between;padding:20px 24px;border-bottom:1px solid #F3F4F6">
          <h2 style="font-size:17px;font-weight:700;margin:0">Yeni qrup yarat</h2>
          <button style="background:none;border:none;cursor:pointer;padding:4px;color:#6B7280" @click="newGroupOpen = false">
            <UIcon name="i-lucide-x" style="width:20px;height:20px" />
          </button>
        </div>
        <div style="padding:24px;display:flex;flex-direction:column;gap:16px">
          <div v-if="newGroupError" style="background:#FEF2F2;border:1px solid #FCA5A5;border-radius:8px;padding:10px 14px;font-size:13.5px;color:#DC2626">
            {{ newGroupError }}
          </div>
          <div>
            <label style="display:block;font-size:13px;font-weight:500;color:#374151;margin-bottom:6px">Qrup adı</label>
            <input
              v-model="newGroupName"
              type="text"
              placeholder="məs. Bakı İnşaat Qrupu"
              style="width:100%;height:42px;border:1px solid #D1D5DB;border-radius:8px;padding:0 12px;font-size:14px;font-family:inherit;outline:none;box-sizing:border-box"
              @keyup.enter="createGroup"
            >
          </div>
          <div style="background:#F0FDF4;border:1px solid #86EFAC;border-radius:8px;padding:10px 12px;font-size:13px;color:#15803D;display:flex;gap:8px;align-items:flex-start">
            <UIcon name="i-lucide-info" style="width:15px;height:15px;flex:none;margin-top:1px" />
            Siz avtomatik olaraq bu qrupun sahibi təyin ediləcəksiniz.
          </div>
        </div>
        <div style="display:flex;justify-content:flex-end;gap:10px;padding:16px 24px;border-top:1px solid #F3F4F6;background:#FAFBFC">
          <button
            style="height:38px;padding:0 18px;background:#fff;color:#374151;border:1px solid #D1D5DB;border-radius:8px;font-size:14px;font-weight:500;cursor:pointer;font-family:inherit"
            @click="newGroupOpen = false"
          >Ləğv et</button>
          <button
            :disabled="newGroupSaving || !newGroupName.trim()"
            style="height:38px;padding:0 20px;background:#3D5AF1;color:#fff;border:none;border-radius:8px;font-size:14px;font-weight:600;cursor:pointer;font-family:inherit;display:inline-flex;align-items:center;gap:8px"
            :style="{ opacity: (newGroupSaving || !newGroupName.trim()) ? '0.6' : '1' }"
            @click="createGroup"
          >
            <UIcon v-if="newGroupSaving" name="i-lucide-loader-2" style="width:15px;height:15px;animation:spin 1s linear infinite" />
            Qrup yarat
          </button>
        </div>
      </div>
    </div>

    <!-- Invite modal -->
    <GroupInviteModal
      v-if="inviteModalGroupId"
      :group-id="inviteModalGroupId"
      :existing-member-ids="inviteModalExistingIds"
      @close="inviteModalGroupId = null"
      @saved="onInviteSaved(inviteModalGroupId!)"
    />
  </div>
</template>
