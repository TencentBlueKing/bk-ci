<template>
    <div class="bk-login-dialog" v-if="isShow">
        <div class="bk-login-wrapper">
            <div class="login-tips" style="padding: 20px;">
                {{$t('登录失效，请')}} <a :href="loginUrl">{{$t('重新登录')}}>></a>
            </div>
        </div>
    </div>
</template>

<script>
    /**
     * app-auth
     * @desc 统一登录
     * @example1 <app-auth type="404"></app-auth>
     */

    export default {
        name: 'app-auth',
        data () {
            return {
                loginUrl: `${window.PAAS_SERVICE_URL}/?c_url=${window.location.href}`,
                isShow: false
            }
        },
        methods: {
            hideLoginModal () {
                this.isShow = false
            },
            showLoginModal () {
                this.isShow = true
            }
        }
    }
</script>

<style scoped>
    @import './index.css';
</style>
