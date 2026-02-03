<template>
    <div
        class="alert-box"
        v-bkloading="{ isLoading: loading }"
    >
        <!-- 告警 -->
        <div class="monitor-section">
            <div class="section-title">{{ $t('realTimeMonitor.alerts') }}</div>
            <div
                class="product-grid"
            >
                <warn-card
                    v-for="item in warnTotal"
                    :key="item.label"
                    :label="$t(`realTimeMonitor.${item.label}`)"
                    :value="item.value"
                    :color="item.color"
                />
            </div>
            <div class="warn-dividing-line"></div>
        </div>
        <!-- 告警分布环形图 -->
        <div class="warn-chart-section">
            <div class="left-section">
                <div class="title">{{ $t('realTimeMonitor.distributionByService') }}</div>
                <donut-chart
                    ref="donutChartRef"
                    :data="warnChartData"
                    :center-text="warnTotalCount"
                    center-sub-text="总告警数"
                    @update:selectedItem="handleSelectedItemChange"
                />
            </div>
            <div class="right-section">
                <div class="title">{{ $t('realTimeMonitor.distributionByOwner') }}</div>
                <div
                    v-if="selectedWarnItem"
                    class="selected-tag"
                >
                    <span class="tag-label">{{ $t('realTimeMonitor.service') }}：{{ selectedWarnItem.name }}</span>
                    <i
                        class="bk-icon icon-close"
                        @click="handleClearSelection"
                    ></i>
                </div>
                <StackedBarChart
                    :data="personDistributionData"
                    :loading="barChartLoading"
                    height="280px"
                />
            </div>
        </div>
    </div>
</template>

<script>
    import { defineComponent, ref, onMounted, watch, computed } from 'vue'
    import useInstance from '@/hook/useInstance'
    import DonutChart from './DonutChart.vue'
    import warnCard from './warnCard.vue'
    import StackedBarChart from './StackedBarChart.vue'
    export default defineComponent({
        name: 'ArtifactRepository',
        components: {
            warnCard,
            DonutChart,
            StackedBarChart
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
            const loading = ref(false)  // 整体页面 loading
            const barChartLoading = ref(false)  // 柱状图 loading
            const warnTotal = ref([
                { label: 'totalAlerts', value: 0,id: 'totalAlerts'},
                { label: 'unrecoveredCount', value: 0,color: '#F09191',id: 'ABNORMAL'},
                { label: 'recoveredCount', value: 0,color: '#87D2A5',id: 'RECOVERED'},
                { label: 'expiredCount', value: 0,color: '#DCDEE5',id: 'CLOSED'},
            ])

            // 告警环形图数据
            const warnChartData = ref([
                {
                    name: '第三方构建机',
                    id:'BKCI_AGENT',
                    value: 0,
                    itemStyle: { color: '#5B8FF9' }
                },
                {
                    name: '流水线',
                    id:'BKCI_PIPELINE',
                    value: 0,
                    itemStyle: { color: '#FAAD14' }
                },
                {
                    name: '制品库',
                    id:'BKCI_REPO',
                    value: 0,
                    itemStyle: { color: '#73D13D' }
                },
            ])
            
            const appointeeList = ref([])
            
            // 按负责人分布数据结构定义（复用）
            const createEmptyPersonDistributionData = () => ({
                xAxisData: [],
                series: [
                    { name: '未恢复数', id: 'ABNORMAL', data: [], color: '#F09191' },
                    { name: '已恢复数', id: 'RECOVERED', data: [], color: '#87D2A5' },
                    { name: '已失效数', id:'CLOSED', data: [], color: '#DCDEE5' }
                ]
            })
            
            // 按负责人分布数据
            const personDistributionData = ref(createEmptyPersonDistributionData())

            // 计算总告警数
            const warnTotalCount = computed(() => {
                return warnChartData.value.reduce((sum, item) => sum + item.value, 0)
            })

            // 选中的告警项
            const selectedWarnItem = ref(null)
            const donutChartRef = ref(null)

            // 处理选中项变化
            const handleSelectedItemChange = (item) => {
                selectedWarnItem.value = item
                // 选中项变化时重新获取柱状图数据
                if (props.timeRange && props.timeRange.length >= 2) {
                    const [startTime, endTime] = props.timeRange
                    getWarnBarData(startTime, endTime)
                }
            }

            // 清除选中
            const handleClearSelection = () => {
                if (donutChartRef.value) {
                    donutChartRef.value.clearSelection()
                }
                // 注意：不需要手动调用 getWarnBarData，因为 clearSelection() 会触发 @update:selectedItem 事件
                // 进而调用 handleSelectedItemChange(null)，自动更新数据
            }

            const handleClick = (id) => {
                emit('item-click', id)
            }

            /**
             * 构建告警查询条件
             * @param {Array} labelsValue - labels 字段的值
             * @param {string} appointeeName - 负责人名称（可选）
             * @returns {Array} 条件数组
             */
            const buildConditions = (labelsValue, appointeeName = null) => {
                const conditions = [
                    {
                        key: "labels",
                        value: labelsValue,
                        method: "eq",
                        condition: "and"
                    }
                ]
                
                if (appointeeName) {
                    conditions.push({
                        key: "appointee",
                        value: [appointeeName],
                        method: "eq",
                        condition: "and"
                    })
                }
                
                return conditions
            }
            
            /**
             * 获取按负责人分布的告警数据（柱状图）
             * @param {string} startTime - 开始时间
             * @param {string} endTime - 结束时间
             */
            const getWarnBarData = async (startTime, endTime) => {
                // 如果没有负责人数据，直接返回空数据
                if (appointeeList.value.length === 0) {
                    personDistributionData.value = createEmptyPersonDistributionData()
                    barChartLoading.value = false
                    return
                }
                
                barChartLoading.value = true
                try {
                    // 创建临时数据对象
                    const tempData = createEmptyPersonDistributionData()
                    
                    // 设置 x 轴数据（负责人名称）
                    tempData.xAxisData = appointeeList.value.map(item => item.name)
                    
                    // 根据是否有选中项，动态设置 labels 的值
                    const labelsValue = selectedWarnItem.value
                        ? [selectedWarnItem.value.id]  // 选中某个服务：BKCI_AGENT/BKCI_PIPELINE/BKCI_REPO
                        : ["BKCI"]  // 未选中：显示全部 BKCI 服务
                    
                    // 并行请求每个负责人的告警数据
                    const promises = appointeeList.value.map(async (appointee, index) => {
                        const conditions = buildConditions(labelsValue, appointee.name)
                        
                        const resultsChart = await proxy.$store.dispatch('pipelines/getWarnInfo', {
                            conditions,
                            start_time: startTime,
                            end_time: endTime,
                            fields: ['labels', 'status', 'appointee'],
                            size: 100,
                            bk_biz_ids: [-4219865],
                        })
                        
                        // 提取 status 数据
                        const statusBuckets = resultsChart.data?.fields
                            ?.find(item => item.field === 'status')?.buckets || []
                        
                        // 为每个 series 填充数据
                        tempData.series.forEach(seriesItem => {
                            const statusData = statusBuckets.find(bucket => bucket.id === seriesItem.id)
                            seriesItem.data[index] = statusData?.count || 0
                        })
                    })
                    
                    // 等待所有请求完成
                    await Promise.all(promises)
                    
                    // 一次性更新整个对象，触发响应式更新
                    personDistributionData.value = tempData
                    
                    console.log('personDistributionData更新完成:', {
                        selected: selectedWarnItem.value?.name || '全部',
                        labels: labelsValue,
                        data: personDistributionData.value
                    })
                } catch (error) {
                    console.error('获取按负责人分布的告警数据失败:', error)
                    // 出错时设置空数据
                    personDistributionData.value = createEmptyPersonDistributionData()
                } finally {
                    barChartLoading.value = false
                }
            }
            
            /**
             * 获取告警总览数据（顶部卡片 + 环形图）
             */
            const fetchData = async () => {
                if (!props.timeRange || props.timeRange.length < 2) return
                
                loading.value = true
                try {
                    const [startTime, endTime] = props.timeRange
                    const conditions = buildConditions(["BKCI"])
                    
                    // 请求告警数据
                    const results = await proxy.$store.dispatch('pipelines/getWarnInfo', {
                        conditions,
                        start_time: startTime,
                        end_time: endTime,
                        fields: ['labels', 'status', 'appointee'],
                        size: 100,
                        bk_biz_ids: [-4219865],
                    })
                    
                    const resultsList = results.data?.fields || []
                    
                    // 重置所有数据
                    warnTotal.value.forEach(item => {
                        item.value = 0
                    })
                    appointeeList.value = []
                    
                    // 重置环形图数据（创建新数组引用以确保触发 watch）
                    warnChartData.value = warnChartData.value.map(item => ({
                        ...item,
                        value: 0
                    }))
                    
                    if (resultsList.length === 0) {
                        console.warn('未获取到告警数据，已重置为空数据')
                        // warnChartData 已更新，会自动触发 DonutChart watch → getWarnBarData
                        return
                    }
                    
                    // 提取各个维度的数据
                    const statusBuckets = resultsList.find(item => item.field === 'status')?.buckets || []
                    const labelsBuckets = resultsList.find(item => item.field === 'labels')?.buckets || []
                    const appointeeBuckets = resultsList.find(item => item.field === 'appointee')?.buckets || []
                    
                    // 更新负责人列表
                    appointeeList.value = appointeeBuckets
                    
                    // 更新顶部卡片数据（总告警数、未恢复数、已恢复数、已失效数）
                    let totalCount = 0
                    statusBuckets.forEach(bucket => {
                        totalCount += bucket.count
                        const foundItem = warnTotal.value.find(item => item.id === bucket.id)
                        if (foundItem) {
                            foundItem.value = bucket.count
                        }
                    })
                    warnTotal.value[0].value = totalCount  // 总告警数
                    
                    // 更新环形图数据（按服务分布）
                    warnChartData.value = warnChartData.value.map(item => {
                        const bucket = labelsBuckets.find(b => b.name === item.id)
                        return { ...item, value: bucket?.count || 0 }
                    })
                    
                    // 注意：不需要手动调用 getWarnBarData
                    // 因为更新 warnChartData 会触发 DonutChart 的 watch
                    // 进而 emit update:selectedItem(null)，自动调用 handleSelectedItemChange(null) → getWarnBarData
                    
                } catch (error) {
                    console.error('获取告警数据失败:', error)
                } finally {
                    loading.value = false
                }
            }
            // 监听时间范围变化
            watch(() => props.timeRange, fetchData, { deep: true })

            onMounted(() => {
                fetchData()
            })

            return {
                handleClick,
                fetchData,
                warnTotal,
                warnTotalCount,
                warnChartData,
                selectedWarnItem,
                donutChartRef,
                handleSelectedItemChange,
                handleClearSelection,
                StackedBarChart,
                personDistributionData,
                barChartLoading,
                loading
            }
        }
    })
</script>

<style lang="scss" scoped>
.alert-box{
    background-color: white;
    padding: 16px 24px 24px 24px;
}
.monitor-section {
    border-radius: 2px;
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
.product-grid {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 16px;
    margin-top: 16px;
}
.warn-dividing-line{
    width: 100%;
    height: 1px;
    background: #DCDEE5;
    margin: 24px 0;
}
.warn-chart-section {
    margin-top: 16px;
    display: flex;
    gap: 24px;
    
    .title {
        font-weight: 700;
        font-size: 12px;
        color: #313238;
        margin-bottom: 16px;
    }
    
    .left-section {
        display: flex;
        flex: 0 0 480px;
        flex-direction: column;
    }
    
    .right-section {
        position: relative;
        flex: 1 1 auto;
        display: flex;
        flex-direction: column;
    }
}
.selected-tag {
    position: absolute;
    top: -3px;
    left: 85px;
    display: inline-flex;
    align-items: center;
    height: 22px;
    line-height: 22px;
    padding: 0px 2px 0px 6px;
    background: #FAFBFD;
    border: 1px solid #DCDEE5;
    border-radius: 2px;
    font-size: 12px;
    color: #4D4F56;
    margin-bottom: 16px;
        
    .icon-close {
        font-size: 14px;
        cursor: pointer;
        color: #979BA5;
        transition: color 0.3s ease;
        
        &:hover {
            color: #3A84FF;
        }
    }
}
</style>