<template>
    <section>
        <!-- 证书名称 start -->
        <div class="bk-form-item is-required cert-input-item">
            <label class="bk-label">证书名称：</label>
            <div class="bk-form-content">
                <input type="text"
                    class="bk-form-input"
                    name="sslTlsId"
                    v-validate="{ required: isValidSSL, regex: /^[a-zA-Z0-9\.\_]{1,100}$/ }"
                    placeholder="请输入证书名称"
                    v-model="formData.certId"
                    :disabled="isEdit"
                    :class="{ 'is-danger': errors.has('sslTlsId') }"
                >
                <p :class="errors.has('sslTlsId') ? 'error-tips' : 'normal-tips'">证书名称不能为空，只支持英文大小写、数字、下划线和英文句号</p>
            </div>
        </div>

        <!-- .crt start -->
        <div class="bk-form-item is-required cert-input-item">
            <label class="bk-label">服务端证书文件：</label>
            <div class="bk-form-content">
                <input type="text"
                    class="bk-form-input"
                    readonly
                    name="serverCrtFileName"
                    v-validate="{ required: isValidSSL }"
                    placeholder="还未选择文件"
                    v-model="formData.serverCrtFileName"
                    :class="{ 'is-danger': errors.has('serverCrtFileName') }"
                />
                <a href="javascript:void(0);" class="file-input" title="选择文件">
                    <div class="file-input-wrap">
                        <input type="file" class="file-input-btn" accept=".crt" @change="handleFileUpload" />
                        <span>上传</span>
                    </div>
                </a>
                <span v-if="formData.serverCrtFileName"><i class="bk-icon icon-check-circle"></i>上传成功</span>
                <p :class="errors.has('serverCrtFileName') ? 'error-tips' : 'normal-tips'">请上传.crt文件</p>
            </div>
        </div>
        <!-- .key start -->
        <div class="bk-form-item is-required cert-input-item">
            <label class="bk-label">服务端密钥文件：</label>
            <div class="bk-form-content">
                <input type="text"
                    class="bk-form-input"
                    readonly
                    name="serverKeyFileName"
                    v-validate="{ required: isValidSSL }"
                    placeholder="还未选择文件"
                    v-model="formData.serverKeyFileName"
                    :class="{ 'is-danger': errors.has('serverKeyFileName') }"
                />
                <a href="javascript:void(0);" class="file-input" title="选择文件">
                    <div class="file-input-wrap">
                        <input type="file" class="file-input-btn" accept=".key" @change="handleFileUpload" />
                        <span>上传</span>
                    </div>
                </a>
                <span v-if="formData.serverKeyFileName"><i class="bk-icon icon-check-circle"></i>上传成功</span>
                <p :class="errors.has('serverKeyFileName') ? 'error-tips' : 'normal-tips'">请上传.key文件</p>
            </div>
        </div>

        <!-- .crt start -->
        <div class="bk-form-item cert-input-item">
            <label class="bk-label">客户端证书文件：</label>
            <div class="bk-form-content">
                <input type="text"
                    class="bk-form-input"
                    readonly
                    name="clientCrtFileName"
                    placeholder="还未选择文件"
                    v-model="formData.clientCrtFileName"
                />
                <a href="javascript:void(0);" class="file-input" title="选择文件">
                    <div class="file-input-wrap">
                        <input type="file" class="file-input-btn" accept=".crt" @change="handleFileUpload($event, true)" />
                        <span>上传</span>
                    </div>
                </a>
                <span v-if="formData.clientCrtFileName"><i class="bk-icon icon-check-circle"></i>上传成功</span>
                <p class="normal-tips">请上传.crt文件</p>
            </div>
        </div>
        <!-- .key start -->
        <div class="bk-form-item cert-input-item">
            <label class="bk-label">客户端密钥文件：</label>
            <div class="bk-form-content">
                <input type="text"
                    class="bk-form-input"
                    readonly
                    name="clientKeyFileName"
                    placeholder="还未选择文件"
                    v-model="formData.clientKeyFileName"
                />
                <a href="javascript:void(0);" class="file-input" title="选择文件">
                    <div class="file-input-wrap">
                        <input type="file" class="file-input-btn" accept=".key" @change="handleFileUpload($event, true)" />
                        <span>上传</span>
                    </div>
                </a>
                <span v-if="formData.clientKeyFileName"><i class="bk-icon icon-check-circle"></i>上传成功</span>
                <p class="normal-tips">请上传.key文件</p>
            </div>
        </div>

        <!-- 描述 start -->
        <div class="bk-form-item cert-textarea-item">
            <label class="bk-label">证书描述：</label>
            <div class="bk-form-content">
                <textarea class="bk-form-textarea" placeholder="请输入证书描述" v-model="formData.remark"></textarea>
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
                const unPostArry = ['clientCrt', 'serverCrt', 'clientKey', 'serverKey', 'remark']

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
                            validateKey = 'clientCrtFileName'
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
                            validateKey = 'clientKeyFileName'
                        } else {
                            this.formData.serverKeyFileName = fileName
                            this.formData.serverKey = files[0]
                            validateKey = 'serverKeyFileName'
                        }
                        break
                }

                this.$validator.validate(validateKey)
            }
        }
    }
</script>
