<template>
    <section style="width: 100%" v-bkloading="{ isLoading }">
        <div class="download-statics">
            共 {{ downloadInfo.total }} 人下载 {{ downloadInfo.account }} 次
        </div>
        <bk-table v-if="list.length"
            ref="downloadTable"
            class="download-statics-table"
            :data="list"
            :pagination="pagination"
            @page-change="handlePageChange"
            @page-limit-change="handleLimitChange"
        >
            <bk-table-column label="用户名" prop="userId"></bk-table-column>
            <bk-table-column label="下载次数" prop="times"></bk-table-column>
            <bk-table-column label="最近下载时间" prop="latestTime" :formatter="formatTime"></bk-table-column>
        </bk-table>
    </section>
</template>
    
<script>
    import { mapActions } from 'vuex'
    export default {
        name: 'download-data',
        props: {
            downloadInfo: {
                type: Object
            },
            localConvertTime: {
                type: Function
            }
        },
        data () {
            return {
                list: [],
                isLoading: false,
                pagination: {
                    current: 1,
                    count: 1,
                    limit: 15,
                    limitList: [10, 15, 20, 25, 30]
                }
            }
        },
        created () {
            this.requestList()
        },
        methods: {
            ...mapActions('experience', [
                'requestDownloadUserCount'
            ]),
            async requestList () {
                try {
                    const { $route, requestDownloadUserCount } = this
                    this.isLoading = true
                    const res = await requestDownloadUserCount({
                        projectId: $route.params.projectId,
                        experienceHashId: $route.params.experienceId
                    })

                    this.downloadList = [
                        ...res.records
                    ]
                    this.pagination.count = this.downloadList.length

                    this.updateList({
                        start: 0,
                        end: this.pagination.limit
                    })
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                } finally {
                    this.isLoading = false
                }
            },
            updateList ({ start = 0, end }) {
                this.list = this.downloadList.slice(start, end)
            },
            /**
             *  每页条数下拉框改变的回调函数
             */
            handlePageChange (page) {
                this.pagination.current = page
                const { pagination } = this
                const start = pagination.limit * (pagination.current - 1)
                const end = start + pagination.limit > pagination.count ? pagination.count : start + pagination.limit
                this.updateList({
                    start,
                    end
                })
            },
            /**
             *  当前页码改变的回调函数
             */
            handleLimitChange (limit) {
                this.pagination.limit = limit
                this.handlePageChange(1)
            },
            formatTime (row) {
                return this.localConvertTime(row.latestTime)
            }
        }
    }
</script>

<style lang="scss" scoped>
    .download-statics {
        margin-bottom: 20px;
        font-size: 14px;
    }
    .download-statics-table {
        width: 100%;
    }
</style>
