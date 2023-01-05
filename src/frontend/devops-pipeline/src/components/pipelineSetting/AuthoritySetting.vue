<template>
    <div v-bkloading="{ isLoading }" class="bkdevops-auth-setting">
        <user-groupings
            v-for="(item, index) in authList" :key="index"
            setting-key="groupRoleId"
            select-key-text="groupName"
            @groupingChange="handleAuthChange"
            :data-index="index"
            :title="item[titleName]"
            :input-label="$t('settings.user')"
            :pipeline-setting-user="item.user_list"
            :extra-user-list="item.extra_user_list"
            :select-label="$t('settings.userGroup')"
            :selected="item.selected"
            :list="projectGroupAndUsers"
            :extra-group-list="item.extra_group_list">
            <span v-if="setType === 'role_code'" class="setting-head-remark" slot="introduce">{{settingTip[item[setType]]}}</span>
            <bk-popover placement="bottom-end" :width="250" slot="setting-extra-tip">
                {{ settingTip.name }}
                <div class="setting-tip-content" slot="content">{{settingTip.content}}</div>
            </bk-popover>
        </user-groupings>
    </div>
</template>
<script>
    import UserGroupings from './UserGroupings'
    export default {
        name: '',
        components: {
            UserGroupings
        },
        props: {
            handleUpdate: {
                type: Function
            },
            authList: {
                type: Array,
                default: []
            },
            setType: {
                type: String
            },
            titleName: {
                type: String,
                default: 'role_name'
            },
            isLoading: {
                type: Boolean
            },
            projectGroupAndUsers: {
                type: Array,
                default: []
            }
        },
        computed: {
            settingTip () {
                return {
                    creator: this.$t('settings.creatorTips'),
                    manager: this.$t('settings.managerTips'),
                    executor: this.$t('settings.executorTips'),
                    viewer: this.$t('settings.viewerTips'),
                    name: this.$t('settings.addition'),
                    content: this.$t('settings.additionDesc')
                }
            }
        },
        methods: {
            handleAuthChange (data) {
                const { authList, handleUpdate } = this
                handleUpdate([
                    ...authList.slice(0, data.dataIndex),
                    {
                        ...authList[data.dataIndex],
                        user_list: data.usersGroup,
                        selected: data.selectedId
                    },
                    ...authList.slice(data.dataIndex + 1)
                ])
            }
        }
    }
</script>

<style lang="scss">
    .setting-head-remark {
        font-weight: normal;
        margin-left: 15px;
        line-height: 41px;
        font-size: 12px;
    }
</style>
