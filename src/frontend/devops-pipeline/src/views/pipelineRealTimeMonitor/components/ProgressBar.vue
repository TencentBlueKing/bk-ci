<template>
    <div class="progress-bar-wrapper">
        <!-- 图例 -->
        <div class="legend-list">
            <div
                v-for="(item, index) in legendData"
                :key="index"
                class="legend-item"
            >
                <span
                    class="legend-icon"
                    :class="item.type"
                ></span>
                <span class="legend-value">{{ item.value }}</span>
                <span class="legend-label">{{ $t(`realTimeMonitor.${item.label}`) }}</span>
            </div>
        </div>
        
        <!-- 第一行进度条: 可用节点 = 空闲节点 + 满并发运行节点 + 并发低于 50% 节点 + 其他并发节点 -->
        <div class="progress-bar second-row">
            <div
                v-for="(item, index) in firstRowData"
                :key="index"
                class="progress-segment"
                :class="item.type"
                :style="{ width: item.percentage + '%' }"
                :title="$t(`realTimeMonitor.${item.label}`) + ': ' + item.value"
            ></div>
        </div>

        <!-- 第二行进度条: 全部节点 = 可用节点 + 离线节点 -->
        <div class="progress-bar">
            <div
                v-for="(item, index) in secondRowData"
                :key="index"
                class="progress-segment"
                :class="item.type"
                :style="{ width: item.percentage + '%' }"
                :title="$t(`realTimeMonitor.${item.label}`) + ': ' + item.value"
            ></div>
        </div>
    </div>
</template>

<script>
    import { defineComponent, computed } from 'vue'
    export default defineComponent({
        name: 'ProgressBar',
        props: {
            data: {
                type: Array,
                default: () => []
            }
        },
        setup (props) {
            // 图例数据（不包含其它发节点）
            const legendData = computed(() => {
                return props.data.filter(item => item.type !== 'other-load')
            })

            // 第2行数据：可用节点 + 离线节点
            const secondRowData = computed(() => {
                const canUse = props.data.find(item => item.type === 'can-use')
                const offline = props.data.find(item => item.type === 'offline')
                
                const data = [canUse, offline].filter(Boolean)
                const total = data.reduce((sum, item) => sum + item.value, 0)
                
                return data.map(item => ({
                    ...item,
                    percentage: total > 0 ? (item.value / total * 100) : 0
                }))
            })

            // 计算可用节点在第一行中的占比
            const canUsePercentage = computed(() => {
                const canUse = props.data.find(item => item.type === 'can-use')
                const offline = props.data.find(item => item.type === 'offline')
                const total = (canUse?.value || 0) + (offline?.value || 0)
                return total > 0 ? ((canUse?.value || 0) / total * 100) : 0
            })

            // 第1行数据：空闲节点 + 满并发运行节点 + 并发低于 50% 节点 + 其他并发节点
            const firstRowData = computed(() => {
                const freeLoad = props.data.find(item => item.type === 'free-load')
                const fullLoad = props.data.find(item => item.type === 'full-load')
                const lowLoad = props.data.find(item => item.type === 'low-load')
                const otherLoad = props.data.find(item => item.type === 'other-load')
                
                const data = [freeLoad, lowLoad, fullLoad ,otherLoad,].filter(Boolean)
                const total = data.reduce((sum, item) => sum + item.value, 0)
                
                return data.map(item => ({
                    ...item,
                    percentage: total > 0 ? (item.value / total * 100) : 0
                }))
            })

            return {
                legendData,
                firstRowData,
                secondRowData,
                canUsePercentage
            }
        }
    })
</script>

<style lang="scss" scoped>
.progress-bar-wrapper {
    margin-top: 2px;
}

.progress-bar {
    display: flex;
    height: 20px;
    overflow: hidden;
    
    &.second-row {
        height: 14px !important;
        width: v-bind('canUsePercentage + "%"');
    }
}

.progress-segment {
    transition: all 0.3s ease;

    // 可用节点 - 绿色
    &.can-use {
        background: #87D2A5;
    }
    
    // 并发低于50%节点 - 橙色
    &.low-load {
        background: #FAAF37;
    }
    
    // 满并发运行节点 - 红色
    &.full-load {
        background: #DC5F69;
    }
    
    // 空闲节点 - 蓝色
    &.free-load {
        background: #87AFFF;
    }
    
    // 离线节点 - 灰色
    &.offline {
        background: #DCDEE5;
    }
    // 其它发节点 - 浅灰色
    &.other-load {
        background: #F0F1F5;
    }
}

.legend-list {
    display: flex;
    align-items: center;
    gap: 24px;
    flex-wrap: wrap;
    margin-bottom: 10px;
}

.legend-item {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: #63656e;
    padding: 6px 8px;
    border-radius: 2px;
    cursor: pointer;
    transition: all 0.3s ease;
    
    &:hover {
        box-shadow: 0 2px 6px 0 rgba(58, 132, 255, 0.15);
        transform: translateY(-2px);
    }
    
    .legend-icon {
        width: 16px;
        height: 16px;
        border-radius: 2px;
        flex-shrink: 0;
    }
    
    .legend-value {
        font-size: 20px;
        color: #313238;
    }
    
    .legend-label {
        font-size: 14px;
        color: #979BA5;
    }
    
    // 图例颜色
    .legend-icon.can-use {
        background: #87D2A5;
    }
    
    .legend-icon.low-load {
        background: #FAAF37;
    }
    
    .legend-icon.full-load {
        background: #DC5F69;
    }
    
    .legend-icon.free-load {
        background: #87AFFF;
    }
    
    .legend-icon.offline {
        background: #DCDEE5;
    }
}
</style>
