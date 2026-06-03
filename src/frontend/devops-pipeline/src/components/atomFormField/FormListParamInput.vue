<template>
    <section class="form-list-param-input">
        <div
            v-if="!localValue.length"
            class="form-list-empty-placeholder"
        >
            {{ $t('noData') }}
        </div>
        <div
            class="form-list-item"
            v-for="(item, rowIndex) in localValue"
            :key="rowIndex"
        >
            <div class="form-list-item-header">
                <span class="item-index">{{ $t('storeMap.formList') }} {{ rowIndex + 1 }}</span>
                <div
                    v-if="!disabled"
                    class="item-actions"
                >
                    <i
                        class="bk-icon icon-plus-circle-shape action-icon add-icon"
                        @click.stop="handleAddItemAt(rowIndex)"
                    />
                    <i
                        class="bk-icon icon-minus-circle-shape action-icon delete-icon"
                        @click.stop="handleDeleteItem(rowIndex)"
                    />
                </div>
            </div>
            <div
                class="form-list-item-body"
                :class="{ 'is-double-col': isDoubleColumn }"
            >
                <div
                    class="form-field-row"
                    :class="{ 'is-span-full': isDoubleColumn && isTextareaParam(field.type) }"
                    v-for="field in fields"
                    :key="field.id"
                >
                    <label class="field-label">
                        <span class="label-text">{{ field.name || field.id }}</span>
                        <span
                            v-if="field.required"
                            class="required-mark"
                        >*</span>
                    </label>
                    <div
                        class="field-control"
                        :class="{ 'is-error': !!getFieldError(rowIndex, field.id) }"
                    >
                        <template v-if="field.readOnly">
                            <span class="readonly-value">{{ getItemFieldValue(item, field.id) }}</span>
                        </template>
                        <template v-else-if="isBooleanParam(field.type)">
                            <enum-input
                                :name="field.id"
                                :list="boolList"
                                :disabled="disabled"
                                :handle-change="(name, value) => handleItemFieldChange(rowIndex, name, value)"
                                :value="getItemFieldValue(item, field.id)"
                            />
                        </template>
                        <template v-else-if="isEnumParam(field.type)">
                            <selector
                                :popover-min-width="180"
                                :disabled="disabled"
                                :name="field.id"
                                :clearable="false"
                                :list="getFieldOptions(field)"
                                :multi-select="false"
                                :handle-change="(name, value) => handleItemFieldChange(rowIndex, name, value)"
                                :value="getItemFieldValue(item, field.id)"
                            />
                        </template>
                        <template v-else-if="isMultipleParam(field.type)">
                            <selector
                                :popover-min-width="180"
                                :disabled="disabled"
                                :name="field.id"
                                :clearable="false"
                                :list="getFieldOptions(field)"
                                :multi-select="true"
                                show-select-all
                                :handle-change="(name, value) => handleItemFieldChange(rowIndex, name, value)"
                                :value="getItemFieldValue(item, field.id)"
                            />
                        </template>
                        <template v-else-if="isCheckboxParam(field.type)">
                            <atom-checkbox
                                :disabled="disabled"
                                :name="field.id"
                                :value="getItemFieldValue(item, field.id)"
                                :handle-change="(name, value) => handleItemFieldChange(rowIndex, name, value)"
                            />
                        </template>
                        <template v-else-if="isTextareaParam(field.type)">
                            <vuex-textarea
                                :disabled="disabled"
                                :handle-change="(name, value) => handleItemFieldChange(rowIndex, name, value)"
                                :name="field.id"
                                :placeholder="field.desc || ''"
                                :value="getItemFieldValue(item, field.id)"
                            />
                        </template>
                        <template v-else>
                            <vuex-input
                                :disabled="disabled"
                                :handle-change="(name, value) => handleItemFieldChange(rowIndex, name, value)"
                                :name="field.id"
                                :placeholder="field.desc || ''"
                                :value="getItemFieldValue(item, field.id)"
                            />
                        </template>
                        <p
                            v-if="getFieldError(rowIndex, field.id)"
                            class="field-error-tip"
                        >
                            {{ getFieldError(rowIndex, field.id) }}
                        </p>
                    </div>
                </div>
            </div>
        </div>
    </section>
</template>

<script>
    import AtomCheckbox from '@/components/atomFormField/AtomCheckbox'
    import EnumInput from '@/components/atomFormField/EnumInput'
    import Selector from '@/components/atomFormField/Selector'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import VuexTextarea from '@/components/atomFormField/VuexTextarea'
    import UseInstance from '@/hook/useInstance'
    import {
        BOOLEAN_LIST,
        MULTIPLE,
        isBooleanParam,
        isCheckboxParam,
        isEnumParam,
        isMultipleParam,
        isTextareaParam
    } from '@/store/modules/atom/paramsConfig'
    import { getFormListDefaultRow, getFormListFieldDefaultValue } from '@/utils/util'
    import { computed, ref, watch } from 'vue'

    export default {
        name: 'FormListParamInput',
        components: {
            AtomCheckbox,
            EnumInput,
            Selector,
            VuexInput,
            VuexTextarea
        },
        props: {
            disabled: {
                type: Boolean,
                default: false
            },
            value: {
                type: [Array, String],
                default: () => ([])
            },
            defaultValue: {
                type: [Array, String],
                default: () => ([])
            },
            fields: {
                type: Array,
                default: () => ([])
            },
            handleChange: {
                type: Function,
                default: () => () => {}
            },
            name: {
                type: String,
                default: ''
            },
            // 一行展示的字段列数：1（默认，编辑面板等窄场景）或 2（执行/调试等宽场景）
            columns: {
                type: Number,
                default: 1,
                validator: (val) => val === 1 || val === 2
            }
        },
        emits: ['input', 'change'],
        setup (props, { emit, expose }) {
            const { t } = UseInstance()

            const localValue = ref([])
            const boolList = BOOLEAN_LIST
            const isDoubleColumn = computed(() => Number(props.columns) >= 2)
            // { [rowIndex]: { [fieldId]: errorMsg } }
            const errors = ref({})

            const getFieldError = (rowIndex, fieldId) => {
                return errors.value?.[rowIndex]?.[fieldId] || ''
            }

            const clearFieldError = (rowIndex, fieldId) => {
                const rowErrors = errors.value[rowIndex]
                if (rowErrors && rowErrors[fieldId]) {
                    const nextRow = { ...rowErrors }
                    delete nextRow[fieldId]
                    const nextErrors = { ...errors.value }
                    if (Object.keys(nextRow).length === 0) {
                        delete nextErrors[rowIndex]
                    } else {
                        nextErrors[rowIndex] = nextRow
                    }
                    errors.value = nextErrors
                }
            }

            const isEmptyFieldValue = (value) => {
                if (value === undefined || value === null) return true
                if (Array.isArray(value)) return value.length === 0
                if (typeof value === 'string') return value.trim() === ''
                return false
            }


            const getFieldOptions = (field) => {
                if (!field.options || !Array.isArray(field.options)) return []
                return field.options.map(opt => ({
                    id: opt.key,
                    name: opt.value || opt.key,
                    value: opt.key,
                    label: opt.value || opt.key
                }))
            }

            const getItemFieldValue = (item, fieldId) => {
                const field = props.fields.find(f => f.id === fieldId)
                if (item && Object.prototype.hasOwnProperty.call(item, fieldId)) {
                    const val = item[fieldId]
                    // MULTIPLE 类型在 emit 时被转成逗号串，回显时拆回数组
                    if (field && field.type === MULTIPLE && typeof val === 'string') {
                        return val.split(',').filter(v => v !== '')
                    }
                    return val
                }
                return field ? getFieldDefaultValue(field) : ''
            }

            const ensureFieldKeys = () => {
                if (!props.fields || !props.fields.length) return
                localValue.value = localValue.value.map(item => {
                    const newItem = { ...item }
                    props.fields.forEach(field => {
                        if (!Object.prototype.hasOwnProperty.call(newItem, field.id)) {
                            newItem[field.id] = getFormListFieldDefaultValue(field)
                        }
                    })
                    return newItem
                })
            }

            const createEmptyItem = () => {
                return getFormListDefaultRow(props.fields, props.defaultValue)
            }

            const emitValueChange = () => {
                const clonedValue = localValue.value.map(item => ({ ...item }))
                if (props.handleChange) {
                    props.handleChange(props.name, clonedValue)
                }
                emit('input', clonedValue)
                emit('change', clonedValue)
            }

            const handleAddItemAt = (index) => {
                localValue.value.splice(index + 1, 0, createEmptyItem())
                emitValueChange()
            }

            const handleDeleteItem = (index) => {
                if (localValue.value.length <= 1) {
                    localValue.value.splice(index, 1, createEmptyItem())
                } else {
                    localValue.value.splice(index, 1)
                }
                // 删除一行后，重新对齐 errors 的行索引
                const nextErrors = {}
                Object.keys(errors.value).forEach(key => {
                    const rowIdx = Number(key)
                    if (rowIdx === index) return
                    const newIdx = rowIdx > index ? rowIdx - 1 : rowIdx
                    nextErrors[newIdx] = errors.value[rowIdx]
                })
                errors.value = nextErrors
                emitValueChange()
            }

            const handleItemFieldChange = (rowIndex, fieldId, value) => {
                const item = { ...localValue.value[rowIndex] }
                const field = props.fields.find(f => f.id === fieldId)
                // MULTIPLE 类型存储为逗号串，方便回传后端
                if (field && field.type === MULTIPLE && Array.isArray(value)) {
                    item[fieldId] = value.join(',')
                } else {
                    item[fieldId] = value
                }
                localValue.value.splice(rowIndex, 1, item)
                if (field && !isEmptyFieldValue(item[fieldId])) {
                    clearFieldError(rowIndex, fieldId)
                }
                emitValueChange()
            }

            const validate = () => {
                const requiredFields = (props.fields || []).filter(f => f && f.required)
                if (!requiredFields.length || !localValue.value.length) {
                    errors.value = {}
                    return true
                }
                const nextErrors = {}
                let isValid = true
                localValue.value.forEach((item, rowIndex) => {
                    requiredFields.forEach(field => {
                        const val = getItemFieldValue(item, field.id)
                        if (isEmptyFieldValue(val)) {
                            if (!nextErrors[rowIndex]) nextErrors[rowIndex] = {}
                            nextErrors[rowIndex][field.id] = t('storeMap.formListFieldRequiredTip')
                            isValid = false
                        }
                    })
                })
                errors.value = nextErrors
                return isValid
            }

            if (typeof expose === 'function') {
                expose({ validate })
            }

            watch(
                () => props.value,
                (val) => {
                    if (Array.isArray(val) && val.length > 0) {
                        localValue.value = val.map(item => ({ ...item }))
                    } else if (typeof val === 'string' && val.trim() !== '' && val.trim() !== '[]') {
                        // 兼容字符串形式（后端有可能直接回传 JSON 串）
                        try {
                            const parsed = JSON.parse(val)
                            localValue.value = Array.isArray(parsed) && parsed.length > 0
                                ? parsed.map(item => ({ ...item }))
                                : []
                        } catch (e) {
                            localValue.value = []
                        }
                    } else {
                        localValue.value = []
                    }
                    ensureFieldKeys()
                    if (localValue.value.length === 0 && props.fields && props.fields.length > 0 && !props.disabled) {
                        localValue.value.push(createEmptyItem())
                    }
                },
                { immediate: true, deep: true }
            )

            watch(
                () => props.fields,
                () => {
                    ensureFieldKeys()
                    errors.value = {}
                },
                { deep: true }
            )

            return {
                localValue,
                boolList,
                isDoubleColumn,
                isBooleanParam,
                isCheckboxParam,
                isEnumParam,
                isMultipleParam,
                isTextareaParam,
                getFieldOptions,
                getItemFieldValue,
                getFieldError,
                handleAddItemAt,
                handleDeleteItem,
                handleItemFieldChange,
                validate
            }
        }
    }
</script>

<style lang="scss" scoped>
    .form-list-param-input {
        width: 100%;
        display: flex;
        flex-direction: column;
        gap: 8px;

        .form-list-empty-placeholder {
            display: flex;
            align-items: center;
            justify-content: center;
            height: 60px;
            font-size: 12px;
            color: #979BA5;
            background: #FAFBFD;
            border: 1px dashed #DCDEE5;
            border-radius: 2px;
        }

        .form-list-item {
            
            border: 1px solid #DCDEE5;
            border-radius: 2px;
            overflow: hidden;

            .form-list-item-header {
                display: flex;
                justify-content: space-between;
                align-items: center;
                padding: 10px 12px;
                background-color: #FAFBFD;
                border-bottom: 1px solid #DCDEE5;

                .item-index {
                    font-size: 12px;
                    color: #63656E;
                    font-weight: 600;
                }

                .item-actions {
                    display: flex;
                    align-items: center;
                    gap: 8px;

                    .action-icon {
                        font-size: 16px;
                        cursor: pointer;

                        &.add-icon {
                            color: #3A84FF;

                            &:hover {
                                color: #699DF4;
                            }
                        }

                        &.delete-icon {
                            color: #979BA5;

                            &:hover {
                                color: #FF5656;
                            }
                        }
                    }
                }
            }

            .form-list-item-body {
                padding: 12px;

                &.is-double-col {
                    display: grid;
                    grid-template-columns: 1fr 1fr;
                    column-gap: 16px;
                    row-gap: 12px;

                    .form-field-row {
                        margin-bottom: 0;

                        &.is-span-full {
                            grid-column: 1 / -1;
                        }
                    }
                }
            }

            .form-field-row {
                margin-bottom: 12px;

                &:last-child {
                    margin-bottom: 0;
                }

                .field-label {
                    display: block;
                    margin-bottom: 4px;
                    font-size: 12px;
                    color: #63656E;
                    line-height: 20px;

                    .required-mark {
                        color: #FF5656;
                        margin-left: 2px;
                    }
                }

                .field-control {
                    .readonly-value {
                        color: #979BA5;
                        font-size: 12px;
                    }

                    &.is-error {
                        ::v-deep .bk-form-input,
                        ::v-deep .bk-form-textarea,
                        ::v-deep input,
                        ::v-deep textarea,
                        ::v-deep .bk-selector-input {
                            border-color: #FF5656 !important;
                        }
                    }

                    .field-error-tip {
                        margin: 4px 0 0;
                        font-size: 12px;
                        line-height: 16px;
                        color: #FF5656;
                    }
                }
            }
        }
    }
</style>
