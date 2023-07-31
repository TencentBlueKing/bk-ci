<template>
    <main class="pipeline-changelog" v-bk-loading="{ isLoading }">
        <header class="pipeline-changelog-header">
            <bk-select>
                <bk-option v-for="creator in creators" :key="creator">
                    {{ creator }}
                </bk-option>
            </bk-select>
        </header>
        <section class="pipeline-changelog-content">
            <bk-table
                height="100%"
                :data="pipelineChangeLogList"
                :pagination="pagination"
            >
                <bk-table-column v-for="column in columns" :key="column.key" v-bind="column" />
            </bk-table>
        </section>
    </main>
</template>

<script>
    import { mapActions, mapGetters } from 'vuex'
    import { convertMiniTime } from '@/utils/util'
    export default {
        data () {
            return {
                isLoading: false,
                pipelineChange: [],
                pagination: {
                    limit: 20,
                    current: 1,
                    count: 0
                },
                versionDesc: ''
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
                    prop: 'operator',
                    label: this.$t('versionNum')
                }, {
                    prop: 'operateTime',
                    label: this.$t('versionDesc'),
                    formatter: (row) => {
                        return convertMiniTime(row.createTime)
                    }
                }]
            }
        },
        created () {
            this.init()
        },
        methods: {

            ...mapActions('pipelines', [
                'requestPipelineChangelogs'
            ]),
            async init (page, limit) {
                try {
                    this.isLoading = true
                    const { projectId, pipelineId } = this.$route.params
                    const { limit: pageSize, current } = this.pagination
                    const res = await this.requestPipelineChangelogs({
                        projectId,
                        pipelineId,
                        page: page ?? current,
                        pageSize: limit ?? pageSize
                    })
                    Object.assign(this.pagination, {
                        current: res.page,
                        limit: res.pageSize,
                        count: res.count
                    })
                    this.pipelineChange = res.records
                    console.log(this.pipelineChange)
                    console.log(res)
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message ?? error
                    })
                } finally {
                    this.isLoading = false
                }
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
