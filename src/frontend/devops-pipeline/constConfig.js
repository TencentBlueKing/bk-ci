module.exports = (version = 'tencent') => {
    const isTx = version === 'tencent'
    return {
        USER_IMG_URL: isTx ? JSON.stringify('//dayu.woa.com') : JSON.stringify(''),
        OPEN_URL: isTx ? JSON.stringify('//open.woa.com') : JSON.stringify(''),
        OIED_URL: isTx ? JSON.stringify('//o.ied.com') : JSON.stringify(''),
        CODECC_SOFWARE_URL: isTx ? JSON.stringify('software.codecc.woa.com') : JSON.stringify(''),
        JOB_URL: isTx ? JSON.stringify('//job.ied.com') : JSON.stringify('')
    }
}
