<template>
    <bk-sideslider
        quick-close
        :width="950"
        :title="$t('template.versionList')"
        :is-show="showVersionSideslider"
        :before-close="handleClose"
        @shown="handleShown"
    >
        <main slot="content" class="pipeline-version-history" v-bkloading="{ isLoading }">
            <header class="pipeline-version-history-header">
                <SearchSelect
                    class="pipeline-version-search-select"
                    :placeholder="filterTips"
                    :data="filterData"
                    v-model="filterKeys"
                    @change="queryVersionList"
                />
            </header>
            <section class="pipeline-version-history-content">
                <bk-table
                    height="100%"
                    :data="pipelineVersionList"
                    :pagination="pagination"
                >
                    <bk-table-column v-for="column in columns" :key="column.prop" v-bind="column">
                        <template v-if="column.prop === 'versionName'" v-slot="{ row }">
                            <div :class="['pipeline-version-name-cell', {
                                'active-version-name': row.version === releaseVersion
                            }]">
                                <i class="devops-icon icon-draft" v-if="row.status === 'COMMITTING'" />
                                <i v-else class="devops-icon icon-check-circle" />
                                {{ row.versionName }}
                                <!-- <span>
                                    [{{ $t('mainBranch') }}]
                                </span> -->
                            </div>
                        </template>
                    </bk-table-column>
                    <bk-table-column width="300" :label="$t('operate')">
                        <template slot-scope="props">
                            <rollback-entry
                                :version="props.row.version"
                            />
                            <bk-button
                                text
                                size="small"
                                theme="primary"
                                :disabled="!pipelineInfo.hasPermission || pipelineInfo.version === props.row.version"
                                @click="deleteVersion(props.row)"
                            >
                                {{ $t('delete') }}
                            </bk-button>
                            <version-diff-entry
                                :version="props.row.version"
                                :release-version="releaseVersion"
                                :current-yaml="currentYaml"
                            />
                        </template>
                    </bk-table-column>
                </bk-table>
            </section>
        </main>
    </bk-sideslider>
</template>

<script>
    import SearchSelect from '@blueking/search-select'
    import { mapActions, mapState, mapMutations } from 'vuex'
    import { convertTime, navConfirm } from '@/utils/util'
    import VersionDiffEntry from './VersionDiffEntry'
    import RollbackEntry from './RollbackEntry'

    import '@blueking/search-select/dist/styles/index.css'
    export default {
        components: {
            SearchSelect,
            VersionDiffEntry,
            RollbackEntry
        },
        props: {
            showVersionSideslider: Boolean,
            currentYaml: {
                type: String,
                required: true
            }
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
            ...mapState('pipelines', ['pipelineInfo']),
            releaseVersion () {
                return this.pipelineInfo?.version
            },
            columns () {
                return [{
                    prop: 'versionName',
                    label: this.$t('versionNum')
                }, {
                    prop: 'desc',
                    label: this.$t('versionDesc')
                }, {
                    prop: 'createTime',
                    label: this.$t('createTime'),
                    formatter: (row) => {
                        return convertTime(row.createTime)
                    }
                }, {
                    prop: 'lastModifyUser',
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
            }
        },
        methods: {
            ...mapMutations('atom', ['SET_PIPELINE_EDITING']),
            ...mapMutations('pipelines', ['PIPELINE_SETTING_MUTATION']),

            ...mapActions('pipelines', [
                'requestPipelineVersionList',
                'deletePipelineVersion'
            ]),
            handleShown () {
                this.init(1)
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
                console.log(this.filterKeys, this.filterQuery)
                this.init(1)
            },
            async getPipelineVersions (page) {
                const { projectId, pipelineId } = this.$route.params
                const res = await this.requestPipelineVersionList({
                    projectId,
                    pipelineId,
                    page,
                    pageSize: this.pagination.limit,
                    ...this.filterQuery
                })
                Object.assign(this.pagination, {
                    current: res.page,
                    limit: res.pageSize,
                    count: res.count
                })
                this.pipelineVersionList = res.records
            },
            deleteVersion (row) {
                if (this.pipelineInfo?.hasPermission && this.releaseVersion !== row.version) {
                    const { projectId, pipelineId } = this.$route.params
                    const content = this.$t('deleteVersionConfirm', [row.versionName])
                    navConfirm({
                        content,
                        theme: 'danger'
                    }).then(() => {
                        this.deletePipelineVersion({
                            projectId,
                            pipelineId,
                            version: row.version
                        }).then(() => {
                            this.getPipelineVersions(1)
                            this.$showTips({
                                message: this.$t('delete') + this.$t('version') + this.$t('success'),
                                theme: 'success'
                            })
                        }).catch(err => {
                            this.$showTips({
                                message: err.message || err,
                                theme: 'error'
                            })
                        })
                    })
                }
            },
            handleClose () {
                this.$emit('close')
                return true
            }
        }
    }
</script>

<style lang="scss">
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
                .icon-check-circle {
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
    .diff-version-header {
        height: 40px;
        background: #1d1d1d;
        display: grid;
        grid-auto-flow: column;
        grid-template-columns: 1fr 1fr;
        align-items: center;
        padding: 0 24px;
        color: #C4C6CC;

    }

</style>
