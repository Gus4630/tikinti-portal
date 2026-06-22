<script setup lang="ts">
definePageMeta({ layout: 'default' })

const route = useRoute()
const config = useRuntimeConfig()
const auth = useAuthStore()

const STATUS_META: Record<string, { label: string; color: string; bg: string }> = {
  UPLOADED:       { label: 'Yükləndi',    color: '#525252', bg: '#F3F4F6' },
  OCR_PROCESSING: { label: 'Emal olunur', color: '#3B82F6', bg: '#EFF6FF' },
  PENDING_REVIEW: { label: 'Yoxlanılır',  color: '#D97706', bg: '#FFFBEB' },
  APPROVED:       { label: 'Təsdiqləndi', color: '#16A34A', bg: '#F0FDF4' },
  PAID:           { label: 'Ödənilib',    color: '#0E7490', bg: '#ECFEFF' },
  DISPUTED:       { label: 'Mübahisəli',  color: '#DC2626', bg: '#FEF2F2' },
  REJECTED:       { label: 'Rədd edildi', color: '#991B1B', bg: '#FEE2E2' },
}

interface ExpenseItem {
  id: string
  description: string
  quantity: number
  unitPrice: number
  totalPrice: number
}

interface ExpenseMedia {
  id: string
  url: string
  mediaType: 'IMAGE' | 'VIDEO'
  caption?: string
  displayOrder?: number
}

interface Expense {
  id: string
  buildingId: string
  buildingName: string
  categoryId: string
  categoryItemCode: string
  categoryItemName: string
  supplierId: string
  supplierName: string
  supplierTaxId?: string
  amount: number
  currency: string
  exchangeRate: number
  amountBaseCurrency: number
  contentHash: string
  imageUrl: string
  status: string
  creationType?: 'MANUAL' | 'OCR'
  notes?: string
  expenseDate: string
  isActive: boolean
  createdAt: string
  updatedAt: string
  media: ExpenseMedia[]
  items: ExpenseItem[]
}

interface Supplier {
  id: string
  name: string
  taxId?: string
}

const headers = computed(() => ({ Authorization: `Bearer ${auth.token}` }))

const { data: expense, refresh } = await useAsyncData('expense-detail', () =>
  $fetch<Expense>(`/api/v1/expenses/${route.params.id}`, {
    baseURL: config.public.apiBase,
    headers: headers.value,
  }),
)

const { data: history } = await useAsyncData('expense-history', () =>
  $fetch<unknown[]>(`/api/v1/expenses/${route.params.id}/history`, {
    baseURL: config.public.apiBase,
    headers: headers.value,
  }).catch(() => []),
)

const statusMeta = computed(() => STATUS_META[String(expense.value?.status)] ?? { label: expense.value?.status, color: '#6B7280', bg: '#F3F4F6' })
const items = computed<ExpenseItem[]>(() => expense.value?.items ?? [])
const mediaList = computed<ExpenseMedia[]>(() => expense.value?.media ?? [])

// ─── Invoice image ─────────────────────────────────────────────────────────
const invoiceBlobUrl = ref<string | null>(null)
const invoiceIsPdf = ref(false)
const invoiceLoading = ref(false)
const invoiceError = ref(false)

async function loadInvoiceImage() {
  if (!expense.value?.imageUrl) return
  invoiceLoading.value = true
  invoiceError.value = false
  try {
    const blob = await $fetch<Blob>(`/api/v1/files/${route.params.id}/invoice`, {
      baseURL: config.public.apiBase,
      headers: headers.value,
      responseType: 'blob',
    } as Parameters<typeof $fetch>[1])
    if (invoiceBlobUrl.value) URL.revokeObjectURL(invoiceBlobUrl.value)
    invoiceIsPdf.value = blob.type?.includes('pdf') ?? false
    invoiceBlobUrl.value = URL.createObjectURL(blob)
  } catch {
    invoiceError.value = true
  } finally {
    invoiceLoading.value = false
  }
}

const isOcr = computed(() => expense.value?.creationType === 'OCR' || expense.value?.imageUrl)
const showFilePanel = computed(() => isOcr.value || mediaList.value.length > 0)

onMounted(() => { if (isOcr.value) loadInvoiceImage() })
onUnmounted(() => { if (invoiceBlobUrl.value) URL.revokeObjectURL(invoiceBlobUrl.value) })

// ─── Media upload ──────────────────────────────────────────────────────────
const mediaFileInput = ref<HTMLInputElement>()
const mediaUploading = ref(false)

async function uploadMedia(file: File) {
  mediaUploading.value = true
  try {
    const reader = new FileReader()
    const base64 = await new Promise<string>((resolve, reject) => {
      reader.onload = () => resolve((reader.result as string).split(',')[1])
      reader.onerror = reject
      reader.readAsDataURL(file)
    })
    await $fetch(`/api/v1/expenses/${route.params.id}/media`, {
      baseURL: config.public.apiBase,
      method: 'POST',
      headers: headers.value,
      body: {
        url: `data:${file.type};base64,${base64}`,
        mediaType: file.type.startsWith('video') ? 'VIDEO' : 'IMAGE',
        caption: file.name,
        displayOrder: mediaList.value.length,
      },
    })
    await refresh()
  } catch { /* ignore */ } finally {
    mediaUploading.value = false
  }
}

function onMediaFileChange(e: Event) {
  const f = (e.target as HTMLInputElement).files?.[0]
  if (f) uploadMedia(f)
}

// ─── Actions ───────────────────────────────────────────────────────────────
const actionLoading = ref('')
const ACTION_ENDPOINT: Record<string, string> = { APPROVED: 'approve', PAID: 'pay', DISPUTED: 'dispute', REJECTED: 'reject' }

async function doAction(action: 'APPROVED' | 'PAID' | 'DISPUTED' | 'REJECTED') {
  actionLoading.value = action
  try {
    await $fetch(`/api/v1/expenses/${route.params.id}/${ACTION_ENDPOINT[action]}`, {
      baseURL: config.public.apiBase, method: 'POST', headers: headers.value,
    })
    await refresh()
  } finally { actionLoading.value = '' }
}

const retriggerLoading = ref(false)
const retriggerSuccess = ref(false)

async function retriggerOcr() {
  retriggerLoading.value = true
  retriggerSuccess.value = false
  try {
    await $fetch(`/api/v1/invoices/${route.params.id}/retrigger`, {
      baseURL: config.public.apiBase, method: 'POST', headers: headers.value,
    })
    retriggerSuccess.value = true
    setTimeout(() => { retriggerSuccess.value = false }, 3000)
    await refresh()
  } finally { retriggerLoading.value = false }
}

const canRetrigger = computed(() => expense.value?.status !== 'APPROVED' && expense.value?.status !== 'PAID')

// ─── Edit modal ────────────────────────────────────────────────────────────
const { resolveError } = useApiError()
const editOpen = ref(false)
const editLoading = ref(false)
const editError = ref('')

const suppliers = ref<Supplier[]>([])

interface EditItem {
  _key: string
  description: string
  quantity: number
  unitPrice: number
}

const editForm = reactive({
  categoryId: '',
  supplierId: '',
  amount: '',
  currency: 'AZN',
  expenseDate: '',
  notes: '',
  items: [] as EditItem[],
})

const editItemsTotal = computed(() =>
  editForm.items.reduce((s, r) => s + (r.quantity || 0) * (r.unitPrice || 0), 0),
)

async function openEdit() {
  editError.value = ''
  editForm.categoryId = String(expense.value?.categoryId ?? '')
  editForm.supplierId = String(expense.value?.supplierId ?? '')
  editForm.amount = String(expense.value?.amount ?? '')
  editForm.currency = String(expense.value?.currency ?? 'AZN')
  editForm.expenseDate = String(expense.value?.expenseDate ?? '')
  editForm.notes = String(expense.value?.notes ?? '')
  editForm.items = (expense.value?.items ?? []).map((it, i) => ({
    _key: String(i),
    description: it.description,
    quantity: it.quantity,
    unitPrice: it.unitPrice,
  }))

  if (!suppliers.value.length) {
    const supsResp = await $fetch<{ content: Supplier[] }>('/api/v1/suppliers/search', {
      baseURL: config.public.apiBase, method: 'POST', headers: headers.value, body: {},
    }).catch(() => ({ content: [] as Supplier[] }))
    suppliers.value = supsResp.content ?? []
  }

  editOpen.value = true
}

function addEditItem() {
  editForm.items.push({ _key: String(Date.now()), description: '', quantity: 1, unitPrice: 0 })
}

function removeEditItem(idx: number) {
  editForm.items.splice(idx, 1)
}

async function saveEdit() {
  editError.value = ''
  editLoading.value = true
  try {
    await $fetch(`/api/v1/expenses/${route.params.id}`, {
      baseURL: config.public.apiBase,
      method: 'PUT',
      headers: headers.value,
      body: {
        categoryId: editForm.categoryId || null,
        supplierId: editForm.supplierId || null,
        amount: parseFloat(editForm.amount),
        currency: editForm.currency,
        expenseDate: editForm.expenseDate,
        notes: editForm.notes || null,
        items: editForm.items.map(({ description, quantity, unitPrice }) => ({ description, quantity, unitPrice })),
      },
    })
    editOpen.value = false
    await refresh()
  } catch (err: unknown) {
    const { message } = resolveError(err)
    editError.value = message
  } finally {
    editLoading.value = false
  }
}

onMounted(() => {
  window.addEventListener('keydown', (e) => { if (e.key === 'Escape') editOpen.value = false })
})

// ─── Helpers ───────────────────────────────────────────────────────────────
function fmt(n: number) {
  return '₼' + n.toLocaleString('az-AZ', { minimumFractionDigits: 2 })
}

function fmtDate(iso: string) {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString('az-AZ', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })
}
</script>

<template>
  <div class="animate-page">
    <!-- Breadcrumb -->
    <div style="display:flex;align-items:center;gap:6px;font-size:13px;color:#6B7280;margin-bottom:14px">
      <NuxtLink to="/expenses" style="cursor:pointer;color:#6B7280;text-decoration:none">Xərclər</NuxtLink>
      <UIcon name="i-lucide-chevron-right" style="width:14px;height:14px;color:#9CA3AF" />
      <span style="font-family:var(--font-mono);color:#374151;font-weight:500">{{ expense?.id?.toString().slice(0, 8) }}…</span>
    </div>

    <!-- Page header -->
    <div style="display:flex;align-items:flex-start;justify-content:space-between;margin-bottom:20px;gap:12px;flex-wrap:wrap">
      <div style="min-width:0">
        <h1 style="font-size:24px;font-weight:700;letter-spacing:-0.02em;margin:0 0 4px">Xərc təfərrüatı</h1>
        <p style="font-size:14px;color:#6B7280;margin:0">{{ expense?.buildingName }}</p>
      </div>
      <div style="display:flex;align-items:center;gap:10px;flex:none;flex-wrap:wrap">
        <button
          style="display:inline-flex;align-items:center;gap:7px;height:40px;padding:0 16px;background:#fff;border:1px solid #D1D5DB;border-radius:8px;font-size:13.5px;font-weight:600;color:#374151;cursor:pointer;font-family:inherit"
          @click="openEdit"
        >
          <UIcon name="i-lucide-pencil" style="width:15px;height:15px" />
          Redaktə et
        </button>
        <span
          :style="`display:inline-flex;align-items:center;gap:6px;padding:5px 14px;border-radius:9999px;font-size:13px;font-weight:600;color:${statusMeta.color};background:${statusMeta.bg}`"
        >
          <span :style="`width:7px;height:7px;border-radius:9999px;background:${statusMeta.color}`" />
          {{ statusMeta.label }}
        </span>
      </div>
    </div>

    <!-- Main grid — 2 col desktop, 1 col mobile -->
    <div
      class="expense-detail-grid"
      style="display:grid;grid-template-columns:1.05fr 1fr;gap:20px;margin-bottom:20px"
    >
      <!-- Left: Invoice viewer (OCR only) + media for all -->
      <div class="tk-card" style="padding:20px;display:flex;flex-direction:column;gap:16px">
        <div style="display:flex;align-items:center;justify-content:space-between">
          <h3 style="font-size:15px;font-weight:600;margin:0">
            {{ isOcr ? 'Qaimə fayl' : 'Fayllar' }}
          </h3>
          <div style="display:flex;align-items:center;gap:8px">
            <a
              v-if="invoiceBlobUrl"
              :href="invoiceBlobUrl"
              :download="`invoice-${route.params.id}`"
              target="_blank"
              style="display:inline-flex;align-items:center;gap:5px;font-size:12.5px;color:#3D5AF1;text-decoration:none;font-weight:500"
            >
              <UIcon name="i-lucide-download" style="width:13px;height:13px" />
              Yüklə
            </a>
            <!-- Upload media button for any expense -->
            <input ref="mediaFileInput" type="file" accept="image/*,video/*" style="display:none" @change="onMediaFileChange">
            <button
              :disabled="mediaUploading"
              style="display:inline-flex;align-items:center;gap:5px;height:30px;padding:0 10px;background:#EEF2FF;color:#3D5AF1;border:none;border-radius:6px;font-size:12px;font-weight:600;cursor:pointer;font-family:inherit"
              @click="mediaFileInput?.click()"
            >
              <UIcon v-if="mediaUploading" name="i-lucide-loader-2" style="width:12px;height:12px;animation:spin 1s linear infinite" />
              <UIcon v-else name="i-lucide-paperclip" style="width:12px;height:12px" />
              Fayl əlavə et
            </button>
          </div>
        </div>

        <!-- OCR invoice image -->
        <div v-if="isOcr" style="background:#F3F4F6;border-radius:10px;overflow:hidden;min-height:280px;display:flex;align-items:center;justify-content:center">
          <div v-if="invoiceLoading" style="display:flex;flex-direction:column;align-items:center;gap:10px;color:#9CA3AF">
            <UIcon name="i-lucide-loader-2" style="width:28px;height:28px;animation:spin 1s linear infinite" />
            <span style="font-size:13px">Fayl yüklənir…</span>
          </div>
          <iframe v-else-if="invoiceBlobUrl && invoiceIsPdf" :src="invoiceBlobUrl" style="width:100%;height:480px;border:none" />
          <img v-else-if="invoiceBlobUrl" :src="invoiceBlobUrl" alt="Qaimə" style="width:100%;max-height:480px;object-fit:contain;display:block" />
          <div v-else style="display:flex;flex-direction:column;align-items:center;gap:10px;padding:40px 0;color:#9CA3AF">
            <UIcon name="i-lucide-file-x" style="width:36px;height:36px" />
            <span style="font-size:13px">{{ invoiceError ? 'Fayl tapılmadı' : 'Fayl yoxdur' }}</span>
          </div>
        </div>

        <!-- No file placeholder for MANUAL with no media -->
        <div v-else-if="!mediaList.length" style="background:#F9FAFB;border:2px dashed #E5E7EB;border-radius:10px;padding:32px;text-align:center;color:#9CA3AF">
          <UIcon name="i-lucide-paperclip" style="width:28px;height:28px;display:block;margin:0 auto 8px" />
          <span style="font-size:13px">Əlavə fayl yoxdur</span>
        </div>

        <div v-if="expense?.notes" style="padding:12px 14px;background:#F9FAFB;border:1px solid #F3F4F6;border-radius:8px">
          <div style="font-size:12px;font-weight:600;color:#6B7280;text-transform:uppercase;letter-spacing:0.03em;margin-bottom:6px">
            {{ isOcr ? 'Qeyd (OCR)' : 'Qeyd' }}
          </div>
          <p style="font-size:13px;color:#374151;margin:0;line-height:1.5">{{ expense.notes }}</p>
        </div>

        <div v-if="mediaList.length">
          <div style="font-size:13px;font-weight:600;color:#374151;margin-bottom:10px">Əlavə fayllar ({{ mediaList.length }})</div>
          <div style="display:flex;flex-wrap:wrap;gap:10px">
            <div
              v-for="m in mediaList"
              :key="m.id"
              style="width:80px;height:80px;border-radius:8px;background:#E5E7EB;border:1px solid #D1D5DB;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:4px;cursor:pointer;overflow:hidden"
            >
              <UIcon :name="m.mediaType === 'IMAGE' ? 'i-lucide-image' : 'i-lucide-video'" style="width:22px;height:22px;color:#6B7280" />
              <span style="font-size:10px;color:#9CA3AF;text-align:center;padding:0 4px;line-height:1.2;max-width:100%;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">
                {{ m.caption ?? (m.mediaType === 'IMAGE' ? 'Şəkil' : 'Video') }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- Right: Details + actions + audit -->
      <div style="display:flex;flex-direction:column;gap:20px">
        <!-- Extracted data -->
        <div class="tk-card" style="overflow:hidden">
          <div style="display:flex;align-items:center;justify-content:space-between;padding:18px 20px;border-bottom:1px solid #F3F4F6;flex-wrap:wrap;gap:10px">
            <div>
              <h3 style="font-size:15px;font-weight:600;margin:0 0 4px">Xərc məlumatları</h3>
              <span style="font-size:12px;color:#6B7280">{{ isOcr ? 'OCR ilə avtomatik dolduruldu' : 'Əl ilə daxil edildi' }}</span>
            </div>
            <span :style="`display:inline-flex;align-items:center;gap:6px;padding:4px 12px;border-radius:9999px;font-size:12px;font-weight:600;color:${statusMeta.color};background:${statusMeta.bg}`">
              {{ statusMeta.label }}
            </span>
          </div>
          <div style="padding:6px 20px">
            <div
              v-for="row in [
                { label: 'Təchizatçı', val: expense?.supplierName, mono: false },
                { label: 'VÖEN', val: expense?.supplierTaxId, mono: true },
                { label: 'Layihə', val: expense?.buildingName, mono: false },
                { label: 'Kateqoriya', val: expense?.categoryItemName, mono: false },
                { label: 'Kateqoriya kodu', val: expense?.categoryItemCode, mono: true },
                { label: 'Tarix', val: expense?.expenseDate, mono: true },
                { label: 'Valyuta', val: expense?.currency, mono: true },
                { label: 'Məzənnə', val: expense?.exchangeRate != null && Number(expense.exchangeRate) !== 1 ? String(expense.exchangeRate) : null, mono: true },
                { label: 'Məbləğ', val: expense?.amount ? fmt(Number(expense.amount)) : null, mono: true },
                { label: 'AZN məbləği', val: expense?.amountBaseCurrency ? fmt(Number(expense.amountBaseCurrency)) : null, mono: true },
              ]"
              :key="row.label"
              style="display:flex;justify-content:space-between;align-items:center;padding:10px 0;border-bottom:1px solid #F3F4F6;gap:12px"
            >
              <span style="font-size:13px;color:#6B7280;flex:none">{{ row.label }}</span>
              <span :style="row.mono ? 'font-family:var(--font-mono);font-size:12.5px;color:#374151;text-align:right' : 'font-size:13.5px;font-weight:500;color:#0A0A0A;text-align:right;max-width:58%'">{{ row.val ?? '—' }}</span>
            </div>
            <div style="display:flex;justify-content:space-between;align-items:center;padding:10px 0;border-bottom:1px solid #F3F4F6">
              <span style="font-size:13px;color:#6B7280">Əlavə edildi</span>
              <span style="font-family:var(--font-mono);font-size:12px;color:#9CA3AF">{{ expense?.createdAt ? fmtDate(expense.createdAt) : '—' }}</span>
            </div>
            <div style="display:flex;justify-content:space-between;align-items:center;padding:10px 0 4px">
              <span style="font-size:13px;color:#6B7280">Son yeniləndi</span>
              <span style="font-family:var(--font-mono);font-size:12px;color:#9CA3AF">{{ expense?.updatedAt ? fmtDate(expense.updatedAt) : '—' }}</span>
            </div>
            <div style="display:flex;justify-content:space-between;align-items:center;padding:13px 0 14px;border-top:1px solid #F3F4F6;margin-top:4px">
              <span style="font-size:14px;font-weight:600">Ümumi məbləğ</span>
              <span style="font-family:var(--font-mono);font-size:17px;font-weight:600;color:#141F5E">{{ fmt(Number(expense?.amountBaseCurrency) || 0) }}</span>
            </div>
          </div>

          <!-- Action buttons -->
          <div class="expense-action-btns" style="display:flex;flex-wrap:wrap;gap:10px;padding:16px 20px;border-top:1px solid #F3F4F6;background:#FAFBFC">
            <button
              :disabled="actionLoading !== '' || expense?.status === 'APPROVED'"
              style="display:inline-flex;align-items:center;gap:7px;height:42px;padding:0 16px;background:#16A34A;color:#fff;border:none;border-radius:8px;font-size:14px;font-weight:600;cursor:pointer;font-family:inherit;flex:1 1 auto;justify-content:center"
              @click="doAction('APPROVED')"
            >
              <UIcon v-if="actionLoading === 'APPROVED'" name="i-lucide-loader-2" style="width:15px;height:15px;animation:spin 1s linear infinite" />
              Təsdiqlə
            </button>
            <button
              :disabled="actionLoading !== '' || expense?.status !== 'APPROVED'"
              style="display:inline-flex;align-items:center;gap:7px;height:42px;padding:0 16px;background:#0E7490;color:#fff;border:none;border-radius:8px;font-size:14px;font-weight:600;cursor:pointer;font-family:inherit;flex:1 1 auto;justify-content:center"
              @click="doAction('PAID')"
            >
              <UIcon v-if="actionLoading === 'PAID'" name="i-lucide-loader-2" style="width:15px;height:15px;animation:spin 1s linear infinite" />
              Ödə
            </button>
            <button
              :disabled="actionLoading !== '' || expense?.status === 'DISPUTED'"
              style="height:42px;padding:0 16px;background:#fff;color:#D97706;border:1px solid #FCD34D;border-radius:8px;font-size:14px;font-weight:600;cursor:pointer;font-family:inherit;flex:1 1 auto"
              @click="doAction('DISPUTED')"
            >Mübahisə et</button>
            <button
              :disabled="actionLoading !== '' || expense?.status === 'REJECTED'"
              style="height:42px;padding:0 16px;background:#fff;color:#DC2626;border:1px solid #FCA5A5;border-radius:8px;font-size:14px;font-weight:600;cursor:pointer;font-family:inherit;flex:1 1 auto"
              @click="doAction('REJECTED')"
            >Rədd et</button>
            <button
              v-if="canRetrigger"
              :disabled="retriggerLoading || actionLoading !== ''"
              :style="`display:inline-flex;align-items:center;gap:7px;height:42px;padding:0 16px;border-radius:8px;font-size:13.5px;font-weight:600;cursor:pointer;font-family:inherit;transition:all .15s;width:100%;justify-content:center;${retriggerSuccess ? 'background:#F0FDF4;color:#16A34A;border:1px solid #86EFAC' : 'background:#fff;color:#6B7280;border:1px solid #D1D5DB'}`"
              @click="retriggerOcr"
            >
              <UIcon
                :name="retriggerSuccess ? 'i-lucide-check' : retriggerLoading ? 'i-lucide-loader-2' : 'i-lucide-refresh-cw'"
                :style="`width:15px;height:15px${retriggerLoading ? ';animation:spin 1s linear infinite' : ''}`"
              />
              {{ retriggerSuccess ? 'Göndərildi' : 'OCR yenidən başlat' }}
            </button>
          </div>
        </div>

        <!-- Audit timeline -->
        <div class="tk-card" style="padding:20px">
          <h3 style="font-size:15px;font-weight:600;margin:0 0 18px">Audit izi</h3>
          <div v-if="Array.isArray(history) && history.length" style="display:flex;flex-direction:column">
            <div v-for="(ev, i) in (history as Record<string,unknown>[])" :key="i" style="display:flex;gap:14px;padding-bottom:18px">
              <div style="display:flex;flex-direction:column;align-items:center;flex:none">
                <span style="width:10px;height:10px;border-radius:9999px;background:#3D5AF1;flex:none" />
                <span v-if="i < (history as unknown[]).length - 1" style="width:2px;flex:1;background:#F3F4F6;margin-top:3px" />
              </div>
              <div style="flex:1;margin-top:-2px">
                <div style="font-size:13.5px;font-weight:500;color:#0A0A0A">
                  {{ STATUS_META[String((ev as Record<string,unknown>).status)]?.label ?? (ev as Record<string,unknown>).status ?? 'Dəyişiklik' }}
                </div>
                <div style="font-size:12px;color:#9CA3AF;margin-top:2px">{{ (ev as Record<string,unknown>).revisionDate }}</div>
              </div>
            </div>
          </div>
          <div v-else style="font-size:13px;color:#9CA3AF;text-align:center;padding:16px 0">Audit məlumatı yoxdur</div>
        </div>
      </div>
    </div>

    <!-- Items table -->
    <div v-if="items.length" class="tk-card" style="overflow:hidden">
      <div style="display:flex;align-items:center;justify-content:space-between;padding:18px 20px;border-bottom:1px solid #F3F4F6;flex-wrap:wrap;gap:10px">
        <div>
          <h3 style="font-size:15px;font-weight:600;margin:0 0 2px">Qaimə maddələri</h3>
          <span style="font-size:12px;color:#6B7280">{{ items.length }} maddə</span>
        </div>
        <span style="font-family:var(--font-mono);font-size:13px;font-weight:600;color:#141F5E">
          Cəmi: {{ fmt(items.reduce((s, r) => s + r.totalPrice, 0)) }}
        </span>
      </div>
      <div style="overflow-x:auto;-webkit-overflow-scrolling:touch">
        <table style="width:100%;border-collapse:collapse;min-width:400px">
          <thead>
            <tr style="background:#F9FAFB">
              <th style="text-align:left;padding:10px 20px;font-size:12px;font-weight:600;color:#6B7280">#</th>
              <th style="text-align:left;padding:10px 12px;font-size:12px;font-weight:600;color:#6B7280">Təsvir</th>
              <th style="text-align:right;padding:10px 12px;font-size:12px;font-weight:600;color:#6B7280;white-space:nowrap">Miqdar</th>
              <th style="text-align:right;padding:10px 12px;font-size:12px;font-weight:600;color:#6B7280;white-space:nowrap">Vahid ₼</th>
              <th style="text-align:right;padding:10px 20px 10px 12px;font-size:12px;font-weight:600;color:#6B7280;white-space:nowrap">Cəmi</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, idx) in items" :key="item.id" :style="idx % 2 === 0 ? 'background:#fff' : 'background:#F9FAFB'">
              <td style="padding:11px 20px;font-size:12.5px;font-family:var(--font-mono);color:#9CA3AF">{{ idx + 1 }}</td>
              <td style="padding:11px 12px;font-size:13.5px;color:#0A0A0A">{{ item.description }}</td>
              <td style="padding:11px 12px;text-align:right;font-family:var(--font-mono);font-size:13px;color:#374151">{{ item.quantity }}</td>
              <td style="padding:11px 12px;text-align:right;font-family:var(--font-mono);font-size:13px;color:#374151">{{ fmt(item.unitPrice) }}</td>
              <td style="padding:11px 20px 11px 12px;text-align:right;font-family:var(--font-mono);font-size:13px;font-weight:600;color:#141F5E">{{ fmt(item.totalPrice) }}</td>
            </tr>
          </tbody>
          <tfoot>
            <tr style="border-top:2px solid #E5E7EB;background:#F9FAFB">
              <td colspan="4" style="padding:12px 12px 12px 20px;font-size:14px;font-weight:600;color:#374151">Ümumi cəmi</td>
              <td style="padding:12px 20px 12px 12px;text-align:right;font-family:var(--font-mono);font-size:15px;font-weight:700;color:#141F5E">
                {{ fmt(items.reduce((s, r) => s + r.totalPrice, 0)) }}
              </td>
            </tr>
          </tfoot>
        </table>
      </div>
    </div>

    <!-- ── Edit modal ─────────────────────────────────────────────────────── -->
    <Teleport to="body">
      <div
        v-if="editOpen"
        style="position:fixed;inset:0;z-index:50;display:flex;align-items:flex-start;justify-content:center;padding:16px;overflow-y:auto"
        @click.self="editOpen = false"
      >
        <div style="position:fixed;inset:0;background:rgba(0,0,0,0.4);backdrop-filter:blur(2px)" @click="editOpen = false" />

        <div style="position:relative;width:100%;max-width:860px;background:#fff;border-radius:14px;box-shadow:0 20px 60px rgba(0,0,0,0.18);overflow:hidden;display:flex;flex-direction:column;margin:auto">
          <!-- Header -->
          <div style="display:flex;align-items:center;justify-content:space-between;padding:20px 24px;border-bottom:1px solid #F3F4F6;flex:none">
            <div>
              <h2 style="font-size:17px;font-weight:700;margin:0 0 2px">Xərci redaktə et</h2>
              <span style="font-size:12.5px;color:#6B7280;font-family:var(--font-mono)">#{{ String(expense?.id ?? '').slice(0, 8) }}</span>
            </div>
            <button style="width:32px;height:32px;border-radius:8px;border:1px solid #E5E7EB;background:#fff;cursor:pointer;display:flex;align-items:center;justify-content:center" @click="editOpen = false">
              <UIcon name="i-lucide-x" style="width:16px;height:16px;color:#6B7280" />
            </button>
          </div>

          <!-- Body -->
          <div style="padding:24px;overflow-y:auto;max-height:70vh;display:flex;flex-direction:column;gap:24px">
            <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px">
              <div style="grid-column:1/-1">
                <label style="display:block;font-size:13px;font-weight:600;color:#374151;margin-bottom:8px">Kateqoriya <span style="color:#DC2626">*</span></label>
                <CategoryPicker v-model="editForm.categoryId" />
              </div>
              <div style="grid-column:1/-1">
                <label style="display:block;font-size:13px;font-weight:600;color:#374151;margin-bottom:6px">Təchizatçı</label>
                <select v-model="editForm.supplierId" style="width:100%;height:42px;padding:0 12px;border:1px solid #D1D5DB;border-radius:8px;color:#0A0A0A;background:#fff;font-family:inherit;outline:none">
                  <option value="">— Seçin —</option>
                  <option v-for="sup in suppliers" :key="sup.id" :value="String(sup.id)">
                    {{ sup.name }}{{ sup.taxId ? ` (VÖEN: ${sup.taxId})` : '' }}
                  </option>
                </select>
              </div>
              <div>
                <label style="display:block;font-size:13px;font-weight:600;color:#374151;margin-bottom:6px">Məbləğ <span style="color:#DC2626">*</span></label>
                <div style="position:relative">
                  <span style="position:absolute;left:12px;top:50%;transform:translateY(-50%);font-size:14px;color:#6B7280;font-family:var(--font-mono)">₼</span>
                  <input v-model="editForm.amount" type="number" step="0.01" min="0" style="width:100%;height:42px;padding:0 12px 0 28px;border:1px solid #D1D5DB;border-radius:8px;font-family:var(--font-mono);outline:none">
                </div>
              </div>
              <div>
                <label style="display:block;font-size:13px;font-weight:600;color:#374151;margin-bottom:6px">Valyuta <span style="color:#DC2626">*</span></label>
                <select v-model="editForm.currency" style="width:100%;height:42px;padding:0 12px;border:1px solid #D1D5DB;border-radius:8px;color:#0A0A0A;background:#fff;font-family:inherit;outline:none">
                  <option value="AZN">AZN</option>
                  <option value="USD">USD</option>
                  <option value="EUR">EUR</option>
                  <option value="TRY">TRY</option>
                </select>
              </div>
              <div>
                <label style="display:block;font-size:13px;font-weight:600;color:#374151;margin-bottom:6px">Tarix <span style="color:#DC2626">*</span></label>
                <input v-model="editForm.expenseDate" type="date" style="width:100%;height:42px;padding:0 12px;border:1px solid #D1D5DB;border-radius:8px;color:#0A0A0A;font-family:inherit;outline:none">
              </div>
              <div style="grid-column:1/-1">
                <label style="display:block;font-size:13px;font-weight:600;color:#374151;margin-bottom:6px">Qeyd</label>
                <textarea v-model="editForm.notes" rows="3" style="width:100%;padding:10px 12px;border:1px solid #D1D5DB;border-radius:8px;color:#0A0A0A;font-family:inherit;outline:none;resize:vertical" />
              </div>
            </div>

            <!-- Items editor -->
            <div>
              <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;flex-wrap:wrap;gap:10px">
                <div>
                  <span style="font-size:14px;font-weight:600;color:#0A0A0A">Qaimə maddələri</span>
                  <span style="font-size:12px;color:#6B7280;margin-left:8px">{{ editForm.items.length }} maddə</span>
                </div>
                <div style="display:flex;align-items:center;gap:12px">
                  <span style="font-family:var(--font-mono);font-size:13px;font-weight:600;color:#141F5E">{{ fmt(editItemsTotal) }}</span>
                  <button
                    style="display:inline-flex;align-items:center;gap:6px;height:36px;padding:0 14px;background:#3D5AF1;color:#fff;border:none;border-radius:7px;font-size:13px;font-weight:600;cursor:pointer;font-family:inherit"
                    @click="addEditItem"
                  >
                    <UIcon name="i-lucide-plus" style="width:14px;height:14px" />
                    Maddə əlavə et
                  </button>
                </div>
              </div>

              <div v-if="editForm.items.length" style="border:1px solid #E5E7EB;border-radius:10px;overflow:hidden">
                <div style="overflow-x:auto;-webkit-overflow-scrolling:touch">
                  <table style="width:100%;border-collapse:collapse;min-width:400px">
                    <thead>
                      <tr style="background:#F9FAFB">
                        <th style="text-align:left;padding:10px 14px;font-size:12px;font-weight:600;color:#6B7280">Təsvir</th>
                        <th style="text-align:right;padding:10px;font-size:12px;font-weight:600;color:#6B7280;white-space:nowrap;width:80px">Miqdar</th>
                        <th style="text-align:right;padding:10px;font-size:12px;font-weight:600;color:#6B7280;white-space:nowrap;width:110px">Vahid ₼</th>
                        <th style="text-align:right;padding:10px 14px 10px 10px;font-size:12px;font-weight:600;color:#6B7280;white-space:nowrap;width:100px">Cəmi ₼</th>
                        <th style="width:40px" />
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="(item, idx) in editForm.items" :key="item._key" :style="idx % 2 === 0 ? '' : 'background:#F9FAFB'">
                        <td style="padding:8px 14px">
                          <input v-model="item.description" type="text" placeholder="Məhsul / xidmət adı" style="width:100%;height:36px;padding:0 10px;border:1px solid #E5E7EB;border-radius:6px;font-family:inherit;outline:none">
                        </td>
                        <td style="padding:8px 10px">
                          <input v-model.number="item.quantity" type="number" min="1" style="width:100%;height:36px;padding:0 8px;border:1px solid #E5E7EB;border-radius:6px;font-family:var(--font-mono);text-align:right;outline:none">
                        </td>
                        <td style="padding:8px 10px">
                          <input v-model.number="item.unitPrice" type="number" min="0" step="0.01" style="width:100%;height:36px;padding:0 8px;border:1px solid #E5E7EB;border-radius:6px;font-family:var(--font-mono);text-align:right;outline:none">
                        </td>
                        <td style="padding:8px 14px 8px 10px;text-align:right;font-family:var(--font-mono);font-size:13px;font-weight:600;color:#141F5E;white-space:nowrap">
                          {{ fmt((item.quantity || 0) * (item.unitPrice || 0)) }}
                        </td>
                        <td style="padding:8px 10px 8px 4px;text-align:center">
                          <button style="width:28px;height:28px;border-radius:6px;border:1px solid #FCA5A5;background:#FEF2F2;cursor:pointer;display:flex;align-items:center;justify-content:center" @click="removeEditItem(idx)">
                            <UIcon name="i-lucide-trash-2" style="width:13px;height:13px;color:#DC2626" />
                          </button>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
              <div v-else style="border:2px dashed #E5E7EB;border-radius:10px;padding:28px;text-align:center;color:#9CA3AF;font-size:13px">
                Maddə yoxdur — yuxarıdakı düyməni basın
              </div>
            </div>

            <div v-if="editError" style="padding:12px 14px;background:#FEF2F2;border:1px solid #FCA5A5;border-radius:8px;font-size:13px;color:#DC2626">
              {{ editError }}
            </div>
          </div>

          <!-- Footer -->
          <div style="display:flex;align-items:center;justify-content:flex-end;gap:10px;padding:16px 24px;border-top:1px solid #F3F4F6;background:#FAFBFC;flex:none">
            <button style="height:42px;padding:0 20px;background:#fff;border:1px solid #D1D5DB;border-radius:8px;font-size:14px;font-weight:600;color:#374151;cursor:pointer;font-family:inherit" @click="editOpen = false">
              Ləğv et
            </button>
            <button
              :disabled="editLoading"
              style="display:inline-flex;align-items:center;gap:7px;height:42px;padding:0 22px;background:#3D5AF1;color:#fff;border:none;border-radius:8px;font-size:14px;font-weight:600;cursor:pointer;font-family:inherit"
              @click="saveEdit"
            >
              <UIcon v-if="editLoading" name="i-lucide-loader-2" style="width:15px;height:15px;animation:spin 1s linear infinite" />
              Yadda saxla
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
