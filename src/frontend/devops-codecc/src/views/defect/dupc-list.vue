<template>
    <div>
        <div class="breadcrumb">
            <div class="breadcrumb-name">{{breadcrumb.name}}</div>
            <div class="breadcrumb-extra">
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
                    @row-click="handleListRowClick"
                    @sort-change="handleSortChange"
                >
                    <bk-table-column type="index" :label="$t('defect.序号')" align="center" width="70"></bk-table-column>
                    <bk-table-column :label="$t('defect.文件名')" prop="fileName"></bk-table-column>
                    <bk-table-column :label="$t('defect.重复块数')" prop="blockNum"></bk-table-column>
                    <bk-table-column :label="$t('defect.重复行数')" prop="dupLines" sortable="custom" label-class-name="col-sort-label" class-name="col-sort"></bk-table-column>
                    <bk-table-column :label="$t('defect.函数总行数')" prop="totalLines"></bk-table-column>
                    <bk-table-column :label="$t('defect.重复率')" prop="dupRate"></bk-table-column>
                    <bk-table-column :label="$t('defect.相关作者')" prop="authorList"></bk-table-column>
                    <bk-table-column :label="$t('defect.风险')" prop="riskFactor" :render-header="renderHeader">
                        <template slot-scope="props">
                            <span :class="`color-${{ 1: 'major', 2: 'minor', 4: 'info' }[props.row.riskFactor]}`">{{defectSeverityMap[props.row.riskFactor]}}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column :label="$t('defect.修改时间')" prop="fileChangeTime" sortable="custom">
                        <template slot-scope="props">
                            <span>{{formatTime(props.row.fileChangeTime, 'YYYY-MM-DD')}}</span>
                        </template>
                    </bk-table-column>
                </bk-table>
            </div>
        </div>
        <Record :visiable.sync="show" :data="this.$route.name" />
    </div>
</template>

<script>
    import util from '@/mixins/util'
    import { mapState } from 'vuex'
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
                searchParams: {
                    taskId: this.$route.params.taskId,
                    toolName: 'DUPC',
                    checker: query.checker || '',
                    author: query.author,
                    severity: this.numToArray(query.severity),
                    fileList: [],
                    sortField: 'dupLines',
                    sortType: 'DESC',
                    pageNum: 1,
                    pageSize: 50
                },
                searchFormData: {
                    checkerList: [],
                    authorList: [],
                    filePathTree: {},
                    filePathShow: ''
                },
                pagination: {
                    current: 1,
                    count: 1,
                    limit: 50
                },
                lintListData: {
                    defectList: {
                        content: [],
                        totalElements: 0
                    },
                    superHighCount: 0,
                    highCount: 0,
                    mediumCount: 0
                },
                isFilePathDropdownShow: false,
                toolId: 'DUPC',
                editor: null,
                show: false,
                searchInput: ''
            }
        },
        computed: {
            ...mapState([
                'toolMeta'
            ]),
            ...mapState('tool', {
                toolMap: 'mapList'
            }),
            ...mapState('defect', [
                'detail'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            taskId () {
                return this.$route.params.taskId
            },
            toolName () {
                return this.$route.params.tool
            },
            breadcrumb () {
                const toolId = this.toolId
                let toolDisplayName = (this.toolMap[toolId] || {}).displayName || ''
                const names = [this.$route.meta.title || this.$t('nav.重复文件')]
                if (toolDisplayName) {
                    toolDisplayName = this.$t(`toolName.${toolDisplayName}`)
                    names.unshift(toolDisplayName)
                }

                return { name: names.join(' / ') }
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
                        this.pagination.count = this.lintListData.defectList.totalElements
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
                    this.lintListData = list
                    this.formatFilePath(params.filePathTree)
                    this.searchFormData = Object.assign({}, this.searchFormData, params)
                    this.pagination.count = this.lintListData.defectList.totalElements
                })
            },
            // 给文件路径树加上icon
            formatFilePath (filepath) {
                if (filepath.children && filepath.children.length) {
                    filepath.openedIcon = 'icon-folder-open'
                    filepath.closedIcon = 'icon-folder'
                    filepath.children.forEach(this.formatFilePath)
                } else {
                    filepath.icon = 'icon-file'
                }
            },
            // 获取告警列表
            fetchLintList () {
                return this.$store.dispatch('defect/lintList', this.searchParams, { showLoading: true })
            },
            // 获取告警筛选参数
            fetchLintParams () {
                const params = this.$route.params
                params.toolId = 'DUPC'
                return this.$store.dispatch('defect/lintParams', params)
            },
            handleListRowClick (row, event, column) {
                const { entityId, filePath } = row
                const query = { entityId, filePath }
                const resolved = this.$router.resolve({
                    name: 'defect-dupc-detail',
                    params: this.$route.params,
                    query
                })
                const href = `${window.DEVOPS_SITE_URL}/console${resolved.href}`
                window.open(href, '_blank')
            },
            renderHeader (h, data) {
                const extreHigh = this.$t('charts.极高风险(>=20%)')
                const high = this.$t('charts.高风险11%20%')
                const directive = {
                    name: 'bkTooltips',
                    content: `<p>${extreHigh}</p><p>${high}</p>`,
                    placement: 'right'
                }
                return <span class="custom-header-cell" v-bk-tooltips={ directive }>{ data.column.label }</span>
            },
            // 告警列表排序
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
            getDefectCountBySeverity (severity) {
                const severityFieldMap = {
                    1: 'superHighCount',
                    2: 'highCount',
                    4: 'mediumCount'
                }
                const count = this.lintListData[severityFieldMap[severity]]
                return count > 100000 ? this.$t('st.10万+') : count
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
    }
</style>
