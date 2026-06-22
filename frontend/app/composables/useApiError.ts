export interface ApiError {
  errorCode: string
  errorMessage: string
  params: Record<string, unknown>
  requestId?: string
  timestamp?: string
}

// Azerbaijani translations keyed by the backend's symbolic errorMessage strings.
// When adding i18n support, replace this map with calls to your i18n library.
const AZ_MESSAGES: Record<string, string> = {
  data_not_found:            'Məlumat tapılmadı.',
  already_exist:             'Bu məlumat artıq mövcuddur.',
  duplicate_invoice:         'Bu qaimə-faktura sistemdə artıq mövcuddur.',
  bad_request:               'Yanlış sorğu.',
  conflict:                  'Məlumat münaqişəsi baş verdi.',
  forbidden:                 'Bu əməliyyat üçün icazəniz yoxdur.',
  unauthorized:              'Giriş tələb olunur.',
  unexpected_internal_error: 'Sistem xətası baş verdi. Zəhmət olmasa yenidən cəhd edin.',
  validation_error:          'Daxil edilən məlumatlar yanlışdır.',
  unique_violation:          'Bu dəyər artıq mövcuddur.',
  building_not_found:        'Layihə tapılmadı.',
  category_not_found:        'Kateqoriya tapılmadı.',
  supplier_not_found:        'Təchizatçı tapılmadı.',
  expense_not_found:         'Xərc tapılmadı.',
  user_not_found:            'İstifadəçi tapılmadı.',
  currency_not_found:        'Valyuta məzənnəsi tapılmadı.',
  invalid_credentials:       'İstifadəçi adı və ya şifrə yanlışdır.',
  refresh_token_invalid:     'Sessiya müddəti bitib. Zəhmət olmasa yenidən daxil olun.',
  ocr_retrigger_not_allowed: 'Bu xərclə bağlı OCR emalını yenidən başlatmaq mümkün deyil.',
  ocr_no_file:               'Bu xərc üçün fayl tapılmadı.',
}

export function parseApiError(err: unknown): ApiError {
  const data = (err as { data?: unknown })?.data
  if (data && typeof data === 'object' && 'errorCode' in (data as object)) {
    return data as ApiError
  }
  return { errorCode: '0008', errorMessage: 'unexpected_internal_error', params: {} }
}

export function translateError(errorMessage: string): string {
  return AZ_MESSAGES[errorMessage] ?? 'Xəta baş verdi. Yenidən cəhd edin.'
}

export function useApiError() {
  function resolveError(err: unknown): { message: string; apiError: ApiError } {
    const apiError = parseApiError(err)
    return { message: translateError(apiError.errorMessage), apiError }
  }

  return { resolveError }
}
