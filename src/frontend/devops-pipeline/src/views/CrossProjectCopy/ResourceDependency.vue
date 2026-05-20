<template>
    <div class="resource-dependency">
        <!-- 顶部描述区域 -->
        <div class="resource-dependency__header">
            <div class="header-content">
                <p class="header-title">
                    {{ $t('resourceDependency') }}
                    <i class="devops-icon icon-question-circle-shape header-icon"></i>
                </p>
                <p class="header-desc">
                    {{ $t('resourceDependencytips') }}
                </p>
            </div>
            <div class="header-actions">
                <bk-button
                    text
                    @click="handleRefresh"
                >
                    {{ $t('recheck') }}
                </bk-button>
            </div>
        </div>
        
        <hr style="border: 0; border-top: 1px solid #DCDEE5; margin: 16px 0 24px;" />

        <!-- 推荐处理策略提示 -->
        <div class="recommend-strategy">
            <Logo
                name="recommend"
                size="32"
                class="recommend-logo"
            />
            <div class="recommend-content">
                <p class="recommend-title">{{ $t('recommendStrategy') }}</p>
                <i18n
                    path="recommendDesc"
                    tag="p"
                    class="recommend-desc"
                >
                    <span class="bold">{{ $t('reuseExisting') }}</span>
                </i18n>
            </div>
            <span
                class="recommend-btn"
                @click="handleSetStrategy"
            >
                <Logo
                    name="electricity"
                    size="14"
                />
                {{ $t('oneClickSetup') }}
            </span>
        </div>

        <!-- 主内容区域 -->
        <div class="resource-dependency__content">
            <!-- 左侧资源列表 -->
            <div class="resource-list">
                <!-- 流水线依赖资源 -->
                <div class="category-section">
                    <div class="category-header">
                        <span class="category-name">{{ $t('pipelineDependencyResources') }}</span>
                        <span class="category-count">{{ $t('itemsPending', [pendingPipelineCount]) }}</span>
                    </div>
                    <div class="category-items">
                        <div
                            v-for="item in pipelineResourceTypes"
                            :key="item.type"
                            class="resource-item"
                            :class="{ 'is-active': activeResourceType === item.type }"
                            @click="handleSelectResourceType(item.type)"
                        >
                            <span class="item-name">{{ item.name }}</span>
                            <div class="item-badges">
                                <span class="badge badge-source">{{ item.sourceCount }}</span>
                                <span class="badge badge-target">{{ item.targetCount }}</span>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 流水线归属资源 -->
                <div class="category-section">
                    <div class="category-header">
                        <span class="category-name">{{ $t('pipelineOwnershipResources') }}</span>
                    </div>
                    <div class="category-items">
                        <div
                            v-for="item in scanResourceTypes"
                            :key="item.type"
                            class="resource-item"
                            :class="{ 'is-active': activeResourceType === item.type }"
                            @click="handleSelectResourceType(item.type)"
                        >
                            <span class="item-name">{{ item.name }}</span>
                            <div class="item-badges">
                                <span class="badge badge-count">{{ item.count }}</span>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 目标项目冲突 -->
                <div class="category-section">
                    <div class="category-header">
                        <span class="category-name">{{ $t('targetProjectConflict') }}</span>
                        <span class="category-count">{{ $t('itemsPending', [pendingTargetCount]) }}</span>
                    </div>
                    <div class="category-items">
                        <div
                            v-for="item in targetResourceTypes"
                            :key="item.type"
                            class="resource-item"
                            :class="{ 'is-active': activeResourceType === item.type }"
                            @click="handleSelectResourceType(item.type)"
                        >
                            <span class="item-name">{{ item.name }}</span>
                            <div class="item-badges">
                                <span class="badge badge-source">{{ item.sourceCount }}</span>
                                <span class="badge badge-target">{{ item.targetCount }}</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 右侧详情区域 -->
            <div class="resource-detail">
                <!-- 资源标题 -->
                <div class="detail-header">
                    <p>
                        <span class="detail-title">{{ currentResourceTitle }}</span>
                        <span class="detail-subtitle">{{ currentResourceSubtitle }}</span>
                    </p>
                    <bk-tag
                        theme="success"
                        v-if="currentResourceSubtitle === 'auto-id-strategy'"
                    >
                        {{ $t('autoIdStrategySelected') }}
                    </bk-tag>
                </div>

                <!-- 标签页和搜索 -->
                <div
                    class="detail-tabs-wrapper"
                    v-if="showTabsView"
                >
                    <div class="custom-tabs">
                        <div
                            v-for="tab in tabList"
                            :key="tab.name"
                            :class="['custom-tab-item', { 'is-active': activeTab === tab.name }]"
                            @click="activeTab = tab.name"
                        >
                            <span class="tab-label">{{ tab.label }}</span>
                            <span class="tab-count">{{ tab.count }}</span>
                        </div>
                    </div>
                    <div class="tabs-right">
                        <bk-input
                            v-model="detailSearchKeyword"
                            :placeholder="$t('searchResourceName')"
                            clearable
                            right-icon="bk-icon icon-search"
                        >
                        </bk-input>
                    </div>
                </div>

                <!-- 资源详情列表 -->
                <div class="detail-content">
                    <p class="empty-tips">{{ $t('noData') }}</p>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    export default {
        name: 'ResourceDependency',
        props: {
            formData: {
                type: Object,
                required: true
            }
        },
        data () {
            return {
                searchKeyword: '',
                detailSearchKeyword: '',
                activeResourceType: 'template',
                activeTab: 'all',
                pendingPipelineCount: 17,
                pendingTargetCount: 3,
                tabList: [
                    { name: 'all', label: this.$t('all'), count: 5 },
                    { name: 'pending', label: this.$t('pending'), count: 3 },
                    { name: 'processed', label: this.$t('processed'), count: 2 }
                ],
                // 流水线依赖资源
                pipelineResourceTypes: [
                    { type: 'template', name: this.$t('pipelineTemplate'), subtitle: this.$t('templateSubtitle'), sourceCount: 3, targetCount: 5 },
                    { type: 'atom', name: this.$t('codeRepository'), subtitle: this.$t('codeRepositorySubtitle'), sourceCount: 4, targetCount: 5 },
                    { type: 'buildEnv', name: this.$t('buildEnvironment'), subtitle: this.$t('buildEnvSubtitle'), sourceCount: 2, targetCount: 3 },
                    { type: 'buildNode', name: this.$t('buildNode'), subtitle: this.$t('buildNodeSubtitle'), sourceCount: 2, targetCount: 3 },
                    { type: 'deployEnv', name: this.$t('deployEnvironment'), subtitle: this.$t('deployEnvSubtitle'), sourceCount: 2, targetCount: 3 },
                    { type: 'deployNode', name: this.$t('deployNode'), subtitle: this.$t('deployNodeSubtitle'), sourceCount: 2, targetCount: 3 },
                    { type: 'ticket', name: this.$t('credential'), subtitle: this.$t('credentialSubtitle'), sourceCount: 2, targetCount: 3 }
                ],
                // 流水线归属资源
                scanResourceTypes: [
                    { type: 'transferSet', name: this.$t('pipelineLabel'), subtitle: this.$t('pipelineLabelSubtitle'), count: 20 },
                    { type: 'transferGroup', name: this.$t('pipelineGroup'), subtitle: this.$t('pipelineGroupSubtitle'), count: 7 }
                ],
                // 目标项目冲突
                targetResourceTypes: [
                    { type: 'pipelineConflict', name: this.$t('pipelineConflict'), subtitle: this.$t('pipelineConflictSubtitle'), sourceCount: 3, targetCount: 3 }
                ]
            }
        },
        computed: {
            // 是否显示标签页视图（流水线依赖资源和目标项目冲突）
            showTabsView () {
                const pipelineTypes = this.pipelineResourceTypes.map(item => item.type)
                const targetTypes = this.targetResourceTypes.map(item => item.type)
                return pipelineTypes.includes(this.activeResourceType) || targetTypes.includes(this.activeResourceType)
            },
            // 当前选中的资源标题
            currentResourceTitle () {
                const allTypes = [
                    ...this.pipelineResourceTypes,
                    ...this.scanResourceTypes,
                    ...this.targetResourceTypes
                ]
                const current = allTypes.find(item => item.type === this.activeResourceType)
                return current ? current.name : ''
            },
            // 当前选中的资源副标题
            currentResourceSubtitle () {
                // 流水线依赖资源
                const pipelineResource = this.pipelineResourceTypes.find(item => item.type === this.activeResourceType)
                if (pipelineResource) {
                    return pipelineResource.subtitle
                }

                // 流水线归属资源
                const scanResource = this.scanResourceTypes.find(item => item.type === this.activeResourceType)
                if (scanResource) {
                    return scanResource.subtitle
                }

                // 目标项目冲突
                const targetResource = this.targetResourceTypes.find(item => item.type === this.activeResourceType)
                if (targetResource) {
                    // 如果第一步选择了自动生成新 ID 策略，显示提示标签
                    const pipelineIdStrategy = this.formData?.configScope?.pipelineIdStrategy
                    if (pipelineIdStrategy === 'auto') {
                        return 'auto-id-strategy' // 特殊标记，用于模板中判断
                    }
                    return targetResource.subtitle
                }

                return ''
            }
        },
        methods: {
            handleUpdate (stepName, field, value) {
                this.$emit('update-form-data', stepName, field, value)
            },
            handleRefresh () {
                console.log('重新检查')
            },
            handleSetStrategy () {
                console.log('一键设置')
            },
            handleSelectResourceType (type) {
                this.activeResourceType = type
                console.log('选择资源类型：', type)
            }
        }
    }
</script>

<style lang="scss" scoped>
    .resource-dependency {
        display: flex;
        flex-direction: column;
        height: 100%;

        &__header {
            display: flex;
            align-items: flex-start;
            justify-content: space-between;

            .header-content {
                flex: 1;

                .header-title {
                    display: flex;
                    align-items: center;
                    margin-bottom: 8px;
                    font-size: 14px;
                    font-weight: 700;
                    color: #313238;
                    
                    .header-icon {
                        margin-left: 8px;
                        color: #979BA5;
                        font-size: 16px;
                    }
                }

                .header-desc {
                    font-size: 12px;
                    color: #979BA5;
                    line-height: 20px;
                }
            }

            .header-actions {
                margin-left: 16px;
                font-size: 14px;
                color: #3A84FF;
            }
        }

        .recommend-strategy {
            display: flex;
            align-items: center;
            height: 74px;
            padding: 12px 16px 12px 24px;
            background: linear-gradient(90deg, #F1F6FF 3.58%, #E9F3FF 97.57%);
            margin-bottom: 8px;

            .recommend-logo {
                margin-right: 16px;
            }

            .recommend-content {
                flex: 1;
            }

            .recommend-title {
                font-weight: 700;
                margin-bottom: 8px;
                color: #3a84ff;
                font-size: 14px;
            }

            .recommend-desc {
                color: #4D4F56;
                font-size: 12px;
                font-weight: 400;
                .bold {
                    font-weight: 700;
                }
            }

            .recommend-btn {
                display: flex;
                height: 32px;
                min-width: 64px;
                padding: 0 16px;
                justify-content: center;
                align-items: center;
                gap: 6px;
                cursor: pointer;
                border-radius: 16px;
                border: 1px solid #3A84FF;
                color: #3A84FF;
                font-size: 14px;
                background-color: #fff;

                svg {
                    vertical-align: middle;
                }
            }
        }

        &__content {
            display: flex;
            flex: 1;
            overflow: hidden;

            .resource-list {
                display: flex;
                width: 200px;
                flex-shrink: 0;
                flex-direction: column;
                align-items: flex-start;
                align-self: stretch;
                background: #FAFBFD;
                border-right: 1px solid #DCDEE5;
                border-radius: 2px;
                overflow: auto;

                .category-section {
                    width: 100%;
                    margin-top: 8px;

                    &:last-child {
                        border-bottom: none;
                    }

                    .category-header {
                        display: flex;
                        height: 36px;
                        align-items: center;
                        justify-content: space-between;
                        padding: 8px 12px;
                        background: #FAFBFD;
                        font-size: 12px;

                        .category-name {
                            color: #979BA5;
                        }

                        .category-count {
                            display: flex;
                            padding: 0 6px;
                            align-items: center;
                            align-content: center;
                            flex-wrap: wrap;
                            border-radius: 8px;
                            background:#FDEED8;
                            font-size: 10px;
                            color: #E38B02;
                        }
                    }

                    .category-items {
                        .resource-item {
                            display: flex;
                            align-items: center;
                            justify-content: space-between;
                            height: 36px;
                            line-height: 36px;
                            padding: 0 12px 0 24px;
                            cursor: pointer;
                            border-right: 2px solid transparent;

                            &:hover {
                                background: #F5F7FA;
                            }

                            &.is-active {
                                background: #E1ECFF;
                                border-right: 2px solid #3A84FF;

                                .item-name {
                                    color: #3A84FF;
                                }
                            }

                            .item-name {
                                font-size: 12px;
                                color: #4D4F56;
                            }

                            .item-badges {
                                display: flex;
                                justify-content: center;
                                align-items: center;
                                height: 16px;
                                line-height: 16px;
                                border-radius: 20px;
                                background: #FDF4E9;
                                font-size: 10px;

                                .badge {

                                    &.badge-source {
                                        border-radius: 20px;
                                        padding: 0 6px;
                                        background: #FDF4E9;
                                        color: #E38B02;
                                    }

                                    &.badge-target {
                                        border-radius: 20px;
                                        padding: 0 6px;
                                        background: #F0F1F5;
                                        color: #4D4F56;
                                    }

                                    &.badge-count {
                                        border-radius: 20px;
                                        padding: 0 6px;
                                        background: #F0F1F5;
                                        color: #4D4F56;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            .resource-detail {
                display: flex;
                flex-direction: column;
                flex: 1;
                padding: 16px 0 16px 16px;
                background: #FFFFFF;

                .detail-header {
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    margin-bottom: 16px;

                    .detail-title {
                        font-size: 14px;
                        font-weight: 700;
                        color: #313238;
                        margin-right: 16px;
                    }

                    .detail-subtitle {
                        font-size: 12px;
                        color: #979BA5;
                    }
                }

                .detail-tabs-wrapper {
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    height: 32px;
                    margin-bottom: 24px;

                    .custom-tabs {
                        display: inline-flex;
                        align-items: center;
                        background: #F0F1F5;
                        border-radius: 2px;
                        padding: 4px;
                        gap: 4px;

                        .custom-tab-item {
                            display: inline-flex;
                            align-items: center;
                            padding: 5px 12px;
                            border-radius: 2px;
                            font-size: 12px;
                            cursor: pointer;
                            transition: all 0.2s ease;
                            white-space: nowrap;

                            .tab-label {
                                color: #4D4F56;
                            }

                            .tab-count {
                                margin-left: 4px;
                                color: #4D4F56;
                                padding: 0px 6px;
                                border-radius: 8px;
                                background-color: #fff;
                            }

                            &.is-active {
                                background: #FFFFFF;
                                box-shadow: 0 2px 4px 0 #0000001a;

                                .tab-label {
                                    color: #3A84FF;
                                }

                                .tab-count {
                                    color: #3A84FF;
                                    background: #E1ECFF;
                                }
                            }
                        }
                    }

                    .tabs-right {
                        display: flex;
                        justify-content: flex-end;
                        flex: 1;
                        margin-left: 20%;
                    }
                }

                .detail-content {
                    flex: 1;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                }
            }
        }
    }
</style>
