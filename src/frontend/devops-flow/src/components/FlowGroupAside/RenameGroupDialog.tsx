import { defineComponent, reactive, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { Dialog, Input, Message } from 'bkui-vue';
import styles from './CreateGroupDialog.module.css';

interface Props {
  isShow: boolean
  groupId: string
  currentName: string
  isLoading: boolean
}

export const RenameGroupDialog = defineComponent({
  name: 'RenameGroupDialog',
  props: {
    isShow: {
      type: Boolean,
      default: false,
    },
    groupId: {
      type: String,
      required: true,
    },
    currentName: {
      type: String,
      required: true,
    },
    isLoading: {
      type: Boolean,
      default: false,
    },
  },
  emits: ['update:isShow', 'confirm'],
  setup(props: Props, { emit }) {
    const { t } = useI18n();
    const group = reactive({
      name: props.currentName,
    });

    watch(() => props.currentName, (newVal) => {
      group.name = newVal;
    });

    watch(() => props.isShow, (newVal) => {
      if (newVal) {
        group.name = props.currentName;
      }
    });

    const resetForm = () => {
      group.name = props.currentName;
    };

    const handleClose = () => {
      emit('update:isShow', false);
      resetForm();
    };

    const handleConfirm = () => {
      if (!group.name.trim()) {
        Message({ theme: 'error', message: t('flow.dialog.renameGroup.groupNameRequired') });
        return;
      }
      emit('confirm', { id: props.groupId, name: group.name.trim() });
      handleClose();
    };

    return () => (
      <Dialog
        isShow={props.isShow}
        title={t('flow.dialog.renameGroup.title')}
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
                <label class={styles.label}>
                  {t('flow.dialog.renameGroup.groupName')}
                </label>
                <Input
                  v-model={group.name}
                  placeholder={t('flow.dialog.renameGroup.groupNamePlaceholder')}
                  maxlength={50}
                />
              </div>
            </div>
          ),
        }}
      </Dialog>
    );
  },
});

