<template>
    <pipeline v-if="!isLoading" :pipeline="pipeline" :is-editing="isEditing" :show-header="false"></pipeline>
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
            isEditing: Boolean,
            isLoading: Boolean,
            pipeline: Object
        },
        data () {
            return {
                showAtomYet: false,
                showLinkAtomYet: false
            }
        },
        watch: {
            pipeline: {
                deep: true,
                handler (newVal, oldVal) {
                    this.isLoading = false
                    if (this.isPipelineIdDiff) { // 如果是切换了pipeline，无需置为编辑状态
                        this.requestInterceptAtom()
                        this.isPipelineIdDiff = false
                        return
                    }
                    if (newVal && newVal.stages) {
                        let { hash } = this.$route
                        const linkAtomIndex = this.getLinkAtomIndex(newVal.stages, hash)
                        if (hash === '#codecc') {
                            hash = 'linuxPaasCodeCCScript'
                        } else {
                            hash = hash.substr(1)
                        }
                        if (!this.showAtomYet) {
                            const atomIndex = this.getAtomIndex(newVal.stages, hash)
                            atomIndex && this.togglePropertyPanel({
                                isShow: true,
                                editingElementPos: atomIndex
                            })
                            // 只在首次加载进入编辑页面下弹出手动触发弹窗
                            this.showAtomYet = true
                        }
                        if (newVal && linkAtomIndex && !this.showLinkAtomYet) {
                            this.togglePropertyPanel({
                                isShow: true,
                                editingElementPos: linkAtomIndex
                            })
                            // 只在首次加载进入编辑页面下弹出來弹窗
                            this.showLinkAtomYet = true
                        }
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
            getLinkAtomIndex (stages, hash) { // 新增
                let index = null
                const atomId = hash.substr(1)
                stages.forEach((stage, sIndex) => {
                    stage.containers.forEach((container, cIndex) => {
                        container.elements.forEach((ele, eIndex) => {
                            if (ele.id === atomId) {
                                index = {
                                    stageIndex: sIndex,
                                    containerIndex: cIndex,
                                    elementIndex: eIndex
                                }
                            }
                        })
                    })
                })
                return index
            },
            getAtomIndex (stages, atomName) {
                let index = null
                stages.forEach((stage, sIndex) => {
                    stage.containers.forEach((container, cIndex) => {
                        container.elements.forEach((ele, eIndex) => {
                            if (ele['@type'] === atomName || ele.atomCode === atomName) {
                                index = {
                                    stageIndex: sIndex,
                                    containerIndex: cIndex,
                                    elementIndex: eIndex
                                }
                            }
                        })
                    })
                })
                return index
            }
        }
    }
</script>
