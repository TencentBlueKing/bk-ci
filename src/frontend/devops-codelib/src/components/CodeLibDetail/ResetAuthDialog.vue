<template>
    <bk-dialog
        class="codelib-operate-dialog"
        v-model="isShow"
        :width="780"
        :padding="24"
        :quick-close="false"
        render-directive="if"
        :show-footer="!isOAUTH"
    >
        <h3
            slot="header"
            class="bk-dialog-title"
        >
            {{ $t('codelib.resetAuth') }}
        </h3>
        <bk-form
            :label-width="120"
        >
            <bk-form-item
                :label="$t('codelib.authType')"
                :required="true"
                property="authType"
            >
                <bk-radio-group
                    v-model="newRepoInfo.authType"
                >
                    <bk-radio
                        v-if="isOAUTH"
                        class="mr20"
                        value="OAUTH"
                    >
                        OAUTH
                    </bk-radio>
                    <bk-radio
                        v-else-if="isSSH"
                        class="mr20"
                        value="SSH"
                    >
                        SSH
                    </bk-radio>
                    <bk-radio
                        v-else
                        value="HTTP"
                    >
                        {{ $t('codelib.用户名密码+个人token') }}
                    </bk-radio>
                </bk-radio-group>

                <div class="codelib-oauth" v-if="isOAUTH">
                    <bk-button
                        theme="primary"
                        @click="openValidate"
                    >
                        {{ $t('codelib.oauthCert') }}
                    </bk-button>
                    <div
                        v-if="isGit || isTGit"
                        class="oauth-tips"
                    >
                        <p>{{ $t('codelib.如需重置，请先点击按钮授权。') }}</p>
                        <p>{{ $t('codelib.此授权用于平台和工蜂进行交互，用于如下场景：') }}</p>
                        <p>1.{{ $t('codelib.注册 Webhook 到工蜂') }}</p>
                        <p>2.{{ $t('codelib.回写提交检测状态到工蜂') }}</p>
                        <p>3.{{ $t('codelib.流水线中 Checkout 代码') }}</p>
                        <p>{{ $t('codelib.需拥有代码库 Devloper 及以上权限，建议使用公共账号授权') }}</p>
                    </div>
                    <div
                        v-else-if="isGithub"
                        class="oauth-tips"
                    >
                        <p>{{ $t('codelib.如需重置，请先点击按钮授权。') }}</p>
                        <p>{{ $t('codelib.此授权用于平台和 Github 进行交互，用于如下场景：') }}</p>
                        <p>1.{{ $t('codelib.回写 Commit statuses 到 Github') }}</p>
                        <p>2.{{ $t('codelib.流水线中 Checkout 代码') }}</p>
                        <p>{{ $t('codelib.需拥有代码库 Push 权限') }}</p>
                    </div>
                </div>
            </bk-form-item>
            <bk-form-item
                v-if="!isOAUTH"
                :label="$t('codelib.codelibCredential')"
                :required="true"
                property="credentialId"
            >
                <bk-select
                    v-model="newRepoInfo.credentialId"
                    :loading="isLoadingTickets"
                    searchable
                    :clearable="false"
                    name="credentialId"
                    class="codelib-credential-selector"
                    :placeholder="$t('codelib.credentialPlaceholder')"
                    @toggle="getTickets"
                >
                    <bk-option
                        v-for="(option, index) in tickets"
                        :key="option.credentialId"
                        :id="option.credentialId"
                        :name="option.credentialId">
                        <span>
                            {{option.credentialId}}
                        </span>
                        <i
                            class="devops-icon icon-edit2 cre-icon"
                            @click.stop="goToEditCre(index)"
                        >
                        </i>
                    </bk-option>
                </bk-select>
                <span
                    class="text-link"
                    @click="addCredential"
                >
                    {{ $t('codelib.new') }}
                </span>
            </bk-form-item>
        </bk-form>
        <footer slot="footer">
            <bk-button
                theme="primary"
                :loading="isSaveLoading"
                @click="handleConfirm"
            >
                {{ $t('codelib.confirm') }}
            </bk-button>
            <bk-button
                :loading="isSaveLoading"
                @click="isShow = false"
            >
                {{ $t('codelib.cancel') }}
            </bk-button>
        </footer>
    </bk-dialog>
</template>

<script>
    import {
        mapState,
        mapActions
    } from 'vuex'
    import {
        getCodelibConfig
    } from '../../config'
    export default {
        props: {
            curRepo: {
                type: Object,
                default: () => {}
            },
            repoInfo: {
                type: Object,
                default: () => {}
            },
            type: {
                type: String,
                default: ''
            },
            isTGit: Boolean,
            isGit: Boolean,
            isGithub: Boolean,
            fetchRepoDetail: Function
        },
        data () {
            return {
                isShow: false,
                isSaveLoading: false,
                isLoadingTickets: false,
                newRepoInfo: {}
            }
        },
        computed: {
            ...mapState('codelib', [
                'tickets'
            ]),
            isOAUTH () {
                return this.newRepoInfo.authType === 'OAUTH'
            },
            isSSH () {
                return this.newRepoInfo.authType === 'SSH'
            },
            projectId () {
                return this.$route.params.projectId
            },

            codelibTypeName () {
                return this.repoInfo && this.repoInfo['@type']
                    ? this.repoInfo['@type']
                    : ''
            },

            credentialTypes () {
                return this.codelibConfig.credentialTypes
            },
            codelibConfig () {
                return (
                    getCodelibConfig(
                        this.codelibTypeName,
                        this.curRepo.svnType,
                        this.curRepo.authType
                    ) || {}
                )
            }
        },
        watch: {
            repoInfo (val) {
                this.newRepoInfo = {
                    ...this.newRepoInfo,
                    ...val
                }
            },
            curRepo (val) {
                this.newRepoInfo = {
                    ...this.newRepoInfo,
                    ...val
                }
            },
            credentialTypes () {
                if (!this.isOAUTH) this.getTickets(true)
            }
        },
        methods: {
            ...mapActions('codelib', [
                'editRepo',
                'requestTickets',
                'refreshGitOauth',
                'refreshGithubOauth'
            ]),
            openValidate () {
                if (this.isGit || this.isTGit) {
                    this.refreshGitOauth({
                        type: this.isGit ? 'git' : 'tgit',
                        resetType: this.isGit ? 'resetGitOauth' : 'resetTGitOauth',
                        redirectUrl: window.location.href
                    }).then(res => {
                        if (res.status === 200) {
                            this.newRepoInfo = {
                                ...this.newRepoInfo,
                                userName: this.$store.state.user.username
                            }
                            this.handleUpdateRepo()
                        } else {
                            window.location.href = res.url
                        }
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
                } else if (this.isGithub) {
                    this.refreshGithubOauth({
                        projectId: this.projectId,
                        resetType: 'resetGithubOauth',
                        redirectUrl: window.location.href
                    }).then(res => {
                        if (res.status === 200) {
                            this.newRepoInfo = {
                                ...this.newRepoInfo,
                                userName: this.$store.state.user.username
                            }
                            this.handleUpdateRepo()
                        } else {
                            window.location.href = res.url
                        }
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
                }
            },

            async getTickets (val) {
                if (val) {
                    const { projectId, credentialTypes } = this
                    this.isLoadingTickets = true
                    await this.requestTickets({
                        projectId,
                        credentialTypes
                    })
                    this.isLoadingTickets = false
                }
            },

            goToEditCre (index) {
                const { projectId, credentialList } = this
                const { credentialId } = credentialList[index]
                window.open(
                    `/console/ticket/${projectId}/editCredential/${credentialId}`,
                    '_blank'
                )
            },
            addCredential () {
                const { projectId, codelibConfig } = this
                window.open(
                    `/console/ticket/${projectId}/createCredential/${codelibConfig.addType}/true`,
                    '_blank'
                )
            },

            handleUpdateRepo () {
                this.isSaveLoading = true
                this.editRepo({
                    projectId: this.projectId,
                    repositoryHashId: this.newRepoInfo.repositoryHashId,
                    params: this.newRepoInfo
                }).then(() => {
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('codelib.重置成功')
                    })
                    this.fetchRepoDetail(this.newRepoInfo.repositoryHashId)
                }).catch((e) => {
                    this.$bkMessage({
                        theme: 'error',
                        message: e || e.message
                    })
                }).finally(() => {
                    this.isSaveLoading = false
                    this.isShow = false
                })
            },
            handleConfirm () {
                if (this.isOAUTH) return
                this.handleUpdateRepo()
            }
        }
    }
</script>

<style lang="scss">
    .codelib-oauth {
        margin: 20px 0;
        .refresh-oauth {
            color: #3A84FF;
            cursor: pointer;
        }
        .refresh-icon {
            margin-left: 20px;
        }
        .oauth-tips {
            margin-top: 16px;
            font-size: 12px;
            color: #979BA5;
        }
    }
</style>
