<template>
    <div
        v-if="showAiEntry"
        class="ai-float-wrapper"
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
        <span
            :class="['ai-float-btn', { 'ai-float-btn-active': panelVisible }]"
            @click="togglePanel"
        >
            <img :src="aiblukeingBanner" />
        </span>
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
    const aiEligibleProjectCodes = ref<Set<string>>(new Set())
    const aiProjectsFetchDone = ref(false)
    const showAiEntry = ref(false)
    const store = useStore()

    /** Latest context reported by the inner pipeline iframe in this tab. */
    const subAppContext = ref<{ projectId?: string; pipelineId?: string; buildId?: string }>({})

    const iframeSrc = computed(() => {
        return `${window.PUBLIC_URL_PREFIX || ''}/ai/`
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
    })

    onBeforeUnmount(() => {
        eventBus.$off(AI_IFRAME_EVENTS.CLOSE_PANEL, handleClosePanel)
        eventBus.$off(AI_IFRAME_EVENTS.UPDATE_SUB_CONTEXT, handleSubContextUpdate)
        eventBus.$off(AI_IFRAME_EVENTS.REQUEST_CONTEXT, postContextToAi)
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
    position: fixed;
    right: 24px;
    bottom: 24px;
    z-index: 2500;
    background-color: #fff;
}

.ai-float-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 66px;
    height: 66px;
    border: none;
    border-radius: 50%;
    background-color: white;
    cursor: pointer;
    padding: 8px;
    border: 3px solid #eaebf0;
    box-shadow: 0 4px 16px rgba(58, 132, 255, .4);
    transition: transform .2s, box-shadow .2s;

    &:hover {
        transform: scale(1.08);
    }

    > img {
        width: 100%;
        height: 100%;
        object-fit: cover;
    }

    &-active {
        background: linear-gradient(135deg, #7b61ff 0%, #3a84ff 100%);
    }
}


.ai-panel {
    position: fixed;
    right: 0;
    bottom: 60px;
    width: 666px;
    height: 100vh;
    right: 0;
    bottom: 0;
    border-radius: 12px;
    box-shadow: 0 8px 40px rgba(0, 0, 0, .15);
    overflow: hidden;
    display: flex;
    flex-direction: column;
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
