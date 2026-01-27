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
    import { defineComponent, ref, onMounted } from 'vue'
    import ProgressBar from './ProgressBar.vue'
    import useInstance from '@/hook/useInstance'
    export default defineComponent({
        name: 'BuildResources',
        components: {
            ProgressBar
        },
        emits: ['item-click'],
        setup (_, { emit }) {
            const loading = ref(false)
            const { proxy } = useInstance()
            // 节点进度条数据
            // 全部节点 = 可用节点 + 离线节点
            // 可用节点 = 空闲节点 + 满并发运行节点 + 并发低于 50% 节点 + 其他并发节点
            const nodeProgressData = ref([
                {
                    value: 0,
                    label: 'availableNodes',
                    type: 'can-use'
                },
                {
                    value: 0,
                    label: 'offlineNodes',
                    type: 'offline'
                },
                {
                    value: '--',
                    label: 'idleNodes',
                    type: 'free-load'
                },
                {
                    value: '--',
                    label: 'lowConcurrencyNodes',
                    type: 'low-load'
                },
                {
                    value: '--',
                    label: 'fullConcurrencyNodes',
                    type: 'full-load'
                },
                {
                    value: 1,
                    label: 'otherNodes',
                    type: 'other-load'
                }
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

            const handleClickJump = (label) => {
                console.log('click',label)
                emit('item-click', label)
            }

            onMounted(() => {
                fetchData()
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