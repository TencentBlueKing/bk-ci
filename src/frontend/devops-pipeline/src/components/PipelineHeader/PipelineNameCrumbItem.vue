<template>
    <div class="pipeline-name-crumb">
        <bk-select
            class="pipeline-name-crumb-select"
            searchable
            :remote-method="handleSearchPipeline"
            :value="currentPipeline"
            :popover-width="222"
            @change="doSelectPipeline"
        >
            <span
                class="pipeline-name-crumb-select-trigger"
                slot="trigger"
            >
                <span @click="goHistory">{{ pipelineName }}</span>
                <div
                    class="pipeline-pac-indicator"
                    @click.stop.prevent=""
                >
                    <pac-tag
                        v-if="showPacTag && pacEnabled"
                        :info="yamlInfo"
                    />
                </div>
                <i
                    @click.prevent
                    v-bk-tooltips="$t('subpage.switchPipelineTooltips')"
                    class="devops-icon icon-shift"
                ></i>
            </span>
            <bk-option
                v-for="pipeline in pipelineList"
                :key="pipeline.pipelineId"
                :id="pipeline.pipelineId"
                :name="pipeline.pipelineName"
            >
            </bk-option>
        </bk-select>
    </div>
</template>

<script>
    import { mapState, mapGetters, mapActions } from 'vuex'
    import PacTag from '@/components/PacTag'
    import { RESOURCE_ACTION } from '@/utils/permission'

    export default {
        components: {
            PacTag
        },
        props: {
            showPacTag: {
                type: Boolean,
                default: true
            },
            pipelineName: {
                type: String,
                default: '--'
            }
        },
        data () {
            return {
                pipelineList: [],
                currentPipeline: null
            }
        },
        computed: {
            ...mapState('atom', [
                'pipelineInfo'
            ]),
            ...mapGetters({
                pacEnabled: 'atom/pacEnabled'
            }),
            yamlInfo () {
                return this.pipelineInfo?.yamlInfo
            },
            archiveFlag () {
                return this.$route.query.archiveFlag
            }
        },
        created () {
            this.fetchPipelineList()
        },
        methods: {
            ...mapActions({
                searchPipelineList: 'pipelines/searchPipelineList'
            }),
            goHistory (e) {
                if (this.$route.name !== 'pipelinesHistory') {
                    e.stopPropagation()

                    this.$router.push({
                        name: 'pipelinesHistory',
                        params: {
                            ...this.$route.params,
                            type: 'history',
                            version: this.pipelineInfo?.releaseVersion
                        },
                        query: {
                            ...(this.archiveFlag ? { archiveFlag: this.archiveFlag } : {})
                        }
                    })
                }
            },
            async fetchPipelineList () {
                const { projectId, pipelineId } = this.$route.params
                try {
                    const list = await this.search()

                    this.pipelineList = this.generatePipelineList(list)
                } catch (err) {
                    this.handleError(err, {
                        projectId,
                        resourceCode: pipelineId,
                        action: RESOURCE_ACTION.VIEW
                    })
                }
            },
            async doSelectPipeline (pipelineId) {
                try {
                    const { $route } = this
                    const name = $route.params.buildNo ? 'pipelinesHistory' : $route.name

                    this.$router.push({
                        name,
                        params: {
                            projectId: $route.params.projectId,
                            pipelineId
                        },
                        query: $route.query
                    })
                    // 清空搜索
                    const list = await this.search()
                    this.pipelineList = this.generatePipelineList(list)
                } catch (error) {
                    this.handleError(error, {
                        projectId: this.$route.params.projectId,
                        resourceCode: pipelineId,
                        action: RESOURCE_ACTION.VIEW
                    })
                }
            },
            async handleSearchPipeline (value) {
                const list = await this.search(value)
                this.pipelineList = this.generatePipelineList(list, this.pipelineInfo)
            },
            search (searchName = '') {
                return this.searchPipelineList({
                    projectId: this.$route.params.projectId,
                    searchName,
                    archiveFlag: this.$route.query.archiveFlag
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

<style lang="scss">
    .pipeline-name-crumb {
        display: inline-flex;
        &-select {
            border: 0;
            &.is-focus  {
                box-shadow: none;
            }
        }
        &-select-trigger {
            border: 0;
            display: flex;
            align-items: center;
            justify-content: center;
            grid-gap: 8px;
            font-size: 14px;
            line-height: 1;
            .devops-icon.icon-shift {
                color: #979BA5;
                display: inline-flex;
                width: 16px;
                height: 16px;
                font-size: 12px;
                background: #F0F1F5;
                border-radius: 2px;
                align-items: center;
                justify-content: center;
            }
        }
    }
</style>
