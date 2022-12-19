interface ILoginData {
  login_url?: string,
  target?: string
}

const getLoginUrl = () => {
  var cUrl = `${location.origin}/public/login_success.html?is_ajax=1`;
  if (/=%s/.test(import.meta.env.VITE_LOGIN_SERVICE_URL)) {
      return import.meta.env.VITE_LOGIN_SERVICE_URL.replace(/%s/, cUrl)
  }else {
      const loginUrl = new URL(import.meta.env.VITE_LOGIN_SERVICE_URL)
      if (/=$/.test(loginUrl.search)) {
        return import.meta.env.VITE_LOGIN_SERVICE_URL + cUrl
      } else {
        loginUrl.searchParams.append('c_url', cUrl)
        return loginUrl.href
      }
  }
}

/** 无权限弹窗 */
class AuthModel {
  isShow: boolean;
  loginWindow: Window;
  checkWindowTimer: number;

  constructor() {
    this.isShow = false;
    this.loginWindow = null;
    this.checkWindowTimer = -1;
    window.addEventListener('message', this.messageListener.bind(this), false);
  }
  showLoginModal(data: ILoginData = {}) {
    if (this.isShow) return;
    this.isShow = true;
    let url = data.login_url;
    if (!url) {
      url = getLoginUrl()
    }
    const width = 700;
    const height = 510;
    const { availHeight, availWidth } = window.screen;
    this.loginWindow = window.open(url, '_blank', `
      width=${width},
      height=${height},
      left=${(availWidth - width) / 2},
      top=${(availHeight - height) / 2},
      channelmode=0,
      directories=0,
      fullscreen=0,
      location=0,
      menubar=0,
      resizable=0,
      scrollbars=0,
      status=0,
      titlebar=0,
      toolbar=0,
      close=0
    `);
    this.checkWinClose();
  }
  checkWinClose() {
    this.checkWindowTimer && clearTimeout(this.checkWindowTimer);
    this.checkWindowTimer = setTimeout(() => {
      if (!this.loginWindow || this.loginWindow.closed) {
        this.hideLoginModal();
        clearTimeout(this.checkWindowTimer);
        return;
      }
      this.checkWinClose();
    }, 300);
  }
  messageListener({ data = {} }: MessageEvent) {
    if (data === null || typeof data !== 'object' || data.target !== 'bk-login' || !this.loginWindow) return;

    this.hideLoginModal();
  }
  hideLoginModal() {
    this.isShow = false;
    if (this.loginWindow) {
      this.loginWindow.close();
    }
    this.loginWindow = null;
  }
}

export default new AuthModel();
