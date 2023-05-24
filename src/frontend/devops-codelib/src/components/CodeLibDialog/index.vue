<template>
    <bk-dialog class="codelib-operate-dialog" v-model="isShow" :width="width" :padding="padding" :close-icon="false" :quick-close="false" :loading="loading" @confirm="submitCodelib" @cancel="handleCancel">
        <h3 slot="header" class="bk-dialog-title">{{title}}</h3>
        <form class="bk-form" v-bkloading="{ isLoading: saving || fetchingCodelibDetail }">
            <div class="bk-form-item is-required" v-if="isGit || isGitLab">
                <label class="bk-label">{{ $t('codelib.codelibMode') }}:</label>
                <bk-radio-group v-model="codelib.authType" @change="authTypeChange(codelib)" class="bk-form-content form-radio">
                    <bk-radio value="OAUTH" v-if="isGit">OAUTH</bk-radio>
                    <bk-radio value="SSH" v-if="!isTGit">SSH</bk-radio>
                    <bk-radio value="HTTP">HTTP</bk-radio>
                    <bk-radio value="HTTPS" v-if="isTGit">HTTPS</bk-radio>
                </bk-radio-group>
            </div>
            <div class="bk-form-item" v-if="(isGit || isGithub) && codelib.authType === 'OAUTH' || (isTGit && codelib.authType === 'T_GIT_OAUTH')">
                <div class="bk-form-item is-required" v-if="hasPower">
                    <!-- 源代码地址 start -->
                    <div class="bk-form-item is-required">
                        <label class="bk-label">{{ $t('codelib.address') }}:</label>
                        <div class="bk-form-content">
                            <bk-select
                                v-model="codelibUrl"
                                v-bind="selectComBindData"
                                v-validate="'required'"
                                name="name"
                                class="codelib-credential-selector"
                            >
                                <bk-option v-for="option in oAuth.project"
                                    :key="option.httpUrl"
                                    :id="option.httpUrl"
                                    :name="option.httpUrl">
                                </bk-option>
                            </bk-select>
                            <span class="error-tips" v-if="urlErrMsg || errors.has('name')">
                                {{ urlErrMsg || errors.first("name") }}
                            </span>
                        </div>
                    </div>
                    <!-- 源代码地址 end -->

                    <!-- 别名 start -->
                    <div class="bk-form-item is-required">
                        <label class="bk-label">{{ $t('codelib.aliasName') }}:</label>
                        <div class="bk-form-content" :class="{ 'is-danger': errors.has('aliasName') }">
                            <input type="text" class="bk-form-input" :placeholder="$t('codelib.aliasNameEnter')" name="codelibAliasName" v-model.trim="codelibAliasName" data-vv-validate-on="blur" v-validate="{ required: true, max: 60, aliasUnique: [projectId, repositoryHashId] }" :class="{ 'is-danger': errors.has('codelibAliasName') }">
                            <span class="error-tips" v-if="errors.has('codelibAliasName')">
                                {{ errors.first('codelibAliasName') }}
                            </span>
                        </div>
                    </div>
                    <!-- 别名 end -->
                </div>
                <div class="bk-form-item is-required" v-else>
                    <div class="bk-form-content" :class="{ 'is-danger': errors.has('powerValidate') }" :style="isGithub ? { textAlign: 'center', marginLeft: 0 } : {}">
                        <button class="bk-button bk-primary" type="button" @click="openValidate">{{ $t('codelib.oauthCert') }}</button>
                        <input type="text" value="" name="powerValidate" v-validate="{ required: true }" style="width: 0; height: 0; border: none; z-index: -20; opacity: 0;">
                        <span class="error-tips" v-if="errors.has('powerValidate')">
                            {{ errors.first('powerValidate') }}
                        </span>
                    </div>
                </div>
            </div>
            <div class="bk-form-item" v-else>
                <div class="bk-form-item is-required" v-if="codelibConfig.label === 'SVN'">
                    <label class="bk-label">{{ $t('codelib.codelibPullType') }}:</label>
                    <bk-radio-group v-model="codelib.svnType" @change="svnTypeChange(codelib)" class="bk-form-content form-radio">
                        <bk-radio value="ssh">SSH</bk-radio>
                        <bk-radio value="http">HTTP/HTTPS</bk-radio>
                    </bk-radio-group>
                </div>
                <!-- 源代码地址 start -->
                <div class="bk-form-item is-required" v-if="!isP4">
                    <label class="bk-label">{{ $t('codelib.address') }}:</label>
                    <div class="bk-form-content">
                        <input type="text" class="bk-form-input" :placeholder="urlPlaceholder" name="codelibUrl" v-model.trim="codelibUrl" :v-validate="'required' ? !isP4 : false" :class="{ 'is-danger': urlErrMsg || errors.has('codelibUrl') }">
                        <span class="error-tips" v-if="(urlErrMsg || errors.has('codelibUrl') && !isP4)">
                            {{ urlErrMsg || errors.first("codelibUrl") }}
                        </span>
                        <div v-else-if="isSvn" class="example-tips">
                            {{ codelib.svnType === 'ssh' ? $t('codelib.sshExampleTips') : $t('codelib.httpExampleTips') }}
                        </div>
                    </div>
                </div>
                <!-- 源代码地址 end -->

                <!-- 服务器 start -->
                <div class="bk-form-item is-required" v-if="isP4">
                    <label class="bk-label">p4 port:</label>
                    <div class="bk-form-content">
                        <div class="flex-content">
                            <input type="text" class="bk-form-input" :placeholder="portPlaceholder" name="codelibPort" v-model.trim="codelibPort" v-validate="'required'" :class="{ 'is-danger': errors.has('codelibPort') }">
                            <i class="devops-icon icon-info-circle tip-icon" v-bk-tooltips="$t('codelib.portTips')"></i>
                        </div>
                        <span class="error-tips" v-if="errors.has('codelibPort')">
                            {{ errors.first("codelibPort") }}
                        </span>
                    </div>
                </div>
                <!-- 服务器 end -->
                
                <!-- 别名 start -->
                <div class="bk-form-item is-required">
                    <label class="bk-label">{{ $t('codelib.aliasName') }}:</label>
                    <div class="bk-form-content" :class="{ 'is-danger': errors.has('aliasName') }">
                        <input type="text" class="bk-form-input" :placeholder="$t('codelib.aliasNameEnter')" name="codelibAliasName" v-model.trim="codelibAliasName" data-vv-validate-on="blur" v-validate="{ required: true, max: 60, aliasUnique: [projectId, repositoryHashId] }" :class="{ 'is-danger': errors.has('codelibAliasName') }">
                        <span class="error-tips" v-if="errors.has('codelibAliasName')">
                            {{ errors.first('codelibAliasName') }}
                        </span>
                    </div>
                </div>
                <!-- 别名 end -->

                <!-- 访问凭据 start -->
                <div class="bk-form-item is-required" v-if="codelibConfig.label !== 'Github'">
                    <label class="bk-label">{{ $t('codelib.codelibCredential') }}:</label>
                    <div class="bk-form-content code-lib-credential" :class="{ 'is-danger': errors.has('credentialId') }">
                        <bk-select v-model="credentialId"
                            :loading="isLoadingTickets"
                            searchable
                            :clearable="false"
                            v-validate="'required'"
                            name="credentialId"
                            class="codelib-credential-selector"
                            :placeholder="$t('codelib.credentialPlaceholder')"
                            @toggle="refreshTicket"
                        >
                            <bk-option v-for="(option, index) in credentialList"
                                :key="index"
                                :id="option.credentialId"
                                :name="option.credentialId">
                                <span>{{option.credentialId}}</span>
                                <i class="devops-icon icon-edit2 cre-icon" @click.stop="goToEditCre(index)"></i>
                            </bk-option>
                        </bk-select>
                        <span class="text-link" @click="addCredential">{{ $t('codelib.new') }}</span>
                    </div>
                    <span class="error-tips" v-if="errors.has('credentialId')">{{ $t('codelib.credentialRequired') }}</span>
                </div>
                <!-- 访问凭据 end -->
            </div>
        </form>
    </bk-dialog>
</template>

<script>
    import { mapActions, mapState } from 'vuex'
    import { getCodelibConfig, isGit, isGitLab, isGithub, isP4, isSvn, isTGit } from '../../config/'
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
                loading: false,
                saving: true,
                urlErrMsg: '',
                hasValidate: false,
                placeholders: {
                    url: {
                        SVN: this.$t('codelib.svnUrlPlaceholder'),
                        Git: this.$t('codelib.gitUrlPlaceholder'),
                        TGit: this.$t('codelib.tgitUrlPlaceholder'),
                        Gitlab: this.$t('codelib.gitlabUrlPlaceholder'),
                        HTTP: this.$t('codelib.httpUrlPlaceholder'),
                        HTTPS: this.$t('codelib.httpsUrlPlaceholder')
                    },
                    cred: {
                        SVN: this.$t('codelib.svnCredPlaceholder'),
                        Git: this.$t('codelib.gitCredPlaceholder'),
                        Gitlab: this.$t('codelib.gitlabCredPlaceholder')
                    },
                    port: {
                        P4: 'localhost:1666'
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
                    (this.isTGit
                        ? this.tGitOAuth.status
                        : this.isGit
                            ? this.gitOAuth.status
                            : this.githubOAuth.status) !== 403
                )
            },
            oAuth () {
                return this.isTGit
                    ? this.tGitOAuth
                    : this.isGit
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
                return (
                    getCodelibConfig(
                        this.codelibTypeName,
                        this.codelib.svnType,
                        this.codelib.authType
                    ) || {}
                )
            },
            title () {
                return this.$t('codelib.linkRepo', [
                    this.codelibConfig.label
                ])
            },
            isGit () {
                return isGit(this.codelibTypeName)
            },
            isTGit () {
                return isTGit(this.codelibTypeName)
            },
            isGitLab () {
                return isGitLab(this.codelibTypeName)
            },
            isSvn () {
                return isSvn(this.codelibTypeName)
            },
            isP4 () {
                return isP4(this.codelibTypeName)
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

                    param.aliasName = this.codelib.aliasName || alias
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
            codelibPort: {
                get () {
                    return this.codelib.url
                },
                set (url) {
                    const param = {
                        projectName: url,
                        url
                    }
                    this.updateCodelib(param)
                }
            },
            urlPlaceholder () {
                return (
                    this.placeholders.url[this.codelibConfig.label]
                    || this.placeholders.url[this.codelib.authType]
                )
            },
            credentialPlaceholder () {
                return this.placeholders.cred[this.codelibConfig.label]
            },
            portPlaceholder () {
                return this.placeholders.port[this.codelibConfig.label]
            },
            selectComBindData () {
                const bindData = {
                    searchable: true,
                    clearable: false,
                    placeholder: this.$t('codelib.codelibUrlPlaceholder')
                }
                if (this.isGit) {
                    bindData.remoteMethod = this.handleSearchCodeLib
                }
                return bindData
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
            'tGitOAuth.status': function (newStatus) {
                if (this.isTGit) {
                    this.hasValidate = true
                    this.saving = false
                }
            },
            'githubOAuth.status': function (newStatus) {
                if (this.isGithub) {
                    this.hasValidate = true
                    this.saving = false
                }
            },
            isShow (val) {
                if (!val) {
                    this.setTemplateCodelib()
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
                'checkTGitOAuth',
                'setTemplateCodelib'
            ]),
            async submitCodelib () {
                const {
                    projectId,
                    user: { username },
                    codelib,
                    createOrEditRepo,
                    repositoryHashId
                } = this
                const params = Object.assign({}, codelib, { userName: username })
                this.loading = true
                try {
                    const valid = await this.$validator.validate()

                    if (valid && !this.urlErrMsg) {
                        this.saving = true
                        if (this.isSvn) {
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
                                ? this.$t('codelib.successfullyEdited')
                                : this.$t('codelib.successfullyAdded'),
                            theme: 'success'
                        })
                        this.refreshCodelibList()
                    }
                } catch (e) {
                    if (e.code === 403) {
                        const actionId = this.$permissionActionMap[repositoryHashId ? 'edit' : 'create']
                        this.$showAskPermissionDialog({
                            noPermissionList: [{
                                actionId,
                                resourceId: this.$permissionResourceMap.code,
                                instanceId: repositoryHashId
                                    ? [{
                                        id: repositoryHashId,
                                        name: codelib.aliasName
                                    }]
                                    : null,
                                projectId
                            }]
                        })
                    } else {
                        this.$bkMessage({
                            message: e.message,
                            theme: 'error'
                        })
                    }
                    this.saving = false
                } finally {
                    this.$nextTick(() => (this.loading = false))
                }
            },

            handleSearchCodeLib (search) {
                const { projectId, codelibTypeConstants } = this
                this.checkOAuth({
                    projectId,
                    type: codelibTypeConstants,
                    search
                })
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
                this.updateCodelib({
                    url: '',
                    aliasName: '',
                    credentialId: '',
                    projectName: '',
                    authType: '',
                    svnType: ''
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
                const { projectId, codelibConfig } = this
                window.open(
                    `/console/ticket/${projectId}/createCredential/${codelibConfig.addType}/true`,
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
        margin-left: 0;
        >label {
            margin-right: 30px;
        }
    }
    .cre-icon {
        float: right;
        margin-top: 10px;
    }
    .flex-content {
        display: flex;
        justify-content: center;
        align-items: center;
        .tip-icon {
            margin-left: 5px;
        }
    }
    .example-tips {
        color: #c4c6cd;
        font-size: 12px;
    }
</style>
