<template>
    <bk-dialog
        width="800"
        v-model="isSaveAsTemplateShow"
        :title="$t('newlist.saveAsTemp')"
        :close-icon="false"
        :mask-close="false"
        :auto-close="false"
        header-position="left"
        @confirm="submit"
        @cancel="cancel">
        <bk-form v-if="isSaveAsTemplateShow" v-bkloading="{ isLoading: isSubmiting }" :model="formModel">
            <bk-form-item
                v-for="item in formModel"
                :key="item.name"
                :desc="item.desc"
                :property="item.name"
                :rules="item.rules"
                :label="$t(`template.${item.label}`)"
            >
                <bk-input
                    v-if="item.name === 'templateName'"
                    v-model="model.templateName"
                    :placeholder="$t(`template.${item.placeholder}`)"
                />
                <bk-radio-group v-else v-model="model.isCopySetting">
                    <bk-radio
                        v-for="(entry, key) in optionGroups"
                        :key="key"
                        :value="entry.value"
                    >
                        {{ entry.label }}
                    </bk-radio>
                </bk-radio-group>
            </bk-form-item>
        </bk-form>
    </bk-dialog>
</template>

<script>
    import { mapActions } from 'vuex'
    export default {
        name: 'save-as-template-dialog',
        props: {
            isSaveAsTemplateShow: Boolean,
            pipeline: {
                type: Object,
                required: true
            }
        },
        data () {
            return {
                isSubmiting: false,
                model: {
                    templateName: '',
                    isCopySetting: false
                }
            }
        },
        computed: {
            optionGroups () {
                return [
                    { label: 'true', value: true },
                    { label: 'false', value: false }
                ]
            },
            formModel () {
                return [
                    {
                        name: 'templateName',
                        label: 'name',
                        placeholder: 'nameInputTips',
                        rules: [
                            {
                                required: true,
                                message: this.$t('template.nameInputTips')
                            },
                            {
                                max: 30,
                                message: this.$t('pipelineNameInputTips')
                            }
                        ]
                    },
                    {
                        name: 'applySetting',
                        label: 'applySetting',
                        desc: this.$t('template.tipsSetting')
                    }
                ]
            }
        },
        methods: {
            ...mapActions('pipelines', [
                'saveAsTemplate'
            ]),
            reset () {
                this.model = {
                    templateName: '',
                    applySetting: false
                }
            },
            async submit () {
                if (this.isSubmiting) return
                let message = this.$t('newlist.saveAsTempSuc')
                let theme = 'success'
                try {
                    this.isSubmiting = true
                    const { projectId, pipelineId } = this.pipeline
                    const postData = {
                        pipelineId,
                        ...this.model
                    }
                    const { id } = await this.saveAsTemplate({ projectId, postData })
                    this.cancel()

                    this.$router.push({
                        name: 'templateEdit',
                        params: { templateId: id }
                    })
                } catch (e) {
                    message = e.message || this.$t('newlist.saveAsTempFail')
                    theme = 'error'
                } finally {
                    this.$showTips({
                        message,
                        theme
                    })
                    this.isSubmiting = false
                }
            },
            cancel () {
                this.reset()
                this.$emit('cancel')
            }
        }
    }
</script>
