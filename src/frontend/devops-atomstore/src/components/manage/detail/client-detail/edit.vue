<template>
    <section v-bkloading="{ isLoading }">
        <bk-form
            :label-width="100"
            :model="formData"
            class="manage-detail-edit"
            ref="clientEdit"
            :rules="rules"
            v-if="!isLoading"
        >
            <basic-info :basic-info.sync="formData" />
            
            <bk-form-item class="mt20">
                <bk-button
                    theme="primary"
                    @click="save"
                    :loading="isSaving"
                >
                    {{ $t('store.保存') }}
                </bk-button>
                <bk-button
                    :disabled="isSaving"
                    @click="$router.back()"
                >
                    {{ $t('store.取消') }}
                </bk-button>
            </bk-form-item>
        </bk-form>
    </section>
</template>

<script>
    import BasicInfo from '@/components/basicInfo.vue'

    export default {
        components: {
            BasicInfo
        },

        props: {
            detail: Object
        },
        
        data () {
            return {
                formData: this.initFormData(),
                isLoading: false,
                isSaving: false,
                rules: {
                    name: [
                        {
                            required: true,
                            pattern: /^(?!.*^\s)(?!.*\s$)[\u4e00-\u9fa5a-zA-Z0-9\-_.\s]{1,40}$/,
                            message: this.$t('由汉字、英文字母、数字、连字符(-)、下划线(_)或点(.)组成，不超过40个字符'),
                            trigger: 'blur',
                        },
                    ],
                    classifyCode: [
                        {
                            required: true,
                            message: this.$t('应用分类不能为空'),
                            trigger: 'change',
                        },
                    ],
                    logoUrl: [
                        {
                            required: true,
                            message: this.$t('Logo必填'),
                            trigger: 'blur',
                        },
                    ],
                    summary: [
                        {
                            required: true,
                            message: this.$t('应用简介不能为空'),
                            trigger: 'blur',
                        },
                    ]}
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

        methods: {
            initFormData () {
                const data = JSON.parse(JSON.stringify(this.detail))
                if (data.classify && !data.classifyCode) {
                    data.classifyCode = data.classify.classifyCode
                }
                return data
            },

            save () {
                this.$refs.clientEdit?.validate().then(() => {
                    this.isSaving = true
                    const { name, classifyCode, summary, description, logoUrl } = this.formData
                    const putData = {
                        storeCode: this.detail.storeCode,
                        params: { name, classifyCode, summary, description, logoUrl }
                    }
                    this.$store.dispatch('store/updateBasicInfo', putData).then(() => {
                        this.$store.dispatch('store/clearDetail')
                        this.$store.dispatch('store/setDetail', this.formData)
                        this.hasChange = false
                        this.$router.back()
                    }).catch((err) => this.$bkMessage({ message: err.message || err, theme: 'error' })).finally(() => (this.isSaving = false))
                }, (validator) => {
                    this.$bkMessage({ message: validator.content || validator, theme: 'error' })
                })
            }
        }
    }
</script>
