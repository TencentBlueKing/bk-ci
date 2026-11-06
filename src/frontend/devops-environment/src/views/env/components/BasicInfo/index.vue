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
                        v-if="field.editable && !envHashId.startsWith('-')"
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
        <section
            v-if="showDockerLimit"
            class="docker-section"
        >
            <h3 class="docker-title">
                <span>{{ $t('environment.dockerBuildGroup') }}</span>
                <bk-popover
                    theme="light"
                    placement="top-start"
                    :max-width="420"
                    ext-cls="docker-build-tip-popover"
                >
                    <i class="devops-icon icon-info-circle docker-tip-icon"></i>
                    <div slot="content">
                        <docker-build-tip />
                    </div>
                </bk-popover>
            </h3>
            <div class="info-item">
                <span class="info-label">
                    <span
                        class="limit-hint"
                        v-bk-tooltips="cpuTipOptions"
                    >{{ $t('environment.containerCpuLimit') }}</span>：
                </span>
                <span class="info-value">
                    <template v-if="editingField !== 'cpu'">
                        {{ cpuText }}
                        <i
                            v-if="canEditLimit"
                            class="bk-icon icon-edit-line edit-icon"
                            v-perm="{
                                permissionData: {
                                    projectId: projectId,
                                    resourceType: ENV_RESOURCE_TYPE,
                                    resourceCode: envHashId,
                                    action: ENV_RESOURCE_ACTION.EDIT
                                }
                            }"
                            @click="handleStartEdit('cpu')"
                        ></i>
                    </template>
                    <template v-else>
                        <bk-input
                            class="info-input limit-input"
                            v-model="cpuDraft"
                            :disabled="isSaving"
                            :placeholder="$t('environment.concurrencyUnlimited')"
                            ref="editInput"
                        />
                        <span class="limit-suffix">{{ $t('environment.cpuCore') }}</span>
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
            <div class="info-item">
                <span class="info-label">
                    <span
                        class="limit-hint"
                        v-bk-tooltips="memoryTipOptions"
                    >{{ $t('environment.containerMemoryLimit') }}</span>：
                </span>
                <span class="info-value">
                    <template v-if="editingField !== 'memory'">
                        {{ memoryText }}
                        <i
                            v-if="canEditLimit"
                            class="bk-icon icon-edit-line edit-icon"
                            v-perm="{
                                permissionData: {
                                    projectId: projectId,
                                    resourceType: ENV_RESOURCE_TYPE,
                                    resourceCode: envHashId,
                                    action: ENV_RESOURCE_ACTION.EDIT
                                }
                            }"
                            @click="handleStartEdit('memory')"
                        ></i>
                    </template>
                    <template v-else>
                        <bk-input
                            class="mem-amount"
                            v-model="memAmount"
                            :disabled="isSaving"
                            :placeholder="$t('environment.concurrencyUnlimited')"
                            ref="editInput"
                        />
                        <bk-select
                            v-model="memUnit"
                            class="mem-unit"
                            :clearable="false"
                            :disabled="isSaving"
                        >
                            <bk-option
                                id="m"
                                name="m"
                            />
                            <bk-option
                                id="g"
                                name="g"
                            />
                        </bk-select>
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
        </section>
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
    import useEnvAside from '@/hooks/useEnvAside'
    import { convertTime } from '@/utils/util'
    import { OS_LABEL_MAP } from '@/store/constants'
    import DockerBuildTip from '@/components/DockerBuildTip.vue'
    import {
        cpuLimitError,
        formatMemoryLimit,
        isUnlimited,
        joinMemoryLimit,
        limitErrorKey,
        memoryLimitError,
        normalizeCpu,
        normalizeMemory,
        parseMemoryLimit
    } from '@/utils/containerLimit'
    
    export default {
        name: 'BasicInfo',
        components: {
            DockerBuildTip
        },
        setup () {
            const { proxy } = useInstance()
            const {
                currentEnv,
                envHashId,
                projectId,
                isBuildEnv,
                fetchEnvDetail,
                updateEnvDetail
            } = useEnvDetail()
            const {
                isCreateResType
            } = useEnvAside()
            
            const isLoading = ref(false)
            const isSaving = ref(false)
            const editingField = ref(null)
            const editingValue = ref('')
            const editInput = ref(null)
            const dockerConfig = ref({ cpus: '', memory: '' })
            const cpuDraft = ref('')
            const memAmount = ref('')
            const memUnit = ref('g')
            const showDockerLimit = computed(() => isBuildEnv.value && !isCreateResType.value)
            const canEditLimit = computed(() => !String(envHashId.value || '').startsWith('-'))
            const limitTip = (text) => ({
                allowHTML: true,
                theme: 'light',
                placements: ['top-start'],
                maxWidth: 360,
                content: `<div class="container-limit-tip"><p>${text}</p></div>`
            })
            const cpuTipOptions = computed(() => limitTip(proxy.$t('environment.cpuFormatTip')))
            const memoryTipOptions = computed(() => limitTip(proxy.$t('environment.memoryFormatTip')))
            const cpuText = computed(() => {
                const text = normalizeCpu(dockerConfig.value.cpus)
                if (!text) return proxy.$t('environment.concurrencyUnlimited')
                return `${text} ${proxy.$t('environment.cpuCore')}`
            })
            const memoryText = computed(() => {
                return formatMemoryLimit(dockerConfig.value.memory) || proxy.$t('environment.concurrencyUnlimited')
            })

            let configRequest = 0
            const loadDockerConfig = async () => {
                const requestId = ++configRequest
                const hashId = envHashId.value
                if (!showDockerLimit.value || !hashId || currentEnv.value?.envHashId !== hashId) {
                    dockerConfig.value = { cpus: '', memory: '' }
                    return
                }
                try {
                    const res = await proxy.$store.dispatch('environment/requestDockerConfig', {
                        projectId: projectId.value,
                        scope: 'ENV',
                        resourceId: hashId
                    })
                    if (requestId !== configRequest) return
                    dockerConfig.value = {
                        cpus: res?.cpus ?? '',
                        memory: res?.memory ?? ''
                    }
                } catch (e) {
                    if (requestId !== configRequest) return
                    proxy.$bkMessage({
                        theme: 'error',
                        message: e.message || e
                    })
                }
            }
            watch(() => envHashId.value, () => {
                getEnvDetail()
            })
            watch(
                () => [envHashId.value, currentEnv.value?.envHashId, currentEnv.value?.envType, isCreateResType.value],
                () => {
                    loadDockerConfig()
                },
                { immediate: true }
            )
            const infoFields = computed(() => [
                {
                    key: 'name',
                    label: proxy.$t('environment.environmentName'),
                    editable: true,
                    type: 'input',
                    maxlength: 30
                },
                ...(isCreateResType.value ? [
                    {
                        key: 'os',
                        label: proxy.$t('environment.nodeInfo.os'),
                        editable: false
                    }
                ] : []),
                ...(!isCreateResType.value ? [
                    {
                        key: 'envType',
                        label: proxy.$t('environment.environmentType'),
                        editable: false
                    }
                ] : []
                ),
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
                if (key === 'os' && value) {
                    return OS_LABEL_MAP[value] || value
                }
                return value
            }
            
            // 开始编辑
            const handleStartEdit = (fieldKey) => {
                editingField.value = fieldKey
                if (fieldKey === 'cpu') {
                    cpuDraft.value = isUnlimited(dockerConfig.value.cpus) ? '' : normalizeCpu(dockerConfig.value.cpus)
                } else if (fieldKey === 'memory') {
                    const parsed = parseMemoryLimit(isUnlimited(dockerConfig.value.memory) ? '' : dockerConfig.value.memory)
                    memAmount.value = parsed.amount
                    memUnit.value = parsed.unit
                } else {
                    editingValue.value = getFieldValue(fieldKey) || ''
                }
                nextTick(() => {
                    if (editInput.value) {
                        const input = Array.isArray(editInput.value) ? editInput.value[0] : editInput.value
                        input?.focus?.()
                    }
                })
            }
            
            // 保存编辑
            const saveDockerLimit = async (fieldKey) => {
                const params = {}
                if (fieldKey === 'cpu') {
                    const raw = normalizeCpu(cpuDraft.value)
                    const code = cpuLimitError(raw)
                    if (code) {
                        proxy.$bkMessage({ theme: 'error', message: proxy.$t(limitErrorKey('cpu', code)) })
                        return
                    }
                    if (raw === normalizeCpu(dockerConfig.value.cpus)) {
                        handleCancelEdit()
                        return
                    }
                    params.cpus = raw
                } else {
                    const raw = normalizeMemory(joinMemoryLimit(memAmount.value, memUnit.value))
                    const code = memoryLimitError(raw)
                    if (code) {
                        proxy.$bkMessage({ theme: 'error', message: proxy.$t(limitErrorKey('memory', code)) })
                        return
                    }
                    const current = isUnlimited(dockerConfig.value.memory) ? '' : normalizeMemory(dockerConfig.value.memory)
                    if (raw === current) {
                        handleCancelEdit()
                        return
                    }
                    params.memory = raw
                }
                try {
                    isSaving.value = true
                    await proxy.$store.dispatch('environment/saveDockerConfig', {
                        projectId: projectId.value,
                        scope: 'ENV',
                        resourceId: envHashId.value,
                        params
                    })
                    dockerConfig.value = { ...dockerConfig.value, ...params }
                    proxy.$bkMessage({
                        theme: 'success',
                        message: proxy.$t('environment.successfullySaved')
                    })
                    handleCancelEdit()
                } catch (err) {
                    proxy.$bkMessage({
                        theme: 'error',
                        message: err.message || err
                    })
                } finally {
                    isSaving.value = false
                }
            }

            const handleSaveEdit = async () => {
                if (!editingField.value || isSaving.value) return
                if (editingField.value === 'cpu' || editingField.value === 'memory') {
                    await saveDockerLimit(editingField.value)
                    return
                }
                
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
                isCreateResType,
                ENV_RESOURCE_ACTION,
                ENV_RESOURCE_TYPE,

                showDockerLimit,
                canEditLimit,
                cpuText,
                memoryText,
                cpuTipOptions,
                memoryTipOptions,
                cpuDraft,
                memAmount,
                memUnit,
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
        margin-bottom: 24px;
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

    .docker-section {
        margin-top: 8px;
        padding-top: 16px;
        border-top: 1px solid #f0f1f5;
    }

    .docker-title {
        display: flex;
        align-items: center;
        margin: 0 0 16px;
        font-size: 14px;
        font-weight: 700;
        line-height: 22px;
        color: #313238;
    }

    .docker-tip-icon {
        margin-left: 4px;
        color: #979ba5;
        font-size: 14px;
    }

    .inline-popover {
        display: inline-flex;
        align-items: center;
    }

    .limit-hint {
        border-bottom: 1px dashed #979ba5;
        cursor: help;
    }

    .limit-input {
        max-width: 160px;
    }

    .limit-suffix {
        margin-left: 8px;
        color: #63656e;
        flex-shrink: 0;
    }

    .mem-amount {
        width: 120px;
    }

    .mem-unit {
        width: 72px;
        margin-left: 8px;
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
