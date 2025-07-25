<template>
    <main
        class="pipeline-changelog"
        v-bkloading="{ isLoading }"
    >
        <header class="pipeline-changelog-header">
            <bk-select
                v-model="filterCreator"
                @change="getChangelogs(1)"
                :placeholder="$t('audit.operator')"
            >
                <bk-option
                    v-for="creator in operatorList"
                    :key="creator"
                    :id="creator"
                    :name="creator"
                />
            </bk-select>
        </header>
        <section
            class="pipeline-changelog-content"
            ref="tableBox"
        >
            <bk-table
                :max-height="$refs?.tableBox?.offsetHeight"
                :data="operateLogs"
                :pagination="pagination"
                @page-change="handlePageChange"
                @page-limit-change="handlePageLimitChange"
            >
                <bk-table-column
                    v-for="column in columns"
                    :key="column.key"
                    v-bind="column"
                />
            </bk-table>
        </section>
    </main>
</template>

<script>
    import { convertTime } from '@/utils/util'
    import { mapActions } from 'vuex'
    export default {
        data () {
            return {
                isLoading: false,
                operateLogs: [],
                operatorList: [],
                filterCreator: '',
                pagination: {
                    limit: 20,
                    current: 1,
                    count: 0
                }
            }
        },
        computed: {
            columns () {
                return [{
                    prop: 'operator',
                    label: this.$t('audit.operator')
                }, {
                    prop: 'operateTime',
                    label: this.$t('audit.operateTime'),
                    formatter: (row) => {
                        return convertTime(row.operateTime)
                    }
                }, {
                    prop: 'operationLogStr',
                    label: this.$t('audit.operateLogDesc')
                }]
            }
        },
        watch: {
            '$route.params.pipelineId': {
                handler (val) {
                    this.$nextTick(() => {
                        this.init()
                    })
                },
                immediate: true
            }
        },
        methods: {
            ...mapActions('pipelines', [
                'requestPipelineChangelogs',
                'requestPipelineOperatorList'
            ]),
            async getChangelogs (page, limit) {
                try {
                    this.isLoading = true
                    const { projectId, pipelineId } = this.$route.params
                    const { limit: pageSize, current } = this.pagination
                    const changeLogs = await this.requestPipelineChangelogs({
                        projectId,
                        pipelineId,
                        creator: this.filterCreator,
                        page: page ?? current,
                        pageSize: limit ?? pageSize,
                        archiveFlag: this.$route.query.archiveFlag
                    })
                    Object.assign(this.pagination, {
                        current: changeLogs.page,
                        limit: changeLogs.pageSize,
                        count: changeLogs.count
                    })
                    this.operateLogs = changeLogs.records
                } catch (error) {
                    console.log(error)
                } finally {
                    this.isLoading = false
                }
            },
            async init (page, limit) {
                try {
                    const { projectId, pipelineId } = this.$route.params

                    const [, operatorList] = await Promise.all([
                        this.getChangelogs(page, limit),
                        this.requestPipelineOperatorList({
                            projectId,
                            pipelineId,
                            archiveFlag: this.$route.query.archiveFlag
                        })
                    ])
                    this.operatorList = operatorList
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message ?? error
                    })
                }
            },
            handlePageChange (page) {
                this.pagination.current = page
                this.init(page)
            },
            handlePageLimitChange (limit) {
                this.pagination.current = 1
                this.pagination.limit = limit
                this.init(this.pagination.current, limit)
            }
        }
    }
</script>

<style lang="scss">
    .pipeline-changelog {
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
