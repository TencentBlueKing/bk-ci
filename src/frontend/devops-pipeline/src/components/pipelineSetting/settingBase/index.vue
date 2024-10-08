<template>
    <section
        class="bk-form pipeline-setting base"
        v-if="!isLoading"
    >
        <div class="setting-container">
            <form-field
                :required="true"
                :label="$t('name')"
                :is-error="errors.has('name')"
                :error-msg="errors.first('name')"
            >
                <bk-input
                    :placeholder="$t('settings.namePlaceholder')"
                    v-model.trim="templateSetting.pipelineName"
                    name="name"
                    @change="setIsEditing"
                    v-validate="'required|max:40'"
                />
            </form-field>

            <form-field
                :required="false"
                :label="$t('settings.label')"
                v-if="tagGroupList.length"
            >
                <div class="form-group form-group-inline">
                    <div
                        :class="grouInlineCol"
                        v-for="(filter, index) in tagGroupList"
                        :key="index"
                    >
                        <label class="group-title">{{ filter.name }}</label>
                        <bk-select
                            ext-cls="setting-select"
                            :value="labelValues[index]"
                            @selected="handleLabelSelect(index, arguments)"
                            @clear="handleLabelSelect(index, [[]])"
                            multiple
                        >
                            <bk-option
                                v-for="(option, oindex) in filter.labels"
                                :key="oindex"
                                :id="option.id"
                                :name="option.name"
                            >
                            </bk-option>
                        </bk-select>
                    </div>
                </div>
            </form-field>

            <form-field
                :label="$t('desc')"
                :is-error="errors.has('desc')"
                :error-msg="errors.first('desc')"
            >
                <textarea
                    name="desc"
                    v-model.trim="templateSetting.desc"
                    :placeholder="$t('settings.descPlaceholder')"
                    class="bk-form-textarea"
                    v-validate.initial="'max:100'"
                    @change="setIsEditing"
                />
            </form-field>

            <form-field
                class="namingConvention"
            >
                <syntax-style-configuration
                    :inherited-dialect="templateSetting.pipelineAsCodeSettings?.inheritedDialect ?? true"
                    :pipeline-dialect="templateSetting.pipelineAsCodeSettings?.pipelineDialect ?? 'CLASSIC'"
                    @inherited-change="inheritedChange"
                    @pipeline-dialect-change="pipelineDialectChange"
                />
            </form-field>

            <form-field
                :label="$t('settings.runLock')"
                class="opera-lock-radio"
            >
                <running-lock
                    :pipeline-setting="templateSetting"
                    :handle-running-lock-change="handleRunningLockChange"
                />
            </form-field>

            <form-field
                :label="$t('settings.notice')"
                style="margin-bottom: 0px"
            >
                <notify-tab
                    :editable="!isDisabled && hasPermission"
                    :success-subscription-list="templateSetting?.successSubscriptionList ?? []"
                    :fail-subscription-list="templateSetting?.failSubscriptionList ?? []"
                    :update-subscription="handleUpdateNotify"
                />
            </form-field>

            <div
                class="handle-btn"
                style="margin-left: 146px;"
            >
                <bk-button
                    v-if="isEnabledPermission"
                    @click="saveTemplateSetting()"
                    theme="primary"
                    v-perm="{
                        permissionData: {
                            projectId: projectId,
                            resourceType: 'pipeline_template',
                            resourceCode: templateId,
                            action: TEMPLATE_RESOURCE_ACTION.EDIT
                        }
                    }"
                    key="saveBtn"
                >
                    {{ $t('save') }}
                </bk-button>
                <bk-button
                    v-else
                    @click="saveTemplateSetting()"
                    theme="primary"
                    :disabled="isDisabled || !hasPermission"
                >
                    {{ $t('save') }}
                </bk-button>
                <bk-button @click="exit">{{ $t('cancel') }}</bk-button>
            </div>
        </div>
    </section>
</template>

<script>
    import { NotifyTab } from '@/components/PipelineEditTabs/'
    import FormField from '@/components/AtomPropertyPanel/FormField.vue'
    import RunningLock from '@/components/pipelineSetting/RunningLock'
    import { mapActions, mapGetters, mapState } from 'vuex'
    import SyntaxStyleConfiguration from '@/components/syntaxStyleConfiguration'
    import {
        TEMPLATE_RESOURCE_ACTION
    } from '@/utils/permission'
    export default {
        components: {
            NotifyTab,
            FormField,
            RunningLock,
            SyntaxStyleConfiguration
        },
        props: {
            isLoading: Boolean,
            isDisabled: {
                type: Boolean,
                default: false
            },
            isEnabledPermission: Boolean
        },
        data () {
            return {
                isEditing: false
            }
        },
        computed: {
            ...mapState('pipelines', [
                'templateSetting',
                'currentPipelineDialect'
            ]),
            ...mapGetters({
                tagGroupList: 'pipelines/getTagGroupList'
            }),
            hasPermission () {
                return this.templateSetting?.hasPermission !== false
            },
            projectId () {
                return this.$route.params.projectId
            },
            templateId () {
                return this.$route.params.templateId
            },
            grouInlineCol () {
                const classObj = {}
                let key = 'group-inline '
                key += this.tagGroupList.length < 3 ? this.tagGroupList.length < 2 ? '' : 'column-2' : 'column-3'
                classObj[key] = true
                return classObj
            },
            labelValues () {
                const labels = this.templateSetting.labels
                return this.tagGroupList.map((tag) => {
                    const currentLables = tag.labels || []
                    const value = []
                    currentLables.forEach((label) => {
                        const index = labels.findIndex((item) => (item === label.id))
                        if (index > -1) value.push(label.id)
                    })
                    return value
                })
            },
            runTypeMap () {
                return {
                    MULTIPLE: 'MULTIPLE',
                    SINGLE: 'SINGLE',
                    GROUP: 'GROUP_LOCK',
                    LOCK: 'LOCK'
                }
            },
            isSingleLock () {
                return [this.runTypeMap.GROUP, this.runTypeMap.SINGLE].includes(this.templateSetting.runLockType)
            },
            formRule () {
                const requiredRule = {
                    required: this.isSingleLock,
                    message: this.$t('editPage.checkParamTip'),
                    trigger: 'blur'
                }
                return {
                    concurrencyGroup: [
                        requiredRule
                    ],
                    maxQueueSize: [
                        requiredRule,
                        {
                            validator: (val) => {
                                const intVal = parseInt(val, 10)
                                return !this.isSingleLock || (intVal <= 20 && intVal >= 0)
                            },
                            message: `${this.$t('settings.largestNum')}${this.$t('numberRange', [0, 20])}`,
                            trigger: 'change'
                        }
                    ],
                    waitQueueTimeMinute: [
                        requiredRule,
                        {
                            validator: (val) => {
                                const intVal = parseInt(val, 10)
                                return !this.isSingleLock || (intVal <= 1440 && intVal >= 1)
                            },
                            message: `${this.$t('settings.lagestTime')}${this.$t('numberRange', [1, 1440])}`,
                            trigger: 'change'
                        }
                    ]
                }
            },
            TEMPLATE_RESOURCE_ACTION () {
                return TEMPLATE_RESOURCE_ACTION
            }
        },
        watch: {
            isEditing () {
                this.isStateChange()
            }
        },
        created () {
            this.requestTemplateSetting(this.$route.params)
            this.requestGrouptLists()
        },
        methods: {
            ...mapActions('pipelines', [
                'requestTemplateSetting',
                'updateTemplateSetting',
                'getPipelineDialect'
            ]),
            ...mapActions('atom', [
                'updatePipelineSetting'
            ]),
            handleLabelSelect (index, arg) {
                let labels = []
                this.labelValues.forEach((value, valueIndex) => {
                    if (valueIndex === index) labels = labels.concat(arg[0])
                    else labels = labels.concat(value)
                })
                this.templateSetting.labels = labels
                this.setIsEditing()
            },
            isStateChange () {
                this.$emit('setState', this.isEditing)
            },
            setIsEditing () {
                this.isEditing = true
            },
            handleChangeRunType (name, value) {
                Object.assign(this.templateSetting, { [name]: value })
                this.setIsEditing()
            },
            exit () {
                this.$emit('cancel')
            },
            /** *
             * 获取标签及其分组
             */
            async requestGrouptLists () {
                const { $store } = this
                let res
                try {
                    res = await $store.dispatch('pipelines/requestTagList', {
                        projectId: this.projectId
                    })
                    $store.commit('pipelines/updateGroupLists', res)
                    this.dataList = this.tagGroupList
                    // 获取当前项目语法风格
                    await this.getPipelineDialect(this.projectId)
                } catch (err) {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                }
            },
            async saveTemplateSetting () {
                if (this.errors.any()) return
                this.isDisabled = true
                let result
                let resData
                try {
                    const { templateSetting } = this
                    Object.assign(templateSetting, { projectId: this.projectId, successSubscription: undefined, failSubscription: undefined })
                    resData = await this.$ajax.put(`/process/api/user/templates/projects/${this.projectId}/templates/${this.templateId}/settings`, templateSetting)

                    if (resData && resData.data) {
                        this.$showTips({
                            message: `${templateSetting.pipelineName}  ${this.$t('updateSuc')}`,
                            theme: 'success'
                        })
                        this.isEditing = false
                        this.isStateChange()
                        result = true
                    } else {
                        this.$showTips({
                            message: `${templateSetting.pipelineName}${this.$t('updateFail')}`,
                            theme: 'error'
                        })
                    }
                } catch (err) {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                    result = false
                }
                this.isDisabled = false
                return result
            },
            handleRunningLockChange (param) {
                Object.assign(this.templateSetting, param)
                this.setIsEditing()
            },
            handleUpdateNotify (name, value) {
                Object.assign(this.templateSetting, { [name]: value })
                this.setIsEditing()
            },
            inheritedChange (value) {
                const settings = this.templateSetting.pipelineAsCodeSettings
                settings.inheritedDialect = value

                if (value) {
                    settings.pipelineDialect = this.currentPipelineDialect
                }
            },
            pipelineDialectChange (value) {
                this.templateSetting.pipelineAsCodeSettings.pipelineDialect = value
            }
        }
    }
</script>

<style lang="scss">
    @import './../../../scss/conf.scss';

    .pipeline-setting.base{
        position:absolute;
        width: 100%;
        padding:  20px;
        & .setting-container{
            width: 60%;
            min-width: 880px;
        }
         .bk-form-item{
             /* margin-bottom: 30px; */
             & .bk-form-content .bk-form-radio{
                display: block;
             }
             .bk-form-control {
                line-height: inherit;
             }
        }
        .form-group-inline {
            font-size: 0;
            &:after {
                display: block;
                content: '';
                height: 0;
                width: 0;
                clear: both;
            }
            .setting-select {
                background: #fff;
            }
            .group-inline  {
                float: left;
                width: 100%;
                margin-right: 6px;
                .group-title {
                    display: inline-block;
                    max-width: 100%;
                    // margin-bottom: 5px;
                    line-height: 34px;
                    font-size: 14px;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                    color: #63656E;
                }
                &.column-2 {
                    width: calc((100% - 6px) / 2);
                }
                &.column-3 {
                    width: calc((100% - 12px) / 3);
                }
                &:last-child {
                    margin-right: 0;
                }
            }
        }
        .form-group-link {
            width: 100%;
        }
        .namingConvention {
            position: relative;
        }
        .opera-lock-radio {
            margin-bottom: 0;
            .view-radio {
                margin-top: 8px;
            }
        }
        .opera-lock {
            margin-top: 0;
            .bk-form-content {
                margin-left: 180px;
                padding: 7px 0;
            }
            .opera-lock-item {
                display: inline-block;
                .opera-lock-label {
                    display: inline-block;
                    font-size: 14px;
                }
                + .opera-lock-item {
                    margin-left: 80px;
                }
            }
            .bk-form-control {
                position: relative;
                display: inline-table;
                width: auto;
                background-color: #f2f4f8;
                color: #63656e;
            }
            .is-danger {
                position: absolute;
                top: 100%;
                left: 0;
                font-size: 12px;
                color: $failColor;
            }
            .bk-form-input {
                width: 80px;
                border-radius: 0;
            }
            .group-box {
                vertical-align: middle;
                display: table-cell;
                position: relative;
                border: 1px solid #c4c6cc;
                border-left: none;
                border-radius: 0 2px 2px 0;
            }
            .group-text {
                color: #63656e;
                padding: 0 15px;
                white-space: nowrap;
                font-size: 14px;
            }
        }
    }
    .item-notice {
        .notice-group {
            margin-top: 8px;
            .atom-checkbox-list-item {
                font-weight: bold;
                width: 170px;
                padding: 0 20px 10px 0;
                overflow: hidden;
                text-overflow:ellipsis;
                white-space: nowrap;
            }
        }
        .notify-setting-no-data {
            vertical-align: top;
            font-size: 12px;
            color: #63656e;
        }
    }
    .notice-user-content {
        max-width: 300px;
        white-space: normal;
        word-wrap: break-word;
        font-weight: 400;
    }
    .checkbox-group {
        .bk-form-checkbox {
            width: 250px !important;
        }
    }
</style>
