import { defineComponent, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { storeToRefs } from 'pinia'
import { Dialog, Loading, Input, Tree, Checkbox, Button, Message } from 'bkui-vue'
import { SvgIcon } from '@/components/SvgIcon'
import { useAddToGroupStore } from '@/stores/addToGroupStore'
import styles from './AddToGroupPopup.module.css'

export default defineComponent({
  name: 'AddToGroupPopup',
  components: {
    SvgIcon,
  },
  props: {
    isShow: {
      type: Boolean,
      default: false,
    },
    data: {
      type: Object,
      default: () => ({}),
    },
    loading: {
      type: Boolean,
      default: false,
    },
  },
  emits: ['update:isShow', 'confirm'],
  setup(props, { emit }) {
    const { t } = useI18n()
    const store = useAddToGroupStore()
    const { loading, filterKeyword, pipelineGroupsTree, selectedGroups } = storeToRefs(store)

    watch(
      () => props.isShow,
      (newValue) => {
        if (newValue) {
          store.initPopup(props.data)
        }
      },
      { immediate: true },
    )

    const onClose = () => {
      emit('update:isShow', false)
      store.emptySelectedGroups()
      filterKeyword.value = ''
    }

    const onConfirm = () => {
      const viewIds = selectedGroups.value.map((i) => i.id)
      if (viewIds.length === 0) {
        Message({ message: t('flow.dialog.addGroup.pleaseSelectGroup'), theme: 'error' })
        return
      }

      emit('confirm', props.data.pipelineId, viewIds)
    }

    return () => (
      <Dialog
        is-show={props.isShow}
        quick-close={false}
        class={styles.addToGroupPopup}
        width={800}
        zIndex={1000}
        isLoading={props.loading}
        onClosed={onClose}
        onHidden={onClose}
        onConfirm={onConfirm}
      >
        <Loading loading={loading.value} size="small">
          <div class={styles.addGroupMain}>
            <aside class={styles.addGroupLeft}>
              <header>{t('flow.content.addTo')}</header>
              <p
                v-html={t('flow.dialog.addGroup.addToGroupTitle', [props.data?.name])}
                class={styles.addToGroupTitle}
              ></p>
              <Input
                placeholder={t('flow.dialog.addGroup.searchFlowGroup')}
                v-model={filterKeyword.value}
                type="search"
              />
              <Tree
                data={pipelineGroupsTree.value}
                expand-all
                node-key="id"
                selectable={false}
                show-node-type-icon={false}
                class={styles.addToFlowGroupList}
                label="name"
                search={filterKeyword.value}
              >
                {{
                  node: (node: any) => {
                    return (
                      <div
                        class={styles.addToFlowGroupTreeNode}
                        onClick={(e: Event) => e.stopPropagation()}
                      >
                        <div class="flex-1">
                          <Checkbox
                            modelValue={node.checked}
                            indeterminate={node.indeterminate}
                            disabled={node.disabled}
                            onChange={(checked: boolean) => store.handleCheck(checked, node)}
                            class={styles.iconMiddle}
                          />
                          <span class={styles.addedGroupName}>{node.name}</span>
                          {node.hasChild ? <span>({node.children?.length || 0})</span> : null}
                        </div>
                        {node.desc ? <span class={styles.addedGroupDesc}>{node.desc}</span> : null}
                      </div>
                    )
                  },
                }}
              </Tree>
            </aside>
            <aside class={styles.addGroupRight}>
              <header>{t('flow.dialog.addGroup.resultPreview')}</header>
              <p class="pt-lg pb-lg">
                <span class={styles.resultPreviewTip}>
                  {t('flow.dialog.addGroup.selectedGroup')}
                  <i class={styles.addGroupCount}> {selectedGroups.value.length} </i>
                  {t('flow.dialog.addGroup.selectedGroupSuffix')}
                </span>
                {selectedGroups.value.length ? (
                  <Button
                    theme="primary"
                    text
                    size="small"
                    onClick={() => store.emptySelectedGroups()}
                  >
                    <span class="text-xs">{t('flow.common.reset')}</span>
                  </Button>
                ) : null}
              </p>
              <ul class={styles.addGroupResultPreviewList}>
                {selectedGroups.value.map((group) => (
                  <li key={group.id} class={styles.viewItem}>
                    <span class="flex-1 text-ellipsis">{group.name}</span>
                    <span onClick={() => store.remove(group)} class={styles.iconClose}>
                      <SvgIcon name="close-line" size={14} />
                    </span>
                  </li>
                ))}
              </ul>
            </aside>
          </div>
        </Loading>
      </Dialog>
    )
  },
})
