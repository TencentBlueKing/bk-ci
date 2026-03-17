import ArtifactDownloadButton from '@/components/ArtifactDownloadButton'
import CopyToCustomRepoDialog from '@/components/CopyToCustomRepoDialog'
import EmptyPage from '@/components/EmptyPage'
import IframeReport from '@/components/IframeReport'
import { SvgIcon } from '@/components/SvgIcon'
import ThirdPartyReport from '@/components/ThirdPartyReport'
import { ROUTE_NAMES } from '@/constants/routes'
import { useOutputs } from '@/hooks/useOutputs'
import { Button, Input, Loading, SearchSelect, Table, Tag } from 'bkui-vue'
import { computed, defineComponent, nextTick, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import styles from './Outputs.module.css'

export default defineComponent({

  name: 'Outputs',
  setup() {
    const { t } = useI18n()
    const route = useRoute()

    // 根据路由名称判断当前 tab
    const currentTab = computed(() => {
      return route.name === ROUTE_NAMES.FLOW_DETAIL_ARTIFACTS ? 'artifacts' : 'reports'
    })

    const {
      activeOutput,
      activeOutputDetail,
      artifactValue,
      artifactFilterData,
      isLoading,
      isDetailLoading,

      keyWord,
      copyToDialogRef,
      iframeReportRef,
      qualityMetadata,

      thirdPartyReportList,
      visibleOutputs,
      isActiveThirdReport,
      isCustomizeReport,
      btns,
      artifactMoreActions,
      infoBlocks,

      init,
      getArtifactDate,
      setActiveOutput,

      fullScreenViewReport,
      updateSearchKey,
      initializeArtifactValue,
    } = useOutputs(currentTab)

    // 表格列配置
    const columns = [
      {
        label: t('flow.execute.key'),
        field: 'key',
        minWidth: 100,
      },
      {
        label: t('flow.execute.value'),
        field: 'value',
        minWidth: 100,
        render: ({ row }: any) => (
          <>
            {row.color && <span class={styles.colorBlock} style={{ backgroundColor: row.color }} />}
            <span title={row.value}>{row.value}</span>
          </>
        ),
      },
      {
        label: t('flow.execute.description'),
        field: 'description',
        minWidth: 100,
      },
    ]

    watch(
      () => visibleOutputs.value,
      (newOutputs: any) => {
        if (newOutputs.length > 0) {
          setActiveOutput(newOutputs[0])
        } else {
          activeOutputDetail.value = null
        }
      },
    )

    watch(
      () => currentTab.value,
      () => {
        keyWord.value = ''
        activeOutput.value = null
        activeOutputDetail.value = null
        artifactValue.value = []
        nextTick(init)
      },
    )

    watch(
      () => route.params.buildNo,
      () => {
        nextTick(init)
      },
    )

    watch(
      () => route.query.metadataKey,
      (newVal) => {
        if (newVal) {
          qualityMetadata.value = {
            labelKey: newVal as string,
            values: route.query.metadataValues?.toString().split(','),
          }
          initializeArtifactValue()
        }
      },
      { immediate: true },
    )

    // 生命周期
    onMounted(() => {
      const tasks: Promise<void>[] = [getArtifactDate()]
      if (!route.query.metadataKey) {
        tasks.push(init())
      }
      Promise.all(tasks)
    })

    return () => (
      <>
        <Loading loading={isLoading.value} class={styles.pipelineExecOutputs}>
          {!isLoading.value && (
            <>
              {/* 侧边栏 */}
              <aside
                class={styles.pipelineExecOutputsAside}
                style={{ width: currentTab.value === 'reports' ? '300px' : '30%' }}
              >
                <div class={styles.pipelineExecOutputsFilterInput}>
                  {currentTab.value === 'artifacts' && (
                    <div class={styles.artifactSearch}>
                      <p>{t('flow.execute.metaData')}</p>
                      <SearchSelect
                        class={styles.searchInput}
                        uniqueSelect
                        data={artifactFilterData.value}
                        placeholder={t('flow.execute.itemPlaceholder')}
                        modelValue={artifactValue.value}
                        onUpdate:modelValue={updateSearchKey}
                      />
                    </div>
                  )}
                  <Input
                    class={styles.inputSearch}
                    clearable
                    type="search"
                    placeholder={t(`flow.execute.${currentTab.value}FilterPlaceholder`)}
                    v-model={keyWord.value}
                  />
                </div>

                {visibleOutputs.value.length > 0 ? (
                  <ul class={styles.pipelineExecOutputsList}>
                    {visibleOutputs.value.map((output: any) => (
                      <li
                        key={output.id}
                        class={[output.id === activeOutput.value?.id ? styles.active : '']}
                        onClick={() => setActiveOutput(output)}
                      >
                        <SvgIcon name={output.icon} size={12} />
                        <span class={styles.outputName} title={output.name}>
                          {output.name}
                        </span>
                        <span class={styles.outputSize}>{output.size}</span>
                        <p class={styles.outputHoverIconBox}>
                          {output.downloadable && (
                            <ArtifactDownloadButton
                              output={output}
                              downloadIcon
                              path={output.fullPath}
                              name={output.name}
                              artifactoryType={output.artifactoryType}
                            />
                          )}
                          {output.isReportOutput && (
                            <span
                              onClick={(e: Event) => {
                                e.stopPropagation()
                                fullScreenViewReport(output)
                              }}
                              class={styles.fullScreen}
                            >
                              <SvgIcon name="full-screen" size={14} />
                            </span>
                          )}
                        </p>
                      </li>
                    ))}
                  </ul>
                ) : (
                  <div class={styles.noOutputsPlaceholder}>
                    <EmptyPage />
                  </div>
                )}
              </aside>

              {/* 主内容区 */}
              <section class={styles.pipelineExecOutputsSection}>
                {isCustomizeReport.value ? (
                  <IframeReport
                    ref={iframeReportRef}
                    report-name={activeOutput.value?.name}
                    index-file-url={activeOutput.value?.indexFileUrl}
                  />
                ) : isActiveThirdReport.value ? (
                  <ThirdPartyReport report-list={thirdPartyReportList.value} />
                ) : (
                  <Loading loading={isDetailLoading.value}>
                    {activeOutputDetail.value ? (
                      <>
                        <div class={styles.pipelineExecOutputHeader}>
                          <span class={styles.pipelineExecOutputHeaderName}>
                            <SvgIcon name={activeOutputDetail.value.icon} />
                            <span title={activeOutputDetail.value.name} class={styles.outputDetailName}>
                              {activeOutputDetail.value.name}
                            </span>
                          </span>
                          <p class="flex-center">
                            <Tag theme="info">{t(activeOutputDetail.value.artifactoryTypeTxt)}</Tag>
                          </p>
                          <p class={styles.pipelineExecOutputActions}>
                            {activeOutput.value?.downloadable && (
                              <ArtifactDownloadButton
                                output={activeOutput.value}
                                path={activeOutput.value.fullPath}
                                name={activeOutput.value.name}
                                artifactoryType={activeOutput.value.artifactoryType}
                              />
                            )}
                            {btns.value.map((btn) => (
                              <Button text theme="primary" key={btn.text} onClick={btn.handler}>
                                {btn.text}
                              </Button>
                            ))}
                          </p>
                        </div>

                        <div class={styles.pipelineExecOutputArtifact}>
                          {infoBlocks.value.map((block) => (
                            <div key={block.title} class={styles.pipelineExecOutputBlock}>
                              <h6 class={styles.pipelineExecOutputBlockTitle}>{block.title}</h6>
                              {block.key === 'meta' ? (
                                <Table
                                  data={block.value}
                                  border="outer"
                                  columns={columns as any}
                                  class={styles.triggerTable}
                                >
                                  {{
                                    empty: () => (
                                      <div class={styles.emptyState}>{t('flow.common.noData')}</div>
                                    ),
                                  }}
                                </Table>
                              ) : (
                                <ul class={styles.pipelineExecOutputBlockContent}>
                                  {'block' in block &&
                                    block.block?.map((row: any) => (
                                      <li
                                        key={row.key}
                                        style={{
                                          alignItems: row.key === 'fullName' ? 'baseline' : 'center',
                                        }}
                                      >
                                        <span class={styles.pipelineExecOutputBlockRowLabel}>
                                          {row.name}：
                                        </span>
                                        {row.key === 'fullName' ? (
                                          <span class={styles.pipelineExecOutputBlockRowFullName}>
                                            {block.value[row.key] || '--'}
                                          </span>
                                        ) : (
                                          <span class={styles.pipelineExecOutputBlockRowValue}>
                                            {block.value[row.key] || '--'}
                                          </span>
                                        )}
                                      </li>
                                    ))}
                                </ul>
                              )}
                            </div>
                          ))}
                        </div>
                      </>
                    ) : (
                      <div class={styles.noOutputsPlaceholder}>
                        <EmptyPage />
                      </div>
                    )}
                  </Loading>
                )}
              </section>
            </>
          )}
        </Loading>
        {activeOutput.value && (
          <CopyToCustomRepoDialog ref={copyToDialogRef} artifact={activeOutput.value} />
        )}
      </>
    )
  },
})
