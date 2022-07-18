<template>
    <div class="machine-record-wrapper">
        <bk-table
            size="small"
            class="record-table"
            :outer-border="false"
            :data="recordList"
            @page-change="handlePageChange"
            @page-limit-change="handlePageLimitChange"
            :pagination="pagination"
            :empty-text="$t('noData')"
        >
            <bk-table-column :label="$t('time')" prop="actionTime" min-width="160">
                <template slot-scope="props">
                    {{ localConvertTime(props.row.actionTime) }}
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('status')" prop="action" min-width="160">
                <template slot-scope="props">
                    <span :title="props.row.action" :class="props.row.action === 'ONLINE' ? 'online' : 'offline'">{{ props.row.action === 'ONLINE' ? $t('setting.nodeInfo.online') : $t('setting.nodeInfo.offline') }}</span>
                </template>
            </bk-table-column>
        </bk-table>
    </div>
</template>

<script>
    import { convertTime } from '@/utils/util'
    import { bus } from '@/utils/bus'
    import { setting } from '@/http'
    import { mapState } from 'vuex'

    export default {
        data () {
            return {
                recordList: [],
                pagination: {
                    count: 30,
                    current: 1,
                    limitList: [10, 20, 50, 100],
                    limit: 10
                }
            }
        },
        computed: {
            ...mapState(['projectId']),
            nodeHashId () {
                return this.$route.params.agentId
            }
        },
        created () {
            bus.$off('refreshAction')
            bus.$on('refreshAction', () => {
                this.requestActionList(this.pagination.current, this.pagination.limit)
            })
        },
        mounted () {
            this.requestActionList(this.pagination.current, this.pagination.limit)
        },
        methods: {
            async requestActionList (page, pageSize) {
                try {
                    const res = await setting.requestActionList({
                        projectId: this.projectId,
                        nodeHashId: this.nodeHashId,
                        page: page || 1,
                        pageSize: pageSize || 10
                    })
                    this.recordList.splice(0, this.recordList.length, ...res.records || [])
                    this.pagination.count = res.count
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            handlePageLimitChange (limit) {
                this.pagination.current = 1
                this.pagination.limit = limit
                this.requestActionList(this.pagination.current, this.pagination.limit)
            },
            handlePageChange (newPage) {
                this.pagination.current = newPage
                this.requestActionList(newPage, this.pagination.limit)
            },
            /**
             * 处理时间格式
             */
            localConvertTime (timestamp) {
                return convertTime(timestamp * 1000)
            }
        }
    }
</script>

<style lang="postcss">
    @import '@/css/conf';
    .machine-record-wrapper {
        padding: 20px 0;
        .record-table {
            .no-data {
                padding: 20px 0;
                text-align: center;
            }
            .online {
                color: #30D878;
            }
            .offline {
                color: #C3CDD7;
            }
        }
        .ci-paging {
            margin: 20px 20px 10px;
        }
    }
</style>
