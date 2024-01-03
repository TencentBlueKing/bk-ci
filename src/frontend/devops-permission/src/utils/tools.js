import _ from 'lodash';

const tools = {
  /**
   * 深拷贝函数
   * @param {Object}} obj copy 对象
   */
  deepClone(obj) {
    return _.cloneDeepWith(obj);
  },
  /**
   * 深比较函数
   */
  isDataEqual(a, b) {
    return _.isEqual(a, b);
  },

  // 获取 cookie object
  getCookie(key) {
    const cookieStr = document.cookie || '';
    const cookieArr = cookieStr.split(';').filter(v => v);
    const cookieObj = cookieArr.reduce((res, cookieItem) => {
        const [key, value] = cookieItem.split('=');
        const cKey = (key || '').trim();
        const cVal = (value || '').trim();
        res[cKey] = cVal;
        return res;
    }, {});
    return cookieObj[key] || '';
  },
};

export default tools;
