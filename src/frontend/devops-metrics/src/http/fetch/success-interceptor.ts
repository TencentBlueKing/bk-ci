import RequestError from './request-error';

// 请求成功执行拦截器
export default async (response, config) => {
  const {
    status = response.status,
    data,
    message = response.statusText,
  } = await response[config.responseType]();
  if (response.ok) {
    // 对应 HTTP 请求的状态码 200 到 299
    // 如果后端有自己的code，则需要自己在response后再判断一层
    switch (status) {
      // 接口请求成功
      case 0:
        return Promise.resolve(data);
      // 后端业务处理报错
      default:
        throw new RequestError(status, message || '系统错误', data);
    }
  } else {
    // 处理 http 非 200 异常
    throw new RequestError(status || -1, message || '系统错误', data);
  }
};
