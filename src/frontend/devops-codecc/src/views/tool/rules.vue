<template>
    <div class="main-content rules">
        <div v-if="toolList.length">
            <div class="breadcrumb">
                <div class="breadcrumb-name tool-list-tab">
                    <bk-tab :active.sync="active" @tab-change="handleTableChange" type="border-card">
                        <bk-tab-panel
                            v-for="(panel, index) in toolList"
                            v-bind="panel"
                            :key="index">
                        </bk-tab-panel>
                    </bk-tab>
                </div>
            <!-- <div class="breadcrumb-extra">
                <a @click="show = true"><i class="bk-icon icon-order"></i>{{$t('操作记录')}}</a>
            </div> -->
            </div>
            <div class="main-container">
                <div class="rules-header">
                    <div class="rules-header-left content">
                        <span>{{$t('规则集')}}</span>
                        <span class="rules-header-left-split">|</span>
                        <span>{{ usingData[1].checkerSetName }}</span>
                    </div>
                    <div class="rules-header-right content">
                        <bk-button theme="primary" @click="showChooseCollection">{{$t('选择规则集')}}</bk-button>
                        <bk-button theme="default" @click="showSaveCollection">{{$t('存为规则集')}}</bk-button>
                    </div>
                </div>
                <bk-table style="margin-top: 15px;"
                    :data="usingData"
                    :row-border="false"
                    :show-header="false"
                    :cell-style="cellStyle"
                    size="small">
                    <bk-table-column :label="$t('规则集ID')" prop="checkerSetId"></bk-table-column>
                    <bk-table-column :label="$t('语言')" prop="codeLang">
                        <template slot-scope="scope"> <span :title="scope.row.codeLang">{{ scope.row.codeLang }}</span></template>
                    </bk-table-column>
                    <bk-table-column :label="$t('开启规则数')" prop="checkerCount"></bk-table-column>
                    <bk-table-column :label="$t('发布者')" prop="creator"></bk-table-column>
                    <bk-table-column :label="$t('版本')" prop="version">
                        <template slot-scope="scope">
                            <span>
                                {{ scope.row.version }}
                                <i
                                    @click="showUpdateCollection"
                                    v-show="usingData[1].version && scope.row.version.indexOf('v') !== -1 && scope.row.version !== scope.row.latestVersion"
                                    class="bk-icon icon-arrows-up-circle"
                                    style="cursor: pointer;"
                                    :title="$t('可更新为x', { x: scope.row.latestVersion })"
                                ></i>
                            </span>
                        </template>
                    </bk-table-column>
                    <div slot="empty">
                        <div class="codecc-table-empty-text">
                            <img src="../../images/empty.png" class="empty-img">
                            <div>{{$t('暂无数据')}}</div>
                        </div>
                    </div>
                </bk-table>
                <divider />
                <div id="card-position" ref="parents" class="rules-body">
                    <div class="rules-body-right">
                        <bk-select @toggle="searchIcon" id="searchSelect" @clear="contentClose" @change="search" v-model="searchData" class="rules-body-select" searchable>
                            <bk-option v-for="(option, index) in optionData"
                                :key="index"
                                :id="option"
                                :name="option">
                            </bk-option>
                        </bk-select>
                    </div>
                    <p>{{$t('默认')}}</p>
                    <div class="rules-body-default" v-for="(value, index) in rulesData" :key="index">
                        <Card :ref="index" v-if="index === 0" :data="rulesData[0]" :index="index" @change="contentVisibaleChange" />
                    </div>
                    <p v-if="toolRules.checkerPackages && toolRules.checkerPackages.length > 1">{{$t('推荐')}}</p>
                    <div class="rules-body-recommend" v-for="(value, index) in rulesData" :key="index" :style="index !== 0 ? '' : 'padding: 0px;'">
                        <Card :ref="index" v-if="index !== 0" :data="value" :index="index" @change="contentVisibaleChange" @close="contentClose" />
                    </div>
                </div>
            </div>

        </div>
        <div v-else>
            <div class="main-container large boder-none">
                <div class="no-task">
                    <empty title="" :desc="'未找到工具对应的规则集。请先添加相应工具'">
                        <template v-slot:action>
                            <bk-button size="large" theme="primary" @click="addTool">{{$t('添加工具')}}</bk-button>
                        </template>
                    </empty>
                </div>
            </div>
        </div>
        <bk-dialog v-model="chooseCollection.visiable"
            width="743px"
            theme="primary"
            :ext-cls="'choose-collection'"
            :position="{ top: 50, left: 5 }"
            :mask-close="false"
            @confirm="relateCheckers">
            <div class="choose-collection-title">
                <span>{{$t('请选择一个规则集')}}</span>
            </div>
            <div class="choose-collection-header">
                <bk-container class="container" :col="20" :gutter="4">
                    <bk-row>
                        <bk-col :span="9"><div class="container-left">
                            <p :title="$t('下列结果符合x语言', { languages: formatLang(detail.codeLang) })">{{$t('下列结果符合x语言', { languages: formatLang(detail.codeLang) })}}</p>
                        </div></bk-col>
                        <bk-col :span="11"><div>
                            <bk-input
                                style="width: 381px;"
                                v-model="searchValue"
                                :placeholder="$t('搜索')"
                                :clearable="true"
                                :right-icon="'bk-icon icon-search'">
                            </bk-input>
                        </div></bk-col>
                    </bk-row>
                </bk-container>
            </div>
            <div class="choose-collection-body">
                <bk-table
                    ref="refTable"
                    class="choose-collection-table"
                    size="small"
                    :data="chooseList"
                    :show-header="false"
                    :row-style="{ backgroundColor: '#fafbfd', cursor: 'pointer' }"
                    :cell-style="{ padding: '0' }"
                    :default-expand-all="true"
                    v-bkloading="{ isLoading: isLoading, title: '数据加载中' }"
                    @cell-click="toExpand">
                    <bk-table-column type="expand" width="30" align="center">
                        <template slot-scope="props">
                            <bk-table @row-click="changeChecked" cell-style="{ font-size: 12px }" :row-style="{ cursor: 'pointer' }" :show-header="false" :data="props.row.children" :outer-border="false" :header-cell-style="{ background: '#fff', borderRight: 'none' }">
                                <bk-table-column width="320">
                                    <template slot-scope="scope">
                                        <bk-radio-group name="checkersName" v-model="chooseData.selected">
                                            <bk-radio :class="scope.row.checkerSetName" :value="scope.row">{{scope.row.checkerSetName}}</bk-radio>
                                        </bk-radio-group>
                                    </template>
                                </bk-table-column>
                                <bk-table-column prop="checkerCount" :label="$t('已开启规则')">
                                    <template slot-scope="scope">{{ scope.row.checkerCount + '条开启规则' }}</template>
                                </bk-table-column>
                                <bk-table-column prop="creator" :label="$t('来源')"></bk-table-column>
                                <template slot="empty">
                                    {{$t('暂无数据')}}
                                </template>
                            </bk-table>
                        </template>
                    </bk-table-column>
                    <bk-table-column :label="$t('列表')" prop="list"></bk-table-column>
                    <div slot="empty">
                        <div class="codecc-table-empty-text">
                            <img src="../../images/empty.png" class="empty-img">
                            <div>{{$t('暂无数据')}}</div>
                        </div>
                    </div>
                </bk-table>
            </div>
        </bk-dialog>
        <bk-dialog v-model="saveCollection.visiable"
            width="743px"
            theme="primary"
            :ext-cls="'save-collection'"
            :mask-close="false"
            :position="{ top: 50, left: 5 }"
            @confirm="save">
            <div class="save-collection-title">
                <span>{{$t('存为规则集')}}</span>
            </div>
            <div class="save-collection-header">
                <span @click="changeTab" class="left" :class="saveCollection.type === 'create' ? 'isUse' : ''">{{$t('新建一个')}}</span>
                <span @click="changeTab" class="right" :class="saveCollection.type === 'upgrade' ? 'isUse' : ''">{{$t('更新现有')}}</span>
            </div>
            <div class="save-collection-body">
                <bk-form :label-width="120" :model="formData" ref="ruleSetForm">
                    <bk-form-item :rules="formRules.checkerSetName" :property="saveCollection.type === 'create' ? 'checkerSetNameC' : editingName ? 'checkerSetNameE' : 'checkerSetNameU'" style="height: 30px;" :label="$t('规则集')">
                        <bk-input class="rule-input" v-if="saveCollection.type === 'create'" v-model="formData.checkerSetNameC" :placeholder="$t('可使用中文、字母、数字、下划线，如：企鹅电竞前端规范')"></bk-input>
                        <bk-select @change="updateUsing" style="width: 440px;" v-if="saveCollection.type === 'upgrade' && !editingName" v-model="formData.checkerSetNameU" searchable>
                            <bk-option v-for="option in checkersList"
                                :key="option.checkerSetId"
                                :id="option.checkerSetId"
                                :name="option.checkerSetName">
                            </bk-option>
                        </bk-select>
                        <bk-input class="rule-input" v-if="saveCollection.type === 'upgrade' && editingName" v-model="formData.checkerSetNameE" :placeholder="$t('可使用中文、字母、数字、下划线，如：企鹅电竞前端规范')"></bk-input>
                        <span class="edit-button" v-if="saveCollection.type === 'upgrade' && !editingName" @click="editName"><i class="edit-icon codecc-icon icon-edit"></i>{{$t('修改规则集')}}</span>
                        <span class="edit-name-button" v-if="saveCollection.type === 'upgrade' && editingName" @click="editName"><i class="edit-name-icon bk-icon icon-check-circle"></i>{{$t('重选')}}</span>
                    </bk-form-item>
                    <bk-form-item :rules="formRules.checkerSetId" :property="saveCollection.type === 'create' ? 'checkerSetIdC' : 'checkerSetIdU'" :label="$t('英文ID')">
                        <bk-input class="rule-input" v-if="saveCollection.type === 'create'" v-model="formData.checkerSetIdC" :placeholder="$t('可使用字母、数字、下划线，如：penguin_web_standard')"></bk-input>
                        <span v-if="saveCollection.type === 'create' && /^[0-9a-zA-Z_]{1,50}$/.test(formData.checkerSetIdC)" v-bk-tooltips="$t('新建完成后不可更改')" class="top-start">
                            <i style="padding-left: 5px;" class="bk-icon icon-info-circle"></i>
                        </span>
                        <p v-if="saveCollection.type === 'upgrade'">{{ usingCheckerSetId || '--' }}</p>
                    </bk-form-item>
                    <bk-form-item :label="$t('语言')">
                        {{ formatLang(detail.codeLang) }}
                    </bk-form-item>
                    <bk-form-item :label="$t('工具')">
                        {{displayToolName}}
                    </bk-form-item>
                    <bk-form-item :label="$t('规则数量')">
                        {{using}}
                    </bk-form-item>
                    <bk-form-item :label="$t('可见范围')">
                        <bk-radio-group v-model="formData.scope">
                            <bk-radio :value="1">{{$t('公开')}}
                                <span v-bk-tooltips="$t('所有用户可使用')" class="top-start">
                                    <i class="bk-icon icon-info-circle"></i>
                                </span>
                            </bk-radio>
                            <span style="padding-right: 40px;">
                            </span>
                            <bk-radio :value="2">{{$t('仅当前项目可用')}}</bk-radio>
                        </bk-radio-group>
                    </bk-form-item>
                    <bk-form-item>
                        <bk-checkbox-group v-if="saveCollection.type === 'upgrade'" v-model="formData.upgradeMyOtherTasks">
                            <bk-checkbox :value="'Y'">{{$t('你共有x个任务正使用该规则集，将它们同步更新', { num: usingProject })}}</bk-checkbox>
                        </bk-checkbox-group>
                    </bk-form-item>
                </bk-form>
            </div>
        </bk-dialog>
        <bk-dialog v-model="upgradeCollection.visiable"
            width="743px"
            theme="primary"
            :render-directive="'if'"
            :ext-cls="'upgrade-collection'"
            :mask-close="false"
            :position="{ top: 20, left: 5 }"
            @confirm="relateCheckers">
            <div class="upgrade-collection-title">
                <span>{{$t('更新版本')}}</span>
            </div>
            <div v-bkloading="{ isLoading: isLoading, title: '数据加载中' }" class="upgrade-collection-body">
                <bk-form :label-width="120">
                    <bk-form-item :label="$t('版本:')">
                        <p>{{ usingData[1].version }} >>> {{ usingData[1].latestVersion }}</p>
                    </bk-form-item>
                    <bk-form-item :label="$t('规则数:')">
                        <p>{{ upgradeShowingData.checkerCountFrom }} >>> {{ upgradeShowingData.checkerCountTo }}</p>
                    </bk-form-item>
                    <bk-form-item :label="$t('更新时间:')">
                        <p>{{ formatDate(upgradeShowingData.lastUpdateTime) }}</p>
                    </bk-form-item>
                    <bk-form-item :label="$t('发布者:')">
                        <p>{{ upgradeData.creator }}</p>
                    </bk-form-item>
                    <bk-form-item :label="$t('变更详情:')">
                    </bk-form-item>
                    <bk-input
                        ext-cls="change-area"
                        :readonly="true"
                        :type="'textarea'"
                        :rows="7"
                        v-model="changeContent">
                    </bk-input>
                    <bk-form-item>
                        <bk-checkbox-group class="change-under" v-model="formData.upgradeMyOtherTasks">
                            <bk-checkbox :value="'Y'">{{$t('你共有x个任务正使用该规则集，将它们同步更新', { num: usingUpgradeProject })}}</bk-checkbox>
                        </bk-checkbox-group>
                    </bk-form-item>
                </bk-form>
            </div>
        </bk-dialog>
        <Record :visiable.sync="show" :data="this.$route.name" />
    </div>
</template>

<script>
    import { mapState } from 'vuex'
    import { format } from 'date-fns'
    import Card from '@/components/rules-card/index'
    import Divider from '@/components/divider'
    import Record from '@/components/operate-record/index'
    import Empty from '@/components/empty'

    export default {
        components: {
            Card,
            Divider,
            Record,
            Empty
        },
        data () {
            return {
                active: this.$route.params.toolId,
                show: false,
                searchData: '',
                chooseCollection: {
                    visiable: false
                },
                saveCollection: {
                    visiable: false,
                    type: 'create'
                },
                upgradeCollection: {
                    visiable: false,
                    type: false
                },
                chooseList: [{
                    id: '1',
                    list: this.$t('我创建的/我的任务正在使用'),
                    children: []
                }, {
                    id: '2',
                    list: this.$t('CodeCC推荐'),
                    children: []
                }, {
                    id: '3',
                    list: this.$t('更多公开规则集'),
                    children: []
                }],
                formData: {
                    upgradeMyOtherTasks: [],
                    checkerSetNameC: '', // 新建的规则集
                    checkerSetNameU: '', // 更新规则集
                    checkerSetNameE: '', // 新命名的规则集
                    checkerSetIdU: '', // 新命名的规则集ID
                    checkerSetIdC: '', // 新建的规则集ID
                    scope: 1
                },
                chooseData: {
                    selected: ''
                },
                searchValue: '',
                checkersList: [],
                usingProject: 0,
                usingCheckerSetId: '',
                editingName: false,
                formRules: {
                    checkerSetName: [
                        {
                            required: true,
                            message: '必填项',
                            trigger: 'blur'
                        },
                        {
                            regex: /^[a-zA-Z0-9_\u4e00-\u9fa5]{1,50}/,
                            message: '请以中文、字母、数字、下划线命名',
                            trigger: 'blur'
                        }
                    ],
                    checkerSetId: [
                        {
                            required: true,
                            message: '必填项',
                            trigger: 'blur'
                        },
                        {
                            regex: /^[0-9a-zA-Z_]{1,50}$/,
                            message: '请以字母、数字、下划线命名',
                            trigger: 'blur'
                        }
                    ]
                },
                isLoading: false,
                upgradeShowingData: {}
            }
        },
        computed: {
            ...mapState('tool', {
                toolRules: 'rules',
                toolCheckers: 'checkers',
                toolMap: 'mapList'
            }),
            ...mapState('task', {
                detail: 'detail'
            }),
            ...mapState([
                'toolMeta'
            ]),
            rulesData () {
                let rulesData = []
                if (this.toolRules && this.toolRules.checkerPackages) {
                    rulesData = Object.assign([], this.toolRules.checkerPackages)
                }
                rulesData.map(item => {
                    item.open = false
                })
                return rulesData
            },
            upgradeData () {
                let upgradeData = {}
                if (this.toolRules && this.toolRules.checkerSet) {
                    upgradeData = Object.assign({}, this.toolRules.checkerSet)
                }
                return upgradeData
            },
            usingData () {
                const usingData = [{
                    checkerSetId: '规则集ID',
                    creator: '发布者',
                    version: '版本',
                    checkerCount: '开启规则数',
                    codeLang: '语言'
                }, {}]
                if (this.toolRules && this.toolRules.checkerSet) {
                    usingData[1] = this.toolRules.checkerSet
                    usingData[1].checkerSetName = this.toolRules.checkerSet.checkerSetName || '--'
                    usingData[1].codeLang = this.formatLang(usingData[1].codeLang)
                    usingData[1].checkerSetId = this.toolRules.checkerSet.checkerSetId || '--'
                    usingData[1].creator = this.toolRules.checkerSet.creator || '--'
                    usingData[1].version = this.toolRules.checkerSet.version ? 'v' + this.toolRules.checkerSet.version : '--'
                    usingData[1].latestVersion = this.toolRules.checkerSet.latestVersion
                        ? 'v' + this.toolRules.checkerSet.latestVersion
                        : ''
                }
                return usingData
            },
            optionData () {
                const optionData = []
                for (const item in this.rulesData) {
                    for (const i in this.rulesData[item].checkerList) {
                        optionData.push(this.rulesData[item].checkerList[i].checkerKey)
                    }
                }
                if (document.getElementById('searchSelect')) {
                    let icon = document.getElementById('searchSelect').childNodes[0].getAttribute('class', 'bk-select-angle')
                    icon = icon.replace('icon-angle-down', 'icon-search')
                    document.getElementById('searchSelect').childNodes[0].setAttribute('class', icon)
                }
                return optionData
            },
            using () {
                let using = 0
                for (const item in this.rulesData) {
                    using = using + this.rulesData[item].openCheckerNum
                }
                return using
            },
            taskId () {
                return this.$route.params.taskId
            },
            toolName () {
                return this.$route.params.toolId
            },
            changeContent () {
                let changeContent = ''
                const arr = this.upgradeShowingData.packages || []
                arr.forEach(item => {
                    changeContent = changeContent + item.pkgName + ':\n' + item.differentCheckers.join('\n') + '\n'
                })
                return changeContent
            },
            displayToolName () {
                const vm = this
                const { enableToolList } = this.detail
                let displayToolName = ''
                enableToolList.forEach(tool => {
                    if (tool.toolName === vm.toolName) displayToolName = tool.toolDisplayName
                })
                return displayToolName
            },
            usingUpgradeProject () {
                let usingUpgradeProject = 0
                this.checkersList.forEach(checkers => {
                    if (this.toolRules && checkers.checkerSetId === this.toolRules.checkerSet.checkerSetId) {
                        usingUpgradeProject = checkers.usage
                    }
                })
                return usingUpgradeProject
            },
            toolList () {
                const tools = this.detail.enableToolList.filter(item => {
                    return item.toolName !== 'CCN' && item.toolName !== 'DUPC'
                })
                tools.map(item => {
                    item.name = item.toolName
                    item.label = item.toolDisplayName
                })
                if (tools.length) this.init()
                return tools
            }
        },
        watch: {
            'searchValue': {
                handler: function (oldVal, newVal) {
                    console.log(this.searchValue)
                    this.searching(this.searchValue)
                },
                deep: true
            },
            'upgradeCollection.visiable': {
                handler: function (oldVal, newVal) {
                    if (newVal) this.upgradeCollection.type = false
                },
                deep: true
            },
            'saveCollection.visiable': {
                handler: function (oldVal, newVal) {
                    if (newVal) this.editingName = false
                },
                deep: true
            }
        },
        methods: {
            init () {
                const { taskId, toolId } = this.$route.params
                this.$store.dispatch('tool/rules', { taskId, toolName: toolId })
            },
            contentClose (index) {
                this.rulesData.map(item => {
                    item.open = false
                })
                for (let i = 0; i < this.rulesData.length; i++) {
                    this.rulesData[i] = Object.assign({}, this.rulesData[i])
                }
                this.$forceUpdate()
            },
            contentVisibaleChange (index) {
                if (this.rulesData[index].open === false) {
                    this.rulesData.map(item => {
                        item.open = false
                    })
                    this.rulesData[index].open = true
                    if (this.$refs[index] && this.$refs[index][0]) {
                        const top = 18 + this.$refs[index][0].$el.offsetTop
                        setTimeout(() => {
                            document.getElementsByClassName('main-container')[0].scrollTo({
                                left: 0,
                                top: top,
                                behavior: 'smooth'
                            })
                        }, 0)
                    }
                } else {
                    this.rulesData.map(item => {
                        item.open = false
                    })
                }
                for (let i = 0; i < this.rulesData.length; i++) {
                    this.rulesData[i] = Object.assign({}, this.rulesData[i])
                }
                this.$forceUpdate()
            },
            search (val) {
                for (const item in this.rulesData) {
                    for (const i in this.rulesData[item].checkerList) {
                        if (this.rulesData[item].checkerList[i].checkerKey === val) {
                            this.rulesData.map(item => {
                                item.open = false
                            })
                            this.rulesData[item].open = true
                            if (this.$refs[item] && this.$refs[item][0]) {
                                const top = 18 + this.$refs[item][0].$el.offsetTop
                                setTimeout(() => {
                                    document.getElementsByClassName('main-container')[0].scrollTo({
                                        top: top,
                                        left: 0,
                                        behavior: 'smooth'
                                    })
                                }, 0)
                            }
                            for (let i = 0; i < this.rulesData.length; i++) {
                                this.rulesData[i] = Object.assign({}, this.rulesData[i])
                            }
                            this.$forceUpdate()
                            this.$refs[item][0].position(i)
                        }
                    }
                }
            },
            cellStyle ({ row, rowIndex }) {
                if (rowIndex === 0) {
                    return { fontSize: '12px', border: 'none', backgroundColor: '#fff' }
                } else if (rowIndex === 1) {
                    return { fontSize: '14px', border: 'none', backgroundColor: '#fff' }
                }
            },
            toExpand (row, colum, event, cell) {
                this.$refs.refTable.toggleRowExpansion(row)
            },
            showChooseCollection () {
                this.getRelateCheckerList()
                this.chooseCollection.visiable = true
            },
            showSaveCollection () {
                this.getUserCheckerList()
                this.saveCollection.visiable = true
            },
            changeChecked (row, colum, event, cell) {
                this.chooseData.selected = row
            },
            searching (val) {
                const searchChecker = function (arr) {
                    for (const i in arr) {
                        const arr1 = arr[i].filter(item => item.checkerSetName.indexOf(val) > -1)
                        const arr2 = arr[i].filter(item => item.checkerCount.toString().indexOf(val) > -1)
                        const arr3 = arr[i].filter(item => item.creator.indexOf(val) > -1)
                        vm.chooseList[i].children = Array.from(new Set(arr1.concat(arr2, arr3)))
                    }
                }
                const vm = this
                const arr = []
                for (const j in this.toolCheckers.checkerSets[0]) {
                    if (Array.isArray(this.toolCheckers.checkerSets[0][j])) {
                        arr.push(this.toolCheckers.checkerSets[0][j])
                    }
                }
                searchChecker(arr)
            },
            getRelateCheckerList () {
                this.isLoading = true
                this.$store.dispatch('tool/checker', { toolNames: [this.toolName], taskId: this.taskId }).then(res => {
                    if (res.checkerSets && res.checkerSets.length > 0) {
                        this.chooseList[0].children = res.checkerSets[0].myProjUse || {}
                        this.chooseList[2].children = res.checkerSets[0].others || {}
                        this.chooseList[1].children = res.checkerSets[0].recommended || {}
                    }
                }).catch(e => {
                    this.$bkMessage({ theme: 'error', message: '获取关联规则集列表失败' })
                }).finally(() => {
                    this.isLoading = false
                })
            },
            getUserCheckerList () {
                this.$store.dispatch('tool/checkerList', { toolName: this.toolName }).then(res => {
                    if (res) {
                        this.checkersList = res.userCreatedCheckerSets
                    }
                })
            },
            save () {
                if (this.saveCollection.type === 'create') {
                    const params = {
                        data: {
                            checkerSetId: this.formData.checkerSetIdC,
                            checkerSetName: this.formData.checkerSetNameC,
                            scope: this.formData.scope
                        },
                        taskId: this.taskId,
                        toolName: this.toolName
                        
                    }
                    this.$store.dispatch('tool/createChecker', params).then(res => {
                        if (res.data) {
                            this.$bkMessage({ theme: 'success', message: '新建成功' })
                        }
                    }).catch(e => {
                        this.$bkMessage({ theme: 'error', message: '新建失败' })
                    }).finally(() => {
                        this.init()
                        this.formData = {
                            upgradeMyOtherTasks: [],
                            checkerSetNameC: '',
                            checkerSetNameU: '',
                            checkerSetNameE: '',
                            checkerSetIdU: '',
                            checkerSetIdC: '',
                            scope: 1
                        }
                    })
                } else {
                    const params = {
                        data: {
                            upgradeMyOtherTasks: this.formData.upgradeMyOtherTasks[0] ? 'Y' : 'N',
                            checkerSetName: this.editingName
                                ? this.formData.checkerSetNameE
                                : this.changeIntoCheckerName(this.formData.checkerSetNameU),
                            scope: this.formData.scope
                        },
                        taskId: this.taskId,
                        toolName: this.toolName,
                        checkerSetId: this.formData.checkerSetIdU
                    }
                    this.$store.dispatch('tool/updateCheckers', params).then(res => {
                        if (res) {
                            this.$bkMessage({ theme: 'success', message: '更新成功' })
                        }
                    }).catch(e => {
                        this.$bkMessage({ theme: 'error', message: '更新失败' })
                    }).finally(() => {
                        this.init()
                        this.formData = {
                            upgradeMyOtherTasks: [],
                            checkerSetNameC: '',
                            checkerSetNameU: '',
                            checkerSetNameE: '',
                            checkerSetIdU: '',
                            checkerSetIdC: '',
                            scope: 1
                        }
                    })
                }
            },
            relateCheckers () {
                let params = {}
                if (this.upgradeCollection.type) {
                    params = {
                        toolCheckerSets: [{
                            toolName: this.toolName,
                            checkerSetId: this.toolRules.checkerSet.checkerSetId,
                            version: this.toolRules.checkerSet.latestVersion.slice(1),
                            upgradeCheckerSetOfUserTasks: this.formData.upgradeMyOtherTasks[0] ? 'Y' : 'N'
                        }],
                        taskId: this.taskId
                    }
                } else {
                    if (this.chooseData.selected === '') {
                        this.$bkMessage({ theme: 'warning', message: '请先选择规则集' })
                        return
                    }
                    params = {
                        toolCheckerSets: [{
                            toolName: this.toolName,
                            checkerSetId: this.chooseData.selected.checkerSetId,
                            version: this.chooseData.selected.version
                        }],
                        taskId: this.taskId
                    }
                }
                this.$store.dispatch('tool/relateCheckers', params).then(res => {
                    if (res.data) {
                        this.$bkMessage({ theme: 'success', message: '关联成功' })
                    }
                }).catch(e => {
                    this.$bkMessage({ theme: 'error', message: '关联失败' })
                }).finally(() => {
                    this.init()
                    this.formData = {
                        upgradeMyOtherTasks: [],
                        checkerSetNameC: '',
                        checkerSetNameU: '',
                        checkerSetNameE: '',
                        checkerSetIdU: '',
                        checkerSetIdC: '',
                        scope: 1
                    }
                })
            },
            formatLang (num) {
                return this.toolMeta.LANG.map(lang => lang.key & num ? lang.name : '').filter(name => name).join('; ')
            },
            updateUsing (newValue, oldValue) {
                const arr = this.checkersList.filter(item => item.checkerSetId === newValue)
                if (arr.length === 1) {
                    this.usingProject = arr[0].usage
                    this.formData.scope = arr[0].scope
                    this.formData.checkerSetIdU = arr[0].checkerSetId
                    this.usingCheckerSetId = arr[0].checkerSetId
                } else {
                    this.usingProject = 0
                    this.formData.scope = ''
                    this.usingCheckerSetId = '--'
                    this.formData.checkerSetIdU = ''
                }
            },
            editName () {
                if (this.formData.checkerSetNameU === '') {
                    this.$bkMessage({ theme: 'warning', message: '请先选择规则集' })
                    return
                }
                this.$refs.ruleSetForm.formItems.forEach(item => {
                    item.clearValidator()
                })
                this.editingName = !this.editingName
            },
            changeTab () {
                this.$refs.ruleSetForm.formItems.forEach(item => {
                    item.clearValidator()
                })
                this.saveCollection.type === 'create' ? this.saveCollection.type = 'upgrade' : this.saveCollection.type = 'create'
            },
            searchIcon (val) {
                if (val) {
                    document.getElementById('searchSelect').childNodes[0].style.display = 'none'
                } else {
                    document.getElementById('searchSelect').childNodes[0].style.display = ''
                }
            },
            formatDate (dateNum) {
                return dateNum ? format(dateNum, 'YYYY-MM-DD HH:mm:ss') : '--'
            },
            showUpdateCollection () {
                this.getUserCheckerList()
                this.getChangeCheckerList()
                this.upgradeCollection.visiable = true
                this.saveCollection.type = 'upgrade'
                this.upgradeCollection.type = true
            },
            getChangeCheckerList () {
                this.isLoading = true
                const params = {
                    fromVersion: this.toolRules.checkerSet.version.slice(1),
                    toVersion: this.toolRules.checkerSet.latestVersion.slice(1),
                    toolName: this.toolName,
                    checkerSetId: this.toolRules.checkerSet.checkerSetId
                }
                this.$store.dispatch('defect/newVersion', params).then(res => {
                    if (res) {
                        this.upgradeShowingData = res
                    }
                }).catch(e => {
                }).finally(() => {
                    this.isLoading = false
                })
            },
            changeIntoCheckerName (id) {
                return this.checkersList.find(item => item.checkerSetId === id).checkerSetName
            },
            handleTableChange (value) {
                const toolName = value
                const params = { ...this.$route.params, toolId: toolName }
                this.$router.push({
                    name: 'tool-rules',
                    params
                })
            },
            addTool () {
                this.$router.push({
                    name: 'task-settings-tools'
                })
            }
        }
    }
</script>

<style lang="postcss" scoped>
    @import '../../css/variable.css';
    .main-content {
        padding: 16px 20px 0px 16px;
        .breadcrumb {
            padding: 0px!important;
            .breadcrumb-name {
                background: rgb(97, 82, 82);
            }
        }
        .main-container {
            height: calc(100vh - 130px);
            border: 1px solid #dcdee5;
            margin: 0px!important;
            background: white;
            &.large {
                height: calc(100vh - 90px);
            }
        }
    }
    >>>.bk-tab-section {
        display: none;
    }
    .rules {
        .rules-header {
            .rules-header-left {
                float: left;
                margin-top: 5px;
                .rules-header-left-split {
                    color: #979ba5;
                    padding: 0 20px;
                }
            }
            .rules-header-right {
                float: right;
                padding-bottom: 12px;
            }
        }
        .container {
            padding: 0!important;
        }
        .content {
            span {
                font-size: 16px;
                font-weight:bold;
                color: #63656e;
            }
        }
        .rules-body {
            font-size:14px;
            color: #63656e;
            p {
                padding: 10px 0;
                font-weight:bold;
            }
            p:nth-child(2) {
                padding-top: 5px;
            }
            .rules-body-default, .rules-body-recommend {
                display: inline-table;
                height:19px;
                line-height:19px;
                padding: 5px;
            }
            .rules-body-right {
                display: flex;
                float: right;
                .rules-body-select {
                    width: 420px;
                    background-color: #ffffff;
                }
            }
        }
    }
    .choose-collection-title {
        position: relative;
        top: -20px;
        font-size: 20px;
    }
    .choose-collection-header {
        position: relative;
        .container {
            padding: 0!important;
        }
    }
    .choose-collection-body {
        margin-top: 15px;
        overflow: scroll;
        height: 350px;
    }
    .save-collection-title {
        position: relative;
        top: -20px;
        font-size: 20px;
    }
    .save-collection-header {
        position: relative;
        font-size: 14px;
        .left {
            position: absolute;
            left: 33%;
            width: 120px;
            line-height: 32px;
            text-align: center;
            border: 1px solid #c4c6cc;
            cursor: pointer;
            z-index: 1;
        }
        .right {
            position: absolute;
            left: 50.1%;
            width: 120px;
            line-height: 32px;
            text-align: center;
            border: 1px solid #c4c6cc;
            cursor: pointer;
            z-index: 1;
        }
        .isUse {
            color: #3a84ff;
            border: 1px solid #3a84ff;
            background-color: #e1ecff;
            z-index: 2;
        }
    }
    .save-collection-body {
        padding-top: 53px;
        .rule-input {
            width: 440px;
        }
        .edit-button {
            position: relative;
            top: -31px;
            left: 450px;
            cursor: pointer;
        }
        .edit-icon {
            position: relative;
            top: -1px;
            right: 5px;
            cursor: pointer;
        }
        .edit-name-icon {
            position: relative;
            top: 33px;
            right: 464px;
            cursor: pointer;
        }
        .edit-name-button {
            position: relative;
            top: -31px;
            left: 465px;
            cursor: pointer;
        }
    }
    .upgrade-collection-title {
        position: relative;
        top: -20px;
        font-size: 20px;
    }
    .upgrade-collection-header {
        position: relative;
        font-size: 14px;
    }
    .upgrade-collection-body {
        padding-top: 10px;
        .change-area {
            padding: 0 30px;
        }
        .change-under {
            padding-top: 20px;
        }
    }
    >>>.bk-dialog-body {
        min-height: 420px;
    }
    >>>.bk-table-expanded-cell {
        padding: 0!important;
    }
    input[type="radio"] {
        position: relative;
        display: inline-block;
        vertical-align: middle;
        width: 16px;
        height: 16px;
        margin: 0 5px 0 0;
        border: 1px solid #979ba5;
        border-radius: 50%;
        background-color: #fff;
        background-clip: content-box;
        outline: none;
        color: #fff;
        visibility: visible;
        cursor: pointer;
        -webkit-appearance: none;
    }
    input[type="radio"]:checked {
        padding: 3px;
        color: #3a84ff;
        border-color: currentColor;
        background-color: currentColor;
    }
    >>>.bk-form-content {
        width: 472px;
        line-height: 30px;
    }
    >>>.bk-table-empty-text {
        padding: 0;
    }
    .container-left {
        max-width: 300px;
        padding-top: 5px;
        overflow: hidden;
        white-space: nowrap;
        text-overflow: ellipsis;
    }
    >>>.bk-form-textarea {
        background-color: #f6f9fa;
    }
    .breadcrumb {
        background: transparent!important;
        .tool-list-tab {
            overflow: auto;
        }
        >>>.bk-tab.bk-tab-unborder-card {
            .bk-tab-header {
                background-image: none;
                font-weight: 700;
            }
            .bk-tab-section {
                display: none;
            }
            .bk-tab-label-wrapper .bk-tab-label-list {
                .bk-tab-label-item {
                    &.active {
                        background: transparent;
                    }
                }
            }
        }
    }
    .choose-collection-table {
        >>>.bk-form-radio {
            font-size: 12px;
        }
    }
    .no-task {
        padding-top: 150px;
    }
</style>
