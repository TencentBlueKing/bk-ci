<template>
    <div class="biz-container pipeline-subpages create-instance-wrapper"
        v-bkloading="{
            isLoading: loading.isLoading,
            title: loading.title
        }">
        <inner-header>
            <div class="instance-header" slot="left">
                <span class="inner-header-title" slot="left"><i class="devops-icon icon-angle-left" @click="toInstanceManage()"></i>{{ $t('template.templateInstantiation') }}</span>
            </div>
        </inner-header>
        <div class="sub-view-port" v-if="showContent">
            <div class="template-information">
                <div class="template-introduction">
                    <logo name="pipeline" class="template-logo"></logo>
                    <div class="template-name">{{ template.templateName }}</div>
                    <div class="template-creator"><span>{{ $t('creator') }}：</span>{{ template.creator }}</div>
                    <div class="template-brief">
                        <label>{{ $t('desc') }}：</label>
                        <p>{{ template.description }}</p>
                    </div>
                </div>
                <div class="template-pipeline-conf">
                    <div class="template-version-handler">
                        <label class="conf-title">{{ $t('template.templateVersion') }}</label>
                        <div class="select-row">
                            <bk-select
                                v-model="instanceVersion"
                                @change="changeVersion"
                                style="width: 320px"
                            >
                                <bk-option v-for="option in versionList" :key="option.version" :id="option.version" :name="option.versionName">
                                </bk-option>
                            </bk-select>
                            <label class="bk-form-checkbox template-setting-checkbox">
                                <bk-checkbox
                                    v-model="isTemplateSetting">
                                    {{ $t('template.applyTemplateSetting') }}
                                </bk-checkbox>
                                <bk-popover placement="top">
                                    <i class="devops-icon icon-info-circle"></i>
                                    <div slot="content" style="white-space: pre-wrap; min-width: 200px">
                                        <div>{{ $t('template.applySettingTips') }}</div>
                                    </div>
                                </bk-popover>
                            </label>
                        </div>
                        <div v-if="!instanceVersion" class="error-tips">{{ $t('template.templateVersionErrTips') }}</div>
                    </div>
                    <div class="cut-line"></div>
                    <div class="instance-pipeline">
                        <label class="conf-title">{{ $t('template.newPipelineName') }}</label>
                        <div class="pipeline-name-box">
                            <div :class="{ 'pipeline-item': true, 'active-item': entry.selected, 'unselect-hover': !entry.selected }"
                                v-for="(entry, index) in pipelineNameList" :key="index" @click="lastCilckPipeline(index)">{{ entry.pipelineName }}
                                <i class="delete-btn" v-if="!hashVal" @click="deletePipelineName(index)"></i>
                            </div>
                            <div class="pipeline-item add-item" @click="addPipelineName()" v-if="!hashVal">
                                <i class="plus-icon"></i>
                                <span>{{ $t('template.addPipelineInstance') }}</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div class="pipeline-instance-conf" v-if="pipelineNameList.length && instanceVersion ">
                <section v-for="(param, index) in pipelineNameList" :key="index">
                    <template v-if="param.pipelineName === currentPipelineParams.pipelineName">
                        <section class="params-item" v-if="param.buildParams">
                            <div class="info-title"><span>{{ currentPipelineParams.pipelineName }}</span>：{{ $t('template.newPipelineName') }}</div>
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
                                    <form-field :required="true" :label="$t('buildNum')">
                                        <vuex-input :disabled="disabled" input-type="number" name="buildNo" placeholder="BuildNo" v-validate.initial="'required|numeric'" :value="param.buildParams.buildNo" :handle-change="handleBuildNoChange" />
                                        <p v-if="errors.has('buildNo')" :class="errors.has('buildNo') ? 'error-tips' : 'normal-tips'">{{ $t('template.buildNumErrTips') }}</p>
                                    </form-field>
                                    <form-field class="flex-colspan-2" :required="true" :is-error="errors.has('buildNoType')" :error-msg="errors.first('buildNoType')">
                                        <enum-input :list="buildNoRules" :disabled="disabled" name="buildNoType" v-validate.initial="'required|string'" :value="param.buildParams.buildNoType" :handle-change="handleBuildNoChange" />
                                    </form-field>
                                </div>
                            </div>
                        </section>
                        <section class="params-item" v-if="param.params && param.params.filter(item => buildNoParams.indexOf(item.id) === -1 ).length">
                            <div class="info-title"><span>{{ currentPipelineParams.pipelineName }}</span>：{{ $t('template.pipelineVar') }}</div>
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
                            <div class="info-title"><span>{{ currentPipelineParams.pipelineName }}</span>：{{ $t('template.templateConst') }}</div>
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
                <bk-button theme="primary" size="normal" @click="submit()"><span>{{ $t('template.instantiate') }}</span></bk-button>
                <span class="cancel-btn" @click="toInstanceManage()">{{ $t('cancel') }}</span>
            </div>
        </div>
        <instance-pipeline-name :show-instance-create="showInstanceCreate"
            @comfire="comfireHandler"
            @cancel="cancelHandler"></instance-pipeline-name>
        <instance-message :show-instance-message="showInstanceMessage"
            :success-list="successList"
            :fail-list="failList"
            :fail-message="failMessage"
            @cancel="cancelMessage">
        </instance-message>
        <bk-dialog v-model="showUpdateDialog"
            :close-icon="false"
            header-position="left"
            :title="$t('template.updateDialogTitle')">
            <div style="padding: 10px 0px 20px">{{ $t('template.updateDialogContent') }}</div>
            <div slot="footer" class="container-footer">
                <div class="footer-wrapper">
                    <bk-button theme="primary" @click="toInstanceManage(true)">
                        {{ $t('confirm') }}
                    </bk-button>
                </div>
            </div>
        </bk-dialog>
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
    import { allVersionKeyList } from '@/utils/pipelineConst'

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
                    ...allVersionKeyList
                ],
                loading: {
                    isLoading: false,
                    title: ''
                },
                template: {},
                buildParams: {},
                paramValues: {},
                templateParamValues: {},
                showUpdateDialog: false
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
        async mounted () {
            this.requestTemplateDatail(this.curVersionId)
            this.handlePipeLineName()
            if (this.hashVal) {
                this.requestPipelineParams(this.hashVal, this.curVersionId)
            }
            if (this.$route.query.useTemplateSettings === 'true') {
                this.isTemplateSetting = true
            }
        },
        methods: {
            async requestTemplateDatail (versionId) {
                const { $store, loading } = this
                loading.isLoading = true

                try {
                    const res = await $store.dispatch('pipelines/requestTemplateDatail', {
                        projectId: this.projectId,
                        templateId: this.templateId,
                        versionId: versionId
                    })
                    if (res && res.template && res.template.tips) {
                        this.$showTips({
                            message: res.template.tips,
                            theme: 'error'
                        })
                    }
                    this.template.templateName = res.templateName
                    this.template.creator = res.creator
                    this.template.description = res.description
                    this.versionList = res.versions
                    this.handleParams(res.template.stages)
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

                pipeline.forEach(item => {
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
                this.hashVal.forEach((item, index) => {
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
            toInstanceManage (isUpdate = false) {
                const route = {
                    name: 'templateInstance',
                    params: {
                        projectId: this.projectId,
                        pipelineId: this.pipelineId
                    }
                }
                if (isUpdate) {
                    route.query = this.$route.query
                }
                this.$router.push(route)
            },
            changeVersion (newVal) {
                this.requestTemplateDatail(newVal)
                if (this.hashVal && newVal) this.requestPipelineParams(this.hashVal, newVal)
            },
            addPipelineName () {
                if (!this.instanceVersion) {
                    this.$showTips({
                        message: this.$t('template.templateVersionErrTips'),
                        theme: 'error'
                    })
                } else {
                    this.showInstanceCreate = true
                }
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
            },
            async submit () {
                if (!this.pipelineNameList.length) {
                    this.$showTips({
                        message: this.$t('template.submitErrTips'),
                        theme: 'error'
                    })
                } else if (!this.instanceVersion) {
                    this.$showTips({
                        message: this.$t('template.templateVersionErrTips'),
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
                            useTemplateSettings: this.isTemplateSetting,
                            params
                        }
                        if (this.hashVal) {
                            res = await $store.dispatch('pipelines/updateTemplateInstance', payload)
                            if (res) {
                                this.showUpdateDialog = true
                            }
                        } else {
                            res = await $store.dispatch('pipelines/createTemplateInstance', payload)
                            if (res) {
                                const successCount = res.successPipelines.length
                                const failCount = res.failurePipelines.length

                                if (successCount && !failCount) {
                                    message = this.$t('template.submitSucTips', [successCount])
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
    @import '@/scss/mixins/ellipsis';

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
                .bk-form {
                    display: flex;
                    flex-wrap: wrap;
                    justify-content: space-between;
                    padding-top: 10px;
                }
                .bk-form-content {
                    position: relative;
                    float: left;
                    margin-left: 0;
                    width: 100%;
                }
                .bk-form-item {
                    margin-top: 0;
                    width: 48%;
                }
                .bk-label {
                    width: 100%;
                    text-align: left;
                    @include ellipsis();
                }
                .bk-form .bk-form-item:before, .bk-form:after {
                    display: none;
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
                width: 320px;
                margin-top: 20px;
                margin-left: 10px;
                .bk-label {
                    width: 160px;
                }
                .bk-form-input {
                    width: 145px !important;
                }
            }
        }
        .params-flex-col {
            display: flex;
            padding: 0 40px;
            background: #fff;
            .bk-form-item {
                display: flex;
                margin: 20px 30px 0 20px;
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
