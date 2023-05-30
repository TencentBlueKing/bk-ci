<template>
    <div class="codelib-content">
        <template v-if="hasCodelibs || aliasName.length || isLoading">
            <div id="codelib-list-content">
                <layout :flod="isListFlod" @on-flod="handleLayoutFlod">
                    <template>
                        <section class="header-content">
                            <link-code-lib
                                v-if="codelibs && codelibs.hasCreatePermission"
                                :create-codelib="createCodelib"
                            >
                            </link-code-lib>
                            <bk-button
                                v-else
                                theme="primary"
                                @click.stop="goCreatePermission"
                            >
                                <i class="devops-icon icon-plus"></i>
                                <span>{{ $t('codelib.linkCodelib') }}</span>
                            </bk-button>
                            <bk-input :placeholder="$t('codelib.aliasNamePlaceholder')"
                                :class="{
                                    'codelib-search': true,
                                    'is-fold-search': isListFlod
                                }"
                                :clearable="true"
                                right-icon="icon-search"
                                v-model="aliasName"
                                @enter="refreshCodelibList(projectId, page, pageSize, aliasName)"
                                @change="clearAliasName"
                            >
                            </bk-input>
                        </section>
                        <code-lib-table
                            v-bkloading="{ isLoading }"
                            v-bind="codelibs"
                            :cur-repo-id.sync="curRepoId"
                            :limit="limit"
                            :alias-name.sync="aliasName"
                            :is-list-flod.sync="isListFlod"
                            :switch-page="switchPage"
                            @updateFlod="handleUpdateFlod"
                            @handleSortChange="handleSortChange"
                        >
                        </code-lib-table>
                    </template>
                    <template slot="flod">
                        <code-lib-detail
                            :cur-repo-id="curRepoId"
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
                @click="toApplyPermission"
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
    import {
        codelibTypes,
        getCodelibConfig,
        isGit,
        isGithub,
        isGitLab,
        isTGit,
        isP4,
        CODE_REPOSITORY_CACHE,
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
                isLoading: !this.codelibs,
                defaultPagesize: 10,
                startPage: 1,
                showCodelibDialog: false,
                aliasName: '',
                projectList: [],
                sortBy: '',
                sortType: '',
                isListFlod: false,
                curRepoId: ''
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
            /**
             * @desc 展示的列表列
             * @returns { Object }
             */
            allColumnMap () {
                if (this.isListFlod) {
                    return {
                        version: true,
                        statusDesc: true
                    }
                }
                return this.selectedTableColumn.reduce((result, item) => {
                    result[item.id] = true
                    return result
                }, {})
            }
        },

        watch: {
            codelibs: function () {
                this.isLoading = false
            },
            projectId (projectId) {
                this.refreshCodelibList(projectId)
            }
        },

        created () {
            // this.initCache()
        },

        async mounted () {
            this.init()
            this.projectList = this.$store.state.projectList

            this.refreshCodelibList()
            if (
                this.$route.hash.includes('popupGit')
                || this.$route.hash.includes('popupGithub')
            ) {
                const type = this.$route.hash.includes('popupGithub')
                    ? 'github'
                    : 'git'
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
                const cachae = JSON.parse(localStorage.getItem(CODE_REPOSITORY_CACHE))
                const { top } = getOffset(document.getElementById('codelib-list-content'))
                const windowHeight = window.innerHeight
                const tableHeadHeight = 42
                const paginationHeight = 63
                const windownOffsetBottom = 20
                const listTotalHeight = windowHeight - top - tableHeadHeight - paginationHeight - windownOffsetBottom - 52
                const tableRowHeight = 42

                const id = (query && query.id) || (cachae && cachae.id) || ''
                const page = (query && query.page) || (cachae && cachae.page) || 1
                const limit = (query && query.limit) || (cachae && cachae.limit) || Math.floor(listTotalHeight / tableRowHeight)

                this.defaultPagesize = limit

                if (id) {
                    this.isListFlod = true
                    this.curRepoId = id
                    this.startPage = page
                    this.$router.push({
                        query: {
                            id,
                            page,
                            limit
                        }
                    })
                }
            },

            clearAliasName () {
                if (this.aliasName === '') this.refreshCodelibList()
            },

            switchPage (page, pageSize) {
                const { projectId } = this
                this.refreshCodelibList(projectId, page, pageSize)
            },

            refreshCodelibList (
                projectId = this.projectId,
                page = this.startPage,
                pageSize = this.defaultPagesize,
                aliasName = this.aliasName,
                sortBy = this.sortBy,
                sortType = this.sortType
            ) {
                this.isLoading = true
                this.requestList({
                    projectId,
                    aliasName,
                    page,
                    pageSize,
                    sortBy,
                    sortType
                })
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

            async toApplyPermission () {
                this.applyPermission(this.$permissionActionMap.create, this.$permissionResourceMap.code, [{
                    id: this.projectId,
                    type: this.$permissionResourceTypeMap.PROJECT
                }])
            },

            goCreatePermission () {
                this.$showAskPermissionDialog({
                    noPermissionList: [{
                        actionId: this.$permissionActionMap.create,
                        resourceId: this.$permissionResourceMap.code,
                        instanceId: [],
                        projectId: this.projectId
                    }]
                })
            },

            handleSortChange (payload) {
                const { sortBy, sortType } = payload
                this.sortBy = sortBy
                this.sortType = sortType
                this.refreshCodelibList()
            },

            handleUpdateFlod (payload) {
                this.isListFlod = payload
            },

            handleUpdateRepo (id) {
                console.log(id, 12312312312)
                this.curRepoId = id
            },

            handleLayoutFlod () {
                console.log(123)
            }
        }
    }
</script>

<style lang="scss">
.codelib-content {
    min-height: 100%;
    padding: 20px 30px 0;
    .codelib-search {
        width: 480px;
    }
    .is-fold-search {
        width: 270px;
    }
}
.header-content {
    display: flex;
    justify-content: space-between;
}
</style>
