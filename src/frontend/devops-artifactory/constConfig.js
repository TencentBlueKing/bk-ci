module.exports = (version = 'tencent') => {
    const isTx = version === 'tencent'
    return {
        CHECK_ENV_URL: isTx ? JSON.stringify('http://devgw.devops.oa.com') : JSON.stringify('')
    }
}
