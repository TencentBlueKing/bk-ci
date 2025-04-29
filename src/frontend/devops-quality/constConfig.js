module.exports = (version = 'tencent') => {
    const isTx = version === 'tencent'
    return {
        USER_IMG_URL: isTx ? JSON.stringify('//rhrc.woa.com/photo/150') : JSON.stringify('')
    }
}
