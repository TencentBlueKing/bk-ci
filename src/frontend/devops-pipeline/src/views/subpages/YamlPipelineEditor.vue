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
            <atom-selector :container="container" v-bind="editingElementPos" />
        </template>
        <template v-if="editingElementPos">
            <template v-if="(typeof editingElementPos.elementIndex !== 'undefined')">
                <atom-property-panel
                    :element-index="editingElementPos.elementIndex"
                    :container-index="editingElementPos.containerIndex"
                    :stage-index="editingElementPos.stageIndex"
                    editable
                    :stages="pipeline.stages"
                    :is-instance-template="pipeline.instanceFromTemplate"
                />
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
            }
        },
        methods: {
            ...mapActions('atom', [
                'toggleAtomSelectorPopup',
                'togglePropertyPanel',
                'addAtom',
                'setPipelineEditing'
            ]),
            getStageByIndex (stageIndex) {
                const { getStage, pipeline } = this
                return getStage(pipeline.stages, stageIndex)
            },
            handleYamlChange (yaml) {
                console.log(yaml)
                this.$store.commit('atom/SET_PIPELINE_YAML', yaml)
                this.setPipelineEditing(true)
            },
            addPlugin () {
                const pos = this.$refs.editor.editor.getPosition()
                console.log('getpos', pos)
                this.togglePropertyPanel({
                    isShow: true,
                    editingElementPos: {
                        stageIndex: 3,
                        containerIndex: 0,
                        elementIndex: 0
                    }
                })
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
