/** 公约 HTML 白名单，对齐联调文档 / 原型 */
export const CONVENTION_XSS_OPTIONS = {
    whiteList: {
        p: [],
        ol: [],
        ul: [],
        li: [],
        h1: [],
        h2: [],
        h3: [],
        strong: ['style'],
        em: [],
        br: [],
        a: ['href', 'title', 'target', 'rel'],
        span: ['style', 'class']
    },
    css: {
        whiteList: {
            color: true,
            'font-weight': true
        }
    },
    stripIgnoreTag: true,
    stripIgnoreTagBody: ['script', 'style']
}

export function toConventionLang (locale?: string): 'zh' | 'en' {
    if (locale && locale.toLowerCase().startsWith('en')) return 'en'
    return 'zh'
}
