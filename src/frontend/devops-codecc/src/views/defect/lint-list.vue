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
                <bk-form :label-width="80" class="search-form">
                    <bk-container col="3" margin="0" gutter="20">
                        <bk-row>
                            <bk-col :span="1">
                                <bk-form-item :label="$t('defect.规则')" label-width="36">
                                    <bk-select v-model="searchParams.checker" searchable>
                                        <bk-option-group
                                            v-for="(group, index) in searchFormData.checkerList"
                                            :name="group.typeName"
                                            :key="index"
                                        >
                                            <bk-option
                                                v-for="(checker, checkIndex) in group.checkers"
                                                :key="checkIndex"
                                                :id="checker"
                                                :name="checker"
                                            >
                                            </bk-option>
                                        </bk-option-group>
                                    </bk-select>
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
                                            <div class="filepath-name" :class="{ 'unselect': !searchFormData.filePathShow }" :title="searchFormData.filePathShow">{{searchFormData.filePathShow ? searchFormData.filePathShow : $t('st.请选择')}}</div>
                                            <i :class="['bk-icon icon-angle-down', { 'icon-flip': isFilePathDropdownShow }]"></i>
                                        </bk-button>
                                        <div class="filepath-dropdown-content" slot="dropdown-content" @click="e => e.stopPropagation()">
                                            <div class="content-hd">
                                                <bk-input v-model="searchInput" class="search-input" :clearable="true" :placeholder="$t('defect.搜索文件夹、告警路径名称')" @input="handleFilePathSearch"></bk-input>
                                            </div>
                                            <div class="content-bd">
                                                <bk-tree
                                                    ref="filePathTree"
                                                    :data="searchFormData.filePathTree.name ? [searchFormData.filePathTree] : []"
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
                            <bk-col :span="1">
                                <bk-form-item :label="$t('defect.级别')" label-width="36">
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
                                <bk-form-item :label="$t('defect.告警类型')">
                                    <bk-checkbox-group v-model="searchParams.fileType" class="checkbox-group">
                                        <bk-checkbox :value="1">{{$t('defect.新告警')}}(<em class="count">{{newDefectCount}}</em>)</bk-checkbox>
                                        <bk-checkbox :value="2">
                                            {{$t('defect.历史告警')}}(<em class="count">{{historyDefectCount}}</em>)
                                        </bk-checkbox>
                                        <span @click="function () {}" v-bk-tooltips.top="{ content: typeTips, width: 220 }" class="defect-type-tips">
                                            <i class="bk-iconcool bk-icon-tishi"></i>
                                        </span>
                                    </bk-checkbox-group>
                                </bk-form-item>
                            </bk-col>
                            <bk-col :span="1">
                                <div class="stat">{{$t('defect.文件x个，告警x条', { file: totalFileCount || 0, defect: totalDefectCount || 0 })}}</div>
                            </bk-col>
                        </bk-row>
                    </bk-container>
                </bk-form>

                <bk-container flex col="3" margin="0" gutter="8">
                    <bk-row>
                        <bk-col :span="1">
                            <bk-table
                                height="280"
                                class="file-list-table"
                                :row-class-name="handleCurrentRow"
                                ref="fileListTable"
                                :empty-text="$t('st.暂无数据')"
                                :data="lintFileList"
                                @row-click="handleFileListRowClick"
                                @sort-change="handleFileListSortChange"
                            >
                                <bk-table-column
                                    :label="$t('defect.文件名称')"
                                    prop="filePath"
                                    sortable="custom"
                                    :formatter="(row, column, cellValue, index) => (cellValue || '').split('/').pop()"
                                >
                                </bk-table-column>
                                <bk-table-column :label="$t('defect.告警数')" prop="defectCount" sortable="custom"></bk-table-column>
                                <div slot="append" v-show="isFileListLoadMore">
                                    <div class="table-append-loading">{{$t('defect.正在加载第x-y个，请稍后···', { x: nextPageStartNum, y: nextPageStartNum + searchParams.pageSize })}}</div>
                                </div>
                            </bk-table>
                        </bk-col>
                        <bk-col :span="2">
                            <bk-table
                                height="280"
                                class="defect-list-table"
                                row-class-name="list-row"
                                highlight-current-row="true"
                                ref="defectListTable"
                                :empty-text="$t('st.未选择文件')"
                                :data="lintDetail.lintDefectList"
                                @row-click="handleDefectListRowClick"
                                @sort-change="handleDefectListSortChange"
                            >
                                <bk-table-column width="110" :label="$t('defect.告警行号')" prop="lineNum" :sortable="lintDefectListNotEmpty ? 'custom' : false"></bk-table-column>
                                <bk-table-column :label="$t('defect.告警规则')" prop="checker"></bk-table-column>
                                <bk-table-column :label="$t('defect.告警处理人')" prop="author"></bk-table-column>
                                <bk-table-column
                                    width="110"
                                    :label="$t('defect.告警级别')"
                                    prop="severity"
                                    :sortable="lintDefectListNotEmpty ? 'custom' : false"
                                >
                                    <template slot-scope="props">
                                        <span :class="`color-${{ 1: 'major', 2: 'minor', 4: 'info' }[props.row.severity]}`">{{defectSeverityMap[props.row.severity]}}</span>
                                    </template>
                                </bk-table-column>
                                <bk-table-column
                                    width="150"
                                    :label="$t('defect.提交日期')"
                                    prop="lineUpdateTime"
                                    :sortable="lintDefectListNotEmpty ? 'custom' : false"
                                    :formatter="(row, column, cellValue, index) => formatTime(cellValue, 'YYYY-MM-DD')"
                                >
                                </bk-table-column>
                            </bk-table>
                        </bk-col>
                    </bk-row>
                </bk-container>

                <div class="code-section" v-show="lintDefectListNotEmpty">
                    <div class="toolbar">
                        <div class="filepath">{{$t('defect.文件路径')}}：<em class="text" v-if="currentLintFile">{{currentLintFile.filePath}}</em></div>
                        <div class="fullscreen"><i class="bk-icon icon-full-screen" :title="$t('defect.全屏')" @click="handleCodeFullScreen"></i></div>
                    </div>
                    <div id="codeViewer" @click="handleCodeViewerClick"></div>
                </div>
                <div class="unselect-file" v-show="!lintDefectListNotEmpty">
                    <empty :title="$t('st.未选择文件')" size="small" />
                </div>

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
                                    <b class="filename">{{(currentLintFile.filePath || '').split('/').pop()}}</b>
                                    <div class="filepath" :title="currentLintFile.filePath">{{$t('defect.文件路径')}}：{{currentLintFile.filePath}}</div>
                                </div>
                            </div>
                            <div id="codeViewerInDialog" @click="handleCodeViewerInDialogClick"></div>
                        </div>
                        <div class="col-aside">
                            <div class="basic-info" v-if="currentLintFile">
                                <dl class="basic-info">
                                    <div class="item">
                                        <dt>{{$t('defect.告警数')}}</dt>
                                        <dd>{{currentLintFile.defectCount}}</dd>
                                    </div>
                                    <div class="item ignore">
                                        <dt style="width: 150px">{{$t('defect.文件创建时间')}}</dt>
                                        <dd v-if="currentLintFile.createdDate">{{formatTime(currentLintFile.createdDate, 'YYYY-MM-DD HH:mm:ss')}}</dd>
                                    </div>
                                    <div class="item ignore">
                                        <dt>{{$t('defect.忽略告警')}}</dt>
                                        <dd v-if="toolId === 'GOML'">
                                            {{$t('defect.在该行末尾添加')}}
                                            <pre class="ignore-code">// nolint</pre>
                                        </dd>
                                        <dd v-else-if="toolId === 'DETEKT'">
                                            {{$t('defect.添加')}}
                                            <pre class="ignore-code">@Suppress("rule_1", "rule_2", ...)</pre>
                                        </dd>
                                        <dd v-else-if="toolId === 'ESLINT'">
                                            1:
                                            <pre class="ignore-code">/* eslint-disable */
/* {{$t('defect.需忽略的代码块')}} */
/* eslint-enable */</pre>
                                            2: {{$t('defect.在该行末尾添加')}}
                                            <pre class="ignore-code">// eslint-disable-line</pre>
                                        </dd>
                                        <dd v-else-if="toolId === 'CHECKSTYLE'">
                                            <pre class="ignore-code">// CHECKSTYLE:OFF
/* {{$t('defect.需忽略的代码块')}} */
// CHECKSTYLE:ON</pre>
                                        </dd>
                                        <dd v-else-if="toolId === 'OCCHECK'">
                                            {{$t('defect.在该行末尾添加')}}
                                            <pre class="ignore-code">// NOLINT</pre>
                                            {{$t('defect.或在该行上方添加')}}
                                            <pre class="ignore-code">// NOLINTNEXTLINE</pre>
                                        </dd>
                                        <dd v-else-if="toolId === 'PYLINT'">
                                            {{$t('defect.在逻辑块内或逻辑行末尾添加')}}
                                            <pre class="ignore-code"># pylint: disable=rule_1,
rule_2,...</pre>
                                        </dd>
                                        <dd v-else-if="toolId === 'CPPLINT'">
                                            {{$t('defect.在该行末尾添加')}}
                                            <pre class="ignore-code">// NOLINT</pre>
                                        </dd>
                                        <dd v-else-if="toolId === 'PHPCS'">
                                            1:
                                            <pre class="ignore-code">// phpcs:disable
/* {{$t('defect.需忽略的代码块')}} */
// phpcs:enable</pre>
                                            2: {{$t('defect.在该行末尾添加')}}
                                            <pre class="ignore-code">// phpcs:ignore</pre>
                                        </dd>
                                        <dd v-else-if="toolId === 'CCN'">
                                            {{$t('defect.在函数头或函数内添加')}}
                                            <pre class="ignore-code">// #lizard forgives</pre>
                                        </dd>
                                    </div>
                                </dl>
                            </div>
                            <div class="operate-section">
                                <bk-table
                                    height="240"
                                    class="defect-list-table"
                                    row-class-name="list-row"
                                    highlight-current-row="true"
                                    :empty-text="$t('st.未选择文件')"
                                    ref="defectListTableInDialog"
                                    :data="lintDetail.lintDefectList"
                                    @row-click="handleDefectListRowInDialogClick"
                                >
                                    <bk-table-column :label="$t('defect.告警行号')" prop="lineNum"></bk-table-column>
                                    <bk-table-column
                                        :label="$t('defect.告警级别')"
                                        prop="severity"
                                    >
                                        <template slot-scope="props">
                                            <span :class="`color-${{ 1: 'major', 2: 'minor', 4: 'info' }[props.row.severity]}`">{{defectSeverityMap[props.row.severity]}}</span>
                                        </template>
                                    </bk-table-column>
                                </bk-table>
                            </div>
                            <div class="toggle-file">
                                <bk-button :disabled="fileIndex - 1 < 0" @click="handlePrevFileClick">{{$t('defect.上一文件')}}</bk-button>
                                <bk-button :disabled="fileIndex + 1 >= totalFileCount" @click="handleNextFileClick">{{$t('defect.下一文件')}}</bk-button>
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
                <div class="tips"><i class="bk-icon icon-info-circle"></i>{{$t('defect.原处理人所有告警都将转给新处理人')}}</div>
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
    import Empty from '@/components/empty'
    import Record from '@/components/operate-record/index'

    export default {
        components: {
            Empty,
            Record
        },
        mixins: [util],
        data () {
            const query = this.$route.query
            const toolId = this.$route.params.toolId

            return {
                toolId,
                defectSeverityMap: {
                    1: this.$t('defect.严重'),
                    2: this.$t('defect.一般'),
                    4: this.$t('defect.提示')
                },
                lintListData: {
                    lintFileList: {
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
                    toolName: toolId,
                    checker: query.checker || '',
                    author: query.author,
                    severity: this.numToArray(query.severity),
                    fileType: this.numToArray(query.fileType, [1, 2]),
                    fileList: [],
                    sortField: '',
                    sortType: '',
                    pageNum: 1,
                    pageSize: 100
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
                    toolName: toolId
                },
                codeViewer: null,
                codeViewerInDialog: null,
                isFilePathDropdownShow: false,
                isFileListLoadMore: false,
                isDefectListLoadMore: false,
                defectDetailDialogVisiable: false,
                authorEditDialogVisiable: false,
                currentDefectDetail: {
                    hintId: undefined,
                    eventTimes: 0,
                    eventSource: undefined
                },
                pageChange: false,
                fileIndex: -1,
                codeMirrorDefaultCfg: {
                    lineNumbers: true,
                    scrollbarStyle: 'simple',
                    theme: 'darcula',
                    firstLineNumber: 1,
                    placeholder: this.emptyText,
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
            ...mapState([
                'toolMeta'
            ]),
            ...mapState('tool', {
                toolMap: 'mapList'
            }),
            typeTips () {
                return this.$t('defect.接入时间之后产生的告警为新告警', { accessTime: this.formatTime(this.lintListData.firstAnalysisSuccessTime, 'YYYY-MM-DD') })
            },
            lintFileList () {
                return this.lintListData.lintFileList.content
            },
            currentLintFile () {
                return this.lintFileList[this.fileIndex]
            },
            totalFileCount () {
                const totalElements = this.lintListData.lintFileList.totalElements
                return totalElements > 100000 ? this.$t('st.10万+') : totalElements
            },
            totalDefectCount () {
                const totalCheckerCount = this.lintListData.totalCheckerCount
                return totalCheckerCount > 100000 ? this.$t('st.10万+') : totalCheckerCount
            },
            newDefectCount () {
                const newDefectCount = this.lintListData.newDefectCount
                return newDefectCount > 100000 ? this.$t('st.10万+') : newDefectCount
            },
            historyDefectCount () {
                const historyDefectCount = this.lintListData.historyDefectCount
                return historyDefectCount > 100000 ? this.$t('st.10万+') : historyDefectCount
            },
            nextPageStartNum () {
                return this.searchParams.pageNum * this.searchParams.pageSize + 1
            },
            lintDefectListNotEmpty () {
                return (this.lintDetail.lintDefectList || []).length
            },
            breadcrumb () {
                const toolId = this.$route.params.toolId
                const toolDisplayName = (this.toolMap[toolId] || {}).displayName || ''
                const names = [this.$route.meta.title || this.$t('nav.告警管理')]
                if (toolDisplayName) {
                    names.unshift(toolDisplayName)
                }

                return { name: names.join(' / ') }
            }
        },
        watch: {
            // 监听查询参数变化，则获取列表
            searchParams: {
                handler () {
                    this.fetchLintList().then(list => {
                        if (this.pageChange) {
                            // 将一页的数据追加到列表
                            this.lintListData.lintFileList.content = this.lintListData.lintFileList.content.concat(list.lintFileList.content)

                            // 隐藏加载条
                            this.isFileListLoadMore = false

                            // 重置页码变更标记
                            this.pageChange = false
                        } else {
                            this.lintListData = { ...this.lintListData, ...list }

                            // 重置文件下的告警详情
                            this.lintDetail = {}
                        }
                    })
                },
                deep: true
            },
            defectDetailSearchParams: {
                handler () {
                    this.fetchLintDetail().then(detail => {
                        this.lintDetail = { ...this.lintDetail, ...detail }

                        // 更新代码展示组件内容等
                        this.updateCodeViewer(this.codeViewer)

                        if (this.codeViewerInDialog) {
                            this.updateCodeViewer(this.codeViewerInDialog)
                        }
                        // 默认选中文件的第一个告警
                        this.$nextTick(() => this.handleDefectListRowClick(this.lintDetail.lintDefectList[0]))
                    }).finally(() => {
                        bus.$emit('hide-app-loading')
                    })
                },
                deep: true
            },
            currentDefectDetail: {
                handler () {
                    this.emptyText = this.$t('se.未选择文件')
                    this.locateHint()
                },
                deep: true
            },
            defectDetailDialogVisiable (val, oldVal) {
                if (val === true) {
                    // dialog中的codemirror的自动刷新默认需要250毫秒的延时，所以这里需要等待就绪
                    setTimeout(() => {
                        this.$refs.defectListTableInDialog.doLayout()
                        this.locateHintByName('dialog', true)
                    }, 0)
                } else {
                    this.locateHintByName('main', true)
                    this.codeViewerInDialog.setValue('')
                    this.codeViewerInDialog.setOption('firstLineNumber', 1)
                }
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
            }
        },
        mounted () {
            // 初始化CodeMirror
            this.codeViewer = CodeMirror(document.getElementById('codeViewer'), this.codeMirrorDefaultCfg)

            // 表格boby容器
            const fileListTableBodyWrapper = this.$refs.fileListTable.$refs.bodyWrapper

            // 告警文件列表滚动加载
            fileListTableBodyWrapper.addEventListener('scroll', (event) => {
                const dom = event.target
                // 总页数
                const totalPages = this.lintListData.lintFileList.totalPages
                // 当前页码
                const currentPageNum = this.searchParams.pageNum
                // 是否滚动到底部
                const hasScrolledToBottom = dom.scrollTop + dom.offsetHeight >= dom.scrollHeight

                // 触发翻页加载
                if (hasScrolledToBottom && currentPageNum + 1 <= totalPages && this.isFileListLoadMore === false) {
                    // 显示加载条
                    this.isFileListLoadMore = true
                    // 变更页码触发查询
                    this.searchParams.pageNum += 1
                    // 标记为页面变更查询
                    this.pageChange = true
                }
            })
        },
        methods: {
            async fetchPageData () {
                const { $route: { params } } = this
                await Promise.all([
                    this.fetchLintList(),
                    this.$store.dispatch('defect/lintParams', params)
                ]).then(([list, params]) => {
                    this.lintListData = { ...this.lintListData, ...list }

                    // 给文件路径树加上icon
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
                return this.$store.dispatch('defect/lintList', this.searchParams, { showLoading: this.pageChange ? false : true })
            },
            fetchLintDetail () {
                const pattern = this.toolMap[this.$route.params.toolId]['pattern']
                const params = { ...this.searchParams, ...this.defectDetailSearchParams, pattern }
                // console.log(this.defectDetailSearchParams, 'params')
                bus.$emit('show-app-loading')
                return this.$store.dispatch('defect/lintDetail', params)
            },
            handleCodeFullScreen () {
                this.defectDetailDialogVisiable = true
                setTimeout(() => {
                    const width = 700 - document.getElementsByClassName('filename')[0].offsetWidth
                    document.getElementsByClassName('filepath')[1].style.width = width + "px"
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
            },
            handleFileListRowClick (row, event, column) {
                this.fileIndex = this.lintFileList.findIndex(file => file.entityId === row.entityId)
                // 筛选后，告警详情为空，此时要把参数强制置空，不然点击文件不能触发请求
                if (!this.lintDetail.lintDefectList) {
                    this.defectDetailSearchParams.entityId = ''
                }
                this.defectDetailSearchParams.entityId = row.entityId
                this.defectDetailSearchParams.filePath = row.filePath
            },
            handleDefectListRowClick (row, event, column) {
                this.defectListRowClick({ row, event, column }, 'main-row')
            },
            handleDefectListRowInDialogClick (row, event, column) {
                this.defectListRowClick({ row, event, column }, 'dialog-row')
            },
            handleFileListSortChange ({ column, prop, order }) {
                // 更新查询参数自动触发新的列表查询
                const orders = { ascending: 'ASC', descending: 'DESC' }
                this.searchParams = { ...this.searchParams, ...{ sortField: prop, sortType: orders[order] } }
            },
            handleDefectListSortChange ({ column, prop, order }) {
                const orders = { ascending: 'ASC', descending: 'DESC' }
                this.defectDetailSearchParams = { ...this.defectDetailSearchParams, ...{ sortField: prop, sortType: orders[order] } }
            },
            handlePrevFileClick () {
                this.defectDetailSearchParams.entityId = this.lintFileList[--this.fileIndex].entityId
            },
            handleNextFileClick () {
                this.defectDetailSearchParams.entityId = this.lintFileList[++this.fileIndex].entityId
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

            getDefectCountBySeverity (severity) {
                const severityFieldMap = {
                    1: 'seriousCheckerCount',
                    2: 'normalCheckerCount',
                    4: 'promptCheckerCount'
                }
                const count = this.lintListData[severityFieldMap[severity]]
                return count > 100000 ? this.$t('st.10万+') : count
            },

            handleCodeViewerClick (event, eventSource) {
                this.codeViewerClick(event, 'main-code')
            },
            handleCodeViewerInDialogClick (event, eventSource) {
                this.codeViewerClick(event, 'dialog-code')
            },
            locateHint () {
                const eventFrom = this.currentDefectDetail.eventSource.split('-').shift()
                // 默认处理页面中的代码展示
                this.locateHintByName(eventFrom)
            },
            locateHintByName (name, visiableToggle) {
                const { hintId, eventSource } = this.currentDefectDetail

                // 确实存在未点击告警直接打开全屏的情况，这种情况没有hintId
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
                addClass(currentRow, 'current-row')

                if (eventTrigger === 'code') {
                    // 滚动到当前表格行
                    defectListTableBodyWrapper.scrollTo({
                        top: currentRow.offsetTop,
                        behavior: 'smooth'
                    })
                }

                if (eventTrigger === 'row' || visiableToggle === true) {
                    // 滚动到告警代码位置
                    const { trimBeginLine } = this.lintDetail
                    const middleHeight = codeViewer.getScrollerElement().offsetHeight / 2
                    const lineHeight = codeViewer.defaultTextHeight()
                    codeViewer.scrollIntoView({ line: lineNum - trimBeginLine, ch: 0 }, middleHeight - lineHeight)
                }

                // 告警代码区域高亮
                const lintHints = lintWrapper.getElementsByClassName(`lint-hints-${hintId}`)
                this.activeLintHint(lintHints[0])
            },

            defectListRowClick ({ row, event }, eventSource) {
                if (this.lintDetail.fileContent === '') return

                // 代码所在行
                const lineNum = row.lineNum - 1

                // 得到表格行索引
                const rowIndex = event ? getClosest(event.target, 'tr').rowIndex : 0

                // 记录当前告警id
                const hintId = `${lineNum}-${rowIndex}`

                // 触发watch
                this.currentDefectDetail.hintId = hintId
                this.currentDefectDetail.eventSource = eventSource
                this.currentDefectDetail.eventTimes++
            },

            codeViewerClick (event, eventSource) {
                const lintHints = getClosest(event.target, '.lint-hints')

                // 如果点击的是lint告警区域
                if (lintHints) {
                    // 触发watch
                    this.currentDefectDetail.hintId = lintHints.dataset.hintId
                    this.currentDefectDetail.eventSource = eventSource
                    this.currentDefectDetail.eventTimes++
                }
            },

            // 代码展示相关
            updateCodeViewer (codeViewer) {
                const { fileName, fileContent, trimBeginLine } = this.lintDetail
                const { mode } = CodeMirror.findModeByFileName(fileName)
                import(`codemirror/mode/${mode}/${mode}.js`).then(m => {
                    codeViewer.setOption('mode', mode)
                })
                if (this.lintDetail.fileContent === '') {
                    this.emptyText = this.$t('st.文件内容为空')
                    codeViewer.setValue(this.emptyText)
                    return
                }
                codeViewer.setOption('firstLineNumber', trimBeginLine !== 0 ? trimBeginLine : 1)
                codeViewer.setValue(fileContent)

                // 创建告警提示块
                this.buildLintHints(codeViewer)
            },
            buildLintHints (codeViewer) {
                const { defectSeverityMap } = this
                const { lintDefectList } = this.lintDetail
                const defectListCount = lintDefectList.length
                const { trimBeginLine } = this.lintDetail

                lintDefectList.forEach((item, i) => {
                    // 与 codemirror 行号对齐
                    const lineNum = item.lineNum - 1

                    // 下一告警与当前告警处在同一行，用于输出时合并作者
                    const isSameLine = i < defectListCount - 1 && lineNum === lintDefectList[i + 1].lineNum - 1

                    const hints = document.createElement('div')
                    const hintId = `${lineNum}-${i}`
                    hints.innerHTML = `
                        <i class="lint-icon bk-icon icon-right-shape"></i>
                        <div class="lint-info">
                            <p>${this.$t('defect.告警规则')}：${item.checker} | ${item.checkerType} | ${defectSeverityMap[item.severity]}</p>
                            <p class="checker-detail">${item.checkerDetail}</p>
                            <p>${this.$t('defect.告警内容')}：${item.message}</p>
                            ${isSameLine ? '' : `<p>${this.$t('defect.告警处理人')}：${item.author}</p>`}
                        </div>
                    `
                    hints.className = `lint-hints lint-hints-${hintId}`
                    hints.dataset.hintId = hintId

                    codeViewer.addLineWidget(lineNum - trimBeginLine, hints, {
                        coverGutter: false,
                        noHScroll: false,
                        above: true
                    })
                    codeViewer.addLineClass(lineNum - trimBeginLine, 'wrap', 'lint-hints-wrap')
                })
            },
            activeLintHint (lintHint) {
                toggleClass(lintHint, 'active')

                // 切换所有lint wrap的active
                const lintHintsWrap = getClosest(lintHint, '.lint-hints-wrap')
                document.querySelectorAll('.lint-hints-wrap').forEach(el => el.classList.remove('active'))
                addClass(lintHintsWrap, 'active')
                // addClass(lintHintsWrap.previousSibling, 'prev')
                this.codeViewer.refresh()
                if (this.codeViewerInDialog) {
                    this.codeViewerInDialog.refresh()
                }
            },
            openSlider () {
                this.show = true
            },
            numToArray (num, arr = [1, 2, 4]) {
                let filterArr = arr.filter(x => x & num)
                filterArr = filterArr.length ? filterArr : arr
                return filterArr
            },
            handleCurrentRow ({ row, rowIndex }) {
                return rowIndex === this.fileIndex ? 'current-row list-row' : 'list-row'
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
    @import '../../css/variable.css';
    @import '../../assets/bk_icon_font/style.css';
    $codeSectionHeight: calc(100% - 400px);
    $toolbarHeight: 42px;

    .main-container {
        padding: 20px 33px 0!important;
        margin: 0 -13px!important;
        .main-content-inner {
            height: 100%;
        }
    }
    .code-section {
        height: $codeSectionHeight;
        border: 1px solid #dcdee5;
        margin-top: 10px;
        background: #fff;
        padding: 0 15px 15px;

        .toolbar {
            display: flex;
            height: $toolbarHeight;
            padding-top: 2px;
            justify-content: space-between;
            align-items: center;

            .filepath {
                font-size: 12px;
                border-left: 4px solid $itemBorderColor;
                padding-left: 8px;

                .text {
                    color: #63656e;
                    font-style: normal;
                }
            }
            .fullscreen {
                .icon-full-screen {
                    cursor: pointer;

                    &:hover {
                        color: #63656e;
                    }
                }
            }
        }
    }

    #codeViewer {
        width: 100%;
        height: calc(100% - $toolbarHeight);
    }

    .file-list-table,
    .defect-list-table {
        >>> .list-row {
            cursor: pointer;
        }
    }

    .table-append-loading {
        text-align: center;
        padding: 12px 0;
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
    }

    .unselect-file {
        margin-top: 10px;
        border: 1px solid #dcdee5;
        height: $codeSectionHeight;
        display: flex;
        align-items: center;
        justify-content: center;

        >>> .empty {
            .title {
                color: #737987
            }
        }
    }

    .defect-type-tips {
        margin-left: -12px;
        position: relative;
        top: 1px;
        .bk-iconcool {
            font-size: 16px;
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

            .operate-section {
                padding-top: 20px;
                height: calc(100vh - 370px);
            }
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

                &.ignore {
                    display: block;
                    .ignore-code {
                        color: #5e9a34;
                        margin: 0;
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
    >>>.icon-empty {
        background: url(../../images/empty.png) no-repeat 50% 0;
        background-size: contain;
        margin: 0 auto;
        width: 64px;
        height: 40px;
        display: inline-block;
        &:before {
            content: '';
        }
    }
    >>>.bk-form-content {
        width: 89%;
    }
</style>
