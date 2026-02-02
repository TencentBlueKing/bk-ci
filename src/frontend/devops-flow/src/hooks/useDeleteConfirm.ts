import { InfoBox } from 'bkui-vue'
import { h } from 'vue'
import { useI18n } from 'vue-i18n'
import { SvgIcon } from '../components/SvgIcon'
import styles from '../styles/useDeleteConfirm.module.css'

interface DeleteConfirmOptions {
  title?: string
  message: string | (() => any)
  description?: string
  confirmText?: string
  cancelText?: string
  theme?: 'primary' | 'danger' | 'warning' | 'success'
  onConfirm: () => void | Promise<void>
  onCancel?: () => void
}

export function useDeleteConfirm() {
  const { t } = useI18n()

  const showDeleteConfirm = (options: DeleteConfirmOptions) => {
    const {
      title,
      message,
      description,
      confirmText,
      cancelText,
      theme = 'danger',
      onConfirm,
      onCancel,
    } = options

    const messageContent = typeof message === 'function' ? message() : message

    const infoBoxInstance = InfoBox({
      title: title || '',
      content: h(
        'div',
        {
          class: styles.deleteConfirmContent,
        },
        [
          h(SvgIcon, {
            name: 'exclamation-circle-shape',
            class: [styles.deleteConfirmIcon, theme === 'danger' && styles.dangerIcon],
            size: 48,
          }),
          h('div', { class: styles.deleteConfirmMessage }, messageContent),
        ],
      ),
      confirmText: confirmText || t('flow.actions.delete'),
      cancelText: cancelText || t('flow.common.close'),
      theme,
      onConfirm: async () => {
        try {
          await onConfirm()
          infoBoxInstance.hide()
        } catch (error) {
          console.error('Delete confirm action failed:', error)
          // 不关闭弹窗，让用户重试
        }
      },
      onClose: () => {
        onCancel?.()
        return true
      },
    })

    return infoBoxInstance
  }

  return {
    showDeleteConfirm,
  }
}
