<template>
    <section class="credential-certificate-content">
        <content-header>
            <template slot="left">
                <span class="inner-header-title">
                    {{ pageType === 'create' ? $t('ticket.createCredential') : $t('ticket.editCredential') }}
                </span>
            </template>
        </content-header>

        <section
            class="sub-view-port"
            v-bkloading="{
                isLoading: loading.isLoading,
                title: loading.title
            }"
        >
            <empty-tips
                v-if="!hasPermission && showContent"
                :title="emptyTipsConfig.title"
                :desc="emptyTipsConfig.desc"
                :btns="emptyTipsConfig.btns"
            >
            </empty-tips>
            
            <bk-form
                v-if="showContent && hasPermission"
                class="bk-form-wrapper"
            >
                <!-- 凭据类型 start -->
                <bk-form-item
                    required
                    desc-type="icon"
                    :label="$t('ticket.type')"
                    :desc="getTypeDesc(localConfig.credentialType)"
                >
                    <selector
                        :list="ticketType"
                        :value="localConfig.credentialType"
                        class="credential-type-selector"
                        :disabled="nameReadOnly || typeReadOnly"
                        :item-selected="changeTicketType"
                    >
                    </selector>
                </bk-form-item>
                <!-- 凭据类型 end -->

                <!-- 凭据名称 start -->
                <bk-form-item
                    required
                    :label="$t('ticket.id')"
                >
                    <bk-input
                        name="credentialId"
                        v-validate="{ required: true, regex: /^[a-zA-Z0-9\_]{1,40}$/ }"
                        :placeholder="$t('ticket.credential.validateId')"
                        v-model="localConfig.credentialId"
                        :class="{
                            'is-danger': errors.has('credentialId')
                        }"
                        :disabled="nameReadOnly"
                    />
                    <p
                        class="error-tips"
                        v-show="errors.has('credentialId')"
                    >
                        {{ `${$t('ticket.credential.validateId')}` }}
                    </p>
                </bk-form-item>
                <!-- 凭据名称 end -->

                <!-- 凭据别名 start -->
                <bk-form-item
                    desc-type="icon"
                    :label="$t('ticket.alias')"
                    :desc="$t('ticket.credential.nameTips')"
                >
                    <bk-input
                        name="credentialName"
                        v-validate="{ regex: /^[\u4e00-\u9fa5a-zA-Z0-9\-\.\_]{0,64}$/ }"
                        :placeholder="$t('ticket.credential.validateName')"
                        v-model="localConfig.credentialName"
                        :class="{
                            'is-danger': errors.has('credentialName')
                        }"
                    />
                    <p
                        class="error-tips"
                        v-show="errors.has('credentialName')"
                    >
                        {{ `${$t('ticket.credential.validateName')}` }}
                    </p>
                </bk-form-item>
                <!-- 凭据别名 end -->

                <!-- 凭据内容 start -->
                <bk-form-item
                    v-for="(obj, key) in newModel"
                    :key="key"
                    :required="obj.rules"
                    :label="$t(obj.label)"
                >
                    <bk-input
                        v-if="obj.type === 'password'"
                        v-validate="obj.rule"
                        :name="key"
                        v-model="localConfig.credential[obj.modelName]"
                        v-bind="obj"
                        @focus="handlePwdFocus(obj.modelName)"
                        :password-icon="creId ? [] : ['icon-eye-slash', 'icon-eye']"
                        :placeholder="$t(obj.placeholder)"
                        :class="{ 'is-danger': errors.has(key) }"
                    />
                    <component
                        v-else
                        v-validate="($t(obj.label) === $t('ticket.credential.sshKey') && localConfig.credential[obj.modelName] === '******') ? {} : obj.rule"
                        :is="obj.component"
                        :name="key"
                        v-model="localConfig.credential[obj.modelName]"
                        @focus="handlePwdFocus(obj.modelName)"
                        v-bind="obj"
                        :placeholder="$t(obj.placeholder)"
                        :class="{ 'is-danger': errors.has(key) }"
                    >
                    </component>
                            
                    <p
                        class="error-tips"
                        v-show="errors.has(key)"
                    >
                        {{ $t(obj.errorMsg) }}
                    </p>
                </bk-form-item>
                <!-- 凭据内容 end -->

                <!-- 凭据描述 start -->
                <bk-form-item :label="$t('ticket.remark')">
                    <textarea
                        class="bk-form-textarea"
                        :placeholder="$t('ticket.credential.credentialRemark')"
                        name="credentialDesc"
                        v-validate="{ required: false, max: 50 }"
                        :class="{
                            'is-danger': errors.has('credentialDesc')
                        }"
                        v-model="localConfig.credentialRemark"
                    ></textarea>
                    <p
                        class="error-tips"
                        v-show="errors.has('credentialDesc')"
                    >
                        {{ $t('ticket.credential.remarkLenLimit') }}
                    </p>
                </bk-form-item>
                <!-- 凭据描述 end -->

                <div class="operate-btn">
                    <bk-button
                        v-perm="{
                            permissionData: {
                                projectId: projectId,
                                resourceType: CRED_RESOURCE_TYPE,
                                resourceCode: pageType === 'create' ? projectId : creId,
                                action: pageType === 'create' ? CRED_RESOURCE_ACTION.CREATE : CRED_RESOURCE_ACTION.EDIT
                            }
                        }"
                        key="comfirmBtn"
                        theme="primary"
                        @click="submit"
                    >
                        {{ $t('ticket.comfirm') }}
                    </bk-button>
                    <bk-button @click="cancel">{{ $t('ticket.cancel') }}</bk-button>
                </div>
            </bk-form>
        </section>
    </section>
</template>
<script>
    import Selector from '@/components/atomFormField/Selector'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import VuexTextarea from '@/components/atomFormField/VuexTextarea'
    import empty from '@/components/common/empty'
    import emptyTips from '@/components/devops/emptyTips'
    import { CRED_RESOURCE_ACTION, CRED_RESOURCE_TYPE } from '@/utils/permission'
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
                CRED_RESOURCE_TYPE,
                CRED_RESOURCE_ACTION,
                ticketDocsUrl: this.BKCI_DOCS.TICKET_DOC,
                showContent: false,
                hasPermission: true,
                newModel: {},
                pageType: 'create',
                localConfig: {},
                nameReadOnly: false,
                typeReadOnly: false,
                isClearPwded: {
                    v1: false,
                    v2: false,
                    v3: false,
                    v4: false
                },
                loading: {
                    isLoading: true,
                    title: this.$t('ticket.loadingTitle')
                },
                editInitCredential: ['v1', 'v2', 'v3', 'v4'],
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
                            handler: this.applyPermission,
                            text: this.$t('ticket.applyPermission')
                        }
                    ]
                },
                submiting: false
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
            changeProject () {
                this.iframeUtil.toggleProjectMenu(true)
            },
            applyPermission () {
                this.handleNoPermission({
                    projectId: this.projectId,
                    resourceType: CRED_RESOURCE_TYPE,
                    resourceCode: this.projectId,
                    action: CRED_RESOURCE_ACTION.CREATE
                })
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
                try {
                    const result = await this.$validator.validateAll()
                    if (!result) {
                        throw new Error(this.$t('ticket.validateFailed'))
                    }
                    
                    const credential = {
                        credentialId: this.localConfig.credentialId,
                        credentialRemark: this.localConfig.credentialRemark,
                        credentialType: this.localConfig.credentialType,
                        credentialName: this.localConfig.credentialName
                    }
                    for (const key in this.newModel) {
                        Object.assign(credential, { [key]: this.localConfig.credential[key] })
                    }
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
                    const message = this.$t('ticket.credential.successfullysavedential')
                    const theme = 'success'

                    this.$bkMessage({
                        message,
                        theme
                    })
                    this.$nextTick(() => {
                        this.$router.push({
                            name: 'credentialList'
                        })
                    })
                } catch (e) {
                    const resourceCode = this.pageType === 'create' ? this.projectId : this.creId
                    const action = this.pageType === 'create' ? CRED_RESOURCE_ACTION.CREATE : CRED_RESOURCE_ACTION.EDIT

                    this.handleError(
                        e,
                        {
                            projectId: this.projectId,
                            resourceType: CRED_RESOURCE_TYPE,
                            resourceCode,
                            action
                        }
                    )
                } finally {
                    this.submiting = false
                }
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
                this.isClearPwded = {
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
                if (this.creId) {
                    this.pageType = 'edit'
                    this.nameReadOnly = true
                    this.loading.isLoading = true
                    try {
                        const res = await this.$store.dispatch('ticket/requestCredentialDetail', {
                            projectId: this.projectId,
                            creId: this.creId
                        })
                        Object.assign(this.localConfig, {
                            credentialId: res.credentialId,
                            credentialRemark: res.credentialRemark,
                            credentialType: res.credentialType,
                            credentialName: res.credentialName,
                            credential: this.editInitCredential.reduce((acc, key) => {
                                acc[key] = res[key]
                                return acc
                            }, {})
                        })
                        this.newModel = this.getTicketByType(res.credentialType)
                    } catch (e) {
                        this.handleError(
                            e,
                            {
                                projectId: this.projectId,
                                resourceType: CRED_RESOURCE_TYPE,
                                resourceCode: this.creId,
                                action: CRED_RESOURCE_ACTION.VIEW
                            }
                        )
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
            },
            handlePwdFocus (key) {
                const isSensitive = this.editInitCredential.includes(key)
                if (this.creId && !this.isClearPwded[key] && isSensitive) {
                    this.localConfig.credential[key] = ''
                    this.isClearPwded[key] = true
                }
            }
        }
    }
</script>

<style lang="scss">
    @import './../scss/conf';

    .operate-btn {
        margin: 30px 0 0 140px
    }
    
    .bk-form-wrapper {
        max-width: 750px;
        .bk-form-content {
            .error-tips {
                max-width: 550px;
                font-size: 12px;
                padding-top: 6px;
            }
        }
    
        .link-tips {
            margin-left: 28px;
        }
    }
    
</style>
