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
                            :key="index"
                            @click="item.handler"
                            v-bk-tooltips="{
                                content: $t('noDraft'),
                                disabled: item.showTooltips
                            }"
                            v-perm="{
                                ...item.vPerm
                            }"
                        >
                            <a
                                href="javascript:;"
                                :class="['develop-txt', { 'txt-disabled': item.disabled }]"
                            >
                                {{ item.label }}
                            </a>
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

        <bk-dialog
            v-model="showVersionDiffDialog"
            render-directive="if"
            header-position="left"
            :draggable="false"
            ext-cls="diff-version-dialog"
            width="90%"
            :title="$t('diff')"
        >
            <div
                class="diff-version-dialog-content"
                v-bkloading="{ isLoading: isLoadYaml, color: '#1d1d1d' }"
            >
                <div class="pipeline-yaml-diff-wrapper">
                    <yaml-diff
                        :old-yaml="activeYaml"
                        height="100%"
                        :new-yaml="currentYaml"
                    />
                </div>
            </div>
            <footer slot="footer">
                <bk-button
                    @click="showVersionDiffDialog = false"
                >
                    {{ $t('close') }}
                </bk-button>
            </footer>
        </bk-dialog>
    </div>
</template>

<script>
    import ModeSwitch from '@/components/ModeSwitch'
    import { UPDATE_PIPELINE_INFO } from '@/store/modules/atom/constants'
    import {
        RESOURCE_ACTION
    } from '@/utils/permission'
    import { UI_MODE } from '@/utils/pipelineConst'
    import { showPipelineCheckMsg } from '@/utils/util'
    import { mapActions, mapGetters, mapState } from 'vuex'
    import PipelineBreadCrumb from './PipelineBreadCrumb.vue'
    import ReleaseButton from './ReleaseButton'
    import YamlDiff from '@/components/YamlDiff'

    export default {
        components: {
            PipelineBreadCrumb,
            ReleaseButton,
            ModeSwitch,
            YamlDiff
        },
        props: {
            isSwitchPipeline: Boolean
        },
        data () {
            return {
                isLoading: false,
                isReleaseSliderShow: false,
                showVersionDiffDialog: false,
                isLoadYaml: false,
                activeYaml: '',
                currentYaml: ''
            }
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
                draftBaseVersionName: 'atom/getDraftBaseVersionName'
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
            diffVersion () {
                return this.currentVersion !== this.activeVersion
            },
            actionConfMenus () {
                const { projectId } = this.$route.params
                return [
                    {
                        label: this.$t('diff'),
                        handler: this.initDiff,
                        disabled: !this.diffVersion,
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
                        disabled: !this.diffVersion && this.currentVersion !== 1,
                        showTooltips: this.diffVersion || this.currentVersion === 1
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
                'updateContainer',
                'fetchPipelineByVersion',
                'requestPipeline'
            ]),
            ...mapActions('pipelines', [
                'deletePipelineVersion',
                'updatePipelineActionState',
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
                    const { data: { version, versionName } } = await this.saveDraftPipeline({
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

                    this.$store.commit(`atom/${UPDATE_PIPELINE_INFO}`, {
                        canDebug: true,
                        canRelease: true,
                        baseVersion: this.pipelineInfo?.baseVersion ?? this.pipelineInfo?.releaseVersion ?? this.pipelineInfo?.version,
                        baseVersionName: this.pipelineInfo?.baseVersionName ?? this.pipelineInfo?.releaseVersionName ?? this.pipelineInfo?.versionName,
                        baseVersionStatus: this.pipelineInfo?.latestVersionStatus,
                        version,
                        versionName
                    })

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
                        class: 'overflow',
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
            async diffVersionDeleteConfirm () {
                try {
                    await this.deletePipelineVersion({
                        projectId: this.projectId,
                        pipelineId: this.pipelineId,
                        version: this.currentVersion
                    })

                    // 删除草稿时需要更新pipelineInfo
                    this.$store.commit(`atom/${UPDATE_PIPELINE_INFO}`, {
                        version: this.pipelineInfo?.releaseVersion,
                        versionName: this.pipelineInfo?.releaseVersionName,
                        canDebug: false,
                        canRelease: false
                    })
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
            async fetchPipelineYaml (version) {
                try {
                    const res = await this.fetchPipelineByVersion({
                        projectId: this.projectId,
                        pipelineId: this.pipelineId,
                        version
                    })
                    if (res?.yamlSupported) {
                        return res.yamlPreview.yaml
                    }
                    throw new Error(res?.yamlInvalidMsg)
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message,
                        zIndex: 3000
                    })
                    return ''
                }
            },
            async initDiff () {
                if (this.diffVersion) {
                    this.showVersionDiffDialog = true
                    this.isLoadYaml = true
                    const [activeYaml, currentYaml] = await Promise.all([
                        this.fetchPipelineYaml(this.activeVersion),
                        this.fetchPipelineYaml(this.currentVersion)
                    ])
                    this.activeYaml = activeYaml
                    this.currentYaml = currentYaml
                    this.isLoadYaml = false
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
                if (this.diffVersion) {
                    this.$bkInfo({
                        ...commonConfig,
                        subHeader: this.createSubHeader(this.pipelineSetting.pipelineName, this.draftBaseVersionName),
                        confirmFn: this.diffVersionDeleteConfirm
                    })
                }
                if (this.currentVersion === 1) {
                    this.$bkInfo({
                        ...commonConfig,
                        subTitle: this.$t('deleteDraftPipeline'),
                        confirmFn: this.deletePipelineConfirm
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
        &:hover {
            color: #72737c !important;
        }
    }

    .txt-disabled {
        color: #c4c6cc !important;
        cursor: not-allowed;

        &:hover {
            color: #c4c6cc !important;
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
    .overflow {
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }
}
</style>
