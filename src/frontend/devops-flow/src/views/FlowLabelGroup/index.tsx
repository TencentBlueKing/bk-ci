import { SvgIcon } from '@/components/SvgIcon'
import EmptyPage from '@/components/EmptyPage'
import { Button, Dialog, Input, Loading } from 'bkui-vue'
import { defineComponent, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import type { GroupLabelItem, GroupResponse } from '@/api/flowLabelGroup'
import { useFlowLabelGroup } from '@/hooks/useFlowLabelGroup'
import styles from './FlowLabelGroup.module.css'

export default defineComponent({
  name: 'FlowLabelGroup',
  setup() {
    const { t } = useI18n()
    const {
      loading,
      showContent,
      tagList,
      isShowInputIndex,
      labelValue,
      activeEditKey,
      tagValue,
      btnDisabled,
      groupDialog,
      // Computed
      isShowGroupBtn,
      // Utilities
      getEditKey,
      // Group Operations
      showGroupInput,
      handleGroupCancel,
      handleGroupSave,
      showGroupDialog,
      handleGroupDialogConfirm,
      deleteGroup,
      clearGroupDialogError,
      // Tag Operations
      tagEdit,
      tagAdd,
      tagSave,
      tagCancel,
      tagModify,
      deleteTag,
      // init
      init,
    } = useFlowLabelGroup()

    onMounted(() => {
      init()
    })

    // ==================== Render Helpers ====================
    function renderGroupTitle(group: GroupResponse, groupIndex: number) {
      const isEditing = isShowInputIndex.value === groupIndex

      return (
        <div class={styles.groupTitle}>
          <div class={styles.titleText}>
            <span>
              <b style="margin-right: 8px">{groupIndex + 1}</b>
              {!isEditing && <span>{group.name}</span>}
            </span>
            {!isEditing ? (
              <span>
                <a class={styles.entryLink} onClick={() => showGroupInput(groupIndex, group.name)}>
                  {t('flow.labelGroup.rename')}
                </a>
                <a class={styles.entryLink} onClick={() => deleteGroup(groupIndex)}>
                  {t('flow.labelGroup.delete')}
                </a>
              </span>
            ) : (
              <div class={styles.groupTitleInput}>
                <Input
                  style="width: 94%; margin-right: 10px;"
                  placeholder={t('flow.labelGroup.groupNamePlaceholder')}
                  v-model={labelValue.value}
                  maxlength={20}
                  onBlur={() => {}}
                  onEnter={() => handleGroupSave(groupIndex)}
                />
                <div class={styles.groupTitleBtns}>
                  <a class={styles.entryLink} onClick={() => handleGroupSave(groupIndex)}>
                    {t('flow.labelGroup.save')}
                  </a>
                  <a
                    class={styles.entryLink}
                    onClick={() => handleGroupCancel(groupIndex, group.name)}
                  >
                    {t('flow.labelGroup.cancel')}
                  </a>
                </div>
              </div>
            )}
          </div>
        </div>
      )
    }

    function renderTagCard(tag: GroupLabelItem, groupIndex: number, tagIndex: number) {
      const editKey = getEditKey(groupIndex, tagIndex)
      const isEditing = activeEditKey.value === editKey

      return (
        <div
          key={tagIndex}
          class={[styles.groupCard, isEditing && styles.groupEdit]}
          onMouseleave={() => {}}
        >
          <div class={styles.groupCardTitle}>
            <span class={styles.tagText}>{tag.name}</span>
            <Input
              class={styles.tagInput}
              v-model={tagValue.value}
              maxlength={20}
              placeholder={t('flow.labelGroup.tagNamePlaceholder')}
              onEnter={() => tagModify(groupIndex, tagIndex)}
            />
          </div>
          {!isEditing ? (
            <div class={styles.groupCardTools}>
              <span onClick={() => tagEdit(groupIndex, tagIndex)}>
                <SvgIcon name="edit" size={12} class={styles.groupCardIcon} />
              </span>
              <span onClick={() => deleteTag(groupIndex, tagIndex)}>
                <SvgIcon name="trash-bin" size={12} class={styles.groupCardIcon} />
              </span>
            </div>
          ) : (
            <div class={styles.groupCardEditTools}>
              <span onClick={() => tagSave(groupIndex, tagIndex)}>
                <SvgIcon name="check-line" size={12} class={styles.groupCardEditIcon} />
              </span>
              <span onClick={() => tagCancel(groupIndex, tagIndex)}>
                <SvgIcon name="close-line" size={12} class={styles.groupCardEditIcon} />
              </span>
            </div>
          )}
        </div>
      )
    }

    function renderGroupCards() {
      return tagList.value.map((group, groupIndex) => (
        <div key={group.id} class={styles.groupCards}>
          {renderGroupTitle(group, groupIndex)}

          {group.labels.map((tag, tagIndex) => renderTagCard(tag, groupIndex, tagIndex))}

          {group.labels.length < 12 && (
            <Button
              class={styles.groupCardAdd}
              disabled={btnDisabled.value}
              onClick={() => tagAdd(groupIndex)}
            >
              <SvgIcon name="add-small" size={18} />
              {t('flow.labelGroup.addTag')}
            </Button>
          )}
        </div>
      ))
    }

    function renderEmptyState() {
      if (tagList.value.length > 0) return null

      return (
        <div class={styles.emptyState}>
          <EmptyPage title={t('flow.labelGroup.emptyTitle')}>
            <div class={styles.emptyContent}>
              <p>{t('flow.labelGroup.desc')}</p>
              <p>{t('flow.labelGroup.contentOne')}</p>
              <p>{t('flow.labelGroup.contentTwo')}</p>
            </div>
            <div class={styles.emptyAction}>
              <Button theme="primary" onClick={showGroupDialog}>
                {t('flow.labelGroup.addGroup')}
              </Button>
            </div>
          </EmptyPage>
        </div>
      )
    }

    function renderHint() {
      if (tagList.value.length === 0) return null

      return (
        <div class={styles.groupHint}>
          <SvgIcon name="info-line" size={14} />
          <span>{t('flow.labelGroup.hint')}</span>
        </div>
      )
    }

    // ==================== Render ====================
    return () => (
      <div class={styles.flowLabelGroup}>
        <Loading loading={loading.value}>
          <header class={styles.header}>
            <h3>{t('flow.labelGroup.title')}</h3>
          </header>

          {showContent.value && (
            <section class={styles.content}>
              {renderHint()}
              {renderGroupCards()}

              {isShowGroupBtn.value && (
                <div class={styles.groupCards}>
                  <Button
                    class={styles.groupCardCreate}
                    disabled={btnDisabled.value}
                    onClick={showGroupDialog}
                  >
                    <SvgIcon name="add-small" size={18} />
                    {t('flow.labelGroup.addGroup')}
                  </Button>
                </div>
              )}

              {renderEmptyState()}
            </section>
          )}
        </Loading>

        <Dialog
          v-model:is-show={groupDialog.value.isShow}
          title={groupDialog.value.title}
          width={480}
          header-position="left"
          onConfirm={handleGroupDialogConfirm}
        >
          <div class={styles.dialogContent}>
            <div class={styles.formItem}>
              <label class={styles.formLabel}>{t('flow.labelGroup.groupName')}</label>
              <Input
                v-model={groupDialog.value.value}
                placeholder={t('flow.labelGroup.groupNamePlaceholder')}
                maxlength={20}
                class={groupDialog.value.error ? styles.inputError : ''}
                onInput={clearGroupDialogError}
              />
              {groupDialog.value.error && (
                <div class={styles.errorText}>{groupDialog.value.error}</div>
              )}
            </div>
          </div>
        </Dialog>
      </div>
    )
  },
})
