<template>
    <bk-dialog class="codelib-operate-dialog" v-model="isShow" :width="width" :padding="padding" :close-icon="false" :quick-close="false">
        <h3 slot="header" class="bk-dialog-title">{{title}}</h3>
        <form class="bk-form" v-bkloading="{ isLoading: saving || fetchingCodelibDetail }">
            <div class="bk-form-item is-required" v-if="isGit">
                <label class="bk-label">源代码方式:</label>
                <bk-radio-group v-model="codelib.authType" @change="authTypeChange(codelib)" class="bk-form-content form-radio">
                    <bk-radio value="OAUTH" v-if="isGit">OAUTH</bk-radio>
                    <bk-radio value="SSH">SSH</bk-radio>
                    <bk-radio value="HTTP">HTTP</bk-radio>
                </bk-radio-group>
            </div>
            <div class="bk-form-item" v-if="(isGit || isGithub) && codelib.authType === 'OAUTH'">
                <div class="bk-form-item is-required" v-if="hasPower">
                    <!-- 源代码地址 start -->
                    <div class="bk-form-item is-required">
                        <label class="bk-label">源代码地址:</label>
                        <div class="bk-form-content">
                            <bk-select v-model="codelibUrl"
                                searchable
                                :clearable="false"
                                v-validate="&quot;required&quot;"
                                name="name"
                                class="codelib-credential-selector"
                                placeholder="请选择代码库地址"
                            >
                                <bk-option v-for="(option, index) in oAuth.project"
                                    :key="index"
                                    :id="option.httpUrl"
                                    :name="option.httpUrl">
                                </bk-option>
                            </bk-select>
                            <span class="error-tips" v-if="urlErrMsg || errors.has(&quot;name&quot;)">
                                {{ urlErrMsg || errors.first("name") }}
                            </span>
                        </div>
                    </div>
                    <!-- 源代码地址 end -->

                    <!-- 别名 start -->
                    <div class="bk-form-item is-required">
                        <label class="bk-label">别名:</label>
                        <div class="bk-form-content" :class="{ &quot;is-danger&quot;: errors.has(&quot;aliasName&quot;) }">
                            <input type="text" class="bk-form-input" placeholder="请输入别名" name="codelibAliasName" v-model.trim="codelibAliasName" data-vv-validate-on="blur" v-validate="{ required: true, max: 60, aliasUnique: [projectId, repositoryHashId] }" :class="{ &quot;is-danger&quot;: errors.has(&quot;codelibAliasName&quot;) }">
                            <span class="error-tips" v-if="errors.has(&quot;codelibAliasName&quot;)">
                                {{ errors.first('codelibAliasName') }}
                            </span>
                        </div>
                    </div>
                    <!-- 别名 end -->
                </div>
                <div class="bk-form-item is-required" v-else>
                    <div class="bk-form-content" :class="{ &quot;is-danger&quot;: errors.has(&quot;powerValidate&quot;) }" :style="isGithub ? { textAlign: &quot;center&quot;, marginLeft: 0 } : {}">
                        <button class="bk-button bk-primary" type="button" @click="openValidate">OAUTH认证</button>
                        <input type="text" value="" name="powerValidate" v-validate="{ required: true }" style="width: 0; height: 0; border: none; z-index: -20; opacity: 0;">
                        <span class="error-tips" v-if="errors.has(&quot;powerValidate&quot;)">
                            {{ errors.first('powerValidate') }}
                        </span>
                    </div>
                </div>
            </div>
            <div class="bk-form-item" v-else>
                <div class="bk-form-item is-required" v-if="codelibConfig.label === 'SVN'">
                    <label class="bk-label">源代码拉取方式:</label>
                    <bk-radio-group v-model="codelib.svnType" @change="svnTypeChange(codelib)" class="bk-form-content form-radio">
                        <bk-radio value="ssh">SSH</bk-radio>
                        <bk-radio value="http">HTTP</bk-radio>
                    </bk-radio-group>
                </div>
                <!-- 源代码地址 start -->
                <div class="bk-form-item is-required">
                    <label class="bk-label">源代码地址:</label>
                    <div class="bk-form-content">
                        <input type="text" class="bk-form-input" :placeholder="urlPlaceholder" name="codelibUrl" v-model.trim="codelibUrl" v-validate="&quot;required&quot;" :class="{ &quot;is-danger&quot;: urlErrMsg || errors.has(&quot;codelibUrl&quot;) }">
                        <span class="error-tips" v-if="urlErrMsg || errors.has(&quot;codelibUrl&quot;)">
                            {{ urlErrMsg || errors.first("codelibUrl") }}
                        </span>
                    </div>
                </div>
                <!-- 源代码地址 end -->

                <!-- 别名 start -->
                <div class="bk-form-item is-required">
                    <label class="bk-label">别名:</label>
                    <div class="bk-form-content" :class="{ &quot;is-danger&quot;: errors.has(&quot;aliasName&quot;) }">
                        <input type="text" class="bk-form-input" placeholder="请输入别名" name="codelibAliasName" v-model.trim="codelibAliasName" data-vv-validate-on="blur" v-validate="{ required: true, max: 60, aliasUnique: [projectId, repositoryHashId] }" :class="{ &quot;is-danger&quot;: errors.has(&quot;codelibAliasName&quot;) }">
                        <span class="error-tips" v-if="errors.has(&quot;codelibAliasName&quot;)">
                            {{ errors.first('codelibAliasName') }}
                        </span>
                    </div>
                </div>
                <!-- 别名 end -->

                <!-- 访问凭据 start -->
                <div class="bk-form-item is-required" v-if="codelibConfig.label !== 'Github'">
                    <label class="bk-label">访问凭据:</label>
                    <div class="bk-form-content code-lib-credential" :class="{ &quot;is-danger&quot;: errors.has(&quot;credentialId&quot;) }">
                        <bk-select v-model="credentialId"
                            :loading="isLoadingTickets"
                            searchable
                            :clearable="false"
                            v-validate="&quot;required&quot;"
                            name="credentialId"
                            class="codelib-credential-selector"
                            placeholder="请选择相应类型的凭据"
                            @toggle="refreshTicket"
                        >
                            <bk-option v-for="(option, index) in credentialList"
                                :key="index"
                                :id="option.credentialId"
                                :name="option.credentialId">
                                <span>{{option.credentialId}}</span>
                                <i class="bk-icon icon-edit2 cre-icon" @click.stop="goToEditCre(index)"></i>
                            </bk-option>
                        </bk-select>
                        <span class="text-link" @click="addCredential">新增</span>
                    </div>
                    <span class="error-tips" v-if="errors.has(&quot;credentialId&quot;)">凭据ID不能为空</span>
                </div>
                <!-- 访问凭据 end -->
            </div>
        </form>
        <div slot="footer">
            <div class="footer-handler">
                <bk-button theme="primary" @click="submitCodelib">确定</bk-button>
                <bk-button theme="default" @click="handleCancel">取消</bk-button>
            </div>
        </div>
    </bk-dialog>
</template>

<script>
    import { mapActions, mapState } from 'vuex'
    import { getCodelibConfig, isSvn, isGit, isGithub } from '../../config/'
    import { parsePathAlias, parsePathRegion } from '../../utils'
    export default {
        name: 'codelib-dialog',
        props: {
            padding: {
                type: Number,
                default: 20
            },
            width: {
                type: Number,
                default: 700
            },
            refreshCodelibList: {
                type: Function,
                required: true
            }
        },
        data () {
            return {
                isLoadingTickets: false,
                saving: true,
                urlErrMsg: '',
                hasValidate: false,
                placeholders: {
                    url: {
                        SVN: '请输入相应类型的SVN代码库地址',
                        Git: '请输入SSH方式的Git代码库地址',
                        TGit: '请输入SSH方式的TGit代码库地址',
                        Gitlab: '请输入HTTP方式的Gitlab代码库地址',
                        HTTP: '请输入HTTP方式的Git代码库地址',
                        HTTPS: '请输入HTTPS方式的TGit代码库地址'
                    },
                    cred: {
                        SVN: '请选择相应类型的凭据',
                        Git: '请选择SSH私钥+私有Token类型的凭据',
                        Gitlab: '请选择AccessToken类型的凭据'
                    }
                }
            }
        },

        computed: {
            ...mapState({
                user: 'user'
            }),
            ...mapState('codelib', [
                'tickets',
                'codelib',
                'showCodelibDialog',
                'fetchingCodelibDetail',
                'gitOAuth',
                'githubOAuth',
                'tGitOAuth'
            ]),
            isShow: {
                get () {
                    return this.showCodelibDialog
                },
                set (showCodelibDialog) {
                    this.toggleCodelibDialog({
                        showCodelibDialog
                    })
                }
            },
            hasPower () {
                return (
                    (this.isGit
                        ? this.gitOAuth.status
                        : this.githubOAuth.status) !== 403
                )
            },
            oAuth () {
                return this.isGit
                    ? this.gitOAuth
                    : this.githubOAuth
            },
            codelibTypeName () {
                return this.codelib && this.codelib['@type']
                    ? this.codelib['@type']
                    : ''
            },
            codelibTypeConstants () {
                return this.codelibTypeName
                    .toLowerCase()
                    .replace(/^\S*?([github|git|tgit])/i, '$1')
            },
            codelibConfig () {
                console.log(
                    '0000',
                    getCodelibConfig(
                        this.codelibTypeName,
                        this.codelib.svnType,
                        this.codelib.authType
                    ) || {}
                )
                return (
                    getCodelibConfig(
                        this.codelibTypeName,
                        this.codelib.svnType,
                        this.codelib.authType
                    ) || {}
                )
            },
            title () {
                return `关联${this.codelibConfig.label || ''}代码库`
            },
            isGit () {
                return isGit(this.codelibTypeName)
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
            codelibUrl: {
                get () {
                    return this.codelib.url
                },
                set (url) {
                    const { codelib, codelibTypeName } = this
                    const { alias, msg } = parsePathAlias(
                        codelibTypeName,
                        url,
                        codelib.authType,
                        codelib.svnType
                    )
                    if (msg) {
                        this.urlErrMsg = msg
                    }
                    const param = {
                        projectName: alias,
                        url
                    }

                    param.aliasName = alias
                    this.urlErrMsg = msg
                    this.updateCodelib(param)
                }
            },
            codelibAliasName: {
                get () {
                    return this.codelib.aliasName
                },

                set (aliasName) {
                    this.updateCodelib({
                        aliasName
                    })
                }
            },
            urlPlaceholder () {
                console.log(
                    'URL PLACE HOLDER',
                    this.codelib.authType,
                    this.codelibConfig.label
                )
                return (
                    this.placeholders['url'][this.codelib.authType]
                    || this.placeholders['url'][this.codelibConfig.label]
                )
            },
            credentialPlaceholder () {
                return this.placeholders['cred'][this.codelibConfig.label]
            }
        },

        watch: {
            tickets () {
                this.isLoadingTickets = false
            },
            codelib: {
                deep: true,
                handler: async function (newVal, oldVal) {
                    const { projectId, codelibTypeConstants } = this

                    if (newVal.authType === 'OAUTH' && !this.hasValidate) {
                        await this.checkOAuth({
                            projectId,
                            type: codelibTypeConstants
                        })
                    }
                    if (newVal.authType === 'T_GIT_OAUTH' && !this.hasValidate) {
                        await this.checkOAuth({
                            projectId,
                            type: codelibTypeConstants
                        })
                    }
                    this.saving = false
                }
            },
            'gitOAuth.status': function (newStatus) {
                if (this.isGit) {
                    this.hasValidate = true
                    this.saving = false
                }
            },
            'githubOAuth.status': function (newStatus) {
                if (this.isGithub) {
                    this.hasValidate = true
                    this.saving = false
                }
            }
        },
        
        methods: {
            ...mapActions('codelib', [
                'requestTickets',
                'createOrEditRepo',
                'toggleCodelibDialog',
                'updateCodelib',
                'gitOAuth',
                'checkOAuth',
                'checkTGitOAuth'
            ]),
            async submitCodelib () {
                const {
                    projectId,
                    user: { username },
                    codelib,
                    codelibTypeName,
                    createOrEditRepo,
                    repositoryHashId
                } = this
                const params = Object.assign({}, codelib, { userName: username })
                try {
                    const valid = await this.$validator.validate()

                    if (valid && !this.urlErrMsg) {
                        this.saving = true
                        if (isSvn(codelibTypeName)) {
                            params.region = parsePathRegion(codelib.url)
                        }
                        await createOrEditRepo({
                            projectId,
                            params,
                            hashId: repositoryHashId
                        })
                        this.toggleCodelibDialog(false)
                        this.hasValidate = false
                        this.saving = true
                        this.$bkMessage({
                            message: repositoryHashId
                                ? '修改代码库成功'
                                : '添加代码库成功',
                            theme: 'success'
                        })
                        this.refreshCodelibList()
                    }
                } catch (e) {
                    if (e.code === 403) {
                        this.iframeUtil.showAskPermissionDialog({
                            noPermissionList: [
                                {
                                    resource: '代码库',
                                    option: repositoryHashId ? '编辑' : '创建'
                                }
                            ],
                            applyPermissionUrl: `/backend/api/perm/apply/subsystem/?client_id=code&project_code=${
                                projectId
                            }&service_code=code&${
                                repositoryHashId
                                    ? `role_manager=repertory`
                                    : 'role_creator=repertory'
                            }`
                        })
                    } else {
                        this.$bkMessage({
                            message: e.message,
                            theme: 'error'
                        })
                    }
                    this.saving = false
                }
            },

            async openValidate () {
                this.$emit(
                    'powersValidate',
                    this[`${this.codelibTypeConstants}OAuth`].url
                )
            },
            handleCancel () {
                this.urlErrMsg = ''
                this.hasValidate = false
                this.saving = true
                this.$validator.reset()
                this.toggleCodelibDialog({
                    showCodelibDialog: false
                })
            },
            authTypeChange (codelib) {
                // 切换重置参数
                Object.assign(codelib, {
                    aliasName: '',
                    credentialId: '',
                    url: ''
                })
                this.$validator.reset()
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
                    credentialTypes
                })
            },
            refreshTicket (isShow) {
                isShow && this.getTickets()
            },
            addCredential () {
                const { projectId, credentialTypes } = this
                window.open(
                    `/console/ticket/${projectId}/createCredential/${credentialTypes}/true`,
                    '_blank'
                )
            },
            svnTypeChange () {
                this.updateCodelib({
                    url: '',
                    aliasName: '',
                    credentialId: ''
                })
                this.$validator.reset()
                this.urlErrMsg = ''
            }
        }
    }
</script>

<style lang="scss">
    .code-lib-credential {
        display: flex;
        align-items: center;
        > .codelib-credential-selector {
            width: 300px;
            display: inline-block;
            margin-right: 4px;
        }
        .error-tips {
            display: block;
        }
        .text-link {
            cursor: pointer;
            color: #3c96ff;
            line-height: 1.5;
            font-size: 12px;
        }
    }

    .form-radio {
        margin-top: 4px;
        >label {
            margin-right: 30px;
        }
    }
    .cre-icon {
        float: right;
        margin-top: 10px;
    }
</style>
