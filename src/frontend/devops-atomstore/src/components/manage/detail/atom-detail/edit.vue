<template>
    <section v-bkloading="{ isLoading }">
        <bk-form :label-width="100" :model="formData" class="manage-detail-edit" ref="atomEdit" v-if="!isLoading">
            <bk-form-item :label="$t('store.名称')" :rules="[requireRule($t('store.名称')), nameRule, numMax(40)]" :required="true" property="name" error-display-type="normal">
                <bk-input v-model="formData.name" :placeholder="$t('store.请输入中英文名称，不超过40个字符')"></bk-input>
            </bk-form-item>
            <bk-form-item :label="$t('store.分类')" :rules="[requireRule($t('store.分类'))]" :required="true" property="classifyCode" error-display-type="normal">
                <bk-select v-model="formData.classifyCode" searchable :clearable="false" @toggle="requestAtomClassify" :loading="isLoadingClassify">
                    <bk-option v-for="(option, index) in sortList"
                        :key="index"
                        :id="option.classifyCode"
                        :name="option.classifyName">
                    </bk-option>
                </bk-select>
            </bk-form-item>
            <bk-form-item :label="$t('store.功能标签')" property="labelIdList">
                <bk-select :placeholder="$t('store.请选择功能标签')"
                    v-model="formData.labelIdList"
                    show-select-all
                    searchable
                    multiple
                    @toggle="requestAtomlabels"
                    :loading="isLoadingLabel"
                >
                    <bk-option v-for="(option, index) in labelList"
                        :key="index"
                        :id="option.id"
                        :name="option.labelName">
                    </bk-option>
                </bk-select>
            </bk-form-item>
            <bk-form-item :label="$t('store.简介')" :rules="[requireRule($t('store.简介')), numMax(256)]" :required="true" property="summary" :desc="$t('store.展示在插件市场以及流水线选择插件页面。')" error-display-type="normal">
                <bk-input v-model="formData.summary" :placeholder="$t('store.插件一句话简介，不超过256个字符')"></bk-input>
            </bk-form-item>
            <bk-form-item :label="$t('store.描述')"
                property="description"
                :desc="`${$t('store.atomRemark')}<br>${$t('store.展示在插件市场查看插件详情界面，帮助用户快速了解插件和解决遇到的问题。')}`">
                <mavon-editor class="remark-input"
                    ref="mdHook"
                    v-model="formData.description"
                    :toolbars="toolbars"
                    :external-link="false"
                    :box-shadow="false"
                    :language="mavenLang"
                    preview-background="#fff"
                    @imgAdd="addImage"
                />
            </bk-form-item>
            <bk-form-item :label="$t('store.发布者')" :rules="[requireRule($t('store.发布者'))]" :required="true" property="publisher" error-display-type="normal">
                <bk-select v-model="formData.publisher">
                    <bk-option v-for="publisher in publishersList" :key="publisher.id" :id="publisher.publisherCode" :name="publisher.publisherName"></bk-option>
                </bk-select>
            </bk-form-item>
            <bk-form-item :required="true" property="logoUrl" error-display-type="normal" class="edit-logo">
                <select-logo :form="formData" type="ATOM" :is-err="false" ref="logoUrlError"></select-logo>
            </bk-form-item>
            <bk-form-item>
                <bk-button theme="primary" @click="save" :loading="isSaving">{{ $t('store.保存') }}</bk-button>
                <bk-button :disabled="isSaving" @click="$router.back()">{{ $t('store.取消') }}</bk-button>
            </bk-form-item>
        </bk-form>
    </section>
</template>

<script>
    import selectLogo from '@/components/common/selectLogo'
    import { toolbars } from '@/utils/editor-options'

    export default {
        components: {
            selectLogo
        },

        props: {
            detail: Object
        },
        
        data () {
            return {
                formData: JSON.parse(JSON.stringify(this.detail)),
                sortList: [],
                labelList: [],
                isLoading: true,
                isLoadingClassify: false,
                isLoadingLabel: false,
                isSaving: false,
                toolbars,
                nameRule: {
                    validator: (val) => (/^[\u4e00-\u9fa5a-zA-Z0-9-_. ]+$/.test(val)),
                    message: this.$t('store.由汉字、英文字母、数字、连字符、下划线或点组成，不超过40个字符'),
                    trigger: 'blur'
                },
                publishersList: []
            }
        },

        computed: {
            userName () {
                return this.$store.state.user.username
            },
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
            Promise.all([this.requestAtomlabels(true), this.requestAtomClassify(true), this.fetchPublishersList(this.detail.atomCode)]).finally(() => {
                this.isLoading = false
            })
        },

        methods: {
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

            hackData () {
                this.formData.labelIdList = this.formData.labelList.map(label => label.id)
                this.formData.description = this.formData.description || `#### ${this.$t('store.插件功能')}\n\n#### ${this.$t('store.适用场景')}\n\n#### ${this.$t('store["使用限制和受限解决方案[可选]"]')}\n\n#### ${this.$t('store.常见的失败原因和解决方案')}`
            },

            save () {
                this.$refs.atomEdit.validate().then(() => {
                    this.isSaving = true
                    const { name, classifyCode, summary, description, logoUrl, iconData, publisher, labelIdList, privateReason } = this.formData
                    this.formData.labelList = this.labelList.filter((label) => (this.formData.labelIdList.includes(label.id)))
                    const putData = {
                        atomCode: this.detail.atomCode,
                        data: { name, classifyCode, summary, description, logoUrl, iconData, publisher, labelIdList, privateReason }
                    }
                    this.$store.dispatch('store/modifyAtomDetail', putData).then(() => {
                        this.$store.dispatch('store/clearDetail')
                        this.$store.dispatch('store/setDetail', this.formData)
                        this.hasChange = false
                        this.$router.back()
                    }).catch((err) => this.$bkMessage({ message: err.message || err, theme: 'error' })).finally(() => (this.isSaving = false))
                }, (validator) => {
                    this.$bkMessage({ message: validator.content || validator, theme: 'error' })
                })
            },

            requestAtomlabels (isOpen) {
                if (!isOpen) return
                this.isLoadingLabel = true
                return this.$store.dispatch('store/requestAtomLables').then((res) => (this.labelList = res || [])).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => (this.isLoadingLabel = false))
            },

            requestAtomClassify (isOpen) {
                if (!isOpen) return
                this.isLoadingClassify = true
                return this.$store.dispatch('store/requestAtomClassify').then((res) => {
                    this.sortList = res
                    this.sortList = this.sortList.filter(item => item.classifyCode !== 'trigger')
                }).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => (this.isLoadingClassify = false))
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
            },

            fetchPublishersList (atomCode) {
                this.$store.dispatch('store/getPublishersList', { atomCode }).then(res => {
                    this.publishersList = res
                    const result = this.publishersList.find(i => i.publisherCode === this.userName)
                    if (!result) {
                        this.publishersList.push({
                            publisherCode: this.userName,
                            publisherName: this.userName
                        })
                    }
                }).catch(() => [])
            }
        }
    }
</script>
