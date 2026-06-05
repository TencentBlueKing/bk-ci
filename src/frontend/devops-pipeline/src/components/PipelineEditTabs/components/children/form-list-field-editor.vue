<template>
    <section class="form-list-field-editor">
        <p class="field-subtitle">{{ t('storeMap.formListFieldObjectConfigDesc') }}</p>
        <bk-table
            :data="localFields"
            :outer-border="true"
            :row-border="true"
            size="small"
            :row-class-name="handleRowClassName"
            @row-click="handleRowClick"
        >
            <bk-table-column
                :label="getColumnLabel('formListFieldId', true)"
                prop="id"
                :width="120"
            >
                <template #default="{ row, $index }">
                    <div :class="['field-cell', { 'is-error': fieldErrors[$index]?.id }]">
                        <bk-input
                            size="small"
                            :disabled="disabled"
                            :placeholder="t('nameInputTips')"
                            :value="row.id"
                            @input="(val) => handleFieldChange($index, 'id', val)"
                            @change="(val) => handleFieldChange($index, 'id', val)"
                            @blur="validateField($index)"
                        />
                        <p
                            v-if="fieldErrors[$index]?.id && !fieldErrors[$index]?.idOnly"
                            class="bk-form-help is-danger"
                        >
                            {{ fieldErrors[$index].id }}
                        </p>
                    </div>
                </template>
            </bk-table-column>
            <bk-table-column
                :label="getColumnLabel('formListFieldType', true)"
                prop="type"
                :width="150"
            >
                <template #default="{ row, $index }">
                    <bk-select
                        size="small"
                        :disabled="disabled"
                        :value="row.type"
                        :clearable="false"
                        @change="(val) => handleFieldTypeChange($index, val)"
                    >
                        <bk-option
                            v-for="item in fieldTypeList"
                            :key="item.id"
                            :id="item.id"
                            :name="item.name"
                        />
                    </bk-select>
                </template>
            </bk-table-column>
            <bk-table-column
                :label="t('storeMap.formListFieldOptions')"
            >
                <template #default="{ row, $index }">
                    <div :class="['field-cell', { 'is-error': fieldErrors[$index]?.options }]">
                        <div class="option-cell-content">
                            <span
                                v-if="fieldErrors[$index]?.options"
                                class="bk-form-help is-danger option-error-tip"
                                v-bk-tooltips="{ content: fieldErrors[$index].options, allowHTML: false }"
                            >
                                <i class="bk-icon icon-exclamation-circle" />
                            </span>
                            <option-tags-cell
                                v-if="isSelectorFieldType(row.type)"
                                :options="row.options || []"
                                :disabled="disabled"
                                :on-delete="(optIdx) => handleDeleteOption($index, optIdx)"
                                :on-edit="() => handleShowOptionEditor($index)"
                            />
                            <span
                                v-else
                                class="no-options-tip"
                            >{{ t('storeMap.formListFieldNoOption') }}</span>
                        </div>
                    </div>
                </template>
            </bk-table-column>
            <bk-table-column
                :label="t('storeMap.formListFieldRequired')"
                :width="60"
                :center="true"
            >
                <template #default="{ row, $index }">
                    <bk-checkbox
                        :disabled="disabled"
                        :value="row.required"
                        @change="(val) => handleFieldChange($index, 'required', val)"
                    />
                </template>
            </bk-table-column>
            <bk-table-column
                :label="t('storeMap.formListFieldDesc')"
                prop="desc"
                :min-width="100"
            >
                <template #default="{ row, $index }">
                    <bk-input
                        size="small"
                        :disabled="disabled"
                        :placeholder="t('editPage.descTips')"
                        :value="row.desc"
                        @input="(val) => handleFieldChange($index, 'desc', val)"
                        @change="(val) => handleFieldChange($index, 'desc', val)"
                    />
                </template>
            </bk-table-column>
            <bk-table-column
                :label="t('storeMap.formListFieldAction')"
                :width="60"
                :center="true"
            >
                <template #default="{ row, $index }">
                    <div class="action-icons">
                        <i
                            class="bk-icon icon-plus-circle-shape action-icon add-icon"
                            @click.stop="handleAddFieldAt($index)"
                        />
                        <i
                            class="bk-icon icon-minus-circle-shape action-icon delete-icon"
                            :class="{ 'disabled': localFields.length <= 1 }"
                            @click.stop="handleDeleteField($index)"
                        />
                    </div>
                </template>
            </bk-table-column>
        </bk-table>

        <!-- ResizePanel for option editing -->
        <resize-panel
            :visible="optionEditorVisible"
            :width="480"
            :min-width="320"
            :max-width="800"
            ext-cls="form-list-option-sideslider"
        >
            <template #header>
                {{ t('storeMap.formListFieldOptionTitle') }}
                <span
                    v-if="optionEditorTypeName"
                    class="option-title-type"
                >
                    {{ optionEditorTypeName }}
                </span>
            </template>
            <template #content>
                <key-options
                    ref="keyOptions"
                    class="option-panel-content"
                    :disabled="disabled"
                    :options="editingOptions"
                    :handle-change-options="handleEditingOptionsChange"
                />
            </template>
            <template #footer>
                <bk-button
                    theme="primary"
                    outline
                    @click="handleOptionEditorConfirm"
                >
                    {{ t('confirm') }}
                </bk-button>
                <bk-button
                    @click="handleOptionEditorCancel"
                >
                    {{ t('cancel') }}
                </bk-button>
            </template>
        </resize-panel>
    </section>
</template>

<script>
    import ResizePanel from '@/components/common/ResizePanel.vue'
    import UseInstance from '@/hook/useInstance'
    import {
        BOOLEAN,
        CHECKBOX,
        FORM_LIST_FIELD_TYPE_LIST,
        STRING,
        isFormListSelectorFieldType
    } from '@/store/modules/atom/paramsConfig'
    import { computed, defineComponent, reactive, ref, watch } from 'vue'
    import KeyOptions from './key-options'
    import OptionTagsCell from './option-tags-cell.vue'

    const ID_REGEX = /^[a-zA-Z][a-zA-Z0-9_]{0,63}$/

    function createDefaultField () {
        return {
            id: '',
            name: '',
            type: STRING,
            desc: '',
            required: false,
            defaultValue: '',
            readOnly: false,
            valueNotEmpty: false,
            options: [],
            category: ''
        }
    }

    export default defineComponent({
        name: 'FormListFieldEditor',
        components: {
            KeyOptions,
            OptionTagsCell,
            ResizePanel
        },
        props: {
            disabled: {
                type: Boolean,
                default: false
            },
            fields: {
                type: Array,
                default: () => ([])
            },
            handleChange: {
                type: Function,
                required: true
            }
        },
        setup (props, { expose }) {
            const { proxy, t } = UseInstance()

            // 把 paramsConfig 提供的字段类型列表 + i18n key 转成下拉显示项
            const fieldTypeList = computed(() =>
                FORM_LIST_FIELD_TYPE_LIST.map(item => ({
                    id: item.id,
                    name: t(`storeMap.${item.nameKey}`)
                }))
            )

            const localFields = ref([])
            const fieldErrors = reactive({})
            const activeRowIndex = ref(-1)

            // 选项编辑面板的状态
            const optionEditorVisible = ref(false)
            const editingFieldIndex = ref(-1)
            const editingOptions = ref([])

            // 侧滑面板 header 上展示的字段类型名
            const optionEditorTypeName = computed(() => {
                if (editingFieldIndex.value < 0) return ''
                const field = localFields.value[editingFieldIndex.value]
                if (!field) return ''
                const typeItem = fieldTypeList.value.find(item => item.id === field.type)
                return typeItem ? typeItem.name : field.type
            })

            watch(() => props.fields, (val) => {
                const fields = val || []
                localFields.value = fields.length ? fields : [createDefaultField()]
            }, { immediate: true, deep: true })

            localFields.value = (props.fields && props.fields.length) ? props.fields : [createDefaultField()]

            function isSelectorFieldType (type) {
                return isFormListSelectorFieldType(type)
            }

            function getColumnLabel (key, required) {
                const label = t(`storeMap.${key}`)
                return required ? `*${label}` : label
            }

            function handleRowClassName ({ rowIndex }) {
                return rowIndex === activeRowIndex.value ? 'active-row' : ''
            }

            function handleRowClick ({ rowIndex }) {
                activeRowIndex.value = rowIndex
            }

            function handleAddFieldAt (index) {
                localFields.value.splice(index + 1, 0, createDefaultField())
                emitFieldsChange()
            }

            function handleDeleteField (index) {
                if (localFields.value.length <= 1) return
                localFields.value.splice(index, 1)
                if (activeRowIndex.value >= localFields.value.length) {
                    activeRowIndex.value = localFields.value.length - 1
                }
                // rebuild error map
                const newErrors = {}
                Object.keys(fieldErrors).forEach(key => {
                    const errIdx = parseInt(key, 10)
                    if (errIdx < index) {
                        newErrors[key] = fieldErrors[key]
                    } else if (errIdx > index) {
                        newErrors[errIdx - 1] = fieldErrors[key]
                    }
                })
                Object.keys(fieldErrors).forEach(k => delete fieldErrors[k])
                Object.assign(fieldErrors, newErrors)
                emitFieldsChange()
            }

            function handleFieldChange (index, name, value) {
                const field = localFields.value[index]
                if (field[name] === value) return
                Object.assign(field, { [name]: value })
                localFields.value.splice(index, 1, field)
                validateField(index)
                emitFieldsChange()
            }

            function handleFieldTypeChange (index, value) {
                const field = localFields.value[index]
                if (field.type === value) return
                const updates = { type: value }
                if (value === BOOLEAN) {
                    updates.defaultValue = false
                } else if (value === CHECKBOX) {
                    updates.defaultValue = []
                } else {
                    updates.defaultValue = ''
                }
                updates.options = isSelectorFieldType(value) ? (field.options || []) : []
                Object.assign(field, updates)
                localFields.value.splice(index, 1, field)
                validateField(index)
                emitFieldsChange()
            }

            // option tag operations
            function handleDeleteOption (fieldIndex, optIndex) {
                const field = localFields.value[fieldIndex]
                if (!field.options) return
                field.options.splice(optIndex, 1)
                localFields.value.splice(fieldIndex, 1, field)
                validateField(fieldIndex)
                emitFieldsChange()
            }

            // option editor in-dialog panel
            function handleShowOptionEditor (fieldIndex) {
                editingFieldIndex.value = fieldIndex
                activeRowIndex.value = fieldIndex
                const field = localFields.value[fieldIndex]
                editingOptions.value = (field.options || []).map(opt => ({ ...opt }))
                optionEditorVisible.value = true
            }

            // key-options callback: updates editingOptions in real time
            function handleEditingOptionsChange (name, value) {
                if (name === 'options') {
                    editingOptions.value = [...value]
                }
            }

            function handleOptionEditorConfirm () {
                if (hasPendingBatchInput()) {
                    proxy.$bkMessage({
                        theme: 'warning',
                        message: t('storeMap.formListFieldBatchPendingTip'),
                        limit: 1
                    })
                    return
                }
                const optionError = validateOptions(editingOptions.value)
                if (optionError) {
                    proxy.$bkMessage({
                        theme: 'error',
                        message: optionError,
                        limit: 1
                    })
                    return
                }
                const field = localFields.value[editingFieldIndex.value]
                Object.assign(field, { options: [...editingOptions.value] })
                localFields.value.splice(editingFieldIndex.value, 1, field)
                validateField(editingFieldIndex.value)
                emitFieldsChange()
                optionEditorVisible.value = false
            }

            function hasPendingBatchInput () {
                return !!proxy.$refs.keyOptions?.hasPendingBatchInput?.()
            }

            function handleOptionEditorCancel () {
                optionEditorVisible.value = false
            }

            function validateField (index) {
                const field = localFields.value[index]
                const errors = {}
                if (!field.id) {
                    errors.id = t('storeMap.formListFieldIdRequired')
                    errors.idOnly = true
                } else if (!ID_REGEX.test(field.id)) {
                    errors.id = t('storeMap.formListFieldIdFormat')
                } else {
                    const duplicateIdx = localFields.value.findIndex(
                        (f, i) => i !== index && f.id === field.id
                    )
                    if (duplicateIdx !== -1) {
                        errors.id = t('storeMap.formListFieldIdDuplicate')
                    }
                }
                if (isSelectorFieldType(field.type)) {
                    const optionError = validateOptions(field.options || [])
                    if (optionError) {
                        errors.options = optionError
                    }
                }
                fieldErrors[index] = errors
            }

            function validateOptions (options) {
                if (!options.length) {
                    return t('editPage.requiredTips', ['options'])
                }
                const keySet = new Set()
                for (const option of options) {
                    if (!option.key) {
                        return t('editPage.requiredTips', ['value'])
                    }
                    if (keySet.has(option.key)) {
                        return t('editPage.noRepeatTips', ['value'])
                    }
                    keySet.add(option.key)
                }
                return ''
            }

            function validateAllFields () {
                let isValid = true
                localFields.value.forEach((field, index) => {
                    validateField(index)
                    const errors = fieldErrors[index] || {}
                    const realErrors = Object.keys(errors).filter(key => {
                        if (key.endsWith('Only')) return false
                        return true
                    })
                    if (realErrors.length > 0) {
                        isValid = false
                    }
                })
                return isValid
            }

            function getFirstError () {
                for (const index of Object.keys(fieldErrors)) {
                    const errors = fieldErrors[index] || {}
                    const firstKey = Object.keys(errors).find(key => {
                        if (key.endsWith('Only')) return false
                        return true
                    })
                    if (firstKey) return errors[firstKey]
                }
                return ''
            }

            function emitFieldsChange () {
                props.handleChange('fields', [...localFields.value])
            }

            expose({
                validateAllFields,
                getFirstError,
                getFields: () => [...localFields.value]
            })

            return {
                fieldTypeList,
                localFields,
                fieldErrors,
                activeRowIndex,
                optionEditorVisible,
                optionEditorTypeName,
                editingOptions,
                isSelectorFieldType,
                getColumnLabel,
                handleRowClassName,
                handleRowClick,
                handleAddFieldAt,
                handleDeleteField,
                handleFieldChange,
                handleFieldTypeChange,
                handleDeleteOption,
                handleShowOptionEditor,
                handleEditingOptionsChange,
                handleOptionEditorConfirm,
                handleOptionEditorCancel,
                validateField,
                t
            }
        }
    })
</script>

<style lang="scss" scoped>
    .form-list-field-editor {
        height: 100%;

        .field-subtitle {
            font-size: 12px;
            color: #979BA5;
            margin-bottom: 16px;
            line-height: 20px;
        }

        // bk-table customization
        :deep(.bk-table) {
            .bk-table-header-wrapper {
                th {
                    font-size: 12px;
                    font-weight: 600;
                    color: #63656E;
                    background-color: #FAFBFD;
                }
            }

            .bk-table-body-wrapper {
                td {
                    vertical-align: middle;
                    font-size: 12px;
                }

                tr.active-row {
                    td {
                        background-color: #F0F5FF;
                    }
                }
            }

        }

        .field-cell {
            &.is-error {
                :deep(.bk-form-input) {
                    border-color: #EA3636;
                }
            }
        }

        .no-options-tip {
            color: #C4C6CC;
            font-size: 12px;
        }

        .option-cell-content {
            display: flex;
            align-items: center;
            min-width: 0;
        }

        .option-error-tip {
            display: inline-flex;
            align-items: center;
            flex-shrink: 0;
            margin: 0 4px 0 0;
            cursor: help;

            .bk-icon {
                flex-shrink: 0;
            }
        }

        .action-icons {
            display: flex;
            align-items: center;
            justify-content: center;
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

                    &:hover:not(.disabled) {
                        color: #FF5656;
                    }

                    &.disabled {
                        cursor: not-allowed;
                        opacity: 0.4;
                    }
                }
            }
        }

        .text-link {
            color: #3A84FF;
            cursor: pointer;
            font-size: 12px;

            &:hover {
                color: #699DF4;
            }

            i {
                margin-right: 4px;
            }
        }
    }

    .form-list-option-sideslider {
        bottom: 47px;
        .option-title-type {
            color: #979BA5;
            margin-left: 8px;
            display: flex;
            align-items: center;
            border-left: 1px solid #C4C6CC;
            padding-left: 8px;
            line-height: 1;
        }
        
        // resize panel content styles
        .option-panel-content {
            padding: 20px 24px;
        }
    }



</style>
