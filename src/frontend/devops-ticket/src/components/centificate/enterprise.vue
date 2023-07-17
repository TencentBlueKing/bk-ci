<template>
    <section>
        <!-- 证书名称 start -->
        <div class="bk-form-item is-required cert-input-item">
            <label class="bk-label">{{ $t('ticket.cert.certName') }}：</label>
            <div class="bk-form-content">
                <input type="text"
                    class="bk-form-input"
                    name="enterpriseId"
                    v-validate="{ required: isValidEnterprise, regex: /^[a-zA-Z0-9\.\_]{1,30}$/ }"
                    :placeholder="$t('ticket.cert.namePlaceholer')"
                    v-model="formData.certId"
                    :disabled="isEdit"
                    :class="{ 'is-danger': errors.has('enterpriseId') }"
                >
                <p :class="errors.has('enterpriseId') ? 'error-tips' : 'normal-tips'">{{ $t('ticket.cert.validateName') }}</p>
            </div>
        </div>

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

    export default {
        mixins: [validMixin],
        props: {
            isEdit: Boolean,
            certData: Object
        },

        data () {
            return {
                formData: {
                    certId: '',
                    mobileProvisionFileName: '',
                    fileMobileProvision: '',
                    remark: ''
                }
            }
        },

        computed: {
            postData () {
                const data = new FormData()
                const keys = Object.keys(this.formData) || []

                keys.forEach((key) => {
                    if (key !== 'mobileProvisionFileName' && key !== 'remark') {
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
            },

            handleFileUpload (e, _boolean) {
                const files = e.target.files || []
                if (!files.length) return

                const fileName = files[0].name
                // const ext = fileName.slice(fileName.lastIndexOf('.'))
                const validateKey = 'mobileProvisionFileName'

                this.formData.mobileProvisionFileName = fileName
                this.formData.fileMobileProvision = files[0]

                this.$validator.validate(validateKey)
            }
        }
    }
</script>
