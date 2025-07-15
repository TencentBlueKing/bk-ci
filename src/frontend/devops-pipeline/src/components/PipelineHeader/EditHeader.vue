<template>
    <div class="pipeline-edit-header">
        <pipeline-bread-crumb
            :is-loading="!isPipelineNameReady"
        >
            <span class="pipeline-edit-header-tag">
                <bk-tag>
                    <span
                        v-bk-overflow-tips
                        class="edit-header-draft-tag"
                    >
                        {{ currentVersionName }}
                    </span>
                </bk-tag>
            </span>
        </pipeline-bread-crumb>
        <mode-switch
            :save="saveDraft"
        />
        <aside class="pipeline-edit-right-aside">
            <bk-button
                :disabled="saveStatus"
                :loading="saveStatus"
                @click="goBack"
            >
                {{ $t("cancel") }}
            </bk-button>

            <bk-button
                :disabled="saveStatus || !isEditing"
                :loading="saveStatus"
                outline
                theme="primary"
                @click="saveDraft"
                v-perm="{
                    hasPermission: canEdit,
                    disablePermissionApi: true,
                    permissionData: {
                        projectId,
                        resourceType: 'pipeline',
                        resourceCode: pipelineId,
                        action: RESOURCE_ACTION.EDIT
                    }
                }"
            >
                {{ $t("saveDraft") }}
            </bk-button>
            <bk-button
                :disabled="!canDebug"
                :loading="executeStatus"
                v-perm="{
                    hasPermission: canExecute,
                    disablePermissionApi: true,
                    permissionData: {
                        projectId,
                        resourceType: 'pipeline',
                        resourceCode: pipelineId,
                        action: RESOURCE_ACTION.EXECUTE
                    }
                }"
                @click="exec(true)"
            >
                {{ $t("debug") }}
            </bk-button>
            <bk-dropdown-menu
                trigger="click"
                align="center"
            >
                <div
                    slot="dropdown-trigger"
                >
                    <i class="manage-icon manage-icon-more-fill"></i>
                </div>
                <div slot="dropdown-content">
                    <ul
                        class="bk-dropdown-list"
                        slot="dropdown-content"
                    >
                        <li
                            v-for="(item, index) in actionConfMenus"
                            :class="['develop-txt', {
                                'develop-txt-disabled': item.disabled
                            }]"
                            :key="index"
                            @click="item.handler"
                            v-bk-tooltips="{
                                content: $t('noDraft'),
                                disabled: item.showTooltips
                            }"
                            v-perm="item.vPerm ? item.vPerm : {}"
                        >
                            <template v-if="item.label">
                                {{ item.label }}
                            </template>
                            <template v-else>
                                <component
                                    :is="item.component"
                                    v-bind="item.componentProps"
                                    :disabled="item.disabled"
                                />
                            </template>
                        </li>
                    </ul>
                </div>
            </bk-dropdown-menu>

            <!-- <more-actions /> -->
            <release-button
                :can-release="canRelease && !isEditing"
                :project-id="projectId"
                :pipeline-id="pipelineId"
            />
        </aside>
    </div>
</template>

<script>
    import ModeSwitch from '@/components/ModeSwitch'
    import VersionDiffEntry from '@/components/PipelineDetailTabs/VersionDiffEntry.vue'
    import {
        RESOURCE_ACTION
    } from '@/utils/permission'
    import { UI_MODE } from '@/utils/pipelineConst'
    import { showPipelineCheckMsg } from '@/utils/util'
    import { mapActions, mapGetters, mapState } from 'vuex'
    import PipelineBreadCrumb from './PipelineBreadCrumb.vue'
    import ReleaseButton from './ReleaseButton'

    export default {
        components: {
            PipelineBreadCrumb,
            ReleaseButton,
            ModeSwitch,
            VersionDiffEntry
        },
        props: {
            isSwitchPipeline: Boolean
        },
        computed: {
            ...mapState([
                'pipelineMode'
            ]),
            ...mapState('atom', [
                'pipeline',
                'saveStatus',
                'pipelineWithoutTrigger',
                'pipelineSetting',
                'pipelineYaml',
                'pipelineInfo'
            ]),
            ...mapState('pipelines', ['executeStatus', 'isManage']),
            ...mapGetters({
                isCurPipelineLocked: 'atom/isCurPipelineLocked',
                isEditing: 'atom/isEditing',
                checkPipelineInvalid: 'atom/checkPipelineInvalid',
                draftBaseVersionName: 'atom/getDraftBaseVersionName',
                hasDraftPipeline: 'atom/hasDraftPipeline',
                isCommittingPipeline: 'atom/isCommittingPipeline'
            }),
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            },
            canEdit () {
                return this.pipelineInfo?.permissions?.canEdit ?? true
            },
            canExecute () {
                return this.pipelineInfo?.permissions?.canExecute ?? true
            },
            canDebug () {
                return (this.pipelineInfo?.canDebug ?? false) && !this.saveStatus && !this.isCurPipelineLocked
            },
            RESOURCE_ACTION () {
                return RESOURCE_ACTION
            },
            btnDisabled () {
                return this.saveStatus || this.executeStatus
            },
            canRelease () {
                return (this.pipelineInfo?.canRelease ?? false) && !this.saveStatus
            },
            isTemplatePipeline () {
                return this.pipelineInfo?.instanceFromTemplate ?? false
            },
            versionName () {
                return this.pipelineInfo?.versionName ?? '--'
            },
            currentVersionName () {
                if (this.pipelineInfo?.canDebug) {
                    return this.$t('editPage.draftVersion', [this.draftBaseVersionName])
                }
                return this.versionName
            },
            currentVersion () {
                return this.pipelineInfo?.version ?? ''
            },
            isPipelineNameReady () {
                return this.pipelineSetting?.pipelineId === this.$route.params.pipelineId
            },
            activeVersion () {
                return this.pipelineInfo?.releaseVersion ?? ''
            },
            actionConfMenus () {
                const { projectId } = this.$route.params
                return [
                    {
                        component: VersionDiffEntry,
                        componentProps: {
                            version: this.activeVersion,
                            latestVersion: this.currentVersion,
                            theme: 'normal',
                            size: 'small',
                            showButton: false
                        },
                        handler: () => {},
                        disabled: !this.hasDraftPipeline,
                        showTooltips: true
                    },
                    {
                        label: this.$t('draftExecRecords'),
                        handler: this.goDraftDebugRecord,
                        disabled: !this.canDebug,
                        vPerm: {
                            hasPermission: this.canExecute,
                            disablePermissionApi: true,
                            permissionData: {
                                projectId,
                                resourceType: 'pipeline',
                                resourceCode: this.pipelineId,
                                action: this.RESOURCE_ACTION.EXECUTE
                            }
                        },
                        showTooltips: true
                    },
                    {
                        label: this.$t('deleteDraft'),
                        handler: this.handelDelete,
                        disabled: !(this.hasDraftPipeline || this.isCommittingPipeline),
                        showTooltips: this.hasDraftPipeline || this.isCommittingPipeline
                    }
                ]
            }
        },
        watch: {
            isTemplatePipeline: {
                handler (val) {
                    if (val) {
                        this.updatePipelineMode(UI_MODE)
                    }
                },
                immediate: true
            }
        },
        methods: {
            ...mapActions({
                updatePipelineMode: 'updatePipelineMode'
            }),
            ...mapActions('atom', [
                'setPipelineEditing',
                'saveDraftPipeline',
                'setSaveStatus',
                'requestPipelineSummary',
                'updateContainer'
            ]),
            ...mapActions('pipelines', [
                'deletePipelineVersion',
                'patchDeletePipelines'
            ]),
            async exec (debug) {
                if (debug && this.isEditing) {
                    const result = await this.saveDraft()
                    if (!result) {
                        return
                    }
                }
                this.$router.push({
                    name: 'executePreview',
                    query: {
                        ...(debug ? { debug: '' } : {})
                    },
                    params: {
                        ...this.$route.params,
                        version: this.pipelineInfo?.[debug ? 'version' : 'releaseVersion']
                    }
                })
            },
            formatParams (pipeline) {
                const params = pipeline.stages[0].containers[0].params
                const paramList
                    = params
                        && params.map((param) => {
                            const { paramIdKey, ...temp } = param
                            return temp
                        })
                this.updateContainer({
                    container: this.pipeline.stages[0].containers[0],
                    newParam: {
                        params: paramList
                    }
                })
            },

            async saveDraft () {
                try {
                    this.setSaveStatus(true)
                    const pipeline = Object.assign({}, this.pipeline, {
                        stages: [
                            this.pipeline.stages[0],
                            ...this.pipelineWithoutTrigger.stages
                        ]
                    })
                    const { projectId, pipelineId, pipelineSetting, checkPipelineInvalid, pipelineYaml } = this
                    const { inValid, message } = checkPipelineInvalid(pipeline.stages, pipelineSetting)
                    if (inValid) {
                        throw new Error(message)
                    }
                    // 清除流水线参数渲染过程中添加的key
                    this.formatParams(pipeline)

                    // 请求执行构建
                    await this.saveDraftPipeline({
                        projectId,
                        pipelineId,
                        baseVersion: this.pipelineInfo?.baseVersion,
                        storageType: this.pipelineMode,
                        modelAndSetting: {
                            model: {
                                ...pipeline,
                                name: pipelineSetting.pipelineName,
                                desc: pipelineSetting.desc
                            },
                            setting: Object.assign(pipelineSetting, {
                                failSubscription: undefined,
                                successSubscription: undefined
                            })
                        },
                        yaml: pipelineYaml
                    })
                    this.setPipelineEditing(false)

                    await this.requestPipelineSummary(this.$route.params)

                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('editPage.saveDraftSuccess', [pipelineSetting.pipelineName]),
                        limit: 1
                    })
                    return true
                } catch (e) {
                    const { projectId, pipelineId } = this.$route.params

                    if (e.code === 2101244) {
                        showPipelineCheckMsg(this.$bkMessage, e.message, this.$createElement)
                    } else {
                        this.handleError(e, {
                            projectId,
                            resourceCode: pipelineId,
                            action: RESOURCE_ACTION.EDIT
                        })
                    }
                    return false
                } finally {
                    this.setSaveStatus(false)
                }
            },
            createSubHeader (pipelineName, draftBaseVersionName) {
                const h = this.$createElement
                return h('div', { class: 'draft-delete' }, [
                    h('p', {
                        class: 'text-overflow',
                        directives: [
                            {
                                name: 'bk-tooltips',
                                value: pipelineName
                            }
                        ]
                    }, [
                        h('span', { class: 'label' }, `${this.$t('pipeline')} ：`),
                        h('span', pipelineName)
                    ]),
                    h('p', [
                        h('span', { class: 'label' }, `${this.$t('draft')} ：`),
                        h('span', `${this.$t('baseOn', [draftBaseVersionName])} `)
                    ])
                ])
            },
            async deleteDraftConfirm () {
                try {
                    await this.deletePipelineVersion({
                        projectId: this.projectId,
                        pipelineId: this.pipelineId,
                        version: this.currentVersion
                    })

                    // 删除草稿时需要更新pipelineInfo
                    await this.requestPipelineSummary(this.$route.params)
                    this.$showTips({
                        message: this.$t('delete') + this.$t('version') + this.$t('success'),
                        theme: 'success'
                    })
                    this.$router.push({
                        name: 'pipelinesHistory'
                    })
                } catch (err) {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                }
            },
            async deletePipelineConfirm () {
                try {
                    const params = {
                        projectId: this.projectId,
                        pipelineIds: [this.pipelineId]
                    }
                    const { data } = await this.patchDeletePipelines(params)
                    const hasErr = Object.keys(data)[0] !== this.pipelineId
                    if (hasErr) {
                        throw Error(this.$t('deleteFail'))
                    }
                    this.$showTips({
                        message: this.$t('delete') + this.$t('version') + this.$t('success'),
                        theme: 'success'
                    })

                    this.$router.push({
                        name: 'PipelineManageList'
                    })
                } catch (err) {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                }
            },
            goDraftDebugRecord () {
                if (this.canDebug) {
                    this.$router.push({
                        name: 'draftDebugRecord'
                    })
                }
            },
            /**
             * 删除草稿
             */
            async handelDelete () {
                const commonConfig = {
                    title: this.$t('sureDeleteDraft'),
                    okText: this.$t('delete'),
                    cancelText: this.$t('cancel'),
                    theme: 'danger',
                    width: 470,
                    confirmLoading: true
                }
                if (this.isCommittingPipeline) {
                    this.$bkInfo({
                        ...commonConfig,
                        subTitle: this.$t('deleteDraftPipeline'),
                        confirmFn: this.deletePipelineConfirm
                    })
                } else if (this.hasDraftPipeline) {
                    this.$bkInfo({
                        ...commonConfig,
                        subHeader: this.createSubHeader(this.pipelineSetting.pipelineName, this.draftBaseVersionName),
                        confirmFn: this.deleteDraftConfirm
                    })
                }
            },
            goBack () {
                this.$router.back()
            }
        }
    }
</script>

<style lang="scss">
@import '@/scss/conf';
@import '@/scss/mixins/ellipsis';
.pipeline-edit-header {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  padding: 0 0 0 14px;
  align-self: stretch;
  .pipeline-edit-header-tag {
    display: flex;
    align-items: center;
    grid-gap: 8px;
    line-height: 1;
    .bk-tag {
        margin: 0;
        max-width: 222px;
        .edit-header-draft-tag {
            @include ellipsis();
            width: 100%;
        }
    }
  }
  .pipeline-edit-right-aside {
    display: grid;
    grid-gap: 10px;
    grid-auto-flow: column;
    height: 100%;
    align-items: center;
    justify-content: center;
  }
}
.pipeline-save-error-list-box {
    display: flex;
    flex-direction: column;
    grid-gap: 10px;
    .pipeline-save-error-list-item {

        > p {
            margin-bottom: 12px;
        }
        .pipeline-save-error-list {
            > li {
                line-height: 26px;
                a {
                    color: $primaryColor;
                    margin-left: 10px;
                    text-align: right;
                }
            }
        }
    }
}
.manage-icon-more-fill {
    font-size: 20px;
    padding: 3px;

    &:hover,
    &.active {
        background-color: #dddee6;
        color: #3a85ff;
        border-radius: 50%;
    }
}
.bk-dropdown-list {
    .develop-txt {
        display: block;
        height: 32px;
        line-height: 33px;
        padding: 0 16px;
        white-space: nowrap;
        font-size: 12px;
        cursor: pointer;
        &:hover {
            background-color: #f0f1f5;
        }
        &.develop-txt-disabled {
            cursor: not-allowed;
            color: #c4c6cc;
        }
    }
}
.draft-delete {
    text-align: center;
    color: #43444a;

    p {
        margin-bottom: 14px;
        max-width: 370px;
    }
    .label {
        color: #76777f;
    }
    .text-overflow {
        @include ellipsis();
    }
}
</style>
