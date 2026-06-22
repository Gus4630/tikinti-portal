<script setup lang="ts">
const props = defineProps<{ buildings: Record<string, unknown>[] }>()
const emit = defineEmits<{ close: []; uploaded: [] }>()

const config = useRuntimeConfig()
const auth = useAuthStore()
const context = useContextStore()
const { resolveError } = useApiError()

type OcrState = 'idle' | 'processing' | 'done'
const form = reactive({ buildingId: context.activeBuildingId || '', categoryId: '', amount: '', file: null as File | null })
const dragOver = ref(false)
const uploading = ref(false)
const ocrState = ref<OcrState>('idle')

interface ErrorState {
  message: string
  duplicateExpenseId: string | null
}
const errorState = ref<ErrorState | null>(null)
const fileInput = ref<HTMLInputElement>()

function setFile(f: File) {
  form.file = f
  ocrState.value = 'processing'
  setTimeout(() => { ocrState.value = 'done' }, 1500)
}

function onDrop(e: DragEvent) {
  dragOver.value = false
  const f = e.dataTransfer?.files?.[0]
  if (f) setFile(f)
}

function onFileChange(e: Event) {
  const t = e.target as HTMLInputElement
  if (t.files?.[0]) setFile(t.files[0])
}

function formatBytes(bytes: number) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

async function submit() {
  if (!form.file || !form.buildingId) {
    errorState.value = { message: 'Layihə seçin və fayl əlavə edin.', duplicateExpenseId: null }
    return
  }
  errorState.value = null
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', form.file)
    fd.append('buildingId', form.buildingId)
    if (form.categoryId) fd.append('categoryId', form.categoryId)
    if (form.amount) fd.append('amount', form.amount)
    await $fetch('/api/v1/invoices/upload', {
      baseURL: config.public.apiBase,
      method: 'POST',
      headers: { Authorization: `Bearer ${auth.token}` },
      body: fd,
    })
    emit('uploaded')
    emit('close')
  } catch (err: unknown) {
    const { message, apiError } = resolveError(err)
    const duplicateExpenseId = apiError.errorMessage === 'duplicate_invoice'
      ? String(apiError.params?.expenseId ?? '')
      : null
    errorState.value = { message, duplicateExpenseId }
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <div style="position:fixed;inset:0;z-index:150;display:flex;align-items:center;justify-content:center;padding:16px">
    <div style="position:absolute;inset:0;background:rgba(13,20,64,0.45)" @click="emit('close')" />

    <div style="position:relative;background:#fff;border-radius:12px;width:100%;max-width:580px;max-height:92vh;overflow-y:auto;box-shadow:0 20px 60px rgba(0,0,0,0.2);z-index:1;display:flex;flex-direction:column">
      <!-- Header -->
      <div style="display:flex;align-items:center;justify-content:space-between;padding:20px 24px;border-bottom:1px solid #F3F4F6;position:sticky;top:0;background:#fff;z-index:1;flex:none">
        <h2 style="font-size:17px;font-weight:700;margin:0">Qaimə-faktura yüklə</h2>
        <button style="background:none;border:none;cursor:pointer;color:#9CA3AF;padding:4px" @click="emit('close')">
          <UIcon name="i-lucide-x" style="width:20px;height:20px" />
        </button>
      </div>

      <div style="padding:24px;display:flex;flex-direction:column;gap:18px;flex:1">
        <!-- Error banner -->
        <div v-if="errorState" style="background:#FEF2F2;border:1px solid #FCA5A5;border-radius:8px;padding:12px 14px;font-size:13.5px;color:#DC2626;display:flex;flex-direction:column;gap:8px">
          <span>{{ errorState.message }}</span>
          <NuxtLink
            v-if="errorState.duplicateExpenseId"
            :to="`/expenses/${errorState.duplicateExpenseId}`"
            style="display:inline-flex;align-items:center;gap:5px;font-size:13px;font-weight:600;color:#3D5AF1;text-decoration:none"
            @click="emit('close')"
          >
            <UIcon name="i-lucide-arrow-right" style="width:13px;height:13px" />
            Mövcud xərcə bax
          </NuxtLink>
        </div>

        <!-- Dropzone -->
        <div
          :style="`border:2px dashed ${dragOver ? '#3D5AF1' : '#C7D2FE'};border-radius:10px;padding:${form.file ? '16px' : '36px'};text-align:center;background:${dragOver ? '#F0F4FF' : '#FAFBFC'};cursor:pointer;transition:all .15s`"
          @dragover.prevent="dragOver = true"
          @dragleave="dragOver = false"
          @drop.prevent="onDrop"
          @click="!form.file && fileInput?.click()"
        >
          <input ref="fileInput" type="file" accept="image/*,application/pdf" style="display:none" @change="onFileChange">

          <div v-if="!form.file">
            <div style="width:48px;height:48px;border-radius:9999px;background:#E0E9FF;display:flex;align-items:center;justify-content:center;margin:0 auto 12px">
              <UIcon name="i-lucide-upload-cloud" style="width:24px;height:24px;color:#3D5AF1" />
            </div>
            <div style="font-size:14px;font-weight:600;color:#374151;margin-bottom:4px">Qaimə-faktura yükləyin</div>
            <div style="font-size:12.5px;color:#9CA3AF">Faylı bura sürükləyin və ya klikləyin · PDF, JPG, PNG</div>
          </div>

          <div v-else style="display:flex;align-items:center;gap:14px;text-align:left">
            <div style="width:42px;height:42px;border-radius:8px;background:#E0E9FF;display:flex;align-items:center;justify-content:center;flex:none">
              <UIcon name="i-lucide-file-text" style="width:20px;height:20px;color:#3D5AF1" />
            </div>
            <div style="flex:1;min-width:0">
              <div style="font-size:13.5px;font-weight:500;color:#141F5E;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">{{ form.file.name }}</div>
              <div style="font-size:12px;color:#9CA3AF;margin-top:2px">{{ formatBytes(form.file.size) }}</div>
            </div>
            <div v-if="ocrState === 'processing'" style="display:flex;align-items:center;gap:6px;font-size:12.5px;color:#3B82F6;flex:none">
              <UIcon name="i-lucide-loader-2" style="width:14px;height:14px;animation:spin 1s linear infinite" />
              Emal olunur...
            </div>
            <div v-else-if="ocrState === 'done'" style="display:inline-flex;align-items:center;gap:5px;font-size:12px;font-weight:600;color:#16A34A;background:#F0FDF4;border:1px solid #86EFAC;border-radius:9999px;padding:2px 9px;flex:none">
              <UIcon name="i-lucide-check" style="width:12px;height:12px" />
              Emal olundu
            </div>
            <button style="background:none;border:none;cursor:pointer;color:#9CA3AF;padding:2px;flex:none" @click.stop="fileInput?.click()">
              <UIcon name="i-lucide-refresh-cw" style="width:15px;height:15px" />
            </button>
          </div>
        </div>

        <!-- Project select -->
        <div>
          <label style="display:block;font-size:13px;font-weight:600;color:#374151;margin-bottom:6px">
            Layihə <span style="color:#EF4444">*</span>
          </label>
          <select
            v-model="form.buildingId"
            style="width:100%;height:40px;border:1px solid #D1D5DB;border-radius:8px;padding:0 12px;font-size:13.5px;background:#fff;color:#0A0A0A;font-family:inherit;outline:none"
          >
            <option value="">Layihə seçin</option>
            <option v-for="b in buildings" :key="String(b.id)" :value="b.id">{{ b.name }}</option>
          </select>
        </div>

        <!-- Amount -->
        <div>
          <label style="display:block;font-size:13px;font-weight:600;color:#374151;margin-bottom:6px">
            Məbləğ (ƏDV-siz)
            <span v-if="ocrState === 'done'" style="font-size:12px;color:#9CA3AF;font-weight:400"> — OCR ilə dolur</span>
          </label>
          <div style="position:relative">
            <span style="position:absolute;left:12px;top:50%;transform:translateY(-50%);font-size:14px;font-weight:500;color:#6B7280;font-family:var(--font-mono)">₼</span>
            <input
              v-model="form.amount"
              type="number"
              step="0.01"
              min="0"
              placeholder="0.00"
              style="width:100%;height:40px;border:1px solid #D1D5DB;border-radius:8px;padding:0 12px 0 28px;font-size:14px;font-family:var(--font-mono);outline:none;box-sizing:border-box"
            >
          </div>
        </div>

        <!-- Category picker -->
        <div>
          <label style="display:block;font-size:13px;font-weight:600;color:#374151;margin-bottom:8px">
            Kateqoriya
            <span style="font-size:12px;color:#9CA3AF;font-weight:400">(istəyə bağlı)</span>
          </label>
          <CategoryPicker v-model="form.categoryId" />
        </div>
      </div>

      <!-- Footer -->
      <div style="display:flex;gap:10px;padding:16px 24px;border-top:1px solid #F3F4F6;background:#FAFBFC;position:sticky;bottom:0;flex:none">
        <button
          style="flex:1;height:44px;background:#fff;color:#374151;border:1px solid #D1D5DB;border-radius:8px;font-size:14px;font-weight:600;cursor:pointer;font-family:inherit"
          @click="emit('close')"
        >Ləğv et</button>
        <button
          :disabled="uploading || !form.file || !form.buildingId"
          style="flex:1;height:44px;background:#3D5AF1;color:#fff;border:none;border-radius:8px;font-size:14px;font-weight:600;cursor:pointer;font-family:inherit;display:flex;align-items:center;justify-content:center;gap:8px;transition:opacity .15s"
          :style="{ opacity: (!form.file || !form.buildingId) ? '0.5' : '1' }"
          @click="submit"
        >
          <UIcon v-if="uploading" name="i-lucide-loader-2" style="width:16px;height:16px;animation:spin 1s linear infinite" />
          {{ uploading ? 'Yüklənir...' : 'Yüklə və göndər' }}
        </button>
      </div>
    </div>
  </div>
</template>
