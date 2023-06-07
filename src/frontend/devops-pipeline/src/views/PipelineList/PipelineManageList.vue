<template>
    <main class="pipeline-list-main">
        <div class="recycle-bin-header" v-if="isDeleteView">
            <h5>{{$t('restore.recycleBin')}}</h5>
            <bk-input :placeholder="$t('restore.restoreSearchTips')" />
        </div>
        <template v-else>

            <h5 class="current-pipeline-group-name">
                <bk-tag v-bk-tooltips="pipelineGroupType.tips" v-if="pipelineGroupType" type="stroke">{{ pipelineGroupType.label }}</bk-tag>
                <span>{{currentViewName}}</span>
            </h5>
            <header class="pipeline-list-main-header">
                <div>
                    <bk-dropdown-menu trigger="click">
                        <bk-button theme="primary" icon="plus" slot="dropdown-trigger">
                            {{$t('newlist.addPipeline')}}
                        </bk-button>
                        <ul class="bk-dropdown-list" slot="dropdown-content">
                            <li v-for="(item, index) of newPipelineDropdown" :key="index">
                                <a href="javascript:;" @click="item.action">{{ item.text }}</a>
                            </li>
                        </ul>
                    </bk-dropdown-menu>
                    <span v-bk-tooltips="noManagePermissionTips">
                        <bk-button
                            v-if="pipelineGroupType"
                            @click="handleAddToGroup"
                            :disabled="canNotMangeProjectedGroup"
                        >
                            {{$t('pipelineCountEdit')}}
                        </bk-button>
                    </span>
                    <bk-button @click="goPatchManage">{{$t('patchManage')}}</bk-button>
                </div>
                <div class="pipeline-list-main-header-right-area">
                    <pipeline-searcher
                        v-if="allPipelineGroup.length"
                        v-model="filters"
                    />
                    <bk-dropdown-menu trigger="click" class="pipeline-sort-dropdown-menu" align="right">
                        <template slot="dropdown-trigger">
                            <bk-button class="icon-button">
                                <logo :name="currentSortIconName" size="12" />
                            </bk-button>
                        </template>
                        <ul class="bk-dropdown-list" slot="dropdown-content">
                            <li
                                v-for="item in sortList"
                                :key="item.id"
                                :active="item.active"
                                @click="changeSortType(item.id)"
                            >
                                <a class="pipeline-sort-item" href="javascript:;">
                                    {{ item.name }}
                                    <logo class="pipeline-sort-item-icon" :name="item.sortIcon" size="12" />
                                </a>
                            </li>
                        </ul>
                    </bk-dropdown-menu>
                    <div class="bk-button-group">
                        <bk-button
                            :class="{
                                'icon-button': true,
                                'is-selected': isTableLayout
                            }"
                            @click="switchLayout('table')"
                        >
                            <logo name="list" size="14" />
                        </bk-button>
                        <bk-button
                            :class="{
                                'icon-button': true,
                                'is-selected': isCardLayout
                            }"
                            @click="switchLayout('card')"
                        >
                            <logo name="card" size="14" />
                        </bk-button>
                    </div>
                </div>
            </header>
        </template>
        <div class="pipeline-list-box">
            <pipeline-table-view
                v-if="isTableLayout"
                :filter-params="filters"
                ref="pipelineBox"
            />
            <pipelines-card-view
                v-else-if="isCardLayout"
                :filter-params="filters"
                ref="pipelineBox"
            />

        </div>
        <add-to-group-dialog
            :add-to-dialog-show="pipelineActionState.addToDialogShow"
            :pipeline="pipelineActionState.activePipeline"
            @close="closeAddToDialog"
            @done="refresh"
        />
        <remove-confirm-dialog
            :type="pipelineActionState.confirmType"
            :is-show="pipelineActionState.isConfirmShow"
            :group-name="currentViewName"
            :group-id="$route.params.viewId"
            :pipeline-list="pipelineActionState.activePipelineList"
            @close="closeRemoveConfirmDialog"
            @done="refresh"
        />
        <copy-pipeline-dialog
            :is-copy-dialog-show="pipelineActionState.isCopyDialogShow"
            :pipeline="pipelineActionState.activePipeline"
            @cancel="closeCopyDialog"
            @done="refresh"
        />
        <save-as-template-dialog
            :is-save-as-template-show="pipelineActionState.isSaveAsTemplateShow"
            :pipeline="pipelineActionState.activePipeline"
            @cancel="closeSaveAsDialog"
            @done="refresh"
        />
        <pipeline-template-popup
            :toggle-popup="toggleTemplatePopup"
            :is-show.sync="templatePopupShow"
        />
        <import-pipeline-popup
            :is-show.sync="importPipelinePopupShow"
        />
        <pipeline-group-edit-dialog
            :group="activeGroup"
            @close="handleCloseEditCount"
            @done="refresh"
        />
    </main>
</template>
<script>
    import { mapActions, mapState } from 'vuex'
    import webSocketMessage from '@/utils/webSocketMessage'
    import AddToGroupDialog from '@/views/PipelineList/AddToGroupDialog'
    import RemoveConfirmDialog from '@/views/PipelineList/RemoveConfirmDialog'
    import CopyPipelineDialog from '@/components/PipelineActionDialog/CopyPipelineDialog'
    import SaveAsTemplateDialog from '@/components/PipelineActionDialog/SaveAsTemplateDialog'
    import PipelineSearcher from './PipelineSearcher'
    import PipelineTableView from '@/components/pipelineList/PipelineTableView'
    import PipelinesCardView from '@/components/pipelineList/PipelinesCardView'
    import PipelineTemplatePopup from '@/components/pipelineList/PipelineTemplatePopup'
    import ImportPipelinePopup from '@/components/pipelineList/ImportPipelinePopup'
    import PipelineGroupEditDialog from '@/views/PipelineList/PipelineGroupEditDialog'

    import piplineActionMixin from '@/mixins/pipeline-action-mixin'
    import Logo from '@/components/Logo'
    import { PIPELINE_SORT_FILED, ORDER_ENUM } from '@/utils/pipelineConst'
    import { bus, ADD_TO_PIPELINE_GROUP } from '@/utils/bus'
    import { getCacheViewId } from '@/utils/util'
    import {
        ALL_PIPELINE_VIEW_ID,
        DELETED_VIEW_ID
    } from '@/store/constants'

    const TABLE_LAYOUT = 'table'
    const CARD_LAYOUT = 'card'
    export default {
        components: {
            Logo,
            AddToGroupDialog,
            RemoveConfirmDialog,
            CopyPipelineDialog,
            SaveAsTemplateDialog,
            PipelinesCardView,
            PipelineTableView,
            PipelineSearcher,
            PipelineTemplatePopup,
            ImportPipelinePopup,
            PipelineGroupEditDialog
        },
        mixins: [piplineActionMixin],
        data () {
            const { page, pageSize, sortType, collation, ...restQuery } = this.$route.query
            return {
                layout: this.getLs('pipelineLayout') || TABLE_LAYOUT,
                hasCreatePermission: false,
                filters: restQuery,
                templatePopupShow: false,
                importPipelinePopupShow: false,
                activeGroup: null,
                newPipelineDropdown: [{
                    text: this.$t('newPipelineFromTemplateLabel'),
                    action: this.toggleTemplatePopup
                }, {
                    text: this.$t('newPipelineFromJSONLabel'),
                    action: this.toggleImportPipelinePopup
                }]
            }
        },
        computed: {
            ...mapState('pipelines', [
                'allPipelineGroup',
                'pipelineActionState',
                'isManage'
            ]),
            isAllPipelineView () {
                return this.$route.params.viewId === ALL_PIPELINE_VIEW_ID
            },
            isDeleteView () {
                return this.$route.params.viewId === DELETED_VIEW_ID
            },
            isTableLayout () {
                return this.isDeleteView || this.layout === TABLE_LAYOUT
            },
            isCardLayout () {
                return this.layout === CARD_LAYOUT
            },
            currentGroup () {
                return this.groupMap?.[this.$route.params.viewId]
            },
            currentViewName () {
                return this.currentGroup?.i18nKey ? this.$t(this.currentGroup.i18nKey) : (this.currentGroup?.name ?? '')
            },
            canNotMangeProjectedGroup () {
                return this.currentGroup?.projected && !this.isManage
            },
            pipelineGroupType () {
                if (this.currentGroup?.viewType > 0) {
                    const typeAlias = ['', 'dynamic', 'static']
                    const tips = this.currentGroup?.viewType === 1 ? 'dynamicGroupTips' : 'staticGroupTips'

                    return {
                        label: this.$t(typeAlias[this.currentGroup?.viewType ?? 0]),
                        tips: this.$t(tips)
                    }
                }
                return null
            },
            noManagePermissionTips () {
                return {
                    content: this.$t('groupEditDisableTips'),
                    disabled: !this.canNotMangeProjectedGroup
                }
            },
            sortList () {
                return [
                    {
                        id: PIPELINE_SORT_FILED.pipelineName,
                        name: this.$t('newlist.orderByAlpha')
                    }, {
                        id: PIPELINE_SORT_FILED.createTime,
                        name: this.$t('newlist.orderByCreateTime')
                    }, {
                        id: PIPELINE_SORT_FILED.updateTime,
                        name: this.$t('newlist.orderByUpdateTime')
                    }, {
                        id: PIPELINE_SORT_FILED.latestBuildStartDate,
                        name: this.$t('newlist.orderByExecuteTime')
                    }
                ].map(sort => ({
                    ...sort,
                    active: this.isActiveSort(sort.id),
                    sortIcon: this.getSortIconName(sort.id)
                }))
            },
            currentSortIconName () {
                return this.getSortIconName(this.$route.query.sortType)
            }

        },
        watch: {
            '$route.params.projectId': function () {
                this.filters = {}
                this.$nextTick(() => {
                    if (!this.isAllPipelineView) {
                        this.goList()
                    } else {
                        this.$refs.pipelineBox?.requestList?.({
                            page: 1,
                            pageSize: 50
                        })
                    }
                    this.checkHasCreatePermission()
                    this.handleCloseEditCount()
                    this.templatePopupShow = false
                })
            },
            '$route.params.viewId': function () {
                this.filters = {}
            }
        },
        created () {
            this.goList()
            this.checkHasCreatePermission()
        },

        mounted () {
            webSocketMessage.installWsMessage(this.$refs.pipelineBox?.updatePipelineStatus)
            bus.$off(ADD_TO_PIPELINE_GROUP, this.handleAddToGroup)
            bus.$on(ADD_TO_PIPELINE_GROUP, this.handleAddToGroup)
        },

        beforeDestroy () {
            webSocketMessage.unInstallWsMessage()
            bus.$off(ADD_TO_PIPELINE_GROUP, this.handleAddToGroup)
        },

        methods: {
            ...mapActions('pipelines', [
                'requestHasCreatePermission'
            ]),
            isActiveSort (sortType) {
                return this.$route.query.sortType === sortType
            },
            getSortIconName (sortType) {
                if (this.isActiveSort(sortType) && this.$route.query.collation) {
                    return `sort-${this.$route.query.collation.toLowerCase()}`
                }
                return 'sort'
            },
            goList () {
                if (!this.$route.params.viewId) {
                    const viewId = getCacheViewId(this.$route.params.projectId)
                    this.$router.replace({
                        name: 'PipelineManageList',
                        params: {
                            ...this.$route.params,
                            viewId
                        }
                    })
                } else {
                    this.$refs.pipelineBox?.requestList?.({
                        page: 1,
                        pageSize: 50
                    })
                }
            },
            goPatchManage () {
                this.$router.push({
                    name: 'patchManageList'
                })
            },
            getLs (key) {
                return localStorage.getItem(key) || null
            },
            switchLayout (layout) {
                this.layout = layout
                localStorage.setItem('pipelineLayout', layout)
            },
            changeSortType (sortType) {
                const { sortType: currentSort, collation, ...restQuery } = this.$route.query
                const newSortQuery = {
                    ...restQuery,
                    sortType,
                    collation
                }

                if (sortType === currentSort) {
                    newSortQuery.collation = collation === ORDER_ENUM.descending ? ORDER_ENUM.ascending : ORDER_ENUM.descending
                } else {
                    switch (sortType) {
                        case PIPELINE_SORT_FILED.pipelineName:
                            newSortQuery.collation = ORDER_ENUM.ascending
                            break
                        case PIPELINE_SORT_FILED.createTime:
                        case PIPELINE_SORT_FILED.updateTime:
                        case PIPELINE_SORT_FILED.latestBuildStartDate:
                            newSortQuery.collation = ORDER_ENUM.descending
                            break
                    }
                }
                localStorage.setItem('pipelineSortType', sortType)
                localStorage.setItem('pipelineSortCollation', newSortQuery.collation)

                this.$router.push({
                    ...this.$route,
                    query: newSortQuery
                })
            },

            async checkHasCreatePermission () {
                const res = await this.requestHasCreatePermission(this.$route.params)
                this.hasCreatePermission = res
            },

            toggleTemplatePopup () {
                if (!this.hasCreatePermission) {
                    this.toggleCreatePermission()
                } else {
                    this.templatePopupShow = !this.templatePopupShow
                }
            },
            handleAddToGroup () {
                if (this.currentGroup) {
                    this.activeGroup = this.currentGroup
                }
            },

            handleCloseEditCount () {
                this.activeGroup = null
            },

            toggleImportPipelinePopup () {
                this.importPipelinePopupShow = !this.importPipelinePopupShow
            },

            toggleCreatePermission () {
                this.setPermissionConfig(this.$permissionResourceMap.pipeline, this.$permissionActionMap.create)
            },
            refresh () {
                this.$refs.pipelineBox?.refresh?.()
            }
        }
    }

</script>

<style lang="scss">
    @import '@/scss/mixins/ellipsis';
    @import '@/scss/conf';
    .recycle-bin-header {
        display: grid;
        grid-template-columns: 1fr 6fr;
        align-items: center;
        > h5 {
            color: #313238;
        }
    }
    .pipeline-sort-dropdown-menu {
        margin: 0 8px;
        .bk-dropdown-list {
            a.pipeline-sort-item {
                display: flex;
                align-items: center;
                justify-content: space-between;
                .pipeline-sort-item-icon {
                    margin-left: 6px;
                }
            }
            [active],
            [active]:hover {
                > a.pipeline-sort-item {
                    color: $primaryColor;
                 }
            }
        }
    }
    // TODO: hack
    .icon-button {
        min-width: auto;
        width: 32px;
        padding: 0;
        > div {
            display: flex;
            align-items: center;
            justify-content: center;
            > span {
                display: flex;
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
        width: 100%;
        flex-wrap: wrap;
    }
    .pipeline-group-name-tag {
        @include ellipsis();
        flex-shrink: 0;
        max-width: 100px;
    }
    .pipeline-list-box {
        flex: 1;
        overflow: hidden;
    }
</style>
