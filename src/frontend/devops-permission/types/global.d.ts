declare interface Window {
  SITE_URL: string
  BK_IAM_URL_PREFIX: string
  BK_ITSM_URL_PREFIX: string
  $syncUrl: (url: string) => void
}
