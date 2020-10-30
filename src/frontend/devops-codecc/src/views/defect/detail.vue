<template>
    <div :class="['code-fullscreen', { 'full-active': isFullScreen }]" v-bkloading="{ isLoading: isLoading, opacity: 0.3 }">
        <i class="bk-icon toggle-full-icon" :class="isFullScreen ? 'icon-un-full-screen' : 'icon-full-screen'" @click="setFullScreen"></i>
        <div class="col-main">
            <div class="file-bar">
                <div class="filemeta" v-if="lintDetail">
                    <b class="filename">{{(lintDetail.filePath || '').split('/').pop()}}</b>
                    <div v-show="type === 'file'" class="filepath" :title="lintDetail.filePath">{{$t('文件路径')}}：{{lintDetail.filePath}}</div>
                </div>
                <bk-button class="fr mr10" theme="primary" @click="locateFirst()">{{$t('问题位置')}}</bk-button>
            </div>
            <div id="codeViewerInDialog" :class="isFullScreen ? 'full-code-viewer' : 'un-full-code-viewer'" @click="handleCodeViewerInDialogClick"></div>
        </div>
        <div v-show="type === 'file'" class="col-aside">
            <div class="basic-info" v-if="currentFile">
                <dl class="basic-info">
                    <div class="item">
                        {{$t('问题数')}}{{lintDetail.lintDefectList && lintDetail.lintDefectList.length}}，已全部展示
                    </div>
                </dl>
            </div>
            <div class="operate-section" :class="{ 'full-operate-section': isFullScreen }">
                <bk-table
                    height="100%"
                    class="defect-list-table"
                    row-class-name="list-row"
                    highlight-current-row="true"
                    :empty-text="$t('未选择文件')"
                    ref="defectListTableInDialog"
                    :data="lintDetail.lintDefectList"
                    @row-click="handleDefectListRowInDialogClick">
                    <bk-table-column width="30" class-name="mark-row">
                        <template slot-scope="props">
                            <span v-if="props.row.mark === 1" v-bk-tooltips="'已标记处理'" class="codecc-icon icon-mark"></span>
                            <span v-if="props.row.mark === 2" v-bk-tooltips="'标记处理后重新扫描仍为问题'" class="codecc-icon icon-mark re-mark"></span>
                        </template>
                    </bk-table-column>
                    <bk-table-column width="83" :label="$t('所在行')" prop="lineNum"></bk-table-column>
                    <bk-table-column
                        width="86"
                        :label="$t('问题级别')"
                        prop="severity">
                        <template slot-scope="props">
                            <span :class="`color-${{ 1: 'major', 2: 'minor', 3: 'info' }[props.row.severity]}`">{{defectSeverityDetailMap[props.row.severity]}}</span>
                        </template>
                    </bk-table-column>
                    <div slot="empty">
                        <div class="codecc-table-empty-text">
                            <img src="../../images/empty.png" class="empty-img">
                            <div>{{$t('未选择文件')}}</div>
                        </div>
                    </div>
                </bk-table>
            </div>
            <div class="toggle-file">
                <bk-button :disabled="fileIndex - 1 < 0" @click="triggerRowClick('prev')">{{$t('上一文件')}}</bk-button>
                <bk-button :disabled="fileIndex + 1 >= totalCount" @click="triggerRowClick('next')">{{$t('下一文件')}}</bk-button>
            </div>
        </div>

        <div v-show="type === 'defect'" class="col-aside">
            <div class="operate-section-defect">
                <div class="basic-info-defect" :class="{ 'full-screen-info': isFullScreen }" v-if="currentFile">
                    <div class="block">
                        <div class="item">
                            <span class="fail" v-if="currentFile.status === 1"><span class="cc-dot"></span>{{$t('待修复')}}</span>
                            <span class="success" v-else-if="currentFile.status & 2"><span class="cc-dot"></span>{{$t('已修复')}}</span>
                            <span class="warn" v-else-if="currentFile.status & 4"><span class="cc-dot"></span>{{$t('已忽略')}}</span>
                            <span v-if="currentFile.status === 1 && currentFile.mark" class="cc-mark">
                                <span v-if="currentFile.mark === 1" v-bk-tooltips="'已标记处理'">
                                    <span class="codecc-icon icon-mark"></span>
                                    <span>{{$t('已标记处理')}}</span>
                                </span>
                                <span v-if="currentFile.mark === 2" v-bk-tooltips="'标记处理后重新扫描仍为问题'">
                                    <span class="codecc-icon icon-mark re-mark"></span>
                                    <span>{{$t('已标记处理')}}</span>
                                </span>
                            </span>
                        </div>
                        <div v-if="currentFile.status === 1" class="item">
                            <bk-button v-if="currentFile.mark" class="item-button" @click="handleMark(0, false, entityId)">
                                {{$t('取消标记')}}
                            </bk-button>
                            <bk-button v-else theme="primary" class="item-button" @click="handleMark(1, false, entityId)">
                                {{$t('标记处理')}}
                            </bk-button>
                        </div>
                        <div class="item">
                            <bk-button class="item-button" @click="handleComent(entityId)">{{$t('评论')}}</bk-button>
                        </div>
                        <div class="item">
                            <bk-button v-if="currentFile.status & 4" class="item-button" @click="handleIgnore('RevertIgnore', false, entityId)">
                                {{$t('恢复忽略')}}
                            </bk-button>
                            <bk-button v-else-if="!(currentFile.status & 2)" class="item-button" @click="handleIgnore('IgnoreDefect', false, entityId)">
                                {{$t('忽略问题')}}
                            </bk-button>
                        </div>
                    </div>
                    <div class="block">
                        <div class="item">
                            <dt>{{$t('ID')}}</dt>
                            <dd>{{currentFile.entityId}}</dd>
                        </div>
                        <div class="item">
                            <dt>{{$t('级别')}}</dt>
                            <dd>{{defectSeverityMap[currentFile.severity]}}</dd>
                        </div>
                    </div>
                    <div class="block">
                        <div class="item">
                            <dt>{{$t('创建时间')}}</dt>
                            <dd class="small">{{currentFile.createTime | formatDate}}</dd>
                        </div>
                        <div class="item" v-if="currentFile.status & 2">
                            <dt>{{$t('修复时间')}}</dt>
                            <dd class="small">{{currentFile.fixedTime | formatDate}}</dd>
                        </div>
                        <div class="item">
                            <dt>{{$t('首次发现')}}</dt>
                            <dd>{{currentFile.createBuildNumber ? '#' + currentFile.createBuildNumber : '--'}}</dd>
                        </div>
                        <div class="item">
                            <dt>{{$t('提交时间')}}</dt>
                            <dd class="small">{{currentFile.lineUpdateTime | formatDate}}</dd>
                        </div>
                    </div>
                    <div class="block" v-if="currentFile.status & 4">
                        <div class="item">
                            <dt>{{$t('忽略时间')}}</dt>
                            <dd class="small">{{currentFile.ignoreTime | formatDate}}</dd>
                        </div>
                        <div class="item">
                            <dt>{{$t('忽略人')}}</dt>
                            <dd>{{currentFile.ignoreAuthor}}</dd>
                        </div>
                        <div class="item disb">
                            <dt>{{$t('忽略原因')}}</dt>
                            <dd>{{getIgnoreReasonByType(currentFile.ignoreReasonType)}}
                                {{currentFile.ignoreReason ? '：' + currentFile.ignoreReason : ''}}
                            </dd>
                        </div>
                    </div>
                    <div class="block">
                        <div class="item">
                            <dt v-if="currentFile.status === 1" class="curpt" @click.stop="handleAuthor(1, entityId, currentFile.author)">
                                {{$t('处理人')}}
                                <span class="bk-icon icon-edit2 fs20"></span>
                            </dt>
                            <dt v-else>
                                {{$t('处理人')}}
                            </dt>
                            <dd>{{currentFile.author}}</dd>
                        </div>
                    </div>
                    <div class="block">
                        <div class="item disb">
                            <dt>{{$t('规则')}}</dt>
                            <dd>{{currentFile.checker}}</dd>
                        </div>
                    </div>
                    <div class="block">
                        <div class="item disb">
                            <dt>{{$t('代码库路径')}}</dt>
                            <a target="_blank" :href="lintDetail.filePath">{{lintDetail.filePath}}</a>
                        </div>
                    </div>
                </div>
                <div class="toggle-file">
                    <bk-button :disabled="fileIndex - 1 < 0" @click="triggerRowClick('prev')">{{$t('上一问题')}}</bk-button>
                    <bk-button :disabled="fileIndex + 1 >= totalCount" @click="triggerRowClick('next')">{{$t('下一问题')}}</bk-button>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    import CodeMirror from '@/common/codemirror'
    import { getClosest, addClass, formatDiff } from '@/common/util'
    import { format } from 'date-fns'
    import { bus } from '@/common/bus'

    export default {
        props: {
            type: {
                type: String,
                default: 'file'
            },
            isLoading: {
                type: Boolean,
                default: false
            },
            visiable: {
                type: Boolean,
                default: false
            },
            isFullScreen: {
                type: Boolean,
                default: false
            },
            currentFile: {
                type: Object,
                default: {}
            },
            fileIndex: {
                type: Number,
                default: 0
            },
            entityId: {
                type: String
            },
            totalCount: {
                type: Number,
                default: 0
            },
            lintDetail: {
                type: Object,
                default: {}
            },
            handleMark: {
                type: Function
            },
            handleComent: {
                type: Function
            },
            deleteComment: {
                type: Function
            },
            handleIgnore: {
                type: Function
            },
            handleAuthor: {
                type: Function
            },
            triggerRowClick: {
                type: Function
            }
        },
        data () {
            return {
                toolId: this.$route.params.toolId,
                defectSeverityMap: {
                    1: this.$t('严重'),
                    2: this.$t('一般'),
                    3: this.$t('提示'),
                    4: this.$t('提示')
                },
                defectSeverityDetailMap: {
                    1: this.$t('严重'),
                    2: this.$t('一般'),
                    3: this.$t('提示'),
                    4: this.$t('提示')
                },
                currentDefectDetail: {
                    hintId: undefined,
                    eventTimes: 0,
                    eventSource: undefined
                },
                codeViewerInDialog: null,
                codeMirrorDefaultCfg: {
                    lineNumbers: true,
                    scrollbarStyle: 'simple',
                    theme: 'summerfruit',
                    lineWrapping: true,
                    placeholder: this.emptyText,
                    firstLineNumber: 1,
                    readOnly: true
                },
                commentList: [],
                scrollLine: 0
            }
        },
        watch: {
            currentDefectDetail: {
                handler () {
                    this.emptyText = this.$t('未选择文件')
                    this.locateHint()
                },
                deep: true
            },
            visiable (val) {
                if (!val) {
                    this.codeViewerInDialog.setValue('')
                    this.codeViewerInDialog.setOption('firstLineNumber', 1)
                }
            }
        },
        methods: {
            setFullScreen () {
                this.$emit('update:is-full-screen', !this.isFullScreen)
            },
            handleCodeFullScreen (type) {
                setTimeout(() => {
                    const width = 580 - document.getElementsByClassName('filename')[0].offsetWidth
                    document.getElementsByClassName('filepath')[0].style.width = width + 'px'
                }, 0)

                if (!this.codeViewerInDialog) {
                    const codeMirrorConfig = {
                        ...this.codeMirrorDefaultCfg,
                        ...{ autoRefresh: true }
                    }
                    this.codeViewerInDialog = CodeMirror(document.getElementById('codeViewerInDialog'), codeMirrorConfig)

                    this.codeViewerInDialog.on('update', () => {})
                }

                this.updateCodeViewer(type)
                document.codeM = this.codeViewerInDialog
            },
            // 代码展示相关
            updateCodeViewer (type) {
                const codeViewer = this.codeViewerInDialog
                const { fileName, fileContent, trimBeginLine } = this.lintDetail
                const codeMirrorMode = CodeMirror.findModeByFileName(fileName)
                const mode = codeMirrorMode && codeMirrorMode.mode
                import(`codemirror/mode/${mode}/${mode}.js`).then(m => {
                    codeViewer.setOption('mode', mode)
                }).finally(() => {
                    codeViewer.setOption('firstLineNumber', trimBeginLine !== 0 ? trimBeginLine : 1)
                    if (!this.lintDetail.fileContent) {
                        this.emptyText = this.$t('文件内容为空')
                        codeViewer.setValue(this.emptyText)
                    } else {
                        codeViewer.setValue(fileContent)
                    }
                    // 创建问题提示块
                    this.buildLintHints(codeViewer)
                    codeViewer.refresh()
                    this.locateHint()
                    if (type === 'scroll') this.handleScroll()
                })
            },
            // 创建问题提示块
            buildLintHints (codeViewer) {
                const { defectSeverityDetailMap } = this
                const { lintDefectList } = this.lintDetail
                // const defectListCount = lintDefectList.length
                const { trimBeginLine } = this.lintDetail
                lintDefectList.forEach((item, i) => {
                    let checkerComment = ''
                    let handleContent = ''
                    // 与 codemirror 行号对齐
                    const lineNum = item.lineNum - 1

                    // 下一问题与当前问题处在同一行，用于输出时合并作者
                    // const isSameLine = i < defectListCount - 1 && lineNum === lintDefectList[i + 1].lineNum - 1

                    const messageDom = document.createElement('span')
                    messageDom.innerText = `${item.message}`
                    const hints = document.createElement('div')
                    const hintId = `${lineNum}-${i}`
                    const hasComment = item.codeComment

                    if (this.type === 'file') {
                        handleContent = `
                            <p class="handle-head">
                                <a class="btn codecc-icon ${item.mark ? (item.mark === 1 ? 'icon-marked' : 'icon-marked re-marked') : 'icon-mark un-mark'}"
                                    data-option="mark" 
                                    data-entityid="${item.entityId}" 
                                    data-mark="${item.mark}">
                                    <span>${item.mark ? '取消标记为已处理' : '标记为已处理'}</span>
                                </a>
                                <a class="btn codecc-icon icon-ignore" data-option="ignore" data-entityid="${item.entityId}">
                                    <span>忽略问题</span>
                                </a>
                                <a class="btn codecc-icon icon-comment" data-option="comment"
                                    data-entityid="${item.entityId}"
                                    data-commentId="${hasComment ? item.codeComment.entityId : ''}">
                                    <span>评论问题</span>
                                </a>
                            </p>
                        `
                    } else handleContent = ''

                    if (item.codeComment) {
                        for (let i = 0; i < item.codeComment.commentList.length; i++) {
                            checkerComment += `
                                <p class="comment-item">
                                    <span class="info">
                                        <i class="codecc-icon icon-commenter"></i>
                                        <span>${item.codeComment.commentList[i]['userName']}</span>
                                        <span title="${item.codeComment.commentList[i]['comment']}">${item.codeComment.commentList[i]['comment']}</span>
                                    </span>
                                    <span class="handle">
                                        <span>${this.formatTime(item.codeComment.commentList[i]['commentTime'])}</span>
                                        <i class="bk-icon icon-delete" 
                                            data-singlecommentid="comment-${item.codeComment.commentList[i]['singleCommentId']}"
                                            data-commentid="comment-${item.codeComment.entityId}"
                                        ></i>
                                    </span>
                                </p>`
                        }
                    } else checkerComment = ''

                    hints.innerHTML = `
                        <i class="lint-icon bk-icon icon-right-shape"></i>
                        <div class="lint-info">
                            <div class="lint-head">
                                ${messageDom.outerHTML}
                                ${handleContent}
                            </div>
                            <p class="tag-line">
                                <span class="tag">
                                    <span class="bk-icon icon-exclamation-circle-shape type-${item.severity}"></span>
                                    ${item.checker} ${item.checkerType ? '| ' + item.checkerType : ''} | ${defectSeverityDetailMap[item.severity]}
                                </span>
                                <span class="tag">
                                    <span class="codecc-icon icon-creator"></span>
                                    ${item.author || '--'}
                                    ${this.type === 'defect' ? '' : `<span class="bk-icon icon-edit2 fs20" data-entityid="${item.entityId}" data-author="${item.author || '--'}"></span>`}
                                </span>
                                <span class="tag">
                                    <span class="codecc-icon icon-time"></span>
                                    ${this.formatDate(item.createTime)}
                                    ${item.createBuildNumber ? '#' + item.createBuildNumber : '--'}创建
                                </span>
                            </p>
                            <p class="checker-detail">${item.checkerDetail}</p>
                            ${checkerComment ? `<div class="checker-comment">${checkerComment}</div>` : ''}
                        </div>
                    `
                    hints.className = `lint-hints linter lint-hints-${hintId}`
                    hints.dataset.hintId = hintId

                    codeViewer.addLineWidget(lineNum - trimBeginLine, hints, {
                        coverGutter: false,
                        noHScroll: false,
                        above: true
                    })
                    codeViewer.addLineClass(lineNum - trimBeginLine, 'wrap', 'lint-hints-wrap main linter')
                    bus.$emit('hide-app-loading')
                    // codeViewer.refresh()
                })
            },
            handleCodeViewerInDialogClick (event, eventSource) {
                this.codeViewerClick(event, 'dialog-code')
            },
            codeViewerClick (event, eventSource) {
                const lintHints = getClosest(event.target, '.lint-hints')
                const headHanle = getClosest(event.target, '.btn')
                const editAuthor = getClosest(event.target, '.icon-edit2')
                const commentCon = getClosest(event.target, '.checker-comment')
                const delHandle = getClosest(event.target, '.icon-delete')

                if (lintHints) {
                    const hintId = lintHints.dataset.hintId
                    this.scrollLine = hintId.split('-')[0]
                }

                // 如果点击的是标记/忽略/评论按钮
                if (headHanle) {
                    const type = headHanle.getAttribute('data-option')
                    const entityId = headHanle.dataset['entityid']
                    if (type === 'comment') {
                        const commentId = headHanle.getAttribute('data-commentId')
                        this.handleComent(entityId, commentId)
                    } else if (type === 'mark') {
                        let mark = headHanle.dataset['mark']
                        mark = mark === '0' || mark === 'undefined' ? 1 : 0
                        this.handleMark(mark, false, entityId)
                    } else if (type === 'ignore') {
                        this.handleIgnore('IgnoreDefect', false, entityId)
                    }
                    return
                }
                if (editAuthor) {
                    const author = editAuthor.dataset['author']
                    const entityId = editAuthor.dataset['entityid']
                    this.handleAuthor(1, entityId, author)
                    return
                }
                // 如果点击的是删除评论
                if (delHandle) {
                    const that = this
                    this.$bkInfo({
                        title: '删除评论',
                        subTitle: '确定要删除该条评论吗？',
                        maskClose: true,
                        confirmFn () {
                            const delSingleObj = delHandle.getAttribute('data-singlecommentid')
                            const delCommentObj = delHandle.getAttribute('data-commentid')
                            const singleCommentId = delSingleObj.split('-').pop()
                            const commentId = delCommentObj.split('-').pop()
                            that.deleteComment(commentId, singleCommentId)
                        }
                    })
                    return
                }
                // 如果点击的是lint问题区域
                if (lintHints && !commentCon) {
                    // 触发watch
                    this.currentDefectDetail.hintId = lintHints.dataset.hintId
                    this.currentDefectDetail.eventSource = eventSource
                    this.currentDefectDetail.eventTimes++
                }
            },
            handleDefectListRowInDialogClick (row, event, column) {
                if (!this.lintDetail.fileContent) return

                // 代码所在行
                const lineNum = row.lineNum - 1

                // 得到表格行索引
                const rowIndex = event ? getClosest(event.target, 'tr').rowIndex : 0

                // 记录当前问题id
                const hintId = `${lineNum}-${rowIndex}`

                // 触发watch
                this.currentDefectDetail.hintId = hintId
                this.currentDefectDetail.eventSource = 'dialog-row'
                this.currentDefectDetail.eventTimes++
            },
            locateHint () {
                const eventFrom = this.currentDefectDetail.eventSource.split('-').shift()
                // 默认处理页面中的代码展示
                this.locateHintByName(eventFrom)
            },
            locateHintByName (name, visiableToggle) {
                const { hintId, eventSource } = this.currentDefectDetail

                // 确实存在未点击问题直接打开全屏的情况，这种情况没有hintId
                if (!hintId) {
                    return
                }

                const [lineNum, rowIndex] = hintId.split('-')
                const eventTrigger = eventSource.split('-').pop()
                const defectListTable = name === 'main' ? this.$refs.defectListTable : this.$refs.defectListTableInDialog
                const defectListTableBodyWrapper = defectListTable.$refs.bodyWrapper
                const codeViewer = name === 'main' ? this.codeViewer : this.codeViewerInDialog
                const lintWrapper = codeViewer.getWrapperElement()

                const rows = defectListTableBodyWrapper.querySelectorAll('tr')
                const currentRow = rows[rowIndex]

                // 表格行加当前高亮样式
                rows.forEach(el => el.classList.remove('current-row'))
                if (currentRow) {
                    addClass(currentRow, 'current-row')
                    if (eventTrigger === 'code') {
                        // 滚动到当前表格行
                        defectListTableBodyWrapper.scrollTo({
                            top: currentRow.offsetTop,
                            behavior: 'smooth'
                        })
                    }
                }

                if (eventTrigger === 'row' || visiableToggle === true) {
                    // 滚动到问题代码位置
                    const { trimBeginLine } = this.lintDetail
                    const middleHeight = codeViewer.getScrollerElement().offsetHeight / 2
                    const lineHeight = codeViewer.defaultTextHeight()
                    codeViewer.scrollIntoView({ line: lineNum - trimBeginLine, ch: 0 }, middleHeight - lineHeight)
                }

                // 问题代码区域高亮
                const lintHints = lintWrapper.getElementsByClassName(`lint-hints-${hintId}`)
                this.activeLintHint(lintHints[0])
            },
            activeLintHint (lintHint) {
                if (!lintHint) return
                // 切换所有lint wrap的active
                const lintHintsWrap = getClosest(lintHint, '.lint-hints-wrap')
                const isActive = lintHint.classList.contains('active')
                document.querySelectorAll('.active').forEach(el => el.classList.remove('active'))
                if (!isActive) {
                    addClass(lintHint, 'active')
                    addClass(lintHintsWrap, 'active')
                }
            },
            locateFirst () {
                this.$nextTick(() => {
                    this.handleDefectListRowInDialogClick(this.lintDetail.lintDefectList[0])
                })
            },
            getIgnoreReasonByType (type) {
                const typeMap = {
                    1: this.$t('检查工具误报'),
                    2: this.$t('设计如此'),
                    4: this.$t('其他')
                }
                return typeMap[type]
            },
            formatTime (time) {
                return formatDiff(time)
            },
            formatDate (date) {
                return date ? format(date, 'YYYY-MM-DD') : '--'
            },
            handleScroll () {
                const codeViewer = this.codeViewerInDialog
                const lineNum = this.scrollLine || this.currentFile.lineNum - 1
                const { trimBeginLine } = this.lintDetail
                const middleHeight = codeViewer.getScrollerElement().offsetHeight / 2
                const lineHeight = codeViewer.defaultTextHeight()
                codeViewer.scrollIntoView({ line: lineNum - trimBeginLine, ch: 0 }, middleHeight - lineHeight)
            }
        }
    }
</script>

<style lang="postcss" scoped>
    @import '../../css/mixins.css';

    .code-fullscreen {
        display: flex;
        &.full-active {
            padding-top: 30px;
        }
        .toggle-full-icon {
            position: absolute;
            top: -22px;
            right: 14px;
            color: #979ba5;
            cursor: pointer;
            &.icon-un-full-screen {
                top: 8px;
            }
        }
        .col-main {
            flex: 1;
            max-width: calc(100% - 250px);
        }
        .col-aside {
            flex: none;
            width: 240px;
            background: #f0f1f5;
            padding: 12px 20px;
            margin-left: 16px;

            .operate-section {
                height: calc(100vh - 270px);
                &.full-operate-section {
                    height: calc(100vh - 170px);
                }
            }
        }

        .file-bar {
            height: 42px;

            .filemeta {
                display: inline-block;
                margin-top: -2px;
                font-size: 12px;
                border-left: 4px solid #3a84ff;
                padding-left: 8px;
                .filename {
                    font-size: 16px;
                }
                .filepath {
                    width: 700px;
                    display: inline-block;
                    vertical-align: bottom;
                    margin-left: 8px;
                    line-height: 24px;
                    @mixin ellipsis;
                }
            }
        }
        .toggle-file {
            text-align: center;
            display: flex;
            justify-content: space-between;
            margin: 10px 0;
        }

        .operate-section,
        .basic-info {
            .title {
                font-size: 14px;
                color: #313238;
            }
        }

        .basic-info {
            .item {
                display: flex;
                padding-bottom: 9px;

                dt {
                    width: 90px;
                    flex: none;
                }
                dd {
                    flex: 1;
                    color: #313238;
                }
            }
        }
        .operate-section-defect {
            height: 100%;
        }
        .basic-info-defect {
            height: calc(100% - 60px);
            max-height: calc(100vh - 240px);
            overflow-y: scroll;
            margin-right: -29px;
            padding-right: 20px;
            &.full-screen-info {
                max-height: calc(100vh - 140px);
            }
            .title {
                font-size: 14px;
                color: #313238;
            }
            .block {
                padding: 5px 0;
                border-bottom: 1px dashed #c4c6cc;
                &:last-of-type {
                    border-bottom: none;
                    padding-bottom: 20px;
                }
                .item {
                    display: flex;
                    padding: 5px 0;

                    dt {
                        width: 90px;
                        flex: none;
                    }
                    dd {
                        /* flex: 1; */
                        color: #313238;
                        word-break: break-all;
                        &.small {
                            width: 80px;
                        }
                    }
                    a {
                        color: #313238;
                        word-break: break-all;
                    }
                    .item-button {
                        width: 200px;
                    }
                }
            }
        }
    }

    #codeViewerInDialog {
        font-size: 14px;
        width: 100%;
        border: 1px solid #eee;
        border-left: none;
        border-right: none;
    }
    .un-full-code-viewer {
        height: calc(100vh - 200px);
    }
    .full-code-viewer {
        height: calc(100vh - 100px);
    }
    >>>.icon-mark {
        color: #53cad1;
        &.re-mark {
            color: #facc48;
        }
        &.un-mark {
            color: #b0b0b0;
        }
    }
    >>>.icon-marked {
        color: #53cad1;
        font-size: 18px;
        &.re-marked {
            color: #facc48;
        }
    }
    .cc-mark {
        width: 114px;
        background: white;
        border-radius: 12px;
        padding: 0 8px;
        line-height: 23px;
        margin-left: 27px;
    }
    >>>.bk-table {
        .mark-row {
            .cell {
                padding-left: 15px!important;
            }
        }
    }
</style>
