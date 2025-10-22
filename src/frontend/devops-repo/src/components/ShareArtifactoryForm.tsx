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
import { defineComponent, reactive, watch } from 'vue';
import { Form, Input, Select, TagInput } from 'bkui-vue';
import { useI18n } from 'vue-i18n';

const { FormItem } = Form;

interface FormParam {
  authorizedUserSet: string[],
  authorizedIpSet: string[],
  permits: number,
  time: number,
}

interface FormField {
  label: string
  property: string
  FieldComponent: Function
}

export default defineComponent({
  props: {
    fullPath: {
      type: String,
      required: false,
    },
  },
  emits: ['change'],
  setup(props, ctx) {
    const { t } = useI18n();
    const shareParams = reactive<FormParam>({
      authorizedUserSet: [],
      authorizedIpSet: [],
      permits: 0,
      time: 7,
    });
    const shareTimeOptions = [
      { id: 1,
        name: '1',
      },
      { id: 7,
        name: '7',
      },
      { id: 30,
        name: '30',
      },
      { id: 0,
        name: '永久',
      },
    ];
    const formConf: FormField[] = [{
      label: t('accessUser'),
      property: 'authorizedUserSet',
      FieldComponent: () => (
        <TagInput
          v-model={shareParams.authorizedUserSet}
          allowCreate
          placeholder={t('accessUserPlaceholder')}
        />
      ),
    }, {
      label: t('accessIP'),
      property: 'authorizedIpSet',
      FieldComponent: () => (
        <TagInput
          v-model={shareParams.authorizedIpSet}
          allowCreate
          placeholder={t('accessIPPlaceholder')}
        />
      ),
    }, {
      label: t('accessCount'),
      property: 'permits',
      FieldComponent: () => (
        <Input
          v-model={shareParams.permits}
          type='number'
          placeholder={t('accessCountPlaceholder')}
        />
      ),
    }, {
      label: t('validity'),
      property: 'time',
      FieldComponent: () => (
        <Select v-model={shareParams.time} placeholder={t('accessExpired')}>
          {
            shareTimeOptions.map(opt => (
              <Select.Option
                value={opt.id}
                label={opt.name}
              >
              </Select.Option>
            ))
          }
        </Select>
      ),
    }];

    watch(() => ({
      ...shareParams,
    }), () => {
      const { authorizedIpSet, authorizedUserSet, time, permits } = shareParams;
      ctx.emit('change', {
        fullPathSet: [
          props.fullPath,
        ],
        type: 'DOWNLOAD',
        host: `${location.origin}/web/generic`,
        needsNotify: authorizedUserSet.length > 0,
        authorizedIpSet,
        authorizedUserSet,
        permits: permits > 0 ? permits : undefined,
        expireSeconds: time > 0 ? time * 86400 : undefined,
      });
    }, { immediate: true });

    return () => (
      <Form labelWidth={80}>
        {
          formConf.map(item => (
            <FormItem class="share-artifactory-form-field" property={item.property} label={item.label}>
                {item.FieldComponent()}
            </FormItem>
          ))
        }
    </Form>
    );
  },
});
