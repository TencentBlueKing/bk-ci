<template>
    <div class="notify-setting-comp">
        <bk-form
            v-if="subscription"
            ref="notifyForm"
            class="new-ui-form"
            form-type="vertical"
            :label-width="580"
            :model="subscription"
            :rules="notifyRules"
        >
            <bk-form-item property="types" :label="$t('settings.noticeType')" error-display-type="normal" :required="true">
                <bk-checkbox-group :value="subscription.types" @change="value => updateSubscription('types', value)">
                    <bk-checkbox v-for="item in noticeList" :key="item.id" :value="item.value">
                        {{ item.name }}
                    </bk-checkbox>
                </bk-checkbox-group>
            </bk-form-item>
            <bk-form-item :label="$t('settings.noticeGroup')">
                <bk-checkbox-group :value="subscription.groups" @change="value => updateSubscription('groups', value)">
                    <bk-checkbox v-for="item in projectGroupAndUsers" :key="item.groupId" :value="item.groupId" class="groups-users-checkbox">
                        {{ item.groupName }}
                        <bk-popover placement="top">
                            <span class="info-notice-length">({{ item.users.length }})</span>
                            <div slot="content" style="max-width: 300px;word-wrap:break-word; word-break: normal">{{ item.users.length ? item.users.join(';') : $t('settings.emptyNoticeGroup') }}</div>
                        </bk-popover>
                    </bk-checkbox>
                </bk-checkbox-group>
            </bk-form-item>
            <bk-form-item :label="$t('settings.additionUser')">
                <staff-input
                    :handle-change="(name, value) => subscription.users = value.join(',')"
                    :value="subscription.users.split(',').filter(Boolean)"
                    :placeholder="$t('settings.additionUserPlaceholder')">
                </staff-input>
            </bk-form-item>
            <bk-form-item property="content" :label="$t('settings.noticeContent')" error-display-type="normal" :required="true">
                <bk-input
                    type="textarea"
                    :value="subscription.content"
                    @change="value => updateSubscription('content', value)"
                />
            </bk-form-item>
            
            <bk-form-item v-if="subscription.types.includes('RTX')">
                <atom-checkbox
                    style="width: auto"
                    name="wechatGroupFlag"
                    :text="$t('settings.enableGroup')"
                    :desc="groupIdDesc"
                    :handle-change="updateSubscription"
                    :value="subscription.wechatGroupFlag">
                </atom-checkbox>
            </bk-form-item>
            <bk-form-item v-if="subscription.wechatGroupFlag" :label="$t('settings.groupIdLabel')">
                <group-id-selector
                    class="item-groupid"
                    :handle-change="updateSubscription"
                    name="wechatGroup"
                    :value="subscription.wechatGroup"
                    :placeholder="$t('settings.groupIdTips')"
                    icon-class="icon-question-circle"
                    desc-direction="top">
                </group-id-selector>
            </bk-form-item>
            <bk-form-item v-if="subscription.wechatGroupFlag">
                <atom-checkbox
                    style="width: auto;"
                    name="wechatGroupMarkdownFlag"
                    :text="$t('settings.wechatGroupMarkdownFlag')"
                    :handle-change="updateSubscription"
                    :value="subscription.wechatGroupMarkdownFlag">
                </atom-checkbox>
            </bk-form-item>
        </bk-form>
    </div>
</template>

<script>
    import AtomCheckbox from '@/components/atomFormField/AtomCheckbox'
    import StaffInput from '@/components/atomFormField/StaffInput'
    import GroupIdSelector from '@/components/atomFormField/groupIdSelector'
    export default {
        name: 'notify-setting',
        components: {
            GroupIdSelector,
            StaffInput,
            AtomCheckbox
        },
        props: {
            subscription: Object,
            updateSubscription: Function,
            projectGroupAndUsers: Array
        },
        data () {
            return {
                notifyRules: {
                    types: [
                        {
                            message: this.$t('requiredSelectItem'),
                            trigger: 'blur',
                            validator: function (val) {
                                return val.length >= 1
                            }
                        }
                    ],
                    content: [
                        {
                            required: true,
                            message: this.$t('requiredItem'),
                            trigger: 'blur'
                        }
                    ]
                }
            }
        },
        computed: {
            noticeList () {
                return [
                    { id: 4, name: this.$t('settings.emailNotice'), value: 'EMAIL' },
                    { id: 1, name: this.$t('settings.rtxNotice'), value: 'RTX' },
                    { id: 5, name: this.$t('settings.voice'), value: 'VOICE' }
                    // { id: 2, name: this.$t('settings.wechatNotice'), value: 'WECHAT' },
                    // { id: 3, name: this.$t('settings.smsNotice'), value: 'SMS' }
                ]
            },
            groupIdDesc () {
                return this.$t('settings.groupIdDesc')
            },
            pipelineSettingUser () {
                return this.subscription && this.subscription.users ? this.subscription.users.split(',') : []
            }
        },
        methods: {
            handleUsers (name, value) {
                this.updateSubscription(name, value.join(','))
            }
        }
    }
</script>

<style lang="scss">
    .notify-setting-comp {
        .bk-form .bk-form-content {
            min-height: 24px;
        }
        .bk-form-checkbox {
            display: inline-block;
            padding: 0;
            width: auto;
            margin-right: 24px;
        }
        .groups-users-checkbox {
            width: 150px;
            margin-bottom: 4px;
            margin-right: 8px;
            .bk-checkbox-text {
                max-width: 126px;
                overflow: hidden;
                white-space: nowrap;
                text-overflow: ellipsis;
            }
        }
    }
</style>
