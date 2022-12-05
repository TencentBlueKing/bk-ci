<template>
    <section>
        <div class="bk-form bk-form-vertical">
            <div v-if="isThirdParty && container.baseOS === 'LINUX'" class="slave-tips">
                <p class="slave-tips-title">如果非root账号启动agent, 请使用root帐号登录构建机运行以下命令：</p>
                <p class="code-backgroud">mkdir -p /data/codecc_software<br>mount -t nfs -o {{ CODECC_SOFWARE_URL }}:/data/codecc_software /data/codecc_software</p>
            </div>
            <template v-for="(obj, key) in commonModel[&quot;row&quot;]">
                <form-field v-if="!obj.hidden" :key="key" :desc="obj.desc" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                    <component :is="obj.component" v-validate.initial="Object.assign(obj.rule, { required: obj.required })" :name="key" :handle-change="key === &quot;tools&quot; ? handleChooseTools : (key === &quot;languages&quot; ? handleChooseLang : handleUpdateElement)" :value="element[key]" v-bind="obj"></component>
                </form-field>
            </template>
            <template v-for="prop in accordionList">
                <accordion show-checkbox show-content :key="prop.id" v-if="!newModel[prop.id].hidden || (showScript && prop.id === 'script')">
                    <header class="var-header" slot="header">
                        <span>{{ getPropName(prop.name) }}</span>
                        <i class="devops-icon icon-angle-down" style="display:block"></i>
                    </header>
                    <div slot="content" class="bk-form bk-form-vertical">
                        <form-field v-for="key of prop.item" v-if="!newModel[key].hidden" :key="key" :desc="newModel[key].desc" :required="newModel[key].required" :label="newModel[key].label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                            <component :is="newModel[key].component"
                                v-validate.initial="Object.assign({}, newModel[key].rule, { required: newModel[key].required })"
                                :lang="lang"
                                :name="key"
                                :handle-change="handleUpdateElement"
                                :task="task"
                                :task-id="taskId"
                                :task-name="taskName"
                                :turbo-value="banAllBooster"
                                :project-id="projectId"
                                :element-id="elementId"
                                :disabled="inputDisabled"
                                :turbo-disabled="turboDisabled"
                                @handleChange="handleUpdateTurbo"
                                :value="element[key]"
                                v-bind="newModel[key]">
                            </component>
                        </form-field>
                    </div>
                </accordion>
            </template>
            <accordion show-checkbox show-content key="otherChoice">
                <header class="var-header" slot="header">
                    <span>其它选项</span>
                    <i class="devops-icon icon-angle-down" style="display:block"></i>
                </header>
                <div slot="content" class="bk-form bk-form-vertical">
                    <template v-for="key in otherChoice">
                        <form-field v-if="!newModel[key].hidden" :key="key" :desc="newModel[key].desc" :required="newModel[key].required" :label="newModel[key].label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                            <component :is="newModel[key].component" v-validate.initial="Object.assign({}, newModel[key].rule, { required: newModel[key].required })" :name="key" :handle-change="handleUpdateElement" :value="element[key]" v-bind="newModel[key]"></component>
                        </form-field>
                    </template>
                </div>
            </accordion>
        </div>
    </section>
</template>

<script>
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'
    import { mapActions, mapGetters, mapState } from 'vuex'
    export default {
        name: 'codecc',
        mixins: [atomMixin, validMixins],
        data () {
            return {
                newModel: {},
                task: {},
                elementId: '',
                banAllBooster: false,
                taskId: '',
                taskName: '',
                isShow: false,
                isLoading: false,
                inputDisabled: false,
                turboDisabled: false,
                btnDisabled: false,
                baseOSType: '',
                disAllowToolsInThird: ['CHECKSTYLE', 'STYLECOP', 'SENSITIVE', 'PHPCS', 'DETEKT', 'OCCHECK'],
                dataMap: [
                    {
                        old: 'C',
                        new: 'C_CPP'
                    },
                    {
                        old: 'C_PLUS_PLUSH',
                        new: 'C_CPP'
                    },
                    {
                        old: 'OBJECTIVE_C',
                        new: 'OC'
                    },
                    {
                        old: 'JAVASCRIPT',
                        new: 'JS'
                    }
                ],
                otherChoice: ['path', 'asynchronous']
            }
        },
        computed: {
            ...mapGetters('atom', [
                'checkPipelineInvalid',
                'getEditingElementPos'
            ]),
            ...mapState('atom', [
                'pipeline'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            commonModel () {
                const { languages, tools } = this.newModel
                return {
                    row: { languages, tools }
                }
            },
            accordionList () {
                return [
                    {
                        id: 'script',
                        name: 'Coverity',
                        item: ['scanType', 'scriptType', 'script', 'scriptTurbo', 'coverityToolSetId', 'klocworkToolSetId', 'pinpointToolSetId']
                    },
                    {
                        id: 'cpplintToolSetId',
                        name: 'CppLint',
                        item: ['cpplintToolSetId']
                    },
                    {
                        id: 'checkStyleToolSetId',
                        name: 'Checkstyle',
                        item: ['checkStyleToolSetId']
                    },
                    {
                        id: 'eslintRc',
                        name: 'ESLint',
                        item: ['eslintRc', 'eslintToolSetId']
                    },
                    {
                        id: 'styleCopToolSetId',
                        name: 'StyleCop',
                        item: ['styleCopToolSetId']
                    },
                    {
                        id: 'goPath',
                        name: 'Gometalinter',
                        item: ['goPath', 'gometalinterToolSetId']
                    },
                    {
                        id: 'detektToolSetId',
                        name: 'detekt',
                        item: ['detektToolSetId']
                    },
                    {
                        id: 'phpcsStandard',
                        name: 'PHPCS',
                        item: ['phpcsStandard', 'phpcsToolSetId']
                    },
                    {
                        id: 'pyVersion',
                        name: 'PyLint',
                        item: ['pyVersion', 'pylintToolSetId']
                    },
                    {
                        id: 'occheckToolSetId',
                        name: 'OCCheck',
                        item: ['occheckToolSetId']
                    },
                    {
                        id: 'horuspyToolSetId',
                        name: '荷鲁斯高危组件',
                        item: ['horuspyToolSetId']
                    },
                    {
                        id: 'woodpeckerToolSetId',
                        name: '啄木鸟敏感信息',
                        item: ['woodpeckerToolSetId']
                    },
                    {
                        id: 'ripsToolSetId',
                        name: 'RIPS',
                        item: ['ripsToolSetId']
                    },
                    {
                        id: 'sensitiveToolSetId',
                        name: '敏感信息',
                        item: ['sensitiveToolSetId']
                    },
                    {
                        id: 'ccnThreshold',
                        name: '圈复杂度',
                        item: ['ccnThreshold']
                    }
                ]
            },
            langList () {
                return this.newModel.scriptType.list
            },
            lang () {
                const lang = this.langList.find(stype => stype.value === this.element.scriptType)
                return lang ? lang.id : ''
            },
            langMap () { // 获取语言是否非编译Map
                return this.newModel.languages.list.reduce((langMap, l) => {
                    langMap[l.id] = l.compile
                    return langMap
                }, {})
            },
            hasCompileLang () { // 当前选中语言是否包含编译型语言
                const { langMap, element } = this
                return element.languages.some(lang => langMap[lang])
            },
            showScript () {
                const showScript = this.element.tools.some(tool => (tool === 'COVERITY' || tool === 'KLOCWORK' || tool === 'PINPOINT'))
                return showScript
            },
            isMacos () {
                // 是否是macos构建环境
                return this.container && this.container.baseOS === 'MACOS'
            }
        },
        watch: {
            'element.eslintRc' (newVal) {
                this.newModel.eslintToolSetId.preFilter = { key: 'eslintRc', value: newVal }
                this.handleUpdateElement('eslintToolSetId', '')
            },
            'element.phpcsStandard' (newVal) {
                this.newModel.phpcsToolSetId.preFilter = { key: 'phpcsStandard', value: newVal }
                this.handleUpdateElement('phpcsToolSetId', '')
            },
            'element.languages' (newVal) {
                if (newVal.length && this.element.tools.includes('COVERITY')) {
                    this.newModel.coverityToolSetId.preFilter = { key: 'codeLangs', value: newVal }
                } else if (newVal.length && this.element.tools.includes('KLOCWORK')) {
                    this.newModel.klocworkToolSetId.preFilter = { key: 'codeLangs', value: newVal }
                } else if (newVal.length && this.element.tools.includes('PINPOINT')) {
                    this.newModel.pinpointToolSetId.preFilter = { key: 'codeLangs', value: newVal }
                }
            }
        },

        created () {
            this.elementId = this.element.id || false
            this.newModel = JSON.parse(JSON.stringify(this.atomPropsModel))
            console.log(this.element)

            // this.newModel = this.atomPropsModel
            if (this.element.tools) {
                this.handleChooseTools('tools', this.element.tools)
            } else {
                this.handleChooseTools('tools', ['COVERITY'])
            }
            if (this.element.scanType === undefined) {
                this.handleUpdateElement('scanType', '1')
            }
            if (this.element.languages && this.element.languages.length) {
                this.element.languages.forEach((lang, index) => {
                    if (this.dataMap.filter(item => item.old === lang).length > 0) {
                        this.element.languages.splice(index, 1, this.getDataMap(lang))
                    }
                })
                const list = this.element.languages
                this.element.languages = Array.from(new Set(list))
                this.handleChooseLang('languages', this.element.languages)
            }
            // if (this.container && this.container.baseOS === 'WINDOWS' && this.isThirdParty) {
            //     this.handleStrictLang()
            // }
            if (this.container && this.container.baseOS === 'WINDOWS') {
                // this.handleStrictLang()
                this.newModel.scriptType.list = [{ label: 'BAT', value: 'BAT', id: 'batchfile' }]
                this.handleUpdateElement('scriptType', 'BAT')
                if (this.element.script === '# Coverity/Klocwork将通过调用编译脚本来编译您的代码，以追踪深层次的缺陷\n# 请使用依赖的构建工具如maven/cmake等写一个编译脚本build.sh\n# 确保build.sh能够编译代码\n# cd path/to/build.sh\n# sh build.sh') {
                    this.handleUpdateElement('script', 'REM Coverity/Klocwork将通过调用编译脚本来编译您的代码，以追踪深层次的缺陷\nREM 请使用依赖的构建工具如maven/cmake等写一个编译脚本build.sh\nREM 确保build.sh能够编译代码\nREM cd path/to/build.sh\nREM sh build.sh')
                }
            }
            this.baseOSType = this.container.baseOS
            if (this.baseOSType === 'LINUX') {
                this.initData()
            }
        },
        destroyed () {
            this.handleUpdateElement('compilePlat', this.container.baseOS)
        },
        methods: {
            ...mapActions('pipelines', [
                'requestTurboIofo',
                'setTurboSwitch',
                'updateToTurbo'
            ]),
            ...mapActions('atom', [
                'setPipeline'
            ]),
            handleChooseLang (name, value) {
                this.handleUpdateElement(name, value)
                this.handleIntro(value)
                this.handleTurbo()
                if (this.showScript && this.hasCompileLang) {
                    this.newModel.script.hidden = false
                    this.newModel.scriptType.hidden = false
                    this.newModel.scanType.hidden = false
                    this.newModel.scriptTurbo.hidden = false
                } else if (!this.showScript) {
                    this.newModel.script.hidden = true
                    this.newModel.scriptType.hidden = true
                    this.newModel.scanType.hidden = true
                    this.newModel.scriptTurbo.hidden = true
                }
            },
            handleChooseTools (name, value) {
                this.handleUpdateElement(name, value)
                this.handleTurbo()
                if (value.filter(item => item === 'ESLINT').length > 0) {
                    this.newModel.eslintRc.hidden = false
                    this.newModel.eslintToolSetId.hidden = false
                    this.newModel.eslintToolSetId.preFilter = { key: 'eslintRc', value: this.element.eslintRc }
                } else {
                    this.newModel.eslintRc.hidden = true
                    this.newModel.eslintToolSetId.hidden = true
                }
                if (value.filter(item => item === 'PYLINT').length > 0) {
                    this.newModel.pyVersion.hidden = false
                    this.newModel.pylintToolSetId.hidden = false
                } else {
                    this.newModel.pyVersion.hidden = true
                    this.newModel.pylintToolSetId.hidden = true
                }
                if (value.filter(item => item === 'PHPCS').length > 0) {
                    this.newModel.phpcsStandard.hidden = false
                    this.newModel.phpcsToolSetId.hidden = false
                    this.newModel.phpcsToolSetId.preFilter = { key: 'phpcsStandard', value: this.element.phpcsStandard }
                } else {
                    this.newModel.phpcsStandard.hidden = true
                    this.newModel.phpcsToolSetId.hidden = false
                }
                if (value.filter(item => item === 'GOML').length > 0) {
                    this.newModel.gometalinterToolSetId.hidden = false
                } else {
                    this.newModel.gometalinterToolSetId.hidden = true
                }
                if (value.filter(item => item === 'GOML').length > 0 || value.filter(item => item === 'GOCILINT').length > 0) {
                    this.newModel.goPath.hidden = false
                } else {
                    this.newModel.goPath.hidden = true
                }
                if (value.filter(item => item === 'CCN').length > 0) {
                    this.newModel.ccnThreshold.hidden = false
                } else {
                    this.newModel.ccnThreshold.hidden = true
                }
                if (value.filter(item => item === 'COVERITY').length > 0) {
                    this.newModel.scanType.hidden = false
                } else {
                    this.newModel.scanType.hidden = true
                }
                if (value.filter(item => item === 'CPPLINT').length > 0) {
                    this.newModel.cpplintToolSetId.hidden = false
                } else {
                    this.newModel.cpplintToolSetId.hidden = true
                }
                if (value.filter(item => item === 'CHECKSTYLE').length > 0) {
                    this.newModel.checkStyleToolSetId.hidden = false
                } else {
                    this.newModel.checkStyleToolSetId.hidden = true
                }
                if (value.filter(item => item === 'STYLECOP').length > 0) {
                    this.newModel.styleCopToolSetId.hidden = false
                } else {
                    this.newModel.styleCopToolSetId.hidden = true
                }
                if (value.filter(item => item === 'DETEKT').length > 0) {
                    this.newModel.detektToolSetId.hidden = false
                } else {
                    this.newModel.detektToolSetId.hidden = true
                }
                if (value.filter(item => item === 'HORUSPY').length > 0) {
                    this.newModel.horuspyToolSetId.hidden = false
                } else {
                    this.newModel.horuspyToolSetId.hidden = true
                }
                if (value.filter(item => item === 'WOODPECKER_SENSITIVE').length > 0) {
                    this.newModel.woodpeckerToolSetId.hidden = false
                } else {
                    this.newModel.woodpeckerToolSetId.hidden = true
                }
                if (value.filter(item => item === 'RIPS').length > 0) {
                    this.newModel.ripsToolSetId.hidden = false
                } else {
                    this.newModel.ripsToolSetId.hidden = true
                }
                if (value.filter(item => item === 'SENSITIVE').length > 0) {
                    this.newModel.sensitiveToolSetId.hidden = false
                } else {
                    this.newModel.sensitiveToolSetId.hidden = true
                }
                if (value.filter(item => item === 'OCCHECK').length > 0) {
                    this.newModel.occheckToolSetId.hidden = false
                } else {
                    this.newModel.occheckToolSetId.hidden = true
                }
                if (this.showScript) {
                    if (this.hasCompileLang) {
                        this.newModel.scanType.hidden = !this.element.tools.includes('COVERITY')
                        this.newModel.script.hidden = false
                        this.newModel.scriptType.hidden = false
                        this.newModel.scriptTurbo.hidden = false
                    } else {
                        this.newModel.scanType.hidden = true
                        this.newModel.script.hidden = true
                        this.newModel.scriptType.hidden = true
                        this.newModel.scriptTurbo.hidden = true
                    }

                    if (this.element.tools.includes('COVERITY')) {
                        this.newModel.coverityToolSetId.hidden = false
                        this.newModel.coverityToolSetId.preFilter = { key: 'codeLangs', value: this.element.languages }
                    } else {
                        this.newModel.coverityToolSetId.hidden = true
                    }
                    if (this.element.tools.includes('KLOCWORK')) {
                        this.newModel.klocworkToolSetId.hidden = false
                        this.newModel.klocworkToolSetId.preFilter = { key: 'codeLangs', value: this.element.languages }
                    } else {
                        this.newModel.klocworkToolSetId.hidden = true
                    }
                    if (this.element.tools.includes('PINPOINT')) {
                        this.newModel.pinpointToolSetId.hidden = false
                        this.newModel.pinpointToolSetId.preFilter = { key: 'codeLangs', value: this.element.languages }
                    } else {
                        this.newModel.pinpointToolSetId.hidden = true
                    }
                } else {
                    this.newModel.script.hidden = true
                    this.newModel.scanType.hidden = true
                    this.newModel.scriptType.hidden = true
                    this.newModel.scriptTurbo.hidden = true
                    this.newModel.pinpointToolSetId.hidden = true
                    this.newModel.klocworkToolSetId.hidden = true
                    this.newModel.coverityToolSetId.hidden = true
                }
            },
            handleIntro (vals) {
                let list = []
                vals.forEach(val => {
                    const curLang = this.newModel.languages.list.find(item => item.id === val)
                    if (curLang && curLang.intro) {
                        list = list.concat(curLang.intro)
                    }
                    list = Array.from(new Set(list))
                })
                this.newModel.tools.list.forEach((tool, index) => {
                    if (list.filter(item => tool.id === item).length > 0) {
                        this.newModel.tools.list[index].disabled = false
                    } else {
                        this.newModel.tools.list[index].disabled = true
                        const newTools = this.element.tools.filter(item => tool.id !== item)
                        this.handleChooseTools('tools', newTools)
                    }
                    // mac只允许使用coverity
                    if (this.isMacos && this.isThirdParty && this.newModel.tools.list[index].id !== 'COVERITY') {
                        this.newModel.tools.list[index].disabled = true
                    }
                    // 第三方机不允许使用某些工具
                    if (this.isThirdParty && this.container.baseOS !== 'LINUX' && this.disAllowToolsInThird.includes(this.newModel.tools.list[index].id)) {
                        this.newModel.tools.list[index].disabled = true
                    }
                    // windows只允许使用coverity和klcowork
                    if (this.container.baseOS === 'WINDOWS' && this.newModel.tools.list[index].id !== 'COVERITY' && this.newModel.tools.list[index].id !== 'KLOCWORK') {
                        this.newModel.tools.list[index].disabled = true
                    }
                })
            },
            handleTurbo () { // 限制c++&Coverity可以配置编译加速
                const lang = this.element.languages
                const tools = this.element.tools
                if (lang.includes('C_CPP') && tools.includes('COVERITY')) {
                    this.turboDisabled = false
                    this.newModel.scriptTurbo.desc = '一个编译脚本只能有一个编译加速任务'
                } else {
                    this.turboDisabled = true
                    this.newModel.scriptTurbo.desc = '目前只支持C/C++语言和Coverity工具'
                }
            },
            getDataMap (val) {
                const item = this.dataMap.find(item => item.old === val)
                if (item && item.new) {
                    return item.new
                } else {
                    return ''
                }
            },
            getPropName (name) {
                if (name === 'Coverity') {
                    const nameList = []
                    if (this.element.tools.filter(tool => tool === 'COVERITY').length) {
                        nameList.push('Coverity')
                    }
                    if (this.element.tools.filter(tool => tool === 'KLOCWORK').length) {
                        nameList.push('Klocwork')
                    }
                    if (this.element.tools.filter(tool => tool === 'PINPOINT').length) {
                        nameList.push('Pinpoint')
                    }
                    name = nameList.join('、')
                }
                if (name === 'Gometalinter') {
                    if (this.element.tools.filter(tool => tool === 'GOML').length && this.element.tools.filter(tool => tool === 'GOCILINT').length) {
                        name = 'Gometalinter、GolangCI-Lint'
                    }
                    if (!this.element.tools.filter(tool => tool === 'GOML').length && this.element.tools.filter(tool => tool === 'GOCILINT').length) {
                        name = 'GolangCI-Lint'
                    }
                }
                return name
            },
            handleStrictLang () {
                this.newModel.languages.list.forEach((lang, index) => {
                    this.newModel.languages.list[index].disabled = true
                })
            },
            async handleUpdateTurbo (name, value) {
                if (this.elementId && this.taskId) {
                    const { checkPipelineInvalid, $route: { params }, pipeline } = this
                    const { inValid, message } = checkPipelineInvalid(pipeline.stages)
                    try {
                        if (inValid) {
                            throw new Error(message)
                        }
                        const { data } = await this.$ajax.put(`/process/api/user/pipelines/${params.projectId}/${params.pipelineId}`, pipeline)
                        if (data) {
                            this.updatePipelineToTurbo(pipeline)
                            this.turboDisabled = true
                            try {
                                const { taskId } = this
                                const res = await this.setTurboSwitch({
                                    banAllBooster: !value,
                                    taskId
                                })
                                if (res) {
                                    this.banAllBooster = value
                                }
                            } catch (err) {
                                this.$bkMessage({
                                    message: err.message ? err.message : err,
                                    theme: 'error'
                                })
                            } finally {
                                this.turboDisabled = false
                            }
                        } else {
                            this.$showTips({
                                message: `${pipeline.name}修改失败`,
                                theme: 'error'
                            })
                        }
                    } catch (e) {
                        if (e.code === 403) { // 没有权限编辑
                            this.setPermissionConfig(this.$permissionResourceMap.pipeline, this.$permissionActionMap.edit, [{
                                id: this.pipeline.pipelineId,
                                name: this.pipeline.name
                            }], params.projectId, this.getPermUrlByRole(params.projectId, this.pipeline.pipelineId, this.roleMap.manager))
                        } else {
                            this.$showTips({
                                message: e.message,
                                theme: 'error'
                            })
                        }
                    }
                } else if (this.elementId && !this.taskId) {
                    this.isLoading = true
                    try {
                        const res = await this.requestTurboIofo({
                            bsPipelineId: this.$route.params.pipelineId,
                            bsElementId: this.elementId
                        })
                        if (res) {
                            res.data.banAllBooster === 'false' ? (this.banAllBooster = true) : (this.banAllBooster = false)
                            this.task = Object.assign({}, res.data)
                            this.taskId = res.data.taskId
                            this.taskName = res.data.taskName
                            if (!this.taskId) {
                                this.banAllBooster = value
                                this.$bkInfo({
                                    subTitle: '暂无编译加速任务，点击去新建任务',
                                    closeIcon: false,
                                    confirmFn: this.goRegist,
                                    cancelFn: () => {
                                        this.banAllBooster = false
                                    }
                                })
                            }
                        }
                    } catch (err) {
                        this.$bkMessage({
                            message: err.message ? err.message : err,
                            theme: 'error'
                        })
                    } finally {
                        this.isLoading = false
                    }
                } else {
                    this.$bkInfo({
                        subTitle: '暂无编译加速任务，点击去新建任务',
                        closeIcon: false,
                        confirmFn: this.goRegist,
                        cancelFn: () => {
                            this.banAllBooster = false
                        }
                    })
                }
            },
            async initData () {
                try {
                    const res = await this.requestTurboIofo({
                        bsPipelineId: this.$route.params.pipelineId,
                        bsElementId: this.elementId
                    })
                    if (res) {
                        res.data.banAllBooster === 'false' ? (this.banAllBooster = true) : (this.banAllBooster = false)
                        this.task = Object.assign({}, res.data)
                        this.taskId = res.data.taskId
                        this.taskName = res.data.taskName
                    }
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                }
            },
            dialogHide () {
                this.isShow = false
            },
            // 新建编译加速任务跳转
            async goRegist () {
                const { checkPipelineInvalid, $route: { params }, pipeline } = this
                const { inValid, message } = checkPipelineInvalid(pipeline.stages)
                this.btnDisabled = true
                this.handleUpdateElement('compilePlat', this.container.baseOS)
                const tab = window.open('about:blank')
                try {
                    if (inValid) {
                        throw new Error(message)
                    }
                    const { data } = await this.$ajax.put(`/process/api/user/pipelines/${params.projectId}/${params.pipelineId}`, pipeline)
                    if (data) {
                        // this.requestPipeline(this.$route.params)
                        const response = await this.$ajax.get(`/process/api/user/pipelines/${params.projectId}/${params.pipelineId}`)
                        this.setPipeline(response.data)
                        this.updatePipelineToTurbo(response.data)
                        const { containerIndex, elementIndex, stageIndex } = this.getEditingElementPos
                        const container = response.data.stages[stageIndex]
                        this.elementId = container.containers[containerIndex].elements[elementIndex].id

                        tab.location = `${WEB_URL_PREFIX}/turbo/${this.projectId}/registration#${this.$route.params.pipelineId}&${this.elementId}`
                    } else {
                        this.$showTips({
                            message: `${pipeline.name}修改失败`,
                            theme: 'error'
                        })
                        tab.close()
                    }
                } catch (e) {
                    if (e.code === 403) { // 没有权限编辑
                        this.setPermissionConfig(this.$permissionResourceMap.pipeline, this.$permissionActionMap.edit, [{
                            id: this.pipeline.pipelineId,
                            name: this.pipeline.name
                        }], params.projectId, this.getPermUrlByRole(params.projectId, this.pipeline.pipelineId, this.roleMap.manager))
                    } else {
                        this.$showTips({
                            message: e.message,
                            theme: 'error'
                        })
                    }
                    tab.close()
                } finally {
                    this.btnDisabled = false
                }
            },
            updatePipelineToTurbo (pipeline) {
                try {
                    this.updateToTurbo({
                        pipelineId: this.$route.params.pipelineId,
                        params: pipeline
                    })
                } catch (e) {
                    this.$showTips({
                        message: e.message || e,
                        theme: 'error'
                    })
                }
            }
        }
    }
</script>

<style lang="scss">
    .slave-tips {
        margin: 20px 0;
        .slave-tips-title {
            margin-bottom: 5px;
        }
        .code-backgroud {
            font-weight: bold;
            color: #c7c7c7;
            background: #373636;
            border-radius: 5px;
            padding: 10px;
            word-break: break-word;
        }
    }
    .dialog-regist {
        position: relative;
        .regist-content {
            padding: 45px 65px 15px 65px;
        }
        .regist-footer {
            text-align: center;
            padding: 20px 65px 40px;
            font-size: 0;
            .bk-button {
                width: 110px;
                height: 36px;
                font-size: 14px;
                border: 1px solid #c3cdd7;
                border-radius: 2px;
                box-shadow: none;
                outline: none;
                background-color: #fff;
                text-overflow: ellipsis;
                overflow: hidden;
                white-space: nowrap;
                cursor: pointer;
                &.bk-primary {
                    margin-right: 20px;
                    color: #fff;
                    background-color: #3c96ff;
                    border-color: #3c96ff;
                }
            }
        }
    }
</style>
