<template>
    <div class="monitor-section half-section">
        <div class="section-title">{{ $t('realTimeMonitor.currentBuildResources') }}</div>
        <div v-bkloading="{ isLoading: loading }">
            <progress-bar :data="nodeProgressData" />
        </div>
    </div>
</template>

<script>
    import { defineComponent, ref, onMounted } from 'vue'
    import ProgressBar from './ProgressBar.vue'

    export default defineComponent({
        name: 'BuildResources',
        components: {
            ProgressBar
        },
        setup () {
            const loading = ref(false)
        
            // 节点进度条数据
            // 全部节点 = 可用节点 + 离线节点
            // 可用节点 = 空闲节点 + 满并发运行节点 + 并发低于 50% 节点 + 其他并发节点
            const nodeProgressData = ref([
                {
                    value: 1,
                    label: 'availableNodes',
                    type: 'can-use'
                },
                {
                    value: 1,
                    label: 'offlineNodes',
                    type: 'offline'
                },
                {
                    value: 1,
                    label: 'idleNodes',
                    type: 'free-load'
                },
                {
                    value: 1,
                    label: 'lowConcurrencyNodes',
                    type: 'low-load'
                },
                {
                    value: 1,
                    label: 'fullConcurrencyNodes',
                    type: 'full-load'
                },
                {
                    value: 1,
                    label: 'otherNodes',
                    type: 'other-load'
                }
            ])

            /**
             * 获取构建资源数据（模拟数据，添加loading效果）
             */
            const fetchData = async () => {
                loading.value = true
                try {
                    // 模拟异步请求延迟
                    await new Promise(resolve => setTimeout(resolve, 500))
                    // 这里可以添加实际的API调用来获取构建资源数据
                    console.log('构建资源数据获取完成')
                } catch (error) {
                    console.error('获取构建资源数据失败:', error)
                } finally {
                    loading.value = false
                }
            }

            onMounted(() => {
                fetchData()
            })

            return {
                loading,
                nodeProgressData,
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