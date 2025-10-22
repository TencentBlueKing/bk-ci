<template>
    <bk-dialog
        :class="{
            'codelib-operate-dialog': true,
            'codelib-orerate-oauth-dialog': !showDialogFooter
        }"
        v-model="isShow"
        :width="780"
        :padding="24"
        :quick-close="false"
        :show-footer="showDialogFooter"
    >
        <h3
            slot="header"
            class="bk-dialog-title"
        >
            {{ title }}
        </h3>
        <component
            ref="form"
            :is="comName"
            :oauth-user-list="oauthUserList"
        />
        <footer slot="footer">
            <template v-if="showDialogFooter">
                <bk-button
                    class="mr5"
                    theme="primary"
                    :loading="isLoading"
                    @click="submitCodelib"
                >
                    {{ $t('codelib.confirm') }}
                </bk-button>
                <bk-button
                    @click="handleCancel"
                    :loading="isLoading"
                >
                    {{ $t('codelib.cancel') }}
                </bk-button>
            </template>
        </footer>
    </bk-dialog>
</template>

<script>
    import P4 from './P4'
    import SVN from './SVN'
    import Git from './Git'
    import TGit from './TGit'
    import Github from './Github'
    import Gitlab from './Gitlab'
    import Custom from './Custom'
    import {
        isP4,
        isSvn,
        isGit,
        isTGit,
        isGithub,
        isGitLab,
        isScmGit,
        isScmSvn,
        getCodelibConfig,
        CODE_REPOSITORY_CACHE
    } from '../../config/'
    import { mapActions, mapState } from 'vuex'
    import { parsePathRegion } from '../../utils'
    import dialogMixin from './mixin.js'
    export default {
        name: 'codelib-dialog',
        components: {
            Github,
            Gitlab,
            SVN,
            TGit,
            Git,
            P4,
            Custom
        },
        mixins: [dialogMixin],
        props: {
            refreshCodelibList: {
                type: Function,
                required: true
            },
            oauthUserList: {
                type: Array,
                default: () => []
            }
        },
        data () {
            return {
                hasValidate: false,
                isLoading: false
            }
        },
        computed: {
            ...mapState('codelib', [
                'codelib',
                'gitOAuth',
                'tgitOAuth',
                'githubOAuth',
                'showCodelibDialog'
            ]),
            
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
            showDialogFooter () {
                return (this.oAuth.hasPower && this.isOAUTH) || !this.isOAUTH
            },
            title () {
                return this.$t('codelib.linkRepo', [
                    this.codelibConfig.label
                ])
            },
            isScmGit () {
                return isScmGit(this.codelibTypeName)
            },
            isScmSvn () {
                return isScmSvn(this.codelibTypeName)
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
            codelibConfig () {
                return (
                    getCodelibConfig(
                        this.codelibTypeName,
                        this.codelib.svnType,
                        this.codelib.authType
                    ) || {}
                )
            },
            codelibTypeName () {
                return this.codelib && this.codelib['@type']
                    ? this.codelib['@type']
                    : ''
            },
            comName () {
                const comMap = {
                    Git: 'Git',
                    TGit: 'TGit',
                    GitHub: 'Github',
                    SVN: 'SVN',
                    P4: 'P4',
                    GitLab: 'Gitlab'
                }

                return comMap[this.codelibConfig.label] || 'Custom'
            },
            
            projectId () {
                return this.$route.params.projectId
            },

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

            isOAUTH () {
                if (this.isScmGit || this.isScmSvn) {
                    return this.codelib.credentialType === 'OAUTH'
                }
                return this.codelib.authType === 'OAUTH'
            },

            shouldCheckScmAuth () {
                return {
                    scmCode: this.codelib.scmCode,
                    userName: this.codelib.userName
                }
            }
        },
        watch: {
            'gitOAuth.status': function (newStatus) {
                if (this.isGit) {
                    this.hasValidate = true
                }
            },
            'tgitOAuth.status': function (newStatus) {
                if (this.isTGit) {
                    this.hasValidate = true
                }
            },
            'githubOAuth.status': function (newStatus) {
                if (this.isGithub) {
                    this.hasValidate = true
                }
            },
            isShow (val) {
                if (!val) {
                    this.setTemplateCodelib()
                }
            },
            
            shouldCheckScmAuth: {
                handler: async function (newVal) {
                    const { projectId, codelibTypeConstants } = this
                    if (this.codelib['@type']?.startsWith('scm') && this.codelib.credentialType === 'OAUTH') {
                        await this.checkScmOAuth({
                            projectId,
                            scmCode: this.codelib.scmCode,
                            type: codelibTypeConstants,
                            username: this.codelib.userName
                        })
                    } else if (this.codelib.authType === 'OAUTH') {
                        await this.checkOAuth({
                            projectId,
                            type: codelibTypeConstants,
                            username: this.codelib.userName
                        })
                    }
                }
            }
        },
        methods: {
            ...mapActions('codelib', [
                'checkOAuth',
                'checkScmOAuth',
                'checkTGitOAuth',
                'updateCodelib',
                'createRepo',
                'toggleCodelibDialog',
                'setTemplateCodelib'
            ]),
            
            async submitCodelib () {
                if (this.isOAUTH && !this.oAuth.hasPower) {
                    this.toggleCodelibDialog(false)
                    return
                }
                
                const {
                    projectId,
                    codelib,
                    createRepo,
                    repositoryHashId
                } = this
                const params = Object.assign({}, codelib)
                try {
                    this.$refs.form.$refs.form.validate().then(async () => {
                        if (!this.urlErrMsg) {
                            if (this.isSvn) {
                                params.region = parsePathRegion(codelib.url)
                            }
                            this.isLoading = true
                            await createRepo({
                                projectId,
                                params,
                                hashId: repositoryHashId
                            }).then((res) => {
                                this.$router.push({
                                    query: {
                                        id: res.hashId
                                    }
                                })
                                localStorage.setItem(CODE_REPOSITORY_CACHE, JSON.stringify({
                                    id: res.hashId
                                }))
                                this.$emit('updateRepoId', res.hashId)
                                this.toggleCodelibDialog(false)
                                this.hasValidate = false
                                this.$bkMessage({
                                    message: repositoryHashId
                                        ? this.$t('codelib.successfullyEdited')
                                        : this.$t('codelib.successfullyAdded'),
                                    theme: 'success'
                                })
                                this.refreshCodelibList()
                            }).catch(e => {
                                this.$bkMessage({
                                    theme: 'error',
                                    message: e.message || e
                                })
                            }).finally(() => {
                                this.isLoading = false
                            })
                        }
                    }, validator => {
                        console.error(validator)
                    })
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
                }
            },
            handleCancel () {
                this.hasValidate = false
                this.$refs.form.$refs.form.clearError()
                this.toggleCodelibDialog(false)
                this.updateCodelib({
                    url: '',
                    aliasName: '',
                    credentialId: '',
                    projectName: '',
                    authType: '',
                    svnType: '',
                    userName: ''
                })
            }
        }
    }
</script>

<style lang="scss">
    .codelib-orerate-oauth-dialog {
        .bk-dialog-footer {
            display: none;
        }
    }
    .bk-dialog-title {
        text-align: left;
        font-size: 14px;
        color: #313238;
        font-weight: 400;
    }
    .codelib-credential-selector {
        width: 300px;
        display: inline-block;
        margin-right: 4px;
    }
    .error-tips {
        display: block;
    }
    .add-cred-btn {
        position: relative;
        top: -10px;
        cursor: pointer;
        color: #3c96ff;
        line-height: 1.5;
        font-size: 12px;
    }

    .form-radio {
        margin-top: 4px;
        margin-left: 0;
        >label {
            margin-right: 30px;
        }
    }
    .bk-option-content {
        display: flex;
        .name {
            display: inline-block;
            width: 92%;
            overflow: hidden;
            text-overflow: ellipsis;
        }
        .cre-icon {
            position: absolute;
            right: 15px;
            margin-top: 10px;
        }
    }
    .flex-content {
        display: flex;
        justify-content: center;
        align-items: center;
        .tip-icon {
            margin-left: 5px;
        }
    }
    .bk-form:nth-child(1) {
        margin-top: -20px !important;
    }
    .bk-form-item {
        margin-top: 20px !important;
    }
    .example-tips {
        color: #c4c6cd;
        font-size: 12px;
    }
</style>

<style lang="scss" scoped>
    .bk-form-control {
        display: list-item !important;
    }
</style>
