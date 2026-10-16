import Vue from 'vue'
import {
    REPOSITORY_API_URL_PREFIX
} from '../../store/constants'
import { mapActions, mapState } from 'vuex'
import {
    isP4,
    isSvn,
    isGit,
    isTGit,
    isGithub,
    isGitLab,
    isScmGit,
    isScmSvn,
    getCodelibConfig
} from '../../config/'
import { parsePathAlias } from '../../utils'
import { cloneDeep } from 'lodash-es'
const vue = new Vue()
export default {
    props: {
        oauthUserList: {
            type: Array,
            default: () => []
        },
        isEditMode: {
            type: Boolean,
            default: false
        },
        enablePac: {
            type: Boolean,
            default: false
        }
    },
    data () {
        return {
            editCacheCodelib: null,
            pacProjectName: '', // 已开启PAC的项目名
            isLoadingTickets: false,
            urlErrMsg: '',
            disabledPACBtn: false,
            placeholders: {
                url: {
                    SVNssh: this.$t('codelib.svnUrlPlaceholder'),
                    SVNhttp: this.$t('codelib.svnUrlPlaceholder'),
                    GitSSH: this.$t('codelib.gitUrlPlaceholder'),
                    GitHTTP: this.$t('codelib.httpUrlPlaceholder'),
                    TGit: this.$t('codelib.tgitUrlPlaceholder'),
                    GitlabSSH: this.$t('codelib.gitlabUrlPlaceholder'),
                    GitlabHTTP: this.$t('codelib.gitlabUrlPlaceholder'),
                    HTTP: this.$t('codelib.httpUrlPlaceholder'),
                    HTTPS: this.$t('codelib.httpsUrlPlaceholder'),
                    SSH: this.$t('codelib.gitUrlPlaceholder')
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
        ...mapState('codelib', [
            'tickets',
            'codelib',
            'showCodelibDialog',
            'fetchingCodelibDetail',
            'gitOAuth',
            'githubOAuth',
            'tgitOAuth',
            'scmgitOAuth',
            'scmsvnOAuth',
            'providerConfig'
        ]),
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
        oauthProjectList () {
            if (this.usePacUrlInput) {
                return []
            }
            const projects = [...(this.oAuth.project || [])]
            const url = this.codelib?.url
            if (this.isEditMode && url && !projects.some(p => p.httpUrl === url)) {
                projects.unshift({
                    httpUrl: url,
                    nameWithNameSpace: this.codelib.aliasName
                })
            }
            return projects
        },
        oauthUserOptions () {
            const users = [...(this.oauthUserList || [])]
            const userName = this.codelib?.userName
            if (userName && !users.some(u => u.username === userName)) {
                users.unshift({ username: userName })
            }
            return users
        },
        codelibTypeName () {
            return this.codelib && this.codelib['@type']
                ? this.codelib['@type']
                : ''
        },
        codelibTypeConstants () {
            const codelibTypes = this.isScmGit || this.isScmSvn
                ? ['scmgit', 'scmsvn']
                : ['github', 'git', 'tgit']

            const regex = new RegExp(`^\\S*?(${codelibTypes.join('|')})`, 'i')
            return this.codelibTypeName.toLowerCase().replace(regex, '$1')
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
            let payload = `${this.codelibConfig.label}${this.codelib.authType}`
            if (this.codelibConfig.label === 'SVN') {
                payload = `${this.codelibConfig.label}${this.codelib.svnType}`
            }
            return (
                this.placeholders.url[payload]
                || this.placeholders.url[this.codelib.authType]
            )
        },
        credentialPlaceholder () {
            return this.placeholders.cred[this.codelibConfig.label]
        },
        portPlaceholder () {
            return this.placeholders.port[this.codelibConfig.label]
        },
        isScmConfig () {
            return this.codelib?.scmCode?.includes('SCM_')
        },
        usePacUrlInput () {
            return this.isEditMode && this.enablePac && this.isOAUTH
        },
        selectComBindData () {
            const bindData = {
                searchable: true,
                clearable: false,
                placeholder: this.$t('codelib.codelibUrlPlaceholder')
            }
            // 编辑模式下也需要支持远程搜索，方便切换代码库地址
            if (this.isGit) {
                bindData.remoteMethod = this.handleSearchCodeLib
            }
            if (this.isScmGit) {
                bindData.remoteMethod = this.handleSearchScmCodeLib
            }
            return bindData
        },
        formRules () {
            const _ = this
            const rulesMap = {
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
                        trigger: 'blur'
                    },
                    {
                        validator: async function (value) {
                            let result = true
                            await vue.$ajax.get(
                                `${REPOSITORY_API_URL_PREFIX}/user/repositories/${_.projectId}/hasAliasName?aliasName=${value}${_.repositoryHashId ? `&repositoryHashId=${_.repositoryHashId}` : ''}`
                            )
                                .then((res) => {
                                    result = !res
                                })
                            return result
                        },
                        message: this.$t('codelib.代码库别名不能重复'),
                        trigger: 'change'
                    }
                ],
                credentialId: [
                    {
                        required: true,
                        message: this.$t('codelib.credentialRequired'),
                        trigger: 'blur'
                    }
                ]
            }

            if (this.codelibConfig.label === 'Github') delete rulesMap.credentialId

            return rulesMap
        },
        isOAUTH () {
            if (this.isScmGit || this.isScmSvn) {
                return this.codelib.credentialType === 'OAUTH'
            }
            return this.codelib.authType === 'OAUTH'
        },
        repositoryType () {
            const typeMap = {
                codeP4: 'CODE_P4',
                codeSvn: 'CODE_SVN',
                codeGit: 'CODE_GIT',
                codeTGit: 'CODE_TGIT',
                codeGitlab: 'CODE_GITLAB',
                github: 'GITHUB',
                scmGit: 'SCM_GIT',
                scmSvn: 'SCM_SVN'
            }
            return typeMap[this.codelibTypeName]
        }
    },
    watch: {
        tickets () {
            this.isLoadingTickets = false
        },
        'codelib.url': function (newVal, oldVal) {
            if (!this.isEditMode) {
                this.handleCheckPacProject(newVal)
            }
            const { codelib, codelibTypeName } = this
            const { alias, msg } = parsePathAlias(
                codelibTypeName,
                newVal,
                codelib.authType,
                codelib.svnType
            )
            this.urlErrMsg = msg
            
            if (!newVal) {
                this.urlErrMsg = ''
            }

            if (this.isEditMode && newVal === oldVal) {
                return
            }

            const projectName = this.isP4
                ? newVal
                : (alias || this.codelib.projectName || '')
            
            const param = {
                projectName,
                url: newVal
            }

            if (!this.isEditMode) {
                param.aliasName = alias || this.codelib.aliasName
            }
            this.updateCodelib(param)
        },

        isEditMode (val) {
            if (val && this.codelib?.repositoryHashId) {
                this.editCacheCodelib = cloneDeep(this.codelib)
            } else if (!val) {
                this.editCacheCodelib = null
            }
        },
        
        showCodelibDialog (val) {
            if (!val) {
                this.pacProjectName = ''
            }
        }
    },

    methods: {
        ...mapActions('codelib', [
            'requestTickets',
            'toggleCodelibDialog',
            'updateCodelib',
            'gitOAuth',
            'checkScmOAuth',
            'checkOAuth',
            'checkTGitOAuth',
            'setTemplateCodelib',
            'checkPacProject'
        ]),

        handleSearchCodeLib (search) {
            const { projectId, codelibTypeConstants } = this
            this.checkOAuth({
                projectId,
                type: codelibTypeConstants,
                search,
                username: this.codelib.userName
            })
        },

        handleSearchScmCodeLib (search) {
            const { projectId, codelibTypeConstants } = this
            this.checkScmOAuth({
                projectId,
                type: codelibTypeConstants,
                scmCode: this.codelib.scmCode,
                search,
                username: this.codelib.userName
            })
        },

        openValidate () {
            window.location.href = this[`${this.codelibTypeConstants}OAuth`].url
        },
        
        authTypeChange (codelib) {
            if (this.isEditMode) {
                const cache = this.editCacheCodelib || {}
                const val = codelib.authType
                const aliasName = codelib.aliasName
                if (val === cache.authType) {
                    Object.assign(codelib, {
                        url: cache.url,
                        credentialId: cache.credentialId,
                        userName: cache.userName,
                        aliasName
                    })
                    this.$refs.form?.clearError()
                    this.urlErrMsg = ''
                    if (val !== 'OAUTH') {
                        this.$nextTick(() => this.getTickets())
                    }
                    return
                }
                if (this.isGitLab) {
                    if (val === 'HTTP' && cache.authType === 'SSH') {
                        const { url } = codelib
                        codelib.url = `https://${url.split('@')[1].replace(':', '/')}`
                        codelib.credentialId = ''
                    } else if (val === 'SSH' && cache.authType === 'HTTP') {
                        const { url } = codelib
                        codelib.url = `git@${url.split('://')[1].replace('.com/', '.com:')}`
                        codelib.credentialId = ''
                    }
                }
                if (this.isGit) {
                    if (['OAUTH', 'HTTP'].includes(val) && cache.authType === 'SSH') {
                        const { url } = codelib
                        codelib.url = url.replace('com:', 'com/').replace('git@', 'https://')
                        codelib.credentialId = ''
                    } else if (val === 'SSH' && ['OAUTH', 'HTTP'].includes(cache.authType)) {
                        const { url } = codelib
                        if (url.startsWith('https://')) {
                            codelib.url = url.replace('com/', 'com:').replace('https://', 'git@')
                        } else {
                            codelib.url = url.replace('com/', 'com:').replace('http://', 'git@')
                        }
                        codelib.credentialId = ''
                    } else if (val === 'HTTP' && cache.authType === 'OAUTH') {
                        codelib.url = cache.url
                        codelib.credentialId = ''
                    } else if (val === 'OAUTH' && cache.authType === 'HTTP') {
                        const { url } = codelib
                        if (url.startsWith('http://')) {
                            codelib.url = url.replace('http://', 'https://')
                        }
                        codelib.userName = cache.userName || codelib.userName
                    }
                }
                codelib.aliasName = aliasName
                this.$refs.form?.clearError()
                this.urlErrMsg = ''
                if (val !== 'OAUTH') {
                    this.$nextTick(() => this.getTickets())
                }
                return
            }
            // 切换重置参数
            Object.assign(codelib, {
                aliasName: '',
                credentialId: '',
                url: ''
            })
            this.$refs.form.clearError()
            this.urlErrMsg = ''
        },

        authTypeChangeAsCustom (codelib) {
            const credentialTypeItem = this.providerConfig?.credentialTypeList?.find(
                i => i.credentialType === codelib.credentialType
            )
            const authType = credentialTypeItem?.authType
            if (this.isEditMode) {
                const cache = this.editCacheCodelib || {}
                const val = codelib.credentialType
                const oldVal = cache.credentialType
                const aliasName = codelib.aliasName
                Object.assign(codelib, {
                    authType,
                    svnType: authType
                })
                if (val === oldVal) {
                    Object.assign(codelib, {
                        url: cache.url,
                        credentialId: cache.credentialId,
                        userName: cache.userName,
                        aliasName
                    })
                } else {
                    if (this.isScmGit && oldVal) {
                        if ((val.includes('OAUTH') || val.includes('USERNAME_PASSWORD')) && oldVal.includes('SSH')) {
                            const { url } = codelib
                            codelib.url = url.replace('com:', 'com/').replace('git@', 'https://')
                        }
                        if (val.includes('SSH') && (oldVal.includes('OAUTH') || oldVal.includes('USERNAME_PASSWORD'))) {
                            const { url } = codelib
                            if (url.startsWith('https://')) {
                                codelib.url = url.replace('com/', 'com:').replace('https://', 'git@')
                            } else {
                                codelib.url = url.replace('com/', 'com:').replace('http://', 'git@')
                            }
                        }
                    }
                    codelib.credentialId = ''
                    if (val.includes('OAUTH')) {
                        codelib.userName = cache.userName || codelib.userName
                    }
                    codelib.aliasName = aliasName
                }
                this.$refs.form?.clearError()
                this.urlErrMsg = ''
                if (!val?.includes('OAUTH')) {
                    this.$nextTick(() => this.getTickets())
                }
                return
            }
            Object.assign(codelib, {
                authType,
                svnType: authType,
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
                credentialTypes: (this.isScmGit || this.isScmSvn) ? this.codelib.credentialType : credentialTypes
            })
        },
        refreshTicket (isShow) {
            isShow && this.getTickets()
        },
        addCredential () {
            const { projectId, codelibConfig } = this
            const credentialType = this.isScmGit || this.isScmSvn ? this.codelib.credentialType : codelibConfig.addType
            window.open(
                `/console/ticket/${projectId}/createCredential/${credentialType}/true`,
                '_blank'
            )
        },
        svnTypeChange () {
            if (this.isEditMode) {
                const cache = this.editCacheCodelib || {}
                const val = this.codelib.svnType
                if (val === cache.svnType) {
                    this.updateCodelib({
                        url: cache.url,
                        credentialId: cache.credentialId
                    })
                    this.$refs.form?.clearError()
                    this.urlErrMsg = ''
                    this.$nextTick(() => this.getTickets())
                    return
                }
                const { url } = this.codelib
                const urlArr = (url || '').split('://')
                const hostPart = urlArr.length > 1 ? urlArr[1] : url
                const newUrl = val === 'ssh'
                    ? `svn+ssh://${hostPart}`
                    : `https://${hostPart}`
                this.updateCodelib({
                    url: newUrl,
                    credentialId: ''
                })
                this.$refs.form?.clearError()
                this.urlErrMsg = ''
                this.$nextTick(() => this.getTickets())
                return
            }
            this.updateCodelib({
                url: '',
                aliasName: '',
                credentialId: ''
            })
            this.$refs.form.clearError()
            this.urlErrMsg = ''
        },

        resetEditCache () {
            this.editCacheCodelib = null
        },

        initEditCache (codelib) {
            this.editCacheCodelib = cloneDeep(codelib)
        },

        /**
         * @desc 校验项目是否已经开启PAC模式
         * @params {String} repoUrl 仓库url
         */
        handleCheckPacProject (repoUrl) {
            if (this.isEditMode) return
            if (this.providerConfig.pacEnabled && this.isOAUTH && repoUrl) {
                this.checkPacProject({
                    repoUrl,
                    repositoryType: this.repositoryType
                }).then((res) => {
                    this.pacProjectName = res
                })
            }
        }
    }
}
