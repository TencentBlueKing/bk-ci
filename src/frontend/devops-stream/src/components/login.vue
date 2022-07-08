<template>
    <section
        class="login-home"
        v-if="isShowDialog"
    >
        <dashboard-header></dashboard-header>
        <img src="/static/images/login-top.png" class="image-top">
        <section class="login-main">
            <h3 class="login-title">{{ $t('signIn') }}</h3>
            <ul class="login-buttons">
                <li class="login-button" @click="signIn('github')">
                    <icon
                        name="github-logo"
                        size="30"
                    ></icon>
                    {{ $t('signInGithub') }}
                </li>
            </ul>
        </section>
        <span class="svg-bottom">
            <span></span>
            <span></span>
            <span></span>
        </span>
        <span class="bottom-info">Copyright © 2012-2022 Tencent BlueKing. All Rights Reserved. 腾讯蓝鲸 版权所有</span>
    </section>
</template>

<script>
    import {
        defineComponent,
        computed
    } from '@vue/composition-api'
    import {
        common
    } from '@/http'
    import store from '@/store'
    import {
        messageError
    } from '@/common/bkmagic'
    import dashboardHeader from '@/components/dashboard-header.vue'

    export default defineComponent({
        components: {
            dashboardHeader
        },
        setup () {
            const isShowDialog = computed(() => {
                return store.state.showLoginDialog
            })

            const signIn = (type) => {
                common
                    .getLoginUrl(type)
                    .then((res) => {
                        location.href = res
                    })
                    .catch((err) => {
                        messageError(err.message || err)
                    })
            }

            return {
                isShowDialog,
                signIn
            }
        }
    })
</script>

<style lang="postcss" scoped>
    .login-home {
        z-index: 9999;
        position: fixed;
        top: 0;
        left: 0;
        bottom: 0;
        right: 0;
        background-color: #fff;
    }
    .login-main {
        position: absolute;
        top: calc(50% - 200px);
        left: calc(50% - 335px);
        height: 370px;
        width: 670px;
        background: #FFFFFF;
        box-shadow: 1px 1px 6px 0 rgba(0,0,0,0.50);
        border-radius: 10px;
    }
    .login-title {
        text-align: center;
        margin-top: 50px;
        height: 59px;
        line-height: 59px;
        font-weight: 600;
        font-size: 42px;
        color: #000000;
    }
    .login-buttons {
        display: flex;
        align-items: center;
        flex-direction: column;
        margin-top: 77px;
    }
    .login-button {
        position: relative;
        display: flex;
        align-items: center;
        background: #182132;
        border-radius: 5px;
        color: white;
        line-height: 72px;
        width: 370px;
        text-align: center;
        font-size: 19px;
        border-radius: 5px;
        cursor: pointer;
        padding-left: 30px;
        svg {
            margin-right: 70px;
        }
        &:before {
            content: '';
            background: #fff;
            width: 1px;
            height: 30px;
            position: absolute;
            left: 95px;
            top: 21px;
        }
    }
    .image-top {
        position: absolute;
        width: 440px;
        top: calc(50% - 330px);
        left: calc(50% - 220px);
    }
    .svg-bottom {
        position: absolute;
        top: calc(50% + 250px);
        width: 100%;
        height: 20px;
        background: #D7E6FA;
        display: flex;
        align-items: center;
        justify-content: space-around;
        span {
            border-radius: 100%;
            width: 71px;
            height: 71px;
            background: #D7E6FA;
        }
    }
    .bottom-info {
        position: absolute;
        width: 100%;
        bottom: 30px;
        font-weight: 400;
        font-size: 14px;
        color: #000000;
        text-align: center;
    }
</style>
