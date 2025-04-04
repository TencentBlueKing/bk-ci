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
            {{ username }}
            <i class="devops-icon icon-down-shape ml5" />
        </div>
        <template slot="content">
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
            window.open(`${window.location.origin}/console/${name}`, '_self')
        }

        get menu (): object[] {
            try {
                return [
                    {
                        to: '/console/pm',
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

    .user-menu-item {
        color: $fontWeightColor;
        cursor: pointer;
        &:hover {
            color: #737987 !important;
        }
    }
</style>
