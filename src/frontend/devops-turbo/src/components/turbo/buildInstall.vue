
<template>
    <section class="build-wrapper build-install-form">
        <div class="build-tooltips">
            <i class="bk-icon icon-info-circle-shape"></i>
            <span>输入IP等信息并下载安装包，以便蓝盾接受你构建机的加速请求</span>
        </div>
        <bk-form :label-width="130" :model="machineParam">
            <devops-form-item label="构建机 IP：" :required="true" :property="'machineIp'"
                :is-error="errors.has('machineIp')"
                :error-msg="errors.first('machineIp')">
                <bk-input
                    type="text"
                    name="machineIp"
                    placeholder="请输入构建机IP，IP之间用英文逗号分隔, 250字符以内"
                    maxlength="250"
                    v-validate="'required|multiIp'"
                    v-model.trim="machineParam.machineIp">
                </bk-input>
                <bk-popover placement="right" :width="200" class="prompt-tips">
                    <i class="bk-icon icon-info-circle"></i>
                    <div slot="content" style="white-space: normal;">
                        <div class="">多IP请用英文逗号分隔，蓝盾会将该IP设置为白名单</div>
                    </div>
                </bk-popover>
            </devops-form-item>
            <bk-form-item label="构建机地址：">
                <div class="bk-form-inline">
                    <vv-selector
                        name="machineZone"
                        :list="citys"
                        :placeholder="'请选择'"
                        :display-key="'showName'"
                        :setting-key="'zoneName'"
                        :multi-select="false"
                        v-model="machineParam.machineZone">
                    </vv-selector>
                </div>
            </bk-form-item>
            <bk-form-item label="编译器：" class="form-average-two">
                <div class="bk-form-inline" style="width:226px;">
                    <vv-selector
                        :list="envList"
                        :display-key="'name'"
                        :setting-key="'name'"
                        v-model="taskParam.envList"
                        :multi-select="false"
                        :disabled="false"
                        @item-selected="envSelect">
                    </vv-selector>
                </div>
                <div class="bk-form-inline" style="width:226px;">
                    <vv-selector
                        name="gccVersion"
                        v-validate="'required'"
                        :list="gccVersion"
                        :display-key="'name'"
                        :setting-key="'version'"
                        v-model="taskParam.gccVersion"
                        :multi-select="false"
                        :disabled="false">
                    </vv-selector>
                    <p v-if="errors.has('gccVersion')" class="is-danger">{{ errors.first('gccVersion') }}</p>
                </div>
            </bk-form-item>
            <bk-form-item label="下载安装包：">
                <bk-button theme="default" :disabled="isBtnDisabled" @click="downLoad">点击下载</bk-button>
                <p class="bk-text-section">
                    下载编译加速包 LD_Turbo_install.tar.gz 并发送至自己的构建机进行安装<br />
                    1、解压软件包：LD_Turbo_install.tar.gz<br />
                    2、执行.sh 脚本安装 distcc&ccache 等客户端：cd LD_Turbo_install&&./install.sh
                </p>
            </bk-form-item>
            <bk-form-item>
                <bk-button theme="primary" :disabled="isBtnDisabled" @click="configCommit">提交</bk-button>
            </bk-form-item>
        </bk-form>
    </section>
</template>

<script>
    import { mapGetters } from 'vuex'
    import vvSelector from '@/components/common/vv-selector'
    import buildOption from '@/components/buildOption'

    export default {
        components: {
            vvSelector
        },
        mixins: [buildOption],
        data () {
            return {
                taskParam: {
                    gccVersion: 'gcc4.4.6',
                    bsPipelineId: '-1'
                },
                machineParam: { // 构建机配置信息
                    bsAgentId: '-1',
                    machineIp: '',
                    machineZone: 'shenzhen'
                }
            }
        },
        computed: {
            ...mapGetters('turbo', [
                'getRegister'
            ]),
            ...mapGetters([
                'onlineProjectList'
            ])
        },
        created () {
            this.$validator.extend('multiIp', {
                getMessage: field => `格式不正确，IP之间用,分隔`,
                validate: function (value) {
                    return /^(((25[0-5]|2[0-4]\d|1?\d?\d)\.){3}(25[0-5]|2[0-4]\d|1?\d?\d)[,$]?)+$/.test(value)
                }
            })
            Object.assign(this.taskParam, this.getRegister)
            this.taskParam.bsProjectId = this.projectId
        },
        
        methods: {
            downLoad () {
                window.open(`http://api.devops.oa.com/turbo/api/user/turbo/task/software/${this.taskParam.taskId}/LD_Turbo_install.tar.gz`)
            },
            buildSave () {
                console.log('提交')
            },
            // 安装
            async machineCommit () {
                this.isDisabled = true
                try {
                    const res = await this.$store.dispatch('turbo/commitMachine', {
                        params: Object.assign({}, this.machineParam, { gccVersion: this.taskParam.gccVersion }, { bsProjectId: this.projectId }),
                        taskId: this.taskParam.taskId,
                        operType: 'reg'
                    })
                    if (res) {
                        this.$store.commit('turbo/modifyProcessHead', {
                            process: 'registSuccess',
                            current: 2
                        })
                    }
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                } finally {
                    this.isDisabled = false
                }
            },
            // 提交配置
            async configCommit () { //  machineId
                const validate = await this.$validator.validateAll()
                console.log(this.errors)
                if (!validate || this.errors.any()) {
                    return false
                }
                this.isDisabled = true
                try {
                    let projectName = ''
                    this.onlineProjectList.forEach(project => {
                        if (project.project_code === this.projectId) {
                            projectName = project.project_name
                        }
                    })
                    const { taskId, taskName, ccacheEnabled, banDistcc, bsPipelineId, bsPipelineName, bsVmSeqId, bsElementId, gccVersion, machineNum, cpuNum, cacheSize, buildMachineId } = this.taskParam
                    const { machineZone } = this.machineParam
                    const res = await this.$store.dispatch('turbo/commitBuild', {
                        params: Object.assign({
                            taskId: taskId,
                            taskName: taskName,
                            ccacheEnabled: ccacheEnabled,
                            banDistcc: banDistcc,
                            bsProjectId: this.projectId,
                            bsPipelineId: bsPipelineId,
                            bsPipelineName: bsPipelineName,
                            bsVmSeqId: bsVmSeqId,
                            bsElementId: bsElementId,
                            city: machineZone,
                            gccVersion: gccVersion,
                            machineNum: machineNum,
                            cpuNum: cpuNum,
                            cacheSize: cacheSize,
                            buildMachineId: buildMachineId
                        }),
                        operType: 'reg',
                        projectName: projectName
                    })
                    if (res) {
                        this.$store.commit('turbo/setRegister', {
                            gccVersion: gccVersion
                        })
                        this.machineCommit()
                    }
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                } finally {
                    this.isDisabled = false
                }
            }
        }
    }
</script>

<style lang="scss">
    @import '../../assets/scss/conf';
    .build-install-form {
        .bk-form .prompt-tips {
            position: absolute;
            left: 469px;
            top: 7px;
        }
    }
    .form-average-two .bk-form-inline {
        width: 226px;
        .text {
            width: 100%;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }
        &+.bk-form-inline {
            margin-left: 4px;
        }
    }

</style>
