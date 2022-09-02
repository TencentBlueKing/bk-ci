<template>
    <bk-dialog
        width="800"
        v-model="isCopyDialogShow"
        :title="$t('newlist.copyPipeline')"
        :mask-close="false"
        :close-icon="false"
        :auto-close="false"
        header-position="left"
        @confirm="submit"
        @cancel="cancel"
    >
        <bk-form :model="model" v-bkloading="{ isLoading: isSubmiting }">
            <bk-form-item
                v-for="item in formModel"
                :key="item.name"
                :label="$t(item.name)"
                :rules="item.rules"
                :property="item.name"
            >
                <bk-input
                    :placeholder="$t(item.placeholder)"
                    :value="item.value"
                    @input="item.handleInput"
                />
            </bk-form-item>
        </bk-form>
    </bk-dialog>
</template>

<script>
    import piplineActionMixin from '@/mixins/pipeline-action-mixin'
    export default {
        name: 'copy-pipeline-dialog',
        mixins: [piplineActionMixin],
        props: {
            isCopyDialogShow: Boolean,
            pipeline: {
                type: Object,
                requied: true
            }
        },
        data () {
            return {
                isSubmiting: false,
                model: {
                    name: `${this.pipeline?.pipelineName}_copy`,
                    desc: ''
                }
            }
        },
        computed: {
            formModel () {
                return [
                    {
                        name: 'name',
                        placeholder: 'pipelineNameInputTips',
                        value: this.model.name,
                        rules: [
                            {
                                required: true,
                                message: this.$t('pipelineNameInputTips'),
                                trigger: 'blur'
                            },
                            {
                                max: 40,
                                message: this.$t('pipelineNameInputTips'),
                                trigger: 'blur'
                            }
                        ],
                        handleInput: (val) => {
                            this.model.name = val
                        }
                    },
                    {
                        name: 'desc',
                        placeholder: 'pipelineDescInputTips',
                        value: this.model.desc,
                        rules: [
                            {
                                max: 100,
                                message: this.$t('pipelineDescInputTips'),
                                trigger: 'blur'
                            }
                        ],
                        handleInput: (val) => {
                            this.model.desc = val
                        }
                    }
                ]
            }
        },
        watch: {
            'pipeline.pipelineId': function () {
                this.model.name = `${this.pipeline?.pipelineName}_copy`
            }
        },
        methods: {
            async submit () {
                this.isSubmiting = true

                this.copy(this.model, this.pipeline)
                this.isSubmiting = false
                this.cancel()
            },
            reset () {
                this.model.name = ''
                this.model.desc = ''
            },
            cancel () {
                this.reset()
                this.$emit('cancel')
            }
        }
    }
</script>

<style>

</style>
