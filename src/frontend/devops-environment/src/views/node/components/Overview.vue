<template>
    <div class="node-overview-container">
        <div class="overview-main">
            <div class="overview-section">
                <div class="node-basic-info info-grid">
                    <div
                        v-for="field in basicInfoFields"
                        :key="field.key"
                        class="info-item"
                    >
                        <span class="info-label">{{ field.label }}:</span>
                        <span class="info-value">{{ getFieldValue(field) }}</span>
                    </div>
                </div>
                <div class="section-header mt10">
                    <span class="section-title">{{ $t('environment.buildAgent') }}</span>
                </div>
                <div class="agent-info info-grid">
                    <div
                        v-for="field in agentInfoFields"
                        :key="field.key"
                        class="info-item"
                    >
                        <span class="info-label">{{ getFieldLabel(field) }}:</span>
                        <span class="info-value">{{ getFieldValue(field) }}</span>
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

            // 基本信息字段配置
            const basicInfoFields = computed(() => [
                { label: proxy.$t('environment.nodeInfo.hostName'), key: 'hostname' },
                { label: proxy.$t('environment.nodeInfo.os'), key: 'osName' },
                { label: 'IP', key: 'ip' },
                { label: proxy.$t('environment.envInfo.creator'), key: 'createdUser' },
                { label: proxy.$t('environment.envInfo.creationTime'), key: 'createdTime' }
            ])

            // Agent 信息字段配置
            const agentInfoFields = computed(() => [
                { label: proxy.$t('environment.nodeInfo.agentVersion'), key: 'agentVersion' },
                { label: proxy.$t('environment.nodeUsage'), key: 'usage', staticValue: proxy.$t('environment.build') },
                { label: proxy.$t('environment.nodeInfo.workerVersion'), key: 'slaveVersion' },
                { label: proxy.$t('environment.nodeInfo.installPath'), key: 'agentInstallPath' },
                { label: proxy.$t('environment.nodeInfo.lastActiveTime'), key: 'lastHeartbeatTime' },
                { label: proxy.$t('environment.nodeInfo.startUser'), key: 'startedUser' }
            ])

            // 获取字段标签
            const getFieldLabel = (field) => {
                return field.dynamicLabel ? field.dynamicLabel() : field.label
            }

            // 获取字段值
            const getFieldValue = (field) => {
                if (field.staticValue) return field.staticValue
                if (field.dynamicValue) return field.dynamicValue() || '-'
                return currentNode.value?.[field.key] || '-'
            }

            return {
                currentNode,
                basicInfoFields,
                agentInfoFields,
                getFieldLabel,
                getFieldValue
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
