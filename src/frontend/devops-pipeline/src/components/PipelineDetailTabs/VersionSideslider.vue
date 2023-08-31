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
                    <i v-else class="devops-icon icon-check-circle" />
                    <span>
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
                            <i class="devops-icon icon-check-circle" />
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
    import { mapState, mapMutations, mapActions } from 'vuex'
    import { convertTime } from '@/utils/util'
    import VersionHistorySideSlider from './VersionHistorySideSlider'
    export default {
        name: 'versionSideslider',
        emit: ['input', 'update:value', 'change'],
        components: {
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
            ...mapState('pipelines', [
                'pipelineInfo'
            ]),
            ...mapState('atom', [
                'pipelineYaml'
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
            }
        },
        watch: {
            value (val) {
                console.log(212, val, this.versionList)
                this.activeVersion = this.versionList.find(item => item.version === val)
            }
        },
        created () {
            this.handlePipelineVersionList()
        },
        methods: {
            convertTime,
            ...mapMutations('atom', ['SET_PIPELINE_EDITING']),
            ...mapMutations('pipelines', ['PIPELINE_SETTING_MUTATION']),
            ...mapActions('pipelines', [
                'requestPipelineVersionList'
            ]),
            showVersionSideSlider () {
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
                        return {
                            ...item,
                            displayName: `V${item.version} (${item.versionName})`,
                            isDraft: item.status === 'COMMITTING',
                            isRelease: item.status === 'RELEASED'
                        }
                    })
                    this.activeVersion = this.versionList.find(item => item.version === this.pipelineInfo?.version)
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
                this.activeVersion = version
                this.$emit('change', version.version, version)
                this.$emit('input', version.version, version)
                this.$emit('update:value', version.version, version)
            },
            isCurrentVersion (version) {
                return version?.version === this.pipelineInfo?.version
            },
            searchVersion () {
                console.log(this.searchKeyword, 212)
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
        }
        font-size: 16px;
    }
    > span {
        @include ellipsis();
    }
    cursor: pointer;
}
.pipeline-version-dropmenu-content {
    width: 360px;
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
