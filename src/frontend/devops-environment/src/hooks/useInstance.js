import { getCurrentInstance } from 'vue'
export default function useInstance () {
    const vm = getCurrentInstance()
    const proxy = vm.proxy

    return {
        vm,
        proxy,
        bkMessage: proxy.$bkMessage,
        bkInfo: proxy.$bkInfo,
        i18n: proxy.$i18n,
        showTips: proxy.$showTips,
        validator: proxy.$validator,
        userInfo: proxy.$userInfo,
        h: proxy.$createElement
    }
}