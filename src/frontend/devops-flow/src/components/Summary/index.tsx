import ArtifactQuality from '@/components/ArtifactQuality'
import MaterialItem from '@/components/MaterialItem'
import { SvgIcon } from '@/components/SvgIcon'
import { ROUTE_NAMES } from '@/constants/routes'
import { useExecuteDetail } from '@/hooks/useExecuteDetail'
import { type ExecuteDetailData } from '@/types/flow'
import { Button, Input, Message, Popover } from 'bkui-vue'
import { computed, defineComponent, ref, watch, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import styles from './Summary.module.css'

export default defineComponent({
  name: 'SummaryWrapper',
  props: {
    visible: {
      type: Boolean,
      default: true,
    },
    execDetail: {
      type: Object as PropType<ExecuteDetailData>,
      required: true,
    },
  },
  setup(props) {
    const { t } = useI18n()
    const route = useRoute()
    const router = useRouter()
    const curVersionDesc = ref('')
    const isChangeRemark = ref(false)
    const remarkEditable = ref(false)
    const isShowMoreMaterial = ref(false)
    const buildNo = ref(route.params.buildNo as string)
    const projectId = ref(route.params.projectId as string)
    const flowId = ref(route.params.flowId as string)
    const remark = ref(props.execDetail.remark)
    const tempRemark = ref(props.execDetail.remark)
    const { requestUpdateRemark, fetchVersionDetail } = useExecuteDetail()

    const webhookInfo = computed(() => {
      return props.execDetail.webhookInfo || null
    })
    const visibleMaterial = computed(() => {
      if (Array.isArray(props.execDetail?.material) && props.execDetail?.material.length > 0) {
        return props.execDetail?.material
      }
      return []
    })
    const instanceFromTemplate = computed(() => {
      return props.execDetail?.model?.instanceFromTemplate ?? false
    })
    const isConstraintTemplate = computed(() => {
      return (
        instanceFromTemplate.value && props.execDetail?.templateInfo?.instanceType === 'CONSTRAINT'
      )
    })
    const templateRows = computed(() => {
      return [
        {
          id: t('flow.execute.templateName'),
          content: props.execDetail?.templateInfo?.templateName ?? '--',
        },
        {
          id: t('flow.execute.templateVersion'),
          content: props.execDetail?.templateInfo?.versionName ?? '--',
          link: {
            name: 'templateEdit',
            params: {
              templateId: props.execDetail?.templateInfo?.templateId,
            },
          },
        },
      ]
    })
    const artifactQuality = computed(() => props.execDetail?.artifactQuality)
    watch(
      () => props.execDetail,
      (val, oldVal) => {
        if (val.remark !== tempRemark.value) {
          tempRemark.value = val.remark
          remark.value = val.remark
        }
        if (val?.curVersion !== oldVal?.curVersion) {
          updateCurVersionDesc()
        }
      },
      { immediate: true },
    )

    function showMoreMaterial() {
      isShowMoreMaterial.value = true
    }

    function hideMoreMaterial() {
      isShowMoreMaterial.value = false
    }

    function showRemarkEdit() {
      tempRemark.value = remark.value
      remarkEditable.value = true
    }

    function hideRemarkEdit() {
      remarkEditable.value = false
    }

    async function updateCurVersionDesc() {
      try {
        const result = await fetchVersionDetail(props.execDetail.curVersion)
        curVersionDesc.value = result.description || ''
      } catch (error: any) {
        Message({
          message: error.message || error,
          theme: 'error',
        })
      }
    }

    async function handleRemarkChange() {
      if (isChangeRemark.value) {
        return
      }

      try {
        if (tempRemark.value !== remark.value) {
          isChangeRemark.value = true
          await requestUpdateRemark({
            projectId: projectId.value,
            pipelineId: flowId.value,
            buildId: buildNo.value,
            remark: tempRemark.value as string,
          })
          remark.value = tempRemark.value
          Message({
            theme: 'success',
            message: t('flow.common.updateSuc'),
          })
        }
      } catch (e) {
        tempRemark.value = remark.value
        Message({
          theme: 'error',
          message: t('flow.common.updateFail'),
        })
      } finally {
        isChangeRemark.value = false
        remarkEditable.value = false
      }
    }

    function goOutputs(values: any) {
      router.push({
        name: ROUTE_NAMES.FLOW_DETAIL_OUTPUTS,
        params: route.params,
        query: {
          metadataKey: values[0].labelKey,
          metadataValues: values.map((item: any) => item.value).join(','),
        },
      })
    }

    return () => (
      <>
        {props.visible ? (
          <header class={styles.execDetailSummary}>
            <div class={styles.execDetailSummaryInfo}>
              <div class={styles.execDetailSummaryInfoMaterial}>
                <span class={styles.execDetailSummaryInfoBlockTitle}>
                  {t('flow.execute.triggerRepo')}
                </span>
                {webhookInfo.value ? (
                  <div class={styles.execDetailSummaryInfoMaterialList}>
                    <MaterialItem
                      class={styles.visibleMaterialRow}
                      material={webhookInfo.value}
                      isWebhook={true}
                      showMore={false}
                    />
                  </div>
                ) : (
                  <span class={styles.noExecMaterial}>--</span>
                )}
              </div>

              <div class={styles.execDetailSummaryInfoMaterial}>
                <span class={styles.execDetailSummaryInfoBlockTitle}>
                  {t('flow.execute.material')}
                </span>
                {visibleMaterial.value.length > 0 ? (
                  <div class={styles.execDetailSummaryInfoMaterialList}>
                    <MaterialItem
                      class={styles.visibleMaterialRow}
                      material={visibleMaterial.value[0]}
                      showMore={visibleMaterial.value.length > 1}
                      onMouseEnter={showMoreMaterial}
                    />
                    {isShowMoreMaterial.value && (
                      <ul class={styles.allExecMaterialList} onMouseleave={hideMoreMaterial}>
                        {visibleMaterial.value.map((material, index) => (
                          <li key={index}>
                            <MaterialItem
                              material={material}
                              showMore={index === 0}
                              showMorePlaceholder={index !== 0}
                            />
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>
                ) : (
                  <span class={styles.noExecMaterial}>--</span>
                )}
              </div>

              <div style={{ overflow: 'hidden' }}>
                <span class={styles.execDetailSummaryInfoBlockTitle}>
                  {t('flow.execute.flowVersion')}
                </span>
                <div class={styles.execDetailSummaryInfoBlockContent}>
                  {isConstraintTemplate.value && (
                    <Popover
                      trigger="click"
                      class={styles.instanceTemplateInfo}
                      placement="bottom"
                      width="360"
                      theme="light"
                      v-slots={{
                        default: () => (
                          <SvgIcon class={styles.templateInfoEntry} name="constraint" size={14} />
                        ),
                        content: () => (
                          <div class={styles.pipelineTemplateInfoPopover}>
                            <header class={styles.templateInfoHeader}>
                              {t('flow.execute.constraintModeDesc')}
                            </header>
                            <section class={styles.templateInfoSection}>
                              {templateRows.value.map((row) => (
                                <p key={row.id}>
                                  <label>{row.id}：</label>
                                  <span>{row.content}</span>
                                  {row.link && (
                                    <router-link
                                      class={styles.templateLinkIcon}
                                      to={row.link}
                                      target="_blank"
                                    >
                                      <SvgIcon name="tiaozhuan" size={14} />
                                    </router-link>
                                  )}
                                </p>
                              ))}
                            </section>
                          </div>
                        ),
                      }}
                    />
                  )}
                  <Popover
                    placement="top"
                    maxWidth="500"
                    v-slots={{
                      default: () => (
                        <span class={styles.pipelineCurVersionSpan}>
                          {props.execDetail.curVersionName}
                        </span>
                      ),
                      content: () => (
                        <div>
                          <p>
                            <label>{t('flow.execute.versionNum')}：</label>
                            <span>{props.execDetail.curVersionName}</span>
                          </p>
                          <p>
                            <label>{t('flow.execute.versionDesc')}：</label>
                            <span>{curVersionDesc.value || '--'}</span>
                          </p>
                        </div>
                      ),
                    }}
                  />
                </div>
              </div>

              <div class={styles.execRemarkBlock}>
                <span class={styles.execDetailSummaryInfoBlockTitle}>
                  {t('flow.common.remark')}
                  {remarkEditable.value ? (
                    <span class={styles.pipelineExecRemarkActions}>
                      <Button text theme="primary" onClick={handleRemarkChange}>
                        {t('flow.common.save')}
                      </Button>
                      <Button text theme="primary" onClick={hideRemarkEdit}>
                        {t('flow.common.cancel')}
                      </Button>
                    </span>
                  ) : (
                    <span onClick={showRemarkEdit}>
                      <SvgIcon name="edit" size={14} class={styles.execRemarkEditIcon} />
                    </span>
                  )}
                </span>
                <div class={styles.execDetailSummaryInfoBlockContent}>
                  {remarkEditable.value ? (
                    <Input
                      type="textarea"
                      vModel={tempRemark.value}
                      maxlength={4096}
                      placeholder={t('flow.execute.addRemarkForBuild')}
                      class={styles.execRemark}
                    />
                  ) : (
                    <span
                      v-bk-tooltips={{
                        content: remark.value,
                        disabled: !remark.value,
                        allowHTML: false,
                        delay: [300, 0],
                      }}
                      class={styles.execRemark}
                    >
                      {remark.value || '--'}
                    </span>
                  )}
                </div>
              </div>
            </div>

            {artifactQuality.value && Object.keys(artifactQuality.value).length > 0 && (
              <div class={styles.partQualityBlock}>
                <span class={styles.partQualityBlockTitle}>
                  {t('flow.execute.artifactQuality')}
                </span>
                <ArtifactQuality data={artifactQuality.value} onGoOutputs={goOutputs} />
              </div>
            )}
          </header>
        ) : null}
      </>
    )
  },
})
