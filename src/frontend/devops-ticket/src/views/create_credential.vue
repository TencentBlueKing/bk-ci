<template>

    <section class="credential-certificate-content">
        <content-header>
            <template slot="left">
                <span class="inner-header-title">{{ $t('ticket.createCredential') }}</span>
            </template>
        </content-header>

        <section
            class="sub-view-port"
            v-bkloading="{
                isLoading: loading.isLoading,
                title: loading.title
            }">

            <empty-tips
                v-if="!hasPermission && showContent"
                :title="emptyTipsConfig.title"
                :desc="emptyTipsConfig.desc"
                :btns="emptyTipsConfig.btns">
            </empty-tips>

            <div class="bk-form credential-setting">
                <div v-if="showContent && hasPermission" class="bk-form-wrapper">
                    <!-- 凭据类型 start -->
                    <div class="bk-form-item is-required">
                        <label class="bk-label">{{ $t('ticket.type') }}：</label>
                        <div class="bk-form-content credential-type-content">
                            <selector
                                :list="ticketType"
                                :value="localConfig.credentialType"
                                class="credential-type-selector"
                                :disabled="nameReadOnly || typeReadOnly"
                                :item-selected="changeTicketType"
                            >
                            </selector>
                            <bk-popover placement="right" :max-width="250">
                                <i class="devops-icon icon-info-circle"></i>
                                <div slot="content" style="white-space: normal;">
                                    <div>
                                        {{ getTypeDesc(localConfig.credentialType) }}
                                        <a style="color:#3c96ff" target="_blank" :href="ticketDocsUrl">
                                            {{ $t('ticket.learnMore') }}
                                        </a>
                                    </div>
                                </div>
                            </bk-popover>
                        </div>
                    </div>
                    <!-- 凭据类型 end -->

                    <!-- 凭据名称 start -->
                    <div class="bk-form-item is-required">
                        <label class="bk-label">{{ $t('ticket.id') }}：</label>
                        <div class="bk-form-content">
                            <input type="text" name="credentialId" v-validate="{ required: true, regex: /^[a-zA-Z0-9\_]{1,40}$/ }" class="bk-form-input" :placeholder="$t('ticket.credential.validateId')"
                                v-model="localConfig.credentialId"
                                :class="{
                                    'is-danger': errors.has('credentialId')
                                }"
                                :disabled="nameReadOnly"
                            >
                            <p class="error-tips"
                                v-show="errors.has('credentialId')">
                                {{ `${$t('ticket.credential.validateId')}` }}
                            </p>
                        </div>
                    </div>
                    <!-- 凭据名称 end -->

                    <!-- 凭据别名 start -->
                    <div class="bk-form-item">
                        <label class="bk-label">{{ $t('ticket.name') }}：</label>
                        <div class="bk-form-content">
                            <input type="text" name="credentialName" v-validate="{ regex: /^[\u4e00-\u9fa5a-zA-Z0-9\-\.\_]{0,64}$/ }" class="bk-form-input" :placeholder="$t('ticket.credential.validateName')"
                                v-model="localConfig.credentialName"
                                :class="{
                                    'is-danger': errors.has('credentialName')
                                }"
                            >
                            <i class="devops-icon icon-info-circle name-icon" v-bk-tooltips="{ content: $t('ticket.credential.nameTips') }"></i>
                            <p class="error-tips"
                                v-show="errors.has('credentialName')">
                                {{ `${$t('ticket.credential.validateName')}` }}
                            </p>
                        </div>
                    </div>
                    <!-- 凭据别名 end -->

                    <!-- 凭据内容 start -->
                    <div v-for="(obj, key) in newModel" :key="key" :class="{ 'bk-form-item': true, 'is-required': obj.rules }">
                        <label v-if="obj.label" class="bk-label">{{ $t(obj.label) }}：</label>
                        <div class="bk-form-content">
                            <a v-if="obj.type === 'password' && localConfig.credential[obj.modelName] !== '******'" href="javascript:;" @click="toggleShowPwdCon(obj.modelName)"><i :class="showPwdCon[obj.modelName] ? 'devops-icon icon-hide' : 'devops-icon icon-eye'"></i></a>
                            <component v-validate="($t(obj.label) === $t('ticket.credential.sshKey') && localConfig.credential[obj.modelName] === '******') ? {} : obj.rule" v-if="obj.type !== 'password' || !showPwdCon[obj.modelName]" :is="obj.component" :name="key" :handle-change="updateElement" v-model="localConfig.credential[obj.modelName]" v-bind="obj" :placeholder="$t(obj.placeholder)" :class="{ 'is-danger': errors.has(key) }"></component>
                            <component v-validate="obj.rule" v-if="obj.type === 'password' && showPwdCon[obj.modelName]" :is="obj.component" :name="key" :handle-change="updateElement" v-model="localConfig.credential[obj.modelName]" type="text" v-bind="obj" :placeholder="$t(obj.placeholder)" :class="{ 'is-danger': errors.has(key) }"></component>
                            <p class="error-tips"
                                v-show="errors.has(key)">
                                {{$t(obj.errorMsg)}}
                            </p>
                        </div>
                    </div>
                    <!-- 凭据内容 end -->

                    <!-- 凭据描述 start -->
                    <div class="bk-form-item cre-content">
                        <label class="bk-label">{{ $t('ticket.remark') }}：</label>
                        <div class="bk-form-content">
                            <textarea class="bk-form-textarea" :placeholder="$t('ticket.credential.credentialRemark')" name="credentialDesc" v-validate="{ required: false, max: 50 }"
                                :class="{
                                    'is-danger': errors.has('credentialDesc')
                                }"
                                v-model="localConfig.credentialRemark"
                            ></textarea>
                            <p class="error-tips"
                                v-show="errors.has('credentialDesc')">
                                {{ $t('ticket.credential.remarkLenLimit') }}
                            </p>
                        </div>
                    </div>
                    <!-- 凭据描述 end -->

                    <div class="operate-btn">
                        <bk-button theme="primary" @click="submit">{{ $t('ticket.comfirm') }}</bk-button>
                        <bk-button @click="cancel">{{ $t('ticket.cancel') }}</bk-button>
                    </div>
                </div>
            </div>

        </section>
    </section>
</template>

<script>
    import empty from '@/components/common/empty'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import VuexTextarea from '@/components/atomFormField/VuexTextarea'
    import Selector from '@/components/atomFormField/Selector'
    import emptyTips from '@/components/devops/emptyTips'
    import { mapGetters } from 'vuex'

    export default {
        components: {
            'vue-input': VuexInput,
            'vue-textarea': VuexTextarea,
            'bk-empty': empty,
            emptyTips,
            Selector
        },
        data () {
            return {
                ticketDocsUrl: this.BKCI_DOCS.TICKET_DOC,
                showContent: false,
                hasPermission: true,
                newModel: {},
                pageType: 'create',
                localConfig: {},
                nameReadOnly: false,
                typeReadOnly: false,
                showPwdCon: {
                    v1: false,
                    v2: false,
                    v3: false,
                    v4: false
                },
                loading: {
                    isLoading: true,
                    title: this.$t('ticket.loadingTitle')
                },
                editInitCredential: {
                    v1: '******',
                    v2: '******',
                    v3: '******',
                    v4: '******'
                },
                emptyTipsConfig: {
                    title: this.$t('ticket.noPermission'),
                    desc: this.$t('ticket.credential.noCreateCredPermissionTips'),
                    btns: [
                        {
                            type: 'primary',
                            size: 'normal',
                            handler: this.changeProject,
                            text: this.$t('ticket.switchProject')
                        },
                        {
                            type: 'success',
                            size: 'normal',
                            handler: this.goToApplyPerm,
                            text: this.$t('ticket.applyPermission')
                        }
                    ]
                }
            }
        },
        computed: {
            ...mapGetters('ticket', [
                'getTicketByType',
                'getTicketType',
                'getDefaultCredential'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            creId () {
                return this.$route.params.credentialId
            },
            type () {
                return this.$route.params.type ? this.$route.params.type : 'PASSWORD'
            },
            routeName () {
                return this.$route.name
            },
            ticketType () {
                return this.getTicketType()
            }
        },
        watch: {
            routeName: async function () {
                await this.handleCreate()
                if (this.$route.params.locked) {
                    this.typeReadOnly = true
                } else {
                    this.typeReadOnly = false
                }
            },
            projectId: async function () {
                await this.handleCreate()
                if (this.$route.params.locked) {
                    this.typeReadOnly = true
                } else {
                    this.typeReadOnly = false
                }
            }
        },
        async created () {
            await this.handleCreate()
            if (this.$route.params.locked) {
                this.typeReadOnly = true
            }
        },
        methods: {
            updateElement () {

            },
            toggleShowPwdCon (modelName) {
                this.showPwdCon[modelName] = !this.showPwdCon[modelName]
            },
            changeProject () {
                this.iframeUtil.toggleProjectMenu(true)
            },
            goToApplyPerm () {
                // const url = `/backend/api/perm/apply/subsystem/?client_id=ticket&project_code=${this.projectId}&service_code=ticket&role_creator=credential`
                // window.open(url, '_blank')
                this.applyPermission(this.$permissionActionMap.create, this.$permissionResourceMap.credential, [{
                    id: this.projectId,
                    type: this.$permissionResourceTypeMap.PROJECT
                }])
            },
            cancel () {
                this.$router.push({
                    name: 'credentialList'
                })
            },
            getTypeDesc (type) {
                const curType = this.ticketType.find(item => item.id === type)
                return curType.desc ? curType.desc : type
            },
            async submit () {
                this.$validator.validateAll().then(async (result) => {
                    if (result) {
                        const credential = {
                            credentialId: this.localConfig.credentialId,
                            credentialRemark: this.localConfig.credentialRemark,
                            credentialType: this.localConfig.credentialType,
                            credentialName: this.localConfig.credentialName
                        }
                        for (const key in this.newModel) {
                            Object.assign(credential, { [key]: this.localConfig.credential[key] })
                        }
                        let message, theme
                        try {
                            if (this.pageType === 'create') {
                                await this.$store.dispatch('ticket/createCredential', {
                                    projectId: this.projectId,
                                    credential
                                })
                            } else {
                                await this.$store.dispatch('ticket/editCredential', {
                                    projectId: this.projectId,
                                    creId: this.localConfig.credentialId,
                                    credential
                                })
                            }
                            message = this.$t('ticket.credential.successfullysavedential')
                            theme = 'success'
                        } catch (err) {
                            message = err.message ? err.message : err
                            theme = 'error'
                        } finally {
                            this.$bkMessage({
                                message,
                                theme
                            })
                            if (theme === 'success') {
                                this.$router.push({
                                    name: 'credentialList'
                                })
                            }
                        }
                    }
                })
            },
            changeTicketType (id, data) {
                this.$router.push({
                    name: 'createCredentialWithType',
                    params: {
                        type: id
                    }
                })
                this.newModel = this.getTicketByType(id)
                this.localConfig.credentialType = id
                this.localConfig.credential = {
                    v1: '',
                    v2: '',
                    v3: '',
                    v4: ''
                }
                this.showPwdCon = {
                    v1: false,
                    v2: false,
                    v3: false,
                    v4: false
                }
            },
            async handleCreate () {
                this.localConfig = {
                    credentialId: '',
                    credentialType: 'PASSWORD',
                    credentialRemark: '',
                    credentialName: '',
                    credential: {
                        v1: '',
                        v2: '',
                        v3: '',
                        v4: ''
                    }
                }
                if (this.$route.name === 'editCredential') {
                    this.pageType = 'edit'
                    this.nameReadOnly = true
                    this.loading.isLoading = true
                    try {
                        const res = await this.$store.dispatch('ticket/requestCredentialDetail', {
                            projectId: this.projectId,
                            creId: this.creId
                        })
                        const data = res
                        Object.assign(this.localConfig, {
                            credentialId: data.credentialId,
                            credentialRemark: data.credentialRemark,
                            credentialType: data.credentialType,
                            credentialName: data.credentialName,
                            credential: this.editInitCredential
                        })
                        this.newModel = this.getTicketByType(data.credentialType)
                    } catch (err) {
                        const message = err.message ? err.message : err
                        const theme = 'error'
                        this.$bkMessage({
                            message,
                            theme
                        })
                    } finally {
                        this.loading.isLoading = false
                    }
                } else {
                    this.pageType = 'create'
                    this.nameReadOnly = false
                    this.localConfig.credentialType = this.type
                    this.newModel = this.getTicketByType(this.type)
                    this.loading.isLoading = true
                    try {
                        const res = await this.$store.dispatch('ticket/requestCredentialPermission', {
                            projectId: this.projectId
                        })
                        this.hasPermission = res
                    } catch (err) {
                        const message = err.message ? err.message : err
                        const theme = 'error'
                        this.$bkMessage({
                            message,
                            theme
                        })
                    } finally {
                        this.loading.isLoading = false
                    }
                }
                this.showContent = true
            }
        }
    }
</script>

<style lang="scss">
    @import './../scss/conf';

    .operate-btn {
        margin: 30px 0 0 140px
    }
    .credential-setting {
        width: 100%;
        max-width: initial;
        .bk-form-wrapper {
            max-width: 750px;
        }
        .bk-label {
            margin-top: 13px;
            min-width: 120px;
            width: auto;
        }
        .bk-form-content {
            margin-left: 140px;
            margin-top: 10px;
            .error-tips {
                max-width: 550px;
                font-size: 12px;
                padding-top: 6px;
            }
        }
        .devops-icon.name-icon {
            padding-left: 0;
            margin-left: 4px;
        }
        .bk-form-input, .bk-form-password, .bk-selector, .bk-form-textarea {
            width: 90%
        }
        .credential-type-content {
            display: flex;
            align-items: center;
        }
        .credential-type-selector {
            display: inline-block;
            width: 550px;
        }
        .icon-hide,.icon-eye {
            right: 4%;
            position: absolute;
            padding: 10px;
            color: #808080;
        }
        .icon-info-circle {
            padding-left: 8px;
            color: #C3CDD7;
            font-size: 14px;
        }
        .link-tips {
            margin-left: 28px;
        }
    }
</style>
