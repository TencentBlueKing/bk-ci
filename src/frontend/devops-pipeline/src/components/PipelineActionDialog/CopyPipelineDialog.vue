<template>
    <bk-dialog
        width="480"
        v-model="isCopyDialogShow"
        ext-cls="auto-height-dialog"
        header-position="left"
        render-directive="if"
        :title="$t('newlist.copyPipeline')"
        :mask-close="false"
        :close-icon="false"
        :auto-close="false"
        :loading="isSubmiting"
        @confirm="submit"
        @cancel="cancel"
    >
        <bk-form :model="model" form-type="vertical" v-bkloading="{ isLoading: isSubmiting }" ref="copyForm">
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
        <PipelineGroupSelector
            class="pipeline-group-selector-form"
            ref="pipelineGroupSelector"
            v-model="groupValue"
            :has-manage-permission="isManage"
            :pipeline-name="model.name"
        />
    </bk-dialog>
</template>

<script>
    import piplineActionMixin from '@/mixins/pipeline-action-mixin'
    import PipelineGroupSelector from './PipelineGroupSelector'
    import { mapActions, mapState } from 'vuex'

    export default {
        name: 'copy-pipeline-dialog',
        components: {
            PipelineGroupSelector
        },
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
                    name: this.initName(),
                    desc: ''
                },
                groupValue: {
                    labels: [],
                    staticViews: []
                }
            }
        },
        computed: {
            ...mapState('pipelines', [
                'isManage'
            ]),
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
                                trigger: 'change'
                            }
                        ],
                        handleInput: (val) => {
                            this.model.name = val
                        }
                    },
                    {
                        name: 'desc',
                        placeholder: 'desc',
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
                if (this.isCopyDialogShow) {
                    this.model.name = this.initName()
                    this.$nextTick(() => {
                        this.$refs.copyForm?.validate()
                    })
                }
            }
        },
        methods: {
            ...mapActions('pipelines', [
                'requestGetGroupLists'
            ]),
            initName () {
                return this.pipeline?.pipelineName ? `${this.pipeline?.pipelineName}_copy` : ''
            },
            async submit () {
                this.isSubmiting = true
                const res = await this.copy({
                    ...this.model,
                    ...this.groupValue
                }, this.pipeline)
                
                this.isSubmiting = false
                if (res) {
                    this.requestGetGroupLists(this.$route.params)
                    this.cancel()
                    this.$emit('done')
                }
            },
            reset () {
                this.model = {
                    name: '',
                    desc: ''
                }
                this.groupValue = {
                    labels: [],
                    staticViews: []
                }
                this.$refs.pipelineGroupSelector?.reset?.()
                this.$refs.copyForm?.clearError?.()
            },
            cancel () {
                this.reset()
                this.$emit('cancel')
            }
        }
    }
</script>

<style lang="scss">
    @import '@/scss/conf';
    .pipeline-group-selector-form {
        margin-top: 8px;
    }

</style>
