import { Message } from 'bkui-vue';
import authModel from '@/common/auth-model';

// 请求执行失败拦截器
export default (error, config) => {
  const {
    code,
    message,
    response,
  } = error;
  switch (code) {
    // 用户登录状态失效
    case 401:
      authModel.showLoginModal(response);
  }
  // 全局捕获错误给出提示
  if (config.globalError) {
    Message({ theme: 'error', message });
  }
  return Promise.reject(error);
};
