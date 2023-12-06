<template>
    <bk-sideslider
        :is-show.sync="value"
        :width="800"
        @showen="showReleaseSlider"
        @hidden="hideReleaseSlider"
        ext-cls="release-pipeline-side-slider"
    >
        <header slot="header" class="release-pipeline-side-slider-header">
            {{ $t('releasePipeline') }}
            <bk-tag radius="10px">{{ $t('releasePipelineVersion', [versionName]) }}</bk-tag>
            <span>
                {{ $t('releasePipelineBaseVersion', [baseVersionName]) }}
            </span>
        </header>
        <section slot="content" v-bkloading="{ isLoading }" class="release-pipeline-pac-form">
            <div class="release-pipeline-pac-conf">
                <aside class="release-pipeline-pac-conf-leftside">
                    <label for="enablePac">
                        {{ $t('pacMode') }}
                    </label>
                    <bk-switcher
                        :disabled="pacEnabled"
                        theme="primary"
                        name="enablePac"
                        v-model="releaseParams.enablePac"
                        @change="handlePacEnableChange"
                    />
                </aside>
                <aside v-if="releaseParams.enablePac" class="release-pipeline-pac-conf-rightside">
                    <label for="enablePac">
                        {{ $t('codelibSrc') }}
                    </label>
                    <bk-radio-group v-model="releaseParams.scmType">
                        <bk-radio
                            v-for="item in pacSupportScmTypeList"
                            :key="item.id"
                            :value="item.id"
                        >
                            {{ $t(item.value) }}
                        </bk-radio>
                    </bk-radio-group>
                </aside>
            </div>
            <bk-form
                v-if="!releaseParams.enablePac || (releaseParams.enablePac && hasOauth)"
                label-width="auto"
                form-type="vertical"
                :model="releaseParams"
                :rules="rules"
                ref="releaseForm"
                class="release-pipeline-pac-setting"
                error-display-type="normal"
            >
                <div v-if="releaseParams.enablePac && hasOauth">
                    <header @click="togglePacCodelibSettingForm" class="release-pac-pipeline-form-header">
                        {{ $t('codelibSetting') }}
                        <i :class="['devops-icon icon-angle-right', {
                            'pac-codelib-form-show': showPacCodelibSetting
                        }]" />
                    </header>
                    <section v-show="showPacCodelibSetting">
                        <label class="yaml-info-codelib-label" for="yamlCodelib">
                            {{$t('yamlCodeLib')}}
                            <i
                                class="devops-icon icon-question-circle-shape"
                                v-bk-tooltips="$t('aaaa')"
                            />
                        </label>
                        <bk-form-item required property="repoHashId">
                            <bk-select
                                id="yamlCodelib"
                                searchable
                                enable-scroll-load
                                v-model="releaseParams.repoHashId"
                                :scroll-loading="scrollLoadmoreConf"
                                @scroll-end="fetchPacEnableCodelibList"
                                :show-empty="false"
                                :placeholder="$t('editPage.atomForm.selectTips')"
                            >
                                <template v-if="pacEnableCodelibList.length > 0">
                                    <bk-option v-for="option in pacEnableCodelibList"
                                        :key="option.repositoryHashId"
                                        :id="option.repositoryHashId"
                                        :name="option.aliasName">
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
                        <label class="yaml-info-codelib-label" for="yamlFilePath">
                            {{$t('yamlDir')}}
                            <i
                                class="devops-icon icon-question-circle-shape"
                                v-bk-tooltips="$t('aaaa')"
                            />
                        </label>
                        <bk-form-item
                            required
                            property="filePath"
                        >
                            <bk-input
                                v-model="releaseParams.filePath"
                                id="yamlFilePath"
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
                                {{ $t(option, [baseVersionBranch]) }}
                            </bk-radio>
                        </bk-radio-group>
                    </bk-form-item>

                </div>
                <div class="release-pipeline-pac-pipeline-conf">
                    <header class="release-pac-pipeline-form-header">
                        {{ $t('pipelineSetting') }}
                    </header>
                    <PipelineGroupSelector
                        v-model="groupValue"
                        :dynamic-group-editable="false"
                        :pipeline-name="pipelineName"
                        ref="pipelineGroupSelector"
                        :has-manage-permission="isManage"
                    />
                </div>
            </bk-form>
            <div v-if="releaseParams.enablePac && !hasOauth" class="pac-oauth-enable" v-bkloading="{ isLoading: refreshing }">
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
        <footer v-if="!releaseParams.enablePac || hasOauth" slot="footer" class="release-pipeline-pac-footer">
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
    import { mapActions, mapState, mapGetters } from 'vuex'
    import { UPDATE_PIPELINE_INFO } from '@/store/modules/atom/constants'

    export default {
        components: {
            PipelineGroupSelector
        },
        props: {
            value: {
                type: Boolean,
                default: false
            },
            baseVersionName: {
                type: String,
                default: '--'
            },
            versionName: {
                type: String,
                default: '--'
            },
            version: {
                type: [String, Number],
                required: true
            }
        },
        data () {
            return {
                isLoading: false,
                showPacCodelibSetting: false,
                pacEnableCodelibList: [],
                hasOauth: false,
                oauthing: false,
                refreshing: false,
                scrollLoadmoreConf: {
                    isLoading: false,
                    page: 1,
                    pageSize: 10,
                    total: 0,
                    size: 'small'
                },
                releaseParams: {
                    enablePac: false,
                    scmType: '',
                    description: '',
                    repoHashId: '',
                    filePath: '',
                    targetAction: 'CHECKOUT_BRANCH_AND_REQUEST_MERGE'
                },
                groupValue: {
                    labels: this.pipelineSetting?.labels || [],
                    staticViews: []
                }
            }
        },
        computed: {
            ...mapState('atom', [
                'pipelineInfo',
                'pipelineSetting'
            ]),
            ...mapState('pipelines', [
                'isManage'
            ]),
            ...mapGetters('atom', ['isBranchVersion', 'pacEnabled', 'yamlInfo']),
            ...mapState('common', ['pacSupportScmTypeList']),
            baseVersionBranch () {
                return this.pipelineInfo?.baseVersionBranch
            },
            pipelineName () {
                return this.pipelineInfo?.pipelineName
            },

            rules () {
                return {
                    repoHashId: [{
                        required: true,
                        message: this.$t('stageReview.requireRule', [this.$t('yamlCodeLib')]),
                        trigger: 'blur'
                    }],
                    filePath: [{
                        required: true,
                        message: this.$t('stageReview.requireRule', [this.$t('filePath')]),
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
                    this.isBranchVersion ? 'PUSH_BRANCH_AND_REQUEST_MERGE' : 'CHECKOUT_BRANCH_AND_REQUEST_MERGE'
                ]
            }
        },
        watch: {
            value (val) {
                if (val) {
                    this.init()
                }
            },
            yamlInfo: {
                handler: function (val) {
                    if (val) {
                        Object.assign(this.releaseParams, val)
                    }
                },
                immediate: true
            },
            pacEnabled: {
                handler: function (val) {
                    this.releaseParams.enablePac = val
                },
                immediate: true
            },
            'releaseParams.enablePac': {
                handler: function (val) {
                    if (val) {
                        this.init()
                    }
                },
                immediate: true
            },
            'releaseParams.scmType': {
                handler: function (val) {
                    if (val) {
                        this.$nextTick(() => {
                            this.refreshOatuStatus()
                        })
                    }
                },
                immediate: true
            },
            'pipelineSetting.labels': {
                handler: function (val) {
                    this.groupValue.labels = val
                },
                immediate: true
            }
        },
        beforeDestroy () {
            window.__bk_zIndex_manager.zIndex = 2000
        },
        methods: {
            ...mapActions('atom', [
                'releaseDraftPipeline',
                'setSaveStatus'
            ]),
            ...mapActions('common', [
                'isPACOAuth',
                'getSupportPacScmTypeList',
                'getPACRepoList'
            ]),
            async init () {
                if (this.releaseParams.enablePac) {
                    this.isLoading = true
                    await this.getSupportPacScmTypeList()
                    this.releaseParams.scmType = this.pacSupportScmTypeList[0]?.id
                    this.isLoading = false
                    this.$nextTick(() => {
                        this.fetchPacEnableCodelibList()
                    })
                }
            },
            async fetchPacEnableCodelibList () {
                try {
                    if (this.scrollLoadmoreConf.isLoading || (this.scrollLoadmoreConf.total && this.scrollLoadmoreConf.total <= this.pacEnableCodelibList.length)) {
                        return
                    }
                    this.scrollLoadmoreConf.isLoading = true
                    const { projectId } = this.$route.params
                    const { scmType } = this.releaseParams
                    const response = await this.getPACRepoList({
                        projectId,
                        repositoryType: scmType,
                        enablePac: true,
                        permission: 'USE',
                        page: this.scrollLoadmoreConf.page,
                        pageSize: this.scrollLoadmoreConf.pageSize
                    })
                    Object.assign(this.scrollLoadmoreConf, {
                        total: response.total,
                        page: response.page,
                        pageSize: response.pageSize
                    })
                    this.pacEnableCodelibList = [
                        ...this.pacEnableCodelibList,
                        ...response.records
                    ]
                } catch (error) {

                } finally {
                    this.scrollLoadmoreConf.isLoading = false
                }
            },
            handlePacEnableChange (val) {
                this.showPacCodelibSetting = val
            },
            async releasePipeline () {
                const { pipelineId, projectId } = this.$route.params
                try {
                    this.setSaveStatus(true)
                    await this.$refs?.releaseForm?.validate?.()
                    const { repoHashId, scmType, filePath, ...rest } = this.releaseParams
                    const { data: { version, versionName } } = await this.releaseDraftPipeline({
                        projectId,
                        pipelineId,
                        version: this.version,
                        params: {
                            ...rest,
                            staticViews: this.groupValue.staticViews,
                            yamlInfo: rest.enablePac
                                ? {
                                    scmType,
                                    repoHashId,
                                    filePath
                                }
                                : null
                        }
                    })
                    this.$store.commit(`atom/${UPDATE_PIPELINE_INFO}`, {
                        version,
                        versionName,
                        releaseVersion: version,
                        releaseVersionName: versionName,
                        canDebug: false,
                        canRelease: false
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
                                    name: this.pipelineName
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
                this.releaseParams = {
                    enablePac: this.pacEnabled,
                    description: '',
                    scmType: '',
                    ...this.yamlInfo,
                    targetAction: 'CHECKOUT_BRANCH_AND_REQUEST_MERGE'
                }
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
                    this.hasOauth = await this.isPACOAuth({
                        projectId: this.$route.params.projectId,
                        redirectUrIType: 'SPEC',
                        redirectUrl: location.href,
                        repositoryType: this.releaseParams.scmType
                    })
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
                    this.hasOauth = await this.isPACOAuth({
                        projectId: this.$route.params.projectId,
                        repositoryType: this.releaseParams.scmType
                    })
                } catch (error) {
                    console.log(error)
                } finally {
                    this.refreshing = false
                }
            }
        }
    }
</script>

<style lang="scss">
@import '@/scss/conf';
.release-pipeline-side-slider {
    z-index: 2020;
}
.release-pipeline-side-slider-header {
    display: flex;
    align-items: center;
    grid-gap: 12px;
    > span {
        color: #979BA5;
        font-size: 12px;
    }
}
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

.yaml-info-codelib-label {
    position: relative;
    font-size: 12px;
    height: 32px;
    display: inline-flex;
    align-items: center;
    grid-gap: 24px;
    & +.yaml-info-codelib-label {
        margin-top: 20px;
    }
    &:after {
        content: '*';
        position: absolute;
        display: inline-block;
        right: 28px;
        top: 2px;
        color: #EA3636;
    }
    > .devops-icon {
        font-size: 14px;
        color: #979BA5;
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
