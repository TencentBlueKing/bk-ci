import { defineComponent, ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Loading, Button, Sideslider } from 'bkui-vue'
import { SvgIcon } from '@/components/SvgIcon'
import { allVersionKeyList } from '@/utils/flowConst'
import { useExecuteDetail } from '@/hooks/useExecuteDetail'
import { type BuildParamItem, type BuildParamProperty } from '@/api/executeDetail'
import styles from './StartParams.module.css'

interface ParamItem extends BuildParamItem {
  isDiff?: boolean
}

export default defineComponent({
  name: 'StartParams',
  setup() {
    const { t } = useI18n()
    const route = useRoute()
    const { executeDetail, getStartupParams } = useExecuteDetail()

    const isLoading = ref(false)
    const params = ref<ParamItem[]>([])
    const defaultParamMap = ref<Record<string, any>>({})
    const activeParam = ref<ParamItem | null>(null)
    const isDetailShow = ref(false)
    const overflowSpan = ref<boolean[]>([])
    const buildParamProperties = ref<BuildParamProperty[]>([])
    const isVisibleVersion = ref(false)

    const valueRefs = ref<(HTMLElement | null)[]>([])

    const allParams = computed(() => {
      const valueMap = params.value.reduce<Record<string, any>>((acc, item) => {
        acc[item.key] = item.value
        return acc
      }, {})

      return buildParamProperties.value.map((item) => ({
        ...item,
        value: valueMap[item.id],
      }))
    })

    onMounted(() => {
      init()
    })

    watch(
      () => route.params.buildNo,
      () => init(),
    )

    function resetOverflowFlags() {
      nextTick(() => {
        overflowSpan.value = valueRefs.value.map((span) => {
          if (!span) return false
          return span.scrollWidth > span.clientWidth
        })
      })
    }

    function isDefaultDiff({ key, value }: ParamItem) {
      const defaultValue = defaultParamMap.value[key]
      if (typeof defaultValue === 'boolean') {
        return defaultValue.toString() !== value?.toString()
      }
      return defaultValue !== value
    }

    async function init() {
      try {
        isLoading.value = true
        const { projectId, flowId, buildNo } = route.params
        const urlParams = {
          projectId: projectId as string,
          pipelineId: flowId as string,
          buildId: buildNo as string,
        }

        const { buildParams, paramProperties } = await getStartupParams(urlParams)

        buildParamProperties.value = paramProperties
        isVisibleVersion.value = paramProperties.some((item) => allVersionKeyList.includes(item.id))

        defaultParamMap.value = buildParams.reduce<Record<string, any>>((acc, item) => {
          acc[item.key] = item.defaultValue
          return acc
        }, {})

        params.value = buildParams.map((item) => ({
          ...item,
          isDiff: isDefaultDiff(item),
        }))
        resetOverflowFlags()
      } catch (e) {
        console.error(e)
      } finally {
        isLoading.value = false
      }
    }

    function showDetail(param: ParamItem) {
      isDetailShow.value = true
      activeParam.value = param
    }

    function hideDetail() {
      activeParam.value = null
      isDetailShow.value = false
    }

    function formatParamValue(value: any): string {
      if (typeof value === 'object' && value !== null) {
        try {
          return JSON.stringify(value, null, 2)
        } catch (error) {
          return String(value)
        }
      }

      return String(value)
    }

    return () => (
      <Loading loading={isLoading.value} class={styles.startupParameterBox}>
        <div class={styles.startupParameterWrapper}>
          {params.value.map((param, index) => (
            <div class={styles.buildParamRow} key={param.key || index}>
              <span class={styles.buildParamSpan}>
                <span class={styles.buildParamKeySpan} title={param.key}>
                  {param.key}
                </span>
                {param.desc ? (
                  <span
                    v-bk-tooltips={{ content: param.desc, allowHTML: false }}
                    class={styles.questionCircle}
                  >
                    <SvgIcon name="question-circle" size="12"></SvgIcon>
                  </span>
                ) : null}
              </span>

              <span class={styles.buildParamSpan}>
                {typeof param.value !== 'undefined' ? (
                  <>
                    <span
                      ref={(el) => (valueRefs.value[index] = el as HTMLElement)}
                      class={[
                        styles.buildParamValueSpan,
                        param.isDiff ? styles.diffParamValue : '',
                      ]}
                    >
                      {formatParamValue(param.value)}
                    </span>
                    {overflowSpan.value[index] ? (
                      <Button
                        text
                        class={styles.viewParamValueDetail}
                        size="small"
                        theme="primary"
                        onClick={() => showDetail(param)}
                      >
                        {t('flow.execute.detail')}
                      </Button>
                    ) : null}
                  </>
                ) : (
                  <span>--</span>
                )}
              </span>
            </div>
          ))}

          <Sideslider
            quick-close
            width={640}
            title={t('flow.execute.paramDetail')}
            is-show={isDetailShow.value}
            onClosed={hideDetail}
          >
            {{
              default: () => (
                <div class={styles.startupParamDetailWrapper}>
                  {activeParam.value ? (
                    <>
                      <p>{activeParam.value.key}</p>
                      <pre>{String(activeParam.value.value ?? '--')}</pre>
                    </>
                  ) : null}
                </div>
              ),
            }}
          </Sideslider>
        </div>
      </Loading>
    )
  },
})
