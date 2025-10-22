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
import { computed, defineComponent, reactive, ref } from 'vue';
import { Form, Input, Progress, Radio } from 'bkui-vue';
import { classes, formatPath, formatSize, getFileExt, getIconNameByExt } from '@/utils';
import { useI18n } from 'vue-i18n';
import Icon from './Icon';
import { FileState } from '@/utils/vue-ts';
import request from '@/utils/request';

const { FormItem } = Form;
const RadioGroup = Radio.Group;
export default defineComponent({
  props: {
    multiple: {
      type: Boolean,
    },
    repoPath: {
      type: String,
      default: '',
    },
    uploadUrl: {
      type: String,
      required: true,
    },
  },
  emits: ['change', 'progress', 'uploaded'],
  setup(props, ctx) {
    const { t } = useI18n();
    const inputRef = ref();
    const percent = ref(0);
    const isUploading = ref(false);
    let controller: AbortController;
    const initState: FileState = {
      file: null,
      name: '',
      size: 0,
      type: '',
      ext: '',
      overwrite: false,
    };
    const fileState = reactive<FileState>({
      ...initState,
    });
    const uploadFileUrl = computed(() => {
      const destFilePath = encodeURIComponent(`${formatPath(props.repoPath)}${fileState?.name ?? ''}`);
      return `${props.uploadUrl}/${destFilePath}`;
    });
    const extIconName = computed(() => getIconNameByExt(fileState.ext));
    const uploadProgressCls = computed(() => classes({
      visible: isUploading.value,
    }, 'bk-repo-upload-progress'));

    function handleSelectFile(evt: Event) {
      const { files } = evt.target as HTMLInputElement;
      if (!files?.length) return;
      const file = files[0];
      Object.assign(fileState, {
        file,
        name: file.name,
        size: formatSize(file.size),
        type: file.type,
        ext: getFileExt(file.name),
      });
      ctx.emit('change', fileState, evt);
    }

    function clear() {
      Object.assign(fileState, initState);
      percent.value = 0;
      if (inputRef.value) {
        inputRef.value.value = '';
      }
      abortUpload();
    }

    async function upload() {
      try {
        if (isUploading.value) return;
        isUploading.value = true;
        controller = new AbortController();
        if (!fileState.overwrite) {
          const res = await request.head(uploadFileUrl.value);
          if (res.status === 200) {
            throw new Error(t('fileExist', { name: fileState.name }));
          }
        }
        const formData = new FormData();

        formData.append('file', fileState.file!);
        const res = await request.put(uploadFileUrl.value, formData, {
          onUploadProgress: (progressEvt: ProgressEvent) => {
            percent.value = Math.round((progressEvt.loaded * 100) / progressEvt.total);
            ctx.emit('progress', percent.value, progressEvt);
          },
          headers: {
            'Content-Type': fileState.type || 'application/octet-stream',
            'X-BKREPO-OVERWRITE': fileState.overwrite,
            'X-BKREPO-EXPIRES': fileState.expires ?? 0,
          },
          signal: controller.signal,
        });
        ctx.emit('uploaded', res);
      } catch (error) {
        throw error;
      } finally {
        isUploading.value = false;
      }
    }

    function abortUpload() {
      controller?.abort();
    }

    ctx.expose({
      clear,
      upload,
      isUploading,
      abortUpload,
    });

    return () => (
       <div class="bk-repo-uploader">
        {
          fileState.file !== null ? (
            <>
              <span
                class="bk-repo-uploader-clear-icon"
                onClick={clear}
              >
                <Icon name="close" size={24} />
              </span>
              <aside class="bk-repo-uploader-icon">
                <Icon name={extIconName.value} size={48} />
                <span>{fileState.size}</span>
              </aside>
              <div class="bk-repo-uploader-info">
                <Form labelWidth={88}>
                  <FormItem
                    class="bk-repo-file-info-item"
                    label={t('fileName')}
                    required
                  >
                    <Input v-model={fileState.name} />
                  </FormItem>
                  <FormItem
                    class="bk-repo-file-info-item"
                    label={t('overwrite')}
                  >
                    <RadioGroup v-model={fileState.overwrite}>
                      <Radio label={true}>{t('allow')}</Radio>
                      <Radio label={false}>{t('notAllow')}</Radio>
                    </RadioGroup>
                  </FormItem>
                </Form>
                <Progress class={uploadProgressCls.value} percent={percent.value} showText={false} />
              </div>
            </>
          ) : (
            <>
              <Icon name="upload" size="20" />
              <input
                class="bk-repo-uploader-input"
                ref={inputRef}
                type="file"
                multiple={props.multiple}
                onChange={handleSelectFile}
              />
              <span class="bk-repo-uploader-placeholder">{t('uploadPlaceholder')}</span>
            </>
          )
        }
       </div>
    );
  },
});
