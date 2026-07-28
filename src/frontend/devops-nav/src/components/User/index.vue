<template>
    <BkLoginUserinfo
        :userinfo="userinfo"
        :render-slot="renderSlot"
        :action-list="actionList"
    />
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Component, Prop } from 'vue-property-decorator'
    import { Action } from 'vuex-class'
    import { clickoutside } from '../../directives/index'
    import { addRoutePrefix } from '@/utils/util'
    import { getUserTimeZone } from '../../../../common-lib/time.js'
    import BkLoginUserinfo from '@blueking/login-userinfo/vue2'
    import('@blueking/login-userinfo/vue2/vue2.css')

    @Component({
        components: {
            BkLoginUserinfo
        },
        directives: {
            clickoutside
        }
    })
    export default class User extends Vue {
        @Prop()
        username: string

        @Prop()
        avatarUrl: string

        @Prop()
        chineseName: string

        @Prop()
        bkpaasUserId: string

        @Action togglePopupShow

        get tenantId (): string {
            const tenantInfo = (window as any).tenantInfoForDisplay
            return (tenantInfo && tenantInfo.tenantId) || ''
        }

        get userSettingUrl (): string {
            const domain = window.LOCALE_DOMAIN
            return domain ? `https://bkuser.${domain}` : ''
        }

        get actionList () {
            return [
                {
                    text: this.$t('projectManage'),
                    icon: 'projectManage',
                    theme: 'primary',
                    href: addRoutePrefix('/console/pm')
                },
                {
                    text: this.$t('accessCenter'),
                    icon: 'accessCenter',
                    theme: 'primary',
                    href: addRoutePrefix('/console/permission')
                },
                {
                    text: this.$t('oauthManage'),
                    icon: 'oauthManage',
                    theme: 'primary',
                    href: addRoutePrefix('/console/permission/auth/oauth')
                },
                {
                    text: this.$t('userSetting'),
                    icon: 'userCircle',
                    href: this.userSettingUrl,
                    target: '_blank',
                    theme: 'primary',
                },
                {
                    text: this.$t('logout'),
                    icon: 'logout',
                    theme: 'danger',
                    handle: this.logout,
                },
            ]
        }
        get userinfo () {
            const name = `${this.username}(${this.chineseName})`
            
            return {
                name,
                organization: this.tenantId || '--',
                timezone: getUserTimeZone(),
            }
        }

        renderSlot (h) {
            return h('bk-user-display-name', { 'user-id': `${this.username}(${this.chineseName})` })
        }

        logout (): void {
            try {
                const loginUrl = new URL(window.getLoginUrl())
                loginUrl.searchParams.append('is_from_logout', '1')
                console.log(loginUrl.href)
                window.location.href = loginUrl.href
            } catch (error) {
                console.error(error)
            }
        }
    }
</script>

<style lang="scss">
    @import '../../assets/scss/conf';

    $dropmenuWidth: 212px;

    .user-entry {
        display: flex;
        height: 32px;
        line-height: 32px;
        padding:0 12px;
        align-items: center;
    }

    .user-tenant-info {
        padding: 8px 16px 12px;
        border-bottom: 1px solid #f0f1f5;
        margin-bottom: 4px;
        min-width: $dropmenuWidth;
        box-sizing: border-box;
    }

    .user-tenant-info-row {
        display: flex;
        justify-content: space-between;
        align-items: center;
        line-height: 22px;
        font-size: 12px;
        & + .user-tenant-info-row {
            margin-top: 4px;
        }
    }

    .user-tenant-info-label {
        color: #979ba5;
        margin-right: 12px;
        flex-shrink: 0;
    }

    .user-tenant-info-value {
        color: #63656e;
        text-align: right;
        word-break: break-all;
    }

    .user-menu-item {
        color: $fontWeightColor;
        cursor: pointer;
        &:hover {
            color: #737987 !important;
        }
    }
</style>
