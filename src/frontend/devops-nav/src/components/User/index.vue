<template>
    <bk-popover
        theme="light navigation-message"
        placement="bottom"
        trigger="click"
        :arrow="false"
        ref="popoverRef"
        :on-hide="handleHide"
        :on-show="handleShow"
    >
        <div
            class="user-entry"
        >
            <bk-user-display-name :user-id="username"></bk-user-display-name>
            <i class="devops-icon icon-down-shape ml5" />
        </div>
        <template slot="content">
            <div class="user-tenant-info">
                <p class="user-tenant-info-row">
                    <span class="user-tenant-info-label">{{ $t('enterpriseSpace') }}</span>
                    <span class="user-tenant-info-value">{{ tenantId || '--' }}</span>
                </p>
                <p class="user-tenant-info-row">
                    <span class="user-tenant-info-label">{{ $t('defaultTimeZone') }}</span>
                    <span class="user-tenant-info-value">{{ timeZone || '--' }}</span>
                </p>
            </div>
            <li
                v-for="(item, index) in menu"
                :key="index"
                class="bkci-dropdown-item"
            >
                <router-link
                    v-if="item.to"
                    class="user-menu-item"
                    :to="item.to"
                    @click="hideUserInfo"
                >
                    {{ item.label }}
                </router-link>
                <span
                    v-else-if="item.cb"
                    class="user-menu-item"
                    @click.stop="item.cb(item.name)"
                >{{ item.label }}</span>
            </li>
        </template>
    </bk-popover>
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Component, Prop } from 'vue-property-decorator'
    import { Action } from 'vuex-class'
    import { clickoutside } from '../../directives/index'
    import { addRoutePrefix } from '@/utils/util'
    import { DEFAULT_USER_TIME_ZONE } from '../../../../common-lib/time'

    @Component({
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

        get timeZone (): string {
            const tenantInfo = (window as any).tenantInfoForDisplay
            const userInfo = window.userInfo
            return (tenantInfo && tenantInfo.timeZone)
                || (userInfo && userInfo.timeZone)
                || DEFAULT_USER_TIME_ZONE
        }

        hideUserInfo (item): void {
            this.$refs.popoverRef.hideHandler()
        }

        handleShow () {
            this.togglePopupShow(true)
        }

        handleHide () {
            this.togglePopupShow(false)
        }

        updatePage (name) {
            window.open(`${window.location.origin}${window.getRoutePrefix()}/${name}`, '_self')
        }

        get menu (): object[] {
            try {
                return [
                    {
                        to: addRoutePrefix('/console/pm'),
                        label: this.$t('projectManage')
                    },
                    {
                        cb: this.updatePage,
                        label: this.$t('accessCenter'),
                        name: 'permission'
                    },
                    {
                        cb: this.updatePage,
                        label: this.$t('oauthManage'),
                        name: 'permission/auth/oauth'
                    },
                    {
                        cb: this.logout,
                        label: this.$t('logout')
                    }
                ]
            } catch (e) {
                console.warn(e)
                return []
            }
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
