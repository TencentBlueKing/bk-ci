<template>
    <div
        :style="{ height }"
        ref="diffBox"
        class="pipeline-yaml-diff"
        v-bkloading="{ isLoading, color: '#1d1d1d' }"
    >
    </div>
</template>

<script>
    import MonacoEditor from '@/utils/monacoEditor'
    // 差异块状态：0 表示视口在第一处差异之上，1~N 表示视口处于第 n 处差异
    const DEFAULT_DIFF_STATE = {
        count: 0,
        currentIndex: 0,
        hasPrev: false,
        hasNext: false
    }
    export default {
        props: {
            height: {
                type: String,
                default: '666px'
            },
            oldYaml: {
                type: String,
                default: ''
            },
            newYaml: {
                type: String,
                default: ''
            },
            readOnly: {
                type: Boolean,
            },
        },
        data () {
            return {
                isLoading: false,
                diffBlocks: [],
                // 跳转固定态：打开/点击箭头后计数固定为跳转目标，用户滚动后才按视口重算
                stickyIndex: null,
                // 跳转后忽略滚动事件的截止时间（吸收同步滚动的定位抖动）
                suppressRecalcUntil: 0
            }
        },
        watch: {
            oldYaml (newValue) {
                if (this.editor) {
                    this.diff(newValue, this.newYaml)
                }
            },
            newYaml (newValue) {
                if (this.editor) {
                    this.diff(this.oldYaml, newValue)
                }
            }
        },
        async mounted () {
            this.isLoading = true
            this.monaco = await MonacoEditor.instance()
            this.isLoading = false

            this.editor = this.monaco.editor.createDiffEditor(
                this.$refs.diffBox,
                {
                    language: 'yaml',
                    readOnly: this.readOnly,
                    automaticLayout: true,
                    unicodeHighlight: {
                        ambiguousCharacters: false
                    }
                }
            )
            this.disposables = [
                // diff 计算完成后更新差异块并定位到第一处
                this.editor.onDidUpdateDiff(() => this.updateDiffBlocks()),
                this.editor.getModifiedEditor().onDidScrollChange(() => this.updateCurrentDiffIndex()),
                this.editor.getOriginalEditor().onDidScrollChange(() => this.updateCurrentDiffIndex()),
                // 弹窗打开瞬间布局尚未稳定时，布局完成后重算当前差异块位置
                this.editor.getModifiedEditor().onDidLayoutChange(() => this.updateCurrentDiffIndex())
            ]
            this.diff(this.oldYaml, this.newYaml)
        },
        beforeDestroy () {
            (this.disposables || []).forEach(disposable => disposable?.dispose?.())
            this.disposables = []
            if (this.originalModel) this.originalModel.dispose()
            if (this.modifiedModel) this.modifiedModel.dispose()
            if (this.editor) {
                this.editor.dispose()
            }
        },
        methods: {
            diff (oldYaml, newYaml) {
                const originalModel = this.monaco.editor.createModel(
                    oldYaml,
                    'yaml'
                )
                const modifiedModel = this.monaco.editor.createModel(
                    newYaml,
                    'yaml'
                )
                this.editor.setModel({
                    original: originalModel,
                    modified: modifiedModel
                })
                // 释放旧的 model，避免切换版本对比时内存泄漏
                if (this.originalModel) this.originalModel.dispose()
                if (this.modifiedModel) this.modifiedModel.dispose()
                this.originalModel = originalModel
                this.modifiedModel = modifiedModel
                // 切换版本后重置差异块状态，等待 diff 重新计算
                this.diffBlocks = []
                this.stickyIndex = null
                this.suppressRecalcUntil = 0
                this.emitDiffState(DEFAULT_DIFF_STATE)
            },
            // 将连续的增删改合并为一处差异块；中间隔着未改行则拆分为两处
            updateDiffBlocks () {
                const lineChanges = this.editor.getLineChanges() || []
                const blocks = []
                lineChanges.forEach(change => {
                    const last = blocks[blocks.length - 1]
                    if (last
                        && change.originalStartLineNumber <= last.originalEndLineNumber + 1
                        && change.modifiedStartLineNumber <= last.modifiedEndLineNumber + 1
                    ) {
                        last.originalEndLineNumber = Math.max(last.originalEndLineNumber, change.originalEndLineNumber)
                        last.modifiedEndLineNumber = Math.max(last.modifiedEndLineNumber, change.modifiedEndLineNumber)
                    } else {
                        blocks.push({
                            originalStartLineNumber: change.originalStartLineNumber,
                            originalEndLineNumber: change.originalEndLineNumber,
                            modifiedStartLineNumber: change.modifiedStartLineNumber,
                            modifiedEndLineNumber: change.modifiedEndLineNumber
                        })
                    }
                })
                this.diffBlocks = blocks
                if (blocks.length) {
                    // 打开/切换版本后，第一处差异滚动到视口中部
                    this.$nextTick(() => this.revealDiffBlock(0))
                } else {
                    this.updateCurrentDiffIndex()
                }
            },
            // 判定当前所在差异块：块的定位行越过视口中线才算到达该块。
            // 左右两栏行号会因大块增删而错位（如大段删除只存在于左栏、大段新增只存在于右栏），
            // 任一栏还停留在第 n 块时仍算第 n 块，因此取两栏计算结果中较小的序号
            getCurrentDiffIndex () {
                return Math.min(
                    this.calcReachedIndex(
                        this.editor.getOriginalEditor(),
                        block => block.originalStartLineNumber || block.originalEndLineNumber
                    ),
                    this.calcReachedIndex(
                        this.editor.getModifiedEditor(),
                        block => block.modifiedStartLineNumber || block.modifiedEndLineNumber
                    )
                )
            },
            // 计算某栏视口下已到达的差异块序号：块在该栏的定位行（与跳转居中使用的行一致）越过视口中线即视为到达
            calcReachedIndex (editor, getRevealLine) {
                const model = editor.getModel()
                if (!model) {
                    return 0
                }
                const lineHeight = editor.getOption(this.monaco.editor.EditorOption.lineHeight)
                const layoutInfo = editor.getLayoutInfo()
                if (!lineHeight || !layoutInfo?.height) {
                    return 0
                }
                const lineCount = model.getLineCount()
                const scrollTop = editor.getScrollTop()
                const viewTop = Math.floor(scrollTop / lineHeight) + 1
                const viewBottom = Math.ceil((scrollTop + layoutInfo.height) / lineHeight)
                // 滚动条已在顶端且第一处差异仍在视口内时，当前处就是第一处：
                // 第一处贴着文件开头时滚不到视口中部，避免中线判定一直停在后面的块上
                const firstRevealLine = Math.min(Math.max(1, getRevealLine(this.diffBlocks[0])), lineCount)
                if (scrollTop === 0 && firstRevealLine <= viewBottom) {
                    return 1
                }
                const middleLine = (viewTop + viewBottom) / 2
                let currentIndex = 0
                this.diffBlocks.forEach((block, index) => {
                    // 与 revealDiffBlock 相同的钳制规则，保证跳转后序号稳定
                    const revealLine = Math.min(Math.max(1, getRevealLine(block)), lineCount)
                    // +2 容差：居中定位后左右两栏的同步滚动会有 1~2 行的定位偏差，
                    // 容差过小会导致打开/跳转后序号被判定回上一块
                    if (revealLine <= middleLine + 2) {
                        currentIndex = index + 1
                    }
                })
                return currentIndex
            },
            // 根据视口位置判定当前所在差异块（仅用户滚动时重算，跳转后的计数由 stickyIndex 固定）
            updateCurrentDiffIndex () {
                if (!this.editor || !this.diffBlocks.length) {
                    this.emitDiffState(DEFAULT_DIFF_STATE)
                    return
                }
                // 跳转后的窗口期内不重算，吸收同步滚动带来的定位抖动
                if (Date.now() < this.suppressRecalcUntil) {
                    return
                }
                const currentIndex = this.getCurrentDiffIndex()
                // 用户滚动后解除跳转固定态
                this.stickyIndex = null
                this.emitDiffState({
                    count: this.diffBlocks.length,
                    currentIndex,
                    // 箭头可用性由序号推导：n=1 时上箭头不可用，n=N 时下箭头不可用
                    hasPrev: currentIndex > 1,
                    hasNext: currentIndex < this.diffBlocks.length
                })
            },
            emitDiffState (state) {
                const last = this.lastEmittedState
                if (last
                    && last.count === state.count
                    && last.currentIndex === state.currentIndex
                    && last.hasPrev === state.hasPrev
                    && last.hasNext === state.hasNext
                ) {
                    return
                }
                this.lastEmittedState = state
                this.$emit('diff-change', state)
            },
            // 将目标差异块滚动到视口中部，左右两栏一起滚动
            revealDiffBlock (index) {
                const block = this.diffBlocks[index]
                if (!block || !this.editor) {
                    return
                }
                const originalEditor = this.editor.getOriginalEditor()
                const modifiedEditor = this.editor.getModifiedEditor()
                const originalLineCount = originalEditor.getModel()?.getLineCount() || 1
                const modifiedLineCount = modifiedEditor.getModel()?.getLineCount() || 1
                // 纯增/纯删的空区间行号可能为 0 或超出文件行数，需钳制到有效范围
                const originalLine = Math.min(
                    Math.max(1, block.originalStartLineNumber || block.originalEndLineNumber),
                    originalLineCount
                )
                const modifiedLine = Math.min(
                    Math.max(1, block.modifiedStartLineNumber || block.modifiedEndLineNumber),
                    modifiedLineCount
                )
                originalEditor.revealLineInCenter(originalLine)
                modifiedEditor.revealLineInCenter(modifiedLine)
                // 计数先认跳转：打开或点击箭头后，计数固定为这次要去的那一处，
                // 目标块因贴近文件开头/末尾而无法滚动到视口中部时也停在这一处
                this.stickyIndex = index + 1
                this.suppressRecalcUntil = Date.now() + 300
                this.emitDiffState({
                    count: this.diffBlocks.length,
                    currentIndex: this.stickyIndex,
                    hasPrev: this.stickyIndex > 1,
                    hasNext: this.stickyIndex < this.diffBlocks.length
                })
            },
            // 箭头导航使用的当前序号：跳转固定态（打开/点击箭头后未滚动）以 stickyIndex 为准，
            // 与计数显示保持一致；避免顶部边界场景下视口重算（顶部规则）把导航基准算回上一块
            getNavIndex () {
                if (!this.editor || !this.diffBlocks.length) {
                    return 0
                }
                if (this.stickyIndex != null) {
                    return this.stickyIndex
                }
                return this.getCurrentDiffIndex()
            },
            // 跳转到上一处差异，到头即停住
            goPrev () {
                const index = this.getNavIndex()
                if (index > 1) {
                    this.revealDiffBlock(index - 2)
                }
            },
            // 跳转到下一处差异，到头即停住
            goNext () {
                const index = this.getNavIndex()
                if (index < this.diffBlocks.length) {
                    this.revealDiffBlock(index)
                }
            }
        }
    }
</script>

<style lang="scss">
    .pipeline-yaml-diff {
        background: #1d1d1d;
    }
</style>
