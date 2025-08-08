<template>
    <bk-sideslider
        :is-show.sync="value"
        :width="640"
        @shown="showReleaseSlider"
        @hidden="hideReleaseSlider"
        ext-cls="release-pipeline-side-slider"
    >
        <header
            slot="header"
            :class="['release-pipeline-side-slider-header', {
                'has-pac-tag': pacEnabled
            }]"
        >
            {{ $t("releasePipeline") }}
            <PacTag
                v-if="pacEnabled"
                :info="pipelineInfo?.yamlInfo"
            />
            <span
                v-bk-overflow-tips
                class="release-pipeline-new-version"
            >
                {{ $t("releasePipelineVersion",[newReleaseVersionName]) }}
            </span>
            <span v-bk-overflow-tips>
                {{ $t("releasePipelineBaseVersion", [draftBaseVersionName]) }}
            </span>
        </header>
        <section
            slot="content"
            v-bkloading="{ isLoading: isLoading || releasing }"
            class="release-pipeline-pac-form"
        >
            <div
                v-if="!pacEnabled"
                class="release-pipeline-pac-conf"
            >
                <aside class="release-pipeline-pac-conf-leftside">
                    <label for="enablePac">
                        {{ $t("pacMode") }}
                        <span
                            class="devops-icon icon-info-circle"
                            v-bk-tooltips="pacDesc"
                        />
                    </label>
                    <bk-switcher
                        :disabled="pacEnabled || isTemplatePipeline"
                        theme="primary"
                        name="enablePac"
                        :title="isTemplatePipeline ? $t('templateYamlNotSupport') : ''"
                        v-model="releaseParams.enablePac"
                        @change="handlePacEnableChange"
                    />
                </aside>
                <aside
                    v-if="releaseParams.enablePac && hasPacSupportScmTypeList"
                    class="release-pipeline-pac-conf-rightside"
                >
                    <label for="enablePac">
                        {{ $t("codelibSrc") }}
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
                    <header
                        @click="togglePacCodelibSettingForm"
                        class="release-pac-pipeline-form-header"
                    >
                        {{ $t("codelibSetting") }}
                        <i
                            :class="[
                                'devops-icon icon-angle-right',
                                {
                                    'pac-codelib-form-show': showPacCodelibSetting
                                }
                            ]"
                        />
                    </header>
                    <section v-show="showPacCodelibSetting">
                        <label
                            class="yaml-info-codelib-label"
                            for="yamlCodelib"
                        >
                            {{ $t("yamlCodeLib") }}
                            <i
                                class="devops-icon icon-info-circle-shape"
                                v-bk-tooltips="$t('yamlCodeLibDesc')"
                            />
                        </label>
                        <bk-form-item
                            required
                            property="repoHashId"
                        >
                            <bk-select
                                id="yamlCodelib"
                                :disabled="pacEnabled"
                                searchable
                                enable-scroll-load
                                v-model="releaseParams.repoHashId"
                                :scroll-loading="scrollLoadmoreConf"
                                :loading="isInitPacRepo"
                                :show-empty="false"
                                :placeholder="$t('editPage.atomForm.selectTips')"
                                :z-index="2600"
                                @scroll-end="fetchPacEnableCodelibList(false)"
                                @toggle="refreshPacEnableCodelibList"
                            >
                                <template v-if="pacEnableCodelibList.length">
                                    <bk-option
                                        v-for="option in pacEnableCodelibList"
                                        :key="option.repositoryHashId"
                                        :id="option.repositoryHashId"
                                        :name="option.aliasName"
                                    >
                                    </bk-option>
                                </template>
                                <bk-loading
                                    is-loading
                                    mode="spin"
                                    size="small"
                                    v-else-if="isInitPacRepo"
                                >
                                </bk-loading>
                                <bk-exception
                                    v-else
                                    scene="part"
                                    type="empty"
                                >
                                    <span class="no-pac-enable-codelib-yet">
                                        {{ $t("noPacEnableCodelibYet") }}
                                    </span>
                                </bk-exception>
                                <p
                                    class="enable-pac-codelib-link"
                                    slot="extension"
                                    @click="goCodelib"
                                >
                                    <i class="devops-icon icon-jump-link" />
                                    {{ $t("goCodelibsEnablePac") }}
                                </p>
                            </bk-select>
                        </bk-form-item>
                        <label
                            class="yaml-info-codelib-label"
                            for="yamlFilePath"
                        >
                            {{ $t("yamlDir") }}
                            <i
                                class="devops-icon icon-info-circle-shape"
                                v-bk-tooltips="$t('yamlDirDesc')"
                            />
                        </label>
                        <bk-form-item
                            required
                            property="filePath"
                        >
                            <bk-input
                                :disabled="pacEnabled"
                                v-model="releaseParams.filePath"
                                id="yamlFilePath"
                                :placeholder="$t('yamlFilePathPlaceholder')"
                            >
                                <span
                                    class="group-text"
                                    slot="prepend"
                                >{{ filePathDir }}</span>
                            </bk-input>
                        </bk-form-item>
                    </section>
                </div>
                <div class="release-pipeline-pac-submit-conf">
                    <header class="release-pac-pipeline-form-header">
                        {{ $t("submitSetting") }}
                    </header>

                    <bk-form-item
                        :required="releaseParams.enablePac"
                        :label="$t('versionDesc')"
                        property="description"
                    >
                        <bk-input
                            type="textarea"
                            maxlength="512"
                            v-model="releaseParams.description"
                            :placeholder="$t(
                                releaseParams.enablePac
                                    ? 'commitMsgPlaceholder'
                                    : 'versionDescPlaceholder'
                            )
                            "
                        />
                        <span
                            v-if="releaseParams.enablePac"
                            class="release-pac-version-desc"
                        >
                            {{ $t("commitMsgDesc") }}
                        </span>
                    </bk-form-item>
                    <bk-form-item
                        v-if="releaseParams.enablePac"
                        required
                        :label="$t('targetBranch')"
                        property="targetAction"
                    >
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
                    <bk-form-item
                        v-if="isCommitToBranch"
                        :label="$t('targetBranch')"
                    >
                        <bk-select
                            v-model="releaseParams.targetBranch"
                            :placeholder="$t('editPage.selectBranchTips')"
                            searchable
                            :remote-method="handleBranchSerach"
                        >
                            <bk-option
                                v-for="branch in branchList"
                                :key="branch"
                                :id="branch"
                                :name="branch"
                            >
                            </bk-option>
                        </bk-select>
                    </bk-form-item>
                </div>
            </bk-form>
            <div
                v-if="releaseParams.enablePac && !hasOauth"
                class="pac-oauth-enable"
                v-bkloading="{ isLoading: refreshing }"
            >
                <header v-if="hasPacSupportScmTypeList">
                    <bk-button
                        :loading="oauthing"
                        :disabled="oauthing"
                        theme="primary"
                        size="large"
                        @click="requestOauth"
                    >
                        {{ $t("oauth") }}
                    </bk-button>
                    <span
                        :class="[
                            'text-link',
                            {
                                disabled: refreshing
                            }
                        ]"
                        @click="refreshOatuStatus"
                    >
                        <i class="devops-icon icon-refresh" />
                        {{ $t("refreshOauthStatus") }}
                    </span>
                </header>
                <p
                    class="pac-oauth-tips"
                    v-html="$t(hasPacSupportScmTypeList ? 'oauthPacTips' : 'withoutOauthCodelib')"
                ></p>
            </div>
        </section>
        <footer
            v-if="!releaseParams.enablePac || hasOauth"
            slot="footer"
            class="release-pipeline-pac-footer"
        >
            <bk-button
                theme="primary"
                :loading="releasing"
                :disabled="releasing"
                @click="releasePipeline"
            >
                {{ $t("release") }}
            </bk-button>
            <version-diff-entry
                v-if="releaseParams.enablePac"
                :text="false"
                theme=""
                :disabled="releasing"
                :can-switch-version="false"
                :version="pipelineInfo?.releaseVersion"
                :latest-version="version"
            >
                {{ $t("checkDiff") }}
            </version-diff-entry>
            <bk-button
                :disabled="releasing"
                @click="cancelRelease"
            >
                {{ $t("cancelRelease") }}
            </bk-button>
        </footer>
    </bk-sideslider>
</template>

<script>
    import Logo from '@/components/Logo'
    import PacTag from '@/components/PacTag.vue'
    import VersionDiffEntry from '@/components/PipelineDetailTabs/VersionDiffEntry'
    import { TARGET_ACTION_ENUM, VERSION_STATUS_ENUM } from '@/utils/pipelineConst'
    import { mapActions, mapGetters, mapState } from 'vuex'

    export default {
        components: {
            VersionDiffEntry,
            PacTag
        },
        props: {
            value: {
                type: Boolean,
                default: false
            },
            draftBaseVersionName: {
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
                releasing: false,
                showPacCodelibSetting: false,
                pacEnableCodelibList: [],
                hasOauth: false,
                oauthing: false,
                refreshing: false,
                filePathDir: '.ci/',
                newReleaseVersionName: '--',
                branchList: [],
                scrollLoadmoreConf: {
                    isLoading: false,
                    page: 1,
                    pageSize: 10,
                    total: 0,
                    size: 'mini'
                },
                isInitPacRepo: false,
                releaseParams: {
                    enablePac: false,
                    targetBranch: '',
                    scmType: '',
                    description: '',
                    repoHashId: '',
                    filePath: '',
                    targetAction: ''
                }
            }
        },
        computed: {
            ...mapState('atom', [
                'pipelineInfo',
                'pipeline',
                'pipelineSetting'
            ]),
            ...mapState('pipelines', ['isManage']),
            ...mapGetters('atom', ['pacEnabled', 'yamlInfo']),
            ...mapState('common', ['pacSupportScmTypeList']),
            pacDesc () {
                return {
                    content: this.$t('pacDesc'),
                    maxWidth: 300
                }
            },
            baseVersionBranch () {
                return this.pipelineInfo?.baseVersionName || '--'
            },
            pipelineName () {
                return this.pipelineSetting?.pipelineName
            },
            isTemplatePipeline () {
                return this.pipelineInfo?.instanceFromTemplate ?? false
            },
            isCommitToBranch () {
                return this.releaseParams.targetAction === TARGET_ACTION_ENUM.COMMIT_TO_BRANCH
            },
            viewNames () {
                return this.pipelineInfo?.viewNames || []
            },
            rules () {
                return {
                    repoHashId: [
                        {
                            required: true,
                            message: this.$t('stageReview.requireRule', [this.$t('yamlCodeLib')]),
                            trigger: 'blur'
                        }
                    ],
                    filePath: [
                        {
                            required: true,
                            regex: /\.ya?ml$/,
                            message: this.$t('yamlFilePathErrorTip'),
                            trigger: 'blur'
                        }
                    ],
                    description: [
                        {
                            required: this.releaseParams.enablePac,
                            message: this.$t('stageReview.requireRule', [this.$t('versionDesc')]),
                            trigger: 'blur'
                        }
                    ],
                    targetAction: [
                        {
                            required: true,
                            message: this.$t('stageReview.requireRule', [this.$t('targetBranch')]),
                            trigger: 'blur'
                        }
                    ]
                }
            },
            isDraftBaseBranchVersion () {
                return this.pipelineInfo?.baseVersionStatus === VERSION_STATUS_ENUM.BRANCH
            },
            targetActionOptions () {
                return [
                    ...(
                        this.isDraftBaseBranchVersion
                            ? [
                                TARGET_ACTION_ENUM.COMMIT_TO_SOURCE_BRANCH,
                                TARGET_ACTION_ENUM.COMMIT_TO_SOURCE_BRANCH_AND_REQUEST_MERGE
                                // 提交到指定分支
                            ]
                            : [TARGET_ACTION_ENUM.CHECKOUT_BRANCH_AND_REQUEST_MERGE]
                    ),
                    TARGET_ACTION_ENUM.COMMIT_TO_MASTER,
                    TARGET_ACTION_ENUM.COMMIT_TO_BRANCH
                ]
            },
            hasPacSupportScmTypeList () {
                return this.pacSupportScmTypeList?.length > 0
            },
            canManualStartup () {
                try {
                    const manualAtom = this.pipeline?.stages?.[0]?.containers[0]?.elements?.find(e => e.atomCode === 'manualTrigger')
                    return manualAtom?.additionalOptions?.enable
                } catch (error) {
                    return false
                }
            },
            prefetchParams () {
                return {
                    targetBranch: this.releaseParams.targetBranch,
                    targetAction: this.releaseParams.targetAction,
                    repoHashId: this.releaseParams.repoHashId
                }
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
                        Object.assign(this.releaseParams, {
                            ...val,
                            filePath: this.trimCIPrefix(val.filePath)
                        })
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
                    if (val && this.pacEnabled) {
                        this.$nextTick(() => {
                            this.refreshOatuStatus()
                        })
                    }
                },
                immediate: true
            },
            isCommitToBranch: {
                handler: function (val) {
                    if (val) {
                        this.fetchBranchList()
                    }
                },
                immediate: true
            },
            'releaseParams.repoHashId': {
                handler: function (val) {
                    if (this.isCommitToBranch) {
                        this.releaseParams.targetBranch = ''
                        this.$nextTick(() => {
                            this.fetchBranchList()
                        })
                    }
                }
            },
            prefetchParams: {
                deep: true,
                handler: function (val) {
                    this.prefetchReleaseVersion(val)
                }
            }
        },
        mounted () {
            this.preZIndex = window.__bk_zIndex_manager.zIndex
            window.__bk_zIndex_manager.zIndex = 2050
        },
        beforeDestroy () {
            window.__bk_zIndex_manager.zIndex = this.preZIndex
        },
        methods: {
            ...mapActions('atom', [
                'releaseDraftPipeline',
                'requestPipelineSummary',
                'setSaveStatus',
                'prefetchPipelineVersion',
                'requestScmBranchList'
            ]),
            ...mapActions('common', ['isPACOAuth', 'getSupportPacScmTypeList', 'getPACRepoList']),
            async init () {
                try {
                    this.isLoading = true
                    const { enablePac } = this.releaseParams
                    await Promise.all([
                        ...(enablePac
                            ? [
                                this.getSupportPacScmTypeList()
                            ]
                            : []
                        ),
                        this.prefetchReleaseVersion(this.prefetchParams)
                    ])

                    if (enablePac && this.hasPacSupportScmTypeList) {
                        this.releaseParams.scmType = this.pacSupportScmTypeList[0]?.id
                        this.$nextTick(() => {
                            this.fetchPacEnableCodelibList(true)
                            if (this.isDraftBaseBranchVersion) {
                                this.releaseParams.targetAction = TARGET_ACTION_ENUM.COMMIT_TO_SOURCE_BRANCH_AND_REQUEST_MERGE
                            }
                        })
                    }
                } catch (error) {
                    this.handleError(error, {
                        projectId: this.$route.params.projectId,
                        resourceCode: this.$route.params.pipelineId,
                        resourceType: 'pipeline',
                        action: this.$permissionResourceAction.EDIT
                    })
                } finally {
                    this.isLoading = false
                }
            },
            async prefetchReleaseVersion (params) {
                try {
                    if (!this.version || (params.targetAction === TARGET_ACTION_ENUM.COMMIT_TO_BRANCH && !params.targetBranch)) {
                        return
                    }
                    const newReleaseVersion = await this.prefetchPipelineVersion({
                        ...this.$route.params,
                        version: this.version,
                        ...params
                    })
                    this.newReleaseVersionName = newReleaseVersion?.newVersionName || '--'
                } catch (error) {
                    this.handleError(error, {
                        projectId: this.$route.params.projectId,
                        resourceCode: this.$route.params.pipelineId,
                        resourceType: 'pipeline',
                        action: this.$permissionResourceAction.EDIT
                    })
                }
            },
            async fetchPacEnableCodelibList (init = false) {
                try {
                    if (
                        this.isInitPacRepo
                        || this.scrollLoadmoreConf.isLoading
                        || (this.scrollLoadmoreConf.total > 0 && this.scrollLoadmoreConf.total <= this.pacEnableCodelibList.length)
                    ) {
                        return
                    }
                    if (init) {
                        this.scrollLoadmoreConf.page = 1
                        this.pacEnableCodelibList = []
                        this.isInitPacRepo = true
                    } else {
                        this.scrollLoadmoreConf.isLoading = true
                        this.scrollLoadmoreConf.page += 1
                    }
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
                        total: response.count,
                        page: response.page,
                        pageSize: response.pageSize
                    })
                    this.pacEnableCodelibList = [...this.pacEnableCodelibList, ...response.records]
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message || error
                    })
                } finally {
                    this.scrollLoadmoreConf.isLoading = false
                    this.isInitPacRepo = false
                }
            },
            refreshPacEnableCodelibList (show) {
                if (show) {
                    this.fetchPacEnableCodelibList(true)
                }
            },
            async fetchBranchList (search) {
                try {
                    const res = await this.requestScmBranchList({
                        projectId: this.$route.params.projectId,
                        repositoryHashId: this.releaseParams.repoHashId,
                        search
                    })
                    this.branchList = Array.from(new Set(res.data))
                } catch (error) {
                    console.error(error)
                }
            },
            handleBranchSerach (keyword) {
                return this.fetchBranchList(keyword)
            },
            handlePacEnableChange (val) {
                this.showPacCodelibSetting = val
            },
            async releasePipeline () {
                const { pipelineId, projectId } = this.$route.params
                try {
                    if (this.releasing) return
                    this.releasing = true
                    this.setSaveStatus(true)
                    await this.$refs?.releaseForm?.validate?.()
                    const {
                        fileUrl,
                        webUrl,
                        pathWithNamespace,
                        repoHashId,
                        scmType,
                        filePath,
                        targetAction,
                        ...rest
                    } = this.releaseParams
                    const {
                        data: { versionName, targetUrl, updateBuildNo }
                    } = await this.releaseDraftPipeline({
                        projectId,
                        pipelineId,
                        version: this.version,
                        params: {
                            ...rest,
                            ...(rest.enablePac
                                ? {
                                    targetAction
                                }
                                : {}
                            ),
                            yamlInfo: rest.enablePac
                                ? {
                                    scmType,
                                    repoHashId,
                                    filePath: `${this.filePathDir}${filePath}`
                                }
                                : null
                        }
                    })

                    await this.requestPipelineSummary(this.$route.params)

                    const tipsI18nKey = this.releaseParams.enablePac
                        ? 'pacPipelineReleaseTips'
                        : 'releaseTips'
                    const tipsArrayLength = this.releaseParams.enablePac ? 2 : 0
                    const isPacMR
                        = this.releaseParams.enablePac
                            && [
                                TARGET_ACTION_ENUM.CHECKOUT_BRANCH_AND_REQUEST_MERGE,
                                TARGET_ACTION_ENUM.COMMIT_TO_SOURCE_BRANCH_AND_REQUEST_MERGE
                            ].includes(this.releaseParams.targetAction)
                    const h = this.$createElement
                    const instance = this.$bkInfo({
                        width: 600,
                        position: {
                            top: 100,
                            left: 100
                        },
                        extCls: 'release-info-dialog',
                        showFooter: false,
                        subHeader: h('div', {
                            attrs: {
                                class: 'release-info-content'
                            }
                        }, [
                            isPacMR
                                ? h('span', {
                                    attrs: {
                                        class: 'part-of-mr'
                                    }
                                })
                                : h('i', {
                                    attrs: {
                                        class: 'devops-icon icon-check-small release-success-icon'
                                    }
                                }),
                            h('p', {
                                attrs: {
                                    class: 'release-info-title'
                                }
                            }, this.$t(isPacMR ? 'pacMRRelaseTips' : 'releaseSuc')),
                            h('h3', {
                                class: 'release-info-text',
                                domProps: {
                                    innerHTML: this.$t(isPacMR ? 'pacMRRelaseSuc' : 'relaseSucTips', [
                                        versionName
                                    ])
                                }
                            }),
                            updateBuildNo && !tipsArrayLength
                                ? h('div', { class: 'warning-box' }, [
                                    h(Logo, { size: 14, name: 'warning-circle-fill' }),
                                    h('span', this.$t('buildNoBaseline.resetRequiredTips'))
                                ])
                                : null,
                            ...(tipsArrayLength > 0
                                ? [
                                    h(
                                        'p',
                                        {
                                            attrs: {
                                                class: 'pipeline-release-suc-tips'
                                            }
                                        },
                                        [
                                            h('h3', {}, this.$t('pacPipelineConfRule')),
                                            ...Array.from({ length: tipsArrayLength }).map((_, index) => {
                                                if (index === 1 && this.releaseParams.enablePac) {
                                                    return h('ul', {}, [
                                                        h('span', {}, this.$t(`${tipsI18nKey}${index}`)),
                                                        Array(3)
                                                            .fill(0)
                                                            .map((_, i) =>
                                                                h(
                                                                    'li',
                                                                    {
                                                                        domProps: {
                                                                            innerHTML: this.$t(
                                                                                `${tipsI18nKey}${index}-${i}`
                                                                            )
                                                                        },
                                                                        style: {
                                                                            marginLeft: '32px',
                                                                            listStyle: 'disc'
                                                                        }
                                                                    }
                                                                )
                                                            )
                                                    ])
                                                }
                                                return h('span', {}, this.$t(`${tipsI18nKey}${index}`))
                                            })
                                        ]
                                    )]
                                : []),
                            h(
                                'footer',
                                {
                                    style: {
                                        display: 'flex',
                                        gridGap: '10px',
                                        marginTop: '20px',
                                        justifyContent: 'center'
                                    }
                                },
                                [
                                    this.releaseParams.enablePac && isPacMR
                                        ? h(
                                            'bk-button',
                                            {
                                                props: {
                                                    theme: 'primary'
                                                },
                                                on: {
                                                    click: () => {
                                                        this.$bkInfo.close(instance.id)
                                                        window.open(targetUrl, '_blank')
                                                    }
                                                }
                                            },
                                            this.$t('dealMR')
                                        )
                                        : h(
                                            'bk-button',
                                            {
                                                props: {
                                                    theme: 'primary'
                                                },
                                                on: {
                                                    click: () => {
                                                        this.$bkInfo.close(instance.id)
                                                        if (!updateBuildNo) {
                                                            this.$router.push({
                                                                name: 'executePreview',
                                                                params: {
                                                                    ...this.$route.params,
                                                                    version: this.pipelineInfo?.releaseVersion
                                                                }
                                                            })
                                                        } else {
                                                            this.$router.push({
                                                                name: 'pipelinesHistory',
                                                                params: {
                                                                    projectId,
                                                                    pipelineId,
                                                                    type: 'pipeline',
                                                                    isDirectShowVersion: true,
                                                                    version: this.pipelineInfo?.releaseVersion
                                                                }
                                                            })
                                                        }
                                                    }
                                                }
                                            },
                                            this.$t(!updateBuildNo ? 'goExec' : 'buildNoBaseline.goReset')
                                        ),
                                    h(
                                        'bk-button',
                                        {
                                            on: {
                                                click: () => {
                                                    this.$bkInfo.close(instance.id)
                                                    !updateBuildNo && this.$router.push({
                                                        name: 'pipelinesHistory',
                                                        params: {
                                                            projectId,
                                                            pipelineId,
                                                            type: 'pipeline',
                                                            version: this.pipelineInfo?.releaseVersion
                                                        }
                                                    })
                                                }
                                            }
                                        },
                                        this.$t(!updateBuildNo ? 'checkPipeline' : 'return')
                                    )

                                ]
                            )
                        ])
                    })
                    this.hideReleaseSlider()
                } catch (e) {
                    if (e.state === 'error') {
                        e.message = e.content
                    }
                    this.handleError(e, {
                        projectId,
                        resourceCode: pipelineId,
                        action: this.$permissionResourceAction.EDIT
                    })
                    return {
                        code: e.code,
                        message: e.message
                    }
                } finally {
                    this.setSaveStatus(false)
                    this.releasing = false
                }
            },
            showReleaseSlider () {
                this.$emit('input', true)
            },
            hideReleaseSlider () {
                this.cancelRelease()
                this.releaseParams = {
                    enablePac: this.pacEnabled,
                    description: '',
                    ...(this.yamlInfo
                        ? {
                            ...this.yamlInfo,
                            filePath: this.trimCIPrefix(this.yamlInfo.filePath)
                        }
                        : {}),
                    targetAction: ''
                }
            },
            cancelRelease () {
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
                    const res = await this.isPACOAuth({
                        projectId: this.$route.params.projectId,
                        redirectUrIType: 'SPEC',
                        redirectUrl: location.href,
                        repositoryType: this.releaseParams.scmType
                    })
                    if (res?.status === 403) {
                        window.open(res.url, '_blank')
                    }
                    this.hasOauth = res.status === 200
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message
                    })
                } finally {
                    this.oauthing = false
                }
            },
            async refreshOatuStatus () {
                if (this.refreshing) return
                try {
                    this.refreshing = true
                    // TODO: 刷新Oauth状态
                    const res = await this.isPACOAuth({
                        projectId: this.$route.params.projectId,
                        repositoryType: this.releaseParams.scmType
                    })
                    if (res?.status === 403) {
                        window.open(res.url, '_blank')
                    }
                    this.hasOauth = res.status === 200
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message
                    })
                } finally {
                    this.refreshing = false
                }
            },
            trimCIPrefix (filePath) {
                return filePath.startsWith(this.filePathDir)
                    ? filePath.replace(this.filePathDir, '')
                    : filePath
            }
        }
    }
</script>

<style lang="scss">
@import "@/scss/conf";
@import "@/scss/mixins/ellipsis";

.release-pipeline-side-slider-header {
    display: grid;
    align-items: center;
    grid-template-columns: max-content min-content 1fr;
    grid-gap: 12px;
    padding: 0 16px 0 0;
    height: 100%;
    line-height: 1;
    overflow: hidden;

    &.has-pac-tag {
        grid-template-columns: max-content max-content min-content 1fr;
    }

    .release-pipeline-new-version {
        background: #f5f6fa;
        border-radius: 10px;
        background: rgba(151, 155, 165, .1);
        border-color: rgba(220, 222, 229, .6);
        height: 22px;
        line-height: 22px;
        padding: 0 12px;
        max-width: 300px;
    }

    >span {
        color: #979ba5;
        font-size: 12px;
        flex: 1;
        flex-shrink: 0;
        @include ellipsis();
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
        border-bottom: 1px solid #dcdee5;
        padding-bottom: 8px;
        margin-bottom: 16px;

        .devops-icon.icon-angle-right {
            transition: all 0.3s;
            display: inline-flex;
            justify-content: center;
            width: 12px;
            height: 12px;
            line-height: 1;
            font-size: 12px;

            &.pac-codelib-form-show {
                display: inline-flex;
                transform: rotate(90deg);
            }
        }
    }

    .release-pipeline-pac-conf {
        display: flex;
        background: #fafbfd;
        height: 80px;
        align-items: center;
        font-size: 12px;

        > :first-child {
            border-right: 1px solid #dcdee5;
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
            color: #979ba5;
            letter-spacing: 0;
            line-height: 20px;
        }

        .pac-pipeline-dest-branch-radio {
            display: flex;
            margin-bottom: 8px;
            .bk-radio-text {
                @include ellipsis();
                flex: 1;
            }
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

    &+.yaml-info-codelib-label {
        margin-top: 20px;
    }

    &:after {
        content: "*";
        position: absolute;
        display: inline-block;
        right: 28px;
        top: 10px;
        color: #ea3636;
    }

    >.devops-icon {
        font-size: 14px;
        color: #979ba5;
    }
}

.release-pipeline-pac-footer {
    padding: 0 24px;
}

.release-info-dialog {
    .bk-dialog-tool {
        display: none !important;
    }
    .bk-dialog.bk-info-box {
        height: 100% !important;
        top: 0;
        .bk-dialog-content {
            top: 50%;
            transform: translateY(-55%);
        }
    }
}
.release-info-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    margin-top: 30px;

    .release-success-icon {
        border-radius: 50%;
        height: 42px;
        width: 42px;
        display: flex;
        align-items: center;
        justify-content: center;
        background-color: #e5f6ea;
        color: #3fc06d;
        font-size: 36px;
    }

    .part-of-mr {
        position: relative;
        width: 42px;
        height: 42px;
        background-color: #E1ECFF;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        &:before {
            position: absolute;
            content: '';
            width: 0;
            height: 0;
            border: 14px solid #3A84FF;
            border-top-color: transparent;
            position: absolute;
            transform: rotate(-45deg);
            border-radius: 50%;
        }
        &:after {
            content: '';
            position: absolute;
            border: 2px solid #3A84FF;
            width: 28px;
            height: 28px;
            border-radius: 50%;

        }
    }

    > h3 {
        width: 100%;
        margin: 0 0 16px 0;
        font-size: 14px;
        font-weight: normal;
        text-align: left;
    }

    .release-info-title {
        font-size: 20px;
        color: #313238;
        margin: 22px 0 12px 0;
        line-height: 1.2;
        text-align: center;
    }

    .pipeline-release-suc-tips {
        background: #f5f6fa;
        display: flex;
        font-size: 14px;
        flex-direction: column;
        border-radius: 2px;
        padding: 14px 20px;
        letter-spacing: 0;
        line-height: 22px;
        text-align: left;
        > h3 {
            font-weight: 700;
            font-size: 14px;
            margin: 0 0 10px 0;
        }
    }

    .release-info-text {
        width: 100%;
        padding: 12px 16px;
        background-color: #F5F6FA;
        border-radius: 2px;
    }

    .warning-box {
        width: 100%;
        display: flex;
        align-items: center;
        background: #FFF4E2;
        padding: 6px 10px;
        font-size: 14px;
        border-radius: 2px;

        span {
            margin-left: 10px;
        }
    }

}

.no-pac-enable-codelib-yet {
    color: #c4c6cc;
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

    >header {
        display: flex;
        align-items: center;
        grid-gap: 24px;
    }

    .pac-oauth-tips {
        color: #979ba5;
        line-height: 22px;
        margin-top: 16px;
        font-size: 12px;
    }
}

.belongs-to-groups-box {
    margin-top: 10px;
    display: flex;
    flex-direction: column;
    grid-gap: 12px;
    font-size: 12px;
}
</style>
