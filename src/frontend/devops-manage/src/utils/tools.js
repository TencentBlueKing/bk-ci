const tools = {
  // 获取 cookie object
  getCookie(key) {
    if (!key) return '';
    return document.cookie
      .split(';')
      .map(cookie => cookie.split('=').map(decodeURIComponent).map(v => v.trim()))
      .reduce((acc, [cKey, cVal]) => (cKey === key ? cVal : acc), '');
  },
};

export default tools;
