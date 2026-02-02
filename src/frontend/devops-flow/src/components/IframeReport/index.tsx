import { defineComponent, ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Button } from 'bkui-vue'
import { SvgIcon } from '../SvgIcon'
import styles from './IframeReport.module.css'

export default defineComponent({
  name: 'IframeReport',
  props: {
    reportIcon: {
      type: String,
      default: 'order',
    },
    reportName: {
      type: String,
      default: '',
    },
    indexFileUrl: {
      type: String,
      default: '',
    },
  },
  setup(props) {
    const { t } = useI18n()
    const fullScreenView = ref(false)
    const reportIframe = ref<HTMLIFrameElement>()

    function toggleFullScreen() {
      fullScreenView.value = !fullScreenView.value
    }

    function handleIframeLoad() {
      if (reportIframe.value?.contentDocument?.body?.scrollHeight) {
        reportIframe.value.style.height = `${reportIframe.value.contentDocument.body.scrollHeight}px`
      }
    }

    onMounted(() => {
      reportIframe.value?.addEventListener('load', handleIframeLoad)
    })

    return () => (
      <section
        class={[
          styles.iframeReportSection,
          fullScreenView.value ? styles.pipelineReportFullScreen : '',
        ]}
      >
        <div class={styles.pipelineExecReportHeader}>
          <span class={styles.pipelineExecReportHeaderName}>
            <SvgIcon name={props.reportIcon} size={14} />
            {props.reportName}
          </span>
          <Button text theme="primary" onClick={toggleFullScreen}>
            <span class={styles.fullScreenToggler}>
              <SvgIcon name={fullScreenView.value ? 'un-full-screen' : 'full-screen'} size={14} />
              {t(
                fullScreenView.value
                  ? 'flow.execute.exitFullscreen'
                  : 'flow.execute.fullScreenView',
              )}
            </span>
          </Button>
        </div>
        <div class={styles.pipelineExecReportContent}>
          <iframe
            ref={reportIframe}
            class={styles.execThirdPartyReport}
            allowfullscreen
            src={props.indexFileUrl}
          />
        </div>
      </section>
    )
  },
})
