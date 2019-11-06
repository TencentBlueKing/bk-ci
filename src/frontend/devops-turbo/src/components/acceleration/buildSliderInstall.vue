<template>
    <section class="build-wrapper" v-bkloading="{ isLoading: isLoading }">
        <bk-form :label-width="130" :model="taskParam">
            <bk-form-item label="任务 ID：" class="form-item-text">
                <p class="bk-form-text">{{ taskParam.taskId }}<span class="desc-ash">（系统自动生成）</span></p>
            </bk-form-item>
            <bk-form-item label="创建时间：" class="form-item-text">
                <p class="bk-form-text">{{ transDateStr(taskParam.createdDate) }}</p>
            </bk-form-item>
            <bk-form-item label="创建人：" class="form-item-text">
                <p class="bk-form-text">{{ taskParam.createdBy }}</p>
            </bk-form-item>
            <devops-form-item label="任务名称：" :required="true" :property="'taskName'"
                :is-error="errors.has('taskName')"
                :error-msg="errors.first('taskName')">
                <bk-input
                    type="text"
                    name="taskName"
                    placeholder="请输入任务名称"
                    v-validate="'max:20|required'"
                    v-model.trim="taskParam.taskName">
                </bk-input>
            </devops-form-item>
            <bk-form-item label="构建类型：" class="form-item-text">
                <p class="bk-form-text">{{ taskParam.machineType === '1' ? '公共构建机' : taskParam.machineType === '2' ? '第三方构建机' : '下载安装包至自己的构建机，进行构建和加速' }}</p>
            </bk-form-item>
            <bk-form-item label="构建机系统：" class="form-item-text">
                <p class="bk-form-text">{{ paramsMap['osType'][taskParam.osType] }}</p>
            </bk-form-item>
            <bk-form-item label="项目语言：" class="form-item-text">
                <p class="bk-form-text">{{ setLanguage(taskParam.projLang) }}</p>
            </bk-form-item>
            <bk-form-item label="项目类型：" class="form-item-text">
                <p class="bk-form-text">{{ taskParam.projType === '1' ? '后台服务' : (taskParam.projType === '2' ? 'Unity3D' : 'UE4') }}</p>
            </bk-form-item>
            <bk-form-item label="构建工具：" class="form-item- text">
                <p class="bk-form-text">{{ paramsMap['toolType'][taskParam.toolType] }}</p>
            </bk-form-item>
            <bk-form-item label="加速方案：" class="form-item-text label-item-input">
                <div class="bk-form-inline">
                    <vv-selector
                        name="accelerateId"
                        v-validate="'required'"
                        :list="programList"
                        :placeholder="'请选择加速方案'"
                        :display-key="'name'"
                        :setting-key="'id'"
                        :multi-select="false"
                        :disabled="pipelineDisabled"
                        v-model="accelerateId"
                        @item-selected="accelerateSelect">
                    </vv-selector>
                </div>
                <p v-if="errors.has('accelerateId')" class="is-danger">{{ errors.first('accelerateId') }}</p>
            </bk-form-item>
            <devops-form-item label="第三方构建机IP：" :property="'machineIp'"
                :is-error="errors.has('machineIp')"
                :error-msg="errors.first('machineIp')">
                <bk-input
                    type="text"
                    name="machineIp"
                    placeholder="请输入构建机IP，IP之间用英文逗号分隔, 250字符以内"
                    maxlength="250"
                    v-validate="'required|multiIp'"
                    v-model.trim="taskParam.machineIp">
                </bk-input>
                <bk-popover placement="right" :width="200" class="prompt-tips">
                    <i class="bk-icon icon-info-circle"></i>
                    <div slot="content" style="white-space: normal;">
                        <div class="">多IP请用英文逗号分隔，蓝盾会将该IP设置为白名单</div>
                    </div>
                </bk-popover>
            </devops-form-item>
            <bk-form-item label="构建机地址：" class="form-item-text label-item-input">
                <div class="bk-form-inline">
                    <vv-selector
                        name="machineZone"
                        :list="citys"
                        :placeholder="'请选择'"
                        :display-key="'showName'"
                        :setting-key="'zoneName'"
                        :multi-select="false"
                        :disabled="pipelineDisabled"
                        v-model="machineParam.machineZone">
                    </vv-selector>
                </div>
                <p v-if="errors.has('machineZone')" class="is-danger">{{ errors.first('machineZone') }}</p>
            </bk-form-item>
            <bk-form-item label="编译器：" class="form-item-text label-item-input form-average-two-small">
                <div class="bk-form-inline" style="width:184px;">
                    <vv-selector
                        :list="envList"
                        :display-key="'name'"
                        :setting-key="'name'"
                        :selected.sync="taskParam.envList"
                        :multi-select="false"
                        :disabled="false"
                        v-model="taskParam.envList"
                        @item-selected="envSelect">
                    </vv-selector>
                </div>
                <div class="bk-form-inline" style="width:184px;">
                    <vv-selector
                        name="gccVersion"
                        v-validate="'required'"
                        :list="gccVersion"
                        :display-key="'name'"
                        :setting-key="'version'"
                        :selected.sync="taskParam.gccVersion"
                        :multi-select="false"
                        v-model="taskParam.gccVersion"
                        :disabled="false">
                    </vv-selector>
                    <p v-if="errors.has('gccVersion')" class="is-danger">{{ errors.first('gccVersion') }}</p>
                </div>
            </bk-form-item>
            <bk-form-item label="本地构建路径：" class="form-item-text">
                <p class="bk-form-text">{{ taskParam.bsPipelineName || '--' }}</p>
            </bk-form-item>
            <bk-form-item class="form-item-text">
                <a :href="`http://api.devops.oa.com/turbo/api/user/turbo/task/software/${taskParam.taskId}/LD_Turbo_install.tar.gz`" class="bk-form-link" style="margin-left:0;">点击下载编译加速包</a>
                <a :href="`${DOCS_URL_PREFIX}/%E6%89%80%E6%9C%89%E6%9C%8D%E5%8A%A1/%E7%BC%96%E8%AF%91%E5%8A%A0%E9%80%9F/%E7%94%A8%E6%88%B7%E6%8C%87%E5%8D%97/%E7%AC%AC%E4%B8%89%E6%96%B9%E6%9E%84%E5%BB%BA%E6%9C%BA%E4%BD%BF%E7%94%A8LD-Turbo%E5%AE%A2%E6%88%B7%E7%AB%AF.html`"
                    class="bk-form-link" target="_blank"
                >查看编译加速包使用教程</a>
            </bk-form-item>
            <bk-form-item label="分布式编译加速：" class="form-item-text">
                <p class="bk-form-text">{{ taskParam.banDistcc === 'false' && typeof taskParam.machineNum !== 'undefined' ? `${ taskParam.machineNum }台加速机器(共${ taskParam.cpuNum }核)` : '无'}}<a href="wxwork://message/?username=DevOps" class="bk-form-link">如需升级资源请联系DevOps小助手</a></p>
            </bk-form-item>
            <bk-form-item label="其他参数：" class="form-item-text">
                <p class="bk-form-text">{{ taskParam.ccacheEnabled === 'true' && typeof taskParam.cacheSize !== 'undefined' ? `ccache目录大小${ taskParam.cacheSize }MB` : '无'}}</p>
            </bk-form-item>
            <bk-form-item>
                <bk-button theme="primary" :disabled="isDisabled" @click="buildSave()">保存</bk-button>
                <bk-button theme="default" :disabled="isDisabled" @click="configCancel">取消</bk-button>
            </bk-form-item>
        </bk-form>
    </section>
</template>

<script>
    import vvSelector from '@/components/common/vv-selector'
    import { mapGetters } from 'vuex'
    
    export default {
        components: {
            vvSelector
        },
        props: {
            taskInfo: {
                type: Object
            },
            envList: {
                type: Array
            },
            isLoading: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                isDisabled: false, // 按钮状态
                showLinkElement: false, // 备选
                pipelineDisabled: false,
                accelerateId: '',
                DOCS_URL_PREFIX: DOCS_URL_PREFIX,
                paramsMap: {
                    'osType': {
                        '1': 'tlinux'
                    },
                    'compilerType': {
                        '1': 'gcc',
                        '2': 'clang'
                    },
                    'toolType': {
                        '1': 'make/cmake',
                        '2': 'blade',
                        '3': 'bazel'
                    }
                },
                gccVersion: [],
                programList: [ // 配置方案
                    {
                        name: 'ccache+distcc',
                        id: '1',
                        banDistcc: 'false',
                        ccacheEnabled: 'true'
                    },
                    {
                        name: 'ccache',
                        id: '2',
                        banDistcc: 'true',
                        ccacheEnabled: 'true'
                    },
                    {
                        name: 'distcc',
                        id: '3',
                        banDistcc: 'false',
                        ccacheEnabled: 'false'
                    }
                ],
                optLanguage: [
                    {
                        label: 'C/C++',
                        value: '1',
                        isDisabled: false
                    },
                    {
                        label: 'Objective C/C++',
                        value: '2',
                        isDisabled: true
                    },
                    {
                        label: 'C#',
                        value: '3',
                        isDisabled: true
                    }
                ],
                citys: [
                    {
                        zoneName: 'shenzhen',
                        showName: '深圳'
                    },
                    {
                        zoneName: 'shanghai',
                        showName: '上海'
                    },
                    {
                        zoneName: 'chengdu',
                        showName: '成都'
                    },
                    {
                        zoneName: 'tianjin',
                        showName: '天津'
                    }
                ],
                loading: {
                    machine: false
                },
                taskParam: {
                    banDistcc: '',
                    bsVmSeqId: '',
                    bsElementId: '',
                    bsPipelineId: '',
                    bsPipelineName: '',
                    ccacheEnabled: '',
                    createdDate: '',
                    gccVersion: '',
                    latestStatus: '',
                    machineType: '',
                    projLang: '',
                    projType: '',
                    taskId: '',
                    taskName: '',
                    projectName: '',
                    city: ''
                },
                machineParam: { // 构建机配置信息
                    bsAgentId: '-1',
                    bsProjectId: '',
                    machineIp: '',
                    machineHostName: '',
                    machineZone: ''
                }

            }
        },
        computed: {
            ...mapGetters([
                'enableProjectList'
            ]),
            projectId () {
                return this.$route.params.projectId
            }
        },
        created () {
            this.$validator.extend('multiIp', {
                getMessage: field => `格式不正确，IP之间用,分隔`,
                validate: function (value) {
                    return /^(((25[0-5]|2[0-4]\d|1?\d?\d)\.){3}(25[0-5]|2[0-4]\d|1?\d?\d)[,$]?)+$/.test(value)
                }
            })
            this.init()
        },
        
        methods: {
            accelerateSelect (value, item) {
                Object.assign(this.taskParam, {
                    banDistcc: item.banDistcc,
                    ccacheEnabled: item.ccacheEnabled
                })
            },

            buildSave () {
                this.configCommit()
                this.machineCommit()
            },

            envSelect (value, item) { // 选择编译器
                this.gccVersion = item.gccVersion
            },
            // 提交配置
            async configCommit () { //  machineId
                const validate = await this.$validator.validateAll()
                if (!validate || this.errors.any()) {
                    return false
                }
                this.isLoading = true
                try {
                    let projectName = ''
                    this.enableProjectList.forEach(project => {
                        if (project.projectCode === this.projectId) {
                            projectName = project.projectName
                        }
                    })
                    const { taskId, taskName, ccacheEnabled, banDistcc, bsPipelineId, bsPipelineName, bsVmSeqId, bsElementId, gccVersion, machineNum, cpuNum, cacheSize, buildMachineId, machineIp } = this.taskParam
                    const res = await this.$store.dispatch('turbo/commitBuild', {
                        params: {
                            taskId: taskId,
                            taskName: taskName,
                            ccacheEnabled: ccacheEnabled,
                            banDistcc: banDistcc,
                            bsProjectId: this.projectId,
                            bsPipelineId: bsPipelineId || '-1',
                            bsPipelineName: bsPipelineName,
                            bsVmSeqId: bsVmSeqId,
                            bsElementId: bsElementId,
                            gccVersion: gccVersion,
                            machineNum: machineNum,
                            cpuNum: cpuNum,
                            cacheSize: cacheSize,
                            city: this.machineParam.machineZone,
                            machineIp: machineIp,
                            buildMachineId: buildMachineId
                        },
                        operType: 'conf',
                        projectName: projectName
                    })
                    if (res) {
                        this.$emit('configCancel')
                        this.$bkMessage({
                            message: '保存成功',
                            theme: 'success'
                        })
                        this.$emit('buildSetting', this.taskParam)
                    }
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                } finally {
                    this.isLoading = false
                }
            },

            configCancel () {
                this.$emit('configCancel')
            },
            
            transDateStr (dateStr) {
                if (!dateStr) {
                    return ''
                }
                const date = new Date(dateStr) // 时间戳为10位需*1000，时间戳为13位的话不需乘1000
                const Y = date.getFullYear() + '年'
                const M = (date.getMonth() + 1 < 10 ? '0' + (date.getMonth() + 1) : date.getMonth() + 1) + '月'
                const D = date.getDate() + '日 '
                const h = date.getHours() + ':'
                const m = date.getMinutes() < 10 ? '0' + date.getMinutes() : date.getMinutes()
                // let s = date.getSeconds();
                return Y + M + D + h + m
            },
            setLanguage (lang) {
                const langArr = lang.split(',')
                let langStr = ''
                langArr.forEach(item => {
                    this.optLanguage.forEach(itemLang => {
                        if (item === itemLang.value) {
                            langStr += langStr ? '、' + itemLang.label : itemLang.label
                        }
                    })
                })
                return langStr
            },
            
            // 安装
            async machineCommit () {
                const validate = await this.$validator.validateAll()
                if (!validate || this.errors.any()) {
                    return false
                }
                this.isDisabled = true
                try {
                    // console.log('构建机安装参数:', Object.assign({}, this.machineParam, {gccVersion: this.taskParam.gccVersion}, {bsProjectId: this.projectId}))
                    await this.$store.dispatch('turbo/commitMachine', {
                        params: Object.assign({}, this.machineParam, {
                            gccVersion: this.taskParam.gccVersion,
                            bsProjectId: this.projectId,
                            machineIp: this.taskParam.machineIp,
                            machineId: this.taskParam.buildMachineId
                        }),
                        taskId: this.taskParam.taskId,
                        operType: 'conf'
                    })
                    // if (res) {
                    //     this.$bkMessage({
                    //         message: '配置成功',
                    //         theme: 'success'
                    //     })
                    // }
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                } finally {
                    this.isDisabled = false
                }
            },

            init () {
                const { taskParam, taskInfo, programList } = this
                // 参数初始化
                Object.assign(taskParam, taskInfo)
                !taskParam.cacheSize && (taskParam.cacheSize = 1024)
                !taskParam.machineNum && (taskParam.machineNum = 18) && (taskParam.cpuNum = 144)
                !taskParam.gccVersion && (taskParam.gccVersion = 'gcc4.4.6')
                let envList
                this.envList.forEach(env => {
                    env.gccVersion.forEach(item => {
                        if (item.version === this.taskParam.gccVersion) {
                            envList = Object.assign({}, env)
                        }
                    })
                })
                if (envList) {
                    this.taskParam.envList = envList.name
                    this.gccVersion = envList.gccVersion
                }
                this.machineParam.machineZone = taskParam.city
                programList.forEach(item => {
                    (item.banDistcc === taskParam.banDistcc && item.ccacheEnabled === taskParam.ccacheEnabled) && (this.accelerateId = item.id)
                })
            }
        }
    }
</script>
