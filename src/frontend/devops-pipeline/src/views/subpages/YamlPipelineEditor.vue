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
            <YamlPreviewPopup v-if="showAtomYaml" @close="resetPreviewAtomYaml" :yaml="atomYaml" />
            <atom-selector
                v-else
                v-bind="editingElementPos"
                :container="container"
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
    import YamlPreviewPopup from '@/components/YamlPreviewPopup'
    export default {
        components: {
            YamlEditor,
            AtomSelector,
            AtomPropertyPanel,
            YamlPreviewPopup
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
                isUpdateElement: false,
                tempPos: {}
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
                'osList',
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
                    this.$nextTick(() => {
                        this.resetTempData()
                    })
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
                'addStage',
                'addContainer',
                'deleteAtom',
                'deleteStage'
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
                    this.tempPos = {}
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
            async atomModel2Yaml () {
                const { data } = await this.previewAtomYAML({
                    projectId: this.$route.params.projectId,
                    pipelineId: this.$route.params.pipelineId,
                    ...this.element
                })
                return data
            },
            async previewAtom () {
                if (this.isPreviewingAtomYAML) return
                try {
                    this.isPreviewingAtomYAML = true
                    const yaml = await this.atomModel2Yaml()
                    this.showAtomYaml = true
                    this.toggleAtomSelectorPopup(true)
                    this.atomYaml = yaml
                } catch (error) {
                    console.error(error)
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message
                    })
                } finally {
                    this.isPreviewingAtomYAML = false
                }
            },
            resetPreviewAtomYaml () {
                this.showAtomYaml = false
                this.atomYaml = ''
            },
            handleStepClick (editingElementPos, atom) {
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
            getInsertPos (stageIndex, containerIndex) {
                try {
                    const stages = this.pipelineWithoutTrigger?.stages
                    if (!Array.isArray(stages) || stages.length === 0) {
                        return null
                    }

                    const stage = stages[stageIndex ?? stages.length - 1]
                    const lastIndex = Array.isArray(stage?.containers) && stage?.containers?.length > 0 ? stage?.containers?.length - 1 : 0
                    return {
                        stageIndex: stageIndex ?? stages.length - 1,
                        containerIndex: containerIndex ?? lastIndex
                    }
                } catch (error) {
                    return null
                }
            },
            async addPlugin () {
                const pos = this.$refs.editor.editor.getPosition()
                const { data } = await this.yamlNavToPipelineModel({
                    projectId: this.$route.params.projectId,
                    line: pos.lineNumber,
                    column: pos.column,
                    body: this.pipelineYaml
                })
                let modelPos = this.getInsertPos(data.stageIndex, data.containerIndex)
                let container = this.pipelineWithoutTrigger?.stages[modelPos?.stageIndex]?.containers[modelPos?.containerIndex]

                let stepIndex = data.stepIndex ?? (container?.elements?.length ?? 0) - 1

                if (!container) {
                    this.addStage({
                        stageIndex: 0,
                        stages: this.pipelineWithoutTrigger?.stages
                    })
                    const containers = this.pipelineWithoutTrigger?.stages[0]?.containers
                    this.addContainer({
                        containers,
                        type: this.osList[2].value
                    })
                    modelPos = {
                        stageIndex: 0,
                        containerIndex: 0
                    }
                    container = this.pipelineWithoutTrigger?.stages[0]?.containers[0]
                    stepIndex = container.elements?.length - 1
                    this.tempPos.addStage = true
                }
                Object.assign(this.tempPos, modelPos, {
                    stepIndex
                })
                this.addAtom({
                    ...modelPos,
                    atomIndex: stepIndex,
                    container
                })
            },
            resetTempData () {
                if (this.tempPos.addStage) {
                    this.deleteStage({
                        stageIndex: this.tempPos.stageIndex
                    })
                } else if (Number.isInteger(this.tempPos.stepIndex)) {
                    this.deleteAtom({
                        container: this.pipelineWithoutTrigger?.stages[this.tempPos.stageIndex]?.containers[this.tempPos.containerIndex],
                        atomIndex: this.tempPos.stepIndex + 1
                    })
                }
                this.tempPos = {}
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
                    if (!this.atomYaml) {
                        this.atomYaml = await this.atomModel2Yaml()
                    }

                    this.$refs.editor.insertFragmentAtPos(this.atomYaml, this.editingElementPos)

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
            justify-content: flex-start;
            flex-shrink: 0;
        }

        &-editor {
            flex: 1;
        }
    }
</style>
