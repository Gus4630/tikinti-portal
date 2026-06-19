<script setup lang="ts">
const props = defineProps<{ buildings: Record<string, unknown>[] }>()
const emit = defineEmits<{ close: []; uploaded: [] }>()

const config = useRuntimeConfig()
const auth = useAuthStore()

const form = reactive({ buildingId: '', file: null as File | null })
const dragOver = ref(false)
const uploading = ref(false)
const error = ref('')
const fileInput = ref<HTMLInputElement>()

function onDrop(e: DragEvent) {
  dragOver.value = false
  const f = e.dataTransfer?.files?.[0]
  if (f) form.file = f
}

function onFileChange(e: Event) {
  const t = e.target as HTMLInputElement
  if (t.files?.[0]) form.file = t.files[0]
}

async function submit() {
  if (!form.file || !form.buildingId) {
    error.value = 'Layihə seçin və fayl əlavə edin.'
    return
  }
  error.value = ''
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', form.file)
    fd.append('buildingId', form.buildingId)
    await $fetch('/api/v1/invoices/upload', {
      baseURL: config.public.apiBase,
      method: 'POST',
      headers: { Authorization: `Bearer ${auth.token}` },
      body: fd,
    })
    emit('uploaded')
    emit('close')
  } catch (e: unknown) {
    error.value = 'Yükləmə uğursuz oldu. Yenidən cəhd edin.'
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <div style="position:fixed;inset:0;z-index:50;display:flex;align-items:center;justify-content:center;padding:16px">
    <!-- Backdrop -->
    <div style="position:absolute;inset:0;background:rgba(0,0,0,0.4)" @click="emit('close')" />

    <!-- Modal -->
    <div class="tk-card" style="position:relative;width:100%;max-width:480px;padding:28px;z-index:1">
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:22px">
        <h2 style="font-size:18px;font-weight:700;margin:0">Qaimə yüklə</h2>
        <button style="background:none;border:none;cursor:pointer;color:#9CA3AF;padding:4px" @click="emit('close')">
          <UIcon name="i-lucide-x" style="width:20px;height:20px" />
        </button>
      </div>

      <div v-if="error" style="background:#FEF2F2;border:1px solid #FCA5A5;border-radius:8px;padding:10px 14px;font-size:13.5px;color:#DC2626;margin-bottom:16px">
        {{ error }}
      </div>

      <!-- Building select -->
      <div style="margin-bottom:16px">
        <label style="display:block;font-size:13px;font-weight:500;color:#374151;margin-bottom:7px">Layihə</label>
        <select v-model="form.buildingId" style="width:100%;height:40px;border:1px solid #D1D5DB;border-radius:8px;padding:0 12px;font-size:14px;background:#fff;color:#0A0A0A;font-family:inherit">
          <option value="">Layihə seçin</option>
          <option v-for="b in buildings" :key="String(b.id)" :value="b.id">{{ b.name }}</option>
        </select>
      </div>

      <!-- Dropzone -->
      <div
        :style="`border:2px dashed ${dragOver ? '#3D5AF1' : '#D1D5DB'};border-radius:10px;padding:32px;text-align:center;background:${dragOver ? '#F0F4FF' : '#FAFBFC'};cursor:pointer;transition:all .15s`"
        @dragover.prevent="dragOver = true"
        @dragleave="dragOver = false"
        @drop.prevent="onDrop"
        @click="fileInput?.click()"
      >
        <input ref="fileInput" type="file" accept="image/*,application/pdf" style="display:none" @change="onFileChange">
        <UIcon name="i-lucide-upload-cloud" style="width:36px;height:36px;color:#9CA3AF;margin-bottom:10px" />
        <div v-if="form.file" style="font-size:14px;font-weight:500;color:#141F5E">{{ form.file.name }}</div>
        <div v-else>
          <div style="font-size:14px;font-weight:500;color:#374151;margin-bottom:4px">Faylı bura sürükləyin</div>
          <div style="font-size:12.5px;color:#9CA3AF">JPG, PNG, PDF · maks. 20 MB</div>
        </div>
      </div>

      <div style="display:flex;gap:10px;margin-top:22px">
        <button
          style="flex:1;height:44px;background:#fff;color:#374151;border:1px solid #D1D5DB;border-radius:8px;font-size:14px;font-weight:600;cursor:pointer;font-family:inherit"
          @click="emit('close')"
        >Ləğv et</button>
        <button
          :disabled="uploading"
          style="flex:1;height:44px;background:#3D5AF1;color:#fff;border:none;border-radius:8px;font-size:14px;font-weight:600;cursor:pointer;font-family:inherit;display:flex;align-items:center;justify-content:center;gap:8px"
          @click="submit"
        >
          <UIcon v-if="uploading" name="i-lucide-loader-2" style="width:16px;height:16px;animation:spin 1s linear infinite" />
          {{ uploading ? 'Yüklənir...' : 'Yüklə' }}
        </button>
      </div>
    </div>
  </div>
</template>
