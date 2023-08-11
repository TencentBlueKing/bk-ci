<template>
    <main class="pipeline-version-history" v-bkloading="{ isLoading }">
        <header class="pipeline-version-history-header">
            <bk-select
                v-model="searchKeys.creator"
                enable-scroll-load
                :scroll-loading="bottomLoadingOptions"
                @scroll-end="loadMoreCreator"
                @change="getPipelineVersions(1)"
            >
                <bk-option
                    v-for="creator in creatorList"
                    :key="creator"
                    :id="creator"
                    :name="creator"
                >
                    {{ creator }}
                </bk-option>
            </bk-select>
            <bk-input
                v-model="searchKeys.description"
                @enter="getPipelineVersions(1)"
                right-icon="bk-icon icon-search"
                clearable
                @clear="getPipelineVersions(1)"
                :placeholder="$t('变更说明')"
            />
        </header>
        <section class="pipeline-version-history-content">
            <bk-table
                height="100%"
                :data="pipelineVersionList"
                :pagination="pagination"
            >
                <bk-table-column v-for="column in columns" :key="column.key" v-bind="column" />
                <bk-table-column width="300" :label="$t('operate')">
                    <template slot-scope="props">
                        <bk-button
                            text
                            size="small"
                            theme="primary"
                            @click.stop="requestTemplateByVersion(props.row.version)"
                        >
                            {{ $t('rollback') }}
                        </bk-button>
                        <bk-button
                            text
                            size="small"
                            theme="primary"
                            :disabled="!currentPipeline.hasPermission || currentPipeline.version === props.row.version"
                            @click="deleteVersion(props.row)"
                        >
                            {{ $t('delete') }}
                        </bk-button>
                    </template>
                </bk-table-column>
            </bk-table>
        </section>
    </main>
</template>

<script>
    import { mapActions, mapGetters, mapMutations } from 'vuex'
    import { convertMiniTime, navConfirm } from '@/utils/util'
    export default {
        data () {
            return {
                isLoading: false,
                pipelineVersionList: [],
                creatorList: [],
                pagination: {
                    limit: 20,
                    current: 1,
                    count: 0
                },
                creatorPagination: {
                    limit: 20,
                    current: 1,
                    hasNext: true
                },
                bottomLoadingOptions: {
                    size: 'small',
                    isLoading: false
                },
                searchKeys: {
                    creator: undefined,
                    description: undefined
                }
            }
        },
        computed: {
            ...mapGetters('pipelines', ['getCurPipeline']),
            currentPipeline () {
                return this.getCurPipeline
            },
            creators () {
                return [
                    'lockiechen'
                ]
            },
            columns () {
                return [{
                    prop: 'versionName',
                    label: this.$t('versionNum')
                }, {
                    prop: 'version',
                    label: this.$t('versionDesc')
                }, {
                    prop: 'createTime',
                    label: this.$t('createTime'),
                    formatter: (row) => {
                        return convertMiniTime(row.createTime)
                    }
                }, {
                    prop: 'creator',
                    label: this.$t('creator')
                }, {
                    prop: 'updateTime',
                    label: this.$t('details.lastModified'),
                    formatter: (row) => {
                        return convertMiniTime(row.updateTime)
                    }
                }, {
                    prop: 'lastModifyUser',
                    label: this.$t('lastUpdater')
                }]
            }
        },
        created () {
            this.init(1)
        },
        methods: {
            ...mapMutations('atom', ['SET_PIPELINE_EDITING']),
            ...mapMutations('pipelines', ['PIPELINE_SETTING_MUTATION']),
            ...mapActions('atom', ['setPipeline']),
            ...mapActions('pipelines', [
                'requestPiplineCreators',
                'requestPipelineVersionList',
                'deletePipelineVersion',
                'requestPipelineByVersion',
                'requestPipelineSettingByVersion'
            ]),
            async init (page) {
                try {
                    this.isLoading = true

                    await Promise.all([
                        this.getPipelineVersions(page),
                        this.getCreatorList(page)
                    ])
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message ?? error
                    })
                } finally {
                    this.isLoading = false
                }
            },
            async getCreatorList (page) {
                const { projectId, pipelineId } = this.$route.params
                const { data } = await this.requestPiplineCreators({
                    projectId,
                    pipelineId,
                    page,
                    pageSize: this.creatorPagination.limit
                })
                this.creatorList = this.creatorList.concat(data.records)
                console.log(this.creatorList, data)
                this.creatorPagination = {
                    page: data.page,
                    hasNext: this.creatorList.length < data.count
                }
            },
            async getPipelineVersions (page) {
                const { projectId, pipelineId } = this.$route.params
                const res = await this.requestPipelineVersionList({
                    projectId,
                    pipelineId,
                    page,
                    pageSize: this.pagination.limit,
                    ...this.searchKeys
                })
                Object.assign(this.pagination, {
                    current: res.page,
                    limit: res.pageSize,
                    count: res.count
                })
                this.pipelineVersionList = res.records
            },
            async loadMoreCreator () {
                try {
                    this.bottomLoadingOptions.isLoading = true
                    await this.getCreatorList(this.creatorPagination.page + 1)
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message ?? error
                    })
                } finally {
                    this.bottomLoadingOptions.isLoading = false
                }
            },
            requestTemplateByVersion (version) {
                let theme, message
                Promise.all([
                    this.requestPipelineByVersion({ ...this.$route.params, version }),
                    this.requestPipelineSettingByVersion({ ...this.$route.params, version })
                ]).then(([{ data }, { data: settingData }]) => {
                    this.setPipeline(data)
                    this.PIPELINE_SETTING_MUTATION({ pipelineSetting: settingData })
                    this.SET_PIPELINE_EDITING(true)
                    theme = 'success'
                    message = this.$t('subpage.loadPipelineVersionSuccess', { version })
                    this.showVersionSideslider = false
                }).catch(err => {
                    theme = 'error'
                    message = err.message || err
                }).finally(() => {
                    this.$bkMessage({
                        theme,
                        message
                    })
                })
            },
            deleteVersion (row) {
                if (this.currentPipeline.hasPermission && this.currentPipeline.version !== row.version) {
                    const { projectId, pipelineId } = this.$route.params
                    const content = this.$t('delete') + this.$t('version') + row.versionName
                    navConfirm({ content }).then(() => {
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
            }
        }
    }
</script>

<style lang="scss">
    .pipeline-version-history {
        padding: 24px;
        display: flex;
        flex-direction: column;
        height: 100%;
        overflow: hidden;
        &-header {
            display: grid;
            grid-auto-flow: column;
            grid-template-columns: 180px 400px;
            grid-gap: 8px;
            margin-bottom: 16px;
        }
        &-content {
            flex: 1;
            overflow: hidden;
        }
    }
</style>
