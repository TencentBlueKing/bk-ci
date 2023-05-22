<template>
    <div class="version-sideslider-container">
        <div
            class="pipeline-latest"
            @click="showVersionSideslider = true"
        >
            {{latestPipelineVersionInfo}}
        </div>
        <bk-sideslider
            :width="580"
            :title="$t('template.versionList')"
            :is-show.sync="showVersionSideslider"
            :quick-close="true"
            @shown="handlePaginationChange()">
            <section class="m20" slot="content" v-bkloading="{ isLoading, title: $t('loadingTips') }">
                <bk-table
                    :data="versionList"
                    :pagination="pagination"
                    @page-change="current => handlePaginationChange({ current })"
                    @page-limit-change="limit => handlePaginationChange({ limit })"
                    size="small"
                >
                    <bk-table-column :label="$t('versionNum')" prop="version" width="80">
                        <template slot-scope="props">
                            {{props.row.version}}
                        </template>
                    </bk-table-column>
                    <bk-table-column :label="$t('restore.createTime')" prop="createTime">
                        <template slot-scope="props">
                            <span>{{ convertTime(props.row.createTime) }}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column :label="$t('lastUpdater')" prop="creator"></bk-table-column>
                    <bk-table-column :label="$t('operate')">
                        <template slot-scope="props">
                            <bk-button theme="primary" text @click.stop="requestTemplateByVersion(props.row.version)">{{ $t('load') }}</bk-button>
                            <bk-button class="ml10" theme="primary" text :disabled="!currentPipeline.hasPermission || currentPipeline.pipelineVersion === props.row.version" @click="deleteVersion(props.row)">{{ $t('delete') }}</bk-button>
                        </template>
                    </bk-table-column>
                </bk-table>
            </section>
        </bk-sideslider>
    </div>
</template>
<script>
    import { mapGetters, mapMutations, mapActions } from 'vuex'
    import { convertTime, navConfirm } from '@/utils/util'
    export default {
        name: 'versionSideslider',
        data () {
            return {
                isLoading: false,
                showVersionSideslider: false,
                versionList: [],
                pagination: {
                    size: 'small',
                    type: 'compact',
                    align: 'right',
                    current: 1,
                    count: 0,
                    limit: 15,
                    limitList: [15],
                    showLimit: false
                }
            }
        },
        computed: {
            ...mapGetters('pipelines', ['getCurPipeline']),
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            },
            currentPipeline () {
                return this.getCurPipeline
            },
            // 最新的流水线版本信息
            latestPipelineVersionInfo () {
                console.log(this.currentPipeline)
                return this.currentPipeline
                    ? this.$t('editPage.pipelineVersionSaveAt', [this.currentPipeline.pipelineVersion, this.currentPipeline.deploymentTime])
                    : this.$t('pipelinesEdit')
            }
        },
        methods: {
            convertTime,
            ...mapMutations('atom', ['SET_PIPELINE_EDITING']),
            ...mapMutations('pipelines', ['PIPELINE_SETTING_MUTATION']),
            ...mapActions('atom', ['setPipeline']),
            ...mapActions('pipelines', [
                'requestPipelineVersionList',
                'deletePipelineVersion',
                'requestPipelineByVersion',
                'requestPipelineSettingByVersion'
            ]),
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
                    pageSize: this.pagination.limit
                }).then(({ data: { records, count } }) => {
                    this.versionList = records
                    this.pagination.count = count
                }).catch(err => {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                }).finally(() => {
                    this.isLoading = false
                })
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
                if (this.currentPipeline.hasPermission && this.currentPipeline.pipelineVersion !== row.version) {
                    const content = this.$t('delete') + this.$t('version') + row.version
                    navConfirm({ content }).then(() => {
                        this.deletePipelineVersion({
                            projectId: this.projectId,
                            pipelineId: this.pipelineId,
                            version: row.version
                        }).then(() => {
                            this.handlePaginationChange()
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
<style lang="scss" scoped>
@import "@/scss/conf";
.version-sideslider-container {
    line-height: initial;
    .pipeline-latest {
        font-size: 12px;
        color: $fontColor;
        border-bottom: 1px solid;
        line-height: initial;
        cursor: pointer;
    }
}
</style>
