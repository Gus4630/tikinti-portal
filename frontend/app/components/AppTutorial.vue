<script setup lang="ts">
import { TUTORIAL_STEPS, type TutorialStep } from '~/composables/useTutorial'

const tutorial = useTutorial()
const router = useRouter()

const isNavigating = ref(false)
const spotlightRect = ref<{ top: number; left: number; width: number; height: number } | null>(null)
const cardPos = ref<Record<string, string>>({})
const isMobile = ref(false)
const cardEl = ref<HTMLElement | null>(null)

const PAD = 8

function cardWidth(step: TutorialStep) {
  return step.preview ? 400 : 330
}

function findEl(step: TutorialStep): Element | null {
  if (!step.target) return null
  isMobile.value = window.innerWidth < 768

  if (isMobile.value && step.mobileTarget) {
    const el = document.querySelector(`[data-tutorial="${step.mobileTarget}"]`)
    if (el) {
      const r = el.getBoundingClientRect()
      if (r.width + r.height > 0) return el
    }
  }
  return document.querySelector(`[data-tutorial="${step.target}"]`)
}

async function recalc() {
  await nextTick()
  const step = tutorial.currentStep.value
  if (!step) return

  isMobile.value = window.innerWidth < 768
  const CW = cardWidth(step)
  const el = findEl(step)

  if (el) {
    const r = el.getBoundingClientRect()
    spotlightRect.value = {
      top: r.top - PAD,
      left: r.left - PAD,
      width: r.width + PAD * 2,
      height: r.height + PAD * 2,
    }

    if (isMobile.value) {
      const midY = r.top + r.height / 2
      if (midY > window.innerHeight * 0.5) {
        cardPos.value = { top: '16px', left: '16px', right: '16px' }
      } else {
        cardPos.value = {
          bottom: 'calc(env(safe-area-inset-bottom, 0px) + 80px)',
          left: '16px',
          right: '16px',
        }
      }
    } else {
      const vw = window.innerWidth
      const vh = window.innerHeight

      let left = r.right + PAD + 14
      if (left + CW > vw - PAD) left = Math.max(PAD, r.left - CW - 14)

      // Use bottom anchor when element is in the lower 45% of viewport
      const elMidY = r.top + r.height / 2
      if (elMidY > vh * 0.55) {
        cardPos.value = { bottom: `${PAD}px`, left: `${left}px`, width: `${CW}px` }
      } else {
        const top = Math.max(PAD, r.top + r.height / 2 - 120)
        cardPos.value = { top: `${top}px`, left: `${left}px`, width: `${CW}px` }
      }
    }
  } else {
    spotlightRect.value = null
    if (isMobile.value) {
      cardPos.value = {
        top: '50%',
        left: '50%',
        transform: 'translate(-50%, -50%)',
        width: 'calc(100% - 32px)',
        maxWidth: `${CW}px`,
      }
    } else {
      cardPos.value = {
        top: '50%',
        left: '50%',
        transform: 'translate(-50%, -50%)',
        width: `${CW}px`,
      }
    }
  }
}

watch(tutorial.step, async (newStep) => {
  const step = TUTORIAL_STEPS[newStep]
  if (step?.navigateTo) {
    isNavigating.value = true
    try {
      await router.push(step.navigateTo)
    } catch {
      // already on route
    }
    await new Promise(r => setTimeout(r, 180))
    isNavigating.value = false
  }
  // Switch report tab if this step targets a specific tab
  if (step?.reportTab) {
    await nextTick()
    const tabBtn = document.querySelector(`[data-tutorial-tab="${step.reportTab}"]`) as HTMLButtonElement
    tabBtn?.click()
    await nextTick()
  }
  await recalc()
})

watch(tutorial.active, (v) => {
  if (v) nextTick().then(recalc)
})

onMounted(() => {
  window.addEventListener('resize', recalc)
  if (tutorial.active.value) recalc()
})

onUnmounted(() => window.removeEventListener('resize', recalc))
</script>

<template>
  <Teleport to="body">
    <Transition name="tut-fade">
      <div v-if="tutorial.active.value" class="tut-root">
        <!-- Backdrop when no spotlight target -->
        <div v-if="!spotlightRect" class="tut-backdrop" />

        <!-- Spotlight — box-shadow creates the dark surround -->
        <div
          v-if="spotlightRect"
          class="tut-spotlight"
          :style="{
            top: spotlightRect.top + 'px',
            left: spotlightRect.left + 'px',
            width: spotlightRect.width + 'px',
            height: spotlightRect.height + 'px',
          }"
        />

        <!-- Tutorial card -->
        <div ref="cardEl" class="tut-card" :style="{ position: 'absolute', ...cardPos }">
          <!-- Gradient progress bar -->
          <div class="tut-bar">
            <div class="tut-bar-fill" :style="{ width: tutorial.progress.value + '%' }" />
          </div>

          <!-- Step counter + skip -->
          <div class="tut-meta">
            <span class="tut-counter">{{ tutorial.step.value + 1 }} / {{ TUTORIAL_STEPS.length }}</span>
            <button class="tut-skip" @click="tutorial.finish()">Atla ×</button>
          </div>

          <!-- Loading spinner during navigation -->
          <div v-if="isNavigating" class="tut-loading">
            <UIcon name="i-lucide-loader-2" style="width:26px;height:26px;animation:spin 0.8s linear infinite;color:#9CA3AF" />
          </div>

          <template v-else>
            <!-- Step icon + title -->
            <div class="tut-icon-wrap">
              <UIcon
                :name="tutorial.currentStep.value?.icon ?? 'i-lucide-info'"
                style="width:22px;height:22px;color:#3D5AF1"
              />
            </div>
            <h3 class="tut-title">{{ tutorial.currentStep.value?.title }}</h3>
            <p class="tut-body">{{ tutorial.currentStep.value?.description }}</p>

            <!-- Mock-data preview panel -->
            <TutorialPreview
              v-if="tutorial.currentStep.value?.preview && !isMobile"
              :type="tutorial.currentStep.value.preview"
            />
          </template>

          <!-- Prev / Next -->
          <div class="tut-nav">
            <button
              v-if="!tutorial.isFirst.value"
              class="tut-btn tut-btn--ghost"
              :disabled="isNavigating"
              @click="tutorial.prev()"
            >
              ← Geri
            </button>
            <div style="flex:1" />
            <button
              class="tut-btn tut-btn--primary"
              :disabled="isNavigating"
              @click="tutorial.next()"
            >
              {{ tutorial.isLast.value ? 'Bitir ✓' : 'İrəli →' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style>
.tut-root {
  position: fixed;
  inset: 0;
  z-index: 9500;
  pointer-events: all;
}

.tut-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.72);
  pointer-events: none;
}

.tut-spotlight {
  position: absolute;
  border-radius: 10px;
  box-shadow: 0 0 0 9999px rgba(0, 0, 0, 0.72);
  pointer-events: none;
  z-index: 1;
  transition:
    top 0.3s cubic-bezier(0.4, 0, 0.2, 1),
    left 0.3s cubic-bezier(0.4, 0, 0.2, 1),
    width 0.3s cubic-bezier(0.4, 0, 0.2, 1),
    height 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.tut-card {
  z-index: 2;
  background: #fff;
  border-radius: 16px;
  padding: 22px;
  box-shadow:
    0 0 0 1px rgba(0, 0, 0, 0.06),
    0 20px 60px rgba(0, 0, 0, 0.28);
  pointer-events: all;
  max-height: calc(100vh - 24px);
  overflow-y: auto;
}

@media (max-width: 767px) {
  .tut-card { padding: 18px 16px; }
}

.tut-bar {
  height: 3px;
  background: #F3F4F6;
  border-radius: 9999px;
  overflow: hidden;
  margin-bottom: 16px;
}

.tut-bar-fill {
  height: 100%;
  background: linear-gradient(to right, #3D5AF1, #F59E0B);
  border-radius: 9999px;
  transition: width 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}

.tut-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.tut-counter {
  font-size: 11px;
  font-weight: 600;
  color: #9CA3AF;
  font-family: var(--font-mono, monospace);
  letter-spacing: 0.05em;
}

.tut-skip {
  font-size: 12px;
  color: #9CA3AF;
  background: none;
  border: none;
  cursor: pointer;
  padding: 3px 8px;
  border-radius: 6px;
  font-family: inherit;
  transition: background 0.12s, color 0.12s;
}
.tut-skip:hover { background: #F3F4F6; color: #6B7280; }

.tut-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: #EEF2FF;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}

.tut-title {
  font-size: 16px;
  font-weight: 700;
  color: #0A0A0A;
  margin: 0 0 6px;
  letter-spacing: -0.02em;
  line-height: 1.3;
}

.tut-body {
  font-size: 13px;
  color: #6B7280;
  line-height: 1.62;
  margin: 0 0 16px;
}

.tut-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 28px 0;
}

.tut-nav {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
}

.tut-btn {
  height: 38px;
  padding: 0 18px;
  border-radius: 9px;
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  font-family: inherit;
  display: inline-flex;
  align-items: center;
  transition: background 0.12s, transform 0.1s, opacity 0.12s;
}
.tut-btn:disabled { opacity: 0.45; cursor: not-allowed; }
.tut-btn:not(:disabled):active { transform: scale(0.96); }

.tut-btn--primary { background: #3D5AF1; color: #fff; }
.tut-btn--primary:not(:disabled):hover { background: #2D44D4; }

.tut-btn--ghost { background: #F3F4F6; color: #374151; }
.tut-btn--ghost:not(:disabled):hover { background: #E5E7EB; }

.tut-fade-enter-active,
.tut-fade-leave-active { transition: opacity 0.25s ease; }
.tut-fade-enter-from,
.tut-fade-leave-to { opacity: 0; }
</style>
