<template>
    <div class="edit-pipeline-yaml">
        <header
            v-if="pipelineEditable"
            class="edit-pipeline-yaml-header"
        >
            <bk-button @click="addPlugin">
                {{ $t('addPlugin') }}
            </bk-button>
        </header>
        <section class="edit-pipeline-yaml-editor">
            <YamlEditor
                ref="editor"
                show-yaml-plugin
                :read-only="!pipelineEditable"
                :value="pipelineYaml"
                @change="handleYamlChange"
                :highlight-ranges="yamlHighlightBlock"
                @step-click="handleStepClick"
            />
        </section>
        <template v-if="container">
            <YamlPreviewPopup
                v-if="showAtomYaml"
                @close="resetPreviewAtomYaml"
                :yaml="atomYaml"
            />
            <atom-selector
                v-else
                v-bind="editingElementPos"
                :container="container"
            />
        </template>
        <template v-if="editingElementPos">
            <template v-if="(typeof editingElementPos.elementIndex !== 'undefined')">
                <atom-property-panel
                    v-bind="editingElementPos"
                    close-confirm
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
                            {{ $t('editPage.append') }}
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
    import AtomPropertyPanel from '@/components/AtomPropertyPanel'
    import AtomSelector from '@/components/AtomSelector'
    import YamlEditor from '@/components/YamlEditor'
    import YamlPreviewPopup from '@/components/YamlPreviewPopup'
    import { isObject } from '@/utils/util'
    import { mapActions, mapGetters, mapState } from 'vuex'
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
                tempPos: {},
                editingModel: null
            }
        },
        computed: {
            ...mapState('atom', [
                'pipeline',
                'pipelineWithoutTrigger',
                'pipelineYaml',
                'pipelineSetting',
                'editingElementPos',
                'isPipelineEditing'
            ]),
            ...mapGetters('atom', [
                'osList',
                'getContainers',
                'getStage'
            ]),
            instanceFromTemplate () {
                return this.editingModel?.instanceFromTemplate ?? false
            },
            stages () {
                return this.editingModel?.stages ?? []
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
                return this.editable && !this.instanceFromTemplate
            }
        },
        watch: {
            editingElementPos (val) {
                if (!val) {
                    this.resetPreviewAtomYaml()
                    this.isUpdateElement = false
                    this.$nextTick(() => {
                        this.resetTempData()
                    })
                }
            },
            pipelineWithoutTrigger: {
                immediate: true,
                handler (val) {
                    this.editingModel = val
                }
            }
        },

        methods: {
            ...mapActions('atom', [
                'toggleAtomSelectorPopup',
                'togglePropertyPanel',
                'addAtom',
                'updateAtomInput',
                'updateAtom',
                'setPipelineEditing',
                'yamlNavToPipelineModel',
                'transfer',
                'previewAtomYAML',
                'insertAtomYAML',
                'addStage',
                'addContainer',
                'deleteAtom',
                'deleteStage',
                'setPipelineYaml'
            ]),
            getStageByIndex (stageIndex) {
                const { getStage, editingModel } = this
                return getStage(editingModel.stages, stageIndex)
            },
            handleYamlChange (yaml) {
                this.setPipelineYaml(yaml)
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
            async handleStepClick (editingElementPos) {
                const { stageIndex, containerIndex, elementIndex } = editingElementPos
                try {
                    // TODO: 需要先把当前的yaml转化为Model,展示侧边栏
                    const model = await this.transfer({
                        ...this.$route.params,
                        actionType: 'FULL_YAML2MODEL',
                        oldYaml: this.pipelineYaml
                    })
                    this.editingModel = {
                        ...model.modelAndSetting.model,
                        stages: model.modelAndSetting.model.stages.slice(1)
                    }
                    const container = this.editingModel?.stages?.[stageIndex]?.containers?.[containerIndex]
                    const element = container?.elements?.[elementIndex]
                    if (!element) {
                        this.addAtom({
                            ...editingElementPos,
                            container,
                            atomIndex: elementIndex - 1
                        })
                        this.isUpdateElement = true
                        return
                    }
                    this.isUpdateElement = true
                    this.togglePropertyPanel({
                        isShow: true,
                        editingElementPos
                    })
                } catch (error) {
                    // TODO: 转换报错 Fallback
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message
                    })
                }
            },
            getInsertPos (stageIndex, containerIndex) {
                try {
                    const stages = this.editingModel?.stages
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
                let container = this.editingModel?.stages[modelPos?.stageIndex]?.containers[modelPos?.containerIndex]

                let stepIndex = data.stepIndex ?? (container?.elements?.length ?? 0) - 1

                if (!container) {
                    this.addStage({
                        stageIndex: 0,
                        stages: this.editingModel?.stages
                    })
                    const containers = this.editingModel?.stages[0]?.containers
                    this.addContainer({
                        containers,
                        type: this.osList[2].value
                    })
                    modelPos = {
                        stageIndex: 0,
                        containerIndex: 0
                    }
                    container = this.editingModel?.stages[0]?.containers[0]
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
                        container: this.editingModel?.stages[this.tempPos.stageIndex]?.containers[this.tempPos.containerIndex],
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
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message
                    })
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
