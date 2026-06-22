<script setup lang="ts">
definePageMeta({ layout: 'default' })

const config = useRuntimeConfig()
const auth = useAuthStore()
const context = useContextStore()
const headers = computed(() => ({ Authorization: `Bearer ${auth.token}` }))

const { data, refresh } = await useAsyncData('buildings', () =>
  $fetch<{ content: unknown[] }>('/api/v1/buildings/search', {
    baseURL: config.public.apiBase,
    method: 'POST',
    headers: headers.value,
    body: { page: 0, perPage: 50, groupId: context.activeGroupId || undefined },
  }).catch(() => ({ content: [] })),
  { watch: [() => context.activeGroupId] },
)

const { data: groupsData } = await useAsyncData('buildings-groups', () =>
  $fetch<Record<string, unknown>[]>('/api/v1/groups', {
    baseURL: config.public.apiBase,
    headers: headers.value,
  }).catch(() => []),
)

const buildings = computed(() => (data.value?.content ?? []) as Record<string, unknown>[])
const groups = computed(() => (groupsData.value ?? []) as Record<string, unknown>[])

const modalOpen = ref(false)
const editBuilding = ref<Record<string, unknown> | null>(null)

function openCreate() { editBuilding.value = null; modalOpen.value = true }
function openEdit(b: Record<string, unknown>) { editBuilding.value = b; modalOpen.value = true }
function onSaved() { modalOpen.value = false; refresh() }

function utilPct(b: Record<string, unknown>) {
  if (!b.budgetLimit || !b.totalSpent) return 0
  return Math.min(100, (Number(b.totalSpent) / Number(b.budgetLimit)) * 100)
}

function barColor(b: Record<string, unknown>) {
  const pct = utilPct(b)
  if (pct >= 100) return '#DC2626'
  if (pct >= 80) return '#F59E0B'
  return '#3D5AF1'
}

function statusTag(b: Record<string, unknown>) {
  if (!b.budgetLimit) return { label: 'Büdcəsiz', color: '#6B7280', bg: '#F3F4F6' }
  const pct = utilPct(b)
  if (pct >= 100) return { label: 'Büdcə aşılıb', color: '#DC2626', bg: '#FEF2F2' }
  if (pct >= 80) return { label: 'Büdcə həddında', color: '#D97706', bg: '#FFFBEB' }
  return { label: 'Büdcə daxilında', color: '#16A34A', bg: '#F0FDF4' }
}

function initials(name: string) {
  return String(name ?? '?').split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2)
}

function fmt(n: number) {
  return '₼' + n.toLocaleString('az-AZ')
}

// ─── Category budget panel ────────────────────────────────────────────────────
interface BudgetRow {
  id: string
  categoryId: string
  categoryItemCode: string
  categoryItemName: string
  level1: string
  level2: string
  level3: string
  budgetLimit: number
  notes?: string
}

const budgetBuilding = ref<Record<string, unknown> | null>(null)
const budgets = ref<BudgetRow[]>([])
const budgetsLoading = ref(false)
const budgetError = ref('')
const budgetSaving = ref(false)
const budgetDeletingId = ref<string | null>(null)

const budgetForm = reactive({
  categoryId: '',
  budgetLimit: '',
  notes: '',
})
const editingBudget = ref<BudgetRow | null>(null)

async function openBudgets(b: Record<string, unknown>, e: Event) {
  e.stopPropagation()
  budgetBuilding.value = b
  budgetError.value = ''
  Object.assign(budgetForm, { categoryId: '', budgetLimit: '', notes: '' })
  editingBudget.value = null
  await loadBudgets()
}

async function loadBudgets() {
  if (!budgetBuilding.value) return
  budgetsLoading.value = true
  try {
    budgets.value = await $fetch<BudgetRow[]>(`/api/v1/buildings/${budgetBuilding.value.id}/budgets`, {
      baseURL: config.public.apiBase,
      headers: headers.value,
    })
  } finally {
    budgetsLoading.value = false
  }
}

function startEditBudget(row: BudgetRow) {
  editingBudget.value = row
  Object.assign(budgetForm, {
    categoryId: row.categoryId,
    budgetLimit: String(row.budgetLimit),
    notes: row.notes ?? '',
  })
}

async function saveBudget() {
  if (!budgetBuilding.value || !budgetForm.categoryId || !budgetForm.budgetLimit) {
    budgetError.value = 'Kateqoriya və məbləğ tələb olunur.'
    return
  }
  budgetError.value = ''
  budgetSaving.value = true
  try {
    await $fetch(`/api/v1/buildings/${budgetBuilding.value.id}/budgets/${budgetForm.categoryId}`, {
      baseURL: config.public.apiBase,
      method: 'PUT',
      headers: headers.value,
      body: {
        budgetLimit: Number(budgetForm.budgetLimit),
        notes: budgetForm.notes.trim() || undefined,
      },
    })
    Object.assign(budgetForm, { categoryId: '', budgetLimit: '', notes: '' })
    editingBudget.value = null
    await loadBudgets()
  } catch {
    budgetError.value = 'Saxlama xətası baş verdi.'
  } finally {
    budgetSaving.value = false
  }
}

async function deleteBudget(row: BudgetRow) {
  if (!budgetBuilding.value) return
  budgetDeletingId.value = row.id
  try {
    await $fetch(`/api/v1/buildings/${budgetBuilding.value.id}/budgets/${row.categoryId}`, {
      baseURL: config.public.apiBase,
      method: 'DELETE',
      headers: headers.value,
    })
    await loadBudgets()
  } finally {
    budgetDeletingId.value = null
  }
}

onMounted(() => {
  window.addEventListener('keydown', e => { if (e.key === 'Escape') budgetBuilding.value = null })
})
</script>

<template>
  <div class="animate-page">
    <div class="page-hdr">
      <div>
        <div style="font-size:12px;font-weight:500;color:#9CA3AF;letter-spacing:0.04em;text-transform:uppercase;margin-bottom:6px">Aktivlər</div>
        <h1 style="font-size:26px;font-weight:700;letter-spacing:-0.02em;margin:0 0 4px">Binalar</h1>
        <p style="font-size:14px;color:#6B7280;margin:0">{{ buildings.length }} layihə</p>
      </div>
      <div class="page-hdr-actions">
        <button
          style="display:inline-flex;align-items:center;gap:8px;height:44px;padding:0 20px;background:#3D5AF1;color:#fff;border:none;border-radius:8px;font-size:14px;font-weight:600;cursor:pointer;font-family:inherit"
          @click="openCreate"
        >
          <UIcon name="i-lucide-plus" style="width:16px;height:16px" />
          Yeni layihə
        </button>
      </div>
    </div>

    <!-- Responsive grid: 3 cols desktop, 2 tablet, 1 mobile -->
    <div class="rg-3">
      <div
        v-for="b in buildings"
        :key="String(b.id)"
        class="tk-card building-card"
        style="padding:22px;cursor:pointer"
        @click="openEdit(b)"
      >
        <div style="display:flex;align-items:flex-start;justify-content:space-between;margin-bottom:18px">
          <h3 style="font-size:16px;font-weight:600;margin:0;max-width:72%;text-wrap:balance">{{ b.name }}</h3>
          <span
            :style="`font-size:11.5px;font-weight:600;padding:3px 9px;border-radius:9999px;white-space:nowrap;color:${statusTag(b).color};background:${statusTag(b).bg}`"
          >{{ statusTag(b).label }}</span>
        </div>

        <div style="display:flex;justify-content:space-between;margin-bottom:6px">
          <span style="font-size:13px;color:#6B7280">Faktiki xərc</span>
          <span style="font-family:var(--font-mono);font-size:14px;font-weight:600">{{ fmt(Number(b.totalSpent) || 0) }}</span>
        </div>
        <div v-if="b.budgetLimit" style="display:flex;justify-content:space-between;margin-bottom:14px">
          <span style="font-size:13px;color:#6B7280">Büdcə</span>
          <span style="font-family:var(--font-mono);font-size:13.5px;color:#6B7280">{{ fmt(Number(b.budgetLimit)) }}</span>
        </div>

        <div v-if="b.budgetLimit" class="progress-track" style="margin-bottom:8px">
          <div class="progress-bar" :style="{ width: utilPct(b) + '%', background: barColor(b) }" />
        </div>
        <div v-if="b.budgetLimit" style="display:flex;justify-content:space-between;font-size:12.5px">
          <span style="color:#9CA3AF">İstifadə</span>
          <span :style="`font-family:var(--font-mono);font-weight:600;color:${barColor(b)}`">{{ utilPct(b).toFixed(0) }}%</span>
        </div>

        <div v-if="b.address || b.groupName" style="margin-top:14px;padding-top:14px;border-top:1px solid #F3F4F6;display:flex;align-items:center;gap:10px">
          <div v-if="b.groupName" style="width:26px;height:26px;border-radius:9999px;background:#141F5E;color:#fff;display:flex;align-items:center;justify-content:center;font-size:10px;font-weight:700;flex:none">
            {{ initials(String(b.groupName)) }}
          </div>
          <div style="min-width:0;flex:1">
            <div v-if="b.groupName" style="font-size:12px;font-weight:500;color:#374151;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">{{ b.groupName }}</div>
            <div v-if="b.address" style="font-size:11.5px;color:#9CA3AF;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">
              <UIcon name="i-lucide-map-pin" style="width:11px;height:11px;vertical-align:middle" />
              {{ b.address }}
            </div>
          </div>
        </div>

        <div style="margin-top:14px;padding-top:12px;border-top:1px solid #F3F4F6;display:flex;justify-content:flex-end">
          <button
            style="display:inline-flex;align-items:center;gap:7px;height:34px;padding:0 16px;background:#3D5AF1;color:#fff;border:none;border-radius:8px;font-size:13px;font-weight:600;cursor:pointer;font-family:inherit;box-shadow:0 1px 4px rgba(61,90,241,0.25)"
            @click="openBudgets(b, $event)"
          >
            <UIcon name="i-lucide-wallet" style="width:14px;height:14px" />
            Büdcə limitləri
          </button>
        </div>
      </div>

      <div v-if="!buildings.length" style="grid-column:1/-1;text-align:center;padding:60px;color:#9CA3AF;font-size:14px">
        Hələ ki bina yoxdur
      </div>
    </div>

    <BuildingModal
      v-if="modalOpen"
      :building="editBuilding"
      :groups="groups"
      @close="modalOpen = false"
      @saved="onSaved"
    />

    <!-- Budget panel overlay -->
    <Teleport to="body">
      <div
        v-if="budgetBuilding"
        style="position:fixed;inset:0;background:rgba(0,0,0,0.45);z-index:200;display:flex;align-items:flex-start;justify-content:flex-end"
        @click.self="budgetBuilding = null"
      >
        <div style="width:min(640px,100vw);height:100vh;background:#fff;display:flex;flex-direction:column;box-shadow:-4px 0 24px rgba(0,0,0,0.12)">

          <!-- Drawer header -->
          <div style="display:flex;align-items:center;justify-content:space-between;padding:20px 24px;border-bottom:1px solid #F3F4F6;flex:none">
            <div>
              <div style="font-size:11px;font-weight:600;color:#9CA3AF;text-transform:uppercase;letter-spacing:0.06em;margin-bottom:3px">Büdcə limitləri</div>
              <h2 style="font-size:18px;font-weight:700;margin:0;letter-spacing:-0.01em">{{ budgetBuilding.name }}</h2>
            </div>
            <button
              style="width:34px;height:34px;border-radius:8px;border:1px solid #E5E7EB;background:#fff;cursor:pointer;display:flex;align-items:center;justify-content:center;color:#6B7280"
              @click="budgetBuilding = null"
            >
              <UIcon name="i-lucide-x" style="width:16px;height:16px" />
            </button>
          </div>

          <!-- Scrollable body -->
          <div style="flex:1;overflow-y:auto;padding:20px 24px;display:flex;flex-direction:column;gap:20px">

            <!-- Existing budget rows -->
            <div>
              <div style="font-size:12px;font-weight:600;color:#6B7280;text-transform:uppercase;letter-spacing:0.06em;margin-bottom:10px">Mövcud limitlər</div>

              <div v-if="budgetsLoading" style="display:flex;align-items:center;gap:8px;padding:20px 0;color:#9CA3AF">
                <UIcon name="i-lucide-loader-2" style="width:16px;height:16px;animation:spin 1s linear infinite" />
                <span style="font-size:13px">Yüklənir...</span>
              </div>

              <div v-else-if="!budgets.length" style="padding:24px;text-align:center;background:#F9FAFB;border:1px solid #E5E7EB;border-radius:10px">
                <UIcon name="i-lucide-wallet" style="width:24px;height:24px;color:#D1D5DB;display:block;margin:0 auto 8px" />
                <p style="font-size:13px;color:#9CA3AF;margin:0">Bu bina üçün hələ büdcə limiti təyin edilməyib</p>
              </div>

              <div v-else style="display:flex;flex-direction:column;gap:8px">
                <div
                  v-for="row in budgets"
                  :key="row.id"
                  style="display:flex;align-items:center;gap:12px;padding:12px 14px;background:#fff;border:1px solid #E5E7EB;border-radius:8px"
                >
                  <div style="flex:1;min-width:0">
                    <div style="font-size:13.5px;font-weight:600;color:#0A0A0A">{{ row.categoryItemName }}</div>
                    <div style="font-size:11.5px;color:#9CA3AF;margin-top:2px">
                      {{ [row.level1, row.level2, row.level3].filter(Boolean).join(' › ') }}
                    </div>
                    <div v-if="row.notes" style="font-size:11.5px;color:#6B7280;margin-top:2px;font-style:italic">{{ row.notes }}</div>
                  </div>
                  <div style="text-align:right;flex:none">
                    <div style="font-family:var(--font-mono);font-size:14px;font-weight:700;color:#0A0A0A">{{ fmt(row.budgetLimit) }}</div>
                    <div style="display:flex;gap:4px;margin-top:6px;justify-content:flex-end">
                      <button
                        style="width:28px;height:28px;border-radius:6px;border:1px solid #E5E7EB;background:#fff;cursor:pointer;display:flex;align-items:center;justify-content:center;color:#6B7280"
                        title="Düzəliş et"
                        @click="startEditBudget(row)"
                      >
                        <UIcon name="i-lucide-pencil" style="width:13px;height:13px" />
                      </button>
                      <button
                        :style="`width:28px;height:28px;border-radius:6px;border:1px solid #FEE2E2;background:#FFF5F5;cursor:pointer;display:flex;align-items:center;justify-content:center;${budgetDeletingId === row.id ? 'opacity:0.6' : 'color:#DC2626'}`"
                        title="Sil"
                        :disabled="budgetDeletingId === row.id"
                        @click="deleteBudget(row)"
                      >
                        <UIcon name="i-lucide-trash-2" style="width:13px;height:13px" />
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Add / Edit form -->
            <div style="background:#F9FAFB;border:1px solid #E5E7EB;border-radius:10px;padding:18px">
              <div style="font-size:12px;font-weight:600;color:#6B7280;text-transform:uppercase;letter-spacing:0.06em;margin-bottom:14px">
                {{ editingBudget ? 'Limiti düzəliş et' : 'Yeni limit əlavə et' }}
              </div>

              <!-- Category picker -->
              <div style="margin-bottom:14px">
                <label style="display:block;font-size:12px;font-weight:600;color:#374151;margin-bottom:6px">Kateqoriya</label>
                <CategoryPicker v-model="budgetForm.categoryId" />
              </div>

              <!-- Amount + Notes -->
              <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-bottom:14px">
                <div>
                  <label style="display:block;font-size:12px;font-weight:600;color:#374151;margin-bottom:6px">Büdcə limiti (₼)</label>
                  <input
                    v-model="budgetForm.budgetLimit"
                    type="number"
                    min="0"
                    step="0.01"
                    placeholder="0.00"
                    style="width:100%;height:38px;padding:0 12px;border:1px solid #D1D5DB;border-radius:8px;font-size:13.5px;font-family:var(--font-mono);color:#0A0A0A;outline:none;box-sizing:border-box"
                  >
                </div>
                <div>
                  <label style="display:block;font-size:12px;font-weight:600;color:#374151;margin-bottom:6px">Qeyd (ixtiyari)</label>
                  <input
                    v-model="budgetForm.notes"
                    type="text"
                    placeholder="Əlavə qeyd..."
                    style="width:100%;height:38px;padding:0 12px;border:1px solid #D1D5DB;border-radius:8px;font-size:13.5px;color:#0A0A0A;font-family:inherit;outline:none;box-sizing:border-box"
                  >
                </div>
              </div>

              <div v-if="budgetError" style="font-size:12px;color:#DC2626;margin-bottom:10px">{{ budgetError }}</div>

              <div style="display:flex;gap:8px">
                <button
                  :disabled="budgetSaving"
                  style="display:inline-flex;align-items:center;gap:6px;height:38px;padding:0 18px;background:#3D5AF1;color:#fff;border:none;border-radius:8px;font-size:13.5px;font-weight:600;cursor:pointer;font-family:inherit;disabled:opacity:0.6"
                  @click="saveBudget"
                >
                  <UIcon v-if="budgetSaving" name="i-lucide-loader-2" style="width:14px;height:14px;animation:spin 1s linear infinite" />
                  <UIcon v-else name="i-lucide-save" style="width:14px;height:14px" />
                  {{ editingBudget ? 'Yadda saxla' : 'Əlavə et' }}
                </button>
                <button
                  v-if="editingBudget"
                  style="display:inline-flex;align-items:center;gap:6px;height:38px;padding:0 16px;background:#fff;color:#6B7280;border:1px solid #E5E7EB;border-radius:8px;font-size:13.5px;font-weight:500;cursor:pointer;font-family:inherit"
                  @click="editingBudget = null; Object.assign(budgetForm, { categoryId: '', budgetLimit: '', notes: '' })"
                >
                  Ləğv et
                </button>
              </div>
            </div>

          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
