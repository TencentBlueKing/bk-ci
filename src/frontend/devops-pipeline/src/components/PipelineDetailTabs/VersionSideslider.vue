<template>
    <div class="version-sideslider-container">
        <bk-dropdown-menu trigger="click" @show="focusSearchInput">
            <div
                class="pipeline-version-dropmenu-trigger"
                slot="dropdown-trigger"
            >
                <bk-spin v-if="isLoading" size="mini"></bk-spin>
                <template v-else>
                    <i v-if="isActiveDraft" class="devops-icon icon-draft" />
                    <logo v-else-if="isActiveBranchVersion" name="branch" size="14" />
                    <i v-else class="devops-icon icon-check-circle" />
                    <span v-if="isActiveDraft">{{ $t('editPage.draftVersion', [draftBaseVersionName]) }}</span>
                    <span v-else>
                        {{ activeVersionName }}
                    </span>
                    <i class="devops-icon icon-shift" />
                </template>
            </div>
            <ul v-bkloading="{ isLoading }" class="pipeline-version-dropmenu-content" slot="dropdown-content">
                <p @click.stop="">
                    <bk-input
                        v-model.trim="searchKeyword"
                        ref="versionSearchInput"
                        behavior="simplicity"
                        class="pipeline-version-search"
                        clearable
                        @clear="searchVersion"
                        @enter="searchVersion"
                    />
                </p>
                <template v-if="versionList.length > 0">
                    <li
                        v-for="item in versionList"
                        @click="switchVersion(item)"
                        :key="item.version"
                        :class="{
                            'pipeline-current-version': isCurrentVersion(item),
                            'pipeline-version-active': item.version === selectedVersionId
                        }"
                    >
                        <p>
                            <i v-if="item.isDraft" class="devops-icon icon-draft" style="font-size: 14px" />
                            <logo v-else-if="item.isBranchVersion" name="branch" size="14" />
                            <i v-else class="devops-icon icon-check-circle" />
                            <span class="pipeline-version-name">
                                {{ item.displayName }}
                            </span>
                            <!-- <span class="pipeline-version-main-branch">
                                [{{ $t('mainBranch') }}]
                            </span> -->
                        </p>
                        <span>
                            {{ item.description || '--' }}
                        </span>
                    </li>
                </template>
                <div v-else class="pipeline-version-empty-indicator">
                    <bk-exception
                        scene="part"
                        :type="searchKeyword ? 'search-empty' : 'empty'"
                    />
                </div>
                <li class="show-all-pipeline-version-entry" @click="showVersionSideSlider">
                    {{ $t('viewAll') }}
                </li>
            </ul>
        </bk-dropdown-menu>
        <VersionHistorySideSlider
            :show-version-sideslider="showVersionSideslider"
            :current-yaml="pipelineYaml"
            @close="closeVersionSideSlider"
        />
    </div>
</template>
<script>
    import { mapState, mapActions } from 'vuex'
    import { convertTime, generateDisplayName } from '@/utils/util'
    import VersionHistorySideSlider from './VersionHistorySideSlider'
    import Logo from '@/components/Logo'
    export default {
        name: 'versionSideslider',
        emit: ['input', 'update:value', 'change'],
        components: {
            Logo,
            VersionHistorySideSlider
        },
        props: {
            editable: {
                type: Boolean,
                default: true
            },
            value: {
                type: [String, Number],
                default: ''
            }
        },
        data () {
            return {
                isLoading: false,
                showVersionSideslider: false,
                versionList: [],
                searchKeyword: '',
                activeVersion: null,
                pagination: {
                    current: 1,
                    count: 0,
                    limit: 15
                }
            }
        },
        computed: {
            ...mapState('atom', [
                'pipelineYaml',
                'pipelineInfo'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            },
            // 最新的流水线版本信息
            activeVersionName () {
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
                return generateDisplayName(this.activeVersion?.baseVersion, this.activeVersion?.baseVersionName)
            }
        },
        watch: {
            value (val) {
                this.activeVersion = this.versionList.find(item => item.version === val)
            },
            pipelineId () {
                this.handlePipelineVersionList()
            }
        },
        created () {
            this.handlePipelineVersionList()
        },
        methods: {
            convertTime,
            ...mapActions('pipelines', [
                'requestPipelineVersionList'
            ]),
            ...mapActions('atom', [
                'setShowVariable'
            ]),
            showVersionSideSlider () {
                this.setShowVariable(false)
                this.showVersionSideslider = true
            },
            handlePaginationChange ({ current = 1, limit = this.pagination.limit } = {}) {
                this.pagination.current = current
                this.pagination.limit = limit
                this.handlePipelineVersionList()
            },
            handlePipelineVersionList () {
                this.isLoading = true
                this.requestPipelineVersionList({
                    projectId: this.projectId,
                    pipelineId: this.pipelineId,
                    page: this.pagination.current,
                    pageSize: this.pagination.limit,
                    versionName: this.searchKeyword
                }).then(({ records, count }) => {
                    this.versionList = records.map(item => {
                        const isDraft = item.status === 'COMMITTING'
                        const displayName = generateDisplayName(item.version, item.versionName)
                        return {
                            ...item,
                            displayName: isDraft ? this.$t('draft') : displayName,
                            description: isDraft ? this.$t('baseOn', [generateDisplayName(item.baseVersion, item.baseVersionName)]) : (item.description || '--'),
                            isBranchVersion: item.status === 'BRANCH',
                            isDraft,
                            isRelease: item.status === 'RELEASED'
                        }
                    })
                    const activeVersion = this.versionList.find(item => item.version === this.value)
                    this.switchVersion(activeVersion)
                }).catch(err => {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                }).finally(() => {
                    this.isLoading = false
                })
            },
            switchVersion (version) {
                if (version) {
                    this.activeVersion = version
                    this.$emit('change', version.version, version)
                    this.$emit('input', version.version, version)
                    this.$emit('update:value', version.version, version)
                }
            },
            isCurrentVersion (version) {
                return version?.version === this.pipelineInfo?.releaseVersion
            },
            searchVersion () {
                this.handlePipelineVersionList()
            },
            focusSearchInput () {
                this.$nextTick(() => {
                    this.$refs.versionSearchInput?.focus?.()
                })
            },
            closeVersionSideSlider () {
                this.showVersionSideslider = false
            }
        }
    }
</script>
<style lang="scss" scoped>
@import "@/scss/conf";
@import "@/scss/mixins/ellipsis";
.pipeline-version-dropmenu-trigger {
    display: grid;
    align-items: center;
    grid-auto-flow: column;
    grid-template-columns: auto 1fr auto;
    grid-gap: 6px;
    height: 24px;
    background: #F0F1F5;
    padding: 0 10px;
    min-width: 200px;
    .icon-draft,
    .icon-check-circle {
        &.icon-check-circle {
            color: $successColor;
            font-size: 18px;
        }
        font-size: 14px;
    }
    > span {
        @include ellipsis();
    }
    cursor: pointer;
}
.pipeline-version-dropmenu-content {
    width: 100%;
    min-width: 360px;
    display: flex;
    flex-direction: column;
    max-height: 360px;
    overflow: auto;
    .pipeline-version-search {
        padding: 0 12px;
    }
    .pipeline-version-empty-indicator {
        display: flex;
        align-items: center;
        justify-content: center;
        height: 360px;
    }
    >.show-all-pipeline-version-entry {
        color: $primaryColor;
        display: flex;
        align-items: center;
        padding: 12px 0;
    }
    > li {
        display: flex;
        flex-direction: column;
        line-height: 20px;
        grid-gap: 2px;
        padding: 8px 16px;
        font-size: 12px;
        cursor: pointer;
        &:hover,
        &.pipeline-version-active {
            background: #E1ECFF;
            color: $primaryColor;
        }
        &.pipeline-current-version {
            .icon-check-circle {
                color: $successColor;
            }
        }

        > p {
            display: grid;
            grid-auto-flow: column;
            grid-template-columns: 18px 1fr auto;
            align-items: center;
            grid-gap: 4px;
            .devops-icon {
                font-size: 18px;
                flex-shrink: 0;
            }
            .pipeline-version-main-branch {
                color: $successColor;
            }
            .pipeline-version-name {
                font-weight: 700;
                @include ellipsis();
            }
        }
        > span {
            color: #979BA5;
            @include ellipsis();
        }
    }
}
</style>
