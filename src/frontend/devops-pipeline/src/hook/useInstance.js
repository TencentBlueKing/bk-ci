export default function useInstance (vm) {
    const route = vm.$route
    const router = vm.$router
    const store = vm.$store
    const bkMessage = vm.$bkMessage
    const bkInfo = vm.$bkInfo
    const i18n = vm.$i18n
    const showTips = vm.showTips
    const validator = vm.validator
    const userInfo = vm.$userInfo
    return {
        route,
        router,
        store,
        bkMessage,
        bkInfo,
        i18n,
        showTips,
        validator,
        userInfo
    }
}
