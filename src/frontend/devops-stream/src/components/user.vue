<template>
    <div
        v-bk-clickoutside="hideUserInfo"
        :class="{ 'devops-user-info': true, 'active': show }"
    >
        <div
            class="user-entry"
            @click.stop="toggleUserInfo"
        >
            {{ user.username }}
            <bk-icon type="down-shape" />
        </div>
        <div
            v-if="show"
            class="user-info-dropmenu"
        >
            <p class="user-avatar">
                <img
                    :src="user.avatarUrl"
                    alt="userAvatar"
                >
                <span>{{ user.chineseName }}</span>
            </p>
            <slot name="menu">
                <ul>
                    <li v-if="!showLoginDialog">
                        <span class="user-menu-item" @click.stop="logout">
                            {{$t('logout')}}
                        </span>
                    </li>
                </ul>
            </slot>
        </div>
    </div>
</template>

<script>
    import { mapState } from 'vuex'

    export default ({
        props: {
            user: {
                type: Object,
                required: true
            }
        },

        data () {
            return {
                show: false
            }
        },

        computed: {
            ...mapState(['showLoginDialog'])
        },

        methods: {
            toggleUserInfo () {
                this.show = !this.show
            },

            hideUserInfo () {
                this.show = false
            },

            logout () {
                location.href = window.getLoginUrl(`http://${location.hostname}/_logout/`)
            }
        }
    })
</script>

<style lang="postcss" scoped>
    @import '@/css/conf';

    $dropmenuWidth: 212px;
    .devops-user-info {
        position: relative;
        display: flex;
        align-items: center;
        color: $fontLigtherColor;
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
            &:hover {
                color: #fff;
            }
        }

        .icon-down-shape {
            vertical-align: -2px;
            margin-left: 2px;
        }

        .user-info-dropmenu {
            width: $dropmenuWidth;
            position: absolute;
            background: white;
            border: 1px solid $borderWeightColor;
            border-radius: 2px;
            box-shadow: 0 3px 6px rgba(51, 60, 72, 0.12);
            top: 42px;
            right: 0;
            z-index: 3;
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
                        cursor: pointer;
                        &:hover {
                            color: $primaryColor;
                        }
                    }
                }
            }
        }
    }
</style>
