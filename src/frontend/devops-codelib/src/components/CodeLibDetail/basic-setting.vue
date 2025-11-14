<template>
    <section class="basic-setting">
        <!-- 授权 -->
        <div class="form-item">
            <div class="label">
                {{ $t('codelib.auth') }}
                <bk-popover placement="top">
                    <Icon
                        name="help"
                        size="14"
                        class="auth-help-icon"
                    />
                    <div slot="content">
                        <template v-if="isGit || isTGit">
                            <p>{{ $t('codelib.此授权用于平台和代码库进行交互，涉及如下功能：') }}</p>
                            <p>1.{{ $t('codelib.注册 Webhook 到代码库，用于事件触发场景') }}</p>
                            <p>2.{{ $t('codelib.回写提交检测状态到代码库，用于代码库支持 checker 拦截合并请求场景') }}</p>
                            <p>3.{{ $t('codelib.流水线中 Checkout 代码') }}</p>
                            <p>{{ $t('codelib.需拥有代码库注册 Webhook 权限') }}</p>
                        </template>
                    </div>
                </bk-popover>
            </div>
            <div class="content">
                <div class="auth">
                    <Icon
                        name="check-circle"
                        size="14"
                        class="icon-success"
                    />
                    <template v-if="repoInfo.svnType">
                        <span>
                            {{ repoInfo.svnType || curRepo.svnType }}@
                        </span>
                        <a
                            v-if="(repoInfo.svnType) && !['OAUTH'].includes(repoInfo.svnType)"
                            :href="`/console/ticket/${repoInfo.projectId}/editCredential/${repoInfo.credentialId}`"
                            target="_blank"
                        >
                            {{ repoInfo.credentialId }}
                        </a>
                        <span v-else>
                            {{ repoInfo.userName || curRepo.userName }}
                        </span>
                    </template>
                    <template v-else>
                        <span>
                            {{ repoInfo.authType || curRepo.authType }}@
                        </span>
                        <a
                            v-if="(repoInfo.authType) && !['OAUTH'].includes(repoInfo.authType)"
                            :href="`/console/ticket/${repoInfo.projectId}/editCredential/${repoInfo.credentialId}`"
                            target="_blank"
                        >
                            {{ repoInfo.credentialId }}
                        </a>
                        <span v-else>
                            {{ repoInfo.userName || curRepo.userName }}
                        </span>
                    </template>
                    <a
                        class="reset-bth"
                        v-perm="{
                            hasPermission: curRepo.canEdit,
                            disablePermissionApi: true,
                            permissionData: {
                                projectId: projectId,
                                resourceType: RESOURCE_TYPE,
                                resourceCode: curRepo.repositoryHashId,
                                action: RESOURCE_ACTION.EDIT
                            }
                        }"
                        @click="handleResetAuth"
                    >
                        {{ $t('codelib.resetAuth') }}
                    </a>
                </div>
            </div>
        </div>
        
        <!-- PAC 模式 -->
        <div
            class="form-item"
            v-if="providerConfig.pacEnabled"
        >
            <div class="label">
                {{ $t('codelib.PACmode') }}
            </div>
            <p class="pac-tips">{{ $t('codelib.pacTips') }}</p>
            <div class="content">
                <div class="pac-mode">
                    <div
                        class="switcher-item"
                        :class="{ 'disabled-pac': (!repoInfo.enablePac && pacProjectName) || syncStatus === 'SYNC' }"
                        v-perm="{
                            hasPermission: curRepo.canEdit,
                            disablePermissionApi: true,
                            permissionData: {
                                projectId: projectId,
                                resourceType: RESOURCE_TYPE,
                                resourceCode: curRepo.repositoryHashId,
                                action: RESOURCE_ACTION.EDIT
                            }
                        }"
                        @click="handleTogglePacStatus"
                    >
                    </div>
                   
                    <bk-switcher
                        v-model="repoInfo.enablePac"
                        theme="primary"
                        :disabled="(!repoInfo.enablePac && pacProjectName) || syncStatus === 'SYNC'"
                    >
                    </bk-switcher>
                    <span
                        class="ml10"
                        v-if="!repoInfo.enablePac && pacProjectName"
                    >
                        {{ $t('codelib.当前代码库已在【】项目中开启 PAC 模式', [pacProjectName]) }}
                        <i
                            v-bk-tooltips="$t('codelib.相同代码库只支持在一个蓝盾项目下开启 PAC 模式')"
                            class="bk-icon bk-dialog-mark bk-dialog-warning icon-exclamation info-icon"
                        />
                    </span>

                    <div class="pac-enable">
                        {{ syncStatus === 'SYNC' ? $t('codelib.PAC 模式开启中') : repoInfo.enablePac ?
                            $t('codelib.已开启 PAC 模式')
                            : $t('codelib.未开启 PAC 模式') }}
                    </div>

                    <div
                        v-if="syncStatus === 'SUCCEED'"
                        class="pipeline-count"
                    >
                        <i18n
                            tag="div"
                            path="codelib.共N条流水线"
                        >
                            <button
                                class="bk-text-button"
                                @click="isShowPipeline = true"
                            >
                                {{ pipelineCount }}
                            </button>
                        </i18n>
                    </div>
                    
                    <!-- 同步中 -->
                    <span
                        class="async-status"
                        v-if="syncStatus === 'SYNC'"
                    >
                        <div class="bk-spin-loading bk-spin-loading-mini bk-spin-loading-primary">
                            <bk-loading
                                class="mr10"
                                is-loading
                                mode="spin"
                            />
                        </div>
                        <span class="ml5">{{ $t('codelib.正在同步代码库流水线') }}</span>
                    </span>

                    <!-- 同步失败 -->
                    <template v-if="syncStatus === 'FAILED'">
                        <i class="bk-icon bk-dialog-mark bk-dialog-warning icon-exclamation failed-icon"></i>
                        <span class="ml5">{{ $t('codelib.代码库部分 YAML 文件同步失败') }}</span>
                        <a
                            class="ml10"
                            text
                            @click="handleShowSyncFailedDetail"
                        >{{ $t('codelib.查看失败详情') }}</a>
                        <a
                            class="ml10"
                            text
                            @click="handleRefreshSync"
                        >{{ $t('codelib.重试') }}</a>
                    </template>
                </div>
            </div>
        </div>
        <!-- 历史信息 -->
        <div class="form-item">
            <div class="label">
                {{ $t('codelib.historyInfo') }}
            </div>
            <div class="history-content">
                <div class="history-item">
                    <span class="label">{{ $t('codelib.creator') }}</span>
                    <span class="value">{{ curRepo.createUser }}</span>
                </div>
                <div class="history-item">
                    <span class="label">{{ $t('codelib.recentlyEditedBy') }}</span>
                    <span class="value">{{ curRepo.updatedUser }}</span>
                </div>
                <div class="history-item">
                    <span class="label">{{ $t('codelib.createdTime') }}</span>
                    <span class="value">{{ prettyDateTimeFormat(Number(curRepo.createTime + '000')) }}</span>
                </div>
                <div class="history-item">
                    <span class="label">{{ $t('codelib.lastModifiedTime') }}</span>
                    <span class="value">{{ prettyDateTimeFormat(Number(curRepo.updatedTime + '000')) }}</span>
                </div>
            </div>
        </div>
        <bk-dialog
            ext-cls="close-repo-confirm-dialog"
            :value="showClosePac"
            :show-footer="false"
            @value-change="handleToggleShowClosePac"
        >
            <span class="close-confirm-title">
                {{ $t('codelib.关闭 PAC 模式') }}
            </span>
            <span class="close-confirm-tips">
                <p>{{ $t('codelib.检测到默认分支仍存在ci 文件目录。') }}</p>
                <p>
                    {{ $t('codelib.请先将目录') }}
                    <span>{{ $t('codelib.改名或删除') }}</span>
                    {{ $t('codelib.避免项目成员从代码库查看时误认为 YAML 文件仍生效') }}
                </p>
            </span>
            <div class="ci-status-warpper">
                <div
                    v-if="hasCiFolder"
                    class="bk-spin-loading bk-spin-loading-mini bk-spin-loading-primary"
                >
                    <bk-loading
                        class="mr10"
                        is-loading
                        mode="spin"
                    />
                </div>
                <div
                    v-else
                    class="success-icon"
                >
                    <i class="bk-icon import-status-icon icon-check-1 success-icon"></i>
                </div>
                <div class="operate-btn">
                    <p>{{ hasCiFolder ? $t('codelib.等待处理') : $t('codelib.文件目录已清空') }}</p>
                    <a
                        :href="repoInfo.url"
                        target="_blank"
                    >
                        <icon
                            name="tiaozhuan"
                            size="14"
                            class="jump-icon"
                        />
                        {{ $t('codelib.前往代码库') }}
                    </a>
                    <div class="split-line"></div>
                    <a @click="handleCheckHasCiFolder(true)">
                        <icon
                            name="refresh"
                            size="14"
                            :class="{ 'refresh-icon': true, 'refreshing': refreshLoading }"
                        />
                        {{ $t('codelib.刷新') }}
                    </a>
                </div>
            </div>
            <span class="close-confirm-footer">
                <bk-button
                    class="mr10"
                    theme="primary"
                    :disabled="hasCiFolder"
                    @click="handleClosePac"
                >
                    {{ $t('codelib.关闭PAC') }}
                </bk-button>
                <bk-button
                    @click="showClosePac = !showClosePac"
                >
                    {{ $t('codelib.取消') }}
                </bk-button>
            </span>
        </bk-dialog>

        <bk-dialog
            ext-cls="oauth-confirm-dialog"
            :width="500"
            :value="showOauthDialog"
            :show-footer="false"
            @value-change="handleToggleShowOauthDialog"
        >
            <span class="toggle-pac-warning-icon">
                <i class="devops-icon icon-exclamation" />
            </span>
            <span class="oauth-confirm-title">
                {{ $t('codelib.PAC 模式需使用 OAUTH 授权') }}
            </span>
            <span
                v-if="isGit"
                class="oauth-confirm-tips"
            >
                <p>{{ $t('codelib.尚未授权，请先点击按钮授权。') }}</p>
                <p>{{ $t('codelib.此授权用于平台和代码库进行交互，涉及如下功能：') }}</p>
                <p>1.{{ $t('codelib.注册 Webhook 到代码库，用于事件触发场景') }}</p>
                <p>2.{{ $t('codelib.回写提交检测状态到代码库，用于代码库支持 checker 拦截合并请求场景') }}</p>
                <p>3.{{ $t('codelib.流水线中 Checkout 代码') }}</p>
                <p>{{ $t('codelib.需拥有代码库注册 Webhook 权限') }}</p>
            </span>
            <bk-button
                class="ml10"
                theme="primary"
                @click="openValidate"
            >
                {{ $t('codelib.oauthCert') }}
            </bk-button>
        </bk-dialog>
        <ResetAuthDialog
            ref="resetAuth"
            :cur-repo="curRepo"
            :repo-info="repoInfo"
            :type="type"
            :user-id="userId"
            :is-p4="isP4"
            :is-svn="isSvn"
            :is-git-lab="isGitLab"
            :is-t-git="isTGit"
            :is-git="isGit"
            :is-github="isGithub"
            :is-scm-git="isScmGit"
            :is-scm-svn="isScmSvn"
            :fetch-repo-detail="fetchRepoDetail"
            @updateList="updateList"
        />

        <bk-dialog
            v-model="showSyncFailedDetail"
            ext-cls="failed-detail-dialog"
            header-position="left"
            :border="true"
            width="720"
            :title="$t('codelib.代码库同步失败')"
        >
            <div class="title-tips">{{ $t('codelib.检测到代码库中以下流水线 YAML 文件同步失败，请处理后重试') }}</div>
            <bk-table
                :data="syncFailedPipelineList"
            >
                <bk-table-column
                    :label="$t('codelib.流水线文件')"
                    width="220"
                    prop="filePath"
                    show-overflow-tooltip
                >
                    <template slot-scope="{ row }">
                        <a
                            :href="row.fileUrl"
                            target="_blank"
                        >{{ row.filePath }}</a>
                    </template>
                </bk-table-column>
                <bk-table-column
                    :label="$t('codelib.失败详情')"
                    prop="reasonDetail"
                    show-overflow-tooltip
                >
                </bk-table-column>
            </bk-table>
            <template slot="footer">
                <bk-button @click="showSyncFailedDetail = !showSyncFailedDetail">{{ $t('codelib.关闭') }}</bk-button>
            </template>
        </bk-dialog>

        <bk-sideslider
            :is-show.sync="isShowPipeline"
            :width="700"
            quick-close
            :title="$t('codelib.代码库下管理的流水线')"
        >
            <div
                slot="content"
                style="padding: 20px;"
            >
                <bk-table
                    v-bkloading="{ isLoading: isFetchLoading }"
                    :data="pipelineList"
                    :pagination="pipelinePagination"
                    @page-change="handlePageChange"
                    @page-limit-change="handleLimitChange"
                >
                    <bk-table-column
                        :label="$t('codelib.流水线名称')"
                        prop="pipelineName"
                    >
                        <template slot-scope="{ row }">
                            <a
                                :href="`/console/pipeline/${projectId}/${row.pipelineId}/history/history`"
                                target="_blank"
                            >{{ row.pipelineName }}</a>
                        </template>
                    </bk-table-column>
                </bk-table>
            </div>
        </bk-sideslider>
    </section>
</template>
<script>
    import {
        isP4,
        isGit,
        isGithub,
        isGitLab,
        isSvn,
        isTGit,
        isScmGit,
        isScmSvn
    } from '../../config/'
    import {
        mapState,
        mapActions
    } from 'vuex'
    import {
        RESOURCE_ACTION,
        RESOURCE_TYPE
    } from '@/utils/permission'
    import {
        prettyDateTimeFormat
    } from '@/utils/'
    import ResetAuthDialog from './ResetAuthDialog.vue'
 
    export default {
        name: 'basicSetting',
        components: {
            ResetAuthDialog
        },
        props: {
            type: {
                type: String,
                default: ''
            },
            repoInfo: {
                type: Object,
                default: () => {}
            },
            curRepo: {
                type: Object,
                default: () => {}
            },
            pacProjectName: {
                type: String,
                default: ''
            },
            fetchRepoDetail: {
                type: Function
            },
            refreshCodelibList: {
                type: Function
            }
        },
        data () {
            return {
                syncStatus: '',
                time: 1000,
                loopTimer: null,
                RESOURCE_ACTION,
                RESOURCE_TYPE,
                isEditing: false,
                hasCiFolder: false,
                showClosePac: false,
                showEnablePac: false,
                showOauthDialog: false,
                codelibTypeConstants: '',
                userId: '',
                showSyncFailedDetail: false,
                syncFailedPipelineList: [],
                pipelineCount: 0,
                refreshLoading: false,
                isShowPipeline: false,
                pipelineList: [],
                pipelinePagination: {
                    current: 1,
                    count: 0,
                    limit: 10
                },
                isFetchLoading: false
            }
        },
        computed: {
            ...mapState('codelib', [
                'gitOAuth',
                'githubOAuth',
                'tgitOAuth',
                'codelibTypes'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            isOAUTH () {
                return this.repoInfo.authType === 'OAUTH'
            },
            repoId () {
                return this.$route.query.id
            },
            repositoryType () {
                return this.curRepo.type
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
            isScmGit () {
                return isScmGit(this.type)
            },
            isScmSvn () {
                return isScmSvn(this.type)
            },
            isGit () {
                return isGit(this.type)
            },
            isTGit () {
                return isTGit(this.type)
            },
            isGitLab () {
                return isGitLab(this.type)
            },
            isSvn () {
                return isSvn(this.type)
            },
            isP4 () {
                return isP4(this.type)
            },
            isGithub () {
                return isGithub(this.type)
            },
            providerConfig () {
                return this.codelibTypes.find(i => i.scmCode === this.repoInfo.scmCode) || {}
            }
        },
        watch: {
            type: {
                handler (val) {
                    this.codelibTypeConstants = val.toLowerCase()
                        .replace(/^\S*?([github|git|tgit])/i, '$1')
                },
                immediate: true
            },
            'repoInfo.url': {
                handler (val) {
                    setTimeout(async () => {
                        if (!val) return
                        const { resetType, userId } = this.$route.query
                        if (['checkGitOauth', 'checkTGitOauth', 'checkGithubOauth'].includes(resetType)) {
                            await this.handleTogglePacStatus()
                        } else if (['resetGitOauth', 'resetTGitOauth', 'resetGithubOauth', 'resetScmOauth'].includes(resetType)) {
                            this.userId = userId
                        }
                    }, 200)
                },
                deep: true
            },
            repoId () {
                this.time = 1000
                this.syncStatus = ''
            },
            
            showSyncFailedDetail (val) {
                if (val) {
                    this.getListYamlSync({
                        projectId: this.projectId,
                        repositoryHashId: this.repoInfo.repoHashId
                    }).then(res => {
                        this.syncFailedPipelineList = res
                    }).catch((e) => {
                        this.$bkMessage({
                            theme: 'error',
                            message: e.message || e
                        })
                    })
                }
            },
            'repoInfo.yamlSyncStatus': {
                handler (val) {
                    this.syncStatus = val
                },
                deep: true,
                immediate: true
            },
            syncStatus (val) {
                if (val === 'SUCCEED') {
                    this.fetchPacPipelineCount()
                }
                if (val === 'SYNC') {
                    this.fetchYamlSyncStatus()
                }
            },

            isShowPipeline (val) {
                if (val) {
                    this.fetchYamlPipelines()
                } else {
                    this.pipelineList = []
                }
            }
        },
        created () {
            if (this.syncStatus === 'SUCCEED') {
                this.fetchPacPipelineCount()
            } else if (this.syncStatus === 'SYNC') {
                this.fetchYamlSyncStatus()
            }
        },
        methods: {
            ...mapActions('codelib', [
                'editRepo',
                'refreshGitOauth',
                'refreshGithubOauth',
                'closePac',
                'enablePac',
                'changeMrBlock',
                'checkHasCiFolder',
                'retrySyncRepository',
                'getListYamlSync',
                'getYamlSyncStatus',
                'getPacPipelineCount',
                'getYamlPipelines'
            ]),
            prettyDateTimeFormat,

            /**
             * 开启通用设置编辑状态
             */
            handleEditCommon () {
                this.isEditing = true
            },

            /**
             * 通用设置 —> 保存
             */
            handleSaveCommon () {
                this.changeMrBlock({
                    projectId: this.projectId,
                    repositoryHashId: this.repoInfo.repoHashId,
                    enableMrBlock: this.repoInfo.settings.enableMrBlock
                }).then(() => {
                    this.$bkMessage({
                        message: this.$t('codelib.保存成功'),
                        theme: 'success'
                    })
                }).finally(() => {
                    this.isEditing = false
                })
            },

            /**
             * 重置授权
             */
            handleResetAuth () {
                this.$refs.resetAuth.isShow = true
            },

            async handleCheckHasCiFolder (loading = false) {
                this.refreshLoading = loading
                await this.checkHasCiFolder({
                    projectId: this.projectId,
                    repositoryHashId: this.repoInfo.repoHashId
                }).then(res => {
                    this.hasCiFolder = res
                }).catch(e => {
                    console.error(e)
                }).finally(() => {
                    this.refreshLoading = false
                })
            },
        
            /**
             * 开启/关闭PAC模式
             * 关闭PAC需校验仓库状态 是否存在.ci文件夹
             *  true -> 存在.ci文件夹
             *  false -> 不存在.ci文件夹
             */
            async handleTogglePacStatus () {
                if ((!this.repoInfo.enablePac && this.pacProjectName) || this.repoInfo.yamlSyncStatus === 'SYNC') return
                switch (this.codelibTypeConstants) {
                    case 'git':
                        await this.refreshGitOauth({
                            type: 'git',
                            resetType: 'checkGitOauth',
                            redirectUrl: window.location.href
                        })
                        break
                    case 'github':
                        await this.refreshGithubOauth({
                            projectId: this.projectId,
                            resetType: 'checkGithubOauth',
                            redirectUrl: window.location.href
                        })
                        break
                    case 'tgit':
                        await this.refreshGitOauth({
                            type: 'tgit',
                            resetType: 'checkTGitOauth',
                            redirectUrl: window.location.href
                        })
                        break
                }
                
                if (this.repoInfo.enablePac) {
                    this.$emit('update:pacProjectName', '')
                    await this.handleCheckHasCiFolder()
                    if (this.hasCiFolder) {
                        this.showClosePac = true
                    } else {
                        this.$bkInfo({
                            title: this.$t('codelib.确定关闭 PAC 模式？'),
                            confirmLoading: true,
                            confirmFn: async () => {
                                try {
                                    await this.handleClosePac()
                                    this.showClosePac = false
                                    return true
                                } catch {
                                    return false
                                }
                            }
                        })
                    }
                } else {
                    if (!this.oAuth.hasPower) {
                        this.showOauthDialog = true
                        return
                    }
                    if (this.isOAUTH) {
                        this.$bkInfo({
                            title: this.$t('codelib.确定开启 PAC 模式？'),
                            confirmLoading: true,
                            confirmFn: async () => {
                                try {
                                    await this.handleEnablePac()
                                    return true
                                } catch {
                                    return false
                                }
                            }
                        })
                    } else {
                        this.$bkInfo({
                            type: 'warning',
                            title: this.$t('codelib.PAC 模式需使用 OAUTH 授权'),
                            subTitle: this.$t('codelib.确定重置授权为 OAUTH，同时开启 PAC 模式吗？'),
                            confirmLoading: true,
                            confirmFn: async () => {
                                try {
                                    const newRepoInfo = {
                                        ...this.repoInfo
                                    }
                                    if (newRepoInfo.authType === 'SSH' && newRepoInfo['@type'] === 'codeGit') {
                                        const urlMap = newRepoInfo.url.split(':')
                                        const hostName = urlMap[0].split('@')[1]
                                        const repoName = urlMap[1]
                                        newRepoInfo.url = `https://${hostName}/${repoName}`
                                    }
                                    newRepoInfo.authType = 'OAUTH'
                                    newRepoInfo.enablePac = true
                                    await this.handleUpdateRepo(newRepoInfo)
                                    setTimeout(async () => {
                                        await this.handleEnablePac()
                                    }, 500)
                                    return true
                                } catch (e) {
                                    this.$bkMessage({
                                        theme: 'error',
                                        message: e || e.message
                                    })
                                }
                            }
                        })
                    }
                }
            },

            /**
             * 更新代码库
             */
            handleUpdateRepo (repo) {
                this.editRepo({
                    projectId: this.projectId,
                    repositoryHashId: repo.repoHashId,
                    params: repo
                }).then(async () => {
                    await this.fetchRepoDetail(repo.repoHashId)
                    await this.refreshCodelibList()
                }).catch((e) => {
                    this.$bkMessage({
                        theme: 'error',
                        message: e || e.message
                    })
                }).finally(() => {
                    const { id, page, limit } = this.$route.query
                    this.$router.push({
                        query: {
                            id,
                            page,
                            limit
                        }
                    })
                })
            },

            /**
             * 关闭PAC
             */
            async handleClosePac () {
                try {
                    await this.closePac({
                        projectId: this.projectId,
                        repositoryHashId: this.repoInfo.repoHashId
                    })

                    this.$bkMessage({
                        message: this.$t('codelib.关闭成功'),
                        theme: 'success'
                    })
                    await this.fetchRepoDetail(this.repoInfo.repoHashId)
                    await this.refreshCodelibList()
                } catch (e) {
                    this.$bkMessage({
                        message: e.message || e,
                        theme: 'error'
                    })
                }
            },
            /**
             * 开启PAC
             */
            async handleEnablePac () {
                try {
                    await this.enablePac({
                        projectId: this.projectId,
                        repositoryHashId: this.repoInfo.repoHashId
                    })

                    this.$bkMessage({
                        message: this.$t('codelib.开启成功'),
                        theme: 'success'
                    })
                    const { id, page, limit } = this.$route.query
                    this.$router.push({
                        query: {
                            id,
                            page,
                            limit
                        }
                    })
                    await this.fetchRepoDetail(this.repoInfo.repoHashId)
                    await this.refreshCodelibList()
                } catch (e) {
                    this.$bkMessage({
                        message: e.message || e,
                        theme: 'error'
                    })
                }
            },

            handleToggleShowClosePac (val) {
                if (!val) {
                    this.showClosePac = val
                }
            },

            handleToggleShowOauthDialog (val) {
                if (!val) {
                    this.showOauthDialog = val
                }
            },

            async openValidate () {
                window.location.href = this[`${this.codelibTypeConstants}OAuth`].url
            },

            updateList () {
                this.$emit('updateList')
            },

            /**
             * 查看代码库同步失败详情
             */
            handleShowSyncFailedDetail () {
                this.showSyncFailedDetail = true
            },

            /**
             * 代码库同步 -- 重试
             */
            handleRefreshSync () {
                this.retrySyncRepository({
                    projectId: this.projectId,
                    repositoryHashId: this.repoInfo.repoHashId
                }).then(res => {
                    this.time = 5000
                    this.syncStatus = 'SYNC'
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('codelib.重试成功，正在同步')
                    })
                }).catch(e => {
                    this.$bkMessage({
                        theme: 'error',
                        message: e.message || e
                    })
                })
            },

            fetchYamlSyncStatus () {
                clearTimeout(this.loopTimer)
                
                if (this.syncStatus === 'SYNC') {
                    this.loopTimer = setTimeout(() => {
                        this.getYamlSyncStatus({
                            projectId: this.projectId,
                            repositoryHashId: this.repoInfo.repoHashId
                        }).then(res => {
                            this.syncStatus = res
                            this.time = 10000
                            this.fetchYamlSyncStatus()
                        }).catch((e) => {
                            this.$bkMessage({
                                message: e.message || e,
                                theme: 'error'
                            })
                        })
                    }, this.time)
                }
            },

            fetchPacPipelineCount () {
                this.getPacPipelineCount({
                    projectId: this.projectId,
                    repositoryHashId: this.repoInfo.repoHashId
                }).then(res => {
                    this.pipelineCount = res
                })
            },
            fetchYamlPipelines () {
                this.isFetchLoading = true
                this.getYamlPipelines({
                    projectId: this.projectId,
                    repositoryHashId: this.repoInfo.repoHashId,
                    page: this.pipelinePagination.current,
                    pageSize: this.pipelinePagination.limit
                }).then(res => {
                    this.pipelineList = res.records
                    this.isFetchLoading = false
                    this.pipelinePagination.count = res.count
                }).catch(e => {
                    console.error(e)
                })
            },

            handlePageChange (page) {
                this.pipelinePagination.current = page
                this.fetchYamlPipelines()
            },

            handleLimitChange (limit) {
                this.pipelinePagination.current = 1
                this.pipelinePagination.limit = limit
                this.fetchYamlPipelines()
            }
        }
    }
</script>
<style lang='scss'>
    .basic-setting {
        .form-item {
            margin-bottom: 40px;

            .label {
                display: flex;
                align-items: center;
                font-weight: 700;
                font-size: 14px;
                color: #63656E;
                ::v-deep .bk-button-text {
                    font-weight: 400 !important;
                    font-size: 12px !important;
                }
            }
            .content,
            .history-content {
                margin-top: 16px;
                font-size: 12px;
            }
            .history-content {
                max-width: 1000px;
            }
            .pac-tips {
                margin-top: 8px;
                font-size: 12px;
                color: #979BA5;
            }
            .pac-mode {
                display: flex;
                align-items: center;
            }
            .switcher-item {
                width: 36px;
                height: 20px;
                border-radius: 12px;
                opacity: 0;
                position: absolute;
                z-index: 100;
                cursor: pointer;
            }
            .disabled-pac {
                cursor: not-allowed;
            }
            .edit-icon {
                position: relative;
                top: 2px;
                margin-left: 18px;
                color: #979BA5;
                cursor: pointer;
            }
            .pac-enable {
                margin: 0 24px 0 8px;
            }
            .pipeline-count {
                height: 24px;
                line-height: 24px;
                padding: 0 15px;
                background: #F5F7FA;
                border-radius: 12px;
            }
            .async-status {
                display: flex;
            }
            .failed-icon {
                background-color: #ff9c01;
                width: 16px;
                height: 16px;
                line-height: 16px;
                color: #fff;
                border-radius: 50%;
            }
            .info-icon {
                display: inline-block;
                background-color: #ccc;
                width: 16px;
                height: 16px;
                line-height: 16px;
                color: #fff;
                border-radius: 50%;
                margin-right: 5px;
                cursor: pointer;
            }
            .help-icon {
                cursor: pointer;
                margin-left: 8px;
                color: #979BA5;
            }
            .auth-help-icon {
                position: relative;
                top: 2px;
                cursor: pointer;
                margin-left: 8px;
                color: #979BA5;
            }
            .auth {
                display: inline-block;
                height: 32px;
                line-height: 32px;
                padding: 0 16px;
                background: #F5F7FA;
                border-radius: 16px;
            }
            .icon-success {
                position: relative;
                top: 2px;
                color: #3FC06D;
            }
            .reset-bth {
                &::before {
                    content: "";
                    position: relative;
                    display: inline-block;
                    top: 4px;
                    width: 1px;
                    height: 16px;
                    margin: 0 16px;
                    background: #DCDEE5;
                }
            }
            .common-btn {
                font-size: 12px;
                font-weight: 400;
                height: 0;
            }
            .merge-request {
                display: flex;
                align-items: center;
                font-size: 12px;
                color: #979BA5;
                white-space: nowrap;
            }
            .request-result {
                font-size: 12px;
                color: #63656E;
                margin-left: 18px;
            }
            .common-radio-group {
                margin-left: 30px;
            }
            ::v-deep .bk-form-radio {
                font-size: 12px !important;
            }
        }
        .history-item {
            display: inline-flex;
            line-height: 16px;
            width: 280px;
            margin-right: 150px;
            margin-bottom: 16px;
            .label {
                width: 120px;
                font-size: 12px;
                font-weight: 400;
                color: #979BA5;
            }
            .content {
                font-size: 12px;
                color: #63656E;
            }
        }
    }
    .oauth-confirm-dialog,
    .close-repo-confirm-dialog {
        text-align: center;
        .bk-dialog-body {
            display: flex;
            flex-direction: column;
            align-items: center;
            max-height: calc(50vh - 50px);
        }
        .toggle-pac-warning-icon {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            background-color: #FFE8C3;
            color: #FF9C01;
            width: 42px;
            height: 42px;
            font-size: 24px;
            border-radius: 50%;
            flex-shrink: 0;
        }
        .oauth-confirm-title {
            font-size: 20px;
            color: #313238;
            margin: 20px 0 8px;
        }
        .close-confirm-title {
            font-size: 20px;
            color: #313238;
            margin-bottom: 15px;
        }
        .ci-status-warpper {
            display: flex;
            align-items: center;
        }
        .success-icon {
            width: 35px;
            height: 35px;
            background: rgba(63, 192, 109, 0.1);
            border-radius: 50%;
            i {
                font-size: 35px;
                color: #3FC06D;
            }
        }
        .rotate {
            height: 8px !important;
            transform-origin: 50% -6px !important;
            width: 4px !important;
        }
        .operate-btn {
            text-align: left;
            margin-left: 20px;
            p {
                font-weight: 700;
                font-size: 14px;
                color: #63656E;
                margin-bottom: 5px;
            }
            a {
                font-size: 12px;
            }
            .jump-icon,
            .refresh-icon {
                position: relative;
                top: 2px;
            }
            .refreshing {
                transition: all .5s linear;
                transform: rotate(540deg);
            }
        }

        .split-line {
            display: inline-block;
            width: 1px;
            height: 16px;
            background: #DCDEE5;
            margin: 0 5px;
            position: relative;
            top: 3px;
        }
        
        .close-confirm-tips {
            text-align: left;
            color: #63656E;
            font-size: 12px;
            margin-bottom: 30px;
            padding: 10px 20px;
            background: #F5F7FA;
            span {
                color: #FF9C01;
            }
        }
        .oauth-confirm-tips {
            text-align: left;
            margin-top: 16px;
            margin-bottom: 30px;
            font-size: 14px;
            color: #979BA5;
        }
        .close-confirm-footer {
            margin-top: 24px;
        }
    }
    .failed-detail-dialog {
        .title-tips {
            font-size: 12px;
            margin-bottom: 10px;
        }
    }
</style>
