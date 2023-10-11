<template>
    <div>
        <pipeline-index v-if="!loadPreview"></pipeline-index>
        <preview v-if="loadPreview" v-show="false"></preview>
    </div>
</template>

<script>
    import { bus } from '@/utils/bus'
    import PipelineIndex from '../subpages/index.vue'
    import Preview from '../subpages/preview.vue'

    export default {
        name: 'ftpPipeline',
        components: {
            PipelineIndex,
            Preview
        },
        data () {
            return {
                loadPreview: false
            }
        },
        computed: {
            ftpComponent () {
                return PipelineIndex
            }
        },
        beforeRouteLeave (to, from, next) {
            if (from.name === 'ftpPipelinesDetail' || from.name === 'ftpPipelinesEdit' || from.name === 'ftpPipelinesHistory') {
                if (to.name === 'pipelinesPreview') {
                    this.loadPreview = true
                    // 先加载preview页面，待500ms后执行start-execute方法
                    setTimeout(() => {
                        bus.$emit('start-execute')
                        setTimeout(() => {
                            this.loadPreview = false
                        }, 500)
                    }, 500)
                } else if (to.name === 'pipelinesDetail') {
                    this.$router.push({
                        name: 'ftpPipelinesDetail',
                        params: {
                            ...to.params
                        }
                    })
                    setTimeout(() => {
                        this.$forceUpdate()
                    }, 100)
                } else if (to.name === 'pipelinesEdit') {
                    this.$router.push({
                        name: 'ftpPipelinesEdit',
                        params: {
                            ...to.params
                        }
                    })
                    setTimeout(() => {
                        this.$forceUpdate()
                    }, 100)
                } else if (to.name === 'pipelinesHistory') {
                    this.$router.push({
                        name: 'ftpPipelinesHistory',
                        params: {
                            ...to.params
                        }
                    })
                    setTimeout(() => {
                        this.$forceUpdate()
                    }, 100)
                } else {
                    next()
                }
            }
        }
    }
</script>
