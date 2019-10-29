<template>
    <section class="registration-wrapper">
        <bk-form :label-width="120" :model="taskParam">
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
            <bk-form-item label="构建机类型：" :required="true">
                <bk-radio-group v-model="taskParam.machineType" class="machine-type-item">
                    <bk-radio v-for="(machine, index) in optBuild" :key="index" :value="machine.value" :disabled="machine.isDisabled">{{ machine.label }}</bk-radio>
                </bk-radio-group>
            </bk-form-item>
            <bk-form-item label="构建机系统：">
                <p class="bk-form-p">目前仅支持<strong>tlinux</strong></p>
            </bk-form-item>
            <bk-form-item label="项目语言：">
                <p class="bk-form-p">目前仅支持<strong>C/C++</strong></p>
            </bk-form-item>
            <bk-form-item label="项目类型：">
                <p class="bk-form-p">目前仅支持<strong>后台服务</strong></p>
            </bk-form-item>
            <bk-form-item label="编译器：">
                <bk-radio-group v-model="taskParam.compilerType" class="compiler-type-item">
                    <bk-radio v-for="(type, index) in optcompiler" :key="index" :value="type.value" :disabled="type.isDisabled">{{ type.label }}</bk-radio>
                </bk-radio-group>
                <div class="selected-item-tooltips"
                    :class="{ 'selected-item-1': taskParam.compilerType === '2' }">
                    <div class="bk-form-p" v-if="taskParam.compilerType === '1'" v-text="gccVersionList"></div>
                    <div class="bk-form-p" v-if="taskParam.compilerType === '2'" v-text="clangVersionList"></div>
                </div>
            </bk-form-item>
            <bk-form-item label="构建工具：">
                <bk-radio-group v-model="taskParam.toolType" class="tool-type-item">
                    <bk-radio v-for="(type, index) in optTool" :key="index" :value="type.value" :disabled="type.isDisabled">{{ type.label }}</bk-radio>
                </bk-radio-group>
                <div class="selected-item-tooltips"
                    :class="{ 'selected-item-2': taskParam.toolType === '1','selected-item-3': taskParam.toolType === '2','selected-item-4': taskParam.toolType === '3' }">
                    <div class="bk-form-p" v-if="taskParam.toolType === '1'">目前仅支持cmake 2.8.x及以上版本</div>
                    <div class="bk-form-p" v-if="taskParam.toolType === '2'">目前仅支持blade官方发布版本</div>
                    <div class="bk-form-p" v-if="taskParam.toolType === '3'">目前仅支持bazel 0.18.1及以上版本</div>
                </div>
            </bk-form-item>
            <bk-form-item label="加速方案：">
                <p class="bk-form-text" style="width: 810px;">根据你的选择，为你推荐了以下加速方案</p>
                <schemes :program-list="programList" :accelerate-id="accelerateId" @schemeChange="schemeChange"></schemes>
                <p class="bk-form-text bk-form-text-small">其它编译器版本/构建工具需求请联系<a href="wxwork://message/?username=DevOps">蓝盾人工客服</a></p>
            </bk-form-item>
            <bk-form-item>
                <bk-button theme="primary" :disabled="isBtnDisabled" @click="registCommit">注册并下一步</bk-button>
                <bk-button theme="default" :disabled="isBtnDisabled" @click="$parent.registCancel">取消</bk-button>
            </bk-form-item>
        </bk-form>
    </section>
</template>

<script>
    import schemes from '@/components/turbo/schemes'
    import { mapGetters } from 'vuex'
    import buildOption from '@/components/buildOption'

    export default {
        components: {
            schemes
        },
        mixins: [buildOption],
        data () {
            return {
                projLang: ['1'],
                gccVersionList: '',
                clangVersionList: '',
                taskParam: {
                    machineType: '1',
                    banDistcc: 'false',
                    ccacheEnabled: 'true',
                    projType: '1',
                    osType: '1',
                    compilerType: '1',
                    toolType: '1'
                }
            }
        },
        computed: {
            ...mapGetters([
                'enableProjectList'
            ])
        },
        created () {
            this.getCompilerConfig()
        },
        methods: {
            schemeChange (scheme) {
                this.accelerateId = scheme.id
                Object.assign(this.taskParam, {
                    banDistcc: scheme.banDistcc,
                    ccacheEnabled: scheme.ccacheEnabled
                })
            },
            async registCommit () {
                const validate = await this.$validator.validateAll()
                if (!validate || this.errors.any()) {
                    return false
                }
                const { projectId, $store, projLang } = this
                const { taskName, machineType, osType, compilerType, toolType, projType, ccacheEnabled, banDistcc } = this.taskParam
                Object.assign(this.taskParam, {
                    bsProjectId: projectId
                })
                this.isBtnDisabled = true
                let projectName = ''
                this.enableProjectList.forEach(project => {
                    if (project.project_code === this.projectId) {
                        projectName = project.project_name
                    }
                })
                try {
                    const res = await $store.dispatch('turbo/commitRegist', {
                        params: Object.assign({
                            taskName: taskName,
                            machineType: machineType,
                            osType: osType,
                            compilerType: compilerType,
                            toolType: toolType,
                            banDistcc: banDistcc,
                            ccacheEnabled: ccacheEnabled,
                            bsProjectId: projectId,
                            projType: projType,
                            projLang: projLang.join(',')
                        }),
                        projectName: projectName
                    })
                    if (res) {
                        $store.commit('turbo/setRegister', {
                            taskName: taskName,
                            taskId: res.taskId,
                            banDistcc: banDistcc,
                            ccacheEnabled: ccacheEnabled,
                            machineType: machineType,
                            osType: osType,
                            compilerType: compilerType,
                            toolType: toolType
                        })
                        $store.commit('turbo/modifyProcessHead', {
                            process: machineType === '1' ? 'buildPublic' : (machineType === '2' ? 'buildThird' : 'buildInstall'),
                            current: 1
                        })
                    }
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                } finally {
                    this.isBtnDisabled = false
                }
            },
            async getCompilerConfig () {
                const { projectId, $store } = this
                try {
                    const res = await $store.dispatch('turbo/getCompilerConfig', {
                        projectId: projectId
                    })
                    if (res) {
                        for (let i = 0; i < res.length; i++) {
                            let versionList = ''
                            if (res[i]['paramCode'] === 'gcc') {
                                versionList = res[i]['paramExtend1'].replace(/;/g, '、')
                                this.gccVersionList = `目前仅支持${versionList}版本`
                            } else if (res[i]['paramCode'] === 'clang') {
                                versionList = res[i]['paramExtend1'].replace(/;/g, '、')
                                this.clangVersionList = `目前仅支持${versionList}版本`
                            }
                        }
                    }
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                }
            }
        }
    }
</script>

<style lang="scss">
    @import '../../assets/scss/conf';
    .registration-wrapper {
        .machine-type-item {
            display: inline-grid;
            .bk-form-radio {
                margin-top: 10px;
                &:first-child {
                    margin-top: 6px;
                }
            }
        }
        .compiler-type-item,
        .tool-type-item {
            .bk-form-radio {
                margin: 4px 30px 4px 0;
            }
        }
    }
</style>
