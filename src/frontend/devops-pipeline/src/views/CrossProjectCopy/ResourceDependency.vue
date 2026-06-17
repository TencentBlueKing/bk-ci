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
                <!-- <bk-button
                    v-if="!isLoading"
                    text
                    @click="handleRecheck"
                >
                    {{ $t('recheck') }}
                </bk-button> -->
            </div>
        </div>
        
        <hr style="border: 0; border-top: 1px solid #DCDEE5; margin: 16px 0 24px;" />

        <div
            v-bkloading="{ isLoading: isLoading, title: $t('rechecking'), zIndex: 10 }"
            class="resource-dependency__loading"
        >
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
                    v-bkloading="{ isLoading: setupLoading, size: 'small', color: 'rgba(255, 255, 255, 0)' }"
                    @click="handleSetStrategy"
                >
                    <template v-if="!setupLoading">

                        <Logo
                            name="electricity"
                            size="14"
                        />
                        {{ $t('oneClickSetup') }}
                    </template>
                </span>
            </div>
            <!-- 处理策略完成提示 -->
            <bk-tag
                v-if="showStrategyCompletedTag"
                closable
                class="strategy-completed-tag"
                theme="success"
                @close="handleCloseStrategyTag"
            >
                <div class="tag-content">
                    <Logo
                        name="check-circle-shape"
                        size="16px"
                    />
                    <p>
                        <span> {{ $t('strategyCompleted', [strategyCompletedData.processedCount]) }}</span>
                        <i18n
                            path="strategyCompletedDesc"
                            tag="p"
                        >
                            <span v-if="strategyCompletedData.nodeNotSetCount > 0">{{ $t('nodeAuthorized', [strategyCompletedData.nodeNotSetCount]) }}</span>
                        </i18n>
                    </p>
                </div>
            </bk-tag>

            <!-- 主内容区域 -->
            <div
                class="resource-dependency__content"
                v-if="resourceData.length"
            >
                <!-- 左侧资源列表 -->
                <div class="resource-list">
                    <div
                        v-for="category in visibleResourceCategories"
                        :key="category.type"
                        class="category-section"
                    >
                        <div class="category-header">
                            <span class="category-name">{{ category.name }}</span>
                            <span
                                v-if="category.totalPendingCount > 0"
                                class="category-count"
                            >
                                {{ $t('itemsPending',[category.totalPendingCount]) }}
                            </span>
                        </div>
                        <div class="category-items">
                            <div
                                v-for="item in category.items"
                                :key="item.type"
                                class="resource-item"
                                :class="{ 'is-active': activeResourceType === item.type }"
                                @click="handleSelectResourceType(item.type)"
                            >
                                <span class="item-name">{{ item.name }}</span>
                                <div class="item-badges">
                                    <span
                                        v-if="item.unprocessedCount"
                                        class="badge badge-source"
                                    >
                                        {{ item.unprocessedCount }}
                                    </span>
                                    <span
                                        v-if="item.totalCount"
                                        class="badge badge-target"
                                    >
                                        {{ item.totalCount }}
                                    </span>
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
                            v-if="isTargetConflictType && configScopeData?.pipelineCopyStrategy === PipelineIdStrategy.PIPELINE_CREATE_NEW_ID"
                        >
                            {{ $t('autoIdStrategySelected') }}
                        </bk-tag>
                    </div>

                    <!-- 标签页和搜索 -->
                    <div
                        class="detail-tabs-wrapper"
                        v-if="showTabsView"
                    >
                        <CustomTabs
                            v-if="!isReadOnly"
                            class="tabs-left"
                            :active-tab="activeTab"
                            :tabs="tabList"
                            @tab-change="activeTab = $event"
                        />
                        <div class="tabs-right">
                            <bk-input
                                v-model="resourceName"
                                :placeholder="$t('searchResourceName')"
                                clearable
                                right-icon="bk-icon icon-search"
                            >
                            </bk-input>
                        </div>
                    </div>

                    <!-- 资源详情列表 -->
                    <div class="detail-content">
                        <!-- 单策略资源类型（流水线标签、流水线分组）：只渲染一个策略选择组件 -->
                        <template v-if="isSingleStrategyResource && singleStrategyItem.resourceType">
                            <component
                                :is="currentResourceComponent"
                                :item="singleStrategyItem"
                                :is-read-only="isReadOnly"
                                @strategy-change="handleSingleStrategyChange"
                            />
                        </template>
                        <!-- 普通资源类型：遍历 resources 列表渲染 -->
                        <template v-else-if="currentResourceList.length > 0">
                            <component
                                :is="currentResourceComponent"
                                v-for="(item, index) in currentResourceList"
                                :key="item.resourceId"
                                :item="item"
                                :is-read-only="isReadOnly"
                                :is-last="index === currentResourceList.length - 1"
                                :credential-options="credentialOptions"
                                :is-oauth="getOAuthStatus(item)"
                                :oauth-url="getOAuthUrl(item)"
                                @strategy-change="handleComponentStrategyChange(index, $event)"
                                @credential-select-change="handleCredentialSelectChange($event, index)"
                                @refresh-oauth-authorize="handleRefreshOAuth(item)"
                            />
                        </template>
                        <bk-exception
                            v-else
                            type="empty"
                            scene="part"
                            class="detail-empty"
                        />
                    </div>
                </div>
            </div>
            <bk-exception
                v-else
                type="empty"
                scene="part"
                class="source-empty"
            />
        </div>

        <!-- 高风险操作提示弹窗 -->
        <bk-dialog
            v-model="riskWarningDialog.visible"
            theme="primary"
            :mask-close="false"
            :esc-close="false"
            :width="640"
            footer-position="right"
            header-position="left"
            :title="$t('riskWarning')"
            :on-close="handleRiskConfirm"
        >
            <RiskWarningContent
                :resource-type="activeResourceType"
                :resource-names="[riskWarningDialog.resourceName]"
            />
            <template #footer>
                <bk-button
                    theme="primary"
                    @click="handleRiskConfirm"
                >
                    {{ $t('IKnow') }}
                </bk-button>
            </template>
        </bk-dialog>

        <!-- 重新检查弹窗 -->
        <bk-dialog
            v-model="isShowRecheckDialog"
            theme="primary"
            :mask-close="true"
            :esc-close="true"
            footer-position="center"
            :width="500"
            class="recheck-confirm-dialog"
        >
            <template #header>
                <span class="info-icon">
                    <i class="devops-icon icon-exclamation" />
                </span>
                <p class="recheck-title">{{ $t('confirmRecheckTitle') }}</p>
            </template>
            <div class="recheck-content">
                <p class="recheck-desc">{{ $t('recheckDesc') }}</p>
                <ul class="recheck-tips">
                    <li>
                        <i18n
                            path="recheckTip1"
                            tag="span"
                        >
                            <span class="highlight">{{ $t('recheckTip1Highlight') }}</span>
                        </i18n>
                    </li>
                    <li>
                        <i18n
                            path="recheckTip2"
                            tag="span"
                        >
                            <span class="highlight">{{ $t('recheckTip2Highlight') }}</span>
                        </i18n>
                    </li>
                    <li>{{ $t('recheckTip3') }}</li>
                </ul>
            </div>
            <template #footer>
                <bk-button
                    theme="primary"
                    @click="handleStartCheck"
                >
                    {{ $t('startCheck') }}
                </bk-button>
                <bk-button
                    @click="handleCancelRecheck"
                >
                    {{ $t('cancel') }}
                </bk-button>
            </template>
        </bk-dialog>
    </div>
</template>

<script>
    import Logo from '@/components/Logo'
    import { mapActions } from 'vuex'
    import { PipelineCopyResourceType, PipelineCopyStrategy, HIGH_RISK_STRATEGIES, PipelineIdStrategy } from '@/store/modules/crossProjectCopy/constants'

    // 导入动态组件
    import TemplateResourceItem from './components/TemplateResourceItem.vue'
    import RepositoryResourceItem from './components/RepositoryResourceItem.vue'
    import EnvResourceItem from './components/EnvResourceItem.vue'
    import NodeResourceItem from './components/NodeResourceItem.vue'
    import CredentialResourceItem from './components/CredentialResourceItem.vue'
    import PipelineLabelResourceItem from './components/PipelineLabelResourceItem.vue'
    import PipelineGroupResourceItem from './components/PipelineGroupResourceItem.vue'
    import ConflictResourceItem from './components/ConflictResourceItem.vue'
    import RiskWarningContent from './components/RiskWarningContent.vue'
    import CustomTabs from './components/CustomTabs.vue'

    export default {
        name: 'ResourceDependency',
        components: {
            Logo,
            TemplateResourceItem,
            RepositoryResourceItem,
            EnvResourceItem,
            NodeResourceItem,
            CredentialResourceItem,
            ConflictResourceItem,
            PipelineLabelResourceItem,
            PipelineGroupResourceItem,
            RiskWarningContent,
            CustomTabs
        },
        props: {
            analyzingPipeline: {
                type: Boolean,
                default: false
            },
            configScopeData: {
                type: Object,
                required: true
            },
            isReadOnly: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                resourceName: '',
                activeResourceType: '',
                activeTab: 'all',
                showStrategyCompletedTag: false,
                strategyCompletedData: {
                    processedCount: 0,
                    nodeNotSetCount: 0,
                    pipelineConflictCount: 0
                },
                setupLoading: false,
                rechecking: false,
                riskWarningDialog: {
                    visible: false,
                    resourceName: '', // 记录资源名称
                    currentItem: null // 记录当前操作的对象
                },
                oauthStatusMap: {},
                oauthUrlMap: {},
                // 确认重新检查弹窗显示状态
                isShowRecheckDialog: false,
                targetCredentialOptions: [],
                isLoadingData: false,
                resourceData: [],
                resourceCategories: [
                    {
                        type: 'pipelineDependency',
                        name: this.$t('pipelineDependencyResources'),
                        items: [
                            { type: PipelineCopyResourceType.PIPELINE_TEMPLATE, name: this.$t('pipelineTemplate'), subtitle: this.$t('templateSubtitle'), unprocessedCount: 0, totalCount: 0 },
                            { type: PipelineCopyResourceType.REPOSITORY, name: this.$t('codeRepository'), subtitle: this.$t('codeRepositorySubtitle'), unprocessedCount: 0, totalCount: 0 },
                            { type: PipelineCopyResourceType.BUILD_ENV, name: this.$t('buildEnvironment'), subtitle: this.$t('buildEnvSubtitle'), unprocessedCount: 0, totalCount: 0 },
                            { type: PipelineCopyResourceType.BUILD_NODE, name: this.$t('buildNode'), subtitle: this.$t('buildNodeSubtitle'), unprocessedCount: 0, totalCount: 0 },
                            { type: PipelineCopyResourceType.DEPLOY_ENV, name: this.$t('deployEnvironment'), subtitle: this.$t('deployEnvSubtitle'), unprocessedCount: 0, totalCount: 0 },
                            { type: PipelineCopyResourceType.DEPLOY_NODE, name: this.$t('deployNode'), subtitle: this.$t('deployNodeSubtitle'), unprocessedCount: 0, totalCount: 0 },
                            { type: PipelineCopyResourceType.CREDENTIAL, name: this.$t('credential'), subtitle: this.$t('credentialSubtitle'), unprocessedCount: 0, totalCount: 0 }
                        ]
                    },
                    {
                        type: 'pipelineOwnership',
                        name: this.$t('pipelineOwnershipResources'),
                        items: [
                            { type: PipelineCopyResourceType.PIPELINE_LABEL, name: this.$t('pipelineLabel'), subtitle: this.$t('pipelineLabelSubtitle'), unprocessedCount: 0, totalCount: 0 },
                            { type: PipelineCopyResourceType.PIPELINE_GROUP, name: this.$t('pipelineGroupSource'), subtitle: this.$t('pipelineGroupSubtitle'), unprocessedCount: 0, totalCount: 0 }
                        ]
                    },
                    {
                        type: 'targetConflict',
                        name: this.$t('targetProjectConflict'),
                        items: [
                            { type: PipelineCopyResourceType.PIPELINE, name: this.$t('pipelineConflict'), subtitle: this.$t('pipelineConflictSubtitle'), unprocessedCount: 0, totalCount: 0 }
                        ]
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
            // 统一的 loading 状态 包括：父组件轮询阶段 + 子组件数据加载阶段
            isLoading () {
                return this.analyzingPipeline || this.rechecking || this.isLoadingData
            },
            // 将资源数据数组转为以 resourceType 为 key 的映射，便于快速查找
            resourceDataMap () {
                const map = {}
                this.resourceData.forEach(item => {
                    map[item.resourceType] = item
                })
                return map
            },
            // 统计待处理的依赖资源和归属资源数量（用于提交检查第一项）
            pendingDependencyResourceCount () {
                return this.resourceData.reduce((count, data) => {
                    if (data.resourceType === PipelineCopyResourceType.PIPELINE) return count
                    return count + (data.resources || []).filter(item => item.status === 'UNPROCESSED').length
                }, 0)
            },
            // 统计高风险操作数量（用于提交检查第二项）
            highRiskOperationCount () {
                return Object.entries(HIGH_RISK_STRATEGIES).reduce((count, [type, strategy]) => {
                    const resources = this.resourceDataMap[type]?.resources || []
                    return count + resources.filter(item => item.copyStrategy === strategy).length
                }, 0)
            },
            // 统计待处理的冲突数量（用于提交检查第三项）
            pendingConflictCount () {
                const resources = this.resourceDataMap[PipelineCopyResourceType.PIPELINE]?.resources || []
                return resources.filter(item => item.status === 'UNPROCESSED').length
            },
            currentResourceComponent () {
                const componentMap = {
                    [PipelineCopyResourceType.PIPELINE_TEMPLATE]: 'TemplateResourceItem',
                    [PipelineCopyResourceType.REPOSITORY]: 'RepositoryResourceItem',
                    [PipelineCopyResourceType.BUILD_ENV]: 'EnvResourceItem',
                    [PipelineCopyResourceType.BUILD_NODE]: 'NodeResourceItem',
                    [PipelineCopyResourceType.DEPLOY_ENV]: 'EnvResourceItem',
                    [PipelineCopyResourceType.DEPLOY_NODE]: 'NodeResourceItem',
                    [PipelineCopyResourceType.CREDENTIAL]: 'CredentialResourceItem',
                    [PipelineCopyResourceType.PIPELINE_LABEL]: 'PipelineLabelResourceItem',
                    [PipelineCopyResourceType.PIPELINE_GROUP]: 'PipelineGroupResourceItem',
                    [PipelineCopyResourceType.PIPELINE]: 'ConflictResourceItem'
                }
                return componentMap[this.activeResourceType] || null
            },
            // 当前资源类型对应的全部资源列表
            currentAllResources () {
                return this.resourceDataMap[this.activeResourceType]?.resources || []
            },
            // 当前资源类型下按搜索关键词过滤后的资源列表
            searchedResources () {
                const all = this.currentAllResources
                if (!this.resourceName) return all
                const keyword = this.resourceName.toLowerCase()
                return all.filter(r => r.resourceName?.toLowerCase().includes(keyword))
            },
            // 当前资源类型的 tab 标签页数据（动态计算各状态数量）
            tabList () {
                const all = this.searchedResources
                const unprocessedCount = all.filter(r => r.status === 'UNPROCESSED').length
                const processedCount = all.filter(r => r.status !== 'UNPROCESSED').length
                return [
                    { name: 'all', label: this.$t('all'), count: all.length },
                    { name: 'UNPROCESSED', label: this.$t('pending'), count: unprocessedCount },
                    { name: 'PROCESSED', label: this.$t('processed'), count: processedCount }
                ]
            },
            // 当前资源类型对应的列表数据（根据搜索关键词 + tab 过滤）
            currentResourceList () {
                const resources = this.searchedResources
                if (this.activeTab === 'all') return resources
                if (this.activeTab === 'UNPROCESSED') return resources.filter(r => r.status === 'UNPROCESSED')
                if (this.activeTab === 'PROCESSED') return resources.filter(r => r.status !== 'UNPROCESSED')
                return resources
            },
            // 是否为单策略资源类型（流水线标签、流水线分组只有一个策略选择，不遍历 resources）
            isSingleStrategyResource () {
                return this.activeResourceType === PipelineCopyResourceType.PIPELINE_LABEL
                    || this.activeResourceType === PipelineCopyResourceType.PIPELINE_GROUP
            },
            // 单策略资源的顶层数据项（用于 PIPELINE_LABEL / PIPELINE_GROUP）
            singleStrategyItem () {
                return this.resourceDataMap[this.activeResourceType] || {}
            },
            // 过滤后可见的资源分类
            visibleResourceCategories () {
                return this.resourceCategories.map(category => {
                    const visibleItems = category.items.filter(item => {
                        return item.totalCount > 0
                    })

                    const totalPendingCount = visibleItems.reduce((sum, item) => {
                        return sum + (item.unprocessedCount || 0)
                    }, 0)

                    return {
                        ...category,
                        items: visibleItems,
                        totalPendingCount
                    }
                }).filter(category => {
                    return category.items.length > 0
                })
            },
            // 所有资源项的扁平列表（用于查找当前选中项）
            allResourceItems () {
                return this.resourceCategories.flatMap(category => category.items)
            },
            // 是否显示标签页视图（流水线归属资源不显示）
            showTabsView () {
                const ownershipCategory = this.resourceCategories.find(cat => cat.type === 'pipelineOwnership')
                const isOwnershipType = ownershipCategory?.items.some(item => item.type === this.activeResourceType)
                return !isOwnershipType
            },
            // 当前选中的资源标题
            currentResourceTitle () {
                const current = this.allResourceItems.find(item => item.type === this.activeResourceType)
                return current ? current.name : ''
            },
            // 当前选中的资源副标题
            currentResourceSubtitle () {
                const current = this.allResourceItems.find(item => item.type === this.activeResourceType)
                return current?.subtitle || ''
            },
            // 是否为目标项目冲突类型
            isTargetConflictType () {
                const targetCategory = this.resourceCategories.find(cat => cat.type === 'targetConflict')
                return targetCategory?.items.some(item => item.type === this.activeResourceType) || false
            },
            // 凭据选项（仅当资源类型为 CREDENTIAL 时返回）
            credentialOptions () {
                return this.activeResourceType === PipelineCopyResourceType.CREDENTIAL ? this.targetCredentialOptions : undefined
            },
            // 高风险弹窗内容配置
            riskWarningContent () {
                const type = this.activeResourceType
                const isTicket = type === PipelineCopyResourceType.CREDENTIAL
                const isNode = type === PipelineCopyResourceType.BUILD_NODE || type === PipelineCopyResourceType.DEPLOY_NODE
                const isDeployEnv = type === PipelineCopyResourceType.DEPLOY_ENV
                const isDeployNode = type === PipelineCopyResourceType.DEPLOY_NODE

                const transferRiskPathMap = {
                    [PipelineCopyResourceType.BUILD_ENV]: 'buildEnvTransferRiskWarning',
                    [PipelineCopyResourceType.DEPLOY_ENV]: 'deployEnvTransferRiskWarning',
                    [PipelineCopyResourceType.BUILD_NODE]: 'buildNodeTransferRiskWarning',
                    [PipelineCopyResourceType.DEPLOY_NODE]: 'deployNodeTransferRiskWarning',
                    [PipelineCopyResourceType.CREDENTIAL]: 'credentialTransferRiskWarning'
                }

                const operationImpactDescKeyMap = {
                    [PipelineCopyResourceType.BUILD_ENV]: 'operationImpactDesc',
                    [PipelineCopyResourceType.DEPLOY_ENV]: 'deployEnvOperationImpactDesc',
                    [PipelineCopyResourceType.BUILD_NODE]: 'buildNodeOperationImpactDesc',
                    [PipelineCopyResourceType.DEPLOY_NODE]: 'deployNodeOperationImpactDesc',
                    [PipelineCopyResourceType.CREDENTIAL]: 'credentialOperationImpactDesc'
                }

                return {
                    isTicket,
                    isNode,
                    isDeployEnv,
                    isDeployNode,
                    showSourceProjectRisk: !isTicket,
                    showConfirmPrerequisite: !isTicket,
                    showSubsequentProcessing: !isTicket,
                    showAuditReminder: isTicket,
                    transferRiskPath: transferRiskPathMap[type] || '',
                    operationImpactDescKey: operationImpactDescKeyMap[type] || 'operationImpactDesc',
                    isNodeSourceRisk: isNode
                }
            }
        },
        watch: {
            analyzingPipeline:{
                async  handler (newVal, oldVal) {
                    if (oldVal !== false && newVal === false) {
                        await this.loadResourceDependencies()
                    }
                },
                immediate: true
            },
            // 监听可见资源分类变化，设置默认选中第一个
            visibleResourceCategories: {
                handler (newVal) {
                    // 如果当前没有选中的资源类型，或者当前选中的资源类型不在可见列表中
                    const allVisibleTypes = newVal.flatMap(category => category.items.map(item => item.type))
                    
                    // 如果路由中有 sourceType 参数，优先使用它
                    const routeSourceType = this.$route.query.sourceType
                    if (routeSourceType && allVisibleTypes.includes(routeSourceType)) {
                        this.activeResourceType = routeSourceType
                        return
                    }
                    
                    // 否则按原有逻辑设置默认选中第一个
                    if (!this.activeResourceType || !allVisibleTypes.includes(this.activeResourceType)) {
                        // 设置为第一个可见资源类型
                        if (newVal.length > 0 && newVal[0].items.length > 0) {
                            const firstType = newVal[0].items[0].type
                            this.activeResourceType = firstType
                            // 同步更新路由参数
                            this.$router.replace({
                                query: {
                                    ...this.$route.query,
                                    sourceType: firstType
                                }
                            })
                        }
                    }
                },
                immediate: true
            },
            // 监听待处理资源数量变化
            pendingDependencyResourceCount: {
                handler (newVal) {
                    this.$emit('update-validation-data', {
                        pendingDependencyResourceCount: newVal,
                        highRiskOperationCount: this.highRiskOperationCount,
                        pendingConflictCount: this.pendingConflictCount
                    })
                },
                immediate: true
            },
            highRiskOperationCount: {
                handler (newVal) {
                    this.$emit('update-validation-data', {
                        pendingDependencyResourceCount: this.pendingDependencyResourceCount,
                        highRiskOperationCount: newVal,
                        pendingConflictCount: this.pendingConflictCount
                    })
                },
                immediate: true
            },
            pendingConflictCount: {
                handler (newVal) {
                    this.$emit('update-validation-data', {
                        pendingDependencyResourceCount: this.pendingDependencyResourceCount,
                        highRiskOperationCount: this.highRiskOperationCount,
                        pendingConflictCount: newVal
                    })
                },
                immediate: true
            },
            resourceData: {
                handler () {
                    this.syncResourceDataToParent()
                },
                deep: true
            }
        },
        created () {
            this.PipelineIdStrategy = PipelineIdStrategy
            this.PipelineCopyResourceType = PipelineCopyResourceType
        },
        methods: {
            ...mapActions('crossProjectCopy', [
                'listResourceDetails',
                'getCredentialList',
                'setResourceStrategy'
            ]),
            ...mapActions('common', [
                'isPACOAuth'
            ]),
            /**
             * 获取全部资源依赖数据
             */
            async loadResourceDependencies () {
                this.isLoadingData = true
                try {
                    const data = await this.listResourceDetails({
                        projectId: this.projectId,
                        taskId: this.taskId,
                        params: {}
                    })
                    this.resourceData = data
                    this.syncResourceCategoriesSummary()

                    // 仅当资源依赖中包含 CREDENTIAL 类型时才拉取凭据列表
                    const hasCredential = (data || []).some(item => item.resourceType === PipelineCopyResourceType.CREDENTIAL)
                    if (hasCredential) {
                        await this.loadCredentialList()
                    }

                    // 针对代码库资源，调用 isPACOAuth 检查授权状态
                    const repositoryData = data?.find(item => item.resourceType === PipelineCopyResourceType.REPOSITORY)
                    if (repositoryData?.resources?.length > 0) {
                        const uniqueRepoTypes = [...new Set(repositoryData.resources.map(item => item.resourceProperties?.scmCode).filter(Boolean))]
                        for (const repoType of uniqueRepoTypes) {
                            await this.checkRepositoryOAuth(repoType)
                        }
                    }
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message || error
                    })
                } finally {
                    this.isLoadingData = false
                }
            },
            /**
             * 检查代码库的 OAuth 授权状态
             * @param {String} scmCode - 代码库类型
             */
            async checkRepositoryOAuth (scmCode) {
                try {
                    const res = await this.isPACOAuth({
                        projectId: this.projectId,
                        repositoryType: scmCode
                    })
                    
                    if (res?.status === 403) {
                        this.$set(this.oauthUrlMap, scmCode, res.url || '')
                        this.$set(this.oauthStatusMap, scmCode, false)
                    } else if (res?.status === 200) {
                        this.$set(this.oauthStatusMap, scmCode, true)
                        this.$set(this.oauthUrlMap, scmCode, '')
                    }
                } catch (error) {
                    this.$set(this.oauthStatusMap, scmCode, false)
                    this.$set(this.oauthUrlMap, scmCode, '')
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message || error
                    })
                }
            },
            /**
             * 获取指定资源的 OAuth 授权状态
             * @param {Object} item - 资源项
             * @returns {Boolean} 是否已授权
             */
            getOAuthStatus (item) {
                if (this.activeResourceType !== PipelineCopyResourceType.REPOSITORY) return undefined
                const repositoryType = item.resourceProperties?.scmCode

                return repositoryType ? (this.oauthStatusMap[repositoryType] || false) : false
            },
            /**
             * 获取指定资源的 OAuth 授权 URL
             * @param {Object} item - 资源项
             * @returns {String} OAuth 授权 URL
             */
            getOAuthUrl (item) {
                if (this.activeResourceType !== PipelineCopyResourceType.REPOSITORY) return undefined
                const repositoryType = item.resourceProperties?.scmCode
                return repositoryType ? (this.oauthUrlMap[repositoryType] || '') : ''
            },
            /**
             * 刷新指定资源的 OAuth 授权状态
             * @param {Object} item - 资源项
             */
            async handleRefreshOAuth (item) {
                const repositoryType = item.resourceProperties?.scmCode
                if (!repositoryType) return
                
                // 重新检查该 repositoryType 的 OAuth 状态
                await this.checkRepositoryOAuth(repositoryType)
            },
            /**
             * 加载目标项目有使用权限的凭据列表
             */
            async loadCredentialList () {
                const targetProjectId = this.configScopeData?.targetProjectId
                if (!targetProjectId) return
                try {
                    const data = await this.getCredentialList({ projectId: targetProjectId })
                    this.targetCredentialOptions = data.records
                } catch (error) {
                    this.targetCredentialOptions = []
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message || error
                    })
                }
            },
            /**
             * 把 resourceDataMap 中的 totalCount/unprocessedCount 回填到 resourceCategories 各项
             */
            syncResourceCategoriesSummary () {
                // 从 resources 数组重新计算 unprocessedCount
                this.resourceData.forEach(data => {
                    data.unprocessedCount = (data.resources || []).filter(r => r.status === 'UNPROCESSED').length
                })
                this.resourceCategories.forEach(category => {
                    category.items.forEach(item => {
                        const summary = this.resourceDataMap[item.type]
                        if (summary) {
                            item.totalCount = summary.totalCount || 0
                            item.unprocessedCount = summary.unprocessedCount || 0
                        } else {
                            item.totalCount = 0
                            item.unprocessedCount = 0
                        }
                    })
                })
            },
            /**
             * 同步所有资源数据到父组件
             */
            syncResourceDataToParent () {
                this.$emit('update-form-data', 'resourceData', this.resourceData)
            },
            /**
             * 单策略资源类型的策略变化处理（PIPELINE_LABEL / PIPELINE_GROUP）
             * 流水线标签和分组选择一个策略后，其下所有资源都使用同一策略，需要将所有子资源的 status 更新为 PROCESSED
             */
            handleSingleStrategyChange (value) {
                const item = this.singleStrategyItem
                this.$set(item, 'copyStrategy', value)
                
                if (Array.isArray(item.resources)) {
                    item.resources.forEach(resource => {
                        resource.status = value ? 'PROCESSED' : 'UNPROCESSED'
                        resource.confirmed = value ? true : false
                    })
                }
                
                this.syncResourceCategoriesSummary()
            },
            /**
             * 统一的策略变化处理（由子组件触发）
             */
            handleComponentStrategyChange (index, value) {
                const type = this.activeResourceType
                const item = this.currentResourceList[index]

                const isHighRisk = HIGH_RISK_STRATEGIES[type] === value && !item.hasShownRiskWarning
                
                if (isHighRisk) {
                    this.riskWarningDialog.visible = true
                    this.riskWarningDialog.currentItem = item
                    this.riskWarningDialog.resourceName = item.resourceName
                } else {
                    this.$set(item, 'copyStrategy', value)

                    // 特殊处理：凭据需要选择目标凭据
                    if (type === PipelineCopyResourceType.CREDENTIAL && value === PipelineCopyStrategy.CREDENTIAL_REPLACE_TARGET) {
                        item.status = 'UNPROCESSED'
                    } else if (type === PipelineCopyResourceType.REPOSITORY) {
                        // 代码库类型：检查 OAuth 授权状态
                        const isOAuth = this.getOAuthStatus(item)
                        item.status = isOAuth ? 'PROCESSED' : 'UNPROCESSED'
                    } else {
                        item.status = value ? 'PROCESSED' : 'UNPROCESSED'
                    }
                    this.syncResourceCategoriesSummary()
                }
            },
            handleRecheck () {
                this.isShowRecheckDialog = true
            },
            handleStartCheck () {
                this.isShowRecheckDialog = false
                console.log('开始重新检查')
                this.rechecking = true
                // 通知父组件更新重新检查状态
                this.$emit('update-loading-state', true)
                // TODO: 调用重新检查的 API
            },
            handleCancelRecheck () {
                this.isShowRecheckDialog = false
            },
            async handleSetStrategy () {
                this.setupLoading = true
                try {
                    const data = await this.setResourceStrategy({
                        projectId: this.$route.params.projectId,
                        taskId: this.$route.params.taskId
                    })
                    
                    this.showStrategyCompletedTag = true
                    
                    const { processedCount = 0, nodeNotSetCount = 0, pipelineConflictCount = 0 } = data || {}
                    this.strategyCompletedData = {
                        processedCount,
                        nodeNotSetCount,
                        pipelineConflictCount
                    }
                    
                    // 调用资源详情接口刷新数据
                    await this.loadResourceDependencies()
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message || error
                    })
                } finally {
                    this.setupLoading = false
                }
            },
            handleCloseStrategyTag () {
                this.showStrategyCompletedTag = false
            },
            handleSelectResourceType (type) {
                this.activeResourceType = type
                this.activeTab = 'all'
                this.$router.replace({
                    query: {
                        ...this.$route.query,
                        sourceType: type
                    }
                })
            },
            handleCredentialSelectChange (selectedCredentialId, index) {
                const item = this.currentResourceList[index]
                item.targetResourceId = selectedCredentialId || ''
                item.status = selectedCredentialId ? 'PROCESSED' : 'UNPROCESSED'
                this.syncResourceCategoriesSummary()
            },
            handleRiskConfirm () {
                const item = this.riskWarningDialog.currentItem
                
                if (item) {
                    this.$set(item, 'copyStrategy', HIGH_RISK_STRATEGIES[this.activeResourceType])
                    item.status = 'PROCESSED'
                    item.hasShownRiskWarning = true
                }
                
                this.riskWarningDialog.visible = false
                this.syncResourceCategoriesSummary()
            },
            handleRiskCancel () {
                this.riskWarningDialog.visible = false
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/scss/resource-dependency';
    .resource-dependency {
        display: flex;
        flex-direction: column;
        height: 100%;
        overflow: hidden;
        
            background: white;
            padding: 16px 24px 0;

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

        .resource-dependency__loading {
            display: flex;
            flex-direction: column;
            height: 100%;
            overflow: hidden;
            ::v-deep .bk-loading-body{
                overflow: hidden;
                display: flex;
                flex: 1;
                flex-direction: column;
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
                height: 36px;
                width: 112px;
                padding: 5px 16px;
                justify-content: center;
                text-align: center;
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

        .strategy-completed-tag {
            margin: 0;
            display: flex;
            justify-content: space-between;
            padding: 8px 12px;
            height: 60px;
            margin-bottom: 8px;
            color: #4D4F56;
            .tag-content {
                display: flex;

                svg {
                    vertical-align: middle;
                    margin-top: 4px;
                    color: #2CAF5E;
                    margin-right: 8px;
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
                height: 100%;
                overflow: auto;
                padding: 16px 0 10px 16px;

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

                    .tabs-left {
                        margin-right: 20%;
                    }

                    .tabs-right {
                        display: flex;
                        justify-content: flex-end;
                        flex: 1;
                    }
                }

                .detail-content {
                    flex: 1;
                    overflow-y: auto;
                    padding-right: 16px;
                    position: relative;

                    .detail-empty {
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        height: 200px;
                        color: #979BA5;
                        font-size: 14px;
                    }

                    .readonly-mask {
                        position: absolute;
                        top: 0;
                        left: 0;
                        right: 0;
                        bottom: 0;
                        background: transparent;
                        z-index: 10;
                        cursor: not-allowed;
                    }
                }
            }
        }

    }

    .source-empty {
        height: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
    }

    // 确认重新检查弹窗样式
    .recheck-confirm-dialog {
        text-align: center;
        padding: 8px 0;

        .info-icon {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            background-color: #FCE5C0;
            color: #F59500;
            width: 42px;
            height: 42px;
            font-size: 24px;
            border-radius: 50%;
            flex-shrink: 0;
        }

        .recheck-title {
            color: #313238;
            font-size: 20px;
            margin: 16px 0;
        }

        .recheck-content {
            color: #4D4F56;
            font-size: 14px;
            padding: 12px 16px;
            background: #F5F7FA;
            border-radius: 2px;
            text-align: left;
            margin: 0 8px;
    
            .recheck-tips {
                text-align: left;
                margin: 0;
                padding: 12px 16px 12px 20px;
    
                li {
                    line-height: 20px;
                    margin-bottom: 4px;
                    list-style-type: disc;

                    &:last-child {
                        margin-bottom: 0;
                    }

                    .highlight {
                        color: #E38B02;
                    }
                }

                li::marker {
                    font-size: 18px;
                }
            }
        }

        ::v-deep .bk-dialog-footer {
            border: none;
            background-color: #fff;
            padding-bottom: 24px;
        }
    }
</style>
