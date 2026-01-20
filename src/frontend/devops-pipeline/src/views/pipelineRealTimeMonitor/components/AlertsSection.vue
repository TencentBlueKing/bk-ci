<template>
    <div class="alert-box">
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
            const warnTotal = ref([
                { label: 'totalAlerts', value: 350},
                { label: 'unrecoveredCount', value: 350,color: '#F09191'},
                { label: 'recoveredCount', value: 350,color: '#87D2A5'},
                { label: 'expiredCount', value: 350,color: '#DCDEE5'},
            ])

            // 告警环形图数据
            const warnChartData = computed(() => [
                {
                    name: '流水线',
                    value: 120,
                    itemStyle: { color: '#5B8FF9' }
                },
                {
                    name: '任务',
                    value: 80,
                    itemStyle: { color: '#FAAD14' }
                },
                {
                    name: '节点',
                    value: 100,
                    itemStyle: { color: '#73D13D' }
                },
                {
                    name: '代码库',
                    value: 50,
                    itemStyle: { color: '#9E87FF' }
                }
            ])
            
            // 按负责人分布数据
            const personDistributionData = computed(() => ({
                xAxisData: ['faye', 'carl', 'ming'],
                series: [
                    {
                        name: '未恢复数',
                        data: [10, 8, 5],
                        color: '#F09191'
                    },
                    {
                        name: '已恢复数',
                        data: [20, 30, 50],
                        color: '#87D2A5'
                    },
                    {
                        name: '已失效数',
                        data: [20, 25, 40],
                        color: '#DCDEE5'
                    }
                ]
            }))

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
            }

            // 清除选中
            const handleClearSelection = () => {
                selectedWarnItem.value = null
                if (donutChartRef.value) {
                    donutChartRef.value.clearSelection()
                }
            }

            const handleClick = (id) => {
                emit('item-click', id)
            }

            const fetchData = ()=>{

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
                personDistributionData
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