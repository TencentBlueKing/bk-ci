<template>
    <bk-dialog
        width="480"
        v-model="isCopyDialogShow"
        :title="$t('newlist.copyPipeline')"
        :mask-close="false"
        :close-icon="false"
        :auto-close="false"
        header-position="left"
        @confirm="submit"
        @cancel="cancel"
    >
        <bk-form :model="model" form-type="vertical" v-bkloading="{ isLoading: isSubmiting }" ver>
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
            <bk-form-item :label="$t('label')">
                <PipelineLabelSelector
                    v-model="initTags"
                    @change="updateDynamicGroup"
                />
            </bk-form-item>

            <bk-form-item :label="$t('dynamicPipelineGroup')">
                <bk-select
                    :value="dynamicGroup"
                    disabled
                    :loading="isMatching"
                    multiple
                >
                    <bk-option
                        v-for="group in dynamicPipelineGroups"
                        :key="group.id"
                        :id="group.id"
                        :name="group.name"
                    >
                    </bk-option>
                </bk-select>
            </bk-form-item>
            <bk-form-item :label="$t('staticPipelineGroup')">
                <bk-select
                    multiple
                    v-model="model.staticViews"
                >
                    <bk-option
                        v-for="group in staticPipelineGroups"
                        :key="group.id"
                        :id="group.id"
                        :name="group.name"
                    >
                    </bk-option>
                </bk-select>
            </bk-form-item>
        </bk-form>
    </bk-dialog>
</template>

<script>
    import { mapActions, mapGetters } from 'vuex'
    import piplineActionMixin from '@/mixins/pipeline-action-mixin'
    import PipelineLabelSelector from '@/components/PipelineLabelSelector'
    export default {
        name: 'copy-pipeline-dialog',
        components: {
            PipelineLabelSelector
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
                initTags: {},
                dynamicGroup: [],
                isMatching: false,
                model: {
                    name: `${this.pipeline?.pipelineName}_copy`,
                    labels: [],
                    staticViews: [],
                    desc: ''
                }
            }
        },
        computed: {
            ...mapGetters('pipelines', [
                'staticPipelineGroups',
                'dynamicPipelineGroups'
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
                                trigger: 'blur'
                            }
                        ],
                        handleInput: (val) => {
                            this.model.name = val
                        }
                    },
                    {
                        name: 'label',
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
            ...mapActions('pipelines', [
                'matchDynamicView'
            ]),
            async updateDynamicGroup (tags) {
                this.isMatching = true
                this.model.labels = Object.values(tags).flat()
                try {
                    const labels = Object.keys(tags).map(key => ({
                        groupId: key,
                        labelIds: tags[key]
                    }))

                    const { data } = await this.matchDynamicView({
                        projectId: this.$route.params.projectId,
                        pipelineName: this.model.name,
                        labels
                    })
                    this.dynamicGroup = data
                } catch (e) {
                    console.error(e)
                } finally {
                    this.isMatching = false
                }
            },
            async submit () {
                this.isSubmiting = true

                this.copy(this.model, this.pipeline)
                this.isSubmiting = false
                this.cancel()
                this.$emit('done')
            },
            reset () {
                this.initTags = {}
                this.dynamicGroup = []
                this.isMatching = false
                this.model = {
                    name: '',
                    labels: [],
                    staticViews: [],
                    desc: ''
                }
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
