<template>
    <div>
        <div class="breadcrumb">
            <div class="breadcrumb-name">{{breadcrumb.name}}</div>
            <div class="breadcrumb-extra">
                <a href="javascript:;" @click.prevent="handleAuthorEdit">
                    <i class="bk-icon icon-panel-permission"></i>{{$t('nav.处理人修改')}}
                </a>
                <span class="line">|</span>
                <a @click="openSlider"><i class="bk-icon icon-order"></i>{{$t('nav.操作记录')}}</a>
            </div>
        </div>
        <div class="main-container">
            <div class="main-content-inner main-content-list">
                <bk-form :label-width="90" class="search-form">
                    <bk-container col="3" margin="0" gutter="20">
                        <bk-row>
                            <bk-col :span="1">
                                <bk-form-item :label="$t('defect.风险级别')" label-width="64">
                                    <bk-checkbox-group v-model="searchParams.severity" class="checkbox-group">
                                        <bk-checkbox
                                            v-for="(name, value, index) in defectSeverityMap"
                                            :value="Number(value)"
                                            :key="index"
                                        >
                                            {{name}}(<em :class="['count', `count-${['major', 'minor', 'info'][index]}`]">{{getDefectCountBySeverity(value)}}</em>)
                                        </bk-checkbox>
                                    </bk-checkbox-group>
                                </bk-form-item>
                            </bk-col>
                            <bk-col :span="1">
                                <bk-form-item :label="$t('defect.处理人')">
                                    <bk-select v-model="searchParams.author" searchable>
                                        <bk-option
                                            v-for="(author, index) in searchFormData.authorList"
                                            :key="index"
                                            :id="author"
                                            :name="author"
                                        >
                                        </bk-option>
                                    </bk-select>
                                </bk-form-item>
                            </bk-col>
                            <bk-col :span="1">
                                <bk-form-item :label="$t('defect.文件路径')" class="fixed-width">
                                    <bk-dropdown-menu @show="isFilePathDropdownShow = true" @hide="isFilePathDropdownShow = false" align="right" trigger="click" ref="filePathDropdown">
                                        <bk-button type="primary" slot="dropdown-trigger">
                                            <div class="filepath-name" :title="searchFormData.filePathShow">{{searchFormData.filePathShow ? searchFormData.filePathShow : $t('st.请选择')}}</div>
                                            <i :class="['bk-icon icon-angle-down', { 'icon-flip': isFilePathDropdownShow }]"></i>
                                        </bk-button>
                                        <div class="filepath-dropdown-content" slot="dropdown-content" @click="e => e.stopPropagation()">
                                            <div class="content-hd">
                                                <bk-input v-model="searchInput" :clearable="true" :placeholder="$t('defect.搜索文件夹、告警路径名称')" @input="handleFilePathSearch"></bk-input>
                                            </div>
                                            <div class="content-bd">
                                                <bk-tree
                                                    ref="filePathTree"
                                                    :data="searchFormData.filePathTree.name ? [searchFormData.filePathTree] : []"
                                                    :node-key="'id'"
                                                    :multiple="true"
                                                    :has-border="true"
                                                >
                                                </bk-tree>
                                            </div>
                                            <div class="content-ft">
                                                <bk-button theme="primary" @click="handleFilePathConfirmClick">{{$t('op.确定')}}</bk-button>
                                                <bk-button @click="handleFilePathCancelClick">{{$t('op.取消')}}</bk-button>
                                                <bk-button class="clear-btn" theme="primary" @click="handleFilePathClearClick">{{$t('op.清空选择')}}</bk-button>
                                            </div>
                                        </div>
                                    </bk-dropdown-menu>
                                </bk-form-item>
                            </bk-col>
                        </bk-row>
                    </bk-container>
                </bk-form>

                <bk-table
                    highlight-current-row="true"
                    class="file-list-table"
                    row-class-name="list-row"
                    :empty-text="$t('st.暂无数据')"
                    ref="fileListTable"
                    :data="defectList"
                    :pagination="pagination"
                    @page-change="handlePageChange"
                    @page-limit-change="handlePageLimitChange"
                    @row-click="handleFileListRowClick"
                    @sort-change="handleSortChange"
                >
                    <bk-table-column type="index" :label="$t('defect.序号')" width="110"></bk-table-column>
                    <bk-table-column :label="$t('defect.函数名')" prop="functionName">
                        <template slot-scope="props"> <span :title="props.row.functionName">{{ props.row.functionName }}</span></template>
                    </bk-table-column>
                    <bk-table-column :label="$t('defect.圈复杂度')" prop="ccn" sortable="custom"></bk-table-column>
                    <bk-table-column :label="$t('defect.风险')" prop="riskFactor">
                        <template slot-scope="props">
                            <span :class="`color-${{ 1: 'major', 2: 'minor', 4: 'info' }[props.row.riskFactor]}`">{{defectSeverityMap[props.row.riskFactor]}}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column :label="$t('defect.处理人')" prop="author"></bk-table-column>
                    <bk-table-column :label="$t('defect.文件路径')" prop="relPath"></bk-table-column>
                    <bk-table-column :label="$t('defect.起始行')" prop="startLines"></bk-table-column>
                    <bk-table-column :label="$t('defect.修改时间')" prop="latestDateTime" :formatter="(row, column, cellValue, index) => formatTime(cellValue, 'YYYY-MM-DD')" sortable="custom"></bk-table-column>
                </bk-table>

                <bk-dialog
                    v-model="defectDetailDialogVisiable"
                    :position="{ top: 50 }"
                    :draggable="false"
                    :mask-close="false"
                    :show-footer="false"
                    :close-icon="true"
                    width="80%"
                >
                    <div class="code-fullscreen">
                        <div class="col-main">
                            <div class="file-bar">
                                <div class="filemeta" v-if="currentLintFile">
                                    <b class="filename">{{currentLintFile.functionName}}</b>
                                    <div class="filepath" :title="currentLintFile.relPath">{{$t('defect.文件路径')}}：{{currentLintFile.relPath}}</div>
                                </div>
                            </div>
                            <div id="codeViewerInDialog" @click="handleCodeViewerInDialogClick"></div>
                        </div>
                        <div class="col-aside">
                            <div class="operate-section">
                                <dl class="basic-info" v-if="currentLintFile">
                                    <div class="block">
                                        <div class="item">
                                            <dt>{{$t('defect.圈复杂度')}}</dt>
                                            <dd>{{currentLintFile.ccn}}</dd>
                                        </div>
                                        <div class="item">
                                            <dt>{{$t('defect.风险级别')}}</dt>
                                            <dd>{{defectSeverityMap[currentLintFile.riskFactor]}}</dd>
                                        </div>
                                    </div>
                                    <div class="block">
                                        <div class="item">
                                            <dt>{{$t('defect.处理人')}}</dt>
                                            <dd>{{currentLintFile.author}}</dd>
                                        </div>
                                    </div>
                                    <div class="block">
                                        <div class="item">
                                            <dt>{{$t('defect.起始行')}}</dt>
                                            <dd>{{currentLintFile.startLines}}</dd>
                                        </div>
                                        <div class="item">
                                            <dt>{{$t('defect.函数总行数')}}</dt>
                                            <dd>{{currentLintFile.totalLines}}</dd>
                                        </div>
                                    </div>
                                    <div class="block">
                                        <div class="item ignore">
                                            <dt>{{$t('defect.代码库路径')}}</dt>
                                            <dd>{{currentLintFile.url}}</dd>
                                        </div>
                                    </div>
                                    <div class="block">
                                        <div class="item ignore">
                                            <dt>{{$t('defect.忽略告警')}}</dt>
                                            <dd>{{$t('defect.在函数头或函数内添加')}} // #lizard</dd>
                                        </div>
                                    </div>
                                </dl>
                                <div class="toggle-file">
                                    <bk-button theme="primary" style="width:200px;" @click="scrollIntoView">{{$t('defect.回到函数位置')}}</bk-button>
                                </div>
                                <div class="toggle-file">
                                    <bk-button :disabled="fileIndex - 1 < 0" @click="handleFileListRowClick(lintFileList[--fileIndex])">{{$t('defect.上一函数')}}</bk-button>
                                    <bk-button :disabled="fileIndex + 1 >= totalCount" @click="handleFileListRowClick(lintFileList[++fileIndex])">{{$t('defect.下一函数')}}</bk-button>
                                </div>
                            </div>
                        </div>
                    </div>
                </bk-dialog>

            </div>
        </div>

        <bk-dialog
            v-model="authorEditDialogVisiable"
            width="560"
            theme="primary"
            :mask-close="false"
            header-position="left"
            :title="$t('defect.批量修改告警处理人')"
        >
            <div class="author-edit">
                <div class="tips"><i class="bk-icon icon-info-circle"></i>{{$t('defect.原处理人所有函数都将转给新处理人')}}</div>
                <bk-form :model="authorEditFormData" :label-width="130" class="search-form">
                    <bk-form-item property="sourceAuthor" :rules="rules.sourceAuthor" :label="$t('defect.原处理人')">
                        <bk-input v-model="authorEditFormData.sourceAuthor" style="width: 290px;"></bk-input>
                    </bk-form-item>
                    <bk-form-item :label="$t('defect.新处理人')">
                        <bk-input v-model="authorEditFormData.targetAuthor" style="width: 290px;"></bk-input>
                        <!-- <bk-member-selector v-model="authorEditFormData.targetAuthor" style="width: 290px;"></bk-member-selector> -->
                    </bk-form-item>
                </bk-form>
            </div>
            <div slot="footer">
                <bk-button
                    type="button"
                    theme="primary"
                    :disabled="!authorEditFormData.sourceAuthor.length || !authorEditFormData.targetAuthor.length"
                    :loading="authorEditDialogLoading"
                    @click.native="authorEditConfirm"
                >
                    {{$t('op.确定')}}
                </bk-button>
                <bk-button
                    theme="primary"
                    type="button"
                    :disabled="authorEditDialogLoading"
                    @click.native="authorEditCancel"
                >
                    {{$t('op.取消')}}
                </bk-button>
            </div>
        </bk-dialog>
        <Record :visiable.sync="show" :data="this.$route.name" />
    </div>
</template>

<script>
    import util from '@/mixins/util'
    import { mapState } from 'vuex'
    import { bus } from '@/common/bus'
    import CodeMirror from '@/common/codemirror'
    import { getClosest, addClass, toggleClass } from '@/common/util'
    import Record from '@/components/operate-record/index'

    export default {
        components: {
            Record
        },
        mixins: [util],
        data () {
            const query = this.$route.query

            return {
                defectSeverityMap: {
                    1: this.$t('defect.极高'),
                    2: this.$t('defect.高'),
                    4: this.$t('defect.中')
                },
                toolId: 'CCN',
                lintListData: {
                    defectList: {
                        content: [],
                        totalElements: 0
                    }
                },
                lintDetail: {},
                searchFormData: {
                    checkerList: [],
                    authorList: [],
                    filePathTree: {},
                    filePathShow: ''
                },
                searchParams: {
                    taskId: this.$route.params.taskId,
                    toolName: 'CCN',
                    checker: query.checker || '',
                    author: query.author,
                    severity: this.numToArray(query.severity),
                    fileList: [],
                    sortField: 'ccn',
                    sortType: 'DESC',
                    pageNum: 1,
                    pageSize: 50
                },
                defectDetailSearchParams: {
                    sortField: '',
                    sortType: '',
                    pattern: '',
                    filePath: '',
                    entityId: undefined
                },
                authorEditFormData: {
                    sourceAuthor: '',
                    targetAuthor: '',
                    toolName: 'CCN'
                },
                codeViewerInDialog: null,
                isFilePathDropdownShow: false,
                isFileListLoadMore: false,
                isDefectListLoadMore: false,
                defectDetailDialogVisiable: false,
                authorEditDialogVisiable: false,
                pagination: {
                    current: 1,
                    count: 1,
                    limit: 50
                },
                totalCount: 0,
                fileIndex: -1,
                codeMirrorDefaultCfg: {
                    lineNumbers: true,
                    scrollbarStyle: 'simple',
                    theme: 'darcula',
                    placeholder: this.emptyText,
                    firstLineNumber: 1,
                    readOnly: 'nocursor'
                },
                show: false,
                rules: {
                    sourceAuthor: [
                        {
                            max: 50,
                            message: '不能多于50个字符',
                            trigger: 'blur'
                        }
                    ]
                },
                searchInput: '',
                emptyText: this.$t('st.未选择文件')
            }
        },
        computed: {
            ...mapState('tool', {
                toolMap: 'mapList'
            }),
            breadcrumb () {
                const toolId = this.toolId
                let toolDisplayName = (this.toolMap[toolId] || {}).displayName || ''
                const names = [this.$route.meta.title || this.$t('nav.风险函数')]
                if (toolDisplayName) {
                    toolDisplayName = this.$t(`toolName.${toolDisplayName}`)
                    names.unshift(toolDisplayName)
                }

                return { name: names.join(' / ') }
            },
            lintFileList () {
                return this.lintListData.defectList.content
            },
            currentLintFile () {
                return this.lintFileList[this.fileIndex]
            },
            defectList () {
                return this.lintListData.defectList.content
            }
        },
        watch: {
            // 监听查询参数变化，则获取列表
            searchParams: {
                handler () {
                    this.fetchLintList().then(list => {
                        this.lintListData = { ...this.lintListData, ...list }
                        this.totalCount = this.pagination.count = this.lintListData.defectList.totalElements

                        // 重置文件下的告警详情
                        this.lintDetail = {}
                    })
                },
                deep: true
            },
            defectDetailSearchParams: {
                handler () {
                    this.emptyText = this.$t('se.未选择文件')
                    this.fetchLintDetail().then(detail => {
                        this.lintDetail = { ...this.lintDetail, ...detail }

                        // 查询详情后，全屏显示告警
                        this.handleCodeFullScreen()
                    }).finally(() => {
                        bus.$emit('hide-app-loading')
                    })
                },
                deep: true
            },
            searchInput: {
                handler () {
                    if (this.searchFormData.filePathTree.name) {
                        if (this.searchInput) {
                            // this.searchFormData.filePathTree.expanded = true
                            this.openTree(this.searchFormData.filePathTree)
                        } else {
                            this.searchFormData.filePathTree.expanded = false
                        }
                    }
                },
                deep: true
            },
            defectDetailDialogVisiable :{
                handler () {
                    if (!this.defectDetailDialogVisiable) {
                        this.codeViewerInDialog.setValue('')
                        this.codeViewerInDialog.setOption('firstLineNumber', 1)
                    }
                },
                deep: true
            }
        },
        created () {
        },
        mounted () {
        },
        methods: {
            async fetchPageData () {
                await Promise.all([
                    this.fetchLintList(),
                    this.fetchLintParams()
                ]).then(([list, params]) => {
                    this.lintListData = { ...this.lintListData, ...list }
                    this.totalCount = this.pagination.count = this.lintListData.defectList.totalElements

                    // todo 给文件路径树加上icon
                    function formatFilePath (filepath) {
                        if (filepath.children && filepath.children.length) {
                            filepath.openedIcon = 'icon-folder-open'
                            filepath.closedIcon = 'icon-folder'
                            filepath.children.forEach(formatFilePath)
                        } else {
                            filepath.icon = 'icon-file'
                        }
                    }
                    formatFilePath(params.filePathTree)

                    this.searchFormData = Object.assign({}, this.searchFormData, params)
                }).catch(e => e)
            },
            fetchLintList () {
                return this.$store.dispatch('defect/lintList', this.searchParams, { showLoading: true })
            },
            fetchLintParams () {
                const params = this.$route.params
                params.toolId = 'CCN'
                return this.$store.dispatch('defect/lintParams', params)
            },
            fetchLintDetail () {
                const pattern = this.toolMap[this.$route.params.toolId]['pattern']
                const params = { ...this.searchParams, ...this.defectDetailSearchParams, pattern }
                bus.$emit('show-app-loading')
                return this.$store.dispatch('defect/lintDetail', params)
            },
            getDefectCountBySeverity (severity) {
                const severityFieldMap = {
                    1: 'superHighCount',
                    2: 'highCount',
                    4: 'mediumCount'
                }
                const count = this.lintListData[severityFieldMap[severity]]
                return count > 100000 ? this.$t('st.10万+') : count
            },
            handleSortChange ({ column, prop, order }) {
                const orders = { ascending: 'ASC', descending: 'DESC' }
                this.searchParams = { ...this.searchParams, ...{ pageNum: 1, sortField: prop, sortType: orders[order] } }
            },
            handlePageChange (page) {
                this.pagination.current = page
                this.searchParams = { ...this.searchParams, ...{ pageNum: page } }
            },
            handlePageLimitChange (currentLimit) {
                this.pagination.current = 1 // 切换分页大小时要回到第一页
                this.searchParams = { ...this.searchParams, ...{ pageNum: 1, pageSize: currentLimit } }
            },
            handleFileListRowClick (row, event, column) {
                this.fileIndex = this.defectList.findIndex(file => file.entityId === row.entityId)
                // 筛选后，告警详情为空，此时要把参数强制置空，不然点击文件不能触发请求
                if (!this.lintDetail.lintDefectList) {
                    this.defectDetailSearchParams.entityId = ''
                }
                this.defectDetailSearchParams.entityId = row.entityId
                this.defectDetailSearchParams.filePath = row.filePath
                this.addCurrentRowClass()
            },
            // 表格行加当前高亮样式
            addCurrentRowClass () {
                const defectListTable = this.$refs.fileListTable
                const defectListTableBodyWrapper = defectListTable.$refs.bodyWrapper
                const rows = defectListTableBodyWrapper.querySelectorAll('tr')
                const currentRow = rows[this.fileIndex]
                rows.forEach(el => el.classList.remove('current-row'))
                addClass(currentRow, 'current-row')
            },
            handleCodeFullScreen () {
                this.defectDetailDialogVisiable = true
                setTimeout(() => {
                    const width = 700 - document.getElementsByClassName('filename')[0].offsetWidth
                    document.getElementsByClassName('filepath')[0].style.width = width + "px"
                }, 0)

                if (!this.codeViewerInDialog) {
                    const codeMirrorConfig = {
                        ...this.codeMirrorDefaultCfg,
                        ...{ autoRefresh: true }
                    }
                    this.codeViewerInDialog = CodeMirror(document.getElementById('codeViewerInDialog'), codeMirrorConfig)

                    this.codeViewerInDialog.on('update', () => {})
                }
                this.updateCodeViewer(this.codeViewerInDialog)
                setTimeout(this.scrollIntoView, 250)
            },
            // 代码展示相关
            updateCodeViewer (codeViewer) {
                if (!this.lintDetail.fileContent) {
                    this.emptyText = this.$t('st.文件内容为空')
                    return
                }
                const { fileName, fileContent, trimBeginLine } = this.lintDetail
                const { mode } = CodeMirror.findModeByFileName(fileName)
                import(`codemirror/mode/${mode}/${mode}.js`).then(m => {
                    codeViewer.setOption('mode', mode)
                })
                codeViewer.setValue(fileContent)
                codeViewer.setOption('firstLineNumber', trimBeginLine === 0 ? 1 : trimBeginLine)

                this.buildLintHints(codeViewer)
            },
            // 创建告警提示块
            buildLintHints (codeViewer) {
                const { trimBeginLine } = this.lintDetail
                const { ccn, startLines, totalLines } = this.currentLintFile
                const hints = document.createElement('div')
                const checkerDetail = `
                    <div>
                        <p>${this.$t('detail.如果多个函数存在相同代码路径片段，可以尝试以下技巧：')}</p>
                        <p>${this.$t('detail.技巧名称：提炼函数')}</p>
                        <p>${this.$t('detail.具体方法：将相同的代码片段独立成函数，并在之前的提取位置上条用该函数')}</p>
                        <p>${this.$t('detail.示例代码：')}</p>
                        <pre>void Example(int val){
    if(val &lt; MAX_VAL){
        val = MAX_VAL;
    }
    for(int i = 0; i &lt; val; i++){
        doSomething(i);
    }
}</pre>
            <p>${this.$t('detail.可以提炼成两个函数')}</p>
            <pre>int getValidVal(int val){
    if(val &lt; MAX_VAL){
        return MAX_VAL;
    }
    return val;
    }

    void Example(int val){
    val = getValidVal(val);
    for(int i = 0; i &lt; val; i++){
        doSomething(i);
    }
}</pre>
                    </div>`
                hints.innerHTML = `
                    <i class="lint-icon bk-icon icon-right-shape"></i>
                    <div class="lint-info">
                        <p>${this.$t('detail.圈复杂度为，超过19的建议值，请进行函数功能拆分降低代码复杂度。', { ccn: ccn })}</p>
                        <div class="checker-detail">${checkerDetail}</div>
                    </div>
                `
                hints.className = `lint-hints`
                codeViewer.addLineWidget(startLines - trimBeginLine, hints, {
                    coverGutter: false,
                    noHScroll: false,
                    above: true
                })
                for (let i = startLines - trimBeginLine; i < startLines + totalLines - trimBeginLine; i++) {
                    codeViewer.addLineClass(i, 'wrap', 'lint-hints-wrap')
                }
                setTimeout(this.scrollIntoView, 1)
            },
            // 默认滚动到告警位置
            scrollIntoView () {
                const { trimBeginLine } = this.lintDetail
                const codeViewer = this.codeViewerInDialog
                const startLines = this.currentLintFile.startLines - 1
                const top = codeViewer.charCoords({ line: startLines - trimBeginLine, ch: 0 }, 'local').top
                const lineHeight = codeViewer.defaultTextHeight()
                codeViewer.scrollTo(0, top - 5 * lineHeight)
            },
            handleCodeViewerInDialogClick (event, eventSource) {
                this.codeViewerClick(event, 'dialog-code')
            },
            codeViewerClick (event, eventSource) {
                const lintHints = getClosest(event.target, '.lint-hints')

                // 如果点击的是lint告警区域，展开修复建议
                if (lintHints) {
                    toggleClass(lintHints, 'active')
                }
            },
            // 处理人修改
            handleAuthorEdit () {
                this.authorEditDialogVisiable = true
            },
            authorEditConfirm () {
                const data = this.authorEditFormData
                data.sourceAuthor = [data.sourceAuthor]
                data.targetAuthor = [data.targetAuthor]
                this.$store.dispatch('defect/authorEdit', data, { showLoading: true }).then(res => {
                    if (res.code === '0') {
                        this.authorEditDialogVisiable = false
                        this.$bkMessage({
                            theme: 'success',
                            message: this.$t('op.修改成功')
                        })
                        this.fetchPageData()
                        this.lintDetail = {}
                    }
                }).catch(e => {
                    console.error(e)
                })
            },
            authorEditCancel () {
                this.authorEditDialogVisiable = false
            },

            // 文件路径相关交互
            handleFilePathSearch (val) {
                this.$refs.filePathTree.searchNode(val)
            },
            handleFilePathConfirmClick () {
                const filePathDropdown = this.$refs.filePathDropdown

                const filePath = this.getFilePath()
                this.searchFormData.filePathShow = filePath.join(';')

                filePathDropdown.hide()
            },
            handleFilePathCancelClick () {
                const filePathDropdown = this.$refs.filePathDropdown
                filePathDropdown.hide()
            },
            handleFilePathClearClick () {
                const filePathDropdown = this.$refs.filePathDropdown
                function cancelSelected (item) {
                    item.checked = false
                    item.halfcheck = false
                    if (item.children) {
                        item.children.forEach(cancelSelected)
                    }
                }
                cancelSelected(this.searchFormData.filePathTree)

                const filePath = this.getFilePath()
                this.searchFormData.filePathShow = filePath.join(';')

                filePathDropdown.hide()
            },
            getFilePath () {
                const filePathTree = this.$refs.filePathTree
                const checkedList = filePathTree.getNode(['name', 'parent'])

                const pathMap = {}
                const getPath = function (node, path) {
                    if (node.parent) {
                        if (node.parent.halfcheck === false && !node.hasOwnProperty('halfcheck')) {
                            path.push(node.parent.name)
                            if (node.parent.parent) {
                                getPath(node.parent.parent, path)
                            }
                        } else {
                            path.push(node.name)
                            getPath(node.parent, path)
                        }
                    } else {
                        path.push(node.name)
                    }
                }
                checkedList.forEach(node => {
                    pathMap[node.name] = []
                    getPath(node, pathMap[node.name])
                })
                const pathList = Object.keys(pathMap).map(key => {
                    // console.log(pathMap[key])
                    const path = pathMap[key]
                    path.pop()
                    return path.reverse().join('/')
                })

                const pathListFinal = pathList.filter((item, index) => {
                    return pathList.indexOf(item) === index
                })

                this.searchParams.fileList = pathListFinal

                return pathListFinal
            },
            openSlider () {
                this.show = true
            },
            numToArray (num, arr = [1, 2, 4]) {
                let filterArr = arr.filter(x => x & num)
                filterArr = filterArr.length ? filterArr : arr
                return filterArr
            },
            openTree (arr) {
                if (arr.children) {
                    arr.expanded = true
                    arr.children.forEach(item => {
                        this.openTree(item)
                    })
                }
            }
        }
    }
</script>

<style>
    @import './codemirror.css';
</style>

<style lang="postcss" scoped>
    @import '../../css/mixins.css';
    @import './index.css';

    .main-container {
        padding: 20px 33px 0!important;
        margin: 0 -13px!important;
    }
    .file-list-table {
        >>> .list-row {
            cursor: pointer;
        }
    }
    .filepath-dropdown-content {
        color: #737987;

        .content-hd {
            margin: 16px;
        }
        .content-bd {
            width: 480px;
            height: 360px;
            margin: 16px;
            overflow: auto;
        }
        .content-ft {
            border-top: 1px solid #ded8d8;
            text-align: center;
            padding: 12px 0;
            position: relative;

            .clear-btn {
                position: absolute;
                right: 8px;
            }
        }

        >>> .bk-tree .node-icon {
            margin: 0 4px;
        }
        >>> .bk-tree .tree-drag-node .tree-expanded-icon {
            margin: 0 4px;
        }
    }
    .filepath-name {
        width: 200px;
        text-align: left;
        display: inline-block;
        float: left;
        @mixin ellipsis;

        &+.bk-icon {
            right: 10px;
        }
    }
    .code-fullscreen {
        display: flex;
        .col-main {
            flex: 1;
            max-width: 75%!important;
        }
        .col-aside {
            flex: none;
            width: 240px;
            background: #f0f1f5;
            padding: 12px 20px;
            margin-left: 16px;
        }

        .file-bar {
            height: 36px;

            .filemeta {
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

        .operate-section {
            height: 100%;
        }

        .basic-info {
            height: calc(100% - 110px);
            .title {
                font-size: 14px;
                color: #313238;
            }
            .block {
                padding: 5px 0;
                border-bottom: 1px dashed #c4c6cc;
                &:last-of-type {
                    border-bottom: none;
                    padding-bottom: 50px;
                }
                .item {
                    display: flex;
                    padding: 5px 0;

                    dt {
                        width: 90px;
                        flex: none;
                    }
                    dd {
                        flex: 1;
                        color: #313238;
                        word-break: break-all;
                    }

                    &.ignore {
                        display: block;
                    }
                }
            }
        }
    }
    #codeViewerInDialog {
        width: 100%;
        height: calc(100vh - 200px);
    }
    .author-edit {
        padding: 34px 18px 11px;
        .tips {
            position: absolute;
            top: 66px;
            left: 23px;
            .bk-icon {
                margin-right: 2px;
                color: #ffd695;
            }
            color: #979ba5;
        }
    }
</style>
