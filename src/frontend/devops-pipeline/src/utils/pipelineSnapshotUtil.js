/**
 * 流水线快照工具函数
 * 用于比对流水线状态是否发生实际变化
 */

import { CODE_MODE } from './pipelineConst'

/**
 * 需要排除的字段（不影响实际流水线配置，不应参与快照比对）
 */
const EXCLUDED_FIELDS = new Set([
    'isError',           // 表单验证错误状态
    'isInvalid',         // 表单验证无效状态
    'paramIdKey',        // Vue 列表渲染的临时 key
    'disabled',          // 表单元素禁用状态
    'isReviewError',     // 审核错误标记
    'executeCount',      // 流水线执行次数
    'canRetry',          // 当前构建可重试标识
    'startEpoch',        // 流水线开始执行时间戳
    'version',           // 版本号（服务端自动递增）
    'versionName'        // 版本名称（服务端自动生成）
])

/**
 * 清理对象，递归移除排除字段
 * @param {*} obj - 要清理的对象
 * @returns {*} 清理后的对象
 */
function cleanObject (obj) {
    // 处理 null 和 undefined
    if (obj == null) return obj

    // 处理数组
    if (Array.isArray(obj)) {
        return obj.map(cleanObject)
    }

    // 处理普通对象
    if (typeof obj === 'object' && obj.constructor === Object) {
        const cleaned = {}
        // 按键排序，确保序列化后字符串稳定
        const sortedKeys = Object.keys(obj).sort()
        
        for (const key of sortedKeys) {
            // 跳过排除字段
            if (EXCLUDED_FIELDS.has(key)) continue
            
            cleaned[key] = cleanObject(obj[key])
        }
        
        return cleaned
    }

    // 返回基本类型
    return obj
}

/**
 * 构建流水线快照
 * @param {Object} state - Vuex atom state
 * @param {String} mode - 当前编辑模式：'UI' 或 'CODE'
 * @returns {Object} 快照对象
 */
export function buildPipelineSnapshot (state, mode) {
    const snapshot = {
        mode,
        pipelineSetting: cleanObject(state.pipelineSetting)
    }

    // YAML 模式：只需要 YAML 内容
    if (mode === CODE_MODE) {
        snapshot.pipelineYaml = state.pipelineYaml || ''
    } else {
        // UI 模式：需要完整的流水线结构
        snapshot.pipeline = cleanObject(state.pipeline)
        snapshot.pipelineWithoutTrigger = cleanObject(state.pipelineWithoutTrigger)
    }

    return snapshot
}

/**
 * 深度比对两个值是否相等
 * @param {*} value1 - 值1
 * @param {*} value2 - 值2
 * @returns {Boolean} 是否相等
 */
export function isDeepEqual (value1, value2) {
    // 快速路径：引用相等
    if (value1 === value2) return true

    // null/undefined 检查
    if (value1 == null || value2 == null) {
        return value1 === value2
    }

    // 类型不同
    const type1 = typeof value1
    const type2 = typeof value2
    if (type1 !== type2) return false

    // 基本类型
    if (type1 !== 'object') return value1 === value2

    // 数组比对
    const isArray1 = Array.isArray(value1)
    const isArray2 = Array.isArray(value2)
    
    if (isArray1 !== isArray2) return false
    
    if (isArray1) {
        if (value1.length !== value2.length) return false
        return value1.every((item, index) => isDeepEqual(item, value2[index]))
    }

    // Date 对象
    if (value1 instanceof Date && value2 instanceof Date) {
        return value1.getTime() === value2.getTime()
    }

    // RegExp 对象
    if (value1 instanceof RegExp && value2 instanceof RegExp) {
        return value1.toString() === value2.toString()
    }

    // 构造函数不同
    if (value1.constructor !== value2.constructor) return false

    // 普通对象比对
    const keys1 = Object.keys(value1)
    const keys2 = Object.keys(value2)

    // 键数量不同
    if (keys1.length !== keys2.length) return false

    // 检查所有键是否匹配
    return keys1.every(key =>
        keys2.includes(key) && isDeepEqual(value1[key], value2[key])
    )
}

/**
 * 检查流水线是否被修改
 * @param {Object} currentSnapshot - 当前快照
 * @param {Object} originalSnapshot - 原始快照
 * @returns {Boolean} 是否被修改（true = 有修改，false = 无修改）
 */
export function isPipelineModified (currentSnapshot, originalSnapshot) {
    // 缺少快照时认为未修改
    if (!currentSnapshot || !originalSnapshot) return false
    
    // 深度比对快照，返回是否不相等
    return !isDeepEqual(currentSnapshot, originalSnapshot)
}
