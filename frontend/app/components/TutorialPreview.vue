<script setup lang="ts">
defineProps<{ type: string }>()
</script>

<template>
  <!-- ── Groups ────────────────────────────────────────────────── -->
  <div v-if="type === 'groups'" class="tp-preview">
    <div style="padding:12px 14px;border-bottom:1px solid #F3F4F6;display:flex;align-items:center;gap:10px">
      <div style="width:36px;height:36px;border-radius:9px;background:#EEF2FF;display:flex;align-items:center;justify-content:center;flex:none">
        <UIcon name="i-lucide-users" style="width:18px;height:18px;color:#3D5AF1" />
      </div>
      <div style="flex:1;min-width:0">
        <div style="font-size:13.5px;font-weight:600;color:#0A0A0A">İnşaat Komandası</div>
        <div style="font-size:12px;color:#6B7280;margin-top:1px">3 üzv · qrup</div>
      </div>
      <span class="badge" style="background:#EEF2FF;color:#3D5AF1;border:none">OWNER</span>
    </div>
    <div style="padding:10px 14px;display:flex;align-items:center;gap:6px">
      <div style="width:26px;height:26px;border-radius:9999px;background:#F59E0B;color:#0D1440;display:flex;align-items:center;justify-content:center;font-size:10px;font-weight:700;flex:none">HM</div>
      <div style="width:26px;height:26px;border-radius:9999px;background:#3D5AF1;color:#fff;display:flex;align-items:center;justify-content:center;font-size:10px;font-weight:700;flex:none">AK</div>
      <div style="width:26px;height:26px;border-radius:9999px;background:#6B7280;color:#fff;display:flex;align-items:center;justify-content:center;font-size:10px;font-weight:700;flex:none">NK</div>
      <span style="font-size:11.5px;color:#9CA3AF;margin-left:4px">3 üzv</span>
      <button style="margin-left:auto;display:inline-flex;align-items:center;gap:4px;height:26px;padding:0 10px;background:#EEF2FF;color:#3D5AF1;border:none;border-radius:6px;font-size:11.5px;font-weight:600;cursor:default">
        + Üzv əlavə et
      </button>
    </div>
  </div>

  <!-- ── Buildings ─────────────────────────────────────────────── -->
  <div v-else-if="type === 'buildings'" class="tp-preview">
    <div style="padding:12px 14px;border-bottom:1px solid #F3F4F6;display:flex;align-items:flex-start;justify-content:space-between;gap:8px">
      <div>
        <div style="font-size:14px;font-weight:700;color:#0A0A0A;margin-bottom:2px">Bakı Bağ Evi</div>
        <div style="font-size:12px;color:#6B7280">Novxanı, Bakı · 320 m²</div>
      </div>
      <span class="badge badge-PENDING_REVIEW" style="flex:none">Davam edir</span>
    </div>
    <div style="padding:12px 14px">
      <div style="display:flex;justify-content:space-between;align-items:center;font-size:12px;margin-bottom:6px">
        <span style="color:#6B7280">Büdcə istifadəsi</span>
        <span style="font-weight:600;font-family:monospace;color:#0D1440">₼67,500 / ₼150,000</span>
      </div>
      <div class="progress-track">
        <div class="progress-bar" style="width:45%;background:#F59E0B" />
      </div>
      <div style="font-size:11.5px;color:#D97706;margin-top:5px;font-weight:500">45% · 80%-ə çatanda Telegram bildirişi</div>
    </div>
  </div>

  <!-- ── Categories ────────────────────────────────────────────── -->
  <div v-else-if="type === 'categories'" class="tp-preview" style="font-size:12.5px">
    <div style="padding:10px 14px;border-bottom:1px solid #F3F4F6">
      <div style="font-weight:600;color:#374151;margin-bottom:6px;display:flex;align-items:center;gap:5px">
        <UIcon name="i-lucide-chevron-down" style="width:13px;height:13px;color:#9CA3AF;flex:none" />
        01 · Torpaq və tikinti işləri
      </div>
      <div style="padding-left:18px;display:flex;flex-direction:column;gap:5px">
        <div style="color:#6B7280">01.01 · Torpaq qazma</div>
        <div style="color:#6B7280">01.02 · Bünövrə işləri</div>
      </div>
    </div>
    <div style="padding:10px 14px">
      <div style="font-weight:600;color:#374151;margin-bottom:6px;display:flex;align-items:center;gap:5px">
        <UIcon name="i-lucide-chevron-down" style="width:13px;height:13px;color:#9CA3AF;flex:none" />
        02 · Beton işlər
      </div>
      <div style="padding-left:18px;display:flex;flex-direction:column;gap:5px">
        <div style="color:#6B7280">02.01 · Monolit beton</div>
        <div style="color:#6B7280">02.02 · Hazır beton</div>
      </div>
      <div style="padding-left:18px;color:#3D5AF1;font-weight:600;margin-top:6px">+ 132 kateqoriya daha...</div>
    </div>
  </div>

  <!-- ── Manual expense ─────────────────────────────────────────── -->
  <div v-else-if="type === 'manual-expense'" class="tp-preview">
    <div class="m-row" style="pointer-events:none">
      <span class="m-row-left">
        <span class="m-row-title">Beton MMC</span>
        <span class="m-row-sub">02.01 · Monolit beton · 15.03.2025</span>
      </span>
      <span class="m-row-right">
        <span class="m-row-amount">₼12,500</span>
        <span class="badge badge-APPROVED" style="font-size:10.5px">Təsdiqləndi</span>
      </span>
    </div>
    <div class="m-row" style="pointer-events:none">
      <span class="m-row-left">
        <span class="m-row-title">Metal Kons. ASC</span>
        <span class="m-row-sub">03.02 · Metal konstruksiya · 12.03.2025</span>
      </span>
      <span class="m-row-right">
        <span class="m-row-amount">₼8,750</span>
        <span class="badge badge-PENDING_REVIEW" style="font-size:10.5px">Yoxlanılır</span>
      </span>
    </div>
    <div style="padding:10px 14px;display:flex;justify-content:flex-end">
      <button style="display:inline-flex;align-items:center;gap:5px;height:32px;padding:0 12px;background:#3D5AF1;color:#fff;border:none;border-radius:7px;font-size:12.5px;font-weight:600;cursor:default">
        <UIcon name="i-lucide-plus" style="width:13px;height:13px" />
        Yeni xərc
      </button>
    </div>
  </div>

  <!-- ── Invoice upload ────────────────────────────────────────── -->
  <div v-else-if="type === 'upload'" class="tp-preview">
    <div style="padding:14px;text-align:center">
      <div style="border:2px dashed #93C5FD;border-radius:8px;padding:14px;margin-bottom:12px;background:#EFF6FF">
        <UIcon name="i-lucide-file-text" style="width:24px;height:24px;color:#3D5AF1;margin-bottom:6px" />
        <div style="font-size:12.5px;font-weight:600;color:#374151">qaimə_mart_2025.pdf</div>
        <div style="font-size:11px;color:#9CA3AF;margin-top:2px">1.2 MB · PDF</div>
      </div>
      <div class="progress-track" style="margin-bottom:7px">
        <div class="progress-bar" style="width:65%;background:#3D5AF1" />
      </div>
      <div style="font-size:12px;color:#3D5AF1;font-weight:600">Süni İntellekt oxuyur... 65%</div>
    </div>
  </div>

  <!-- ── AI review ─────────────────────────────────────────────── -->
  <div v-else-if="type === 'ai-review'" class="tp-preview">
    <div style="padding:8px 12px;background:#EEF2FF;border-bottom:1px solid #E5E7EB;display:flex;align-items:center;gap:6px">
      <UIcon name="i-lucide-bot" style="width:13px;height:13px;color:#3D5AF1;flex:none" />
      <span style="font-size:11.5px;font-weight:600;color:#3D5AF1">Süni İntellekt məlumat çıxardı</span>
    </div>
    <div class="m-row" style="pointer-events:none">
      <span style="font-size:12px;color:#6B7280">Məbləğ</span>
      <span style="font-family:monospace;font-size:13px;font-weight:700;color:#0D1440">₼ 8,750 <span style="color:#16A34A">✓</span></span>
    </div>
    <div class="m-row" style="pointer-events:none">
      <span style="font-size:12px;color:#6B7280">Tarix</span>
      <span style="font-size:13px;font-weight:500">18.03.2025 <span style="color:#16A34A">✓</span></span>
    </div>
    <div class="m-row" style="border-bottom:none;pointer-events:none">
      <span style="font-size:12px;color:#6B7280">Kateqoriya</span>
      <span style="font-size:12px;font-weight:500">Metal kons. <span style="color:#16A34A">✓</span></span>
    </div>
    <div style="padding:8px 12px;background:#FFFBEB;border-top:1px solid #FDE68A;display:flex;align-items:center;gap:8px">
      <span class="badge badge-PENDING_REVIEW">Yoxlanılır</span>
      <span style="font-size:11.5px;color:#D97706">"Təsdiqlə" düyməsini basın</span>
    </div>
  </div>

  <!-- ── Expense detail / approve ──────────────────────────────── -->
  <div v-else-if="type === 'expense-detail'" class="tp-preview" style="overflow:hidden">
    <div style="display:grid;grid-template-columns:1fr 1fr">
      <div style="background:#1F2937;padding:16px 12px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:6px">
        <UIcon name="i-lucide-file-text" style="width:22px;height:22px;color:#4B5563" />
        <div style="font-size:10.5px;color:#6B7280;text-align:center">Orijinal qaimə</div>
      </div>
      <div style="padding:10px 12px;display:flex;flex-direction:column;gap:0">
        <div class="m-row" style="padding:5px 0;pointer-events:none">
          <span style="font-size:11px;color:#6B7280">Məbləğ</span>
          <span style="font-family:monospace;font-size:12.5px;font-weight:700;color:#0D1440">₼8,750</span>
        </div>
        <div class="m-row" style="padding:5px 0;pointer-events:none">
          <span style="font-size:11px;color:#6B7280">Tarix</span>
          <span style="font-size:12px;font-weight:500">18.03.2025</span>
        </div>
        <div class="m-row" style="padding:5px 0;border-bottom:none;pointer-events:none">
          <span style="font-size:11px;color:#6B7280">Kateqoriya</span>
          <span style="font-size:11px;font-weight:500">Metal kons.</span>
        </div>
      </div>
    </div>
    <div style="padding:8px 10px;border-top:1px solid #F3F4F6;display:flex;justify-content:flex-end;gap:6px">
      <button style="font-size:11.5px;font-weight:600;color:#DC2626;background:#FEF2F2;border:1px solid #FCA5A5;border-radius:6px;padding:4px 10px;cursor:default">Rədd et</button>
      <button style="font-size:11.5px;font-weight:600;color:#fff;background:#16A34A;border:none;border-radius:6px;padding:4px 12px;cursor:default">✓ Təsdiqlə</button>
    </div>
  </div>

  <!-- ── Reports: budget vs actual ─────────────────────────────── -->
  <div v-else-if="type === 'reports-budget'" class="tp-preview">
    <div style="padding:10px 14px">
      <div style="font-size:10.5px;font-weight:600;color:#9CA3AF;text-transform:uppercase;letter-spacing:0.04em;margin-bottom:10px">Büdcə vs Faktiki (₼)</div>
      <div style="margin-bottom:8px">
        <div style="display:flex;justify-content:space-between;font-size:12px;margin-bottom:5px">
          <span style="font-weight:500;color:#374151">Beton işlər</span>
          <span style="font-weight:600;color:#16A34A;font-family:monospace">₼35k / ₼50k</span>
        </div>
        <div class="progress-track"><div class="progress-bar" style="width:70%;background:#16A34A" /></div>
      </div>
      <div style="margin-bottom:8px">
        <div style="display:flex;justify-content:space-between;font-size:12px;margin-bottom:5px">
          <span style="font-weight:500;color:#374151">Metal kons.</span>
          <span style="font-weight:600;color:#D97706;font-family:monospace">₼28.5k / ₼30k ⚠</span>
        </div>
        <div class="progress-track"><div class="progress-bar" style="width:95%;background:#F59E0B" /></div>
      </div>
      <div>
        <div style="display:flex;justify-content:space-between;font-size:12px;margin-bottom:5px">
          <span style="font-weight:500;color:#374151">Elektrik</span>
          <span style="font-weight:600;color:#3D5AF1;font-family:monospace">₼8.2k / ₼15k</span>
        </div>
        <div class="progress-track"><div class="progress-bar" style="width:55%;background:#3D5AF1" /></div>
      </div>
    </div>
  </div>

  <!-- ── Reports: monthly trend ────────────────────────────────── -->
  <div v-else-if="type === 'reports-trend'" class="tp-preview">
    <div style="padding:10px 14px">
      <div style="font-size:10.5px;font-weight:600;color:#9CA3AF;text-transform:uppercase;letter-spacing:0.04em;margin-bottom:10px">Aylıq Xərc (₼)</div>
      <div style="display:flex;align-items:flex-end;gap:5px;height:72px">
        <div v-for="(bar, i) in [
          { h: '38%', label: 'Yan', color: '#3D5AF1' },
          { h: '55%', label: 'Fev', color: '#3D5AF1' },
          { h: '100%', label: 'Mar', color: '#F59E0B' },
          { h: '62%', label: 'Apr', color: '#3D5AF1' },
          { h: '30%', label: 'May', color: '#3D5AF1' },
        ]" :key="i" style="flex:1;display:flex;flex-direction:column;align-items:center;gap:3px">
          <div :style="{ background: bar.color, borderRadius: '3px 3px 0 0', width: '100%', height: bar.h, marginTop: 'auto' }" />
          <div style="font-size:9.5px;color:#9CA3AF">{{ bar.label }}</div>
        </div>
      </div>
      <div style="display:flex;align-items:center;gap:5px;margin-top:9px">
        <UIcon name="i-lucide-trending-up" style="width:13px;height:13px;color:#D97706;flex:none" />
        <span style="font-size:11.5px;color:#D97706;font-weight:500">Mart — ən yüksək xərc: ₼35,000</span>
      </div>
    </div>
  </div>

  <!-- ── Reports: supplier ledger ──────────────────────────────── -->
  <div v-else-if="type === 'reports-supplier'" class="tp-preview" style="overflow:hidden">
    <div style="display:grid;grid-template-columns:2fr 1fr 1fr;padding:7px 12px;background:#F9FAFB;border-bottom:1px solid #E5E7EB;font-size:10px;font-weight:700;color:#9CA3AF;text-transform:uppercase;letter-spacing:0.04em">
      <span>Təchizatçı</span><span style="text-align:right">Cəm</span><span style="text-align:right">Əməl.</span>
    </div>
    <div
      v-for="row in [
        { name: 'Beton MMC',       total: '₼35,800', count: 4 },
        { name: 'Metal Kons. ASC', total: '₼28,500', count: 2 },
        { name: 'Plitka Mərkəzi',  total: '₼11,200', count: 3 },
      ]"
      :key="row.name"
      style="display:grid;grid-template-columns:2fr 1fr 1fr;padding:8px 12px;border-bottom:1px solid #F3F4F6;font-size:12.5px"
    >
      <span style="font-weight:500;color:#0A0A0A;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ row.name }}</span>
      <span style="text-align:right;font-family:monospace;font-weight:700;color:#141F5E">{{ row.total }}</span>
      <span style="text-align:right;color:#6B7280">{{ row.count }}</span>
    </div>
  </div>

  <!-- ── Reports: cost per m² ──────────────────────────────────── -->
  <div v-else-if="type === 'reports-m2'" class="tp-preview" style="text-align:center;padding:16px 14px">
    <div style="font-size:12px;color:#6B7280;margin-bottom:6px">Bakı Bağ Evi · 320 m²</div>
    <div style="font-size:36px;font-weight:800;color:#0D1440;font-family:monospace;letter-spacing:-0.03em;line-height:1.1">₼ 211</div>
    <div style="font-size:12.5px;color:#6B7280;margin-bottom:12px;margin-top:2px">hər m²-ə düşən xərc</div>
    <div style="height:1px;background:#E5E7EB;margin-bottom:12px" />
    <div style="display:flex;justify-content:space-around">
      <div>
        <div style="font-size:11px;color:#9CA3AF;margin-bottom:4px">Ümumi xərc</div>
        <div style="font-size:13.5px;font-weight:700;font-family:monospace;color:#0D1440">₼67,500</div>
      </div>
      <div>
        <div style="font-size:11px;color:#9CA3AF;margin-bottom:4px">Sahə</div>
        <div style="font-size:13.5px;font-weight:700;font-family:monospace;color:#0D1440">320 m²</div>
      </div>
      <div>
        <div style="font-size:11px;color:#9CA3AF;margin-bottom:4px">Büdcə</div>
        <div style="font-size:13.5px;font-weight:700;font-family:monospace;color:#0D1440">₼150k</div>
      </div>
    </div>
  </div>
</template>

<style>
.tp-preview {
  background: #fff;
  border: 1px solid #E5E7EB;
  border-radius: 10px;
  margin-top: 14px;
  overflow: hidden;
}
</style>
