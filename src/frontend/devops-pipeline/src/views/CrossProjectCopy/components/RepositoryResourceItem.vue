<template>
    <!-- 代码库 -->
    <BaseResourceItem
        :key="item.copyStrategy"
        :item="item"
        :header-title="item.resourceName"
        :strategies="strategyOptions"
        :is-read-only="isReadOnly"
        @strategy-change="handleChange"
    >
        <template #header-extra>
            <p class="repo-url">
                <span>{{ item.resourceProperties?.repositoryType }}</span>
                <!-- <span class="separator">·</span> -->
                <span>{{ item.resourceProperties?.repositoryUrl }}</span>
            </p>
        </template>

        <template #extra-config>
            <div
                v-if="isCreateNew && !isOauth && !isReadOnly"
                class="auth-config-section"
            >
                <span class="info-icon">
                    <i class="devops-icon icon-exclamation" />
                </span>
                <span class="info-text">{{ $t('oauthNotAuthorized') }}</span>
                <bk-button
                    size="small"
                    theme="primary"
                    @click="handleOAuthAuthorize"
                >
                    OAuth
                </bk-button>
                <span
                    @click="handleRefresh"
                    class="refresh-icon"
                >
                    <Logo
                        name="refresh"
                        size="12"
                    />
                </span>
            </div>
        </template>
    </BaseResourceItem>
</template>

<script>
    import BaseResourceItem from './BaseResourceItem.vue'
    import Logo from '@/components/Logo'
    import { PipelineCopyStrategy } from '@/store/modules/crossProjectCopy/constants'

    export default {
        name: 'RepositoryResourceItem',
        components: {
            BaseResourceItem,
            Logo
        },
        props: {
            item: {
                type: Object,
                required: true
            },
            // 是否只读模式
            isReadOnly: {
                type: Boolean,
                default: false
            },
            // OAuth授权状态
            isOauth: {
                type: Boolean,
                default: false
            },
            // OAuth 授权 URL（isPACOAuth 返回 403 时传递）
            oauthUrl: {
                type: String,
                default: ''
            }
        },
        computed: {
            isCreateNew () {
                return this.item.copyStrategy === PipelineCopyStrategy.REPOSITORY_CREATE_NEW
            },
            strategyOptions () {
                return [
                    {
                        value: PipelineCopyStrategy.REPOSITORY_REUSE_SAME_NAME_PROTOCOL,
                        label: this.$t('reuseTargetRepository'),
                        description: this.$t('reuseTargetRepositoryDesc'),
                        disabled: !this.item.targetNameExists,
                        disabledTip: !this.item.targetNameExists ? this.$t('noMatchingRepository') : '',
                        showJumpIcon: false
                    },
                    {
                        value: PipelineCopyStrategy.REPOSITORY_CREATE_NEW,
                        label: this.$t('autoLinkToTarget'),
                        description: this.$t('autoLinkToTargetDesc'),
                        disabled: false
                    }
                ]
            }
        },
        methods: {

            handleOAuthAuthorize () {
                window.open(this.oauthUrl, '_blank')
            },
            handleChange (value) {
                this.$emit('strategy-change', value)
            },
            handleRefresh () {
                this.$emit('refresh-oauth-authorize', this.item)
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/scss/resource-dependency';

    .auth-config-section {
        display: flex;
        align-items: center;
        justify-content: end;
        margin-top: 12px;
        gap: 8px;
        border-radius: 2px;
        background: #FDF4E8;
        padding: 8px;

        .info-text {
            color: #E38B02;
            font-size: 12px;
            font-weight: 400;
            margin-right: 8px;
        }

        .info-icon {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            background-color: #F59500;
            color: #FFFFFF;
            width: 12px;
            height: 12px;
            padding: 8px;
            font-size: 10px;
            border-radius: 50%;
            flex-shrink: 0;
        }

        .refresh-icon {
            width: 26px;
            height: 26px;
            border-radius: 2px;
            border: 1px solid #3A84FF;
            color: #3A84FF;
            background:  #FFF;
            text-align: center;
            line-height: 26px;
        }
    }
</style>
