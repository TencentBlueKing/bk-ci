<template>
    <header class="stream-header">
        <span class="header-info">
            <img class="ci-name" src="./../images/logo.svg" height="48" @click="goToHome" />

            <template v-if="$route.hash && !showMenu">
                <icon name="git" size="18" class="gray-icon"></icon>
                <span class="git-project-path" @click="goToCode">{{ projectInfo.path_with_namespace }}</span>
                <icon
                    name="setting"
                    size="18"
                    :class="computedIconClass"
                    @click.native="goToSetting"
                    v-bk-tooltips="{ content: $t('exception.permissionDeny'), disabled: permission }"
                ></icon>
            </template>
        </span>

        <div class="navigation-header" v-if="showMenu">
            <ol class="header-nav">
                <li v-for="item in menuList" :key="item.name" @click="clickMenu(item)" class="header-nav-item" :class="{ 'item-active': item.active }">
                    {{item.name}}
                    <icon v-if="item.url" name="cc-jump-link" size="16" class="gray-icon" style="margin-left: 2px;margin-top: 3px;"></icon>
                </li>
            </ol>
        </div>

        <section class="user-info">
            <a v-if="!showMenu" class="docs-link" :href="LINK_CONFIG.STREAM" target="_blank"><icon name="helper" class="gray-icon" size="20"></icon></a>
            <user
                class="user-info"
                :user="user"
                :message-num="messageNum"
            />
        </section>
    </header>
</template>

<script>
    import { mapActions, mapState } from 'vuex'
    import { common } from '@/http'
    import user from './user'
    import LINK_CONFIG from '@/conf/link-config.js'

    export default ({
        name: 'StreamHeader',
        components: {
            user
        },
        data () {
            return {
                LINK_CONFIG
            }
        },
        computed: {
            ...mapState(['exceptionInfo', 'projectInfo', 'projectId', 'user', 'permission', 'messageNum']),
            showMenu () {
                return this.$route.name === 'home' || this.$route.name === 'dashboard'
            },
            computedIconClass () {
                const name = this.$route.name
                const settingPages = ['setting', 'basicSetting', 'credentialList', 'agentPools', 'addAgent', 'agentList', 'agentDetail', 'poolSettings']
                const iconColor = settingPages.includes(name) ? 'blue-icon' : 'gray-icon'
                return [iconColor, 'setting']
            },
            menuList () {
                return [
                    {
                        name: 'Dashboard',
                        active: this.$route.name === 'dashboard',
                        routeName: 'dashboard'
                    },
                    {
                        name: 'ChangeLog',
                        active: false,
                        type: 'url',
                        url: LINK_CONFIG.CHANGE_LOG
                    },
                    {
                        name: 'Documentation',
                        active: false,
                        type: 'url',
                        url: LINK_CONFIG.STREAM
                    },
                    {
                        name: 'Issue',
                        active: false,
                        type: 'url',
                        url: LINK_CONFIG.ISSUE
                    }
                ]
            }
        },
        created () {
            this.getUserInfo()
        },
        methods: {
            ...mapActions(['setUser', 'setExceptionInfo']),

            getUserInfo () {
                return common.getUserInfo().then((userInfo = {}) => {
                    this.setUser(userInfo)
                })
            },
            clickMenu (item) {
                if (item.routeName) {
                    this.setExceptionInfo({ type: 200 })
                    this.$router.push({
                        name: item.routeName
                    })
                } else if (item.url) {
                    window.open(item.url, '_blank')
                }
            },
            goToSetting () {
                if (!this.permission) return

                this.setExceptionInfo({ type: 200 })
                this.$router.push({ name: 'basicSetting' })
            },

            goToCode () {
                window.open(this.projectInfo.web_url, '_blank')
            },

            goToHome () {
                this.setExceptionInfo({ type: 200 })
                this.$router.push({
                    name: this.$route.name === 'dashboard' ? 'home' : 'dashboard'
                })
            }
        }
    })
</script>

<style lang="postcss" scoped>
    .stream-header {
        height: 61px;
        padding: 0 20px 0 10px;
        background: #182132;
        /* border-bottom: 1px solid #dde4eb; */
        display: flex;
        align-items: center;
        justify-content: space-between;
        color: #f5f7fa;
        .header-info {
            display: flex;
            justify-content: center;
            align-items: center;
            .ci-name {
                display: inline-block;
                margin: 0 40px 0 12px;
                font-size: 16px;
                color: #f5f7fa;
                cursor: pointer;
            }
            .git-project-path {
                display: inline-block;
                margin: 0 8px;
                color: #f5f7fa;
                cursor: pointer;
                white-space: nowrap;
            }
            .setting {
                cursor: pointer;
            }
            .gray-icon {
                color: #979ba5;
                font-weight: normal;
            }
            .blue-icon {
                color: #3a84ff;
            }
        }
    }

    .bk-dropdown-list li {
        min-width: 65px;
        position: relative;
    }
    .unread:before {
        content: '';
        position: absolute;
        right: 16px;
        top: calc(50% - 3px);
        width: 8px;
        height: 8px;
        border-radius: 100px;
        background: #ff5656;
    }

    .dropdown-trigger-btn {
        cursor: pointer;
        font-size: 14px;
        .name {
            color: #f5f7fa;
            margin: 0 8px;
        }
    }

    .user-info {
        display: flex;
        align-items: center;
        a {
            color: #c3cdd7;
            margin-top: 3px;
            margin-right: 8px;
        }
        a:hover {
            color: #fff;
        }
    }

    .navigation-header {
        -webkit-box-flex:1;
        -ms-flex:1;
        flex:1;
        height:100%;
        display:-webkit-box;
        display:-ms-flexbox;
        display:flex;
        -webkit-box-align:center;
        -ms-flex-align:center;
        align-items:center;
        font-size:14px;
        margin-left: 100px;
    }
    .navigation-header .header-nav {
        display:-webkit-box;
        display:-ms-flexbox;
        display:flex;
        padding:0;
        margin:0;
    }
    .navigation-header .header-nav-item {
        list-style:none;
        height:50px;
        display:-webkit-box;
        display:-ms-flexbox;
        display:flex;
        -webkit-box-align:center;
        -ms-flex-align:center;
        align-items:center;
        margin-right:40px;
        color:#96A2B9;
        min-width:56px
    }
    .navigation-header .header-nav-item.item-active {
        color:#FFFFFF !important;
    }
    .navigation-header .header-nav-item:hover {
        cursor:pointer;
        color:#D3D9E4;
    }
</style>
