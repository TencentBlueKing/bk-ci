<template>
    <div>
        <bk-dialog
            :class="{
                'codelib-operate-dialog': true,
                'codelib-orerate-oauth-dialog': !showDialogFooter
            }"
            v-model="isShow"
            :width="780"
            :padding="24"
            :quick-close="false"
            :show-footer="showDialogFooter"
            render-directive="if"
            @value-change="handleDialogChange"
        >
            <h3
                slot="header"
                class="bk-dialog-title"
            >
                {{ title }}
            </h3>
            <div v-bkloading="{ isLoading: isFetchingData }">
                <component
                    v-if="isShow"
                    ref="form"
                    :is="comName"
                    :oauth-user-list="oauthUserList"
                    :is-edit-mode="true"
                    :enable-pac="enablePac"
                />
            </div>
            <footer slot="footer">
                <template v-if="showDialogFooter">
                    <bk-button
                        class="mr5"
                        theme="primary"
                        :loading="isLoading"
                        :disabled="isFetchingData"
                        @click="submitCodelib"
                    >
                        {{ $t('codelib.confirm') }}
                    </bk-button>
                    <bk-button
                        :loading="isLoading"
                        :disabled="isFetchingData"
                        @click="handleCancel"
                    >
                        {{ $t('codelib.cancel') }}
                    </bk-button>
                </template>
            </footer>
        </bk-dialog>
        <UsingPipelinesDialog
            :is-show.sync="pipelinesDialogPayload.isShow"
            :pipelines-list="pipelinesList"
            :fetch-pipelines-list="fetchPipelinesList"
            :is-loading-more="pipelinesDialogPayload.isLoadingMore"
            :has-load-end="pipelinesDialogPayload.hasLoadEnd"
            :task-repo-type="pipelinesDialogPayload.taskRepoType"
            @confirm="handleConfirmAliasChange"
        />
    </div>
</template>

<script>
    import { cloneDeep } from 'lodash-es'
    import P4 from '../CodeLibDialog/P4'
    import SVN from '../CodeLibDialog/SVN'
    import Git from '../CodeLibDialog/Git'
    import TGit from '../CodeLibDialog/TGit'
    import Github from '../CodeLibDialog/Github'
    import Gitlab from '../CodeLibDialog/Gitlab'
    import Custom from '../CodeLibDialog/Custom'
    import UsingPipelinesDialog from '../UsingPipelinesDialog.vue'
    import {
        isP4,
        isSvn,
        isGit,
        isTGit,
        isGithub,
        isGitLab,
        isScmGit,
        isScmSvn,
        getCodelibConfig,
        convertToCamelCase
    } from '../../config/'
    import { mapActions, mapState } from 'vuex'
    import { parsePathAlias, parsePathRegion } from '../../utils'

    export default {
        name: 'update-repo-dialog',
        components: {
            Github,
            Gitlab,
            SVN,
            TGit,
            Git,
            P4,
            Custom,
            UsingPipelinesDialog
        },
        props: {
            repoInfo: {
                type: Object,
                default: () => ({})
            },
            curRepo: {
                type: Object,
                default: () => ({})
            },
            fetchRepoDetail: {
                type: Function,
                required: true
            },
            refreshCodelibList: {
                type: Function,
                required: true
            }
        },
        data () {
            return {
                isShow: false,
                isLoading: false,
                isFetchingData: false,
                isOpening: false,
                oauthUserList: [],
                pipelinesList: [],
                pipelinesDialogPayload: {
                    isShow: false,
                    isLoadingMore: false,
                    hasLoadEnd: false,
                    page: 1,
                    pageSize: 20,
                    repositoryHashId: '',
                    taskRepoType: 'NAME'
                },
                pendingSaveParams: null
            }
        },
        computed: {
            ...mapState('codelib', [
                'codelib',
                'gitOAuth',
                'tgitOAuth',
                'githubOAuth',
                'scmgitOAuth',
                'scmsvnOAuth',
                'codelibTypes',
                'providerConfig'
            ]),

            enablePac () {
                return !!(this.repoInfo?.enablePac || this.curRepo?.enablePac)
            },

            oAuth () {
                const oauthMap = {
                    isTGit: this.tgitOAuth,
                    isGit: this.gitOAuth,
                    isGithub: this.githubOAuth,
                    isScmGit: this.scmgitOAuth,
                    isScmSvn: this.scmsvnOAuth
                }
                let hasPower = false
                let project = []
                for (const [condition, oauth] of Object.entries(oauthMap)) {
                    if (this[condition]) {
                        hasPower = oauth.status !== 403
                        project = oauth.project
                        break
                    }
                }
                return {
                    hasPower,
                    project
                }
            },

            showDialogFooter () {
                if (this.isFetchingData) {
                    return true
                }
                return (this.oAuth.hasPower && this.isOAUTH) || !this.isOAUTH
            },

            title () {
                const label = this.codelibConfig.label
                    || this.providerConfig?.name
                    || this.codelib?.scmType
                    || ''
                return this.$t('codelib.editRepo', [label])
            },

            isScmGit () {
                return isScmGit(this.codelibTypeName)
            },
            isScmSvn () {
                return isScmSvn(this.codelibTypeName)
            },
            isGit () {
                return isGit(this.codelibTypeName)
            },
            isTGit () {
                return isTGit(this.codelibTypeName)
            },
            isGitLab () {
                return isGitLab(this.codelibTypeName)
            },
            isSvn () {
                return isSvn(this.codelibTypeName)
            },
            isP4 () {
                return isP4(this.codelibTypeName)
            },
            isGithub () {
                return isGithub(this.codelibTypeName)
            },

            codelibTypeName () {
                return this.codelib && this.codelib['@type']
                    ? this.codelib['@type']
                    : ''
            },

            codelibConfig () {
                return (
                    getCodelibConfig(
                        this.codelibTypeName,
                        this.codelib.svnType,
                        this.codelib.authType
                    ) || {}
                )
            },

            codelibTypeConstants () {
                const codelibTypes = this.isScmGit || this.isScmSvn
                    ? ['scmgit', 'scmsvn']
                    : ['github', 'git', 'tgit']
                const regex = new RegExp(`^\\S*?(${codelibTypes.join('|')})`, 'i')
                return this.codelibTypeName.toLowerCase().replace(regex, '$1')
            },

            isOAUTH () {
                if (this.isScmGit || this.isScmSvn) {
                    return this.codelib.credentialType === 'OAUTH'
                }
                return this.codelib.authType === 'OAUTH'
            },

            comName () {
                const comMap = {
                    Git: 'Git',
                    TGit: 'TGit',
                    GitHub: 'Github',
                    SVN: 'SVN',
                    P4: 'P4',
                    GitLab: 'Gitlab'
                }
                return comMap[this.codelibConfig.label] || 'Custom'
            },

            projectId () {
                return this.$route.params.projectId
            },

            repositoryHashId () {
                return this.repoInfo?.repoHashId || this.curRepo?.repositoryHashId || ''
            },

            originalAliasName () {
                return this.repoInfo?.aliasName || this.curRepo?.aliasName || ''
            }
        },
        watch: {
            'codelib.userName' (val, oldVal) {
                if (!this.isShow || this.isOpening || this.isFetchingData || !this.isOAUTH || !val || val === oldVal) return
                this.refreshOAuthByUser(val)
            }
        },
        methods: {
            ...mapActions('codelib', [
                'checkOAuth',
                'checkScmOAuth',
                'editRepo',
                'requestTickets',
                'setCodelib',
                'setTemplateCodelib',
                'setProviderConfig',
                'getOauthUserList',
                'fetchUsingPipelinesList'
            ]),

            resetSharedDialogState () {
                this.setTemplateCodelib()
                this.setCodelib({
                    aliasName: '',
                    credentialId: '',
                    projectName: '',
                    url: '',
                    authType: '',
                    svnType: '',
                    userName: '',
                    repositoryHashId: ''
                })
            },

            open () {
                if (this.isOpening) return
                this.isOpening = true

                const repoData = cloneDeep({
                    ...this.repoInfo,
                    repositoryHashId: this.repositoryHashId
                })
                const typeName = repoData['@type']
                    || (repoData.scmType ? convertToCamelCase(repoData.scmType) : '')
                    || (this.curRepo?.type ? convertToCamelCase(this.curRepo.type) : '')
                repoData['@type'] = typeName
                repoData.userName = repoData.userName || this.curRepo?.userName || ''
                // Github 详情模型无 authType 字段，仅支持 OAUTH，编辑时需补齐以回填单选
                if (isGithub(typeName) && !repoData.authType) {
                    repoData.authType = 'OAUTH'
                }

                const providerConfig = this.codelibTypes.find(i => i.scmCode === repoData.scmCode)
                if (providerConfig) {
                    this.setProviderConfig(providerConfig)
                }

                const isScm = isScmGit(typeName) || isScmSvn(typeName) || typeName?.startsWith('scm')
                let credentialTypes = repoData.credentialType
                if (!isScm) {
                    const codelibConfig = getCodelibConfig(
                        typeName,
                        repoData.svnType,
                        repoData.authType
                    ) || {}
                    credentialTypes = codelibConfig.credentialTypes
                }

                this.setCodelib({
                    ...repoData,
                    credentialTypes
                })
                this.setTemplateCodelib({
                    url: repoData.url,
                    aliasName: repoData.aliasName
                })

                this.isShow = true
                this.isFetchingData = true
                this.pendingSaveParams = null
                this.resetPipelinesDialog()

                this.$nextTick(() => {
                    this.$refs.form?.initEditCache?.(this.codelib)
                    this.fetchDialogData(repoData, typeName, isScm, credentialTypes)
                })
            },

            async fetchDialogData (repoData, typeName, isScm, credentialTypes) {
                try {
                    const codelibTypeConstants = typeName?.toLowerCase().replace(
                        /^\S*?(scmgit|scmsvn|github|git|tgit)/i,
                        '$1'
                    )
                    const requests = [
                        this.requestTickets({
                            projectId: this.projectId,
                            credentialTypes: isScm ? repoData.credentialType : credentialTypes
                        }),
                        this.getOauthUserList({ scmCode: repoData.scmCode }).then((res) => {
                            this.oauthUserList = res || []
                        }).catch(() => {
                            this.oauthUserList = []
                        })
                    ]
                    if (typeName?.startsWith('scm') && repoData.credentialType === 'OAUTH') {
                        requests.unshift(this.checkScmOAuth({
                            projectId: this.projectId,
                            scmCode: repoData.scmCode,
                            type: codelibTypeConstants,
                            username: repoData.userName
                        }))
                    } else if (repoData.authType === 'OAUTH') {
                        requests.unshift(this.checkOAuth({
                            projectId: this.projectId,
                            repositoryHashId: this.repositoryHashId,
                            type: codelibTypeConstants,
                            username: repoData.userName
                        }))
                    }
                    await Promise.all(requests)
                    if (!this.isOAUTH) {
                        this.$refs.form?.getTickets?.()
                    }
                } finally {
                    this.isFetchingData = false
                    this.isOpening = false
                }
            },

            refreshOAuthByUser (username) {
                if (this.isScmGit || this.isScmSvn || this.codelibTypeName?.startsWith('scm')) {
                    this.checkScmOAuth({
                        projectId: this.projectId,
                        scmCode: this.codelib.scmCode,
                        type: this.codelibTypeConstants,
                        username
                    })
                    return
                }
                this.checkOAuth({
                    projectId: this.projectId,
                    repositoryHashId: this.repositoryHashId,
                    type: this.codelibTypeConstants,
                    username
                })
            },

            applyPacConstraints (params) {
                if (!this.enablePac) return params
                const cache = this.$refs.form?.editCacheCodelib || this.repoInfo || {}
                return {
                    ...params,
                    aliasName: cache.aliasName,
                    authType: cache.authType,
                    svnType: cache.svnType,
                    credentialType: cache.credentialType,
                    enablePac: true
                }
            },

            /**
             * 确保提交体含后端非空字段 projectName
             */
            ensureProjectName (params) {
                if (params.projectName) return params
                const cache = this.$refs.form?.editCacheCodelib || this.repoInfo || {}
                const { alias } = parsePathAlias(
                    params['@type'] || this.codelibTypeName,
                    params.url,
                    params.authType,
                    params.svnType
                )
                return {
                    ...params,
                    projectName: alias || cache.projectName || params.aliasName || ''
                }
            },

            resetPipelinesDialog () {
                this.pipelinesList = []
                this.pipelinesDialogPayload = {
                    isShow: false,
                    isLoadingMore: false,
                    hasLoadEnd: false,
                    page: 1,
                    pageSize: 20,
                    repositoryHashId: this.repositoryHashId,
                    taskRepoType: 'NAME'
                }
            },

            async fetchPipelinesList () {
                if (this.pipelinesDialogPayload.isLoadingMore) return
                this.pipelinesDialogPayload.isLoadingMore = true
                try {
                    const res = await this.fetchUsingPipelinesList({
                        projectId: this.projectId,
                        repositoryHashId: this.pipelinesDialogPayload.repositoryHashId,
                        taskRepoType: this.pipelinesDialogPayload.taskRepoType,
                        page: this.pipelinesDialogPayload.page,
                        pageSize: this.pipelinesDialogPayload.pageSize
                    })
                    this.pipelinesList = [...this.pipelinesList, ...res.records]
                    if (this.pipelinesDialogPayload.page === 1 && this.pipelinesList.length) {
                        this.pipelinesDialogPayload.isShow = true
                    }
                    this.pipelinesDialogPayload.hasLoadEnd = res.count === this.pipelinesList.length
                    this.pipelinesDialogPayload.page += 1
                } finally {
                    this.pipelinesDialogPayload.isLoadingMore = false
                }
            },

            async checkAliasChangeBeforeSave (params) {
                // PAC 模式下别名不可改；未改别名时无需提示
                if (this.enablePac || params.aliasName === this.originalAliasName) {
                    return true
                }
                this.resetPipelinesDialog()
                this.pipelinesDialogPayload.repositoryHashId = this.repositoryHashId
                await this.fetchPipelinesList()
                if (this.pipelinesList.length) {
                    this.pendingSaveParams = params
                    return false
                }
                return true
            },

            handleConfirmAliasChange () {
                this.pipelinesDialogPayload.isShow = false
                if (this.pendingSaveParams) {
                    this.doSave(this.pendingSaveParams)
                    this.pendingSaveParams = null
                }
            },

            async submitCodelib () {
                if (this.isFetchingData || this.isLoading) return
                if (this.isOAUTH && !this.oAuth.hasPower) {
                    this.isShow = false
                    return
                }

                const { projectId, codelib, repositoryHashId } = this
                let params = this.ensureProjectName(
                    this.applyPacConstraints(Object.assign({}, codelib))
                )
                try {
                    this.$refs.form.$refs.form.validate().then(async () => {
                        if (!this.$refs.form.urlErrMsg) {
                            if (this.isSvn) {
                                params.region = parsePathRegion(codelib.url)
                            }
                            params = this.ensureProjectName(params)
                            const canSave = await this.checkAliasChangeBeforeSave(params)
                            if (!canSave) return
                            await this.doSave(params)
                        }
                    }, validator => {
                        console.error(validator)
                    })
                } catch (e) {
                    if (e.code === 403) {
                        this.$showAskPermissionDialog({
                            noPermissionList: [{
                                actionId: this.$permissionActionMap.edit,
                                resourceId: this.$permissionResourceMap.code,
                                instanceId: [{
                                    id: repositoryHashId,
                                    name: codelib.aliasName
                                }],
                                projectId
                            }]
                        })
                    } else {
                        this.$bkMessage({
                            message: e.message,
                            theme: 'error'
                        })
                    }
                }
            },

            async doSave (params) {
                const { projectId, repositoryHashId } = this
                this.isLoading = true
                try {
                    await this.editRepo({
                        projectId,
                        repositoryHashId,
                        params
                    })
                    this.isShow = false
                    this.$bkMessage({
                        message: this.$t('codelib.successfullyEdited'),
                        theme: 'success'
                    })
                    await this.fetchRepoDetail(repositoryHashId)
                    await this.refreshCodelibList()
                } catch (e) {
                    this.$bkMessage({
                        theme: 'error',
                        message: e.message || e
                    })
                } finally {
                    this.isLoading = false
                }
            },

            handleCancel () {
                this.$refs.form?.$refs.form?.clearError()
                this.$refs.form?.resetEditCache?.()
                this.pendingSaveParams = null
                this.isShow = false
            },

            handleDialogChange (val) {
                if (!val) {
                    this.$refs.form?.$refs.form?.clearError()
                    this.$refs.form?.resetEditCache?.()
                    this.resetSharedDialogState()
                    this.oauthUserList = []
                    this.isFetchingData = false
                    this.isOpening = false
                    this.pendingSaveParams = null
                    this.resetPipelinesDialog()
                }
            }
        }
    }
</script>

<style lang="scss">
    .codelib-orerate-oauth-dialog {
        .bk-dialog-footer {
            display: none;
        }
    }
    .bk-dialog-title {
        text-align: left;
        font-size: 14px;
        color: #313238;
        font-weight: 400;
    }
    .codelib-credential-selector {
        width: 300px;
        display: inline-block;
        margin-right: 4px;
    }
    .error-tips {
        display: block;
    }
    .add-cred-btn {
        position: relative;
        top: -10px;
        cursor: pointer;
        color: #3c96ff;
        line-height: 1.5;
        font-size: 12px;
    }

    .form-radio {
        margin-top: 4px;
        margin-left: 0;
        >label {
            margin-right: 30px;
        }
    }
    .bk-option-content {
        display: flex;
        .name {
            display: inline-block;
            width: 92%;
            overflow: hidden;
            text-overflow: ellipsis;
        }
        .cre-icon {
            position: absolute;
            right: 15px;
            margin-top: 10px;
        }
    }
    .flex-content {
        display: flex;
        justify-content: center;
        align-items: center;
        .tip-icon {
            margin-left: 5px;
        }
    }
    .bk-form:nth-child(1) {
        margin-top: -20px !important;
    }
    .bk-form-item {
        margin-top: 20px !important;
    }
    .example-tips {
        color: #c4c6cd;
        font-size: 12px;
    }
</style>
