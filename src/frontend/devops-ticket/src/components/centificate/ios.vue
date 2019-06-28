<template>
    <section>
        <!-- 证书名称 start -->
        <div class="bk-form-item is-required cert-input-item">
            <label class="bk-label">证书名称：</label>
            <div class="bk-form-content">
                <input type="text"
                    class="bk-form-input"
                    name="iosId"
                    v-validate="{ required: true, regex: /^[a-zA-Z0-9\.\_]{1,100}$/ }"
                    placeholder="请输入证书名称"
                    v-model="formData.certId"
                    :disabled="isEdit"
                    :class="{ 'is-danger': errors.has('iosId') }"
                >
                <p :class="errors.has('iosId') ? 'error-tips' : 'normal-tips'">证书名称不能为空，只支持英文大小写、数字、下划线和英文句号</p>
            </div>
        </div>

        <!-- p12 start -->
        <div class="bk-form-item is-required cert-input-item">
            <label class="bk-label">P12文件：</label>
            <div class="bk-form-content">
                <input type="text"
                    class="bk-form-input"
                    readonly
                    name="p12FileName"
                    v-validate="{ required: true }"
                    placeholder="还未选择文件"
                    v-model="formData.p12FileName"
                    :class="{ 'is-danger': errors.has('p12FileName') }"
                />
                <a href="javascript:void(0);" class="file-input" title="选择文件">
                    <div class="file-input-wrap">
                        <input type="file" class="file-input-btn" accept=".p12" @change="handleFileUpload" />
                        <span>上传</span>
                    </div>
                </a>
                <span v-if="formData.p12FileName"><i class="bk-icon icon-check-circle"></i>上传成功</span>
                <p :class="errors.has('p12FileName') ? 'error-tips' : 'normal-tips'">请上传.p12结尾的p12文件</p>
            </div>
        </div>

        <!-- 证书密码凭据 start -->
        <div class="bk-form-item cert-input-item">
            <label class="bk-label">证书密码：</label>
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
                    item-text="新增凭据"
                    :item-url="applyCreUrl"
                    style="width: 350px"
                >
                </selector>
                <p class="normal-tips">请选择和P12证书密码关联的凭据</p>
            </div>
        </div>
        <!-- 证书密码凭据 end -->

        <!-- 描述文件 start -->
        <div class="bk-form-item is-required cert-input-item">
            <label class="bk-label">描述文件：</label>
            <div class="bk-form-content">
                <input type="text"
                    class="bk-form-input"
                    readonly
                    name="mobileProvisionFileName"
                    v-validate="{ required: true }"
                    v-model="formData.mobileProvisionFileName"
                    :class="{ 'is-danger': errors.has('mobileProvisionFileName') }"
                    placeholder="还未选择文件"
                />
                <a href="javascript:void(0);" class="file-input" title="选择文件">
                    <div class="file-input-wrap">
                        <input type="file" class="file-input-btn" @change="handleFileUpload" accept=".mobileprovision" />
                        <span>上传</span>
                    </div>
                </a>
                <span v-if="formData.mobileProvisionFileName"><i class="bk-icon icon-check-circle"></i>上传成功</span>
                <p :class="errors.has('mobileProvisionFileName') ? 'error-tips' : 'normal-tips'">请上传.mobileprovision结尾的描述文件</p>
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
