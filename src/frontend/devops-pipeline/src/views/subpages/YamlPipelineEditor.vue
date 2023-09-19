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
                :value="pipelineYaml"
                @change="handleYamlChange"
            />
        </section>
        <template v-if="container">
            <atom-selector
                v-bind="editingElementPos"
                :container="container"
                :show-atom-yaml="showAtomYaml"
                :atom-yaml="atomYaml"
            />
        </template>
        <template v-if="editingElementPos">
            <template v-if="(typeof editingElementPos.elementIndex !== 'undefined')">
                <atom-property-panel
                    v-bind="editingElementPos"
                    :editable="pipelineEditable"
                    :stages="stages"
                    :is-instance-template="instanceFromTemplate"
                >
                    <footer slot="footer">
                        <bk-button
                            theme="primary"
                            @click="confirmAdd"
                        >
                            {{ $t('add') }}
                        </bk-button>
                        <bk-button
                            @click="previewAtom"
                            :disabled="isPreviewingAtomYAML"
                            :loading="isPreviewingAtomYAML"
                        >
                            {{ $t('预览YAML') }}
                        </bk-button>
                        <bk-button
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
                isPreviewingAtomYAML: false
            }
        },
        computed: {
            ...mapState('atom', [
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
                if (this.editingElementPos?.elementIndex) {
                    return this.container.elements[this.editingElementPos.elementIndex]
                }
                return null
            },
            pipelineEditable () {
                return this.editable && !this.pipelineWithoutTrigger.instanceFromTemplate
            }
        },
        methods: {
            ...mapActions('atom', [
                'toggleAtomSelectorPopup',
                'togglePropertyPanel',
                'addAtom',
                'setPipelineEditing',
                'yamlNavToPipelineModel',
                'previewAtomYAML'
            ]),
            getStageByIndex (stageIndex) {
                const { getStage, pipelineWithoutTrigger } = this
                return getStage(pipelineWithoutTrigger.stages, stageIndex)
            },
            handleYamlChange (yaml) {
                console.log(yaml)
                this.$store.commit('atom/SET_PIPELINE_YAML', yaml)
                this.setPipelineEditing(true)
            },
            confirmAdd () {
                console.log(this.pipelineWithoutTrigger, this.element)
                if (this.yamlPos) {
                    this.$refs.editor.editor.executeEdits(this.pipelineYaml, [{
                        range: this.$refs.editor.editor.getSelection(),
                        text: this.atomYaml
                    }])
                    this.$refs.editor.editor.getAction('editor.action.format').run()
                    this.atomYaml = ''
                    this.showAtomYaml = false
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
                    this.toggleAtomSelectorPopup()
                    this.atomYaml = res.data
                } catch (error) {

                } finally {
                    this.isPreviewingAtomYAML = false
                }
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
                console.log('getpos', pos, res)
                const container = this.pipelineWithoutTrigger.stages[res.stageIndex ?? lastStageIndex].containers[res.jobIndex ?? lastContainerIndex]
                this.addAtom({
                    stageIndex: res.stageIndex ?? lastStageIndex,
                    containerIndex: res.jobIndex ?? lastContainerIndex,
                    atomIndex: res.stepIndex ?? lastElemntIndex,
                    container
                })

                this.toggleAtomSelectorPopup()
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
