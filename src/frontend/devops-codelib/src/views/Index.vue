<template>
    <div class="codelib-content">
        <template v-if="hasCodelibs || aliasName.length || isLoading">
            <div id="codelib-list-content">
                <layout
                    :flod.sync="isListFlod"
                    @on-flod="handleLayoutFlod"
                >
                    <template>
                        <section class="header-content">
                            <link-code-lib
                                v-perm="{
                                    hasPermission: codelibs && codelibs.hasCreatePermission,
                                    disablePermissionApi: true,
                                    permissionData: {
                                        projectId: projectId,
                                        resourceType: RESOURCE_TYPE,
                                        resourceCode: projectId,
                                        action: RESOURCE_ACTION.CREATE
                                    }
                                }"
                                :create-codelib="createCodelib"
                            >
                            </link-code-lib>
                            <bk-input
                                :placeholder="$t('codelib.aliasNamePlaceholder')"
                                :class="{
                                    'codelib-search': true,
                                    'is-fold-search': isListFlod
                                }"
                                :clearable="true"
                                right-icon="icon-search"
                                v-model="aliasName"
                                @enter="handleEnterSearch"
                                @change="clearAliasName"
                            >
                            </bk-input>
                        </section>
                        <code-lib-table
                            v-bkloading="{ isLoading }"
                            :limit="limit"
                            v-bind="codelibs"
                            :default-pagesize="defaultPagesize"
                            :cur-repo.sync="curRepo"
                            :switch-page="switchPage"
                            :alias-name.sync="aliasName"
                            :cur-repo-id.sync="curRepoId"
                            :is-list-flod.sync="isListFlod"
                            :refresh-codelib-list="refreshCodelibList"
                            @updateFlod="handleUpdateFlod"
                            @handleSortChange="handleSortChange"
                        >
                        </code-lib-table>
                    </template>
                    <template slot="flod">
                        <code-lib-detail
                            :cur-repo="curRepo"
                            :cur-repo-id.sync="curRepoId"
                            :codelib-list="codelibList"
                            :refresh-codelib-list="refreshCodelibList"
                            :switch-page="switchPage"
                            @updateList="handleUpdateRepoList"
                        />
                    </template>
                </layout>
            </div>
        </template>
        <empty-tips
            v-else-if="codelibs && codelibs.hasCreatePermission"
            :title="$t('codelib.codelib')"
            :desc="$t('codelib.codelibDesc')"
        >
            <bk-button
                v-for="item in codelibTypes"
                :key="item.scmType"
                :ext-cls="{
                    'is-disabled': item.status !== 'SUCCESS'
                }"
                @click="createCodelib(item.scmType, item.scmCode)"
            >
                {{ $t('codelib.linkCodelibLabel', [item.name]) }}
            </bk-button>
        </empty-tips>
        <empty-tips
            v-else
            :title="$t('codelib.noCodelibPermission')"
            :desc="$t('codelib.noPermissionDesc')"
        >
            <bk-button
                theme="primary"
                @click="switchProject"
            >
                {{ $t('codelib.switchProject') }}
            </bk-button>
            <bk-button
                theme="success"
                @click="applyPermission"
            >
                {{ $t('codelib.applyPermission') }}
            </bk-button>
        </empty-tips>
        <code-lib-dialog
            :refresh-codelib-list="refreshCodelibList"
            :oauth-user-list="oauthUserList"
            @updateRepoId="handleUpdateRepo"
        ></code-lib-dialog>
    </div>
</template>

<script>
    import { mapActions, mapState } from 'vuex'
    import CodeLibDetail from '../components/CodeLibDetail'
    import CodeLibDialog from '../components/CodeLibDialog'
    import CodeLibTable from '../components/CodeLibTable'
    import LinkCodeLib from '../components/LinkCodeLib'
    import layout from '../components/layout'
    import {
        CODE_REPOSITORY_CACHE,
        CODE_REPOSITORY_SEARCH_VAL,
        getCodelibConfig,
        convertToCamelCase,
        isGit,
        isGitLab,
        isGithub,
        isP4,
        isSvn,
        isTGit
    } from '../config/'
    import { getOffset } from '../utils/'
    import { RESOURCE_ACTION, RESOURCE_TYPE } from '../utils/permission'
    
    export default {
        name: 'codelib-list',

        components: {
            LinkCodeLib,
            CodeLibTable,
            CodeLibDialog,
            CodeLibDetail,
            layout
        },
       
        data () {
            return {
                RESOURCE_ACTION,
                RESOURCE_TYPE,
                isLoading: !this.codelibs,
                defaultPagesize: 10,
                startPage: 1,
                showCodelibDialog: false,
                aliasName: '',
                projectList: [],
                sortBy: '',
                sortType: '',
                isListFlod: false,
                curRepoId: '',
                curRepo: {},
                oauthUserList: []
            }
        },

        computed: {
            ...mapState({
                user: 'user'
            }),
            ...mapState('codelib', ['codelibTypes', 'codelibs', 'showCodelibDialog', 'gitOAuth']),
            projectId () {
                return this.$route.params.projectId
            },
            hasCodelibs () {
                const { codelibs } = this
                return codelibs && codelibs.records && codelibs.records.length > 0
            },
            codelibList () {
                return this.codelibs && this.codelibs.records
            },
            userId () {
                return this.$route.query.userId || ''
            },
            resetType () {
                return this.$route.query.resetType || ''
            }
        },

        watch: {
            codelibs: function () {
                this.isLoading = false
                if (!this.codelibs.records.length) {
                    this.isListFlod = false
                    localStorage.removeItem(CODE_REPOSITORY_CACHE)
                }
                this.curRepo = (this.codelibs && this.codelibs.records.find(codelib => codelib.repositoryHashId === this.curRepoId)) || this.curRepo
            },
            projectId (projectId) {
                this.aliasName = ''
                localStorage.removeItem(CODE_REPOSITORY_SEARCH_VAL)
                this.isListFlod = false
                this.refreshCodelibList(projectId)
            },
            curRepoId (id) {
                this.curRepo = (this.codelibs && this.codelibs.records.find(codelib => codelib.repositoryHashId === id)) || this.curRepo
            }
        },

        async mounted () {
            const { sortType, sortBy } = this.$route.query
            this.sortType = sortType ?? localStorage.getItem('codelibSortType') ?? ''
            this.sortBy = sortBy ?? localStorage.getItem('codelibSortBy') ?? ''
            await this.init()
            this.projectList = this.$store.state.projectList
            await this.refreshCodelibList()
            if (
                this.$route.hash.includes('popupGit')
                || this.$route.hash.includes('popupGithub')
                || this.$route.hash.includes('popupTGit')
            ) {
                const type = this.$route.hash.includes('popupGithub')
                    ? 'github'
                    : this.$route.hash.includes('popupGit')
                        ? 'git'
                        : 'tgit'
                this.createCodelib(type, '', true)
                this.checkOAuth({ projectId: this.projectId, type })
                const query = { ...this.$route.query }
                delete query.userId
                delete query.resetType
                this.$router.push({
                    query
                })
            } else if (this.$route.fullPath.includes('popupScm')) {
                const scmCode = this.$route.query.scmCode
                const scmType = this.codelibTypes.find(i => i.scmCode === scmCode)?.scmType
                this.createCodelib(scmType, scmCode)
            } else if (!this.resetType && this.userId) {
                const query = { ...this.$route.query }
                delete query.userId
                this.$router.push({
                    query
                })
            }
        },
        methods: {
            ...mapActions([
                'getPermRedirectUrl'
            ]),
            ...mapActions('codelib', [
                'requestList',
                'updateCodelib',
                'toggleCodelibDialog',
                'checkOAuth',
                'setProviderConfig',
                'getOauthUserList'
            ]),

            init () {
                const query = this.$route.query
                const cache = JSON.parse(localStorage.getItem(CODE_REPOSITORY_CACHE))
                const { top } = getOffset(document.getElementById('codelib-list-content'))
                const windowHeight = window.innerHeight
                const tableHeadHeight = 42
                const paginationHeight = 63
                const windowOffsetBottom = 20
                const listTotalHeight = windowHeight - top - tableHeadHeight - paginationHeight - windowOffsetBottom - 74
                const tableRowHeight = 42

                const isCacheProject = this.projectId === (cache && cache.projectId)
                const id = isCacheProject ? query.id || (cache && cache.id) : ''
                const scmType = isCacheProject ? query.scmType || (cache && cache.scmType) : ''
                const page = isCacheProject ? (cache && cache.page) : 1
                const limit = isCacheProject ? (cache && cache.limit) : Math.floor(listTotalHeight / tableRowHeight)
                if (!isCacheProject) {
                    localStorage.removeItem(CODE_REPOSITORY_SEARCH_VAL)
                }
                this.aliasName = query.searchName || JSON.parse(localStorage.getItem(CODE_REPOSITORY_SEARCH_VAL)) || ''
                this.startPage = page
                this.defaultPagesize = Number(limit)
                if (id) {
                    this.isListFlod = true
                    this.curRepoId = id
                    this.$router.push({
                        query: {
                            ...this.$route.query,
                            scmType,
                            id,
                            page,
                            limit
                        }
                    })
                }
            },

            clearAliasName () {
                if (this.aliasName === '') {
                    this.refreshCodelibList()
                    localStorage.removeItem(CODE_REPOSITORY_SEARCH_VAL)
                }
            },

            switchPage (page, pageSize) {
                const { projectId } = this
                this.refreshCodelibList(projectId, page, pageSize)
            },
            async refreshCodelibList (
                projectId = this.projectId,
                page = this.startPage,
                pageSize = this.defaultPagesize,
                aliasName = this.aliasName,
                sortBy = this.sortBy,
                sortType = this.sortType
            ) {
                if (!this.userId) this.isLoading = true
                await this.requestList({
                    projectId,
                    aliasName,
                    page,
                    pageSize,
                    sortBy,
                    sortType
                })
            },

            handleEnterSearch (val) {
                localStorage.setItem(CODE_REPOSITORY_SEARCH_VAL, JSON.stringify(val.trim()))
                this.$router.push({
                    query: {
                        ...this.$route.query,
                        searchName: val.trim()
                    }
                })
                this.refreshCodelibList(this.projectId, 1)
            },

            extractMajorMinorVersion (version) {
                let curVersion = version
                if (curVersion.startsWith('v')) {
                    curVersion = curVersion.substring(1)
                }
                
                const match = curVersion.match(/^(\d+)\./)
                
                if (match) {
                    return `/${match[1]}.0`
                }
                
                return ''
            },
            
            async fetchOauthUserList (scmCode) {
                try {
                    this.oauthUserList = await this.getOauthUserList({
                        scmCode
                    })
                    const defaultUserName = this.oauthUserList[0].username
                    const username = this.user.username
                    const curUserName = this.oauthUserList.find(i => i.username === username)?.username || defaultUserName
                    return curUserName
                } catch (err) {
                    console.error(err)
                    return this.user.username
                }
            },
            getDocUrl (url) {
                const languageCodeMatch = this.$i18n.locale.match(/^[A-Za-z]{2}/)
                const lang = (languageCodeMatch && languageCodeMatch[0].toUpperCase()) || ''
                const version = this.extractMajorMinorVersion(window.BK_CI_VERSION)
                return `/markdown/${lang}/Devops${version}${url}`
            },
            async createCodelib (typeLabel, scmCode, isEdit) {
                const userName = await this.fetchOauthUserList(scmCode)
                const providerConfig = this.codelibTypes.find(i => i.scmCode === scmCode)
                const defaultCredentialType = providerConfig?.credentialTypeList[0]
                this.setProviderConfig(providerConfig)
                const codelibType = this.codelibTypes.find(type => type.scmType === typeLabel)
                if (codelibType?.status === 'DEPLOYING') {
                    this.showUndeployDialog({
                        title: this.$t('codelib.codelibUndeployTitle', [codelibType.name]),
                        desc: this.$t(`codelib.${typeLabel.toLowerCase()}UndeployDesc`),
                        link: `${DOCS_URL_PREFIX}${this.getDocUrl(codelibType.docUrl)}`
                    })
                    return
                }

                if (typeLabel?.startsWith('SCM_')) {
                    const typeName = convertToCamelCase(typeLabel)
                    const CodelibDialog = {
                        showCodelibDialog: true,
                        projectId: this.projectId,
                        typeName,
                        svnType: defaultCredentialType.authType,
                        authType: defaultCredentialType.authType,
                        credentialType: defaultCredentialType.credentialType,
                        scmCode,
                        userName
                    }
                    this.toggleCodelibDialog(CodelibDialog)
                } else {
                    const { credentialTypes, typeName } = getCodelibConfig(typeLabel)
                    const CodelibDialog = {
                        showCodelibDialog: true,
                        projectId: this.projectId,
                        credentialTypes,
                        typeName,
                        svnType: 'ssh',
                        scmCode,
                        userName
                    }
                    if (isGit(typeName) || isGithub(typeName)) {
                        Object.assign(CodelibDialog, { authType: 'OAUTH' })
                        if (isEdit) Object.assign(CodelibDialog, { repositoryHashId: this.$route.hash.split('-')[1] })
                    } else if (isTGit(typeName)) {
                        Object.assign(CodelibDialog, { authType: 'HTTPS' })
                        if (isEdit) Object.assign(CodelibDialog, { repositoryHashId: this.$route.hash.split('-')[1] })
                    } else if (isGitLab(typeName) || isSvn(typeName)) {
                        Object.assign(CodelibDialog, { authType: 'SSH' })
                    } else if (isP4(typeName)) {
                        Object.assign(CodelibDialog, { authType: 'HTTP' })
                    }
                    this.toggleCodelibDialog(CodelibDialog)
                }
            },

            switchProject () {
                this.iframeUtil.toggleProjectMenu(true)
            },

            applyPermission () {
                this.handleNoPermission({
                    projectId: this.projectId,
                    resourceType: RESOURCE_TYPE,
                    resourceCode: this.projectId,
                    action: RESOURCE_ACTION.CREATE
                })
            },
            handleSortChange (payload) {
                const { sortBy, sortType } = payload
                this.sortBy = sortBy
                this.sortType = sortType
                this.refreshCodelibList()
                localStorage.setItem('codelibSortType', sortType)
                localStorage.setItem('codelibSortBy', sortBy)
                const queryKeys = Object.keys(this.$route?.query || {})
                if (!queryKeys.length) return
                this.$router.push({
                    query: {
                        ...this.$route.query,
                        sortBy,
                        sortType
                    }
                })
            },

            handleUpdateFlod (payload) {
                this.isListFlod = payload
            },

            handleUpdateRepo (id) {
                this.startPage = 1
                this.curRepoId = id
            },

            handleUpdateRepoList () {
                const page = this.$route.query.page
                const limit = this.$route.query.limit
                this.refreshCodelibList(this.projectId, page, limit)
            }
        }
    }
</script>

<style lang="scss">
.codelib-content {
    min-height: 100%;
    padding: 20px 30px 0;
    background-color: #F5F7FA;
    .codelib-search {
        width: 480px;
    }
    .is-fold-search {
        width: 240px;
    }
}
.header-content {
    display: flex;
    justify-content: space-between;
}
</style>
