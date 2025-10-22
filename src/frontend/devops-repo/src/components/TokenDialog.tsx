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
import { CREATE_TOKEN } from '@/store/constants';
import { asyncAction, copyToClipboard } from '@/utils';
import { Button, DatePicker, Dialog, Form, Input } from 'bkui-vue';
import { defineComponent, reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import Icon from './Icon';

const { FormItem } = Form;
export default defineComponent({
  props: {
    isShow: {
      type: Boolean,
      default: false,
    },
    projectId: {
      type: String,
      required: true,
    },
  },
  emits: ['close'],
  setup(props, ctx) {
    const { t } = useI18n();
    const store = useStore();
    const tokenForm = ref(null);
    const token = reactive<Record<string, any>>({
      name: '',
      expiredAt: undefined,
      created: false,
      id: undefined,
    });

    function handleCreateTokenDialogClose() {
      resetToken();
      ctx.emit('close');
    }

    function resetToken() {
      token.id = undefined;
      token.name = undefined;
      token.created = false;
      token.expiredAt = undefined;
    }

    function isDisableDate(date: any) {
      const nowDate = new Date();
      nowDate.setHours(0, 0, 0, 0);
      return date < nowDate;
    }

    async function handleConfirm() {
      if (token.created) {
        ctx.emit('close');
        resetToken();
        return;
      }
      // @ts-ignore
      await tokenForm.value.validate();
      const action = asyncAction(async () => store.dispatch(CREATE_TOKEN, {
        projectId: props.projectId,
        name: token.name,
        expiredAt: token.expiredAt?.toISOString(),
        username: store.state.currentUser?.username,
      }), `${token.name}${t('created')}`);

      const id = await action();
      token.created = true;
      token.id = id;
    }

    function handleCopyTokenId() {
      copyToClipboard(token.id, t('copyed'));
    }
    return () => (
      <Dialog
          isShow={props.isShow}
          width={540}
          height={245}
          title={t('addToken')}
          onClosed={handleCreateTokenDialogClose}
        >
          {{
            default: () => (token.created ? (
              <div class="token-created-tip">
                <div class="token-status-icon">
                  <Icon size={60} name="check" />
                </div>
                <div class="token-info">
                  <h3 class="bold">{t('created')}</h3>
                  <p class="token-id-box" onClick={handleCopyTokenId}>
                    <span>{t('tokenIs')}</span>
                    <span class="token-id">{token.id}</span>
                    <Icon name="copy" />
                  </p>
                  <p class="warning-tip">{t('tokenCopyTip')}</p>
                </div>
              </div>
            ) : (
              <Form class="create-token-form" ref={tokenForm} model={token}>
                <FormItem property='name' label={t('name')} required>
                  <Input class="token-name-input" v-model={token.name} maxlength={32} placeholder={t('pleaseInput')} showWordLimit />
                </FormItem>
                <FormItem property='expiredAt' label={t('expires')}>
                  <DatePicker
                    v-model={token.expiredAt}
                    placeholder={t('tokenExpiressTip')}
                    disableDate={isDisableDate}
                    appendToBody
                  >
                  </DatePicker>
                </FormItem>
              </Form>
            )),
            footer: () => (
              <>
                {!token.created && <Button onClick={handleCreateTokenDialogClose}>{t('cancel')}</Button>}
                <Button class="create-repo-dialog-cancel-btn" theme="primary" onClick={handleConfirm}>{t('confirm')}</Button>
              </>
            ),
          }}
        </Dialog>
    );
  },
});
