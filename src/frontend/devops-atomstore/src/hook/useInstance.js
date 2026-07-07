import { getCurrentInstance } from 'vue'
export default function useInstance () {
    const vm = getCurrentInstance()
    const proxy = vm.proxy
    return {
        vm,
        proxy,
        bkMessage: proxy.$bkMessage,
        bkInfo: proxy.$bkInfo,
        t: proxy.$i18n,
        validator: proxy.$validator,
        userInfo: proxy.$userInfo,
        h: proxy.$createElement
    }
}
