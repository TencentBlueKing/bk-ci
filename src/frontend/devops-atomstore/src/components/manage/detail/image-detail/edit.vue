<template>
    <section v-bkloading="{ isLoading }">
        <bk-form :label-width="100" :model="formData" class="manage-detail-edit" ref="editForm" v-if="!isLoading">
            <bk-form-item :label="$t('store.镜像名称')" :rules="[requireRule($t('store.镜像名称')), nameRule, numMax(20)]" :required="true" property="imageName" error-display-type="normal">
                <bk-input v-model="formData.imageName" :placeholder="$t('store.请输入中英文名称，不超过20个字符')"></bk-input>
            </bk-form-item>
            <bk-form-item :label="$t('store.分类')" :rules="[requireRule($t('store.分类'))]" :required="true" property="classifyCode" error-display-type="normal">
                <bk-select v-model="formData.classifyCode" searchable :clearable="false" @toggle="requestClassify" :loading="isLoadingClassify">
                    <bk-option v-for="(option, index) in classifys"
                        :key="index"
                        :id="option.classifyCode"
                        :name="option.classifyName">
                    </bk-option>
                </bk-select>
            </bk-form-item>
            <bk-form-item :label="$t('store.标签')" property="labelIdList">
                <bk-select :placeholder="$t('store.请选择功能标签')"
                    v-model="formData.labelIdList"
                    show-select-all
                    searchable
                    multiple
                    @toggle="requestLabels"
                    :loading="isLoadingLabel"
                >
                    <bk-option v-for="(option, index) in labelList"
                        :key="index"
                        :id="option.id"
                        :name="option.labelName">
                    </bk-option>
                </bk-select>
            </bk-form-item>
            <bk-form-item :label="$t('store.范畴')"
                property="category"
                :required="true"
                :rules="[requireRule($t('store.范畴'))]"
                ref="category"
                error-display-type="normal"
            >
                <bk-select v-model="formData.category"
                    searchable
                    @toggle="requestCategory"
                    :loading="isLoadingCategory"
                >
                    <bk-option v-for="(option, index) in categoryList"
                        :key="index"
                        :id="option.categoryCode"
                        :name="option.categoryName"
                        :placeholder="$t('store.请选择范畴')"
                    >
                    </bk-option>
                </bk-select>
            </bk-form-item>
            <bk-form-item :label="$t('store.简介')" :rules="[requireRule($t('store.功能标签')), numMax(70)]" :required="true" property="summary" error-display-type="normal">
                <bk-input v-model="formData.summary" :placeholder="$t('store.请输入简介')"></bk-input>
            </bk-form-item>
            <bk-form-item :label="$t('store.描述')" property="description">
                <mavon-editor class="remark-input"
                    ref="mdHook"
                    v-model="formData.description"
                    :toolbars="toolbars"
                    :external-link="false"
                    :box-shadow="false"
                    preview-background="#fff"
                    :language="mavenLang"
                    @imgAdd="addImage"
                />
            </bk-form-item>
            <bk-form-item label="Dockerfile" property="dockerFileContent" ref="dockerFileContent">
                <code-section :code.sync="formData.dockerFileContent" :cursor-blink-rate="530" :read-only="false" ref="codeEditor" />
            </bk-form-item>
            <bk-form-item :label="$t('store.发布者')" :rules="[requireRule($t('store.发布者'))]" :required="true" property="publisher" error-display-type="normal">
                <bk-input v-model="formData.publisher" :placeholder="$t('store.请输入')"></bk-input>
            </bk-form-item>
            <bk-form-item :required="true" property="logoUrl" error-display-type="normal" class="edit-logo">
                <select-logo :form="formData" type="IMAGE" :is-err="false" ref="logoUrlError"></select-logo>
            </bk-form-item>
            <bk-form-item>
                <bk-button theme="primary" @click="save" :loading="isSaving">{{ $t('store.保存') }}</bk-button>
                <bk-button :disabled="isSaving" @click="$router.back()">{{ $t('store.取消') }}</bk-button>
            </bk-form-item>
        </bk-form>
    </section>
</template>

<script>
    import { mapActions } from 'vuex'
    import { toolbars } from '@/utils/editor-options'
    import selectLogo from '@/components/common/selectLogo'
    import codeSection from '@/components/common/detailTab/codeSection'

    export default {
        components: {
            selectLogo,
            codeSection
        },

        props: {
            detail: Object
        },
        
        data () {
            return {
                formData: JSON.parse(JSON.stringify(this.detail)),
                classifys: [],
                labelList: [],
                categoryList: [],
                isLoadingClassify: false,
                isLoadingLabel: false,
                isLoadingCategory: false,
                isLoading: true,
                isSaving: false,
                toolbars,
                nameRule: {
                    validator: (val) => (/^[\u4e00-\u9fa5a-zA-Z0-9-]+$/.test(val)),
                    message: this.$t('store.由汉字、英文字母、数字、连字符(-)组成，长度小于20个字符'),
                    trigger: 'blur'
                }
            }
        },
        computed: {
            mavenLang () {
                return this.$i18n.locale === 'en-US' ? 'en' : this.$i18n.locale
            }
        },

        watch: {
            formData: {
                handler () {
                    this.hasChange = true
                },
                deep: true
            }
        },

        created () {
            this.hackData()
            Promise.all([this.requestLabels(true), this.requestClassify(true), this.requestCategory(true)]).finally(() => (this.isLoading = false))
        },

        methods: {
            ...mapActions('store', [
                'requestImageClassifys',
                'requestImageLabel',
                'requestImageCategorys',
                'requestUpdateImageInfo'
            ]),

            hackData () {
                this.formData.labelIdList = this.formData.labelList.map(label => label.id)
                this.formData.description = this.formData.description || this.$t('store.imageMdDesc')
            },

            requireRule (name) {
                return {
                    required: true,
                    message: this.$t('store.validateMessage', [name, this.$t('store.必填项')]),
                    trigger: 'blur'
                }
            },

            numMax (num) {
                return {
                    validator: (val = '') => (val.length <= num),
                    message: this.$t('store.validateNum', [num]),
                    trigger: 'blur'
                }
            },

            save () {
                this.$refs.editForm.validate().then(() => {
                    this.isSaving = true

                    const currentCategory = this.categoryList.find(x => x.categoryCode === this.formData.category) || {}
                    const currentClassify = this.classifys.find(x => x.classifyCode === this.formData.classifyCode) || {}
                    this.formData.dockerFileContent = this.$refs.codeEditor.getValue()
                    this.formData.categoryName = currentCategory.categoryName
                    this.formData.labelList = this.labelList.filter(x => this.formData.labelIdList.includes(x.id)) || []
                    this.formData.classifyName = currentClassify.classifyName
                    this.formData.classifyId = currentClassify.id

                    const postData = {
                        imageCode: this.formData.imageCode,
                        data: this.formData
                    }
                    this.requestUpdateImageInfo(postData).then(() => {
                        this.$bkMessage({ message: this.$t('store.修改成功'), theme: 'success' })
                        this.$store.dispatch('store/clearDetail')
                        this.$store.dispatch('store/setDetail', this.formData)
                        this.hasChange = false
                        this.$router.back()
                    }).catch((err) => this.$bkMessage({ message: err.message || err, theme: 'error' })).finally(() => {
                        this.isSaving = false
                    })
                }, (validator) => {
                    this.$bkMessage({ message: validator.content || validator, theme: 'error' })
                })
            },

            requestLabels (isOpen) {
                if (!isOpen) return
                this.isLoadingLabel = true
                return this.requestImageLabel().then((res) => (this.labelList = res || [])).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => (this.isLoadingLabel = false))
            },

            requestClassify (isOpen) {
                if (!isOpen) return
                this.isLoadingClassify = true
                return this.requestImageClassifys().then((res) => {
                    this.classifys = res || []
                }).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => (this.isLoadingClassify = false))
            },

            requestCategory (isOpen) {
                if (!isOpen) return
                this.isLoadingCategory = true
                return this.requestImageCategorys().then((res) => {
                    this.categoryList = res || []
                }).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => (this.isLoadingCategory = false))
            },

            addImage (pos, file) {
                this.uploadimg(pos, file)
            },

            async uploadimg (pos, file) {
                const formData = new FormData()
                const config = {
                    headers: {
                        'Content-Type': 'multipart/form-data'
                    }
                }
                let message, theme
                formData.append('file', file)

                try {
                    const res = await this.$store.dispatch('store/uploadFile', {
                        formData,
                        config
                    })

                    this.$refs.mdHook.$img2Url(pos, res)
                } catch (err) {
                    message = err.message ? err.message : err
                    theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                    this.$refs.mdHook.$refs.toolbar_left.$imgDel(pos)
                }
            }
        }
    }
</script>
