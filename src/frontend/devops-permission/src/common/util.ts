import { formatByUserTz } from '../../../common-lib/time';

// 获取 cookie object
export function getCookies(strCookie = document.cookie): any {
  if (!strCookie) {
    return {};
  }
  const arrCookie = strCookie.split('; ');// 分割
  const cookiesObj = {};
  arrCookie.forEach((cookieStr) => {
    const arr = cookieStr.split('=');
    const [key, value] = arr;
    if (key) {
      cookiesObj[key] = value;
    }
  });
  return cookiesObj;
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
