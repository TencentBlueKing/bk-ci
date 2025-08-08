import { getCurrentInstance } from 'vue'
import { useI18n } from 'vue-i18n-bridge'
export default function useInstance () {
    const vm = getCurrentInstance()
    const proxy = vm.proxy
    const { t } = useI18n()

    return {
        vm,
        proxy,
        t,
        bkMessage: proxy.$bkMessage,
        bkInfo: proxy.$bkInfo,
        i18n: proxy.$i18n,
        showTips: proxy.$showTips,
        validator: proxy.$validator,
        userInfo: proxy.$userInfo,
        h: proxy.$createElement
    }
}
