<template>
    <div class="main-content-inner main-content-form" :class="editDisabled ? 'from-pipeline' : ''">
        <div v-if="editDisabled" :model="formData">
            <div class="to-pipeline">
                <span v-if="taskDetail.createFrom === 'gongfeng_scan'">{{$t('基础信息配置由CodeCC开源扫描集群自动生成')}}</span>
                <span v-else>{{$t('修改基础信息配置，请前往流水线')}} <a @click="hanldeToPipeline" href="javascript:;">{{$t('立即前往>>')}}</a></span>
            </div>
            <div class="disf">
                <span class="pipeline-label disf">{{$t('任务名称')}}</span>
                <span class="fs14">{{ formData.nameCn }}</span>
            </div>
            <div class="disf">
                <span class="pipeline-label disf">{{$t('任务语言')}}</span>
                <span :class="index === 0 ? '' : 'lang'" class="fs14" v-for="(lang, index) in formatLang(taskDetail.codeLang)" :key="lang">{{ lang }}</span>
            </div>
            <bk-form :label-width="130">
                <div v-for="toolParam in toolConfigParams" :key="toolParam.name">
                    <tool-params-form
                        v-for="param in toolParam.paramsList"
                        :key="param.key"
                        :param="param"
                        :tool="toolParam.name">
                    </tool-params-form>
                </div>
            </bk-form>
            <tool-config-form
                class="form-edit"
                scenes="manage-edit"
                :code-message="codeMessage"
                :tools="configData"
                :code-lang="formData.codeLang"
                @saveBasic="submitData"
                @handleFactorChange="handleFactorChange">
            </tool-config-form>
        </div>
        <div v-else>
            <bk-form :label-width="130" :model="formData" ref="basicInfo">
                <bk-form-item :label="$t('任务名称')" :required="true" :rules="formRules.nameCn" property="nameCn">
                    <bk-input v-model.trim="formData.nameCn"></bk-input>
                </bk-form-item>
                <!-- <bk-form-item :label="$t('英文ID')" property="taskDetail.nameEn">
                    <bk-input v-model="formData.nameEn" readonly></bk-input>
                </bk-form-item> -->
                <bk-form-item :label="$t('任务语言')" class="pb15" :required="true" :rules="formRules.codeLang" property="codeLang">
                    <bk-checkbox-group v-model="formData.codeLang" @change="handleLangChange" class="checkbox-lang">
                        <bk-checkbox v-for="lang in toolMeta.LANG" :key="lang.key" :value="parseInt(lang.key)" class="item fs12">{{lang.fullName}}</bk-checkbox>
                    </bk-checkbox-group>
                </bk-form-item>
                <bk-form :label-width="130">
                    <div v-for="toolParam in toolConfigParams" :key="toolParam.name">
                        <tool-params-form
                            v-for="param in toolParam.paramsList"
                            :key="param.key"
                            :param="param"
                            :tool="toolParam.name">
                        </tool-params-form>
                    </div>
                </bk-form>
                <bk-form-item label-width="0" class="cc-code-message">
                    <tool-config-form
                        class="form-edit"
                        scenes="manage-edit"
                        :code-message="codeMessage"
                        :tools="configData"
                        :code-lang="formData.codeLang"
                        :is-tool-manage="taskDetail.createFrom === 'bs_pipeline'"
                        @saveBasic="submitData"
                        @handleFactorChange="handleFactorChange">
                    </tool-config-form>
                </bk-form-item>
            </bk-form>
        </div>
        
    </div>
</template>

<script>
    import { mapState } from 'vuex'
    import ToolConfigForm from '@/components/tool-config-form'
    import ToolParamsForm from '@/components/tool-params-form'

    export default {
        components: {
            ToolConfigForm,
            ToolParamsForm
        },
        data () {
            return {
                formData: {},
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
                        },
                        {
                            max: 50,
                            message: this.$t('不能多于x个字符', { num: 50 }),
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
                paramsValue: {},
                buttonLoading: false,
                codeMessage: {}
            }
        },
        computed: {
            ...mapState([
                'toolMeta'
            ]),
            ...mapState('tool', {
                toolMap: 'mapList'
            }),
            ...mapState('task', {
                taskDetail: 'detail'
            }),
            configData () {
                const configData = []
                for (const i in this.taskDetail.enableToolList) {
                    configData.push(this.taskDetail.enableToolList[i].toolName)
                }
                return configData
            },
            toolConfigParams () {
                const toolConfigParams = []
                const toolParamList = ['PYLINT', 'GOML']
                for (const key in this.toolMap) {
                    const toolParams = this.taskDetail.enableToolList.find(item => item.toolName === key)
                    if (toolParamList.includes(key) && toolParams) {
                        try {
                            const paramJson = toolParams.paramJson && JSON.parse(toolParams.paramJson)
                            let paramsList = this.toolMap[key] && JSON.parse(this.toolMap[key]['params'])
                            paramsList = paramsList.map(item => {
                                const varName = item.varName
                                if (paramJson && paramJson[varName]) {
                                    item['varDefault'] = paramJson[varName]
                                }
                                return item
                            })
                            this.toolMap[key]['paramsList'] = paramsList
                            toolConfigParams.push(this.toolMap[key])
                        } catch (error) {
                            console.error(error)
                        }
                    }
                }
                return toolConfigParams
            },
            editDisabled () {
                // 最老的v1插件atomCode为空，但createFrom === 'bs_pipeline', 也可编辑
                return (this.taskDetail.atomCode && this.taskDetail.createFrom === 'bs_pipeline') || this.taskDetail.createFrom === 'gongfeng_scan'
            }
        },
        watch: {},
        created () {
            this.init()
        },
        methods: {
            formatLang (num) {
                return this.toolMeta.LANG.map(lang => lang.key & num ? lang.name : '').filter(name => name)
            },
            async init () {
                const params = { taskId: this.$route.params.taskId, showLoading: true }
                const res = await this.$store.dispatch('task/basicInfo', params)
                if (!this.toolMeta.LANG.length) {
                    const res = await this.$store.dispatch('getToolMeta')
                    this.LANG = res.LANG
                }
                res.codeLang = this.toolMeta.LANG.map(lang => lang.key & res.codeLang).filter(lang => lang > 0)
                this.formData = res
                this.codeMessage = await this.$store.dispatch('task/getCodeMessage')
            },
            handleLangChange (newValue) {
                if (!newValue) {
                    newValue = this.formData.codeLang
                }
                const formItem = this.$refs.basicInfo.formItems[1]
                if (newValue.length) {
                    formItem.clearValidator()
                } else {
                    const validator = formItem.validator
                    const msg = this.$t('请选择至少一种任务语言')
                    setTimeout(function () {
                        validator.state = 'error'
                        validator.content = msg
                    }, 100)
                }
            },
            submitData () {
                this.handleLangChange()
                const formItems = this.$refs.basicInfo.formItems
                const devopsToolParams = this.getParamsValue()
                let hasError = false
                for (let index = 0; index < formItems.length; index++) {
                    if (formItems[index].validator && formItems[index].validator.state === 'error') hasError = true
                }

                if (!hasError) {
                    this.buttonLoading = true
                    this.$refs.basicInfo.validate().then(validator => {
                        const params = {
                            taskId: this.$route.params.taskId,
                            nameCn: this.formData.nameCn,
                            codeLang: String(this.formData.codeLang.reduce((n1, n2) => n1 + n2, 0)),
                            devopsToolParams
                        }
                        this.$store.dispatch('task/updateBasicInfo', params).then(res => {
                            if (res === true) {
                                this.$bkMessage({
                                    theme: 'success',
                                    message: this.$t('修改成功')
                                })
                            }
                        }).catch(e => {
                            console.error(e)
                        }).finally(() => {
                            this.buttonLoading = false
                        })
                    }, validator => {
                        // console.log(validator)
                    })
                }
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
            },
            hanldeToPipeline () {
                window.open(`${window.DEVOPS_SITE_URL}/console/pipeline/${this.taskDetail.projectId}/${this.taskDetail.pipelineId}/edit#${this.taskDetail.atomCode}`, '_blank')
            }
        }
    }
</script>

<style lang="postcss" scoped>
    >>>.bk-form-input[readonly] {
        border: none 0;
        background: transparent!important;
    }
    >>>.bk-form .bk-form-content {
        width: 620px;
    }
    .checkbox-lang {
        .item {
            width: 155px;
            line-height: 30px;
        }
    }
    .bk-form-checkbox.fs12 {
        >>>.bk-checkbox-text {
            font-size: 12px;
        }
    }
    .from-pipeline {
        padding: 0px 35px 0px 20px;
        .to-pipeline {
            font-size: 12px;
            border-bottom: 1px solid #e3e3e3;
            margin-bottom: 20px;
            height: 32px;
            a {
                margin-left: 12px;
            }
        }
        .pipeline-label {
            display: inline-block;
            width: 104px;
            text-align: left;
            font-size: 14px;
            /* line-height: 14px; */
            height: 46px;
            font-weight: 600;
        }
        .lang::before {
            content: ' | ';
            /* width: 30px; */
            height: 12px;
            color: #d8d8d8;
            right: 0;
            top: 12px;
            padding: 0px 10px;
        }
    }
    >>>.bk-form-label {
        text-align: left;
    }
    >>>.cc-code-message > .bk-label {
        line-height: 0;
        min-height: 0;
    }
</style>
