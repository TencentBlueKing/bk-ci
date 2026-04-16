<template>
    <div class="scheduling-strategy-container">
        <!-- 顶部提示信息 -->
        <bk-alert
            type="info"
            :title="$t('environment.scheduling.alertTips')"
            :closable="false"
            class="strategy-alert"
        />

        <!-- 操作区域 -->
        <div class="operation-area">
            <bk-button
                theme="primary"
                icon="plus"
                v-perm="{
                    hasPermission: currentEnv.canEdit,
                    disablePermissionApi: true,
                    permissionData: {
                        projectId: projectId,
                        resourceType: ENV_RESOURCE_TYPE,
                        resourceCode: envHashId,
                        action: ENV_RESOURCE_ACTION.EDIT
                    }
                }"
                :disabled="isSortMode"
                @click="handleAddStrategy"
            >
                {{ $t('environment.scheduling.addStrategy') }}
            </bk-button>
            <template v-if="isSortMode">
                <bk-button
                    theme="primary"
                    outline
                    @click="handleSaveSort"
                >
                    {{ $t('environment.scheduling.saveSort') }}
                </bk-button>
                <bk-button
                    @click="handleCancelSort"
                >
                    {{ $t('environment.cancel') }}
                </bk-button>
            </template>
            <bk-button
                v-else
                :disabled="strategyList.length < 2"
                @click="handleStartSort"
            >
                {{ $t('environment.scheduling.adjustSort') }}
            </bk-button>
        </div>

        <!-- 策略列表 -->
        <div
            class="strategy-list"
            v-bkloading="{ isLoading }"
        >
            <template v-if="strategyList.length">
                <draggable
                    v-model="strategyList"
                    :disabled="!isSortMode"
                    handle=".drag-handle"
                    class="strategy-draggable"
                >
                    <div
                        v-for="strategy in strategyList"
                        :key="strategy.id"
                        class="strategy-item"
                        :class="{ 'is-draggable': isSortMode }"
                    >
                        <!-- 头部区域：开关 + 策略名称 + 操作按钮 -->
                        <div class="strategy-header">
                            <div class="header-left">
                                <div
                                    v-if="isSortMode"
                                    class="drag-handle"
                                >
                                    <i class="bk-icon icon-grag-fill"></i>
                                </div>
                                <bk-switcher
                                    v-model="strategy.enabled"
                                    size="small"
                                    theme="primary"
                                    @change="(value) => handleToggleStrategy(strategy, value)"
                                />
                            </div>
                            <!-- 操作按钮 -->
                            <div
                                class="strategy-actions"
                                v-if="!isSortMode && strategy.strategyType !== 'DEFAULT'"
                            >
                                <bk-button
                                    text
                                    theme="primary"
                                    class="mr5"
                                    v-perm="{
                                        hasPermission: currentEnv.canEdit,
                                        disablePermissionApi: true,
                                        permissionData: {
                                            projectId: projectId,
                                            resourceType: ENV_RESOURCE_TYPE,
                                            resourceCode: envHashId,
                                            action: ENV_RESOURCE_ACTION.EDIT
                                        }
                                    }"
                                    @click="handleEditStrategy(strategy)"
                                >
                                    <i class="bk-icon icon-edit-line"></i>
                                </bk-button>
                                <bk-button
                                    text
                                    v-perm="{
                                        hasPermission: currentEnv.canDelete,
                                        disablePermissionApi: true,
                                        permissionData: {
                                            projectId: projectId,
                                            resourceType: ENV_RESOURCE_TYPE,
                                            resourceCode: envHashId,
                                            action: ENV_RESOURCE_ACTION.DELETE
                                        }
                                    }"
                                    @click="handleDeleteStrategy(strategy)"
                                >
                                    <i class="bk-icon icon-delete"></i>
                                </bk-button>
                            </div>
                        </div>

                        <!-- 内容区域：策略条件 -->
                        <div class="strategy-content">
                            <div
                                class="conditions-wrapper"
                                :class="{ 'has-multiple': getStrategyDisplayItems(strategy).length > 1 }"
                            >
                                <!-- 左侧 and 标签区域（多条件时显示） -->
                                <div
                                    v-if="getStrategyDisplayItems(strategy).length > 1"
                                    class="and-connector"
                                >
                                    <span class="connector-tag">and</span>
                                </div>

                                <!-- 右侧条件列表 -->
                                <div class="conditions-list">
                                    <div
                                        v-for="item in getStrategyDisplayItems(strategy)"
                                        :key="item.id"
                                        class="condition-row"
                                    >
                                        <!-- 条件内容 -->
                                        <template v-if="item.type === 'condition'">
                                            <span class="condition-tag">{{ item.label }}</span>
                                            <span class="condition-desc">{{ item.desc }}</span>
                                        </template>

                                        <!-- 标签内容 -->
                                        <template v-else-if="item.type === 'label'">
                                            <span class="label-key-tag">{{ item.key }}</span>
                                            <span class="label-operator">{{ item.operator }}</span>
                                            <span class="label-value">{{ Array.isArray(item.value) ? item.value.join(', ') : item.value }}</span>
                                        </template>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </draggable>
            </template>

            <!-- 空状态 -->
            <template v-else-if="!isLoading">
                <bk-exception
                    type="empty"
                    scene="part"
                >
                    <span>{{ $t('environment.noData') }}</span>
                </bk-exception>
            </template>
        </div>

        <!-- 新增/编辑策略弹窗 -->
        <strategy-dialog
            :value="showStrategyDialog"
            :is-edit="isEditMode"
            :strategy-data="currentStrategy"
            @confirm="handleSaveStrategy"
            @cancel="handleCancelStrategy"
        />
    </div>
</template>

<script>
    import { ref, watch, onMounted } from 'vue'
    import draggable from 'vuedraggable'
    import {
        ENV_RESOURCE_ACTION,
        ENV_RESOURCE_TYPE
    } from '@/utils/permission'
    import useInstance from '@/hooks/useInstance'
    import useEnvDetail from '@/hooks/useEnvDetail'
    import StrategyDialog from './StrategyDialog.vue'

    export default {
        name: 'SchedulingStrategy',
        components: {
            draggable,
            StrategyDialog
        },
        setup () {
            const { proxy } = useInstance()
            const {
                envHashId,
                projectId,
                currentEnv
            } = useEnvDetail()

            const isLoading = ref(false)
            const strategyList = ref([])
            const originalStrategyList = ref([]) // 排序前的原始顺序备份
            const isSortMode = ref(false)
            const showStrategyDialog = ref(false)
            const isEditMode = ref(false)
            const currentStrategy = ref(null)

            // 监听 envHashId 变化，重新获取数据
            watch(() => envHashId.value, () => {
                if (envHashId.value) {
                    fetchStrategyList()
                }
            })

            const mapStrategyData = (strategy) => {
                const conditions = []
                
                // 根据 scope 判断是否有亲和性（PRE_BUILD 表示最近使用，即亲和性）
                if (strategy.scope === 'PRE_BUILD') {
                    conditions.push({ type: 'affinity' })
                }
                
                // 根据 nodeRule 判断节点规则
                if (strategy.nodeRule === 'IDLE') {
                    conditions.push({ type: 'idleNode' })
                } else if (strategy.nodeRule === 'AVAILABLE') {
                    conditions.push({ type: 'availableNode' })
                }
                
                // 如果 conditions 字段已存在且有数据（自定义策略），使用原始数据
                if (strategy.conditions && Array.isArray(strategy.conditions) && strategy.conditions.length > 0) {
                    conditions.length = 0
                    conditions.push(...strategy.conditions)
                }

                // labelSelector 转换为前端 labels 格式
                // 接口返回的格式: { tagKeyId, tagKeyName, op, tagValue: [{ tagValueId, tagValue }] }
                // 前端格式: { key, keyName, operator, value, valueName }
                const labelSelector = strategy.labelSelector || []
                const labels = labelSelector.map(item => {
                    const tagValues = item.tagValue || []
                    const valueIds = tagValues.map(v => v.tagValueId)
                    const valueNames = tagValues.map(v => v.tagValue)
                    
                    return {
                        key: item.tagKeyId,
                        keyName: item.tagKeyName || item.tagKeyId,
                        operator: item.op,
                        // 如果是 IN 操作符，保持数组；否则取第一个值
                        value: item.op === 'IN' ? valueIds : (valueIds[0] || ''),
                        // 用于列表显示的值名称
                        valueName: item.op === 'IN' ? valueNames : (valueNames[0] || '')
                    }
                })

                return {
                    ...strategy,
                    conditions,
                    labels
                }
            }

            // 获取策略列表
            const fetchStrategyList = async () => {
                if (!envHashId.value) return
                try {
                    isLoading.value = true
                    const res = await proxy.$store.dispatch('environment/requestSchedulingStrategyList', {
                        projectId: projectId.value,
                        envHashId: envHashId.value
                    })
                    // 映射后端数据格式
                    strategyList.value = (res || []).map(mapStrategyData)
                } catch (err) {
                    console.error('获取调度策略列表失败:', err)
                    proxy.$bkMessage({
                        theme: 'error',
                        message: err.message || proxy.$t('environment.scheduling.fetchFailed')
                    })
                } finally {
                    isLoading.value = false
                }
            }

            // 新增策略
            const handleAddStrategy = () => {
                isEditMode.value = false
                currentStrategy.value = null
                showStrategyDialog.value = true
            }

            // 编辑策略
            const handleEditStrategy = (strategy) => {
                isEditMode.value = true
                currentStrategy.value = { ...strategy }
                showStrategyDialog.value = true
            }

            // 删除策略
            const handleDeleteStrategy = (strategy) => {
                proxy.$bkInfo({
                    title: proxy.$t('environment.scheduling.deleteConfirmTitle'),
                    subTitle: proxy.$t('environment.scheduling.deleteConfirmTips'),
                    theme: 'danger',
                    confirmFn: async () => {
                        try {
                            await proxy.$store.dispatch('environment/deleteSchedulingStrategy', {
                                projectId: projectId.value,
                                envHashId: envHashId.value,
                                strategyId: strategy.id
                            })
                            proxy.$bkMessage({
                                theme: 'success',
                                message: proxy.$t('environment.successfullyDeleted')
                            })
                            fetchStrategyList()
                        } catch (err) {
                            console.error('删除策略失败:', err)
                            proxy.$bkMessage({
                                theme: 'error',
                                message: err.message
                            })
                        }
                    }
                })
            }

            // 保存策略
            const handleSaveStrategy = async (strategyData) => {
                try {
                    if (isEditMode.value) {
                        await proxy.$store.dispatch('environment/updateSchedulingStrategy', {
                            projectId: projectId.value,
                            envHashId: envHashId.value,
                            strategyId: strategyData.id,
                            params: strategyData
                        })
                    } else {
                        await proxy.$store.dispatch('environment/createSchedulingStrategy', {
                            projectId: projectId.value,
                            envHashId: envHashId.value,
                            params: strategyData
                        })
                    }
                    proxy.$bkMessage({
                        theme: 'success',
                        message: proxy.$t('environment.successfullySaved')
                    })
                    showStrategyDialog.value = false
                    fetchStrategyList()
                } catch (err) {
                    console.error('保存策略失败:', err)
                    proxy.$bkMessage({
                        theme: 'error',
                        message: err.message
                    })
                }
            }

            // 取消策略编辑
            const handleCancelStrategy = () => {
                showStrategyDialog.value = false
                currentStrategy.value = null
            }

            // 开始排序模式
            const handleStartSort = () => {
                // 备份当前顺序
                originalStrategyList.value = JSON.parse(JSON.stringify(strategyList.value))
                isSortMode.value = true
            }

            // 保存排序
            const handleSaveSort = async () => {
                await saveStrategyOrder()
                isSortMode.value = false
                originalStrategyList.value = []
            }

            // 取消排序
            const handleCancelSort = () => {
                // 恢复原来的顺序
                strategyList.value = JSON.parse(JSON.stringify(originalStrategyList.value))
                isSortMode.value = false
                originalStrategyList.value = []
            }

            // 保存策略顺序
            const saveStrategyOrder = async () => {
                try {
                    const orderedIds = strategyList.value.map(s => s.id)
                    await proxy.$store.dispatch('environment/updateSchedulingStrategyOrder', {
                        projectId: projectId.value,
                        envHashId: envHashId.value,
                        params: { orderedIds }
                    })
                    proxy.$bkMessage({
                        theme: 'success',
                        message: proxy.$t('environment.scheduling.sortSaved')
                    })
                    await fetchStrategyList()
                } catch (err) {
                    console.error('保存排序失败:', err)
                    proxy.$bkMessage({
                        theme: 'error',
                        message: err.message
                    })
                    // 失败时重新获取列表恢复原顺序
                    fetchStrategyList()
                }
            }

            // 切换策略启用状态
            const handleToggleStrategy = async (strategy, value) => {
                try {
                    await proxy.$store.dispatch('environment/toggleSchedulingStrategy', {
                        projectId: projectId.value,
                        envHashId: envHashId.value,
                        strategyId: strategy.id,
                        params: { enabled: value }
                    })
                    proxy.$bkMessage({
                        theme: 'success',
                        message: value
                            ? proxy.$t('environment.enableSuccess')
                            : proxy.$t('environment.disableSuccess')
                    })
                } catch (err) {
                    console.error('切换策略状态失败:', err)
                    // 恢复原状态
                    strategy.enabled = !value
                    proxy.$bkMessage({
                        theme: 'error',
                        message: err.message
                    })
                }
            }

            // 获取条件标签
            const getConditionLabel = (condition) => {
                const labelMap = {
                    affinity: proxy.$t('environment.scheduling.affinity'),
                    idleNode: proxy.$t('environment.scheduling.idleNode'),
                    availableNode: proxy.$t('environment.scheduling.availableNode')
                }
                return labelMap[condition.type] || condition.type
            }

            // 获取条件描述
            const getConditionDesc = (condition) => {
                const descMap = {
                    affinity: proxy.$t('environment.scheduling.affinityDesc'),
                    idleNode: proxy.$t('environment.scheduling.idleNodeDesc'),
                    availableNode: proxy.$t('environment.scheduling.availableNodeDesc')
                }
                return descMap[condition.type] || ''
            }

            // 获取操作符标签
            const getOperatorLabel = (operator) => {
                const operatorMap = {
                    'EQUAL': '==',
                    'GTE': '>=',
                    'LTE': '<=',
                    'GT': '>',
                    'LT': '<',
                    'START_WITH': 'StartWith',
                    'END_WITH': 'EndWith',
                    'CONTAINS': 'Contains',
                    'IN': 'in'
                }
                return operatorMap[operator] || operator
            }

            // 获取策略展示项
            const getStrategyDisplayItems = (strategy) => {
                const items = []
                const conditions = strategy.conditions || []
                const labels = strategy.labels || []
                const totalCount = conditions.length + labels.length

                // 添加条件
                conditions.forEach((condition, idx) => {
                    const currentIndex = idx
                    items.push({
                        id: `${strategy.id}-c-${idx}`,
                        type: 'condition',
                        label: getConditionLabel(condition),
                        desc: getConditionDesc(condition),
                        showConnector: currentIndex > 0, // 非第一个显示 and
                        hasNext: currentIndex < totalCount - 1 // 是否有下一个（用于连接线）
                    })
                })

                // 添加标签
                labels.forEach((label, idx) => {
                    const currentIndex = conditions.length + idx
                    items.push({
                        id: `${strategy.id}-l-${idx}`,
                        type: 'label',
                        key: label.keyName || label.key,
                        operator: getOperatorLabel(label.operator),
                        value: label.valueName || label.value,
                        showConnector: currentIndex > 0, // 非第一个显示 and
                        hasNext: currentIndex < totalCount - 1 // 是否有下一个
                    })
                })

                return items
            }

            onMounted(() => {
                fetchStrategyList()
            })

            return {
                // data
                isLoading,
                strategyList,
                isSortMode,
                showStrategyDialog,
                isEditMode,
                currentStrategy,
                projectId,
                envHashId,
                currentEnv,
                ENV_RESOURCE_ACTION,
                ENV_RESOURCE_TYPE,

                // function
                fetchStrategyList,
                handleAddStrategy,
                handleEditStrategy,
                handleDeleteStrategy,
                handleSaveStrategy,
                handleCancelStrategy,
                handleStartSort,
                handleSaveSort,
                handleCancelSort,
                handleToggleStrategy,
                getConditionLabel,
                getConditionDesc,
                getOperatorLabel,
                getStrategyDisplayItems
            }
        }
    }
</script>

<style lang="scss" scoped>
.scheduling-strategy-container {
    height: calc(100% - 90px);

    .strategy-alert {
        margin-bottom: 16px;
    }

    .operation-area {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 16px;
    }

    .strategy-list {
        min-height: 200px;
        max-height: calc(100vh - 400px);
        overflow-y: auto;
    }

    .strategy-draggable {
        display: flex;
        flex-direction: column;
        gap: 12px;
    }

    .strategy-item {
        display: flex;
        flex-direction: column;
        border: 1px solid #DCDEE5;
        border-radius: 2px;
        overflow: hidden;
        transition: all 0.2s ease-in-out;

        &:hover {
            .strategy-header .strategy-actions {
                opacity: 1;
            }
        }

        &.is-draggable {

            &:hover {
                border-color: #3A84FF;
                box-shadow: 0 2px 6px rgba(58, 132, 255, 0.15);
            }
        }

        // 头部区域：灰色背景
        .strategy-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 8px 16px;
            background: #F5F7FA;
            border-bottom: 1px solid #DCDEE5;

            .header-left {
                display: flex;
                align-items: center;
                gap: 8px;
            }

            .strategy-name {
                font-size: 14px;
                font-weight: 500;
                color: #313238;
            }

            .strategy-actions {
                display: flex;
                align-items: center;
                gap: 4px;
                opacity: 0;
                transition: opacity 0.2s;

                .bk-button {
                    padding: 0 4px;
                    min-width: auto;

                    .bk-icon {
                        font-size: 16px;
                        color: #979BA5;
                    }

                    &:hover {
                        .icon-edit-line {
                            color: #3A84FF;
                        }
                        .icon-delete {
                            color: #EA3636;
                        }
                    }
                }
            }
        }

        .drag-handle {
            display: flex;
            align-items: center;
            justify-content: center;
            width: 16px;
            height: 22px;
            cursor: grab;
            color: #C4C6CC;

            &:hover {
                color: #979BA5;
            }

            &:active {
                cursor: grabbing;
            }

            .bk-icon {
                font-size: 16px;
            }
        }

        // 内容区域：白色背景
        .strategy-content {
            padding: 12px 16px;
            background: #fff;
        }

        .conditions-wrapper {
            display: flex;
            align-items: flex-start;

            &.has-multiple {
                .conditions-list {
                    .condition-row {
                        position: relative;
                        padding-left: 12px;

                        // 每行左侧的横向连接线
                        &::before {
                            content: '';
                            position: absolute;
                            left: 0;
                            top: 50%;
                            width: 8px;
                            border-top: 1px dashed #DCDEE5;
                        }
                    }
                }
            }
        }

        .and-connector {
            position: relative;
            display: flex;
            align-items: center;
            justify-content: flex-start;
            width: 40px;
            flex-shrink: 0;
            align-self: stretch;

            .connector-tag {
                display: inline-flex;
                align-items: center;
                justify-content: center;
                min-width: 28px;
                height: 28px;
                padding: 0 4px;
                background: #FFF;
                border: 1px solid #FF9C01;
                border-radius: 4px;
                font-size: 11px;
                color: #FF9C01;
                font-weight: 500;
                z-index: 1;
            }

            // 从 and 标签中间延伸出去的横线
            &::before {
                content: '';
                position: absolute;
                left: 14px;
                top: 50%;
                width: 24px;
                border-top: 1px dashed #DCDEE5;
            }

            // 右侧竖线，连接所有条件
            &::after {
                content: '';
                position: absolute;
                right: 0;
                top: 11px;
                bottom: 11px;
                width: 1px;
                border-right: 1px dashed #DCDEE5;
            }
        }

        .conditions-list {
            display: flex;
            flex-direction: column;
            gap: 4px;
        }

        .condition-row {
            display: flex;
            align-items: center;
            gap: 8px;
            min-height: 22px;
        }


        .condition-tag {
            display: inline-flex;
            align-items: center;
            height: 22px;
            padding: 0 8px;
            border-radius: 2px;
            font-size: 12px;
            white-space: nowrap;
            color: #63656E;
            background: #F0F1F5;
        }

        .condition-desc {
            font-size: 12px;
            color: #979BA5;
            white-space: nowrap;
        }

        .label-key-tag {
            display: inline-flex;
            align-items: center;
            height: 22px;
            padding: 0 8px;
            background: #F0F1F5;
            border-radius: 2px;
            font-size: 12px;
            color: #63656E;
        }

        .label-operator {
            font-size: 12px;
            color: #979BA5;
            margin: 0 6px;
        }

        .label-value {
            font-size: 12px;
            color: #313238;
        }
    }
}
</style>
