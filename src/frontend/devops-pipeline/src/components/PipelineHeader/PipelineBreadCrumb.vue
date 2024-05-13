<template>
    <bread-crumb :value="breadCrumbPath">
        <template v-if="!isLoading">
            <bread-crumb-item v-for="(crumb, index) in breadCrumbs" :key="index" v-bind="crumb">
                <slot v-if="index === breadCrumbs.length - 1"></slot>
            </bread-crumb-item>
        </template>
        <i v-else class="devops-icon icon-circle-2-1 spin-icon" />
    </bread-crumb>
</template>

<script>
    import BreadCrumb from '@/components/BreadCrumb'
    import BreadCrumbItem from '@/components/BreadCrumb/BreadCrumbItem'
    import { RESOURCE_ACTION } from '@/utils/permission'
    import { debounce } from '@/utils/util'
    import { mapActions, mapGetters, mapState } from 'vuex'

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
                pipelineListSearching: false,
                pipelineList: [],
                breadCrumbPath: []
            }
        },
        computed: {
            ...mapState('atom', [
                'pipelineSetting',
                'pipelineInfo'
            ]),
            ...mapGetters({
                pipelineHistoryViewable: 'atom/pipelineHistoryViewable'
            }),
            breadCrumbs () {
                return [{
                    icon: 'pipeline',
                    selectedValue: this.$t('pipeline'),
                    to: {
                        name: 'PipelineManageList'
                    }
                }, this.$route.name === 'pipelineImportEdit'
                    ? {
                        selectedValue: this.pipelineSetting?.pipelineName ?? '--'
                    }
                    : {
                        paramId: 'pipelineId',
                        paramName: 'pipelineName',
                        selectedValue: this.pipelineName ?? this.pipelineSetting?.pipelineName ?? '--',
                        records: this.pipelineList,
                        showTips: true,
                        tipsName: 'switch_pipeline_hint',
                        tipsContent: this.$t('subpage.switchPipelineTooltips'),
                        to: ['pipelinesHistory'].includes(this.$route.name) || !this.pipelineHistoryViewable
                            ? null
                            : {
                                name: 'pipelinesHistory',
                                params: {
                                    ...this.$route.params,
                                    type: 'history',
                                    version: this.pipelineInfo?.releaseVersion
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
            },
            '$route.params.pipelineId' (val) {
                if (val) {
                    this.requestPipelineSummary({
                        projectId: this.$route.params.projectId,
                        pipelineId: val
                    })
                }
            }
        },
        created () {
            this.setSwitchingPipelineVersion(true)
            this.fetchPipelineList()
        },
        methods: {
            ...mapActions({
                searchPipelineList: 'pipelines/searchPipelineList',
                setSwitchingPipelineVersion: 'atom/setSwitchingPipelineVersion',
                requestPipelineSummary: 'atom/requestPipelineSummary'
            }),
            async fetchPipelineList () {
                const { projectId, pipelineId } = this.$route.params
                try {
                    const [list, pipelineInfo] = await Promise.all([
                        this.search(),
                        ...(
                            pipelineId
                                ? [this.requestPipelineSummary({
                                    projectId,
                                    pipelineId
                                })]
                                : []
                        )
                    ])

                    this.pipelineList = this.generatePipelineList(list, pipelineInfo)
                } catch (err) {
                    this.handleError(err, {
                        projectId,
                        resourceCode: pipelineId,
                        action: RESOURCE_ACTION.VIEW
                    })
                }
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
                    const list = await this.search()
                    this.pipelineList = this.generatePipelineList(list, cur)
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

                const list = await this.search(value)
                this.pipelineList = this.generatePipelineList(list, this.pipelineInfo)
                this.pipelineListSearching = false
            },
            search (searchName = '') {
                return this.searchPipelineList({
                    projectId: this.$route.params.projectId,
                    searchName
                })
            },
            generatePipelineList (list, curPipeline) {
                return curPipeline
                    ? [
                        {
                            pipelineId: curPipeline.pipelineId,
                            pipelineName: curPipeline.pipelineName
                        },
                        ...list.filter(item => item.pipelineId !== curPipeline.pipelineId)
                    ]
                    : list
            }
        }
    }
</script>
