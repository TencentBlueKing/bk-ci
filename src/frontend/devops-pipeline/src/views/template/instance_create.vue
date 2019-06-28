<template>
    <div class="biz-container pipeline-subpages create-instance-wrapper"
        v-bkloading="{
            isLoading: loading.isLoading,
            title: loading.title
        }">
        <inner-header>
            <div class="instance-header" slot="left">
                <span class="inner-header-title" slot="left"><i class="bk-icon icon-angle-left" @click="toInstanceManage()"></i>模板实例化</span>
            </div>
        </inner-header>
        <div class="sub-view-port" v-if="showContent">
            <div class="template-information">
                <div class="template-introduction">
                    <!--<img src="@/scss/logo/pipeline.svg" class="template-logo">-->
                    <logo name="pipeline" class="template-logo"></logo>
                    <div class="template-name">{{ template.templateName }}</div>
                    <div class="template-creator"><span>创建人：</span>{{ template.creator }}</div>
                    <div class="template-brief">
                        <label>简介：</label>
                        <p>{{ template.description }}</p>
                    </div>
                </div>
                <div class="template-pipeline-conf">
                    <div class="template-version-handler">
                        <label class="conf-title">模板集版本</label>
                        <div class="select-row">
                            <bk-select
                                v-model="instanceVersion"
                                style="width: 320px"
                            >
                                <bk-option v-for="(option, oindex) in versionList" :key="oindex" :id="option.version" :name="option.versionName">
                                </bk-option>
                            </bk-select>
                            <label class="bk-form-checkbox template-setting-checkbox" v-if="!hashVal">
                                <bk-checkbox
                                    v-model="isTemplateSetting">
                                    同时应用模板设置
                                </bk-checkbox>
                                <bk-popover placement="top">
                                    <i class="bk-icon icon-info-circle"></i>
                                    <div slot="content" style="white-space: pre-wrap; min-width: 200px">
                                        <div>勾选则表示把模板的设置应用到实例化的流水线</div>
                                    </div>
                                </bk-popover>
                            </label>
                        </div>
                    </div>
                    <div class="cut-line"></div>
                    <div class="instance-pipeline">
                        <label class="conf-title">实例化流水线名称</label>
                        <div class="pipeline-name-box">
                            <div :class="{ &quot;pipeline-item&quot;: true, &quot;active-item&quot;: entry.selected, &quot;unselect-hover&quot;: !entry.selected }"
                                v-for="(entry, index) in pipelineNameList" :key="index" @click="lastCilckPipeline(index)">{{ entry.pipelineName }}
                                <i class="delete-btn" v-if="!hashVal" @click="deletePipelineName(index)"></i>
                            </div>
                            <div class="pipeline-item add-item" @click="addPipelineName()" v-if="!hashVal">
                                <i class="plus-icon"></i>
                                <span>新建流水线实例</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- <div class="pipeline-instance-conf" v-if="pipelineNameList.length && (buildParams.buildNo || paramList)">
                <div class="info-title"><span>{{ currentPipelineParams.pipelineName }}</span>的详细配置</div>
                <div v-if='currentPipelineParams.buildParams.buildNoType'>
                    <div class='params-flex-col' ref='buildForm'>
                        <form-field :required='true' label='构建号'>
                            <vuex-input :disabled='disabled' inputType='number' name='buildNo' placeholder='BuildNo' v-validate.initial='"required|numeric"' :value='currentPipelineParams.buildParams.buildNo' :handleChange='handleBuildNoChange' />
                            <p v-if="errors.has('buildNo')" :class="errors.has('buildNo') ? 'error-tips' : 'normal-tips'">构建号不能为空</p>
                        </form-field>
                        <form-field class='flex-colspan-2' :required='true' :is-error='errors.has("buildNoType")' :errorMsg='errors.first("buildNoType")'>
                            <enum-input :list='buildNoRules' :disabled='disabled' name='buildNoType' v-validate.initial='"required|string"' :value='currentPipelineParams.buildParams.buildNoType' :handleChange='handleBuildNoChange' />
                        </form-field>
                    </div>
                </div>
                <div class="pipeline-params-content">
                    <pipeline-params-form ref='paramsForm'
                        :paramValues='currentPipelineParams.paramValues'
                        :handleParamChange='handleParamChange'
                        :params='currentPipelineParams.params'>
                    </pipeline-params-form>
                </div>
            </div> -->

            <div class="pipeline-instance-conf" v-if="pipelineNameList.length">
                <section v-for="(param, index) in pipelineNameList" :key="index">
                    <template v-if="param.pipelineName === currentPipelineParams.pipelineName">
                        <section class="params-item" v-if="param.buildParams">
                            <div class="info-title"><span>{{ currentPipelineParams.pipelineName }}</span>的版本号设置</div>
                            <div v-if="param.buildParams" class="build-params-content">
                                <div class="buildNo-params-content">
                                    <pipeline-params-form
                                        :ref="`paramsForm${index}`"
                                        :param-values="param.paramValues"
                                        :handle-param-change="handleParamChange"
                                        :params="param.versionParams">
                                    </pipeline-params-form>
                                </div>
                                <div class="params-flex-col" ref="buildForm">
                                    <form-field :required="true" label="构建号">
                                        <vuex-input :disabled="disabled" input-type="number" name="buildNo" placeholder="BuildNo" v-validate.initial="&quot;required|numeric&quot;" :value="param.buildParams.buildNo" :handle-change="handleBuildNoChange" />
                                        <p v-if="errors.has('buildNo')" :class="errors.has('buildNo') ? 'error-tips' : 'normal-tips'">构建号不能为空</p>
                                    </form-field>
                                    <form-field class="flex-colspan-2" :required="true" :is-error="errors.has(&quot;buildNoType&quot;)" :error-msg="errors.first(&quot;buildNoType&quot;)">
                                        <enum-input :list="buildNoRules" :disabled="disabled" name="buildNoType" v-validate.initial="&quot;required|string&quot;" :value="param.buildParams.buildNoType" :handle-change="handleBuildNoChange" />
                                    </form-field>
                                </div>
                            </div>
                        </section>
                        <section class="params-item" v-if="param.params && param.params.filter(item => buildNoParams.indexOf(item.id) === -1 ).length">
                            <div class="info-title"><span>{{ currentPipelineParams.pipelineName }}</span>的流水线变量</div>
                            <div class="pipeline-params-content">
                                <pipeline-params-form
                                    :ref="`paramsForm${index}`"
                                    :param-values="param.paramValues"
                                    :handle-param-change="handleParamChange"
                                    :params="param.pipelineParams">
                                </pipeline-params-form>
                            </div>
                        </section>
                        <section class="params-item" v-if="templateParamList.length">
                            <div class="info-title"><span>{{ currentPipelineParams.pipelineName }}</span>的模板常量</div>
                            <div class="pipeline-params-content template-params-content">
                                <pipeline-params-form
                                    :disabled="true"
                                    :ref="`paramsForm${index}`"
                                    :param-values="templateParamValues"
                                    :params="templateParamList">
                                </pipeline-params-form>
                            </div>
                        </section>
                    </template>
                </section>
            </div>
            <div class="create-instance-footer">
                <bk-button theme="primary" size="normal" @click="submit()"><span>实例化</span></bk-button>
                <span class="cancel-btn" @click="toInstanceManage()">取消</span>
            </div>
        </div>
        <instance-pipeline-name :show-instance-create="showInstanceCreate"
            @comfire="comfireHandler"
            @cancel="cancelHandler"></instance-pipeline-name>
        <instance-message :show-instance-message="showInstanceMessage"
            :success-list="successList"
            :fail-list="failList"
            :fail-message="failMessage"
            @cancel="cancelMessage"></instance-message>
    </div>
</template>

<script>
    import { mapGetters } from 'vuex'
    import innerHeader from '@/components/devops/inner_header'
    import PipelineParamsForm from '@/components/pipelineParamsForm.vue'
    import instancePipelineName from '@/components/template/instance-pipeline-name.vue'
    import instanceMessage from '@/components/template/instance-message.vue'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import EnumInput from '@/components/atomFormField/EnumInput'
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import Logo from '@/components/Logo'

    export default {
        components: {
            'inner-header': innerHeader,
            PipelineParamsForm,
            instancePipelineName,
            instanceMessage,
            VuexInput,
            FormField,
            EnumInput,
            Logo
        },
        data () {
            return {
                instanceVersion: '',
                isInit: false,
                showContent: false,
                isTemplateSetting: false,
                showInstanceCreate: false,
                showInstanceMessage: false,
                paramList: [],
                templateParamList: [],
                versionList: [],
                pipelineNameList: [],
                currentPipelineParams: [],
                successList: [],
                failList: [],
                failMessage: {},
                buildNoParams: [
                    'MajorVersion',
                    'MinorVersion',
                    'FixVersion'
                ],
                loading: {
                    isLoading: false,
                    title: ''
                },
                template: {},
                buildParams: {},
                paramValues: {},
                templateParamValues: {}
            }
        },
        computed: {
            ...mapGetters('atom', [
                'buildNoRules'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            },
            templateId () {
                return this.$route.params.templateId
            },
            curVersionId () {
                return this.$route.params.curVersionId
            },
            hashVal () {
                if (this.$route.hash) {
                    const hashVal = this.$route.hash.substr(1, this.$route.hash.length)
                    const pipeline = hashVal.split('&')
                    return pipeline
                }
                return ''
            }
        },
        watch: {
            instanceVersion (newVal) {
                if (newVal && !this.isInit) {
                    this.requestTemplateDatail(newVal)
                    if (this.hashVal) {
                        this.requestPipelineParams(this.hashVal, newVal)
                    }
                }
            }
        },
        async mounted () {
            this.requestTemplateDatail(this.curVersionId)
            if (this.hashVal) {
                this.requestPipelineParams(this.hashVal, this.curVersionId)
            }
        },
        methods: {
            async requestTemplateDatail (versionId) {
                const { $store, loading } = this

                this.isInit = true
                loading.isLoading = true

                try {
                    const res = await $store.dispatch('pipelines/requestTemplateDatail', {
                        projectId: this.projectId,
                        templateId: this.templateId,
                        versionId: versionId
                    })
                    this.template.templateName = res.templateName
                    this.template.creator = res.creator
                    this.template.description = res.description
                    this.versionList = res.versions
                    this.handleParams(res.template.stages)
                    this.handlePipeLineName()

                    const curVersion = this.versionList.filter(val => {
                        return val.version === parseInt(versionId)
                    })
                    this.instanceVersion = curVersion[0].version
                } catch (err) {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                } finally {
                    this.loading.isLoading = false
                    this.showContent = true
                }
            },
            async requestPipelineParams (pipeline, versionId) {
                const { $store, loading } = this
                const params = []

                pipeline.map(item => {
                    params.push({
                        id: item
                    })
                })

                loading.isLoading = true

                try {
                    const res = await $store.dispatch('pipelines/requestPipelineParams', {
                        projectId: this.projectId,
                        templateId: this.templateId,
                        versionId: versionId,
                        params
                    })
                    this.handlePipelineParams(res)
                } catch (err) {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                } finally {
                    this.loading.isLoading = false
                }
            },
            handleParams (stages) {
                this.paramList = stages[0].containers[0].params || []
                this.paramValues = this.paramList.reduce((values, param) => {
                    values[param.id] = param.defaultValue
                    return values
                }, {})
                this.templateParamList = stages[0].containers[0].templateParams || []
                this.templateParamValues = this.templateParamList.reduce((values, param) => {
                    values[param.id] = param.defaultValue
                    return values
                }, {})
                if (stages[0].containers[0].buildNo) {
                    this.buildParams = stages[0].containers[0].buildNo
                } else {
                    this.buildParams = {}
                }

                if (!this.hashVal) {
                    this.pipelineNameList.forEach(item => {
                        item.params = [].concat(this.deepCopy(this.paramList))
                        item.pipelineParams = item.params.filter(item => this.buildNoParams.indexOf(item.id) === -1)
                        item.versionParams = item.params.filter(item => this.buildNoParams.indexOf(item.id) > -1)
                        item.paramValues = this.deepCopy(this.paramValues)
                        item.buildParams = this.buildParams && this.buildParams.buildNoType ? this.deepCopy(this.buildParams) : false
                    })
                }
            },

            /**
             * 初次进来的时候，如果有需要实例化流水线的名字，就带上
             */
            handlePipeLineName () {
                const params = this.$route.params || {}
                const name = params.pipelineName
                if (name) this.comfireHandler(name)
            },

            handlePipelineParams (data) {
                this.pipelineNameList.splice(0, this.pipelineNameList.length)
                this.hashVal.map((item, index) => {
                    const pipelineItem = {
                        pipelineId: data[item].pipelineId,
                        pipelineName: data[item].pipelineName,
                        selected: index === 0
                    }
                    if (data[item].buildNo) {
                        pipelineItem.buildParams = data[item].buildNo
                    }
                    if (data[item].param.length) {
                        const paramValues = data[item].param.reduce((values, param) => {
                            values[param.id] = param.defaultValue
                            return values
                        }, {})
                        pipelineItem.params = [].concat(this.deepCopy(data[item].param))
                        pipelineItem.pipelineParams = pipelineItem.params.filter(item => this.buildNoParams.indexOf(item.id) === -1)
                        pipelineItem.versionParams = pipelineItem.params.filter(item => this.buildNoParams.indexOf(item.id) > -1)
                        pipelineItem.paramValues = paramValues
                    }
                    this.pipelineNameList.push(pipelineItem)
                })
                this.currentPipelineParams = this.pipelineNameList[0]
            },
            toInstanceManage () {
                this.$router.back()
            },
            changeVersion (data) {
                this.isInit = false
            },
            addPipelineName () {
                this.showInstanceCreate = true
            },
            handleParamChange (name, value) {
                this.pipelineNameList.forEach(item => {
                    if (item.pipelineName === this.currentPipelineParams.pipelineName) {
                        item.paramValues[name] = value
                        item.params.forEach(val => {
                            if (val.id === name) {
                                val.defaultValue = value
                            }
                        })
                    }
                })
            },
            handleBuildNoChange (name, value) {
                this.pipelineNameList.forEach(item => {
                    if (item.pipelineName === this.currentPipelineParams.pipelineName) {
                        item.buildParams[name] = value
                    }
                })
            },
            lastCilckPipeline (key) {
                this.pipelineNameList.forEach((item, index) => {
                    item.selected = index === key
                    if (index === key) {
                        this.currentPipelineParams = item
                    }
                })
            },
            deletePipelineName (key) {
                this.pipelineNameList.splice(key, 1)
            },
            comfireHandler (data) {
                const tmpParam = [].concat(this.deepCopy(this.paramList))
                const pipelineParams = tmpParam.filter(item => this.buildNoParams.indexOf(item.id) === -1)
                const versionParams = tmpParam.filter(item => this.buildNoParams.indexOf(item.id) > -1)

                const newPipeline = {
                    pipelineName: data,
                    selected: true,
                    params: tmpParam,
                    pipelineParams,
                    versionParams,
                    buildParams: this.buildParams && this.buildParams.buildNoType ? this.deepCopy(this.buildParams) : false,
                    paramValues: this.deepCopy(this.paramValues)
                }

                this.pipelineNameList.push(newPipeline)
                this.currentPipelineParams = newPipeline
                this.pipelineNameList.forEach((item, index) => {
                    if (index !== (this.pipelineNameList.length - 1)) {
                        item.selected = false
                    }
                })
                this.cancelHandler()
            },
            cancelHandler () {
                this.showInstanceCreate = false
            },
            cancelMessage () {
                this.showInstanceMessage = false
            },
            // 对象深拷贝
            deepCopy (value, target) {
                return JSON.parse(JSON.stringify(value))
                // var target = target || {}
                // for (let i in value) {
                //     if (typeof value[i] === 'object') {
                //         target[i] = (value[i].constructor === Array) ? [] : {}
                //         this.deepCopy(value[i], target[i])
                //     } else {
                //         target[i] = value[i]
                //     }
                // }
                // return target
            },
            async submit () {
                if (!this.pipelineNameList.length) {
                    this.$showTips({
                        message: '请添加流水线实例名称',
                        theme: 'error'
                    })
                } else {
                    const params = []
                    let message, theme
                    const { $store, loading } = this

                    this.pipelineNameList.forEach(pipeline => {
                        params.push({
                            pipelineName: pipeline.pipelineName,
                            pipelineId: this.hashVal ? pipeline.pipelineId : undefined,
                            buildNo: pipeline.buildParams || undefined,
                            param: pipeline.params
                        })
                    })

                    loading.isLoading = true

                    try {
                        let res
                        const payload = {
                            projectId: this.projectId,
                            templateId: this.templateId,
                            versionId: this.instanceVersion,
                            useTemplateSettings: !this.hashVal ? this.isTemplateSetting : undefined,
                            params
                        }
                        if (this.hashVal) {
                            res = await $store.dispatch('pipelines/updateTemplateInstance', payload)
                        } else {
                            res = await $store.dispatch('pipelines/createTemplateInstance', payload)
                        }

                        if (res) {
                            const successCount = res.successPipelines.length
                            const failCount = res.failurePipelines.length

                            if (successCount && !failCount) {
                                message = `你已成功实例化${successCount}条流水线`
                                theme = 'success'

                                this.$showTips({
                                    message: message,
                                    theme: theme
                                })
                                this.toInstanceManage()
                            } else if (failCount) {
                                this.successList = res.successPipelines || []
                                this.failList = res.failurePipelines || []
                                this.failMessage = res.failureMessages || []
                                this.showInstanceMessage = true
                            }
                        }
                    } catch (err) {
                        message = err.message || err
                        theme = 'error'

                        this.$showTips({
                            message: message,
                            theme: theme
                        })
                    } finally {
                        this.loading.isLoading = false
                    }
                }
            }
        }
    }
</script>

<style lang="scss">
    @import './../../scss/conf';

    .pipeline-subpages {
        min-height: 100%;
        .bk-exception {
            position: absolute;
        }
    }
    .create-instance-wrapper {
        flex-direction: column;
        .instance-header {
            .icon-angle-left {
                position: relative;
                top: 2px;
                margin-right: 6px;
                color: $primaryColor;
                cursor: pointer;
            }
        }
        .sub-view-port {
            padding: 30px;
            height: calc(100% - 60px);
            overflow: auto;
        }
        .template-information {
            display: flex;
            min-height: 250px;
            border: 1px solid #C3CDD7;
            background-color: #fff;
        }
        .template-introduction {
            width: 400px;
            padding: 24px;
            border-right: 1px solid #C3CDD7;
            text-align: center;
            .template-logo {
                width: 105px;
                height: 92px;
                margin-bottom: 10px;
                object-fit: cover;
            }
            .template-creator {
                margin-top: 10px;
                color: #C3CDD7;
                font-size: 12px;
            }
            .template-brief {
                display: flex;
                margin-top: 12px;
                font-size: 12px;
                label {
                    min-width: 40px;
                }
            }
            p {
                text-align: left;
            }
        }
        .template-pipeline-conf {
            width: calc(100% - 400px);
            text-align: left;
            .template-version-handler {
                padding: 16px 32px;
            }
            .conf-title {
                font-size: 12px;
                color: #333C48;
            }
            .select-row {
                display: flex;
                margin-top: 11px;
            }
            .bk-selector {
                width: 320px;
                .bk-selector-input,
                .bk-selector-list .text {
                    font-size: 12px;
                }
            }
            .template-setting-checkbox {
                position: relative;
                margin-left: 30px;
                margin-top: 8px;
            }
            .cut-line {
                width: 100%;
                height: 1px;
                margin-top: 10px;
                border-top: 1px solid #C3CDD7;
            }
            .instance-pipeline {
                padding: 14px 32px;
                .pipeline-name-box {
                    margin-top: 12px;
                }
                .pipeline-item {
                    float: left;
                    position: relative;
                    height: 36px;
                    line-height: 36px;
                    margin-bottom: 12px;
                    margin-right: 20px;
                    padding: 0 32px 0 18px;
                    background-color: #fff;
                    border: 1px solid #c3cdd7;
                    // color: #fff;
                    font-size: 14px;
                    cursor: pointer;
                }
                .delete-btn {
                    display: inline-block;
                    width: 10px;
                    height: 10px;
                    overflow: hidden;
                    position: absolute;
                    top: 14px;
                    right: 15px;
                    cursor: pointer;
                }
                .delete-btn::before,
                .delete-btn::after {
                    content: "";
                    position: absolute;
                    top: 50%;
                    left: 0;
                    margin-top: -1px;
                    background-color: #c4c4c4;
                    width: 100%;
                    height: 2px;
                }
                .delete-btn::before {
                    transform: rotate(45deg);
                }
                .delete-btn::after {
                    transform: rotate(-45deg);
                }
                .active-item {
                    background-color: $primaryColor;
                    color: #fff;
                    border: none;
                    .delete-btn::before,
                    .delete-btn::after {
                        background-color: #fff;
                    }
                }
                .unselect-hover {
                    &:hover {
                        border-color: $primaryColor;
                        .delete-btn::before,
                        .delete-btn::after {
                            background-color: $primaryColor;
                        }
                    }
                }
                .add-item {
                    padding: 0 18px;
                    border: 1px dotted #979797;
                    background-color: #fff;
                    color: #333C48;
                    .plus-icon {
                        display: inline-block;
                        background: #333C48;;
                        height: 8px;
                        position: relative;
                        margin-right: 4px;
                        width: 1px;
                        &:after {
                            background: #333C48;;
                            content: "";
                            height: 8px;
                            left: 0;
                            position: absolute;
                            top: 0;
                            width: 1px;
                            transform: rotateZ(90deg)
                        }
                    }
                }
            }
        }
        .pipeline-instance-conf {
            min-width: 930px;
            margin-top: 20px;
            .info-title {
                padding: 12px 0 14px 19px;
                background-color: #FAFBFD;
                border-bottom: 1px solid #EBF0F5;
                font-size: 14px;
                color: #333C48;
            }
            .dashed-line {
                width: 100%;
                height: 1px;
                border: 1px dashed #EBF0F5;
            }
            .pipeline-params-content {
                padding: 10px 20px 25px;
                background-color: #fff;
                .bk-form-item {
                    float: left;
                    margin-top: 20px;
                    width: 46%;
                    height: 40px;
                }
            }
            .template-params-content {
                padding-top: 0;
                padding-bottom: 40px;
            }
        }
        .params-item {
            margin-bottom: 20px;
            border: 1px solid #EBF0F5;
        }
        .build-params-content {
            padding-bottom: 20px;
            background: #fff;
        }
        .buildNo-params-content {
            min-width: 940px;
            padding: 20px 0 0;
            padding-left: 20px;
            .bk-form-item {
                float: left;
                width: 300px;
                margin-top: 20px;
                .bk-form-input {
                    width: 145px;
                }
            }
        }
        .params-flex-col {
            display: flex;
            padding: 0 40px;
            background: #fff;
            .bk-form-item {
                display: flex;
                margin-top: 20px;
                margin-right: 30px;
                font-size: 14px;
                .bk-label {
                    width: 108px;
                    margin-right: 22px;
                    line-height: 36px;
                    text-align: right;
                    font-weight: bold;
                }
                .bk-form-input {
                    width: 145px;
                }
                &:last-child {
                    min-width: 420px;
                }
            }
        }
        .create-instance-footer {
            margin-top: 20px;
            .bk-button,
            .cancel-btn {
                margin-right: 20px;
                font-size: 12px;
            }
            .cancel-btn {
                color: $primaryColor;
                cursor: pointer;
            }
        }
    }
</style>
