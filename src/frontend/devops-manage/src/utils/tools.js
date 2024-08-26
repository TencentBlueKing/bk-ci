const tools = {
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
