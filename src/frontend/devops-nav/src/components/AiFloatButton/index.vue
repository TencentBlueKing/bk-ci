<template>
    <div
        v-if="showAiEntry"
        :class="floatWrapperClasses"
        :style="floatWrapperStyle"
    >
        <transition name="ai-panel-slide">
            <div
                v-if="panelVisible"
                class="ai-panel"
                v-bk-clickoutside="handleClickOutside"
            >
                <div
                    v-if="iframeLoading"
                    class="ai-panel-loading"
                >
                    <div class="ai-loading-spinner" />
                    <span class="ai-loading-text">AI 助手加载中…</span>
                </div>
                <iframe
                    v-if="iframeSrc"
                    ref="iframeRef"
                    :class="['ai-panel-iframe', { 'ai-panel-iframe-hidden': iframeLoading }]"
                    :src="iframeSrc"
                    frameborder="0"
                    allow="clipboard-write"
                    @load="handleIframeLoad"
                />
            </div>
        </transition>
        <button
            v-if="!panelVisible"
            class="ai-float-hit-zone"
            type="button"
            aria-label="Open AI assistant"
            @click="handleFloatButtonClick"
            @pointerdown="handleDragStart"
            @lostpointercapture="handleLostPointerCapture"
        >
            <span
                ref="floatIconRef"
                class="ai-float-btn"
                aria-hidden="true"
            >
                <img
                    :src="aiblukeingBanner"
                    alt=""
                />
            </span>
        </button>
    </div>
</template>

<script setup lang="ts">
    import aiblukeingBanner from '@/assets/images/avatar.png'
    import { useStore } from '@/store'
    import cookie from 'js-cookie'
    import { AI_IFRAME_EVENTS } from '@/utils/constants'
    import eventBus from '@/utils/eventBus'
    import request from '@/utils/request'
    import {
        computed,
        getCurrentInstance,
        onBeforeUnmount,
        onMounted,
        ref,
        watch,
    } from 'vue'

    /**
     * AI 上下文通过 `postMessage` 推送给 AI iframe。每个浏览器标签页都有自己的
     * iframe，所以消息天然按标签页隔离，避免了之前 `localStorage` 跨标签页污染
     * 的问题。
     *
     * 所有 `window.message` 监听与跨 iframe 协议都集中在 `utils/iframeUtil.ts`
     * 中维护，本组件只通过 `eventBus` 接收解码后的事件，并通过 `iframeUtil`
     * 暴露的 `syncAiContext` 助手发送消息。
     */
    const panelVisible = ref(false)
    const iframeLoading = ref(true)
    const iframeRef = ref<HTMLIFrameElement | null>(null)
    const floatIconRef = ref<HTMLElement | null>(null)
    const aiEligibleProjectCodes = ref<Set<string>>(new Set())
    const aiProjectsFetchDone = ref(false)
    const showAiEntry = ref(false)
    const store = useStore()

    type FloatSnapEdge = 'left' | 'right' | 'top' | 'bottom' | null

    const FLOAT_BUTTON_SIZE = 48
    const FLOAT_EDGE_VISIBLE_SIZE = 24
    const FLOAT_EDGE_SNAP_DISTANCE = 16
    const FLOAT_DRAG_THRESHOLD = 4
    const hasDragged = ref(false)
    const isDragging = ref(false)
    const floatPosition = ref<{ left: number; top: number } | null>(null)
    const floatSnapEdge = ref<FloatSnapEdge>(null)
    let activePointerId: number | null = null
    let dragStartX = 0
    let dragStartY = 0
    let dragOffsetX = 0
    let dragOffsetY = 0
    let dragStarted = false
    let dragOriginPosition = { left: 0, top: 0 }
    let activeDragTarget: HTMLElement | null = null
    let ignoreNextClick = false

    /** Latest context reported by the inner pipeline iframe in this tab. */
    const subAppContext = ref<{ projectId?: string; pipelineId?: string; buildId?: string }>({})

    const iframeSrc = computed(() => {
        return `${window.PUBLIC_URL_PREFIX || ''}/ai/`
    })

    const floatWrapperClasses = computed(() => {
        return [
            'ai-float-wrapper',
            {
                'ai-float-wrapper-dragged': hasDragged.value,
                'ai-float-wrapper-dragging': isDragging.value,
                [`ai-float-wrapper-snapped-${floatSnapEdge.value}`]: !!floatSnapEdge.value,
            },
        ]
    })

    const floatWrapperStyle = computed(() => {
        if (!hasDragged.value || !floatPosition.value) {
            return {}
        }
        return {
            left: `${floatPosition.value.left}px`,
            top: `${floatPosition.value.top}px`,
        }
    })

    const instance = getCurrentInstance()
    const root = instance?.proxy as {
        $route?: {
            params?: Record<string, string>
        }
        iframeUtil?: {
            syncAiContext: (target: Window | null | undefined, ctx: Record<string, string>) => void
        }
    } | undefined

    function getCurrentProjectCode () {
        const navProjectId = String(
            root?.$route?.params?.projectId
            || root?.$route?.params?.projectCode
            || '',
        )
        const subProjectId = String(subAppContext.value.projectId || '')
        const cookieProjectId = cookie.get(X_DEVOPS_PROJECT_ID)
        return navProjectId || subProjectId || cookieProjectId
    }

    function getCurrentProjectName (): string {
        const code = getCurrentProjectCode()
        if (!code) return ''
        const list = store.state.projectList as Project[] | null | undefined
        const hit = list?.find(p => p.projectCode === code)
        return hit?.projectName != null && hit.projectName !== '' ? String(hit.projectName) : ''
    }

    function syncShowAiEntry () {
        if (!aiProjectsFetchDone.value) {
            showAiEntry.value = false
            return
        }
        const code = getCurrentProjectCode()
        showAiEntry.value = !!code && aiEligibleProjectCodes.value.has(code)
    }

    async function loadAiEligibleProjects () {
        try {
            const res: string[] = await request.get(`ai/api/user/ai/projects`)
            aiEligibleProjectCodes.value = new Set(res)
        } catch (e) {
            console.warn('[AiFloatButton] failed to load AI-enabled projects', e)
            aiEligibleProjectCodes.value = new Set()
        } finally {
            aiProjectsFetchDone.value = true
            syncShowAiEntry()
        }
    }

    function getMergedContext () {
        const projectId = getCurrentProjectCode()
        const subProjectId = String(subAppContext.value.projectId || '')
        const sameProject = !!projectId && (!subProjectId || subProjectId === projectId)
        return {
            projectId,
            projectName: getCurrentProjectName(),
            pipelineId: sameProject ? String(subAppContext.value.pipelineId || '') : '',
            buildId: sameProject ? String(subAppContext.value.buildId || '') : '',
        }
    }

    function postContextToAi () {
        root?.iframeUtil?.syncAiContext(iframeRef.value?.contentWindow, getMergedContext())
    }

    function togglePanel () {
        if (!panelVisible.value) {
            iframeLoading.value = true
        }
        panelVisible.value = !panelVisible.value
    }

    function handleFloatButtonClick (event: MouseEvent) {
        if (ignoreNextClick) {
            event.preventDefault()
            ignoreNextClick = false
            return
        }
        togglePanel()
    }

    function getViewportSize () {
        return {
            width: window.innerWidth,
            height: window.innerHeight,
        }
    }

    function clamp (value: number, min: number, max: number) {
        return Math.min(Math.max(value, min), Math.max(min, max))
    }

    function getClampedFloatPosition (left: number, top: number) {
        const { width, height } = getViewportSize()
        return {
            left: clamp(left, 0, width - FLOAT_BUTTON_SIZE),
            top: clamp(top, 0, height - FLOAT_BUTTON_SIZE),
        }
    }

    function getSnappedFloatPosition (left: number, top: number) {
        const { width, height } = getViewportSize()
        const maxLeft = Math.max(0, width - FLOAT_BUTTON_SIZE)
        const maxTop = Math.max(0, height - FLOAT_BUTTON_SIZE)
        const position = getClampedFloatPosition(left, top)
        const snapOptions: Array<{ edge: NonNullable<FloatSnapEdge>; distance: number }> = [
            { edge: 'left', distance: position.left },
            { edge: 'right', distance: maxLeft - position.left },
            { edge: 'top', distance: position.top },
            { edge: 'bottom', distance: maxTop - position.top },
        ]
        const snapEdge = snapOptions
            .filter(item => item.distance <= FLOAT_EDGE_SNAP_DISTANCE)
            .sort((a, b) => a.distance - b.distance)[0]?.edge || null

        if (snapEdge === 'left') {
            position.left = 0
        } else if (snapEdge === 'right') {
            position.left = Math.max(0, width - FLOAT_EDGE_VISIBLE_SIZE)
        }

        if (snapEdge === 'top') {
            position.top = 0
        } else if (snapEdge === 'bottom') {
            position.top = Math.max(0, height - FLOAT_EDGE_VISIBLE_SIZE)
        }

        return {
            ...position,
            edge: snapEdge,
        }
    }

    function removeDragListeners () {
        window.removeEventListener('pointermove', handleDragMove)
        window.removeEventListener('pointerup', handleDragEnd)
        window.removeEventListener('pointercancel', handleDragEnd)
        window.removeEventListener('blur', handleDragAbort)
    }

    function releaseActivePointerCapture () {
        const target = activeDragTarget
        const pointerId = activePointerId
        activeDragTarget = null
        activePointerId = null

        if (target && pointerId !== null && target.hasPointerCapture(pointerId)) {
            try {
                target.releasePointerCapture(pointerId)
            } catch (e) {
                // Capture may already be released by the browser.
            }
        }
    }

    function suppressNextClick () {
        ignoreNextClick = true
        window.setTimeout(() => {
            ignoreNextClick = false
        }, 200)
    }

    function finishActiveDrag (shouldSuppressClick = true) {
        const shouldSnap = dragStarted
        removeDragListeners()

        if (shouldSnap && floatPosition.value) {
            const snappedPosition = getSnappedFloatPosition(floatPosition.value.left, floatPosition.value.top)
            floatSnapEdge.value = snappedPosition.edge
            floatPosition.value = {
                left: snappedPosition.left,
                top: snappedPosition.top,
            }
        }

        if (shouldSnap && shouldSuppressClick) {
            suppressNextClick()
        }

        isDragging.value = false
        dragStarted = false
        releaseActivePointerCapture()
    }

    function handleDragStart (event: PointerEvent) {
        if (panelVisible.value || event.button !== 0) {
            return
        }
        if (activePointerId !== null) {
            finishActiveDrag(false)
        }

        const target = event.currentTarget as HTMLElement
        const rect = (floatIconRef.value || target).getBoundingClientRect()
        const position = getClampedFloatPosition(rect.left, rect.top)

        activePointerId = event.pointerId
        activeDragTarget = target
        dragStartX = event.clientX
        dragStartY = event.clientY
        dragOffsetX = event.clientX - position.left
        dragOffsetY = event.clientY - position.top
        dragStarted = false
        dragOriginPosition = position
        target.setPointerCapture(event.pointerId)
        window.addEventListener('pointermove', handleDragMove)
        window.addEventListener('pointerup', handleDragEnd)
        window.addEventListener('pointercancel', handleDragEnd)
        window.addEventListener('blur', handleDragAbort)
    }

    function handleDragMove (event: PointerEvent) {
        if (activePointerId !== event.pointerId) {
            return
        }
        const movedDistance = Math.hypot(event.clientX - dragStartX, event.clientY - dragStartY)
        if (!dragStarted && movedDistance <= FLOAT_DRAG_THRESHOLD) {
            return
        }
        if (!dragStarted) {
            dragStarted = true
            hasDragged.value = true
            isDragging.value = true
            floatSnapEdge.value = null
            floatPosition.value = dragOriginPosition
        }
        floatPosition.value = getClampedFloatPosition(
            event.clientX - dragOffsetX,
            event.clientY - dragOffsetY,
        )
        event.preventDefault()
    }

    function handleDragEnd (event: PointerEvent) {
        if (activePointerId !== event.pointerId) {
            return
        }
        finishActiveDrag()
    }

    function handleLostPointerCapture (event: PointerEvent) {
        if (activePointerId !== event.pointerId) {
            return
        }
        finishActiveDrag()
    }

    function handleDragAbort () {
        if (activePointerId === null) {
            return
        }
        finishActiveDrag(false)
    }

    function handleViewportResize () {
        if (hasDragged.value && floatPosition.value) {
            const { width, height } = getViewportSize()
            const position = getClampedFloatPosition(floatPosition.value.left, floatPosition.value.top)
            if (floatSnapEdge.value === 'right') {
                position.left = Math.max(0, width - FLOAT_EDGE_VISIBLE_SIZE)
            } else if (floatSnapEdge.value === 'bottom') {
                position.top = Math.max(0, height - FLOAT_EDGE_VISIBLE_SIZE)
            }
            floatPosition.value = position
        }
    }

    function handleClickOutside () {
        if (panelVisible.value) {
            panelVisible.value = false
        }
    }

    function handleClosePanel () {
        panelVisible.value = false
    }

    function handleSubContextUpdate (params: Record<string, string>) {
        subAppContext.value = params || {}
    }

    function handleIframeLoad () {
        iframeLoading.value = false
        postContextToAi()
    }

    onMounted(() => {
        loadAiEligibleProjects()
        eventBus.$on(AI_IFRAME_EVENTS.CLOSE_PANEL, handleClosePanel)
        eventBus.$on(AI_IFRAME_EVENTS.UPDATE_SUB_CONTEXT, handleSubContextUpdate)
        eventBus.$on(AI_IFRAME_EVENTS.REQUEST_CONTEXT, postContextToAi)
        window.addEventListener('resize', handleViewportResize)
    })

    onBeforeUnmount(() => {
        eventBus.$off(AI_IFRAME_EVENTS.CLOSE_PANEL, handleClosePanel)
        eventBus.$off(AI_IFRAME_EVENTS.UPDATE_SUB_CONTEXT, handleSubContextUpdate)
        eventBus.$off(AI_IFRAME_EVENTS.REQUEST_CONTEXT, postContextToAi)
        window.removeEventListener('resize', handleViewportResize)
        removeDragListeners()
    })

    watch(subAppContext, () => {
        syncShowAiEntry()
        postContextToAi()
    }, { deep: true })

    watch(showAiEntry, (visible) => {
        if (!visible) {
            panelVisible.value = false
        }
    })
</script>

<style lang="scss" scoped>
.ai-float-wrapper {
    --ai-float-size: 48px;
    --ai-float-hidden-offset: -24px;
    --ai-float-visible-width: 24px;
    --ai-float-hover-buffer: 6px;
    --ai-float-hit-zone-size: calc(var(--ai-float-visible-width) + var(--ai-float-hover-buffer));
    --ai-float-visible-offset: 16px;
    --ai-float-hover-scale: 1.08;
    --ai-float-shadow: 0 4px 16px rgba(58, 132, 255, .4);
    --ai-float-shadow-hover: 0 12px 30px rgba(58, 132, 255, .5);
    position: fixed;
    right: 0;
    bottom: 24px;
    z-index: 2500;
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    pointer-events: none;
}

.ai-float-wrapper-dragged {
    right: auto;
    bottom: auto;
}

.ai-float-wrapper-dragged:not(.ai-float-wrapper-dragging) {
    transition: left .18s ease, top .18s ease;
}

.ai-float-hit-zone {
    position: relative;
    display: block;
    appearance: none;
    width: var(--ai-float-hit-zone-size);
    height: var(--ai-float-size);
    padding: 0;
    border: 0;
    background: transparent;
    pointer-events: auto;
    cursor: pointer;
    touch-action: none;
    user-select: none;
}

.ai-float-wrapper:not(.ai-float-wrapper-dragged) {
    .ai-float-hit-zone:hover {
        .ai-float-btn {
            right: var(--ai-float-visible-offset);
            transform: scale(var(--ai-float-hover-scale));
            box-shadow: var(--ai-float-shadow-hover);
        }
    }
}

.ai-float-wrapper-dragged {
    .ai-float-hit-zone {
        width: var(--ai-float-size);
        height: var(--ai-float-size);
        cursor: grab;
    }

    .ai-float-btn {
        right: 0;
    }
}

.ai-float-wrapper-snapped-left,
.ai-float-wrapper-snapped-right {
    .ai-float-hit-zone {
        width: var(--ai-float-hit-zone-size);
        height: var(--ai-float-size);
    }
}

.ai-float-wrapper-snapped-top,
.ai-float-wrapper-snapped-bottom {
    .ai-float-hit-zone {
        width: var(--ai-float-size);
        height: var(--ai-float-hit-zone-size);
    }
}

.ai-float-wrapper-snapped-left {
    .ai-float-btn {
        right: auto;
        left: var(--ai-float-hidden-offset);
        transform-origin: left center;
    }

    .ai-float-hit-zone:hover {
        .ai-float-btn {
            left: var(--ai-float-visible-offset);
            transform: scale(var(--ai-float-hover-scale));
            box-shadow: var(--ai-float-shadow-hover);
        }
    }
}

.ai-float-wrapper-snapped-right {
    .ai-float-btn {
        right: var(--ai-float-hidden-offset);
        transform-origin: right center;
    }

    .ai-float-hit-zone:hover {
        .ai-float-btn {
            right: var(--ai-float-visible-offset);
            transform: scale(var(--ai-float-hover-scale));
            box-shadow: var(--ai-float-shadow-hover);
        }
    }
}

.ai-float-wrapper-snapped-top {
    .ai-float-btn {
        top: var(--ai-float-hidden-offset);
        right: auto;
        left: 0;
        transform-origin: center top;
    }

    .ai-float-hit-zone:hover {
        .ai-float-btn {
            top: var(--ai-float-visible-offset);
            transform: scale(var(--ai-float-hover-scale));
            box-shadow: var(--ai-float-shadow-hover);
        }
    }
}

.ai-float-wrapper-snapped-bottom {
    .ai-float-btn {
        top: auto;
        right: auto;
        bottom: var(--ai-float-hidden-offset);
        left: 0;
        transform-origin: center bottom;
    }

    .ai-float-hit-zone:hover {
        .ai-float-btn {
            bottom: var(--ai-float-visible-offset);
            transform: scale(var(--ai-float-hover-scale));
            box-shadow: var(--ai-float-shadow-hover);
        }
    }
}

.ai-float-wrapper-dragging {
    .ai-float-hit-zone {
        cursor: grabbing;
    }

    .ai-float-btn {
        transition: none;
        box-shadow: var(--ai-float-shadow-hover);
    }
}

.ai-float-btn {
    position: absolute;
    top: 0;
    right: var(--ai-float-hidden-offset);
    box-sizing: border-box;
    display: flex;
    align-items: center;
    justify-content: center;
    width: var(--ai-float-size);
    height: var(--ai-float-size);
    appearance: none;
    border: none;
    border-radius: 50%;
    background-color: white;
    padding: 6px;
    border: 2px solid #eaebf0;
    box-shadow: var(--ai-float-shadow);
    pointer-events: auto;
    transform: scale(1);
    transform-origin: right center;
    transition: right .22s ease, left .22s ease, top .22s ease, bottom .22s ease, transform .22s ease, box-shadow .22s ease, background .22s ease;
    will-change: transform, box-shadow;

    > img {
        width: 100%;
        height: 100%;
        object-fit: cover;
        pointer-events: none;
        -webkit-user-drag: none;
    }

    &-active {
        right: var(--ai-float-visible-offset);
        background: linear-gradient(135deg, #7b61ff 0%, #3a84ff 100%);
    }
}

.ai-panel {
    position: fixed;
    width: 666px;
    height: 100vh;
    right: 0;
    bottom: 0;
    border-radius: 12px;
    box-shadow: 0 8px 40px rgba(0, 0, 0, .15);
    overflow: hidden;
    display: flex;
    flex-direction: column;
    pointer-events: auto;
}
.ai-panel-loading {
    position: absolute;
    inset: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 12px;
    background: #fff;
    z-index: 1;
}

.ai-loading-spinner {
    width: 32px;
    height: 32px;
    border: 3px solid #eaebf0;
    border-top-color: #3a84ff;
    border-radius: 50%;
    animation: ai-spin .8s linear infinite;
}

@keyframes ai-spin {
    to { transform: rotate(360deg); }
}

.ai-loading-text {
    font-size: 13px;
    color: #979ba5;
}

.ai-panel-iframe {
    flex: 1;
    width: 100%;
    border: none;

    &-hidden {
        opacity: 0;
    }
}

.ai-panel-slide-enter-active,
.ai-panel-slide-leave-active {
    transition: opacity .25s ease, transform .25s ease;
}

.ai-panel-slide-enter,
.ai-panel-slide-leave-to {
    opacity: 0;
    transform: translateY(16px) scale(.96);
}
</style>
