<template>
    <div
        class="advanced-content-wrapper"
        v-bkloading="{ isLoading }"
    >
        <template v-if="hookDetail.length">
            <header class="header">
                <span>{{ $t('environment.nodeInfo.cloudLoginHook') }}</span>
                <span class="login-hook-tips">
                    <i class="bk-icon icon-info-circle info-icon"></i>
                    {{ $t('environment.nodeInfo.loginHookTips') }}
                </span>
                <div class="checkbox-content">
                    <bk-checkbox
                        v-for="item in loginHooks"
                        class="checkbox-item"
                        v-model="item.enable"
                        :key="item.hookType"
                    >
                        {{ item.label }}
                    </bk-checkbox>
                </div>
            </header>
    
            <header class="header">
                <span>{{ $t('environment.nodeInfo.cloudLogoutHook') }}</span>
                <span class="login-hook-tips">
                    <i class="bk-icon icon-info-circle info-icon"></i>
                    {{ $t('environment.nodeInfo.logoutHookTips') }}
                </span>
                <div class="checkbox-content">
                    <bk-checkbox
                        v-for="item in logoutHooks"
                        class="checkbox-item"
                        v-model="item.enable"
                        :key="item.hookType"
                    >
                        {{ item.label }}
                    </bk-checkbox>
                </div>
            </header>
            <bk-button
                theme="primary"
                :loading="saveLoading"
                @click="handleSave"
            >
                {{ $t('environment.saveChange') }}
            </bk-button>
        </template>
    </div>
</template>

<script>
    export default {
        name: 'advanced-tab',
        props: {
            projectId: {
                type: String,
                required: true
            },
            envHashId: {
                type: String,
                required: true
            }
        },
        data () {
            return {
                isLoading: false,
                saveLoading: false,
                hookDetail: []
            }
        },
        computed: {
            hookTypeLabel () {
                return {
                    CLEAN_CREDENTIALS: this.$t('environment.nodeInfo.cleanCredentialsLabel'),
                    IOA_LOGIN: this.$t('environment.nodeInfo.ioaLoginLabel'),
                    IOA_LOGOUT: this.$t('environment.nodeInfo.ioaLogoutLabel')
                }
            },
            loginHooks () {
                return this.hookDetail.filter(i => i.executionType === 'LOG_IN').map(i => {
                    return {
                        ...i,
                        label: this.hookTypeLabel[i.hookType]
                    }
                })
            },
            logoutHooks () {
                return this.hookDetail.filter(i => i.executionType === 'LOG_OUT').map(i => {
                    return {
                        ...i,
                        label: this.hookTypeLabel[i.hookType]
                    }
                })
            }
        },
        created () {
            this.fetchEnvHookDetail()
        },
        methods: {
            async fetchEnvHookDetail () {
                try {
                    this.isLoading = true
                    const res = await this.$store.dispatch('environment/fetchEnvHookDetail', {
                        projectId: this.projectId,
                        envHashId: this.envHashId
                    })
                    this.hookDetail = res
                } catch (e) {
                    this.$bkMessage({
                        theme: 'error',
                        message: e.message || e
                    })
                } finally {
                    this.isLoading = false
                }
            },

            async handleSave () {
                try {
                    this.saveLoading = true
                    const res = await this.$store.dispatch('environment/updateEnvHookDetail', {
                        projectId: this.projectId,
                        envHashId: this.envHashId,
                        params: [
                            ...this.loginHooks,
                            ...this.logoutHooks
                        ]
                    })
                    
                    if (res) {
                        this.$bkMessage({
                            theme: 'success',
                            message: this.$t('environment.successfullySaved')
                        })
                    }
                } catch (e) {
                    this.$bkMessage({
                        theme: 'error',
                        message: e.message || e
                    })
                } finally {
                    this.saveLoading = false
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
    .advanced-content-wrapper {
        color: #2e2d2c;
        padding: 25px !important;
        .header {
            font-size: 20px;
            font-weight: 700;
            margin-bottom: 40px;
        }
        .login-hook-tips {
            font-size: 12px;
            font-weight: 100;
            color: #adacac;
            .info-icon {
                font-size: 14px;
                margin-left: 12px;
                color: #c3cdd7;
            }
        }
        .checkbox-content {
            margin-top: 5px;
            display: flex;
            flex-direction: column;
            .checkbox-item {
                margin-top: 8px;
                font-weight: normal;
            }
        }
    }
</style>
