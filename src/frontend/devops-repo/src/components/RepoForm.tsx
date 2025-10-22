/*
* Tencent is pleased to support the open source community by making
* 蓝鲸智云PaaS平台社区版 (BlueKing PaaS Community Edition) available.
*
* Copyright (C) 2021 Tencent.  All rights reserved.
*
* 蓝鲸智云PaaS平台社区版 (BlueKing PaaS Community Edition) is licensed under the MIT License.
*
* License for 蓝鲸智云PaaS平台社区版 (BlueKing PaaS Community Edition):
*
* ---------------------------------------------------
* Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
* documentation files (the "Software"), to deal in the Software without restriction, including without limitation
* the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
* to permit persons to whom the Software is furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in all copies or substantial portions of
* the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
* THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
* CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
* IN THE SOFTWARE.
*/
import { defineComponent, computed, PropType, reactive, watch, ref } from 'vue';
import { Input, Radio, Form, Link, Popover } from 'bkui-vue';
import { useI18n } from 'vue-i18n';
import {
  ACCESS_AUTH_TYPE_LIST,
  DOCS,
  PROJECT_AUTH,
  PUBLIC_AUTH,
  REPO_TYPE_LIST,
  SYSTEM_AUTH,
} from '@/utils/conf';
import Icon from './Icon';
import { CommonOption, RepoParams } from '@/utils/vue-ts';
import BlockEnumSelect from './BlockEnumSelect';
import { getIconNameByRepoType } from '@/utils';
import { useStore } from '@/store';
import { CHECK_REPO_EXIST } from '@/store/constants';
import { useRouteParams } from '@/hooks';

const { FormItem } = Form;
const RadioGroup = Radio.Group;
export default defineComponent({
  props: {
    modelValue: {
      type: Object as PropType<RepoParams>,
      default: () => ({}),
    },
    isEdit: {
      type: Boolean,
    },
    submitting: Boolean,
  },
  emits: ['update:modelValue', 'change', 'input', 'submit'],
  setup(props, ctx) {
    const { t } = useI18n();
    const formRef = ref();
    const store = useStore();
    const routeParams = useRouteParams();
    const autType = computed(() => {
      switch (true) {
        case props.modelValue?.public:
          return PUBLIC_AUTH;
        case props.modelValue?.system:
          return SYSTEM_AUTH;
        default:
          return PROJECT_AUTH;
      };
    });
    const formData = reactive<RepoParams>({
      ...props.modelValue,
    });
    const repoNameRegex = /^[a-z][\w-]{1,31}$/;
    const metaRuleRegex = /^[^\s]+:[^\s]+/;
    const nameRule = [{
      pattern: repoNameRegex,
      message: t('repoNamePlacehodler'),
      trigger: 'blur',
    }];

    const repoNameRule = [{
      validator: async (name: string) => {
        const isExist = await store.dispatch(CHECK_REPO_EXIST, {
          projectId: routeParams.value.projectId,
          name,
        });
        return !isExist;
      },
      message: t('repoNamePlacehodler1'),
      trigger: 'blur',
    }, ...nameRule];

    const metaRule = [{
      pattern: metaRuleRegex,
      message: t('metadataRule'),
      trigger: 'blur',
    }];

    watch(() => ({ ...formData }), () => {
      ctx.emit('update:modelValue', formData);
      ctx.emit('input', formData);
      ctx.emit('change', formData);
    });

    ctx.expose({
      validate: () => formRef.value?.validate(),
    });

    const formConf = computed(() => {
      const formItem = props.isEdit ? [
        {
          label: 'repoName',
          content: () => (
            <div class="repo-name-item">
              <Icon size={20} name={getIconNameByRepoType(formData.repoType)} />
              <span>{formData.name}</span>
            </div>
          ),
        },
        {
          label: 'repoAddress',
          content: () => (
            <span>{formData.address}</span>
          ),
        },
      ] : [
        {
          label: 'repoType',
          required: true,
          property: 'type',
          content: () => (
            <BlockEnumSelect
              options={REPO_TYPE_LIST}
              v-model={formData.type}
            >
              {{
                default: (option: CommonOption) => (
                  <div class="repo-type-item">
                    <Icon size={30} name={option.id} />
                    <span>{option.name}</span>
                  </div>
                ),
              }}
            </BlockEnumSelect>
          ),
        }, {
          label: 'repoName',
          required: true,
          property: 'name',
          rules: repoNameRule,
          content: () => (
            <Input
              v-model={formData.name}
              placeholder={t('repoNamePlacehodler')}
            />
          ),
        },
      ];
      return [
        ...formItem,
        {
          label: 'accessAuth',
          content: () => (
            <BlockEnumSelect
              modelValue={autType.value}
              onChange={handleAuthTypeChange}
              options={ACCESS_AUTH_TYPE_LIST}
              showCornerCheck
            >
              {{
                default: (option: CommonOption) => (
                  <div class="auth-item">
                    <span class="bold auth-item-header">{t(option?.tips ?? '')}</span>
                    <span class="auth-item-name">{t(option.name)}</span>
                  </div>
                ),
              }}
            </BlockEnumSelect>
          ),
        },
        {
          label: 'mobileDownload',
          property: 'mobile',
          content: () => (
            <>
              <RadioGroup v-model={formData.mobile.enable}>
                <Radio label={true}>{t('open')}</Radio>
                <Radio label={false}>{t('close')}</Radio>
              </RadioGroup>
              {
                formData.mobile.enable && (
                  <>
                    <FormItem property="mobile" label={t('fileName')} required rules={nameRule}>
                      <Input class="repo-meta-input" v-model={formData.mobile.filename} />
                      <Popover content={t('fileNameTips')}>
                        <Icon name="repoHelp"/>
                      </Popover>
                    </FormItem>
                    <FormItem property="mobile" label={t('metaData')} required rules={metaRule}>
                      <Input
                        class="repo-meta-input"
                        placeholder={t('metadataRule')}
                        v-model={formData.mobile.metadata}
                      />
                      <Link theme="primary" target="__blank" href={DOCS.META_DOCS} >{t('metaDataLink')}</Link>
                    </FormItem>
                  </>
                )
              }
            </>
          ),
        },
        {
          label: 'webDownload',
          property: 'web',
          content: () => (
            <>
              <RadioGroup v-model={formData.web.enable}>
                <Radio label={true}>{t('open')}</Radio>
                <Radio label={false}>{t('close')}</Radio>
              </RadioGroup>
              {
                formData.web.enable && (
                  <>
                    <FormItem property='web' label={t('fileName')} required rules={nameRule}>
                      <Input class="repo-meta-input" v-model={formData.web.filename} />
                      <Popover content={t('fileNameTips')}>
                        <Icon name="repoHelp"/>
                      </Popover>
                    </FormItem>
                    <FormItem property='web' label={t('metaData')} required rules={[metaRule]}>
                      <Input
                        class="repo-meta-input"
                        placeholder={t('metadataRule')}
                        v-model={formData.web.metadata}
                      />
                      <Link theme="primary" target="__blank" href={DOCS.META_DOCS} >{t('metaDataLink')}</Link>
                    </FormItem>
                  </>
                )
              }
            </>
          ),
        },
        {
          label: 'description',
          property: 'description',
          content: () => (
            <Input
              type="textarea"
              showWordLimit
              maxlength={200}
              placeholder={t('repoDescriptionPlacehodler')}
              v-model={formData.description}
            />
          ),
        },
      ];
    });

    function handleAuthTypeChange(id: string) {
      formData.system = false;
      formData.public = false;
      switch (id) {
        case PUBLIC_AUTH:
          formData.public = true;
          break;
        case SYSTEM_AUTH:
          formData.system = true;
          break;
      };
    }

    return () => (
      <Form model={formData} ref={formRef} class="repo-form">
        {
          formConf.value.map((item: any) => (
            <FormItem property={item.property} label={t(item.label)} required={item.required} rules={item.rules}>
              {item.content?.()}
            </FormItem>
          ))
        }
      </Form>
    );
  },
});
