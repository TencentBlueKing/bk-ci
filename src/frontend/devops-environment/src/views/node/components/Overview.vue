<template>
    <div class="node-overview-container">
        <div class="overview-main">
            <div class="overview-section">
                <!-- 基本信息：创作流 ID + 所属环境；流水线 机器 / 规格 / 所属环境 / 创建信息 -->
                <div class="basic-rows">
                    <template v-for="block in basicBlocks">
                        <div
                            v-if="block.type === 'id'"
                            :key="block.key"
                            class="info-item"
                        >
                            <span class="info-label">{{ workspaceIdLabel }}:</span>
                            <span
                                class="info-value"
                                :class="{ 'is-copyable': !!workspaceId }"
                            >
                                {{ workspaceId || '-' }}
                                <button
                                    v-if="workspaceId"
                                    type="button"
                                    class="id-copy"
                                    @click="handleCopy(workspaceId)"
                                >
                                    {{ $t('environment.copy') }}
                                </button>
                            </span>
                        </div>
                        <div
                            v-else-if="block.type === 'env'"
                            :key="`env-${block.key}`"
                            class="info-item env-row"
                        >
                            <span class="info-label">{{ $t('environment.belongingEnv') }}:</span>
                            <div class="env-list">
                                <bk-tag
                                    v-for="env in envRelations"
                                    :key="env.envHashId || env.envName"
                                    class="env-tag"
                                    size="small"
                                >
                                    <a
                                        class="env-open"
                                        :href="env.url || 'javascript:;'"
                                        :target="env.url ? '_blank' : undefined"
                                        rel="noopener noreferrer"
                                        @click="openEnv(env, $event)"
                                    >
                                        <span class="env-name">
                                            {{ env.envName }}
                                            <span
                                                v-if="env.strategyType"
                                                class="env-kind"
                                            > · {{ env.strategyType === 'dynamic' ? $t('environment.dynamic') : $t('environment.static') }}</span>
                                        </span>
                                        <svg
                                            class="env-jump"
                                            viewBox="0 0 1024 1024"
                                            aria-hidden="true"
                                        >
                                            <path d="M864 128H496v64h291.2L361.6 617.6l44.8 44.8L832 236.8V528h64V160c0-17.6-14.4-32-32-32z" />
                                            <path d="M832 704v128H192V192h126.4V128H160c-17.6 0-32 14.4-32 32v704c0 17.6 14.4 32 32 32h704c17.6 0 32-14.4 32-32V704h-64z" />
                                        </svg>
                                    </a>
                                </bk-tag>
                                <span
                                    v-if="!envRelations.length"
                                    class="env-empty"
                                >{{ $t('environment.notInAnyEnv') }}</span>
                            </div>
                        </div>
                        <div
                            v-else
                            :key="block.key"
                            class="info-grid"
                        >
                            <div
                                v-for="field in block.fields"
                                :key="field.key"
                                class="info-item"
                            >
                                <span class="info-label">{{ field.label }}:</span>
                                <span class="info-value">{{ getFieldValue(field) }}</span>
                            </div>
                        </div>
                    </template>
                </div>

                <!-- 已释放/已销毁：仅保留上方基本信息 -->
                <template v-if="!isNeutralCreateNode">
                    <!-- 数字分身：入驻的云桌面 -->
                    <template v-if="isCreateResType && isDigitalTwinNode">
                        <div class="section-header">
                            <span class="section-title">{{ $t('environment.residentCloudDesktop') }}</span>
                        </div>
                        <div class="info-grid">
                            <div
                                v-for="field in cloudDesktopFields"
                                :key="field.key"
                                class="info-item"
                            >
                                <span class="info-label">{{ field.label }}:</span>
                                <span
                                    class="info-value"
                                    :class="{ 'is-copyable': field.copy && getFieldValue(field) !== '-' }"
                                >
                                    {{ getFieldValue(field) }}
                                    <button
                                        v-if="field.copy && getFieldValue(field) !== '-'"
                                        type="button"
                                        class="id-copy"
                                        @click="handleCopy(getFieldValue(field))"
                                    >
                                        {{ $t('environment.copy') }}
                                    </button>
                                </span>
                            </div>
                        </div>
                    </template>

                    <!-- Agent 区块 -->
                    <div class="section-header">
                        <span class="section-title">
                            {{ isCreateResType ? $t('environment.creationAgent') : $t('environment.buildAgent') }}
                        </span>
                    </div>
                    <div class="info-grid">
                        <div
                            v-for="field in agentInfoFields"
                            :key="field.key"
                            class="info-item"
                        >
                            <span class="info-label">{{ getFieldLabel(field) }}:</span>
                            <span
                                class="info-value"
                                :class="{ 'is-copyable': field.copy && getFieldValue(field) !== '-' }"
                            >
                                {{ getFieldValue(field) }}
                                <button
                                    v-if="field.copy && getFieldValue(field) !== '-'"
                                    type="button"
                                    class="id-copy"
                                    @click="handleCopy(getFieldValue(field))"
                                >
                                    {{ $t('environment.copy') }}
                                </button>
                            </span>
                        </div>
                    </div>
                </template>
            </div>

            <template v-if="!isNeutralCreateNode">
                <div class="node-overview-title">{{ $t('environment.nodeLoad') }}</div>
                <node-overview-chart></node-overview-chart>
            </template>
        </div>
    </div>
</template>

<script>
    import { computed } from 'vue'
    import useNodeDetail from '@/hooks/useNodeDetail'
    import useInstance from '@/hooks/useInstance'
    import useResourceType from '@/hooks/useResourceType'
    import nodeOverviewChart from '@/components/devops/node-overview-chart'
    const DEPLOYMENT_NODE_TYPES = ['CC', 'CMDB', 'TSTACK', 'UNKNOWN', 'OTHER']

    export default {
        name: 'NodeOverview',
        components: {
            nodeOverviewChart
        },
        setup () {
            // 所属环境已在 fetchNodeDetail 内拉取完毕，页面 loading 与此对齐
            const { currentNode, nodeEnvRelations } = useNodeDetail()
            const { proxy } = useInstance()
            const {
                isCreateResType: checkCreate,
                isDigitalTwin,
                isNeutralStatusNode,
                isCloudDesktop
            } = useResourceType()

            const envRelations = computed(() => nodeEnvRelations.value || [])

            const isCreateResType = computed(() => checkCreate(proxy.$route))
            const isDigitalTwinNode = computed(() => isDigitalTwin(currentNode.value))
            const isNeutralCreateNode = computed(() => {
                return isCreateResType.value && isNeutralStatusNode(currentNode.value)
            })

            const workspaceId = computed(() => currentNode.value?.createWorkspaceId || '')
            const workspaceIdLabel = computed(() => {
                if (isCloudDesktop(currentNode.value)) {
                    return proxy.$t('environment.cloudDesktopIdLabel')
                }
                return proxy.$t('environment.digitalTwinId')
            })

            // 流水线节点用途：部署类节点 → 部署，其余（第三方构建机等）→ 构建
            const usageDisplay = computed(() => {
                const nodeType = currentNode.value?.nodeType
                if (!nodeType) return '-'
                return DEPLOYMENT_NODE_TYPES.includes(nodeType)
                    ? proxy.$t('environment.deploy')
                    : proxy.$t('environment.build')
            })
            // 硬件规格无数据时后端返回空串或 "0"
            const formatSpec = (value) => {
                const text = String(value ?? '').trim()
                return text && text !== '0' ? text : '-'
            }

            // 基本信息分块：创作流 ID + 所属环境；流水线 机器 / 规格 / 所属环境 / 创建信息
            const basicBlocks = computed(() => {
                if (isCreateResType.value) {
                    return [
                        { type: 'id', key: 'id' },
                        { type: 'env', key: 'env' }
                    ]
                }
                return [
                    {
                        type: 'grid',
                        key: 'machine',
                        fields: [
                            { label: proxy.$t('environment.nodeInfo.hostName'), key: 'hostname' },
                            { label: proxy.$t('environment.nodeInfo.os'), key: 'osName' },
                            { label: 'IP', key: 'ip' },
                            { label: proxy.$t('environment.nodeInfo.usage'), key: 'usage', dynamicValue: () => usageDisplay.value }
                        ]
                    },
                    {
                        type: 'grid',
                        key: 'spec',
                        fields: [
                            { label: proxy.$t('environment.cpuCores'), key: 'ncpus', dynamicValue: () => formatSpec(currentNode.value?.ncpus) },
                            { label: proxy.$t('environment.nodeInfo.ram'), key: 'memTotal', dynamicValue: () => formatSpec(currentNode.value?.memTotal) }
                        ]
                    },
                    { type: 'env', key: 'env' },
                    {
                        type: 'grid',
                        key: 'audit',
                        fields: [
                            { label: proxy.$t('environment.envInfo.creator'), key: 'createdUser' },
                            { label: proxy.$t('environment.envInfo.creationTime'), key: 'createdTime' }
                        ]
                    }
                ]
            })

            // 入驻的云桌面：createWorkspaceId / displayName / osName / ip 取节点列表
            const formatOsName = (name) => {
                if (!name) return '-'
                const text = String(name).trim()
                if (!text) return '-'
                return text.charAt(0).toUpperCase() + text.slice(1).toLowerCase()
            }
            const cloudDesktopFields = computed(() => [
                {
                    label: proxy.$t('environment.cloudDesktopIdLabel'),
                    key: 'createWorkspaceId',
                    copy: true,
                    dynamicValue: () => currentNode.value?.createWorkspaceId || '-'
                },
                {
                    label: proxy.$t('environment.nodeInfo.displayName'),
                    key: 'displayName',
                    dynamicValue: () => currentNode.value?.displayName || '-'
                },
                {
                    label: proxy.$t('environment.nodeInfo.os'),
                    key: 'osName',
                    dynamicValue: () => formatOsName(
                        currentNode.value?.osName || currentNode.value?.os
                    )
                },
                {
                    label: 'IP',
                    key: 'ip',
                    dynamicValue: () => currentNode.value?.ip || '-'
                }
            ])

            // Agent 信息：Agent ID → 版本 → Worker → 路径 → 心跳 → 启动用户；创作流末尾追加用途
            const agentInfoFields = computed(() => {
                const fields = [
                    {
                        label: 'Agent ID',
                        key: 'agentId',
                        copy: true,
                        dynamicValue: () => currentNode.value?.agentId || currentNode.value?.agentHashId || '-'
                    },
                    { label: proxy.$t('environment.nodeInfo.agentVersion'), key: 'agentVersion' },
                    { label: proxy.$t('environment.nodeInfo.workerVersion'), key: 'slaveVersion' },
                    { label: proxy.$t('environment.nodeInfo.installPath'), key: 'agentInstallPath' },
                    { label: proxy.$t('environment.nodeInfo.lastActiveTime'), key: 'lastHeartbeatTime' },
                    { label: proxy.$t('environment.nodeInfo.startUser'), key: 'startedUser' }
                ]
                if (isCreateResType.value) {
                    fields.push({
                        label: proxy.$t('environment.nodeUsage'),
                        key: 'usage',
                        dynamicValue: () => currentNode.value?.usage
                            || currentNode.value?.nodeUsage
                            || '-'
                    })
                }
                return fields
            })

            const getFieldLabel = (field) => {
                return field.dynamicLabel ? field.dynamicLabel() : field.label
            }

            const getFieldValue = (field) => {
                if (field.dynamicValue) return field.dynamicValue() || '-'
                return currentNode.value?.[field.key] || '-'
            }

            const handleCopy = async (value) => {
                if (!value || value === '-') return
                try {
                    await navigator.clipboard.writeText(value)
                    proxy.$bkMessage({
                        theme: 'success',
                        message: proxy.$t('environment.copySuccess')
                    })
                } catch (e) {
                    const input = document.createElement('input')
                    input.value = value
                    document.body.appendChild(input)
                    input.select()
                    document.execCommand('copy')
                    document.body.removeChild(input)
                    proxy.$bkMessage({
                        theme: 'success',
                        message: proxy.$t('environment.copySuccess')
                    })
                }
            }

            const openEnv = (env, event) => {
                if (!env?.url) {
                    event.preventDefault()
                }
            }

            return {
                currentNode,
                basicBlocks,
                agentInfoFields,
                cloudDesktopFields,
                getFieldLabel,
                getFieldValue,
                isCreateResType,
                isDigitalTwinNode,
                isNeutralCreateNode,
                workspaceId,
                workspaceIdLabel,
                envRelations,
                handleCopy,
                openEnv
            }
        }
    }
</script>

<style lang="scss" scoped>
.node-overview-container {
    height: 100%;
    overflow-y: auto;

    .overview-main {
        display: flex;
        flex-direction: column;
        gap: 16px;
        padding-bottom: 16px;
    }

    .overview-section {
        background: #FAFBFD;
        border-radius: 2px;
        padding: 16px;
    }

    .basic-rows {
        display: flex;
        flex-direction: column;
        gap: 16px;

        & + .section-header {
            margin-top: 16px;
        }
    }

    .section-header {
        display: flex;
        align-items: center;
        margin: 16px 0;

        .section-title {
            font-size: 14px;
            font-weight: 700;
            color: #63656E;
            position: relative;
            padding-left: 12px;

            &::before {
                content: '';
                position: absolute;
                left: 0;
                top: 50%;
                transform: translateY(-50%);
                width: 4px;
                height: 16px;
                background: #A3C5FD;
                border-radius: 1px;
            }
        }
    }

    .info-grid {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 16px 24px;
    }

    .info-item {
        display: flex;
        align-items: flex-start;
        align-items: center;

        .info-label {
            font-size: 12px;
            color: #979BA5;
            min-width: 100px;
            flex-shrink: 0;
            line-height: 20px;
            text-align: right;
        }

        .info-value {
            font-size: 12px;
            color: #63656E;
            line-height: 20px;
            margin-left: 8px;
            word-break: break-all;

            &.is-copyable {
                display: inline-flex;
                align-items: center;
                gap: 8px;
            }
        }
    }

    .id-copy {
        display: none;
        border: 0;
        padding: 0;
        background: none;
        color: #3A84FF;
        font-size: 12px;
        line-height: 20px;
        cursor: pointer;
    }

    .info-value.is-copyable:hover .id-copy {
        display: inline;
    }


    .info-item.env-row {
        .info-label {
            height: 22px;
            line-height: 22px;
        }
    }

    .env-list {
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        gap: 8px;
        flex: 1;
        min-width: 0;
        margin-left: 8px;
    }

    .env-tag {
        margin: 0 !important;
        height: 22px;
        line-height: 20px;
        display: inline-flex;
        align-items: center;
    }

    .env-open {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        padding: 0 4px;
        margin: 0 -4px;
        border-radius: 2px;
        color: #63656E;
        cursor: pointer;
        line-height: 20px;

        .env-name {
            font-size: 12px;
            line-height: 20px;
            color: #63656E;
        }

        .env-kind {
            color: #979BA5;
        }

        .env-jump {
            width: 12px;
            height: 12px;
            display: block;
            fill: #63656E;
            flex: none;
        }

        &:hover {
            color: #3A84FF;
            background: #E1ECFF;

            .env-name {
                color: #3A84FF;
            }

            // 静态/动态保持灰色，不跟名称一起变蓝
            .env-kind {
                color: #979BA5;
            }

            .env-jump {
                fill: #3A84FF;
            }
        }
    }

    .env-empty {
        font-size: 12px;
        color: #63656E;
        line-height: 22px;
    }

    .node-overview-title {
        font-size: 14px;
        font-weight: 700;
        color: #63656E;
    }
}
</style>
