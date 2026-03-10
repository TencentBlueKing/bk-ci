<template>
    <div class="real-time-monitor">
        <div class="time-select">
            <DatePicker
                :model-value="modelValue"
                :timezone.sync="timezone"
                @update:modelValue="handleValueChange"
            />
            <div
                class="time-select-right"
                @click="handleClickJump('goMonitorBoard')"
            >
                <Logo
                    style="margin-right: 6px;font-size: 12px;margin-top: 2px;"
                    size="12"
                    name="go-monitor-board"
                /><span style="font-size: 14px;">{{ $t('realTimeMonitor.gotoMonitorDashboard') }}</span>
            </div>
        </div>

        <!-- 当前流水线状态 & 执行统计 -->
        <div class="dual-section">
            <CurrentPipelineStatus
                :time-range="timeRange"
                @item-click="handleClickJump"
            />
            <ExecutionStatistics
                :time-range="timeRange"
                @item-click="handleClickJump"
            />
        </div>

        <!-- 当前构建资源 & 当前资源使用 -->
        <div class="dual-section">
            <BuildResources
                @item-click="handleClickJump"
                :time-range="timeRange"
            />
            <ResourceUsage
                :time-range="timeRange"
                @item-click="handleClickJump"
            />
        </div>

        <!-- 制品库 -->
        <ArtifactRepository
            :time-range="timeRange"
            @item-click="handleClickJump"
        />
        
        <!-- 告警模块（已注释，保留结构） -->
        <AlertsSection :time-range="timeRange" />
    </div>
</template>

<script>
    import { defineComponent, ref } from 'vue'
    import Logo from '@/components/Logo'
    import useInstance from '@/hook/useInstance'
    import CurrentPipelineStatus from './components/CurrentPipelineStatus.vue'
    import ExecutionStatistics from './components/ExecutionStatistics.vue'
    import BuildResources from './components/BuildResources.vue'
    import ResourceUsage from './components/ResourceUsage.vue'
    import ArtifactRepository from './components/ArtifactRepository.vue'
    import { generateJumpUrl, urlMap,buildNodesUrl } from './components/constant'
    import { getTimeRange24h,convertTimeToTimestamp } from './components/util'
    import DatePicker from '@blueking/date-picker/vue2'
    import '@blueking/date-picker/vue2/vue2.css'
    import AlertsSection from './components/AlertsSection.vue'
    export default defineComponent({
        name: 'PipelineRealTimeMonitor',
        components: {
            CurrentPipelineStatus,
            ExecutionStatistics,
            BuildResources,
            ResourceUsage,
            ArtifactRepository,
            DatePicker,
            AlertsSection,
            Logo
        },
        setup () {
            const { proxy } = useInstance()
            const modelValue = ref(['now-24h', 'now',])
            const timezone = ref('Asia/Shanghai')
        
            // 初始化时间范围
            const timeRange = ref(getTimeRange24h())

            /**
             * 处理时间变化
             */
            const  handleValueChange = (v, info) =>{
                modelValue.value = v
                const timeArr = [info[0].formatText,info[1].formatText]
                const timestamps = convertTimeToTimestamp(timeArr)
                // 使用新的时间范围获取数据
                timeRange.value = timestamps
            }

            /**
             * 处理下钻点击跳转到监控页面
             */
            const handleClickJump = (val) => {
                const [originalStartTime, endTime] = timeRange.value
                let startTime = originalStartTime
                console.log('click',val)
                if(val==='availableNodes'||val==='offlineNodes'){
                    const statusMap = {
                        availableNodes: 'NORMAL',
                        offlineNodes: 'ABNORMAL'
                    }
                    const currentUrl = buildNodesUrl(proxy.$route.params.projectId,statusMap[val])
                    console.log(currentUrl,'currentUrlcurrentUrl')
                    window.open(currentUrl, '_blank')
                    return
                }
                
                // 针对当前流水线状态的项目，进行时间范围限制处理
                const currentPipelineStatusIds = ['runningPipelines', 'waitingPipelines', 'waitingJob', 'auditPipelines']
                if (currentPipelineStatusIds.includes(val)) {
                    // 检查时间差是否大于两天（2天 = 2 * 24 * 60 * 60 秒）
                    const twoDaysInSeconds = 2 * 24 * 60 * 60
                    const timeDiff = endTime - startTime
                    
                    if (timeDiff > twoDaysInSeconds) {
                        // 如果时间差大于两天，将startTime调整为endTime往前两天
                        startTime = endTime - twoDaysInSeconds
                    }
                }
                
                const projectId = proxy.$route.params.projectId
            
                // 从urlMap对象中获取对应的URL生成函数
                const func = urlMap[val]
                if (!func) {
                    console.error(`未找到 ${val} 对应的URL生成函数`)
                    return
                }
            
                // 将秒级时间戳(10位)转换为毫秒级时间戳(13位)
                const startTimeMs = startTime * 1000
                const endTimeMs = endTime * 1000
                console.log('转换后时间范围(毫秒):', projectId, { startTimeMs, endTimeMs })
            
                const url = generateJumpUrl(func(projectId, startTimeMs, endTimeMs))
                console.log('跳转URL:', url)
                window.open(url, '_blank')
            }
            return {
                modelValue,
                timezone,
                timeRange,
                handleClickJump,
                handleValueChange
            }
        }
    })
</script>

<style lang="scss" scoped>
.real-time-monitor {
    padding: 16px 24px;
    background: #f5f7fa;
    min-height: 100vh;
    .time-select {

        display: flex;

        align-items: center;

        justify-content: space-between;
    }
}

.dual-section {
    display: grid;
    grid-template-columns: 870px 1fr;
    gap: 16px;
}
.time-select-right {
    display: flex;
    align-items: center;
    color: #3a84ff;
    cursor: pointer;
}
</style>
