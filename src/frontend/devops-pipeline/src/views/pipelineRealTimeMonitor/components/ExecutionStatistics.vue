<template>
    <div class="monitor-section half-section">
        <div class="section-title">{{ $t('realTimeMonitor.executionStatistics') }}</div>
        <div
            v-bkloading="{ isLoading: loading }"
            class="capacity-grid"
        >
            <capacity-card
                v-for="item in executionStatisticsList"
                :key="item.label"
                :label="$t(`realTimeMonitor.${item.label}`)"
                :value="item.value"
                :unit="item.unit"
                :show-icon-logo="item.showIconLogo"
                :icon-logo-name="item.iconLogoName"
                @click="handleClick(item.id)"
            />
        </div>
    </div>
</template>

<script>
    import { defineComponent, ref, onMounted, watch } from 'vue'
    import CapacityCard from './CapacityCard.vue'
    import { failuresNum, cancelNum, successNum, successRate } from './constant'
    import useInstance from '@/hook/useInstance'
    export default defineComponent({
        name: 'ExecutionStatistics',
        components: {
            CapacityCard
        },
        props: {
            timeRange: {
                type: Array,
                required: true
            }
        },
        emits: ['item-click'],
        setup (props, { emit }) {
            const { proxy } = useInstance()
            const loading = ref(false)
            const executionStatisticsList = ref([
                { id: 'failuresNum', label: 'failureCount', value: null, unit: '次', showIconLogo: true, iconLogoName: 'icon-close-circle' },
                { id: 'cancelNum', label: 'cancelCount', value: null, unit: '次', showIconLogo: true, iconLogoName: 'icon-close-circle' },
                { id: 'successNum', label: 'successCount', value: null, unit: '次', showIconLogo: true, iconLogoName: 'icon-close-circle' },
                { id: 'successRate', label: 'successRate', value: null, unit: '%' }
            ])

            /**
             * 提取数据的辅助函数
             */
            const extractValue = (result, id = null) => {
                if (!result?.data?.series || result.data.series.length === 0) {
                    return '--'
                }
                const datapoint = result.data.series[0]?.datapoints?.[0][0]
                if (datapoint === undefined || datapoint === null) {
                    return '--'
                }
            
                let numValue = Number(datapoint)
                
                // 如果是 successRate，需要乘以100转换为百分比
                if (id === 'successRate') {
                    numValue = numValue * 100
                }
                
                if (!Number.isInteger(numValue) && !isNaN(numValue)) {
                    return numValue.toFixed(2)
                }
                return numValue
            }

            /**
             * 获取执行统计数据
             */
            const fetchData = async () => {
                if (!props.timeRange || props.timeRange.length < 2) return
            
                loading.value = true
                try {
                    const [startTime, endTime] = props.timeRange
                
                    const promqlConfigs = [
                        { id: 'failuresNum', promql: failuresNum },
                        { id: 'cancelNum', promql: cancelNum },
                        { id: 'successNum', promql: successNum },
                        { id: 'successRate', promql: successRate }
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

                    executionStatisticsList.value = executionStatisticsList.value.map(item => {
                        const configIndex = promqlConfigs.findIndex(config => config.id === item.id)
                        if (configIndex !== -1) {
                            return { ...item, value: extractValue(results[configIndex], item.id) }
                        }
                        return item
                    })
                } catch (error) {
                    console.error('获取执行统计数据失败:', error)
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
                executionStatisticsList,
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