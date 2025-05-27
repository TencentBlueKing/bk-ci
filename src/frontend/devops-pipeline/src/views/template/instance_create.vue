<template>
    <div
        class="biz-container pipeline-subpages create-instance-wrapper"
        v-bkloading="{
            isLoading: loading.isLoading,
            title: loading.title
        }"
    >
        <inner-header>
            <div
                class="instance-header"
                slot="left"
            >
                <span
                    class="inner-header-title"
                    slot="left"
                ><i
                    class="devops-icon icon-angle-left"
                    @click="toInstanceManage()"
                ></i>{{ $t('template.templateInstantiation') }}</span>
            </div>
        </inner-header>
        <alert-tips
            v-if="enablePipelineNameTips"
            :title="$t('pipelineNameConventions')"
            :message="pipelineNameFormat"
        />
        <div
            class="sub-view-port"
            v-if="showContent"
        >
            <div class="template-information">
                <div class="template-introduction">
                    <logo
                        name="pipeline"
                        class="template-logo"
                    ></logo>
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
                                <bk-option
                                    v-for="option in versionList"
                                    :key="option.version"
                                    :id="option.version"
                                    :name="option.versionName"
                                >
                                </bk-option>
                            </bk-select>
                            <label class="bk-form-checkbox template-setting-checkbox">
                                <bk-checkbox
                                    v-model="isTemplateSetting"
                                >
                                    {{ $t('template.applyTemplateSetting') }}
                                </bk-checkbox>
                                <bk-popover placement="top">
                                    <i class="devops-icon icon-info-circle"></i>
                                    <div
                                        slot="content"
                                        style="white-space: pre-wrap; min-width: 200px"
                                    >
                                        <div>{{ $t('template.applySettingTips') }}</div>
                                    </div>
                                </bk-popover>
                            </label>
                        </div>
                        <div
                            v-if="!instanceVersion"
                            class="error-tips"
                        >
                            {{ $t('template.templateVersionErrTips') }}
                        </div>
                    </div>
                    <div class="cut-line"></div>
                    <div class="instance-pipeline">
                        <label class="conf-title">{{ $t('template.newPipelineName') }}</label>
                        <div class="pipeline-name-box">
                            <div
                                :class="{
                                    'pipeline-item': !entry.isEditing,
                                    'active-item': entry.selected && !entry.isEditing,
                                    'unselect-hover': !entry.selected,
                                    'edit-item': entry.isEditing
                                }"
                                v-for="(entry, index) in pipelineNameList"
                                :key="index"
                                @click="lastClickPipeline(index)"
                            >
                                <div v-show="entry.isEditing">
                                    <bk-input
                                        ref="pipelineNameInput"
                                        class="pipeline-name-input"
                                        v-model="displayName"
                                        :maxlength="128"
                                        :placeholder="$t('pipelineNameInputTips')"
                                    />
                                    <div class="edit-tools">
                                        <i
                                            class="devops-icon icon-check-1 group-card-edit-icon mr10"
                                            @click="handelSavePipelineName(index)"
                                            v-bk-tooltips="$t('save')"
                                        />
                                        <i
                                            class="devops-icon icon-close group-card-edit-icon"
                                            @click="handelCancelSave(index)"
                                            v-bk-tooltips="$t('cancel')"
                                        />
                                    </div>
                                </div>
                                <div v-show="!entry.isEditing">
                                    <div
                                        class="pipeline-name"
                                        v-bk-overflow-tips
                                    >
                                        {{ entry.pipelineName }}
                                    </div>
                                    <bk-icon
                                        class="edit-btn"
                                        type="edit"
                                        size="12"
                                        @click="handleChangePipelineName(index)"
                                        v-bk-tooltips="$t('rename')"
                                    />
                                    <i
                                        class="delete-btn"
                                        v-if="!hashVal && !isCopyInstance"
                                        @click="deletePipelineName(index)"
                                        v-bk-tooltips="$t('delete')"
                                    />
                                </div>
                            </div>
                            <div
                                class="pipeline-item add-item"
                                @click="addPipelineName"
                                v-if="!hashVal && !isCopyInstance"
                            >
                                <i class="plus-icon"></i>
                                <span>{{ $t('template.addPipelineInstance') }}</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div
                class="pipeline-instance-conf"
                v-if="pipelineNameList.length && instanceVersion "
            >
                <section
                    v-for="(param, index) in pipelineNameList"
                    :key="index"
                >
                    <template v-if="param.pipelineName === currentPipelineParams.pipelineName">
                        <section
                            class="params-item"
                            v-if="param.buildParams"
                        >
                            <div class="info-title"><span>{{ currentPipelineParams.pipelineName }}</span>：{{ $t('versionNum') }}</div>
                            <div
                                v-if="param.buildParams"
                                class="build-params-content"
                            >
                                <pipeline-versions-form
                                    :ref="`paramsForm${index}`"
                                    :build-no="param.buildParams"
                                    :disabled="disabled"
                                    :is-instance="true"
                                    :is-init-instance="!hashVal"
                                    :reset-build-no="param.resetBuildNo"
                                    :version-param-values="param.paramValues"
                                    :handle-version-change="handleParamChange"
                                    :handle-build-no-change="handleBuildNoChange"
                                    :handle-check-change="handleCheckChange"
                                ></pipeline-versions-form>
                            </div>
                        </section>
                        <section
                            class="params-item"
                            v-if="param.params && param.params.filter(item => buildNoParams.indexOf(item.id) === -1 ).length"
                        >
                            <div class="info-title"><span>{{ currentPipelineParams.pipelineName }}</span>：{{ $t('template.pipelineVar') }}</div>
                            <div class="pipeline-params-content">
                                <pipeline-params-form
                                    :ref="`paramsForm${index}`"
                                    :param-values="param.paramValues"
                                    :handle-param-change="handleParamChange"
                                    :params="param.pipelineParams"
                                >
                                </pipeline-params-form>
                            </div>
                        </section>
                        <section
                            class="params-item"
                            v-if="templateParamList.length"
                        >
                            <div class="info-title"><span>{{ currentPipelineParams.pipelineName }}</span>：{{ $t('template.templateConst') }}</div>
                            <div class="pipeline-params-content template-params-content">
                                <pipeline-params-form
                                    :disabled="true"
                                    :ref="`paramsForm${index}`"
                                    :param-values="templateParamValues"
                                    :params="templateParamList"
                                >
                                </pipeline-params-form>
                            </div>
                        </section>
                    </template>
                </section>
            </div>
            <div class="create-instance-footer">
                <bk-button
                    theme="primary"
                    size="normal"
                    @click="submit()"
                >
                    <span>{{ $t('template.instantiate') }}</span>
                </bk-button>
                <span
                    class="cancel-btn"
                    @click="toInstanceManage()"
                >{{ $t('cancel') }}</span>
            </div>
        </div>
        <instance-pipeline-name
            :show-instance-create="showInstanceCreate"
            @confirm="confirmHandler"
            @cancel="cancelHandler"
        >
        </instance-pipeline-name>
        <instance-message
            :show-instance-message="showInstanceMessage"
            :success-list="successList"
            :fail-list="failList"
            :fail-message="failMessage"
            @cancel="cancelMessage"
        >
        </instance-message>
        <bk-dialog
            v-model="showUpdateDialog"
            :close-icon="false"
            header-position="left"
            :title="$t('template.updateDialogTitle')"
        >
            <div style="padding: 10px 0px 20px">{{ $t('template.updateDialogContent') }}</div>
            <div
                slot="footer"
                class="container-footer"
            >
                <div class="footer-wrapper">
                    <bk-button
                        theme="primary"
                        @click="toInstanceManage(true)"
                    >
                        {{ $t('confirm') }}
                    </bk-button>
                </div>
            </div>
        </bk-dialog>
    </div>
</template>

<script>
    import Logo from '@/components/Logo'
    import PipelineVersionsForm from '@/components/PipelineVersionsForm.vue'
    import innerHeader from '@/components/devops/inner_header'
    import PipelineParamsForm from '@/components/pipelineParamsForm.vue'
    import instanceMessage from '@/components/template/instance-message.vue'
    import instancePipelineName from '@/components/template/instance-pipeline-name.vue'
    import AlertTips from '@/components/AlertTips.vue'
    import { allVersionKeyList } from '@/utils/pipelineConst'
    import { mapGetters } from 'vuex'
    import { getParamsValuesMap, isObject } from '@/utils/util'
    import { isFileParam, isMultipleParam } from '@/store/modules/atom/paramsConfig'

    export default {
        components: {
            'inner-header': innerHeader,
            PipelineParamsForm,
            instancePipelineName,
            instanceMessage,
            Logo,
            PipelineVersionsForm,
            AlertTips
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
                showUpdateDialog: false,
                displayName: '',
                resetInstanceName: []
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
            type () {
                return this.$route.params.type
            },
            hashVal () {
                if (this.$route.hash) {
                    const hashVal = this.$route.hash.substr(1, this.$route.hash.length)
                    const pipeline = hashVal.split('&')
                    return pipeline
                }
                return ''
            },
            copyPipelineName () {
                return this.$route.params.pipelineName
            },
            queryPipelineId () {
                return this.$route.query?.pipelineId ?? ''
            },
            isCopyInstance () {
                return !!(this.copyPipelineName && this.queryPipelineId)
            },
            curProject () {
                return this.$store.state.curProject
            },
            enablePipelineNameTips () {
                return this.curProject?.properties?.enablePipelineNameTips ?? false
            },
            pipelineNameFormat () {
                return this.curProject?.properties?.pipelineNameFormat ?? ''
            }
        },
        async mounted () {
            this.requestTemplateDatail(this.curVersionId)
            if (this.$route.query.useTemplateSettings) {
                this.isTemplateSetting = true
            }
            if (this.curVersionId) {
                this.instanceVersion = this.curVersionId
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

                    if (this.hashVal || this.isCopyInstance) {
                        await this.requestPipelineParams(versionId)
                    } else {
                        this.handleParams(res.template.stages)
                    }
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
            async requestPipelineParams (versionId) {
                const { $store, loading } = this
                const pipelines = this.isCopyInstance ? [this.queryPipelineId] : this.hashVal
                loading.isLoading = true

                try {
                    const res = await $store.dispatch('pipelines/requestPipelineParams', {
                        projectId: this.projectId,
                        templateId: this.templateId,
                        versionId: versionId,
                        params: pipelines.map(id => ({
                            id
                        }))
                    })
                    this.handlePipelineParams(pipelines, res)
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
                this.paramValues = getParamsValuesMap(this.paramList)
                this.templateParamList = stages[0].containers[0].templateParams || []
                this.templateParamValues = getParamsValuesMap(this.templateParamList)
                if (stages[0].containers[0].buildNo) {
                    this.buildParams = stages[0].containers[0].buildNo
                } else {
                    this.buildParams = {}
                }

                if (!this.hashVal && !this.isCopyInstance) {
                    if (this.copyPipelineName) {
                        this.pipelineNameList.push({
                            pipelineName: this.copyPipelineName,
                            selected: true
                        })
                    }
                    this.pipelineNameList.forEach(item => {
                        item.params = this.deepCopyParams(this.paramList)
                        item.pipelineParams = item.params.filter(item => this.buildNoParams.indexOf(item.id) === -1)
                        item.versionParams = item.params.filter(item => this.buildNoParams.indexOf(item.id) > -1)
                        item.paramValues = this.deepCopy(this.paramValues)
                        item.buildParams = this.buildParams && this.buildParams.buildNoType ? this.deepCopy(this.buildParams) : false
                    })
                }
            },

            handlePipelineParams (pipelines, data) {
                this.pipelineNameList = pipelines.map((id, index) => {
                    const item = data[id]
                    const pipelineItem = {
                        pipelineId: id,
                        pipelineName: id === this.queryPipelineId ? this.copyPipelineName : item.pipelineName,
                        selected: index === 0
                    }
                    if (item.buildNo) {
                        pipelineItem.buildParams = item.buildNo
                        pipelineItem.resetBuildNo = false
                        pipelineItem.initBuildNo = item.buildNo.buildNo
                    }
                    if (item.param.length) {
                        pipelineItem.params = this.deepCopyParams(item.param)
                        const paramValues = getParamsValuesMap(pipelineItem.params)
                        pipelineItem.pipelineParams = pipelineItem.params.filter(sub => this.buildNoParams.indexOf(sub.id) === -1)
                        pipelineItem.versionParams = pipelineItem.params.filter(sub => this.buildNoParams.indexOf(sub.id) > -1)
                        pipelineItem.paramValues = paramValues
                    }
                    return pipelineItem
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
                if (newVal && newVal !== this.curVersionId) {
                    this.$router.push({
                        ...this.$route,
                        params: {
                            ...this.$route.params,
                            curVersionId: newVal
                        }
                    })
                    this.requestTemplateDatail(newVal)
                }
            },
            addPipelineName () {
                this.pipelineNameList.forEach(pipeline => {
                    this.$set(pipeline, 'isEditing', false)
                })
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
                        item.params.forEach((i) => {
                            if (i.id === name) {
                                if (isFileParam(i.type) && typeof value === 'object') {
                                    i.defaultValue = value.directory
                                    i.randomStringInPath = value.latestRandomStringInPath
                                } else {
                                    i.defaultValue = value
                                }
                            }
                        })
                    }
                })
            },
            handleBuildNoChange (name, value) {
                this.pipelineNameList.forEach(item => {
                    if (item.pipelineName === this.currentPipelineParams.pipelineName) {
                        item.buildParams[name] = value
                        if (this.hashVal || this.isCopyInstance) {
                            item.resetBuildNo = item.initBuildNo !== Number(value)
                        }
                    }
                })
            },
            handleCheckChange (value) {
                this.pipelineNameList.forEach(item => {
                    if (item.pipelineName === this.currentPipelineParams.pipelineName) {
                        item.resetBuildNo = value
                    }
                })
            },
            lastClickPipeline (key) {
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

            async handleChangePipelineName (index) {
                this.pipelineNameList.forEach((pipeline, i) => {
                    if (i === index) {
                        this.displayName = pipeline.pipelineName
                        this.$set(pipeline, 'isEditing', true)
                    } else {
                        this.$set(pipeline, 'isEditing', false)
                    }
                })
            },
            handelSavePipelineName (index) {
                this.$set(this.pipelineNameList[index], 'isEditing', false)
                this.$set(this.pipelineNameList[index], 'pipelineName', this.displayName)
            },
            handelCancelSave (index) {
                this.$set(this.pipelineNameList[index], 'isEditing', false)
            },
            confirmHandler (data) {
                const tmpParam = this.deepCopyParams(this.paramList)
                const pipelineParams = tmpParam.filter(item => this.buildNoParams.indexOf(item.id) === -1)
                const versionParams = tmpParam.filter(item => this.buildNoParams.indexOf(item.id) > -1)

                const newPipeline = {
                    pipelineName: data,
                    selected: true,
                    params: tmpParam,
                    pipelineParams,
                    versionParams,
                    buildParams: this.buildParams && this.buildParams.buildNoType ? this.deepCopy(this.buildParams) : false,
                    paramValues: getParamsValuesMap(pipelineParams)

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
            deepCopyParams (params) {
                return [].concat(this.deepCopy(params)).map(p => {
                    return {
                        ...p,
                        defaultValue: isMultipleParam(p.type) && Array.isArray(p.defaultValue) ? p.defaultValue.join(',') : p.defaultValue,
                        readOnly: false
                    }
                })
            },
            async handleInstance (params) {
                let message, theme
                const { $store, loading } = this

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
                    const h = this.$createElement
                    let isEmptyValue

                    this.pipelineNameList.forEach(pipeline => {
                        let buildNo
                        if (pipeline.buildParams && typeof pipeline.buildParams === 'object') {
                            const { currentBuildNo, ...buildParams } = pipeline.buildParams
                            buildNo = buildParams
                        }
                        params.push({
                            pipelineName: pipeline.pipelineName,
                            pipelineId: this.hashVal ? pipeline.pipelineId : undefined,
                            buildNo,
                            param: pipeline.params,
                            resetBuildNo: pipeline.resetBuildNo
                        })
                        isEmptyValue = pipeline?.params?.some(item => {
                            return isObject(item.defaultValue)
                                ? Object.values(item.defaultValue).every(val => !val)
                                : false
                        })
                    })
                    if (isEmptyValue) {
                        this.$showTips({
                            message: this.$t('newlist.paramsErr'),
                            theme: 'error'
                        })
                        return
                    }
                    const isRequired = params.some(item => item.buildNo && (typeof item.buildNo.buildNo === 'undefined' || item.buildNo.buildNo === ''))
                 
                    if (isRequired) {
                        this.$showTips({
                            message: this.$t('template.buildNumErrTips'),
                            theme: 'error'
                        })
                        return
                    }
                    this.resetInstanceName = params.filter(item => item.resetBuildNo).map(item => item.pipelineName)
  
                    if (this.resetInstanceName.length) {
                        this.$bkInfo({
                            width: 600,
                            type: 'warning',
                            title: this.$t('buildNoBaseline.forthcomingReset'),
                            okText: this.$t('buildNoBaseline.instanceConfirm'),
                            cancelText: this.$t('cancel'),
                            subHeader: h('div', { class: 'reset-content' }, [
                                h('div', { class: 'reset-pipeline-name' }, [
                                    h('p', { class: 'reset-info' }, this.$t('buildNoBaseline.forthcomingResetPipeline')),
                                    h('ul', { class: 'pipeline-list' }, this.resetInstanceName.map(
                                        name => h('li', `- ${name}`)
                                    ))
                                ]),
                                h('p', { class: 'reset-info' }, this.$t('buildNoBaseline.uncheck'))
                            ]),
                            confirmFn: () => {
                                this.handleInstance(params)
                            }
                        })
                    } else {
                        this.handleInstance(params)
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
                .edit-item {
                    position: relative;
                    float: left;
                    margin-bottom: 12px;
                    margin-right: 20px;
                }
                .edit-tools {
                    position: absolute;
                    top: 13px;
                    right: 20px;
                    font-size: 10px;
                }
                .group-card-edit-icon {
                    cursor: pointer;
                }
                .pipeline-name-input {
                    width: 200px;
                    margin-right: 10px;
                    input {
                        height: 36px;
                        line-height: 36px;
                        padding-right: 60px !important;
                    }
                }
                .pipeline-item {
                    float: left;
                    position: relative;
                    height: 36px;
                    line-height: 36px;
                    margin-bottom: 12px;
                    margin-right: 20px;
                    padding: 0 50px 0 18px;
                    background-color: #fff;
                    border: 1px solid #c3cdd7;
                    // color: #fff;
                    font-size: 14px;
                    cursor: pointer;
                    max-width: 200px;
                }
                .pipeline-name {
                    overflow: hidden;
                    white-space: nowrap;
                    text-overflow: ellipsis;
                }
                .edit-btn {
                    position: absolute;
                    top: 12px;
                    right: 30px;
                    cursor: pointer;
                }
                .delete-btn {
                    display: inline-block;
                    width: 14px;
                    height: 14px;
                    overflow: hidden;
                    position: absolute;
                    top: 12px;
                    right: 10px;
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
            padding: 20px 0 20px 20px;
            background: #fff;
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
    .reset-content {
        font-size: 14px;
        text-align: left;
        .reset-pipeline-name {
            width: 100%;
            padding: 14px;
            margin-bottom: 20px;
            border-radius: 2px;
            background-color: #f5f6fa;
            .reset-info {
                margin-bottom: 20px;
            }
            li {
                margin: 8px 0;
            }
        }

    }
</style>
