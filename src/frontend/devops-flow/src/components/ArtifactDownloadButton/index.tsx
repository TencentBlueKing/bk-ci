import { defineComponent, ref, computed, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { Popover, Button, Dialog, Message } from 'bkui-vue'
import { SvgIcon } from '../SvgIcon'
import { useOutputs } from '@/hooks/useOutputs'
import { type ArtifactoryType } from '@/api/outputs'
import { ARTIFACTORY_TYPE } from '@/utils/flowConst'
import { head } from '@/utils/http'
import styles from './ArtifactDownloadButton.module.css'

export default defineComponent({
  name: 'ArtifactDownloadButton',
  props: {
    downloadIcon: Boolean,
    artifactoryType: {
      type: String as PropType<ArtifactoryType>,
      required: true,
    },
    name: {
      type: String,
      required: true,
    },
    path: {
      type: String,
      required: true,
    },
    output: {
      type: Object,
      required: true,
    },
  },
  setup(props, { emit }) {
    const { t } = useI18n()
    const route = useRoute()
    const visible = ref(false)
    const signingMap = ref(new Map())
    const isLoading = ref(false)
    const timer = ref()
    const curResolve = ref()
    const currentTab = computed(() => route.params.type as string)

    const { getFolderSize, requestDownloadUrl } = useOutputs(currentTab)

    const disabled = computed(() => {
      // 目录超10Gb 禁用状态
      if (props.output?.folder) {
        const size = getFolderSize(props.output)
        return size >= 10 * 1024 * 1024 * 1024
      }
      return false
    })

    const btnDisabled = computed(() => {
      return disabled.value || isLoading.value
    })

    function setVisible(value: boolean) {
      visible.value = value
    }

    async function downLoadFile() {
      try {
        if (btnDisabled.value) return

        if (signingMap.value.get(props.path)) {
          setVisible(true)
          return
        }
        isLoading.value = true
        const url2 = await requestDownloadUrl(props.artifactoryType, props.path)

        if (url2) {
          const result = await checkApkSigned(url2)
          if (result) {
            window.location.href = url2
            return
          } else {
            signingMap.value.set(props.path, true)
            setVisible(true)
            const result = await pollingCheckSignedApk(url2)
            if (result) {
              setVisible(false)
              Message({
                theme: 'success',
                message: t('flow.execute.apkSignSuccess', [props.name]),
              })

              window.location.href = url2
            }
            signingMap.value.delete(props.path)
          }
        }
      } catch (err: any) {
        Message({ theme: 'error', message: err.message || err })
      } finally {
        isLoading.value = false
      }
    }

    function pollingCheckSignedApk(url: string) {
      clearTimeout(timer.value)
      return new Promise(async (resolve) => {
        const result = await checkApkSigned(url)
        curResolve.value = resolve
        if (!result) {
          timer.value = setTimeout(() => {
            resolve(pollingCheckSignedApk(url))
          }, 5000)
          return
        }
        clearTimeout(timer.value)
        resolve(result)
      })
    }

    async function checkApkSigned(url: string) {
      try {
        await head(url, { meta: { silent: true } })
        return true // 请求成功，APK 已签名
      } catch (err: any) {
        return err.status !== 451
      }
    }

    function cancelDownloading() {
      clearTimeout(timer.value)
      curResolve.value?.(false)
      curResolve.value = null
      setVisible(false)
    }

    return () =>
      props.artifactoryType !== ARTIFACTORY_TYPE.IMAGE ? (
        <>
          <Popover disabled={!btnDisabled.value || isLoading.value}>
            {{
              default: () => (
                <>
                  {isLoading.value ? (
                    <span onClick={(e: Event) => e.stopPropagation()}>
                      <SvgIcon name="circle-2-1" class="spinIcon" size={12} />
                    </span>
                  ) : props.downloadIcon ? (
                    <span
                      onClick={(e: Event) => {
                        e.stopPropagation()
                        downLoadFile()
                      }}
                    >
                      <SvgIcon
                        name="download"
                        size={12}
                        class={btnDisabled.value ? styles.artifactoryDownloadIconDisabled : ''}
                      />
                    </span>
                  ) : (
                    <Button
                      text
                      onClick={downLoadFile}
                      disabled={btnDisabled.value}
                      theme="primary"
                    >
                      {t('flow.execute.download')}
                    </Button>
                  )}
                </>
              ),
              content: () => (
                <p>
                  {disabled.value
                    ? t('flow.execute.downloadDisabledTips')
                    : t('flow.execute.noDownloadPermTips')}
                </p>
              ),
            }}
          </Popover>

          <Dialog width={500} v-model:is-show={visible.value} theme="primary">
            {{
              default: () => (
                <>
                  <b class={styles.signingTips}>
                    <SvgIcon name="circle-2-1" class="spinIcon" />
                    {t('flow.execute.needSignTips', [props.name])}
                  </b>
                  <span class={styles.signingDurationTips}>
                    {t('flow.execute.apkSignDurationTips')}
                  </span>
                </>
              ),
              footer: () => (
                <Button onClick={cancelDownloading}>{t('flow.execute.downloadLater')}</Button>
              ),
            }}
          </Dialog>
        </>
      ) : null
  },
})
