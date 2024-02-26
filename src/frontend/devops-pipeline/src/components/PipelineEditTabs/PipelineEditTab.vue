<template>
    <pipeline class="edit-bk-pipeline" v-if="!isLoading" :pipeline="pipeline" :show-header="false"></pipeline>
</template>

<script>
    import Pipeline from '@/components/Pipeline'
    import { mapActions } from 'vuex'

    export default {
        name: 'pipeline-edit-tab',
        components: {
            Pipeline
        },
        props: {
            isLoading: Boolean,
            pipeline: Object
        },
        data () {
            return {
                showAtomYet: false
            }
        },
        computed: {
            pipelineId () {
                return this.$route.params.pipelineId
            }
        },
        watch: {
            pipelineId (newVal, oldVal) {
                if (newVal !== oldVal) {
                    this.requestInterceptAtom({
                        projectId: this.$route.params.projectId,
                        pipelineId: newVal
                    })
                }
            },
            pipeline (newVal, oldVal) {
                if (newVal?.stages) {
                    const { hash } = this.$route
                    const atomIndex = this.getAtomIndex(newVal.stages, hash)
                    if (!this.showAtomYet && typeof atomIndex !== 'undefined') {
                        this.togglePropertyPanel({
                            isShow: true,
                            editingElementPos: atomIndex
                        })
                        // 只在首次加载进入编辑页面下弹出手动触发弹窗
                        this.showAtomYet = true
                    }
                }
            }
        },
        methods: {
            ...mapActions('atom', [
                'togglePropertyPanel'
            ]),
            ...mapActions('common', [
                'requestInterceptAtom'
            ]),

            getAtomIndex (stages, hash) { // 新增
                let pos
                const keyword = hash.substr(1)
                stages.forEach((stage, sIndex) => {
                    stage.containers.forEach((container, cIndex) => {
                        container.elements.forEach((ele, eIndex) => {
                            if ([ele.id, ele['@type'], ele.atomCode].includes(keyword)) {
                                pos = {
                                    stageIndex: sIndex,
                                    containerIndex: cIndex,
                                    elementIndex: eIndex
                                }
                            }
                        })
                    })
                })
                return pos
            }
        }
    }
</script>

<style lang="scss">
    .edit-bk-pipeline {
        .bk-pipeline {
            padding-left: 50px;
        }
        .scroll-container {
            .scroll-wraper {
                padding: 0 0 0 6px;
            }
            &:before {
                top: 24px;
            }
        }
    }
</style>
