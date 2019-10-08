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
            :empty-text="'暂无数据'"
        >
            <bk-table-column label="时间" prop="actionTime" min-width="160">
                <template slot-scope="props">
                    {{ localConvertTime(props.row.actionTime) }}
                </template>
            </bk-table-column>
            <bk-table-column label="所属Job" prop="action" min-width="160">
                <template slot-scope="props">
                    <span :title="props.row.action" :class="props.row.action === 'ONLINE' ? 'online' : 'offline'">{{ props.row.action === 'ONLINE' ? '上线' : '下线' }}</span>
                </template>
            </bk-table-column>
        </bk-table>
    </div>
</template>

<script>
    import { convertTime } from '@/utils/util'
    import { bus } from '@/utils/bus'

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
            projectId () {
                return this.$route.params.projectId
            },
            nodeHashId () {
                return this.$route.params.nodeHashId
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
                    const res = await this.$store.dispatch('environment/requestActionList', {
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

<style lang="scss">
    @import './../../../scss/conf';
    .machine-record-wrapper {
        padding: 20px 0;
        .record-table {
            // border-top: 1px solid $borderWeightColor;
            // border-bottom: 1px solid $borderWeightColor;
            // th, td {
            //     height: 42px;
            //     &:first-child {
            //         padding-left: 30px;
            //     }
            // }
            // th {
            //     color: #333C48;
            //     background-color: #FAFAFA;
            //     font-weight: normal;
            // }
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
