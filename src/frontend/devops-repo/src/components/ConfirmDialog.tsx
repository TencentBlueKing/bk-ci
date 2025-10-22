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
import { defineComponent } from 'vue';
import { Dialog, Button } from 'bkui-vue';

import { useI18n } from 'vue-i18n';
import Icon from './Icon';
export default defineComponent({
  props: {
    isShow: {
      type: Boolean,
      default: false,
    },
    theme: {
      type: String,
    },
    message: {
      type: String,
    },
    description: {
      type: String,
    },
    isSubmiting: Boolean,
  },
  emits: ['close', 'submit'],
  setup(props, ctx) {
    const { t } = useI18n();
    function handleClose() {
      ctx.emit('close', false);
    }

    function handleConfirm() {
      ctx.emit('submit');
    }

    return () => (
      <Dialog
        isShow={props.isShow}
        width={360}
        height={222}
        title={t('operationConfirm')}
        onClosed={handleClose}
      >
        {{
          default: () => (
            <main class="bk-repo-confirm-dialog-main">
              <Icon name="exclamation-triangle-shape" size="60" />
              <div class="bk-repo-confirm-dialog-content">
                <p>{props.message}</p>
                { props.description && <p class="bk-repo-confirm-desc">{props.description}</p> }
              </div>
            </main>
          ),
          footer: () => (
            <>
              <Button onClick={handleClose}>{t('cancel')}</Button>
              <Button
                class="create-repo-dialog-cancel-btn"
                theme="danger"
                disabled={props.isSubmiting}
                loading={props.isSubmiting}
                onClick={handleConfirm}
              >
                {t('confirm')}
              </Button>
            </>
          ),
        }}
      </Dialog>
    );
  },
});
