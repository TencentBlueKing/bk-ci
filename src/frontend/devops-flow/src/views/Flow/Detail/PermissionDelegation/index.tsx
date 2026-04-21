import { defineComponent, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Loading, Dialog, Tag, Button } from 'bkui-vue'
import { SvgIcon } from '@/components/SvgIcon'
import { convertTime } from '@/utils/util'
import { usePermissionDelegation } from '@/hooks/usePermissionDelegation'
import styles from './permissionDelegation.module.css'

export default defineComponent({
  name: 'PermissionDelegation',
  setup() {
    const { t } = useI18n()

    // 使用权限代持Hook
    const {
      resourceAuthData,
      loading,
      resetLoading,
      showExpiredTag,
      showResetDialog,
      showFailedDialog,
      failedArr,
      fetchResourceAuth,
      handleReset,
      handleShowResetDialog,
      handleToggleShowResetDialog,
    } = usePermissionDelegation()

    onMounted(() => {
      fetchResourceAuth()
    })

    return () => (
      <>
        <div class={styles.permissionDelegation}>
          <Loading loading={loading.value}>
            <section class={styles.contentWarpper}>
              <header class={styles.header}>
                <SvgIcon class={styles.helpDocumentFill} name="help-document-fill" size={20} />
                {t('flow.delegation.delegationPermission')}
              </header>
              <div class={styles.content}>
                <p>{t('flow.delegation.tips1')}</p>
                <p class={styles.tips2}>
                  <i18n-t keypath="flow.delegation.tips2" tag="span">
                    <span class={styles.highlight}>{t('flow.delegation.flowExecPermission')}</span>
                  </i18n-t>
                </p>
                <p>
                  <i18n-t keypath="flow.delegation.tips3" tag="span">
                    <span class={styles.highlight}>BK_CI_AUTHORIZER</span>
                  </i18n-t>
                </p>
                <p class={styles.tips4}>{t('flow.delegation.tips4')}</p>
                <p>
                  <i18n-t keypath="flow.delegation.tips5" tag="span">
                    <span class={styles.highlight}>{t('flow.delegation.newOperator')}</span>
                  </i18n-t>
                </p>
              </div>
            </section>

            <div class={styles.panelContent}>
              <p>
                <label class={styles.blockRowLabel}>
                  {t('flow.delegation.proxyHolderForExecutionPermissions')}
                </label>
                <span
                  class={[
                    styles.blockRowValue,
                    !resourceAuthData.value.executePermission ? styles.resetRow : '',
                  ]}
                >
                  <span
                    class={[
                      styles.name,
                      !resourceAuthData.value.executePermission ? styles.notPermission : '',
                    ]}
                    v-bk-tooltips={{
                      content: t('flow.delegation.expiredTips'),
                      disabled: resourceAuthData.value.executePermission,
                    }}
                  >
                    {resourceAuthData.value.handoverFrom}
                  </span>
                  {showExpiredTag.value && <Tag theme="danger">{t('flow.delegation.expired')}</Tag>}
                  <span class={styles.refreshAuth} onClick={handleShowResetDialog}>
                    <SvgIcon class={styles.refreshIcon} name="refresh-line" size={14} />
                    {t('flow.delegation.resetAuthorization')}
                  </span>
                </span>
              </p>
              <p>
                <label class={styles.blockRowLabel}>{t('flow.delegation.authTime')}</label>
                <span class={styles.blockRowValue}>
                  {convertTime(resourceAuthData.value.handoverTime)}
                </span>
              </p>
            </div>
          </Loading>

          <Dialog
            class={styles.resetAuthDialog}
            v-model:is-show={showResetDialog.value}
            onValueChange={handleToggleShowResetDialog}
          >
            {{
              default: () => (
                <>
                  <p class={styles.resetDialogTitle}>{t('flow.delegation.confirmReset')}</p>
                  <p class={styles.resetDialogTips}>
                    <p>
                      <i18n-t keypath="flow.delegation.resetAuthTips1" tag="span">
                        <span class={styles.highlight}>{t('flow.delegation.yourPermission')}</span>
                      </i18n-t>
                    </p>
                    <p>{t('flow.delegation.resetAuthTips2')}</p>
                  </p>
                </>
              ),
              footer: () => (
                <>
                  <Button
                    class={styles.mr10}
                    theme="primary"
                    loading={resetLoading.value}
                    onClick={handleReset}
                  >
                    {t('flow.delegation.reset')}
                  </Button>
                  <Button
                    loading={resetLoading.value}
                    onClick={() => (showResetDialog.value = !showResetDialog.value)}
                  >
                    {t('flow.delegation.cancel')}
                  </Button>
                </>
              ),
            }}
          </Dialog>

          <Dialog class={styles.resetAuthDialog} v-model:is-show={showFailedDialog.value}>
            {{
              default: () => (
                <>
                  <p class={styles.resetDialogTitle}>{t('flow.delegation.resetFailed')}</p>
                  <div class={styles.resetFailedItem}>
                    {failedArr.value.map((item, index) => (
                      <div key={item}>
                        {index > 0 && <span>{index}.</span>}
                        <span innerHTML={item} />
                      </div>
                    ))}
                  </div>
                </>
              ),
              footer: () => (
                <>
                  <Button
                    theme="primary"
                    loading={resetLoading.value}
                    onClick={() => (showFailedDialog.value = !showFailedDialog.value)}
                  >
                    {t('flow.delegation.confirm')}
                  </Button>
                </>
              ),
            }}
          </Dialog>
        </div>
      </>
    )
  },
})
