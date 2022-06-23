/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_AJAX_URL_PREFIX: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
