<template>
    <infinite-scroll
        ref="infiniteScroll"
        :data-fetcher="fetchList"
        :page-size="defaultPageSize"
        scroll-box-class-name="pipeline-list-box"
        v-slot="slotProps"
    >
        <ul class="pipelines-card-view-list">
            <li v-for="pipeline of slotProps.list" :key="pipeline.pipelineId" @click="goHistory(pipeline.pipelineId)">
                <pipeline-card
                    :pipeline="pipeline"
                    :remove-handler="removeHandler"
                    :exec-pipeline="execPipeline"
                    :apply-permission="applyPermission"

                >
                </pipeline-card>
            </li>
        </ul>
    </infinite-scroll>
</template>

<script>
    import piplineActionMixin from '@/mixins/pipeline-action-mixin'
    import PipelineCard from '@/components/pipelineList/PipelineCard'
    import InfiniteScroll from '@/components/InfiniteScroll'

    export default {
        components: {
            PipelineCard,
            InfiniteScroll
        },
        mixins: [piplineActionMixin],
        props: {
            filterParams: {
                type: Object,
                default: () => ({})
            },
            sortType: {
                type: String,
                default: 'CREATE_TIME'
            }
        },
        data () {
            return {
                isLoading: false,
                isPatchOperate: false,
                defaultPageSize: 16,
                activePipeline: null
            }
        },
        watch: {
            '$route.params.viewId': function () {
                this.$refs.infiniteScroll?.updateList?.()
            },
            sortType: function () {
                this.$refs.infiniteScroll?.updateList?.()
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
                    sortType: this.sortType,
                    viewId: this.$route.params.viewId,
                    ...this.filterParams
                })
                console.log(res)
                return res
            },
            refresh () {
                this.$refs.infiniteScroll?.updateList?.()
            }
        }
    }

</script>

<style lang="scss">
    @import '@/scss/mixins/ellipsis';
    @import '@/scss/conf';

    .pipelines-card-view-list {
        display: grid;
        grid-template-columns: repeat(auto-fill, 408px);
        grid-gap: 24px;
        > li {
            cursor: pointer;
        }
    }
</style>
