<template>
    <section class="bk-form pipeline-setting power" v-bkloading="{ isLoading }">
        <div class="bk-form-item powerType">
            <label class="bk-label label">{{ $t('settings.authType') }}：</label>
            <div class="bk-form-content">
                <label class="bk-form-radio" v-for="type in powerTypeList" :key="type.value">
                    <input
                        type="radio"
                        :value="type.value"
                        :name="type.name"
                        :checked="powerType === type.value"
                        @change="handlePowerRadio"
                    />
                    <i class="bk-radio-text">{{type.label}}</i>
                </label>
            </div>
        </div>
        <user-groupings
            v-show="powerType === 'role'"
            v-for="(item, index) in roles" :key="index"
            @groupingChange="dataChange"
            :max-width="maxWidth"
            :data-index="index"
            :setting-key="settingId"
            :select-key-text="settingName"
            :title="item.role_name"
            :input-label="$t('settings.user')"
            :pipeline-setting-user="item.user_list"
            :extra-user-list="item.extra_user_list"
            :select-label="$t('settings.userGroup')"
            :selected="item.selected"
            :list="projectGroupAndUsers"
            :extra-group-list="item.extra_group_list">
            <span class="setting-head-remark" slot="introduce">{{toolTipSet(item)}}</span>
            <bk-popover placement="bottom-end" slot="setting-extra-tip">
                {{settingTip.name}}
                <div class="setting-tip-content" slot="content">{{settingTip.content}}</div>
            </bk-popover>
        </user-groupings>
        <user-groupings
            v-show="powerType === 'policy'"
            v-for="(item, index) in policies" :key="index"
            @groupingChange="dataChange"
            :max-width="maxWidth"
            :data-index="index"
            :setting-key="settingId"
            :select-key-text="settingName"
            :title="item.policy_name"
            :input-label="$t('settings.user')"
            :pipeline-setting-user="item.user_list"
            :extra-user-list="item.extra_user_list"
            :select-label="$t('settings.userGroup')"
            :selected="item.selected"
            :list="projectGroupAndUsers"
            :extra-group-list="item.extra_group_list">
            <!-- <span class="setting-head-remark" slot="introduce">{{toolTipSet(item)}}</span> -->
            <bk-popover placement="bottom-end" slot="setting-extra-tip">
                {{settingTip.name}}
                <div class="setting-tip-content" slot="content">{{settingTip.content}}</div>
            </bk-popover>
        </user-groupings>
        <div class="opt-btn" v-if="!isLoading">
            <bk-button @click="saveUserSetting" :disabled="isDisabled" type="primary">{{ $t('save') }}</bk-button>
            <bk-button @click="exit" :disabled="isDisabled">{{ $t('cancel') }}</bk-button>
        </div>
    </section>
</template>

<script>
    import UserGroupings from '@/components/pipelineSetting/UserGroupings'
    import { mapActions, mapState } from 'vuex'
    import { HttpError } from '@/utils/util'
    import * as cookie from 'js-cookie'

    export default {
        components: {
            UserGroupings
        },
        props: {
            settingId: {
                type: String,
                default: 'groupRoleId'
            },
            isDisabled: {
                type: Boolean,
                default: false
            },
            settingName: {
                type: String,
                default: 'groupName'
            }
        },
        data () {
            return {
                isLoading: true,
                maxWidth: '70%',
                settingTip: {
                    creator: this.$t('settings.creatorTips'),
                    manager: this.$t('settings.managerTips'),
                    executor: this.$t('settings.executorTips'),
                    viewer: this.$t('settings.viewerTips'),
                    name: this.$t('settings.addition'),
                    content: this.$t('settings.additionDesc')
                },
                powerType: 'role',
                powerTypeList: [
                    {
                        label: this.$t('settings.accordingRoles'),
                        name: 'powerType',
                        value: 'role'
                    },
                    {
                        label: this.$t('settings.accordingFunc'),
                        name: 'powerType',
                        value: 'policy'
                    }
                ],
                roles: null,
                policies: null
            }
        },
        computed: {
            ...mapState('common', [
                'pipelineSetting',
                'projectGroupAndUsers'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            },
            longProjectId () {
                return this.$store.state.curProject.projectId || ''
            }
        },
        watch: {
            '$route.params.pipelineId': async function (pipelineId, oldId) {
                this.isEditing = false
                this.resetFlag = true
                await this.requestRoleList()
            }
        },
        async created () {
            if (!this.projectGroupAndUsers.length) {
                this.requestProjectGroupAndUsers(this.$route.params)
            }
            if (!this.longProjectId) {
                await this.$store.dispatch('requestProjectDetail', { projectId: this.$route.params.projectId })
            }
            await this.requestRoleList()
        },
        methods: {
            ...mapActions('pipelines', [
                'requestProjectGroupAndUsers'
            ]),
            dataChange (data) {
                const dataIndex = data.dataIndex
                if (this.powerType === 'role') {
                    this.roles[dataIndex].user_list = data.usersGroup
                    this.roles[dataIndex].selected = data.selectedId
                } else {
                    this.policies[dataIndex].user_list = data.usersGroup
                    this.policies[dataIndex].selected = data.selectedId
                }
                this.isStateChange(true)
            },
            isStateChange (isChange) {
                this.$emit('setState', {
                    isEditing: isChange
                })
            },
            toolTipSet (roleItem) {
                const item = roleItem.role_code
                return this.settingTip[item]
            },
            async requestRoleList () {
                const res = await this.$store.dispatch('pipelines/requestRoleList', {
                    projectId: this.longProjectId,
                    pipelineId: this.pipelineId
                })
                this.roles = res.role
                this.roles.map((rolesItem) => {
                    const arr = []
                    rolesItem.group_list.map((item) => {
                        arr.push(item.group_id)
                    })
                    rolesItem.selected = arr
                })
                this.policies = res.policy
                this.policies.map((policyItem) => {
                    const arr = []
                    policyItem.group_list.map((item) => {
                        arr.push(item.group_id)
                    })
                    policyItem.selected = arr
                })
                this.isLoading = false
            },
            async saveUserSetting () {
                const data = {
                    project_id: this.longProjectId,
                    resource_type_code: 'pipeline',
                    resource_code: this.pipelineId,
                    role: this.roles,
                    policy: this.policies
                }
                this.isDisabled = true
                data.role.map((item) => {
                    item.group_list = item.selected
                })
                data.policy.map((item) => {
                    item.group_list = item.selected
                })
                try {
                    const res = await this.$ajax.put(`/backend/api/perm/service/pipeline/mgr_resource/permission/`, data, { headers: { 'X-CSRFToken': cookie.get('paas_perm_csrftoken') } })
                    if (res) {
                        if (res.code === 403) {
                            throw new HttpError(403, res.message)
                        } else {
                            this.$showTips({
                                message: `${this.pipelineSetting.pipelineName}${this.$t('updateSuc')}`,
                                theme: 'success'
                            })
                            this.isStateChange(false)
                        }
                    }
                } catch (err) {
                    if (err.code === 403) { // 没有权限执行
                        this.askManagePermission()
                    } else {
                        this.$showTips({
                            message: err.message || err,
                            theme: 'error'
                        })
                    }
                }
                this.isDisabled = false
            },
            handlePowerRadio (e) {
                const { value } = e.target
                this.powerType = value
            },
            exit () {
                this.$emit('cancel')
            },
            askManagePermission () {
                this.$showAskPermissionDialog({
                    noPermissionList: [{
                        actionId: this.$permissionActionMap.manage,
                        resourceId: this.$permissionResourceMap.pipeline,
                        instanceId: [{
                            id: this.pipelineId,
                            name: this.pipelineSetting.pipelineName
                        }],
                        projectId: this.projectId
                    }],
                    applyPermissionUrl: `/backend/api/perm/apply/subsystem/?client_id=pipeline&project_code=${this.projectId}&service_code=pipeline&role_manager=pipeline:${this.pipelineId}`
                })
            }
        }
    }
</script>

<style lang="scss">
    @import './../../../scss/conf.scss';
    .pipeline-setting.power{
        min-height: 100%;
        .powerType {
            margin-bottom: 10px;
            .label {
                width: 90px;
                text-align: left;
            }
            .bk-form-content {
                margin-left: 90px
            }
        }
    }
    .setting-head-remark {
        margin-left: 15px;
        line-height: 41px;
        font-size: 12px;
        font-weight: 400;
        // color: #c4cdd6;
    }
    .opt-btn {
        margin-top: 30px;
    }
</style>
