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
                <bk-badge
                    dot
                    :visible="!!constraintInfo?.upgradeFlag"
                    theme="danger"
                >
                    <bk-popover
                        v-if="instanceFromTemplate"
                        class="template-instance-tag"
                        theme="light"
                        @click.stop=""
                        :on-show="handleShowPopover"
                        :on-hide="handleHidePopover"
                    >
                    
                        <span>
                            {{ $t('constraint') }}
                        </span>
                    
                        <section
                            slot="content"
                        >
                            <p>{{ $t('template.constraintMode') }}</p>
                            <div
                                class="constraint-info-area"
                                v-bkloading="constraintInfoFetching"
                            >
                                <p>
                                    <label>{{ $t('template.name') }}</label>
                                    <a
                                        class="text-link"
                                        target="_blank"
                                        :href="constraintInfo?.templateDetailsUrl"
                                    >
                                        {{ constraintInfo?.templateName ?? '--' }}
                                    </a>
                                </p>
                                <p>
                                    <label>{{ $t('template.templateVersion') }}</label>
                                    <span v-bk-overflow-tips="constraintInfo?.templateVersionName">
                                        {{ constraintInfo?.templateVersionName ?? '--' }}
                                    </span>
                                    <a
                                        v-if="constraintInfo?.upgradeFlag"
                                        class="text-link"
                                        target="_blank"
                                        :href="constraintInfo?.upgradeUrl"
                                    >
                                        {{ $t('template.goUpgrade') }}
                                    </a>
                                </p>
                            </div>
                        </section>
                    </bk-popover>
                </bk-badge>
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
    import {
        PROCESS_API_URL_PREFIX
    } from '@/store/constants'

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
                currentPipeline: null,
                contraintApiSignal: null,
                constraintInfo: null,
                constraintInfoFetching: { isLoading: false, size: 'small' }
            }
        },
        computed: {
            ...mapState('atom', [
                'pipelineInfo'
            ]),
            ...mapGetters({
                pacEnabled: 'atom/pacEnabled',
                instanceFromTemplate: 'atom/instanceFromTemplate'
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
            this.handleShowPopover()
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
            },
            async handleShowPopover () {
                try {
                    this.constraintInfoFetching = {
                        isLoading: true,
                        size: 'small'
                    }
                    this.contraintApiSignal = new AbortController()
                    const { projectId, pipelineId, version } = this.$route.params
                    const res = await this.$ajax({
                        url: `${PROCESS_API_URL_PREFIX}/user/pipeline/template/v2/${projectId}/pipelines/${pipelineId}/versions/${version ?? this.pipelineInfo?.releaseVersion}/related/info`,
                        method: 'GET',
                        signal: this.contraintApiSignal?.signal
                    })
                    this.constraintInfo = res.data

                } catch (error) {
                    console.log(error)
                } finally {
                    this.constraintInfoFetching = {
                        isLoading: false,
                        size: 'small'
                    }
                }
            },
            handleHidePopover () {
                this.contraintApiSignal?.abort()
            }
        }
    }
</script>

<style lang="scss">
    @import "@/scss/mixins/ellipsis";
    .template-instance-tag {
        font-size: 12px;
        line-height: 20px;
        color: #4D4F56;
        background: #FAFBFD;
        border: 1px solid #DCDEE5;
        border-radius: 2px;
        padding: 0 8px;
    }
    .constraint-info-area {
        background: #F5F7FA;
        margin-top: 8px;
        border-radius: 2px;
        display: flex;
        flex-direction: column;
        padding: 4px 16px;

        gap: 6px;
        width: 280px;
        
        > p {
            height: 32px;
            display: flex;
            align-items: center;
            grid-gap: 20px;
            flex-shrink: 0;
            > span {
                @include ellipsis();
                flex: 1;
            }
        }
    }
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
