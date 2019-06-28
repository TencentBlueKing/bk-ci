<template>
    <div class="biz-container bkdevops-history-subpage pipeline-subpages">
        <inner-header class="customer-inner-header">
            <div class="history-bread-crumb" slot="left">
                <bread-crumb class="bread-crumb-comp" separator="/" :value="breadCrumbPath">
                    <template v-if="pipelineList && pipelineList.length">
                        <bread-crumb-item v-for="(crumb, index) in breadCrumbs" :key="index" v-bind="crumb">
                            <div class="build-num-switcher" v-if="$route.name === 'pipelinesDetail' && index === breadCrumbs.length - 1">
                                <template v-if="execDetail">
                                    <span>#{{ execDetail.buildNum }}</span>
                                    <p>
                                        <i class="bk-icon icon-angle-up" :disabled="execDetail.latestBuildNum === execDetail.buildNum || isLoading" @click="switchBuildNum(1)" />
                                        <i class="bk-icon icon-angle-down" :disabled="1 === execDetail.buildNum || isLoading" @click="switchBuildNum(-1)" />
                                    </p>
                                </template>
                            </div>
                        </bread-crumb-item>
                    </template>
                    <i v-else class="bk-icon icon-circle-2-1 spin-icon" />
                </bread-crumb>
            </div>
            <template v-if="$route.name === 'pipelinesPreview'" slot="right">
                <router-link :to="{ name: 'pipelinesEdit' }"><bk-button>编辑</bk-button></router-link>
                <bk-button :disabled="btnDisabled" :icon="executeStatus ? 'loading' : ''" theme="primary" @click="startExcuete">
                    执行
                </bk-button>
            </template>
            <template v-else slot="right">
                <bk-button v-if="$route.name === 'pipelinesEdit'" @click="save" :disabled="saveBtnDisabled" :icon="saveStatus ? 'loading' : ''" theme="primary">
                    保存
                </bk-button>
                <router-link v-else :to="{ name: 'pipelinesEdit' }"><bk-button>编辑</bk-button></router-link>
                <triggers
                    class="bkdevops-header-trigger-btn"
                    :pipeline-id="pipelineId"
                    :status="pipelineStatus"
                    :can-manual-startup="canManualStartup"
                    :before-exec="isEditing ? save : undefined"
                    @exec="toExecute">
                    <section slot="exec-bar" slot-scope="triggerProps">
                        <bk-button v-if="pipelineStatus !== 'running'" theme="primary" :disabled="btnDisabled || !canManualStartup || triggerProps.isDisable" :icon="executeStatus || triggerProps.isDisable ? 'loading' : ''" :title="canManualStartup ? '' : '不支持手动启动流水线'">
                            {{ isEditing ? '保存并执行' : '执行' }}
                        </bk-button>
                    </section>
                </triggers>

                <div :class="{ 'more-operation-entry': true, 'active': isDropmenuShow }">
                    <show-tooltip placement="bottom-end" content="这里可以将当前流水线保存为模板哦" key="more_operation" name="more_operation" style="z-index: 1">
                        <div class="entry-btn">
                            <i class="entry-circle" v-for="i in [1, 2, 3]" :key="i" />
                        </div>
                    </show-tooltip>
                    <div class="more-operation-dropmenu">
                        <ul>
                            <li @click="renamePipeline">重命名</li>
                            <li @click="toggleCollect">{{curPipeline.hasCollect ? '取消收藏' : '收藏'}}</li>
                        </ul>
                        <ul>
                            <li @click="copyPipeline">复制为</li>
                            <li @click="showTemplateDialog">另存为模板</li>
                            <!-- <li>导出</li> -->
                            <li @click="deletePipeline">删除</li>
                        </ul>
                    </div>
                </div>
            </template>
        </inner-header>
        <router-view class="biz-content" v-bkloading="{ isLoading }"></router-view>
        <portal-target name="artifactory-popup"></portal-target>

        <bk-dialog width="500" :loading="dialogConfig.loading" v-model="isDialogShow" :title="dialogConfig.title" @confirm="dialogConfig.handleDialogConfirm" @cancel="dialogConfig.handleDialogCancel">
            <bk-form :model="dialogConfig.formData" form-type="vertical" style="padding: 0 10px">
                <bk-form-item v-for="item in dialogConfig.formConfig" :label="item.label" :required="item.required" :rules="item.rules" :property="item.name" :key="item.name">
                    <bk-radio-group v-if="item.component === 'enum-input'" v-model="dialogConfig.formData[item.name]">
                        <bk-radio class="bkdevops-radio" :value="true">是</bk-radio>
                        <bk-radio class="bkdevops-radio" :value="false">否</bk-radio>
                    </bk-radio-group>
                    <component v-else :is="item.component" v-model="dialogConfig.formData[item.name]" v-bind="item.bindData"></component>
                </bk-form-item>
            </bk-form>
        </bk-dialog>
    </div>
</template>

<script>
    import { mapGetters, mapActions, mapState } from 'vuex'
    import BreadCrumb from '@/components/BreadCrumb'
    import BreadCrumbItem from '@/components/BreadCrumb/BreadCrumbItem'
    import innerHeader from '@/components/devops/inner_header'
    import triggers from '@/components/pipeline/triggers'
    import { bus } from '@/utils/bus'
    import pipelineOperateMixin from '@/mixins/pipeline-operate-mixin'
    import showTooltip from '@/components/common/showTooltip'

    export default {
        components: {
            innerHeader,
            triggers,
            BreadCrumb,
            showTooltip,
            BreadCrumbItem
        },
        mixins: [pipelineOperateMixin],
        data () {
            return {
                breadCrumbPath: [],
                isLoading: false,
                hasNoPermission: false,
                isDialogShow: false,
                dialogConfig: {
                    title: '',
                    loading: false,
                    formData: [],
                    formConfig: [],
                    handleDialogConfirm: () => {},
                    handleDialogCancel: () => {}
                },
                pipelineFormData: {
                    name: '',
                    desc: ''
                },
                templateFormData: {
                    isCopySetting: false,
                    templateName: ''
                }
            }
        },
        computed: {
            ...mapState('atom', [
                'execDetail',
                'editingElementPos',
                'isPropertyPanelVisible'
            ]),
            ...mapGetters({
                'isEditing': 'atom/isEditing',
                'getAllElements': 'atom/getAllElements'
            }),
            hasCodeccAtom () {
                if (this.execDetail && this.execDetail.model) {
                    return this.getAllElements(this.execDetail.model.stages).some(element => element['@type'] === 'linuxPaasCodeCCScript')
                }
                return false
            },
            templateFormConfig () {
                return [{
                    name: 'templateName',
                    label: '模板名称',
                    required: true,
                    rules: [],
                    component: 'bk-input',
                    bindData: {
                        placeholder: '请输入模板名称',
                        maxlength: 40
                    }
                }, {
                    name: 'isCopySetting',
                    label: '应用设置',
                    required: true,
                    rules: [],
                    component: 'enum-input'
                }]
            },
            renameFormConfig () {
                return [{
                    name: 'name',
                    label: '流水线名称',
                    required: true,
                    rules: [],
                    component: 'bk-input',
                    bindData: {
                        placeholder: '请输入流水线名称',
                        maxlength: 40
                    }
                }, {
                    name: 'desc',
                    label: '流水线描述',
                    rules: [],
                    component: 'bk-input',
                    bindData: {
                        placeholder: '请输入流水线描述'
                    }
                }]
            },
            btnDisabled () {
                return this.saveStatus || this.executeStatus
            },
            saveBtnDisabled () {
                return this.saveStatus || this.executeStatus || Object.keys(this.pipelineSetting).length === 0
            },
            canManualStartup () {
                return this.curPipeline ? this.curPipeline.canManualStartup : false
            },
            pipelineStatus () {
                return this.canManualStartup ? 'ready' : 'disable'
            },
            curItemTab () {
                return this.$route.params.type || 'executeDetail'
            },
            showRetryIcon () {
                return this.execDetail && (this.execDetail.latestVersion === this.execDetail.curVersion) && ['RUNNING', 'QUEUE', 'SUCCEED'].indexOf(this.execDetail.status) < 0
            },
            breadCrumbs () {
                return [{
                    icon: 'pipeline',
                    selectedValue: '流水线',
                    to: {
                        name: 'pipelinesList'
                    }
                }, {
                    paramId: 'pipelineId',
                    paramName: 'pipelineName',
                    selectedValue: this.pipelineId,
                    records: [
                        ...this.pipelineList
                    ],
                    showTips: true,
                    tipsName: 'switch_pipeline_hint',
                    tipsContent: '点击这里可以切换流水线哦',
                    to: this.$route.name === 'pipelinesHistory' ? null : {
                        name: 'pipelinesHistory'
                    },
                    handleSelected: this.handleSelected
                }, {
                    selectedValue: this.$route.meta.title
                }]
            }
        },
        watch: {
            pipelineId (newVal) {
                this.updateCurPipelineId(newVal)
            }
        },
        created () {
            this.fetchPipelineList()
            if (!this.longProjectId) {
                this.$store.dispatch('requestProjectDetail', { projectId: this.projectId })
            }
        },
        methods: {
            ...mapActions('pipelines', [
                'requestPipelinesList',
                'requestExecPipeline'
            ]),
            ...mapActions('atom', [
                'requestPipelineExecDetailByBuildNum'
            ]),
            handleSelected (pipelineId, cur) {
                const { projectId, $route } = this

                this.$store.commit('pipelines/updateCurPipeline', cur)

                const name = $route.params.buildNo ? 'pipelinesHistory' : $route.name
                this.$router.push({
                    name,
                    params: {
                        projectId,
                        pipelineId
                    }
                })
            },
            async switchBuildNum (int = 0) {
                const { execDetail } = this
                const buildNum = execDetail.buildNum + int
                if (!this.isLoading && buildNum !== execDetail.buildNum && execDetail.latestBuildNum >= buildNum && buildNum >= 1) {
                    try {
                        this.isLoading = true
                        const response = await this.requestPipelineExecDetailByBuildNum({
                            buildNum,
                            ...this.$route.params
                        })

                        this.$router.push({
                            name: 'pipelinesDetail',
                            params: {
                                ...this.$route.params,
                                buildNo: response.data.id
                            }
                        })
                    } catch (error) {

                    } finally {
                        this.isLoading = false
                    }
                }
            },
            startExcuete () {
                bus.$emit('start-execute')
            },
            renamePipeline () {
                this.isDialogShow = true
                this.dialogConfig = {
                    title: '流水线重命名',
                    formData: {
                        ...this.pipelineFormData,
                        name: this.curPipeline.pipelineName
                    },
                    loading: false,
                    formConfig: this.renameFormConfig.slice(0, 1),
                    handleDialogConfirm: async () => {
                        try {
                            this.dialogConfig.loading = true
                            await this.rename(this.dialogConfig.formData, this.projectId, this.pipelineId)
                            this.dialogConfig.loading = false
                            this.resetDialog()
                        } catch (e) {
                            console.warn(e)
                        }
                    },
                    handleDialogCancel: this.resetDialog
                }
            },
            copyPipeline () {
                const { curPipeline } = this
                this.isDialogShow = true
                this.dialogConfig = {
                    title: '复制流水线',
                    formData: {
                        ...this.pipelineFormData,
                        name: `${curPipeline.pipelineName}_copy`
                    },
                    loading: false,
                    formConfig: this.renameFormConfig,
                    handleDialogConfirm: async () => {
                        try {
                            this.dialogConfig.loading = true
                            await this.copy(this.dialogConfig.formData, curPipeline.pipelineId)
                            this.dialogConfig.loading = false
                            this.resetDialog()
                        } catch (e) {
                            console.warn(e)
                        }
                    },
                    handleDialogCancel: this.resetDialog
                }
            },
            resetDialog () {
                this.isDialogShow = false
                this.tempPipline = {
                    name: '',
                    desc: ''
                }
                this.dialogConfig = {
                    title: '',
                    formData: {},
                    formConfig: [],
                    handleDialogConfirm: () => {},
                    handleDialogCancel: () => {}
                }
            },
            showTemplateDialog () {
                this.isDialogShow = true
                this.dialogConfig = {
                    title: '另存为模板',
                    loading: false,
                    formData: this.templateFormData,
                    formConfig: this.templateFormConfig,
                    handleDialogConfirm: async () => {
                        try {
                            const { projectId, pipelineId, dialogConfig } = this
                            this.dialogConfig.loading = true
                            await this.saveAsPipelineTemplate(projectId, pipelineId, dialogConfig.formData.templateName, dialogConfig.formData.isCopySetting)
                            this.dialogConfig.loading = false
                            this.resetDialog()
                        } catch (e) {
                            console.warn(e)
                        }
                    },
                    handleDialogCancel: this.resetDialog
                }
            },
            toggleCollect () {
                this.togglePipelineCollect(this.curPipeline.pipelineId, !this.curPipeline.hasCollect)
            },
            deletePipeline () {
                this.delete(this.curPipeline)
            },
            toExecute (...args) {
                const goDetail = ['pipelinesEdit', 'pipelinesDetail'].indexOf(this.$route.name) > -1
                this.executePipeline(...args, goDetail)
            }
        }
    }
</script>

<style lang="scss">
    @import "../../scss/conf";
    .bkdevops-history-subpage {
        min-height: 100%;
        flex-direction: column;
        .bk-exception {
            position: absolute;
        }
        .more-operation-entry {
            display: flex;
            width: 30px;
            height: 100%;
            flex-direction: column;
            float: right;
            justify-content: center;
            align-items: center;
            cursor: pointer;
            padding-top: 5px;

            &:hover,
            &.active {
                &:before {
                    content: '';
                    position: absolute;
                    width: 18px;
                    height: 36px;
                    top: 14px;
                    background-color: #E0ECFF;
                    z-index: 0;
                }
                i.entry-circle {
                    background-color: $primaryColor;
                }
            }

            &:hover {
                .more-operation-dropmenu {
                    display: block;
                }
            }

            i.entry-circle {
                display: flex;
                width: 18px;
                margin: 5px 0;
                background-color: $fontWeightColor;
                width: 3px;
                height: 3px;
                border-radius: 50%;
                z-index: 1;
            }
        }
        .more-operation-dropmenu {
            position: absolute;
            display: none;
            right: -6px;
            top: 58px;
            width:163px;
            background-color:white;
            box-shadow:0px 2px 6px 0px rgba(0,0,0,0.07);
            border:1px solid #dcdee5;
            z-index: 12;
            &:before {
                content: '';
                position: absolute;
                right: 7px;
                top: -6px;
                background: white;
                width: 10px;
                height: 10px;
                transform: rotate(45deg);
                border-left: 1px solid #DCDEE5;
                border-top: 1px solid #DCDEE5;
            }
            > ul {
                padding-top: 8px;
                &:first-child {
                    border-bottom: 1px solid #DCDEE5;
                }
                > li {
                    font-size: 12px;
                    line-height: 32px;
                    text-align: left;
                    padding: 0 15px;
                    cursor: pointer;
                    &:hover {
                        color: $primaryColor;
                        background-color: #EAF3FF;
                    }
                }
            }
        }

        .customer-inner-header {
            .inner-header-left {
                width: 60%
            }
            .inner-header-right {
                width: 40%
            }
        }
        .history-bread-crumb {
            display: flex;
            height: 100%;
            align-items: center;
            .bread-crumb-comp {
                flex: 1;
            }
            .build-num-switcher {
                display: flex;
                align-items: center;
                > p {
                    display: flex;
                    flex-direction: column;
                    > i.bk-icon {
                        color: $primaryColor;
                        cursor: pointer;
                        font-size: 10px;

                        &[disabled] {
                            color: $fontLigtherColor;
                            cursor: auto;
                        }
                    }
                }
            }
        }
    }
    .crumb-spin-loading {
        display: inline-block;
    }

    .pipelines-triggers.bkdevops-header-trigger-btn {
        width: auto;
        display: inline-block;
        height: auto;
        font-size: inherit;
    }
     .bkdevops-pipeline-tab-card {
        // display: flex;
        // overflow: hidden;
        // flex-direction: column;
        border: 0;
        background-color: transparent;
        &-setting {
            font-size: 18px;
            display: flex;
            align-items: center;
            height: 100%;

            .bk-icon {
                color: $fontLigtherColor;
                padding-left: 16px;
                cursor: pointer;
                &:hover,
                &.active {
                    color: $primaryColor;
                }
            }
        }
        .bk-tab-header {
            background: transparent;
            .bk-tab-label-wrapper .bk-tab-label-list .bk-tab-label-item {
                min-width: auto;
                padding: 0;
                margin-right: 30px;
                text-align: left;
                font-weight: bold;
                &.active {
                    color: $primaryColor;
                    background: transparent
                }
            }
        }
        .bk-tab-section {
            width: 100%;
            padding: 0;
            margin-top: 10px;
            flex: 1;
            overflow: hidden;
            .bk-tab-content {
                height: 100%;
                display: flex;
                flex-direction: column;
            }
        }
    }
</style>
