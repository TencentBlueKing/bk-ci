<template>
    <div class="notify-setting-comp" v-if="subscription">
        <bk-form>
            <bk-form-item :label="$t('settings.noticeType')">
                <bk-checkbox-group :value="subscription.types" @change="value => updateSubscription('types', value)">
                    <bk-checkbox v-for="item in noticeList" :key="item.id" :value="item.value">
                        {{ item.name }}
                    </bk-checkbox>
                </bk-checkbox-group>
            </bk-form-item>
            <bk-form-item :label="$t('settings.noticeGroup')">
                <bk-checkbox-group :value="subscription.groups" @change="value => updateSubscription('groups', value)">
                    <bk-checkbox v-for="item in projectGroupAndUsers" :key="item.groupId" :value="item.groupId">
                        {{ item.groupName }}
                        <bk-popover placement="top">
                            <span class="info-notice-length">({{ item.users.length }})</span>
                            <div slot="content" style="max-width: 300px;word-wrap:break-word; word-break: normal">{{ item.users.length ? item.users.join(';') : $t('settings.emptyNoticeGroup') }}</div>
                        </bk-popover>
                    </bk-checkbox>
                </bk-checkbox-group>
            </bk-form-item>
            <bk-form-item :label="$t('settings.additionUser')">
                <staff-input :handle-change="handleUsers" name="users" :value="pipelineSettingUser"></staff-input>
            </bk-form-item>
            <bk-form-item :label="$t('settings.noticeContent')">
                <textarea name="desc" v-model="subscription.content" class="bk-form-textarea"></textarea>
            </bk-form-item>
            <bk-form-item>
                <atom-checkbox style="width: auto"
                    :handle-change="updateSubscription"
                    name="detailFlag"
                    :text="$t('settings.pipelineLink')"
                    :desc="$t('settings.pipelineLinkDesc')"
                    :value="subscription.detailFlag">
                </atom-checkbox>
            </bk-form-item>
            <bk-form-item>
                <atom-checkbox
                    style="width: auto"
                    name="wechatGroupFlag"
                    :text="$t('settings.enableGroup')"
                    :desc="groupIdDesc"
                    :handle-change="updateSubscription"
                    :value="subscription.wechatGroupFlag">
                </atom-checkbox>
                <group-id-selector style="margin-left: -150px" class="item-groupid" v-if="subscription.wechatGroupFlag"
                    :handle-change="updateSubscription"
                    name="wechatGroup"
                    :value="subscription.wechatGroup"
                    :placeholder="$t('settings.groupIdTips')"
                    icon-class="icon-question-circle"
                    desc-direction="top">
                </group-id-selector>
                <atom-checkbox
                    v-if="subscription.wechatGroupFlag"
                    style="width: auto;margin-top: 10px;"
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
    import GroupIdSelector from '@/components/atomFormField/groupIdSelector'
    import StaffInput from '@/components/atomFormField/StaffInput/index.vue'
    import { mapActions } from 'vuex'
    export default {
        name: 'notify-setting',
        components: {
            StaffInput,
            GroupIdSelector,
            AtomCheckbox
        },
        props: {
            subscription: Object,
            updateSubscription: Function,
            projectGroupAndUsers: Array
        },
        computed: {
            noticeList () {
                return [
                    { id: 1, name: this.$t('settings.rtxNotice'), value: 'RTX' },
                    { id: 4, name: this.$t('settings.emailNotice'), value: 'EMAIL' },
                    { id: 3, name: this.$t('settings.smsNotice'), value: 'SMS' }
                ]
            },
            groupIdDesc () {
                return this.$t('settings.groupIdDesc')
            },
            pipelineSettingUser () {
                return this.subscription && this.subscription.users ? this.subscription.users.split(',') : []
            }
        },
        created () {
            this.requestProjectGroupAndUsers(this.$route.params)
        },
        methods: {
            ...mapActions('pipelines', [
                'requestProjectGroupAndUsers'
            ]),
            handleUsers (name, value) {
                this.updateSubscription(name, value.join(','))
            }
        }
    }
</script>

<style lang="scss">
    .notify-setting-comp{
        .bk-form-checkbox {
            display: inline-block;
            line-height: 34px;
            padding: 0;
            width: 180px;
        }
    }
</style>
