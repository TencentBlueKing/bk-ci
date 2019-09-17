<template>
    <bk-dialog
        v-model="showLoginDialog"
        class="devops-login-dialog"
        :show-footer="false"
        :width="width"
        theme="primary"
        @confirm="toApplyPermission"
    >
        <main class="devops-login-iframe-container">
            <iframe
                :src="iframeSrc"
                scrolling="no"
                border="0"
                width="618"
                height="560"
            />
        </main>
    </bk-dialog>
</template>

<script lang='ts'>
    import Vue from 'vue'
    import { Component } from 'vue-property-decorator'
    import { Action } from 'vuex-class'

    @Component
    export default class LoginDialog extends Vue {
        @Action setUserInfo
        iframeSrc: string = `${LOGIN_SERVICE_URL}/plain?app_code=1&c_url=${location.origin}/console/static/login_success.html?is_ajax=1`
        showLoginDialog: boolean = true
        width: number = 666
        beforeDestroy () {
          location.reload()
        }
    }
</script>

<style lang="scss">
    @import '../../assets/scss/conf';

    .devops-login-dialog {
        .devops-login-iframe-container {
            height: 560px;
            iframe {
                border: 0
            }
        }
    }
</style>
