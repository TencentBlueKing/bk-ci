<template>
    <div class="build-log-container">
        <!-- 日志内容区域 -->
        <RecycleScroller
            ref="scrollContainer"
            :items="logs"
            :buffer="1000"
            key-field="lineNo"
            size-field="height"
            class="log-content"
        >
            <template #default="{ item }">
                <div
                    class="log-item"
                    :class="{ 'group-title': item.isGroupTitle }"
                    @click="item.handleGroupClick"
                >
                    <label class="line-number">{{ item.lineNo }}</label>
                    <p class="log-message">
                        <span
                            v-if="item.isGroupTitle"
                            class="fold-icon"
                            :class="{ folded: foldSet.has(item.lineNo) }"
                        >
                            <i class="bk-icon icon-down-shape"></i>
                        </span>
                        <span
                            class="log-text"
                            :style="{ color: getLogMsgColor(item.message) }"
                            v-html="item.message"
                        ></span>
                    </p>
                </div>
            </template>
        </RecycleScroller>
        <bk-loading
            v-if="isLogLoading"
            :is-loading="isLogLoading"
            size="small"
            :opacity="0"
            ext-cls="loading-tip"
        ></bk-loading>
        <!-- 重新构建按钮 -->
        <bk-button
            theme="primary"
            class="rebuild-btn"
            @click="handleRebuild"
            :disabled="isEdit || isInfoRunning"
        >
            {{ $t('store.重新构建') }}
        </bk-button>
    </div>
</template>

<script setup name="BuildLogProgress">
    import { ref, shallowRef, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
    import { RecycleScroller } from 'vue-virtual-scroller'
    import 'vue-virtual-scroller/dist/vue-virtual-scroller.css'
    import UseInstance from '@/hook/useInstance.js'
    import AnsiUp from 'ansi_up'

    const props = defineProps({
        pipelineId: {
            type: String,
            default: ''
        },
        projectCode: {
            type: String,
            default: ''
        },
        buildId: {
            type: String,
            default: ''
        },
        currentStep: {
            type: Object,
            required: true
        },
        isEdit: {
            type: Boolean,
            required: true
        },
        runningStep: {
            type: Object,
            required: true
        }
    })

    const emit = defineEmits(['rebuild'])

    const { proxy } = UseInstance()
    const { $store, $router, $route, $t, $bkMessage } = proxy

    const ansiUp = new AnsiUp()
    const fullLogs = shallowRef([])
    const folders = ref([])
    const foldSet = ref(new Set())
    const scrollContainer = ref()
    let buildTimer
    const isRunning = ref(false)
    const isLogLoading = ref(false)
    const isFolded = ref(false)
    const infoBuild = ref(true)

    // 计算属性：日志显示列表（与 React 版本的 logs 对应）
    const logs = computed(() => {
        const resultLogs = []

        if (folders.value.length > 0) {
            folders.value.forEach(([start, end], i) => {
                const prevEnd = i === 0 ? 0 : folders.value[i - 1][1] + 1
                resultLogs.push(...fullLogs.value.slice(prevEnd, start + 1))

                // 对于最后一个片段，添加从片段结束到数组结束的部分
                if (i === folders.value.length - 1) {
                    resultLogs.push(...fullLogs.value.slice(end + 1))
                }
            })
            return resultLogs
        }
        return [...fullLogs.value]
    })

    // 判断构建是否正在运行
    const isInfoRunning = computed(() => {
        return props.currentStep.status === 'DOING'
            || (props.runningStep?.status === 'DOING' && props.runningStep?.code === 'build')
    })

    // 监听日志变化，自动滚动到底部
    watch(logs, (logs) => {
        if (logs && !isFolded.value) {
            scrollToBottom()
        }
    })

    onMounted(() => {
        loopLog()
        scrollToBottom()
    })

    onBeforeUnmount(() => {
        refreshBuildProgress()
    })

    // 暴露方法给父组件
    defineExpose({
        refreshBuildProgress
    })

    function refreshBuildProgress () {
        clearTimeout(buildTimer)
        infoBuild.value = false
    }

    // 获取日志
    async function fetchLog () {
        try {
            isLogLoading.value = true
            const res = await $store.dispatch('store/fetchLog', {
                pipelineId: props.pipelineId,
                projectCode: props.projectCode,
                buildId: props.buildId
            })

            if (res.logs.length) {
                isRunning.value = res.finished
                fullLogs.value = res.logs.map((log, index) => {
                    const message = formatMsg(log.message)
                    const height = calcHeightByMessage(log.message)
                    const isGroupTitle = log.message.startsWith('##[group]')
                    
                    return {
                        lineNo: index,
                        isGroupTitle,
                        fold: false,
                        originMsg: log.message,
                        message,
                        height,
                        handleGroupClick: () => {
                            isFolded.value = true
                            if (!isGroupTitle) return
                            
                            const length = fullLogs.value.slice(index).findIndex(item =>
                                item.originMsg.startsWith('##[endgroup]')
                            )

                            if (!foldSet.value.has(index)) {
                                folders.value.push([index, index + length])
                                foldSet.value.add(index)
                            } else {
                                folders.value = folders.value.filter(item => item[0] !== index)
                                foldSet.value.delete(index)
                            }
                            folders.value.sort(([start], [start2]) => start - start2)
                        }
                    }
                })
                isLogLoading.value = false
            } else {
                res.message && $bkMessage({
                    theme: 'error',
                    message: res.message
                })
            }
        } catch (error) {
            refreshBuildProgress()
        }
    }

    // 循环获取日志
    async function loopLog () {
        clearTimeout(buildTimer)
        await fetchLog()
        
        if ((isInfoRunning.value || !isRunning.value) && infoBuild.value) {
            isLogLoading.value = true
            buildTimer = setTimeout(() => {
                loopLog()
            }, 2000)
        } else {
            isLogLoading.value = false
            infoBuild.value = false
        }
    }

    // 滚动到底部
    async function scrollToBottom () {
        if (scrollContainer.value) {
            const lastIndex = fullLogs.value.length - 1
            await nextTick()
            scrollContainer.value.scrollToItem(lastIndex)
        }
    }

    // 计算消息高度（用于虚拟滚动）
    function calcHeightByMessage (msg) {
        const canvas = document.createElement('canvas')
        const ctx = canvas.getContext('2d')
        ctx.font = '12px "Microsoft YaHei", system-ui, sans-serif'
        const result = ctx.measureText(msg)
        return 32 * Math.max(1, Math.ceil(result.width / 740))
    }

    // 获取日志消息颜色
    function getLogMsgColor (msg) {
        switch (true) {
            case msg.startsWith('##[debug]'):
                return '#0EA5E9'
            case msg.startsWith('##[warning]'):
                return '#F59E0B'
            case msg.startsWith('##[error]'):
                return '#F73131'
            case msg.startsWith('##[info]'):
            default:
                return '#666666'
        }
    }

    // 格式化消息（处理 ANSI 转义码）
    function formatMsg (msg) {
        ansiUp.escape_html = !/<\/a>/gi.test(msg)
        return ansiUp.ansi_to_html(msg.replace(/^##\[\w+\]/, ''))
    }

    // 重新构建
    function handleRebuild () {
        if (props.currentStep.status === 'DOING') return
        isLogLoading.value = true
        emit('rebuild')
    }
</script>

<style lang="scss" scoped>
.build-log-container {
    position: relative;
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    
    .log-content {
        flex: 1;
        background-color: #F5F5F5;
        border: 1px solid #D9D9D9;
        border-radius: 2px;
        overflow-y: auto;
        height: 100%;
        
        ::v-deep .vue-recycle-scroller__item-wrapper {
            overflow: visible;
        }
        
        .log-item {
            display: flex;
            font-size: 12px;
            line-height: 32px;
            transition: all 0.15s linear;
            
            &.group-title {
                cursor: pointer;
                
                &:hover .log-message {
                    border-color: #999;
                }
            }
            
            .line-number {
                flex-shrink: 0;
                width: 60px;
                padding: 0 10px;
                text-align: right;
                color: #666666;
            }
            
            .log-message {
                flex: 1;
                position: relative;
                padding-left: 20px;
                border: 1px solid transparent;
                border-radius: 2px;
                word-break: break-all;
                
                .fold-icon {
                    position: absolute;
                    left: 0;
                    color: #666666;
                    font-size: 18px;
                    cursor: pointer;
                    transition: transform 0.3s;
                    
                    &.folded {
                        transform: rotate(-90deg);
                    }
                }
                
                .log-text {
                    ::v-deep span {
                        font-weight: 500;
                    }
                    
                    ::v-deep a {
                        color: #3A84FF;
                        text-decoration: underline;
                        
                        &:hover {
                            opacity: 0.8;
                        }
                    }
                }
            }
        }
    }

    .loading-tip {
      position: fixed !important;
      height: 25px;
      bottom: 65px;
      right: auto;
      margin: auto;
      left: 0;
      background-color: rgba(0, 0, 0, 0);
      margin-bottom: 24px;
    }
    
    .rebuild-btn {
        margin-top: 30px;
        width: 90px;
        height: 32px;
        font-size: 14px;
    }
}
</style>
