/**
 * Stores 统一导出
 * 
 * Store 命名规范：
 * - 文件名使用小写字母，多个单词用驼峰命名
 * - Store 函数名使用 use + 名称 + Store 格式
 * - 如：useFlowModelStore, useAtomStore
 * 
 * Store 分类：
 * 1. 核心业务 Store - Flow 模型、插件、权限等
 * 2. 列表/记录 Store - 执行记录、触发记录等
 * 3. UI 状态 Store - 全局 UI 状态
 * 4. 基础 Store - 认证、日志等
 */

// ============================================================================
// 核心业务 Stores
// ============================================================================

/** Flow 模型状态管理 */
export { useFlowModelStore } from './flowModel'

/** 插件配置和版本缓存 */
export { useAtomStore } from './atom'

/** 创作流分组管理 */
export { useFlowGroupStore } from './flowGroup'

/** 创作流信息 */
export { useFlowInfoStore } from './flowInfoStore'

/** 权限代持管理 */
export { usePermissionDelegationStore } from './permissionDelegation'

/** 编辑模式管理 */
export { useModeStore } from './flowMode'

// ============================================================================
// 列表/记录 Stores
// ============================================================================

/** 创作流列表内容 */
export { DialogType, useFlowHomeContentStore } from './flowContentList'

/** 执行记录 */
export { useExecutionRecordStore } from './executionRecord'

/** 执行详情 */
export { useExecuteDetailStore } from './executeDetail'

/** 执行预览 */
export {
    usePreviewStore,
    type AtomicState,
    type BuildNoConfig,
    type ParamCategory,
    type ParamsRecord,
    type ProcessedProperty,
    type SkipAtomsRecord
} from './preview'

/** 触发记录 */
export { useTriggerRecordStore } from './triggerRecord'

/** 变更日志 */
export { useChangeLogStore } from './changeLog'

// ============================================================================
// 弹窗/操作 Stores
// ============================================================================

/** 新建创作流 */
export { useNewFlowStore } from './createFlowStore'

/** 添加到分组 */
export { useAddToGroupStore, type TreeNode } from './addToGroupStore'

// ============================================================================
// UI 状态 Stores
// ============================================================================

/** 全局 UI 状态 */
export { useUIStore } from './ui'

// ============================================================================
// 基础 Stores
// ============================================================================

/** 认证状态 */
export { useAuthStore } from './auth'

/** HTTP 日志 */
export { useHttpLogStore } from './httpLog'




