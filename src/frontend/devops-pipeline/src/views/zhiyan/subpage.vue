<template>
    <component
        :is="zyComponent"
        @on-pipeline-delegate="onPipelineDelegate"
        @on-pipeline-update="onPipelineUpdate"
    >
        <component
            :is="bkComponent"
            ref="pipeline"
            v-bind="$attrs"
            v-on="$listeners"
        />
    </component>
</template>

<script>
    import PipelineEdit from '../subpages/edit.vue'
    import PipelinePreview from '../subpages/preview.vue'
    import PipelineExecDetail from '../subpages/exec_detail.vue'

    const COMPONENTS = {
        zyPipelinesEdit: PipelineEdit,
        zyPipelinesDetail: PipelineExecDetail,
        zyPipelinesPreview: PipelinePreview
    }

    const ZY_NAMES = {
        zyPipelinesEdit: 'DevopsEditWrap',
        zyPipelinesDetail: 'DevopsExecDetailWrap',
        zyPipelinesPreview: 'DevopsPreviewWrap'
    }

    export default {
        name: 'ZyPipelineSubpage',
        inject: ['getZyComponent'],
        computed: {
            zyComponent () {
                return this.getZyComponent(ZY_NAMES[this.$route.name])
            },
            bkComponent () {
                return COMPONENTS[this.$route.name]
            }
        },
        methods: {
            onPipelineDelegate (action, ...args) {
                if (!this.$refs.pipeline) {
                    return
                }

                this.$refs.pipeline[action](...args)
            },
            onPipelineUpdate (name, value) {
                if (!this.$refs.pipeline) {
                    return
                }

                this.$refs.pipeline[name] = value
            }
        }
    }
</script>
