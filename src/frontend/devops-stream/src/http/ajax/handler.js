import store from '@/store'

function requestHandler (config) {
    return config
}

function errorHandler (error) {
    console.log('error catch', error)
    return Promise.reject(Error(error.message || '网络出现问题，请检查你的网络是否正常'))
}

function successHandler (response) {
    const { data: { code, data, message, status }, status: httpStatus } = response
    const errorMsg = { httpStatus, message, code: code || status }
    if (httpStatus === 401) {
        location.href = window.getLoginUrl()
    } else if ([503, 403, 418, 419].includes(httpStatus)) {
        store.dispatch('setExceptionInfo', { type: httpStatus, message })
        return
    } else if ((typeof code !== 'undefined' && code !== 0) || (typeof status !== 'undefined' && status !== 0)) {
        return Promise.reject(errorMsg)
    }
    return data === undefined ? response.data : data
}

export { errorHandler, successHandler, requestHandler }
