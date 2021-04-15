<!-- deprecated -->
<template>
    <div>
        <bk-tab :active.sync="active" type="unborder-card">
            <bk-tab-panel
                v-for="(panel, index) in panels"
                v-bind="panel"
                :key="index"
            >
                <div class="tool-added" v-if="index === 0">
                    <div class="tool-list">
                        <div class="tool-list-item fl">
                            <div class="mb20">
                                <span class="pr10">{{$t('已接入')}}</span>
                                <bk-button theme="primary"
                                    :disabled="!pickedEnableList.length || pickedEnableList.length >= toolEnableList.length || taskStatus.gongfengProjectId"
                                    @click="disableDialog.visiable = true">
                                    {{$t('停用')}}
                                </bk-button>
                            </div>
                            <div v-for="(tool, toolIndex) in toolEnableList" :key="toolIndex" class="tool-item">
                                <tool-card :tool="tool" :picked="pickedEnableList.includes(tool.name || tool.toolName)" :source="'enable'" @click="handleToolCardClick"></tool-card>
                            </div>
                        </div>
                        <div v-if="toolDisableList.length" class="tool-list-item fl mt20">
                            <div class="mb20">
                                <span class="pr10">{{$t('已停用')}}</span>
                                <bk-button theme="primary"
                                    :disabled="!pickedDisableList.length"
                                    @click="changeToolStatus">
                                    {{$t('启用')}}
                                </bk-button>
                            </div>
                            <div v-for="(tool, toolIndex) in toolDisableList" :key="toolIndex" class="tool-item">
                                <tool-card :tool="tool" :picked="pickedDisableList.includes(tool.name || tool.toolName)" :source="'disable'" @click="handleToolCardClick"></tool-card>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="tool-rest" v-if="index === 1">
                    <div class="tool-list" v-show="currentStep === 1">
                        <div class="mb20">
                            <span class="pr10">{{$t('待添加')}}</span>
                            <bk-button theme="primary"
                                :disabled="!pickedRestList.length || taskStatus.gongfengProjectId"
                                @click="handleAddTool">
                                {{$t('添加')}}
                            </bk-button>
                        </div>
                        <div v-for="(tool, toolIndex) in toolRestList" :key="toolIndex" class="tool-item">
                            <tool-card :tool="tool" @click="handleToolCardClick"></tool-card>
                        </div>
                    </div>
                </div>
            </bk-tab-panel>
        </bk-tab>
        <bk-dialog
            width="480"
            header-position="left"
            v-model="disableDialog.visiable"
            :title="$t('停用x工具', { tool: currentToolDisplayName })"
            :mask-close="false"
            @confirm="confirm"
            @value-change="disableDialogVisiableChange"
        >
            <div class="disable-alert"><i class="bk-icon icon-info-circle alert-icon"></i>{{$t('停用后该工具的数据将被保留，功能将被停止。您也可以重新启用该工具。')}}</div>
            <bk-input
                :placeholder="$t('请输入停用原因，至少10个字符…')"
                :type="'textarea'"
                :rows="3"
                :maxlength="100"
                v-model="disableReason"
            >
            </bk-input>
            <div slot="footer">
                <bk-button type="button" :disabled="disableReason.length < 10" :loading="disableDialog.loading" theme="primary" @click.native="toolDisableConfirm">
                    {{$t('确定')}}
                </bk-button>
                <bk-button type="button" :disabled="disableDialog.loading" @click.native="toolDisableCancel">
                    {{$t('取消')}}
                </bk-button>
            </div>
        </bk-dialog>
        <tool-config-form
            v-show="currentStep === 2 && active === 'tool-more'"
            class="form-add"
            scenes="manage-add"
            is-tool-manage="true"
            :tools="pickedRestList"
            :code-message="codeMessage"
            :success="toolAddSuccess"
            @update="updateTaskTool"
            @handlePrev="currentStep = 1"
        />
        <tool-params-side-form
            ref="toolParamsSideForm"
            v-show="Object.keys(toolConfigParams).length && currentStep !== 2"
            @handleFactorChange="handleFactorChange"
            :has-save="active === 'tool-added'"
            :tools-num="toolsNum"
            :tool-config-params="toolConfigParams">
        </tool-params-side-form>
    </div>
</template>

<script>
    import { mapState } from 'vuex'
    import ToolCard from '@/components/tool-card'
    import ToolConfigForm from '@/components/tool-config-form'
    import ToolParamsSideForm from '@/components/tool-params-side-form'

    export default {
        components: {
            ToolCard,
            ToolConfigForm,
            ToolParamsSideForm
        },
        data () {
            return {
                panels: [
                    { name: 'tool-added', label: this.$t('已有工具') },
                    { name: 'tool-more', label: this.$t('更多工具') }
                ],
                active: this.$route.query.add === '1' ? 'tool-more' : 'tool-added',
                disableDialog: {
                    visiable: false,
                    loading: false
                },
                currentToolName: undefined,
                disableReason: '',
                checkerSets: [],
                ruleSetFactor: {},
                pickedEnableList: [],
                pickedDisableList: [],
                pickedRestList: [],
                currentStep: 1,
                relateCheckerSetTools: []
            }
        },
        computed: {
            ...mapState('task', {
                taskDetail: 'detail',
                codeMessage: 'codes',
                taskStatus: 'status'
            }),
            ...mapState('tool', {
                toolMap: 'mapList',
                toolSimpleMap: 'mapSimpleList'
            }),
            taskId () {
                return this.$route.params.taskId
            },
            toolEnableList () {
                return this.taskDetail.enableToolList
            },
            toolDisableList () {
                return this.taskDetail.disableToolList
            },
            toolAddedList () {
                const { enableToolList, disableToolList } = this.taskDetail
                const toolAddedList = enableToolList.concat(disableToolList)

                return toolAddedList
            },
            toolRestList () {
                const { toolMap, toolSimpleMap } = this
                const toolRestList = []
                const toolAddedList = this.toolAddedList
                Object.keys(toolMap).map(key => {
                    const tool = toolMap[key]
                    if (toolSimpleMap[key] && toolSimpleMap[key].recommend && !toolAddedList.find(item => item.toolName === tool.name)) {
                        toolRestList.push(tool)
                    }
                })

                return toolRestList
            },
            currentToolDisplayName () {
                const toolNames = this.pickedEnableList.map(tool => {
                    const toolName = this.toolMap[tool].displayName
                    return this.$t(`${toolName}`)
                })
                return toolNames.join('、')
            },
            toolConfigParams () {
                const { toolMap, active, toolEnableList, toolRestList, pickedRestList } = this
                const toolConfigParams = []
                let tools = []
                if (active === 'tool-added') {
                    tools = toolEnableList
                } else {
                    tools = toolRestList.filter(item => pickedRestList.includes(item.name))
                }
                tools.map(toolItem => {
                    const toolName = toolItem.toolName || toolItem.name
                    if (!toolName || !toolMap[toolName]) return
                    const checkerSets = this.checkerSets.find(item => item.toolName === toolName)
                    const { displayName, params } = toolMap[toolName]
                    let paramsList = []
                    if (params) {
                        try {
                            paramsList = JSON.parse(params)
                        } catch (error) {
                            console.error(error)
                        }
                    }
                    const toolParams = this.taskDetail.enableToolList.find(item => item.toolName === toolName)
                    let paramJson = ''
                    let checkerSetId = ''
                    if (active === 'tool-added' && toolParams) {
                        paramJson = toolParams.paramJson && JSON.parse(toolParams.paramJson)
                        checkerSetId = toolParams.checkerSet && toolParams.checkerSet.checkerSetId
                        paramsList = paramsList.map(item => {
                            const varName = item.varName
                            if (paramJson && paramJson[varName]) {
                                item['varDefault'] = paramJson[varName]
                            }
                            return item
                        })
                    }
                    const checkerSetsList = []
                    const setNameMap = {
                        myProjUse: '我创建的/我的任务正在使用',
                        recommended: 'CodeCC推荐',
                        others: '更多公开规则集'
                    }
                    for (const key in checkerSets) {
                        const item = {}
                        item.id = key
                        item.name = setNameMap[key]
                        
                        if (item.name) {
                            item.children = checkerSets[key].filter(checkerSet => {
                                if (this.ruleSetFactor[toolName] && checkerSet.paramJson) { // 切换影响规则集的参数
                                    return this.ruleSetFactor[toolName] === checkerSet.paramJson
                                }
                                if (checkerSet.paramJson && this.relateCheckerSetTools.includes(toolName)) { // 筛选规则集参数
                                    const paramSelect = {}
                                    const paramSelectKey = paramsList[0] && paramsList[0]['varName']
                                    paramSelect[paramSelectKey] = paramsList[0] && paramsList[0]['varDefault']
                                    return checkerSet.paramJson === JSON.stringify(paramSelect)
                                }
                                return true
                            })
                            checkerSetsList.push(item)
                        }
                        if (key === 'recommended') {
                            if (!checkerSetId || this.ruleSetFactor[toolName]) {
                                checkerSetId = item.children[0] && item.children[0]['checkerSetId']
                            }
                        }
                    }
                    const item = { toolName, displayName, paramsList, checkerSetsList, checkerSetId }
                    if (paramsList.length || checkerSetsList.length) toolConfigParams.push(item)
                })
                return toolConfigParams
            },
            toolsNum () {
                const { active, toolEnableList, pickedRestList } = this
                return active === 'tool-added' ? toolEnableList.length : pickedRestList.length
            }
        },
        created () {
            this.init()
        },
        methods: {
            async init () {
                this.$store.dispatch('task/getRelateCheckerSetTools').then(res => {
                    this.relateCheckerSetTools = res.data.paramJsonRelateCheckerSetTools
                })
                await this.$store.dispatch('tool/updated', { showLoading: true })
                await this.$store.dispatch('task/getCodeMessage')
                this.$store.dispatch('tool/checker', { toolNames: Object.keys(this.toolSimpleMap), taskId: this.taskId }).then(res => {
                    this.checkerSets = res.checkerSets
                })
            },
            toolDisableConfirm () {
                this.changeToolStatus(false)
            },
            toolDisableCancel () {
                this.disableDialog.visiable = false
            },
            changeToolStatus (enabled) {
                const data = {
                    toolNameList: enabled ? this.pickedDisableList : this.pickedEnableList,
                    stopReason: enabled ? '' : this.disableReason,
                    manageType: enabled ? 'Y' : 'N'
                }
                this.$store.dispatch('task/changeToolStatus', data).then(res => {
                    if (res.code === '0') {
                        this.disableDialog.visiable = false
                        this.updateTaskTool()
                    }
                }).catch(e => {
                    console.error(e)
                }).finally(() => {
                    this.pickedDisableList = []
                    this.pickedEnableList = []
                })
            },
            disableDialogVisiableChange (visiable) {
                // 如果是关闭dialog则迫使拉取数据以恢复停用开关按钮状态
                if (!visiable) {
                    this.updateTaskTool()
                }
                this.disableReason = ''
            },
            toolAddSuccess () {
                this.$router.push({ name: 'task-detail' })
            },
            updateTaskTool () {
                // 重新获取任务详情，以更新视图，如左侧导航
                this.$store.dispatch('task/detail', { showLoading: true })
                this.$store.dispatch('task/getRepoList', { projCode: this.$route.params.projectId })
            },
            handleFactorChange (factor, toolName) {
                if (toolName === 'ESLINT' || toolName === 'PHPCS') {
                    const item = {}
                    item[toolName] = JSON.stringify(factor)
                    this.ruleSetFactor = Object.assign({}, item)
                }
            },
            handleToolCardClick (e, { name, source }) {
                const sourceMap = {
                    'new': this.pickedRestList,
                    'enable': this.pickedEnableList,
                    'disable': this.pickedDisableList
                }
                const nameIndex = sourceMap[source].indexOf(name)
                if (nameIndex !== -1) {
                    sourceMap[source].splice(nameIndex, 1)
                } else {
                    sourceMap[source].push(name)
                }
                if (source === 'enable' && this.pickedEnableList.length === this.toolEnableList.length) {
                    this.$bkMessage({ theme: 'error', message: this.$t('不能停用所有工具') })
                }
            },
            handleAddTool () {
                const { toolAddedList, pickedRestList, codeMessage } = this
                const addedCK = toolAddedList.find(item => item.toolName === 'COVERITY' || item.toolName === 'KLOCWORK' || item.toolName === 'PINPOINT')
                const pickedCK = pickedRestList.includes('COVERITY') || pickedRestList.includes('KLOCWORK') || pickedRestList.includes('PINPOINT')
                if (!addedCK && pickedCK) {
                    this.currentStep = 2
                } else {
                    const pickedTools = this.pickedRestList.map(toolName => {
                        return {
                            toolName,
                            taskId: this.taskId
                        }
                    })
                    let tools = this.$refs.toolParamsSideForm.getParamsValue()
                    tools = Object.assign(pickedTools, tools)
                    const obj = {}
                    tools = tools.reduce(function (item, next) { // 去重
                        if (!obj[next.toolName]) {
                            obj[next.toolName] = true
                            item.push(next)
                        }
                        return item
                    }, [])
                    const postData = { ...codeMessage, tools }
                    this.$store.dispatch('task/addTool', postData).then(res => {
                        if (res.data === true) {
                            this.$router.push({ name: 'task-detail' })
                        }
                    }).catch(e => {
                        console.error(e)
                    }).finally(() => {
                        this.updateTaskTool()
                    })
                }
            }
        }
    }
</script>

<style lang="postcss" scoped>
    @import '../../css/mixins.css';

    .tool-list {
        @mixin clearfix;
        .tool-list-item {
            width: 100%;
        }
        .tool-item {
            float: left;
            margin-right: 16px;
            margin-bottom: 16px;
        }
    }

    .sideslider-content {
        padding: 20px;
        .form-add {
            width: 80%;
        }

        .sideslider-header-extra {
            position: absolute;
            right: 16px;
            top: 0;
            height: 60px;
            line-height: 60px;
            font-size: 12px;
        }
    }

    .disable-alert {
        position: relative;
        background: #f0f1f5;
        border-radius:2px;
        border:1px solid #f0f1f5;
        padding: 12px 20px 12px 48px;
        font-size: 12px;
        color: #63656e;
        margin-bottom: 12px;

        .alert-icon {
            position: absolute;
            left: 16px;
            top: 22px;
            color: #a3c5fd;
            font-size: 18px;
        }
    }
    .bk-sideslider-wrapper {
        overflow: hidden;
    }
    >>>.form-add {
        .bk-form-item {
            width: 50%;
            min-width: 560px;
        }
    }
</style>
