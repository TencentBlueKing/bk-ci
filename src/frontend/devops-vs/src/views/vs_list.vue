<template>
    <div class="vs-list-wrapper">
        <content-header>
            <div slot="left">扫描记录</div>
            <p slot="right">本服务由金刚团队（企业微信：KingKong）提供后台支持</p>
        </content-header>

        <section class="sub-view-port"
            v-bkloading="{
                isLoading: loading.isLoading,
                title: loading.title
            }">
            <template v-if="showContent && vsList.length">
                <bk-table style="margin-top: 15px;"
                    :data="vsList"
                    :pagination="pagination"
                    :row-class-name="rowClassFun"
                    @page-change="pageChangedHandler"
                    @page-limit-change="pageCountChangedHandler"
                    @row-click="toRowReport"
                >
                    <bk-table-column label="文件名（版本号）">
                        <template slot-scope="props">
                            <span :title="props.row.fileName + '（' + props.row.version + '）'" class="primary-color">{{ props.row.fileName }}（{{ props.row.version }}）</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column label="流水线">
                        <template slot-scope="props">
                            <span :title=" props.row.pipelineName">{{ props.row.pipelineName }}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column label="构建号" prop="buildNo" width="150"></bk-table-column>
                    <bk-table-column label="大小" prop="fileSize" :formatter="localCoverSize" width="150"></bk-table-column>
                    <bk-table-column label="开始时间" prop="createTime" :formatter="localConvertTime"></bk-table-column>
                    <bk-table-column label="执行人" prop="creator" width="150"></bk-table-column>
                    <bk-table-column label="状态" width="150">
                        <template slot-scope="props">
                            <div v-if="props.row.status === '扫描中'">
                                <div class="bk-spin-loading bk-spin-loading-mini bk-spin-loading-primary">
                                    <div class="rotate rotate1"></div>
                                    <div class="rotate rotate2"></div>
                                    <div class="rotate rotate3"></div>
                                    <div class="rotate rotate4"></div>
                                    <div class="rotate rotate5"></div>
                                    <div class="rotate rotate6"></div>
                                    <div class="rotate rotate7"></div>
                                    <div class="rotate rotate8"></div>
                                </div>
                                <span>{{ props.row.status }}</span>
                            </div>
                            <div v-if="props.row.status === '成功'">
                                <span class="status-icon done-stutus"></span>
                                <span>{{ props.row.status }}</span>
                            </div>
                            <div v-if="props.row.status === '失败'">
                                <span class="status-icon undone-stutus"></span>
                                <span>{{ props.row.status }}</span>
                            </div>
                        </template>
                    </bk-table-column>
                </bk-table>
            </template>

            <empty-data v-if="showContent && !vsList.length"
                :empty-info="emptyInfo"
                :to-create-fn="toCreateFn">
            </empty-data>
        </section>
    </div>
</template>

<script>
    import emptyData from './empty-data'
    import { convertTime } from '@/utils/util'

    export default {
        components: {
            emptyData
        },
        data () {
            return {
                showContent: false,
                vsList: [],
                loading: {
                    isLoading: false,
                    title: ''
                },
                pagination: {
                    count: 1,
                    current: 1,
                    limit: 10
                },
                emptyInfo: {
                    title: '暂无扫描记录',
                    desc: '您可以在新增扫描中选择文件执行扫描'
                }
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            }
        },
        watch: {
            projectId (val) {
                this.pagination.current = 1
                this.init()
            }
        },
        async mounted () {
            await this.init()
        },
        methods: {
            async init () {
                const { loading } = this

                loading.isLoading = true
                loading.title = '数据加载中，请稍候'

                try {
                    await this.requestList()
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                } finally {
                    setTimeout(() => {
                        this.loading.isLoading = false
                    }, 1000)
                }
            },
            /**
             * 获取扫描记录列表
             */
            async requestList () {
                const page = this.pagination.current
                const pageSize = this.pagination.limit

                try {
                    const res = await this.$store.dispatch('vs/requestVsList', {
                        projectId: this.projectId,
                        page,
                        pageSize
                    })

                    this.vsList.splice(0, this.vsList.length)
                    if (res.records) {
                        res.records.forEach(item => {
                            this.vsList.push(item)
                        })
                        this.pagination.count = res.count
                    }
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }

                this.showContent = true
            },
            toRowReport (row, event, column) {
                if (row.status === '成功') {
                    this.$router.push({
                        name: 'vsReport',
                        params: {
                            projectId: this.projectId,
                            vsId: row.id
                        }
                    })
                }
            },
            rowClassFun ({ row }) {
                if (row.status === '成功') return 'canClick'
            },
            /**
             *  每页条数下拉框改变的回调函数
             */
            async pageCountChangedHandler (limit) {
                if (this.pagination.limit === limit) return
                this.pagination.limit = limit
                this.pagination.current = 1
                this.loading.isLoading = true
                await this.requestList()
                this.loading.isLoading = false
            },
            /**
             *  当前页码改变的回调函数
             */
            async pageChangedHandler (page) {
                this.pagination.current = page
                this.loading.isLoading = true
                await this.requestList()
                this.loading.isLoading = false
            },
            toCreateFn () {
                this.$router.push({
                    name: 'createVs',
                    params: {
                        projectId: this.projectId
                    }
                })
            },
            localCoverSize (row, column, cellValue, index) {
                const size = row.fileSize
                return `${((size / 1024) / 1024).toFixed(2)} MB`
            },
            /**
             * 处理时间格式
             */
            localConvertTime (row, column, cellValue, index) {
                return convertTime(row.createTime)
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import './../scss/conf';

    .vs-list-wrapper {
        overflow: auto;

        .vs-list-header {
            display: flex;
            justify-content: space-between;
            padding: 18px 20px;
            width: 100%;
            height: 60px;
            border-bottom: 1px solid $borderWeightColor;
            background-color: #fff;
            box-shadow:0px 2px 5px 0px rgba(51,60,72,0.03);

            .title {
                font-size: 16px;
            }
        }

        .vs-table-wrapper {
            border: 1px solid $borderWeightColor;
        }
    }
</style>
