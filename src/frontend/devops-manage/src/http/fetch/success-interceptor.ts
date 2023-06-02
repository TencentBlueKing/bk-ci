import type { IFetchConfig } from './index';
import RequestError from './request-error';

// 请求成功执行拦截器
export default async (response: Response, config: IFetchConfig) => {
  const {
    status = response.status,
    data,
    message = response.statusText,
  } = await response[config.responseType]();
  if (response.ok) {
    // 对应 HTTP 请求的状态码 200 到 299
    // 校验接口返回的数据，status 为 0 表示业务成功
    switch (status) {
      // 接口请求成功
      case 0:
        return Promise.resolve(data);
      case 200:
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
