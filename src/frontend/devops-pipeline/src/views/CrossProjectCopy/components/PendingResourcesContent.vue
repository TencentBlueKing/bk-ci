<template>
    <div class="pending-resources-content">
        <!-- CustomTabs -->
        <custom-tabs
            :active-tab="currentStatusTab"
            :tabs="statusTabs"
            @tab-change="handleStatusTabChange"
            class="resource-tabs"
        />
        
        <!-- 资源列表 -->
        <div class="resource-list">
            <template v-if="resourceList && resourceList.length > 0">
                <resource-item-card
                    v-for="item in resourceList"
                    :key="item.id"
                    :data="item"
                    action-button-text="我已补齐"
                    @handle-complete="handleComplete"
                />
            </template>
            <bk-exception
                v-else
                type="empty"
                scene="part"
            />
        </div>
    </div>
</template>

<script>
    import CustomTabs from './CustomTabs.vue'
    import ResourceItemCard from './ResourceItemCard.vue'
    
    export default {
        name: 'PendingResourcesContent',
        components: {
            CustomTabs,
            ResourceItemCard
        },
        props: {
            // 接收父组件传递的资源数据
            resourceData: {
                type: Array,
                default: () => []
            }
        },
        data () {
            return {
                // 当前激活的状态tab
                currentStatusTab: 'all',
                // 资源列表数据 (原始数据)
                allResources: [],
                // 过滤后的资源列表 (用于显示)
                resourceList: []
            }
        },
        computed: {
            // 动态计算tab数量
            tabs () {
                const allCount = this.allResources.length
                const unprocessedCount = this.allResources.filter(item => item.status === 'UNPROCESSED').length
                const processedCount = allCount - unprocessedCount
                
                return [
                    { name: 'all', label: '全部', totalCount: allCount },
                    { name: 'UNPROCESSED', label: '待处理', totalCount: unprocessedCount },
                    { name: 'PROCESSED', label: '已处理', totalCount: processedCount }
                ]
            },
            // 用于显示的 tabs (带格式化label)
            statusTabs () {
                return this.tabs.map(tab => ({
                    ...tab,
                    label: `${tab.label}（${tab.totalCount}）`
                }))
            }
        },
        watch: {
            // 监听 resourceData 变化
            resourceData: {
                immediate: true,
                handler (newData) {
                    if (newData && newData.length > 0) {
                        this.allResources = newData
                        this.filterResourceList()
                    }
                }
            }
        },
        methods: {
            // 过滤资源列表 (前端过滤)
            filterResourceList () {
                if (this.currentStatusTab === 'all') {
                    this.resourceList = this.allResources
                } else {
                    this.resourceList = this.allResources.filter(item => item.status === this.currentStatusTab)
                }
            },
            
            // 状态tab切换
            handleStatusTabChange (tabName) {
                this.currentStatusTab = tabName
                this.filterResourceList()
            },
            
            // 我已补齐 - 通知父组件更新数据
            handleComplete (updatedData) {
                // 触发事件通知父组件更新对应的 resourceData
                this.$emit('update-resource', updatedData)
            }
        }
    }
</script>

<style lang="scss" scoped>
// 待补齐资源和资源转移处理事项
.pending-resources-content {

    .resource-list {
        background: white;
        padding: 16px 0;
        border-radius: 2px;
    }
}
</style>
