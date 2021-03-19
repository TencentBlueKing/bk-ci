<template>
    <div class="main-content-outer">
        <div class="new-task">
            <div class="new-task-main">
                <div class="main-top">{{$t('接入任务')}}</div>
                <h2 class="main-title"></h2>
                <div class="new-task-content">
                    <div class="step-basic">
                        <bk-form class="pb20" :label-width="130" :model="formData" ref="basicForm">
                            <bk-form-item :label="$t('任务名称')" :required="true" :rules="formRules.nameCn" property="nameCn">
                                <bk-input v-model.trim="formData.nameCn"></bk-input>
                            </bk-form-item>
                            <bk-form-item :label="$t('任务语言')" :required="true" property="codeLang" :rules="formRules.codeLang">
                                <bk-checkbox-group v-model="formData.codeLang" @change="handleLangChange" class="checkbox-lang">
                                    <bk-checkbox v-for="lang in toolMeta.LANG" :key="lang.key" :value="parseInt(lang.key, 10)" class="item fs12">{{lang.fullName}}</bk-checkbox>
                                </bk-checkbox-group>
                            </bk-form-item>
                            <bk-form-item :label="$t('规则集')" :required="true" style="height: 50px;" v-if="Object.keys(checkersetMap).length">
                                <span class="select-tool cc-ellipsis" :title="toolCnList.join('、')" v-if="toolCnList.length">{{$t('涉及工具')}} {{toolCnList.join('、')}}</span>
                            </bk-form-item>
                            <checkerset-select
                                ref="checkerSetList"
                                :data="checkersetMap"
                                @handleToolChange="handleToolChange">
                            </checkerset-select>
                        </bk-form>
                        <bk-form :label-width="130">
                            <div v-for="toolParam in toolConfigParams" class="pb20" :key="toolParam.name">
                                <tool-params-form
                                    v-for="param in toolParam.paramsList"
                                    :key="param.key"
                                    :param="param"
                                    :tool="toolParam.name"
                                    @handleFactorChange="handleFactorChange">
                                </tool-params-form>
                            </div>
                        </bk-form>
                        <tool-config-form
                            class="form-add"
                            scenes="register-add"
                            :tools="toolList"
                            :code-lang="formData.codeLang"
                            ref="toolConfigForm"
                        />
                    </div>
                </div>

                <div class="new-task-footer">
                    <bk-button theme="primary" :loading="buttonLoading" @click="handleCompletelClick">{{$t('完成')}}</bk-button>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    import { mapState } from 'vuex'
    import ToolConfigForm from '@/components/tool-config-form'
    import ToolParamsForm from '@/components/tool-params-form'
    import checkersetSelect from '@/components/checkerset-select'

    export default {
        components: {
            ToolConfigForm,
            ToolParamsForm,
            checkersetSelect
        },
        data () {
            return {
                formRules: {
                    nameCn: [
                        {
                            required: true,
                            message: this.$t('必填项'),
                            trigger: 'blur'
                        },
                        {
                            regex: /^[\u4e00-\u9fa5_a-zA-Z0-9]+$/,
                            message: this.$t('需由中文、字母、数字或下划线组成'),
                            trigger: 'blur'
                        }
                    ],
                    codeLang: [
                        {
                            required: true,
                            message: this.$t('必填项'),
                            trigger: 'change'
                        }
                    ]
                },
                formData: {
                    nameCn: '',
                    codeLang: []
                },
                toolList: [],
                checkerset: {},
                checkersetMap: {},
                formValidator: {},
                buttonLoading: false,
                paramsValue: {}
            }
        },
        computed: {
            ...mapState([
                'toolMeta'
            ]),
            ...mapState('tool', {
                toolMap: 'mapList'
            }),
            projectId () {
                return this.$route.params.projectId
            },
            toolConfigParams () {
                const toolConfigParams = []
                const toolParamList = ['PYLINT', 'GOML']
                for (const key in this.toolMap) {
                    if (toolParamList.includes(key) && this.toolList.includes(key)) {
                        try {
                            this.toolMap[key]['paramsList'] = this.toolMap[key] && JSON.parse(this.toolMap[key]['params'])
                            toolConfigParams.push(this.toolMap[key])
                        } catch (error) {
                            console.error(error)
                        }
                    }
                }
                return toolConfigParams
            },
            toolCnList () {
                return this.toolList.map(item => this.toolMap[item] && this.toolMap[item]['displayName'])
            }
        },
        created () {
            this.init()
        },
        methods: {
            init () {
                this.$store.dispatch('checkerset/params')
                this.$store.dispatch('checkerset/count', { projectId: this.projectId })
                this.$store.commit('task/updateDetail', {})
                this.$store.dispatch('checkerset/categoryList').then(res => {
                    this.checkerset = res
                })
            },
            handleLangChange (newValue) {
                const formItem = this.$refs.basicForm.formItems[1]
                if (!newValue) {
                    newValue = this.formData.codeLang
                }
                const validator = { state: newValue.length > 0, content: this.$t('请选择至少一种任务语言'), formItem, field: 'codeLang' }
                this.hintFormItem(validator)

                const codeLang = this.formData.codeLang
                const checkersetMap = {}
                codeLang.map(item => {
                    const name = this.toolMeta.LANG.find(lang => Number(lang.key) === item).name
                    const checkerset = { ...this.checkerset }
                    for (const key in checkerset) {
                        checkerset[key] = checkerset[key].filter(checker => checker.codeLang & item)
                    }
                    const list = checkerset
                    checkersetMap[name] = list
                })
                this.$refs.checkerSetList.handleChange()
                this.checkersetMap = checkersetMap
            },
            hintFormItem (validator) {
                const { state, formItem, field, content } = validator
                this.formValidator[field] = validator
                if (state) {
                    formItem.clearValidator()
                } else {
                    const validator = formItem.validator
                    setTimeout(function () {
                        validator.state = 'error'
                        validator.content = content
                    }, 100)
                }
            },
            handleToolChange (toolList) {
                this.toolList = toolList
            },
            handleCompletelClick () {
                if (!this.$refs.checkerSetList.handleValidate()) return false // 规则集验证
                this.handleLangChange()
                this.$refs.basicForm.validate().then(validator => {
                    let hasError = false
                    Object.keys(this.formValidator).forEach(key => {
                        const validator = this.formValidator[key]
                        if (!validator.state) {
                            this.hintFormItem(validator)
                            hasError = true
                        }
                    })
                    if (hasError) {
                        return false
                    } else {
                        this.$refs.toolConfigForm.$refs.codeForm.validate().then(validator => {
                            this.submitData()
                        }, validator => {
                            return false
                        })
                    }
                }, validator => {
                    return false
                })
            },
            submitData () {
                const codeData = this.$refs.toolConfigForm.getSubmitData()
                const checkerSetList = this.$refs.checkerSetList.getCheckerset()
                const devopsToolParams = this.getParamsValue()
                const codeLang = String(this.formData.codeLang.reduce((n1, n2) => n1 + n2, 0))
                const postData = { ...this.formData, ...codeData, checkerSetList, codeLang, devopsToolParams }
                this.buttonLoading = true
                this.$store.dispatch('task/addTool', postData).then(res => {
                    // 成功则进入一下步
                    this.$router.push({
                        name: 'task-detail',
                        params: { ...this.$route.params, taskId: res.data.taskId }
                    })
                }).catch(e => {
                    console.error(e)
                }).finally(() => {
                    this.buttonLoading = false
                })
            },
            handleFactorChange (factor, toolName) {
                this.paramsValue[toolName] = Object.assign({}, this.paramsValue[toolName], factor)
            },
            getParamsValue () {
                const tools = this.toolConfigParams.map(item => {
                    const { name, paramsList } = item
                    const paramObj = {}
                    let varName = ''
                    let chooseValue = ''
                    paramsList.forEach(param => {
                        const key = param.varName
                        varName = param.varName
                        paramObj[key] = (this.paramsValue[name] && this.paramsValue[name][key]) || param.varDefault
                        chooseValue = paramObj[key]
                    })
                    const paramJson = Object.keys(paramObj).length ? JSON.stringify(paramObj) : ''
                    return { toolName: name, paramJson, varName, chooseValue }
                })
                return tools
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .main-top {
        background: #fafbfd;
        border-bottom: 1px solid #dcdee5;
        height: 56px;
        line-height: 56px;
        padding: 0 30px;
    }
    .new-task-main {
        --asideHorizontalPadding: 32px;
        display: block;
        background:#fff;
        border-radius:2px;
        box-shadow: 0 2px 12px 0 hsla(0, 0%, 87%, 0.5), 0 2px 13px 0 hsla(0, 0%, 87%, 0.5);
        height: calc(100% - 140px);
        min-height: 600px;
        margin-bottom: 60px;

        .new-task-aside {
            flex: none;
            background:#fafbfd;
            padding: 28px var(--asideHorizontalPadding);
            border-right: 1px solid #dcdee5;
        }
        
        .new-task-content {
            margin: auto;
            padding: 12px;
            overflow: hidden;
            width: 800px;
        }
    }
    .step-basic {
        .bk-form {
            width: 90%;
        }
    }
    .checkbox-lang {
        .item {
            width: 140px;
            line-height: 30px;
        }
    }
    .bk-form-checkbox.fs12 {
        >>>.bk-checkbox-text {
            font-size: 12px;
        }
    }
    .new-task-footer {
        text-align: center;
        margin-top: 20px;
        padding-bottom: 20px;
    }
    >>> .bk-form .bk-label {
        font-weight: bold;
    }
    .select-tool {
        font-size: 12px;
        color: #999;
        line-height: 31px;
        max-width: 568px;
        display: inline-block;
    }
</style>
