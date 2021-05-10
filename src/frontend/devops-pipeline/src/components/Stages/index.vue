<template>
    <draggable v-model="computedStage" v-bind="dragOptions" :move="checkMove" class="devops-stage-list">
        <Stage
            class="list-item"
            v-for="(stage, index) in computedStage"
            :key="stage.id"
            :editable="editable"
            :stage="stage"
            :is-preview="isPreview"
            :is-exec-detail="isExecDetail"
            :can-skip-element="canSkipElement"
            :stage-index="index"
            :stage-length="computedStage.length"
            :containers="stage.containers">
        </Stage>
    </draggable>
</template>

<script>
    import { mapActions } from 'vuex'
    import Stage from './Stage'
    import { hashID } from '@/utils/util'
    export default {
        components: {
            Stage
        },
        props: {
            editable: {
                type: Boolean,
                default: true
            },
            isPreview: {
                type: Boolean,
                default: false
            },
            isExecDetail: {
                type: Boolean,
                default: false
            },
            canSkipElement: {
                type: Boolean,
                default: false
            },
            stages: {
                type: Array,
                default: []
            }
        },
        computed: {
            computedStage: {
                get () {
                    return this.stages
                },
                set (stages) {
                    const data = stages.map((stage, index) => {
                        const name = `stage-${index + 1}`
                        const id = `s-${hashID()}`
                        if (!stage.containers) { // container
                            return {
                                id,
                                name,
                                containers: [stage]
                            }
                        }

                        return {
                            id,
                            ...stage
                        }
                    })
                    this.setPipelineStage(data)
                    this.setPipelineEditing(true)
                }
            },
            dragOptions () {
                return {
                    group: 'pipeline-job',
                    ghostClass: 'sortable-ghost-atom',
                    chosenClass: 'sortable-chosen-atom',
                    animation: 130,
                    disabled: !this.editable
                }
            }
        },
        beforeDestroy () {
            window.showLinuxTipYet = false
        },
        methods: {
            ...mapActions('atom', ['setPipelineStage', 'setPipelineEditing']),
            checkMove (event) {
                const dragContext = event.draggedContext || {}
                const element = dragContext.element || {}
                const isTrigger = element.containers[0]['@type'] === 'trigger'
                const isFinally = element.finally === true

                const relatedContext = event.relatedContext || {}
                const relatedelement = relatedContext.element || {}
                const isRelatedTrigger = relatedelement['@type'] === 'trigger'
                const isTriggerStage = relatedelement.containers && relatedelement.containers[0]['@type'] === 'trigger'
                const isRelatedFinally = relatedelement.finally === true

                return !isTrigger && !isRelatedTrigger && !isTriggerStage && !isFinally && !isRelatedFinally
            }
        }
    }
</script>

<style lang="scss">
    @import 'Stage';
    .devops-stage-list {
        display: flex;
        padding-right: 120px;
        width: fit-content;
        position: relative;
        align-items: flex-start;
        padding-top: $StagepaddingTop;
    }

    .list-item {
        transition: transform .2s ease-out;
    }

    .list-enter, .list-leave-to
        /* .list-complete-leave-active for below version 2.1.8 */ {
        opacity: 0;
        transform: translateY(36px) scale(0, 1);
    }

    .list-leave-active {
        position: absolute !important;
    }

</style>
