import { ref, shallowRef } from 'vue'
import { getExternalAgents, getMcpServers, getPrompts, getSkills } from '../api'

export function useResources() {
  const prompts = ref<string[]>([])
  const resources = ref<any[]>([])
  const shortcuts = shallowRef<any[]>([])

  async function loadPrompts() {
    try {
      const list = await getPrompts()
      prompts.value = (list || []).map((p: any) => p.content)
    } catch {
      // prompts are optional
    }
  }

  async function loadResources() {
    const allResources: any[] = []

    const [skillList, agentList, mcpList] = await Promise.all([
      getSkills().catch(() => []),
      getExternalAgents().catch(() => []),
      getMcpServers().catch(() => []),
    ])

    for (const skill of skillList || []) {
      allResources.push({
        id: skill.id || skill.skillId,
        name: skill.name || skill.skillName,
        icon: skill.icon || null,
        type: 'tool',
        source: 'skill',
        ...skill,
      })
    }

    for (const agent of agentList || []) {
      allResources.push({
        id: agent.id || agent.configId,
        name: agent.name || agent.agentName,
        icon: agent.icon || null,
        type: 'tool',
        source: 'agent',
        ...agent,
      })
    }

    for (const mcp of mcpList || []) {
      allResources.push({
        id: mcp.id || mcp.serverId,
        name: mcp.name || mcp.serverName,
        icon: mcp.icon || null,
        type: 'mcp',
        source: 'mcp',
        ...mcp,
      })
    }

    resources.value = allResources

    shortcuts.value = allResources
      .filter((r: any) => r.type === 'tool')
      .slice(0, 6)
      .map((r: any) => ({
        id: r.id,
        name: r.name,
        description: r.description || '',
      }))
  }

  return { prompts, resources, shortcuts, loadPrompts, loadResources }
}
