<template>
    <div class="main-content-outer">
        <h2 class="main-title">{{$t('new.新增代码检查任务')}}</h2>
        <div class="new-task">
            <div class="new-task-main">
                <div class="new-task-aside">
                    <bk-steps :steps="newTaskSteps" :cur-step="currentStep + 1"></bk-steps>
                </div>
                <div class="new-task-content">
                    <div class="step-basic" v-if="step === 'basic'">
                        <bk-form :label-width="120" v-if="formData" :model="formData" ref="basicForm">
                            <bk-form-item :label="$t('basic.英文名称')" :required="true" :rules="formRules.nameEn" property="nameEn">
                                <bk-input v-model.trim="formData.nameEn" :readonly="isEdit" @blur="handleNameEnBlur"></bk-input>
                            </bk-form-item>
                            <bk-form-item :label="$t('basic.中文名称')" :required="true" :rules="formRules.nameCn" property="nameCn">
                                <bk-input v-model.trim="formData.nameCn"></bk-input>
                            </bk-form-item>
                            <bk-form-item :label="$t('basic.任务语言')" :required="true" property="codeLang" :rules="formRules.codeLang">
                                <bk-checkbox-group v-model="formData.codeLang" @change="handleLangChange" class="checkbox-lang">
                                    <bk-checkbox v-for="lang in toolMeta.LANG" :key="lang.key" :value="parseInt(lang.key, 10)" class="item">{{lang.name}}</bk-checkbox>
                                </bk-checkbox-group>
                            </bk-form-item>
                        </bk-form>
                    </div>
                    <div class="step-tools" v-else-if="step === 'tools'">
                        <dl class="tool-list">
                            <template v-for="(toolType, typeIndex) in toolMeta.TOOL_TYPE">
                                <div v-if="toolListGroupByType[toolType.key]" :key="typeIndex" class="tool-list-group">
                                    <dt class="group-title">{{toolType.fullName}}</dt>
                                    <dd v-for="(tool, toolIndex) in toolListGroupByType[toolType.key]" :key="toolIndex" class="tool-item">
                                        <tool-card :tool="tool" :picked="formData.tools.includes(tool.name)" @click="handleToolCardClick"></tool-card>
                                    </dd>
                                </div>
                            </template>
                        </dl>
                    </div>
                    <div class="step-code" v-else-if="step === 'code'">
                        <tool-config-form
                            class="form-add"
                            scenes="register-add"
                            :tools="formData.tools"
                            ref="toolConfigForm"
                        />
                    </div>
                    <div class="step-finish" v-else-if="step === 'finish'">
                        <div class="result result-success">
                            <i class="bk-icon icon-check-circle result-icon success-icon"></i>
                            <p class="result-text">{{$t('new.恭喜，你已创建完成')}}</p>
                            <bk-button theme="primary" type="button" @click="() => $router.push({ name: 'task-detail' })">{{$t('new.查看任务详情')}}</bk-button>
                        </div>
                    </div>
                </div>
            </div>
            <div class="new-task-footer" v-show="currentStep < newTaskSteps.length - 1">
                <bk-button type="button" @click="handleCancelClick">{{$t('op.取消')}}</bk-button>
                <bk-button theme="primary" type="submit" :loading="buttonLoading.register" @click="handleRegisterAndNextClick" v-show="currentStep === 0">{{ isEdit ? $t("op.保存") : $t("op.注册")}}{{$t("op.并下一步")}}</bk-button>
                <bk-button theme="primary" type="button" @click="handlePrevlClick" v-show="currentStep > 0">{{$t('op.上一步')}}</bk-button>
                <bk-button theme="primary" type="button" @click="handleNextlClick" :disabled="step === 'tools' && !formData.tools.length" v-show="currentStep > 0 && currentStep < newTaskSteps.length - 2">{{$t('op.下一步')}}</bk-button>
                <bk-button theme="primary" type="button" :loading="buttonLoading.complete" @click="handleCompletelClick" v-show="currentStep === newTaskSteps.length - 2">{{$t("op.保存")}}</bk-button>
            </div>
        </div>
    </div>
</template>

<script>
    import { mapState } from 'vuex'
    import ToolCard from '@/components/tool-card'
    import ToolConfigForm from '@/components/tool-config-form'

    export default {
        components: {
            ToolCard,
            ToolConfigForm
        },
        data () {
            return {
                newTaskSteps: [
                    { name: 'basic', title: this.$t('nav.基本信息'), icon: 1 },
                    { name: 'tools', title: this.$t('nav.添加工具'), icon: 2 },
                    { name: 'code', title: this.$t('nav.配置代码库'), icon: 3 },
                    { name: 'finish', title: this.$t('nav.完成'), icon: 4 }
                ],
                step: this.$route.query.step || 'basic',
                formRules: {
                    nameEn: [
                        {
                            required: true,
                            message: this.$t('st.必填项'),
                            trigger: 'blur'
                        },
                        {
                            regex: /^\w+$/,
                            message: this.$t('st.需由字母、数字或下划线组成'),
                            trigger: 'blur'
                        }
                    ],
                    nameCn: [
                        {
                            required: true,
                            message: this.$t('st.必填项'),
                            trigger: 'blur'
                        },
                        {
                            regex: /^[\u4e00-\u9fa5_a-zA-Z0-9]+$/,
                            message: this.$t('st.需由中文、字母、数字或下划线组成'),
                            trigger: 'blur'
                        }
                    ],
                    codeLang: [
                        {
                            required: true,
                            message: this.$t('st.必填项'),
                            trigger: 'change'
                        }
                    ]
                },
                formData: {
                    nameEn: '',
                    nameCn: '',
                    codeLang: [],
                    tools: []
                },
                formValidator: {},
                buttonLoading: {
                    register: false,
                    complete: false
                }
            }
        },
        computed: {
            ...mapState([
                'toolMeta',
                'taskId',
                'constants'
            ]),
            ...mapState('tool', {
                toolMap: 'mapList'
            }),
            ...mapState('task', {
                taskDetail: 'detail'
            }),
            isEdit () {
                return this.$route.params.taskId > 0
            },
            currentStep () {
                let stepIndex = this.newTaskSteps.findIndex(item => item.name === this.step)
                stepIndex = stepIndex === -1 ? 0 : stepIndex
                return stepIndex
            },
            toolListGroupByType () {
                const { toolMap } = this
                const list = {}

                // 组织成以type为键的结构
                Object.keys(toolMap).forEach(key => {
                    const tool = toolMap[key]
                    const type = tool.type
                    if (list[type]) {
                        list[type].push(tool)
                    } else {
                        list[type] = [tool]
                    }
                })

                return list
            }
        },
        beforeRouteUpdate (to, from, next) {
            this.step = to.query.step || 'basic'
            next()
        },
        methods: {
            fetchPageData () {
                if (this.step === 'basic' && this.$route.params.taskId) {
                    const { nameEn, nameCn, codeLang } = this.taskDetail
                    let initData = {}
                    if (this.isEdit) {
                        initData = {
                            nameEn,
                            nameCn,
                            codeLang: this.toolMeta.LANG.map(lang => lang.key & codeLang).filter(lang => lang > 0)
                        }
                    }
                    this.formData = Object.assign({}, this.formData, initData)
                } else if (this.step === 'tools') {
                    this.updateList()
                }
            },
            async updateList () {
                await this.$store.dispatch('tool/updated', { showLoading: true })
            },
            handleCancelClick () {
                this.$router.push({ name: 'task-list' })
            },
            handlePrevlClick () {
                if (this.currentStep === 1) {
                    this.$router.push({
                        name: 'task-new',
                        params: this.$route.params
                    })
                } else {
                    this.step = this.newTaskSteps[this.currentStep - 1].name
                }
            },
            handleNextlClick () {
                this.step = this.newTaskSteps[this.currentStep + 1].name
            },
            handleRegisterAndNextClick () {
                this.$refs.basicForm.validate().then(validator => {
                    // 触发任务语言是否选择验证
                    this.handleLangChange()

                    // 检查是否有验证错误
                    let hasError = false
                    Object.keys(this.formValidator).forEach(key => {
                        const validator = this.formValidator[key]
                        if (!validator.state) {
                            this.hintFormItem(validator)
                            hasError = true
                        }
                    })

                    if (!hasError) {
                        this.buttonLoading.register = true
                        const { formData } = this
                        const data = {
                            taskId: this.$route.params.taskId,
                            nameEn: formData.nameEn,
                            nameCn: formData.nameCn,
                            codeLang: String(formData.codeLang.reduce((n1, n2) => n1 + n2, 0))
                        }

                        const action = this.isEdit ? 'task/update' : 'task/create'
                        this.$store.dispatch(action, data).then(res => {
                            // 成功则进入一下步
                            // this.handleNextlClick()
                            this.$router.push({
                                name: 'task-new',
                                params: { taskId: res.taskId || data.taskId },
                                query: { step: 'tools' }
                            })
                        }).catch(e => {
                            console.error(e)
                        }).finally(() => {
                            this.buttonLoading.register = false
                        })
                    }
                }, validator => {
                    console.log(validator)
                })
            },
            handleCompletelClick () {
                const toolConfigForm = this.$refs.toolConfigForm
                toolConfigForm.$refs.codeForm.validate().then(validator => {
                    const data = toolConfigForm.getSubmitData()

                    this.buttonLoading.complete = true
                    this.$store.dispatch('task/addTool', data).then(res => {
                        // 成功则进入一下步
                        this.handleNextlClick()
                    }).catch(e => {
                        console.error(e)
                    }).finally(() => {
                        this.buttonLoading.complete = false
                    })
                }, validator => {
                    console.log(validator)
                })
            },
            async handleNameEnBlur (value, event) {
                if (value.length && this.formRules.nameEn[1].regex.test(value) && !this.isEdit) {
                    try {
                        const res = await this.$store.dispatch('task/checkname', { nameEn: value })
                        const formItem = this.$refs.basicForm.formItems[0]
                        const validator = { state: res.data === false, content: this.$t('st.该英文ID已存在'), formItem, field: 'nameEn' }
                        this.hintFormItem(validator)
                    } catch (e) {
                        console.error(e)
                    }
                }
            },
            async handleNameCnBlur (value, event) {
                if (value.length && this.formRules.nameCn[1].regex.test(value)) {
                    try {
                        const res = await this.$store.dispatch('task/checkname', { 'nameCn': value })
                        const formItem = this.$refs.basicForm.formItems[1]
                        const validator = { state: res.code === 0, content: this.$t('st.该任务名已存在'), formItem, field: 'nameCn' }
                        this.hintFormItem(validator)
                    } catch (e) {
                        console.error(e)
                    }
                }
            },
            handleLangChange (newValue) {
                const formItem = this.$refs.basicForm.formItems[2]
                if (!newValue) {
                    newValue = this.formData.codeLang
                }
                const validator = { state: newValue.length > 0, content: this.$t('st.请选择至少一种任务语言'), formItem, field: 'codeLang' }
                this.hintFormItem(validator)
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
            handleToolCardClick (e, { name }) {
                const nameIndex = this.formData.tools.indexOf(name)
                if (nameIndex !== -1) {
                    this.formData.tools.splice(nameIndex, 1)
                } else {
                    this.formData.tools.push(name)
                }
            }
        }
    }
</script>

<style lang="postcss" scoped>
    @import '../../css/mixins.css';

    .new-task-main {
        --asideHorizontalPadding: 32px;
        display: flex;
        background:#fff;
        box-shadow:0px 2px 4px 0px rgba(0, 0, 0, 0.05);
        border-radius:2px;
        border:1px solid #dcdee5;

        .new-task-aside {
            flex: none;
            background:#fafbfd;
            padding: 28px var(--asideHorizontalPadding);
            border-right: 1px solid #dcdee5;
        }
        .new-task-content {
            flex: 1;
            margin: 28px 0;
            padding: 12px;
            height: 520px;
            overflow: auto;
        }
    }

    .bk-steps {
        flex-direction: column;
        height: 360px;
        >>>.bk-step {
            &:after {
                content: "";
                position: absolute;
                left: 17px;
                top: 0;
                height: 100%;
                width: auto;
                border: 1px dashed #dcdee5;
                background: none;
            }
            &.done:after {
                border-color: #3a84ff;
            }
            &.current {
                position: relative;
                &:before {
                    content: '';
                    position: absolute;
                    width: 10px;
                    height: 10px;
                    border: 1px solid #dcdee5;
                    background-color: white;
                    right: calc(-7px - var(--asideHorizontalPadding));
                    top: 13px;
                    transform: rotate(45deg);
                    border-right-color: transparent;
                    border-top-color: transparent;
                }
            }
            .bk-step-title {
                text-align: left;
            }
            .bk-step-indicator {
                float: left;
                margin-right: 12px;
            }
            .bk-step-title {
                overflow: hidden;
            }
        }
    }

    .new-task-footer {
        text-align: right;
        margin-top: 20px;
    }

    .checkbox-lang {
        .item {
            width: 112px;
            margin-bottom: 16px;
        }
    }

    .step-basic,
    .step-code {
        .bk-form {
            width: 55%;
        }
    }
    .step-tools {
        padding: 0 24px;
    }

    .tool-list {
        @mixin clearfix;
        .tool-list-group {
            float: left;
            @mixin clearfix;
            .group-title {
                font-size: 14px;
                color: #979ba5;
                font-weight: bold;
                margin-bottom: 8px;
            }

            .tool-item {
                float: left;
                margin-right: 16px;
                margin-bottom: 16px;
            }
        }
    }

    .step-finish {
        display: flex;
        align-items: center;
        height: 100%;

        .result {
            text-align: center;
            flex: 1;
            margin-top: -120px;

            .result-text {
                color: #4a4a4a;
                font-size: 18px;
                margin: 18px 0;
            }

            .result-icon {
                font-size: 54px;
            }
        }

        .result-success {
            .success-icon {
                color: #00c873;
            }
        }
    }
</style>
