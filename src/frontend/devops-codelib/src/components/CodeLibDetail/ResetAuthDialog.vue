<template>
    <bk-dialog
        class="codelib-operate-dialog"
        v-model="isShow"
        :width="780"
        :padding="24"
        :quick-close="false"
        render-directive="if"
        :show-footer="!isOAUTH"
        @value-change="handleClose"
    >
        <h3
            slot="header"
            class="bk-dialog-title"
        >
            {{ $t('codelib.resetAuth') }}
        </h3>
        <bk-alert
            class="reset-auth-tips"
            type="error"
            closable
            :title="$t('codelib.重置授权会使正在运行的流水线中已获取到的授权信息失效，从而导致执行失败，请谨慎操作。')"
        />
        <bk-form
            ref="form"
            :model="newRepoInfo"
            :label-width="165"
            :rules="rules"
        >
            <template v-if="!isScmConfig">
                <!-- Github 重置授权 -->
                <bk-form-item
                    v-if="isGithub"
                    :label="$t('codelib.authType')"
                    :required="true"
                    property="authType"
                >
                    <bk-radio-group
                        v-model="newRepoInfo.authType"
                    >
                        <bk-radio
                            class="mr20"
                            value="OAUTH"
                        >
                            OAUTH
                        </bk-radio>
                    </bk-radio-group>
    
                    <div
                        class="codelib-oauth"
                        v-if="isOAUTH"
                    >
                        <bk-button
                            theme="primary"
                            :loading="isSaveLoading"
                            @click="openValidate"
                        >
                            {{ $t('codelib.oauthCert') }}
                        </bk-button>
                        
                        <div class="oauth-tips">
                            <p>{{ $t('codelib.如需重置，请先点击按钮授权。') }}</p>
                            <p>{{ $t('codelib.此授权用于平台和 Github 进行交互，用于如下场景：') }}</p>
                            <p>1.{{ $t('codelib.回写 Commit statuses 到 Github') }}</p>
                            <p>2.{{ $t('codelib.流水线中 Checkout 代码') }}</p>
                            <p>{{ $t('codelib.需拥有代码库 Push 权限') }}</p>
                        </div>
                    </div>
                </bk-form-item>
    
                <!-- Git、 TGit 重置授权 -->
                <bk-form-item
                    v-if="isGit || isTGit"
                    :label="$t('codelib.authType')"
                    :required="true"
                    property="authType"
                >
                    <bk-radio-group
                        v-model="newRepoInfo.authType"
                    >
                        <template v-if="isGit">
                            <bk-radio
                                class="mr20"
                                value="OAUTH"
                            >
                                OAUTH
                            </bk-radio>
                            <bk-radio
                                class="mr20"
                                value="SSH"
                                :disabled="repoInfo.enablePac"
                            >
                                SSH
                            </bk-radio>
                            <bk-radio
                                value="HTTP"
                                :disabled="repoInfo.enablePac"
                            >
                                {{ $t('codelib.用户名+密码') }}
                            </bk-radio>
                        </template>
                        <template v-else>
                            <bk-radio
                                value="HTTPS"
                            >
                                {{ $t('codelib.用户名+密码') }}
                            </bk-radio>
                        </template>
                    </bk-radio-group>
    
                    <div
                        class="codelib-oauth"
                        v-if="isOAUTH"
                    >
                        <bk-button
                            theme="primary"
                            :loading="isSaveLoading"
                            @click="openValidate"
                        >
                            {{ $t('codelib.oauthCert') }}
                        </bk-button>
                        <div
                            class="oauth-tips"
                        >
                            <p>{{ $t('codelib.如需重置，请先点击按钮授权。') }}</p>
                            <p>{{ $t('codelib.此授权用于平台和代码库进行交互，涉及如下功能：') }}</p>
                            <p>1.{{ $t('codelib.注册 Webhook 到代码库，用于事件触发场景') }}</p>
                            <p>2.{{ $t('codelib.回写提交检测状态到代码库，用于代码库支持 checker 拦截合并请求场景') }}</p>
                            <p>3.{{ $t('codelib.流水线中 Checkout 代码') }}</p>
                            <p>{{ $t('codelib.需拥有代码库注册 Webhook 权限') }}</p>
                        </div>
                    </div>
                </bk-form-item>
    
                <!-- Gitlab 重置授权 -->
                <bk-form-item
                    v-if="isGitLab"
                    :label="$t('codelib.authType')"
                    :required="true"
                    property="authType"
                >
                    <bk-radio-group
                        v-model="newRepoInfo.authType"
                    >
                        <bk-radio
                            class="mr20"
                            value="SSH"
                        >
                            SSH
                        </bk-radio>
                        <bk-radio
                            value="HTTP"
                        >
                            {{ $t('codelib.访问令牌') }}
                        </bk-radio>
                    </bk-radio-group>
                </bk-form-item>
                <!-- SVN 重置授权 -->
                <bk-form-item
                    v-if="isSvn"
                    :label="$t('codelib.authType')"
                    :required="true"
                    property="svnType"
                >
                    <bk-radio-group
                        v-model="newRepoInfo.svnType"
                    >
                        <bk-radio
                            class="mr20"
                            value="ssh"
                        >
                            SSH
                        </bk-radio>
                        <bk-radio
                            value="http"
                        >
                            {{ $t('codelib.用户名+密码') }}
                        </bk-radio>
                    </bk-radio-group>
                </bk-form-item>
            </template>
            <template v-else>
                <bk-form-item
                    :label="$t('codelib.authType')"
                    :required="true"
                    property="credentialType"
                >
                    <bk-radio-group
                        v-model="newRepoInfo.credentialType"
                    >
                        <bk-radio
                            v-for="auth in providerConfig.credentialTypeList"
                            :key="auth.credentialType"
                            class="mr20"
                            :value="auth.credentialType"
                            :disabled="repoInfo.enablePac"
                        >
                            {{ auth.name }}
                        </bk-radio>
                    </bk-radio-group>
                    <div
                        class="codelib-oauth"
                        v-if="isOAUTH"
                    >
                        <bk-button
                            theme="primary"
                            :loading="isSaveLoading"
                            @click="openValidate"
                        >
                            {{ $t('codelib.oauthCert') }}
                        </bk-button>
                        <div
                            class="oauth-tips"
                        >
                            <p>{{ $t('codelib.如需重置，请先点击按钮授权。') }}</p>
                            <p>{{ $t('codelib.此授权用于平台和代码库进行交互，涉及如下功能：') }}</p>
                            <p>1.{{ $t('codelib.注册 Webhook 到代码库，用于事件触发场景') }}</p>
                            <p>2.{{ $t('codelib.回写提交检测状态到代码库，用于代码库支持 checker 拦截合并请求场景') }}</p>
                            <p>3.{{ $t('codelib.流水线中 Checkout 代码') }}</p>
                            <p>{{ $t('codelib.需拥有代码库注册 Webhook 权限') }}</p>
                        </div>
                    </div>
                </bk-form-item>
            </template>
            <template
                v-if="!isOAUTH"
            >
                <bk-form-item
                    :label="addressTitle"
                    :required="true"
                    property="url"
                    error-display-type="normal"
                >
                    <bk-input
                        v-model.trim="newRepoInfo.url"
                        :disabled="!isSvn"
                    >
                    </bk-input>
                </bk-form-item>
                <bk-form-item
                    v-if="!isOAUTH"
                    :label="$t('codelib.codelibCredential')"
                    :required="true"
                    property="credentialId"
                    error-display-type="normal"
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
                            :name="option.credentialId"
                        >
                            <span
                                class="name"
                                :title="option.credentialId"
                            >
                                {{ option.credentialId }}
                            </span>
                            <i
                                class="devops-icon icon-edit2 cre-icon"
                                @click.stop="goToEditCre(index)"
                            >
                            </i>
                        </bk-option>
                    </bk-select>
                    <span
                        class="add-cred-btn"
                        @click="addCredential"
                    >
                        {{ $t('codelib.new') }}
                    </span>
                </bk-form-item>
            </template>
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
    import { cloneDeep } from 'lodash-es'
    import {
        mapActions,
        mapState
    } from 'vuex'
    import {
        getCodelibConfig,
        CODE_REPOSITORY_CACHE,
        CODE_REPOSITORY_SEARCH_VAL
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
            userId: {
                type: String,
                default: ''
            },
            isP4: Boolean,
            isSvn: Boolean,
            isGitLab: Boolean,
            isTGit: Boolean,
            isGit: Boolean,
            isGithub: Boolean,
            fetchRepoDetail: Function,
            isScmGit: Boolean,
            isScmSvn: Boolean
        },
        data () {
            return {
                isShow: false,
                isSaveLoading: false,
                isLoadingTickets: false,
                newRepoInfo: {},
                cacheRepoInfo: {},
                rules: {
                    credentialId: [
                        {
                            required: true,
                            message: this.$t('codelib.请选择凭证'),
                            trigger: 'blur'
                        }
                    ]
                }
            }
        },
        computed: {
            ...mapState('codelib', [
                'tickets',
                'codelibTypes'
            ]),
            isOAUTH () {
                const prop = (this.isScmGit || this.isScmSvn) ? 'credentialType' : 'authType'
                return this.newRepoInfo[prop] === 'OAUTH'
            },

            projectId () {
                return this.$route.params.projectId
            },

            isScmConfig () {
                return this.newRepoInfo.scmType?.startsWith('SCM_')
            },

            codelibTypeName () {
                return this.newRepoInfo && this.newRepoInfo?.['@type']
                    ? this.newRepoInfo?.['@type']
                    : ''
            },

            credentialTypes () {
                return this.codelibConfig.credentialTypes
            },
            codelibConfig () {
                return (
                    getCodelibConfig(
                        this.codelibTypeName,
                        this.newRepoInfo.svnType,
                        this.newRepoInfo.authType
                    ) || {}
                )
            },
            credentialList () {
                return this.tickets || []
            },
            addressTitle () {
                const type = this.isSvn ? this.newRepoInfo.svnType : this.newRepoInfo.authType
                const titleMap = {
                    http: this.$t('codelib.HTTP/HTTPS地址'),
                    HTTP: this.$t('codelib.HTTP/HTTPS地址'),
                    HTTPS: this.$t('codelib.HTTP/HTTPS地址'),
                    SSH: this.$t('codelib.SSH地址'),
                    ssh: this.$t('codelib.SSH地址')
                }
                return titleMap[type] || this.$t('codelib.address')
            },

            providerConfig () {
                return this.codelibTypes.find(i => i.scmCode === this.newRepoInfo.scmCode) || {}
            },
            username () {
                return this.$store.state?.user?.username || ''
            }
        },
        watch: {
            isShow (val) {
                if (val) {
                    if (!this.isOAUTH) this.getTickets()
                }
            },
            repoInfo: {
                handler (val) {
                    this.newRepoInfo = {
                        ...this.newRepoInfo,
                        ...val
                    }
                    this.cacheRepoInfo = cloneDeep(this.newRepoInfo)
                },
                deep: true,
                immediate: true
            },
            curRepo: {
                handler (val) {
                    this.newRepoInfo = {
                        ...this.newRepoInfo,
                        ...val
                    }
                    this.cacheRepoInfo = cloneDeep(this.newRepoInfo)
                },
                deep: true,
                immediate: true
            },
            'newRepoInfo.authType': {
                handler (val) {
                    if (val === this.cacheRepoInfo.authType) {
                        this.newRepoInfo.url = this.cacheRepoInfo.url
                        this.newRepoInfo.credentialId = this.cacheRepoInfo.credentialId
                        return
                    }
                    if (this.isGitLab) {
                        if (val === 'HTTP' && this.cacheRepoInfo.authType === 'SSH') {
                            const { url } = this.newRepoInfo
                            this.newRepoInfo.url = `https://${url.split('@')[1].replace(':', '/')}`
                            this.newRepoInfo.credentialId = ''
                        }

                        if (val === 'SSH' && this.cacheRepoInfo.authType === 'HTTP') {
                            const { url } = this.newRepoInfo
                            this.newRepoInfo.url = `git@${url.split('://')[1].replace('.com/', '.com:')}`
                            this.newRepoInfo.credentialId = ''
                        }
                    }

                    if (this.isGit) {
                        if (['OAUTH', 'HTTP'].includes(val) && this.cacheRepoInfo.authType === 'SSH') {
                            const { url } = this.newRepoInfo
                            this.newRepoInfo.url = url.replace('com:', 'com/').replace('git@', 'https://')
                            this.newRepoInfo.credentialId = ''
                        }
                        
                        if (val === 'SSH' && ['OAUTH', 'HTTP'].includes(this.cacheRepoInfo.authType)) {
                            const { url } = this.newRepoInfo
                            if (url.startsWith('https://')) {
                                this.newRepoInfo.url = url.replace('com/', 'com:').replace('https://', 'git@')
                            } else {
                                this.newRepoInfo.url = url.replace('com/', 'com:').replace('http://', 'git@')
                            }
                            this.newRepoInfo.credentialId = ''
                        }

                        if (val === 'HTTP' && this.cacheRepoInfo.authType === 'OAUTH') {
                            this.newRepoInfo.url = this.cacheRepoInfo.url
                            this.newRepoInfo.credentialId = ''
                        }

                        // 老数据存在http开头的仓库处理
                        if (val === 'OAUTH' && this.cacheRepoInfo.authType === 'HTTP') {
                            const { url } = this.newRepoInfo
                            if (url.startsWith('http://')) {
                                this.newRepoInfo.url = url.replace.replace('http://', 'https://')
                            }
                        }
                    }
                },
                deep: true
            },

            'newRepoInfo.credentialType': {
                handler (val, oldVal) {
                    const authType = this.providerConfig.credentialTypeList?.find(i => i.credentialType === val)?.authType
                    this.newRepoInfo.authType = authType
                    if (this.isScmGit) {
                        if ((val.includes('OAUTH') || val.includes('USERNAME_PASSWORD')) && oldVal?.includes('SSH')) {
                            const { url } = this.newRepoInfo
                            this.newRepoInfo.url = url.replace('com:', 'com/').replace('git@', 'https://')
                        }
                        if (val.includes('SSH') && (oldVal?.includes('OAUTH') || oldVal?.includes('USERNAME_PASSWORD'))) {
                            const { url } = this.newRepoInfo
                            if (url.startsWith('https://')) {
                                this.newRepoInfo.url = url.replace('com/', 'com:').replace('https://', 'git@')
                            } else {
                                this.newRepoInfo.url = url.replace('com/', 'com:').replace('http://', 'git@')
                            }
                        }
                    }
                    if (oldVal) {
                        this.newRepoInfo.credentialId = ''
                    }
                },
                deep: true
            },

            'newRepoInfo.svnType': {
                handler (val) {
                    if (this.isSvn) {
                        if (val === this.cacheRepoInfo.svnType) {
                            this.newRepoInfo.url = this.cacheRepoInfo.url
                            this.newRepoInfo.credentialId = this.cacheRepoInfo.credentialId
                            return
                        }
                        const { url } = this.newRepoInfo
                        const urlArr = url.split('://')
                        this.newRepoInfo.url = val === 'ssh'
                            ? `svn+ssh://${urlArr[1]}`
                            : `https://${urlArr[1]}`
                        this.newRepoInfo.credentialId = ''
                    }
                },
                deep: true
            },

            userId (val) {
                if (val) {
                    this.newRepoInfo = {
                        ...this.newRepoInfo,
                        userName: val,
                        authType: 'OAUTH',
                        credentialType: 'OAUTH',
                        authIdentity: ''
                    }
                    this.$nextTick(() => {
                        this.handleUpdateRepo()
                    })
                }
            }
        },
        methods: {
            ...mapActions('codelib', [
                'editRepo',
                'requestTickets',
                'refreshGitOauth',
                'refreshGithubOauth',
                'refreshScmOauth'
            ]),
            openValidate () {
                const { scmType, id, page, limit, searchName } = this.$route.query
                const { projectId } = this.$route.params
                localStorage.setItem(CODE_REPOSITORY_CACHE, JSON.stringify({
                    scmType,
                    id,
                    page,
                    limit,
                    projectId
                }))
                localStorage.setItem(CODE_REPOSITORY_SEARCH_VAL, JSON.stringify(searchName || ''))
                if (this.isGit || this.isTGit) {
                    this.refreshGitOauth({
                        type: this.isGit ? 'git' : 'tgit',
                        resetType: this.isGit ? 'resetGitOauth' : 'resetTGitOauth',
                        redirectUrl: window.location.href,
                        refreshToken: true
                    }).then(res => {
                        window.location.href = res.url
                        this.$emit('updateList')
                    }).finally(() => {
                        const newQuery = { ...this.$route.query }
                        delete newQuery.resetType
                        delete newQuery.userId
                        this.$router.push({
                            query: {
                                ...newQuery
                            }
                        })
                    })
                } else if (this.isGithub) {
                    this.refreshGithubOauth({
                        projectId: this.projectId,
                        resetType: 'resetGithubOauth',
                        redirectUrl: window.location.href,
                        refreshToken: true
                    }).then(res => {
                        window.location.href = res.url
                    }).finally(() => {
                        const newQuery = { ...this.$route.query }
                        delete newQuery.resetType
                        delete newQuery.userId
                        this.$router.push({
                            query: {
                                ...newQuery
                            }
                        })
                    })
                } else if (this.isScmConfig) {
                    const redirectUrl = `${window.location.href}&resetType=resetScmOauth&userId=${this.username}`
                    this.refreshScmOauth({
                        redirectUrl: encodeURIComponent(redirectUrl),
                        scmCode: this.newRepoInfo.scmCode
                    }).then(res => {
                        window.location.href = res.url
                    })
                }
            },

            async getTickets () {
                const { projectId, credentialTypes } = this
                this.isLoadingTickets = true
                await this.requestTickets({
                    projectId,
                    credentialTypes: this.isScmConfig ? this.newRepoInfo.credentialType : credentialTypes
                })
                this.isLoadingTickets = false
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
                const credentialType = this.isScmConfig ? this.newRepoInfo.credentialType : codelibConfig.addType
                window.open(
                    `/console/ticket/${projectId}/createCredential/${credentialType}/true`,
                    '_blank'
                )
            },

            handleUpdateRepo () {
                this.isSaveLoading = true
                this.editRepo({
                    projectId: this.projectId,
                    repositoryHashId: this.newRepoInfo.repoHashId,
                    params: this.newRepoInfo
                }).then(() => {
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('codelib.重置成功')
                    })
                    this.$emit('updateList')
                    this.isShow = false
                }).catch((e) => {
                    this.$bkMessage({
                        theme: 'error',
                        message: e.message || e
                    })
                }).finally(() => {
                    this.isSaveLoading = false
                    const newQuery = { ...this.$route.query }
                    delete newQuery.resetType
                    delete newQuery.userId
                    this.fetchRepoDetail(this.newRepoInfo.repositoryHashId, false)
                    this.$router.push({
                        query: {
                            ...newQuery
                        }
                    })
                })
            },
            handleConfirm () {
                if (this.isOAUTH) return
                this.$refs.form.validate().then(async () => {
                    await this.handleUpdateRepo()
                })
            },
            handleClose (val) {
                if (!val) {
                    this.newRepoInfo = {
                        ...this.newRepoInfo,
                        ...this.cacheRepoInfo
                    }
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
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
    .bk-dialog-title {
        font-size: 20px;
    }
    .reset-auth-tips {
        margin-left: 70px;
        width: 80%;
    }
</style>
