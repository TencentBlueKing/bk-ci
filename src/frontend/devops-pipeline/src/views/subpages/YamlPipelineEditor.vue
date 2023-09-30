<template>
    <div class="edit-pipeline-yaml">
        <header class="edit-pipeline-yaml-header">
            <bk-button
                @click="addPlugin"
            >
                {{$t('addPlugin')}}
            </bk-button>
        </header>
        <section class="edit-pipeline-yaml-editor">
            <YamlEditor
                ref="editor"
                show-yaml-plugin
                :value="pipelineYaml"
                @change="handleYamlChange"
                :highlight-ranges="yamlHighlightBlock"
                @step-click="handleStepClick"
            />
        </section>
        <template v-if="container">
            <atom-selector
                v-bind="editingElementPos"
                :container="container"
                :show-atom-yaml="showAtomYaml"
                :before-close="resetPreviewAtomYaml"
                :atom-yaml="atomYaml"
            />
        </template>
        <template v-if="editingElementPos">
            <template v-if="(typeof editingElementPos.elementIndex !== 'undefined')">
                <atom-property-panel
                    close-confirm
                    v-bind="editingElementPos"
                    :editable="pipelineEditable"
                    :stages="stages"
                    :is-instance-template="instanceFromTemplate"
                >
                    <footer slot="footer">
                        <bk-button
                            v-if="isUpdateElement"
                            :disabled="isUpdating"
                            :loading="isUpdating"
                            @click="syncModelToYaml"
                        >
                            {{ $t('applyToYaml') }}
                        </bk-button>
                        <bk-button
                            v-else
                            theme="primary"
                            :loading="isAdding"
                            :disabled="isAdding"
                            @click="confirmAdd"
                        >
                            {{ $t('add') }}
                        </bk-button>

                        <bk-button
                            @click="previewAtom"
                            :disabled="!canPreviewYaml"
                            :loading="isPreviewingAtomYAML"
                        >
                            {{ $t('previewYaml') }}
                        </bk-button>
                        <bk-button
                            :disabled="isAdding || isUpdating"
                            @click="cancelAdd"
                        >
                            {{ $t('cancel') }}
                        </bk-button>
                    </footer>
                </atom-property-panel>
            </template>
        </template>
    </div>
</template>

<script>
    import YamlEditor from '@/components/YamlEditor'
    import { isObject } from '@/utils/util'
    import { mapState, mapActions, mapGetters } from 'vuex'
    import AtomSelector from '@/components/AtomSelector'
    import AtomPropertyPanel from '@/components/AtomPropertyPanel'
    export default {
        components: {
            YamlEditor,
            AtomSelector,
            AtomPropertyPanel
        },
        props: {
            editable: {
                type: Boolean,
                default: true
            }
        },
        data () {
            return {
                showAtomYaml: false,
                atomYaml: '',
                isPreviewingAtomYAML: false,
                yamlHighlightBlock: [],
                isAdding: false,
                isUpdating: false,
                isUpdateElement: false
            }
        },
        computed: {
            ...mapState('atom', [
                'pipeline',
                'pipelineWithoutTrigger',
                'pipelineYaml',
                'pipelineSetting',
                'editingElementPos'
            ]),
            ...mapGetters('atom', [
                'getContainers',
                'getStage'
            ]),
            instanceFromTemplate () {
                return this.pipelineWithoutTrigger?.instanceFromTemplate ?? false
            },
            stages () {
                return this.pipelineWithoutTrigger?.stages ?? []
            },
            container () {
                if (isObject(this.editingElementPos)) {
                    const { stageIndex, containerIndex } = this.editingElementPos
                    const stage = this.getStageByIndex(stageIndex)
                    const containers = this.getContainers(stage)
                    return containers[containerIndex]
                }
                return null
            },
            element () {
                return this.container?.elements?.[this.editingElementPos?.elementIndex] ?? null
            },
            canPreviewYaml () {
                return this.element?.atomCode && !this.isPreviewingAtomYAML && !this.isAdding
            },
            pipelineEditable () {
                return this.editable && !this.pipelineWithoutTrigger.instanceFromTemplate
            }
        },
        watch: {
            editingElementPos (val) {
                if (!val) {
                    this.resetPreviewAtomYaml()
                }
            }
        },
        methods: {
            ...mapActions('atom', [
                'toggleAtomSelectorPopup',
                'togglePropertyPanel',
                'addAtom',
                'updateAtom',
                'setPipelineEditing',
                'yamlNavToPipelineModel',
                'previewAtomYAML',
                'insertAtomYAML',
                'transfertModelToYaml'
            ]),
            getStageByIndex (stageIndex) {
                const { getStage, pipelineWithoutTrigger } = this
                return getStage(pipelineWithoutTrigger.stages, stageIndex)
            },
            handleYamlChange (yaml) {
                this.$store.commit('atom/SET_PIPELINE_YAML', yaml)

                this.yamlHighlightBlock = []
                this.setPipelineEditing(true)
            },
            async confirmAdd () {
                try {
                    this.isAdding = true
                    const pos = this.$refs.editor.editor.getPosition()
                    this.toggleAtomSelectorPopup(false)
                    const { data } = await this.insertAtomYAML({
                        projectId: this.$route.params.projectId,
                        pipelineId: this.$route.params.pipelineId,
                        line: pos.lineNumber,
                        column: pos.column,
                        yaml: this.pipelineYaml,
                        data: this.element,
                        type: 'INSERT'
                    })
                    this.handleYamlChange(data.yaml)
                    this.yamlHighlightBlock = data.mark ? [data.mark] : []
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message
                    })
                } finally {
                    this.resetPreviewAtomYaml()
                    this.isAdding = false
                    this.togglePropertyPanel({
                        isShow: false
                    })
                }
            },
            async previewAtom () {
                if (this.isPreviewingAtomYAML) return
                try {
                    this.showAtomYaml = true
                    this.isPreviewingAtomYAML = true
                    const res = await this.previewAtomYAML({
                        projectId: this.$route.params.projectId,
                        pipelineId: this.$route.params.pipelineId,
                        ...this.element
                    })
                    this.toggleAtomSelectorPopup(true)
                    this.atomYaml = res.data
                } catch (error) {
                    console.error(error)
                } finally {
                    this.isPreviewingAtomYAML = false
                }
            },
            resetPreviewAtomYaml () {
                this.showAtomYaml = false
                this.atomYaml = ''
            },
            handleStepClick (editingElementPos, atom) {
                console.log(editingElementPos, atom)
                const { stageIndex, containerIndex, elementIndex } = editingElementPos
                const element = this.pipelineWithoutTrigger.stages[stageIndex].containers[containerIndex].elements[elementIndex]
                this.updateAtom({
                    element,
                    newParam: atom
                })
                this.isUpdateElement = true
                this.togglePropertyPanel({
                    isShow: true,
                    editingElementPos
                })
            },
            async addPlugin () {
                const pos = this.$refs.editor.editor.getPosition()
                this.yamlPos = pos
                const res = await this.yamlNavToPipelineModel({
                    projectId: this.$route.params.projectId,
                    line: pos.lineNumber,
                    column: pos.column,
                    body: this.pipelineYaml
                })
                const lastStageIndex = this.pipelineWithoutTrigger.stages.length - 1
                const lastContainerIndex = this.pipelineWithoutTrigger.stages[lastStageIndex].containers.length - 1
                const lastElemntIndex = this.pipelineWithoutTrigger.stages[lastStageIndex].containers[lastContainerIndex].elements.length - 1
                const container = this.pipelineWithoutTrigger.stages[res.stageIndex ?? lastStageIndex].containers[res.jobIndex ?? lastContainerIndex]
                this.addAtom({
                    stageIndex: res.stageIndex ?? lastStageIndex,
                    containerIndex: res.jobIndex ?? lastContainerIndex,
                    atomIndex: res.stepIndex ?? lastElemntIndex,
                    container
                })
            },
            cancelAdd () {
                this.togglePropertyPanel({
                    isShow: false
                })
                this.isUpdateElement = false
                this.toggleAtomSelectorPopup(false)
                this.resetPreviewAtomYaml()
            },
            async syncModelToYaml () {
                try {
                    this.isUpdating = true
                    const pipeline = Object.assign({}, this.pipeline, {
                        stages: [
                            this.pipeline.stages[0],
                            ...(this.pipelineWithoutTrigger?.stages ?? [])
                        ]
                    })
                    await this.transfertModelToYaml({
                        projectId: this.$route.params.projectId,
                        pipelineId: this.$route.params.pipelineId,
                        actionType: 'FULL_MODEL2YAML',
                        modelAndSetting: {
                            model: pipeline,
                            setting: this.pipelineSetting
                        },
                        oldYaml: this.pipelineYaml
                    })
                    this.togglePropertyPanel({
                        isShow: false
                    })
                    this.resetPreviewAtomYaml()
                    this.isUpdateElement = false
                } catch (error) {
                    console.log(error)
                } finally {
                    this.isUpdating = false
                }
                return false
            }
        }
    }
</script>

<style lang="scss">
    .edit-pipeline-yaml {
        display: flex;
        flex-direction: column;
        padding: 24px;
        height: 100%;
        overflow: hidden;
        grid-gap: 16px;
        &-header {
            display: flex;
            justify-content: flex-end;
            flex-shrink: 0;
        }

        &-editor {
            flex: 1;
        }
    }
</style>
