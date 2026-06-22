<script setup lang="ts">
interface Supplier {
  id: string
  name: string
  taxId?: string
}

interface SelectedCategory {
  id: string | number
  itemName: string
  itemCode?: string
  level1: string
  level2: string
  level3: string
}

const props = defineProps<{
  buildings: Record<string, unknown>[]
}>()

const emit = defineEmits<{
  close: []
  saved: []
}>()

const config = useRuntimeConfig()
const auth = useAuthStore()
const context = useContextStore()
const { resolveError } = useApiError()

interface FormItem {
  _key: string
  description: string
  quantity: number
  unitPrice: number
}

const DRAFT_KEY = 'tk_expense_draft'

interface DraftData {
  buildingId: string
  categoryId: string
  supplierId: string
  amount: string
  currency: string
  expenseDate: string
  notes: string
  items: FormItem[]
  selectedCategory: SelectedCategory | null
}

function loadDraft(): DraftData | null {
  try {
    const raw = sessionStorage.getItem(DRAFT_KEY)
    return raw ? JSON.parse(raw) : null
  } catch { return null }
}

function saveDraft() {
  try {
    const data: DraftData = {
      buildingId: form.buildingId,
      categoryId: form.categoryId,
      supplierId: form.supplierId,
      amount: form.amount,
      currency: form.currency,
      expenseDate: form.expenseDate,
      notes: form.notes,
      items: [...form.items],
      selectedCategory: selectedCategory.value,
    }
    sessionStorage.setItem(DRAFT_KEY, JSON.stringify(data))
  } catch {}
}

function clearDraft() {
  sessionStorage.removeItem(DRAFT_KEY)
}

const draft = loadDraft()

const form = reactive({
  buildingId: draft?.buildingId || context.activeBuildingId || '',
  categoryId: draft?.categoryId || '',
  supplierId: draft?.supplierId || '',
  amount: draft?.amount || '',
  currency: draft?.currency || 'AZN',
  expenseDate: draft?.expenseDate || new Date().toISOString().slice(0, 10),
  notes: draft?.notes || '',
  items: (draft?.items ?? []) as FormItem[],
})

// ── Category picker overlay ──────────────────────────────────────────────────
const catPickerOpen = ref(false)
const selectedCategory = ref<SelectedCategory | null>(draft?.selectedCategory ?? null)

function onCategorySelected(cat: SelectedCategory) {
  selectedCategory.value = cat
  catPickerOpen.value = false
}

function clearCategory() {
  form.categoryId = ''
  selectedCategory.value = null
}

// Persist draft on every change
watch(() => ({ ...form, items: [...form.items] }), saveDraft, { deep: true })
watch(selectedCategory, saveDraft)

// ── Item form bottom sheet ───────────────────────────────────────────────────
const itemFormOpen = ref(false)
const itemFormIdx = ref(-1)
const itemForm = reactive({ description: '', quantity: 1, unitPrice: 0 })
const itemFormTotal = computed(() => (itemForm.quantity || 0) * (itemForm.unitPrice || 0))

function openAddItem() {
  itemFormIdx.value = -1
  Object.assign(itemForm, { description: '', quantity: 1, unitPrice: 0 })
  itemFormOpen.value = true
}

function openEditItem(idx: number) {
  itemFormIdx.value = idx
  const item = form.items[idx]
  Object.assign(itemForm, { description: item.description, quantity: item.quantity, unitPrice: item.unitPrice })
  itemFormOpen.value = true
}

function confirmItem() {
  if (!itemForm.description.trim()) return
  if (itemFormIdx.value === -1) {
    form.items.push({ _key: String(Date.now()), ...itemForm })
  } else {
    Object.assign(form.items[itemFormIdx.value], {
      description: itemForm.description,
      quantity: itemForm.quantity,
      unitPrice: itemForm.unitPrice,
    })
  }
  itemFormOpen.value = false
}

function removeItem(idx: number) {
  form.items.splice(idx, 1)
}

const itemsTotal = computed(() =>
  form.items.reduce((s, r) => s + (r.quantity || 0) * (r.unitPrice || 0), 0),
)

function fmt(n: number) {
  return '₼' + n.toLocaleString('az-AZ', { minimumFractionDigits: 2 })
}

// ── Suppliers ────────────────────────────────────────────────────────────────
const loading = ref(false)
const error = ref('')
const suppliers = ref<Supplier[]>([])
const suppliersLoading = ref(false)

onMounted(async () => {
  // Sync default building if draft had none (user might have switched building since last draft)
  if (!form.buildingId && context.activeBuildingId) {
    form.buildingId = context.activeBuildingId
  }

  suppliersLoading.value = true
  try {
    const res = await $fetch<{ content: Supplier[] }>('/api/v1/suppliers/search', {
      baseURL: config.public.apiBase,
      method: 'POST',
      headers: { Authorization: `Bearer ${auth.token}` },
      body: {},
    })
    suppliers.value = res.content ?? []
  } catch {
    // fail silently — supplier select will just be empty
  } finally {
    suppliersLoading.value = false
  }
})

const currencies = ['AZN', 'USD', 'EUR', 'TRY']

async function save() {
  error.value = ''

  if (!form.buildingId) { error.value = 'Layihə seçilməlidir.'; return }
  if (!form.categoryId) { error.value = 'Kateqoriya seçilməlidir.'; return }
  const hasItems = form.items.length > 0
  if (!hasItems && (!form.amount || Number(form.amount) <= 0)) { error.value = 'Məbləğ müsbət rəqəm olmalıdır.'; return }
  if (!form.expenseDate) { error.value = 'Xərc tarixi tələb olunur.'; return }

  loading.value = true
  try {
    await $fetch('/api/v1/expenses', {
      baseURL: config.public.apiBase,
      method: 'POST',
      headers: { Authorization: `Bearer ${auth.token}` },
      body: {
        buildingId: form.buildingId,
        categoryId: form.categoryId,
        supplierId: form.supplierId || undefined,
        // When items are present, backend computes amount from them; otherwise use manual input
        amount: hasItems ? undefined : Number(form.amount),
        currency: form.currency,
        expenseDate: form.expenseDate,
        notes: form.notes.trim() || undefined,
        items: hasItems
          ? form.items.map(({ description, quantity, unitPrice }) => ({ description, quantity, unitPrice }))
          : undefined,
      },
    })
    clearDraft()
    emit('saved')
    emit('close')
  } catch (err: unknown) {
    const { message } = resolveError(err)
    error.value = message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <!-- ── Main modal ─────────────────────────────────────────────────────────── -->
  <div style="position:fixed;inset:0;z-index:150;display:flex;align-items:center;justify-content:center;padding:16px">
    <div style="position:absolute;inset:0;background:rgba(0,0,0,0.5);backdrop-filter:blur(3px)" @click="emit('close')" />

    <div style="position:relative;background:#fff;border-radius:12px;width:100%;max-width:560px;max-height:90vh;display:flex;flex-direction:column;box-shadow:0 20px 60px rgba(0,0,0,0.18)">

      <!-- Header -->
      <div style="display:flex;align-items:center;justify-content:space-between;padding:18px 24px;border-bottom:1px solid #F3F4F6;flex:none;border-radius:12px 12px 0 0">
        <h2 style="font-size:17px;font-weight:700;margin:0;letter-spacing:-0.01em">Yeni xərc</h2>
        <button style="width:32px;height:32px;border-radius:8px;border:1px solid #E5E7EB;background:#fff;cursor:pointer;display:flex;align-items:center;justify-content:center" @click="emit('close')">
          <UIcon name="i-lucide-x" style="width:16px;height:16px;color:#6B7280" />
        </button>
      </div>

      <!-- Body -->
      <div style="padding:20px 24px;display:flex;flex-direction:column;gap:16px;overflow-y:auto;flex:1">

        <!-- Error banner -->
        <div v-if="error" style="background:#FEF2F2;border:1px solid #FCA5A5;border-radius:8px;padding:10px 14px;font-size:13px;color:#DC2626">{{ error }}</div>

        <!-- Building -->
        <div>
          <label style="display:block;font-size:12.5px;font-weight:600;color:#374151;margin-bottom:6px">Layihə <span style="color:#EF4444">*</span></label>
          <select v-model="form.buildingId" style="width:100%;height:40px;border:1.5px solid #E5E7EB;border-radius:8px;padding:0 12px;font-size:13.5px;background:#fff;color:#0A0A0A;font-family:inherit;outline:none;box-sizing:border-box">
            <option value="">Layihə seçin</option>
            <option v-for="b in buildings" :key="String(b.id)" :value="b.id">{{ b.name }}</option>
          </select>
        </div>

        <!-- Category: compact tap-to-open -->
        <div>
          <label style="display:block;font-size:12.5px;font-weight:600;color:#374151;margin-bottom:6px">Kateqoriya <span style="color:#EF4444">*</span></label>

          <!-- Selected -->
          <div
            v-if="selectedCategory"
            style="display:flex;align-items:center;gap:10px;padding:10px 14px;border:1.5px solid #C7D2FE;border-radius:8px;background:#EEF2FF;cursor:pointer"
            @click="catPickerOpen = true"
          >
            <UIcon name="i-lucide-check-circle-2" style="width:16px;height:16px;color:#3D5AF1;flex:none" />
            <div style="flex:1;min-width:0">
              <div style="font-size:13.5px;font-weight:600;color:#0A0A0A;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ selectedCategory.itemName }}</div>
              <div style="font-size:11.5px;color:#6B7280;margin-top:2px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ [selectedCategory.level1, selectedCategory.level2, selectedCategory.level3].filter(Boolean).join(' › ') }}</div>
            </div>
            <button
              style="flex:none;width:26px;height:26px;border-radius:6px;border:none;background:rgba(61,90,241,0.12);cursor:pointer;display:flex;align-items:center;justify-content:center"
              @click.stop="clearCategory"
            >
              <UIcon name="i-lucide-x" style="width:12px;height:12px;color:#3D5AF1" />
            </button>
          </div>

          <!-- Empty -->
          <button
            v-else
            style="width:100%;height:44px;border:1.5px dashed #D1D5DB;border-radius:8px;background:#FAFAFA;display:flex;align-items:center;justify-content:space-between;padding:0 14px;cursor:pointer;font-family:inherit;box-sizing:border-box"
            @click="catPickerOpen = true"
          >
            <span style="font-size:13.5px;color:#9CA3AF">Kateqoriya seçin...</span>
            <UIcon name="i-lucide-chevron-right" style="width:16px;height:16px;color:#D1D5DB" />
          </button>
        </div>

        <!-- Supplier -->
        <div>
          <label style="display:block;font-size:12.5px;font-weight:600;color:#374151;margin-bottom:6px">
            Təchizatçı <span style="font-size:12px;color:#9CA3AF;font-weight:400">(istəyə bağlı)</span>
          </label>
          <select v-model="form.supplierId" :disabled="suppliersLoading" style="width:100%;height:40px;border:1.5px solid #E5E7EB;border-radius:8px;padding:0 12px;font-size:13.5px;background:#fff;color:#0A0A0A;font-family:inherit;outline:none;box-sizing:border-box">
            <option value="">{{ suppliersLoading ? 'Yüklənir...' : '— Seçin —' }}</option>
            <option v-for="s in suppliers" :key="s.id" :value="s.id">{{ s.name }}{{ s.taxId ? ` (${s.taxId})` : '' }}</option>
          </select>
        </div>

        <!-- Amount + Currency -->
        <div style="display:grid;grid-template-columns:1fr 110px;gap:12px">
          <div>
            <label style="display:block;font-size:12.5px;font-weight:600;color:#374151;margin-bottom:6px">
              Məbləğ <span v-if="!form.items.length" style="color:#EF4444">*</span>
              <span v-else style="font-size:11.5px;font-weight:400;color:#9CA3AF">(maddələrdən hesablanır)</span>
            </label>
            <!-- When items exist: read-only computed total -->
            <div
              v-if="form.items.length"
              style="width:100%;height:40px;border:1.5px solid #E5E7EB;border-radius:8px;padding:0 12px;font-size:14px;font-family:var(--font-mono);box-sizing:border-box;color:#0A0A0A;background:#F9FAFB;display:flex;align-items:center"
            >{{ fmt(itemsTotal) }}</div>
            <!-- When no items: manual input -->
            <input
              v-else
              v-model="form.amount"
              type="number"
              step="0.01"
              min="0"
              placeholder="0.00"
              style="width:100%;height:40px;border:1.5px solid #E5E7EB;border-radius:8px;padding:0 12px;font-size:14px;font-family:var(--font-mono);outline:none;box-sizing:border-box;color:#0A0A0A"
            >
          </div>
          <div>
            <label style="display:block;font-size:12.5px;font-weight:600;color:#374151;margin-bottom:6px">Valyuta <span style="color:#EF4444">*</span></label>
            <select v-model="form.currency" style="width:100%;height:40px;border:1.5px solid #E5E7EB;border-radius:8px;padding:0 12px;font-size:13.5px;background:#fff;color:#0A0A0A;font-family:inherit;outline:none;box-sizing:border-box">
              <option v-for="c in currencies" :key="c" :value="c">{{ c }}</option>
            </select>
          </div>
        </div>

        <!-- Date -->
        <div>
          <label style="display:block;font-size:12.5px;font-weight:600;color:#374151;margin-bottom:6px">Xərc tarixi <span style="color:#EF4444">*</span></label>
          <input v-model="form.expenseDate" type="date" style="width:100%;height:40px;border:1.5px solid #E5E7EB;border-radius:8px;padding:0 12px;font-size:13.5px;font-family:inherit;outline:none;box-sizing:border-box;color:#0A0A0A;background:#fff">
        </div>

        <!-- Notes -->
        <div>
          <label style="display:block;font-size:12.5px;font-weight:600;color:#374151;margin-bottom:6px">
            Qeydlər <span style="font-size:12px;color:#9CA3AF;font-weight:400">(istəyə bağlı)</span>
          </label>
          <textarea v-model="form.notes" rows="2" placeholder="Xərcə aid qısa qeyd..." style="width:100%;border:1.5px solid #E5E7EB;border-radius:8px;padding:10px 12px;font-size:13.5px;color:#0A0A0A;font-family:inherit;outline:none;resize:none;box-sizing:border-box" />
        </div>

        <!-- Items section -->
        <div>
          <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:10px">
            <div>
              <span style="font-size:13px;font-weight:600;color:#374151">Qaimə maddələri</span>
              <span v-if="form.items.length" style="margin-left:8px;font-size:12px;color:#6B7280">{{ form.items.length }} maddə · <span style="font-family:var(--font-mono);font-weight:600;color:#141F5E">{{ fmt(itemsTotal) }}</span></span>
            </div>
            <button
              type="button"
              style="display:inline-flex;align-items:center;gap:5px;height:32px;padding:0 12px;background:#EEF2FF;color:#3D5AF1;border:none;border-radius:7px;font-size:12.5px;font-weight:600;cursor:pointer;font-family:inherit;flex:none"
              @click="openAddItem"
            >
              <UIcon name="i-lucide-plus" style="width:13px;height:13px" />
              Əlavə et
            </button>
          </div>

          <div style="display:flex;flex-direction:column;gap:8px">
            <div
              v-for="(item, idx) in form.items"
              :key="item._key"
              style="display:flex;align-items:center;gap:10px;padding:10px 14px;background:#F9FAFB;border:1px solid #E5E7EB;border-radius:8px"
            >
              <div style="flex:1;min-width:0">
                <div style="font-size:13.5px;font-weight:500;color:#0A0A0A;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ item.description || '—' }}</div>
                <div style="font-size:12px;color:#6B7280;margin-top:2px">
                  {{ item.quantity }} ×
                  <span style="font-family:var(--font-mono)">{{ fmt(item.unitPrice) }}</span>
                  =
                  <span style="font-family:var(--font-mono);font-weight:600;color:#141F5E">{{ fmt((item.quantity || 0) * (item.unitPrice || 0)) }}</span>
                </div>
              </div>
              <button
                type="button"
                style="width:28px;height:28px;border-radius:6px;border:1px solid #E5E7EB;background:#fff;cursor:pointer;display:flex;align-items:center;justify-content:center;flex:none"
                title="Düzəlt"
                @click="openEditItem(idx)"
              >
                <UIcon name="i-lucide-pencil" style="width:13px;height:13px;color:#6B7280" />
              </button>
              <button
                type="button"
                style="width:28px;height:28px;border-radius:6px;border:1px solid #FCA5A5;background:#FEF2F2;cursor:pointer;display:flex;align-items:center;justify-content:center;flex:none"
                title="Sil"
                @click="removeItem(idx)"
              >
                <UIcon name="i-lucide-trash-2" style="width:13px;height:13px;color:#DC2626" />
              </button>
            </div>

            <div v-if="!form.items.length" style="border:2px dashed #E5E7EB;border-radius:8px;padding:18px;text-align:center;color:#9CA3AF;font-size:12.5px">
              Maddə yoxdur — "Əlavə et" düyməsini basın
            </div>
          </div>
        </div>

      </div>

      <!-- Footer -->
      <div style="display:flex;justify-content:flex-end;gap:10px;padding:16px 24px;border-top:1px solid #F3F4F6;background:#FAFBFC;border-radius:0 0 12px 12px;flex:none">
        <button style="height:40px;padding:0 18px;background:#fff;color:#374151;border:1px solid #D1D5DB;border-radius:8px;font-size:14px;font-weight:500;cursor:pointer;font-family:inherit" @click="emit('close')">Ləğv et</button>
        <button
          :disabled="loading"
          style="height:40px;padding:0 22px;background:#3D5AF1;color:#fff;border:none;border-radius:8px;font-size:14px;font-weight:600;cursor:pointer;font-family:inherit;display:inline-flex;align-items:center;gap:8px"
          :style="{ opacity: loading ? '0.7' : '1' }"
          @click="save"
        >
          <UIcon v-if="loading" name="i-lucide-loader-2" style="width:15px;height:15px;animation:spin 1s linear infinite" />
          {{ loading ? 'Saxlanılır...' : 'Saxla' }}
        </button>
      </div>

    </div>
  </div>

  <!-- ── Category picker overlay ──────────────────────────────────────────── -->
  <Teleport to="body">
    <Transition name="sheet">
      <div v-if="catPickerOpen" class="s-overlay">
        <div class="s-backdrop" @click="catPickerOpen = false" />
        <div class="s-panel s-panel--tall">
          <div class="s-handle"><div class="s-handle-bar" /></div>
          <div class="s-head">
            <h3 class="s-title">Kateqoriya seçin</h3>
            <button class="s-close-btn" @click="catPickerOpen = false">
              <UIcon name="i-lucide-x" style="width:15px;height:15px;color:#6B7280" />
            </button>
          </div>
          <div class="s-body">
            <CategoryPicker v-model="form.categoryId" @selected="onCategorySelected" />
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>

  <!-- ── Item form overlay ──────────────────────────────────────────────────── -->
  <Teleport to="body">
    <Transition name="sheet">
      <div v-if="itemFormOpen" class="s-overlay">
        <div class="s-backdrop" @click="itemFormOpen = false" />
        <div class="s-panel">
          <div class="s-handle"><div class="s-handle-bar" /></div>
          <div style="padding:8px 24px 28px">
            <div class="s-head" style="margin-bottom:20px">
              <h3 class="s-title">{{ itemFormIdx === -1 ? 'Maddə əlavə et' : 'Maddəni düzəlt' }}</h3>
              <button class="s-close-btn" @click="itemFormOpen = false">
                <UIcon name="i-lucide-x" style="width:15px;height:15px;color:#6B7280" />
              </button>
            </div>

            <div style="display:flex;flex-direction:column;gap:14px">
              <div>
                <label style="display:block;font-size:12.5px;font-weight:600;color:#374151;margin-bottom:6px">Təsvir <span style="color:#EF4444">*</span></label>
                <input
                  v-model="itemForm.description"
                  type="text"
                  placeholder="Məhsul / xidmət adı"
                  style="width:100%;height:44px;border:1.5px solid #E5E7EB;border-radius:8px;padding:0 13px;font-size:14px;font-family:inherit;outline:none;box-sizing:border-box;color:#0A0A0A"
                >
              </div>

              <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px">
                <div>
                  <label style="display:block;font-size:12.5px;font-weight:600;color:#374151;margin-bottom:6px">Miqdar</label>
                  <input
                    v-model.number="itemForm.quantity"
                    type="number"
                    min="1"
                    placeholder="1"
                    style="width:100%;height:44px;border:1.5px solid #E5E7EB;border-radius:8px;padding:0 13px;font-size:15px;font-family:var(--font-mono);outline:none;box-sizing:border-box;color:#0A0A0A"
                  >
                </div>
                <div>
                  <label style="display:block;font-size:12.5px;font-weight:600;color:#374151;margin-bottom:6px">Vahid qiyməti (₼)</label>
                  <input
                    v-model.number="itemForm.unitPrice"
                    type="number"
                    min="0"
                    step="0.01"
                    placeholder="0.00"
                    style="width:100%;height:44px;border:1.5px solid #E5E7EB;border-radius:8px;padding:0 13px;font-size:15px;font-family:var(--font-mono);outline:none;box-sizing:border-box;color:#0A0A0A"
                  >
                </div>
              </div>

              <div
                v-if="itemFormTotal > 0"
                style="display:flex;align-items:center;justify-content:space-between;padding:10px 14px;background:#F0FDF4;border:1px solid #BBF7D0;border-radius:8px"
              >
                <span style="font-size:13px;color:#15803D;font-weight:500">Cəmi</span>
                <span style="font-family:var(--font-mono);font-size:15px;font-weight:700;color:#15803D">{{ fmt(itemFormTotal) }}</span>
              </div>

              <button
                :disabled="!itemForm.description.trim()"
                style="width:100%;height:46px;background:#3D5AF1;color:#fff;border:none;border-radius:10px;font-size:14px;font-weight:600;cursor:pointer;font-family:inherit;margin-top:2px"
                :style="{ opacity: !itemForm.description.trim() ? '0.45' : '1' }"
                @click="confirmItem"
              >
                {{ itemFormIdx === -1 ? 'Əlavə et' : 'Yadda saxla' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
/* ── Shared overlay shell ──────────────────────────────────────────────────── */
.s-overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  display: flex;
  flex-direction: column;
  justify-content: flex-end; /* mobile: bottom sheet */
  align-items: stretch;
}
.s-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
}
.s-panel {
  position: relative;
  background: #fff;
  border-radius: 16px 16px 0 0;
  box-shadow: 0 -4px 30px rgba(0, 0, 0, 0.12);
}
.s-panel--tall {
  max-height: 88vh;
  display: flex;
  flex-direction: column;
}
.s-handle {
  display: flex;
  justify-content: center;
  padding: 10px 0 2px;
  flex: none;
}
.s-handle-bar {
  width: 36px;
  height: 4px;
  border-radius: 2px;
  background: #E5E7EB;
}
.s-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 20px 14px;
  flex: none;
}
.s-title {
  font-size: 16px;
  font-weight: 700;
  margin: 0;
}
.s-close-btn {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  border: 1px solid #E5E7EB;
  background: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
.s-body {
  flex: 1;
  overflow-y: auto;
  padding: 0 20px 32px;
}

/* ── Desktop: centered dialog instead of bottom sheet ─────────────────────── */
@media (min-width: 640px) {
  .s-overlay {
    justify-content: center;
    align-items: center;
    padding: 16px;
  }
  .s-panel {
    border-radius: 12px;
    width: 100%;
    max-width: 540px;
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  }
  .s-panel--tall {
    max-height: 80vh;
  }
  .s-handle {
    display: none;
  }
  .s-head {
    padding: 18px 24px 14px;
  }
  .s-body {
    padding: 0 24px 24px;
  }
}

/* ── Transition: slide-up on mobile, fade+scale on desktop ────────────────── */
.sheet-enter-active,
.sheet-leave-active {
  transition: none;
}
/* backdrop */
.sheet-enter-active .s-backdrop,
.sheet-leave-active .s-backdrop {
  transition: opacity 0.22s ease;
}
.sheet-enter-from .s-backdrop,
.sheet-leave-to .s-backdrop {
  opacity: 0;
}
/* panel — mobile: translateY, desktop: scale */
.sheet-enter-active .s-panel,
.sheet-leave-active .s-panel {
  transition: transform 0.22s cubic-bezier(0.32, 0.72, 0, 1), opacity 0.18s ease;
}
.sheet-enter-from .s-panel,
.sheet-leave-to .s-panel {
  transform: translateY(100%);
  opacity: 1;
}
@media (min-width: 640px) {
  .sheet-enter-from .s-panel,
  .sheet-leave-to .s-panel {
    transform: scale(0.95) translateY(0);
    opacity: 0;
  }
  .sheet-enter-active .s-panel,
  .sheet-leave-active .s-panel {
    transition: transform 0.18s cubic-bezier(0.34, 1.2, 0.64, 1), opacity 0.15s ease;
  }
}
</style>
