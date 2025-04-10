import { Message } from 'bkui-vue';
import type { IFetchConfig } from './index';

// 请求执行失败拦截器
export default (error: any, config: IFetchConfig) => {
  const {
    code,
    response
  } = error;
  switch (code) {
    // 用户登录状态失效
    case 401:
      window.$toggleLoginDialog?.(true);
  }
  // 全局捕获错误给出提示
  if (config.globalError && !document.getElementsByClassName('bk-message').length) {
    Message({ theme: 'error', message: error.message || error });
  }
  return Promise.reject(error);
};
