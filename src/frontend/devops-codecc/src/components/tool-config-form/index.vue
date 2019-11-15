<template>
    <bk-form :class="scenes === 'manage-edit' ? 'edit' : ''" :label-width="120" :model="formData" ref="codeForm">
        <div v-if="scenes === 'manage-edit'" class="edit-title">{{$t('repo.代码仓库')}}</div>
        <bk-form-item :label="this.$t('repo.代码仓库')" :required="true" :rules="formRules.repositoryHashId" property="repositoryHashId" class="form-item-repo">
            <bk-select
                v-model="formData.repositoryHashId"
                @toggle="handleRepoSelectToggle"
                @change="handleRepoSelectChange"
                :disabled="isToolManage"
                searchable
            >
                <bk-option
                    v-for="(option, index) in repoList"
                    :key="index"
                    :id="option.repoHashId"
                    :name="option.aliasName">
                </bk-option>
            </bk-select>
            <a :href="codeUrl" target="_blank" class="repo-add-link">{{$t('op.新增')}} <i class="bk-icon icon-edit"></i></a>
        </bk-form-item>
        <bk-form-item :label="$t('repo.分支')" :required="true" property="branch" :rules="formRules.branch" v-if="isGitRepo">
            <bk-input :disabled="isToolManage" v-model.trim="formData.branch"></bk-input>
            <!-- <bk-select 
                v-model="formData.branch"
                searchable
                @change="handleBranchSelectChange"
            >
                <bk-option
                    v-for="(value, index) in branchList"
                    :key="index"
                    :id="index"
                    :name="value">
                </bk-option>
            </bk-select> -->
        </bk-form-item>
        <bk-form-item>
        </bk-form-item>
        <div v-if="scenes === 'manage-edit'" class="edit-title">{{$t('repo.其他信息')}}</div>
        <div v-if="scenes !== 'manage-edit'">
            <template v-for="(params, toolName) in toolParams">
                <bk-form-item
                    v-for="param in params"
                    :label="param.labelName"
                    :property="param.varName"
                    :key="param.key"
                    class="left"
                >
                    <bk-input
                        v-bk-tooltips="{
                            content: param.varTips,
                            width: isToolManage ? 400 : 300,
                            placement: isToolManage ? 'bottom' : 'right'
                        }"
                        v-model.trim="formData.toolParams[toolName][param.varName]"
                        v-if="param.varType === 'STRING'"
                    ></bk-input>
                    <bk-radio-group v-model="formData.toolParams[toolName][param.varName]" v-if="param.varType === 'BOOLEAN'" class="radio-param">
                        <bk-radio :value="true" class="item">{{$t('repo.是')}}</bk-radio>
                        <bk-radio :value="false" class="item">{{$t('repo.否')}}</bk-radio>
                    </bk-radio-group>
                    <bk-radio-group v-if="param.varType === 'RADIO'" v-model="formData.toolParams[toolName][param.varName]" class="radio-param">
                        <bk-radio :title="option.name" @change="getLang(option, param)" v-for="(option, index) in param.varOptionList" :value="option.id" :key="index" class="item">{{option.name}}</bk-radio>
                    </bk-radio-group>
                    <bk-checkbox-group v-model="formData.toolParams[toolName][param.varName]" v-if="param.varType === 'CHECKBOX'" class="checkbox-param">
                        <bk-checkbox v-for="(option, index) in param.varOptionList" :value="option.id" :key="index" class="item">{{option.name}}</bk-checkbox>
                    </bk-checkbox-group>
                    <div v-if="param.varType === 'TEXTAREA'">
                        <Ace
                            class="ace-wrapper"
                            :read-only="disabled"
                            :value="value"
                            :lang="lang"
                            :name="name"
                            v-model="formData.toolParams[toolName][param.varName]"
                            @input="handleScriptInput"
                            height="300"
                            width="100%">
                        </Ace>
                    </div>
                </bk-form-item>
            </template>
        </div>
        <div :class="isToolManage ? 'active' : ''" v-else>
            <template>
                <bk-form-item
                    v-for="(param, key) in formData.toolConfigList"
                    :label="param.labelName"
                    :rules="param.varType !== 'STRING' ? formRules.chooseValue : formRules.inputValue"
                    :property="`toolConfigList.${key}.chooseValue` ? `toolConfigList.${key}.chooseValue` : ''"
                    :key="key"
                    v-bk-tooltips="{ content: param.varTips, width: 300, placement: 'right' }"
                    class="left"
                >
                    <bk-input v-model.trim="param.chooseValue" v-if="param.varType === 'STRING'"></bk-input>
                    <bk-radio-group v-model="param.chooseValue" v-if="param.varType === 'BOOLEAN'" class="radio-param">
                        <bk-radio :value="true" class="item">{{$t('repo.是')}}</bk-radio>
                        <bk-radio :value="false" class="item">{{$t('repo.否')}}</bk-radio>
                    </bk-radio-group>
                    <bk-radio-group v-if="param.varType === 'RADIO'" v-model="param.chooseValue" class="radio-param">
                        <bk-radio v-for="(option, index) in param.varOptionList" :value="option.id" :key="index" class="item">{{option.name}}</bk-radio>
                    </bk-radio-group>
                    <bk-checkbox-group v-model="param.chooseValue" v-if="param.varType === 'CHECKBOX'" class="checkbox-param">
                        <bk-checkbox v-for="(option, index) in param.varOptionList" :value="option.id" :key="index" class="item">{{option.name}}</bk-checkbox>
                    </bk-checkbox-group>
                    <div v-if="param.varType === 'TEXTAREA'">
                        <Ace
                            class="ace-wrapper"
                            :read-only="disabled"
                            :value="value"
                            :lang="formData.toolConfigList[key - 1].chooseValue === 'shell' ? 'sh' : formData.toolConfigList[key - 1].chooseValue"
                            :name="name"
                            v-model="param.chooseValue"
                            @input="handleScriptInput"
                            height="300"
                            width="100%">
                        </Ace>
                    </div>
                </bk-form-item>
            </template>
        </div>
        <bk-form-item class="footer" v-if="scenes !== 'register-add'">
            <bk-button
                :loading="buttonLoading"
                theme="primary"
                :title="scenes === 'manage-add' ? $t('op.提交') : $t('op.保存')"
                @click.stop.prevent="handleSubmit"
            >
                {{scenes === 'manage-add' ? $t("op.提交") : $t("op.保存")}}
            </bk-button>
        </bk-form-item>
    </bk-form>
</template>

<script>
    import { mapState } from 'vuex'
    import Ace from '@/components/ace-editor'
    import fieldMixin from '../fieldMixin'

    export default {
        name: 'tool-config-form',
        components: {
            Ace
        },
        mixins: [fieldMixin],
        props: {
            tools: {
                type: Array,
                default () {
                    return []
                }
            },
            scenes: {
                type: String,
                default: 'add',
                validator (value) {
                    if (['register-add', 'manage-add', 'setting', 'manage-edit'].indexOf(value) === -1) {
                        console.error(`type property is not valid: '${value}'`)
                        return false
                    }
                    return true
                }
            },
            beforeSubmit: {
                type: Function
            },
            success: {
                type: Function
            },
            codeMessage: {
                type: Object,
                default: {}
            },
            isToolManage: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                formRules: {
                    repositoryHashId: [
                        {
                            required: true,
                            message: this.$t('st.必填项'),
                            trigger: 'change'
                        }
                    ],
                    branch: [
                        {
                            required: true,
                            message: this.$t('st.必填项'),
                            trigger: 'blur'
                        },
                        {
                            max: 50,
                            message: this.$t('st.不能多于x个字符', { num: 50 }),
                            trigger: 'blur'
                        }
                    ],
                    inputValue: [
                        {
                            max: 50,
                            message: this.$t('st.不能多于x个字符', { num: 50 }),
                            trigger: 'blur'
                        }
                    ],
                    chooseValue: [
                        {
                            required: true,
                            message: this.$t('st.必填项'),
                            trigger: 'change'
                        }
                    ]
                },
                formData: {
                    repositoryHashId: '',
                    branch: 'master',
                    toolParams: {},
                    toolConfigList: []
                },
                repoList: [],
                repoSelected: {},
                buttonLoading: false,
                lang: 'sh',
                codeUrl: `${window.DEVOPS_SITE_URL}/console/codelib/${this.$route.params.projectId}`,
                isSubmit: false,
                repoChange: true
            }
        },
        computed: {
            ...mapState('tool', {
                toolMap: 'mapList'
            }),
            isGitRepo () {
                const gitRepoType = ['CODE_GIT', 'CODE_GITLAB', 'GITHUB']
                return gitRepoType.indexOf(this.repoSelected.type) !== -1
            },
            toolParams () {
                const { toolMap } = this
                const toolParams = {}
                if (this.codeMessage && this.repoList) {
                    for (const i in this.repoList) {
                        if (this.codeMessage.repoHashId === this.repoList[i].repoHashId) {
                            this.$set(this.formData, 'repositoryHashId', this.codeMessage.repoHashId)
                            this.$set(this.formData, 'branch', this.codeMessage.branch === '' ? 'master' : this.codeMessage.branch)
                        }
                    }
                }
                this.tools.forEach(toolName => {
                    if (toolMap[toolName]) {
                        const params = JSON.parse(toolMap[toolName].params)
                        if (params.length) {
                            toolParams[toolName] = params
                            this.formData[toolName] = params
                        }
                    }
                })
                return Object.assign({}, toolParams)
            }
            // branchList () {
            //     let data = {}
            //     if (this.codeMessage.repoHashId && this.repoChange) {
            //         for (const i in this.repoList) {
            //             if (this.repoList[i].repoHashId === this.codeMessage.repoHashId) {
            //                 data = {
            //                     projCode: this.$route.params.projectId,
            //                     url: this.repoList[i].url,
            //                     type: this.repoList[i].type
            //                 }
            //             }
            //         }
            //         this.repoChange = false
            //     }
            //     return data === {} ? [] : this.$store.dispatch('task/getBranches', data)
            // }
        },
        watch: {
            tools (tools) {
                this.initFormData(tools)
            },
            'toolParams': {
                handler (newVal, oldVal) {
                    this.tools.forEach(toolName => {
                        if (this.toolMap[toolName]) {
                            const params = JSON.parse(this.toolMap[toolName].params)
                            if (params.length) {
                                this.toolParams[toolName] = params
                            }
                        }
                    })
                },
                deep: true
            },
            codeMessage: {
                handler () {
                    if (this.codeMessage && this.repoList) {
                        if (this.codeMessage.toolConfigList.length > 0) {
                            this.codeMessage.toolConfigList.forEach(item => {
                                if (!item.chooseValue) {
                                    item.chooseValue = ''
                                }
                            })
                            this.$set(this.formData, 'toolConfigList', this.codeMessage.toolConfigList)
                        }
                    }
                },
                deep: true
            }
        },
        created () {
            this.initFormData(this.tools)
            this.fetchRepos()
        },
        methods: {
            getLang (option, param) {
                if (param.varName === 'PROJECT_BUILD_TYPE') {
                    if (option.id === 'shell') {
                        this.lang = 'sh'
                    }
                }
            },
            initFormData (tools) {
                const { toolMap } = this

                // 填充已选择工具默认值到formData
                tools.forEach(toolName => {
                    if (toolMap[toolName]) {
                        const params = JSON.parse(toolMap[toolName].params)
                        if (params.length && !this.formData.toolParams.hasOwnProperty(toolName)) {
                            this.formData.toolParams[toolName] = {}
                        }
                        params.forEach(param => {
                            if (!this.formData.toolParams[toolName].hasOwnProperty(param.varName)) {
                                this.formData.toolParams[toolName][param.varName] = param.varDefault
                            }
                        })
                    }
                })
            },
            async fetchRepos () {
                try {
                    const projCode = this.$route.params.projectId
                    this.repoList = await this.$store.dispatch('task/getRepoList', { projCode })
                } catch (e) {
                    console.error(e)
                }
            },
            async handleRepoSelectToggle (isOpen) {
                if (isOpen) {
                    this.fetchRepos()
                }
            },
            handleRepoSelectChange (id) {
                this.repoChange = true
                this.repoSelected = this.repoList.find(repo => repo.repoHashId === id) || {}
                this.$refs.codeForm.formItems[0].validate('change')
            },
            handleBranchSelectChange () {
                this.$refs.codeForm.formItems[1].validate('blur')
            },
            getSubmitData () {
                const tools = this.tools.map(toolName => {
                    return {
                        toolName,
                        taskId: this.$route.params.taskId,
                        paramJson: JSON.stringify(this.formData.toolParams[toolName])
                    }
                })
                if (this.codeMessage.toolConfigList && this.codeMessage.toolConfigList.length > 0) {
                    this.codeMessage.toolConfigList.map(item => {
                        item.varOptionList = []
                    })
                }
                const data = this.scenes !== 'manage-edit' ? {
                    taskId: this.$route.params.taskId,
                    repositoryHashId: this.formData.repositoryHashId,
                    branchName: this.formData.branch,
                    scmType: this.repoSelected.type,
                    tools
                } : {
                    taskId: this.$route.params.taskId,
                    repoHashId: this.formData.repositoryHashId,
                    branch: this.formData.branch,
                    toolConfigList: this.codeMessage.toolConfigList,
                    scmType: this.repoSelected.type
                }

                return data
            },
            handleSubmit (event) {
                if (this.scenes !== 'manage-edit' && this.formData.toolConfigList.length === 0) {
                    this.formData.toolConfigList.push({ chooseValue: '' })
                }
                this.$refs.codeForm.validate().then(validator => {
                    this.isSubmit = true
                    const data = this.getSubmitData()
                    const isManageAddScenes = (this.scenes === 'manage-add' || this.scenes === 'manage-edit')

                    if (isManageAddScenes) {
                        this.buttonLoading = true
                    }
                    if (this.beforeSubmit) {
                        this.beforeSubmit()
                    }
                    if (this.scenes === 'manage-edit') {
                        this.$store.dispatch('task/saveCodeMessage', data).then(res => {
                            if (res.data === true) {
                                this.$bkMessage({ theme: 'success', message: this.$t('op.保存成功') })
                            }
                        }).catch(e => {
                            this.$bkMessage({ theme: 'error', message: this.$t('op.保存失败') })
                        }).finally(() => {
                            this.$store.dispatch('task/getCodeMessage')
                            this.buttonLoading = false
                            this.isSubmit = false
                        })
                    } else {
                        this.$store.dispatch('task/addTool', data).then(res => {
                            if (res.code === '0') {
                                if (this.success) {
                                    this.success()
                                }
                            }
                        }).catch(e => {
                            console.error(e)
                        }).finally(() => {
                            if (isManageAddScenes) {
                                this.buttonLoading = false
                                this.isSubmit = false
                            }
                        })
                    }
                }, validator => {
                    console.log(validator)
                })

                this.$emit('submit', event)
            },
            handleScriptInput (content) {
                this.handleChange(this.name, content)
            }
        }
    }
</script>

<style lang="postcss" scoped>
    @import '../../css/variable.css';
    .form-item-repo {
        .repo-add-link {
            font-size: 12px;
            position: absolute;
            right: -52px;
            top: 8px;
            color: #3a84ff;
        }
    }
    .edit-title {
        border-bottom: 1px solid $bgHoverColor;
        padding-bottom: 10px;
        margin-bottom: 22px;
        font-size: 14px;
        color: #63656e;
        font-weight: bold;
    }
    /* .bk-form-item {
        width: 50%;
    } */
    .footer {
        padding-top: 20px;
    }

    .radio-param,
    .checkbox-param {
        .item {
            margin-right: 8px;
        }
        .ace-wrapper {
            padding-top: 5px;
        }
    }
    .edit {
        .bk-form-item {
            width: 50%;
        }
    }
    .active {
        .bk-form-item {
            width: 50%;
        }
    }
</style>
