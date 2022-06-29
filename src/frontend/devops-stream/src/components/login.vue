<template>
    <bk-dialog
        :value="isShowDialog"
        :show-footer="false"
        @after-leave="hiddleDialog"
    >
        <h3>Sign in for Stream CI</h3>
        <ul>
            <li @click="signIn('github')">
                <icon
                    name="github-logo"
                ></icon>
                SIGN IN WITH GITHUB
            </li>
        </ul>
    </bk-dialog>
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

            const hiddleDialog = () => {
                store.dispatch('setShowLoginDialog', false)
            }

            return {
                isShowDialog,
                signIn,
                hiddleDialog
            }
        }
    })
</script>

<style lang="postcss" scoped>

</style>
