<template>
    <div class="flex-container">
        <bk-dropdown-menu trigger="click" align="bottom">
            <div slot="dropdown-trigger" class="more-operation-entry">
                <i class="entry-circle" v-for="i in [1, 2, 3]" :key="i" />
            </div>
            <div class="more-operation-dropmenu" slot="dropdown-content">
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
        </bk-dropdown-menu>
        <rename-dialog
            :is-show="isRenameDialogShow"
            v-bind="curPipeline"
            :project-id="$route.params.projectId"
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
        <import-pipeline-popup
            :handle-import-success="handleImportModifyPipeline"
            :is-show.sync="showImportDialog"
        ></import-pipeline-popup>
        <remove-confirm-dialog
            :type="pipelineActionState.confirmType"
            :is-show="pipelineActionState.isConfirmShow"
            :pipeline-list="pipelineActionState.activePipelineList"
            @close="closeRemoveConfirmDialog"
            @done="goHome"
        />
        <export-dialog :is-show.sync="showExportDialog"></export-dialog>
    </div>
</template>

<script>
    import { mapState, mapGetters, mapMutations, mapActions } from 'vuex'
    import ImportPipelinePopup from '@/components/pipelineList/ImportPipelinePopup'
    import exportDialog from '@/components/ExportDialog'
    import CopyPipelineDialog from '@/components/PipelineActionDialog/CopyPipelineDialog'
    import SaveAsTemplateDialog from '@/components/PipelineActionDialog/SaveAsTemplateDialog'
    import RenameDialog from '@/components/PipelineActionDialog/RenameDialog'
    import RemoveConfirmDialog from '@/views/PipelineList/RemoveConfirmDialog'
    import pipelineActionMixin from '@/mixins/pipeline-action-mixin'
    export default {
        components: {
            ImportPipelinePopup,
            exportDialog,
            CopyPipelineDialog,
            SaveAsTemplateDialog,
            RenameDialog,
            RemoveConfirmDialog
        },
        mixins: [pipelineActionMixin],
        data () {
            return {
                isRenameDialogShow: false,
                hasNoPermission: false,
                showExportDialog: false,
                showImportDialog: false
            }
        },
        computed: {
            ...mapState('pipelines', ['pipelineActionState']),
            ...mapGetters({
                curPipeline: 'pipelines/getCurPipeline'
            }),
            isTemplatePipeline () {
                return this.curPipeline?.instanceFromTemplate ?? false
            },
            actionConfMenus () {
                const pipeline = {
                    ...this.curPipeline,
                    projectId: this.$route.params.projectId
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
                        },
                        {
                            label: 'newlist.saveAsTemp',
                            handler: () => this.saveAsTempHandler(pipeline)
                        },
                        {
                            label: 'newlist.jumpToTemp',
                            handler: () => this.jumpToTemplate(pipeline),
                            hidden: !this.isTemplatePipeline
                        },
                        {
                            label: 'delete',
                            handler: () => this.deleteHandler(pipeline)
                        }
                    ]
                ]
            }
        },
        methods: {
            ...mapActions('atom', ['setPipelineEditing', 'setPipeline', 'setEditFrom']),
            ...mapActions('pipelines', ['setPipelineSetting', 'requestToggleCollect']),
            ...mapMutations('pipelines', ['updateCurPipelineByKeyValue']),
            toggleRenameDialog (show = false) {
                this.isRenameDialogShow = show
            },
            renameDone (name) {
                this.$nextTick(() => {
                    this.updateCurPipelineByKeyValue({
                        key: 'pipelineName',
                        value: name
                    })
                    this.pipelineSetting
                        && Object.keys(this.pipelineSetting).length
                        && this.updatePipelineSetting({
                            container: this.pipelineSetting,
                            param: {
                                pipelineName: name
                            }
                        })
                })
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
                this.$nextTick(() => {
                    console.log('this.curPipeline', this.curPipeline)
                    const pipelineVersion = this.curPipeline.pipelineVersion
                    const pipelineName = this.curPipeline.pipelineName
                    this.setPipelineSetting({
                        ...result.setting,
                        pipelineName,
                        pipelineId: this.curPipeline.pipelineId,
                        projectId: this.$route.params.projectId
                    })
                    this.setPipeline({
                        ...result.model,
                        name: pipelineName,
                        latestVersion: pipelineVersion,
                        instanceFromTemplate: false
                    })
                    this.setPipelineEditing(true)
                    this.$router.push({
                        name: 'pipelinesEdit'
                    })
                })
            },

            async toggleCollect () {
                const isCollect = !this.curPipeline.hasCollect
                let message = isCollect ? this.$t('collectSuc') : this.$t('uncollectSuc')
                let theme = 'success'
                try {
                    await this.requestToggleCollect({
                        projectId: this.$route.params.projectId,
                        ...this.curPipeline,
                        isCollect
                    })
                    this.updateCurPipelineByKeyValue({
                        key: 'hasCollect',
                        value: isCollect
                    })
                } catch (err) {
                    message = err.message || err
                    theme = 'error'
                } finally {
                    this.$showTips({
                        message,
                        theme
                    })
                }
            }
        }
    }
</script>

<style lang="scss">
@import "@/scss/conf";
.flex-container {
  display: flex;
}
.more-operation-entry {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  padding: 5px 10px;

  &:hover,
  &.active {
    background-color: #e0ecff;
    i.entry-circle {
      background-color: $primaryColor;
    }
  }

  i.entry-circle {
    display: flex;
    width: 18px;
    margin: 2px 0;
    background-color: $fontWeightColor;
    width: 3px;
    height: 3px;
    border-radius: 50%;
    z-index: 1;
  }
}
.more-operation-dropmenu {
  width: 120px;
  > ul {
    &:first-child {
      border-bottom: 1px solid #dcdee5;
    }
    > li {
      font-size: 12px;
      line-height: 32px;
      text-align: left;
      padding: 0 12px;
      cursor: pointer;
      &:hover {
        color: $primaryColor;
        background-color: #eaf3ff;
        a {
          color: $primaryColor;
        }
      }
    }
  }
}
</style>
