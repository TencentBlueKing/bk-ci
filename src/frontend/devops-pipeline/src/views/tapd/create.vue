<template>
    <div class="tapd-create-pipeline-wrapper">
        <div class="tapd-create-pipeline-header">
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
        </div>
        <div class="tapd-create-pipeline-body">
            <create-pipeline />
        </div>
    </div>
</template>

<script>
    import CreatePipeline from '@/views/CreatePipeline.vue'
    import bridge, { TAPD_EVENTS } from '@/utils/tapd-iframe-bridge'

    // 创建页对主站的固定业务标识：只需告诉主站"这是创建页"即可，无需 params/query
    const CREATE_PAGE_PAYLOAD = Object.freeze({ routeName: 'createPipeline' })

    export default {
        name: 'tapdCreatePipelineEntry',
        components: {
            CreatePipeline
        },
        watch: {
            // 监听自身路由变化 -> 主动通知主站
            $route (to) {
                this.syncRouteToParent(to)
            }
        },
        created () {
            this.bridgeUnsubs = []
            // 启动 postMessage 监听（幂等）
            bridge.start()
            // 订阅来自主站的事件
            this.bridgeUnsubs.push(
                bridge.on(TAPD_EVENTS.CLOSE, () => {
                    // 主站关闭了 drawer：做清理
                    this.$emit('bk-pipeline-close')
                })
            )
        },
        mounted () {
            // 通知主站：iframe 已就绪
            bridge.post(TAPD_EVENTS.READY, CREATE_PAGE_PAYLOAD)
            // 同步一次当前路由（用于主站首次打开时对齐标题/URL）
            this.syncRouteToParent(this.$route)
        },
        beforeDestroy () {
            if (this.bridgeUnsubs) {
                this.bridgeUnsubs.forEach(unsub => unsub && unsub())
                this.bridgeUnsubs = []
            }
        },
        methods: {
            onBack () {
                // 通知主站回到宿主页面，与执行历史页逻辑一致
                bridge.post(TAPD_EVENTS.BACK_TO_HOST)
            },
            syncRouteToParent (route) {
                if (!route || !route.name) return
                // 只告知主站这里是创建页，无须携带 params/query
                bridge.post(TAPD_EVENTS.ROUTE_CHANGE, CREATE_PAGE_PAYLOAD)
            }
        }
    }
</script>

<style lang="scss">
    @import "@/scss/conf";

    .tapd-create-pipeline-wrapper {
        width: 100%;
        height: 100%;
        display: flex;
        flex-direction: column;
        overflow: hidden;

        .tapd-create-pipeline-header {
            position: relative;
            width: 100%;
            height: 0;

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
        }

        .tapd-create-pipeline-body {
            flex: 1;
            overflow: hidden;
            display: flex;
            flex-direction: column;

            > .create-pipeline-page-wrapper {
                flex: 1;
            }
        }

        /* 与 header.vue 保持一致：隐藏内部面包屑的返回与首项 */
        .pipeline-bread-crumb-aside .pipeline-bread-crumb {
            margin-left: 60px;

            .bk-breadcrumb-goback,
            .bk-breadcrumb-item:nth-child(2) {
                display: none;
            }
        }
    }
</style>
