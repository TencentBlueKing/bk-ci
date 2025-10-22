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
import { useStore } from '@/store';
import { FETCH_USER_SETTING, SET_USER_INFO, UPDATE_USER_INFO } from '@/store/constants';
import { asyncAction } from '@/utils';
import { UserInfoFields } from '@/utils/vue-ts';
import { Button, Form, Input } from 'bkui-vue';
import { computed, defineComponent, onBeforeMount, ref } from 'vue';
import { useI18n } from 'vue-i18n';

const { FormItem } = Form;
interface UserMenuItem {
  label: string
  key: UserInfoFields
}
export default defineComponent({
  setup() {
    const { t } = useI18n();
    const store = useStore();
    const editingField = ref();
    const editFieldValue = ref();
    const editing = ref(false);
    const loading = ref(true);
    const currentUser = computed(() => store.state.currentUser);
    const items: UserMenuItem[] = [
      {
        label: t('chineseName'),
        key: 'name',
      },
      {
        label: t('phone'),
        key: 'phone',
      },
      {
        label: t('email'),
        key: 'email',
      },
    ];

    onBeforeMount(getUserDetail);

    async function getUserDetail() {
      loading.value = true;
      await store.dispatch(FETCH_USER_SETTING);
      loading.value = false;
    }

    function handleEdit(key?: UserInfoFields) {
      if (typeof key === 'string') {
        editingField.value = key;
        editFieldValue.value = currentUser.value?.[key] ?? '';
      } else {
        editingField.value = undefined;
        editFieldValue.value = '';
      }
    }

    async function confirmEdit() {
      const body = {
        userId: currentUser.value?.username,
        [editingField.value]: editFieldValue.value,
      };
      const action = asyncAction(() => store.dispatch(UPDATE_USER_INFO, {
        username: currentUser.value?.username,
        body,
      }), t('editSuccess'));
      editing.value = true;
      await action();
      store.commit(SET_USER_INFO, body);
      editing.value = false;
      handleEdit();
    }

    return () => (
      <>
        <Form class="bk-repo-user-info-form">
          {
            items.map(item => (
              <FormItem  key={item.key} label={item.label}>
                <div class="bk-repo-user-info-form-item">
                  {
                    editingField.value === item.key ? (
                      <>
                        <Input v-model={editFieldValue.value}  maxlength={32} showWordLimit />
                        <Button class="user-info-item-editing-cancel-btn" onClick={handleEdit}>{t('cancel')}</Button>
                        <Button theme='primary' onClick={confirmEdit}>{t('confirm')}</Button>
                      </>
                    ) : (
                      <>
                        <span class="bk-repo-user-info-item-value">{currentUser.value?.[item.key] ?? '--'}</span>
                        {!editingField.value && <Button text theme='primary' onClick={() => handleEdit(item.key)}>{t('modify')}</Button>}
                      </>
                    )
                  }
                </div>
              </FormItem>
            ))
          }
        </Form>
      </>
    );
  },
});
