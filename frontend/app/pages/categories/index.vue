<script setup lang="ts">
definePageMeta({ layout: 'default' })

const config = useRuntimeConfig()
const auth = useAuthStore()
const { resolveError } = useApiError()
const headers = computed(() => ({ Authorization: `Bearer ${auth.token}` }))

interface Category {
  id: string
  itemCode: string
  level1: string
  level2: string
  level3: string
  itemName: string
  itemDescription?: string
  displayOrder?: number
  isActive: boolean
}

// ─── Fetch (paginate through all pages, backend max is 100) ──────────────────
async function fetchAllCategories(): Promise<Category[]> {
  const results: Category[] = []
  let page = 0
  while (true) {
    const res = await $fetch<{ content: Category[]; last: boolean }>('/api/v1/categories/search', {
      baseURL: config.public.apiBase,
      method: 'POST',
      headers: headers.value,
      body: { page, perPage: 100, sortBy: 'displayOrder', sortDir: 'ASC' },
    })
    results.push(...(res.content ?? []))
    if (res.last) break
    page++
  }
  return results
}

const { data: rawCategories, refresh } = await useAsyncData('categories-page',
  () => fetchAllCategories().catch(() => [] as Category[]),
)
const allCategories = computed<Category[]>(() => rawCategories.value ?? [])

// ─── Filter/search ───────────────────────────────────────────────────────────
const search = ref('')
const activeLevel1 = ref<string | null>(null)

const filtered = computed(() => {
  let list = allCategories.value.filter(c => c.isActive !== false)
  if (search.value.trim()) {
    const q = search.value.toLowerCase()
    list = list.filter(c =>
      c.itemName.toLowerCase().includes(q) ||
      c.itemCode.toLowerCase().includes(q) ||
      c.level1.toLowerCase().includes(q) ||
      c.level2.toLowerCase().includes(q) ||
      c.level3.toLowerCase().includes(q),
    )
  }
  if (activeLevel1.value) list = list.filter(c => c.level1 === activeLevel1.value)
  return list
})

// ─── Tree grouping ───────────────────────────────────────────────────────────
const level1Groups = computed(() => {
  const seen = new Set<string>()
  allCategories.value.forEach(c => seen.add(c.level1))
  return [...seen].sort()
})

// Record<level1, Record<level2, Category[]>>
const tree = computed(() => {
  const result: Record<string, Record<string, Category[]>> = {}
  filtered.value.forEach(c => {
    if (!result[c.level1]) result[c.level1] = {}
    if (!result[c.level1][c.level2]) result[c.level1][c.level2] = []
    result[c.level1][c.level2].push(c)
  })
  return result
})

// ─── Combobox helpers ────────────────────────────────────────────────────────
const existingLevel1s = computed(() =>
  [...new Set(allCategories.value.map(c => c.level1))].sort()
)
function existingLevel2s(l1: string) {
  return [...new Set(allCategories.value.filter(c => c.level1 === l1).map(c => c.level2))].sort()
}
function existingLevel3s(l1: string, l2: string) {
  return [...new Set(
    allCategories.value.filter(c => c.level1 === l1 && c.level2 === l2).map(c => c.level3),
  )].sort()
}

// ─── Collapse state (default: collapsed) ─────────────────────────────────────
// undefined or true = collapsed; false = expanded
const collapsed = ref<Record<string, boolean>>({})
function isExpandedL1(l1: string) { return collapsed.value[l1] === false }
function toggleL1(l1: string) { collapsed.value[l1] = isExpandedL1(l1) }
const collapsedL2 = ref<Record<string, boolean>>({})
function isExpandedL2(key: string) { return collapsedL2.value[key] === false }
function toggleL2(key: string) { collapsedL2.value[key] = isExpandedL2(key) }

// ─── Delete ───────────────────────────────────────────────────────────────────
const deletingId = ref<string | null>(null)
const confirmDeleteId = ref<string | null>(null)

async function deleteCategory(id: string) {
  deletingId.value = id
  try {
    await $fetch(`/api/v1/categories/${id}`, {
      baseURL: config.public.apiBase,
      method: 'DELETE',
      headers: headers.value,
    })
    confirmDeleteId.value = null
    await refresh()
  } finally {
    deletingId.value = null
  }
}

// ─── Modal ────────────────────────────────────────────────────────────────────
const modalOpen = ref(false)
const modalMode = ref<'create' | 'edit'>('create')
const editingId = ref<string | null>(null)
const saving = ref(false)
const modalError = ref('')

const form = reactive({
  level1: '',
  level2: '',
  level3: '',
  itemName: '',
  itemDescription: '',
})

// Level2/3 options cascade on form.level1/level2 changes
const formLevel2Options = computed(() => existingLevel2s(form.level1))
const formLevel3Options = computed(() => existingLevel3s(form.level1, form.level2))

const formLoading = ref(false)

watch(() => form.level1, () => {
  if (!formLoading.value) { form.level2 = ''; form.level3 = '' }
})
watch(() => form.level2, () => {
  if (!formLoading.value) form.level3 = ''
})

// ─── Auto-code generation ────────────────────────────────────────────────────
// Pattern from seed: PREFIX.L1_NUM.L2_NUM.L3_NUM.ITEM_NUM  (all two-digit, zero-padded)
function deriveLevel1Prefix(l1: string): string {
  const existing = allCategories.value.find(c => c.level1 === l1)
  if (existing) return existing.itemCode.split('.')[0]
  return l1.replace(/[^A-Za-z]/g, '').slice(0, 8).toUpperCase() || 'CAT'
}

function deriveItemCode(l1: string, l2: string, l3: string): string {
  const cats = allCategories.value
  const pad = (n: number) => String(n).padStart(2, '0')

  const prefix = deriveLevel1Prefix(l1)

  const l1List = [...new Set(cats.map(c => c.level1))]
  const l1Idx = l1List.indexOf(l1)
  const l1Num = l1Idx >= 0 ? l1Idx + 1 : l1List.length + 1

  const l2List = [...new Set(cats.filter(c => c.level1 === l1).map(c => c.level2))]
  const l2Idx = l2List.indexOf(l2)
  const l2Num = l2Idx >= 0 ? l2Idx + 1 : l2List.length + 1

  const l3List = [...new Set(cats.filter(c => c.level1 === l1 && c.level2 === l2).map(c => c.level3))]
  const l3Idx = l3List.indexOf(l3)
  const l3Num = l3Idx >= 0 ? l3Idx + 1 : l3List.length + 1

  const maxItem = Math.max(0, ...cats
    .filter(c => c.level1 === l1 && c.level2 === l2 && c.level3 === l3)
    .map(c => parseInt(c.itemCode.split('.').pop() ?? '0', 10)))
  const itemNum = maxItem + 1

  return `${prefix}.${pad(l1Num)}.${pad(l2Num)}.${pad(l3Num)}.${pad(itemNum)}`
}

function deriveDisplayOrder(): number {
  return Math.max(0, ...allCategories.value.map(c => c.displayOrder ?? 0)) + 10
}

const previewCode = computed(() => {
  if (modalMode.value === 'edit') {
    return allCategories.value.find(c => c.id === editingId.value)?.itemCode ?? ''
  }
  if (form.level1 && form.level2 && form.level3) {
    return deriveItemCode(form.level1, form.level2, form.level3)
  }
  return ''
})

function openCreate(prefillL1 = '', prefillL2 = '') {
  modalMode.value = 'create'
  editingId.value = null
  Object.assign(form, {
    level1: prefillL1 || '',
    level2: prefillL2 || '',
    level3: '',
    itemName: '',
    itemDescription: '',
  })
  modalError.value = ''
  modalOpen.value = true
}

function openEdit(cat: Category) {
  formLoading.value = true
  modalMode.value = 'edit'
  editingId.value = cat.id
  Object.assign(form, {
    level1: cat.level1,
    level2: cat.level2,
    level3: cat.level3,
    itemName: cat.itemName,
    itemDescription: cat.itemDescription ?? '',
  })
  modalError.value = ''
  modalOpen.value = true
  nextTick(() => { formLoading.value = false })
}

async function saveForm() {
  modalError.value = ''
  if (!form.level1.trim()) { modalError.value = 'Səviyyə 1 tələb olunur.'; return }
  if (!form.level2.trim()) { modalError.value = 'Səviyyə 2 tələb olunur.'; return }
  if (!form.level3.trim()) { modalError.value = 'Səviyyə 3 tələb olunur.'; return }
  if (!form.itemName.trim()) { modalError.value = 'Maddə adı tələb olunur.'; return }

  const l1 = form.level1.trim()
  const l2 = form.level2.trim()
  const l3 = form.level3.trim()

  const existingCat = allCategories.value.find(c => c.id === editingId.value)
  const itemCode = modalMode.value === 'edit'
    ? (existingCat?.itemCode ?? deriveItemCode(l1, l2, l3))
    : deriveItemCode(l1, l2, l3)
  const displayOrder = modalMode.value === 'edit'
    ? (existingCat?.displayOrder ?? deriveDisplayOrder())
    : deriveDisplayOrder()

  const body = {
    itemCode,
    level1: l1,
    level2: l2,
    level3: l3,
    itemName: form.itemName.trim(),
    itemDescription: form.itemDescription.trim() || undefined,
    displayOrder,
  }

  saving.value = true
  try {
    if (modalMode.value === 'create') {
      await $fetch('/api/v1/categories', {
        baseURL: config.public.apiBase, method: 'POST', headers: headers.value, body,
      })
    } else {
      await $fetch(`/api/v1/categories/${editingId.value}`, {
        baseURL: config.public.apiBase, method: 'PUT', headers: headers.value, body,
      })
    }
    modalOpen.value = false
    await refresh()
  } catch (err: unknown) {
    const { message } = resolveError(err)
    modalError.value = message
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  window.addEventListener('keydown', e => { if (e.key === 'Escape') { modalOpen.value = false; confirmDeleteId.value = null } })
})


const totalCount = computed(() => allCategories.value.filter(c => c.isActive !== false).length)
</script>

<template>
  <div class="animate-page">
    <!-- Page header -->
    <div style="display:flex;align-items:flex-start;justify-content:space-between;margin-bottom:22px;gap:12px;flex-wrap:wrap">
      <div>
        <div style="font-size:12px;font-weight:500;color:#9CA3AF;letter-spacing:0.04em;text-transform:uppercase;margin-bottom:4px">Məlumat kataloqu</div>
        <h1 style="font-size:26px;font-weight:700;letter-spacing:-0.02em;margin:0 0 4px">Kateqoriyalar</h1>
        <p style="font-size:14px;color:#6B7280;margin:0">
          Xərclərin təsnifatı üçün çoxsəviyyəli kateqoriya ağacı
          <span style="margin-left:8px;background:#EEF2FF;color:#3D5AF1;font-size:12px;font-weight:600;padding:2px 8px;border-radius:20px">{{ totalCount }}</span>
        </p>
      </div>
      <button
        style="display:inline-flex;align-items:center;gap:7px;height:42px;padding:0 20px;background:#3D5AF1;color:#fff;border:none;border-radius:8px;font-size:14px;font-weight:600;cursor:pointer;font-family:inherit;flex:none"
        @click="openCreate()"
      >
        <UIcon name="i-lucide-plus" style="width:16px;height:16px" />
        Yeni kateqoriya
      </button>
    </div>

    <!-- Toolbar: search + level1 filter -->
    <div style="display:flex;flex-direction:column;gap:10px;margin-bottom:18px">
      <!-- Search row -->
      <div style="display:flex;align-items:center;gap:10px">
        <div style="position:relative;flex:1;max-width:360px">
          <UIcon name="i-lucide-search" style="position:absolute;left:11px;top:50%;transform:translateY(-50%);color:#9CA3AF;width:15px;height:15px;pointer-events:none" />
          <input
            v-model="search"
            type="text"
            placeholder="Kateqoriya, kod, ad axtar..."
            style="width:100%;height:38px;background:#F3F4F6;border:1px solid transparent;border-radius:8px;padding:0 12px 0 34px;font-size:13.5px;outline:none;font-family:inherit;box-sizing:border-box"
          >
        </div>
        <span v-if="search" style="font-size:12px;color:#9CA3AF;white-space:nowrap;flex:none">
          {{ filtered.length }} nəticə
        </span>
      </div>

      <!-- Level1 chip strip — horizontal scroll, never wraps -->
      <div class="cat-chip-strip">
        <button
          :class="activeLevel1 === null ? 'cat-chip cat-chip--active' : 'cat-chip'"
          @click="activeLevel1 = null"
        >
          Hamısı
          <span class="cat-chip-count">{{ allCategories.filter(c => c.isActive !== false).length }}</span>
        </button>
        <button
          v-for="l1 in level1Groups"
          :key="l1"
          :class="activeLevel1 === l1 ? 'cat-chip cat-chip--active' : 'cat-chip'"
          @click="activeLevel1 = activeLevel1 === l1 ? null : l1"
        >
          {{ l1 }}
          <span class="cat-chip-count">{{ allCategories.filter(c => c.isActive !== false && c.level1 === l1).length }}</span>
        </button>
      </div>
    </div>

    <!-- Tree -->
    <div style="display:flex;flex-direction:column;gap:12px">
      <!-- Empty state -->
      <div
        v-if="Object.keys(tree).length === 0"
        class="tk-card"
        style="padding:56px 24px;text-align:center"
      >
        <UIcon name="i-lucide-tag" style="width:40px;height:40px;color:#D1D5DB;margin-bottom:12px" />
        <div style="font-size:15px;font-weight:600;color:#374151;margin-bottom:6px">Kateqoriya tapılmadı</div>
        <div style="font-size:13px;color:#9CA3AF;margin-bottom:20px">
          {{ search ? 'Axtarış nəticəsi yoxdur' : 'Hələ kateqoriya əlavə edilməyib' }}
        </div>
        <button
          style="display:inline-flex;align-items:center;gap:7px;height:40px;padding:0 18px;background:#3D5AF1;color:#fff;border:none;border-radius:8px;font-size:13.5px;font-weight:600;cursor:pointer;font-family:inherit"
          @click="openCreate()"
        >
          <UIcon name="i-lucide-plus" style="width:15px;height:15px" />
          Kateqoriya əlavə et
        </button>
      </div>

      <!-- Level 1 accordion sections -->
      <div
        v-for="(l2Map, l1) in tree"
        :key="l1"
        class="tk-card"
        style="overflow:hidden"
      >
        <!-- Level 1 header -->
        <div
          style="display:flex;align-items:center;gap:12px;padding:14px 20px;cursor:pointer;user-select:none;border-bottom:1px solid #F3F4F6"
          :style="{ background: isExpandedL1(String(l1)) ? '#F8F9FF' : '#fff' }"
          @click="toggleL1(String(l1))"
        >
          <UIcon
            :name="isExpandedL1(String(l1)) ? 'i-lucide-chevron-down' : 'i-lucide-chevron-right'"
            style="width:16px;height:16px;color:#6B7280;flex:none;transition:transform .15s"
          />
          <div style="width:10px;height:10px;border-radius:3px;background:#3D5AF1;flex:none" />
          <span
            style="font-size:13px;font-weight:700;color:#0A0A0A;flex:1;min-width:0"
            :style="isExpandedL1(String(l1)) ? {} : { overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap' }"
          >{{ l1 }}</span>
          <span style="font-size:12px;font-weight:600;color:#6B7280;background:#F3F4F6;padding:2px 8px;border-radius:20px;white-space:nowrap;flex:none">
            {{ Object.values(l2Map).flat().length }} maddə
          </span>
          <button
            style="display:inline-flex;align-items:center;gap:4px;height:28px;padding:0 10px;background:#EEF2FF;color:#3D5AF1;border:none;border-radius:6px;font-size:11.5px;font-weight:600;cursor:pointer;font-family:inherit;flex:none;white-space:nowrap"
            @click.stop="openCreate(String(l1))"
          >
            <UIcon name="i-lucide-plus" style="width:11px;height:11px" />
            Əlavə et
          </button>
        </div>

        <!-- Level 2 subgroups (hidden when l1 is collapsed) -->
        <div v-if="isExpandedL1(String(l1))">
          <div
            v-for="(items, l2) in l2Map"
            :key="l2"
            style="border-bottom:1px solid #F3F4F6"
          >
            <!-- Level 2 header -->
            <div
              style="display:flex;align-items:center;gap:10px;padding:10px 20px 10px 36px;cursor:pointer;user-select:none;background:#FAFBFF"
              @click="toggleL2(`${l1}::${l2}`)"
            >
              <UIcon
                :name="isExpandedL2(`${l1}::${l2}`) ? 'i-lucide-chevron-down' : 'i-lucide-chevron-right'"
                style="width:14px;height:14px;color:#9CA3AF;flex:none"
              />
              <div style="width:8px;height:8px;border-radius:2px;background:#F59E0B;flex:none" />
              <span style="font-size:13.5px;font-weight:600;color:#374151;flex:1">{{ l2 }}</span>
              <span style="font-size:11.5px;color:#9CA3AF">{{ items.length }} maddə</span>
              <button
                style="display:inline-flex;align-items:center;gap:4px;height:26px;padding:0 10px;background:#FFFBEB;color:#D97706;border:none;border-radius:5px;font-size:11.5px;font-weight:600;cursor:pointer;font-family:inherit"
                @click.stop="openCreate(String(l1), String(l2))"
              >
                <UIcon name="i-lucide-plus" style="width:11px;height:11px" />
                Əlavə et
              </button>
            </div>

            <!-- Items table -->
            <div v-if="isExpandedL2(`${l1}::${l2}`)" style="overflow-x:auto;-webkit-overflow-scrolling:touch">
              <table style="width:100%;border-collapse:collapse;min-width:560px">
                <thead>
                  <tr style="background:#F9FAFB">
                    <th style="text-align:left;padding:8px 20px 8px 52px;font-size:11.5px;font-weight:600;color:#9CA3AF;white-space:nowrap;width:100px">Kod</th>
                    <th style="text-align:left;padding:8px 12px;font-size:11.5px;font-weight:600;color:#9CA3AF">Səviyyə 3</th>
                    <th style="text-align:left;padding:8px 12px;font-size:11.5px;font-weight:600;color:#9CA3AF">Maddə adı</th>
                    <th style="width:80px" />
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="(cat, ci) in items"
                    :key="cat.id"
                    :style="ci % 2 === 0 ? 'background:#fff' : 'background:#FAFAFA'"
                  >
                    <td style="padding:10px 12px 10px 52px">
                      <span style="font-family:var(--font-mono);font-size:12px;font-weight:600;color:#3D5AF1;background:#EEF2FF;padding:2px 8px;border-radius:5px;white-space:nowrap">
                        {{ cat.itemCode }}
                      </span>
                    </td>
                    <td style="padding:10px 12px;font-size:13px;color:#6B7280;white-space:nowrap">
                      {{ cat.level3 }}
                    </td>
                    <td style="padding:10px 12px">
                      <div style="font-size:13.5px;font-weight:500;color:#0A0A0A">{{ cat.itemName }}</div>
                      <div v-if="cat.itemDescription" style="font-size:12px;color:#9CA3AF;margin-top:2px;max-width:300px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ cat.itemDescription }}</div>
                    </td>
                    <td style="padding:10px 12px 10px 8px">
                      <div style="display:flex;align-items:center;justify-content:flex-end;gap:4px">
                        <!-- Edit -->
                        <button
                          style="width:30px;height:30px;border-radius:6px;border:1px solid #E5E7EB;background:#fff;cursor:pointer;display:flex;align-items:center;justify-content:center"
                          title="Redaktə et"
                          @click="openEdit(cat)"
                        >
                          <UIcon name="i-lucide-pencil" style="width:13px;height:13px;color:#6B7280" />
                        </button>
                        <!-- Delete with inline confirm -->
                        <template v-if="confirmDeleteId === cat.id">
                          <button
                            :disabled="deletingId === cat.id"
                            style="height:30px;padding:0 10px;border-radius:6px;border:none;background:#DC2626;color:#fff;cursor:pointer;font-size:12px;font-weight:600;font-family:inherit;display:inline-flex;align-items:center;gap:4px"
                            @click="deleteCategory(cat.id)"
                          >
                            <UIcon v-if="deletingId === cat.id" name="i-lucide-loader-2" style="width:12px;height:12px;animation:spin 1s linear infinite" />
                            Sil
                          </button>
                          <button
                            style="width:30px;height:30px;border-radius:6px;border:1px solid #E5E7EB;background:#fff;cursor:pointer;display:flex;align-items:center;justify-content:center"
                            @click="confirmDeleteId = null"
                          >
                            <UIcon name="i-lucide-x" style="width:13px;height:13px;color:#9CA3AF" />
                          </button>
                        </template>
                        <button
                          v-else
                          style="width:30px;height:30px;border-radius:6px;border:1px solid #FCA5A5;background:#FEF2F2;cursor:pointer;display:flex;align-items:center;justify-content:center"
                          title="Sil"
                          @click="confirmDeleteId = cat.id"
                        >
                          <UIcon name="i-lucide-trash-2" style="width:13px;height:13px;color:#DC2626" />
                        </button>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ── Create / Edit Modal ───────────────────────────────────────────────── -->
    <Teleport to="body">
      <div
        v-if="modalOpen"
        style="position:fixed;inset:0;z-index:150;display:flex;align-items:flex-start;justify-content:center;padding:16px;overflow-y:auto"
        @click.self="modalOpen = false"
      >
        <div style="position:fixed;inset:0;background:rgba(0,0,0,0.6);backdrop-filter:blur(4px)" @click="modalOpen = false" />

        <div style="position:relative;width:100%;max-width:600px;background:#fff;border-radius:14px;box-shadow:0 20px 60px rgba(0,0,0,0.18);overflow:hidden;display:flex;flex-direction:column;margin:auto">
          <!-- Header -->
          <div style="display:flex;align-items:center;justify-content:space-between;padding:20px 24px;border-bottom:1px solid #F3F4F6;flex:none">
            <div>
              <h2 style="font-size:17px;font-weight:700;margin:0 0 2px">
                {{ modalMode === 'create' ? 'Yeni kateqoriya' : 'Kateqoriyanı redaktə et' }}
              </h2>
              <span style="font-size:12.5px;color:#6B7280">Xərc kateqoriya ağacı</span>
            </div>
            <button
              style="width:32px;height:32px;border-radius:8px;border:1px solid #E5E7EB;background:#fff;cursor:pointer;display:flex;align-items:center;justify-content:center"
              @click="modalOpen = false"
            >
              <UIcon name="i-lucide-x" style="width:16px;height:16px;color:#6B7280" />
            </button>
          </div>

          <!-- Body -->
          <div style="padding:24px;overflow-y:auto;max-height:72vh;display:flex;flex-direction:column;gap:18px">
            <div v-if="modalError" style="padding:10px 14px;background:#FEF2F2;border:1px solid #FCA5A5;border-radius:8px;font-size:13px;color:#DC2626">
              {{ modalError }}
            </div>

            <!-- Hierarchy path: level1 > level2 > level3 -->
            <div>
              <!-- Live breadcrumb path -->
              <div style="display:flex;align-items:center;gap:6px;margin-bottom:14px;min-height:20px;flex-wrap:wrap">
                <span :style="`font-size:13px;font-weight:600;${form.level1 ? 'color:#0A0A0A' : 'color:#D1D5DB'}`">{{ form.level1 || 'Səviyyə 1' }}</span>
                <UIcon name="i-lucide-chevron-right" style="width:13px;height:13px;color:#D1D5DB;flex:none" />
                <span :style="`font-size:13px;font-weight:600;${form.level2 ? 'color:#0A0A0A' : 'color:#D1D5DB'}`">{{ form.level2 || 'Səviyyə 2' }}</span>
                <UIcon name="i-lucide-chevron-right" style="width:13px;height:13px;color:#D1D5DB;flex:none" />
                <span :style="`font-size:13px;font-weight:600;${form.level3 ? 'color:#0A0A0A' : 'color:#D1D5DB'}`">{{ form.level3 || 'Səviyyə 3' }}</span>
              </div>

              <!-- Step fields — no indentation, full-width, never disabled -->
              <div style="display:flex;flex-direction:column;gap:10px">
                <!-- Step 1 -->
                <div style="display:flex;align-items:flex-start;gap:10px">
                  <div style="width:24px;height:24px;border-radius:50%;background:#3D5AF1;color:#fff;font-size:11px;font-weight:700;display:flex;align-items:center;justify-content:center;flex:none;margin-top:7px">1</div>
                  <div style="flex:1">
                    <input
                      v-model="form.level1"
                      list="dl-l1"
                      type="text"
                      placeholder="Əsas kateqoriya"
                      style="width:100%;height:38px;border:1.5px solid #E5E7EB;border-radius:8px;padding:0 12px;font-size:13.5px;font-family:inherit;outline:none;box-sizing:border-box;transition:border-color .15s"
                      @focus="(e) => (e.target as HTMLInputElement).style.borderColor='#3D5AF1'"
                      @blur="(e) => (e.target as HTMLInputElement).style.borderColor='#E5E7EB'"
                    >
                    <datalist id="dl-l1">
                      <option v-for="v in existingLevel1s" :key="v" :value="v" />
                    </datalist>
                  </div>
                </div>

                <!-- Step 2 -->
                <div style="display:flex;align-items:flex-start;gap:10px">
                  <div :style="`width:24px;height:24px;border-radius:50%;font-size:11px;font-weight:700;display:flex;align-items:center;justify-content:center;flex:none;margin-top:7px;transition:background .2s,color .2s;${form.level1 ? 'background:#F59E0B;color:#fff' : 'background:#F3F4F6;color:#9CA3AF'}`">2</div>
                  <div style="flex:1">
                    <input
                      v-model="form.level2"
                      :list="form.level1 ? 'dl-l2' : undefined"
                      type="text"
                      placeholder="Alt kateqoriya"
                      style="width:100%;height:38px;border:1.5px solid #E5E7EB;border-radius:8px;padding:0 12px;font-size:13.5px;font-family:inherit;outline:none;box-sizing:border-box;transition:border-color .15s"
                      @focus="(e) => (e.target as HTMLInputElement).style.borderColor='#F59E0B'"
                      @blur="(e) => (e.target as HTMLInputElement).style.borderColor='#E5E7EB'"
                    >
                    <datalist id="dl-l2">
                      <option v-for="v in formLevel2Options" :key="v" :value="v" />
                    </datalist>
                    <p v-if="!form.level1" style="font-size:11.5px;color:#B91C1C;margin:4px 0 0">Əvvəlcə Səviyyə 1 doldurun</p>
                  </div>
                </div>

                <!-- Step 3 -->
                <div style="display:flex;align-items:flex-start;gap:10px">
                  <div :style="`width:24px;height:24px;border-radius:50%;font-size:11px;font-weight:700;display:flex;align-items:center;justify-content:center;flex:none;margin-top:7px;transition:background .2s,color .2s;${form.level2 ? 'background:#16A34A;color:#fff' : 'background:#F3F4F6;color:#9CA3AF'}`">3</div>
                  <div style="flex:1">
                    <input
                      v-model="form.level3"
                      :list="form.level2 ? 'dl-l3' : undefined"
                      type="text"
                      placeholder="Detallaşdırma"
                      style="width:100%;height:38px;border:1.5px solid #E5E7EB;border-radius:8px;padding:0 12px;font-size:13.5px;font-family:inherit;outline:none;box-sizing:border-box;transition:border-color .15s"
                      @focus="(e) => (e.target as HTMLInputElement).style.borderColor='#16A34A'"
                      @blur="(e) => (e.target as HTMLInputElement).style.borderColor='#E5E7EB'"
                    >
                    <datalist id="dl-l3">
                      <option v-for="v in formLevel3Options" :key="v" :value="v" />
                    </datalist>
                    <p v-if="!form.level2" style="font-size:11.5px;color:#B91C1C;margin:4px 0 0">Əvvəlcə Səviyyə 2 doldurun</p>
                  </div>
                </div>
              </div>
            </div>

            <!-- Code is generated server-side; show informational note on create only -->
            <div
              v-if="modalMode === 'create' && form.level1 && form.level2 && form.level3"
              style="display:flex;align-items:center;gap:8px;padding:9px 13px;background:#F0FDF4;border:1px solid #BBF7D0;border-radius:8px"
            >
              <UIcon name="i-lucide-hash" style="width:13px;height:13px;color:#16A34A;flex:none" />
              <span style="font-size:12px;color:#15803D">Kod backend tərəfindən avtomatik veriləcək</span>
            </div>

            <!-- Item name -->
            <div>
              <label style="display:block;font-size:13px;font-weight:500;color:#374151;margin-bottom:6px">
                Maddə adı <span style="color:#EF4444">*</span>
              </label>
              <input
                v-model="form.itemName"
                type="text"
                placeholder="Tam maddə adı"
                maxlength="200"
                style="width:100%;height:40px;border:1px solid #D1D5DB;border-radius:7px;padding:0 11px;font-size:13.5px;font-family:inherit;outline:none;box-sizing:border-box"
              >
            </div>

            <!-- Description -->
            <div>
              <label style="display:block;font-size:13px;font-weight:500;color:#374151;margin-bottom:6px">
                Açıqlama <span style="font-size:12px;color:#9CA3AF;font-weight:400">(istəyə bağlı)</span>
              </label>
              <textarea
                v-model="form.itemDescription"
                rows="3"
                placeholder="Kateqoriyanın ətraflı təsviri..."
                style="width:100%;border:1px solid #D1D5DB;border-radius:7px;padding:10px 11px;font-size:13.5px;color:#0A0A0A;font-family:inherit;outline:none;resize:vertical;box-sizing:border-box"
              />
            </div>
          </div>

          <!-- Footer -->
          <div style="display:flex;align-items:center;justify-content:flex-end;gap:10px;padding:16px 24px;border-top:1px solid #F3F4F6;background:#FAFBFC;flex:none">
            <button
              style="height:40px;padding:0 18px;background:#fff;border:1px solid #D1D5DB;border-radius:8px;font-size:14px;font-weight:500;color:#374151;cursor:pointer;font-family:inherit"
              @click="modalOpen = false"
            >
              Ləğv et
            </button>
            <button
              :disabled="saving"
              style="display:inline-flex;align-items:center;gap:7px;height:40px;padding:0 22px;background:#3D5AF1;color:#fff;border:none;border-radius:8px;font-size:14px;font-weight:600;cursor:pointer;font-family:inherit"
              :style="{ opacity: saving ? '0.7' : '1' }"
              @click="saveForm"
            >
              <UIcon v-if="saving" name="i-lucide-loader-2" style="width:15px;height:15px;animation:spin 1s linear infinite" />
              {{ saving ? 'Saxlanılır...' : 'Yadda saxla' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
/* Horizontal scroll chip strip — hides scrollbar on all browsers */
.cat-chip-strip {
  display: flex;
  align-items: center;
  gap: 6px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  padding-bottom: 2px; /* room for focus ring */
  scrollbar-width: none; /* Firefox */
}
.cat-chip-strip::-webkit-scrollbar { display: none; } /* Chrome/Safari */

.cat-chip {
  flex: none;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 32px;
  padding: 0 12px;
  border-radius: 20px;
  font-size: 12.5px;
  font-weight: 500;
  white-space: nowrap;
  cursor: pointer;
  font-family: inherit;
  border: 1px solid #E5E7EB;
  background: #fff;
  color: #6B7280;
  transition: border-color .12s, background .12s, color .12s;
}
.cat-chip:hover { border-color: #C7D2FE; color: #3D5AF1; }
.cat-chip--active {
  border-color: #3D5AF1;
  background: #3D5AF1;
  color: #fff;
}
.cat-chip--active:hover { background: #3451D1; border-color: #3451D1; color: #fff; }

.cat-chip-count {
  font-size: 10.5px;
  font-weight: 700;
  padding: 1px 5px;
  border-radius: 10px;
  background: rgba(0,0,0,0.08);
  color: inherit;
  line-height: 1.4;
}
.cat-chip--active .cat-chip-count { background: rgba(255,255,255,0.25); }
</style>
