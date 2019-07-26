<template>
    <div class="codelib-content" v-bkloading="{ isLoading, title: &quot;正在加载代码库列表&quot; }">
        <template v-if="hasCodelibs">
            <link-code-lib v-if="codelibs.hasCreatePermission" :create-codelib="createCodelib"></link-code-lib>
            <bk-button theme="primary" v-else @click.stop="goCreatePermission">
                <i class="bk-icon icon-plus"></i>
                <span>关联代码库</span>
            </bk-button>
            <code-lib-table v-bind="codelibs" :switch-page="switchPage"></code-lib-table>
        </template>
        <empty-tips v-else-if="codelibs && codelibs.hasCreatePermission" title="代码库" desc="代码库服务（Code）是将 SVN 及 GIT 的代码库与用户、凭据等进行关联，统一项目代码库的配置管理，便利于流水线代码库原子的编排。">
            <bk-button v-for="typeLabel in codelibTypes" theme="primary" :key="typeLabel" @click="createCodelib(typeLabel)">
                关联{{typeLabel}}代码库
            </bk-button>
        </empty-tips>
        <empty-tips v-else title="无代码库权限" desc="你在该项目[代码库]下没有[创建]权限，请切换项目或申请相应权限">
            <bk-button type="primary" @click="switchProject">切换项目</bk-button>
            <bk-button type="success" @click="goApplyPerm">申请权限</bk-button>
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
        isGitLab
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
                projectList: []
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
            ...mapActions('codelib', [
                'requestList',
                'updateCodelib',
                'toggleCodelibDialog',
                'checkOAuth'
            ]),

            switchPage (page, pageSize) {
                const { projectId } = this
                this.refreshCodelibList(projectId, page, pageSize)
            },

            refreshCodelibList (
                projectId = this.projectId,
                page = this.startPage,
                pageSize = this.defaultPagesize
            ) {
                this.isLoading = true
                this.requestList({
                    projectId,
                    page,
                    pageSize
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
                if (isGitLab(typeName)) {
                    Object.assign(CodelibDialog, { authType: 'HTTP' })
                }
                this.toggleCodelibDialog(CodelibDialog)
            },

            powerValidate (url) {
                // console.log(url)
                window.open(url, '_self')
            },

            switchProject () {
                this.iframeUtil.toggleProjectMenu(true)
            },

            goApplyPerm () {
                const url = `/backend/api/perm/apply/subsystem/?client_id=code&project_code=${
                    this.projectId
                }&service_code=code&role_creator=repertory`
                window.open(url, '_blank')
            },

            goCreatePermission () {
                this.iframeUtil.showAskPermissionDialog({
                    noPermissionList: [
                        {
                            resource: '代码库',
                            option: '创建'
                        }
                    ],
                    applyPermissionUrl: `/backend/api/perm/apply/subsystem/?client_id=code&project_code=${
                        this.projectId
                    }&service_code=code&role_creator=repertory`
                })
            }
        }
    }
</script>

<style lang="scss">
.codelib-content {
    min-height: 100%;
    padding: 20px 30px 0;
}
</style>
