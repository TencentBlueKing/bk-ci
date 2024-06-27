<template>
    <main class="delegation-permission" v-bkloading="{ isLoading }">
        <section class="content-warpper">
            <header class="header">
                <logo class="mr5" name="help-document-fill" size="20" />
                {{ $t('delegationPermission') }}
            </header>
            <div class="content">
                <p>{{ $t('delegation.tips1') }}</p>
                <i18n
                    class="mt10"
                    tag="p"
                    path="delegation.tips2">
                    <span class="highlight">{{ $t('delegation.pipelineExecPermission') }}</span>
                </i18n>
                <i18n
                    tag="p"
                    path="delegation.tips3">
                    <span class="highlight">{{ 'BK_CI_START_USER_ID' }}</span>
                </i18n>
                <p class="mt20">{{ $t('delegation.tips4') }}</p>
                <i18n
                    tag="p"
                    path="delegation.tips5">
                    <span class="highlight">{{ $t('delegation.newOperator') }}</span>
                </i18n>
            </div>
        </section>

        <section class="mt30">
            <div class="form-field">
                <label class="bk-label">{{ $t('delegation.proxyHolderForExecutionPermissions') }}</label>
                <span class="bk-form-content">{{ resourceAuthData.handoverFrom }}</span>
                <span
                    class="refresh-auth"
                    v-perm="{
                        hasPermission: hasResetPermission,
                        disablePermissionApi: true,
                        permissionData: {
                            projectId,
                            resourceType: 'pipeline',
                            resourceCode: pipelineId,
                            action: RESOURCE_ACTION.MANAGE
                        }
                    }"
                    @click="handleShowResetDialog">
                    <logo class="mr10" name="refresh" size="14" />
                    {{ $t('delegation.resetAuthorization') }}
                </span>
            </div>
            <div class="form-field">
                <label class="bk-label">{{ $t('delegation.authTime') }}</label>
                <span class="bk-form-content">{{ convertTime(resourceAuthData.handoverTime) }}</span>
            </div>
        </section>
        
        <bk-dialog
            ext-cls="reset-auth-confirm-dialog"
            :value="showResetDialog"
            :show-footer="false"
            @value-change="handleToggleShowResetDialog"
        >
            <span class="close-confirm-title">
                {{ $t('delegation.confirmReset') }}
            </span>
            <span class="close-confirm-tips">
                <i18n
                    tag="p"
                    path="delegation.resetAuthTips1">
                    <span class="highlight">{{ $t('delegation.yourPermission') }}</span>
                </i18n>
                <p>{{ $t('delegation.resetAuthTips2') }}</p>
            </span>
            <span class="close-confirm-footer">
                <bk-button
                    class="mr10 btn"
                    theme="primary"
                    :loading="resetLoading"
                    @click="handleReset"
                >
                    {{ $t('delegation.reset') }}
                </bk-button>
                <bk-button
                    class="btn"
                    :loading="resetLoading"
                    @click="showResetDialog = !showResetDialog">
                    {{ $t('delegation.cancel') }}
                </bk-button>
            </span>
        </bk-dialog>
    </main>
</template>

<script>
    import Logo from '@/components/Logo'
    import { mapActions, mapState } from 'vuex'
    import { convertTime } from '@/utils/util'
    import {
        RESOURCE_ACTION
    } from '@/utils/permission'
    export default {
        components: {
            Logo
        },
        data () {
            return {
                showResetDialog: false,
                resetLoading: false,
                resourceAuthData: {}
            }
        },
        computed: {
            ...mapState('atom', ['pipelineInfo']),
            hasResetPermission () {
                return this.pipelineInfo.permissions.canManage
            },
            RESOURCE_ACTION () {
                return RESOURCE_ACTION
            },
            pipelineId () {
                return this.pipelineInfo.pipelineId
            },
            projectId () {
                return this.$route.params.projectId
            }
        },
        created () {
            this.fetchResourceAuth()
        },
        methods: {
            convertTime,
            ...mapActions('pipelines', [
                'getResourceAuthorization',
                'resetPipelineAuthorization'
            ]),
            handleShowResetDialog () {
                this.showResetDialog = true
            },
            handleToggleShowResetDialog (val) {
                if (!val) {
                    this.showResetDialog = false
                    this.resetLoading = false
                }
            },
            async fetchResourceAuth () {
                try {
                    this.resourceAuthData = await this.getResourceAuthorization({
                        projectId: this.projectId,
                        resourceType: 'pipeline',
                        resourceCode: this.pipelineId
                    })
                } catch (e) {
                    console.error(e)
                }
            },
            async handleReset () {
                this.resetLoading = true
                try {
                    this.resetLoading = false
                    const res = await this.resetPipelineAuthorization({
                        projectId: this.projectId,
                        params: {
                            projectCode: this.projectId,
                            resourceType: 'pipeline',
                            handoverChannel: 'OTHER',
                            resourceAuthorizationHandoverList: [
                                {
                                    projectCode: this.projectId,
                                    resourceType: 'pipeline',
                                    resourceName: this.resourceAuthData.resourceName,
                                    resourceCode: this.resourceAuthData.resourceCode,
                                    handoverFrom: this.resourceAuthData.handoverFrom,
                                    handoverTo: this.$userInfo.username
                                }
                            ]
                        }
                    })
                    this.showResetDialog = false
                    if (res.SUCCESS.length) {
                        this.fetchResourceAuth()
                        this.$bkMessage({
                            theme: 'success',
                            message: this.$t('delegation.resetSuc')
                        })
                    } else if (res.FAILED.length) {
                        const message = res.FAILED[0]?.handoverFailedMessage || ''
                        this.$bkMessage({
                            theme: 'error',
                            message
                        })
                    }
                } catch (e) {
                    this.resetLoading = false
                    this.$bkMessage({
                        theme: 'error',
                        message: e.message || e
                    })
                }
            }
        }
    }
</script>

<style lang="scss">
    .delegation-permission {
        padding: 24px;
        font-size: 12px;
        .content-warpper {
            border-radius: 2px;
            border: 1px solid #DCDEE5;
        }
        .header {
            display: flex;
            align-items: center;
            height: 40px;
            background-color: #F5F7FA;
            padding: 0 16px;
            font-size: 14px;
            font-weight: 700;
        }
        .content {
            padding: 20px 16px;
            line-height: 24px;
            color: #63656E;
            font-size: 12px;
        }
        .highlight {
            color: #FF9C01;
            font-weight: 700;
        }
        .form-field {
            display: flex;
            font-size: 12px;
            padding-left: 5px;
            margin-bottom: 10px;
        }
        .bk-form-content {
            color: #313238;
        }
        .refresh-auth {
            display: flex;
            align-items: center;
            margin-left: 20px;
            cursor: pointer;
            color: #3A84FF;
        }
    }
    .reset-auth-confirm-dialog {
        text-align: center;
        .bk-dialog-body {
            display: flex;
            flex-direction: column;
            align-items: center;
            max-height: calc(50vh - 50px);
        }
        
        .close-confirm-title {
            font-size: 20px;
            color: #313238;
            margin-bottom: 15px;
        }
        
        .close-confirm-tips {
            text-align: left;
            color: #63656E;
            font-size: 12px;
            padding: 10px 20px;
            background: #F5F7FA;
            span {
                color: #FF9C01;
            }
        }
        .close-confirm-footer {
            margin-top: 24px;
            .btn {
                width: 88px;
            }
        }
    }
</style>
