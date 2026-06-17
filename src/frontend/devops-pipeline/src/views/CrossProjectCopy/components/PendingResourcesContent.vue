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
                    :action-button-text="$t('completedByMe')"
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
            resourceData: {
                type: Array,
                default: () => []
            }
        },
        data () {
            return {
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
                    { name: 'all', label: this.$t('All'), totalCount: allCount },
                    { name: 'UNPROCESSED', label: this.$t('pending'), totalCount: unprocessedCount },
                    { name: 'PROCESSED', label: this.$t('processed'), totalCount: processedCount }
                ]
            },
            statusTabs () {
                return this.tabs.map(tab => ({
                    ...tab,
                    label: `${tab.label}（${tab.totalCount}）`
                }))
            }
        },
        watch: {
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
            filterResourceList () {
                if (this.currentStatusTab === 'all') {
                    this.resourceList = this.allResources
                } else {
                    this.resourceList = this.allResources.filter(item => item.status === this.currentStatusTab)
                }
            },
            handleStatusTabChange (tabName) {
                this.currentStatusTab = tabName
                this.filterResourceList()
            },
            handleComplete (updatedData) {
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
