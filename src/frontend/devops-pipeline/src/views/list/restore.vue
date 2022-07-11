<template>
    <div class="pipeline-restore-list" v-bkloading="{
        isLoading: loading.isLoading,
        title: loading.title
    }">
        <template v-if="showContent && recycleList.length">
            <div class="restore-list-content">
                <div class="info-header">
                    <p>{{ $t('restore.title') }}</p>
                    <bk-input v-model="searchVal" :placeholder="$t('restore.restoreSearchTips')" class="search-input"></bk-input>
                </div>
                <div class="restore-table-wrapper">
                    <bk-table
                        :data="curRecycleList"
                        size="small">
                        <bk-table-column :label="$t('pipelineName')" prop="pipelineName"></bk-table-column>
                        <bk-table-column :label="$t('restore.deleter')" prop="lastModifyUser" width="150"></bk-table-column>
                        <bk-table-column :label="$t('restore.deleteTime')" prop="updateTime" :formatter="formatTime" width="180"></bk-table-column>
                        <bk-table-column :label="$t('restore.createTime')" prop="createTime" :formatter="formatTime" width="180"></bk-table-column>
                        <bk-table-column :label="$t('operate')" width="150">
                            <template slot-scope="props">
                                <bk-button theme="primary" text @click="restore(props.row)">{{ $t('restore.restore') }}</bk-button>
                            </template>
                        </bk-table-column>
                    </bk-table>
                </div>
            </div>
        </template>
        <empty-tips v-if="showContent && !recycleList.length"
            :title="emptyTipsConfig.title"
            :desc="emptyTipsConfig.desc"
            :btns="emptyTipsConfig.btns">
        </empty-tips>
    </div>
</template>

<script>
    import { mapActions } from 'vuex'
    import emptyTips from '@/components/pipelineList/imgEmptyTips'
    import { convertTime } from '@/utils/util'

    export default {
        components: {
            emptyTips
        },
        data () {
            return {
                showContent: false,
                recycleList: [],
                loading: {
                    isLoading: false,
                    title: ''
                },
                emptyTipsConfig: {
                    title: this.$t('restore.emptyTitle'),
                    desc: this.$t('restore.emptyDesc'),
                    btns: []
                },
                searchVal: ''
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            curRecycleList () {
                return this.recycleList.filter(i => i.pipelineName.includes(this.searchVal))
            }
        },
        async mounted () {
            console.log('restore page')
            await this.requestRecycleList()
        },
        methods: {
            ...mapActions('pipelines', [
                'requestRecyclePipelineList',
                'restorePipeline'
            ]),
            /** *
             * 获取待还原流水线列表
             */
            async requestRecycleList (flag) {
                const { loading } = this

                loading.isLoading = true

                try {
                    const res = await this.requestRecyclePipelineList({
                        projectId: this.projectId
                    })
                    this.recycleList = res.records || []
                } catch (err) {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                } finally {
                    this.loading.isLoading = false
                    this.showContent = true
                }
            },
            /** *
             * 恢复流水线
             */
            async restore (row) {
                try {
                    const res = await this.restorePipeline({
                        projectId: this.projectId,
                        pipelineId: row.pipelineId
                    })
                    if (res) {
                        this.$showTips({
                            message: this.$t('restore.restoreSuc'),
                            theme: 'success'
                        })
                    }
                } catch (err) {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                } finally {
                    this.requestRecycleList()
                }
            },
            formatTime (row, cell, value) {
                return convertTime(value)
            }
        }
    }
</script>

<style lang='scss'>
    @import './../../scss/conf';
    .pipeline-restore-list {
        min-height: calc(100% - 40px);
        .restore-list-content {
            margin: 25px auto;
            width: 1080px;
        }
        .restore-table-wrapper {
            margin-top: 16px;
        }
        .info-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .search-input {
            width: 280px;
        }
    }
</style>
