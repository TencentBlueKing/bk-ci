<template>
    <section>
        <!-- 名称 start -->
        <div class="bk-form-item is-required cert-input-item">
            <label class="bk-label">{{ $t('ticket.cert.certName') }}：</label>
            <div class="bk-form-content">
                <input
                    type="text"
                    class="bk-form-input"
                    name="androidId"
                    v-validate="{ required: true, regex: /^[a-zA-Z0-9\.\_]{1,100}$/ }"
                    :placeholder="$t('ticket.cert.namePlaceholer')"
                    v-model="formData.certId"
                    :disabled="isEdit"
                    :class="{ 'is-danger': errors.has('androidId') }"
                >
                <p :class="errors.has('androidId') ? 'error-tips' : 'normal-tips'">{{ $t('ticket.cert.validateName') }}</p>
            </div>
        </div>

        <!-- jks文件 start -->
        <div class="bk-form-item is-required cert-input-item">
            <label class="bk-label">{{ $t('ticket.cert.jksFile') }}：</label>
            <div class="bk-form-content">
                <input
                    type="text"
                    class="bk-form-input"
                    readonly
                    name="jksFileName"
                    v-validate="{ required: true }"
                    v-model="formData.jksFileName"
                    :class="{ 'is-danger': errors.has('jksFileName') }"
                    :placeholder="$t('ticket.cert.emptyFile')"
                />
                <a
                    href="javascript:void(0);"
                    class="file-input"
                    :title="$t('ticket.cert.selectFile')"
                >
                    <div class="file-input-wrap">
                        <input
                            type="file"
                            class="file-input-btn"
                            accept=".jks,.keystore"
                            @change="handleFileUpload"
                        />
                        <span>{{ $t('ticket.cert.upload') }}</span>
                    </div>
                </a>
                <span v-if="formData.jksFileName"><i class="devops-icon icon-check-circle"></i>{{ $t('ticket.cert.successfullyUpload') }}</span>
                <p :class="errors.has('jksFileName') ? 'error-tips' : 'normal-tips'">{{ $t('ticket.cert.filePlaceholer') }}</p>
            </div>
        </div>

        <!-- 证书密码凭据 start -->
        <div class="bk-form-item is-required cert-input-item">
            <label class="bk-label">{{ $t('ticket.cert.certPassword') }}：</label>
            <div class="bk-form-content">
                <selector
                    name="credentialId"
                    v-model="formData.credentialId"
                    v-validate="{ required: true }"
                    :list="credentialList"
                    :display-key="'credentialId'"
                    :setting-key="'credentialId'"
                    :searchable="true"
                    :search-key="'credentialId'"
                    :is-loading="isCredentialLoading"
                    :value="formData.credentialId"
                    :item-selected="selectCredentialId"
                    :class="{ 'is-danger': errors.has('credentialId') }"
                    :toggle-visible="refreshTicket"
                    has-add-item="true"
                    :item-text="$t('ticket.createCredential')"
                    :item-url="applyCreUrl"
                    style="width: 350px"
                >
                </selector>
                <p :class="errors.has('credentialId') ? 'error-tips' : 'normal-tips'">{{ $t('ticket.cert.passwordPlaceholer') }}</p>
            </div>
        </div>
        <!-- 证书密码凭据 end -->

        <!-- 证书别名 start -->
        <div class="bk-form-item is-required cert-input-item">
            <label class="bk-label">{{ $t('ticket.cert.certAlias') }}：</label>
            <div class="bk-form-content">
                <input
                    type="text"
                    class="bk-form-input"
                    name="alias"
                    v-validate="{ required: true }"
                    :placeholder="$t('ticket.cert.certAliasPlaceHolder')"
                    v-model="formData.alias"
                    :class="{ 'is-danger': errors.has('alias') }"
                >
                <p :class="errors.has('alias') ? 'error-tips' : 'normal-tips'">{{ $t('ticket.cert.aliasPlaceHolder') }}</p>
            </div>
        </div>
        <!-- 证书别名 end -->

        <!-- 别名密码 start -->
        <div class="bk-form-item is-required cert-input-item">
            <label class="bk-label">{{ $t('ticket.cert.aliasPassword') }}：</label>
            <div class="bk-form-content">
                <selector
                    name="aliasCredentialId"
                    v-validate="{ required: true }"
                    v-model="formData.aliasCredentialId"
                    :list="credentialList"
                    :display-key="'credentialId'"
                    :setting-key="'credentialId'"
                    :searchable="true"
                    :is-loading="isCredentialLoading"
                    :value="formData.aliasCredentialId"
                    :class="{ 'is-danger': errors.has('aliasCredentialId') }"
                    :toggle-visible="refreshTicket"
                    :item-selected="selectAliasCredentialId"
                    has-add-item="true"
                    :item-text="$t('ticket.createCredential')"
                    :item-url="applyCreUrl"
                    style="width: 350px"
                >
                </selector>
                <p :class="errors.has('aliasCredentialId') ? 'error-tips' : 'normal-tips'">{{ $t('ticket.cert.aliasPasswordPlaceholer') }}</p>
            </div>
        </div>
        <!-- 别名密码 end -->

        <!-- 描述 start -->
        <div class="bk-form-item cert-textarea-item">
            <label class="bk-label">{{ $t('ticket.cert.certRemark') }}：</label>
            <div class="bk-form-content">
                <textarea
                    class="bk-form-textarea"
                    :placeholder="$t('ticket.cert.certRemarkPlaceHolder')"
                    v-model="formData.remark"
                ></textarea>
            </div>
        </div>
    </section>
</template>

<script>
    import validMixin from './validMixin'
    import selector from '@/components/atomFormField/Selector'

    export default {
        components: {
            selector
        },
        mixins: [validMixin],
        props: {
            isEdit: Boolean,
            applyCreUrl: String,
            certData: Object
        },

        data () {
            return {
                formData: {
                    certId: '',
                    jksFileName: '',
                    fileJks: '',
                    credentialId: '',
                    alias: '',
                    aliasCredentialId: '',
                    remark: ''
                },
                credentialList: [],
                isCredentialLoading: false
            }
        },

        computed: {
            postData () {
                const data = new FormData()
                const keys = Object.keys(this.formData) || []

                keys.forEach((key) => {
                    if (key !== 'jksFileName' && key !== 'remark') {
                        const value = this.formData[key]
                        if (value) data.append(key, value)
                    }
                })
                if (this.formData.remark) data.append('certRemark', this.formData.remark)

                return data
            }
        },

        mounted () {
            this.initFormData()
        },

        methods: {
            initFormData () {
                if (!this.isEdit) return
                this.formData = this.certData
                this.refreshTicket(true)
            },

            refreshTicket (param) {
                this.isCredentialLoading = true
                this.$parent.refreshTicket(param)
                    .then(res => (this.credentialList = res || []))
                    .catch(() => {})
                    .finally(() => (this.isCredentialLoading = false))
            },

            handleFileUpload (e, _boolean) {
                const files = e.target.files || []
                if (!files.length) return

                const fileName = files[0].name
                // const ext = fileName.slice(fileName.lastIndexOf('.'))
                const validateKey = 'jksFileName'

                this.formData.jksFileName = fileName
                this.formData.fileJks = files[0]

                this.$validator.validate(validateKey)
            },

            selectCredentialId (value) {
                this.formData.credentialId = value
            },

            selectAliasCredentialId (value) {
                this.formData.aliasCredentialId = value
            }
        }
    }
</script>
