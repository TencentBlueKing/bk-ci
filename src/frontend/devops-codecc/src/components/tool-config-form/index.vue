<template>
    <div v-if="scenes === 'manage-edit' && ((taskDetail.atomCode && taskDetail.createFrom === 'bs_pipeline') || taskDetail.createFrom === 'gongfeng_scan')" :model="formData" ref="codeForm">
        <div class="disf">
            <span class="pipeline-label">{{$t('代码仓库')}}</span>
            <!-- <span class="fs14">{{ formData.aliasName || codeMessage.repoUrl || '--' }}</span> -->
            <span v-if="!codeMessage.repoUrl || !codeMessage.repoUrl.length">--</span>
            <ul v-else>
                <li class="fs14 pb5" v-for="item in codeMessage.repoUrl" :key="item">{{item}}</li>
            </ul>
        </div>
        <div class="disf" v-if="isGitRepo || codeMessage.branch.length">
            <span class="pipeline-label">{{$t('分支')}}</span>
            <!-- <span class="fs14">{{ codeMessage.branch || formData.branch }}</span> -->
            <span v-if="!codeMessage.branch || !codeMessage.branch.length">--</span>
            <ul v-else>
                <li class="fs14 pb5" v-for="item in codeMessage.branch" :key="item">{{item}}</li>
            </ul>
        </div>
        <div v-if="scenes !== 'manage-edit'">
            <tool-compile-form v-show="shownTool" ref="editData" :code-message="toolParams" :is-tool-manage="isToolManage" />
        </div>
        <div :class="isToolManage ? 'active' : ''" v-else>
            <template>
                <tool-compile-form v-show="shownTool" ref="editData" class="pb20" style="width: 50%;" :code-message="codeMessage" :is-tool-manage="isToolManage" />
            </template>
        </div>
    </div>
    <bk-form v-else :class="scenes === 'manage-edit' ? 'edit' : ''" :label-width="130" :model="formData" ref="codeForm">
        <bk-form-item :label="this.$t('代码仓库')" :required="true" :rules="formRules.repoHashId" property="repoHashId" class="form-item-repo">
            <bk-select
                v-model="formData.repoHashId"
                @toggle="handleRepoSelectToggle"
                @change="handleRepoSelectChange"
                :loading="isloading"
                :disabled="isToolManage"
                searchable
            >
                <bk-option
                    v-for="option in repoList"
                    :key="option.repoHashId"
                    :id="option.repoHashId"
                    :name="option.aliasName">
                </bk-option>
            </bk-select>
            <a :href="codeUrl" target="_blank" class="repo-add-link">{{$t('新增')}} <i class="codecc-icon icon-link"></i></a>
        </bk-form-item>
        <bk-form-item :label="$t('分支')" :required="true" property="branch" :rules="formRules.branch" v-if="isGitRepo">
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
        <div style="height: 20px;"></div>
        <div v-if="scenes !== 'manage-edit'">
            <tool-compile-form v-show="shownTool" ref="editData" :code-message="toolParams" :is-tool-manage="isToolManage" />
        </div>
        <div :class="isToolManage ? 'active' : ''" v-else>
            <template>
                <tool-compile-form v-show="shownTool" ref="editData" class="pb20" style="width: 50%;" :code-message="codeMessage" :is-tool-manage="isToolManage" />
            </template>
        </div>
        <bk-form-item class="footer" v-if="scenes !== 'register-add'">
            <bk-button v-if="scenes === 'manage-add'" @click="handlePrev">{{$t('上一步')}}</bk-button>
            <bk-button
                :loading="buttonLoading"
                theme="primary"
                :title="scenes === 'manage-add' ? $t('提交') : $t('保存')"
                @click.stop.prevent="handleSubmit"
            >
                {{scenes === 'manage-add' ? $t('提交') : $t('保存')}}
            </bk-button>
        </bk-form-item>
    </bk-form>
</template>

<script>
    import { mapState } from 'vuex'
    import toolCompileForm from '@/components/tool-compile-form'
    import fieldMixin from '../fieldMixin'

    export default {
        name: 'tool-config-form',
        components: {
            toolCompileForm
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
            },
            codeLang: {
                type: Array,
                default: []
            }
        },
        data () {
            return {
                formRules: {
                    repoHashId: [
                        {
                            required: true,
                            message: this.$t('必填项'),
                            trigger: 'change'
                        }
                    ],
                    branch: [
                        {
                            required: true,
                            message: this.$t('必填项'),
                            trigger: 'blur'
                        },
                        {
                            max: 50,
                            message: this.$t('不能多于x个字符', { num: 50 }),
                            trigger: 'blur'
                        }
                    ]
                },
                formData: {
                    repoHashId: '',
                    branch: 'master',
                    toolParams: {},
                    toolConfigList: [],
                    aliasName: ''
                },
                repoList: [],
                repoSelected: {},
                buttonLoading: false,
                lang: 'sh',
                codeUrl: `${window.DEVOPS_SITE_URL}/console/codelib/${this.$route.params.projectId}`,
                isSubmit: false,
                repoChange: true,
                compileToolList: [],
                compileToolVersionList: [],
                compile: {
                    compileEnv: '',
                    compileTool: [''],
                    compileToolVersion: []
                },
                selectLoading: false,
                toolList: [
                    {}
                ],
                isCreate: true,
                scriptLang: ['1', '2', '4', '16', '512', '4096'],
                isloading: false
            }
        },
        computed: {
            ...mapState('tool', {
                toolMap: 'mapList'
            }),
            ...mapState('task', {
                taskDetail: 'detail'
            }),
            taskId () {
                return this.$route.params.taskId
            },
            isGitRepo () {
                const gitRepoType = ['CODE_GIT', 'CODE_GITLAB', 'GITHUB']
                return gitRepoType.indexOf(this.repoSelected.type) !== -1 || this.taskDetail.createFrom === 'gongfeng_scan'
            },
            toolParams () {
                const toolParams = {}
                for (const i in this.repoList) {
                    if (this.codeMessage.repoHashId === this.repoList[i].repoHashId && !this.formData.repoHashId) {
                        this.$set(this.formData, 'repoHashId', this.codeMessage.repoHashId)
                        this.$set(this.formData, 'aliasName', this.repoList[i].aliasName)
                        const branch = this.codeMessage.branch || (this.codeMessage.codeInfo && this.codeMessage.codeInfo[0] && this.codeMessage.codeInfo[0].branch)
                        this.$set(this.formData, 'branch', branch)
                    }
                }
                return Object.assign({}, toolParams)
            },
            shownTool () {
                let shownTool = false
                this.tools.forEach(toolName => {
                    if (['COVERITY', 'KLOCWORK', 'PINPOINT', 'CODEQL', 'CLANG', 'SPOTBUGS'].includes(toolName) && this.hasScript) {
                        shownTool = true
                    }
                })
                return shownTool
            },
            hasScript () {
                // 非编译语言不显示脚本框
                let hasScript = false
                let codeLangNum = this.taskDetail.codeLang
                if (this.codeLang.length) {
                    codeLangNum = String(this.codeLang.reduce((n1, n2) => n1 + n2, 0))
                }
                if (codeLangNum && this.scriptLang.find(lang => lang & codeLangNum)) hasScript = true
                return hasScript
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
            'toolParams': {
                handler (newVal, oldVal) {
                    // this.tools.forEach(toolName => {
                    //     if (this.toolMap[toolName] && this.toolMap[toolName].params) {
                    //         const params = JSON.parse(this.toolMap[toolName].params)
                    //         console.log(params)
                    //     }
                    // })
                },
                deep: true
            },
            codeMessage: {
                handler () {
                    for (const i in this.repoList) {
                        if (this.codeMessage.repoHashId === this.repoList[i].repoHashId && !this.formData.repoHashId) {
                            this.$set(this.formData, 'repoHashId', this.codeMessage.repoHashId)
                            this.$set(this.formData, 'aliasName', this.repoList[i].aliasName)
                            this.$set(this.formData, 'branch', this.codeMessage.branch === '' ? 'master' : this.codeMessage.branch)
                        }
                    }
                    this.tools.forEach(toolName => {
                        if (['COVERITY', 'KLOCWORK', 'PINPOINT', 'CODEQL', 'CLANG', 'SPOTBUGS'].includes(toolName) && this.hasScript) {
                            this.shownTool = true
                        }
                    })
                    if (this.codeMessage && this.repoList) {
                        this.compile.compileEnv = this.codeMessage.osType || 'LINUX'
                    }
                },
                deep: true
            }
        },
        created () {
            this.fetchRepos()
        },
        methods: {
            async fetchRepos () {
                try {
                    this.isloading = true
                    const projCode = this.$route.params.projectId
                    this.repoList = await this.$store.dispatch('task/getRepoList', { projCode })
                } catch (e) {
                    console.error(e)
                } finally {
                    this.isloading = false
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
                this.$set(this.formData, 'aliasName', this.repoSelected.aliasName)
                this.$refs.codeForm.formItems[0].validate('change')
            },
            handleBranchSelectChange () {
                this.$refs.codeForm.formItems[1].validate('blur')
            },
            getSubmitData () {
                const receiveData = Object.assign(this.$refs.editData.compile, { ...this.$refs.editData.scriptData })
                const buildEnv = {}
                if (receiveData) {
                    for (let i = 0; i < receiveData.compileTool.length; i++) {
                        const key = receiveData.compileTool[i]
                        const value = receiveData.compileToolVersion[i]
                        buildEnv[key] = value
                    }
                }
                const branch = Array.isArray(this.formData.branch) ? this.formData.branch.join() : this.formData.branch
                const data = this.scenes !== 'manage-edit' ? {
                    taskId: this.taskId,
                    aliasName: this.formData.aliasName,
                    repoHashId: this.formData.repoHashId,
                    branch,
                    scmType: this.repoSelected.type,
                    osType: receiveData.compileEnv || '',
                    buildEnv: buildEnv || '',
                    projectBuildType: receiveData.projectBuildType || '',
                    projectBuildCommand: receiveData.projectBuildCommand || ''
                } : {
                    taskId: this.$route.params.taskId,
                    aliasName: this.formData.aliasName,
                    repoHashId: this.formData.repoHashId,
                    branch,
                    scmType: this.repoSelected.type,
                    osType: receiveData.compileEnv || '',
                    buildEnv: buildEnv || '',
                    projectBuildType: receiveData.projectBuildType || '',
                    projectBuildCommand: receiveData.projectBuildCommand || ''
                }
                if (!this.hasScript) {
                    data.osType = ''
                    data.buildEnv = ''
                    data.projectBuildType = ''
                    data.projectBuildCommand = ''
                }
                return data
            },
            handleSubmit (event) {
                this.$refs.codeForm.validate().then(validator => {
                    this.isSubmit = true
                    const data = this.getSubmitData()
                    const isManageAddScenes = (this.scenes === 'manage-add' || this.scenes === 'manage-edit')

                    if (isManageAddScenes) {
                        this.buttonLoading = true
                    }
                    if (this.scenes === 'manage-edit') {
                        this.$store.dispatch('task/saveCodeMessage', data).then(res => {
                            if (res.data === true) {
                                this.$emit('saveBasic')
                            }
                        }).catch(e => {
                            this.$bkMessage({ theme: 'error', message: this.$t('保存失败') })
                        }).finally(() => {
                            this.$store.dispatch('task/getCodeMessage')
                            this.buttonLoading = false
                            this.isSubmit = false
                        })
                    } else {
                        const pickedTools = this.tools.map(toolName => {
                            return {
                                toolName,
                                taskId: this.taskId
                            }
                        })
                        let tools = this.$parent.$refs.toolParamsSideForm.getParamsValue()
                        tools = Object.assign(pickedTools, tools)
                        const obj = {}
                        tools = tools.reduce(function (item, next) { // 去重
                            if (!obj[next.toolName]) {
                                obj[next.toolName] = true
                                item.push(next)
                            }
                            return item
                        }, [])
                        const postData = { ...data, tools }
                        this.$store.dispatch('task/addTool', postData).then(res => {
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
                                this.$emit('update')
                            }
                        })
                    }
                }, validator => {
                    console.log(validator)
                })

                this.$emit('submit', event)
            },
            handlePrev () {
                this.$emit('handlePrev')
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
            line-height: 32px;
            right: -52px;
            top: 0;
            color: #3a84ff;
        }
    }
    /* .bk-form-item {
        width: 50%;
    } */
    .footer {
        /* padding-top: 20px; */
    }

    .radio-param,
    .checkbox-param {
        .item {
            margin-right: 8px;
            line-height: 25px;
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
    .compile-tool {
        margin-bottom: -57px;
        div {
            div {
                width: 49%;
            }
        }
    }
    .compile-version {
        position: relative;
        top: -32px;
        left: 51%;
    }
    .tool-icon {
        position: relative;
        top: -57px;
        left: 102%;
        .bk-icon {
            cursor: pointer;
        }
    }
    .pipeline-label {
        display: inline-block;
        width: 104px;
        text-align: left;
        font-size: 14px;
        line-height: 14px;
        height: 46px;
        font-weight: 600;
    }
</style>
