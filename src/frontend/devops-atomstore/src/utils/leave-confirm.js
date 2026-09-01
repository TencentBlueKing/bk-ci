/**
 * @desc 页面编辑状态未保存离开确认
 * @param {Function} $bkInfo - bkInfo 方法
 * @param {Function} $t - 国际化方法
 * @param {string} message - 可选的自定义提示信息
 * @returns {Promise<boolean>}
 */
const leaveConfirm = ($bkInfo, $t, message) => {
    return new Promise((resolve) => {
        $bkInfo({
            title: $t('store.确认离开当前页？'),
            subTitle: message || $t('store.离开将会导致未保存信息丢失'),
            confirmFn: () => {
                resolve(true)
            },
            cancelFn: () => {
                resolve(false)
            }
        })
    })
}

export default leaveConfirm
