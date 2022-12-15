<template>
    <section class="bk-form pipeline-setting base" v-if="!isLoading">
        <div class="setting-container">
            <form-field :required="true" :label="$t('name')" :is-error="errors.has(&quot;name&quot;)" :error-msg="errors.first(&quot;name&quot;)">
                <input class="bk-form-input" :placeholder="$t('settings.namePlaceholder')" v-model="pipelineSetting.pipelineName" name="name" v-validate.initial="&quot;required|max:40&quot;" />
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

            <form-field :label="$t('desc')" :is-error="errors.has(&quot;desc&quot;)" :error-msg="errors.first(&quot;desc&quot;)">
                <textarea name="desc" v-model="pipelineSetting.desc" :placeholder="$t('settings.descPlaceholder')" class="bk-form-textarea" v-validate.initial="&quot;max:100&quot;"></textarea>
            </form-field>

            <form-field :label="$t('settings.runLock')" class="opera-lock-radio">
                <bk-radio-group v-model="pipelineSetting.runLockType">
                    <bk-radio v-for="(entry, key) in runTypeList" :key="key" :value="entry.value" class="view-radio">{{ entry.label }}</bk-radio>
                </bk-radio-group>
            </form-field>
            <div class="bk-form-item opera-lock" v-if="pipelineSetting.runLockType === 'SINGLE'">
                <div class="bk-form-content">
                    <div class="opera-lock-item">
                        <label class="opera-lock-label">{{ $t('settings.largestNum') }}：</label>
                        <div class="bk-form-control control-prepend-group control-append-group">
                            <input type="text" name="maxQueueSize" :placeholder="$t('settings.itemPlaceholder')" class="bk-form-input" v-validate.initial="&quot;required|numeric|max_value:20|min_value:0&quot;" v-model.number="pipelineSetting.maxQueueSize">
                            <div class="group-box group-append">
                                <div class="group-text">{{ $t('settings.item') }}</div>
                            </div>
                            <p v-if="errors.has('maxQueueSize')" class="is-danger">{{errors.first("maxQueueSize")}}</p>
                        </div>
                    </div>
                    <div class="opera-lock-item">
                        <label class="opera-lock-label">{{ $t('settings.lagestTime') }}：</label>
                        <div class="bk-form-control control-prepend-group control-append-group">
                            <input type="text" name="waitQueueTimeMinute" :placeholder="$t('settings.itemPlaceholder')" class="bk-form-input" v-validate.initial="'required|numeric|max_value:1440|min_value:1'" v-model.number="pipelineSetting.waitQueueTimeMinute">
                            <div class="group-box group-append">
                                <div class="group-text">{{ $t('settings.minutes') }}</div>
                            </div>
                            <p v-if="errors.has('waitQueueTimeMinute')" class="is-danger">{{errors.first("waitQueueTimeMinute")}}</p>
                        </div>
                    </div>
                </div>
            </div>
            <form-field :label="$t('settings.notify')" style="margin-bottom: 0px">
                <bk-tab :active="curNavTab.name" type="unborder-card" @tab-change="changeCurTab">
                    <bk-tab-panel
                        v-for="(entry, index) in subscriptionList"
                        :key="index"
                        v-bind="entry"
                    >
                        <div class="notice-tab">
                            <div class="bk-form-item item-notice">
                                <label class="bk-label">{{ $t('settings.noticeType') }}：</label>
                                <div class="bk-form-content notice-group">
                                    <bk-checkbox-group :value="pipelineSubscription.types" @change="handleCheckNoticeType">
                                        <bk-checkbox v-for="item in noticeList" :key="item.value" :value="item.value" class="atom-checkbox-list-item">
                                            {{ item.name }}
                                        </bk-checkbox>
                                    </bk-checkbox-group>
                                </div>
                            </div>
                            <div class="bk-form-item item-notice">
                                <label class="bk-label">{{ $t('settings.noticeGroup') }}：</label>
                                <div class="bk-form-content notice-group">
                                    <bk-checkbox-group :value="pipelineSubscription.groups" @change="handleSwitch">
                                        <bk-checkbox v-for="item in projectGroupAndUsers" :key="item.value" :value="item.groupId" class="atom-checkbox-list-item">
                                            {{ item.groupName }}
                                            <bk-popover placement="top">
                                                <span class="info-notice-length">({{item.users.length}})</span>
                                                <div class="notice-user-content" slot="content">{{item.users.length ? item.users.join(';') : $t('settings.emptyNoticeGroup')}}</div>
                                            </bk-popover>
                                        </bk-checkbox>
                                    </bk-checkbox-group>
                                </div>
                            </div>
                            <form-field :label="$t('settings.additionUser')">
                                <staff-input :handle-change="(name,value) => pipelineSubscription.users = value.join(&quot;,&quot;)" name="users" :value="pipelineSettingUser"></staff-input>
                            </form-field>

                            <form-field :label="$t('settings.noticeContent')" :is-error="errors.has(&quot;content&quot;)" :error-msg="errors.first(&quot;content&quot;)">
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
    import { mapActions, mapState, mapGetters } from 'vuex'
    import FormField from '@/components/AtomPropertyPanel/FormField.vue'
    import StaffInput from '@/components/atomFormField/StaffInput/index.vue'
    import GroupIdSelector from '@/components/atomFormField/groupIdSelector'
    import AtomCheckbox from '@/components/atomFormField/AtomCheckbox'
    export default {
        components: {
            FormField,
            StaffInput,
            GroupIdSelector,
            AtomCheckbox
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
                runTypeList: [
                    {
                        label: this.$t('settings.runningOption.multiple'),
                        value: 'MULTIPLE'
                    },
                    {
                        label: this.$t('settings.runningOption.lock'),
                        value: 'LOCK'
                    },
                    {
                        label: this.$t('settings.runningOption.single'),
                        value: 'SINGLE'
                    }
                ],
                subscriptionList: [
                    { label: this.$t('settings.whenSuc'), name: 'success' },
                    { label: this.$t('settings.whenFail'), name: 'fail' }
                ],
                curNavTab: { label: this.$t('settings.buildSuc'), name: 'success' },
                noticeList: [
                    { id: 1, name: this.$t('settings.rtxNotice'), value: 'RTX' },
                    { id: 4, name: this.$t('settings.emailNotice'), value: 'EMAIL' },
                    { id: 2, name: this.$t('settings.wechatNotice'), value: 'WECHAT' },
                    { id: 3, name: this.$t('settings.smsNotice'), value: 'SMS' }
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
                'pipelineSetting',
                'projectGroupAndUsers'
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
            this.requestProjectGroupAndUsers(this.$route.params)
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
                'updatePipelineSetting',
                'requestProjectGroupAndUsers'
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
