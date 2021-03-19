<template>
    <div :class="isToolManage ? 'active' : ''">
        <div v-if="(taskDetail.atomCode && taskDetail.createFrom === 'bs_pipeline') || taskDetail.createFrom === 'gongfeng_scan'">
            <div class="disf">
                <span class="pipeline-label">{{$t('编译环境')}}</span>
                <span class="fs14">{{ compile.compileEnv }}</span>
            </div>
            <div class="disf">
                <span class="pipeline-label">{{$t('编译工具')}}</span>
                <span class="fs14">{{ formatBuildEnv(codeMessage.buildEnv) }}</span>
            </div>
            <div class="disf">
                <span class="pipeline-label">{{$t('脚本类型')}}</span>
                <span class="fs14">{{ codeMessage.projectBuildType }}</span>
            </div>
            <div>
                <span class="pipeline-label">{{$t('脚本内容')}}</span>
                <Ace
                    class="ace-wrapper bash-content"
                    :read-only="true"
                    :value="value"
                    :lang="scriptData.projectBuildType.toUpperCase() === 'SHELL' ? 'sh' : scriptData.projectBuildType"
                    :name="name"
                    v-model="scriptData.projectBuildCommand"
                    @input="handleScriptInput"
                    height="300"
                    width="100%">
                </Ace>
                <!-- <pre class="bash-content">{{codeMessage.projectBuildCommand}}</pre> -->
            </div>
        </div>
        <div v-else>
            <bk-form-item :label="$t('编译环境')">
                <bk-select
                    @change="getCompileToolList"
                    :clearable="false"
                    placeholder=" "
                    v-model="compile.compileEnv"
                >
                    <bk-option v-for="option in bulidEnvList"
                        :key="option.id"
                        :id="option.id"
                        :name="option.name">
                    </bk-option>
                </bk-select>
            </bk-form-item>
            <bk-form-item v-for="(item, index) in toolList" :key="item" class="compile-tool" :label="index === 0 ? $t('编译工具') : ''">
                <bk-select
                    @change="getVersionList"
                    :clearable="false"
                    :loading="selectLoading"
                    placeholder=" "
                    popover-width="50%"
                    v-model="compile.compileTool[index]"
                >
                    <bk-option v-for="option in compileToolList"
                        :key="option.name"
                        :id="option.name"
                        :name="option.name"
                        :disabled="compile.compileTool.includes(option.name)"
                    >
                    </bk-option>
                </bk-select>
                <bk-select
                    class="compile-version"
                    :clearable="false"
                    placeholder=" "
                    popover-width="50%"
                    v-model="compile.compileToolVersion[index]"
                >
                    <bk-option v-for="option in compileToolVersionList[index]"
                        :key="option"
                        :id="option"
                        :name="option">
                    </bk-option>
                </bk-select>
                <div class="tool-icon">
                    <i class="bk-icon icon-plus" @click="addTool(index)" v-if="index === toolList.length - 1"></i>
                    <i class="bk-icon icon-close" @click="deleteTool(index)" v-if="toolList.length > 1 || compile.compileTool[index]"></i>
                </div>
            </bk-form-item>
            <bk-form-item :label="$t('脚本类型')">
                <bk-radio-group v-model="scriptData.projectBuildType" class="radio-param">
                    <bk-radio v-for="(option, index) in varOptionList" :value="option.id" :key="index" class="item">{{option.name}}</bk-radio>
                </bk-radio-group>
            </bk-form-item>
            <bk-form-item :label="$t('脚本内容')">
                <Ace
                    class="ace-wrapper"
                    :read-only="disabled"
                    :value="value"
                    :lang="scriptData.projectBuildType.toUpperCase() === 'SHELL' ? 'sh' : scriptData.projectBuildType"
                    :name="name"
                    v-model="scriptData.projectBuildCommand"
                    @input="handleScriptInput"
                    height="300"
                    width="100%">
                </Ace>
            </bk-form-item>
        </div>
    </div>
</template>
<script>
    import Ace from '@/components/ace-editor'
    import fieldMixin from '../fieldMixin'
    import { mapState } from 'vuex'
    
    export default {
        name: 'tool-compile-form',
        components: {
            Ace
        },
        mixins: [fieldMixin],
        props: {
            param: {
                type: Object
            },
            isToolManage: {
                type: Boolean,
                default: true
            },
            codeMessage: {
                type: Object,
                default: {}
            }
        },
        data () {
            return {
                varOptionList: [{ name: 'shell', id: 'SHELL' }],
                formRules: {
                    inputValue: [
                        {
                            max: 50,
                            message: this.$t('不能多于x个字符', { num: 50 }),
                            trigger: 'blur'
                        }
                    ],
                    chooseValue: [
                        {
                            required: true,
                            message: this.$t('必填项'),
                            trigger: 'change'
                        }
                    ]
                },
                scriptData: {
                    projectBuildType: 'SHELL',
                    projectBuildCommand: '# Coverity/Klocwork将通过调用编译脚本来编译您的代码，以追踪深层次的缺陷\n# 请使用依赖的构建工具如maven/cmake等写一个编译脚本build.sh\n# 确保build.sh能够编译代码\n# cd path/to/build.sh\n# sh build.sh\n'
                },
                bulidEnvList: [
                    {
                        id: 'LINUX',
                        name: 'LINUX'
                    }
                    // {
                    //     id: 'MACOS',
                    //     name: 'MACOS'
                    // }
                ],
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
                submitData: {}
            }
        },
        computed: {
            ...mapState('task', {
                taskDetail: 'detail'
            })
        },
        watch: {
            codeMessage: {
                handler () {
                    if (this.codeMessage) {
                        this.compile.compileEnv = this.codeMessage.osType || 'LINUX'
                        this.scriptData.projectBuildType = this.codeMessage.projectBuildType || 'SHELL'
                        this.scriptData.projectBuildCommand
                            = this.codeMessage.projectBuildCommand || '# Coverity/Klocwork将通过调用编译脚本来编译您的代码，以追踪深层次的缺陷\n# 请使用依赖的构建工具如maven/cmake等写一个编译脚本build.sh\n# 确保build.sh能够编译代码\n# cd path/to/build.sh\n# sh build.sh\n'
                    }
                },
                deep: true
            }
        },
        created () {
            this.getCompileToolList()
        },
        methods: {
            getCompileToolList (val) {
                if (val) {
                    this.selectLoading = true
                    this.$store.dispatch('task/getCompileTool', val).then(res => {
                        this.compileToolList = res.data
                    }).catch(e => {
                        console.error(e)
                    }).finally(() => {
                        this.selectLoading = false
                        if (this.codeMessage.buildEnv && this.isCreate) {
                            Object.keys(this.codeMessage.buildEnv).forEach(key => {
                                if (this.compile.compileTool[0] === '') {
                                    this.compile.compileTool[0] = key
                                } else {
                                    this.compile.compileTool.push(key)
                                }
                                this.getVersionList(key)
                                this.compile.compileToolVersion.push(this.codeMessage.buildEnv[key])
                                this.compile.compileTool = Array.from(new Set(this.compile.compileTool))
                                this.compile.compileToolVersion = Array.from(new Set(this.compile.compileToolVersion))
                                this.toolList = this.compile.compileTool.concat()
                                this.isCreate = false
                            })
                        } else {
                            this.toolList = [{}]
                            this.compile.compileTool = []
                            this.compile.compileToolVersion = []
                            this.compileToolVersionList = []
                        }
                    })
                }
            },
            getVersionList (newValue, oldValue) {
                const vm = this
                if (newValue !== '') {
                    this.compileToolList.forEach((item, index) => {
                        if (item.name === newValue) {
                            if (vm.compileToolVersionList.filter(item => item.name === newValue).length === 0) {
                                if (typeof (oldValue) === 'object' || oldValue === '' || oldValue === undefined) {
                                    vm.compileToolVersionList[vm.compileToolVersionList.length] = item.versions
                                    vm.compileToolVersionList = Array.from(new Set(vm.compileToolVersionList))
                                } else {
                                    vm.compileToolVersionList[vm.compile.compileTool.findIndex(item => item === newValue)] = item.versions
                                    vm.compile.compileToolVersion[vm.compile.compileTool.findIndex(item => item === newValue)] = ''
                                    vm.compileToolVersionList = Array.from(new Set(vm.compileToolVersionList))
                                }
                            }
                        }
                    })
                }
            },
            addTool (index) {
                if (this.compile.compileTool[index]) {
                    this.toolList.push({})
                }
            },
            deleteTool (index) {
                if (this.toolList.length > 1) {
                    this.compile.compileTool.splice(index, 1)
                    this.toolList.splice(index, 1)
                    this.compileToolVersionList.splice(index, 1)
                    setTimeout(() => {
                        this.compile.compileToolVersion.splice(index, 1)
                    }, 0)
                } else if (this.toolList.length === 1) {
                    this.compile.compileTool = []
                    this.toolList = [{}]
                    this.compileToolVersionList = []
                    this.compile.compileToolVersion = []
                }
            },
            handleScriptInput (content) {
                this.handleChange(this.name, content)
            },
            formatBuildEnv (content) {
                let env = ''
                for (const name in content) {
                    env += name + ' - ' + content[name] + '  '
                }
                return env
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .compile-tool {
        margin-bottom: -62px;
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
    .radio-param {
        .item {
            margin-right: 8px;
            line-height: 25px;
        }
        .ace-wrapper {
            padding-top: 5px;
        }
    }
    .active {
        .bk-form-item {
            width: 50%;
        }
    }
    .tool-icon {
        position: relative;
        top: -63px;
        left: 102%;
        .bk-icon {
            cursor: pointer;
            font-size: 20px;
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
    .bash-content {
        /* background-color: #fafbfd; */
        /* white-space: pre-wrap; */
        margin: 0px;
        position: relative;
        bottom: 46px;
        left: 104px;
        /* border: 1px solid #dcdee5; */
        padding: 13px 22px 22px 15px;
    }
</style>
