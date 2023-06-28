<template>

    <bk-popover
        theme="light navigation-message"
        placement="bottom"
        :arrow="false"
        trigger="click"
        ref="popoverRef">
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
                    @click.native="hideUserInfo"
                >
                    {{ item.label }}
                </router-link>
                <span
                    v-else-if="item.cb"
                    class="user-menu-item"
                    @click.stop="item.cb"
                >{{ item.label }}</span>
            </li>
        </template>
    </bk-popover>
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Component, Prop, Watch } from 'vue-property-decorator'
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

        show: boolean = false

        @Action togglePopupShow

        toggleUserInfo (show: boolean): void {
            this.show = !this.show
        }

        hideUserInfo (item): void {
            this.show = false
            if (item) {
                if (item.to === this.$route.fullPath) return
                this.$router.push(item.to)
            }
        }

        @Watch('show')
        handleShow (show, oldVal) {
            if (show !== oldVal) {
                this.togglePopupShow(show)
            }
        }

        get menu (): object[] {
            try {
                return [
                    {
                        to: '/console/pm',
                        label: this.$t('projectManage')
                    },
                    {
                        to: '/console/permission',
                        label: this.$t('myPermission')
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
                const url = new URL(window.getLoginUrl())
                url.searchParams.append('is_from_logout', '1')
                console.log(url.href)
                window.location.href = url.href
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
