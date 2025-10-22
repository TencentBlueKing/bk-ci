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
import { defineComponent, reactive, ref } from 'vue';
import { Dialog, Button } from 'bkui-vue';
import { useI18n } from 'vue-i18n';
import { RepoParams } from '@/utils/vue-ts';
import { useRoute } from 'vue-router';
import { useStore } from '@/store';
import { CREATE_REPO } from '@/store/constants';
import RepoForm from './RepoForm';
import { GENERIC_REPO } from '@/utils/conf';
import { asyncAction } from '@/utils';

export default defineComponent({
  props: {
    modelValue: {
      type: Boolean,
    },
  },
  emits: ['update:modelValue', 'change', 'input', 'confirm'],
  setup(props, ctx) {
    const { t } = useI18n();
    const route = useRoute();
    const store = useStore();
    const repoFormRef = ref();
    const submitting = ref(false);
    const repo = reactive<RepoParams>({
      type: GENERIC_REPO,
      public: false,
      system: false,
      name: '',
      mobile: {
        enable: false,
        filename: '',
        metadata: '',
      },
      web: {
        enable: false,
        filename: '',
        metadata: '',
      },
      description: '',
    });

    async function handleConfirm() {
      const action = asyncAction(async () => {
        await store.dispatch(CREATE_REPO, {
          repoFormData: repo,
          projectId: route.params.projectId,
        });
        ctx.emit('confirm');
        handleClose();
      }, `${repo.name}${t('created')}`);
      submitting.value = true;
      await action();
      submitting.value = false;
    }

    function toggleIsShow(isShow: boolean) {
      ctx.emit('update:modelValue', isShow);
      ctx.emit('change', isShow);
      ctx.emit('input', isShow);
    }

    function handleClose() {
      toggleIsShow(false);
    }

    function handleUpdateRepo(val: RepoParams) {
      Object.assign(repo, val);
    }


    return () => (
      <Dialog
        isShow={props.modelValue}
        width={800}
        height={603}
        title={t('createRepo')}
        onClosed={handleClose}
      >
        {{
          default: () => (
            <RepoForm
              ref={repoFormRef}
              modelValue={repo}
              onInput={handleUpdateRepo}
            >
            </RepoForm>
          ),
          footer: () => (
            <>
              <Button
                loading={submitting.value}
                disabled={submitting.value}
                theme="primary"
                onClick={handleConfirm}
              >{t('confirm')}</Button>
              <Button class="create-repo-dialog-cancel-btn" onClick={handleClose}>{t('cancel')}</Button>
            </>
          ),
        }}
      </Dialog>
    );
  },
});
