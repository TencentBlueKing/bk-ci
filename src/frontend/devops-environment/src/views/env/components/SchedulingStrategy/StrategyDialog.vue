<template>
    <bk-dialog
        :value="value"
        :title="dialogTitle"
        :width="800"
        :show-footer="true"
        :mask-close="false"
        header-position="left"
        @cancel="handleCancel"
    >
        <div class="strategy-dialog-content">
            <!-- 策略名称 -->
            <div class="form-section strategy-name-section">
                <span class="strategy-name-label">{{ $t('environment.scheduling.strategyName') }}</span>
                <bk-input
                    v-model="formData.strategyName"
                    class="strategy-name-input"
                    :placeholder="$t('environment.scheduling.strategyNamePlaceholder')"
                    :maxlength="128"
                />
            </div>

            <!-- 亲和性选项 -->
            <div class="form-section">
                <bk-checkbox
                    v-model="formData.affinityEnabled"
                    class="strategy-checkbox"
                >
                    <span class="checkbox-tag">{{ $t('environment.scheduling.affinity') }}</span>
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
                                <span class="radio-tag">{{ $t('environment.scheduling.idleNode') }}</span>
                                <span class="radio-desc">{{ $t('environment.scheduling.idleNodeDesc') }}</span>
                            </bk-radio>
                        </div>
                        <div class="radio-item">
                            <bk-radio value="availableNode">
                                <span class="radio-tag">{{ $t('environment.scheduling.availableNode') }}</span>
                                <span class="radio-desc">{{ $t('environment.scheduling.availableNodeDesc') }}</span>
                            </bk-radio>
                        </div>
                    </bk-radio-group>
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
                    <!-- 标签表格 -->
                    <table class="labels-table">
                        <thead>
                            <tr>
                                <th class="label-key-col">
                                    <span class="required">*</span>
                                    {{ $t('environment.labelKey') }}
                                </th>
                                <th class="label-value-col">
                                    <span class="required">*</span>
                                    {{ $t('environment.labelValue') }}
                                </th>
                                <th class="label-actions-col"></th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr
                                v-for="(label, index) in formData.labels"
                                :key="index"
                            >
                                <td class="label-key-col">
                                    <bk-select
                                        v-model="label.key"
                                        class="label-key-select"
                                        :placeholder="$t('environment.pleaseSelect')"
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
                                </td>
                                <td class="label-value-col">
                                    <div class="value-cell">
                                        <bk-select
                                            v-model="label.operator"
                                            class="label-operator-select"
                                            :clearable="false"
                                            @change="handleOperatorChange(index)"
                                        >
                                            <bk-option
                                                v-for="op in operatorList"
                                                :key="op.id"
                                                :id="op.id"
                                                :name="op.name"
                                            />
                                        </bk-select>
                                        <!-- 有预定义值列表且非输入类操作符时显示下拉选择 -->
                                        <bk-select
                                            v-if="shouldShowSelect(label.key, label.operator)"
                                            v-model="label.value"
                                            class="label-value-input"
                                            :placeholder="$t('environment.pleaseEnter')"
                                            :multiple="isMultiValueOperator(label.operator)"
                                            :clearable="true"
                                            searchable
                                        >
                                            <bk-option
                                                v-for="val in getPreDefinedValues(label.key)"
                                                :key="val.id"
                                                :id="val.name"
                                                :name="val.name"
                                            />
                                        </bk-select>
                                        <!-- 无预定义值或输入类操作符时显示输入框 -->
                                        <bk-input
                                            v-else
                                            v-model="label.value"
                                            class="label-value-input"
                                            :placeholder="$t('environment.pleaseEnter')"
                                        />
                                    </div>
                                </td>
                                <td class="label-actions-col">
                                    <div class="label-actions">
                                        <span
                                            class="action-icon add-icon"
                                            @click="handleAddLabel"
                                        >
                                            <i class="bk-icon icon-plus-circle"></i>
                                        </span>
                                        <span
                                            class="action-icon delete-icon"
                                            :class="{ disabled: formData.labels.length <= 1 }"
                                            @click="formData.labels.length > 1 && handleRemoveLabel(index)"
                                        >
                                            <i class="bk-icon icon-minus-circle"></i>
                                        </span>
                                    </div>
                                </td>
                            </tr>
                        </tbody>
                    </table>
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

            // 操作符列表
            const operatorList = [
                { id: 'EQUAL', name: '==' },
                { id: 'GTE', name: '>=' },
                { id: 'LTE', name: '<=' },
                { id: 'GT', name: '>' },
                { id: 'LT', name: '<' },
                { id: 'START_WITH', name: 'StartWith' },
                { id: 'END_WITH', name: 'EndWith' },
                { id: 'CONTAINS', name: 'Contains' },
                { id: 'IN', name: 'in' }
            ]

            const defaultFormData = {
                strategyName: '',
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
            watch(() => props.value, async (newVal) => {
                if (newVal) {
                    // 先获取标签列表数据，确保选项加载完成后再初始化表单
                    await fetchTagList()
                    
                    if (props.isEdit && props.strategyData) {
                        // 编辑模式，初始化表单数据
                        initFormData(props.strategyData)
                    } else {
                        // 新增模式，重置表单
                        resetForm()
                    }
                }
            })

            // 监听标签勾选状态，勾选时自动添加一条空数据
            watch(() => formData.value.labelsEnabled, (newVal) => {
                if (newVal && formData.value.labels.length === 0) {
                    formData.value.labels.push({
                        key: '',
                        operator: 'EQUAL',
                        value: ''
                    })
                }
            })

            // 初始化表单数据（编辑模式）
            const initFormData = (data) => {
                const conditions = data.conditions || []
                const labels = data.labels || []

                formData.value = {
                    id: data.id,
                    strategyName: data.strategyName || '',
                    enabled: data.enabled !== false, // 保留启用状态
                    affinityEnabled: conditions.some(c => c.type === 'affinity'),
                    nodeLoadEnabled: conditions.some(c => c.type === 'idleNode' || c.type === 'availableNode'),
                    nodeLoadType: conditions.find(c => c.type === 'idleNode' || c.type === 'availableNode')?.type || 'idleNode',
                    labelsEnabled: !!(labels && labels.length),
                    labels: labels.map(l => ({
                        key: l.key,
                        keyName: l.keyName,
                        operator: l.operator,
                        value: l.value
                    }))
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
                    // 转换为标签键选项列表
                    tagKeyList.value = (res || []).map(tag => ({
                        id: tag.tagKeyId,
                        name: tag.tagKeyName
                    }))
                    // 构建标签值映射表
                    const valuesMap = {}
                    ;(res || []).forEach(tag => {
                        if (tag.tagValues && tag.tagValues.length > 0) {
                            valuesMap[tag.tagKeyId] = tag.tagValues.map(val => ({
                                id: val.tagValueId,
                                name: val.tagValueName
                            }))
                        }
                    })
                    tagValuesMap.value = valuesMap
                } catch (err) {
                    console.error('获取标签列表失败:', err)
                }
            }

            // 标签键变化时清空值并保存键名
            const handleLabelKeyChange = (index) => {
                const label = formData.value.labels[index]
                // 保存标签键名称
                label.keyName = tagKeyList.value.find(t => t.id === label.key)?.name || ''
                // 根据当前操作符类型决定清空为空字符串还是空数组
                if (isMultiValueOperator(label.operator) && !isInputOperator(label.operator)) {
                    label.value = []
                } else {
                    label.value = ''
                }
            }

            // 操作符变化时处理值格式
            const handleOperatorChange = (index) => {
                const label = formData.value.labels[index]
                
                // 切换到输入类操作符时，清空值（因为选择框的ID值与输入框的字符串值不兼容）
                if (isInputOperator(label.operator)) {
                    label.value = ''
                    return
                }
                
                // 如果从非多值操作符切换到多值操作符，且有预定义值，需要将值转为数组
                if (isMultiValueOperator(label.operator) && hasPreDefinedValues(label.key)) {
                    if (label.value && !Array.isArray(label.value)) {
                        label.value = [label.value]
                    } else if (!label.value) {
                        label.value = []
                    }
                } else {
                    // 从多值操作符切换到单值操作符
                    if (Array.isArray(label.value)) {
                        label.value = label.value[0] || ''
                    }
                }
            }

            // 判断是否有预定义值列表
            const hasPreDefinedValues = (key) => {
                const values = tagValuesMap.value[key]
                return values && values.length > 0
            }

            // 获取预定义值列表
            const getPreDefinedValues = (key) => {
                return tagValuesMap.value[key] || []
            }

            // 判断是否是支持多值的操作符
            const isMultiValueOperator = (operator) => {
                return operator === 'IN'
            }

            // 判断是否是需要输入框的操作符（START_WITH、END_WITH、CONTAINS）
            const isInputOperator = (operator) => {
                return ['START_WITH', 'END_WITH', 'CONTAINS'].includes(operator)
            }

            // 判断是否应该显示选择框（有预定义值且不是输入类型操作符）
            const shouldShowSelect = (key, operator) => {
                return hasPreDefinedValues(key) && !isInputOperator(operator)
            }

            // 获取值输入框的 placeholder
            const getValuePlaceholder = (operator) => {
                if (isMultiValueOperator(operator)) {
                    return proxy.$t('environment.scheduling.multiValuePlaceholder')
                }
                return proxy.$t('environment.scheduling.inputLabelValue')
            }

            // 添加标签
            const handleAddLabel = () => {
                formData.value.labels.push({
                    key: '',
                    operator: 'EQUAL',
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

                if (!formData.value.affinityEnabled && !formData.value.nodeLoadEnabled && !formData.value.labelsEnabled) {
                    proxy.$bkMessage({
                        theme: 'warning',
                        message: proxy.$t('environment.scheduling.atLeastOneCondition')
                    })
                    return
                }

                try {
                    isSaving.value = true

                    // 构建 labelSelector
                    const labelSelector = formData.value.labelsEnabled
                        ? formData.value.labels
                            .filter(l => l.key && (Array.isArray(l.value) ? l.value.length : l.value !== ''))
                            .map(l => ({
                                tagKeyId: l.key,
                                op: l.operator,
                                values: Array.isArray(l.value) ? l.value : [l.value]
                            }))
                        : []

                    // 构建提交数据
                    const submitData = {
                        id: formData.value.id,
                        strategyName: (formData.value.strategyName || '').trim(),
                        enabled: formData.value.enabled !== false,
                        labelSelector,
                        scope: formData.value.affinityEnabled ? 'PRE_BUILD' : 'ALL',
                        ...(formData.value.nodeLoadEnabled ? { nodeRule: formData.value.nodeLoadType === 'idleNode' ? 'IDLE' : 'AVAILABLE' } : {})
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
                operatorList,

                // function
                handleLabelKeyChange,
                handleOperatorChange,
                handleAddLabel,
                handleRemoveLabel,
                handleConfirm,
                handleCancel,
                hasPreDefinedValues,
                getPreDefinedValues,
                isMultiValueOperator,
                getValuePlaceholder,
                shouldShowSelect
            }
        }
    }
</script>

<style lang="scss" scoped>
.strategy-dialog-content {
    max-height: 500px;
    overflow-y: auto;

    .form-section {
        padding: 8px 0;

        &:first-child {
            padding-top: 0;
        }

        &:last-child {
            padding-bottom: 0;
        }

        &.strategy-name-section {
            padding-bottom: 16px;
            display: flex;
            color: #313238;
            align-items: center;
            gap: 8px;

            .strategy-name-label {
                flex-shrink: 0;
                font-size: 14px;
                text-align: right;
            }

            .strategy-name-input {
                flex: 1;
            }
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

        .checkbox-tag {
            display: inline-flex;
            align-items: center;
            height: 22px;
            padding: 0 8px;
            border-radius: 2px;
            font-size: 12px;
            color: #63656E;
            background: #F0F1F5;
            border: 1px solid #DCDEE5;
        }

        .checkbox-desc {
            font-size: 12px;
            color: #979BA5;
            margin-left: 8px;
        }

        .sub-options {
            margin-top: 12px;
            padding-left: 16px;

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
                    align-items: center;

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

            .radio-tag {
                display: inline-flex;
                align-items: center;
                height: 22px;
                padding: 0 8px;
                margin-left: 4px;
                border-radius: 2px;
                font-size: 12px;
                color: #63656E;
                background: #F0F1F5;
                border: 1px solid #DCDEE5;
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
            .labels-table {
                width: 100%;
                border-collapse: collapse;
                border: 1px solid #DCDEE5;
                
                th, td {
                    border: 1px solid #DCDEE5;
                    padding: 8px 12px;
                    text-align: left;
                    vertical-align: middle;
                }
                
                thead {
                    background: #FAFBFD;
                    
                    th {
                        font-size: 12px;
                        font-weight: normal;
                        color: #313238;
                        
                        .required {
                            color: #EA3636;
                            margin-right: 4px;
                        }
                    }
                }
                
                tbody {
                    tr {
                        background: #fff;
                        
                        &:hover {
                            background: #F5F7FA;
                        }
                    }
                }
                
                .label-key-col {
                    width: 240px;
                }
                
                .label-value-col {
                    .value-cell {
                        display: flex;
                        align-items: center;
                        gap: 8px;
                    }
                    
                    .label-operator-select {
                        width: 100px;
                        flex-shrink: 0;
                    }
                    
                    .label-value-input {
                        width: 250px;
                        flex-shrink: 0;
                    }
                }
                
                .label-actions-col {
                    width: 80px;
                    
                    .label-actions {
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        gap: 8px;
                    }
                    
                    .action-icon {
                        display: inline-flex;
                        align-items: center;
                        justify-content: center;
                        cursor: pointer;
                        
                        .bk-icon {
                            font-size: 20px;
                        }
                        
                        &.add-icon {
                            .bk-icon {
                                font-size: 18px;
                            }
                            
                            &:hover .bk-icon {
                                color: #699DF4;
                            }
                        }
                        
                        &.delete-icon {
                            .bk-icon {
                                font-size: 18px;
                            }
                            
                            &:hover:not(.disabled) .bk-icon {
                                color: #EA3636;
                            }
                            
                            &.disabled {
                                cursor: not-allowed;
                                
                                .bk-icon {
                                    color: #DCDEE5;
                                }
                            }
                        }
                    }
                }
                
                .label-key-select {
                    width: 240px;
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
