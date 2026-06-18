<template>
    <div class="node-settings-container">
        <div class="settings-content">
            <!-- 最大构建并发数 -->
            <div class="setting-item">
                <span class="setting-label">最大构建并发数：</span>
                <span class="setting-value">
                    <!-- 非编辑状态 -->
                    <template v-if="editingField !== 'parallel'">
                        {{ currentNode.parallelTaskCount }}
                        <i
                            class="bk-icon icon-edit-line edit-icon"
                            @click="handleStartEdit('parallel')"
                        ></i>
                    </template>
                    
                    <!-- 编辑状态 -->
                    <template v-else>
                        <bk-input
                            class="info-input"
                            v-model="editingValue"
                            type="number"
                            :min="0"
                            :max="100"
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

            <!-- docker构建最大并发数 -->
            <div class="setting-item">
                <span class="setting-label">docker构建最大并发数：</span>
                <span class="setting-value">
                    <!-- 非编辑状态 -->
                    <template v-if="editingField !== 'docker'">
                        {{ currentNode.dockerParallelTaskCount ?? '-' }}
                        <i
                            class="bk-icon icon-edit-line edit-icon"
                            @click="handleStartEdit('docker')"
                        ></i>
                    </template>
                    
                    <!-- 编辑状态 -->
                    <template v-else>
                        <bk-input
                            class="info-input"
                            v-model="editingValue"
                            type="number"
                            :min="0"
                            :max="100"
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
        <div class="env-params-content">
            <p class="title">{{ $t('environment.environmentVariable') }}</p>
            <env-param />
        </div>
    </div>
</template>

<script>
    import { ref, nextTick } from 'vue'
    import useNodeDetail from '@/hooks/useNodeDetail'
    import useInstance from '@/hooks/useInstance'
    import EnvParam from './EnvParam'

    export default {
        name: 'NodeSettings',
        components: {
            EnvParam
        },
        setup () {
            const {
                currentNode,
                fetchNodeDetail,
                saveParallelTaskCount,
                saveDockerParallelTaskCount
            } = useNodeDetail()
            const { proxy } = useInstance()

            const isSaving = ref(false)
            const editingField = ref(null)
            const editingValue = ref('')
            const editInput = ref(null)

            // 开始编辑
            const handleStartEdit = (field) => {
                editingField.value = field
                if (field === 'parallel') {
                    editingValue.value = currentNode.value.parallelTaskCount ?? ''
                } else if (field === 'docker') {
                    editingValue.value = currentNode.value.dockerParallelTaskCount ?? ''
                }
                
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
                
                const count = parseInt(editingValue.value)
                if (isNaN(count) || count < 0 || count > 100) {
                    proxy.$bkMessage({
                        message: proxy.$t('environment.nodeInfo.parallelTaskCountTips'),
                        theme: 'error'
                    })
                    return
                }

                const field = editingField.value
                const oldValue = field === 'parallel'
                    ? currentNode.value.parallelTaskCount
                    : currentNode.value.dockerParallelTaskCount
                
                // 如果值没有变化，直接取消编辑
                if (count === oldValue) {
                    handleCancelEdit()
                    return
                }

                try {
                    isSaving.value = true
                    
                    if (field === 'parallel') {
                        await saveParallelTaskCount(count)
                    } else if (field === 'docker') {
                        await saveDockerParallelTaskCount(count)
                    }
                    
                    proxy.$bkMessage({
                        message: proxy.$t('environment.successfullySaved'),
                        theme: 'success'
                    })
                    
                    // 刷新节点详情
                    await fetchNodeDetail()
                    handleCancelEdit()
                    isSaving.value = false
                } catch (e) {
                    proxy.$bkMessage({
                        theme: 'error',
                        message: e.message || e
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
                currentNode,
                isSaving,
                editingField,
                editingValue,
                editInput,
                handleStartEdit,
                handleSaveEdit,
                handleCancelEdit
            }
        }
    }
</script>

<style lang="scss" scoped>
.node-settings-container {
    padding: 20px 0;
    height: 100%;
    overflow-y: auto;
    
    .settings-content {
        background: #FAFBFD;
        border-radius: 2px;
        padding: 16px;
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 16px 24px;
    }

    .setting-item {
        display: flex;
        align-items: center;
        line-height: 20px;
        font-size: 12px;

        .setting-label {
            min-width: 160px;
            color: #63656E;
            padding-right: 4px;
            text-align: right;
        }

        .setting-value {
            display: flex;
            align-items: center;
            flex: 1;
            color: #313238;

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

            .info-input {
                max-width: 200px;
            }
        }
    }

    .env-params-content {
        margin-top: 20px;
        .title {
            font-weight: 700;
            font-size: 14px;
            color: #63656E;
            margin-bottom: 20px;
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
