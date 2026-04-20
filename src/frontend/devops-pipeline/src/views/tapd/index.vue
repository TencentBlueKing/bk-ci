<template>
    <div>
        <pipeline-index></pipeline-index>
    </div>
</template>

<script>
    import PipelineIndex from '../subpages/index.vue'
    import bridge, { TAPD_EVENTS } from '@/utils/tapd-iframe-bridge'

    // 通用路由名 -> tapd 专属路由名 的映射
    const ROUTE_NAME_MAP = Object.freeze({
        pipelinesPreview: 'tapdExecutePreview',
        executePreview: 'tapdExecutePreview',
        pipelinesDetail: 'tapdPipelinesStdDetail',
        pipelinesEdit: 'tapdPipelinesEdit',
        pipelinesHistory: 'tapdPipelinesHistory'
    })

    // 受支持的路由名（iframe 内的 tapd 专属路由），用 Set 加速查找
    const SUPPORTED_ROUTE_NAMES = new Set(Object.values(ROUTE_NAME_MAP))

    /**
     * 从路由对象中提取主站需要的参数
     */
    function extractRoutePayload (route) {
        if (!route) return {}
        return {
            routeName: route.name,
            params: {
                projectId: route.params.projectId || '',
                pipelineId: route.params.pipelineId || '',
                buildNo: route.params.buildNo || ''
            },
            query: { ...(route.query || {}) }
        }
    }

    export default {
        name: 'tapdPipeline',
        components: {
            PipelineIndex
        },
        watch: {
            // 监听自身路由变化 -> 主动通知主站
            $route (to) {
                this.syncRouteToParent(to)
            }
        },
        created () {
            // 保存桥接服务返回的 unsubscribe 列表（非响应式，直接挂实例）
            this.bridgeUnsubs = []
            // 启动 postMessage 监听（幂等）
            bridge.start()
            // 订阅来自主站的事件
            this.bridgeUnsubs.push(
                bridge.on(TAPD_EVENTS.NAVIGATE, payload => {
                    this.handleParentNavigate(payload || {})
                }),
                bridge.on(TAPD_EVENTS.CLOSE, () => {
                    // 主站关闭了 drawer：做清理（例如关闭内部 dialog、停止轮询等）
                    this.$emit('bk-pipeline-close')
                })
            )
        },
        mounted () {
            // 通知主站：iframe 已就绪，可下发初始 navigate 指令
            bridge.post(TAPD_EVENTS.READY, extractRoutePayload(this.$route))
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
            /**
             * 主站下发 navigate：在 iframe 内部以 replace 方式切换路由
             * payload: { routeName, params, query }
             */
            handleParentNavigate (payload) {
                const targetName = ROUTE_NAME_MAP[payload.routeName] || payload.routeName
                if (!targetName || !SUPPORTED_ROUTE_NAMES.has(targetName)) {
                    return
                }
                // 若已在目标路由且参数一致，避免重复 replace 触发重渲染
                if (this.isSameRoute(this.$route, targetName, payload.params)) {
                    return
                }
                this.$router.replace({
                    name: targetName,
                    params: { ...(payload.params || {}) },
                    query: { ...(payload.query || {}) }
                }).catch(() => { /* 忽略重复导航异常 */ })
            },

            /**
             * 同名且同 params 则视为相同路由（query 宽松比较，避免过度敏感）
             */
            isSameRoute (current, name, params = {}) {
                if (!current || current.name !== name) return false
                const curParams = current.params || {}
                const keys = Object.keys(params)
                for (let i = 0; i < keys.length; i++) {
                    const k = keys[i]
                    if (String(params[k] || '') !== String(curParams[k] || '')) {
                        return false
                    }
                }
                return true
            },

            /**
             * 把当前路由同步给主站
             */
            syncRouteToParent (route) {
                if (!route || !route.name) return
                bridge.post(TAPD_EVENTS.ROUTE_CHANGE, extractRoutePayload(route))
            }
        },

        /**
         * 通用路由名 -> tapd 专属路由名 的映射，避免 iframe 内误跳通用路由
         */
        beforeRouteLeave (to, from, next) {
            if (!SUPPORTED_ROUTE_NAMES.has(from.name)) {
                return next()
            }
            const mappedName = ROUTE_NAME_MAP[to.name]
            if (mappedName && mappedName !== to.name) {
                // 先中断当前跳转，再 replace 到 tapd 专属路由
                next(false)
                this.$router.replace({
                    name: mappedName,
                    params: { ...to.params },
                    query: { ...(to.query || {}) }
                }).catch(() => { /* 忽略重复导航异常 */ })
                return
            }
            return next()
        }
    }
</script>
