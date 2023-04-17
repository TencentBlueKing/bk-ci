import axios from 'axios';

const request = axios.create({
  validateStatus: (status) => {
    if (status > 400) {
      console.warn(`HTTP 请求出错 status: ${status}`);
    }
    return status >= 200 && status <= 503;
  },
  withCredentials: true,
});

function errorHandler(error) {
  return Promise.reject(error);
}

request.interceptors.response.use((response) => {
  const { data: { status, message, code, result } } = response;
  const httpStatus = response.status;
  if (httpStatus === 503) {
    const errMsg = {
      status: httpStatus,
      message: (window.pipelineVue.$i18n && window.pipelineVue.$i18n.t('err503')) || 'service is in deployment',
    };
    return Promise.reject(errMsg);
  } if (httpStatus === 403) {
    const errorMsg = { httpStatus, code: httpStatus, message: (window.pipelineVue.$i18n && window.pipelineVue.$i18n.t('err403')) || 'Permission Deny' };
    return Promise.reject(errorMsg);
  } if ((typeof status !== 'undefined' && status !== 0) || (typeof result !== 'undefined' && !result)) {
    const errorMsg = { httpStatus, message, code: code || status };
    return Promise.reject(errorMsg);
  } if (httpStatus === 400) {
    const errorMsg = { httpStatus, message: (window.pipelineVue.$i18n && window.pipelineVue.$i18n.t('err400')) || 'service is abnormal' };
    return Promise.reject(errorMsg);
  }

  return response.data;
}, errorHandler);

export default request;
