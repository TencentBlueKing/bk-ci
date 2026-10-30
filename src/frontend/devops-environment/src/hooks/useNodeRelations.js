/**
 * 节点关联数据适配层：所属环境 + 流水线/创作流引用
 * 确认弹窗与详情概览共用；创作流引用暂返回空数组（后端聚合接口 TODO）
 *
 * 所属环境：详情 thirdPartyAgentDetail.envIds（envHashId 数组），
 * 再按 id 拉取环境详情补齐名称/类型。
 */

const cache = new Map()

function buildEnvUrl (projectId, resType, env) {
    const envType = env.envType || 'BUILD'
    const envHashId = env.envHashId
    if (!envHashId) return ''
    const res = resType || 'pipeline'
    return `${window.location.origin}/console/environment/${projectId}/${res}/env/${envType}/${envHashId}`
}

function buildPipelineEditUrl (projectId, pipelineId) {
    if (!pipelineId) return ''
    return `${window.location.origin}/console/pipeline/${projectId}/${pipelineId}/edit`
}

function mapEnvironments (list = [], projectId, resType) {
    return list.map(env => ({
        envName: env.name || env.envName || '',
        envHashId: env.envHashId || '',
        envType: env.envType || '',
        // TAG = 动态，NODE = 静态
        strategyType: env.envNodeType === 'TAG' ? 'dynamic' : 'static',
        url: buildEnvUrl(projectId, resType, env)
    })).filter(item => item.envHashId || item.envName)
}

function mapPipelineReferences (records = [], projectId) {
    return (records || []).map(item => {
        const id = item.pipelineId || item.id || ''
        const name = item.pipelineName || item.name || id
        return {
            id,
            name,
            url: buildPipelineEditUrl(projectId, id)
        }
    }).filter(item => item.id || item.name)
}

/**
 * 按 envIds 批量拉环境详情
 */
async function resolveEnvironmentsByIds ({ projectId, envIds, store, resType }) {
    const ids = (envIds || []).filter(Boolean)
    if (!ids.length) return []

    const settled = await Promise.all(ids.map(async (envHashId) => {
        try {
            const detail = await store.dispatch('environment/requestEnvDetail', {
                projectId,
                envHashId
            })
            if (!detail) {
                return { envHashId, name: envHashId }
            }
            return {
                ...detail,
                envHashId: detail.envHashId || envHashId
            }
        } catch (e) {
            console.error('requestEnvDetail failed', envHashId, e)
            return { envHashId, name: envHashId }
        }
    }))

    return mapEnvironments(settled, projectId, resType)
}

/**
 * @param {Object} options
 * @param {string} options.projectId
 * @param {string} options.nodeHashId
 * @param {boolean} options.isCreateResType
 * @param {string} [options.agentId] - 流水线引用查询用
 * @param {string[]} [options.envIds] - 详情接口 envIds
 * @param {Object} options.store - vuex store（proxy.$store）
 * @param {string} [options.resType]
 * @param {boolean} [options.force] - 跳过缓存
 */
export async function fetchNodeRelations ({
    projectId,
    nodeHashId,
    isCreateResType = false,
    agentId,
    envIds,
    store,
    resType,
    force = false
}) {
    if (!projectId || !store) {
        return { environments: [], references: [] }
    }

    const resolvedEnvIds = Array.isArray(envIds) ? envIds : []
    const resolvedResType = resType || (isCreateResType ? 'creative-stream' : 'pipeline')
    const cacheKey = `${projectId}:${nodeHashId || 'noid'}:${isCreateResType ? 'create' : 'pipeline'}:${resolvedEnvIds.join(',')}`
    if (!force && cache.has(cacheKey)) {
        return cache.get(cacheKey)
    }

    let environments = []
    let references = []

    try {
        environments = await resolveEnvironmentsByIds({
            projectId,
            envIds: resolvedEnvIds,
            store,
            resType: resolvedResType
        })
    } catch (e) {
        console.error('fetchNodeRelations environments failed', e)
        environments = []
    }

    if (isCreateResType) {
        // TODO: 创作流引用待后端聚合接口，当前返回空数组
        references = []
    } else if (agentId) {
        try {
            const res = await store.dispatch('environment/requestAgentPipelineList', {
                projectId,
                body: {
                    agentId,
                    view: 'PIPELINE',
                    page: 1,
                    pageSize: 100
                }
            })
            const records = res?.records || res?.data?.records || (Array.isArray(res) ? res : [])
            references = mapPipelineReferences(records, projectId)
        } catch (e) {
            console.error('fetchNodeRelations pipelines failed', e)
            references = []
        }
    }

    const result = { environments, references }
    cache.set(cacheKey, result)
    return result
}

export function invalidateNodeRelationsCache (nodeHashId) {
    if (!nodeHashId) {
        cache.clear()
        return
    }
    for (const key of cache.keys()) {
        if (key.includes(`:${nodeHashId}:`)) {
            cache.delete(key)
        }
    }
}

export default function useNodeRelations () {
    return {
        fetchNodeRelations,
        invalidateNodeRelationsCache
    }
}
