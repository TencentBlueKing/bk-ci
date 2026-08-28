import { formatByUserTz } from '../../../common-lib/time';

// 获取 cookie
export function getCookie(name: string): string {
  try {
    const decodedCookie = decodeURIComponent(document.cookie);
    const cookies = decodedCookie.split(';');
    for (const cookie of cookies) {
      const [key, value] = cookie.trim().split('=');
      if (key === name) {
        return value;
      }
    }
    return '';
  } catch (e) {
    console.error('get cookie error', e);
    return '';
  }
}

/**
 * 获取部署在域名子路径下时的路径前缀
 * 生产环境由部署脚本将 __BK_CI_PUBLIC_PATH__ 替换为真实前缀；
 * 开发环境占位符未被替换时按空前缀处理
 * @returns {string} 规范化后的前缀，如 '/sub' 或 ''
 */
export function getPublicUrlPrefix(): string {
  const prefix = window.PUBLIC_URL_PREFIX || '';
  if (!prefix || prefix.startsWith('__') || prefix === '/') {
    return '';
  }
  return prefix.replace(/\/+$/, '');
}

/**
 * 检查是不是 object 类型
 * @param item
 * @returns {boolean}
 */
export function isObject(item: any) {
  return Object.prototype.toString.apply(item) === '[object Object]';
}


/**
 * 深度合并多个对象
 * @param objectArray 待合并列表
 * @returns {object} 合并后的对象
 */
export function deepMerge(...objectArray: object[]) {
  return objectArray.reduce((acc, obj) => {
    Object.keys(obj || {}).forEach((key) => {
      const pVal = acc[key];
      const oVal = obj[key];

      if (isObject(pVal) && isObject(oVal)) {
        acc[key] = deepMerge(pVal, oVal);
      } else {
        acc[key] = oVal;
      }
    });

    return acc;
  }, {});
}

/**
 * 时间格式化（按用户时区）
 */
export function timeFormatter(val: string | number, format = 'YYYY-MM-DD HH:mm:ss') {
  if (val === null || val === undefined || val === '') return '--';
  return formatByUserTz(val, undefined, format);
}
