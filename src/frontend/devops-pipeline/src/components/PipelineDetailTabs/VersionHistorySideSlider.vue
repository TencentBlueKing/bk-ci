<template>
    <bk-sideslider
        quick-close
        :width="1080"
        :is-show="showVersionSideslider"
        :before-close="handleClose"
        @shown="handleShown"
        ext-cls="pipeline-version-sideslider"
        :transfer="false"
    >
        <header slot="header">
            {{ $t('template.versionList') }}
            <bk-popover class="pipeline-version-rule-tips">
                <span class="pipeline-version-rule-tips-trigger">
                    <i class="devops-icon icon-question-circle" />
                    {{ $t('versionRule') }}
                </span>
                <div slot="content">
                    <p>{{ $t('versionRule') }}</p>
                    <p>{{ $t('versionRuleP') }}</p>
                    <p>{{ $t('versionRuleT') }}</p>
                    <p>{{ $t('versionRuleA') }}</p>
                </div>
            </bk-popover>
        </header>
        <main
            slot="content"
            class="pipeline-version-history"
            v-bkloading="{ isLoading }"
        >
            <header class="pipeline-version-history-header">
                <search-select
                    class="pipeline-version-search-select"
                    :placeholder="filterTips"
                    :data="filterData"
                    v-model="filterKeys"
                    @change="queryVersionList"
                />
            </header>
            <section
                class="pipeline-version-history-content"
                ref="tableBox"
            >
                <bk-table
                    :data="pipelineVersionList"
                    :pagination="pagination"
                    @page-change="handlePageChange"
                    @page-limit-change="handleLimitChange"
                    :max-height="$refs?.tableBox?.offsetHeight"
                    size="small"
                >
                    <empty-exception
                        :type="emptyType"
                        slot="empty"
                        @clear="clearFilter"
                    ></empty-exception>
                    <bk-table-column
                        v-for="column in columns"
                        :key="column.prop"
                        v-bind="column"
                    >
                        <template
                            v-if="column.prop === 'versionName'"
                            v-slot="{ row }"
                        >
                            <div
                                :class="['pipeline-version-name-cell', {
                                    'active-version-name': row.version === releaseVersion
                                }]"
                            >
                                <span>
                                    <i
                                        class="devops-icon icon-edit-line"
                                        v-if="row.isDraft"
                                    />
                                    <logo
                                        v-else-if="row.isBranchVersion"
                                        name="branch"
                                        size="16"
                                    />
                                    <i
                                        v-else
                                        class="devops-icon icon-check-circle"
                                    />
                                </span>
                                {{ row.versionName }}
                                <!-- <span>
                                    [{{ $t('mainBranch') }}]
                                </span> -->
                            </div>
                        </template>
                    </bk-table-column>
                    <bk-table-column
                        :label="$t('operate')"
                        :width="320"
                        prop="operate"
                        fixed="right"
                    >
                        <div
                            slot-scope="props"
                            class="pipeline-history-version-operate"
                        >
                            <bk-button
                                v-if="props.row.isDraft"
                                text
                                @click="goDebugRecords"
                            >
                                {{ $t('draftExecRecords') }}
                            </bk-button>
                            <rollback-entry
                                v-if="props.row.canRollback && !archiveFlag"
                                :has-permission="canEdit"
                                :version="props.row.version"
                                :pipeline-id="$route.params.pipelineId"
                                :project-id="$route.params.projectId"
                                :version-name="props.row.versionName"
                                :draft-base-version-name="draftBaseVersionName"
                                :is-active-draft="props.row.isDraft"
                                :is-active-branch-version="props.row.isBranchVersion"
                                :draft-creator="props.row?.creator"
                                :draft-create-time="props.row?.createTime"
                            />
                            <version-diff-entry
                                v-if="props.row.version !== releaseVersion"
                                :version="props.row.currentDiffVersion"
                                :latest-version="props.row.latestDiffVersion"
                                :archive-flag="archiveFlag"
                            />
                            <bk-button
                                v-if="!archiveFlag"
                                text
                                theme="primary"
                                :disabled="releaseVersion === props.row.version"
                                @click="deleteVersion(props.row)"
                            >
                                {{ $t('delete') }}
                            </bk-button>
                        </div>
                    </bk-table-column>
                </bk-table>
            </section>
        </main>
    </bk-sideslider>
</template>

<script>
    import Logo from '@/components/Logo'
    import EmptyException from '@/components/common/exception'
    import { VERSION_STATUS_ENUM } from '@/utils/pipelineConst'
    import { convertTime, navConfirm } from '@/utils/util'
    import SearchSelect from '@blueking/search-select'
    import '@blueking/search-select/dist/styles/index.css'
    import { mapActions, mapGetters, mapState } from 'vuex'
    import RollbackEntry from './RollbackEntry'
    import VersionDiffEntry from './VersionDiffEntry'
    export default {
        components: {
            SearchSelect,
            VersionDiffEntry,
            RollbackEntry,
            EmptyException,
            Logo
        },
        props: {
            showVersionSideslider: Boolean
        },
        data () {
            return {
                isLoading: false,
                pipelineVersionList: [],
                pagination: {
                    limit: 20,
                    current: 1,
                    count: 0
                },
                filterKeys: []
            }
        },
        computed: {
            ...mapState('atom', ['pipelineInfo']),
            ...mapGetters({
                draftBaseVersionName: 'atom/getDraftBaseVersionName'
            }),
            releaseVersion () {
                return this.pipelineInfo?.releaseVersion
            },
            canEdit () {
                return this.pipelineInfo?.permissions?.canEdit
            },
            columns () {
                return [{
                    prop: 'versionName',
                    width: 120,
                    label: this.$t('versionNum'),
                    showOverflowTooltip: true
                }, {
                    prop: 'description',
                    width: 120,
                    label: this.$t('versionDesc'),
                    showOverflowTooltip: true
                }, {
                    prop: 'createTime',
                    label: this.$t('createTime'),
                    showOverflowTooltip: true,
                    width: 156,
                    formatter: (row) => {
                        return convertTime(row.createTime)
                    }
                }, {
                    prop: 'creator',
                    width: 120,
                    label: this.$t('creator')
                }, {
                    prop: 'updateTime',
                    label: this.$t('lastUpdateTime'),
                    showOverflowTooltip: true,
                    width: 156,
                    formatter: (row) => {
                        return convertTime(row.updateTime)
                    }
                }, {
                    prop: 'updater',
                    width: 120,
                    label: this.$t('audit.operator')
                }]
            },
            filterTips () {
                return this.filterData.map(item => item.name).join(' / ')
            },
            filterData () {
                return [{
                    name: this.$t('version'),
                    id: 'versionName'
                }, {
                    name: this.$t('versionDesc'),
                    id: 'description'
                }, {
                    name: this.$t('audit.operator'),
                    id: 'creator'
                }]
            },
            filterQuery () {
                return this.filterKeys.reduce((query, item) => {
                    query[item.id] = item.values.map(value => value.id).join(',')
                    return query
                }, {})
            },
            emptyType () {
                return this.filterKeys.length > 0 ? 'search-empty' : 'empty'
            },
            archiveFlag () {
                return this.$route.query.archiveFlag
            }
        },
        mounted () {
            this.preZIndex = window.__bk_zIndex_manager.zIndex
            window.__bk_zIndex_manager.zIndex = 2500
        },
        beforeDestroy () {
            window.__bk_zIndex_manager.zIndex = this.preZIndex
        },
        methods: {
            ...mapActions({
                requestPipelineSummary: 'atom/requestPipelineSummary',
                requestPipelineVersionList: 'pipelines/requestPipelineVersionList',
                deletePipelineVersion: 'pipelines/deletePipelineVersion',
                setHistoryPageStatus: 'pipelines/setHistoryPageStatus'
            }),
            handleShown () {
                this.handlePageChange(1)
            },
            async init (page) {
                try {
                    this.isLoading = true
                    await this.getPipelineVersions(page)
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message ?? error
                    })
                } finally {
                    this.isLoading = false
                }
            },
            queryVersionList () {
                this.handlePageChange(1)
            },
            handlePageChange (page) {
                this.pagination.current = page
                this.init(page)
            },
            handleLimitChange (limit) {
                this.pagination.limit = limit
                this.$nextTick(() => {
                    this.handlePageChange(1)
                })
            },
            async getPipelineVersions (page) {
                const { projectId, pipelineId } = this.$route.params
                const res = await this.requestPipelineVersionList({
                    projectId,
                    pipelineId,
                    page,
                    pageSize: this.pagination.limit,
                    archiveFlag: this.archiveFlag,
                    ...this.filterQuery
                })
                Object.assign(this.pagination, {
                    current: res.page,
                    limit: res.pageSize,
                    count: res.count
                })
                this.pipelineVersionList = res.records.map(item => {
                    const isDraft = item.status === VERSION_STATUS_ENUM.COMMITTING
                    return {
                        ...item,
                        isDraft,
                        canRollback: !isDraft,
                        isBranchVersion: item.status === VERSION_STATUS_ENUM.BRANCH,
                        versionName: item.versionName || this.$t('editPage.draftVersion', [item.baseVersionName]),
                        currentDiffVersion: !isDraft ? item.version : this.releaseVersion,
                        latestDiffVersion: !isDraft ? this.releaseVersion : item.version
                    }
                })
            },
            async deleteVersion (row) {
                if (this.releaseVersion !== row.version) {
                    const { projectId, pipelineId } = this.$route.params
                    const content = this.$t('deleteVersionConfirm', [row.versionName])
                    const confirm = await navConfirm({
                        content,
                        type: 'error',
                        theme: 'danger'
                    })
                    if (confirm) {
                        try {
                            await this.deletePipelineVersion({
                                projectId,
                                pipelineId,
                                version: row.version
                            })
                            this.handlePageChange(1)
                            this.$showTips({
                                message: this.$t('delete') + this.$t('version') + this.$t('success'),
                                theme: 'success'
                            })

                            this.requestPipelineSummary(this.$route.params)
                        } catch (err) {
                            this.$showTips({
                                message: err.message || err,
                                theme: 'error'
                            })
                        }
                    }
                }
            },
            handleClose () {
                this.$emit('close')
                this.clearFilter(false)
                return true
            },
            clearFilter (refresh = true) {
                this.filterKeys = []
                refresh && this.queryVersionList()
            },
            goDebugRecords () {
                this.$router.push({
                    name: 'draftDebugRecord',
                    query: {
                        ...(this.archiveFlag ? { archiveFlag: this.archiveFlag } : {})
                    }
                })
            }
        }
    }
</script>

<style lang="scss">
    @import "@/scss/conf";
    @import "@/scss/select-dark-theme.scss";
    .pipeline-version-history {
        padding: 24px;
        display: flex;
        flex-direction: column;
        height: calc(100vh - 60px);
        overflow: hidden;
        &-header {
            margin-bottom: 20px;
            display: flex;
            justify-content: flex-end;
            .pipeline-version-search-select {
                width: 500px;
                ::placeholder {
                    color: #c4c6cc;
                }
            }
        }
        &-content {
            flex: 1;
            overflow: hidden;
            .pipeline-version-name-cell {
                display: flex;
                align-items: center;
                grid-gap: 6px;
                > span {
                    flex-shrink: 0;
                    font-size: 16px;
                }
                &.active-version-name .icon-check-circle {
                    color: #2DCB56;
                }
            }
        }
    }
    .pipeline-yaml-diff-content {
        height: 666px;
        overflow: auto;
        position: relative;
    }
    .pipeline-history-version-operate {
        display: flex;
        grid-gap: 16px;
        align-items: center;
    }
    .pipeline-version-rule-tips {
        margin-left: 24px;
        color: $primaryColor;
        .pipeline-version-rule-tips-trigger {
            display: flex;
            align-items: center;
            grid-gap: 4px;
            font-size: 12px;
            > i {
                font-size: 14px;
            }
        }
    }
</style>
