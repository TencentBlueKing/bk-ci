<template>
    <bk-sideslider :is-show.sync="show" :quick-close="true" @hidden="hidden" :width="622" :title="isEdit ? 'Edit Credential' : 'Add Credential'">
        <bk-form :model="formData" ref="credentialForm" slot="content" class="credential-form" form-type="vertical" label-width="400">
            <bk-form-item label="Type" :required="true" property="credentialType" :desc="{ content: computedTicket.desc, width: '400px' }" error-display-type="normal">
                <bk-select v-model="formData.credentialType" :clearable="false" @change="changeCredentialType" :disabled="isEdit">
                    <bk-option v-for="option in ticketTypes"
                        :key="option.id"
                        :id="option.id"
                        :name="option.name">
                    </bk-option>
                </bk-select>
            </bk-form-item>
            <bk-form-item label="Key" :required="true" :rules="[requireRule('Code'), idRule]" property="credentialId" error-display-type="normal">
                <bk-input v-model="formData.credentialId" placeholder="It is composed of English letters, numbers or underscores (_), no more than 40 words" :disabled="isEdit"></bk-input>
            </bk-form-item>
            <bk-form-item label="Display Name" property="credentialName" :rules="[nameRule]" :desc="{ content: 'When referencing credentials through variables in the pipeline, only referencing by name is supported', width: '400px' }" error-display-type="normal">
                <bk-input v-model="formData.credentialName" placeholder="It is composed of Chinese characters, English letters, numbers, hyphens (-), underscores (_) or English periods, and no more than 30 characters"></bk-input>
            </bk-form-item>
            <bk-form-item :label="com.label" :required="com.required" :property="com.id" v-for="com in computedTicket.content" :key="com.id" error-display-type="normal" :rules="com.rules">
                <bk-input v-model="formData[com.id]" :type="com.type" :placeholder="com.placeholder"></bk-input>
            </bk-form-item>
            <bk-form-item label="Description" property="credentialRemark">
                <bk-input type="textarea" v-model="formData.credentialRemark" placeholder="Please enter a credential description"></bk-input>
            </bk-form-item>
            <bk-form-item>
                <bk-button ext-cls="mr5" theme="primary" title="Submit" @click.stop.prevent="submitData" :loading="isLoading">Submit</bk-button>
                <bk-button ext-cls="mr5" title="Cancel" @click="hidden" :disabled="isLoading">Cancel</bk-button>
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
                        name: 'Password',
                        desc: 'Used for information that needs to be encrypted and saved in the Tencent CI, such as certificate passwords, fields that need to be encrypted in scripts, etc.',
                        content: [
                            { id: 'v1', label: 'password', type: 'text', required: true, rules: [this.requireRule('password')], placeholder: 'Please enter the password' }
                        ]
                    },
                    {
                        id: 'MULTI_LINE_PASSWORD',
                        name: 'Multi-line password',
                        desc: 'Used for information that needs to be encrypted and saved in the Tencent CI, such as certificate passwords, fields that need to be encrypted in scripts, etc.',
                        content: [
                            { id: 'v1', label: 'password', type: 'textarea', required: true, rules: [this.requireRule('password')], placeholder: 'Please enter the password' }
                        ]
                    },
                    {
                        id: 'USERNAME_PASSWORD',
                        name: 'User name+password',
                        desc: 'Used for information that needs to be encrypted and saved in the Tencent CI, such as certificate passwords, fields that need to be encrypted in scripts, etc.',
                        content: [
                            { id: 'v1', label: 'username', type: 'text', placeholder: 'please enter user name' },
                            { id: 'v2', label: 'password', type: 'text', required: true, rules: [this.requireRule('password')], placeholder: 'Please enter the password' }
                        ]
                    },
                    {
                        id: 'ACCESSTOKEN',
                        name: 'AccessToken',
                        desc: 'An access token contains the security information of this login session, which is used to associate the Gitlab type code library',
                        content: [
                            { id: 'v1', label: 'access_token', type: 'text', required: true, rules: [this.requireRule('access_token')], placeholder: 'Please enter AccessToken' }
                        ]
                    },
                    {
                        id: 'SECRETKEY',
                        name: 'SecretKey',
                        desc: 'Used for information that needs to be encrypted and saved in the Tencent CI, such as certificate passwords, fields that need to be encrypted in scripts, etc.',
                        content: [
                            { id: 'v1', label: 'secretKey', type: 'text', required: true, rules: [this.requireRule('secretKey')], placeholder: 'Please enter SecretKey' }
                        ]
                    },
                    {
                        id: 'APPID_SECRETKEY',
                        name: 'AppId+SecretKey',
                        desc: 'The key-value pair type used to set the key value, such as the user account password to be filled in by the bugly atom, api call, etc.',
                        content: [
                            { id: 'v1', label: 'appId', type: 'text', required: true, rules: [this.requireRule('appId')], placeholder: 'Please enter appId' },
                            { id: 'v2', label: 'secretKey', type: 'text', required: true, rules: [this.requireRule('secretKey')], placeholder: 'Please enter SecretKey' }
                        ]
                    },
                    {
                        id: 'SSH_PRIVATEKEY',
                        name: 'SSH key',
                        desc: 'SSH contains public and private keys, which are used to associate with SVN type code libraries. For SSH configuration instructions, please refer to the Blue Shield documentation center',
                        content: [
                            {
                                id: 'v1',
                                label: 'privateKey',
                                type: 'textarea',
                                required: true,
                                rules: [
                                    {
                                        validator: (val) => (/^(-----BEGIN (RSA|OPENSSH) PRIVATE KEY-----){1}[\s\S]*(-----END (RSA|OPENSSH) PRIVATE KEY-----)$/.test(val)),
                                        message: 'SSH contains public and private keys, which are used to associate with SVN type code libraries. For SSH configuration instructions, please refer to the Blue Shield documentation center',
                                        trigger: 'blur'
                                    },
                                    this.requireRule('privateKey')
                                ],
                                placeholder: 'SSH contains public and private keys, which are used to associate with SVN type code libraries. For SSH configuration instructions, please refer to the Blue Shield documentation center'
                            },
                            { id: 'v2', label: 'passphrase', type: 'text', placeholder: 'Please enter the private key password' }
                        ]
                    },
                    {
                        id: 'TOKEN_SSH_PRIVATEKEY',
                        name: 'SSH private key + private Token',
                        desc: 'Used to associate Git type code libraries using ssh',
                        content: [
                            { id: 'v1', label: 'token', type: 'text', required: true, rules: [this.requireRule('token')], placeholder: 'Please enter token' },
                            {
                                id: 'v2',
                                label: 'privateKey',
                                type: 'textarea',
                                required: true,
                                rules: [
                                    {
                                        validator: (val) => (/^(-----BEGIN (RSA|OPENSSH) PRIVATE KEY-----){1}[\s\S]*(-----END (RSA|OPENSSH) PRIVATE KEY-----)$/.test(val)),
                                        message: 'SSH contains public and private keys, which are used to associate with SVN type code libraries. For SSH configuration instructions, please refer to the Blue Shield documentation center',
                                        trigger: 'blur'
                                    },
                                    this.requireRule('privateKey')
                                ],
                                placeholder: 'SSH contains public and private keys, which are used to associate with SVN type code libraries. For SSH configuration instructions, please refer to the Blue Shield documentation center'
                            },
                            { id: 'v3', label: 'passphrase', type: 'text', placeholder: 'Please enter the private key password' }
                        ]
                    },
                    {
                        id: 'TOKEN_USERNAME_PASSWORD',
                        name: 'User password+private token',
                        desc: 'Used to associate Git type code libraries using http',
                        content: [
                            { id: 'v1', label: 'token', type: 'text', required: true, rules: [this.requireRule('token')], placeholder: 'Please enter token' },
                            { id: 'v2', label: 'username', type: 'text', required: true, rules: [this.requireRule('username')], placeholder: 'please enter user name' },
                            { id: 'v3', label: 'password', type: 'text', required: true, rules: [this.requireRule('password')], placeholder: 'Please enter the password' }
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
                    message: 'It is composed of English letters, numbers or underscores (_), no more than 40 words',
                    trigger: 'blur'
                },
                nameRule: {
                    validator: (val) => (/^[\u4e00-\u9fa5a-zA-Z0-9\-\.\_]{0,30}$/.test(val)),
                    message: 'It is composed of Chinese characters, English letters, numbers, hyphens (-), underscores (_) or English periods, no more than 30 characters, only for display',
                    trigger: 'blur'
                },
                isLoading: false
            }
        },

        computed: {
            ...mapState(['projectId']),

            computedTicket () {
                return this.ticketTypes.find((x) => (x.id === this.formData.credentialType))
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
                    credentialType: 'PASSWORD',
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
                    message: name + ' is required',
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

            hidden () {
                this.$emit('hidden')
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
