<template>
    <draggable
        v-model="computedStages"
        v-bind="dragOptions"
        :move="checkMove"
        class="bk-pipeline"
    >
        <Stage
            class="list-item"
            v-for="(stage, index) in computedStages"
            :ref="stage.id"
            :key="stage.id"
            :editable="editable"
            :stage="stage"
            :is-preview="isPreview"
            :is-exec-detail="isExecDetail"
            :has-finally-stage="hasFinallyStage"
            :stage-index="index"
            :cancel-user-id="cancelUserId"
            :handle-change="updatePipeline"
            :is-latest-build="isLatestBuild"
            :stage-length="computedStages.length"
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
    import { eventBus, hashID, isTriggerContainer } from './util'

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
            currentExecCount: {
                type: Number,
                default: 1
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
        provide () {
            const reactiveData = {};
            [
                'currentExecCount',
                'isPreview',
                'userName',
                'matchRules',
                'editable',
                'isExecDetail',
                'isLatestBuild',
                'canSkipElement',
                'cancelUserId'
            ].forEach((key) => {
                Object.defineProperty(reactiveData, key, {
                    enumerable: true,
                    get: () => this[key]
                })
            })

            return {
                reactiveData
            }
        },
        data () {
            return {
                DELETE_EVENT_NAME,
                COPY_EVENT_NAME
            }
        },
        computed: {
            computedStages: {
                get () {
                    return this.pipeline?.stages?.map((stage) => Object.assign(stage, {
                        isTrigger: this.checkIsTriggerStage(stage)
                    })) ?? []
                },
                set (stages) {
                    const data = stages.map((stage, index) => {
                        const name = `stage-${index + 1}`
                        const id = `s-${hashID()}`
                        if (!stage.containers) {
                            // container
                            return {
                                id,
                                name,
                                containers: [stage]
                            }
                        }
                        return stage
                    })
                    this.updatePipeline(this.pipeline, {
                        stages: data.filter(stage => stage.containers.length)
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
                    const stageLength = this.computedStages.length
                    const last = this.computedStages[stageLength - 1]
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
                customEvents.forEach((eventName) => {
                    const fn = (destory ? eventBus.$off : eventBus.$on).bind(eventBus)
                    fn(eventName, (...args) => {
                        this.$emit(eventName, ...args)
                    })
                })
            },
            checkIsTriggerStage (stage) {
                try {
                    return isTriggerContainer(stage.containers[0])
                } catch (e) {
                    return false
                }
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

                const isTriggerStage = this.checkIsTriggerStage(relatedelement)
                const isRelatedFinally = relatedelement.finally === true

                return (
                    !isTrigger
                    && !isRelatedTrigger
                    && !isTriggerStage
                    && !isFinally
                    && !isRelatedFinally
                )
            },
            handleCopyStage ({ stageIndex, stage }) {
                this.pipeline.stages.splice(stageIndex + 1, 0, stage)
            },
            handleDeleteStage (stageId) {
                this.pipeline.stages = this.pipeline.stages.filter(stage => stage.id !== stageId)
            },
            expandPostAction (stageId, matrixId, containerId) {
                return new Promise((resolve, reject) => {
                    try {
                        let jobInstance = this.$refs?.[stageId]?.[0]?.$refs?.[containerId]?.[0]?.$refs?.jobBox
                        if (matrixId) {
                            jobInstance = this.$refs?.[stageId]?.[0]?.$refs?.[matrixId]?.[0]?.$refs?.jobBox?.$refs[containerId]?.[0]
                        }
                        console.log(jobInstance, 'jobInstance')
                        jobInstance?.$refs?.atomList?.expandPostAction?.()
                        this.$nextTick(() => {
                            resolve(true)
                        })
                    } catch (error) {
                        console.error(error)
                        resolve(false)
                    }
                })
            },
            expandMatrix (stageId, matrixId, containerId) {
                console.log('expandMatrix', stageId, matrixId, containerId)
                return new Promise((resolve, reject) => {
                    try {
                        const jobInstance = this.$refs?.[stageId]?.[0]?.$refs?.[matrixId]?.[0]?.$refs?.jobBox
                        jobInstance?.toggleMatrixOpen?.(true)
                        this.$nextTick(() => {
                            jobInstance?.$refs[containerId]?.[0]?.toggleShowAtom(true)
                            resolve(true)
                        })
                    } catch (error) {
                        console.error(error)
                        resolve(false)
                    }
                })
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
  transition: transform 0.2s ease-out;
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
