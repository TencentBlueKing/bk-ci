<template>
    <div class="notify-setting-comp" v-if="subscription">
        <bk-form>
            <bk-form-item :label="$t('settings.noticeType')" :required="true">
                <bk-checkbox-group :value="subscription.types" @change="value => updateSubscription('types', value)">
                    <bk-checkbox v-for="item in noticeList" :key="item.id" :value="item.value">
                        {{ item.name }}
                    </bk-checkbox>
                </bk-checkbox-group>
            </bk-form-item>
            <bk-form-item :label="$t('settings.additionUser')" :required="true">
                <staff-input
                    :handle-change="(name, value) => subscription.users = value.join(',')"
                    :value="subscription.users.split(',').filter(Boolean)"
                    :placeholder="$t('settings.additionUserPlaceholder')">
                </staff-input>
            </bk-form-item>
            <bk-form-item :label="$t('settings.noticeContent')" :required="true">
                <textarea name="desc" v-model="subscription.content" class="bk-form-textarea"></textarea>
            </bk-form-item>
            <!-- <bk-form-item>
                <atom-checkbox style="width: auto"
                    :handle-change="updateSubscription"
                    name="detailFlag"
                    :text="$t('settings.pipelineLink')"
                    :desc="$t('settings.pipelineLinkDesc')"
                    :value="subscription.detailFlag">
                </atom-checkbox>
            </bk-form-item> -->
            <bk-form-item v-if="subscription.wechatGroupFlag">
                <atom-checkbox
                    style="width: auto"
                    name="wechatGroupFlag"
                    :text="$t('settings.enableGroup')"
                    :desc="groupIdDesc"
                    :handle-change="updateSubscription"
                    :value="subscription.wechatGroupFlag">
                </atom-checkbox>
                <group-id-selector style="margin-left: -150px" class="item-groupid"
                    :handle-change="updateSubscription"
                    name="wechatGroup"
                    :value="subscription.wechatGroup"
                    :placeholder="$t('settings.groupIdTips')"
                    icon-class="icon-question-circle"
                    desc-direction="top">
                </group-id-selector>
                <atom-checkbox
                    v-if="subscription.wechatGroupFlag"
                    style="width: auto;margin-top: -45px;"
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
    import GroupIdSelector from '@/components/atomFormField/groupIdSelector'
    import StaffInput from '@/components/atomFormField/StaffInput'
    export default {
        name: 'notify-setting',
        components: {
            GroupIdSelector,
            StaffInput
        },
        props: {
            subscription: Object,
            updateSubscription: Function
        },
        computed: {
            noticeList () {
                return [
                    { id: 1, name: this.$t('settings.rtxNotice'), value: 'WEWORK' }
                    // { id: 4, name: this.$t('settings.emailNotice'), value: 'EMAIL' },
                    // { id: 2, name: this.$t('settings.wechatNotice'), value: 'WECHAT' },
                    // { id: 3, name: this.$t('settings.smsNotice'), value: 'SMS' }
                ]
            },
            groupIdDesc () {
                return this.$t('settings.groupIdDesc')
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
        .notify-setting-no-data {
            vertical-align: top;
            font-size: 12px;
            color: #63656e;
        }
    }
</style>
