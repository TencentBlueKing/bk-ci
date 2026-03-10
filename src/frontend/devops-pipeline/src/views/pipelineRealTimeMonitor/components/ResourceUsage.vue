<template>
    <div class="monitor-section half-section">
        <div class="section-title">{{ $t('realTimeMonitor.currentResourceUsage') }}</div>
        <metrics-grid
            v-bkloading="{ isLoading: loading }"
            :columns="3"
        >
            <capacity-card
                v-for="item in sourceStatus"
                :key="item.label"
                :label="$t(`realTimeMonitor.${item.label}`)"
                :value="item.value"
                :show-logo="item.showLogo"
                :logo-name="item.logoName"
                @click="handleClick(item.id)"
            />
        </metrics-grid>
    </div>
</template>

<script>
    import { defineComponent, ref, onMounted, watch } from 'vue'
    import MetricsGrid from './MetricsGrid.vue'
    import CapacityCard from './CapacityCard.vue'
    import { sourceCpu, sourceMemory, sourceDisk, extractValue } from './constant'
    import useInstance from '@/hook/useInstance'
    export default defineComponent({
        name: 'ResourceUsage',
        components: {
            MetricsGrid,
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
            const sourceStatus = ref([
                { id: 'sourceCpu', label: 'cpuUsageOver80', value: null, showLogo: true, logoName: 'source-cpu-use' },
                { id: 'sourceMemory', label: 'memoryUsageOver80', value: null, showLogo: true, logoName: 'source-memory-use' },
                { id: 'sourceDisk', label: 'diskUsageOver80', value: null, showLogo: true, logoName: 'source-disk-use' }
            ])


            /**
             * 获取当前资源使用数据
             */
            const fetchData = async () => {
                if (!props.timeRange || props.timeRange.length < 2) return
            
                loading.value = true
                try {
                    const [startTime, endTime] = props.timeRange
                
                    const promqlConfigs = [
                        { id: 'sourceCpu', promql: sourceCpu },
                        { id: 'sourceMemory', promql: sourceMemory },
                        { id: 'sourceDisk', promql: sourceDisk }
                    ]

                    const results = await Promise.all(
                        promqlConfigs.map(config =>
                            proxy.$store.dispatch('pipelines/getMetrics', {
                                promql: config.promql, // 这些是静态promql
                                start_time: startTime,
                                end_time: endTime
                            })
                        )
                    )

                    sourceStatus.value = sourceStatus.value.map(item => {
                        const configIndex = promqlConfigs.findIndex(config => config.id === item.id)
                        if (configIndex !== -1) {
                            return { ...item, value: extractValue(results[configIndex]) }
                        }
                        return item
                    })
                } catch (error) {
                    console.error('获取当前资源使用数据失败:', error)
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
                sourceStatus,
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
</style>