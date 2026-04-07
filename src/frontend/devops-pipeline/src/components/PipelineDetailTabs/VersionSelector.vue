<template>
    <bk-select
        ref="versionSelector"
        searchable
        :value="value"
        :disabled="isLoading || !editable"
        :loading="isLoading"
        :clearable="false"
        :popover-width="320"
        :ext-cls="extCls"
        :ext-popover-cls="extPopoverCls"
        enable-scroll-load
        :scroll-loading="bottomLoadingOptions"
        @scroll-end="loadMore()"
        :remote-method="searchVersion"
        @change="switchVersion"
        @toggle="selectorToggle"
    >
        <div
            slot="trigger"
            class="pipeline-version-dropmenu-trigger"
        >
            <i
                v-if="isActiveDraft"
                class="devops-icon icon-edit-line"
            />
            <logo
                v-else-if="isActiveBranchVersion"
                class="pipeline-branch-version-icon"
                name="branch"
                size="14"
            />
            <i
                v-else
                :class="['devops-icon icon-check-circle', {
                    'is-release-version-icon': isCurrentVersion(activeVersion)
                }]"
            />
            <p class="pipeline-version-name">
                <span v-bk-overflow-tips>
                    <template v-if="isActiveDraft">
                        <span v-if="draftVersion">{{ convertTime(activeVersion.updateTime) + ` ${activeVersion.updater} ` }}</span>
                        {{ $t('editPage.draftVersion', [draftBaseVersionName]) }}
                    </template>
                    <template v-else>
                        {{ activeDisplayName }}
                    </template>
                </span>
                <i
                    v-if="activeVersion?.latestReleasedFlag"
                    class="pipeline-release-version-tag"
                >
                    {{ $t('latest') }}
                </i>
                <i
                    v-else-if="isActiveDraft && showDraftTag"
                    class="pipeline-draft-version-tag"
                >
                    {{ $t('willRelease') }}
                </i>
            </p>
            <i
                v-if="isLoading"
                class="devops-icon icon-circle-2-1 spin-icon"
            />
            <i
                v-else
                class="bk-icon icon-angle-down"
            />
        </div>
        <bk-option
            v-for="item in versionList"
            :key="item.version"
            :id="item.version"
            :name="item.displayName"
            :class="{
                'pipeline-version-option-item': true,
                'pipeline-version-active': item.version === selectedVersionId
            }"
        >
            <div class="pipeline-version-option-item-name">
                <i
                    v-if="item.isDraft"
                    class="devops-icon icon-edit-line"
                />
                <logo
                    v-else-if="item.isBranchVersion"
                    class="pipeline-branch-version-icon"
                    name="branch"
                    size="14"
                />
                <i
                    v-else
                    :class="['devops-icon icon-check-circle', {
                        'is-release-version-icon': isCurrentVersion(item)
                    }]"
                />
                <p class="pipeline-version-name">
                    <span>
                        <span v-bk-overflow-tips>
                            {{ item.displayName }}
                        </span>
                        <i
                            v-if="isCurrentVersion(item)"
                            class="pipeline-release-version-tag"
                        >
                            {{ $t('latest') }}
                        </i>
                    </span>
                    <span
                        v-bk-overflow-tips
                        class="src-template-version-name"
                        v-if="item.srcTemplateVersionName"
                    >
                        [
                        <logo
                            class="main-branch-icon"
                            size="14"
                            name="main-branch"
                        /> {{ item.srcTemplateVersionName }}
                        ]
                    </span>
                </p>
                <!-- <span class="pipeline-version-main-branch">
                                [{{ $t('mainBranch') }}]
                            </span> -->
            </div>
            <span
                class="pipeline-version-option-item-desc"
                v-bk-overflow-tips
            >
                {{ item.description || '--' }}
            </span>
        </bk-option>
        <p
            v-if="showExtension"
            slot="extension"
            class="show-all-pipeline-version-entry"
            @click="showAll"
        >
            <logo
                name="tiaozhuan"
                :size="16"
            />
            {{ $t('viewAll') }}
        </p>
    </bk-select>
</template>
<script>
    import Logo from '@/components/Logo'
    import { bus, SHOW_VERSION_HISTORY_SIDESLIDER } from '@/utils/bus'
    import { VERSION_STATUS_ENUM } from '@/utils/pipelineConst'
    import { convertTime } from '@/utils/util'
    import { mapActions, mapState } from 'vuex'
    export default {
        name: 'VersionSelector',
        emit: ['input', 'change', 'showAllVersion'],
        components: {
            Logo
        },
        props: {
            extCls: {
                type: String,
                default: 'pipeline-version-selector'
            },
            extPopoverCls: {
                type: String,
                default: ''
            },
            editable: {
                type: Boolean,
                default: true
            },
            showExtension: {
                type: Boolean,
                default: true
            },
            value: {
                type: [String, Number],
                default: ''
            },
            refreshListOnExpand: {
                type: Boolean,
                default: false
            },
            showDraftTag: {
                type: Boolean,
                default: false
            },
            includeDraft: {
                type: Boolean,
                default: true
            },
            buildOnly: {
                type: Boolean,
                default: false
            },
            isTemplate: {
                type: Boolean,
                default: false
            },
            draftVersion: {
                type: Number,
                default: undefined
            },
            // 是否需要同时加载草稿版本列表（用于编辑页版本对比）
            needDraftList: {
                type: Boolean,
                default: false
            },
            uniqueId: {
                type: [String, Number],
                required: true
            }
        },
        data () {
            return {
                isLoading: false,
                activeVersion: null,
                bottomLoadingOptions: {
                    size: 'small',
                    isLoading: false
                },
                showVersionSideslider: false,
                versionList: [],
                searchKeyword: '',
                hasNext: true,
                pagination: {
                    current: 1,
                    count: 0,
                    limit: 15
                }
            }
        },
        computed: {
            ...mapState('atom', [
                'pipelineInfo'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            // 最新的流水线版本信息
            activeDisplayName () {
                return this.activeVersion?.displayName ?? '--'
            },
            selectedVersionId () {
                return this.activeVersion?.version ?? null
            },
            isActiveDraft () {
                return this.activeVersion?.isDraft ?? false
            },
            isActiveBranchVersion () {
                return this.activeVersion?.isBranchVersion ?? false
            },
            draftBaseVersionName () {
                return this.activeVersion?.baseVersionName ?? '--'
            }
        },
        watch: {
            value (val) {
                const activeVersion = this.versionList.find(item => item.version === val)
                if (activeVersion) {
                    this.activeVersion = activeVersion
                }
            },
            uniqueId: {
                handler () {
                    this.hasNext = true
                    this.loadMore(1)
                },
                immediate: true
            }
        },
        mounted () {
            bus.$on(SHOW_VERSION_HISTORY_SIDESLIDER, this.showVersionSideSlider)
        },
        beforeDestroy () {
            bus.$off(SHOW_VERSION_HISTORY_SIDESLIDER, this.showVersionSideSlider)
        },
        methods: {
            convertTime,
            ...mapActions({
                requestPipelineVersionList: 'pipelines/requestPipelineVersionList',
                requestTemplateVersionList: 'templates/requestTemplateVersionList',
                getDraftVersion: 'common/getDraftVersion',
                getTemplateDraftVersion: 'common/getTemplateDraftVersion',
                requestPipelineSummary: 'atom/requestPipelineSummary'
            }),

            // 将草稿版本数据转换为统一格式
            transformDraftVersion (item) {
                return {
                    ...item,
                    version: `draft-${item.draftVersion}`,
                    displayName: convertTime(item.updateTime) + `  ${item.updater}`,
                    description: this.$t('baseOn', [item.baseVersionName]),
                    isBranchVersion: false,
                    isDraft: true,
                    isRelease: false,
                    isDraftVersion: true,
                    initialVersion: item.version
                }
            },
            
            // 将正式版本数据转换为统一格式
            transformReleaseVersion (item) {
                const isDraft = item.status === VERSION_STATUS_ENUM.COMMITTING
                const isBranchVersion = item.status === VERSION_STATUS_ENUM.BRANCH
                const isRelease = item.status === VERSION_STATUS_ENUM.RELEASED
                
                return {
                    ...item,
                    displayName: isDraft ? this.$t('draft') : item.versionName,
                    description: isDraft ? this.$t('baseOn', [item.baseVersionName]) : (item.description || '--'),
                    isBranchVersion,
                    isDraft,
                    isRelease
                }
            },
            
            // 更新分页信息
            updatePagination (response) {
                const { page, count } = response
                const { limit } = this.pagination
                
                this.pagination.page = page
                this.hasNext = count > page * limit
            },
            
            // 加载草稿版本和正式版本（第一页）
            async loadDraftAndReleaseVersions (params) {
                const draftDataSource = this.isTemplate ? this.getTemplateDraftVersion : this.getDraftVersion
                const releaseDataSource = this.isTemplate ? this.requestTemplateVersionList : this.requestPipelineVersionList
                
                const [draftRes, releaseRes] = await Promise.all([
                    draftDataSource(params),
                    releaseDataSource(params)
                ])
                
                // 转换草稿版本数据
                const draftVersions = draftRes.records.map(item => this.transformDraftVersion(item))
                
                // 查找第一个正式发布版本
                const firstReleasedVersion = releaseRes.records.find(
                    item => item.status === VERSION_STATUS_ENUM.RELEASED
                )
                
                // 合并数据：第一个正式发布版本 + 草稿版本列表
                let versions = draftVersions
                if (firstReleasedVersion) {
                    const releasedVersionData = this.transformReleaseVersion(firstReleasedVersion)
                    versions = [releasedVersionData, ...draftVersions]
                }
                
                this.updatePagination(draftRes)
                return versions
            },
            
            // 只加载草稿版本（后续分页）
            async loadDraftVersions (params) {
                const draftDataSource = this.isTemplate ? this.getTemplateDraftVersion : this.getDraftVersion
                const draftRes = await draftDataSource(params)
                
                const versions = draftRes.records.map(item => this.transformDraftVersion(item))
                
                this.updatePagination(draftRes)
                return versions
            },
            
            // 只加载正式版本
            async loadReleaseVersions (params) {
                const dataSource = this.isTemplate ? this.requestTemplateVersionList : this.requestPipelineVersionList
                
                const extendedParams = {
                    ...params,
                    fuzzyVersionName: this.searchKeyword,
                    includeDraft: this.includeDraft,
                    buildOnly: this.buildOnly,
                    archiveFlag: this.$route.query.archiveFlag
                }
                
                const res = await dataSource(extendedParams)
                const versions = res.records.map(item => this.transformReleaseVersion(item))
                
                this.updatePagination(res)
                return versions
            },
            
            async loadMore (page) {
                try {
                    const { projectId, pagination, pipelineInfo } = this
                    const nextPage = page ?? pagination.page + 1
                    if (nextPage > 1 && !this.hasNext) return
                    if (nextPage === 1) {
                        this.isLoading = true
                    } else {
                        this.bottomLoadingOptions.isLoading = true
                    }
                    
                    // needDraftList 为 true 时，无论是否传入 draftVersion 都会同时加载草稿和正式版本列表
                    const shouldLoadDraftList = this.needDraftList || !!this.draftVersion
                    const idKey = this.isTemplate ? 'templateId' : 'pipelineId'
                    
                    const baseParams = {
                        projectId,
                        [idKey]: this.uniqueId,
                        page: nextPage,
                        pageSize: pagination.limit
                    }
                    
                    let versions = []
                    
                    if (shouldLoadDraftList) {
                        const params = {
                            ...baseParams,
                            version: pipelineInfo?.version
                        }
                        
                        // 第一页同时加载草稿和正式版本，后续分页只加载草稿版本
                        versions = nextPage === 1
                            ? await this.loadDraftAndReleaseVersions(params)
                            : await this.loadDraftVersions(params)
                    } else {
                        // 只加载正式版本
                        versions = await this.loadReleaseVersions(baseParams)
                    }
                    
                    // 更新版本列表
                    if (page === 1) {
                        this.versionList = versions
                        const releaseVersion = versions.find(item => item.status === VERSION_STATUS_ENUM.RELEASED)
                        if (releaseVersion?.version > pipelineInfo.releaseVersion) {
                            await this.requestPipelineSummary(this.$route.params)
                            this.switchVersion(this.activeVersion.version)
                        }

                    } else {
                        this.versionList.push(...versions)
                    }
                    
                    // 设置初始选中版本
                    if (!this.activeVersion) {
                        if (this.draftVersion && this.value === this.draftVersion) {
                            const version = this.versionList.find(item => item.draftVersion === this.draftVersion)
                            if (version) {
                                this.switchVersion(version.version)
                            }
                        } else {
                            this.switchVersion(this.value)
                        }
                    }
                } catch (error) {
                    console.log(error)
                } finally {
                    this.bottomLoadingOptions.isLoading = false
                    this.isLoading = false
                }
            },
            switchVersion (versionId) {
                const version = this.versionList.find(item => item.version === versionId)
                if (version) {
                    this.activeVersion = version
                    this.$emit('change', versionId, version)
                    this.$emit('input', versionId, version)
                }
            },
            selectorToggle (status) {
                if (this.refreshListOnExpand && status) { // 展开时更新列表
                    this.hasNext = true
                    this.loadMore(1)
                }
            },
            isCurrentVersion (version) {
                return version?.version === this.pipelineInfo?.releaseVersion && version?.status === VERSION_STATUS_ENUM.RELEASED
            },
            searchVersion (keyword) {
                this.searchKeyword = keyword
                this.$nextTick(() => {
                    this.hasNext = true
                    this.loadMore(1)
                })
            },
            focusSearchInput () {
                this.$nextTick(() => {
                    this.$refs.versionSearchInput?.focus?.()
                })
            },
            close () {
                this.$refs.versionSelector?.close?.()
            },
            showAll () {
                this.$emit('showAllVersion')
            }
        }
    }
</script>
<style lang="scss" scoped>
@import "@/scss/conf";
@import "@/scss/mixins/ellipsis";
.pipeline-version-selector {
    border: 0;
    box-shadow: none;
    &.is-focus {
        .icon-angle-down {
            transform: rotate(180deg);
        }
    }
}
.pipeline-version-dropmenu-trigger {
    display: grid;
    align-items: center;
    grid-template-columns: auto 1fr auto;
    grid-gap: 6px;
    height: 24px;
    line-height: 24px;
    background: #F0F1F5;
    padding: 0 8px;
    min-width: 200px;
    width: 100%;
    cursor: pointer;
    overflow: hidden;
    .icon-edit-line,
    .icon-check-circle {
        color: #979BA5;
        font-size: 14px;
        &.icon-check-circle.is-release-version-icon {
            color: #3FC362;
        }
    }
    .pipeline-branch-version-icon {
        color: #FF9C01;
    }
    .pipeline-version-name {
        display: flex;
        grid-gap: 8px;
        overflow: hidden;
        > span {
            line-height: 24px;
            @include ellipsis();
        }
    }
    .icon-circle-2-1 {
        width: 20px;
        height: 20px;
        display: flex;
        align-items: center;
        justify-content: center;
    }
    .icon-angle-down {
        transition: transform 0.3s;
        font-size: 20px;
    }
}
.pipeline-release-version-tag,
.pipeline-draft-version-tag {
    display: inline-flex;
    align-items: center;
    background: #E4FAF0 ;
    border: 1px solid #A5E0C6;
    color: #14A568;
    padding: 0 4px;
    border-radius: 2px;
    font-size: 10px;
    height: 16px;
    line-height: 16px;
    align-self: center;
    font-style: normal;
    flex-shrink: 0;
}
.pipeline-draft-version-tag {
    background: #3A2F18;
    border: 1px solid #523F1E;
    color: #FC943B;
}

.pipeline-version-option-item {
    display: flex;
    flex-direction: column;
    line-height: 20px;
    grid-gap: 2px;
    padding: 8px 0;
    font-size: 12px;
    cursor: pointer;
    &:hover,
    &.pipeline-version-active {
        background: #E1ECFF;
        color: $primaryColor;
    }

    .pipeline-version-option-item-name {
        display: grid;
        grid-auto-flow: column;
        grid-template-columns: 18px 1fr auto;
        align-items: center;
        grid-gap: 4px;
        .devops-icon {
            font-size: 14px;
            color: #979BA5;
            flex-shrink: 0;
            &.icon-check-circle.is-release-version-icon {
                color: #3FC362;
            }
        }
        .pipeline-branch-version-icon {
            color: #FF9C01;
        }
        .pipeline-version-main-branch {
            color: #3FC362;
        }

        .pipeline-version-name {
            display: flex;
            justify-content: space-between;
            grid-gap: 8px;
            overflow: hidden;
            > span {
                font-weight: 700;
                @include ellipsis();
            }
            
            .src-template-version-name {
                display: flex;
                align-items: center;
                color: #979BA5;
                font-weight: 400;
                max-width: 100px;
            }
            .main-branch-icon {
                margin-right: 4px;
            }
        }
    }
    .pipeline-version-option-item-desc {
        color: #979BA5;
        width: 100%;
        @include ellipsis();
    }
}
.show-all-pipeline-version-entry {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 40px;
    grid-gap: 4px;
    cursor: pointer;
}

</style>
