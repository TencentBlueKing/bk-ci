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
import { computed, defineComponent, ref, watch } from 'vue';
import { Dialog, Button } from 'bkui-vue';

import { useI18n } from 'vue-i18n';
import Uploader from './Uploader';
import { FileState } from '@/utils/vue-ts';
import { formatPath } from '@/utils';
export default defineComponent({
  props: {
    isShow: {
      type: Boolean,
    },
    projectId: {
      type: String,
    },
    repoName: {
      type: String,
    },
    fullPath: {
      type: String,
    },
  },
  emits: ['close', 'submit'],
  setup(props, ctx) {
    const { t } = useI18n();
    const uploadRef = ref();
    const uploading = ref(false);
    const title = computed(() => t('upload') + props.fullPath);
    const uploadUrl = computed(() => {
      const { projectId, repoName, fullPath } = props;
      const destFilePath = encodeURIComponent(`${formatPath(fullPath)}${fileState.value?.name ?? ''}`);
      return `/generic/${projectId}/${repoName}/${destFilePath}`;
    });
    const fileState = ref<FileState>();

    watch(() => props.isShow, () => {
      uploadRef.value?.clear();
    });
    function handleClose() {
      uploadRef.value?.clear();
      ctx.emit('close', false);
    }

    async function handleConfirm() {
      uploading.value = true;
      await uploadRef.value?.upload();
      uploading.value = false;
      ctx.emit('submit', props.fullPath, fileState.value);
    }

    function handleChange(newFile: FileState) {
      fileState.value = newFile;
    }

    return () => (
      <Dialog
        isShow={props.isShow}
        width={620}
        height={300}
        title={title.value}
        onClosed={handleClose}
      >
        {{
          default: () => (
            <Uploader
              ref={uploadRef}
              uploadUrl={uploadUrl.value}
              onChange={handleChange}
            />
          ),
          footer: () => (
            <>
              <Button onClick={handleClose}>{t('cancel')}</Button>
              <Button
                class="create-repo-dialog-cancel-btn"
                loading={uploading.value}
                disabled={uploading.value}
                onClick={handleConfirm}
                theme="primary"
              >
                {t('upload')}
              </Button>
            </>
          ),
        }}
      </Dialog>
    );
  },
});
