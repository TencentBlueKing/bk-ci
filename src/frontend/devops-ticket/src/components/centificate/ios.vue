<template>
    <section>
        <!-- 证书名称 start -->
        <div class="bk-form-item is-required cert-input-item">
            <label class="bk-label">{{ $t('ticket.cert.certName') }}：</label>
            <div class="bk-form-content">
                <input type="text"
                    class="bk-form-input"
                    name="iosId"
                    v-validate="{ required: true, regex: /^[a-zA-Z0-9\.\_]{1,30}$/ }"
                    :placeholder="$t('ticket.cert.namePlaceholer')"
                    v-model="formData.certId"
                    :disabled="isEdit"
                    :class="{ 'is-danger': errors.has('iosId') }"
                >
                <p :class="errors.has('iosId') ? 'error-tips' : 'normal-tips'">{{ $t('ticket.cert.validateName') }}</p>
            </div>
        </div>

        <!-- p12 start -->
        <div class="bk-form-item is-required cert-input-item">
            <label class="bk-label">{{ $t('ticket.cert.p12File') }}：</label>
            <div class="bk-form-content">
                <input type="text"
                    class="bk-form-input"
                    readonly
                    name="p12FileName"
                    v-validate="{ required: true }"
                    :placeholder="$t('ticket.cert.emptyFile')"
                    v-model="formData.p12FileName"
                    :class="{ 'is-danger': errors.has('p12FileName') }"
                />
                <a href="javascript:void(0);" class="file-input" :title="$t('ticket.cert.selectFile')">
                    <div class="file-input-wrap">
                        <input type="file" class="file-input-btn" accept=".p12" @change="handleFileUpload" />
                        <span>{{ $t('ticket.cert.upload') }}</span>
                    </div>
                </a>
                <span v-if="formData.p12FileName"><i class="devops-icon icon-check-circle"></i>{{ $t('ticket.cert.successfullyUpload') }}</span>
                <p :class="errors.has('p12FileName') ? 'error-tips' : 'normal-tips'">{{ $t('ticket.cert.p12FileRule') }}</p>
            </div>
        </div>

        <!-- 证书密码凭据 start -->
        <div class="bk-form-item cert-input-item">
            <label class="bk-label">{{ $t('ticket.cert.certPassword') }}：</label>
            <div class="bk-form-content">
                <selector :list="credentialList"
                    :display-key="'credentialId'"
                    :setting-key="'credentialId'"
                    :searchable="true"
                    :search-key="'credentialId'"
                    :is-loading="isCredentialLoading"
                    :value.sync="formData.credentialId"
                    :toggle-visible="refreshTicket"
                    :item-selected="selectIosCredentialId"
                    has-add-item="true"
                    :item-text="$t('ticket.createCredential')"
                    :item-url="applyCreUrl"
                    style="width: 350px"
                >
                </selector>
                <p class="normal-tips">{{ $t('ticket.cert.p12RelativeCred') }}</p>
            </div>
        </div>
        <!-- 证书密码凭据 end -->

        <!-- 描述文件 start -->
        <div class="bk-form-item is-required cert-input-item">
            <label class="bk-label">{{ $t('ticket.cert.remarkFile') }}：</label>
            <div class="bk-form-content">
                <input type="text"
                    class="bk-form-input"
                    readonly
                    name="mobileProvisionFileName"
                    v-validate="{ required: isValidEnterprise }"
                    v-model="formData.mobileProvisionFileName"
                    :class="{ 'is-danger': errors.has('mobileProvisionFileName') }"
                    :placeholder="$t('ticket.cert.emptyFile')"
                />
                <a href="javascript:void(0);" class="file-input" :title="$t('ticket.cert.selectFile')">
                    <div class="file-input-wrap">
                        <input type="file" class="file-input-btn" @change="handleFileUpload" accept=".mobileprovision" />
                        <span>{{ $t('ticket.cert.upload') }}</span>
                    </div>
                </a>
                <span v-if="formData.mobileProvisionFileName" style="position: relative; top: 2px;"><i class="devops-icon icon-check-circle"></i>{{ $t('ticket.cert.successfullyUpload') }}</span>
                <p :class="errors.has('mobileProvisionFileName') ? 'error-tips' : 'normal-tips'">{{ $t('ticket.cert.remarkFileRule') }}</p>
            </div>
        </div>

        <!-- 描述 start -->
        <div class="bk-form-item cert-textarea-item">
            <label class="bk-label">{{ $t('ticket.cert.certRemark') }}：</label>
            <div class="bk-form-content">
                <textarea class="bk-form-textarea" :placeholder="$t('ticket.cert.certRemarkPlaceHolder')" v-model="formData.remark"></textarea>
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
                    p12FileName: '',
                    fileP12: '',
                    credentialId: '',
                    mobileProvisionFileName: '',
                    fileMobileProvision: '',
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
                const unPostArry = ['p12FileName', 'mobileProvisionFileName', 'remark']

                keys.forEach((key) => {
                    const index = unPostArry.findIndex(x => x === key)
                    if (index === -1) {
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
                const ext = fileName.slice(fileName.lastIndexOf('.'))
                let validateKey = ''

                switch (ext) {
                    case '.p12':
                        this.formData.p12FileName = fileName
                        this.formData.fileP12 = e.target.files[0]
                        validateKey = 'p12FileName'
                        break
                    case '.mobileprovision':
                        this.formData.mobileProvisionFileName = fileName
                        this.formData.fileMobileProvision = e.target.files[0]
                        validateKey = 'mobileProvisionFileName'
                        break
                }

                this.$validator.validate(validateKey)
            },

            selectIosCredentialId (value) {
                this.formData.credentialId = value
            }
        }
    }
</script>
