<template>
    <div
        v-clickoutside="hideUserInfo"
        :class="{ 'devops-user-info': true, 'active': show }"
    >
        <div
            class="user-entry"
            @click.stop="toggleUserInfo"
        >
            {{ username }}
            <span v-if="!isHideHint" class="user-header-hint" />
            <i v-if="!disabled" class="devops-icon icon-down-shape" />
        </div>
        <div
            v-if="show && !disabled"
            class="user-info-dropmenu"
        >
            <p class="user-avatar">
                <img
                    :src="avatarUrl"
                    alt="userAvatar"
                >
                <span>{{ chineseName }}</span>
            </p>
            <slot name="menu">
                <ul>
                    <li
                        v-for="(item, index) in menu"
                        :key="index"
                    >
                        <router-link
                            v-if="item.to"
                            class="user-menu-item"
                            :to="item.to"
                            @click.native="hideUserInfo(item.to)"
                        >
                            {{ item.label }}
                        </router-link>
                        <span
                            v-else-if="item.cb"
                            class="user-menu-item"
                            @click.stop="item.cb"
                        >{{ item.label }}</span>
                        <span v-if="!isHideHint && item.isShowHint" class="user-hint" />
                    </li>
                </ul>
            </slot>
        </div>
    </div>
</template>
<script lang="ts">
    import Vue from 'vue'
    import { Component, Prop, Watch } from 'vue-property-decorator'
    import { Action } from 'vuex-class'
    import bkLogout from '../../utils/bklogout.js'
    import { clickoutside } from '../../directives/index'

    const IS_HIDE_HINT = 'IS_HIDE_HINT'

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
        
        @Prop()
        disabled: boolean

        show: boolean = false

        @Action togglePopupShow

        toggleUserInfo () :void {
            if (!this.disabled) {
                this.show = !this.show
            }
        }

        hideUserInfo (to): void {
            this.show = false
            if (to === '/console/preci/') {
                localStorage.setItem(IS_HIDE_HINT, '1')
                this.isHideHint = Number(localStorage.getItem(IS_HIDE_HINT)) || 1
            }
        }

        @Watch('show')
        handleShow (show, oldVal) {
            if (show !== oldVal) {
                this.togglePopupShow(show)
            }
        }

        created () {
            this.isHideHint = Number(localStorage.getItem(IS_HIDE_HINT)) || 0
        }

        get menu (): object[] {
            try {
                const { projectId } = this.$route.params
                return [
                    {
                        to: '/console/pm',
                        label: this.$t('projectManage')
                    },
                    {
                        to: `/console/perm/my-perm?project_code=${projectId || ''}`,
                        label: this.$t('accessCenter')
                    },
                    {
                        to: '/console/preci/',
                        label: this.$t('PreCI'),
                        isShowHint: true
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
          bkLogout.logout()
          window.location.href = window.getLoginUrl()
        }
    }
</script>

<style lang="scss">
    @import '../../assets/scss/conf';

    $dropmenuWidth: 212px;
    .devops-user-info {
        position: relative;
        display: flex;
        align-items: center;
        color: $fontLighterColor;
        height: 100%;
        cursor: pointer;

        .user-entry {
            display: flex;
            height: 100%;
            padding:0 12px;
            align-items: center;
            .user-header-hint {
                display: inline-block;
                width: 7px;
                height: 7px;
                border-radius: 50%;
                background-color: red;
                margin: 0 5px;
            }
        }

        .devops-icon.icon-down-shape {
            vertical-align: -2px;
        }

        .user-info-dropmenu {
            // display: none;
            width: $dropmenuWidth;
            position: absolute;
            background: white;
            border: 1px solid $borderWeightColor;
            border-radius: 2px;
            box-shadow: 0 3px 6px rgba(51, 60, 72, 0.12);
            top: 53px;
            right: 0;
            cursor: default;
            &:after {
                position: absolute;
                content: '';
                width: 8px;
                height: 8px;
                border: 1px solid $borderWeightColor;
                border-bottom: 0;
                border-right: 0;
                transform: rotate(45deg);
                background: white;
                top: -5px;
                right: 36px;

            }
            .user-avatar {
                display: flex;
                align-items: center;
                padding-bottom: 15px;
                border-bottom: 1px solid $borderWeightColor;
                color: $fontWeightColor;
                padding: 20px;
                > img {
                    width: 34px;
                    height: 34px;
                }
                > span {
                    padding-left: 15px;
                }
            }
            > ul {
                margin: 20px;
                > li {
                    margin: 0 0 10px 0;
                    line-height: 24px;
                    &:last-child {
                        margin-bottom: 0;
                    }
                    .user-menu-item {
                        color: $fontWeightColor;
                        &:hover {
                            color: $aHoverColor;
                        }
                    }
                    .user-hint {
                        display: inline-block;
                        width: 6px;
                        height: 6px;
                        border-radius: 50%;
                        background-color: red;
                        position: relative;
                        top: -2px;
                    }
                }
            }
        }
    }
</style>
