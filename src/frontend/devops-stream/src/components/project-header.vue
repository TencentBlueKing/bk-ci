<template>
    <header class="stream-header">
        <span class="header-info">
            <img class="ci-name" src="./../images/logo.svg" height="48" @click="goToHome" />

            <template v-if="$route.hash">
                <router-link
                    class="stream-link"
                    :to="{ name: 'pipeline' }"
                >
                    <icon
                        name="pipeline-link"
                        size="18"
                        class="link-icon"
                    ></icon>
                    {{$t('header.pipelines')}}
                </router-link>
                <router-link
                    class="stream-link"
                    :to="{ name: 'metric' }"
                    @click="setTitle($t('header.metrics'))"
                >
                    <icon
                        name="metric-link"
                        size="18"
                        class="link-icon"
                    ></icon>
                    {{$t('header.metrics')}}
                </router-link>
                <router-link
                    v-bk-tooltips="{ content: $t('exception.permissionDeny'), disabled: permission }"
                    :class="{
                        'stream-link': true,
                        disabled: !permission
                    }"
                    :is="permission ? 'router-link' : 'span'"
                    :to="{ name: 'basicSetting' }"
                    @click="setTitle($t('header.settings'))"
                >
                    <icon
                        name="setting-link"
                        size="18"
                        class="link-icon"
                    ></icon>
                    {{$t('header.settings')}}
                </router-link>
            </template>
        </span>

        <section class="user-info">
            <bk-select
                class="choose-project"
                searchable
                enable-scroll-load
                :popover-options="{ appendTo: 'parent' }"
                :clearable="false"
                :scroll-loading="bottomLoadingOptions"
                :value="projectInfo.id"
                @scroll-end="getProjectList"
                @selected="chooseProject"
            >
                <span
                    class="choosen-project"
                    slot="trigger"
                >
                    <span class="project-name text-ellipsis" v-bk-overflow-tips>
                        {{ projectInfo.name_with_namespace }}
                    </span>
                    <icon
                        name="cc-jump-link"
                        size="16"
                        class="jump-icon"
                        v-bk-tooltips="{ content: $t('header.goToGit') }"
                        @click.native="goToCode"
                    ></icon>
                    <icon
                        name="angle-down-line"
                        size="12"
                        class="angle-icon"
                    ></icon>
                </span>
                <bk-option v-for="option in projectList"
                    :key="option.id"
                    :id="option.id"
                    :name="option.nameWithNamespace">
                </bk-option>
            </bk-select>
            <span class="user-notifications" @click.stop="goToNotifications">
                <icon
                    size="18"
                    name="notify"
                ></icon>
                <span class="user-hint" v-if="messageNum > 0"></span>
            </span>
            <toggle-language></toggle-language>
            <user
                class="user-info"
                :user="user"
            />
        </section>
    </header>
</template>

<script>
    import { mapActions, mapState } from 'vuex'
    import { common } from '@/http'
    import user from './user'
    import LINK_CONFIG from '@/conf/link-config.js'
    import toggleLanguage from './toggle-language.vue'

    export default ({
        name: 'StreamHeader',
        components: {
            user,
            toggleLanguage
        },
        data () {
            return {
                LINK_CONFIG,
                bottomLoadingOptions: {
                    size: 'small',
                    isLoading: true
                },
                projectList: [],
                pageInfo: {
                    page: 1,
                    pageSize: 20,
                    loadEnd: false
                }
            }
        },
        computed: {
            ...mapState(['exceptionInfo', 'projectInfo', 'user', 'permission', 'messageNum'])
        },
        created () {
            this.getUserInfo()
            this.getProjectList()
        },
        methods: {
            ...mapActions(['setUser', 'setExceptionInfo']),

            setTitle (title) {
                document.title = title
            },

            getProjectList () {
                if (this.pageInfo.loadEnd) {
                    return
                }
                this.bottomLoadingOptions.isLoading = true
                return common.getStreamProjects('MY_PROJECT', this.pageInfo.page, this.pageInfo.pageSize, '').then((res) => {
                    this.pageInfo.loadEnd = !res.hasNext
                    this.pageInfo.page = 1 + this.pageInfo.page
                    this.projectList.push(...res.records)
                }).finally(() => {
                    this.bottomLoadingOptions.isLoading = false
                })
            },

            chooseProject (id) {
                const url = new URL(location.href)
                url.hash = `#${id}`
                location.href = url
                location.reload()
            },

            getUserInfo () {
                return common.getUserInfo().then((userInfo = {}) => {
                    this.setUser(userInfo)
                })
            },

            goToHome () {
                this.setExceptionInfo({ type: 200 })
                this.$router.push({
                    name: 'dashboard'
                })
            },

            goToNotifications () {
                this.$router.push({ name: 'notifications' })
            },

            goToCode () {
                window.open(this.projectInfo.https_url_to_repo, '_blank')
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
            justify-content: flex-start;
            align-items: center;
            flex: 1;
            .ci-name {
                display: inline-block;
                margin: 0 121px 0 12px;
                font-size: 16px;
                color: #f5f7fa;
                cursor: pointer;
            }
            .stream-link {
                display: flex;
                align-items: center;
                line-height: 22px;
                margin-right: 32px;
                cursor: pointer;
                color: #96A2B9;
                &.router-link-active {
                    color: #fff;
                }
                .link-icon {
                    margin-right: 4px;
                }
                &.disabled {
                    cursor: not-allowed;
                }
            }
        }
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

    .user-notifications {
        cursor: pointer;
        display: flex;
        align-items: center;
        color: #c3cdd7;
        margin-right: 25px;
        &:hover {
            color: #fff;
        }
        svg {
            margin-right: 4px;
        }
    }
    .user-hint {
        display: inline-block;
        width: 6px;
        height: 6px;
        border-radius: 50%;
        background-color: red;
        position: relative;
        left: -6px;
        top: -5px;
    }
    .choose-project {
        width: 250px;
        margin-right: 25px;
        border: none;
        &.is-focus .angle-icon {
            transform: rotate(180deg);
        }
        /deep/ .tippy-content, /deep/ .bk-tooltip-content {
            padding: 0;
        }
    }
    .choosen-project {
        display: flex;
        align-items: center;
        background: #252F43;
        line-height: 32px;
        padding: 0 10px;
        color: #D3D9E4;
        .project-name {
            flex: 1;
            max-width: calc(100% - 35px);
        }
        .jump-icon {
            margin-right: 9px;
        }
        .angle-icon {
            transition: transform 200ms;
        }
    }
</style>
