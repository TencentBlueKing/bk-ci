<template>
    <section class="pipeline-group-section">
        <pipeline-group-aside />
        <main class="pipeline-list-main">
            <h5 class="current-pipeline-group-name">{{currentViewName}}</h5>
            <header class="pipeline-list-main-header">
                <div class="pipeline-list-main-header-left-area">
                    <bk-button theme="primary" icon="plus">{{$t('newlist.addPipeline')}}</bk-button>
                    <bk-button>{{$t('patchManage')}}</bk-button>
                </div>
                <div class="pipeline-list-main-header-right-area">
                    <pipeline-searcher
                        v-model="filters"
                        @search="filterPipelines"
                    />
                    <bk-dropdown-menu align="right">
                        <template slot="dropdown-trigger">
                            <bk-button
                                icon="icon-sort"
                            >
                            </bk-button>
                        </template>
                        <ul class="bk-dropdown-list" slot="dropdown-content">
                            <li v-for="item in sortList" :key="item.id" @click="changeSortType(item.id)">
                                <a href="javascript:;">{{ item.name }}</a>
                            </li>
                        </ul>
                    </bk-dropdown-menu>
                    <div class="bk-button-group">
                        <bk-button
                            :class="{
                                'is-selected': isTableLayout
                            }"
                            @click="switchLayout('table')"
                            icon="icon-list"
                        >
                        </bk-button>
                        <bk-button
                            :class="{
                                'is-selected': isCardLayout
                            }"
                            @click="switchLayout('card')"
                            icon="icon-apps"
                        >
                        </bk-button>
                    </div>
                </div>
            </header>
            <pipelines-card-view
                v-if="isCardLayout"
                :fetch-pipelines="getPipelines"
                :filter-params="filters"
                :sort-type="sortType"
                :pipeline-map="pipelineMap"
            />
            <pipeline-table-view
                v-else-if="isTableLayout"
                :fetch-pipelines="getPipelines"
                :filter-params="filters"
                :sort-type="sortType"
                :pipeline-map="pipelineMap"
            ></pipeline-table-view>
        </main>
        <add-to-group-dialog :add-to-dialog-show="addToDialogShow" @close="resetActivePipeline" :pipeline="activePipeline" />
        <remove-confirm-dialog
            :type="confirmType"
            :is-show="isConfirmShow"
            @close="handleCancelRemove"
            :group-name="currentViewName"
            :pipeline-list="activePipelineList"
        />
        <copy-pipeline-dialog
            :is-copy-dialog-show="isCopyDialogShow"
            :pipeline="activePipeline"
            @cancel="resetActivePipeline"
        />
        <save-as-template-dialog
            :is-save-as-template-show="isSaveAsTemplateShow"
            :pipeline="activePipeline"
            @cancel="resetActivePipeline"
        />
    </section>
</template>

<script>
    import { mapActions, mapGetters } from 'vuex'
    import webSocketMessage from '@/utils/webSocketMessage'
    import AddToGroupDialog from '@/views/PipelineList/AddToGroupDialog'
    import RemoveConfirmDialog from '@/views/PipelineList/RemoveConfirmDialog'
    import CopyPipelineDialog from '@/views/PipelineList/CopyPipelineDialog'
    import SaveAsTemplateDialog from '@/views/PipelineList/SaveAsTemplateDialog'
    import PipelineGroupAside from './PipelineGroupAside'
    import PipelineSearcher from './PipelineSearcher'
    import PipelineTableView from '@/components/pipelineList/PipelineTableView'
    import PipelinesCardView from '@/components/pipelineList/PipelinesCardView'
    import { statusAlias } from '@/utils/pipelineStatus'
    import triggerType from '@/utils/triggerType'
    import moment from 'moment'
    import {
        ALL_PIPELINE_VIEW_ID,
        COLLECT_VIEW_ID,
        MY_PIPELINE_VIEW_ID,
        DELETED_VIEW_ID,
        UNCLASSIFIED_PIPELINE_VIEW_ID
    } from '@/store/constants'

    const TABLE_LAYOUT = 'table'
    const CARD_LAYOUT = 'card'
    export default {
        components: {
            PipelineGroupAside,
            AddToGroupDialog,
            RemoveConfirmDialog,
            CopyPipelineDialog,
            SaveAsTemplateDialog,
            PipelinesCardView,
            PipelineTableView,
            PipelineSearcher
        },
        data () {
            return {
                layout: this.getLs('pipelineLayout') || TABLE_LAYOUT,
                pipelineMap: {},
                activePipeline: null,
                isConfirmShow: false,
                confirmType: '',
                activePipelineList: [],
                isSaveAsTemplateShow: false,
                isCopyDialogShow: false,
                addToDialogShow: false,
                hasTemplatePermission: false,
                sortType: this.getLs('pipelineSortType') || 'CREATE_TIME',
                filters: this.$route.query
            }
        },
        computed: {
            ...mapGetters('pipelines', [
                'groupMap'
            ]),
            isTableLayout () {
                return this.layout === TABLE_LAYOUT
            },
            isCardLayout () {
                return this.layout === CARD_LAYOUT
            },
            currentViewName () {
                return this.$t(this.groupMap?.[this.$route.params.viewId]?.name ?? '')
            },
            sortList () {
                return [
                    {
                        id: 'NAME',
                        name: this.$t('newlist.orderByAlpha')
                    }, {
                        id: 'CREATE_TIME',
                        name: this.$t('newlist.orderByCreateTime')
                    }, {
                        id: 'UPDATE_TIME',
                        name: this.$t('newlist.orderByUpdateTime')
                    }, {
                        id: 'LAST_EXEC_TIME',
                        name: this.$t('newlist.orderByExecuteTime')
                    }
                ]
            }

        },
        created () {
            this.checkHasTemplatePermission()
            moment.locale(this.$i18n.locale)
        },

        mounted () {
            webSocketMessage.installWsMessage(this.updatePipelineStatus)
        },

        beforeDestroy () {
            webSocketMessage.unInstallWsMessage()
        },

        methods: {
            ...mapActions('pipelines', [
                'requestAllPipelinesListByFilter',
                'requestToggleCollect',
                'deletePipeline',
                'requestTemplatePermission',
                'requestRecyclePipelineList'
            ]),

            getLs (key) {
                return localStorage.getItem(key) || null
            },
            switchLayout (layout) {
                this.layout = layout
                localStorage.setItem('pipelineLayout', layout)
            },
            changeSortType (sortType) {
                this.sortType = sortType
            },
            getPipelineActions (pipeline, index) {
                const isShowRemovedAction = ![
                    ALL_PIPELINE_VIEW_ID,
                    COLLECT_VIEW_ID,
                    MY_PIPELINE_VIEW_ID,
                    UNCLASSIFIED_PIPELINE_VIEW_ID
                ].includes(this.$route.params.viewId)

                return [
                    {
                        text: (pipeline.hasCollect ? this.$t('uncollect') : this.$t('collect')),
                        handler: async () => {
                            const isCollect = !pipeline.hasCollect
                            try {
                                await this.requestToggleCollect({
                                    ...pipeline,
                                    isCollect
                                })

                                this.pipelineMap[pipeline.pipelineId].hasCollect = isCollect

                                this.$showTips({
                                    message: isCollect ? this.$t('collectSuc') : this.$t('uncollectSuc'),
                                    theme: 'success'
                                })
                            } catch (err) {
                                this.$showTips({
                                    message: err.message || err,
                                    theme: 'error'
                                })
                            }
                        }
                    },
                    {
                        text: this.$t('addTo'),
                        handler: () => {
                            this.addToDialogShow = true
                            this.activePipeline = pipeline
                        }
                    },
                    {
                        text: this.$t('newlist.copyAs'),
                        handler: () => {
                            this.isCopyDialogShow = true
                            this.activePipeline = pipeline
                        }
                    },
                    {
                        text: this.$t('newlist.saveAsTemp'),
                        disable: !this.hasTemplatePermission,
                        handler: () => {
                            this.isSaveAsTemplateShow = true
                            this.activePipeline = pipeline
                        }
                    },
                    ...(pipeline.isInstanceTemplate
                        ? [{
                            text: this.$t('newlist.jumpToTemp'),
                            handler: this.jumpToTemplate,
                            isJumpToTem: true
                        }]
                        : []),
                    ...(isShowRemovedAction
                        ? [{
                            text: this.$t('removeFrom'),
                            handler: () => {
                                this.confirmType = 'remove'
                                this.isConfirmShow = true
                                this.activePipelineList = [pipeline]
                            }
                        }]
                        : []),
                    {
                        text: this.$t('delete'),
                        handler: () => {
                            this.confirmType = 'delete'
                            this.isConfirmShow = true
                            this.activePipelineList = [pipeline]
                        }
                    }
                ]
            },
            resetActivePipeline () {
                this.addToDialogShow = false
                this.isCopyDialogShow = false
                this.isSaveAsTemplateShow = false
                this.activePipeline = null
            },
            handleCancelRemove () {
                this.confirmType = ''
                this.isConfirmShow = false
            },
            async getPipelines (query = {}) {
                try {
                    const { viewId } = this.$route.params
                    let result
                    if (viewId === DELETED_VIEW_ID) {
                        result = await this.requestRecyclePipelineList({
                            projectId: this.$route.params.projectId
                        })
                    } else {
                        this.$router.push({
                            ...this.$route,
                            query: {
                                ...this.$route.query,
                                ...query
                            }
                        })
                        result = await this.requestAllPipelinesListByFilter({
                            projectId: this.$route.params.projectId,
                            ...query
                        })
                    }
                    this.pipelineMap = result.records.reduce((acc, item) => {
                        return {
                            ...acc,
                            [item.pipelineId]: {
                                ...item,
                                latestBuildStartDate: this.getLatestBuildFromNow(item.latestBuildStartTime),
                                progress: this.calcProgress(item),
                                pipelineActions: this.getPipelineActions(item),
                                trigger: triggerType[item.trigger]
                            }
                        }
                    }, {})
                    return result
                } catch (e) {
                    this.$showTips({
                        message: e.message || e,
                        theme: 'error'
                    })
                }
            },
            getLatestBuildFromNow (latestBuildStartTime) {
                return latestBuildStartTime ? moment(latestBuildStartTime).fromNow() : '--'
            },
            calcProgress ({ latestBuildStatus, lastBuildFinishCount, lastBuildTotalCount }) {
                if (latestBuildStatus === statusAlias.RUNNING) {
                    return Math.floor((lastBuildFinishCount / lastBuildTotalCount) * 100)
                }
                return ''
            },
            execPipeline (pipeline) {
                this.$router.push({
                    name: 'pipelinesPreview',
                    params: pipeline
                })
            },
            async checkHasTemplatePermission () {
                this.hasTemplatePermission = await this.requestTemplatePermission(this.$route.params.projectId)
            }
        }
    }

</script>

<style lang="scss">
    @import '@/scss/mixins/ellipsis';
    @import '@/scss/conf';

    .pipeline-group-section {
        flex: 1;
        display: flex;
        .flex-row {
            display: flex;
            align-items: center;
        }
        .pipeline-list-main {
            display: flex;
            flex-direction: column;
            flex: 1;
            padding: 24px;
            .current-pipeline-group-name {
                font-size: 14px;
                line-height: 22px;
                margin: 0 0 16px 0;

            }
            .pipeline-list-main-header {
                display: flex;
                justify-content: space-between;
                margin-bottom: 16px;
                .pipeline-list-main-header-left-area {
                    width: 300px
                }
                .pipeline-list-main-header-right-area {
                    flex: 1;
                    display: flex;
                    .search-pipeline-input {
                        flex: 1;
                        margin-right: 16px;
                    }
                }
            }
            .pipeline-latest-exec-cell {
                display: flex;
                align-items: center;
                .pipeline-exec-status-icon {
                    display: inline-flex;
                    font-size: 22px;
                    margin-right: 10px;

                }
                .pipeline-exec-msg {
                    display: flex;
                    flex-direction: column;
                    font-size: 12px;
                    line-height: 20px;
                    margin-left: 12px;
                    overflow: hidden;
                    .desc {
                        color: #979BA5;
                    }
                    .pipeline-exec-msg-title {
                        @include ellipsis();
                        flex: 1;
                    }
                }
            }
            .hidden {
                visibility: hidden;
            }
            .pipeline-operation-cell {
                display: flex;
                align-items: center;
                .pipeline-exec-btn {
                    width: 60px;
                }
                .more-action-menu {
                    font-size: 0;
                    cursor: pointer;
                    .more-action-menu-trigger {
                        font-size: 18px;
                        padding: 0 6px;
                    }
                }
            }
        }
    }

    .more-action-menu-list {
        .more-action-menu-item {
            line-height: 32px;
            cursor: pointer;
            &:hover {
                background: #E1ECFF;
            }
        }
    }
    .pipeline-group-visible-range-group {
        > :first-child {
            margin-right: 48px;
        }
    }
    .pipeline-group-box-cell {
        display: flex;
        .group-name-tag {
            @include ellipsis();
            max-width: 100px;
        }
    }
</style>
