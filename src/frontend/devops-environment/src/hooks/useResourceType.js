import {
    SERVICE_RESOURCE_TYPE,
    NODE_SOURCE,
    CREATE_NODE_SOURCE_FILTERS,
    DIGITAL_COLLEAGUE_SOURCES,
    ALLNODE
} from '@/store/constants'

/**
 * 节点语义单点出口：资源类型 / nodeSource / Agent 状态 / 已释放已销毁
 * 其余组件禁止自行散落判断。
 *
 * createWorkspaceSource 语义：
 * - REMOTEDEV / IEG_IMATE → 数字同事（同鉴权）
 * - DEVCLOUD → TEG iMate Linux
 * - CLOUD_DESKTOP_NODE → 创作流云桌面
 */
export default function useResourceType () {
    const isCreateResType = (routeOrResType) => {
        const resType = typeof routeOrResType === 'string'
            ? routeOrResType
            : routeOrResType?.params?.resType
        return resType === SERVICE_RESOURCE_TYPE.CREATE
    }

    const isPipelineResType = (routeOrResType) => {
        const resType = typeof routeOrResType === 'string'
            ? routeOrResType
            : routeOrResType?.params?.resType
        return resType === SERVICE_RESOURCE_TYPE.PIPELINE
    }

    /**
     * 取节点来源，优先 createWorkspaceSource
     */
    const getNodeSourceOf = (node) => {
        if (!node) return ''
        return node.createWorkspaceSource || node.nodeSource || ''
    }

    /**
     * 节点类型展示文案 i18n key（由调用方 $t）
     * REMOTEDEV / IEG_IMATE → 数字同事(digitalTwin)
     * CLOUD_DESKTOP_NODE → 云桌面
     * DEVCLOUD → TEG iMate，暂无独立分类文案时不映射到数字同事/云桌面
     */
    const getNodeSourceLabelKey = (nodeOrSource) => {
        const source = typeof nodeOrSource === 'string'
            ? nodeOrSource
            : getNodeSourceOf(nodeOrSource)
        switch (source) {
            case NODE_SOURCE.REMOTEDEV:
            case NODE_SOURCE.IEG_IMATE:
                return 'environment.digitalTwin'
            case NODE_SOURCE.CLOUD_DESKTOP_NODE:
                return 'environment.cloudDesktop'
            default:
                return ''
        }
    }

    const getNodeSourceLabel = (nodeOrSource, $t) => {
        const key = getNodeSourceLabelKey(nodeOrSource)
        return key && $t ? $t(key) : ''
    }

    /**
     * 路由 nodeType 是否为创作流分类过滤值
     */
    const getNodeTypeFilterId = (routeNodeType) => {
        if (!routeNodeType || routeNodeType === ALLNODE) return ''
        if (CREATE_NODE_SOURCE_FILTERS.includes(routeNodeType)) return routeNodeType
        if (['THIRDPARTY', 'CMDB'].includes(routeNodeType)) return routeNodeType
        return ''
    }

    /**
     * 列表请求参数翻译：流水线透传 nodeType；创作流强制 nodeType=CREATE，分类值翻译为 nodeSource
     */
    const toListQuery = (routeNodeType, isCreate) => {
        if (isCreate) {
            const query = { nodeType: 'CREATE' }
            if (CREATE_NODE_SOURCE_FILTERS.includes(routeNodeType)) {
                query.nodeSource = routeNodeType
            }
            return query
        }
        if (routeNodeType && routeNodeType !== ALLNODE && ['THIRDPARTY', 'CMDB'].includes(routeNodeType)) {
            return { nodeType: routeNodeType }
        }
        return {}
    }

    const getNodeStatus = (node) => node?.nodeStatus || node?.status || ''

    /**
     * 是否为数字同事（REMOTEDEV 个人 + IEG_IMATE 团队，同一套鉴权）
     */
    const isDigitalTwin = (node) => {
        const source = getNodeSourceOf(node)
        return DIGITAL_COLLEAGUE_SOURCES.includes(source)
    }

    /**
     * 是否为创作流云桌面节点（CLOUD_DESKTOP_NODE）
     */
    const isCloudDesktop = (node) => {
        return getNodeSourceOf(node) === NODE_SOURCE.CLOUD_DESKTOP_NODE
    }

    // TODO: 已释放/已销毁专用字段待后端确认，当前以 nodeStatus 兜底
    const NEUTRAL_NODE_STATUSES = ['DELETED', 'STOPPED', 'LOST', 'DELETING']

    /**
     * 已释放的数字同事（REMOTEDEV / IEG_IMATE）
     */
    const isReleasedNode = (node) => {
        if (!isDigitalTwin(node)) return false
        return NEUTRAL_NODE_STATUSES.includes(getNodeStatus(node))
    }

    /**
     * 已销毁的云桌面（CLOUD_DESKTOP_NODE）
     */
    const isDestroyedNode = (node) => {
        if (!isCloudDesktop(node)) return false
        return NEUTRAL_NODE_STATUSES.includes(getNodeStatus(node))
    }

    const isNeutralStatusNode = (node) => isReleasedNode(node) || isDestroyedNode(node)

    /**
     * Agent 是否仍存在（已释放/已销毁视为不存在）
     */
    const isAgentPresent = (node) => !isNeutralStatusNode(node)

    /**
     * 创作流 Agent 列：normal / abnormal / ''（中性状态留空）
     * 可用 → normal；正在准备 / 准备失败及其他异常 → abnormal
     */
    const getAgentStatus = (node) => {
        if (isNeutralStatusNode(node)) return ''
        const status = getNodeStatus(node)
        if (['NORMAL', 'BUILD_IMAGE_SUCCESS'].includes(status)) return 'normal'
        return 'abnormal'
    }

    /**
     * 中性状态标签文案 key
     */
    const getNeutralStatusLabelKey = (node) => {
        if (isReleasedNode(node)) return 'environment.released'
        if (isDestroyedNode(node)) return 'environment.destroyed'
        return ''
    }

    /**
     * 是否展示清理入口（仅已释放数字同事 / 已销毁云桌面）
     */
    const canCleanNode = (node) => isNeutralStatusNode(node)

    return {
        isCreateResType,
        isPipelineResType,
        getNodeSourceOf,
        getNodeSourceLabelKey,
        getNodeSourceLabel,
        getNodeTypeFilterId,
        toListQuery,
        getAgentStatus,
        isAgentPresent,
        isReleasedNode,
        isDestroyedNode,
        isNeutralStatusNode,
        getNeutralStatusLabelKey,
        canCleanNode,
        isDigitalTwin,
        isCloudDesktop
    }
}
