<template>
    <!-- 流水线分组 -->
    <BaseResourceItem
        :key="item.copyStrategy"
        :item="item"
        :header-title="$t('processingStrategy')"
        :show-affected-count="false"
        :show-jump-icon="false"
        :strategies="strategyOptions"
        :is-read-only="isReadOnly"
        @strategy-change="handleStrategyChange"
    >
        <template #extra-config>
            <div
                v-if="isAutoReuse"
                class="group-config"
            >
                <bk-collapse v-model="activeCollapseNames">
                    <bk-collapse-item
                        v-for="groupCategory in filteredGroupsData"
                        :key="groupCategory.name"
                        :name="groupCategory.name"
                        hide-arrow
                    >
                        <div class="collapse-header">
                            <i
                                :class="['devops-icon icon-down-shape', {
                                    'is-collaped': !activeCollapseNames.includes(groupCategory.name)
                                }]"
                            />
                            <span class="header-title">{{ $t(groupCategory.titleKey, [groupCategory.groups.length]) }}</span>
                            <span class="header-subtitle">{{ $t(groupCategory.subtitleKey) }}</span>
                        </div>
                        <template #content>
                            <div class="group-split-layout">
                                <!-- 左侧：分组列表 -->
                                <div class="group-list">
                                    <div
                                        v-for="group in groupCategory.groups"
                                        :key="group.resourceId"
                                        class="group-item"
                                        :class="{ active: groupCategory.expandedGroupId === group.resourceId }"
                                        @click="toggleGroupExpand(groupCategory, group)"
                                    >
                                        <div class="group-item-header">
                                            <logo
                                                class="devops-icon"
                                                size="14"
                                                name="group"
                                            />
                                            <span
                                                class="group-name"
                                                :title="group.resourceName"
                                            >{{ group.resourceName }}</span>
                                        </div>
                                        <span class="pipeline-count">{{ group.pipelineCount }}</span>
                                    </div>
                                </div>

                                <!-- 右侧：流水线表格区域 -->
                                <div class="pipeline-area">
                                    <div
                                        v-if="groupCategory.expandedGroupId"
                                        class="pipeline-content"
                                    >
                                        <p
                                            class="pipeline-title"
                                            v-if="getCurrentGroup(groupCategory)"
                                        >
                                            {{ $t('affectPipelines', [getCurrentGroup(groupCategory).resourceName, groupCategory.pipelinePagination.count]) }}
                                        </p>
                                        <div class="pipeline-header">
                                            <CustomTabs
                                                :active-tab="groupCategory.activeTab"
                                                :tabs="tabList"
                                                @tab-change="handleTabChange(groupCategory, $event)"
                                            />
                                            <div class="header-right">
                                                <!-- 搜索框 -->
                                                <bk-input
                                                    v-model="groupCategory.pipelineName"
                                                    :placeholder="$t('searchPipelineName')"
                                                    :right-icon="'bk-icon icon-search'"
                                                    :clearable="true"
                                                    class="pipeline-search"
                                                    @change="handleSearchInput(groupCategory, $event)"
                                                    @clear="handleSearchClear(groupCategory)"
                                                ></bk-input>
                                            </div>
                                        </div>

                                        <!-- 流水线表格 -->
                                        <bk-table
                                            v-bkloading="{ isLoading: groupCategory.loadingPipelines }"
                                            :data="groupCategory.pipelines"
                                            :pagination="groupCategory.pipelinePagination"
                                            :outer-border="false"
                                            :header-border="false"
                                            :max-height="320"
                                            size="small"
                                            @page-change="handlePageChange(groupCategory, $event)"
                                            @page-limit-change="handlePageLimitChange(groupCategory, $event)"
                                        >
                                            <bk-table-column
                                                :label="$t('pipelineName')"
                                                prop="pipelineName"
                                            >
                                                <template slot-scope="{ row }">
                                                    {{ row.pipelineName }}
                                                </template>
                                            </bk-table-column>
                                            <bk-table-column
                                                :label="$t('status')"
                                                width="150"
                                            >
                                                <template slot-scope="{ row }">
                                                    <span class="status-indicator">
                                                        <span
                                                            class="status-dot"
                                                            :class="{ used: row.locked, unused: !row.locked }"
                                                        ></span>
                                                        <span
                                                            v-if="row.locked"
                                                            class="status-text"
                                                        >{{ $t('used') }}</span>
                                                        <span
                                                            v-else
                                                            class="status-text"
                                                        >{{ $t('unused') }}</span>
                                                    </span>
                                                </template>
                                            </bk-table-column>
                                            <bk-table-column
                                                :label="$t('operate')"
                                                width="150"
                                            >
                                                <template slot-scope="{ row }">
                                                    <a
                                                        class="view-detail-link"
                                                        @click="handleViewPipelineDetail(row)"
                                                    >
                                                        <Logo
                                                            name="tiaozhuan"
                                                            size="12"
                                                        />
                                                        {{ $t('checkPipeline') }}
                                                    </a>
                                                </template>
                                            </bk-table-column>
                                            <template #empty>
                                                <bk-exception
                                                    type="search-empty"
                                                    scene="part"
                                                ></bk-exception>
                                            </template>
                                        </bk-table>
                                    </div>
                                </div>
                            </div>
                        </template>
                    </bk-collapse-item>
                </bk-collapse>
            </div>
        </template>
    </BaseResourceItem>
</template>

<script>
    import BaseResourceItem from './BaseResourceItem.vue'
    import CustomTabs from './CustomTabs.vue'
    import Logo from '@/components/Logo'
    import { PipelineCopyStrategy, PipelineCopyResourceType } from '@/store/modules/crossProjectCopy/constants'

    export default {
        name: 'PipelineGroupResourceItem',
        components: {
            BaseResourceItem,
            CustomTabs,
            Logo
        },
        props: {
            item: {
                type: Object,
                required: true
            },
            // 是否只读模式
            isReadOnly: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                // 折叠面板激活项
                activeCollapseNames: ['reusable', 'new'],
                // 分组数据 UI 状态（groups 从 item.resources 获取）
                groupsData: [
                    {
                        name: 'reusable',
                        titleKey: 'reusableGroups',
                        subtitleKey: 'targetProjectExists',
                        groups: [],
                        // 每个分类独立的状态
                        expandedGroupId: null,
                        currentGroup: null,
                        pipelines: [],
                        loadingPipelines: false,
                        activeTab: 'all',
                        pipelineName: '',
                        pipelinePagination: {
                            current: 1,
                            limit: 10,
                            count: 0,
                            showLimit: true,
                            showTotalCount: true
                        }
                    },
                    {
                        name: 'new',
                        titleKey: 'newGroups',
                        subtitleKey: 'targetProjectNotExists',
                        groups: [],
                        // 每个分类独立的状态
                        expandedGroupId: null,
                        currentGroup: null,
                        pipelines: [],
                        loadingPipelines: false,
                        activeTab: 'all',
                        pipelineName: '',
                        pipelinePagination: {
                            current: 1,
                            limit: 10,
                            count: 0,
                            showLimit: true,
                            showTotalCount: true
                        }
                    }
                ]
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            taskId () {
                return this.$route.params.taskId
            },
            isAutoReuse () {
                const copyStrategy = this.isReadOnly
                    ? this.item.resources?.[0]?.copyStrategy
                    : this.item.copyStrategy
                return copyStrategy === PipelineCopyStrategy.PIPELINE_GROUP_AUTO_REUSE_OR_CREATE
            },
            strategyOptions () {
                return [
                    {
                        value: PipelineCopyStrategy.PIPELINE_GROUP_AUTO_REUSE_OR_CREATE,
                        label: this.$t('autoReusePipelineGroup'),
                        description: this.$t('autoReusePipelineGroupDesc'),
                        disabled: false,
                        recommended: true
                    },
                    {
                        value: PipelineCopyStrategy.PIPELINE_GROUP_IGNORE,
                        label: this.$t('skipAllPipelineGroup'),
                        description: this.$t('skipAllPipelineGroupDesc'),
                        disabled: false
                    }
                ]
            },
            filteredGroupsData () {
                return this.groupsData.filter(category => category.groups.length > 0)
            },
            tabList () {
                return [
                    { name: 'all', label: this.$t('all') },
                    { name: 'unused', label: this.$t('unused')},
                    { name: 'used', label: this.$t('used') }
                ]
            }
        },
        watch: {
            // 监听策略变化，自动加载分组数据
            'item.copyStrategy': {
                handler (newVal) {
                    if (newVal === PipelineCopyStrategy.PIPELINE_GROUP_AUTO_REUSE_OR_CREATE) {
                        this.autoSelectFirstGroup()
                    }
                },
                immediate: true
            },
            // 监听接口数据变化，同步分组列表
            'item.resources': {
                handler (newVal) {
                    this.initGroupsData(newVal)
                },
                immediate: true
            },
            // 监听 isAutoReuse 变化（只读模式下需要自动选中第一条数据）
            isAutoReuse: {
                handler (newVal) {
                    if (newVal && this.isReadOnly) {
                        this.autoSelectFirstGroup()
                    }
                },
                immediate: true
            }
        },
        methods: {
            autoSelectFirstGroup () {
                this.$nextTick(() => {
                    this.groupsData.forEach(category => {
                        if (category.groups.length > 0 && !category.expandedGroupId) {
                            const firstGroup = category.groups[0]
                            category.expandedGroupId = firstGroup.resourceId
                            category.currentGroup = firstGroup
                            this.fetchPipelinesData(category)
                        }
                    })
                })
            },
            getCurrentGroup (groupCategory) {
                if (!groupCategory.expandedGroupId) return null
                return groupCategory.groups.find(g => g.resourceId === groupCategory.expandedGroupId)
            },

            handleStrategyChange (value) {
                this.$emit('strategy-change', value)
            },
            handleSearchInput (groupCategory, _value) {
                groupCategory.pipelinePagination.current = 1
                this.fetchPipelinesData(groupCategory)
            },
            handleSearchClear (groupCategory) {
                groupCategory.pipelineName = ''
                groupCategory.pipelinePagination.current = 1
                this.fetchPipelinesData(groupCategory)
            },
            initGroupsData (resources) {
                if (!Array.isArray(resources)) {
                    return
                }

                this.$set(this.groupsData[0], 'groups', [])
                this.$set(this.groupsData[1], 'groups', [])

                resources.forEach(group => {
                    if (group.targetNameExists === true) {
                        this.groupsData[0].groups.push(group)
                    } else {
                        this.groupsData[1].groups.push(group)
                    }
                })
            },
            toggleGroupExpand (groupCategory, group) {
                if (groupCategory.expandedGroupId === group.resourceId) {
                    return
                }
                
                // 点击新分组，展开并加载数据
                groupCategory.expandedGroupId = group.resourceId
                groupCategory.currentGroup = group
                groupCategory.activeTab = 'all'
                groupCategory.pipelineName = ''
                groupCategory.pipelinePagination.current = 1
                this.fetchPipelinesData(groupCategory)
            },
            async fetchPipelinesData (groupCategory) {
                const resourceId = groupCategory.currentGroup?.resourceId
                groupCategory.loadingPipelines = true
                
                try {
                    const res = await this.$store.dispatch('crossProjectCopy/getResourcePipelines', {
                        projectId: this.projectId,
                        taskId: this.taskId,
                        resourceType: PipelineCopyResourceType.PIPELINE_GROUP,
                        resourceId: resourceId,
                        params: {
                            ...(this.innerActiveTab === 'used' ? { locked: true } : this.innerActiveTab === 'unused' ? { locked: false } : {}),
                            ...(this.innerSearchKeyword ? { pipelineName: this.innerSearchKeyword } : {}),
                            page: groupCategory.pipelinePagination.current,
                            pageSize: groupCategory.pipelinePagination.limit
                        }
                    })
                    
                    if (res) {
                        groupCategory.pipelines = res.records
                        groupCategory.pipelinePagination.count = res.count
                    }
                } catch (error) {
                    groupCategory.pipelines = []
                    groupCategory.pipelinePagination.count = 0
                } finally {
                    groupCategory.loadingPipelines = false
                }
            },
            handleTabChange (groupCategory, name) {
                groupCategory.activeTab = name
                groupCategory.pipelinePagination.current = 1
                this.fetchPipelinesData(groupCategory)
            },
            handlePageChange (groupCategory, page) {
                groupCategory.pipelinePagination.current = page
                this.fetchPipelinesData(groupCategory)
            },
            handlePageLimitChange (groupCategory, limit) {
                groupCategory.pipelinePagination.limit = limit
                groupCategory.pipelinePagination.current = 1
                this.fetchPipelinesData(groupCategory)
            },
            handleViewPipelineDetail (pipeline) {
                const url = `${location.origin}/console/pipeline/${this.projectId}/${pipeline.pipelineId}/history/pipeline`
                window.open(url, '_blank')
            }
        }
    }
</script>

<style lang="scss" scoped>
.group-config {
    margin-top: 16px;

    ::v-deep .bk-collapse-item-header,
    ::v-deep .bk-collapse-item-content {
        padding: 0;
    }

    ::v-deep .bk-collapse-item-header {
        height: auto;
    }

    ::v-deep .bk-collapse-item-content {
        padding: 16px 24px 8px;
    }

    .collapse-header {
        display: flex;
        align-items: center;
        padding: 5px 8px;
        height: auto;
        line-height: 20px;
        margin-top: 16px;
        background: #F0F1F5;

        .devops-icon.icon-down-shape {
            font-size: 12px;
            color: #4D4F56;
            display: inline-flex;
            transition: all 0.3s ease;
            margin-right: 8px;
            &.is-collaped {
                transform: rotate(-90deg);
            }
        }

        .header-title {
            font-size: 14px;
            font-weight: 400;
            color: #4D4F56;
        }

        .header-subtitle {
            font-size: 12px;
            color: #979BA5;
            margin-left: 16px;
        }
    }

    // 左右分栏布局
    .group-split-layout {
        display: flex;
        min-height: 200px;

        // 左侧：分组列表
        .group-list {
            flex: 0 0 220px;
            max-height: 500px;
            overflow-y: auto;
            border-right: 1px solid #DCDEE5;
            border-radius: 2px;

            .group-item {
                display: flex;
                align-items: center;
                justify-content: space-between;
                padding: 8px 16px;
                cursor: pointer;
                transition: all 0.3s;

                &:hover {
                    background: #F5F7FA;
                }

                &.active {
                    background: #E1ECFF;
                    padding-left: 14px;

                    .group-item-header {
                        .devops-icon {
                            color: #3A84FF;
                        }

                        .group-name {
                            color: #3A84FF;
                            font-weight: 500;
                        }
                    }

                    .pipeline-count {
                        background: #FFFFFF;
                        color: #1768EF;
                    }
                }

                .group-item-header {
                    display: flex;
                    align-items: center;
                    flex: 1;
                    min-width: 0;

                    .devops-icon {
                        font-size: 14px;
                        color: #979BA5;
                        margin-right: 8px;
                        flex-shrink: 0;
                    }

                    .group-name {
                        font-size: 12px;
                        color: #4D4F56;
                        overflow: hidden;
                        text-overflow: ellipsis;
                        white-space: nowrap;
                    }
                }

                .pipeline-count {
                    flex-shrink: 0;
                    margin-left: 8px;
                    padding: 0 6px;
                    text-align: center;
                    color: #979BA5;
                    background-color: #F0F1F5;
                    border-radius: 11px;
                    font-size: 12px;
                    line-height: 16px;
                }
            }
        }

        // 右侧：流水线区域
        .pipeline-area {
            flex: 1;
            min-width: 0;

            .pipeline-content {
                padding: 12px 16px 8px 16px;
                border-radius: 2px;

                .pipeline-title {
                    color: #4D4F56;
                    font-size: 12px;
                    margin-bottom: 16px;
                }

                .pipeline-header {
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    margin-bottom: 16px;

                    .header-right {
                        display: flex;
                        justify-content: flex-end;
                        flex: 1;
                        margin-left: 20%;
                    }
                }

                .status-indicator {
                    display: inline-flex;
                    align-items: center;
                    gap: 8px;

                    .status-dot {
                        display: inline-block;
                        width: 6px;
                        height: 6px;
                        border-radius: 50%;
                        position: relative;

                        &::before {
                            content: '';
                            position: absolute;
                            top: 50%;
                            left: 50%;
                            transform: translate(-50%, -50%);
                            width: 12px;
                            height: 12px;
                            border-radius: 50%;
                            opacity: 0.2;
                        }

                        &.unused {
                            background: #2DCB56;

                            &::before {
                                background: #2DCB56;
                            }
                        }

                        &.used {
                            background: #C4C6CC;

                            &::before {
                                background: #C4C6CC;
                            }
                        }
                    }

                    .status-text {
                        color: #4D4F56;

                        &.unused {
                            color: #2DCB56;
                        }

                        &.used {
                            color: #979BA5;
                        }
                    }
                }

                .view-detail-link {
                    display: inline-flex;
                    align-items: center;
                    color: #3A84FF;
                    font-size: 12px;
                    gap: 8px;
                    cursor: pointer;

                    &:hover {
                        color: #699df4;
                    }
                }
            }
        }
    }
}
</style>
