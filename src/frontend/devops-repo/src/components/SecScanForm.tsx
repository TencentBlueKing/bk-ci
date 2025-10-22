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
import { useRouteParams } from '@/hooks';
import { useStore } from '@/store';
import { FETCH_SEC_SCAN_LIST } from '@/store/constants';
import { CommonOption } from '@/utils/vue-ts';
import { Form, Select } from 'bkui-vue';
import { defineComponent, onBeforeMount, reactive, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';

const { FormItem } = Form;
const { Option } = Select;
export default defineComponent({
  props: {
    modelValue: {
      type: Object,
      default: () => ({}),
    },
  },
  emits: ['change'],
  setup(props, ctx) {
    const { t } = useI18n();
    const store = useStore();
    const options = ref<CommonOption[]>([]);
    const routeParams = useRouteParams();
    const model = reactive({
      scanType: undefined,
    });
    onBeforeMount(async () => {
      const res = await store.dispatch(FETCH_SEC_SCAN_LIST, {
        projectId: routeParams.value.projectId,
        type: routeParams.value.repoType.toUpperCase(),
      });
      options.value = res;
    });

    watch(() => ({
      ...model,
    }), () => {
      console.log(model);
      ctx.emit('change', model);
    }, { immediate: true });

    return () => (
      <Form model={model}>
        <FormItem required label={t('scanTypeLabel')} property="scanType">
          <Select v-model={model.scanType} >
            {
              options.value.map(option => (
                <Option value={option.id} label={option.name}></Option>
              ))
            }
          </Select>
        </FormItem>
      </Form>
    );
  },
});
