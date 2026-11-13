import { osOf } from '@/components/devops/environment/import-third-party/constants'

export const BATCH_LIMIT_MAX = 500

export function isLinuxOs (node) {
    const code = String(node?.os || node?.osType || '').toUpperCase()
    if (code === 'LINUX') return true
    if (code === 'WINDOWS' || code === 'MACOS' || code === 'MAC') return false
    return osOf(node) === 'LINUX'
}

export function isLinuxBuildNode (node) {
    return !!node && node.nodeType === 'THIRDPARTY' && isLinuxOs(node)
}

export function isUnlimited (value) {
    const text = String(value ?? '').trim().toLowerCase()
    return !text || text === 'unlimited'
}

export function normalizeCpu (value) {
    if (isUnlimited(value)) return ''
    return String(value).trim()
}

export function cpuLimitError (value, required = false) {
    const text = normalizeCpu(value)
    if (!text) return required ? 'required' : ''
    if (!/^\d+(\.\d+)?$/.test(text) || !(Number(text) > 0)) return 'invalid'
    return ''
}

export function normalizeMemory (value) {
    return String(value ?? '').trim().replace(/(m|g)$/i, (unit) => unit.toLowerCase())
}

export function memoryLimitError (value, required = false) {
    const text = String(value ?? '').trim()
    if (!text) return required ? 'required' : ''
    const matched = text.match(/^(\d+)(m|g)$/i)
    if (!matched) return 'format'
    const amount = Number(matched[1])
    const mb = matched[2].toLowerCase() === 'g' ? amount * 1024 : amount
    if (mb < 6) return 'min'
    return ''
}

export function parseMemoryLimit (value) {
    const matched = String(value ?? '').trim().match(/^(\d+)(m|g)$/i)
    if (!matched) return { amount: '', unit: 'g' }
    return { amount: matched[1], unit: matched[2].toLowerCase() }
}

export function joinMemoryLimit (amount, unit) {
    const text = String(amount ?? '').trim()
    if (!text) return ''
    return `${text}${unit === 'm' ? 'm' : 'g'}`
}

export function formatMemoryLimit (value) {
    if (isUnlimited(value)) return ''
    const text = String(value).trim()
    const parsed = parseMemoryLimit(text)
    if (!parsed.amount) return text
    return `${parsed.amount} ${parsed.unit}`
}

export function containerEngineName (runtime) {
    const text = String(runtime || '').trim().toLowerCase()
    if (text === 'podman') return 'Podman'
    if (text === 'docker') return 'Docker'
    return ''
}

export function limitErrorKey (kind, code) {
    if (!code) return ''
    if (kind === 'cpu') {
        return code === 'required' ? 'environment.cpuLimitRequired' : 'environment.cpuLimitInvalid'
    }
    if (code === 'required') return 'environment.memoryLimitRequired'
    if (code === 'min') return 'environment.memoryLimitMin'
    return 'environment.memoryLimitFormat'
}

export function nodeLimitLabel (node) {
    const alias = String(node?.displayName || '').trim()
    const ip = String(node?.ip || '').trim()
    if (alias && ip) return `${alias}（${ip}）`
    return alias || ip || String(node?.nodeHashId || '')
}

export function splitContainerLimitNodes (nodes) {
    const targets = []
    const skipped = []
    let noEdit = 0
    ;(nodes || []).forEach((node) => {
        if (!node?.canEdit) {
            noEdit += 1
            return
        }
        if (isLinuxBuildNode(node)) targets.push(node)
        else skipped.push(node)
    })
    return { targets, skipped, noEdit }
}
