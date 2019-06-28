<template>
    <div class="machine-record-wrapper">
        <bk-table
            size="small"
            class="record-table"
            :outer-border="false"
            :data="recordList"
            :empty-text="'暂无数据'">
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
        <full-paging v-if="recordList.length"
            :size="'small'"
            :page-count-config.sync="pageCountConfig"
            :paging-config.sync="pagingConfig"
            @page-count-changed="pageCountChanged"
            @page-changed="pageChanged">
        </full-paging>
    </div>
</template>

<script>
    import fullPaging from '@/components/common/full-paging'
    import { convertTime } from '@/utils/util'
    import { bus } from '@/utils/bus'

    export default {
        components: {
            fullPaging
        },
        data () {
            return {
                recordList: [],
                pageCountConfig: {
                    totalCount: 30,
                    list: [
                        { id: 10, name: 10 },
                        { id: 20, name: 20 },
                        { id: 50, name: 50 },
                        { id: 100, name: 100 }
                    ],
                    perPageCountSelected: 10
                },
                pagingConfig: {
                    totalPage: 10,
                    curPage: 1
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
                this.requestActionList(this.pagingConfig.curPage, this.pageCountConfig.perPageCountSelected)
            })
        },
        mounted () {
            this.requestActionList(1, 10)
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
                    this.pageCountConfig.totalCount = res.count
                    this.pagingConfig.totalPage = Math.ceil(this.pageCountConfig.totalCount / pageSize)
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            async pageCountChanged () {
                this.pagingConfig.curPage = 1
                await this.requestActionList(this.pagingConfig.curPage, this.pageCountConfig.perPageCountSelected)
            },
            async pageChanged () {
                await this.requestActionList(this.pagingConfig.curPage, this.pageCountConfig.perPageCountSelected)
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
