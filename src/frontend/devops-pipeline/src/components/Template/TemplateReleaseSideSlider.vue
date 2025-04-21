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
            <p>
                {{ $t("template.releasePipelineInstance") }}
                <PacTag
                    v-if="pacEnabled"
                    :info="pipelineInfo?.yamlInfo"
                />
            </p>

            <bk-popover
                v-if="isUnpublishedStatus"
                theme="light"
                :tippy-options="{
                    arrow: false,
                    placement: 'bottom-end'
                }"
                ext-cls="instance-version"
            >
                <span class="release-pipeline-num">共3个实例</span>
                <div slot="content">
                    <div
                        v-for="item in instancesList"
                        :key="item.name"
                        class="instance-list"
                    >
                        <p>
                            <span class="instance-name">{{ item.name }}</span>
                        </p>
                        <span
                            v-bk-overflow-tips
                            class="release-pipeline-new-version"
                        >
                            {{ $t("releasePipelineVersion",[newReleaseVersionName]) }}
                        </span>
                    </div>
                </div>
            </bk-popover>
        </header>
        <section
            slot="content"
            v-bkloading="{ isLoading: isLoading || releasing }"
            class="release-pipeline-pac-form"
        >
            <div v-if="isUnpublishedStatus">
                <div
                    v-if="!pacEnabled"
                    class="release-pipeline-pac-conf"
                >
                    <aside class="release-pipeline-pac-conf-leftside">
                        <label for="enablePac">
                            {{ $t("pacMode") }}
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
                    ref="releaseFormRef"
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
                            <bk-form-item
                                property="filePath"
                            >
                                <table class="instance-filePath">
                                    <thead>
                                        <tr align="left">
                                            <th>实例</th>
                                            <th>
                                                <span class="yaml-path-name">YAML 文件路径</span>
                                            </th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <tr
                                            v-for="item in instancesList"
                                            :key="item.name"
                                        >
                                            <td class="instance-name">{{ item.name }}</td>
                                            <td>
                                                <div class="input-cell">
                                                    <span class="instance-name">{{ filePathDir }}</span>
                                                    <bk-input
                                                        v-model="item.filePath"
                                                        id="yamlFilePath"
                                                        placeholder="请输入"
                                                    />
                                                </div>
                                            </td>
                                        </tr>
                                    </tbody>
                                </table>
                            </bk-form-item>
                        </section>
                    </div>
                    <div class="release-pipeline-pac-submit-conf">
                        <header class="release-pac-pipeline-form-header">
                            {{ $t("submitSetting") }}
                        </header>
    
                        <bk-form-item
                            required
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
                    </div>
                </bk-form>
            </div>
            <div
                slot="content"
                v-else
                class="release-status-main"
            >
                <template v-if="isReleasing">
                    <Logo
                        size="56"
                        name="spinner"
                        class="release-status-icon"
                    />
                    <p class="release-status-title"> 10 个实例正在发布中…</p>
                    <p class="sub-message">你可以在当前页等待或关闭弹窗继续其他操作后续可在按钮处查看发布结果</p>
                    <!-- <div class="release-status-content"></div> -->
                </template>
                <template v-else-if="isPublishSuccess">
                    <Logo
                        size="64"
                        name="success"
                        class="release-status-icon"
                    />
                    <p class="release-status-title"> 100 个流水线实例发布成功</p>
                    <p class="sub-message">接下来你可以在实例列表中查看已发布的实例</p>
                    <bk-button
                        @click="handleClick"
                        class="release-status-btn"
                    >
                        返回列表
                    </bk-button>
                </template>
                <template v-else-if="isPublishFailure">
                    <Logo
                        size="64"
                        name="failure"
                        class="release-status-icon"
                    />
                    <p class="release-status-title"> 100 个流水线实例发布失败</p>
                    <p class="sub-message">接下来你可以重试或关闭弹窗</p>
                    <div class="release-status-btn">
                        <bk-button
                            theme="primary"
                            @click="handleClick"
                        >
                            重试
                        </bk-button>
                        <bk-button
                            @click="handleClick"
                        >
                            修改配置
                        </bk-button>
                        <bk-button
                            @click="handleClick"
                        >
                            关闭
                        </bk-button>
                    </div>
                </template>
                <template v-else-if="isProcessingRequired">
                    <Logo
                        size="64"
                        name="required"
                        class="release-status-icon"
                    />
                    <p class="release-status-title">合并请求创建完成，请到代码库处理...</p>
                    <p class="sub-message pending">版本尚未发布成功，下一步 <span>请到代码库处理合并请求</span> </p>
                    <p class="pac-mode-message">PAC模式下，YAML 文件合入默认分支，才视为发布正式版本</p>
                    <div class="release-status-btn">
                        <bk-button
                            theme="primary"
                            @click="handleClick"
                        >
                            处理合并请求
                        </bk-button>
                        <bk-button
                            @click="handleClick"
                        >
                            返回列表
                        </bk-button>
                    </div>
                </template>
            </div>
        </section>
        <footer
            v-if="isUnpublishedStatus"
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
            <bk-button
                :disabled="releasing"
                @click="cancelRelease"
            >
                {{ $t("cancel") }}
            </bk-button>
        </footer>
    </bk-sideslider>
</template>

<script setup>
    import Logo from '@/components/Logo'
    import PacTag from '@/components/PacTag.vue'
    import { TARGET_ACTION_ENUM, VERSION_STATUS_ENUM } from '@/utils/pipelineConst'
    import { ref, computed, watch, defineProps, defineEmits, nextTick } from 'vue'
    import UseInstance from '@/hook/useInstance'
    
    const { proxy, t, bkMessage, bkInfo, h } = UseInstance()

    const props = defineProps({
        value: {
            type: Boolean,
            default: false
        },
        version: {
            type: [String, Number],
            required: true
        }
    })
    const emits = defineEmits(['input'])

    const isLoading = ref(false)
    const releasing = ref(false)
    const showPacCodelibSetting = ref(false)
    const pacEnableCodelibList = ref([])
    const hasOauth = ref(true)
    const newReleaseVersionName = ref('')
    const scrollLoadmoreConf = ref({
        isLoading: false,
        page: 1,
        pageSize: 10,
        total: 0,
        size: 'mini'
    })
    const releaseParams = ref({
        enablePac: false,
        targetBranch: '',
        scmType: '',
        description: '',
        repoHashId: '',
        filePath: '',
        targetAction: ''
    })
    const isInitPacRepo = ref(false)
    const releaseFormRef = ref(null)
    const instancesList = ref([
        { name: '流水线实例 1', filePath: '' },
        { name: '流水线实例 2', filePath: '' },
        { name: '流水线实例 3', filePath: '' }
    ])
    const isReleasing = ref(false)
    const isPublishSuccess = ref(false)
    const isPublishFailure = ref(false)
    const isProcessingRequired = ref(false)

    const isUnpublishedStatus = computed(() =>
        !isReleasing.value
        && !isPublishSuccess.value
        && !isPublishFailure.value
        && !isProcessingRequired.value
    )

    const pipelineInfo = computed(() => proxy.$store?.state?.atom?.pipelineInfo)
    // const pipeline = computed(() => proxy.$store?.state?.atom?.pipeline)
    // const pipelineSetting = computed(() => proxy.$store?.state?.atom?.pipelineSetting)
    // const isManage = computed(() => proxy.$store?.state?.pipelines?.isManage)
    const pacSupportScmTypeList = computed(() => proxy.$store?.state?.common?.pacSupportScmTypeList)
    
    const pacEnabled = computed(() => proxy.$store?.getters['atom/pacEnabled'])
    const yamlInfo = computed(() => proxy.$store?.getters['atom/yamlInfo'])
    const isTemplate = computed(() => proxy.$store?.getters['atom/isTemplate'])

    const filePathDir = computed(() => `.ci/${isTemplate.value ? 'templates/' : ''}`)
    const baseVersionBranch = computed(() => pipelineInfo.value?.baseVersionName || '--')
    const isTemplatePipeline = computed(() => pipelineInfo.value?.instanceFromTemplate ?? false)
    // const isCommitToBranch = computed(() => releaseParams.value.targetAction === TARGET_ACTION_ENUM.COMMIT_TO_BRANCH)
    const rules = computed(() => ({
        repoHashId: [
            {
                required: true,
                message: t('stageReview.requireRule', [t('yamlCodeLib')]),
                trigger: 'blur'
            }
        ],
        // filePath: [
        //     {
        //         required: true,
        //         regex: /\.ya?ml$/,
        //         message: t('yamlFilePathErrorTip'),
        //         trigger: 'blur'
        //     }
        // ],
        description: [
            {
                required: true,
                message: t('stageReview.requireRule', [t('versionDesc')]),
                trigger: 'blur'
            }
        ],
        targetAction: [
            {
                required: true,
                message: t('stageReview.requireRule', [t('targetBranch')]),
                trigger: 'blur'
            }
        ]
    }))
    const isDraftBaseBranchVersion = computed(() => pipelineInfo.value?.baseVersionStatus === VERSION_STATUS_ENUM.BRANCH)
    const targetActionOptions = computed(() => [
        TARGET_ACTION_ENUM.COMMIT_TO_MASTER,
        TARGET_ACTION_ENUM.CHECKOUT_BRANCH_AND_REQUEST_MERGE
    ])
    const hasPacSupportScmTypeList = computed(() => pacSupportScmTypeList.value?.length > 0)
    const prefetchParams = computed(() => {
        const {
            targetBranch,
            targetAction,
            repoHashId,
            enablePac
        } = releaseParams.value
        return {
            targetBranch,
            targetAction,
            repoHashId,
            enablePac
        }
    })

    watch(props.value, val => {
        if (val) {
            init()
        }
    })
    watch(yamlInfo.value, val => {
        if (val) {
            Object.assign(releaseParams.value, {
                ...val,
                filePath: trimCIPrefix(val.filePath)
            })
        }
    }, { immediate: true })
    watch(pacEnabled.value, val => {
        releaseParams.value.enablePac = val
    }, { immediate: true })
    watch(() => releaseParams.value.enablePac, (val) => {
        if (val) {
            init()
        }
    }, { immediate: true })
    watch(prefetchParams, (val) => {
        prefetchReleaseVersion(val)
    }, { deep: true })

    function errorHandler (error) {
        const resourceType = isTemplate.value ? 'template' : 'pipeline'
        proxy.handleError(error, {
            projectId: proxy.$route.params.projectId,
            resourceCode: proxy.$route.params[`${resourceType}Id`],
            resourceType: resourceType,
            action: proxy.$permissionResourceAction.EDIT
        })
    }
    async function init () {
        try {
            isLoading.value = true
            const enablePac = releaseParams.value.enablePac

            await Promise.all([
                ...(enablePac
                    ? [
                        proxy.$store.dispatch('common/getSupportPacScmTypeList')
                    ]
                    : []
                ),
                prefetchReleaseVersion(prefetchParams.value)
            ])

            if (enablePac && hasPacSupportScmTypeList.value) {
                releaseParams.value.scmType = pacSupportScmTypeList.value[0]?.id
                nextTick(() => {
                    fetchPacEnableCodelibList(true)
                    if (isDraftBaseBranchVersion.value) {
                        releaseParams.value.targetAction = TARGET_ACTION_ENUM.COMMIT_TO_SOURCE_BRANCH_AND_REQUEST_MERGE
                    }
                })
            }
        } catch (error) {
            errorHandler(error)
        } finally {
            isLoading.value = false
        }
    }
    async function prefetchReleaseVersion (params) {
        try {
            const lackTargetAction = params.enablePac && !params.targetAction
            const withoutBranch = params.targetAction === TARGET_ACTION_ENUM.COMMIT_TO_BRANCH && !params.targetBranch
            if (!props.value || !props.version || lackTargetAction || withoutBranch) {
                return
            }
            const datas = {
                ...proxy.$route.params,
                version: props.version,
                ...params
            }
            let newReleaseVersion
            if (isTemplate.value) {
                newReleaseVersion = await proxy.$store.dispatch('atom/prefetchTemplateVersion', datas)
            } else {
                newReleaseVersion = await proxy.$store.dispatch('atom/prefetchPipelineVersion', datas)
            }
            newReleaseVersionName.value = newReleaseVersion?.newVersionName || '--'
        } catch (error) {
            errorHandler(error)
        }
    }
    async function fetchPacEnableCodelibList (init = false) {
        try {
            if (
                isInitPacRepo.value
                || scrollLoadmoreConf.value.isLoading
                || (scrollLoadmoreConf.value.total > 0 && scrollLoadmoreConf.value.total <= pacEnableCodelibList.value.length)
            ) {
                return
            }
            if (init) {
                scrollLoadmoreConf.value.page = 1
                pacEnableCodelibList.value = []
                isInitPacRepo.value = true
            } else {
                scrollLoadmoreConf.value.isLoading = true
                scrollLoadmoreConf.value.page += 1
            }
            const { projectId } = proxy.$route.params
            const { scmType } = releaseParams.value
            const response = await proxy.$store.dispatch('common/getPACRepoList', {
                projectId,
                repositoryType: scmType,
                enablePac: true,
                permission: 'USE',
                page: scrollLoadmoreConf.value.page,
                pageSize: scrollLoadmoreConf.value.pageSize
            })
            Object.assign(scrollLoadmoreConf.value, {
                total: response.count,
                page: response.page,
                pageSize: response.pageSize
            })
            pacEnableCodelibList.value = [...pacEnableCodelibList.value, ...response.records]
        } catch (error) {
            bkMessage({
                theme: 'error',
                message: error.message || error
            })
        } finally {
            scrollLoadmoreConf.value.isLoading = false
            isInitPacRepo.value = false
        }
    }
    function refreshPacEnableCodelibList (show) {
        if (show) {
            fetchPacEnableCodelibList(true)
        }
    }
    function handlePacEnableChange (val) {
        showPacCodelibSetting.value = val
    }
    async function releasePipeline () {
        try {
            if (releasing.value) return
            releasing.value = true
            proxy.$store.dispatch('atom/setSaveStatus', true)
            await releaseFormRef.value?.validate?.()
            const {
                fileUrl,
                webUrl,
                pathWithNamespace,
                repoHashId,
                scmType,
                filePath,
                targetAction,
                ...rest
            } = releaseParams.value

            const postDatas = {
                ...proxy.$route.params,
                version: props.version,
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
                            filePath: `${filePathDir.value}${filePath}`
                        }
                        : null
                }
            }
            
            let res
            if (isTemplate.value) {
                res = await proxy.$store.dispatch('atom/releaseDraftTemplate', postDatas)
            } else {
                res = await proxy.$store.dispatch('atom/releaseDraftPipeline', postDatas)
            }

            const { versionName, targetUrl, updateBuildNo } = res
            if (isTemplate.value) {
                await proxy.$store.dispatch('atom/requestTemplateSummary', proxy.$route.params)
            } else {
                await proxy.$store.dispatch('atom/requestPipelineSummary', proxy.$route.params)
            }

            const tipsI18nKey = releaseParams.value.enablePac
                ? 'pacPipelineReleaseTips'
                : 'releaseTips'
            const tipsArrayLength = releaseParams.value.enablePac ? 2 : 0
            const isPacMR
                = releaseParams.value.enablePac
                    && [
                        TARGET_ACTION_ENUM.CHECKOUT_BRANCH_AND_REQUEST_MERGE,
                        TARGET_ACTION_ENUM.COMMIT_TO_SOURCE_BRANCH_AND_REQUEST_MERGE
                    ].includes(releaseParams.value.targetAction)
           
            const instance = bkInfo({
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
                    }, t(isPacMR ? 'pacMRRelaseTips' : 'releaseSuc')),
                    h('h3', {
                        class: 'release-info-text',
                        domProps: {
                            innerHTML: t(isPacMR ? 'pacMRRelaseSuc' : 'relaseSucTips', [
                                versionName
                            ])
                        }
                    }),
                    updateBuildNo && !tipsArrayLength
                        ? h('div', { class: 'warning-box' }, [
                            h(Logo, { size: 14, name: 'warning-circle-fill' }),
                            h('span', t('buildNoBaseline.resetRequiredTips'))
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
                                    h('h3', {}, t('pacPipelineConfRule')),
                                    ...Array.from({ length: tipsArrayLength }).map((_, index) => {
                                        if (index === 1 && releaseParams.value.enablePac) {
                                            return h('ul', {}, [
                                                h('span', {}, t(`${tipsI18nKey}${index}`)),
                                                Array(3)
                                                    .fill(0)
                                                    .map((_, i) =>
                                                        h(
                                                            'li',
                                                            {
                                                                domProps: {
                                                                    innerHTML: t(
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
                                        return h('span', {}, t(`${tipsI18nKey}${index}`))
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
                            releaseParams.value.enablePac && isPacMR
                                ? h(
                                    'bk-button',
                                    {
                                        props: {
                                            theme: 'primary'
                                        },
                                        on: {
                                            click: () => {
                                                bkInfo.close(instance.id)
                                                window.open(targetUrl, '_blank')
                                            }
                                        }
                                    },
                                    t('dealMR')
                                )
                                : !isTemplate.value
                                    ? h(
                                        'bk-button',
                                        {
                                            props: {
                                                theme: 'primary'
                                            },
                                            on: {
                                                click: () => {
                                                    bkInfo.close(instance.id)
                                                    if (!updateBuildNo) {
                                                        proxy.$router.push({
                                                            name: 'executePreview',
                                                            params: {
                                                                ...proxy.$route.params,
                                                                version: pipelineInfo.value?.releaseVersion
                                                            }
                                                        })
                                                    } else {
                                                        proxy.$router.push({
                                                            name: 'pipelinesHistory',
                                                            params: {
                                                                ...proxy.$route.params,
                                                                type: 'pipeline',
                                                                isDirectShowVersion: true,
                                                                version: pipelineInfo.value?.releaseVersion
                                                            }
                                                        })
                                                    }
                                                }
                                            }
                                        },
                                        t(!updateBuildNo ? 'goExec' : 'buildNoBaseline.goReset')
                                    )
                                    : null,
                            h(
                                'bk-button',
                                {
                                    on: {
                                        click: () => {
                                            bkInfo.close(instance.id)
                                            !updateBuildNo && proxy.$router.push({
                                                name: isTemplate.value ? 'TemplateOverview' : 'pipelinesHistory',
                                                params: {
                                                    ...proxy.$route.params,
                                                    type: 'pipeline',
                                                    version: pipelineInfo.value?.releaseVersion
                                                }
                                            })
                                        }
                                    }
                                },
                                t(!updateBuildNo ? 'checkPipeline' : 'return')
                            )

                        ]
                    )
                ])
            })
            hideReleaseSlider()
        } catch (e) {
            if (e.state === 'error') {
                e.message = e.content
            }
            errorHandler(e)
            return {
                code: e.code,
                message: e.message
            }
        } finally {
            proxy.$store.dispatch('atom/setSaveStatus', false)
            releasing.value = false
        }
    }
    function showReleaseSlider () {
        emits('input', true)
    }
    function hideReleaseSlider () {
        cancelRelease()
        releaseParams.value = {
            enablePac: pacEnabled.value,
            description: '',
            ...(yamlInfo.value
                ? {
                    ...yamlInfo.value,
                    filePath: trimCIPrefix(yamlInfo.value.filePath)
                }
                : {}),
            targetAction: ''
        }
    }
    function cancelRelease () {
        emits('input', false)
    }
    function togglePacCodelibSettingForm () {
        showPacCodelibSetting.value = !showPacCodelibSetting.value
    }
    function goCodelib () {
        window.open(`/console/codelib/${proxy.$route.params.projectId}`, '_blank')
    }
    function trimCIPrefix (filePath) {
        return filePath.startsWith(filePathDir.value)
            ? filePath.replace(filePathDir.value, '')
            : filePath
    }

</script>

<style lang="scss">
@import "@/scss/conf";
@import "@/scss/mixins/ellipsis";

.release-pipeline-side-slider-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 16px 0 0;
    height: 100%;
    line-height: 1;
    overflow: hidden;

    &.has-pac-tag {
        grid-template-columns: max-content max-content min-content 1fr;
    }

    .release-pipeline-num {
        color: #3A84FF;
        cursor: pointer;
        font-size: 12px;
    }
}

.instance-version {

    .instance-list {
        display: flex;
        justify-content: space-between;
        align-items: center;
        width: 318px;
        height: 32px;

        .instance-name {
            flex: 1;
            font-size: 12px;
            color: #4D4F56;
        }
    }

    .release-pipeline-new-version {
        background: #f5f6fa;
        border-radius: 10px;
        background: rgba(151, 155, 165, .1);
        border-color: rgba(220, 222, 229, .6);
        height: 22px;
        line-height: 22px;
        padding: 0 12px;
        min-width: 150px;
        max-width: 300px;
    }

    span {
        color: #979ba5;
        font-size: 12px;
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
    
    .instance-filePath {
        width: 590px;
        margin-top: 12px;
        border-collapse: collapse;

        td,th {
            padding-left: 10px;
            border: 1px solid #e2e4e9;
        }

        tr {
            height: 42px;
            font-size: 12px;
        }

        thead {
            background-color: #fafbfd;
            font-weight: 700;
        }

        .yaml-path-name {
            border-bottom: 1px dashed #b6b9c1;
            position: relative;

            &::after {
                content: '*';
                position: absolute;
                color: $dangerColor;
                font-size: 12px;
                right: -12px;
            }
        }

        tbody {
            tr {
                color: #4D4F56;

                td:first-child {
                    background-color: #f5f7fa;
                }

                .input-cell {
                    display: flex;
                    padding: 10px 10px;

                    .instance-name {
                        background-color: #eff1f5;
                        padding: 0 5px;
                        border-radius: 2px;
                    }
                    .bk-form-input {
                        border: none;
                    }
                }
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
            margin-bottom: 8px;
            margin-right: 20px;
            .bk-radio-text {
                @include ellipsis();
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

.release-status-main {
    width: 100%;
    height: 100%;
    text-align: center;

    .release-status-icon {
        margin-top: 122px;
        margin-bottom: 40px;
    }

    .release-status-title {
        font-size: 24px;
        color: #313238;
        line-height: 32px;
        margin-bottom: 16px;
    }

    .sub-message {
        margin: auto;
        width: 280px;
        font-size: 14px;
        color: #4D4F56;
        text-align: center;
    }

    .pending {
        width: 552px;
        margin-bottom: 16px;
        text-align: left;
        span {
            cursor: pointer;
            font-weight: 700;
        }
    }

    .pac-mode-message {
        margin: auto;
        text-align: left;
        padding-left: 16px;
        width: 552px;
        height: 46px;
        line-height: 46px;
        background: #F5F7FA;
        border-radius: 2px;
        font-size: 14px;
        color: #4D4F56;
    }

    .release-status-content {
        margin: auto;
        margin-top: 22px;
        width: 294px;
        height: 294px;
        border: 1px solid #DCDEE5;
        border-radius: 50%;
        box-shadow: inset 0 1px 13px 0 #0000001a;
    }

    .release-status-btn {
        margin-top: 28px;
    }

}
</style>
