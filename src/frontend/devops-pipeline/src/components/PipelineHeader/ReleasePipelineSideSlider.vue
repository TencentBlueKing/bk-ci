<template>
    <bk-sideslider
        :is-show.sync="value"
        :width="800"
        @showen="showReleaseSlider"
        @hidden="hideReleaseSlider"
        :title="$t('releasePipeline')"
    >
        <section slot="content" class="release-pipeline-pac-form">
            <div class="release-pipeline-pac-conf">
                <aside class="release-pipeline-pac-conf-leftside">
                    <label for="enablePac">
                        {{ $t('pacMode') }}
                    </label>
                    <bk-switcher theme="primary" name="enablePac" v-model="releaseParams.enablePac" />
                </aside>
                <aside class="release-pipeline-pac-conf-rightside">
                    <label for="enablePac">
                        {{ $t('codelibSrc') }}
                    </label>
                    <bk-radio checked>
                        {{ $t('gitcode') }}
                    </bk-radio>
                </aside>
            </div>
            <div class="release-pipeline-pac-setting">
                <p class="release-pipeline-pac-codelib-link release-pac-pipeline-form-header">
                    {{ $t('codelibSetting') }}
                    <i class="devops-icon icon-angle-right" />
                </p>
                <div class="release-pipeline-pac-submit-conf">
                    <header class="release-pac-pipeline-form-header">
                        {{ $t('submitSetting') }}
                    </header>
                    <bk-form form-type="vertical">
                        <bk-form-item :label="$t('versionDesc')">
                            <bk-input
                                type="textarea"
                                v-model="releaseParams.description"
                                :placeholder="$t('commitMsgPlaceholder')"
                            />
                            <span class="release-pac-version-desc">
                                {{ $t('commitMsgDesc') }}
                            </span>
                        </bk-form-item>
                        <bk-form-item :label="$t('targetBranch')">
                            <bk-radio-group v-model="releaseParams.targetAction">
                                <bk-radio
                                    v-for="option in targetActionOptions"
                                    class="pac-pipeline-dest-branch-radio"
                                    :key="option"
                                    :value="option"
                                >
                                    {{ $t(option) }}
                                </bk-radio>
                            </bk-radio-group>
                        </bk-form-item>
                    </bk-form>
                </div>
                <div class="release-pipeline-pac-pipeline-conf">
                    <header class="release-pac-pipeline-form-header">
                        {{ $t('pipelineSetting') }}
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
                @click="hideReleaseSlider"
            >
                {{$t('cancel')}}
            </bk-button>
        </footer>
    </bk-sideslider>
</template>

<script>
    import PipelineGroupSelector from '@/components/PipelineActionDialog/PipelineGroupSelector'
    import { mapActions, mapState, mapMutations } from 'vuex'
    export default {
        components: {
            PipelineGroupSelector
        },
        props: {
            value: {
                type: Boolean,
                default: false
            },
            version: {
                type: [String, Number],
                required: true
            }
        },
        data () {
            return {
                releaseParams: {
                    enablePac: false,
                    description: '',
                    targetAction: 'COMMIT_TO_MASTER',
                    groupValue: {
                        labels: [],
                        staticViews: []
                    }
                }
            }
        },
        computed: {
            ...mapState('pipelines', ['pipelineInfo', 'isManage']),
            targetActionOptions () {
                return [
                    'COMMIT_TO_MASTER',
                    'CHECKOUT_AND_REQUEST_MERGE'
                ]
            }
        },
        methods: {
            ...mapActions('atom', [
                'releaseDraftPipeline',
                'setSaveStatus'
            ]),
            ...mapMutations('pipelines', [
                'updatePipelineInfo'
            ]),
            async releasePipeline () {
                const { pipelineId, projectId } = this.$route.params
                try {
                    this.setSaveStatus(true)
                    const { data } = await this.releaseDraftPipeline({
                        projectId,
                        pipelineId,
                        version: this.version,
                        params: this.releaseParams
                    })
                    console.log(data)
                    this.updatePipelineInfo({
                        key: 'version',
                        value: data.version
                    })
                    this.updatePipelineInfo({
                        key: 'versionName',
                        value: data.versionName
                    })
                    this.updatePipelineInfo({
                        key: 'canDebug',
                        value: false
                    })
                    const tipsI18nKey = this.releaseParams.enablePac ? 'pacPipelineReleaseTips' : 'releaseTips'
                    const tipsArrayLength = this.releaseParams.enablePac ? 4 : 2
                    const h = this.$createElement
                    const instance = this.$bkInfo({
                        type: 'success',
                        title: this.$t('releaseSuc'),
                        width: 600,
                        showFooter: false,
                        subHeader: h('div', {}, [
                            h('p', {
                                attrs: {
                                    class: 'pipeline-release-suc-tips'
                                }
                            }, Array.from({ length: tipsArrayLength }).map((_, index) => h('span', {
                                domProps: {
                                    innerHTML: this.$t(`${tipsI18nKey}${index}`)
                                }
                            }))),
                            h('footer', {
                                style: {
                                    display: 'flex',
                                    gridGap: '10px',
                                    marginTop: '20px',
                                    justifyContent: 'center'
                                }
                            }, [
                                this.releaseParams.enablePac
                                    ? h('bk-button', {
                                        props: {
                                            theme: 'primary'
                                        },
                                        on: {
                                            click: () => {
                                                this.$bkInfo.close(instance.id)
                                            }
                                        }
                                    }, this.$t('查看流水线'))
                                    : null,
                                h('bk-button', {
                                    on: {
                                        click: () => {
                                            this.$bkInfo.close(instance.id)
                                        }
                                    }
                                }, this.$t('返回'))
                            ])
                        ])
                    })
                } catch (e) {
                    this.handleError(e, [
                        {
                            actionId: this.$permissionActionMap.edit,
                            resourceId: this.$permissionResourceMap.pipeline,
                            instanceId: [
                                {
                                    id: pipelineId,
                                    name: this.pipelineInfo?.pipelineName
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
                    this.hideReleaseSlider()
                }
            },
            showReleaseSlider  () {
                this.$emit('input', true)
            },
            hideReleaseSlider () {
                this.$emit('input', false)
            }
        }
    }
</script>

<style lang="scss">
    .release-pipeline-pac-form {
    height: calc(100vh - 108px);
    overflow: auto;
    .release-pac-pipeline-form-header {
        display: flex;
        align-items: center;
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

.pipeline-release-suc-tips {
    background: #F5F6FA;
    display: flex;
    flex-direction: column;
    border-radius: 2px;
    padding: 14px 20px;
    letter-spacing: 0;
    line-height: 22px;
    text-align: left;
    > span > e {
        color: #FF9C01;
    }
  }

</style>
