declare interface Window {
  $syncUrl: (url: string) => void;
  $toggleLoginDialog: (isShow: boolean) => void;
  BK_APIGW_USER_WEB_URL: string;
  PUBLIC_URL_PREFIX: string;
  getRoutePrefix: () => string;
}
