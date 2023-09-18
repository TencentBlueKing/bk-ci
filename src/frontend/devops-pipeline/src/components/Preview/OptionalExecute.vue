<template>
    <div class="execute-detail-option" v-if="pipeline">

        <section v-if="canElementSkip" class="optional-execute-header">
            {{ $t('preview.atomToExec') }}
            <span>
                ({{ $t('preview.skipTipsPrefix') }}
                <span @click.stop="editTrigger" class="text-link item-title-tips-link">
                    {{ $t('preview.manualTrigger') }}
                </span>
                {{ $t('preview.skipTipsSuffix') }})
            </span>
            <bk-checkbox
                v-if="canElementSkip"
                v-model="checkTotal"
                @click.stop
            >
                {{ $t('preview.selectAll') }}/{{ $t('preview.selectNone') }}
            </bk-checkbox>
        </section>

        <div class="pipeline-optional-model">
            <pipeline
                is-preview
                :show-header="false"
                :pipeline="previewPipeline"
                :editable="false"
                :can-skip-element="canElementSkip"
            >
            </pipeline>
        </div>
    </div>
</template>

<script>
    import { mapGetters, mapActions } from 'vuex'
    import { deepClone } from '@/utils/util'
    import Pipeline from '@/components/Pipeline'

    export default {
        components: {
            Pipeline
        },
        props: {
            canElementSkip: Boolean,
            isDebug: Boolean,
            pipeline: {
                type: Object,
                required: true
            }
        },
        data () {
            return {
                previewPipeline: null,
                checkTotal: true
            }
        },
        computed: {
            ...mapGetters('atom', [
                'getAllElements'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            }
        },
        watch: {
            pipeline: {
                immediate: true,
                handler (newVal, oldVal) {
                    if (newVal) {
                        const pipeline = deepClone(newVal)
                        this.setPipelineSkipProp(pipeline.stages, this.checkTotal)
                        this.previewPipeline = Object.assign(pipeline, {
                            stages: newVal.stages.slice(1)
                        })
                    }
                }
            },
            checkTotal (checkedTotal) {
                this.setPipelineSkipProp(this.previewPipeline.stages, checkedTotal)
            },
            previewPipeline: {
                deep: true,
                handler (newVal) {
                    this.$nextTick(() => {
                        const skipedAtoms = this.getSkipedAtoms()
                        this.setSkipedAtomIds({
                            pipelineId: this.pipelineId,
                            skipedAtomIds: skipedAtoms
                        })
                    })
                }
            }
        },
        methods: {
            ...mapActions('pipelines', [
                'setSkipedAtomIds'
            ]),
            getSkipedAtoms () {
                const allElements = this.getAllElements(this.previewPipeline.stages)
                return allElements
                    .filter(element => !element.canElementSkip)
                    .map(element => `devops_container_condition_skip_atoms_${element.id}`)
            },
            setPipelineSkipProp (stages, checkedTotal) {
                stages.forEach(stage => {
                    const stageDisabled = stage.stageControlOption?.enable === false
                    this.$set(stage, 'runStage', !stageDisabled && checkedTotal)
                    stage.containers.forEach(container => {
                        const containerDisabled = container.jobControlOption?.enable === false
                        this.$set(container, 'runContainer', !containerDisabled && checkedTotal)
                        container.elements.forEach(element => {
                            const isSkipEle = element.additionalOptions?.enable === false || containerDisabled
                            this.$set(element, 'canElementSkip', !isSkipEle && checkedTotal)
                        })
                    })
                })
            },

            editTrigger () {
                const url = `${WEB_URL_PREFIX}/pipeline/${this.projectId}/${this.pipelineId}/edit#manualTrigger`
                window.open(url, '_blank')
            }
        }
    }
</script>

<style lang="scss">
    @import '../../scss/conf';
    @import '../../scss/mixins/ellipsis';

    .execute-detail-option {
        display: flex;
        flex-direction: column;
        height: 100%;
        padding: 24px 24px 0 24px;
        .optional-execute-header {
            font-size: 12px;
        }

        .pipeline-optional-model {
            flex: 1;
            overflow: auto;
        }
        .bkci-property-panel {
            .bk-sideslider-wrapper {
                top: 0;
                .bk-sideslider-title {
                    word-wrap: break-word;
                    word-break: break-all;
                    overflow: hidden;
                    padding: 0 0 0 20px !important;
                }
            }
        }
    }
</style>
