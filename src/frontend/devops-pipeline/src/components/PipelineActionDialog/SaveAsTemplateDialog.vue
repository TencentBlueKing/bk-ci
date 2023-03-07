<template>
    <bk-dialog
        ext-cls="bk-devops-center-align-dialog"
        width="600"
        header-position="left"
        render-directive="if"
        v-model="isSaveAsTemplateShow"
        :title="$t('newlist.saveAsTemp')"
        :close-icon="false"
        :mask-close="false"
        :auto-close="false"
        :draggable="false"
        @confirm="submit"
        @cancel="cancel"
    >
        <bk-form
            v-if="isSaveAsTemplateShow"
            v-bkloading="{ isLoading: isSubmiting }"
            :model="formModel"
            label-width="120"
        >
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
                        class="template-copy-setting-radio"
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
                    { label: this.$t('true'), value: true },
                    { label: this.$t('false'), value: false }
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
            ...mapActions('pipelines', ['saveAsTemplate']),
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
                    const { pipelineId } = this.pipeline
                    const { projectId } = this.$route.params
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

<style lang="scss">
.template-copy-setting-radio {
  margin-right: 12px;
}
</style>
