<template>
    <section class="bk-form pipeline-setting base" v-if="!isLoading">
        <div class="setting-container">
            <form-field :required="true" :label="$t('name')" :is-error="errors.has('name')" :error-msg="errors.first('name')">
                <input class="bk-form-input" :placeholder="$t('settings.namePlaceholder')" v-model="pipelineSetting.pipelineName" name="name" v-validate.initial="'required|max:40'" />
            </form-field>

            <form-field :required="false" :label="$t('settings.label')" v-if="tagGroupList.length">
                <div class="form-group form-group-inline">
                    <div :class="grouInlineCol"
                        v-for="(filter, index) in tagGroupList"
                        :key="index">
                        <label class="group-title">{{filter.name}}</label>
                        <bk-select :value="labelValues[index]"
                            @selected="handleLabelSelect(index, arguments)"
                            @clear="handleLabelSelect(index, [[]])"
                            multiple
                        >
                            <bk-option v-for="(option, oindex) in filter.labels" :key="oindex" :id="option.id" :name="option.name">
                            </bk-option>
                        </bk-select>
                    </div>
                </div>
            </form-field>

            <form-field :label="$t('desc')" :is-error="errors.has('desc')" :error-msg="errors.first('desc')">
                <textarea name="desc" v-model="pipelineSetting.desc" :placeholder="$t('settings.descPlaceholder')" class="bk-form-textarea" v-validate.initial="'max:100'"></textarea>
            </form-field>

            <form-field :label="$t('settings.runLock')" class="opera-lock-radio">
                <running-lock
                    :pipeline-setting="pipelineSetting"
                    :handle-running-lock-change="handleRunningLockChange"
                />
            </form-field>
            <form-field :label="$t('settings.notice')" style="margin-bottom: 0px">
                <bk-tab :active="curNavTab.name" type="unborder-card" @tab-change="changeCurTab">
                    <bk-tab-panel
                        v-for="(entry, index) in subscriptionList"
                        :key="index"
                        v-bind="entry"
                    >
                        <div class="notice-tab">
                            <div class="bk-form-item item-notice">
                                <label class="bk-label">{{ $t('settings.noticeType') }}：</label>
                                <div class="bk-form-content">
                                    <bk-checkbox-group :value="pipelineSubscription.types" @change="handleCheckNoticeType">
                                        <bk-checkbox v-for="item in noticeList" :key="item.value" :value="item.value">
                                            {{ item.name }}
                                        </bk-checkbox>
                                    </bk-checkbox-group>
                                </div>
                            </div>
                            <form-field :label="$t('settings.additionUser')">
                                <user-input :handle-change="handleAdditionUserChange" name="users" :value="pipelineSettingUser"></user-input>
                            </form-field>

                            <form-field :label="$t('settings.noticeContent')" :is-error="errors.has('content')" :error-msg="errors.first('content')">
                                <textarea name="desc" v-model="pipelineSubscription.content" class="bk-form-textarea"></textarea>
                            </form-field>

                            <form-field style="margin-bottom: 10px;">
                                <atom-checkbox style="width: auto"
                                    :handle-change="toggleEnable"
                                    name="detailFlag"
                                    :text="$t('settings.pipelineLink')"
                                    :desc="$t('settings.pipelineLinkDesc')"
                                    :value="pipelineSubscription.detailFlag">
                                </atom-checkbox>
                            </form-field>
                            <form-field style="margin-bottom: 10px;">
                                <atom-checkbox style="width: auto"
                                    :handle-change="toggleEnable"
                                    name="wechatGroupFlag"
                                    :text="$t('settings.enableGroup')"
                                    :desc="groupIdDesc"
                                    :value="pipelineSubscription.wechatGroupFlag">
                                </atom-checkbox>
                            </form-field>
                            <group-id-selector class="item-groupid" v-if="pipelineSubscription.wechatGroupFlag"
                                :handle-change="groupIdChange"
                                :value="pipelineSubscription.wechatGroup"
                                :placeholder="$t('settings.groupIdTips')"
                                icon-class="icon-question-circle"
                                desc-direction="top">
                            </group-id-selector>
                            <atom-checkbox
                                v-if="pipelineSubscription.wechatGroupFlag"
                                style="width: auto;margin-top: -45px;margin-left: 155px;"
                                name="wechatGroupMarkdownFlag"
                                :text="$t('settings.wechatGroupMarkdownFlag')"
                                :handle-change="toggleEnable"
                                :value="pipelineSubscription.wechatGroupMarkdownFlag">
                            </atom-checkbox>
                        </div>
                    </bk-tab-panel>
                </bk-tab>
            </form-field>

            <div class="handle-btn" style="margin-left: 146px;">
                <bk-button @click="savePipelineSetting()" theme="primary" :disabled="isDisabled || noPermission">{{ $t('save') }}</bk-button>
                <bk-button @click="exit">{{ $t('cancel') }}</bk-button>
            </div>
        </div>
    </section>
</template>

<script>
    import FormField from '@/components/AtomPropertyPanel/FormField.vue'
    import AtomCheckbox from '@/components/atomFormField/AtomCheckbox'
    import UserInput from '@/components/atomFormField/UserInput/index.vue'
    import GroupIdSelector from '@/components/atomFormField/groupIdSelector'
    import RunningLock from '@/components/pipelineSetting/RunningLock'
    import { mapActions, mapGetters, mapState } from 'vuex'
    export default {
        components: {
            FormField,
            UserInput,
            GroupIdSelector,
            AtomCheckbox,
            RunningLock
        },
        props: {
            isDisabled: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                noPermission: false,
                isEditing: false,
                isLoading: true,
                resetFlag: false,
                subscriptionList: [
                    { label: this.$t('settings.buildFail'), name: 'fail' },
                    { label: this.$t('settings.buildSuc'), name: 'success' }
                ],
                curNavTab: { label: this.$t('settings.buildFail'), name: 'fail' },
                noticeList: [
                    { id: 1, name: this.$t('settings.rtxNotice'), value: 'WEWORK' }
                    // { id: 4, name: this.$t('settings.emailNotice'), value: 'EMAIL' },
                    // { id: 2, name: this.$t('settings.wechatNotice'), value: 'WECHAT' },
                    // { id: 3, name: this.$t('settings.smsNotice'), value: 'SMS' }
                ],
                pipelineSubscription: {
                    groups: [],
                    types: [],
                    users: '',
                    content: ''
                },
                groupIdDesc: this.$t('settings.groupIdDesc'),
                groupIdStorage: []
            }
        },
        computed: {
            ...mapState('pipelines', [
                'pipelineSetting'
            ]),
            ...mapGetters({
                tagGroupList: 'pipelines/getTagGroupList'
            }),
            pipelineSettingUser () {
                return this.pipelineSubscription.users ? this.pipelineSubscription.users.split(',') : []
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
                const labels = this.pipelineSetting.labels
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
                return [this.runTypeMap.GROUP, this.runTypeMap.SINGLE].includes(this.pipelineSetting.runLockType)
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
            }
        },
        watch: {
            pipelineSetting: {
                deep: true,
                handler: function (newVal, oldVal) {
                    // 无权限灰掉保存按钮
                    if (this.pipelineSetting.hasPermission !== undefined && this.pipelineSetting.hasPermission === false) {
                        this.noPermission = true
                    } else {
                        this.noPermission = false
                    }
                    this.curNavTab.name === 'success' ? this.pipelineSubscription = this.pipelineSetting.successSubscription : this.pipelineSubscription = this.pipelineSetting.failSubscription
                    this.isLoading = false
                    if (!this.isEditing && JSON.stringify(oldVal) !== '{}' && newVal !== null && !this.resetFlag) {
                        this.isEditing = true
                    }
                    this.resetFlag = false
                    this.isStateChange()
                }
            }
        },
        created () {
            this.requestTemplateSetting(this.$route.params)
            this.requestGrouptLists()
        },
        mounted () {
            this.list = this.groupIdStorage = localStorage.getItem('groupIdStr') ? localStorage.getItem('groupIdStr').split(';').filter(item => item) : []
        },
        destroyed () {
            this.wechatGroupCompletion()
            this.setGroupidStorage(this.pipelineSubscription)
        },
        methods: {
            ...mapActions('pipelines', [
                'requestTemplateSetting',
                'updatePipelineSetting'
            ]),
            handleLabelSelect (index, arg) {
                let labels = []
                this.labelValues.forEach((value, valueIndex) => {
                    if (valueIndex === index) labels = labels.concat(arg[0])
                    else labels = labels.concat(value)
                })
                this.pipelineSetting.labels = labels
            },
            isStateChange () {
                this.$emit('setState', {
                    isLoading: this.isLoading,
                    isEditing: this.isEditing
                })
            },
            handleChangeRunType (name, value) {
                Object.assign(this.pipelineSetting, { [name]: value })
            },
            handleCheckNoticeType (value) {
                this.pipelineSubscription.types = value
            },
            handleSwitch (value) {
                this.pipelineSubscription.groups = value
            },
            changeCurTab (name) {
                const tab = this.subscriptionList.find(item => item.name === name)
                this.setGroupidStorage(this.pipelineSubscription)
                this.curNavTab = tab
                this.pipelineSubscription = name === 'success' ? this.pipelineSetting.successSubscription : this.pipelineSetting.failSubscription
            },
            toggleEnable (name, value) {
                this.pipelineSubscription[name] = value
                this.updatePipelineSetting({
                    container: this.pipelineSubscription,
                    param: {
                        name: value
                    }
                })
            },
            groupIdChange (name, value) {
                this.pipelineSubscription.wechatGroup = value
                this.updatePipelineSetting({
                    container: this.pipelineSubscription,
                    param: {
                        wechatGroup: this.pipelineSubscription.wechatGroup
                    }
                })
            },
            // 补全末尾分号
            wechatGroupCompletion () {
                const wechatGroup = this.pipelineSubscription.wechatGroup
                if (wechatGroup && wechatGroup.charAt(wechatGroup.length - 1) !== ';') {
                    this.pipelineSubscription.wechatGroup += ';'
                }
            },
            setGroupidStorage (data) {
                if (!data.wechatGroup) {
                    return false
                }
                data.wechatGroup.split(';').filter(item => item).forEach(item => {
                    if (!this.groupIdStorage.includes(item)) {
                        this.groupIdStorage.push(item)
                    }
                })
                localStorage.setItem('groupIdStr', this.groupIdStorage.sort().join(';'))
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
                } catch (err) {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                }
            },
            async savePipelineSetting () {
                if (this.errors.any()) return
                this.wechatGroupCompletion()
                this.isDisabled = true
                let result
                let resData
                try {
                    const { pipelineSetting } = this
                    Object.assign(pipelineSetting, { projectId: this.projectId })
                    resData = await this.$ajax.put(`/process/api/user/templates/projects/${this.projectId}/templates/${this.templateId}/settings`, pipelineSetting)

                    if (resData && resData.data) {
                        this.$showTips({
                            message: `${pipelineSetting.pipelineName}${this.$t('updateSuc')}`,
                            theme: 'success'
                        })
                        this.isEditing = false
                        this.isStateChange()
                        result = true
                    } else {
                        this.$showTips({
                            message: `${pipelineSetting.pipelineName}${this.$t('updateFail')}`,
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
            handleAdditionUserChange (_, value) {
                this.pipelineSubscription.users = value.join(',')
            },
            handleRunningLockChange (param) {
                this.updatePipelineSetting({
                    container: this.pipelineSetting,
                    param
                })
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
             margin-bottom: 30px;
             & .bk-form-content .bk-form-radio{
                display: block;
             }
        }
        .notice-tab {
            padding: 10px 0px 0px;
            margin-left: -70px;
            .bk-form-content {
                margin-left: 155px;
            }
            .item-groupid .bk-tooltip {
                float: left;
                margin-left: -15px;
                line-height: 30px;
            }
            .bk-form-item label{
                display: inline-block;
                width: 145px;
                white-space: nowrap;
                text-overflow: ellipsis;
                overflow: hidden;
                padding-right: 10px;
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
</style>
