<template>
    <section>
        <!-- 证书名称 start -->
        <div class="bk-form-item is-required cert-input-item">
            <label class="bk-label">{{ $t('ticket.cert.certName') }}：</label>
            <div class="bk-form-content">
                <input
                    type="text"
                    class="bk-form-input"
                    name="sslTlsId"
                    v-validate="{ required: isValidSSL, regex: /^[a-zA-Z0-9\.\_]{1,100}$/ }"
                    :placeholder="$t('ticket.cert.namePlaceholer')"
                    v-model="formData.certId"
                    :disabled="isEdit"
                    :class="{ 'is-danger': errors.has('sslTlsId') }"
                >
                <p :class="errors.has('sslTlsId') ? 'error-tips' : 'normal-tips'">{{ $t('ticket.cert.validateName') }}</p>
            </div>
        </div>

        <!-- .crt start -->
        <div class="bk-form-item is-required cert-input-item">
            <label class="bk-label">{{ $t('ticket.cert.serverCertFile') }}：</label>
            <div class="bk-form-content">
                <input
                    type="text"
                    class="bk-form-input"
                    readonly
                    name="serverCrtFileName"
                    v-validate="{ required: isValidSSL }"
                    :placeholder="$t('ticket.cert.emptyFile')"
                    v-model="formData.serverCrtFileName"
                    :class="{ 'is-danger': errors.has('serverCrtFileName') }"
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
                            accept=".crt"
                            @change="handleFileUpload"
                        />
                        <span>{{ $t('ticket.cert.upload') }}</span>
                    </div>
                </a>
                <span v-if="formData.serverCrtFileName"><i class="devops-icon icon-check-circle"></i>{{ $t('ticket.cert.successfullyUpload') }}</span>
                <p :class="errors.has('serverCrtFileName') ? 'error-tips' : 'normal-tips'">{{ $t('ticket.cert.certFileRule') }}</p>
            </div>
        </div>
        <!-- .key start -->
        <div class="bk-form-item is-required cert-input-item">
            <label class="bk-label">{{ $t('ticket.cert.serverKeyFile') }}：</label>
            <div class="bk-form-content">
                <input
                    type="text"
                    class="bk-form-input"
                    readonly
                    name="serverKeyFileName"
                    v-validate="{ required: isValidSSL }"
                    :placeholder="$t('ticket.cert.emptyFile')"
                    v-model="formData.serverKeyFileName"
                    :class="{ 'is-danger': errors.has('serverKeyFileName') }"
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
                            accept=".key"
                            @change="handleFileUpload"
                        />
                        <span>{{ $t('ticket.cert.upload') }}</span>
                    </div>
                </a>
                <span v-if="formData.serverKeyFileName"><i class="devops-icon icon-check-circle"></i>{{ $t('ticket.cert.successfullyUpload') }}</span>
                <p :class="errors.has('serverKeyFileName') ? 'error-tips' : 'normal-tips'">{{ $t('ticket.cert.keyFileRule') }}</p>
            </div>
        </div>

        <!-- .crt start -->
        <div class="bk-form-item cert-input-item">
            <label class="bk-label">{{ $t('ticket.cert.userCertFile') }}：</label>
            <div class="bk-form-content">
                <input
                    type="text"
                    class="bk-form-input"
                    readonly
                    name="clientCrtFileName"
                    :placeholder="$t('ticket.cert.emptyFile')"
                    v-model="formData.clientCrtFileName"
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
                            accept=".crt"
                            @change="handleFileUpload($event, true)"
                        />
                        <span>{{ $t('ticket.cert.upload') }}</span>
                    </div>
                </a>
                <span v-if="formData.clientCrtFileName"><i class="devops-icon icon-check-circle"></i>{{ $t('ticket.cert.successfullyUpload') }}</span>
                <p class="normal-tips">{{ $t('ticket.cert.certFileRule') }}</p>
            </div>
        </div>
        <!-- .key start -->
        <div class="bk-form-item cert-input-item">
            <label class="bk-label">{{ $t('ticket.cert.userKeyFile') }}：</label>
            <div class="bk-form-content">
                <input
                    type="text"
                    class="bk-form-input"
                    readonly
                    name="clientKeyFileName"
                    :placeholder="$t('ticket.cert.emptyFile')"
                    v-model="formData.clientKeyFileName"
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
                            accept=".key"
                            @change="handleFileUpload($event, true)"
                        />
                        <span>{{ $t('ticket.cert.upload') }}</span>
                    </div>
                </a>
                <span v-if="formData.clientKeyFileName"><i class="devops-icon icon-check-circle"></i>{{ $t('ticket.cert.successfullyUpload') }}</span>
                <p class="normal-tips">{{ $t('ticket.cert.keyFileRule') }}</p>
            </div>
        </div>

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
                    serverCrtFileName: '',
                    serverCrt: '',
                    clientCrtFileName: '',
                    clientCrt: '',
                    clientKeyFileName: '',
                    clientKey: '',
                    serverKeyFileName: '',
                    serverKey: '',
                    remark: ''
                }
            }
        },

        computed: {
            postData () {
                const data = new FormData()
                const keys = Object.keys(this.formData) || []
                const unPostArry = ['clientCrtFileName', 'serverCrtFileName', 'clientKeyFileName', 'serverKeyFileName', 'remark']

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
            },

            handleFileUpload (e, _boolean) {
                const files = e.target.files || []
                if (!files.length) return

                const fileName = files[0].name
                const ext = fileName.slice(fileName.lastIndexOf('.'))
                let validateKey = ''

                switch (ext) {
                    case '.crt':
                        if (_boolean) {
                            this.formData.clientCrtFileName = fileName
                            this.formData.clientCrt = files[0]
                        } else {
                            this.formData.serverCrtFileName = fileName
                            this.formData.serverCrt = files[0]
                            validateKey = 'serverCrtFileName'
                        }
                        break
                    case '.key':
                        if (_boolean) {
                            this.formData.clientKeyFileName = fileName
                            this.formData.clientKey = files[0]
                        } else {
                            this.formData.serverKeyFileName = fileName
                            this.formData.serverKey = files[0]
                            validateKey = 'serverKeyFileName'
                        }
                        break
                }
                if (validateKey) {
                    this.$validator.validate(validateKey)
                }
            }
        }
    }
</script>
