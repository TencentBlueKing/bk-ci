import Vue from 'vue'
import {
    REPOSITORY_API_URL_PREFIX
} from '../../store/constants'
import { mapActions, mapState } from 'vuex'
import { getCodelibConfig, isSvn, isGit, isGithub, isTGit, isP4, isGitLab } from '../../config/'
import { parsePathAlias, parsePathRegion } from '../../utils'
const vue = new Vue()
export default {
    props: {
        padding: {
            type: Number,
            default: 24
        },
        width: {
            type: Number,
            default: 800
        },
        refreshCodelibList: {
            type: Function,
            required: true
        }
    },
    data () {
        return {
            isLoadingTickets: false,
            showRefreshBtn: false,
            loading: false,
            saving: true,
            urlErrMsg: '',
            hasValidate: false,
            placeholders: {
                url: {
                    SVN: this.$t('codelib.svnUrlPlaceholder'),
                    Git: this.$t('codelib.gitUrlPlaceholder'),
                    TGit: this.$t('codelib.tgitUrlPlaceholder'),
                    Gitlab: this.$t('codelib.gitlabUrlPlaceholder'),
                    HTTP: this.$t('codelib.httpUrlPlaceholder'),
                    HTTPS: this.$t('codelib.httpsUrlPlaceholder')
                },
                cred: {
                    SVN: this.$t('codelib.svnCredPlaceholder'),
                    Git: this.$t('codelib.gitCredPlaceholder'),
                    Gitlab: this.$t('codelib.gitlabCredPlaceholder')
                },
                port: {
                    P4: 'localhost:1666'
                }
            }
        }
    },

    computed: {
        ...mapState({
            user: 'user'
        }),
        ...mapState('codelib', [
            'tickets',
            'codelib',
            'showCodelibDialog',
            'fetchingCodelibDetail',
            'gitOAuth',
            'githubOAuth',
            'tGitOAuth'
        ]),
        isShow: {
            get () {
                return this.showCodelibDialog
            },
            set (showCodelibDialog) {
                this.toggleCodelibDialog({
                    showCodelibDialog
                })
            }
        },
        hasPower () {
            return (
                (this.isTGit
                    ? this.tGitOAuth.status
                    : this.isGit
                        ? this.gitOAuth.status
                        : this.githubOAuth.status) !== 403
            )
        },
        oAuth () {
            return this.isTGit
                ? this.tGitOAuth
                : this.isGit
                    ? this.gitOAuth
                    : this.githubOAuth
        },
        codelibTypeName () {
            return this.codelib && this.codelib['@type']
                ? this.codelib['@type']
                : ''
        },
        codelibTypeConstants () {
            return this.codelibTypeName
                .toLowerCase()
                .replace(/^\S*?([github|git|tgit])/i, '$1')
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
        title () {
            return this.$t('codelib.linkRepo', [
                this.codelibConfig.label
            ])
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
        credentialList () {
            return this.tickets || []
        },
        projectId () {
            return this.$route.params.projectId
        },
        repositoryHashId () {
            return this.codelib ? this.codelib.repositoryHashId : ''
        },
        credentialTypes () {
            return this.codelibConfig.credentialTypes
        },
        credentialId: {
            get () {
                return this.codelib.credentialId
            },

            set (credentialId) {
                this.updateCodelib({
                    credentialId
                })
            }
        },
        codelibPort: {
            get () {
                return this.codelib.url
            },
            set (url) {
                const param = {
                    projectName: url,
                    url
                }
                this.updateCodelib(param)
            }
        },
        urlPlaceholder () {
            return (
                this.placeholders.url[this.codelibConfig.label]
                || this.placeholders.url[this.codelib.authType]
            )
        },
        credentialPlaceholder () {
            return this.placeholders.cred[this.codelibConfig.label]
        },
        portPlaceholder () {
            return this.placeholders.port[this.codelibConfig.label]
        },
        selectComBindData () {
            const bindData = {
                searchable: true,
                clearable: false
            }
            if (this.isGit) {
                bindData.remoteMethod = this.handleSearchCodeLib
            }
            return bindData
        },
        formRules () {
            const rulesMap = {
                Git: {
                    url: [
                        {
                            required: true,
                            message: this.$t('codelib.codelibUrlPlaceholder'),
                            trigger: 'blur'
                        }
                    ],
                    aliasName: [
                        {
                            required: true,
                            message: this.$t('codelib.aliasNameEnter'),
                            trigger: 'change'
                        },
                        {
                            validator: async function (value) {
                                let result = true
                                await vue.$ajax.get(`${REPOSITORY_API_URL_PREFIX}/user/repositories/${this.projectId}/hasAliasName?aliasName=${value}${this.repositoryHashId ? `&repositoryHashId=${this.repositoryHashId}` : ''}`)
                                    .then((res) => {
                                        result = !res
                                    })
                                return result
                            },
                            message: this.$t('codelib.代码库别名不能重复'),
                            trigger: 'change'
                        }
                    ]
                },
                Github: {
                    url: [
                        {
                            required: true,
                            message: this.$t('codelib.codelibUrlPlaceholder'),
                            trigger: 'blur'
                        }
                    ],
                    aliasName: [
                        {
                            required: true,
                            message: this.$t('codelib.aliasNameEnter'),
                            trigger: 'change'
                        },
                        {
                            validator: async function (value) {
                                let result = true
                                await vue.$ajax.get(`${REPOSITORY_API_URL_PREFIX}/user/repositories/${this.projectId}/hasAliasName?aliasName=${value}${this.repositoryHashId ? `&repositoryHashId=${this.repositoryHashId}` : ''}`)
                                    .then((res) => {
                                        result = !res
                                    })
                                return result
                            },
                            message: this.$t('codelib.代码库别名不能重复'),
                            trigger: 'change'
                        }
                    ]
                },
                TGit: {

                },
                SVN: {

                },
                P4: {

                },
                Gitlab: {

                }
            }

            return rulesMap[this.codelibConfig.label]
        },
        isOAUTH () {
            return this.codelib.authType === 'OAUTH'
        }
    },
    watch: {
        tickets () {
            this.isLoadingTickets = false
        },
        codelib: {
            deep: true,
            handler: async function (newVal, oldVal) {
                if (newVal.authType === oldVal.authType) return
                const { projectId, codelibTypeConstants } = this

                if (newVal.authType === 'OAUTH' && !this.hasValidate) {
                    await this.checkOAuth({
                        projectId,
                        type: codelibTypeConstants
                    })
                }
                if (newVal.authType === 'T_GIT_OAUTH' && !this.hasValidate) {
                    await this.checkOAuth({
                        projectId,
                        type: codelibTypeConstants
                    })
                }
                this.saving = false
            }
        },
        'gitOAuth.status': function (newStatus) {
            if (this.isGit) {
                this.hasValidate = true
                this.saving = false
            }
        },
        'tGitOAuth.status': function (newStatus) {
            if (this.isTGit) {
                this.hasValidate = true
                this.saving = false
            }
        },
        'githubOAuth.status': function (newStatus) {
            if (this.isGithub) {
                this.hasValidate = true
                this.saving = false
            }
        },
        'codelib.url': function (newVal) {
            const { codelib, codelibTypeName } = this
            const { alias, msg } = parsePathAlias(
                codelibTypeName,
                newVal,
                codelib.authType,
                codelib.svnType
            )
            if (msg) {
                this.urlErrMsg = msg
            }
            const param = {
                projectName: alias,
                url: newVal
            }

            param.aliasName = alias || this.codelib.aliasName
            this.urlErrMsg = msg
            this.updateCodelib(param)
        },
        isShow (val) {
            if (!val) {
                this.setTemplateCodelib()
            }
        }
    },

    methods: {
        ...mapActions('codelib', [
            'requestTickets',
            'createOrEditRepo',
            'toggleCodelibDialog',
            'updateCodelib',
            'gitOAuth',
            'checkOAuth',
            'checkTGitOAuth',
            'setTemplateCodelib'
        ]),
        async submitCodelib () {
            const {
                projectId,
                user: { username },
                codelib,
                createOrEditRepo,
                repositoryHashId
            } = this
            const params = Object.assign({}, codelib, { userName: username })
            this.loading = true
            try {
                this.$refs.form.$refs.form.validate().then(async () => {
                    if (!this.urlErrMsg) {
                        this.saving = true
                        if (this.isSvn) {
                            params.region = parsePathRegion(codelib.url)
                        }
                        await createOrEditRepo({
                            projectId,
                            params,
                            hashId: repositoryHashId
                        })
                        this.toggleCodelibDialog(false)
                        this.hasValidate = false
                        this.saving = true
                        this.$bkMessage({
                            message: repositoryHashId
                                ? this.$t('codelib.successfullyEdited')
                                : this.$t('codelib.successfullyAdded'),
                            theme: 'success'
                        })
                        this.refreshCodelibList()
                    }
                }, validator => {
                    console.error(validator)
                })
            } catch (e) {
                if (e.code === 403) {
                    const actionId = this.$permissionActionMap[repositoryHashId ? 'edit' : 'create']
                    this.$showAskPermissionDialog({
                        noPermissionList: [{
                            actionId,
                            resourceId: this.$permissionResourceMap.code,
                            instanceId: repositoryHashId
                                ? [{
                                    id: repositoryHashId,
                                    name: codelib.aliasName
                                }]
                                : null,
                            projectId
                        }]
                    })
                } else {
                    this.$bkMessage({
                        message: e.message,
                        theme: 'error'
                    })
                }
                this.saving = false
            } finally {
                this.$nextTick(() => (this.loading = false))
            }
        },

        handleSearchCodeLib (search) {
            const { projectId, codelibTypeConstants } = this
            this.checkOAuth({
                projectId,
                type: codelibTypeConstants,
                search
            })
        },

        async openValidate () {
            this.showRefreshBtn = true
            window.open(this[`${this.codelibTypeConstants}OAuth`].url, '_blank')
        },
        handleCancel () {
            this.urlErrMsg = ''
            this.hasValidate = false
            this.saving = true
            this.$refs.form.$refs.form.clearError()
            this.updateCodelib({
                url: '',
                aliasName: '',
                credentialId: '',
                projectName: '',
                authType: '',
                svnType: ''
            })
        },
        authTypeChange (codelib) {
            // 切换重置参数
            Object.assign(codelib, {
                aliasName: '',
                credentialId: '',
                url: ''
            })
            this.$refs.form.clearError()
            this.urlErrMsg = ''
        },
        goToEditCre (index) {
            const { projectId, credentialList } = this
            const { credentialId } = credentialList[index]
            window.open(
                `/console/ticket/${projectId}/editCredential/${credentialId}`,
                '_blank'
            )
        },
        getTickets () {
            const { projectId, credentialTypes } = this
            this.isLoadingTickets = true
            this.requestTickets({
                projectId,
                credentialTypes
            })
        },
        refreshTicket (isShow) {
            isShow && this.getTickets()
        },
        addCredential () {
            const { projectId, codelibConfig } = this
            window.open(
                `/console/ticket/${projectId}/createCredential/${codelibConfig.addType}/true`,
                '_blank'
            )
        },
        svnTypeChange () {
            this.updateCodelib({
                url: '',
                aliasName: '',
                credentialId: ''
            })
            this.$refs.form.clearError()
            this.urlErrMsg = ''
        },
        async handleRefreshOAUTH () {
            this.saving = true
            await this.checkOAuth({
                projectId: this.projectId,
                type: this.codelibTypeConstants
            })
            this.saving = false
        }
    }
}
