<template>
    <bk-dialog
        width="500"
        v-model="isCopyDialogShow"
        :ext-cls="`auto-height-dialog copy-pipeline-dialog${isResult ? ' copy-pipeline-result-mode' : ''}`"
        header-position="left"
        render-directive="if"
        :title="$t(isResult ? 'copyReleaseResultTitle' : 'newlist.copyPipeline')"
        :mask-close="false"
        :close-icon="false"
        :auto-close="false"
    >
        <div v-bkloading="{ isLoading: isSubmiting }">
            <div
                v-if="isResult"
                class="copy-pipeline-result"
            >
                <i class="bk-icon icon-check-circle-shape copy-pipeline-result-icon"></i>
                <span
                    class="copy-pipeline-result-name"
                    :title="result.name"
                >{{ result.name }}</span>
                <bk-button
                    text
                    theme="primary"
                    @click="goHistory"
                >
                    {{ $t('newlist.view') }}
                </bk-button>
            </div>
            <template v-else>
                <bk-form
                    :model="model"
                    form-type="vertical"
                    ref="copyForm"
                >
                    <bk-form-item :label="$t('copyMode')">
                        <div class="copy-mode-cards">
                            <div
                                v-for="mode in copyModes"
                                :key="mode.value"
                                :class="['copy-mode-card', { active: copyAsDraft === mode.value }]"
                                @click="copyAsDraft = mode.value"
                            >
                                <div class="copy-mode-card-title">
                                    <span class="copy-mode-radio"></span>
                                    <span>{{ $t(mode.label) }}</span>
                                </div>
                                <p class="copy-mode-desc">
                                    {{ $t(mode.desc) }}
                                </p>
                            </div>
                        </div>
                    </bk-form-item>
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
            </template>
        </div>
        <footer slot="footer">
            <template v-if="isResult">
                <bk-button
                    theme="primary"
                    @click="cancel"
                >
                    {{ $t('finishAction') }}
                </bk-button>
            </template>
            <template v-else>
                <bk-button
                    theme="primary"
                    :loading="isSubmiting"
                    @click="submit"
                >
                    {{ $t(copyAsDraft ? 'copyAsDraftMode' : 'copyReleaseMode') }}
                </bk-button>
                <bk-button
                    :disabled="isSubmiting"
                    @click="cancel"
                >
                    {{ $t('cancel') }}
                </bk-button>
            </template>
        </footer>
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
                copyAsDraft: true,
                result: null,
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
            isResult () {
                return this.result !== null
            },
            copyModes () {
                return [
                    { value: true, label: 'copyAsDraftMode', desc: 'copyAsDraftDesc' },
                    { value: false, label: 'copyReleaseMode', desc: 'copyReleaseDesc' }
                ]
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
                                max: 128,
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
            isCopyDialogShow (show) {
                if (show) {
                    this.resetCopyMode()
                }
            },
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
            resetCopyMode () {
                this.copyAsDraft = true
                this.result = null
            },
            async submit () {
                this.isSubmiting = true
                const newPipelineId = await this.copy({
                    ...this.model,
                    ...this.groupValue,
                    copyAsDraft: this.copyAsDraft
                }, this.pipeline)

                this.isSubmiting = false
                if (!newPipelineId) return

                this.requestGetGroupLists(this.$route.params)
                this.$emit('done')
                if (this.copyAsDraft) {
                    this.$showTips({
                        message: this.$t('copyAsDraftSuc'),
                        theme: 'success'
                    })
                    this.goEditPage(newPipelineId)
                } else {
                    this.result = {
                        id: newPipelineId,
                        name: this.model.name
                    }
                }
            },
            goEditPage (pipelineId) {
                this.cancel()
                this.$router.push({
                    name: 'pipelinesEdit',
                    params: {
                        projectId: this.$route.params.projectId,
                        pipelineId
                    }
                })
            },
            goHistory () {
                const { id } = this.result
                this.cancel()
                this.$router.push({
                    name: 'pipelinesHistory',
                    params: {
                        projectId: this.$route.params.projectId,
                        pipelineId: id,
                        type: 'history'
                    }
                })
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
                this.resetCopyMode()
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

    .copy-pipeline-dialog {
        .copy-mode-cards {
            display: flex;
        }
        .copy-mode-card {
            flex: 1;
            min-width: 0;
            padding: 10px;
            background: #fff;
            border: 1px solid $borderWeightColor;
            border-radius: 2px;
            cursor: pointer;
            transition: border-color .2s, background-color .2s;
            & + .copy-mode-card {
                margin-left: 12px;
            }
            &.active {
                border-color: $primaryColor;
                background: $primaryLightColor;
                .copy-mode-radio {
                    border-color: $primaryColor;
                    &::after {
                        content: '';
                        position: absolute;
                        top: 2px;
                        left: 2px;
                        width: 8px;
                        height: 8px;
                        border-radius: 50%;
                        background: $primaryColor;
                    }
                }
            }
            .copy-mode-card-title {
                line-height: 20px;
                display: flex;
                align-items: center;
                font-weight: 400;
                color: $fontWeightColor;
            }
            .copy-mode-radio {
                position: relative;
                flex-shrink: 0;
                width: 14px;
                height: 14px;
                margin-right: 8px;
                border: 1px solid $borderLightColor;
                border-radius: 50%;
                background: #fff;
            }
            .copy-mode-desc {
                margin-top: 4px;
                line-height: 20px;
                font-size: 12px;
                color: $fontColor;
            }
        }
        // 结果态：上沿不动，高度收缩到结果行
        &.copy-pipeline-result-mode.bk-dialog-wrapper.auto-height-dialog {
            .bk-dialog .bk-dialog-content {
                min-height: 0;
            }
        }
        .copy-pipeline-result {
            display: flex;
            align-items: center;
            .copy-pipeline-result-icon {
                flex-shrink: 0;
                margin-right: 8px;
                font-size: 20px;
                color: $iconSuccessColor;
            }
            .copy-pipeline-result-name {
                flex: 1;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
            }
        }
    }
    
        .auto-height-dialog .bk-form .bk-label .bk-label-text {
            font-weight: 600;
        }
</style>
