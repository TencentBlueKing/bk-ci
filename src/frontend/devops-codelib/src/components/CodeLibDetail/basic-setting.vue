<template>
    <section class="basic-setting">
        <!-- 授权 -->
        <div class="form-item">
            <div class="label">
                {{ $t('codelib.auth') }}
                <bk-popover placement="top">
                    <Icon name="help" size="14" class="auth-help-icon" />
                    <div slot="content">
                        <template v-if="isGit || isTGit">
                            <p>{{ $t('codelib.此授权用于平台和工蜂进行交互，用于如下场景：') }}</p>
                            <p>1.{{ $t('codelib.注册 Webhook 到工蜂') }}</p>
                            <p>2.{{ $t('codelib.回写提交检测状态到工蜂') }}</p>
                            <p>3.{{ $t('codelib.流水线中 Checkout 代码') }}</p>
                            <p>{{ $t('codelib.需拥有代码库 Devloper 及以上权限，建议使用公共账号授权') }}</p>
                        </template>
                        <template v-if="isGithub">
                            <p>{{ $t('codelib.此授权用于平台和 Github 进行交互，用于如下场景：') }}</p>
                            <p>1.{{ $t('codelib.回写 Commit statuses 到 Github') }}</p>
                            <p>2.{{ $t('codelib.流水线中 Checkout 代码') }}</p>
                            <p>{{ $t('codelib.需拥有代码库 Push 权限') }}</p>
                        </template>
                        <template v-if="isSvn">
                            <p>{{ $t('codelib.此授权用于平台和 SVN 代码库进行交互，用于如下场景：') }}</p>
                            <p>1.{{ $t('codelib.注册 Webhook 到代码库') }}</p>
                            <p>2.{{ $t('codelib.流水线中 Checkout 代码') }}</p>
                            <p>{{ $t('codelib.需拥有代码库 Write 权限') }}</p>
                        </template>
                        <template v-if="isP4">
                            <p>{{ $t('codelib.此授权用于平台和 Github 进行交互，用于如下场景：') }}</p>
                            <p>1.{{ $t('codelib.流水线中 Checkout 代码') }}</p>
                            <p>{{ $t('codelib.需拥有代码库 Read 权限') }}</p>
                        </template>
                    </div>
                </bk-popover>
            </div>
            <div class="content">
                <div class="auth">
                    <Icon name="check-circle" size="14" class="icon-success" />
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
        <!-- <div
            class="form-item"
            v-if="isGit"
        >
            <div class="label">
                {{ $t('codelib.PACmode') }}
            </div>
            <p class="pac-tips">{{ $t('codelib.pacTips') }}</p>
            <div class="content">
                <div class="pac-mode">
                    <div
                        class="switcher-item"
                        :class="{ 'disabled-pac': !repoInfo.enablePac && pacProjectName }"
                        @click="handleTogglePacStatus">
                    </div>
                   
                    <bk-switcher
                        v-model="repoInfo.enablePac"
                        theme="primary"
                        :disabled="!repoInfo.enablePac && pacProjectName"
                    >
                    </bk-switcher>
                    <div class="pac-enable">
                        {{ repoInfo.enablePac ? $t('codelib.已开启 PAC 模式') : $t('codelib.未开启 PAC 模式') }}
                    </div>
                    <span v-if="!repoInfo.enablePac && pacProjectName">
                        {{ $t('codelib.当前代码库已在【】项目中开启 PAC 模式', [pacProjectName]) }}
                    </span>
                </div>
            </div>
        </div> -->
        <!-- 通用设置 -->
        <!-- <div
            class="form-item"
            v-if="isGit"
        >
            <div class="label">
                {{ $t('codelib.common') }}
                <span
                    v-if="!isEditing"
                    @click="handleEditCommon">
                    <Icon name="edit-line" size="14" class="edit-icon" />
                </span>
                <span v-else>
                    <bk-button
                        class="common-btn ml20 mr5"
                        text
                        @click="handleSaveCommon"
                    >
                        {{ $t('codelib.save') }}
                    </bk-button>
                    <bk-button
                        class="common-btn"
                        text
                        @click="isEditing = false"
                    >
                        {{ $t('codelib.cancel') }}
                    </bk-button>
                </span>
            </div>
            <div class="content">
                <div class="merge-request">
                    {{ $t('codelib.blockingMergeRequest') }}
                    <Icon name="help" size="14" class="help-icon" />
                    <p v-if="!isEditing" class="request-result">{{ repoInfo.settings.enableMrBlock ? $t('codelib.yes') : $t('codelib.no') }}</p>
                    <bk-radio-group
                        class="common-radio-group"
                        v-else
                        v-model="repoInfo.settings.enableMrBlock">
                        <bk-radio class="mr15" :value="true">
                            {{ $t('codelib.yes') }}
                        </bk-radio>
                        <bk-radio :value="false">
                            {{ $t('codelib.no') }}
                        </bk-radio>
                    </bk-radio-group>
                </div>
            </div>
        </div> -->
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
            @value-change="handlceToggleShowClosePac"
        >
            <span class="toggle-pac-warning-icon">
                <i class="devops-icon icon-exclamation" />
            </span>
            <span class="close-confirm-title">
                {{ $t('codelib.关闭 PAC 模式失败') }}
            </span>
            <span class="close-confirm-tips">
                <p>
                    {{ $t('codelib.检测到默认分支仍存在ci 文件目录，关闭 PAC 模式后该目录下的文件修改将') }}
                    <span>{{ $t('codelib.不再同步到蓝盾流水线') }}</span>
                    {{ $t('codelib.。') }}
                </p>
                <p>
                    {{ $t('codelib.请先将目录') }}
                    <span>{{ $t('codelib.改名或删除') }}</span>
                    {{ $t('codelib.后重试，避免项目其他成员进行无效的YAML文件修改') }}
                    {{ $t('codelib.。') }}
                </p>
            </span>
            <span class="close-confirm-footer">
                <bk-checkbox
                    v-model="isDeleted"
                >
                    {{ $t('codelib.ci目录已改名或删除') }}
                </bk-checkbox>
                <bk-button
                    class="ml10"
                    theme="primary"
                    :disabled="!isDeleted"
                    @click="handleClosePac"
                >
                    {{ $t('codelib.继续关闭') }}
                </bk-button>
            </span>
        </bk-dialog>

        <bk-dialog
            ext-cls="oauth-confirm-dialog"
            :width="500"
            :value="showOauthDialog"
            :show-footer="false"
            @value-change="handlceToggleShowOauthDialog"
        >
            <span class="toggle-pac-warning-icon">
                <i class="devops-icon icon-exclamation" />
            </span>
            <span class="oauth-confirm-title">
                {{ $t('codelib.PAC 模式需使用 OAUTH 授权') }}
            </span>
            <span v-if="isGit" class="oauth-confirm-tips">
                <p>{{ $t('codelib.尚未授权，请先点击按钮授权。') }}</p>
                <p>{{ $t('codelib.此授权用于平台和工蜂进行交互，用于如下场景：') }}</p>
                <p>1.{{ $t('codelib.注册 Webhook 到工蜂') }}</p>
                <p>2.{{ $t('codelib.回写提交检测状态到工蜂') }}</p>
                <p>3.{{ $t('codelib.流水线中 Checkout 代码') }}</p>
                <p>{{ $t('codelib.需拥有代码库 Devloper 及以上权限，建议使用公共账号授权') }}</p>
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
            :fetch-repo-detail="fetchRepoDetail"
            @updateList="updateList"
        />
    </section>
</template>
<script>
    import {
        isP4,
        isGit,
        isGithub,
        isGitLab,
        isSvn,
        isTGit
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
            fetchRepoDetail: {
                type: Function
            },
            refreshCodelibList: {
                type: Function
            }
        },
        data () {
            return {
                RESOURCE_ACTION,
                RESOURCE_TYPE,
                isEditing: false,
                isDeleted: false,
                hasCiFolder: true,
                showClosePac: false,
                showEnablePac: false,
                showOauthDialog: false,
                isP4: false,
                isGit: false,
                isSvn: false,
                isTGit: false,
                isGithub: false,
                isGitLab: false,
                pacProjectName: '',
                codelibTypeConstants: '',
                userId: ''
            }
        },
        computed: {
            ...mapState('codelib', [
                'gitOAuth',
                'githubOAuth',
                'tgitOAuth'
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
            hasPower () {
                return (
                    (this.isTGit
                        ? this.tgitOAuth.status
                        : this.isGit
                            ? this.gitOAuth.status
                            : this.githubOAuth.status) !== 403
                )
            }
        },
        watch: {
            type: {
                handler (val) {
                    this.isP4 = isP4(val)
                    this.isGit = isGit(val)
                    this.isSvn = isSvn(val)
                    this.isTGit = isTGit(val)
                    this.isGithub = isGithub(val)
                    this.isGitLab = isGitLab(val)
                    this.codelibTypeConstants = val.toLowerCase()
                        .replace(/^\S*?([github|git|tgit])/i, '$1')
                },
                immediate: true
            },
            'repoInfo.url': {
                handler (val) {
                    setTimeout(async () => {
                        // await this.handleCheckPacProject(val)
                        const { resetType, userId } = this.$route.query
                        if (['checkGitOauth', 'checkTGitOauth', 'checkGithubOauth'].includes(resetType)) {
                            await this.handleTogglePacStatus()
                        } else if (['resetGitOauth', 'resetTGitOauth', 'resetGithubOauth'].includes(resetType)) {
                            this.userId = userId
                            // await this.handleResetAuth()
                        }
                    }, 200)
                },
                deep: true
            },
            repoId () {
                this.pacProjectName = ''
            },
            codelibTypeConstants (val) {
                // 校验是否已经授权了OAUTh
                // switch (val) {
                //     case 'git':
                //         this.refreshGitOauth({
                //             type: 'git',
                //             resetType: 'checkGitOauth',
                //             redirectUrl: window.location.href
                //         })
                //         break
                //     case 'github':
                //         this.refreshGithubOauth({
                //             projectId: this.projectId,
                //             resetType: 'checkGithubOauth',
                //             redirectUrl: window.location.href
                //         })
                //         break
                //     case 'tgit':
                //         this.refreshGitOauth({
                //             type: 'tgit',
                //             resetType: 'checkTGitOauth',
                //             redirectUrl: window.location.href
                //         })
                //         break
                // }
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
                'checkPacProject'
            ]),
            prettyDateTimeFormat,

            /**
             * 开启通用设置编辑状态
             */
            handleEditCommon () {
                this.isEditing = true
            },

            /**
             * 校验仓库是否已经在其他项目开启了PAC
             */
            handleCheckPacProject (repoUrl) {
                if (this.isGit && repoUrl) {
                    this.checkPacProject({
                        repoUrl,
                        repositoryType: this.repositoryType
                    }).then((res) => {
                        this.pacProjectName = res
                    })
                }
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
        
            /**
             * 开启/关闭PAC模式
             * 关闭PAC需校验仓库状态 是否存在.ci文件夹
             *  true -> 存在.ci文件夹
             *  false -> 不存在.ci文件夹
             */
            async handleTogglePacStatus () {
                if (!this.repoInfo.enablePac && this.pacProjectName) return
                if (this.repoInfo.enablePac) {
                    this.pacProjectName = ''
                    await this.checkHasCiFolder({
                        projectId: this.projectId,
                        repositoryHashId: this.repoInfo.repoHashId
                    }).then(res => {
                        this.hasCiFolder = !res
                    })
                    if (this.hasCiFolder) {
                        this.showClosePac = true
                    } else {
                        this.$bkInfo({
                            title: this.$t('codelib.确定关闭 PAC 模式？'),
                            confirmFn: this.handleClosePac
                        })
                    }
                } else {
                    if (!this.hasPower) {
                        this.showOauthDialog = true
                        return
                    }
                    if (this.isOAUTH) {
                        this.$bkInfo({
                            title: this.$t('codelib.确定开启 PAC 模式？'),
                            confirmFn: this.handleEnablePac
                        })
                    } else {
                        this.$bkInfo({
                            type: 'warning',
                            title: this.$t('codelib.PAC 模式需使用 OAUTH 授权'),
                            subTitle: this.$t('codelib.确定重置授权为 OAUTH，同时开启 PAC 模式吗？'),
                            confirmFn: async () => {
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
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('codelib.开启成功')
                    })
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
            handleClosePac () {
                this.closePac({
                    projectId: this.projectId,
                    repositoryHashId: this.repoInfo.repoHashId
                }).then(async () => {
                    this.$bkMessage({
                        message: this.$t('codelib.关闭成功'),
                        theme: 'success'
                    })
                    this.showClosePac = false
                    await this.fetchRepoDetail(this.repoInfo.repoHashId)
                    await this.refreshCodelibList()
                })
            },
            /**
             * 开启PAC
             */
            handleEnablePac () {
                this.enablePac({
                    projectId: this.projectId,
                    repositoryHashId: this.repoInfo.repoHashId
                }).then(async () => {
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
                }).catch((e) => {
                    this.$bkMessage({
                        message: e.message || e,
                        theme: 'error'
                    })
                })
            },

            handlceToggleShowClosePac (val) {
                if (!val) {
                    this.showClosePac = val
                    this.isDeleted = false
                }
            },

            handlceToggleShowOauthDialog (val) {
                if (!val) {
                    this.showOauthDialog = val
                }
            },

            async openValidate () {
                window.location.href = this[`${this.codelibTypeConstants}OAuth`].url
            },

            updateList () {
                this.$emit('updateList')
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
        .oauth-confirm-title,
        .close-confirm-title {
            font-size: 20px;
            color: #313238;
            margin: 20px 0 8px;
        }
        .close-confirm-tips {
            text-align: left;
            color: #63656E;
            font-size: 14px;
            margin-bottom: 30px;
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
            position: relative;
            left: 50px;
        }
    }
</style>
