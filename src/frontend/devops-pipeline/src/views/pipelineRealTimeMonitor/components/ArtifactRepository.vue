<template>
    <div class="monitor-section">
        <div class="section-title">{{ $t('realTimeMonitor.artifactRepository') }}</div>
        <div
            v-bkloading="{ isLoading: loading }"
            class="product-grid"
        >
            <capacity-card
                v-for="item in capacityMetrics"
                :key="item.label"
                :label="$t(`realTimeMonitor.${item.label}`)"
                :value="item.value"
                :unit="item.unit"
                :show-icon="item.showIcon"
                :icon-name="item.iconName"
                :icon-size="item.iconSize"
                :show-logo="item.showLogo"
                :logo-name="item.logoName"
                @click="handleClick(item.id)"
            />
        </div>
    </div>
</template>

<script>
    import { defineComponent, ref, onMounted, watch,  } from 'vue'
    import useInstance from '@/hook/useInstance'
    import CapacityCard from './CapacityCard.vue'
    import {
        totalArtifacts,
        addedArtifacts,
        deletedArtifacts,
        avgUploadSpeed,
        avgDownloadSpeed,
        uploadArtifacts,
        downloadArtifacts
    } from './constant'

    export default defineComponent({
        name: 'ArtifactRepository',
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
            const capacityMetrics = ref([
                { id: 'totalArtifacts', label: 'totalArtifacts', value: null, unit: 'B', showIcon: false, showLogo: true, logoName: 'metrics-zhipin-total' },
                { id: 'addedArtifacts', label: 'addedArtifacts', value: null, unit: 'B', showIcon: true, iconName: 'icon-plus-circle' },
                { id: 'deletedArtifacts', label: 'deletedArtifacts', value: null, unit: 'B', showIcon: true, iconName: 'icon-minus-circle' },
                { id: 'avgUploadSpeed', label: 'avgUploadSpeed', value: null, unit: 'B', showIcon: true, iconName: 'icon-upload', iconSize: '12px' },
                { id: 'avgDownloadSpeed', label: 'avgDownloadSpeed', value: null, unit: 'B', showIcon: true, iconName: 'icon-download', iconSize: '12px' },
                { id: 'uploadArtifacts', label: 'uploadArtifacts', value: null, unit: 'B', showIcon: true, iconName: 'icon-upload', iconSize: '12px' },
                { id: 'downloadArtifacts', label: 'downloadArtifacts', value: null, unit: 'B', showIcon: true, iconName: 'icon-download', iconSize: '12px' }
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
             * 智能字节单位转换函数 - 自动选择最合适的单位，保留两位小数
             */
            const extractValueWithAutoUnit = (result) => {
                const value = extractValue(result)
                if (value === '--' || isNaN(value)) {
                    return { value: '--', unit: 'B' }
                }
            
                const bytes = Number(value)
                const units = ['B', 'KB', 'MB', 'GB', 'TB']
            
                // 从最大单位开始检查，找到合适的单位
                for (let i = units.length - 1; i >= 0; i--) {
                    const divisor = Math.pow(1000, i) // 使用1000进制
                    const convertedValue = bytes / divisor
                
                    // 如果转换后的值大于等于1，选择该单位
                    if (convertedValue >= 1) {
                        return {
                            value: parseFloat(convertedValue.toFixed(2)), // 保留两位小数
                            unit: units[i]
                        }
                    }
                }
            
                // 如果都小于1，返回原始字节数（保留两位小数）
                return {
                    value: parseFloat(bytes.toFixed(2)),
                    unit: 'B'
                }
            }

            /**
             * 获取制品库数据
             */
            const fetchData = async () => {
                if (!props.timeRange || props.timeRange.length < 2) return
            
                loading.value = true
                try {
                    const [startTime, endTime] = props.timeRange
                    const timeRangeDiff = endTime - startTime
                
                    const promqlConfigs = [
                        { id: 'totalArtifacts', promql: totalArtifacts, isStatic: true },
                        { id: 'addedArtifacts', promql: addedArtifacts },
                        { id: 'deletedArtifacts', promql: deletedArtifacts },
                        { id: 'avgUploadSpeed', promql: avgUploadSpeed },
                        { id: 'avgDownloadSpeed', promql: avgDownloadSpeed },
                        { id: 'uploadArtifacts', promql: uploadArtifacts },
                        { id: 'downloadArtifacts', promql: downloadArtifacts }
                    ]

                    const results = await Promise.all(
                        promqlConfigs.map(config =>
                            proxy.$store.dispatch('pipelines/getMetrics', {
                                promql: config.isStatic
                                    ? config.promql
                                    : (typeof config.promql === 'function' ? config.promql(timeRangeDiff) : config.promql),
                                start_time: startTime,
                                end_time: endTime
                            })
                        )
                    )

                    capacityMetrics.value = capacityMetrics.value.map(item => {
                        const configIndex = promqlConfigs.findIndex(config => config.id === item.id)
                        if (configIndex !== -1) {
                            // 所有制品库相关数据都使用智能单位转换
                            const extractedData = extractValueWithAutoUnit(results[configIndex])
                            return {
                                ...item,
                                value: extractedData.value,
                                unit: extractedData.unit
                            }
                        }
                        return item
                    })
                } catch (error) {
                    console.error('获取制品库数据失败:', error)
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
                capacityMetrics,
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
}

.section-title {
    font-size: 14px;
    color: #313238;
    margin-bottom: 16px;
}

.product-grid {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 16px;
    margin-top: 16px;
}
</style>