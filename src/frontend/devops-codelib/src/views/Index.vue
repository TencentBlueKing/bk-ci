<template>
    <div class="codelib-content" v-bkloading="{ isLoading, title: $t('codelib.laodingTitle') }">
        <template v-if="hasCodelibs">
            <link-code-lib v-if="codelibs.hasCreatePermission" :create-codelib="createCodelib"></link-code-lib>
            <bk-button theme="primary" v-else @click.stop="goCreatePermission">
                <i class="devops-icon icon-plus"></i>
                <span>{{ $t('codelib.linkCodelib') }}</span>
            </bk-button>
            <bk-input :placeholder="$t('codelib.aliasNamePlaceholder')"
                class="codelib-search"
                :clearable="true"
                right-icon="icon-search"
                v-model="aliasName"
                @enter="refreshCodelibList(projectId, page, pageSize, aliasName)"
                @change="clearAliasName"
            >
            </bk-input>
            <code-lib-table v-bind="codelibs" :switch-page="switchPage" @handleSortChange="handleSortChange"></code-lib-table>
        </template>
        <empty-tips v-else-if="codelibs && codelibs.hasCreatePermission" :title="$t('codelib.codelib')" :desc="$t('codelib.codelibDesc')">
            <bk-button v-for="typeLabel in codelibTypes" :key="typeLabel" @click="createCodelib(typeLabel)">
                {{ $t('codelib.linkCodelibLabel', [typeLabel]) }}
            </bk-button>
        </empty-tips>
        <empty-tips v-else :title="$t('codelib.noCodelibPermission')" :desc="$t('codelib.noPermissionDesc')">
            <bk-button theme="primary" @click="switchProject">{{ $t('codelib.switchProject') }}</bk-button>
            <bk-button theme="success" @click="toApplyPermission">{{ $t('codelib.applyPermission') }}</bk-button>
        </empty-tips>
        <code-lib-dialog :refresh-codelib-list="refreshCodelibList" @powersValidate="powerValidate"></code-lib-dialog>
    </div>
</template>

<script>
    import LinkCodeLib from '../components/LinkCodeLib'
    import CodeLibTable from '../components/CodeLibTable'
    import CodeLibDialog from '../components/CodeLibDialog'
    import { mapState, mapActions } from 'vuex'
    import {
        codelibTypes,
        getCodelibConfig,
        isGit,
        isGithub,
        isGitLab,
        isTGit,
        isP4
    } from '../config/'
    export default {
        name: 'codelib-list',

        components: {
            LinkCodeLib,
            CodeLibTable,
            CodeLibDialog
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
                sortType: ''
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

        async created () {
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
                if (isGitLab(typeName)) {
                    Object.assign(CodelibDialog, { authType: 'SSH' })
                }
                if (isP4(typeName)) {
                    Object.assign(CodelibDialog, { authType: 'HTTP' })
                }
                this.toggleCodelibDialog(CodelibDialog)
            },

            powerValidate (url) {
                window.open(url, '_self')
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
            }
        }
    }
</script>

<style lang="scss">
.codelib-content {
    min-height: 100%;
    padding: 20px 30px 0;
    .codelib-search {
        position: absolute;
        top: 20px;
        left: 180px;
        width: 240px;
    }
}
</style>
