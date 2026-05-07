declare interface Window {
  SITE_URL: string;
  BK_IAM_URL_PREFIX: string;
  $syncUrl: (url: string) => void;
  BK_APIGW_USER_WEB_URL: string;
  getRoutePrefix: () => string;
}
