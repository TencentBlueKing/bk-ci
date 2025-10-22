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
import ConfirmDialog from '@/components/ConfirmDialog';
import { PromiseOr } from '@/utils/vue-ts';
import { ref } from 'vue';

export default function useConfirm() {
  const isShow = ref(false);
  const confirming = ref(false);
  const message = ref('');
  const description = ref();
  const confirmAction = ref();
  function toggleisShow(show: boolean) {
    return () => isShow.value = show;
  }

  function onClose() {
    isShow.value = false;
  }

  function showConfirm(msg: string, desc?: string) {
    message.value = msg;
    description.value = desc;
    isShow.value = true;
  }

  function onConfirm(cb: PromiseOr) {
    confirmAction.value = cb;
  }

  async function confirm() {
    confirming.value = true;
    await confirmAction.value?.();
    confirming.value = false;
    onClose();
  }

  function renderConfirmDialog() {
    return (
      <ConfirmDialog
        isShow={isShow.value}
        message={message.value}
        description={description.value}
        onClose={onClose}
        onSubmit={confirm}
        isSubmiting={confirming.value}
      >
      </ConfirmDialog>
    );
  }
  return {
    isShow,
    showConfirm,
    renderConfirmDialog,
    closeConfirm: toggleisShow(false),
    onConfirm,
  };
}
