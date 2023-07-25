<template>
    <bk-sideslider :is-show="show" :before-close="hidden" :quick-close="true" :width="622" :title="isEdit ? $t('setting.ticket.editCredential') : $t('setting.ticket.addCredential')">
        <bk-form :model="formData" ref="credentialForm" slot="content" class="credential-form" form-type="vertical" :label-width="400">
            <bk-form-item :label="$t('setting.ticket.credentialType')" :required="true" property="credentialType" :desc="{ content: computedTicket.desc, width: '400px' }" error-display-type="normal">
                <bk-select v-model="formData.credentialType" :clearable="false" @change="changeCredentialType" :disabled="isEdit">
                    <bk-option v-for="option in ticketTypes"
                        :key="option.id"
                        :id="option.id"
                        :name="option.name">
                    </bk-option>
                </bk-select>
            </bk-form-item>
            <bk-form-item :label="$t('setting.ticket.credentialKey')" :required="true" :desc="{ content: $t('setting.ticket.credentialDesc'), width: '400px' }" :rules="[requireRule('Code'), idRule]" property="credentialId" error-display-type="normal">
                <bk-input v-model="formData.credentialId" @change="handleChange" :placeholder="$t('setting.ticket.credentialIdPlaceholder')" :disabled="isEdit"></bk-input>
            </bk-form-item>
            <bk-form-item :label="$t('displayName')" property="credentialName" :rules="[nameRule]" error-display-type="normal">
                <bk-input v-model="formData.credentialName" @change="handleChange" :placeholder="$t('setting.ticket.credentialIdPlaceholder')"></bk-input>
            </bk-form-item>
            <bk-form-item :label="com.label" :required="com.required" :property="com.id" v-for="com in computedTicket.content" :key="com.id" error-display-type="normal" :rules="com.rules">
                <bk-input v-model="formData[com.id]" @change="handleChange" :type="com.type" :placeholder="com.placeholder"></bk-input>
            </bk-form-item>
            <bk-form-item :label="$t('description')" property="credentialRemark">
                <bk-input type="textarea" v-model="formData.credentialRemark" @change="handleChange" :placeholder="$t('descriptionPlaceholder')"></bk-input>
            </bk-form-item>
            <bk-form-item>
                <bk-button ext-cls="mr5" theme="primary" title="Submit" @click.stop.prevent="submitData" :loading="isLoading">{{$t('submit')}}</bk-button>
                <bk-button ext-cls="mr5" title="Cancel" @click="hidden" :disabled="isLoading">{{$t('cancel')}}</bk-button>
            </bk-form-item>
        </bk-form>
    </bk-sideslider>
</template>

<script>
    import { setting } from '@/http'
    import { mapState } from 'vuex'

    export default {
        props: {
            show: Boolean,
            form: Object
        },

        data () {
            return {
                ticketTypes: [
                    {
                        id: 'PASSWORD',
                        name: this.$t('setting.ticket.password'),
                        desc: this.$t('setting.ticket.passwordDesc'),
                        content: [
                            { id: 'v1', label: this.$t('setting.ticket.password'), type: 'password', required: true, rules: [this.requireRule('password')], placeholder: this.$t('setting.ticket.passwordPlaceholder') }
                        ]
                    },
                    {
                        id: 'MULTI_LINE_PASSWORD',
                        name: this.$t('setting.ticket.multiLinePassword'),
                        desc: this.$t('setting.ticket.passwordDesc'),
                        content: [
                            { id: 'v1', label: this.$t('setting.ticket.password'), type: 'textarea', required: true, rules: [this.requireRule('password')], placeholder: this.$t('setting.ticket.passwordPlaceholder') }
                        ]
                    },
                    {
                        id: 'USERNAME_PASSWORD',
                        name: this.$t('setting.ticket.usernamePassword'),
                        desc: this.$t('setting.ticket.passwordDesc'),
                        content: [
                            { id: 'v1', label: this.$t('setting.ticket.username'), type: 'text', required: true, rules: [this.requireRule('username')], placeholder: this.$t('setting.ticket.usernamePlaceholder') },
                            { id: 'v2', label: this.$t('setting.ticket.password'), type: 'password', required: true, rules: [this.requireRule('password')], placeholder: this.$t('setting.ticket.passwordPlaceholder') }
                        ]
                    },
                    {
                        id: 'ACCESSTOKEN',
                        name: this.$t('setting.ticket.accessToken'),
                        desc: this.$t('setting.ticket.accessTokenDesc'),
                        content: [
                            { id: 'v1', label: this.$t('setting.ticket.accessToken'), type: 'password', required: true, rules: [this.requireRule('access_token')], placeholder: this.$t('setting.ticket.accessTokenDesc') }
                        ]
                    },
                    {
                        id: 'SECRETKEY',
                        name: this.$t('setting.ticket.secretKey'),
                        desc: this.$t('setting.ticket.passwordDesc'),
                        content: [
                            { id: 'v1', label: this.$t('setting.ticket.secretKey'), type: 'password', required: true, rules: [this.requireRule('secretKey')], placeholder: this.$t('setting.ticket.secretKeyPlaceholder') }
                        ]
                    },
                    {
                        id: 'APPID_SECRETKEY',
                        name: this.$t('setting.ticket.appIdSecretKey'),
                        desc: this.$t('setting.ticket.appIdDesc'),
                        content: [
                            { id: 'v1', label: this.$t('setting.ticket.appId'), type: 'text', required: true, rules: [this.requireRule('appId')], placeholder: this.$t('setting.ticket.appIdPlaceholder') },
                            { id: 'v2', label: this.$t('setting.ticket.secretKey'), type: 'password', required: true, rules: [this.requireRule('secretKey')], placeholder: this.$t('setting.ticket.secretKeyPlaceholder') }
                        ]
                    },
                    {
                        id: 'SSH_PRIVATEKEY',
                        name: this.$t('setting.ticket.sshKey'),
                        desc: this.$t('setting.ticket.sshKeyDesc'),
                        content: [
                            {
                                id: 'v1',
                                label: this.$t('setting.ticket.privateKey'),
                                type: 'textarea',
                                required: true,
                                rules: [
                                    {
                                        validator: (val) => (/^(-----BEGIN (RSA|OPENSSH) PRIVATE KEY-----){1}[\s\S]*(-----END (RSA|OPENSSH) PRIVATE KEY-----)$/.test(val)),
                                        message: this.$t('setting.ticket.sshKeyDesc'),
                                        trigger: 'blur'
                                    },
                                    this.requireRule('privateKey')
                                ],
                                placeholder: this.$t('setting.ticket.sshKeyDesc')
                            },
                            { id: 'v2', label: this.$t('setting.ticket.passphrase'), type: 'password', placeholder: this.$t('setting.ticket.keyPasswordPlaceholder') }
                        ]
                    },
                    {
                        id: 'TOKEN_SSH_PRIVATEKEY',
                        name: this.$t('setting.ticket.sshKeyToken'),
                        desc: this.$t('setting.ticket.sshKeyTokenDesc'),
                        content: [
                            { id: 'v1', label: this.$t('setting.ticket.token'), type: 'password', required: true, rules: [this.requireRule('token')], placeholder: this.$t('setting.ticket.tokenPlaceholder') },
                            {
                                id: 'v2',
                                label: this.$t('setting.ticket.privateKey'),
                                type: 'textarea',
                                required: true,
                                rules: [
                                    {
                                        validator: (val) => (/^(-----BEGIN (RSA|OPENSSH) PRIVATE KEY-----){1}[\s\S]*(-----END (RSA|OPENSSH) PRIVATE KEY-----)$/.test(val)),
                                        message: this.$t('setting.ticket.sshKeyDesc'),
                                        trigger: 'blur'
                                    },
                                    this.requireRule('privateKey')
                                ],
                                placeholder: this.$t('setting.ticket.sshKeyDesc')
                            },
                            { id: 'v3', label: this.$t('setting.ticket.passphrase'), type: 'password', placeholder: this.$t('setting.ticket.keyPasswordPlaceholder') }
                        ]
                    },
                    {
                        id: 'TOKEN_USERNAME_PASSWORD',
                        name: this.$t('setting.ticket.passwordToken'),
                        desc: this.$t('setting.ticket.passwordTokenDesc'),
                        content: [
                            { id: 'v1', label: this.$t('setting.ticket.token'), type: 'password', required: true, rules: [this.requireRule('token')], placeholder: this.$t('setting.ticket.tokenPlaceholder') },
                            { id: 'v2', label: this.$t('setting.ticket.username'), type: 'text', required: true, rules: [this.requireRule('username')], placeholder: this.$t('setting.ticket.usernamePlaceholder') },
                            { id: 'v3', label: this.$t('setting.ticket.password'), type: 'password', required: true, rules: [this.requireRule('password')], placeholder: this.$t('setting.ticket.passwordPlaceholder') }
                        ]
                    },
                    {
                        id: 'OAUTHTOKEN',
                        name: 'OauthToken',
                        desc: this.$t('setting.ticket.oauthTokenDesc'),
                        content: [
                            { id: 'v1', label: 'OauthToken', type: 'password', required: true, rules: [this.requireRule('oauth_token')], placeholder: this.$t('setting.ticket.oauthTokenPlaceholder') }
                        ]
                    }
                ],
                formData: {
                    credentialType: 'PASSWORD',
                    credentialId: '',
                    credentialName: '',
                    credentialRemark: '',
                    v1: '',
                    v2: '',
                    v3: '',
                    v4: ''
                },
                idRule: {
                    validator: (val) => (/^[a-zA-Z0-9\_]{1,40}$/.test(val)),
                    message: this.$t('setting.ticket.credentialIdPlaceholder'),
                    trigger: 'blur'
                },
                nameRule: {
                    validator: (val) => (/^[\u4e00-\u9fa5a-zA-Z0-9\-\.\_]{0,30}$/.test(val)),
                    message: this.$t('setting.ticket.credentialNamePlaceholder'),
                    trigger: 'blur'
                },
                isLoading: false
            }
        },

        computed: {
            ...mapState(['projectId']),

            computedTicket () {
                return this.ticketTypes.find((x) => (x.id === this.formData.credentialType)) || {}
            },
            isEdit () {
                return this.form.permissions
            }
        },

        watch: {
            show (val) {
                if (val) {
                    this.initData()
                }
            }
        },

        methods: {
            initData () {
                const defaultForm = {
                    credentialType: '',
                    credentialId: '',
                    credentialName: '',
                    credentialRemark: '',
                    v1: '',
                    v2: '',
                    v3: '',
                    v4: ''
                }
                this.formData = Object.assign(defaultForm, this.form)
            },

            requireRule (name) {
                return {
                    required: true,
                    message: name + this.$t('isRequired'),
                    trigger: 'blur'
                }
            },

            submitData () {
                this.$refs.credentialForm.validate(() => {
                    let method = setting.createTicket
                    const params = [this.projectId, this.formData]
                    if (this.isEdit) {
                        method = setting.modifyTicket
                        params.push(this.formData.credentialId)
                    }
                    this.isLoading = true
                    method(...params).then(() => {
                        const message = this.isEdit ? 'Edit successfully' : 'Added successfully'
                        this.$bkMessage({ theme: 'success', message })
                        this.$emit('success')
                    }).catch((err) => {
                        this.$bkMessage({ theme: 'error', message: err.message || err })
                    }).finally(() => {
                        this.isLoading = false
                    })
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.content || err })
                })
            },

            changeCredentialType (credentialType) {
                this.handleChange()
                this.$refs.credentialForm.clearError()
                this.formData.v1 = ''
                this.formData.v2 = ''
                this.formData.v3 = ''
                this.formData.v4 = ''
                this.$router.replace({
                    params: {
                        credentialType
                    }
                })
            },

            handleChange () {
                window.changeFlag = true
            },

            hidden () {
                if (window.changeFlag) {
                    this.$bkInfo({
                        title: this.$t('确认离开当前页？'),
                        subHeader: this.$createElement('p', {
                            style: {
                                color: '#63656e',
                                fontSize: '14px',
                                textAlign: 'center'
                            }
                        }, this.$t('离开将会导致未保存信息丢失')),
                        okText: this.$t('离开'),
                        confirmFn: () => {
                            window.changeFlag = false
                            this.$emit('hidden')
                        }
                    })
                } else {
                    this.$emit('hidden')
                }
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .credential-form {
        padding: 20px 30px;
        /deep/ button {
            margin: 8px 10px 0 0;
        }
    }
</style>
