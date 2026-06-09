<template>
    <div class="auto-complete-content">
        <bk-collapse v-model="activeCollapseNames">
            <bk-collapse-item
                v-for="group in groupsData"
                :key="group.resourceType"
                :name="group.resourceType"
                hide-arrow
            >
                <div class="collapse-header">
                    <i
                        :class="['devops-icon icon-down-shape', {
                            'is-collaped': !activeCollapseNames.includes(group.resourceType)
                        }]"
                    />
                    <span class="header-title">{{ getGroupTitle(group.resourceType) }} ({{ group.totalCount }})</span>
                    <span class="header-subtitle">{{ getGroupSubtitle(group.resourceType) }}</span>
                </div>
                <template #content>
                    <div class="group-split-layout">
                        <bk-tag
                            v-for="item in group.resources"
                            :key="item.resourceId"
                            class="group-item"
                        >
                            {{ item.resourceName }}
                            <Logo
                                v-if="item.type === 'link'"
                                class="devops-icon"
                                size="14"
                                name="tiaozhuan"
                            />
                        </bk-tag>
                    </div>
                </template>
            </bk-collapse-item>
        </bk-collapse>
    </div>
</template>

<script>
    import Logo from '@/components/Logo'
    import { PipelineCopyResourceTypeMap } from '@/store/modules/crossProjectCopy/constants'
    
    export default {
        name: 'AutoCompleteContent',
        components: {
            Logo
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
                // 折叠面板激活的项 (默认全部展开)
                activeCollapseNames: []
            }
        },
        computed: {
            // 分组数据,直接使用 resourceData
            groupsData () {
                return this.resourceData || []
            }
        },
        watch: {
            // 监听 resourceData 变化,动态设置默认展开的面板
            resourceData: {
                immediate: true,
                handler (newData) {
                    if (newData && newData.length > 0) {
                        // 默认展开所有分组
                        this.activeCollapseNames = newData.map(group => group.resourceType)
                    }
                }
            }
        },
        methods: {
            // 根据 resourceType 获取对应的标题 (使用 constants 中的映射)
            getGroupTitle (resourceType) {
                return PipelineCopyResourceTypeMap[resourceType] || resourceType || '其他'
            },
            
            // 根据 resourceType 获取对应的完成动作描述
            getGroupSubtitle (resourceType) {
                const subtitleMap = {
                    'PIPELINE_TEMPLATE': '已自动复制为新模板',
                    'REPOSITORY': '已自动关联代码库',
                    'BUILD_ENV': '已自动新建环境',
                    'BUILD_NODE': '已自动转移节点',
                    'DEPLOY_ENV': '已自动新建环境',
                    'DEPLOY_NODE': '已自动转移节点',
                    'CREDENTIAL': '已自动复制为新凭据',
                    'PIPELINE_LABEL': '已自动新建标签',
                    'PIPELINE_GROUP': '已自动新建分组',
                    'PIPELINE': '已自动完成'
                }
                return subtitleMap[resourceType] || '已自动完成'
            }
        }
    }
</script>

<style lang="scss" scoped>
.auto-complete-content {
    background: white;
    border-radius: 2px;
    ::v-deep .bk-collapse-item-header,
    ::v-deep .bk-collapse-item-content {
        padding: 0;
    }

    ::v-deep .bk-collapse-item-header {
        height: auto;
    }

    ::v-deep .bk-collapse-item-content {
        padding: 12px 16px 24px;
    }

    .collapse-header {
        display: flex;
        align-items: center;
        padding: 5px 8px;
        height: auto;
        line-height: 20px;
        margin-top: 8px;
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

    .group-split-layout {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
        align-items: center;

        .group-item {
            display: flex;
            align-items: center;
            gap: 4px;
            white-space: nowrap;
        }
    }

}
</style>
