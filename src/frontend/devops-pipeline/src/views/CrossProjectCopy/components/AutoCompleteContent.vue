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
                            <a
                                v-if="item.resourceType !== 'PIPELINE_LABEL' && item.resourceType !== 'PIPELINE_GROUP'"
                                @click="handleJump(item)"
                                class="jump-icon"
                            >
                                <Logo
                                    size="12"
                                    name="tiaozhuan"
                                    style="border: 1px solid #000"
                                />
                            </a>
                        </bk-tag>
                    </div>
                </template>
            </bk-collapse-item>
        </bk-collapse>
    </div>
</template>

<script>
    import Logo from '@/components/Logo'
    import { PipelineCopyResourceTypeI18nKey, ResourceTypeAutoTipsI18nKey } from '@/store/modules/crossProjectCopy/constants'
    
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
            groupsData () {
                return this.resourceData || []
            },
            projectId () {
                return this.$route.params.projectId
            },
        },
        watch: {
            resourceData: {
                immediate: true,
                handler (newData) {
                    if (newData && newData.length > 0) {
                        this.activeCollapseNames = newData.map(group => group.resourceType)
                    }
                }
            }
        },
        methods: {
            getGroupTitle (resourceType) {
                const i18nKey = PipelineCopyResourceTypeI18nKey[resourceType]
                return this.$t(i18nKey)
            },
            
            getGroupSubtitle (resourceType) {
                const i18nKey = ResourceTypeAutoTipsI18nKey[resourceType]
                return this.$t(i18nKey)
            },
            handleJump (item) {
                const { resourceType, resourceId, resourceName } = item
                const projectId = this.projectId
                let url = ''
                switch (resourceType) {
                    case 'PIPELINE_TEMPLATE':
                        url = `/console/pipeline/${projectId}/template/${resourceId}`
                        break
                    case 'REPOSITORY':
                        url = `/console/codelib/${projectId}/?id=${resourceId}&searchName=${resourceName}`
                        break
                    case 'BUILD_ENV':
                    case 'DEPLOY_ENV':
                        url = `/console/environment/${projectId}/pipeline/env/ALL/${resourceId}/node`
                        break
                    case 'BUILD_NODE':
                    case 'DEPLOY_NODE':
                        url = `/console/environment/${projectId}/pipeline/node/allNode?nodeHashId=${resourceId}`
                        break
                    case 'CREDENTIAL':
                        url = `/console/ticket/${projectId}`
                        break
                    case 'PIPELINE':
                    case 'PIPELINE_GROUP':
                    case 'PIPELINE_LABEL':
                        url = `/console/pipeline/${projectId}/${resourceId}/history/pipeline`
                        break
                }
                if (url) {
                    window.open(url, '_blank')
                }
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
    
    .jump-icon svg {
        margin-left: 4px;
        vertical-align: middle;
        cursor: pointer;
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
