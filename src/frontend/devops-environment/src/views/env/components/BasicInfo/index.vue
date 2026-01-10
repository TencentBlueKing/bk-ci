<template>
    <div
        class="basic-info-container"
        v-bkloading="{ isLoading }"
    >
        <div
            v-for="field in infoFields"
            :key="field.key"
            class="info-item"
        >
            <span
                class="info-label"
                v-bk-overflow-tips
            >{{ field.label }}：</span>
            <span
                class="info-value"
            >
                <!-- 非编辑状态 -->
                <template v-if="editingField !== field.key">
                    {{ getFieldValue(field.key) || '--' }}
                    <i
                        v-if="field.editable"
                        class="bk-icon icon-edit-line edit-icon"
                        v-perm="{
                            permissionData: {
                                projectId: projectId,
                                resourceType: ENV_RESOURCE_TYPE,
                                resourceCode: envHashId,
                                action: ENV_RESOURCE_ACTION.EDIT
                            }
                        }"
                        @click="handleStartEdit(field.key)"
                    ></i>
                </template>
                
                <!-- 编辑状态 -->
                <template v-else>
                    <bk-input
                        class="info-input"
                        v-if="field.type === 'input'"
                        v-model="editingValue"
                        :maxlength="field.maxlength"
                        :disabled="isSaving"
                        ref="editInput"
                    />
                    <bk-input
                        class="info-input"
                        v-else-if="field.type === 'textarea'"
                        v-model="editingValue"
                        type="textarea"
                        :maxlength="field.maxlength"
                        :rows="3"
                        :disabled="isSaving"
                        ref="editInput"
                    />
                    <i
                        v-if="!isSaving"
                        class="bk-icon icon-check-line save-icon"
                        @click="handleSaveEdit"
                    />
                    <i
                        v-else
                        class="bk-icon icon-circle-2-1 loading-icon"
                    />
                    <i
                        class="bk-icon icon-close-line cancel-icon"
                        :class="{ disabled: isSaving }"
                        @click="!isSaving && handleCancelEdit()"
                    />
                </template>
            </span>
        </div>
    </div>
</template>

<script>
    import { ref, computed, watch, nextTick, onMounted } from 'vue'
    import {
        ENV_RESOURCE_ACTION,
        ENV_RESOURCE_TYPE
    } from '@/utils/permission'
    import useInstance from '@/hooks/useInstance'
    import useEnvDetail from '@/hooks/useEnvDetail'
    import { convertTime } from '@/utils/util'
    
    export default {
        name: 'BasicInfo',
        setup () {
            const { proxy } = useInstance()
            const {
                currentEnv,
                envHashId,
                projectId,
                fetchEnvDetail,
                updateEnvDetail
            } = useEnvDetail()
            
            const isLoading = ref(false)
            const isSaving = ref(false)
            const editingField = ref(null)
            const editingValue = ref('')
            const editInput = ref(null)
            watch(() => envHashId.value, () => {
                getEnvDetail()
            })
            const infoFields = computed(() => [
                {
                    key: 'name',
                    label: proxy.$t('environment.environmentName'),
                    editable: true,
                    type: 'input',
                    maxlength: 30
                },
                {
                    key: 'envType',
                    label: proxy.$t('environment.environmentType'),
                    editable: false
                },
                {
                    key: 'desc',
                    label: proxy.$t('environment.envInfo.envRemark'),
                    editable: true,
                    type: 'textarea',
                    maxlength: 100
                },
                {
                    key: 'updatedUser',
                    label: proxy.$t('environment.lastModifier'),
                    editable: false
                },
                {
                    key: 'updatedTime',
                    label: proxy.$t('environment.lastModifyTime'),
                    editable: false,
                    isTime: true
                },
                {
                    key: 'createdUser',
                    label: proxy.$t('environment.envInfo.creator'),
                    editable: false
                },
                {
                    key: 'createdTime',
                    label: proxy.$t('environment.envInfo.creationTime'),
                    editable: false,
                    isTime: true
                }
            ])
            
            // 获取环境详情
            const getEnvDetail = async () => {
                try {
                    isLoading.value = true
                    await fetchEnvDetail()
                } catch (err) {
                    proxy.$bkMessage({
                        theme: 'error',
                        message: err.message || e
                    })
                } finally {
                    isLoading.value = false
                }
            }
            
            // 获取字段值
            const getFieldValue = (key) => {
                const value = currentEnv.value?.[key]
                const field = infoFields.value.find(f => f.key === key)
                if (field?.isTime && value) {
                    return convertTime(value * 1000)
                }
                if (key === 'envType' && value) {
                    return proxy.$t(`environment.envInfo.${value}EnvType`)
                }
                return value
            }
            
            // 开始编辑
            const handleStartEdit = (fieldKey) => {
                editingField.value = fieldKey
                editingValue.value = getFieldValue(fieldKey) || ''
                nextTick(() => {
                    if (editInput.value) {
                        const input = Array.isArray(editInput.value) ? editInput.value[0] : editInput.value
                        input?.focus?.()
                    }
                })
            }
            
            // 保存编辑
            const handleSaveEdit = async () => {
                if (!editingField.value || isSaving.value) return
                
                try {
                    const fieldKey = editingField.value
                    const newValue = editingValue.value.trim()
                    const oldValue = getFieldValue(fieldKey)
                    
                    // 如果值没有变化，直接取消编辑
                    if (newValue === oldValue) {
                        handleCancelEdit()
                        return
                    }
                    
                    isSaving.value = true
                    
                    // 更新数据
                    const params = {
                        ...currentEnv.value,
                        [fieldKey]: newValue
                    }
                    
                    await updateEnvDetail(params)
                    
                    // 重新获取环境详情
                    await getEnvDetail()
                    
                    proxy.$bkMessage({
                        theme: 'success',
                        message: proxy.$t('environment.successfullySaved')
                    })
                    
                    handleCancelEdit()
                } catch (err) {
                    console.error('更新失败:', err)
                    proxy.$bkMessage({
                        theme: 'error',
                        message: err.message || proxy.$t('environment.updateFailed')
                    })
                } finally {
                    isSaving.value = false
                }
            }
            
            // 取消编辑
            const handleCancelEdit = () => {
                editingField.value = null
                editingValue.value = ''
            }

            return {
                currentEnv,
                isLoading,
                isSaving,
                infoFields,
                editingField,
                editingValue,
                editInput,
                envHashId,
                projectId,
                ENV_RESOURCE_ACTION,
                ENV_RESOURCE_TYPE,

                getFieldValue,
                handleStartEdit,
                handleSaveEdit,
                handleCancelEdit
            }
        }
    }
</script>

<style lang="scss" scoped>
.basic-info-container {
    height: calc(100% - 90px);
    .info-item {
        display: flex;
        align-items: center;
        margin-bottom: 20px;
        line-height: 20px;
        font-size: 12px;
        
        .info-label {
            width: 120px;
            min-width: 120px;
            text-align: right;
            color: #63656E;
            padding-right: 12px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }
        
        .info-value {
            display: flex;
            align-items: center;
            flex: 1;
            color: #313238;
            word-break: break-all;
            position: relative;
            display: flex;
            align-items: center;
            
            &.info-value-multiline {
                line-height: 22px;
                align-items: flex-start;
            }
            
            .edit-icon {
                margin-left: 8px;
                color: #3A84FF;
                cursor: pointer;
                font-size: 14px;
                flex-shrink: 0;
                
                &:hover {
                    color: #699DF4;
                }
            }
            
            .save-icon,
            .cancel-icon {
                margin-left: 8px;
                cursor: pointer;
                font-size: 16px;
                flex-shrink: 0;
            }
            
            .save-icon {
                color: #2DCB56;
                
                &:hover {
                    color: #45E35F;
                }
            }
            
            .loading-icon {
                margin-left: 12px;
                color: #3A84FF;
                animation: rotating 1s linear infinite;
            }
            
            .cancel-icon {
                color: #FF5656;
                
                &:hover {
                    color: #FF7676;
                }
                
                &.disabled {
                    color: #C4C6CC;
                    cursor: not-allowed;
                    
                    &:hover {
                        color: #C4C6CC;
                    }
                }
            }
        }
        .info-input {
            max-width: 400px;
        }
    }
}

@keyframes rotating {
    from {
        transform: rotate(0deg);
    }
    to {
        transform: rotate(360deg);
    }
}
</style>
