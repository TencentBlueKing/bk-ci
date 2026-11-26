import http from './fetch'

const PROJECT_API = '/ms/project/api'

export type ConventionLang = 'zh' | 'en'

export function toConventionLang(locale?: string): ConventionLang {
  if (locale && locale.toLowerCase().startsWith('en')) return 'en'
  return 'zh'
}

export interface ConventionBody {
  lang: ConventionLang
  content: string
}

export interface ConventionAgreementRecord {
  userId: string
  agreeTime: string
}

export interface ConventionAgreementsResult {
  count: number
  page: number
  pageSize: number
  totalPages: number
  records: ConventionAgreementRecord[]
}

function getAjaxBase(): string {
  const prefix = import.meta.env.VITE_AJAX_URL_PREFIX || '/'
  if (/^https?:\/\//.test(prefix)) return prefix.replace(/\/$/, '')
  return `${location.origin}${prefix}`.replace(/\/$/, '')
}

/** 触发 blob 文件下载 */
function triggerBlobDownload(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

export default {
  getBody(lang: ConventionLang) {
    return http.get(`${PROJECT_API}/user/convention/body`, { lang }) as Promise<ConventionBody>
  },

  saveBody(payload: { lang: ConventionLang; content: string }) {
    return http.put(`${PROJECT_API}/user/convention/body`, payload)
  },

  getAgreements(params: { user?: string; page?: number; pageSize?: number }) {
    return http.get(
      `${PROJECT_API}/user/convention/agreements`,
      params,
    ) as Promise<ConventionAgreementsResult>
  },

  async exportAgreements(user?: string) {
    const url = new URL(`${getAjaxBase()}${PROJECT_API}/user/convention/agreements/export`)
    if (user) url.searchParams.set('user', user)
    const response = await window.fetch(url.toString(), {
      method: 'GET',
      credentials: 'include',
      headers: {
        'X-Requested-With': 'fetch',
      },
    })
    const contentType = response.headers.get('Content-Type') || ''
    if (
      !response.ok
      || contentType.includes('application/json')
      || contentType.includes('text/html')
    ) {
      if (contentType.includes('application/json')) {
        const payload = await response.json().catch(() => ({}))
        throw new Error(payload.message || '导出失败')
      }
      throw new Error('导出失败')
    }
    const disposition = response.headers.get('Content-Disposition') || ''
    const utfMatch = disposition.match(/filename\*=UTF-8''([^;]+)/i)
    const plainMatch = disposition.match(/filename="?([^";]+)"?/i)
    const filename = decodeURIComponent(utfMatch?.[1] || plainMatch?.[1] || '签署记录.csv')
    const blob = await response.blob()
    if ((blob.type || '').includes('text/html')) {
      throw new Error('导出失败')
    }
    triggerBlobDownload(blob, filename)
  },
}
