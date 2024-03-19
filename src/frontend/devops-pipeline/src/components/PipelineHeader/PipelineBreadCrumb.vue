<template>
    <bread-crumb :value="breadCrumbPath">
        <template v-if="pipelineList?.length && !isLoading">
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
    import { RESOURCE_ACTION } from '@/utils/permission'
    import { debounce } from '@/utils/util'

    export default {
        components: {
            BreadCrumb,
            BreadCrumbItem
        },
        props: {
            showRecordEntry: Boolean,
            pipelineName: String,
            isLoading: Boolean
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
            ...mapState('atom', [
                'pipeline',
                'pipelineInfo'
            ]),
            breadCrumbs () {
                console.log('this.pipelineName', this.pipeline)
                return [{
                    icon: 'pipeline',
                    selectedValue: this.$t('pipeline'),
                    to: {
                        name: 'pipelineListEntry'
                    }
                }, this.$route.name === 'pipelineImportEdit'
                    ? {
                        selectedValue: this.pipeline?.name ?? '--'
                    }
                    : {
                        paramId: 'pipelineId',
                        paramName: 'pipelineName',
                        selectedValue: this.pipelineName ?? this.pipeline?.name ?? '--',
                        records: this.pipelineList,
                        showTips: true,
                        tipsName: 'switch_pipeline_hint',
                        tipsContent: this.$t('subpage.switchPipelineTooltips'),
                        to: ['pipelinesHistory'].includes(this.$route.name)
                            ? null
                            : {
                                name: 'pipelinesHistory',
                                params: {
                                    ...this.$route.params,
                                    type: 'history',
                                    version: this.$route.params.version ?? this.pipelineInfo?.releaseVersion
                                }
                            },
                        handleSelected: this.doSelectPipeline,
                        searching: this.pipelineListSearching,
                        handleSearch: debounce(this.handleSearchPipeline, 1000)
                    }, ...(this.showRecordEntry
                    ? [{
                        selectedValue: this.$t('draftExecRecords'),
                        to: {
                            name: 'draftDebugRecord',
                            params: {
                                ...this.$route.params,
                                version: this.pipelineInfo?.version
                            }
                        }
                    }]
                    : []), {
                    selectedValue: ''
                }]
            }
        },
        watch: {
            'pipelineInfo.pipelineName': {
                handler (val) {
                    const title = val ? `${val} | ${this.$t('pipeline')}` : this.$t('documentTitlePipeline')
                    this.$updateTabTitle?.(title)
                },
                immediate: true
            }
        },
        created () {
            this.fetchPipelineList()
        },
        methods: {
            ...mapActions({
                searchPipelineList: 'pipelines/searchPipelineList',
                requestPipelineSummary: 'atom/requestPipelineSummary'
            }),
            async fetchPipelineList (searchName) {
                const { projectId, pipelineId } = this.$route.params
                try {
                    const [list, pipelineInfo] = await Promise.all([
                        this.searchPipelineList({
                            projectId,
                            searchName
                        }),
                        ...(pipelineId
                            ? [this.requestPipelineSummary({
                                projectId,
                                pipelineId
                            })]
                            : [])
                    ])

                    this.setBreadCrumbPipelineList(list, pipelineInfo ?? this.pipelineInfo)
                } catch (err) {
                    this.handleError(err, {
                        projectId,
                        resourceCode: pipelineId,
                        action: RESOURCE_ACTION.VIEW
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
                try {
                    const { $route } = this
                    const name = $route.params.buildNo ? 'pipelinesHistory' : $route.name

                    this.$router.push({
                        name,
                        params: {
                            ...$route.params,
                            projectId: $route.params.projectId,
                            pipelineId
                        }
                    })
                    await this.requestPipelineSummary({
                        pipelineId,
                        projectId: $route.params.projectId
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
                } catch (error) {
                    this.handleError(error, {
                        projectId: this.$route.params.projectId,
                        resourceCode: pipelineId,
                        action: RESOURCE_ACTION.VIEW
                    })
                }
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
