<template>
    <div class="version-sideslider-container">
        <bk-select
            ref="versionSelector"
            searchable
            :value="value"
            :disabled="isLoading"
            :clearable="false"
            :popover-width="320"
            ext-cls="pipeline-version-selector"
            :remote-method="searchVersion"
            @change="switchVersion"
        >
            <div slot="trigger" class="pipeline-version-dropmenu-trigger">
                <i v-if="isActiveDraft" class="devops-icon icon-edit-line" />
                <logo v-else-if="isActiveBranchVersion" class="pipeline-branch-version-icon" name="branch" size="14" />
                <i v-else :class="['devops-icon icon-check-circle', {
                    'is-release-version-icon': isCurrentVersion(activeVersion)
                }]" />
                <span v-if="isActiveDraft">{{ $t('editPage.draftVersion', [draftBaseVersionName]) }}</span>
                <span v-else>
                    {{ activeDisplayName }}
                </span>
                <i class="bk-icon icon-angle-down" />
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
                <p class="pipeline-version-option-item-name">
                    <i v-if="item.isDraft" class="devops-icon icon-edit-line" />
                    <logo v-else-if="item.isBranchVersion" class="pipeline-branch-version-icon" name="branch" size="14" />
                    <i v-else :class="['devops-icon icon-check-circle', {
                        'is-release-version-icon': isCurrentVersion(item)
                    }]" />
                    <span class="pipeline-version-name" v-bk-overflow-tips>
                        {{ item.displayName }}
                    </span>
                    <!-- <span class="pipeline-version-main-branch">
                                [{{ $t('mainBranch') }}]
                            </span> -->
                </p>
                <span class="pipeline-version-option-item-desc" v-bk-overflow-tips>
                    {{ item.description || '--' }}
                </span>
            </bk-option>
            <p slot="extension" class="show-all-pipeline-version-entry" @click="showVersionSideSlider">
                <logo name="tiaozhuan" :size="16" />
                {{ $t('viewAll') }}
            </p>
        </bk-select>
        <VersionHistorySideSlider
            :show-version-sideslider="showVersionSideslider"
            :current-yaml="pipelineYaml"
            @close="closeVersionSideSlider"
        />
    </div>
</template>
<script>
    import Logo from '@/components/Logo'
    import { convertTime } from '@/utils/util'
    import { bus, SHOW_VERSION_HISTORY_SIDESLIDER } from '@/utils/bus'
    import { mapActions, mapState } from 'vuex'
    import VersionHistorySideSlider from './VersionHistorySideSlider'
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
                this.activeVersion = this.versionList.find(item => item.version === val)
            },
            pipelineId: {
                handler () {
                    this.handlePipelineVersionList()
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
            ...mapActions('pipelines', [
                'requestPipelineVersionList'
            ]),
            ...mapActions('atom', [
                'setShowVariable'
            ]),
            showVersionSideSlider () {
                this.setShowVariable(false)
                this.$refs?.versionSelector?.close?.()
                this.showVersionSideslider = true
            },
            handlePaginationChange ({ current = 1, limit = this.pagination.limit } = {}) {
                this.pagination.current = current
                this.pagination.limit = limit
                this.handlePipelineVersionList()
            },
            async handlePipelineVersionList () {
                try {
                    this.isLoading = true
                    const { records } = await this.requestPipelineVersionList({
                        projectId: this.projectId,
                        pipelineId: this.pipelineId,
                        page: this.pagination.current,
                        pageSize: this.pagination.limit,
                        versionName: this.searchKeyword
                    })
                    this.versionList = records.map(item => {
                        const isDraft = item.status === 'COMMITTING'
                        const isBranchVersion = item.status === 'BRANCH'

                        return {
                            ...item,
                            displayName: isDraft ? this.$t('draft') : item.versionName,
                            description: isDraft ? this.$t('baseOn', [item.baseVersionName]) : (item.description || '--'),
                            isBranchVersion,
                            isDraft,
                            isRelease: item.status === 'RELEASED'
                        }
                    })
                    const version = this.versionList.find(item => item.version === this.value)

                    this.activeVersion = version
                } catch (err) {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                } finally {
                    this.isLoading = false
                }
            },
            switchVersion (versionId) {
                const version = this.versionList.find(item => item.version === versionId)
                this.activeVersion = version
                this.$emit('change', versionId, version)
                this.$emit('input', versionId, version)
                this.$emit('update:value', versionId, version)
            },
            isCurrentVersion (version) {
                return version?.version === this.pipelineInfo?.releaseVersion
            },
            searchVersion (keyword) {
                this.searchKeyword = keyword
                this.$nextTick(() => {
                    this.handlePipelineVersionList()
                })
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
    grid-auto-flow: column;
    grid-template-columns: auto 1fr auto;
    grid-gap: 6px;
    height: 24px;
    background: #F0F1F5;
    padding: 0 8px;
    min-width: 200px;
    .icon-edit-line,
    .icon-check-circle {
        &.icon-check-circle.is-release-version-icon {
            color: $successColor;
        }
        color: #979BA5;
        font-size: 14px;
    }
    .pipeline-branch-version-icon {
        color: #FF9C01;
    }
    > span {
        line-height: 24px;
        @include ellipsis();
    }
    cursor: pointer;
    .icon-angle-down {
        transition: transform 0.3s;
        font-size: 20px;
    }
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
                color: $successColor;
            }
        }
        .pipeline-branch-version-icon {
            color: #FF9C01;
        }
        .pipeline-version-main-branch {
            color: $successColor;
        }
        .pipeline-version-name {
            font-weight: 700;
            @include ellipsis();
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
