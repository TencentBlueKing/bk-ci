<template>
    <div class="codelib-content">
        <template v-if="hasCodelibs || aliasName.length || isLoading">
            <div id="codelib-list-content">
                <layout :flod.sync="isListFlod" @on-flod="handleLayoutFlod">
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
                            <bk-input :placeholder="$t('codelib.aliasNamePlaceholder')"
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
                v-for="typeLabel in codelibTypes"
                :key="typeLabel"
                @click="createCodelib(typeLabel)"
            >
                {{ $t('codelib.linkCodelibLabel', [typeLabel]) }}
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
            @updateRepoId="handleUpdateRepo"
        ></code-lib-dialog>
    </div>
</template>

<script>
    import layout from '../components/layout'
    import LinkCodeLib from '../components/LinkCodeLib'
    import CodeLibTable from '../components/CodeLibTable'
    import CodeLibDetail from '../components/CodeLibDetail'
    import CodeLibDialog from '../components/CodeLibDialog'
    import { mapState, mapActions } from 'vuex'
    import { getOffset } from '../utils/'
    import { RESOURCE_ACTION, RESOURCE_TYPE } from '../utils/permission'
    import {
        codelibTypes,
        getCodelibConfig,
        isGit,
        isGitLab,
        isGithub,
        isTGit,
        isP4,
        CODE_REPOSITORY_CACHE,
        CODE_REPOSITORY_SEARCH_VAL,
        isSvn
    } from '../config/'
    
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
                curRepo: {}
            }
        },

        computed: {
            ...mapState('codelib', ['codelibs', 'showCodelibDialog', 'gitOAuth']),
            projectId () {
                return this.$route.params.projectId
            },
            codelibTypes () {
                return codelibTypes
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
            this.init()
            this.projectList = this.$store.state.projectList

            this.refreshCodelibList()
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
                this.createCodelib(type, true)
                this.checkOAuth({ projectId: this.projectId, type })
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
                'checkOAuth'
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

            async createCodelib (typeLabel, isEdit) {
                const { credentialTypes, typeName } = getCodelibConfig(typeLabel)
                const CodelibDialog = {
                    showCodelibDialog: true,
                    projectId: this.projectId,
                    credentialTypes,
                    typeName,
                    svnType: 'ssh'
                }
                if (isGit(typeName) || isGithub(typeName)) {
                    Object.assign(CodelibDialog, { authType: 'OAUTH' })
                    if (isEdit) Object.assign(CodelibDialog, { repositoryHashId: this.$route.hash.split('-')[1] })
                }
                if (isTGit(typeName)) {
                    Object.assign(CodelibDialog, { authType: 'HTTPS' })
                    if (isEdit) Object.assign(CodelibDialog, { repositoryHashId: this.$route.hash.split('-')[1] })
                }
                if (isGitLab(typeName) || isSvn(typeName)) {
                    Object.assign(CodelibDialog, { authType: 'SSH' })
                }
                if (isP4(typeName)) {
                    Object.assign(CodelibDialog, { authType: 'HTTP' })
                }
                this.toggleCodelibDialog(CodelibDialog)
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
