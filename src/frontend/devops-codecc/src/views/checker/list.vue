<template>
    <div class="cc-checkers">
        <div class="cc-side">
            <div class="cc-filter">
                <span class="cc-filter-txt">{{$t('过滤器')}}</span>
                <span class="cc-filter-clear fr" @click="handleClear">
                    <i class="codecc-icon icon-filter-2 cc-link"></i>
                </span>
            </div>
            <bk-input
                class="checker-search"
                :placeholder="$t('按Enter搜索规则名或描述')"
                :clearable="true"
                :right-icon="'bk-icon icon-search'"
                v-model="keyWord"
                @enter="handleKeyWordSearch"
                @clear="handleKeyWordSearch">
            </bk-input>
            <div class="cc-filter-content">
                <cc-collapse
                    ref="searchParams"
                    :search="search"
                    :active-name="activeName"
                    @updateActiveName="updateActiveName"
                    @handleSelect="handleSelect">
                </cc-collapse>
            </div>
        </div>
        <div class="cc-main" v-bkloading="{ isLoading: pageLoading, opacity: 0.6 }">
            <div class="table-config" v-if="isConfig">
                <bk-button :theme="'primary'"
                    class="save-conf"
                    @click="saveConf"
                    :class="hasNoPermission || !selectedConf.length ? 'disable-save' : ''"
                    v-bk-tooltips="{ content: (hasNoPermission || !selectedConf.length) ? hasNoPermission ? '暂无规则集管理权限，可在新建/复制规则集后进行修改。' : '请至少选中1条规则' : null }"
                >{{$t('保存')}}</bk-button>
                <span class="config-txt">
                    共找到<span class="num">{{totalCount}}</span>条，已选中<span class="num">{{selectedConf.length}}</span>条
                </span>
                <div class="cc-keyboard">
                    <span>{{$t('当前已支持键盘操作')}}</span>
                    <bk-button text ext-cls="cc-button" @click="operateDialogVisiable = true">{{$t('如何操作？')}}</bk-button>
                </div>
            </div>
            <span class="table-txt" v-else>
                <span class="table-txt-num">第 <span>{{tableIndex + 1}}</span> / {{totalCount}} 规则</span>
                <span class="table-txt-keyboard fr">
                    <span>{{$t('当前已支持键盘操作')}}</span>
                    <bk-button text ext-cls="cc-button" size="small" @click="operateDialogVisiable = true">{{$t('如何操作？')}}</bk-button>
                </span>
            </span>
            <bk-table
                ref="ruleListTable"
                v-show="isFetched"
                :data="list"
                :row-class-name="handleRowClass"
                @sort-change="handleSortChange"
                @select="selectRules"
                @select-all="selectAllRules">
                <bk-table-column v-if="isConfig" type="selection" width="60" align="center" :selectable="() => !hasNoPermission"></bk-table-column>
                <bk-table-column label="规则名" prop="checkerKey" sortable="custom" min-width="120">
                    <template slot-scope="props">
                        <span class="cc-link" @click="handleRowClick(props.row, props.$index)">{{props.row.checkerKey}}</span>
                    </template>
                </bk-table-column>
                <bk-table-column label="规则描述" prop="checkerDesc" min-width="280">
                    <span slot-scope="props" class="cc-link" @click="handleRowClick(props.row, props.$index)">
                        <span :class="{ 'checker-edit': !hasNoPermission && props.row.editable }">{{props.row.checkerDesc}}</span>
                        <span v-if="!hasNoPermission && props.row.editable" @click.stop="handleCheckerEdit(props.row, false)" class="codecc-icon icon-edit cc-link"></span>
                    </span>
                </bk-table-column>
                <bk-table-column label="适用语言" prop="checkerLanguage" sortable="custom">
                    <template slot-scope="props">
                        <span>{{props.row.checkerLanguage && props.row.checkerLanguage.join()}}</span>
                    </template>
                </bk-table-column>
                <bk-table-column label="类别" prop="checkerCategory" sortable="custom">
                    <template slot-scope="props">
                        <span>{{props.row.checkerCategoryName}}</span>
                    </template>
                </bk-table-column>
                <bk-table-column label="工具" prop="toolName" sortable="custom">
                    <template slot-scope="props">
                        <!-- <a class="cc-link table-tool" :href="iwikiCodeccTool" target="_blank">{{formatTool(props.row.toolName)}}</a> -->
                        {{formatTool(props.row.toolName)}}
                    </template>
                </bk-table-column>
                <bk-table-column label="标签" prop="checkerTag" sortable="custom" width="170">
                    <template slot-scope="props">
                        <span class="checker-tag" v-for="item in props.row.checkerTag" :key="item">{{item}}</span>
                    </template>
                </bk-table-column>
                <div slot="empty">
                    <div class="codecc-table-empty-text">
                        <img src="../../images/empty.png" class="empty-img">
                        <div>{{$t('暂无数据')}}</div>
                    </div>
                </div>
            </bk-table>
        </div>
        <bk-sideslider :is-show.sync="sliderIsShow" :quick-close="true" width="720">
            <div slot="header" class="cc-ellipsis" :title="checker.checkerKey">{{checker.checkerKey}}</div>
            <div slot="content" class="checker-content" v-bkloading="{ isLoading: contentLoading, opacity: 0.6 }">
                <div class="checker-form">
                    <span class="checker-top">
                        <span class="checker-col">
                            <span class="checker-col-label">发布者：</span>
                            <span class="checker-col-content">{{checker.createdBy}}</span>
                        </span>
                        <span class="checker-col">
                            <span class="checker-col-label">更新时间：</span>
                            <span class="checker-col-content">{{formatTime(checker.createdDate)}}</span>
                        </span>
                    </span>
                    <span class="checker-col">
                        <span class="checker-col-label">适用语言：</span>
                        <span class="checker-col-content" v-bk-tooltips="{ content: checker.checkerLanguage && checker.checkerLanguage.join(), delay: 350 }">
                            {{checker.checkerLanguage && checker.checkerLanguage.join()}}
                        </span>
                    </span>
                    <span class="checker-col">
                        <span class="checker-col-label">类别：</span>
                        <span class="checker-col-content">{{checker.checkerCategoryName}}</span>
                    </span>
                    <span class="checker-col">
                        <span class="checker-col-label">严重级别：</span>
                        <span class="checker-col-content" :class="`severity-${checker.severity}`">{{getSeverityByNum(checker.severity)}}</span>
                    </span>
                    <span class="checker-col">
                        <span class="checker-col-label">工具：</span>
                        <span class="checker-col-content">{{formatTool(checker.toolName)}}</span>
                    </span>
                    <span class="checker-col">
                        <span class="checker-col-label label-fixed">标签：</span>
                        <span class="checker-col-content">
                            <span class="checker-tag" v-for="item in checker.checkerTag" :key="item">{{item}}</span>
                        </span>
                    </span>
                    <span class="checker-col" v-if="checker.editable && checker.props">
                        <span class="checker-col-label">参数：</span>
                        <span class="checker-col-content">n=
                            <span>{{checker.propValue}}</span><i class="codecc-icon icon-edit" @click.stop="handleCheckerEdit(checker, true)"></i>
                        </span>
                    </span>
                    <span class="checker-col checker-col-block">
                        <span class="checker-col-label">描述：</span>
                        <span class="checker-col-content">
                            <span>{{checker.checkerDesc}}</span>
                        </span>
                    </span>
                    <span class="checker-col checker-col-block">
                        <span class="checker-col-label">详细说明：</span>
                        <span class="checker-col-content" v-show="!hasDetail">
                            <div v-for="(desc, index) in (checker.checkerDescModel && checker.checkerDescModel.split('\n')) 
                            || (checker.checkerDesc && checker.checkerDesc.split('\n'))" :key="index">{{desc}}</div>
                        </span>
                    </span>
                    <span v-show="!hasDetail">
                        <span class="checker-col checker-col-block">
                            <span class="checker-col-label">错误示例：</span>
                            <div id="errExample" :class="{ 'example-empty': errExampleEmpty }"></div>
                        </span>
                        <span class="checker-col checker-col-block">
                            <span class="checker-col-label">正确示例：</span>
                            <div id="rightExample" :class="{ 'example-empty': rightExampleEmpty }"></div>
                        </span>
                    </span>
                </div>
                <div v-show="hasDetail" class="checker-detail" id="checkerDetail">
                    {{checkerTypeDesc}}
                </div>
            </div>
        </bk-sideslider>

        <bk-dialog
            v-model="updateVisiable"
            theme="primary"
            width="533px"
            class="update-parameter"
            :mask-close="false">
            <div class="update-parameter-title">
                <span>{{$t('修改规则x参数', { name: ruleData.checkerName })}}</span>
            </div>
            <div class="update-parameter-body">
                <bk-form :label-width="120" ref="ruleParameter">
                    <bk-form-item :label="$t('语言')">{{ruleData.checkerLanguage && ruleData.checkerLanguage.join('; ')}}</bk-form-item>
                    <bk-form-item :label="$t('工具')">{{ruleData.toolName}}</bk-form-item>
                    <bk-form-item :label="$t('类型')">{{ruleData.checkerType}}</bk-form-item>
                    <bk-form-item :label="$t('描述')">{{ruleData.checkerDesc}}</bk-form-item>
                    <bk-form-item :label="$t('参数修改')" class="params-input" property="propValue">
                        <bk-input style="width: 69px" :disabled="!isConfig" v-model="ruleData.propValue"></bk-input>
                    </bk-form-item>
                </bk-form>
            </div>
            <div slot="footer">
                <bk-button theme="primary" :disabled="!isConfig" @click="updateParameter">{{$t('确定')}}</bk-button>
                <bk-button @click="updateVisiable = false">{{$t('取消')}}</bk-button>
            </div>
        </bk-dialog>
        <bk-dialog
            v-model="operateDialogVisiable"
            width="605"
            theme="primary"
            :show-footer="false"
            :position="{ top: 50, left: 5 }"
            :title="$t('现已支持键盘操作，提升操作效率')">
            <div>
                <img src="../../images/operate-checker.svg">
            </div>
            <div class="operate-footer" slot="footer">
                <bk-button
                    theme="primary"
                    @click.native="operateDialog.visiable = false"
                >
                    {{$t('关闭')}}
                </bk-button>
            </div>
        </bk-dialog>
    </div>
</template>

<script>
    import { mapState } from 'vuex'
    import { format } from 'date-fns'
    import ccCollapse from '@/components/cc-collapse'
    import CodeMirror from '@/common/codemirror'

    export default {
        components: {
            ccCollapse
        },
        props: {
            isConfig: {
                type: Boolean,
                default: false
            },
            hasNoPermission: {
                type: Boolean,
                default: false
            },
            selectedConf: {
                type: Array,
                default: []
            },
            localRuleParam: {
                type: Array,
                default: []
            },
            checkersetConf: {
                type: Object,
                default: {}
            },
            handleSelectRules: Function,
            saveConf: Function,
            updateConfParame: Function
        },
        data () {
            return {
                activeName: [],
                search: [],
                list: [],
                selectParams: {
                    pageNum: 1,
                    pageSize: 100
                },
                keyWord: undefined,
                checker: {},
                sliderIsShow: false,
                checkerTypeDesc: '',
                totalCount: 0,
                updateVisiable: false,
                ruleData: {},
                tableIndex: 0,
                codeMirrorDefaultCfg: {
                    lineNumbers: true,
                    scrollbarStyle: 'simple',
                    // theme: 'summerfruit',
                    lineWrapping: true,
                    placeholder: this.emptyText,
                    firstLineNumber: 1,
                    readOnly: true,
                    autoRefresh: true
                },
                errCodeViewer: null,
                rightCodeViewer: null,
                operateDialogVisiable: false,
                hasDetail: false,
                paramsRule: [
                    {
                        required: true,
                        message: '必填项',
                        trigger: 'blur'
                    },
                    {
                        regex: /^([1-9][0-9]*)$/,
                        message: '请输入大于0的整数',
                        trigger: 'blur'
                    }
                ],
                errExampleEmpty: false,
                rightExampleEmpty: false,
                pageLoading: false,
                contentLoading: false,
                isFetched: false,
                iwikiCodeccTool: window.IWIKI_CODECC_TOOL
            }
        },
        computed: {
            ...mapState('tool', {
                toolMap: 'mapList'
            }),
            parameter () {
                let props = this.ruleData.props || '{}'
                props = JSON.parse(props)
                return props[0] && props[0]['propValue']
            }
        },
        watch: {
            selectParams: {
                handler (value) {
                    this.fetchSearch(false, value)
                    this.fetchList(value)
                },
                deep: true
            },
            sliderIsShow (value) {
                if (value) {
                    this.$nextTick(() => {
                        const codeMirrorConfig = {
                            ...this.codeMirrorDefaultCfg,
                            autoRefresh: true
                        }
                        this.errCodeViewer = CodeMirror(document.getElementById('errExample'), codeMirrorConfig)
                        this.rightCodeViewer = CodeMirror(document.getElementById('rightExample'), codeMirrorConfig)
                    })
                }
            }
        },
        created () {
            this.fetchSearch(true)
            this.fetchList()
            const vm = this
            document.onkeydown = keyDown
            function keyDown (event) {
                const e = event || window.event
                if (e.target.nodeName !== 'BODY') return
                switch (e.code) {
                    case 'Enter':
                        if (e.target.getAttribute('class') !== 'bk-form-input') vm.handleRowClick()
                        break
                    case 'Escape':
                        vm.sliderIsShow = false
                        break
                    case 'ArrowLeft':
                        if (vm.tableIndex > 0) {
                            vm.tableIndex--
                            vm.screenScroll()
                        }
                        if (vm.sliderIsShow) vm.handleRowClick()
                        break
                    case 'ArrowUp':
                        if (vm.tableIndex > 0) {
                            vm.tableIndex--
                            vm.screenScroll()
                        }
                        if (vm.sliderIsShow) vm.handleRowClick()
                        break
                    case 'ArrowRight':
                        if (vm.tableIndex < vm.list.length - 1) {
                            vm.tableIndex++
                            vm.screenScroll()
                        }
                        if (vm.sliderIsShow) vm.handleRowClick()
                        break
                    case 'ArrowDown':
                        if (vm.tableIndex < vm.list.length - 1) {
                            vm.tableIndex++
                            vm.screenScroll()
                        }
                        if (vm.sliderIsShow) vm.handleRowClick()
                        break
                }
            }
        },
        beforeDestroy () {
            document.onkeydown = null
        },
        methods: {
            async fetchSearch (isInit, params = {}) {
                const payload = this.isConfig ? Object.assign({ checkerLanguage: this.checkersetConf.codeLang }, params) : params
                if (this.isConfig) {
                    payload.checkerSetId = this.checkersetConf.checkerSetId
                    payload.version = this.checkersetConf.version
                    if (this.checkersetConf.legacy && this.checkersetConf.codeLangList.length === 1) {
                        payload.toolName = this.checkersetConf.toolList
                    }
                }
                const res = await this.$store.dispatch('checker/count', payload)
                if (res) {
                    if (this.isConfig) {
                        const filterArr = this.checkersetConf.legacy && this.checkersetConf.codeLangList.length === 1 ? ['checkerLanguage', 'toolName'] : ['checkerLanguage']
                        this.search = res.filter(item => !filterArr.includes(item.name))
                    } else {
                        this.search = res
                    }
                    if (isInit) {
                        this.$nextTick(() => {
                            this.activeName = this.isConfig ? ['checkerCategory', 'toolName'] : ['checkerLanguage', 'checkerCategory']
                            this.$refs.ruleListTable.$refs.bodyWrapper.addEventListener('scroll', event => {
                                const dom = event.target
                                // 是否滚动到底部
                                const hasScrolledToBottom = dom.scrollTop + dom.offsetHeight >= dom.scrollHeight
                                if (hasScrolledToBottom && this.list.length < this.totalCount) {
                                    this.selectParams.pageNum += 1
                                }
                            })
                        })
                    }
                    this.totalCount = res.find(item => item.name === 'total')['checkerCountList'][0]['count']
                }
            },
            async fetchList (selectParams = this.selectParams) {
                this.pageLoading = true
                const params = this.isConfig ? Object.assign({ checkerLanguage: this.checkersetConf.codeLang }, selectParams) : selectParams
                if (this.isConfig) {
                    params.checkerSetId = this.checkersetConf.checkerSetId
                    params.version = this.checkersetConf.version
                    if (this.checkersetConf.legacy && this.checkersetConf.codeLangList.length === 1) {
                        params.toolName = this.checkersetConf.toolList
                    }
                }
                const res = await this.$store.dispatch('checker/list', params)
                this.pageLoading = false
                this.isFetched = true
                if (res) {
                    if (this.selectParams.pageNum > 1) { // 滚动加载
                        this.list = this.list.concat(res)
                        this.handleAutoSelect()
                    } else {
                        this.tableIndex = 0
                        this.list = res
                        this.handleAutoSelect()
                    }
                }
            },
            handleAutoSelect (list = []) {
                this.$nextTick(() => {
                    if (list.length) {
                        list.map(option => {
                            this.$refs.ruleListTable.toggleRowSelection(option, true)
                        })
                    } else {
                        const selectedOption = []
                        this.selectedConf.forEach(item => {
                            const target = this.list.filter(val => val.toolName === item.toolName && val.checkerKey === item.checkerKey)
                            if (target) selectedOption.push(target[0])
                        })
                        selectedOption.map(option => {
                            this.$refs.ruleListTable.toggleRowSelection(option, true)
                        })
                    }
                })
            },
            handleSelect (value) {
                this.selectParams.pageNum = 1
                this.selectParams = Object.assign({}, this.selectParams, value)
            },
            handleKeyWordSearch (value) {
                this.selectParams.pageNum = 1
                this.selectParams = Object.assign({}, this.selectParams, { keyWord: value })
            },
            handleRowClick (row = this.list[this.tableIndex], index = this.tableIndex) {
                this.sliderIsShow = true
                this.contentLoading = true
                this.tableIndex = index
                // this.checker = row
                if (row.editable) {
                    const matchItem = this.localRuleParam.find(item => item.checkerKey === row.checkerKey)
                    if (matchItem) {
                        this.checker = {
                            ...row,
                            props: matchItem.props,
                            propValue: matchItem.propValue
                        }
                    } else {
                        this.checker = {
                            ...row,
                            propValue: JSON.parse(row.props)[0].propValue
                        }
                    }
                } else this.checker = row

                const codeMirrorMode = CodeMirror.findModeByName(row.checkerLanguage[0])
                if (codeMirrorMode && codeMirrorMode.mode) {
                    const mode = codeMirrorMode.mode
                        import(`codemirror/mode/${mode}/${mode}.js`).then(m => {
                            this.errCodeViewer.setOption('mode', mode)
                            this.rightCodeViewer.setOption('mode', mode)
                        })
                }
                
                const checkerKey = row.checkerKey.replace(/\//g, '%2f')
                this.$store.dispatch('defect/getWarnContent', { toolName: row.toolName, checkerKey: checkerKey }).then(res => {
                    if (res.errExample) {
                        this.errCodeViewer.setValue(res.errExample)
                        this.errExampleEmpty = false
                    } else {
                        this.errCodeViewer.setValue('// 暂无')
                        this.errExampleEmpty = true
                    }
                    if (res.rightExample) {
                        this.rightCodeViewer.setValue(res.rightExample)
                        this.rightExampleEmpty = false
                    } else {
                        this.rightCodeViewer.setValue('// 暂无')
                        this.rightExampleEmpty = true
                    }
                    const tools = ['COVERITY', 'KLOCWORK']
                    if (tools.includes(row.toolName) && res.codeExample) {
                        this.hasDetail = true
                        document.getElementById('checkerDetail').innerHTML = res.codeExample
                    } else if (['CHECKSTYLE'].includes(row.toolName)) {
                        this.hasDetail = true
                        document.getElementById('checkerDetail').innerHTML = res.checkerDetailDesc || ''
                    } else {
                        this.hasDetail = false
                    }
                    this.$nextTick(() => {
                        this.errCodeViewer.refresh()
                        this.rightCodeViewer.refresh()
                    })
                }).finally(() => {
                    this.contentLoading = false
                })
            },
            formatTool (tool) {
                return this.toolMap[tool] && this.toolMap[tool].displayName
            },
            getSeverityByNum (num) {
                const map = ['', '严重', '一般', '', '提示']
                return map[num]
            },
            handleClear () {
                this.keyWord = ''
                const { pageNum, pageSize, sortField, sortType } = this.selectParams
                this.selectParams = { pageNum, pageSize, sortField, sortType }
                this.$refs.searchParams.handleClear()
                this.activeName = this.isConfig ? ['checkerCategory', 'toolName'] : ['checkerLanguage', 'checkerCategory']
            },
            updateParameter () {
                if (this.ruleData.propValue) {
                    const props = JSON.parse(this.ruleData.props)
                    props[0].propValue = this.ruleData.propValue
                    const tarList = []
                    if (this.localRuleParam.some(item => item.checkerKey === this.ruleData.checkerKey)) {
                        this.localRuleParam.map(item => {
                            let temp = {}
                            if (item.checkerKey === this.ruleData.checkerKey) {
                                temp = { ...item, props: JSON.stringify(props), propValue: this.ruleData.propValue }
                            } else temp = { ...item }
                            // return temp
                            tarList.push(temp)
                        })
                    } else {
                        this.ruleData.props = JSON.stringify(props)
                        tarList.push(this.ruleData)
                    }
                    this.updateVisiable = false
                    this.updateConfParame(tarList)
                }
            },
            handleCheckerEdit (row, isSlider) {
                const matchItem = this.localRuleParam.find(item => item.checkerKey === row.checkerKey)
                if (matchItem) {
                    this.ruleData = {
                        ...row,
                        props: matchItem.props,
                        propValue: matchItem.propValue
                    }
                } else {
                    this.ruleData = {
                        ...row,
                        propValue: JSON.parse(row.props)[0].propValue
                    }
                }
                this.updateVisiable = true
                if (isSlider) this.sliderIsShow = false
            },
            handleSortChange ({ column, prop, order }) {
                const orders = { ascending: 'ASC', descending: 'DESC' }
                this.selectParams = { ...this.selectParams, pageNum: 1, sortField: prop, sortType: orders[order] }
            },
            selectRules (row, checker) {
                const validArr = row.filter(val => val)
                const isChecked = validArr.filter(item => (item.checkerKey === checker.checkerKey) && (item.toolName === checker.toolName)).length
                this.handleSelectRules(checker, isChecked)
                this.handleSelectRules(validArr, true, true)
            },
            async selectAllRules (data) {
                if (!this.hasNoPermission) {
                    const params = { ...this.selectParams, checkerLanguage: this.checkersetConf.codeLang, pageNum: 1, pageSize: 100000 }
                    const res = await this.$store.dispatch('checker/list', params)
                    const validArr = data.filter(val => val)
                    if (validArr.length) { // 全选
                        this.handleSelectRules(res, true, true)
                    } else { // 取消全选
                        this.handleSelectRules(res, false, true)
                    }
                }
            },
            handleRowClass ({ row, rowIndex }) {
                if (rowIndex === this.tableIndex) {
                    return 'current-row'
                }
                return ''
            },
            screenScroll () {
                this.$nextTick(() => {
                    const childrens = this.$refs.ruleListTable.$refs.bodyWrapper
                    if (childrens) {
                        const height = this.tableIndex > 3 ? (this.tableIndex - 3) * 42 : 0
                        childrens.scrollTo({
                            top: height,
                            behavior: 'smooth'
                        })
                    }
                })
            },
            formatTime (time) {
                return time ? format(time, 'YYYY-MM-DD HH:mm:ss') : '--'
            },
            updateActiveName (activeName) {
                this.activeName = activeName
            }
        }
    }
</script>

<style lang="postcss">
    @import './../defect/codemirror.css';
    .severity-1, .severity-2, .severity-4 {
        &::before {
            content: "";
            display: inline-block;
            width: 10px;
            height: 10px;
            border-radius: 50%;
            background: #FF5656;
            margin-right: 5px;
        }
    }
    .severity-2 {
        &::before {
            background: #FF9C01;
        }
    }
    .severity-4 {
        &::before {
            background: #FFE22B;
        }
    }
</style>

<style lang="postcss" scoped>
    .cc-checkers {
        padding: 0 40px;
        display: flex;
        min-width: 1244px;
        .cc-side, .cc-main {
            background: #fff;
            height: 100%;
            display: block;
            border: 1px solid #dcdee5;
        }
        .cc-side {
            margin-right: 16px;
            padding: 0 16px;
            width: 240px;
            
            .cc-filter {
                height: 52px;
                line-height: 52px;
                border-bottom: 1px solid #dcdee5;
                .cc-filter-txt {
                    font-size: 14px;
                    color: #333333;
                }
                .cc-filter-select {
                    float: right;
                }
                .cc-filter-clear {
                    float: right;
                    position: relative;
                    padding-left: 10px;
                    cursor: pointer;
                    /* &::before {
                        content: "";
                        position: absolute;
                        width: 1px;
                        height: 18px;
                        background-color: #dcdee5;
                        left: 0;
                        top: 18px;
                    } */
                }
            }
            .cc-filter-content {
                overflow-y: scroll;
                max-height: calc(100% - 108px);
                margin: 0 -10px;
                &::-webkit-scrollbar {
                    width: 4px;
                }
                &::-webkit-scrollbar-thumb {
                    border-radius: 13px;
                    background-color: #d4dae3;
                }
            }
            .checker-search {
                padding: 8px 0;
            }
        }
        .cc-main {
            width: calc(100% - 250px);
            padding: 0 24px 24px;
            .table-config {
                padding: 14px 0;
                .disable-save {
                    background-color: #dcdee5;
                    border-color: #dcdee5;
                    color: #fff;
                    cursor: not-allowed;
                }
            }
            .config-txt {
                margin-left: 10px;
                color: #777;
                font-size: 12px;
                .num {
                    color: #3a84ff;
                }
            }
            .cc-keyboard {
                float: right;
                height: 42px;
                font-size: 12px;
                line-height: 30px;
                color: #333;
                .cc-button {
                    font-size: 12px;
                    color: #699df4;
                }
            }
            .table-txt {
                color: #777;
                font-size: 12px;
                line-height: 44px;
            }
            .bk-table {
                height: calc(100% - 40px);
                >>>.bk-table-body-wrapper {
                    height: calc(100% - 43px);
                    overflow-y: scroll;
                    .checker-edit {
                        display: inline-block;
                        max-width: calc(100% - 24px);
                        overflow: hidden;
                        white-space: nowrap;
                        text-overflow: ellipsis;
                    }
                    .icon-edit {
                        padding: 0 8px;
                        position: relative;
                        top: -3px;
                    }
                    .table-tool {
                        color: #63656e;
                        &:hover {
                            color: #3a84ff;
                        }
                    }
                }
            }
        }
    }
    .checker-content {
        margin: 10px 32px;
        .checker-form {
            .checker-col {
                display: inline-block;
                width: 320px;
                font-size: 14px;
                line-height: 14px;
                padding: 9px 0;
                &.checker-col-block {
                    display: block;
                    width: auto;
                    line-height: 24px;
                    padding: 3px 0;
                    .checker-col-content {
                        margin-bottom: -4px;
                        max-width: 570px;
                        white-space: normal;
                        line-height: 24px;
                    }
                }
                .checker-col-label {
                    width: 72px;
                    display: inline-block;
                    color: #63656e;
                    text-align: right;
                    vertical-align: top;
                    &.label-fixed {
                        position: relative;
                        top: 5px;
                    }
                }
                .checker-col-content {
                    width: auto;
                    max-width: 242px;
                    display: inline-block;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                    word-break: break-all;
                    color: #333;
                    margin-bottom: -2px;
                }
                .icon-edit {
                    margin-left: 6px;
                    cursor: pointer;
                    &:hover {
                        color: #3a84ff;
                    }
                }
            }
            .checker-top {
                display: inline-block;
                margin-bottom: 8px;
                padding-bottom: 8px;
                border-bottom: 1px solid #dcdee5;
                .checker-col-label, .checker-col-content {
                    color: #999;
                }
            }
        }
        >>> #checkerDetail {
            font-size: 14px;
            h3.title, h4, p {
                font-size: 14px;
            }
            pre {
                padding: 10px;
                border: 1px solid #dcdee5;
            }
        }
    }
    .checker-tag {
        background: #c9dffa;
        border-radius: 2px;
        padding: 2px 8px;
        display: inline-block;
        line-height: 20px;
    }
    .update-parameter {
        .update-parameter-title {
            width: 470px;
            overflow: hidden;
            position: relative;
            top: -20px;
            font-size: 20px;
        }
        >>>.bk-form-content {
            line-height: 30px;
        }
        .params-input {
            width: 220px;
        }
    }
    #errExample, #rightExample {
        width: 100%;
        height: 160px;
        border: 1px solid #e0e6eb;
    }
    >>> .bk-sideslider-wrapper {
        padding-bottom: 0;
    }
    .example-empty {
        >>> pre.CodeMirror-line {
            color: #a50;
        }
    }
</style>
