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
            <bk-form
                v-if="hasOauth"
                label-width="auto"
                form-type="vertical"
                :model="releaseParams"
                :rules="rules"
                ref="releaseForm"
                class="release-pipeline-pac-setting"
                error-display-type="normal"
            >
                <div>
                    <header @click="togglePacCodelibSettingForm" class="release-pac-pipeline-form-header">
                        {{ $t('codelibSetting') }}
                        <i :class="['devops-icon icon-angle-right', {
                            'pac-codelib-form-show': showPacCodelibSetting
                        }]" />
                    </header>
                    <section v-show="showPacCodelibSetting">
                        <bk-form-item
                            required
                            :label="$t('yamlCodeLib')"
                            :desc="$t('aaaa')"
                            desc-type="icon"
                            desc-icon="bk-icon icon-question-circle-shape"
                            property="codelibUrl"
                        >
                            <bk-select
                                searchable
                                v-model="releaseParams.codelibUrl"
                                :show-empty="false"
                                :placeholder="$t('editPage.atomForm.selectTips')"
                            >
                                <template v-if="pacEnableCodelibList.length > 0">
                                    <bk-option v-for="option in pacEnableCodelibList"
                                        :key="option.id"
                                        :id="option.id"
                                        :name="option.name">
                                    </bk-option>
                                </template>
                                <bk-exception v-else scene="part" type="empty">
                                    <span class="no-pac-enable-codelib-yet">
                                        {{ $t('noPacEnableCodelibYet') }}
                                    </span>
                                </bk-exception>
                                <p class="enable-pac-codelib-link" slot="extension" @click="goCodelib">
                                    <i class="devops-icon icon-jump-link" />
                                    {{ $t('goCodelibsEnablePac') }}
                                </p>
                            </bk-select>
                        </bk-form-item>
                        <bk-form-item
                            required
                            :label="$t('yamlDir')"
                            :desc="$t('aaaa')"
                            desc-type="icon"
                            desc-icon="bk-icon icon-question-circle-shape"
                            property="yamlDir"
                        >
                            <bk-input
                                v-model="releaseParams.yamlDir"
                            >
                                <span class="group-text" slot="prepend">.ci/</span>
                            </bk-input>
                        </bk-form-item>
                    </section>
                </div>
                <div class="release-pipeline-pac-submit-conf">
                    <header class="release-pac-pipeline-form-header">
                        {{ $t('submitSetting') }}
                    </header>

                    <bk-form-item required :label="$t('versionDesc')" property="description">
                        <bk-input
                            type="textarea"
                            v-model="releaseParams.description"
                            :placeholder="$t('commitMsgPlaceholder')"
                        />
                        <span class="release-pac-version-desc">
                            {{ $t('commitMsgDesc') }}
                        </span>
                    </bk-form-item>
                    <bk-form-item required :label="$t('targetBranch')" property="targetAction">
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
            </bk-form>
            <div v-else class="pac-oauth-enable">
                <header>
                    <bk-button
                        :loading="oauthing"
                        :disabled="oauthing"
                        theme="primary"
                        size="large"
                        @click="requestOauth"
                    >
                        {{ $t('oauth') }}
                    </bk-button>
                    <span :class="['text-link', {
                        disabled: refreshing
                    }]" @click="refreshOatuStatus">
                        <i class="devops-icon icon-refresh" />
                        {{ $t('refreshOauthStatus') }}
                    </span>
                </header>
                <p class="pac-oauth-tips" v-html="$t('oauthPacTips')">
                </p>
            </div>
        </section>
        <footer v-if="hasOauth" slot="footer" class="release-pipeline-pac-footer">
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
                showPacCodelibSetting: false,
                pacEnableCodelibList: [],
                hasOauth: false,
                oauthing: false,
                refreshing: false,
                releaseParams: {
                    enablePac: false,
                    description: '',
                    codelibUrl: '',
                    yamlDir: '',
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
            rules () {
                return {
                    codelibUrl: [{
                        required: true,
                        message: this.$t('stageReview.requireRule', [this.$t('yamlCodeLib')]),
                        trigger: 'blur'
                    }],
                    yamlDir: [{
                        required: true,
                        message: this.$t('stageReview.requireRule', [this.$t('yamlDir')]),
                        trigger: 'blur'
                    }],
                    description: [{
                        required: true,
                        message: this.$t('stageReview.requireRule', [this.$t('versionDesc')]),
                        trigger: 'blur'
                    }],
                    targetAction: [{
                        required: true,
                        message: this.$t('stageReview.requireRule', [this.$t('targetBranch')]),
                        trigger: 'blur'
                    }]
                }
            },
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
                    await this.$refs?.releaseForm?.validate?.()

                    const { data: { version, versionName } } = await this.releaseDraftPipeline({
                        projectId,
                        pipelineId,
                        version: this.version,
                        params: this.releaseParams
                    })
                    this.updatePipelineInfo({
                        releaseVersion: version,
                        releaseVersionName: versionName,
                        canDebug: false
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
                    this.hideReleaseSlider()
                } catch (e) {
                    if (e.state === 'error') {
                        e.message = e.content
                    }
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
                }
            },
            showReleaseSlider  () {
                this.$emit('input', true)
            },
            hideReleaseSlider () {
                this.$emit('input', false)
            },
            togglePacCodelibSettingForm () {
                this.showPacCodelibSetting = !this.showPacCodelibSetting
            },
            goCodelib () {
                window.open(`/console/codelib/${this.$route.params.projectId}`, '_blank')
            },
            async requestOauth () {
                if (this.oauthing) return
                try {
                    this.oauthing = true
                    // TODO: 请求Oauth,后端提供接口，先占位
                } catch (error) {

                } finally {
                    this.oauthing = false
                }
            },
            async refreshOatuStatus () {
                if (this.refreshing) return
                try {
                    this.refreshing = true
                    // TODO: 刷新Oauth状态
                } catch (error) {
                    console.log(error)
                } finally {
                    this.refreshing = false
                    this.hasOauth = true
                }
            }
        }
    }
</script>

<style lang="scss">
@import '@/scss/conf';
.release-pipeline-pac-form {
    height: calc(100vh - 114px);
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
        .devops-icon.icon-angle-right {
            transition: all 0.3s;
            &.pac-codelib-form-show {
                display: inline-flex;
                transform: rotate(90deg)
            }
        }
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
            padding-left: 24px;
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
        flex: 1;
        padding: 24px;
        display: flex;
        flex-direction: column;
        grid-gap: 24px;
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
.no-pac-enable-codelib-yet {
    color: #C4C6CC;
    font-size: 12px;
}
.enable-pac-codelib-link {
    display: flex;
    align-items: center;
    justify-content: center;
    grid-gap: 6px;
    cursor: pointer;
    &:hover {
        color: $primaryColor;
    }
}
.pac-oauth-enable {
    margin: 16px 24px;
    > header {
        display: flex;
        align-items: center;
        grid-gap: 24px;
    }
    .pac-oauth-tips {
        color: #979BA5;
        line-height: 22px;
        margin-top: 16px;
        font-size: 12px;
    }
}
</style>
