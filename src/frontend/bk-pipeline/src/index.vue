<template>
    <draggable v-model="computedStage" v-bind="dragOptions" :move="checkMove" class="bk-pipeline">
        <Stage
            class="list-item"
            v-for="(stage, index) in computedStage"
            :key="stage.id"
            :editable="editable"
            :stage="stage"
            :is-preview="isPreview"
            :is-exec-detail="isExecDetail"
            :can-skip-element="canSkipElement"
            :has-finally-stage="hasFinallyStage"
            :stage-index="index"
            :user-name="userName"
            :cancel-user-id="cancelUserId"
            :handle-change="updatePipeline"
            :is-latest-build="isLatestBuild"
            :stage-length="computedStage.length"
            :containers="stage.containers"
            :match-rules="matchRules"
            @[COPY_EVENT_NAME]="handleCopyStage"
            @[DELETE_EVENT_NAME]="handleDeleteStage"
        >
        </Stage>
    </draggable>
</template>

<script>
    import draggable from 'vuedraggable'
    import Stage from './Stage'
    import {
        eventBus,
        hashID
    } from './util'
    import {
        CLICK_EVENT_NAME,
        DELETE_EVENT_NAME,
        COPY_EVENT_NAME,
        ATOM_REVIEW_EVENT_NAME,
        ATOM_QUALITY_CHECK_EVENT_NAME,
        ATOM_CONTINUE_EVENT_NAME,
        ATOM_EXEC_EVENT_NAME,
        ATOM_ADD_EVENT_NAME,
        ADD_STAGE,
        STAGE_CHECK,
        STAGE_RETRY,
        DEBUG_CONTAINER
    } from './constants'

    const customEvents = [
        CLICK_EVENT_NAME,
        DELETE_EVENT_NAME,
        ATOM_REVIEW_EVENT_NAME,
        ATOM_CONTINUE_EVENT_NAME,
        ATOM_EXEC_EVENT_NAME,
        ATOM_QUALITY_CHECK_EVENT_NAME,
        ATOM_ADD_EVENT_NAME,
        ADD_STAGE,
        STAGE_CHECK,
        STAGE_RETRY,
        DEBUG_CONTAINER
    ]
    
    export default {
        components: {
            Stage,
            draggable
        },
        emits: customEvents,
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
            isLatestBuild: {
                type: Boolean,
                default: false
            },
            canSkipElement: {
                type: Boolean,
                default: false
            },
            pipeline: {
                type: Object,
                required: true
            },
            cancelUserId: {
                type: String,
                default: 'unknow'
            },
            userName: {
                type: String,
                default: 'unknow'
            },
            matchRules: {
                type: Array,
                default: () => []
            }
        },
        data () {
            return {
                DELETE_EVENT_NAME,
                COPY_EVENT_NAME
            }
        },
        computed: {
            computedStage: {
                get () {
                    return this.pipeline.stages
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
                        return stage
                    })
                    this.updatePipeline(this.pipeline, {
                        stages: data
                    })
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
            },
            hasFinallyStage () {
                try {
                    const stageLength = this.computedStage.length
                    const last = this.computedStage[stageLength - 1]
                    return last.finally
                } catch (error) {
                    return false
                }
            }
        },
        watch: {
            'pipeline.stages': {
                deep: true,
                handler: function () {
                    this.$emit('input', this.pipeline)
                    this.$emit('change', this.pipeline)
                }
            },
            'pipeline.name': {
                handler: function () {
                    this.$emit('input', this.pipeline)
                    this.$emit('change', this.pipeline)
                }
            }
        },
        mounted () {
            this.registeCustomEvent()
        },
        beforeDestroy () {
            window.showLinuxTipYet = false
            this.registeCustomEvent(true)
        },
        methods: {
            registeCustomEvent (destory = false) {
                customEvents.forEach(eventName => {
                    const fn = (destory ? eventBus.$off : eventBus.$on).bind(eventBus)
                    fn(eventName, (...args) => {
                        this.$emit(eventName, ...args)
                    })
                })
            },
            updatePipeline (model, params) {
                Object.assign(model, params)
            },
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
            },
            handleCopyStage ({ stageIndex, stage }) {
                this.pipeline.stages.splice(stageIndex + 1, 0, stage)
            },
            handleDeleteStage (stageIndex) {
                if (Number.isInteger(stageIndex)) {
                    this.pipeline.stages.splice(stageIndex, 1)
                }
            }
        }
    }
</script>

<style lang="scss">
    .bk-pipeline {
        display: flex;
        padding-right: 120px;
        width: fit-content;
        position: relative;
        align-items: flex-start;
        ul,
        li {
            margin: 0;
            padding: 0;
        }
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
