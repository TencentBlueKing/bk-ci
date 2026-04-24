<template>
    <div class="monitor-section half-section">
        <div class="section-title">{{ $t('realTimeMonitor.currentBuildResources') }}</div>
        <div v-bkloading="{ isLoading: loading }">
            <progress-bar
                :data="nodeProgressData"
                @item-click="handleClickJump"
            />
        </div>
    </div>
</template>

<script>
    import { defineComponent, ref, onMounted, watch } from 'vue'
    import ProgressBar from './ProgressBar.vue'
    import useInstance from '@/hook/useInstance'
    import { freeLoadNum, lowLoadNum, fullLoadNum, extractValue } from './constant'
    export default defineComponent({
        name: 'BuildResources',
        components: {
            ProgressBar
        },
        props: {
            timeRange: {
                type: Array,
                required: true
            }
        },
        emits: ['item-click'],
        setup (props, { emit }) {
            const loading = ref(false)
            const { proxy } = useInstance()
            // 节点进度条数据
            // 全部节点 = 可用节点 + 离线节点
            // 可用节点 = 空闲节点 + 满并发运行节点 + 并发低于 50% 节点 + 其他并发节点
            const nodeProgressData = ref([
                {
                    value: null,
                    label: 'availableNodes',
                    type: 'can-use'
                },
                {
                    value: null,
                    label: 'offlineNodes',
                    type: 'offline'
                },
                {
                    value: null,
                    label: 'idleNodes',
                    type: 'free-load'
                },
                {
                    value:null,
                    label: 'lowConcurrencyNodes',
                    type: 'low-load'
                },
                {
                    value: null,
                    label: 'fullConcurrencyNodes',
                    type: 'full-load'
                },
                // {
                //     value: 1,
                //     label: 'otherNodes',
                //     type: 'other-load'
                // }
            ])

            // 节点状态常量
            const NODE_STATUS = {
                NORMAL: 'NORMAL',
                ABNORMAL: 'ABNORMAL'
            }
            
            const NODE_TYPE = {
                THIRDPARTY: 'THIRDPARTY'
            }
            
            // 节点数据索引常量
            const NODE_INDEX = {
                AVAILABLE: 0,
                OFFLINE: 1
            }

            /**
             * 构建节点查询参数
             * @param {string} projectId - 项目ID
             * @param {string} nodeStatus - 节点状态
             * @param {string} nodeType - 节点类型
             * @returns {string} 查询参数字符串
             */
            const buildNodeParams = (projectId, nodeStatus, nodeType = NODE_TYPE.THIRDPARTY) => {
                return `${projectId}/fetchNodes?nodeStatus=${nodeStatus}&nodeType=${nodeType}`
            }

            /**
             * 更新节点数据
             * @param {number} index - 节点数据索引
             * @param {number} count - 节点数量
             */
            const updateNodeData = (index, count) => {
                if (nodeProgressData.value[index]) {
                    nodeProgressData.value[index].value = count || 0
                }
            }

            /**
             * 获取构建资源数据
             * 并行请求可用节点和离线节点数据以提高性能
             */
            const fetchData = async () => {
                loading.value = true
                
                const projectId = proxy.$route.params.projectId
                const availableNodesParams = buildNodeParams(projectId, NODE_STATUS.NORMAL)
                const offlineNodesParams = buildNodeParams(projectId, NODE_STATUS.ABNORMAL)
                
                try {
                    // 并行请求两个接口以提高性能
                    const [availableNodesRes, offlineNodesRes] = await Promise.all([
                        proxy.$store.dispatch('pipelines/getBuildResource', availableNodesParams),
                        proxy.$store.dispatch('pipelines/getBuildResource', offlineNodesParams)
                    ])
                    
                    // 更新节点数据
                    updateNodeData(NODE_INDEX.AVAILABLE, availableNodesRes?.count)
                    updateNodeData(NODE_INDEX.OFFLINE, offlineNodesRes?.count)
                    
                } catch (error) {
                    console.error('获取构建资源数据失败:', error)
                    // 重置数据为默认值
                    updateNodeData(NODE_INDEX.AVAILABLE, 0)
                    updateNodeData(NODE_INDEX.OFFLINE, 0)
                } finally {
                    loading.value = false
                }
            }

            const fetchDataAvailableNodes = async () => {
                if (!props.timeRange || props.timeRange.length < 2) return
            
                loading.value = true
                const promqlConfigs = [
                    { id: 'idleNodes', promql: freeLoadNum },
                    { id: 'lowConcurrencyNodes', promql: lowLoadNum },
                    { id: 'fullConcurrencyNodes', promql: fullLoadNum }
                ]
                try {
                    const [startTime, endTime] = props.timeRange

                    const results = await Promise.all(
                        promqlConfigs.map(config =>
                            proxy.$store.dispatch('pipelines/getMetrics', {
                                promql: config.promql, // 这些是静态promql
                                start_time: startTime,
                                end_time: endTime
                            })
                        )
                    )
                    // 根据返回结果解析赋值给对应 label 的 value
                    nodeProgressData.value = nodeProgressData.value.map(item => {
                        const configIndex = promqlConfigs.findIndex(config => config.id === item.label)
                        if (configIndex !== -1) {
                            return { ...item, value: extractValue(results[configIndex]) }
                        }
                        return item
                    })
                } catch (error) {
                    console.error('获取当前可用节点的类型数据失败:', error)
                    // 失败时重置为0
                    nodeProgressData.value = nodeProgressData.value.map(item => {
                        if (promqlConfigs.some(config => config.id === item.label)) {
                            return { ...item, value: 0 }
                        }
                        return item
                    })
                } finally {
                    loading.value = false
                }

            }
            const handleClickJump = (label) => {
                emit('item-click', label)
            }

            // 监听时间范围变化
            watch(() => props.timeRange, fetchDataAvailableNodes, { deep: true })

            onMounted(() => {
                fetchData()
                fetchDataAvailableNodes()
            })

            return {
                loading,
                nodeProgressData,
                fetchData,
                handleClickJump
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