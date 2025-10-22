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
import { defineComponent, ref, watch } from 'vue';
import { Input, Form } from 'bkui-vue';

import { useI18n } from 'vue-i18n';

const { FormItem } = Form;
export default defineComponent({
  props: {
    isCreateFolder: {
      type: Boolean,
    },
    value: {
      type: String,
      required: false,
    },
  },
  emits: ['change'],
  setup(props, ctx) {
    const { t } = useI18n();
    const invalidChars = '\\ / : * ? " < > |';
    const fileName = ref(props.value);

    watch(() => props.value, (value) => {
      fileName.value = value;
    });
    watch(() => fileName.value, () => {
      ctx.emit('change', fileName.value);
    }, { immediate: true });
    return () => (
      <Form>
        {
          props.isCreateFolder ? (
            <FormItem required label={t('folderPath')}>
              <Input
                  type="textarea"
                  placeholder={`${t('folderPathPlacehodler', [invalidChars])}`}
                  v-model={fileName.value}
                />
              <span class="desc">{t('createFolderTips')}</span>
            </FormItem>
          ) : (
            <FormItem required labelWidth={88} label={t('fileName')}>
              <Input
                placeholder={`${t('folderNamePlacehodler', [invalidChars])}`}
                v-model={fileName.value}
                maxlength={50}
                showWordLimit
              />
          </FormItem>
          )
        }
      </Form>
    );
  },
});
