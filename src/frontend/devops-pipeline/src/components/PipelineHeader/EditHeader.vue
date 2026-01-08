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
            <div
                class="draft-content"
                v-if="draftList.length > 0"
            >
                <p
                    class="last-save-time"
                    @click="handleShowDraftList"
                >
                    <span>{{ $t("lastSaveTime") }}：</span>
                    {{ lasterDraftInfo?.updater }} {{ lasterDraftInfo?.updateTime }}
                    <i :class="['bk-icon', `icon-angle-${isShowDraftList ? 'up' : 'down'}`]" />
                </p>

                <ul
                    class="draft-list"
                    v-if="isShowDraftList"
                >
                    <li>
                        <i class="bk-icon icon-info-circle tips-icon" />
                        {{ $t('draftsClearedAfterVersionRelease') }}
                    </li>
                    <li
                        v-for="(item, index) in draftList"
                        :key="item.draftVersion"
                        :class="['draft-item', item.draftVersion === lasterDraftInfo?.draftVersion ? 'draft-item-active' : '']"
                    >
                        <p>
                            <span class="version-name">{{ $t('basedOn', item.baseVersion) }}</span>
                            <span class="update-info">{{ item.updater }}{{ item.updateTime }}</span>
                        </p>
                        <span
                            v-if="index !== 0"
                            class="options"
                        >
                            <VersionDiffEntry
                                style="cursor: pointer;"
                                :text="true"
                                :base-yaml="currentEditYaml"
                                :version="item.baseDraftVersion"
                                :can-switch-version="false"
                                @click.native.stop="handleDiff"
                                class="diff-button"
                            >
                                <Logo
                                    name="diff"
                                    size="14"
                                />
                            </VersionDiffEntry>
                            <span
                                class="rollback-icon rollback-button"
                                @click.stop="handleRollback(item)"
                            >
                                <Logo
                                    name="refresh"
                                    size="14"
                                />
                            </span>
                        </span>
                        <span
                            v-else
                            class="update-tip"
                        >{{ $t('lastSaveTime') }}</span>
                    </li>
                </ul>
            </div>
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
                        resourceType: RESOURCE_TYPE.PIPELINE,
                        resourceCode: pipelineId,
                        action: RESOURCE_ACTION.EDIT
                    }
                }"
            >
                <span
                    v-bk-tooltips="{
                        content: $t('noChange'),
                        arrow: true,
                        disabled: !(saveStatus || !isEditing)
                    }"
                >{{ $t("saveDraft") }}</span>
            </bk-button>
            <bk-button
                :disabled="!canDebug"
                :loading="executeStatus"
                v-perm="{
                    hasPermission: canExecute,
                    disablePermissionApi: true,
                    permissionData: {
                        projectId,
                        resourceType: RESOURCE_TYPE.PIPELINE,
                        resourceCode: pipelineId,
                        action: RESOURCE_ACTION.EXECUTE
                    }
                }"
                @click="exec(true)"
            >
                {{ $t("debug") }}
            </bk-button>
            <PipelineEditMoreAction
                :can-debug="canDebug"
                :project-id="projectId"
                :unique-id="pipelineId"
            />

            <!-- <more-actions /> -->
            <release-button
                :can-release="canRelease && !isEditing"
                :project-id="projectId"
                :id="pipelineId"
            />
        </aside>
    </div>
</template>

<script>
    import ModeSwitch from '@/components/ModeSwitch'
    import PipelineEditMoreAction from '@/components/PipelineEditMoreAction.vue'
    import {
        RESOURCE_ACTION,
        RESOURCE_TYPE
    } from '@/utils/permission'
    import { UI_MODE } from '@/utils/pipelineConst'
    import { showPipelineCheckMsg } from '@/utils/util'
    import { mapActions, mapGetters, mapState } from 'vuex'
    import PipelineBreadCrumb from './PipelineBreadCrumb.vue'
    import ReleaseButton from './ReleaseButton'
    import VersionDiffEntry from '@/components/PipelineDetailTabs/VersionDiffEntry'
    import Logo from '@/components/Logo'

    export default {
        components: {
            PipelineBreadCrumb,
            ReleaseButton,
            ModeSwitch,
            VersionDiffEntry,
            Logo,
            PipelineEditMoreAction
        },
        props: {
            isSwitchPipeline: Boolean
        },
        data () {
            return {
                draftList: [],
                lasterDraftInfo: null,
                isShowDraftList: false,
                currentEditYaml: '' // 当前编辑的 YAML 内容
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
            RESOURCE_TYPE () {
                return RESOURCE_TYPE
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
        mounted () {
            this.getDraftList()
            // 监听全局点击事件
            document.addEventListener('click', this.handleGlobalClick)
        },
        beforeDestroy () {
            // 移除全局点击事件监听
            document.removeEventListener('click', this.handleGlobalClick)
        },
        methods: {
            ...mapActions({
                getDraftVersion: 'common/getDraftVersion',
                updatePipelineMode: 'updatePipelineMode'
            }),
            ...mapActions('atom', [
                'setPipelineEditing',
                'saveDraftPipeline',
                'setSaveStatus',
                'requestPipelineSummary',
                'transfer',
                'updateContainer'
            ]),
            async getDraftList () {
                try {
                    const res = await this.getDraftVersion({
                        projectId: this.projectId,
                        pipelineId: this.pipelineId
                    })
                    this.draftList = res
                    this.lasterDraftInfo = this.draftList?.[0]
                } catch (error) {
                    console.log(error)
                }
            },
            // 构建 modelAndSetting 对象
            buildModelAndSetting () {
                const pipeline = Object.assign({}, this.pipeline, {
                    stages: [
                        this.pipeline.stages[0],
                        ...this.pipelineWithoutTrigger.stages
                    ]
                })
                
                return {
                    model: {
                        ...pipeline,
                        name: this.pipelineSetting.pipelineName,
                        desc: this.pipelineSetting.desc
                    },
                    setting: Object.assign(this.pipelineSetting, {
                        failSubscription: undefined,
                        successSubscription: undefined
                    })
                }
            },
            async handleDiff () {
                const modelAndSetting = this.buildModelAndSetting()
                const res = await this.transfer({
                    projectId: this.projectId,
                    pipelineId: this.pipelineId,
                    actionType: 'FULL_MODEL2YAML',
                    modelAndSetting
                })
                this.currentEditYaml = res.newYaml
            },
            handleGlobalClick (event) {
                // 检查点击的是否在 diff 或 rollback 按钮区域内
                const isDiffButton = event.target.closest('.diff-button')
                const isRollbackButton = event.target.closest('.rollback-button')
                const isOptionsArea = event.target.closest('.options')
                // 检查是否点击在弹窗内（弹窗通常有 .bk-dialog 类名）
                const isInDialog = event.target.closest('.bk-dialog-wrapper') || event.target.closest('.bk-dialog')
                
                // 如果点击的不是这些区域，才关闭草稿列表
                if (!isDiffButton && !isRollbackButton && !isOptionsArea && !isInDialog && this.isShowDraftList) {
                    this.isShowDraftList = false
                }
            },
            closeDraftList () {
                this.isShowDraftList = false
            },
            async handleShowDraftList (event) {
                // 阻止事件冒泡，避免立即触发全局点击事件
                event.stopPropagation()
                
                this.isShowDraftList = !this.isShowDraftList
            },
            handleRollback (item) {
                this.$bkInfo({
                    maskClose: false,
                    title: this.$t('confirmRollbackToThisHistory'),
                    subTitle: this.$t('historyRollback', [item.updater, item.updateTime]),
                    confirmFn: () => {
                        // TODO 调用回滚接口
                    }
                })
            },
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
                    const modelAndSetting = this.buildModelAndSetting()

                    // 请求执行构建
                    const { version } = await this.saveDraftPipeline({
                        projectId,
                        pipelineId,
                        baseVersion: this.pipelineInfo?.baseVersion,
                        storageType: this.pipelineMode,
                        modelAndSetting,
                        yaml: pipelineYaml
                    })
                    this.setPipelineEditing(false)

                    await this.requestPipelineSummary(this.$route.params)
                    this.$router.replace({
                        params: {
                            ...this.$route.params,
                            version
                        }
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
    .draft-content {
        position: relative;
        font-size: 12px;
        .last-save-time {
            display: flex;
            align-items: center;
            color: #979BA5;
            cursor: pointer;
            i {
                font-size: 18px;
            }
        }
        .draft-list {
            position: absolute;
            z-index: 2019;
            top: 22px;
            left: 50%;
            transform: translateX(-50%);
            width: 388px;
            background-color: #fff;
            border: 1px solid #DCDEE5;
            box-shadow: 0 2px 6px 0 #0000001a;
            border-radius: 2px;
            .tips-icon {
                font-size: 15px;
            }
            li {
                height: 32px;
                padding: 8px 12px;
                &:first-child {
                    color: #979BA5;
                }
            }
            .draft-item {
                display: flex;
                align-items: center;
                justify-content: space-between;
                cursor: pointer;
                color: #4D4F56;
                &:hover {
                    background: #F5F7FA;
                }
                &:hover .options {
                    visibility: inherit;
                }
                .version-name {
                    background-color: #F0F1F5;
                    border-radius: 2px;
                    padding: 0 8px;
                }
            }
            .draft-item-active {
                background: #E1ECFF;
                .update-info {
                    color: #3A84FF;
                }
            }
            .options {
                // visibility: hidden;
                .rollback-icon {
                    transform: rotate(180deg);
                }
            }
            .update-tip {
                padding: 0 4px;
                background: #FDEED8;
                border-radius: 2px;
                color: #E38B02;
            }
        }
    }
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

</style>
