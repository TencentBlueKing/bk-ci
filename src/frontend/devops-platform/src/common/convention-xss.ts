/** 公约 HTML 白名单，对齐联调文档 */
export const CONVENTION_XSS_OPTIONS = {
  whiteList: {
    p: [],
    ol: [],
    ul: [],
    li: [],
    strong: [],
    em: [],
    br: [],
    a: ['href', 'title', 'target', 'rel'],
    span: ['style', 'class'],
  },
  css: {
    whiteList: {
      color: true,
      'font-weight': true,
    },
  },
  stripIgnoreTag: true,
  stripIgnoreTagBody: ['script', 'style'],
}
