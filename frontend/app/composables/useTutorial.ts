export interface TutorialStep {
  id: string
  icon: string
  title: string
  description: string
  target: string | null
  mobileTarget?: string
  placement: 'center' | 'right' | 'bottom'
  navigateTo?: string
  preview?: string
  reportTab?: string
}

export const TUTORIAL_STEPS: TutorialStep[] = [
  {
    id: 'welcome',
    icon: 'i-lucide-sparkles',
    title: 'SagaGroup-a Xoş Gəldiniz!',
    description: 'Bu interaktiv tur 11 addımda portalın bütün əsas funksiyalarını sizə tanıdacaq — qrup yaratmaqdan süni intelekt-dəstəkli xərc analitikasına qədər.',
    target: null,
    placement: 'center',
  },
  {
    id: 'groups',
    icon: 'i-lucide-users',
    title: '1. Qrup yaradın',
    description: 'Hər şey qrupla başlayır. Qrup komanda üzvlərini bir araya gətirən vahiddir: sahibkar, mühasib, memar — hər birinə rol təyin edilir (OWNER / MEMBER).',
    target: 'nav-groups',
    mobileTarget: 'mobile-more',
    placement: 'right',
    navigateTo: '/groups',
    preview: 'groups',
  },
  {
    id: 'buildings',
    icon: 'i-lucide-building-2',
    title: '2. Bina əlavə edin',
    description: 'Hər inşaat layihəsi bir bina kimi qeydə alınır. Binaya ad, ünvan, sahə (m²) və büdcə limiti verin. Büdcə 80%-ə çatanda Telegram bildirişi göndərilir.',
    target: 'nav-buildings',
    mobileTarget: 'mobile-nav-buildings',
    placement: 'right',
    navigateTo: '/buildings',
    preview: 'buildings',
  },
  {
    id: 'categories',
    icon: 'i-lucide-tag',
    title: '3. Kateqoriyaları idarə edin',
    description: 'Sistem 136 hazır inşaat kateqoriyası ilə gəlir: beton işlər, elektrik, santexnik, bitirmə və s. Siyahıda çatışmayan kateqoriyanı əlavə edə bilərsiniz.',
    target: 'nav-categories',
    mobileTarget: 'mobile-more',
    placement: 'right',
    navigateTo: '/categories',
    preview: 'categories',
  },
  {
    id: 'manual-expense',
    icon: 'i-lucide-pen-line',
    title: '4. Əl ilə xərc daxil edin',
    description: '"+" düyməsi ilə xərci əl ilə qeyd edin: məbləğ, valyuta, tarix, kateqoriya, təchizatçı. Eyni xərc iki dəfə daxil edilərsə sistem avtomatik sizi xəbərdar edir.',
    target: 'nav-expenses',
    mobileTarget: 'mobile-nav-expenses',
    placement: 'right',
    navigateTo: '/expenses',
    preview: 'manual-expense',
  },
  {
    id: 'invoice-upload',
    icon: 'i-lucide-upload',
    title: '5. Qaimə şəklini yükləyin',
    description: 'Kağız qaiməni çəkin və ya PDF-i yükləyin. "Qaimə yüklə" düyməsi ilə faylı seçin — Süni İntellekt məlumatları oxumağa başlayır.',
    target: 'nav-expenses',
    mobileTarget: 'mobile-nav-expenses',
    placement: 'right',
    navigateTo: '/expenses',
    preview: 'upload',
  },
  {
    id: 'ai-review',
    icon: 'i-lucide-bot',
    title: '6. Süni İntellekt məlumatları oxuyur',
    description: 'Süni İntellekt sistemi qaimənizdən məbləği, tarixi, kateqoriyanı avtomatik çıxarır. Tam oxunanlar "Yoxlanılır", natamam olanlar "Mübahisəli" statusu alır.',
    target: 'nav-expenses',
    mobileTarget: 'mobile-nav-expenses',
    placement: 'right',
    navigateTo: '/expenses',
    preview: 'ai-review',
  },
  {
    id: 'expense-approve',
    icon: 'i-lucide-check-circle',
    title: '7. Yoxlayın və təsdiqləyin',
    description: 'Xərc detalına keçin: solda orijinal qaimə şəkli, sağda süni intelektin oxuduğu məlumatlar. Düzəliş edib "Təsdiqlə" basın — xərc hesabatlara düşür.',
    target: 'nav-expenses',
    mobileTarget: 'mobile-nav-expenses',
    placement: 'right',
    navigateTo: '/expenses',
    preview: 'expense-detail',
  },
  {
    id: 'reports-budget',
    icon: 'i-lucide-bar-chart-horizontal',
    title: '8. Büdcə vs Faktiki',
    description: 'Hər kateqoriya üzrə planlaşdırılmış büdcəni faktiki xərclərlə müqayisə edir. Sarı — 80% keçildi, qırmızı — büdcə aşıldı. Excel ixracı da mövcuddur.',
    target: 'nav-reports',
    mobileTarget: 'mobile-more',
    placement: 'right',
    navigateTo: '/reports',
    preview: 'reports-budget',
    reportTab: 'budget',
  },
  {
    id: 'reports-trend',
    icon: 'i-lucide-trending-up',
    title: '9. Aylıq Xərc Trendi',
    description: 'Aylıq xərc dəyişikliyini göstərir. Hansı ay xərclər kəskin artıb? Mövsümi qiymət artımlarını vaxtında görüb strategiya qurursunuz.',
    target: 'nav-reports',
    mobileTarget: 'mobile-more',
    placement: 'right',
    navigateTo: '/reports',
    preview: 'reports-trend',
    reportTab: 'trend',
  },
  {
    id: 'reports-supplier',
    icon: 'i-lucide-truck',
    title: '10. Təchizatçı Dəftəri',
    description: 'Hər təchizatçıya ödənilən ümumi məbləği və əməliyyat sayını göstərir. Hansı şirkətlə ən çox iş görürsünüz? Danışıqlar üçün güclü arqumentdir.',
    target: 'nav-reports',
    mobileTarget: 'mobile-more',
    placement: 'right',
    navigateTo: '/reports',
    preview: 'reports-supplier',
    reportTab: 'ledger',
  },
  {
    id: 'reports-m2',
    icon: 'i-lucide-ruler',
    title: '11. m²-ə düşən xərc',
    description: 'Ümumi xərci binanın sahəsinə böldükdə alınan göstərici. Fərqli layihələri müqayisə etmək üçün ən obyektiv metrikdir — ₼/m² hesabı ilə.',
    target: 'nav-reports',
    mobileTarget: 'mobile-more',
    placement: 'right',
    navigateTo: '/reports',
    preview: 'reports-m2',
    reportTab: 'm2',
  },
  {
    id: 'done',
    icon: 'i-lucide-check-circle-2',
    title: 'Hazırsınız!',
    description: 'Portalın bütün əsas funksiyalarını öyrəndiniz. İndi öz qrupunuzu yaradın, binalarınızı əlavə edin və ilk xərcinizi qeyd edin. Uğurlar!',
    target: null,
    placement: 'center',
  },
]

let _initialized = false

export function useTutorial() {
  const active = useState('tut-active', () => false)
  const step = useState('tut-step', () => 0)
  const completed = useState('tut-completed', () => false)

  function init() {
    if (_initialized || typeof window === 'undefined') return
    _initialized = true
    const seen = localStorage.getItem('tutorial_seen')
    if (seen === 'true') return
    localStorage.setItem('tutorial_seen', 'true')
    active.value = true
  }

  function start() {
    step.value = 0
    active.value = true
    completed.value = false
    if (typeof window !== 'undefined') {
      localStorage.removeItem('tutorial_completed')
    }
  }

  function finish() {
    active.value = false
    completed.value = true
    if (typeof window !== 'undefined') {
      localStorage.setItem('tutorial_completed', 'true')
    }
  }

  function next() {
    if (step.value < TUTORIAL_STEPS.length - 1) {
      step.value++
    } else {
      finish()
    }
  }

  function prev() {
    if (step.value > 0) step.value--
  }

  const currentStep = computed(() => TUTORIAL_STEPS[step.value])
  const progress = computed(() => ((step.value + 1) / TUTORIAL_STEPS.length) * 100)
  const isFirst = computed(() => step.value === 0)
  const isLast = computed(() => step.value === TUTORIAL_STEPS.length - 1)

  return {
    active,
    step,
    steps: TUTORIAL_STEPS,
    completed,
    currentStep,
    progress,
    isFirst,
    isLast,
    init,
    start,
    finish,
    next,
    prev,
  }
}
