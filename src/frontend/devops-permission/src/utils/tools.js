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
};

export default tools;
