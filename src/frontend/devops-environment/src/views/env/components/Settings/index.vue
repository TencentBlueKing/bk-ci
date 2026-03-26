<template>
    <div
        class="settings-container"
        v-bkloading="{ isLoading }"
    >
        <div class="info-item">
            <span
                class="info-label"
                v-bk-overflow-tips
            >{{ $t('environment.workspace') }}：</span>
            <span class="info-value">
                <!-- 非编辑状态 -->
                <template v-if="!isEditing">
                    {{ workspaceValue || defaultWorkspace }}
                    <i
                        class="bk-icon icon-edit-line edit-icon"
                        v-perm="{
                            permissionData: {
                                projectId: projectId,
                                resourceType: ENV_RESOURCE_TYPE,
                                resourceCode: envHashId,
                                action: ENV_RESOURCE_ACTION.EDIT
                            }
                        }"
                        @click="handleStartEdit"
                    ></i>
                </template>
                
                <!-- 编辑状态 -->
                <template v-else>
                    <bk-input
                        class="info-input"
                        v-model="editingValue"
                        :placeholder="defaultWorkspace"
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
    import { ref, computed, nextTick } from 'vue'
    import {
        ENV_RESOURCE_ACTION,
        ENV_RESOURCE_TYPE
    } from '@/utils/permission'
    import useInstance from '@/hooks/useInstance'
    import useEnvDetail from '@/hooks/useEnvDetail'
    
    export default {
        name: 'Settings',
        setup () {
            const { proxy } = useInstance()
            const {
                currentEnv,
                envHashId,
                projectId,
                updateEnvDetail
            } = useEnvDetail()
            
            const isLoading = ref(false)
            const isSaving = ref(false)
            const isEditing = ref(false)
            const editingValue = ref('')
            const editInput = ref(null)

            // 默认工作空间路径
            const defaultWorkspace = computed(() => {
                const envId = currentEnv.value?.envHashId || '<创作流 ID>'
                return `<Agent 安装目录>/workspace/${envId}`
            })

            // 当前工作空间值
            const workspaceValue = computed(() => currentEnv.value?.workspace || '')
            
            // 开始编辑
            const handleStartEdit = () => {
                isEditing.value = true
                editingValue.value = workspaceValue.value || ''
                nextTick(() => {
                    if (editInput.value) {
                        const input = Array.isArray(editInput.value) ? editInput.value[0] : editInput.value
                        input?.focus?.()
                    }
                })
            }
            
            // 保存编辑
            const handleSaveEdit = async () => {
                if (isSaving.value) return
                
                try {
                    const newValue = editingValue.value.trim()
                    const oldValue = workspaceValue.value
                    
                    // 如果值没有变化，直接取消编辑
                    if (newValue === oldValue) {
                        handleCancelEdit()
                        return
                    }
                    
                    isSaving.value = true
                    
                    // 更新数据
                    const params = {
                        ...currentEnv.value,
                        workspace: newValue
                    }
                    
                    await updateEnvDetail(params)
                    
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
                isEditing.value = false
                editingValue.value = ''
            }

            return {
                currentEnv,
                isLoading,
                isSaving,
                isEditing,
                editingValue,
                editInput,
                envHashId,
                projectId,
                defaultWorkspace,
                workspaceValue,
                ENV_RESOURCE_ACTION,
                ENV_RESOURCE_TYPE,

                handleStartEdit,
                handleSaveEdit,
                handleCancelEdit
            }
        }
    }
</script>

<style lang="scss" scoped>
.settings-container {
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
