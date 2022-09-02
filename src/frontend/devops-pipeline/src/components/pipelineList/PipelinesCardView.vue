<template>
    <infinite-scroll
        ref="infiniteScroll"
        :data-fetcher="getPipelines"
        :page-size="defaultPageSize"
        scroll-box-class-name="pipeline-list"
        v-slot="slotProps"
    >
        <ul class="pipelines-card-view-list">
            <li v-for="pipeline of slotProps.list" :key="pipeline.pipelineId">
                <pipeline-card :pipeline="pipeline"></pipeline-card>
            </li>
        </ul>
    </infinite-scroll>
</template>

<script>
    // import { mapActions, mapGetters } from 'vuex'
    import PipelineCard from '@/components/pipelineList/PipelineCard'
    import InfiniteScroll from '@/components/InfiniteScroll'

    export default {
        components: {
            PipelineCard,
            InfiniteScroll
        },
        props: {
            filterParams: {
                type: Object,
                default: () => ({})
            },
            sortType: {
                type: String,
                default: 'CREATE_TIME'
            },
            fetchPipelines: {
                type: Function,
                required: true
            }
        },
        data () {
            return {
                isLoading: false,
                isPatchOperate: false,
                defaultPageSize: 60,
                activePipeline: null
            }
        },
        watch: {
            '$route.params.viewId': function () {
                this.$nextTick(this.getPipelines)
            },
            sortType: function () {
                this.$nextTick(this.getPipelines)
            },
            filterParams: function () {
                this.$nextTick(this.getPipelines)
            }
        },

        methods: {
            getPipelines (page = 1, pageSize = this.defaultPageSize) {
                return this.fetchPipelines({
                    page,
                    pageSize,
                    sortType: this.sortType,
                    viewId: this.$route.params.viewId,
                    ...this.filterParams
                })
            }
        }
    }

</script>

<style lang="scss">
    @import '@/scss/mixins/ellipsis';
    @import '@/scss/conf';

    .pipelines-card-view-list {
        display: grid;
        grid-template-columns: repeat(auto-fill, 300px);
        grid-gap: 24px;
    }
</style>
