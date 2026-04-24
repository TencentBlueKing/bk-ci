import dayjs from 'dayjs'
import UseInstance from './useInstance'

/**
 * 草稿状态管理 Hook
 * 用于获取流水线或模板的草稿状态
 */
export default function useDraftStatus () {
    const { proxy } = UseInstance()

    /**
     * 获取最新草稿状态
     * @param {Object} params - 参数对象
     * @param {string} params.projectId - 项目ID
     * @param {string} params.id - 流水线ID或模板ID
     * @param {string} params.actionType - 操作类型 (EDIT/ROLLBACK)
     * @param {boolean} params.isTemplate - 是否为模板
     * @param {Object} params.pipelineInfo - 流水线信息对象 (可选，用于获取发布版本信息)
     * @returns {Promise<Object>} 返回草稿状态和格式化后的草稿信息
     */
    async function fetchLatestDraftStatus ({ projectId, id, actionType, isTemplate, pipelineInfo }) {
        const action = isTemplate ? 'common/getTemplateDraftStatus' : 'common/getDraftStatus'
        const dynamicKey = isTemplate ? 'templateId' : 'pipelineId'
        const params = {
            projectId,
            actionType,
            [dynamicKey]: id
        }

        const res = await proxy.$store.dispatch(action, params)
        
        return {
            status: res.status,
            draftSaveInfo: {
                updater: res.draft?.updater,
                updateTime: dayjs(res.draft?.updateTime).format('YYYY-MM-DD HH:mm:ss'),
                draftVersionName: res.draft?.baseVersionName,
                draftVersion: res.draft?.version,
                releaseVersionName: res.release?.versionName || pipelineInfo?.releaseVersionName,
                releaseVersion: res.release?.version || pipelineInfo?.releaseVersion
            }
        }
    }

    return {
        fetchLatestDraftStatus
    }
}
