<template>
    <div class="node-overview-container">
        <div class="overview-main">
            <div class="overview-section">
                <div class="node-basic-info">
                    <div class="info-row">
                        <div class="info-item">
                            <span class="info-label">{{ $t('environment.nodeInfo.os') }}:</span>
                            <span class="info-value">{{ currentNode.osName || '-' }}</span>
                        </div>
                        <div class="info-item">
                            <span class="info-label">IP:</span>
                            <span class="info-value">{{ currentNode.ip || '-' }}</span>
                        </div>
                    </div>
                    <div class="info-row">
                        <div class="info-item">
                            <span class="info-label">{{ $t('environment.envInfo.creator') }}:</span>
                            <span class="info-value">{{ currentNode.createdUser || '-' }}</span>
                        </div>
                        <div class="info-item">
                            <span class="info-label">{{ $t('environment.envInfo.creationTime') }}:</span>
                            <span class="info-value">{{ currentNode.createdTime || '-' }}</span>
                        </div>
                    </div>
                </div>
                <div class="section-header mt10">
                    <span class="section-title">{{ $t('environment.buildAgent') }}</span>
                </div>
                <div class="agent-info info-grid">
                    <div class="info-item">
                        <span class="info-label">{{ $t('environment.nodeInfo.agentVersion') }}:</span>
                        <span class="info-value">{{ currentNode.agentVersion || '-' }}</span>
                    </div>
                    <div class="info-item">
                        <span class="info-label">{{ $t('environment.nodeUsage') }}:</span>
                        <span class="info-value">{{ $t('environment.build') }}</span>
                    </div>
                    
                    <div class="info-item">
                        <span class="info-label">{{ $t('environment.nodeInfo.workerVersion') }}:</span>
                        <span class="info-value">{{ currentNode.slaveVersion || '-' }}</span>
                    </div>
                    <div class="info-item">
                        <span class="info-label">{{ $t('environment.nodeInfo.installPath') }}:</span>
                        <span class="info-value">{{ currentNode.agentInstallPath || '-' }}</span>
                    </div>
                    <div class="info-item">
                        <span class="info-label">{{ $t('environment.nodeInfo.lastActiveTime') }}:</span>
                        <span class="info-value">{{ currentNode.lastHeartbeatTime || '-' }}</span>
                    </div>
                    <div class="info-item">
                        <span class="info-label">{{ $t('environment.nodeInfo.startUser') }}:</span>
                        <span class="info-value">{{ currentNode.startedUser || '-' }}</span>
                    </div>
                    <div class="info-item">
                        <span class="info-label">
                            {{ currentNode.os === 'WINDOWS'
                                ? $t('environment.nodeInfo.downloadLink')
                                : $t('environment.nodeInfo.installCommand')
                            }}:</span>
                        <span class="info-value">{{ currentNode.os === 'WINDOWS' ? currentNode.agentUrl : currentNode.agentScript }}</span>
                    </div>
                </div>
            </div>
            <div class="node-overview-title">{{ '机器负载' }}</div>
            <node-overview-chart></node-overview-chart>
        </div>
    </div>
</template>

<script>
    import { computed } from 'vue'
    import useNodeDetail from '@/hooks/useNodeDetail'
    import useInstance from '@/hooks/useInstance'
    import nodeOverviewChart from '@/components/devops/node-overview-chart'

    export default {
        name: 'NodeOverview',
        components: {
            nodeOverviewChart
        },
        setup () {
            const { currentNode } = useNodeDetail()
            const { proxy } = useInstance()

            // 构建 Agent 状态
            const agentStatusClass = computed(() => {
                const status = currentNode.value?.agentStatus
                if (status === 'IMPORT_OK') return 'status-normal'
                if (status === 'IMPORT_EXCEPTION' || status === 'DELETE' || status === 'UN_IMPORT_OK') return 'status-exception'
                return ''
            })

            // 部署 Agent 状态
            const buildAgentStatusClass = computed(() => {
                const status = currentNode.value?.dockerAgentStatus
                if (status === 'IMPORT_OK') return 'status-normal'
                if (status === 'IMPORT_EXCEPTION' || status === 'DELETE' || status === 'UN_IMPORT_OK') return 'status-exception'
                return ''
            })
            // 获取进程名称（保留完整信息）
            const getProcessName = (fullName) => {
                if (!fullName) return '-'
                return fullName
            }

            return {
                currentNode,
                agentStatusClass,
                buildAgentStatusClass,
                getProcessName
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

        .node-basic-info {
            background: transparent;
            margin-bottom: 16px;
        }

        .section-header {
            display: flex;
            align-items: center;
            margin-bottom: 16px;
            
            .status-indicator {
                width: 8px;
                height: 8px;
                border-radius: 50%;
                margin-right: 8px;
                
                &.status-normal {
                    background: #2DCB56;
                }
                
                &.status-exception {
                    background: #EA3636;
                }
            }
            
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

        .info-row {
            display: grid;
            grid-template-columns: repeat(2, 1fr);
            gap: 16px 24px;
            margin-bottom: 16px;

            &.three-cols {
                grid-template-columns: repeat(3, 1fr);
            }

            &:last-child {
                margin-bottom: 0;
            }
        }

        .info-item {
            display: flex;
            align-items: flex-start;
            
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
                color: #313238;
                word-break: break-all;
                line-height: 20px;
                margin-left: 5px;
            }
        }
    }
    .node-overview-title {
        font-weight: 700;
        font-size: 14px;
        color: #63656E;
        margin-top: 20px;
    }
}
</style>
