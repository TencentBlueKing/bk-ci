<template>
    <bread-crumb :value="breadCrumbPath">
        <template v-if="pipelineList && pipelineList.length">
            <bread-crumb-item v-for="(crumb, index) in breadCrumbs" :key="index" v-bind="crumb">
                <slot v-if="index === breadCrumbs.length - 1"></slot>
            </bread-crumb-item>
        </template>
        <i v-else class="devops-icon icon-circle-2-1 spin-icon" />
    </bread-crumb>
</template>

<script>
    import { mapActions, mapGetters, mapState } from 'vuex'
    import BreadCrumb from '@/components/BreadCrumb'
    import BreadCrumbItem from '@/components/BreadCrumb/BreadCrumbItem'
    import { debounce } from '@/utils/util'

    export default {
        components: {
            BreadCrumb,
            BreadCrumbItem
        },
        data () {
            return {
                searchName: '',
                pipelineListSearching: false,
                breadCrumbPath: []
            }
        },
        computed: {
            ...mapGetters({
                pipelineList: 'pipelines/getPipelineList'
            }),
            ...mapState('pipelines', [
                'pipelineInfo'
            ]),
            breadCrumbs () {
                return [{
                    icon: 'pipeline',
                    selectedValue: this.$t('pipeline'),
                    to: {
                        name: 'pipelineListEntry'
                    }
                }, {
                    paramId: 'pipelineId',
                    paramName: 'pipelineName',
                    selectedValue: this.pipelineInfo?.pipelineName ?? '--',
                    records: this.pipelineList,
                    showTips: true,
                    tipsName: 'switch_pipeline_hint',
                    tipsContent: this.$t('subpage.switchPipelineTooltips'),
                    to: this.$route.name === 'pipelinesHistory'
                        ? null
                        : {
                            name: 'pipelinesHistory'
                        },
                    handleSelected: this.doSelectPipeline,
                    searching: this.pipelineListSearching,
                    handleSearch: debounce(this.handleSearchPipeline, 300)
                }, {
                    selectedValue: ''
                }]
            }
        },
        created () {
            this.fetchPipelineList()
        },
        methods: {
            ...mapActions('atom', [
                'requestPipeline'
            ]),
            ...mapActions('pipelines', [
                'searchPipelineList',
                'requestPipelineSummary'
            ]),
            async fetchPipelineList (searchName) {
                try {
                    const { projectId, pipelineId } = this.$route.params
                    const [list, pipelineInfo] = await Promise.all([
                        this.searchPipelineList({
                            projectId,
                            searchName
                        }),
                        this.requestPipelineSummary({
                            projectId,
                            pipelineId
                        })
                    ])

                    this.setBreadCrumbPipelineList(list, pipelineInfo)
                } catch (err) {
                    console.log(err)
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                }
            },
            async setBreadCrumbPipelineList (list, pipeline) {
                if (pipeline && list.every(ele => ele.pipelineId !== pipeline.pipelineId)) {
                    list = [
                        {
                            pipelineId: pipeline.pipelineId,
                            pipelineName: pipeline.pipelineName
                        },
                        ...list
                    ]
                }

                this.$store.commit('pipelines/updatePipelineList', list)
            },
            async doSelectPipeline (pipelineId, cur) {
                const { $route } = this
                const summary = await this.requestPipelineSummary({
                    pipelineId,
                    projectId: $route.params.projectId
                })
                this.requestPipeline({
                    pipelineId,
                    projectId: $route.params.projectId,
                    version: summary.version
                })
                // 清空搜索
                this.searchPipelineList({
                    projectId: $route.params.projectId
                }).then((list) => {
                    this.setBreadCrumbPipelineList(list, {
                        pipelineId,
                        pipelineName: cur.pipelineName
                    })
                })
                const name = $route.params.buildNo ? 'pipelinesHistory' : $route.name

                this.$router.push({
                    name,
                    params: {
                        ...this.$route.params,
                        projectId: $route.params.projectId,
                        pipelineId
                    }
                })
            },
            async handleSearchPipeline (value) {
                if (this.pipelineListSearching) return
                this.pipelineListSearching = true
                await this.fetchPipelineList(value)
                this.pipelineListSearching = false
            }
        }
    }
</script>

<style lang="scss">

</style>
