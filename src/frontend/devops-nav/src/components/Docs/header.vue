<template>
    <div
        class="devops-docs-header"
        :class="{ 'header-static': backgroundStatic }"
    >
        <div class="header-left-bar">
            <router-link
                class="header-logo"
                to="/console"
            >
                <Logo
                    name="devops-logo"
                    width="160"
                />
            </router-link>
        </div>
        <ul class="header-right-bar">
            <li>
                <User
                    class="user-info"
                    v-bind="user"
                >
                    <ul slot="menu">
                        <li>
                            <span
                                class="user-menu-item"
                                @click.stop="logout"
                            >退出</span>
                        </li>
                    </ul>
                </User>
            </li>
            <li>
                <router-link
                    class="icon-btn"
                    to="/console"
                >
                    控制台
                </router-link>
            </li>
        </ul>
    </div>
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Component } from 'vue-property-decorator'
    import { State } from 'vuex-class'
    import User from '../User/index.vue'
    import bkLogout from '../../utils/bklogout.js'

    @Component({
        components: {
            User
        }
    })
    export default class Header extends Vue {
        @State user

        isDropdownMenuVisible: boolean = false
        backgroundStatic: boolean = false

        created () {
            this.monitorScroll()
        }

        // 监听滚动事件（滚动时头部样式切换）
        handleScroll (): void {
            if (window.scrollY > 0) {
                this.backgroundStatic = true
            } else {
                this.backgroundStatic = false
            }
        }

        monitorScroll (): void {
            document.addEventListener('scroll', this.handleScroll, false)
        }

        cancelScroll (): void {
            document.removeEventListener('scroll', this.handleScroll, false)
        }

        beforeDestroy () {
            this.cancelScroll()
        }

        logout (): void {
            bkLogout.logout()
            window.location.href = LOGIN_SERVICE_URL + '/?c_url=' + window.location.href
        }
    }
</script>

<style lang="scss">
    @import '../../assets/scss/conf';
    .devops-docs-header {
        display: flex;
        align-items: center;
        min-width: 1280px;
        position: fixed;
        width: 100%;
        padding: 20px 30px;
        -webkit-box-sizing: border-box;
        box-sizing: border-box;
        height: 72px;
        z-index: 1500;
        -webkit-transition: all .5s;
        -moz-transition: all .5s;
        -ms-transition: all .5s;
        transition: all .5s;
        &.header-static, &:hover {
            background: rgba(0, 0, 0, 0.8);
        }

        .header-left-bar {
            height: 100%;
            flex: 1;
            display: flex;
            align-items: center;
            .header-logo {
                width: 230px;
            }
        }

        .header-right-bar {
            justify-self: flex-end;
            height: $headerHeight;
            display: flex;
            display: flex;
            align-items: center;
            a, .user-info>span {
                color: #fff;
                font-size: 14px;
            }
            & >li {
                margin: 0 15px;
            }
            > .user-info {
                padding:0 12px;
                margin: 0 10px;
            }
        }
    }
</style>
