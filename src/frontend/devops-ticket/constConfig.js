module.exports = (version = 'tencent') => {
    const isTx = version === 'tencent'
    return {
        CODEGIT_URL_PREFIX: isTx ? JSON.stringify('http://git.code.oa.com') : JSON.stringify(''),
        GITLAB_URL_PREFIX: isTx ? JSON.stringify('http://gitlab-paas.open.oa.com') : JSON.stringify('')
    }
}
