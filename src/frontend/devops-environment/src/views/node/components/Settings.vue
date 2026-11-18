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

            <section
                v-if="supportDocker"
                class="docker-build-group"
            >
                <div class="docker-head">
                    <div
                        class="docker-build-head"
                        role="button"
                        tabindex="0"
                        :aria-expanded="dockerGroupOpen"
                        @click="toggleDockerGroup"
                        @keyup.enter="toggleDockerGroup"
                    >
                        <i :class="['bk-icon', 'docker-chevron', dockerGroupOpen ? 'icon-angle-down' : 'icon-angle-right']"></i>
                        <span class="docker-build-title">{{ $t('environment.dockerBuildGroup') }}</span>
                    </div>
                    <span
                        class="docker-title-tip"
                        @click.stop
                    >
                        <bk-popover
                            theme="light"
                            placement="top-start"
                            :max-width="420"
                            ext-cls="docker-build-tip-popover"
                        >
                            <i class="devops-icon icon-info-circle docker-tip-icon"></i>
                            <div slot="content">
                                <docker-build-tip
                                    :show-value-rule="showContainerLimit"
                                    use-this-node
                                />
                            </div>
                        </bk-popover>
                    </span>
                </div>
                <div
                    v-if="!dockerGroupOpen"
                    class="docker-build-summary"
                    role="button"
                    tabindex="0"
                    @click="toggleDockerGroup"
                    @keyup.enter="toggleDockerGroup"
                >
                    {{ dockerSummary }}
                </div>
                <div
                    v-else
                    class="docker-build-body"
                >
                    <div
                        v-if="showContainerEngine"
                        class="setting-item"
                    >
                        <span class="setting-label">
                            <span
                                class="setting-hint"
                                v-bk-tooltips="engineTipOptions"
                            >{{ $t('environment.containerEngine') }}</span>：
                        </span>
                        <span class="setting-value">
                            <template v-if="editingField !== 'engine'">
                                {{ engineText }}
                                <i
                                    class="bk-icon icon-edit-line edit-icon"
                                    @click="handleStartEdit('engine')"
                                ></i>
                            </template>
                            <template v-else>
                                <bk-radio-group
                                    v-model="engineDraft"
                                    class="engine-radios"
                                >
                                    <bk-radio
                                        value="docker"
                                        :disabled="isSaving"
                                    >
                                        Docker
                                    </bk-radio>
                                    <bk-radio
                                        value="podman"
                                        :disabled="isSaving"
                                    >
                                        Podman
                                    </bk-radio>
                                </bk-radio-group>
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
                    <div class="setting-item">
                        <span class="setting-label">{{ $t('environment.dockerBuildMaxConcurrency') }}：</span>
                        <span class="setting-value">
                            <template v-if="editingField !== 'docker'">
                                {{ currentNode.dockerParallelTaskCount ?? '-' }}
                                <i
                                    class="bk-icon icon-edit-line edit-icon"
                                    @click="handleStartEdit('docker')"
                                ></i>
                            </template>
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
                    <template v-if="showContainerLimit">
                        <div class="setting-item">
                            <span class="setting-label">
                                <span
                                    class="setting-hint"
                                    v-bk-tooltips="cpuTipOptions"
                                >{{ $t('environment.containerCpuDefault') }}</span>：
                            </span>
                            <span class="setting-value">
                                <template v-if="editingField !== 'cpu'">
                                    {{ cpuText }}
                                    <i
                                        class="bk-icon icon-edit-line edit-icon"
                                        @click="handleStartEdit('cpu')"
                                    ></i>
                                </template>
                                <template v-else>
                                    <bk-input
                                        class="info-input"
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
                        <div class="setting-item">
                            <span class="setting-label">
                                <span
                                    class="setting-hint"
                                    v-bk-tooltips="memoryTipOptions"
                                >{{ $t('environment.containerMemoryDefault') }}</span>：
                            </span>
                            <span class="setting-value">
                                <template v-if="editingField !== 'memory'">
                                    {{ memoryText }}
                                    <i
                                        class="bk-icon icon-edit-line edit-icon"
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
                    </template>
                </div>
            </section>
        </div>
        <div class="env-params-content">
            <p class="title">{{ $t('environment.environmentVariable') }}</p>
            <env-param />
        </div>
    </div>
</template>

<script>
    import { ref, nextTick, computed, watch } from 'vue'
    import useNodeDetail from '@/hooks/useNodeDetail'
    import useInstance from '@/hooks/useInstance'
    import { DOCKER_SUPPORTED_OS, osOf } from '@/components/devops/environment/import-third-party/constants'
    import { SERVICE_RESOURCE_TYPE } from '@/store/constants'
    import DockerBuildTip from '@/components/DockerBuildTip.vue'
    import {
        cpuLimitError,
        formatMemoryLimit,
        isLinuxBuildNode,
        isUnlimited,
        joinMemoryLimit,
        limitErrorKey,
        memoryLimitError,
        normalizeCpu,
        normalizeMemory,
        parseMemoryLimit
    } from '@/utils/containerLimit'
    import EnvParam from './EnvParam'

    const LINUX_AGENT_NODE_TYPES = ['THIRDPARTY', 'CREATE']
    const normalizeRuntime = (value) => (value === 'podman' ? 'podman' : 'docker')

    export default {
        name: 'NodeSettings',
        components: {
            EnvParam,
            DockerBuildTip
        },
        setup () {
            const {
                currentNode,
                fetchNodeDetail,
                saveParallelTaskCount,
                saveDockerParallelTaskCount,
                fetchNodeDockerConfig,
                saveNodeDockerConfig,
                nodeHashId
            } = useNodeDetail()
            const { proxy } = useInstance()

            // 仅支持 Docker 构建的操作系统（当前为 LINUX）展示 docker 构建最大并发数
            const supportDocker = computed(() => DOCKER_SUPPORTED_OS.includes(osOf(currentNode.value)))
            // 容器引擎只出现在 Linux 第三方构建机和 Linux 创作节点
            const showContainerEngine = computed(() => {
                const node = currentNode.value
                return LINUX_AGENT_NODE_TYPES.includes(node?.nodeType) && osOf(node) === 'LINUX'
            })
            const showContainerLimit = computed(() => {
                return proxy.$route.params.resType !== SERVICE_RESOURCE_TYPE.CREATE
                    && isLinuxBuildNode(currentNode.value)
            })

            const isSaving = ref(false)
            const editingField = ref(null)
            const editingValue = ref('')
            const editInput = ref(null)
            const dockerGroupOpen = ref(false)
            const containerRuntime = ref('docker')
            const engineDraft = ref('docker')
            const cpuLimit = ref('')
            const memoryLimit = ref('')
            const cpuDraft = ref('')
            const memAmount = ref('')
            const memUnit = ref('g')
            const engineText = computed(() => (containerRuntime.value === 'podman' ? 'Podman' : 'Docker'))
            const limitTip = (html) => ({
                allowHTML: true,
                theme: 'light',
                placements: ['top-start'],
                maxWidth: 360,
                content: `<div class="container-limit-tip">${html}</div>`
            })
            const engineTipOptions = computed(() => limitTip(
                `<p>${proxy.$t('environment.containerEngineNeedInstall')}</p><p>${proxy.$t('environment.containerEngineMissingFail')}</p>`
            ))
            const cpuTipOptions = computed(() => limitTip(`<p>${proxy.$t('environment.cpuFormatTip')}</p>`))
            const memoryTipOptions = computed(() => limitTip(`<p>${proxy.$t('environment.memoryFormatTip')}</p>`))
            const summarySeparator = () => (String(proxy.$i18n?.locale || '').toLowerCase().startsWith('zh') ? '，' : ', ')
            const unlimitedText = () => proxy.$t('environment.concurrencyUnlimited')
            const cpuText = computed(() => {
                const text = normalizeCpu(cpuLimit.value)
                if (!text) return unlimitedText()
                return `${text} ${proxy.$t('environment.cpuCore')}`
            })
            const memoryText = computed(() => formatMemoryLimit(memoryLimit.value) || unlimitedText())
            const dockerSummary = computed(() => {
                const parts = []
                if (showContainerEngine.value) parts.push(engineText.value)
                const concurrency = currentNode.value?.dockerParallelTaskCount ?? '-'
                parts.push(proxy.$t('environment.summaryConcurrency', [concurrency]))
                if (showContainerLimit.value) {
                    parts.push(proxy.$t('environment.summaryCpu', [cpuText.value]))
                    parts.push(proxy.$t('environment.summaryMemory', [memoryText.value]))
                }
                return parts.join(summarySeparator())
            })
            let dockerConfigSeq = 0

            // 开始编辑
            const handleStartEdit = (field) => {
                editingField.value = field
                if (field === 'engine') {
                    engineDraft.value = containerRuntime.value
                    return
                }
                if (field === 'parallel') {
                    editingValue.value = currentNode.value.parallelTaskCount ?? ''
                } else if (field === 'docker') {
                    editingValue.value = currentNode.value.dockerParallelTaskCount ?? ''
                } else if (field === 'cpu') {
                    cpuDraft.value = isUnlimited(cpuLimit.value) ? '' : normalizeCpu(cpuLimit.value)
                } else if (field === 'memory') {
                    const parsed = parseMemoryLimit(isUnlimited(memoryLimit.value) ? '' : memoryLimit.value)
                    memAmount.value = parsed.amount
                    memUnit.value = parsed.unit
                }
                
                nextTick(() => {
                    if (editInput.value) {
                        const input = Array.isArray(editInput.value) ? editInput.value[0] : editInput.value
                        input?.focus?.()
                    }
                })
            }

            // 保存编辑
            const handleSaveEngine = async () => {
                const next = normalizeRuntime(engineDraft.value)
                if (next !== 'docker' && next !== 'podman') return
                if (next === containerRuntime.value) {
                    handleCancelEdit()
                    return
                }

                try {
                    isSaving.value = true
                    await saveNodeDockerConfig({ containerRuntime: next })
                    containerRuntime.value = next
                    proxy.$bkMessage({
                        message: proxy.$t('environment.successfullySaved'),
                        theme: 'success'
                    })
                    handleCancelEdit()
                } catch (e) {
                    proxy.$bkMessage({
                        theme: 'error',
                        message: e.message || e
                    })
                } finally {
                    isSaving.value = false
                }
            }

            const showLimitError = (kind, code) => {
                proxy.$bkMessage({
                    theme: 'error',
                    message: proxy.$t(limitErrorKey(kind, code))
                })
            }

            const saveContainerLimit = async (field) => {
                const params = {}
                if (field === 'cpu') {
                    const raw = normalizeCpu(cpuDraft.value)
                    const code = cpuLimitError(raw)
                    if (code) {
                        showLimitError('cpu', code)
                        return
                    }
                    if (raw === normalizeCpu(cpuLimit.value)) {
                        handleCancelEdit()
                        return
                    }
                    params.cpus = raw
                } else {
                    const raw = normalizeMemory(joinMemoryLimit(memAmount.value, memUnit.value))
                    const code = memoryLimitError(raw)
                    if (code) {
                        showLimitError('memory', code)
                        return
                    }
                    const current = isUnlimited(memoryLimit.value) ? '' : normalizeMemory(memoryLimit.value)
                    if (raw === current) {
                        handleCancelEdit()
                        return
                    }
                    params.memory = raw
                }
                try {
                    isSaving.value = true
                    await saveNodeDockerConfig(params)
                    if (Object.prototype.hasOwnProperty.call(params, 'cpus')) cpuLimit.value = params.cpus
                    if (Object.prototype.hasOwnProperty.call(params, 'memory')) memoryLimit.value = params.memory
                    proxy.$bkMessage({
                        message: proxy.$t('environment.successfullySaved'),
                        theme: 'success'
                    })
                    handleCancelEdit()
                } catch (e) {
                    proxy.$bkMessage({
                        theme: 'error',
                        message: e.message || e
                    })
                } finally {
                    isSaving.value = false
                }
            }

            const handleSaveEdit = async () => {
                if (!editingField.value || isSaving.value) return
                if (editingField.value === 'engine') {
                    await handleSaveEngine()
                    return
                }
                if (editingField.value === 'cpu' || editingField.value === 'memory') {
                    await saveContainerLimit(editingField.value)
                    return
                }
                
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
                engineDraft.value = containerRuntime.value
            }

            const toggleDockerGroup = () => {
                const groupFields = ['engine', 'docker', 'cpu', 'memory']
                if (dockerGroupOpen.value && groupFields.includes(editingField.value) && !isSaving.value) {
                    handleCancelEdit()
                }
                if (isSaving.value && groupFields.includes(editingField.value)) return
                dockerGroupOpen.value = !dockerGroupOpen.value
            }

            const loadDockerConfig = async () => {
                const seq = ++dockerConfigSeq
                try {
                    const config = await fetchNodeDockerConfig()
                    if (seq !== dockerConfigSeq) return
                    const payload = config?.data
                        && config.containerRuntime == null
                        && config.cpus == null
                        && config.memory == null
                        ? config.data
                        : config
                    containerRuntime.value = normalizeRuntime(payload?.containerRuntime)
                    cpuLimit.value = payload?.cpus ?? ''
                    memoryLimit.value = payload?.memory ?? ''
                } catch (e) {
                    if (seq !== dockerConfigSeq) return
                    containerRuntime.value = 'docker'
                    proxy.$bkMessage({
                        theme: 'error',
                        message: e.message || e
                    })
                }
            }

            watch(
                () => [nodeHashId.value, showContainerEngine.value || showContainerLimit.value],
                ([id, show], previous) => {
                    const previousId = previous?.[0]
                    if (id !== previousId) {
                        dockerGroupOpen.value = false
                        containerRuntime.value = 'docker'
                        cpuLimit.value = ''
                        memoryLimit.value = ''
                        if (['engine', 'cpu', 'memory'].includes(editingField.value) && !isSaving.value) handleCancelEdit()
                    }
                    if (!id || !show) return
                    loadDockerConfig()
                },
                { immediate: true }
            )

            return {
                currentNode,
                supportDocker,
                showContainerEngine,
                showContainerLimit,
                dockerGroupOpen,
                dockerSummary,
                engineText,
                engineTipOptions,
                cpuTipOptions,
                memoryTipOptions,
                engineDraft,
                cpuDraft,
                memAmount,
                memUnit,
                cpuText,
                memoryText,
                isSaving,
                editingField,
                editingValue,
                editInput,
                toggleDockerGroup,
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

        .setting-label {
            flex: 0 0 160px;
            width: 160px;
        }
    }

    .docker-head {
        display: flex;
        align-items: center;
        height: 22px;
    }

    .docker-title-tip {
        display: inline-flex;
        align-items: center;
        margin-left: 4px;
        line-height: 0;
    }

    .docker-tip-icon {
        color: #979BA5;
        font-size: 14px;
        cursor: pointer;
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
        flex-shrink: 0;
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

            .engine-radios {
                display: inline-flex !important;
                align-items: center;
                flex: 0 0 auto !important;
                width: auto !important;
                max-width: none;

                ::v-deep .bk-form-radio,
                ::v-deep .bk-radio {
                    margin-right: 24px;
                    font-size: 12px;
                }

                ::v-deep .bk-radio-text,
                ::v-deep .bk-radio-label {
                    font-size: 12px;
                }
            }
        }
    }

    .docker-build-group {
        margin-top: 16px;
        padding-top: 16px;
        border-top: 1px solid #DCDEE5;
    }

    .docker-build-head,
    .docker-build-summary {
        margin: 0;
        padding: 0;
        border: 0;
        border-radius: 0;
        background: transparent;
        box-shadow: none;
        cursor: pointer;
        font: inherit;
        text-align: left;
        outline: none;
    }

    .docker-build-head {
        display: inline-flex;
        align-items: center;
        height: 22px;
        color: #313238;

        .docker-chevron {
            width: 12px;
            margin-right: 4px;
            color: #979BA5;
            font-size: 12px;
            line-height: 12px;
        }
    }

    .docker-build-title {
        font-size: 14px;
        font-weight: 700;
        line-height: 22px;
    }

    .docker-build-summary {
        display: block;
        width: auto;
        max-width: none;
        height: auto;
        margin-top: 4px;
        margin-left: 16px;
        overflow: visible;
        white-space: normal;
        font-size: 12px;
        line-height: 20px;
        color: #63656E;

        &:hover {
            color: #3A84FF;
        }
    }

    .docker-build-body {
        margin-top: 12px;

        .setting-item {
            margin-bottom: 16px;
        }

        .setting-item:last-child {
            margin-bottom: 0;
        }
    }

    .setting-hint {
        border-bottom: 1px dashed #979BA5;
        cursor: help;
    }

    .setting-label {
        white-space: nowrap;
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

<style lang="scss">
.node-settings-container .docker-title-tip .bk-tooltip,
.node-settings-container .docker-title-tip .bk-tooltip-ref {
    display: inline-flex !important;
    align-items: center;
    width: auto !important;
    min-width: 0 !important;
    min-height: 0 !important;
    height: auto !important;
    padding: 0 !important;
    border: none !important;
    background: transparent !important;
    box-shadow: none !important;
    line-height: 0 !important;
    vertical-align: middle;
}

.tippy-tooltip:has(.container-limit-tip) {
    background-color: #fff !important;
    color: #63656e !important;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
    border: 1px solid #dcdee5;
}

.tippy-popper[x-placement^='top'] .tippy-tooltip:has(.container-limit-tip) .tippy-arrow {
    border-top-color: #fff !important;
}

.tippy-popper[x-placement^='bottom'] .tippy-tooltip:has(.container-limit-tip) .tippy-arrow {
    border-bottom-color: #fff !important;
}

.tippy-popper[x-placement^='left'] .tippy-tooltip:has(.container-limit-tip) .tippy-arrow {
    border-left-color: #fff !important;
}

.tippy-popper[x-placement^='right'] .tippy-tooltip:has(.container-limit-tip) .tippy-arrow {
    border-right-color: #fff !important;
}

.container-limit-tip {
    max-width: 320px;
    text-align: left;
    font-size: 12px;
    line-height: 20px;
    color: #63656e;
    white-space: normal;
    word-break: break-word;
}

.container-limit-tip p {
    margin: 0;
}

.container-limit-tip p + p {
    margin-top: 4px;
}
</style>