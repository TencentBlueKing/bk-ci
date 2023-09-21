<template>
    <div class="pipeline-edit-header">
        <pipeline-bread-crumb>
            <bk-tag>{{ currentVersionName }}</bk-tag>
        </pipeline-bread-crumb>
        <mode-switch />
        <aside class="pipeline-edit-right-aside">
            <bk-button
                :disabled="saveStatus"
                :loading="saveStatus"
                outline
                theme="primary"
                @click="saveDraft"
            >
                {{ $t("saveDraft") }}
            </bk-button>
            <bk-button
                :disabled="!canDebug"
                :loading="executeStatus"
                @click="exec(true)"
            >
                <span class="debug-pipeline-draft-btn">
                    {{ $t("debug") }}
                    <e>|</e>
                    <i @click.stop="goDraftDebugRecord" class="devops-icon icon-txt" />
                </span>
            </bk-button>
            <bk-button
                theme="primary"
                :disabled="btnDisabled"
                :loading="executeStatus"
                :title="canManualStartup ? '' : this.$t('newlist.cannotManual')"
                @click="exec(false)"
            >
                {{ $t("exec") }}
            </bk-button>
            <!-- <more-actions /> -->
            <span :class="['publish-pipeline-btn', {
                'publish-diabled': saveStatus
            }]" @click="showRelaseSlider">
                <i class="devops-icon icon-check-small" />
                {{ $t('release') }}
            </span>
        </aside>
        <bk-sideslider
            :is-show.sync="isReleaseSliderShow"
            :width="800"
            :title="$t('发布流水线')"
        >
            <section slot="content" class="release-pipeline-pac-form">
                <div class="release-pipeline-pac-conf">
                    <aside class="release-pipeline-pac-conf-leftside">
                        <label for="enablePac">
                            {{ $t('PAC模式') }}
                        </label>
                        <bk-switcher theme="primary" name="enablePac" v-model="releaseParams.enablePac" />
                    </aside>
                    <aside class="release-pipeline-pac-conf-rightside">
                        <label for="enablePac">
                            {{ $t('代码库源') }}
                        </label>
                        <bk-radio checked>
                            {{ $t('工蜂') }}
                        </bk-radio>
                    </aside>
                </div>
                <div class="release-pipeline-pac-setting">
                    <p class="release-pipeline-pac-codelib-link release-pac-pipeline-form-header">
                        {{ $t('代码库设置') }}
                        <i class="devops-icon icon-angle-right" />
                    </p>
                    <div class="release-pipeline-pac-submit-conf">
                        <header class="release-pac-pipeline-form-header">
                            {{ $t('提交设置') }}
                        </header>
                        <bk-form form-type="vertical">
                            <bk-form-item :label="$t('版本描述')">
                                <bk-input
                                    type="textarea"
                                    v-model="releaseParams.description"
                                    :placeholder="$t('请输入提交信息')"
                                />
                                <span class="release-pac-version-desc">
                                    {{ $t('将作为 commit message 提交到代码库') }}
                                </span>
                            </bk-form-item>
                            <bk-form-item :label="$t('目标分支')">
                                <bk-radio-group v-model="releaseParams.targetAction">
                                    <bk-radio
                                        v-for="option in targetActionOptions"
                                        class="pac-pipeline-dest-branch-radio"
                                        :key="option.label"
                                        :value="option.value"
                                    >
                                        {{ option.label }}
                                    </bk-radio>
                                </bk-radio-group>
                            </bk-form-item>
                        </bk-form>
                    </div>
                    <div class="release-pipeline-pac-pipeline-conf">
                        <header class="release-pac-pipeline-form-header">
                            {{ $t('流水线设置') }}
                        </header>
                        <PipelineGroupSelector
                            v-model="releaseParams.groupValue"
                            :pipeline-name="pipelineName"
                            ref="pipelineGroupSelector"
                            :has-manage-permission="isManage"
                        />
                    </div>
                </div>
            </section>
            <footer slot="footer" class="release-pipeline-pac-footer">
                <bk-button
                    theme="primary"
                    @click="releasePipeline"
                >
                    {{$t('release')}}
                </bk-button>
                <bk-button

                >
                    {{$t('预览YAML')}}
                </bk-button>
                <bk-button

                >
                    {{$t('cancel')}}
                </bk-button>
            </footer>
        </bk-sideslider>
    </div>
</template>

<script>
    import { mapState, mapGetters, mapActions, mapMutations } from 'vuex'
    import PipelineBreadCrumb from './PipelineBreadCrumb.vue'
    // import MoreActions from './MoreActions.vue'
    import PipelineGroupSelector from '@/components/PipelineActionDialog/PipelineGroupSelector'
    import { PROCESS_API_URL_PREFIX } from '@/store/constants'
    import ModeSwitch from '@/components/ModeSwitch'
    // import { HttpError } from '@/utils/util'
    export default {
        components: {
            PipelineBreadCrumb,
            // MoreActions,
            ModeSwitch,
            PipelineGroupSelector
        },
        data () {
            return {
                isReleaseSliderShow: false,
                releaseParams: {
                    enablePac: false,
                    description: '',
                    targetAction: 'MAIN',
                    groupValue: {
                        labels: [],
                        staticViews: []
                    }
                }
            }
        },
        computed: {
            ...mapState('atom', [
                'pipeline',
                'saveStatus',
                'pipelineSetting',
                'pipelineYaml'
            ]),
            ...mapState('pipelines', ['pipelineInfo', 'executeStatus', 'isManage']),
            ...mapGetters({
                isCurPipelineLocked: 'pipelines/isCurPipelineLocked',
                isEditing: 'atom/isEditing',
                checkPipelineInvalid: 'atom/checkPipelineInvalid'
            }),
            targetActionOptions () {
                return [{
                    label: this.$t('提交到主干'),
                    value: 'COMMIT_TO_MASTER'
                }, {
                    label: this.$t('新建分支并创建 MR'),
                    value: 'CHECKOUT_AND_REQUEST_MERGE'
                }]
            },
            btnDisabled () {
                return this.canDebug || this.executeStatus || !this.canManualStartup || this.isCurPipelineLocked
            },
            canDebug () {
                return this.pipeline?.canDebug ?? false
            },
            canManualStartup () {
                return this.pipelineInfo?.canManualStartup ?? false
            },
            isTemplatePipeline () {
                return this.pipelineInfo?.instanceFromTemplate ?? false
            },
            currentVersionName () {
                if (this.canDebug) {
                    return this.$t('editPage.draftVersion', [this.pipeline?.baseVersionName ?? '--'])
                }
                return this.pipelineInfo?.versionName ?? '--'
            },
            pipelineName () {
                return this.pipelineInfo?.name ?? '--'
            }
        },
        methods: {
            ...mapActions('atom', [
                'setPipelineEditing',
                'saveDraftPipeline',
                'releaseDraftPipeline',
                'setSaveStatus',
                'updateContainer'
            ]),
            ...mapMutations('pipelines', [
                'updatePipelineInfo'
            ]),
            async exec (debug) {
                this.$router.push({
                    name: 'executePreview',
                    query: {
                        ...(debug ? { debug: '' } : {})
                    },
                    params: {
                        ...this.$route.params,
                        version: this.pipelineInfo?.version
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
                    const { pipelineSetting, checkPipelineInvalid, pipeline, pipelineYaml } = this
                    const { inValid, message } = checkPipelineInvalid(pipeline.stages, pipelineSetting)
                    const { projectId, pipelineId } = this.$route.params
                    if (inValid) {
                        throw new Error(message)
                    }
                    // 清除流水线参数渲染过程中添加的key
                    this.formatParams(pipeline)
                    // if (!pipelineId) {
                    //     return this.importPipelineAndSetting(body)
                    // }

                    // 请求执行构建
                    await this.saveDraftPipeline({
                        projectId,
                        pipelineId,
                        baseVersion: pipeline.baseVersion,
                        modelAndSetting: {
                            model: {
                                ...pipeline,
                                name: pipelineSetting.pipelineName,
                                desc: pipelineSetting.desc
                            },
                            setting: pipelineSetting
                        },
                        yaml: pipelineYaml
                    })
                    this.setPipelineEditing(false)
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('editPage.saveDraftSuccess', [pipelineSetting.pipelineName])
                    })
                } catch (e) {
                    this.handleError(e, [
                        {
                            actionId: this.$permissionActionMap.edit,
                            resourceId: this.$permissionResourceMap.pipeline,
                            instanceId: [
                                {
                                    id: this.pipeline.pipelineId,
                                    name: this.pipeline.name
                                }
                            ],
                            projectId: this.$route.params.projectId
                        }
                    ])
                    return {
                        code: e.code,
                        message: e.message
                    }
                } finally {
                    this.setSaveStatus(false)
                }
            },

            saveSetting () {
                const pipelineSetting = this.getPipelineSetting()
                const { projectId, pipelineId } = this.$route.params
                return this.$ajax.post(
                    `/${PROCESS_API_URL_PREFIX}/user/pipelines/${projectId}/${pipelineId}/saveSetting`,
                    pipelineSetting
                )
            },
            showRelaseSlider () {
                this.isReleaseSliderShow = true
            },
            async releasePipeline () {
                console.log('releasePipeline', this.releaseParams)
                const { pipelineId, projectId } = this.$route.params
                try {
                    this.setSaveStatus(true)
                    console.log(this.pipeline.version, this.pipelineInfo.version)
                    const responses = await this.releaseDraftPipeline({
                        projectId,
                        pipelineId,
                        version: this.pipelineInfo.version,
                        params: this.releaseParams
                    })
                    console.log(responses)
                    this.$showTips({
                        message: this.$t('saveSuc'),
                        theme: 'success'
                    })
                } catch (e) {
                    this.handleError(e, [
                        {
                            actionId: this.$permissionActionMap.edit,
                            resourceId: this.$permissionResourceMap.pipeline,
                            instanceId: [
                                {
                                    id: pipelineId,
                                    name: this.pipeline.name
                                }
                            ],
                            projectId
                        }
                    ])
                    return {
                        code: e.code,
                        message: e.message
                    }
                } finally {
                    this.setSaveStatus(false)
                }
            },
            goDraftDebugRecord () {
                this.$router.push({
                    name: 'draftDebugRecord',
                    params: {
                        version: this.pipelineInfo?.version
                    }
                })
            }
        }
    }
</script>

<style lang="scss">
@import '@/scss/conf';
.pipeline-edit-header {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  padding: 0 0 0 14px;
  align-self: stretch;
  .debug-pipeline-draft-btn {
    display: flex;
    align-items: center;
    grid-gap: 8px;
    > e {
        color: #DCDEE5;
    }
    > i:hover {
        color: $primaryColor;
    }
  }
  .pipeline-edit-right-aside {
    display: grid;
    grid-gap: 10px;
    grid-auto-flow: column;
    height: 100%;
    align-items: center;
    .publish-pipeline-btn {
        height: 100%;
        display: flex;
        align-items: center;
        color: white;
        background: $primaryColor;
        font-size: 14px;
        padding: 0 20px;
        cursor: pointer;
        &.publish-diabled {
            background: #DCDEE5;
            cursor: not-allowed;
        }
        .icon-check-small {
            font-size: 18px;
        }
        &.disabled {
            background: #DCDEE5;
            cursor: not-allowed;
        }
    }
  }

  .release-pipeline-pac-form {
    height: calc(100vh - 108px);
    overflow: auto;
    .release-pac-pipeline-form-header {
        display: flex;
        justify-content: space-between;
        font-weight: 700;
        font-size: 14px;
        border-bottom: 1px solid #DCDEE5;
        padding-bottom: 8px;
        margin-bottom: 16px;
    }
    .release-pipeline-pac-conf {
        display: flex;
        background: #FAFBFD;
        height: 80px;
        align-items: center;
        font-size: 12px;
        >:first-child {
            border-right: 1px solid #DCDEE5;
        }
        .release-pipeline-pac-conf-leftside,
        .release-pipeline-pac-conf-rightside {
            display: flex;
            flex-direction: column;
            padding-left: 32px;
            justify-content: center;
            grid-gap: 8px;
            flex: 1;

            &.release-pipeline-pac-conf-leftside {
                width: 176px;
                flex-shrink: 0;
            }
        }
    }
    .release-pipeline-pac-setting {
        padding: 24px;
        .release-pac-version-desc {
            font-size: 12px;
            color: #979BA5;
            letter-spacing: 0;
            line-height: 20px;
        }
        .pac-pipeline-dest-branch-radio {
            margin-right: 24px;
        }
    }
  }
  .release-pipeline-pac-footer {
    padding: 0 24px;
  }
}
</style>
