<template>
    <div class="flex-container">
        <bk-dropdown-menu
            trigger="click"
            align="bottom"
        >
            <div
                slot="dropdown-trigger"
                class="more-operation-entry"
            >
                <i
                    class="entry-circle"
                    v-for="i in [1, 2, 3]"
                    :key="i"
                />
            </div>
            <div
                v-if="curPipelineId"
                :key="curPipelineId"
                class="more-operation-dropmenu"
                slot="dropdown-content"
            >
                <ul
                    v-for="(parent, index) in actionConfMenus"
                    :key="index"
                >
                    <template v-for="(action, aIndex) in parent">
                        <li
                            v-if="!action.hidden"
                            :key="aIndex"
                            v-perm="{
                                ...action.vPerm
                            }"
                            @click="action.handler"
                        >
                            {{ $t(action.label) }}
                        </li>
                    </template>
                </ul>
            </div>
        </bk-dropdown-menu>
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
            :pipeline-id="$route.params.pipelineId"
            :pipeline-name="pipelineName"
            :is-show.sync="showImportDialog"
        ></import-pipeline-popup>
        <remove-confirm-dialog
            :type="pipelineActionState.confirmType"
            :is-show="pipelineActionState.isConfirmShow"
            :pipeline-list="pipelineActionState.activePipelineList"
            @close="closeRemoveConfirmDialog"
            @done="afterRemovePipeline"
        />
        <export-dialog :is-show.sync="showExportDialog"></export-dialog>
        <disable-dialog
            :value="showDisableDialog"
            :pipeline-id="$route.params.pipelineId"
            :pipeline-name="pipelineName"
            :lock="isCurPipelineLocked"
            :pac-enabled="pacEnabled"
            @close="closeDisablePipeline"
            @done="afterDisablePipeline"
        />
        <delete-archived-dialog
            :is-show-delete-archived-dialog="pipelineActionState.isShowDeleteArchivedDialog"
            :pipeline-list="pipelineActionState.activePipelineList"
            @done="afterRemovePipeline"
            @cancel="closeDeleteArchiveDialog"
        />
    </div>
</template>

<script>
    import exportDialog from '@/components/ExportDialog'
    import CopyPipelineDialog from '@/components/PipelineActionDialog/CopyPipelineDialog'
    import DisableDialog from '@/components/PipelineActionDialog/DisableDialog'
    import { UPDATE_PIPELINE_INFO } from '@/store/modules/atom/constants'
    import { mapActions, mapGetters, mapState } from 'vuex'

    import SaveAsTemplateDialog from '@/components/PipelineActionDialog/SaveAsTemplateDialog'
    import ImportPipelinePopup from '@/components/pipelineList/ImportPipelinePopup'
    import pipelineActionMixin from '@/mixins/pipeline-action-mixin'
    import { RESOURCE_ACTION, TEMPLATE_RESOURCE_ACTION } from '@/utils/permission'
    import { pipelineTabIdMap } from '@/utils/pipelineConst'
    import RemoveConfirmDialog from '@/views/PipelineList/RemoveConfirmDialog'
    import DeleteArchivedDialog from '@/views/PipelineList/DeleteArchivedDialog'
    export default {
        components: {
            ImportPipelinePopup,
            exportDialog,
            CopyPipelineDialog,
            SaveAsTemplateDialog,
            RemoveConfirmDialog,
            DeleteArchivedDialog,
            DisableDialog
        },
        mixins: [pipelineActionMixin],
        data () {
            return {
                hasNoPermission: false,
                showExportDialog: false,
                showImportDialog: false,
                showDisableDialog: false
            }
        },
        computed: {
            ...mapState('atom', ['pipelineInfo']),
            ...mapState('pipelines', ['pipelineActionState']),
            ...mapGetters('atom', ['pacEnabled', 'isCurPipelineLocked']),
            ...mapState('common', [
                'hasProjectPermission'
            ]),
            isTemplatePipeline () {
                return this.pipelineInfo?.instanceFromTemplate ?? false
            },
            pipelineName () {
                return this.pipelineInfo?.pipelineName ?? ''
            },
            curPipelineId () {
                return this.pipelineInfo?.pipelineId
            },
            archiveFlag () {
                return this.$route.query.archiveFlag
            },
            actionConfMenus () {
                const { projectId } = this.$route.params
                const pipeline = {
                    ...(this.pipelineInfo ?? {}),
                    projectId,
                    pac: this.pacEnabled
                }
                const menuItems = [
                    [
                        {
                            label: this.pipelineInfo?.hasCollect ? 'uncollect' : 'collect',
                            handler: this.toggleCollect
                        },
                        {
                            label: 'rename',
                            handler: () => {
                                this.$router.push({
                                    name: 'pipelinesEdit',
                                    query: {
                                        tab: pipelineTabIdMap.setting
                                    }
                                })
                            }
                        }
                    ],
                    [
                        {
                            label: 'newlist.exportPipelineJson',
                            handler: this.exportPipeline,
                            vPerm: {
                                hasPermission: this.archiveFlag
                                    ? this.hasProjectPermission
                                    : pipeline.permissions?.canEdit,
                                disablePermissionApi: true,
                                permissionData: {
                                    projectId,
                                    resourceType: 'pipeline',
                                    resourceCode: pipeline.pipelineId,
                                    action: RESOURCE_ACTION.EDIT
                                }
                            }
                        },
                        {
                            label: 'newlist.importModifyPipelineJson',
                            handler: this.importModifyPipeline,
                            hidden: this.isTemplatePipeline,
                            vPerm: {
                                hasPermission: pipeline.permissions?.canEdit,
                                disablePermissionApi: true,
                                permissionData: {
                                    projectId,
                                    resourceType: 'pipeline',
                                    resourceCode: pipeline.pipelineId,
                                    action: RESOURCE_ACTION.EDIT
                                }
                            }
                        },
                        ...(pipeline.templateId
                            ? [
                                {
                                    label: 'copyAsTemplateInstance',
                                    handler: () => this.copyAsTemplateInstance(pipeline),
                                    vPerm: {
                                        hasPermission: pipeline.permissions?.canManage,
                                        disablePermissionApi: true,
                                        permissionData: {
                                            projectId,
                                            resourceType: 'project',
                                            resourceCode: projectId,
                                            action: RESOURCE_ACTION.CREATE
                                        }
                                    }
                                }
                            ]
                            : []),
                        {
                            label: 'newlist.copyAs',
                            handler: () => this.copyAs(pipeline),
                            vPerm: {
                                hasPermission: pipeline.permissions?.canEdit,
                                disablePermissionApi: true,
                                permissionData: {
                                    projectId,
                                    resourceType: 'pipeline',
                                    resourceCode: pipeline.pipelineId,
                                    action: RESOURCE_ACTION.EDIT
                                }
                            }
                        },
                        {
                            label: 'newlist.saveAsTemp',
                            handler: () => this.saveAsTempHandler(pipeline),
                            vPerm: {
                                permissionData: {
                                    projectId,
                                    resourceType: 'project',
                                    resourceCode: projectId,
                                    action: TEMPLATE_RESOURCE_ACTION.CREATE
                                }
                            }
                        },
                        {
                            id: 'jumpToTemp',
                            label: 'newlist.jumpToTemp',
                            handler: () => this.jumpToTemplate(pipeline),
                            hidden: !this.isTemplatePipeline
                        }
                    ],
                    [
                        {
                            label: this.isCurPipelineLocked ? 'enable' : 'disable',
                            handler: () => this.disablePipeline(),
                            vPerm: {
                                hasPermission: pipeline.permissions?.canEdit,
                                disablePermissionApi: true,
                                permissionData: {
                                    projectId,
                                    resourceType: 'pipeline',
                                    resourceCode: pipeline.pipelineId,
                                    action: RESOURCE_ACTION.EDIT
                                }
                            }
                        },
                        {
                            label: 'delete',
                            handler: () => {
                                if (this.archiveFlag) {
                                    this.openDeleteArchivedDialog(pipeline)
                                } else {
                                    this.deleteHandler(pipeline)
                                }
                            },
                            vPerm: {
                                hasPermission: this.archiveFlag
                                    ? this.hasProjectPermission
                                    : pipeline.permissions?.canDelete,
                                disablePermissionApi: true,
                                permissionData: {
                                    projectId,
                                    resourceType: 'pipeline',
                                    resourceCode: pipeline.pipelineId,
                                    action: RESOURCE_ACTION.DELETE
                                }
                            }
                        }
                    ]
                ]

                if (this.archiveFlag) {
                    // 归档时只保留导出和删除
                    return menuItems.map(subMenu =>
                        subMenu.filter(item =>
                            item.label === 'newlist.exportPipelineJson'
                            || item.label === 'delete'
                        )
                    ).filter(subMenu => subMenu.length > 0)
                }

                return menuItems
            }
        },
        methods: {
            ...mapActions('atom', ['setPipelineEditing', 'setEditFrom']),
            ...mapActions('pipelines', ['requestToggleCollect']),
            exportPipeline () {
                this.showExportDialog = true
            },
            disablePipeline () {
                this.showDisableDialog = true
            },
            closeDisablePipeline () {
                this.showDisableDialog = false
            },
            importModifyPipeline () {
                this.showImportDialog = true
            },
            handleImportModifyPipeline () {
                this.showImportDialog = false
                this.$nextTick(() => {
                    this.$router.push({
                        name: 'pipelinesEdit'
                    })
                })
            },
            afterDisablePipeline (enable) {
                this.$store.commit(`atom/${UPDATE_PIPELINE_INFO}`, {
                    locked: !enable
                })
            },

            async toggleCollect () {
                const isCollect = !this.pipelineInfo?.hasCollect
                let message = isCollect ? this.$t('collectSuc') : this.$t('uncollectSuc')
                let theme = 'success'
                try {
                    await this.requestToggleCollect({
                        projectId: this.$route.params.projectId,
                        ...this.pipelineInfo,
                        isCollect
                    })
                    this.$store.commit(`atom/${UPDATE_PIPELINE_INFO}`, {
                        hasCollect: isCollect
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
            },
            copyAsTemplateInstance (pipeline) {
                const pipelineName = (pipeline.pipelineName + '_copy').substring(0, 128)
                const { templateId, pipelineId, projectId, templateVersion } = pipeline
                window.top.location.href = `${location.origin}/console/pipeline/${projectId}/template/${templateId}/createInstance/${templateVersion}/${pipelineName}?pipelineId=${pipelineId}`
            },
            afterRemovePipeline () {
                this.$router.push({
                    name: 'PipelineManageList'
                })
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
  > ul {
    &:first-child {
      border-bottom: 1px solid #dcdee5;
    }
    &:last-child {
      border-top: 1px solid #dcdee5;
    }
    > li {
      font-size: 12px;
      line-height: 32px;
      text-align: left;
      white-space: nowrap;
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
