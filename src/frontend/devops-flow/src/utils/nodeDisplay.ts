type NodeDisplayInfo = {
  nodeName?: string
  nodeIp?: string
  ip?: string
  displayName?: string
  name?: string
}

function normalizeText(value?: string | null): string {
  return value?.trim() || ''
}

export function getNodeNameIpDisplayText(node: NodeDisplayInfo = {}): string {
  const nodeName = normalizeText(node.nodeName)
    || normalizeText(node.displayName)
    || normalizeText(node.name)
  const nodeIp = normalizeText(node.nodeIp) || normalizeText(node.ip)

  if (nodeName && nodeIp) {
    return `${nodeName}(${nodeIp})`
  }

  return nodeName || nodeIp || '--'
}
