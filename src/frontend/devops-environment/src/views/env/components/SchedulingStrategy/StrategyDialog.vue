<template>
    <bk-dialog
        :value="value"
        :title="dialogTitle"
        :width="640"
        :show-footer="true"
        :mask-close="false"
        header-position="left"
        @cancel="handleCancel"
    >
        <div class="strategy-dialog-content">
            <!-- 亲和性选项 -->
            <div class="form-section">
                <bk-checkbox
                    v-model="formData.affinityEnabled"
                    class="strategy-checkbox"
                >
                    <span class="checkbox-label">{{ $t('environment.scheduling.affinity') }}</span>
                    <span class="checkbox-desc">{{ $t('environment.scheduling.affinityDesc') }}</span>
                </bk-checkbox>
            </div>

            <!-- 节点开发负载 -->
            <div class="form-section">
                <bk-checkbox
                    v-model="formData.nodeLoadEnabled"
                    class="strategy-checkbox"
                >
                    <span class="checkbox-label">{{ $t('environment.scheduling.nodeLoad') }}</span>
                </bk-checkbox>

                <div
                    class="sub-options"
                    v-if="formData.nodeLoadEnabled"
                >
                    <bk-radio-group v-model="formData.nodeLoadType">
                        <div class="radio-item">
                            <bk-radio value="idleNode">
                                <span class="radio-label">{{ $t('environment.scheduling.idleNode') }}</span>
                                <span class="radio-desc">{{ $t('environment.scheduling.idleNodeDesc') }}</span>
                            </bk-radio>
                        </div>
                        <div class="radio-item">
                            <bk-radio value="availableNode">
                                <span class="radio-label">{{ $t('environment.scheduling.availableNode') }}</span>
                                <span class="radio-desc">{{ $t('environment.scheduling.availableNodeDesc') }}</span>
                            </bk-radio>
                        </div>
                    </bk-radio-group>

                    <!-- 编辑模式下的节点负载提示 -->
                    <p
                        v-if="isEdit && formData.nodeLoadEnabled"
                        class="edit-tips"
                    >
                        {{ $t('environment.scheduling.nodeLoadEditTips') }}
                    </p>
                </div>
            </div>

            <!-- 标签选项 -->
            <div class="form-section labels-form-section">
                <bk-checkbox
                    v-model="formData.labelsEnabled"
                    class="strategy-checkbox"
                >
                    <span class="checkbox-label">{{ $t('environment.scheduling.label') }}</span>
                </bk-checkbox>

                <div
                    class="sub-options labels-section"
                    v-if="formData.labelsEnabled"
                >
                    <p class="labels-tips">{{ $t('environment.scheduling.labelTips') }}</p>

                    <!-- 标签表格头部 -->
                    <div
                        class="labels-header"
                        v-if="formData.labels.length"
                    >
                        <span class="header-item label-key-header">{{ $t('environment.scheduling.selectLabelKey') }}</span>
                        <span class="header-item label-operator-header"></span>
                        <span class="header-item label-value-header">{{ $t('environment.scheduling.inputLabelValue') }}</span>
                        <span class="header-item label-actions-header"></span>
                    </div>

                    <!-- 标签列表 -->
                    <div class="labels-list">
                        <div
                            v-for="(label, index) in formData.labels"
                            :key="index"
                            class="label-row"
                        >
                            <bk-select
                                v-model="label.key"
                                class="label-key-select"
                                :placeholder="$t('environment.scheduling.selectLabelKey')"
                                :clearable="false"
                                searchable
                                @change="handleLabelKeyChange(index)"
                            >
                                <bk-option
                                    v-for="tagKey in tagKeyList"
                                    :key="tagKey.id"
                                    :id="tagKey.id"
                                    :name="tagKey.name"
                                />
                            </bk-select>

                            <bk-select
                                v-model="label.operator"
                                class="label-operator-select"
                                :clearable="false"
                            >
                                <bk-option
                                    id="=="
                                    name="=="
                                />
                                <bk-option
                                    id="!="
                                    name="!="
                                />
                                <bk-option
                                    id="in"
                                    name="in"
                                />
                            </bk-select>

                            <bk-input
                                v-model="label.value"
                                class="label-value-input"
                                :placeholder="$t('environment.scheduling.inputLabelValue')"
                            />

                            <div class="label-actions">
                                <bk-button
                                    text
                                    class="label-action-btn delete-btn"
                                    :disabled="formData.labels.length <= 1"
                                    @click="handleRemoveLabel(index)"
                                >
                                    <i class="bk-icon icon-delete"></i>
                                </bk-button>
                                <bk-button
                                    text
                                    class="label-action-btn add-btn"
                                    @click="handleAddLabel"
                                >
                                    <i class="bk-icon icon-plus-circle"></i>
                                </bk-button>
                            </div>
                        </div>

                        <!-- 无标签时显示添加按钮 -->
                        <bk-button
                            v-if="!formData.labels.length"
                            text
                            theme="primary"
                            icon="plus"
                            @click="handleAddLabel"
                        >
                            {{ $t('environment.scheduling.addLabel') }}
                        </bk-button>
                    </div>
                </div>
            </div>
        </div>

        <template slot="footer">
            <bk-button
                theme="primary"
                :loading="isSaving"
                @click="handleConfirm"
            >
                {{ $t('environment.save') }}
            </bk-button>
            <bk-button
                :disabled="isSaving"
                @click="handleCancel"
            >
                {{ $t('environment.cancel') }}
            </bk-button>
        </template>
    </bk-dialog>
</template>

<script>
    import { ref, computed, watch } from 'vue'
    import useInstance from '@/hooks/useInstance'
    import useEnvDetail from '@/hooks/useEnvDetail'

    export default {
        name: 'StrategyDialog',
        props: {
            value: {
                type: Boolean,
                default: false
            },
            isEdit: {
                type: Boolean,
                default: false
            },
            strategyData: {
                type: Object,
                default: null
            }
        },
        emits: ['confirm', 'cancel'],
        setup (props, { emit }) {
            const { proxy } = useInstance()
            const {
                projectId
            } = useEnvDetail()

            const isSaving = ref(false)
            const tagKeyList = ref([])
            const tagValuesMap = ref({})

            const defaultFormData = {
                affinityEnabled: false,
                nodeLoadEnabled: false,
                nodeLoadType: 'idleNode',
                labelsEnabled: false,
                labels: []
            }

            const formData = ref({ ...defaultFormData })

            const dialogTitle = computed(() => {
                return props.isEdit
                    ? proxy.$t('environment.scheduling.editStrategy')
                    : proxy.$t('environment.scheduling.addStrategy')
            })

            // 监听弹窗显示状态
            watch(() => props.value, (newVal) => {
                if (newVal) {
                    if (props.isEdit && props.strategyData) {
                        // 编辑模式，初始化表单数据
                        initFormData(props.strategyData)
                    } else {
                        // 新增模式，重置表单
                        resetForm()
                    }
                    // 获取标签列表
                    fetchTagList()
                }
            })

            // 初始化表单数据（编辑模式）
            const initFormData = (data) => {
                const conditions = data.conditions || []

                formData.value = {
                    id: data.id,
                    affinityEnabled: conditions.some(c => c.type === 'affinity'),
                    nodeLoadEnabled: conditions.some(c => c.type === 'idleNode' || c.type === 'availableNode'),
                    nodeLoadType: conditions.find(c => c.type === 'idleNode' || c.type === 'availableNode')?.type || 'idleNode',
                    labelsEnabled: !!(data.labels && data.labels.length),
                    labels: data.labels ? data.labels.map(l => ({ ...l })) : []
                }
            }

            // 重置表单
            const resetForm = () => {
                formData.value = {
                    ...defaultFormData,
                    labels: []
                }
            }

            // 获取标签列表
            const fetchTagList = async () => {
                try {
                    const res = await proxy.$store.dispatch('environment/requestNodeTagList', projectId.value)
                    tagKeyList.value = (res || []).map(tag => ({
                        id: tag.tagKey,
                        name: tag.tagKey,
                        values: tag.tagValues || []
                    }))
                    // 构建标签值映射
                    tagValuesMap.value = tagKeyList.value.reduce((acc, tag) => {
                        acc[tag.id] = tag.values
                        return acc
                    }, {})
                } catch (err) {
                    console.error('获取标签列表失败:', err)
                }
            }

            // 标签键变化时清空值
            const handleLabelKeyChange = (index) => {
                formData.value.labels[index].value = ''
            }

            // 添加标签
            const handleAddLabel = () => {
                formData.value.labels.push({
                    key: '',
                    operator: '==',
                    value: ''
                })
            }

            // 移除标签
            const handleRemoveLabel = (index) => {
                if (formData.value.labels.length > 1) {
                    formData.value.labels.splice(index, 1)
                }
            }

            // 确认提交
            const handleConfirm = async () => {
                // 表单验证
                if (formData.value.labelsEnabled) {
                    const invalidLabel = formData.value.labels.find(l => !l.key || !l.value)
                    if (formData.value.labels.length && invalidLabel) {
                        proxy.$bkMessage({
                            theme: 'warning',
                            message: proxy.$t('environment.scheduling.labelRequired')
                        })
                        return
                    }
                }

                // 至少选择一个条件
                if (!formData.value.affinityEnabled && !formData.value.nodeLoadEnabled && !formData.value.labelsEnabled) {
                    proxy.$bkMessage({
                        theme: 'warning',
                        message: proxy.$t('environment.scheduling.atLeastOneCondition')
                    })
                    return
                }

                try {
                    isSaving.value = true

                    // 构建提交数据
                    const conditions = []

                    if (formData.value.affinityEnabled) {
                        conditions.push({ type: 'affinity' })
                    }

                    if (formData.value.nodeLoadEnabled) {
                        conditions.push({ type: formData.value.nodeLoadType })
                    }

                    const submitData = {
                        id: formData.value.id,
                        conditions,
                        labels: formData.value.labelsEnabled ? formData.value.labels.filter(l => l.key && l.value) : []
                    }

                    emit('confirm', submitData)
                } catch (err) {
                    console.error('保存失败:', err)
                } finally {
                    isSaving.value = false
                }
            }

            // 取消
            const handleCancel = () => {
                emit('cancel')
            }

            return {
                // data
                formData,
                isSaving,
                dialogTitle,
                tagKeyList,

                // function
                handleLabelKeyChange,
                handleAddLabel,
                handleRemoveLabel,
                handleConfirm,
                handleCancel
            }
        }
    }
</script>

<style lang="scss" scoped>
.strategy-dialog-content {
    max-height: 500px;
    overflow-y: auto;

    .form-section {
        padding: 16px 0;
        border-bottom: 1px solid #EAEBF0;

        &:first-child {
            padding-top: 0;
        }

        &:last-child {
            border-bottom: none;
            padding-bottom: 0;
        }

        &.labels-form-section {
            padding-bottom: 8px;
        }

        .strategy-checkbox {
            display: flex;
            align-items: center;

            ::v-deep .bk-checkbox-text {
                display: flex;
                align-items: center;
            }
        }

        .checkbox-label {
            font-size: 14px;
            color: #313238;
            font-weight: 500;
        }

        .checkbox-desc {
            font-size: 12px;
            color: #979BA5;
            margin-left: 8px;
        }

        .sub-options {
            margin-top: 12px;
            margin-left: 24px;
            padding-left: 16px;
            border-left: 2px solid #EAEBF0;

            .bk-form-radio-group {
                display: flex;
                flex-direction: column;
            }

            .radio-item {
                margin-bottom: 12px;

                &:last-child {
                    margin-bottom: 0;
                }

                .bk-form-radio {
                    display: flex;
                    align-items: flex-start;

                    ::v-deep .bk-radio-text {
                        display: flex;
                        align-items: center;
                        flex-wrap: wrap;
                    }
                }
            }

            .radio-label {
                font-size: 14px;
                color: #313238;
            }

            .radio-desc {
                font-size: 12px;
                color: #979BA5;
                margin-left: 8px;
            }

            .edit-tips {
                margin-top: 8px;
                font-size: 12px;
                color: #3A84FF;
            }
        }

        .labels-section {
            .labels-tips {
                font-size: 12px;
                color: #FF9C01;
                margin-bottom: 12px;
                line-height: 18px;
            }

            .labels-header {
                display: flex;
                align-items: center;
                gap: 8px;
                margin-bottom: 8px;
                padding: 0 4px;

                .header-item {
                    font-size: 12px;
                    color: #313238;

                    &.label-key-header {
                        width: 140px;
                    }

                    &.label-operator-header {
                        width: 80px;
                    }

                    &.label-value-header {
                        flex: 1;
                    }

                    &.label-actions-header {
                        width: 56px;
                    }
                }
            }

            .labels-list {
                .label-row {
                    display: flex;
                    align-items: center;
                    gap: 8px;
                    margin-bottom: 12px;

                    &:last-child {
                        margin-bottom: 0;
                    }

                    .label-key-select {
                        width: 140px;
                        flex-shrink: 0;
                    }

                    .label-operator-select {
                        width: 80px;
                        flex-shrink: 0;
                    }

                    .label-value-input {
                        flex: 1;
                    }

                    .label-actions {
                        display: flex;
                        align-items: center;
                        gap: 4px;
                        width: 56px;
                        flex-shrink: 0;
                    }

                    .label-action-btn {
                        padding: 0 4px;
                        min-width: auto;

                        .bk-icon {
                            font-size: 18px;
                            color: #979BA5;
                        }

                        &.delete-btn:not([disabled]):hover .bk-icon {
                            color: #EA3636;
                        }

                        &.add-btn:hover .bk-icon {
                            color: #3A84FF;
                        }

                        &[disabled] {
                            cursor: not-allowed;
                            .bk-icon {
                                color: #DCDEE5;
                            }
                        }
                    }
                }
            }
        }
    }
}
</style>

<style lang="scss">
/* 弹窗全局样式调整 */
.bk-dialog-wrapper .bk-dialog-header {
    padding-bottom: 16px;
}
</style>
