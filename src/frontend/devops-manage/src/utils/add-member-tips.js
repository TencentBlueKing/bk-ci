const USE_ORDER = ['repertory', 'credential', 'cert', 'env_node']

const USE_LABEL_KEY = {
  repertory: '任意代码库',
  credential: '任意凭据',
  cert: '任意证书',
  env_node: '任意第三方构建机',
}

function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function joinLabels(labels, locale) {
  if (labels.length <= 1) return labels[0] || ''
  const lang = String(locale || 'zh-CN').toLowerCase()
  if (lang.startsWith('en')) {
    if (labels.length === 2) return `${labels[0]} and ${labels[1]}`
    return `${labels.slice(0, -1).join(', ')}, and ${labels[labels.length - 1]}`
  }
  if (lang.startsWith('ja')) {
    if (labels.length === 2) return `${labels[0]}と${labels[1]}`
    return `${labels.slice(0, -1).join('、')}と${labels[labels.length - 1]}`
  }
  if (labels.length === 2) return `${labels[0]}和${labels[1]}`
  return `${labels.slice(0, -1).join('、')}和${labels[labels.length - 1]}`
}

function currentLocale(locale) {
  if (locale) return locale
  return 'zh-CN'
}

// listGroup 记录上由后端提供：
// adminGroup: 是否为项目管理员组，groupCode = manager
// sensitiveResourceTypes: 资源范围为「任意」的使用权限，取值 repertory / credential / cert / env_node
export function buildAddMemberTips({ group = {}, t, docUrl, locale }) {
  const adminGroup = Boolean(group.adminGroup)
  const sensitiveResourceTypes = USE_ORDER.filter((key) => (group.sensitiveResourceTypes || []).includes(key))
  const show = adminGroup || sensitiveResourceTypes.length > 0
  if (!show) {
    return {
      showTips: false,
      tipsHtml: '',
      confirmText: '',
      confirmTips: '',
    }
  }

  const lang = currentLocale(locale)
  const title = adminGroup
    ? t('管理员默认拥有本项目全部资源的管理和操作权限')
    : t('用户组「」可使用', [
      escapeHtml(group.name || ''),
      joinLabels(sensitiveResourceTypes.map((key) => t(USE_LABEL_KEY[key])), lang),
    ])
  const body = adminGroup
    ? t('对方加入后即可使用项目下关联的代码库和第三方构建机，使用项目下创建的凭据和证书，移出项目成员，并再添加管理员。')
    : t('资源范围未限定到具体资源。加入后即可使用这些类型的全部实例。')
  const tipsHtml = [
    `<p style="margin:0 0 8px;font-weight:600;color:#313238;">${title}</p>`,
    `<p style="margin:0;">${body}</p>`,
    `<p style="margin:4px 0 0;">${t('权限清单见「组权限」，管理要求见')}<a href="${escapeHtml(docUrl)}" target="_blank" rel="noopener noreferrer" style="color:#3a84ff;text-decoration:none;">${t('《敏感权限用户组管理须知》')}</a></p>`,
  ].join('')
  const confirmLabel = adminGroup
    ? t('我已知晓管理员权限，并确认对方应当拥有')
    : t('我已知晓该组权限，并确认对方应当拥有')

  return {
    showTips: true,
    tipsHtml,
    confirmText: `<span style="color:#ea3636;font-weight:600;">${confirmLabel}</span>`,
    confirmTips: t('请先勾选确认'),
  }
}
