<template>
    <section
        class="login-home"
        v-if="isShowDialog"
    >
        <section class="login-main">
            <h3 class="login-title">Sign in for Stream CI</h3>
            <ul class="login-buttons">
                <li class="login-button" @click="signIn('github')">
                    <icon
                        name="github-logo"
                        size="25"
                    ></icon>
                    SIGN IN WITH GITHUB
                </li>
            </ul>
        </section>
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

    export default defineComponent({
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
        background-color: rgba(0,0,0,.2);
    }
    .login-main {
        position: absolute;
        top: calc(50% - 350px);
        left: calc(50% - 350px);
        height: 500px;
        width: 700px;
        background: linear-gradient(135deg, #5c73d9 10%, #dd7d7d 100%);
        color: #fff;
        border-radius: 4px;
    }
    .login-title {
        font-size: 30px;
        line-height: 34px;
        text-align: center;
        margin-top: 50px;
    }
    .login-buttons {
        display: flex;
        align-items: center;
        flex-direction: column;
        margin-top: 50px;
    }
    .login-button {
        display: flex;
        align-items: center;
        background-image: linear-gradient(135deg, #FAB2FF 10%, #1904E5 100%);
        border: none;
        color: white;
        line-height: 50px;
        width: 60%;
        text-align: center;
        text-decoration: none;
        font-size: 16px;
        border-radius: 8px;
        cursor: pointer;
        background-size: 200%;
        transition: 0.6s;
        outline: none;
        padding-left: 30px;
        svg {
            margin-right: 70px;
        }
        &:hover {
            box-shadow: 0 10px 20px 0 rgba(47,55,213,0.3);
            background-position: right;
        }
    }
</style>
