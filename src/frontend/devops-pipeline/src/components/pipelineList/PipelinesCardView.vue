<template>
    <main class="pipeline-card-view-box">
        <infinite-scroll
            ref="infiniteScroll"
            :data-fetcher="fetchList"
            :page-size="defaultPageSize"
            scroll-box-class-name="pipeline-card-view-box"
            v-slot="slotProps"
        >
            <PipelineListEmpty class="pipeline-card-list-empty-tips" v-if="slotProps.list.length === 0"></PipelineListEmpty>
            <ul v-else class="pipelines-card-view-list">
                <li v-for="pipeline of slotProps.list" :key="pipeline.pipelineId">
                    <pipeline-card
                        :pipeline="pipeline"
                        :remove-handler="removeHandler"
                        :exec-pipeline="execPipeline"
                        :collect-pipeline="collectHandler"
                        :apply-permission="applyPermission"
                    >
                    </pipeline-card>
                </li>
            </ul>

        </infinite-scroll>
    </main>
</template>

<script>
    import piplineActionMixin from '@/mixins/pipeline-action-mixin'
    import PipelineCard from '@/components/pipelineList/PipelineCard'
    import InfiniteScroll from '@/components/InfiniteScroll'
    import PipelineListEmpty from '@/components/pipelineList/PipelineListEmpty'
    import { ORDER_ENUM, PIPELINE_SORT_FILED } from '@/utils/pipelineConst'
    import { isShallowEqual } from '@/utils/util'

    export default {
        components: {
            PipelineCard,
            InfiniteScroll,
            PipelineListEmpty
        },
        mixins: [piplineActionMixin],
        props: {
            filterParams: {
                type: Object,
                default: () => ({})
            }
        },
        data () {
            return {
                isLoading: false,
                isPatchOperate: false,
                defaultPageSize: 50,
                activePipeline: null
            }
        },
        computed: {
            sortField () {
                const { sortType, collation = ORDER_ENUM.descending } = this.$route.query
                return {
                    sortType: sortType ?? localStorage.getItem('pipelineSortType') ?? PIPELINE_SORT_FILED.createTime,
                    collation: collation ?? localStorage.getItem('pipelineSortCollation') ?? ORDER_ENUM.descending
                }
            }
        },
        watch: {
            '$route.params.viewId': function () {
                this.$refs.infiniteScroll?.updateList?.()
            },
            sortField: function (newSortField, oldSortField) {
                if (!isShallowEqual(newSortField, oldSortField)) {
                    this.$nextTick(() => {
                        this.$refs.infiniteScroll?.updateList?.()
                    })
                }
            },
            filterParams: function () {
                this.$refs.infiniteScroll?.updateList?.()
            }
        },
        methods: {
            async fetchList (page = 1, pageSize = this.defaultPageSize) {
                const res = await this.getPipelines({
                    page,
                    pageSize,
                    viewId: this.$route.params.viewId,
                    ...this.filterParams
                })
                return res
            },
            refresh () {
                this.$refs.infiniteScroll?.updateList?.()
            },
            requestList ({ page }) {
                return this.fetchList(page)
            }
        }
    }

</script>

<style lang="scss">
    @import '@/scss/mixins/ellipsis';
    @import '@/scss/conf';
    .pipeline-card-view-box {
        height: 100%;
        overflow: auto;
    }
    .pipeline-card-list-empty-tips {
        width: 100%;
        height: 100%;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
    }
    .pipelines-card-view-list {
        display: grid;
        grid-gap: 24px;
        grid-template-columns: repeat(auto-fill, minmax(408px, 1fr));
    }
</style>
