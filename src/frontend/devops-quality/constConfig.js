module.exports = (version = 'tencent') => {
    const isTx = version === 'tencent'
    return {
        USER_IMG_URL: isTx ? JSON.stringify('//dayu.woa.com') : JSON.stringify('')
    }
}
