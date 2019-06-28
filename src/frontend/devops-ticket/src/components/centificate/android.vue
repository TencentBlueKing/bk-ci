<template>
    <section>
        <!-- 名称 start -->
        <div class="bk-form-item is-required cert-input-item">
            <label class="bk-label">证书名称：</label>
            <div class="bk-form-content">
                <input type="text"
                    class="bk-form-input"
                    name="androidId"
                    v-validate="{ required: true, regex: /^[a-zA-Z0-9\.\_]{1,100}$/ }"
                    placeholder="请输入证书名称"
                    v-model="formData.certId"
                    :disabled="isEdit"
                    :class="{ 'is-danger': errors.has('androidId') }"
                >
                <p :class="errors.has('androidId') ? 'error-tips' : 'normal-tips'">证书名称不能为空，只支持英文大小写、数字、下划线和英文句号</p>
            </div>
        </div>

        <!-- jks文件 start -->
        <div class="bk-form-item is-required cert-input-item">
            <label class="bk-label">JKS文件：</label>
            <div class="bk-form-content">
                <input type="text"
                    class="bk-form-input"
                    readonly
                    name="jksFileName"
                    v-validate="{ required: true }"
                    v-model="formData.jksFileName"
                    :class="{ 'is-danger': errors.has('jksFileName') }"
                    placeholder="还未选择文件"
                />
                <a href="javascript:void(0);" class="file-input" title="选择文件">
                    <div class="file-input-wrap">
                        <input type="file" class="file-input-btn" accept=".jks,.keystore" @change="handleFileUpload" />
                        <span>上传</span>
                    </div>
                </a>
                <span v-if="formData.jksFileName"><i class="bk-icon icon-check-circle"></i>上传成功</span>
                <p :class="errors.has('jksFileName') ? 'error-tips' : 'normal-tips'">请上传.jks或.keystore结尾的文件</p>
            </div>
        </div>

        <!-- 证书密码凭据 start -->
        <div class="bk-form-item is-required cert-input-item">
            <label class="bk-label">证书密码：</label>
            <div class="bk-form-content">
                <selector name="credentialId"
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
                    item-text="新增凭据"
                    :item-url="applyCreUrl"
                    style="width: 350px"
                >
                </selector>
                <p :class="errors.has('credentialId') ? 'error-tips' : 'normal-tips'">请选择和JKS证书密码关联的凭据</p>
            </div>
        </div>
        <!-- 证书密码凭据 end -->

        <!-- 证书别名 start -->
        <div class="bk-form-item is-required cert-input-item">
            <label class="bk-label">证书别名：</label>
            <div class="bk-form-content">
                <input type="text"
                    class="bk-form-input"
                    name="alias"
                    v-validate="{ required: true }"
                    placeholder="请输入证书别名"
                    v-model="formData.alias"
                    :class="{ 'is-danger': errors.has('alias') }"
                >
                <p :class="errors.has('alias') ? 'error-tips' : 'normal-tips'">证书别名不能为空，只支持英文大小写、数字、下划线和英文句号</p>
            </div>
        </div>
        <!-- 证书别名 end -->

        <!-- 别名密码 start -->
        <div class="bk-form-item is-required cert-input-item">
            <label class="bk-label">别名密码：</label>
            <div class="bk-form-content">
                <selector name="aliasCredentialId"
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
                    item-text="新增凭据"
                    :item-url="applyCreUrl"
                    style="width: 350px"
                >
                </selector>
                <p :class="errors.has('aliasCredentialId') ? 'error-tips' : 'normal-tips'">请选择和别名密码关联的凭据</p>
            </div>
        </div>
        <!-- 别名密码 end -->

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
