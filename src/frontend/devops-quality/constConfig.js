module.exports = (version = 'tencent') => {
    const isTx = version === 'tencent'
    return {
        USER_IMG_URL: isTx ? JSON.stringify('http://dayu.oa.com') : JSON.stringify('')
    }
}
