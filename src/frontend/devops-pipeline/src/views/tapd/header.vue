<template>
    <div class="tapd-pipeline-header">
        <div class="bread-prefix">
            <div class="back-wrap">
                <bk-button
                    variant="outline"
                    class="tapd-back-btn"
                    title="返回"
                    @click="onBack"
                >
                    <i class="devops-icon icon-arrows-left"></i>
                </bk-button>
            </div>
        </div>
        <component
            :is="tapdComponent"
        />
    </div>
</template>

<script>
    import DetailHeader from '../../components/PipelineHeader/DetailHeader.vue'
    import EditHeader from '../../components/PipelineHeader/EditHeader.vue'
    import HistoryHeader from '../../components/PipelineHeader/HistoryHeader.vue'
    import PreviewHeader from '../../components/PipelineHeader/PreviewHeader.vue'
    import bridge, { TAPD_EVENTS } from '@/utils/tapd-iframe-bridge'

    const COMPONENTS = {
        tapdPipelinesStdDetail: DetailHeader,
        tapdPipelinesEdit: EditHeader,
        tapdPipelinesHistory: HistoryHeader,
        tapdExecutePreview: PreviewHeader
    }

    export default {
        name: 'tapdPipelineHeader',
        computed: {
            tapdComponent () {
                return COMPONENTS[this.$route.name] || DetailHeader
            }
        },
        methods: {
            onBack () {
                if (this.$route.name === 'tapdPipelinesHistory') {
                    // 通知主站回到宿主页面
                    bridge.post(TAPD_EVENTS.BACK_TO_HOST)
                } else {
                    this.$router.back()
                }
            }
        }
    }
</script>

<style lang="scss">
    .tapd-pipeline-header {
        @import "@/scss/conf";
        width: 100%;

        .bread-prefix {
            position: absolute;
            top: 7px;
            left: 20px;
            z-index: 10;
            height: 32px;
            line-height: 32px;

            .tapd-back-btn {
                width: 32px;
                height: 32px;
                min-width: 32px;
                border-radius: 50%;

                .devops-icon {
                    position: absolute;
                    left: 8px;
                    top: 7px;
                    font-size: 16px;
                    font-weight: bold;
                }
            }
        }
        .pipeline-bread-crumb-aside .pipeline-bread-crumb {
            margin-left: 60px;

            .bk-breadcrumb-goback,
            .bk-breadcrumb-item:nth-child(2) {
                display: none;
            }
        }
    }
</style>
