<template>
    <div class="monitor-section half-section">
        <div class="section-title">{{ $t('realTimeMonitor.currentPipelineStatus') }}</div>
        <div
            v-bkloading="{ isLoading: loading }"
            class="capacity-grid"
        >
            <capacity-card
                v-for="item in currentStatus"
                :key="item.label"
                :label="$t(`realTimeMonitor.${item.label}`)"
                :value="item.value"
                :show-logo="item.showLogo"
                :logo-name="item.logoName"
                @click="handleClick(item.id)"
            />
        </div>
    </div>
</template>

<script>
    import { defineComponent, ref, onMounted, watch } from 'vue'
    import CapacityCard from './CapacityCard.vue'
    import { runningPipelines, waitingPipelines, waitingJob, auditPipelines } from './constant'
    import useInstance from '@/hook/useInstance'
    export default defineComponent({
        name: 'CurrentPipelineStatus',
        components: {
            CapacityCard
        },
        props: {
            timeRange: {
                type: Array,
                required: true
            },
            onItemClick: {
                type: Function,
                default: () => {}
            }
        },
        emits: ['item-click'],
        setup (props, { emit }) {
            const { proxy } = useInstance()
            const loading = ref(false)
            const currentStatus = ref([
                { id: 'runningPipelines', label: 'runningPipelines', value: null, showLogo: true, logoName: 'running-line' },
                { id: 'waitingPipelines', label: 'queuedPipelines', value: null, showLogo: true, logoName: 'waiting-line' },
                { id: 'waitingJob', label: 'queuedJobs', value: null, showLogo: true, logoName: 'waiting-line' },
                { id: 'auditPipelines', label: 'auditingPipelines', value: null, showLogo: true, logoName: 'audit-line' }
            ])

            /**
             * 提取数据的辅助函数
             */
            const extractValue = (result) => {
                if (!result?.data?.series || result.data.series.length === 0) {
                    return '--'
                }
                const datapoint = result.data.series[0]?.datapoints?.[0][0]
                if (datapoint === undefined || datapoint === null) {
                    return '--'
                }
            
                const numValue = Number(datapoint)
                if (!Number.isInteger(numValue) && !isNaN(numValue)) {
                    return numValue.toFixed(2)
                }
            
                return datapoint
            }

            /**
             * 获取当前流水线状态数据
             */
            const fetchData = async () => {
                if (!props.timeRange || props.timeRange.length < 2) return
            
                loading.value = true
                try {
                    const [originalStartTime, endTime] = props.timeRange
                    let startTime = originalStartTime
                    // 检查时间差是否大于两天（2天 = 2 * 24 * 60 * 60 秒）
                    const twoDaysInMs = 2 * 24 * 60 * 60
                    const timeDiff = endTime - startTime
                    if (timeDiff > twoDaysInMs) {
                        // 如果时间差大于两天，将startTime调整为endTime往前两天
                        startTime = endTime - twoDaysInMs
                    }
                    const promqlConfigs = [
                        { id: 'runningPipelines', promql: runningPipelines },
                        { id: 'waitingPipelines', promql: waitingPipelines },
                        { id: 'waitingJob', promql: waitingJob },
                        { id: 'auditPipelines', promql: auditPipelines }
                    ]
          
                    const results = await Promise.all(
                        promqlConfigs.map(config =>
                            proxy.$store.dispatch('pipelines/getMetrics', {
                                promql: typeof config.promql === 'function'
                                    ? config.promql(endTime - startTime)
                                    : config.promql,
                                start_time: startTime,
                                end_time: endTime
                            })
                        )
                    )

                    currentStatus.value = currentStatus.value.map(item => {
                        const configIndex = promqlConfigs.findIndex(config => config.id === item.id)
                        if (configIndex !== -1) {
                            return { ...item, value: extractValue(results[configIndex]) }
                        }
                        return item
                    })
                } catch (error) {
                    console.error('获取当前流水线状态数据失败:', error)
                } finally {
                    loading.value = false
                }
            }

            const handleClick = (id) => {
                emit('item-click', id)
            }

            // 监听时间范围变化
            watch(() => props.timeRange, fetchData, { deep: true })

            onMounted(() => {
                fetchData()
            })

            return {
                loading,
                currentStatus,
                handleClick,
                fetchData
            }
        }
    })
</script>

<style lang="scss" scoped>
.monitor-section {
    background: #fff;
    border-radius: 2px;
    padding: 16px 24px 24px 24px;
    margin-top: 16px;
    margin-bottom: 16px;

    &.half-section {
        margin-bottom: 0;
    }
}

.section-title {
    font-size: 14px;
    color: #313238;
    margin-bottom: 16px;
}

.capacity-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;
    margin-top: 16px;
}
</style>