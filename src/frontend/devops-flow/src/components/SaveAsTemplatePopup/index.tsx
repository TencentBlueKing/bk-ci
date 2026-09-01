import { defineComponent, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Dialog, Form, Input, Loading, Radio } from 'bkui-vue'
import { type SaveAsTemplateParams } from '@/api/flowContentList'

export default defineComponent({
  name: 'SaveAsTemplatePopup',
  props: {
    isShow: {
      type: Boolean,
      default: false,
    },
    data: {
      type: Object,
      default: () => {},
    },
    loading: {
      type: Boolean,
      default: false,
    },
  },
  emits: ['update:isShow', 'confirm'],
  setup(props, { emit }) {
    const { t } = useI18n()
    const formRef = ref()
    const initFormData = () => {
      return {
        templateName: '',
        copySetting: true,
      }
    }
    const formData = ref<SaveAsTemplateParams>(initFormData())

    const onClose = () => {
      formRef.value.clearValidate()
      formData.value = initFormData()
      emit('update:isShow', false)
    }

    const onConfirm = async () => {
      const valide = await formRef.value.validate()
      if (valide) {
        const params: SaveAsTemplateParams = {
          pipelineId: props.data.pipelineId,
          ...formData.value,
        }
        emit('confirm', params)
      }
    }

    return () => (
      <Dialog
        is-show={props.isShow}
        title={t('flow.content.saveAsTemplate')}
        quick-close={false}
        isLoading={props.loading}
        zIndex={1000}
        onClosed={onClose}
        onHidden={onClose}
        onConfirm={onConfirm}
      >
        <Loading loading={props.loading} size="small">
          <Form ref={formRef} model={formData.value} label-width={120}>
            <Form.FormItem
              property="templateName"
              label={t('flow.dialog.saveAsTemplate.templateName')}
              maxlength={30}
              required
            >
              <Input
                v-model={formData.value.templateName}
                placeholder={t('flow.dialog.saveAsTemplate.templateNamePlaceholder')}
              ></Input>
            </Form.FormItem>
            <Form.FormItem
              property="copySetting"
              label={t('flow.dialog.saveAsTemplate.isCopySetting')}
              description={t('flow.dialog.saveAsTemplate.settingTooltip')}
            >
              <Radio.Group v-model={formData.value.copySetting}>
                <Radio label={true}>{t('flow.dialog.saveAsTemplate.true')}</Radio>
                <Radio label={false}>{t('flow.dialog.saveAsTemplate.false')}</Radio>
              </Radio.Group>
            </Form.FormItem>
          </Form>
        </Loading>
      </Dialog>
    )
  },
})
