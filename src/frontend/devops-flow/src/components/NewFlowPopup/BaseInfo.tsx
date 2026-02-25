import AuthoringEnv from '@/components/AuthoringEnv'
import { SvgIcon } from '@/components/SvgIcon'
import useAuthoringEnvironment from '@/hooks/useAuthoringEnvironment'
import { Form, Input } from 'bkui-vue'
import { defineComponent, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import styles from './Index.module.css'

export default defineComponent({
  name: 'BaseInfo',
  props: {
    modelValue: {
      type: Object,
      default: () => ({
        pipelineName: '',
        desc: '',
        envHashId: '',
      }),
    },
  },
  emits: ['update:modelValue'],
  setup(props, { emit, expose }) {
    const { t } = useI18n()
    const formRef = ref()
    const baseInfoData = ref({ ...props.modelValue })

    const { envSelectList, envListLoading, nodeList, nodeListLoading, goEnvironment } =
      useAuthoringEnvironment({
        autoLoadEnvList: true,
      })

    expose({
      formRef,
    })

    watch(
      () => props.modelValue,
      (nv) => {
        baseInfoData.value = nv
      },
    )

    function handleChange() {
      emit('update:modelValue', baseInfoData.value)
    }

    function updateAuthoringEnv(env: string) {
      baseInfoData.value.envHashId = env
      handleChange()
    }

    function goToEnvironment() {
      goEnvironment(baseInfoData.value.envHashId)
    }

    return () => (
      <Form class={styles.baseInfo} ref={formRef} model={baseInfoData.value} form-type="vertical">
        <div class={styles.baseItem}>
          <p class={styles.baseTitle}>{t('flow.content.basicInfo')}</p>
          <Form.FormItem
            label={t('flow.content.name')}
            property="pipelineName"
            required
            maxlength={128}
          >
            <Input
              v-model={baseInfoData.value.pipelineName}
              onChange={handleChange}
              placeholder={t('flow.content.inputFlowName')}
            ></Input>
          </Form.FormItem>
          <Form.FormItem label={t('flow.content.description')} property="pipelineDesc">
            <Input
              v-model={baseInfoData.value.pipelineDesc}
              onChange={handleChange}
              type="textarea"
            ></Input>
          </Form.FormItem>
        </div>
        <div class={styles.baseItem}>
          <p class={styles.baseTitle}>
            <span>{t('flow.content.creationEnvironment')}</span>
            <span class={styles.titleSet} onClick={goToEnvironment}>
              <SvgIcon name="jump" size={12} class={styles.jumpIcon} />
              {t('flow.content.environmentManagement')}
            </span>
          </p>
          <Form.FormItem property="envHashId" required>
            {{
              error: () => t('flow.content.environmentRequired'),
              default: () => (
                <>
                  <AuthoringEnv
                    isEdit={true}
                    envLoading={envListLoading.value}
                    modelValue={baseInfoData.value.envHashId}
                    onUpdate:modelValue={updateAuthoringEnv}
                    envList={envSelectList.value}
                    nodeLoading={nodeListLoading.value}
                    nodeList={nodeList.value}
                  />
                </>
              ),
            }}
          </Form.FormItem>
        </div>
      </Form>
    )
  },
})
