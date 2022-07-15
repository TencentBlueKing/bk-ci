import dayjs from 'dayjs';
// 获取 cookie object
export function getCookie (key) {
  const cookieStr = document.cookie || ''
  const cookieArr = cookieStr.split(';').filter(v => v)
  const cookieObj = cookieArr.reduce((res, cookieItem) => {
      const [key, value] = cookieItem.split('=')
      const cKey = (key || '').trim()
      const cVal = (value || '').trim()
      res[cKey] = cVal
      return res
  }, {})
  return cookieObj[key] || ''
}

/**
 * 检查是不是 object 类型
 * @param item
 * @returns {boolean}
 */
export function isObject(item) {
  return (item && Object.prototype.toString.apply(item) === '[object Object]');
}


/**
 * 深度合并多个对象
 * @param objectArray 待合并列表
 * @returns {object} 合并后的对象
 */
export function deepMerge(...objectArray) {
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
 * 时间格式化
 * @param val 待格式化时间
 * @param format 格式
 * @returns 格式化后的时间
 */
export function timeFormatter(val, format = 'YYYY-MM-DD HH:mm:ss') {
  return val ? dayjs(val).format(format) : '--';
}
