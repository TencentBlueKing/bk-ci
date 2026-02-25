import { defineComponent, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Dialog, Input, Radio, Button, Message } from 'bkui-vue'
import styles from './CreateGroupDialog.module.css'

interface Props {
  isShow: boolean
  projected: boolean
  isLoading?: boolean
}

export const CreateGroupDialog = defineComponent({
  name: 'CreateGroupDialog',
  props: {
    isShow: {
      type: Boolean,
      default: false,
    },
    projected: {
      type: Boolean,
      required: true,
    },
    isLoading: {
      type: Boolean,
      default: false,
    },
  },
  emits: ['update:isShow', 'confirm'],
  setup(props: Props, { emit }) {
    const { t } = useI18n()
    const group = reactive({
      name: '',
      projected: props.projected,
    })

    watch(
      () => props.projected,
      (newValue) => {
        group.projected = newValue
      },
    )

    // 重置表单
    const resetForm = () => {
      group.name = ''
      group.projected = props.projected
    }

    // 关闭弹窗
    const handleClose = () => {
      emit('update:isShow', false)
      resetForm()
    }

    // 确认创建
    const handleConfirm = () => {
      if (!group.name.trim()) {
        Message({ theme: 'error', message: t('flow.dialog.createGroup.groupNameRequired') })
        return
      }
      emit('confirm', group)
      handleClose()
    }

    return () => (
      <Dialog
        isShow={props.isShow}
        title={t('flow.dialog.createGroup.title')}
        width={480}
        isLoading={props.isLoading}
        onCancel={handleClose}
        onClosed={handleClose}
        onConfirm={handleConfirm}
      >
        {{
          default: () => (
            <div class={styles.dialogContent}>
              <div class={styles.formItem}>
                <label class={styles.label}>{t('flow.dialog.createGroup.groupName')}</label>
                <Input
                  v-model={group.name}
                  placeholder={t('flow.dialog.createGroup.groupNamePlaceholder')}
                  maxlength={50}
                />
              </div>

              <div class={styles.formItem}>
                <label class={styles.label}>
                  {t('flow.dialog.createGroup.visibilityScope')}
                  <span class={styles.required}>*</span>
                </label>
                <Radio.Group v-model={group.projected}>
                  <Radio label={false} class={styles.radio}>
                    {t('flow.dialog.createGroup.personalVisible')}
                  </Radio>
                  <Radio label={true} class={styles.radio}>
                    {t('flow.dialog.createGroup.projectVisible')}
                  </Radio>
                </Radio.Group>
              </div>
            </div>
          ),
        }}
      </Dialog>
    )
  },
})
