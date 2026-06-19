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
  DISPUTED:       { label: 'Mübahisəli',  color: '#DC2626', bg: '#FEF2F2' },
  REJECTED:       { label: 'Rədd edildi', color: '#991B1B', bg: '#FEE2E2' },
}

const headers = computed(() => ({ Authorization: `Bearer ${auth.token}` }))

const { data: expense, refresh } = await useAsyncData('expense-detail', () =>
  $fetch<Record<string, unknown>>(`/api/v1/expenses/${route.params.id}`, {
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

const actionLoading = ref('')

const ACTION_ENDPOINT: Record<string, string> = {
  APPROVED: 'approve',
  DISPUTED: 'dispute',
  REJECTED: 'reject',
}

async function doAction(action: 'APPROVED' | 'DISPUTED' | 'REJECTED') {
  actionLoading.value = action
  try {
    await $fetch(`/api/v1/expenses/${route.params.id}/${ACTION_ENDPOINT[action]}`, {
      baseURL: config.public.apiBase,
      method: 'POST',
      headers: headers.value,
    })
    await refresh()
  } finally {
    actionLoading.value = ''
  }
}

function fmt(n: number) {
  return '₼' + n.toLocaleString('az-AZ', { minimumFractionDigits: 2 })
}
</script>

<template>
  <div class="animate-page">
    <!-- Breadcrumb -->
    <div style="display:flex;align-items:center;gap:6px;font-size:13px;color:#6B7280;margin-bottom:14px">
      <NuxtLink to="/expenses" style="cursor:pointer;color:#6B7280;text-decoration:none;hover:color:#3D5AF1">Xərclər</NuxtLink>
      <UIcon name="i-lucide-chevron-right" style="width:14px;height:14px;color:#9CA3AF" />
      <span style="font-family:var(--font-mono);color:#374151;font-weight:500">{{ expense?.id?.toString().slice(0, 8) }}…</span>
    </div>

    <!-- Page title -->
    <div style="display:flex;align-items:flex-end;justify-content:space-between;margin-bottom:24px">
      <div>
        <h1 style="font-size:26px;font-weight:700;letter-spacing:-0.02em;margin:0 0 4px">Xərc təfərrüatı</h1>
        <p style="font-size:14px;color:#6B7280;margin:0">{{ expense?.buildingName }}</p>
      </div>
      <span
        :style="`display:inline-flex;align-items:center;gap:6px;padding:5px 14px;border-radius:9999px;font-size:13px;font-weight:600;color:${statusMeta.color};background:${statusMeta.bg}`"
      >
        <span :style="`width:7px;height:7px;border-radius:9999px;background:${statusMeta.color}`" />
        {{ statusMeta.label }}
      </span>
    </div>

    <div style="display:grid;grid-template-columns:1.05fr 1fr;gap:20px">
      <!-- Left: Invoice preview -->
      <div class="tk-card" style="padding:20px">
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:14px">
          <h3 style="font-size:15px;font-weight:600;margin:0">Qaimə-faktura</h3>
          <span style="font-size:12.5px;color:#6B7280">Skan</span>
        </div>
        <div style="background:#E5E7EB;border-radius:8px;padding:22px;display:flex;justify-content:center">
          <div style="position:relative;background:#fff;width:100%;max-width:380px;padding:26px 24px;border-radius:3px;box-shadow:0 6px 22px rgba(0,0,0,0.14);font-size:11px;color:#374151">
            <div style="display:flex;justify-content:space-between;align-items:flex-start;border-bottom:2px solid #0D1440;padding-bottom:10px;margin-bottom:12px">
              <div>
                <div style="font-size:13px;font-weight:700;color:#0D1440">{{ expense?.supplierName ?? '—' }}</div>
                <div style="font-family:var(--font-mono);font-size:10px;color:#6B7280;margin-top:3px">VÖEN: {{ expense?.supplierTaxId ?? '—' }}</div>
              </div>
              <div style="text-align:right">
                <div style="font-size:11px;font-weight:700;letter-spacing:0.05em;color:#0D1440">QAİMƏ-FAKTURA</div>
                <div style="font-family:var(--font-mono);font-size:10px;color:#6B7280;margin-top:3px">#{{ String(expense?.id ?? '').slice(0,8) }}</div>
              </div>
            </div>
            <div style="display:flex;justify-content:space-between;font-size:10px;color:#6B7280;margin-bottom:14px">
              <span>Tarix: {{ expense?.expenseDate ?? '—' }}</span>
              <span>Layihə: {{ expense?.buildingName ?? '—' }}</span>
            </div>
            <div style="display:flex;flex-direction:column;gap:5px;align-items:flex-end;font-size:10px">
              <div style="display:flex;gap:24px"><span style="color:#6B7280">Məbləğ:</span><span style="font-family:var(--font-mono);width:80px;text-align:right">{{ fmt(Number(expense?.amount) || 0) }}</span></div>
              <div style="display:flex;gap:24px;font-weight:700;color:#0D1440;border-top:1px solid #E5E7EB;padding-top:5px;margin-top:2px">
                <span>Yekun:</span>
                <span style="font-family:var(--font-mono);width:80px;text-align:right">{{ fmt(Number(expense?.amountBaseCurrency) || 0) }}</span>
              </div>
            </div>
            <div
              v-if="expense?.status === 'APPROVED'"
              style="position:absolute;top:80px;right:14px;transform:rotate(-14deg);border:2.5px solid #16A34A;color:#16A34A;font-weight:700;font-size:13px;letter-spacing:0.08em;padding:4px 12px;border-radius:5px;opacity:0.85"
            >
              TƏSDİQLƏNDİ
            </div>
          </div>
        </div>

        <!-- Notes -->
        <div v-if="expense?.notes" style="margin-top:16px;padding:12px 14px;background:#F9FAFB;border:1px solid #F3F4F6;border-radius:8px">
          <div style="font-size:12px;font-weight:600;color:#6B7280;text-transform:uppercase;letter-spacing:0.03em;margin-bottom:6px">Qeyd (OCR)</div>
          <p style="font-size:13px;color:#374151;margin:0;line-height:1.5">{{ expense.notes }}</p>
        </div>
      </div>

      <!-- Right: Details + actions + audit -->
      <div style="display:flex;flex-direction:column;gap:20px">
        <!-- Extracted data -->
        <div class="tk-card" style="overflow:hidden">
          <div style="display:flex;align-items:center;justify-content:space-between;padding:18px 20px;border-bottom:1px solid #F3F4F6">
            <div>
              <h3 style="font-size:15px;font-weight:600;margin:0 0 4px">Çıxarılan məlumatlar</h3>
              <span style="font-size:12px;color:#6B7280">OCR ilə avtomatik dolduruldu</span>
            </div>
            <span
              :style="`display:inline-flex;align-items:center;gap:6px;padding:4px 12px;border-radius:9999px;font-size:12px;font-weight:600;color:${statusMeta.color};background:${statusMeta.bg}`"
            >
              {{ statusMeta.label }}
            </span>
          </div>
          <div style="padding:6px 20px">
            <div v-for="row in [
              { label: 'Təchizatçı', val: expense?.supplierName, mono: false },
              { label: 'VÖEN', val: expense?.supplierTaxId, mono: true },
              { label: 'Layihə', val: expense?.buildingName, mono: false },
              { label: 'Kateqoriya', val: expense?.categoryItemName, mono: false },
              { label: 'Tarix', val: expense?.expenseDate, mono: true },
              { label: 'Valyuta', val: expense?.currency, mono: true },
              { label: 'Məbləğ', val: expense?.amount ? fmt(Number(expense.amount)) : null, mono: true },
              { label: 'AZN məbləği', val: expense?.amountBaseCurrency ? fmt(Number(expense.amountBaseCurrency)) : null, mono: true },
            ]" :key="row.label" style="display:flex;justify-content:space-between;padding:11px 0;border-bottom:1px solid #F3F4F6">
              <span style="font-size:13px;color:#6B7280">{{ row.label }}</span>
              <span :style="row.mono ? 'font-family:var(--font-mono);font-size:13px' : 'font-size:13.5px;font-weight:500'">{{ row.val ?? '—' }}</span>
            </div>
            <div style="display:flex;justify-content:space-between;padding:13px 0 14px">
              <span style="font-size:14px;font-weight:600">Ümumi məbləğ</span>
              <span style="font-family:var(--font-mono);font-size:17px;font-weight:600;color:#141F5E">{{ fmt(Number(expense?.amountBaseCurrency) || 0) }}</span>
            </div>
          </div>
          <!-- Action buttons -->
          <div style="display:flex;gap:10px;padding:16px 20px;border-top:1px solid #F3F4F6;background:#FAFBFC">
            <button
              :disabled="actionLoading !== '' || expense?.status === 'APPROVED'"
              style="display:inline-flex;align-items:center;gap:7px;height:40px;padding:0 18px;background:#16A34A;color:#fff;border:none;border-radius:8px;font-size:14px;font-weight:600;cursor:pointer;font-family:inherit"
              @click="doAction('APPROVED')"
            >Təsdiqlə</button>
            <button
              :disabled="actionLoading !== '' || expense?.status === 'DISPUTED'"
              style="height:40px;padding:0 18px;background:#fff;color:#D97706;border:1px solid #FCD34D;border-radius:8px;font-size:14px;font-weight:600;cursor:pointer;font-family:inherit"
              @click="doAction('DISPUTED')"
            >Mübahisə et</button>
            <button
              :disabled="actionLoading !== '' || expense?.status === 'REJECTED'"
              style="height:40px;padding:0 18px;background:#fff;color:#DC2626;border:1px solid #FCA5A5;border-radius:8px;font-size:14px;font-weight:600;cursor:pointer;margin-left:auto;font-family:inherit"
              @click="doAction('REJECTED')"
            >Rədd et</button>
          </div>
        </div>

        <!-- Audit timeline -->
        <div class="tk-card" style="padding:20px">
          <h3 style="font-size:15px;font-weight:600;margin:0 0 18px">Audit izi</h3>
          <div v-if="Array.isArray(history) && history.length" style="display:flex;flex-direction:column">
            <div v-for="(ev, i) in (history as Record<string,unknown>[])" :key="i" style="display:flex;gap:14px;padding-bottom:18px;position:relative">
              <div style="display:flex;flex-direction:column;align-items:center;flex:none">
                <span style="width:10px;height:10px;border-radius:9999px;background:#3D5AF1;flex:none" />
                <span v-if="i < (history as unknown[]).length - 1" style="width:2px;flex:1;background:#F3F4F6;margin-top:3px" />
              </div>
              <div style="flex:1;margin-top:-2px">
                <div style="font-size:13.5px;font-weight:500;color:#0A0A0A">{{ ev.status ?? ev.revisionType ?? 'Dəyişiklik' }}</div>
                <div style="font-size:12px;color:#9CA3AF;margin-top:2px">{{ ev.revisionDate }}</div>
              </div>
            </div>
          </div>
          <div v-else style="font-size:13px;color:#9CA3AF;text-align:center;padding:16px 0">Audit məlumatı yoxdur</div>
        </div>
      </div>
    </div>
  </div>
</template>
