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
                    permissionData: {
                        projectId: projectId,
                        resourceType: ENV_RESOURCE_TYPE,
                        resourceCode: envHashId,
                        action: ENV_RESOURCE_ACTION.EDIT
                    }
                }"
                @click="handleAddStrategy"
            >
                {{ $t('environment.scheduling.addStrategy') }}
            </bk-button>
            <bk-button
                :disabled="strategyList.length < 2"
                @click="handleToggleSortMode"
            >
                {{ isSortMode ? $t('environment.scheduling.finishSort') : $t('environment.scheduling.adjustSort') }}
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
                    @end="handleDragEnd"
                    class="strategy-draggable"
                >
                    <div
                        v-for="strategy in strategyList"
                        :key="strategy.id"
                        class="strategy-item"
                        :class="{ 'is-draggable': isSortMode }"
                    >
                        <!-- 拖拽手柄 -->
                        <div
                            v-if="isSortMode"
                            class="drag-handle"
                        >
                            <i class="bk-icon icon-drag"></i>
                        </div>

                        <!-- 启用开关 -->
                        <bk-switcher
                            v-model="strategy.enabled"
                            :disabled="strategy.isDefault"
                            size="small"
                            theme="primary"
                            @change="(value) => handleToggleStrategy(strategy, value)"
                        />

                        <!-- 策略内容 -->
                        <div class="strategy-content">
                            <!-- 条件展示 -->
                            <div class="conditions-wrapper">
                                <span
                                    v-for="item in getStrategyDisplayItems(strategy)"
                                    :key="item.id"
                                    :class="item.class"
                                >
                                    <template v-if="item.type === 'connector'">and</template>
                                    <template v-else-if="item.type === 'condition'">
                                        <span
                                            class="condition-tag"
                                            :class="item.tagClass"
                                        >{{ item.label }}</span>
                                        <span class="condition-desc">{{ item.desc }}</span>
                                    </template>
                                    <template v-else-if="item.type === 'label'">
                                        <span class="label-key">{{ item.key }}</span>
                                        <span class="label-operator">{{ item.operator }}</span>
                                        <span class="label-value">{{ item.value }}</span>
                                    </template>
                                </span>
                            </div>
                        </div>

                        <!-- 操作按钮 -->
                        <div
                            class="strategy-actions"
                            v-if="!isSortMode && !strategy.isDefault"
                        >
                            <bk-button
                                text
                                theme="primary"
                                v-perm="{
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
                                    permissionData: {
                                        projectId: projectId,
                                        resourceType: ENV_RESOURCE_TYPE,
                                        resourceCode: envHashId,
                                        action: ENV_RESOURCE_ACTION.EDIT
                                    }
                                }"
                                @click="handleDeleteStrategy(strategy)"
                            >
                                <i class="bk-icon icon-delete"></i>
                            </bk-button>
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
                projectId
            } = useEnvDetail()

            const isLoading = ref(false)
            const strategyList = ref([])
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

            // 获取策略列表
            const fetchStrategyList = async () => {
                if (!envHashId.value) return
                try {
                    isLoading.value = true
                    const res = await proxy.$store.dispatch('environment/requestSchedulingStrategyList', {
                        projectId: projectId.value,
                        envHashId: envHashId.value
                    })
                    strategyList.value = res || []
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

            // 切换排序模式
            const handleToggleSortMode = () => {
                if (isSortMode.value) {
                    // 退出排序模式时保存顺序
                    saveStrategyOrder()
                }
                isSortMode.value = !isSortMode.value
            }

            // 拖拽结束
            const handleDragEnd = () => {
                // 拖拽结束时可以选择立即保存或等用户点击完成排序
            }

            // 保存策略顺序
            const saveStrategyOrder = async () => {
                try {
                    const orderIds = strategyList.value.map(s => s.id)
                    await proxy.$store.dispatch('environment/updateSchedulingStrategyOrder', {
                        projectId: projectId.value,
                        envHashId: envHashId.value,
                        params: { orderIds }
                    })
                    proxy.$bkMessage({
                        theme: 'success',
                        message: proxy.$t('environment.scheduling.sortSaved')
                    })
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
                        enabled: value
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

            // 获取条件样式类
            const getConditionClass = (condition) => {
                const classMap = {
                    affinity: 'condition-affinity',
                    idleNode: 'condition-idle',
                    availableNode: 'condition-available'
                }
                return classMap[condition.type] || ''
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
                    '==': '==',
                    '!=': '!=',
                    'in': 'in',
                    'not_in': 'not in'
                }
                return operatorMap[operator] || operator
            }

            // 获取策略展示项（展平条件和标签）
            const getStrategyDisplayItems = (strategy) => {
                const items = []
                const conditions = strategy.conditions || []
                const labels = strategy.labels || []

                // 添加条件
                conditions.forEach((condition, idx) => {
                    if (idx > 0) {
                        items.push({
                            id: `${strategy.id}-c-conn-${idx}`,
                            type: 'connector',
                            class: 'condition-connector'
                        })
                    }
                    items.push({
                        id: `${strategy.id}-c-${idx}`,
                        type: 'condition',
                        class: 'condition-item',
                        tagClass: getConditionClass(condition),
                        label: getConditionLabel(condition),
                        desc: getConditionDesc(condition)
                    })
                })

                // 添加标签
                labels.forEach((label, idx) => {
                    if (conditions.length > 0 || idx > 0) {
                        items.push({
                            id: `${strategy.id}-l-conn-${idx}`,
                            type: 'connector',
                            class: 'condition-connector'
                        })
                    }
                    items.push({
                        id: `${strategy.id}-l-${idx}`,
                        type: 'label',
                        class: 'label-item',
                        key: label.key,
                        operator: getOperatorLabel(label.operator),
                        value: label.value
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
                ENV_RESOURCE_ACTION,
                ENV_RESOURCE_TYPE,

                // function
                fetchStrategyList,
                handleAddStrategy,
                handleEditStrategy,
                handleDeleteStrategy,
                handleSaveStrategy,
                handleCancelStrategy,
                handleToggleSortMode,
                handleDragEnd,
                handleToggleStrategy,
                getConditionClass,
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
    }

    .strategy-draggable {
        display: flex;
        flex-direction: column;
        gap: 12px;
    }

    .strategy-item {
        display: flex;
        align-items: flex-start;
        padding: 12px 16px;
        background: #fff;
        border: 1px solid #DCDEE5;
        border-radius: 2px;
        transition: all 0.2s ease-in-out;

        &:hover {
            .strategy-actions {
                opacity: 1;
            }
        }

        &.is-draggable {
            cursor: move;
            border-left: 3px solid #3A84FF;

            &:hover {
                border-color: #3A84FF;
                box-shadow: 0 2px 6px rgba(58, 132, 255, 0.15);
            }
        }

        .drag-handle {
            display: flex;
            align-items: center;
            justify-content: center;
            width: 16px;
            height: 22px;
            margin-right: 12px;
            cursor: grab;
            color: #979BA5;

            &:hover {
                color: #3A84FF;
            }

            &:active {
                cursor: grabbing;
            }

            .bk-icon {
                font-size: 14px;
            }
        }

        .bk-switcher {
            flex-shrink: 0;
            margin-right: 16px;
            margin-top: 1px;
        }

        .strategy-content {
            flex: 1;
            min-width: 0;
        }

        .conditions-wrapper {
            display: flex;
            align-items: center;
            flex-wrap: wrap;
            gap: 8px;
        }

        .condition-connector {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            min-width: 32px;
            height: 22px;
            padding: 0 8px;
            background: #F5F7FA;
            border-radius: 2px;
            font-size: 12px;
            color: #FF9C01;
            font-weight: 500;
        }

        .condition-item {
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .condition-tag {
            display: inline-flex;
            align-items: center;
            height: 22px;
            padding: 0 8px;
            border-radius: 2px;
            font-size: 12px;
            white-space: nowrap;

            &.condition-affinity {
                color: #3A84FF;
                background: #E1ECFF;
            }

            &.condition-idle,
            &.condition-available {
                color: #63656E;
                background: #F0F1F5;
            }
        }

        .condition-desc {
            font-size: 12px;
            color: #979BA5;
            white-space: nowrap;
        }

        .label-item {
            display: inline-flex;
            align-items: center;
            height: 22px;
            padding: 0 8px;
            background: #F5F7FA;
            border: 1px solid #DCDEE5;
            border-radius: 2px;
            gap: 6px;

            .label-key {
                font-size: 12px;
                color: #313238;
                font-weight: 500;
            }

            .label-operator {
                font-size: 12px;
                color: #979BA5;
            }

            .label-value {
                font-size: 12px;
                color: #313238;
            }
        }

        .strategy-actions {
            display: flex;
            align-items: center;
            gap: 4px;
            margin-left: 16px;
            flex-shrink: 0;
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
}
</style>
