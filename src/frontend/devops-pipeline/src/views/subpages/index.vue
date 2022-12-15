<template>
    <div class="biz-container bkdevops-history-subpage pipeline-subpages">
        <inner-header class="customer-inner-header">
            <div class="history-bread-crumb" slot="left">
                <bread-crumb class="bread-crumb-comp" separator="/" :value="breadCrumbPath">
                    <template v-if="pipelineList && pipelineList.length">
                        <bread-crumb-item v-for="(crumb, index) in breadCrumbs" :key="index" v-bind="crumb">
                            <div class="build-num-switcher" v-if="$route.name === 'pipelinesDetail' && index === breadCrumbs.length - 1">
                                <template v-if="execDetail">
                                    <span>#{{ execDetail.buildNum }}</span>
                                    <p>
                                        <i class="devops-icon icon-angle-up" :disabled="execDetail.latestBuildNum === execDetail.buildNum || isLoading" @click="switchBuildNum(1)" />
                                        <i class="devops-icon icon-angle-down" :disabled="1 === execDetail.buildNum || isLoading" @click="switchBuildNum(-1)" />
                                    </p>
                                    <i class="devops-icon icon-txt" :title="$t('history.completedLog')" @click="showLog"></i>
                                </template>
                            </div>
                            <version-sideslider v-else-if="$route.name === 'pipelinesEdit' && index === breadCrumbs.length - 1"></version-sideslider>
                        </bread-crumb-item>
                    </template>
                    <i v-else class="devops-icon icon-circle-2-1 spin-icon" />
                </bread-crumb>

            </div>
            <template v-if="$route.name === 'pipelinesPreview'" slot="right">
                <router-link :to="{ name: 'pipelinesEdit' }"><bk-button>{{ $t('edit') }}</bk-button></router-link>
                <bk-button :disabled="btnDisabled" :icon="executeStatus ? 'loading' : ''" theme="primary" @click="startExcuete">
                    {{ $t('exec') }}
                </bk-button>
            </template>
            <template v-else slot="right">
                <bk-button v-if="isEditPage" @click="save" :disabled="saveBtnDisabled" :icon="saveStatus ? 'loading' : ''" theme="primary">
                    {{ $t('save') }}
                </bk-button>
                <router-link v-else :to="{ name: 'pipelinesEdit' }"><bk-button>{{ $t('edit') }}</bk-button></router-link>
                <triggers
                    class="bkdevops-header-trigger-btn"
                    :pipeline-id="pipelineId"
                    :status="pipelineStatus"
                    :can-manual-startup="canManualStartup"
                    :before-exec="isSaveAndRun ? save : undefined"
                    @exec="toExecute">
                    <section slot="exec-bar" slot-scope="triggerProps">
                        <bk-button v-if="pipelineStatus !== 'running'" theme="primary" :disabled="btnDisabled || !canManualStartup || triggerProps.isDisable" :icon="executeStatus || triggerProps.isDisable ? 'loading' : ''" :title="canManualStartup ? '' : '不支持手动启动流水线'">
                            {{ isSaveAndRun ? $t('subpage.saveAndExec') : $t('exec') }}
                        </bk-button>
                    </section>
                </triggers>

                <div :class="{ 'more-operation-entry': true }">
                    <show-tooltip placement="bottom-end" :content="$t('subpage.saveTempTooltips')" key="more_operation" name="more_operation" style="z-index: 1">
                        <div class="entry-btn">
                            <i class="entry-circle" v-for="i in [1, 2, 3]" :key="i" />
                        </div>
                    </show-tooltip>
                    <div class="more-operation-dropmenu">
                        <ul v-for="(parent, index) in actionConfMenus" :key="index">
                            <li
                                v-for="action in parent"
                                v-if="!action.hidden"
                                :key="action.label"
                                @click="action.handler"
                            >
                                {{ $t(action.label) }}
                            </li>
                        </ul>
                    </div>
                </div>
            </template>
        </inner-header>
        <router-view class="biz-content" v-bkloading="{ isLoading }"></router-view>
        <portal-target name="artifactory-popup"></portal-target>

        <export-dialog :is-show.sync="showExportDialog"></export-dialog>
        <rename-dialog
            :is-show="isRenameDialogShow"
            :project-id="projectId"
            v-bind="curPipeline"
            @close="toggleRenameDialog"
            @done="renameDone"
        />
        <copy-pipeline-dialog
            :is-copy-dialog-show="pipelineActionState.isCopyDialogShow"
            :pipeline="pipelineActionState.activePipeline"
            @cancel="closeCopyDialog"
        />
        <save-as-template-dialog
            :is-save-as-template-show="pipelineActionState.isSaveAsTemplateShow"
            :pipeline="pipelineActionState.activePipeline"
            @cancel="closeSaveAsDialog"
        />
        <import-pipeline-popup :handle-import-success="handleImportModifyPipeline" :is-show.sync="showImportDialog"></import-pipeline-popup>
        <remove-confirm-dialog
            :type="pipelineActionState.confirmType"
            :is-show="pipelineActionState.isConfirmShow"
            :pipeline-list="pipelineActionState.activePipelineList"
            @close="closeRemoveConfirmDialog"
            @done="goHome"
        />
    </div>
</template>

<script>
    import { mapGetters, mapActions, mapState } from 'vuex'
    import BreadCrumb from '@/components/BreadCrumb'
    import BreadCrumbItem from '@/components/BreadCrumb/BreadCrumbItem'
    import innerHeader from '@/components/devops/inner_header'
    import triggers from '@/components/pipeline/triggers'
    import { bus } from '@/utils/bus'
    import pipelineOperateMixin from '@/mixins/pipeline-operate-mixin'
    import pipelineActionMixin from '@/mixins/pipeline-action-mixin'
    import ImportPipelinePopup from '@/components/pipelineList/ImportPipelinePopup'
    import showTooltip from '@/components/common/showTooltip'
    import exportDialog from '@/components/ExportDialog'
    import versionSideslider from '@/components/VersionSideslider'
    import { debounce, navConfirm } from '@/utils/util'
    import CopyPipelineDialog from '@/components/PipelineActionDialog/CopyPipelineDialog'
    import SaveAsTemplateDialog from '@/components/PipelineActionDialog/SaveAsTemplateDialog'
    import RenameDialog from '@/components/PipelineActionDialog/RenameDialog'
    import RemoveConfirmDialog from '@/views/PipelineList/RemoveConfirmDialog'

    export default {
        components: {
            innerHeader,
            triggers,
            BreadCrumb,
            showTooltip,
            BreadCrumbItem,
            exportDialog,
            versionSideslider,
            CopyPipelineDialog,
            ImportPipelinePopup,
            SaveAsTemplateDialog,
            RenameDialog,
            RemoveConfirmDialog
        },
        mixins: [pipelineActionMixin, pipelineOperateMixin],
        data () {
            return {
                tabMap: {
                    trendData: this.$t('history.trendData')
                },
                isRenameDialogShow: false,
                pipelineListSearching: false,
                breadCrumbPath: [],
                isLoading: false,
                hasNoPermission: false,
                showExportDialog: false,
                showImportDialog: false
            }
        },
        computed: {
            ...mapState('pipelines', [
                'pipelineActionState'
            ]),
            ...mapState('atom', [
                'execDetail',
                'editingElementPos',
                'isPropertyPanelVisible',
                'showReviewDialog'
            ]),
            ...mapGetters({
                isEditing: 'atom/isEditing',
                getAllElements: 'atom/getAllElements'
            }),
            isEditPage () {
                return this.$route.name === 'pipelinesEdit'
            },
            isSaveAndRun () {
                return this.isEditing && this.isEditPage && !this.saveBtnDisabled
            },
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            },
            actionConfMenus () {
                const pipeline = {
                    ...this.curPipeline,
                    projectId: this.projectId
                }
                return [
                    [
                        {
                            label: 'rename',
                            handler: () => {
                                this.toggleRenameDialog(true)
                            }
                        },
                        {
                            label: this.curPipeline.hasCollect ? 'uncollect' : 'collect',
                            handler: this.toggleCollect
                        }
                    ],
                    [
                        {
                            label: 'newlist.exportPipelineJson',
                            handler: this.exportPipeline
                        },
                        {
                            label: 'newlist.importModifyPipelineJson',
                            handler: this.importModifyPipeline,
                            hidden: this.isTemplatePipeline
                        },
                        {
                            label: 'newlist.copyAs',
                            handler: () => this.copyAs(pipeline)
                        }, {
                            label: 'newlist.saveAsTemp',
                            handler: () => this.saveAsTempHandler(pipeline)
                        },
                        {
                            label: 'newlist.jumpToTemp',
                            handler: this.jumpToTemplate,
                            hidden: !this.isTemplatePipeline
                        },
                        {
                            label: 'delete',
                            handler: () => this.deleteHandler(this.curPipeline)
                        }
                    ]
                ]
            },
            btnDisabled () {
                return this.saveStatus || this.executeStatus
            },
            saveBtnDisabled () {
                return this.saveStatus || this.executeStatus || Object.keys(this.pipelineSetting).length === 0
            },
            canManualStartup () {
                return this.curPipeline ? this.curPipeline.canManualStartup : false
            },
            pipelineStatus () {
                return this.canManualStartup ? 'ready' : 'disable'
            },
            curItemTab () {
                return this.$route.params.type || 'executeDetail'
            },
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
                    selectedValue: this.curPipeline.pipelineName || '--',
                    records: [
                        ...this.pipelineList
                    ],
                    showTips: true,
                    tipsName: 'switch_pipeline_hint',
                    tipsContent: this.$t('subpage.switchPipelineTooltips'),
                    to: this.$route.name === 'pipelinesHistory'
                        ? null
                        : {
                            name: 'pipelinesHistory'
                        },
                    handleSelected: this.handleSelected,
                    searching: this.pipelineListSearching,
                    handleSearch: debounce(this.handleSearchPipeline, 300)
                }, {
                    selectedValue: this.$route.params.type && this.tabMap[this.$route.params.type] ? this.tabMap[this.$route.params.type] : this.$t(this.$route.name)
                }]
            }

        },

        created () {
            this.fetchPipelineList()
            this.$store.dispatch('requestProjectDetail', { projectId: this.projectId })
        },
        beforeDestroy () {
            this.$store.commit('pipelines/updateCurPipeline', {})
            this.$store.commit('pipelines/updatePipelineList', [])
        },
        methods: {
            ...mapActions('atom', [
                'requestPipelineExecDetailByBuildNum',
                'togglePropertyPanel',
                'setEditFrom'
            ]),
            toggleRenameDialog (show = false) {
                this.isRenameDialogShow = show
            },
            goHome () {
                this.$router.push({
                    name: 'PipelineManageList'
                })
            },
            renameDone (name) {
                this.$nextTick(() => {
                    this.updateCurPipelineByKeyValue('pipelineName', name)
                    this.pipelineSetting && Object.keys(this.pipelineSetting).length && this.updatePipelineSetting({
                        container: this.pipelineSetting,
                        param: {
                            pipelineName: name
                        }
                    })
                })
            },
            handleSelected (pipelineId, cur) {
                if (this.isEditing) {
                    navConfirm({ content: this.$t('editPage.confirmMsg'), type: 'warning' }).then(() => {
                        this.doSelectPipeline(pipelineId, cur)
                    }).catch(() => {
                        // prevent select
                    })
                } else {
                    this.doSelectPipeline(pipelineId, cur)
                }
            },
            doSelectPipeline (pipelineId, cur) {
                const { projectId, $route } = this
                this.updateCurPipeline({
                    pipelineId,
                    projectId
                })
                // 清空搜索
                this.searchPipelineList({
                    projectId
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
                        projectId,
                        pipelineId
                    }
                })
            },
            async handleSearchPipeline (value) {
                if (this.pipelineListSearching) return
                this.pipelineListSearching = true
                await this.fetchPipelineList(value)
                this.pipelineListSearching = false
            },
            async switchBuildNum (int = 0) {
                const { execDetail } = this
                const buildNum = execDetail.buildNum + int
                if (!this.isLoading && buildNum !== execDetail.buildNum && execDetail.latestBuildNum >= buildNum && buildNum >= 1) {
                    try {
                        this.isLoading = true
                        const response = await this.requestPipelineExecDetailByBuildNum({
                            buildNum,
                            ...this.$route.params
                        })

                        this.$router.push({
                            name: 'pipelinesDetail',
                            params: {
                                ...this.$route.params,
                                buildNo: response.data.id
                            }
                        })
                    } catch (error) {

                    } finally {
                        this.isLoading = false
                    }
                }
            },
            showLog () {
                this.togglePropertyPanel({
                    isShow: true,
                    isComplete: true
                })
            },
            startExcuete () {
                bus.$emit('start-execute')
            },
            exportPipeline () {
                this.showExportDialog = true
            },
            importModifyPipeline () {
                this.showImportDialog = true
            },
            handleImportModifyPipeline (result) {
                this.showImportDialog = false
                this.setEditFrom(true)
                if (!this.isEditPage) {
                    this.$router.push({
                        name: 'pipelinesEdit'
                    })
                }
                this.$nextTick(() => {
                    const pipelineVersion = this.curPipeline.pipelineVersion
                    const pipelineName = this.curPipeline.pipelineName
                    this.setPipelineSetting({
                        ...result.setting,
                        pipelineName,
                        pipelineId: this.pipelineId,
                        projectId: this.projectId
                    })
                    this.setPipeline({
                        ...result.model,
                        name: pipelineName,
                        latestVersion: pipelineVersion,
                        instanceFromTemplate: false
                    })
                    this.setPipelineEditing(true)
                })
            },

            async toggleCollect () {
                const isCollect = !this.curPipeline.hasCollect
                const res = await this.togglePipelineCollect(this.curPipeline.pipelineId, isCollect)
                if (res) {
                    this.updateCurPipelineByKeyValue('hasCollect', isCollect)
                }
            },
            toExecute (...args) {
                const goDetail = ['pipelinesEdit', 'pipelinesDetail'].indexOf(this.$route.name) > -1
                this.executePipeline(...args, goDetail)
            }
        }
    }
</script>

<style lang="scss">
    @import "../../scss/conf";
    .bkdevops-history-subpage {
        min-height: 100%;
        flex-direction: column;
        .bk-exception {
            position: absolute;
        }
        .more-operation-entry {
            display: flex;
            width: 30px;
            height: 100%;
            flex-direction: column;
            float: right;
            justify-content: center;
            align-items: center;
            cursor: pointer;
            padding-top: 5px;

            &:hover,
            &.active {
                &:before {
                    content: '';
                    position: absolute;
                    width: 18px;
                    height: 36px;
                    top: 14px;
                    background-color: #E0ECFF;
                    z-index: 0;
                }
                i.entry-circle {
                    background-color: $primaryColor;
                }
            }

            &:hover {
                .more-operation-dropmenu {
                    display: block;
                }
            }

            i.entry-circle {
                display: flex;
                width: 18px;
                margin: 5px 0;
                background-color: $fontWeightColor;
                width: 3px;
                height: 3px;
                border-radius: 50%;
                z-index: 1;
            }
        }
        .more-operation-dropmenu {
            position: absolute;
            display: none;
            right: -6px;
            top: 58px;
            width:163px;
            background-color:white;
            box-shadow:0px 2px 6px 0px rgba(0,0,0,0.07);
            border:1px solid #dcdee5;
            z-index: 12;
            &:before {
                content: '';
                position: absolute;
                right: 7px;
                top: -6px;
                background: white;
                width: 10px;
                height: 10px;
                transform: rotate(45deg);
                border-left: 1px solid #DCDEE5;
                border-top: 1px solid #DCDEE5;
            }
            > ul {
                padding-top: 8px;
                &:first-child {
                    border-bottom: 1px solid #DCDEE5;
                }
                > li {
                    font-size: 12px;
                    line-height: 32px;
                    text-align: left;
                    padding: 0 15px;
                    cursor: pointer;
                    a {
                        color: $fontColor;
                        display: block;
                    }
                    &:hover {
                        color: $primaryColor;
                        background-color: #EAF3FF;
                        a {
                            color: $primaryColor;
                        }
                    }
                }
            }
        }

        .customer-inner-header {
            .inner-header-left {
                width: 60%
            }
            .inner-header-right {
                width: 40%
            }
        }
        .history-bread-crumb {
            display: flex;
            height: 100%;
            align-items: center;
            .bread-crumb-comp {
                flex: 1;
            }
            .build-num-switcher {
                display: flex;
                align-items: center;
                > p {
                    display: flex;
                    flex-direction: column;
                    > i.devops-icon {
                        color: $primaryColor;
                        cursor: pointer;
                        font-size: 10px;

                        &[disabled] {
                            color: $fontLighterColor;
                            cursor: auto;
                        }
                    }
                }
                .icon-txt {
                    font-size: 18px;
                    font-weight: normal;
                    cursor: pointer;
                    &:hover {
                        color: $primaryColor;
                    }
                }
            }
        }
    }
    .crumb-spin-loading {
        display: inline-block;
    }

    .pipelines-triggers.bkdevops-header-trigger-btn {
        width: auto;
        display: inline-block;
        height: auto;
        font-size: inherit;
    }
     .bkdevops-pipeline-tab-card {
        // display: flex;
        // overflow: hidden;
        // flex-direction: column;
        min-height: 100%;
        border: 0;
        background-color: transparent;
        &-setting {
            font-size: 18px;
            display: flex;
            align-items: center;
            height: 100%;

            .devops-icon {
                color: $fontLighterColor;
                padding-left: 16px;
                cursor: pointer;
                &:hover,
                &.active {
                    color: $primaryColor;
                }
            }
        }
        .bk-tab-header {
            background: transparent;
            background-image: none !important;
            .bk-tab-label-wrapper .bk-tab-label-list .bk-tab-label-item {
                min-width: auto;
                padding: 0;
                margin-right: 30px;
                text-align: left;
                font-weight: bold;
                &.active {
                    color: $primaryColor;
                    background: transparent
                }
            }
        }
        .bk-tab-section {
            width: 100%;
            min-height: calc(100% - 60px);
            padding: 0;
            margin-top: 10px;
            flex: 1;
            // overflow: hidden;
            .bk-tab-content {
                height: 100%;
                display: flex;
                flex-direction: column;
            }
        }
    }
</style>
