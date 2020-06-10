module.exports = (version = 'tencent') => {
    const isTx = version === 'tencent'
    return {
        BADGE_URL_PREFIX: isTx ? JSON.stringify('https://api.bkdevops.qq.com') : JSON.stringify(''),
        CHECK_ENV_URL: isTx ? JSON.stringify('http://devgw.devops.oa.com') : JSON.stringify(''),
        USER_IMG_URL: isTx ? JSON.stringify('http://dayu.oa.com') : JSON.stringify(''),
        OPEN_URL: isTx ? JSON.stringify('http://open.oa.com') : JSON.stringify(''),
        OIED_URL: isTx ? JSON.stringify('o.ied.com') : JSON.stringify(''),
        ZHIYUN_URL: isTx ? JSON.stringify('http://ccc.oa.com') : JSON.stringify(''),
        CODECC_SOFWARE_URL: isTx ? JSON.stringify('software.codecc.oa.com') : JSON.stringify(''),
        JOB_URL: isTx ? JSON.stringify('http://job.ied.com') : JSON.stringify('')
    }
}
