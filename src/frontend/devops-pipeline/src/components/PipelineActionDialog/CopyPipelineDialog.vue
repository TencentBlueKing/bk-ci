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
            <bk-form-item :label="$t('label')">
                <span class="pipeline-label-action-span">
                    <router-link target="_blank" :to="addLabelRoute" class="pipeline-label-action-span-btn">
                        <logo name="plus" size="18" />
                        {{$t('addLabel')}}
                    </router-link>
                    <span @click="refreshLabel" class="pipeline-label-action-span-btn">
                        <logo name="refresh" size="16" />
                        {{$t('editPage.atomForm.reflash')}}
                    </span>
                </span>
                <PipelineLabelSelector
                    ref="labelSelector"
                    v-model="initTags"
                    @change="updateDynamicGroup"
                />
            </bk-form-item>

            <bk-form-item :label="$t('dynamicPipelineGroup')">
                <bk-select
                    disabled
                    multiple
                    :value="dynamicGroup"
                    :loading="isMatching"
                    :placeholder="$t('pipelineDynamicMatchPlaceholder')"
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
    import { mapActions, mapGetters, mapState } from 'vuex'
    import piplineActionMixin from '@/mixins/pipeline-action-mixin'
    import PipelineLabelSelector from '@/components/PipelineLabelSelector'
    import Logo from '@/components/Logo'

    export default {
        name: 'copy-pipeline-dialog',
        components: {
            PipelineLabelSelector,
            Logo
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
                    name: this.initName(),
                    labels: [],
                    staticViews: [],
                    desc: ''
                }
            }
        },
        computed: {
            ...mapState('pipelines', [
                'allPipelineGroup'
            ]),
            ...mapGetters('pipelines', [
                'staticPipelineGroups',
                'dynamicPipelineGroups'
            ]),
            addLabelRoute () {
                return {
                    name: 'pipelinesGroup'

                }
            },
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
        created () {
            if (this.allPipelineGroup.length === 0) {
                this.requestGetGroupLists(this.$route.params)
            }
        },
        methods: {
            ...mapActions('pipelines', [
                'matchDynamicView',
                'requestGetGroupLists'
            ]),
            initName () {
                return this.pipeline?.pipelineName ? `${this.pipeline?.pipelineName}_copy` : ''
            },
            refreshLabel () {
                this.$refs?.labelSelector.init?.()
            },
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

                const res = await this.copy(this.model, this.pipeline)
                this.isSubmiting = false
                if (res) {
                    this.cancel()
                    this.$emit('done')
                }
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
    .pipeline-label-action-span {
        position: absolute;
        right: 0;
        top: -32px;
        display: grid;
        grid-gap: 20px;
        grid-template-columns: repeat(2, auto);
        .pipeline-label-action-span-btn {
            color: $primaryColor;
            position: relative;
            display: flex;
            align-items: center;
            cursor: pointer;
            >:first-child {
                margin-right: 6px;
            }
            &:first-child::before {
                content: '|';
                position: absolute;
                right: -13px;
                color: #DCDEE5;
            }
        }

    }
</style>
