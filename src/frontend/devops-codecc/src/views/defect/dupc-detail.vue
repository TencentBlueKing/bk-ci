<template>
    <div class="main">
        <bk-container flex col="2" margin="0">
            <bk-row>
                <bk-col :span="1" class="source-code" id="sourceCodeCol">
                    <div class="title-bar">
                        <div class="field">{{$t('重复源代码块')}}</div>
                        <div class="filename">( {{sourceData.fileName}} )</div>
                        <div class="select-chunk">
                            <bk-dropdown-menu class="select-chunk-text" align="right" trigger="click">
                                <bk-button type="primary" slot="dropdown-trigger">
                                    <span class="t1">{{selectText.source.row || $t('点击选择重复源代码块')}}</span>
                                    <em class="t2" v-show="selectText.source.chunk">{{$t('x块重复', { num: selectText.source.chunk })}}</em>
                                    <i :class="['bk-icon icon-angle-down', { 'icon-flip': isDropdownShow }]"></i>
                                </bk-button>
                                <div class="chunk-list custom-scroll" slot="dropdown-content">
                                    <table class="table-chunk">
                                        <thead>
                                            <tr>
                                                <th>{{$t('代码行')}}</th>
                                                <th>{{$t('作者')}}</th>
                                                <th width="60">{{$t('重复块数')}}</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <tr
                                                v-for="(item, index) in sourceData.blockInfoList"
                                                :key="index"
                                                :data-index="index"
                                                :data-id="`${item.startLines}-${item.endLines}-0`"
                                                :class="{ 'selected': index === selectText.source.index }"
                                                @click="clickGutterLine(index, item)"
                                            >
                                                <td>{{item.startLines}}-{{item.endLines}}</td>
                                                <td>{{item.author || ''}}</td>
                                                <td>{{getTargetChunksBySourceChunk(item).length}}</td>
                                            </tr>
                                        </tbody>
                                    </table>
                                </div>
                            </bk-dropdown-menu>
                        </div>
                    </div>
                    <div class="path-bar">
                        <div class="field">{{$t('源文件路径')}}：</div>
                        <div class="file-path"><input type="text" readonly spellcheck="false" :value="sourceData.filePath"></div>
                    </div>
                    <div class="code-editor" id="codeSource">
                        <div class="placeholder" v-show="!sourceData.fileContent">
                            <div class="placeholder-content">{{$t('正在加载文件内容')}}</div>
                        </div>
                    </div>
                </bk-col>
                <bk-col :span="1" class="target-code" id="targetCodeCol">
                    <div class="title-bar">
                        <div class="field">{{$t('重复目标代码块')}}</div>
                        <div class="filename"></div>
                        <div class="select-chunk" v-if="targetData.fileContent">
                            <bk-dropdown-menu class="select-chunk-text" align="right" trigger="click">
                                <bk-button type="primary" slot="dropdown-trigger">
                                    <span class="t0">{{selectText.target.index + 1}}</span>
                                    <span class="t1">{{selectText.target.file}}</span>
                                    <em class="t2">{{selectText.target.row}}</em>
                                    <i :class="['bk-icon icon-angle-down', { 'icon-flip': isDropdownShow }]"></i>
                                </bk-button>
                                <div class="chunk-list custom-scroll" slot="dropdown-content">
                                    <table class="table-chunk">
                                        <thead>
                                            <tr>
                                                <th>{{$t('块号')}}</th>
                                                <th>{{$t('文件名')}}</th>
                                                <th>{{$t('代码行')}}</th>
                                                <th>{{$t('作者')}}</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <tr
                                                v-for="(item, index) in targetData.blockInfoList"
                                                :key="index"
                                                :data-index="index"
                                                :data-id="`${item.startLines}-${item.endLines}-0`"
                                                :class="{ 'selected': index === selectText.target.index }"
                                                @click="clickTargetChunk(index, item)"
                                            >
                                                <td>{{index + 1}}</td>
                                                <td>{{item.fileName}}</td>
                                                <td>{{item.startLines}}-{{item.endLines}}</td>
                                                <td>{{item.author || ''}}</td>
                                            </tr>
                                        </tbody>
                                    </table>
                                </div>
                            </bk-dropdown-menu>
                        </div>
                    </div>
                    <div class="path-bar">
                        <div class="field">{{$t('目标文件路径')}}：</div>
                        <div class="file-path"><input type="text" readonly spellcheck="false" :value="targetData.filePath"></div>
                    </div>
                    <div class="code-editor" id="codeTarget">
                        <div class="placeholder" v-show="!targetData.fileContent">
                            <div class="placeholder-content">{{$t('未选择左侧源文件重复块')}}</div>
                        </div>
                    </div>
                </bk-col>
            </bk-row>
        </bk-container>
    </div>
</template>

<script>
    import CodeMirror from '@/common/codemirror'
    import { addClass, removeClass } from '@/common/util'

    export default {
        props: {
            entityId: {
                type: String,
                default: ''
            },
            filePath: {
                type: String,
                default: ''
            }
        },
        data () {
            const toolId = 'DUPC'
            const pattern = 'DUPC'

            return {
                toolId: toolId,
                pattern: pattern,
                editorSource: null,
                editorTarget: null,
                selectText: {
                    source: {
                        index: '',
                        row: '',
                        chunk: ''
                    },
                    target: {
                        index: 0,
                        file: '',
                        row: ''
                    }
                },
                params: {
                    entityId: '',
                    toolName: toolId,
                    pattern: pattern,
                    filePath: ''
                },
                sourceData: {}, // 源文件数据
                targetData: {}, // 目标文件数据
                gutterLines: {} // 所有点线的集合
            }
        },
        computed: {
            mergedLines () {
                const sourceChunks = this.sourceData.blockInfoList

                // 初始数据为第0个元素，值为[起始行,结束行,原索引]
                const mergeResult = [[[sourceChunks[0].startLines, sourceChunks[0].endLines, 0]]]
                for (let i = 1, chunkLen = sourceChunks.length; i < chunkLen; i++) {
                    const chunk = sourceChunks[i]
                    const resultItem = [chunk.startLines, chunk.endLines, i]
                    let flag = 0
                    for (let j = 0; j < mergeResult.length; j++) {
                        const len = mergeResult[j].length
                        if (chunk.startLines > mergeResult[j][len - 1][1] + 1) {
                            mergeResult[j].push(resultItem)
                            flag = 1
                            break
                        }
                    }
                    if (flag === 0) {
                        mergeResult.push([resultItem])
                    }
                }

                return mergeResult
            }
        },
        watch: {
            entityId (value) {
                if (value) this.init()
            }
        },
        created () {
        },
        mounted () {
            this.editorSource = CodeMirror(document.getElementById('codeSource'), {
                lineNumbers: true,
                scrollbarStyle: 'simple',
                theme: 'summerfruit',
                placeholder: this.$t('加载中'),
                readOnly: true
            })
            this.editorTarget = CodeMirror(document.getElementById('codeTarget'), {
                lineNumbers: true,
                scrollbarStyle: 'simple',
                theme: 'summerfruit',
                placeholder: this.$t('未选择文件'),
                readOnly: true
            })
        },
        methods: {
            async init () {
                try {
                    // const query = this.$route.query
                    this.clear()
                    const { entityId, filePath, editorSource } = this
                    const params = { ...this.params, entityId, filePath }
                    const res = await this.$store.dispatch('defect/lintDetail', params)
                    if (res.code === '2300005') {
                        this.defectDetailDialogVisiable = false
                        setTimeout(() => {
                            this.$bkInfo({
                                subHeader: this.$createElement('p', {
                                    style: {
                                        fontSize: '20px',
                                        lineHeight: '40px'
                                    }
                                }, this.$t('无法获取问题的代码片段。请先将工蜂OAuth授权给蓝盾。')),
                                confirmFn: () => {
                                    this.$store.dispatch('defect/oauthUrl', { toolName: this.toolId }).then(res => {
                                        window.open(res, '_blank')
                                    })
                                }
                            })
                        }, 500)
                    } else {
                        res.fileContent = res.fileContent || this.$t('文件内容为空')
                        this.sourceData = res
                        editorSource.setValue('')
                        editorSource.refresh()

                        // 更新代码展示组件内容等
                        setTimeout(() => {
                            this.updateEditor()

                            this.drawGutterLine()
                        }, 1)
                        // this.updateEditor()

                        // this.drawGutterLine()
                    }
                   
                } catch (e) {
                    console.error(e)
                }
            },
            updateEditor () {
                const { editorSource, mergedLines } = this
                const { fileName, fileContent, trimBeginLine } = this.sourceData

                // 通过文件后缀动态获取并设置代码展示的mode
                if (fileName) {
                    const { mode } = CodeMirror.findModeByFileName(fileName)
                    import(`codemirror/mode/${mode}/${mode}.js`).then(m => {
                        editorSource.setOption('mode', mode)
                    })
                }

                // 合并后的列，创建gutter
                const gutters = ['CodeMirror-linenumbers']
                mergedLines.forEach((item, i) => gutters.push(`CodeMirror-dupclines dupclines${i}`))
                editorSource.setOption('gutters', gutters)
                editorSource.setOption('firstLineNumber', trimBeginLine === 0 ? 1 : trimBeginLine)

                // 通过计算出画重复率线所占据宽度的加减，使源与目标代码展示宽度更平均
                const gutterElement = editorSource.getGutterElement()
                const gutterLineNumber = gutterElement.getElementsByClassName('CodeMirror-linenumbers')[0]
                const gutterLinesWidth = gutterElement.offsetWidth - gutterLineNumber.offsetWidth
                const gutterDiffWith = gutterLinesWidth / 2
                document.getElementById('targetCodeCol').style.width = `calc(50% - ${gutterDiffWith}px)`
                document.getElementById('sourceCodeCol').style.width = `calc(50% + ${gutterDiffWith}px)`

                // 设置文件内容
                editorSource.setValue(fileContent)
                editorSource.refresh()
            },
            updateTargetEditor (index) {
                const { editorTarget } = this
                const { fileName, fileContent, trimBeginLine } = this.targetData

                // 通过文件后缀动态获取并设置代码展示的mode
                const { mode } = CodeMirror.findModeByFileName(fileName)
                import(`codemirror/mode/${mode}/${mode}.js`).then(m => {
                    editorTarget.setOption('mode', mode)
                })

                // 合并后的列，创建gutter
                // const gutters = ['CodeMirror-linenumbers'](目标列宽度)
                // mergedLines.forEach((item, i) => gutters.push(`CodeMirror-dupclines dupclines${i}`))
                // editorTarget.setOption('gutters', gutters)
                editorTarget.setOption('firstLineNumber', trimBeginLine === 0 ? 1 : trimBeginLine)

                // 设置文件内容
                editorTarget.setValue(fileContent)
                const i = index || 0
                const targetStartLine = this.targetData.blockInfoList[i].startLines
                this.scrollIntoView(this.editorTarget, targetStartLine - trimBeginLine)
                this.chunkAddClass(this.editorTarget, this.targetData.blockInfoList[i])
                editorTarget.refresh()
            },
            getTargetChunksBySourceChunk (chunk) {
                const targetChunks = this.sourceData.targetBlockMap || {}
                const { startLines: startLine, endLines: endLine } = chunk
                return targetChunks[startLine + '_' + endLine] || []
            },
            drawGutterLine () {
                const self = this
                const { editorSource, mergedLines } = this
                const sourceChunks = this.sourceData.blockInfoList || []

                const lineHeight = editorSource.defaultTextHeight()
                const firstLineNumber = editorSource.getOption('firstLineNumber')
                const makeDotLine = function (lineId, lineIndex) {
                    const dotLine = document.createElement('em')
                    dotLine.className = 'dupc-dotline'
                    dotLine.dataset.id = lineId
                    dotLine.dataset.index = lineIndex
                    dotLine.style.height = `${lineHeight}px`

                    dotLine.addEventListener('mouseenter', function () {
                        self.hoverLine(this, 1)
                    })
                    dotLine.addEventListener('mouseleave', function () {
                        self.hoverLine(this, 0)
                    })
                    dotLine.addEventListener('click', function (event) {
                        self.clickGutterLine(lineIndex, sourceChunks[lineIndex])
                        event.stopImmediatePropagation()
                    })

                    return dotLine
                }

                // 每一列
                mergedLines.forEach((lines, i) => {
                    // 每一列中包含1+多条线，去画出每一列中的多条线
                    lines.forEach((item, j) => {
                        // const lineNumbers = item.split('-')
                        const startLine = parseInt(item[0], 10)
                        const endLine = parseInt(item[1], 10)
                        const lineIndex = parseInt(item[2], 10)

                        // 列号
                        const gutterId = `CodeMirror-dupclines dupclines${i}`
                        // 线号
                        const lineId = `${startLine}-${endLine}`

                        const line = []
                        for (let n = startLine - firstLineNumber; n <= endLine - firstLineNumber; n++) {
                            const dotLine = makeDotLine(lineId, lineIndex)
                            line.push(dotLine)
                            editorSource.setGutterMarker(n, gutterId, dotLine)
                        }
                        this.gutterLines[lineId] = line
                    })
                })
            },
            hoverLine (dotLine, isEnter) {
                const lineId = dotLine.dataset.id

                // 找到包含所有点的那一条线
                const gutterLine = this.gutterLines[lineId]

                // 切换每一个点的hover样式
                const toggleClassFn = isEnter ? addClass : removeClass
                gutterLine.forEach(dot => toggleClassFn(dot, 'hover'))
            },
            // 线画出选中样式
            drawLineClicked (lineId) {
                const { gutterLines } = this
                const gutterLineKeys = Object.keys(gutterLines)
                const gutterLine = gutterLines[lineId]
                gutterLineKeys.forEach(i => {
                    const item = gutterLines[i]
                    item.forEach(dot => removeClass(dot, 'selected'))
                })
                gutterLine.forEach(dot => addClass(dot, 'selected'))
            },
            // 点击重复线
            async clickGutterLine (index, chunk) {
                const { startLines, endLines } = chunk
                this.selectText.source.index = index
                const lineId = `${startLines}-${endLines}`
                this.selectText.source.row = this.$t('x行', { num: lineId })
                const blockInfoList = this.getTargetChunksBySourceChunk(chunk)
                this.selectText.source.chunk = blockInfoList.length
                this.targetData = Object.assign(this.targetData, { blockInfoList })

                this.drawLineClicked(lineId)

                // 滚动到相应位置
                this.scrollIntoView(this.editorSource, startLines)
                this.chunkAddClass(this.editorSource, chunk)

                // 默认选中第一个重复目标代码块
                this.clickTargetChunk(0, blockInfoList[0])
            },
            // 给重复块加类
            chunkAddClass (codeViewer, chunk) {
                const { startLines, endLines } = chunk
                const trimBeginLine = codeViewer.getOption('firstLineNumber')
                codeViewer.eachLine(hanlde => {
                    codeViewer.removeLineClass(hanlde, 'wrap', 'background')
                })
                for (let i = startLines - trimBeginLine; i < endLines - trimBeginLine + 1; i++) {
                    codeViewer.addLineClass(i, 'wrap', 'background')
                }
            },
            // 把重复块滚动到视图
            scrollIntoView (codeViewer, startLine) {
                const top = codeViewer.charCoords({ line: startLine, ch: 0 }, 'local').top
                const lineHeight = codeViewer.defaultTextHeight()
                codeViewer.scrollTo(0, top - 5 * lineHeight)
            },
            // 选择重复目标代码块
            async clickTargetChunk (index, chunk) {
                const { startLines, endLines, fileName } = chunk
                await this.getTargetContent(chunk)
                this.selectText.target.index = index
                this.selectText.target.file = fileName
                const lineId = `${startLines}-${endLines}`
                this.selectText.target.row = this.$t('x行', { num: lineId })

                this.updateTargetEditor(index)
            },
            // 获取目标文件内容
            async getTargetContent (chunk) {
                const fileParams = {
                    toolName: this.toolId,
                    filePath: chunk.sourceFile,
                    beginLine: chunk.startLines,
                    endLine: chunk.endLines
                }
                const fileData = await this.$store.dispatch('defect/fileContent', fileParams)
                this.targetData = Object.assign({}, this.targetData, fileData)
            },
            clear () {
                this.sourceData = {}
                this.targetData = {}
                this.gutterLines = {}
                this.selectText.source = {
                    index: '',
                    row: '',
                    chunk: ''
                }
            }
        }
    }
</script>

<style>
    @import './codemirror.css';
</style>

<style lang="postcss" scoped>
    @import './defect-list.css';

    .main {
        padding: 20px;
    }
    .title-bar {
        display: flex;
        height: 54px;
        font-size: 20px;
        align-items: center;
        color: #333;

        .field {
            flex: none;
        }
        .filename {
            flex: 1;
            padding: 0 5px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .select-chunk {
            color: #fff;
            font-size: 12px;
            cursor: pointer;
        }
    }

    .chunk-list {
        width: 380px;
        max-height: 300px;
        overflow: auto;
        border: 1px solid #6293b1;
        background: #fff;
        box-shadow: 0px 10px 15px 0px rgba(0,0,0,0.17);
        user-select: none;
    }

    .table-chunk {
        width: 100%;
        background-color: #fff;
        z-index: 101;
        color: #63656e;
        text-align: left;
    }
    .table-chunk th,
    .table-chunk td {
        padding: 8px;
        line-height: 26px;
        white-space: nowrap;
    }
    .table-chunk th:first-child,
    .table-chunk td:first-child {
        padding-left: 16px;
    }
    .table-chunk tr th {
        background-color: #f7f9fb;
        font-weight: normal;
        color:#555;
    }

    .table-chunk tbody tr:hover {
        color:#3a84ff;
        background-color: #eaf3ff;
    }
    .table-chunk tbody tr.selected {
        color:#3a84ff;
        background-color: #f4f6fa;
    }

    .path-bar {
        display: flex;
        height: 44px;
        line-height: 44px;
        color: #555;
        padding: 0 10px;
        border: 1px solid #d5dbdf;
        border-bottom: 0 none;
        background-color: #f7f9fb;

        .field {
            font-size: 14px;
            flex: none;
        }

        .file-path {
            flex: 1;
            overflow: hidden;
            white-space: nowrap;

            input[type="text"] {
                width: 100%;
                color: #555;
                font-size: 14px;
                border: 0 none;
                background: none;
                line-height: 44px;
                cursor: text;
                outline: none;
            }
        }
    }

    .code-editor {
        height: calc(100vh - 170px);
        border: 1px solid #d1d1d1;
        max-width: 100%;
        position: relative;

        .placeholder {
            position: absolute;
            display: flex;
            align-items: center;
            width: 100%;
            height: 100%;
            z-index: 9;
            background-color: #f7f9fb;

            .placeholder-content {
                flex: 1;
                font-size: 18px;
                color: #bfbfbf;
                text-align: center;
                user-select: none;
            }
        }
    }

    .select-chunk-text .t1 {
        padding-right: 25px;
    }

    .select-chunk-text .t2 {
        font-style: normal;
        padding-right: 10px;
    }

    .chunk-text-flex {
        display: flex;
    }

    .select-chunk-text .t0 {
        float: left;
        padding-right: 5px;
    }

    .target-code .select-chunk-text .t1 {
        padding-right: 25px;
        max-width: 300px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        float: left;
        text-align: left;
    }

    .target-code .select-chunk-text .t2 {
        display: inline;
    }

    >>>.bk-dropdown-menu .bk-dropdown-content {
        padding: 0;
    }
</style>
