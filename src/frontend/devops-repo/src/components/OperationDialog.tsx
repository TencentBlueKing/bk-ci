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
import { defineComponent, computed, ref, reactive, toRefs } from 'vue';
import { Dialog, Button } from 'bkui-vue';

import { useI18n } from 'vue-i18n';
import { asyncAction, formatPath } from '@/utils';
import { OperateName } from '@/utils/vue-ts';
import RenameForm from './RenameForm';
import ShareArtifactoryForm from './ShareArtifactoryForm';
import RepoTreeForm from './RepoTreeForm';
import Uploader from './Uploader';
import { useStore } from '@/store';
import UpgradePackageForm from './UpgradePackageForm';
import { useConfirm } from '@/hooks';
import SecScanForm from './SecScanForm';
export default defineComponent({
  props: {
    projectId: {
      type: String,
    },
    repoName: {
      type: String,
    },
  },
  emits: ['close', 'done'],
  setup(props, ctx) {
    const { t } = useI18n();
    const uploadRef = ref();
    const loading = ref(false);
    const formValue = ref();

    const dialogSize = reactive({
      width: 360,
      height: 222,
    });
    const store = useStore();
    const isShow = ref(false);
    const { renderConfirmDialog, showConfirm, onConfirm } = useConfirm();
    const { artifact, filesCount, operation, done } = toRefs(store.state.operationProps);

    const conf = computed(() => {
      let isCreateFolder = false;
      let isMove = false;
      let title = '';
      let component;
      let value: string | undefined;
      switch (operation?.value?.id) {
        case OperateName.CREATE_FOLDER:
          isCreateFolder = true;
        case OperateName.RENAME:
          isShow.value = true;
          value = isCreateFolder ? formatPath(artifact?.value?.fullPath) : artifact?.value?.displayName;
          title = `${t(isCreateFolder ? 'createFolder' : 'rename')}(${value ?? ''})`;
          Object.assign(dialogSize, {
            width: 450,
            height: 260,
          });
          component = () => (
            <RenameForm
              value={value}
              isCreateFolder={isCreateFolder}
              onChange={handleChange}
            />
          );
          break;
        case OperateName.SHARE:
          isShow.value = true;
          title = `${t('share')}(${artifact?.value?.fullPath})`;
          Object.assign(dialogSize, {
            width: 520,
            height: 380,
          });
          component = () => (
              <ShareArtifactoryForm
                fullPath={artifact?.value?.fullPath}
                onChange={handleChange}
              />
          );
          break;
        case OperateName.UPLOAD: {
          isShow.value = true;
          title = `${t('upload')}(${artifact?.value?.fullPath})`;
          const uploadUrl = `/generic/${props.projectId}/${props.repoName}`;
          Object.assign(dialogSize, {
            width: 520,
            height: 300,
          });
          component = () => (
                <Uploader
                  ref={uploadRef}
                  uploadUrl={uploadUrl}
                  repoPath={artifact?.value?.fullPath}
                  onChange={handleChange}
                />
          );
          break;
        }
        case OperateName.MOVE:
          isMove = true;
          isShow.value = true;
        case OperateName.COPY:
          title = `${t(isMove ? '' : 'copy')}(${artifact?.value?.fullPath})`;
          Object.assign(dialogSize, {
            width: 520,
            height: 500,
          });
          component = () => (
            <RepoTreeForm
              onChange={handleChange}
            />
          );
          break;
        case OperateName.UPGRADE:
          isShow.value = true;
          title = t('artifactUpgrade');
          Object.assign(dialogSize, {
            width: 400,
            height: 260,
          });
          component = () => (
            <UpgradePackageForm
              onChange={handleChange}
            />
          );
          break;
        case OperateName.SECSCAN:
          isShow.value = true;
          title = t('scanTypeTitle');
          Object.assign(dialogSize, {
            width: 500,
            height: 360,
          });
          component = () => (
              <SecScanForm
                onChange={handleChange}
              />
          );
          break;
        case OperateName.DELETE: {
          const deleteArtifactDesc = artifact?.value?.folder ? t('filesCountTips', [filesCount?.value]) : undefined;
          onConfirm(handleSubmit);
          showConfirm(operation.value.confirmMessage!, deleteArtifactDesc);
          break;
        }
        default:
          if (operation?.value?.callback) {
            operation.value.callback?.(formValue.value);
            handleClose();
          }
      }

      return {
        title,
        component,
      };
    });

    function handleChange(value: any) {
      formValue.value = value;
    }

    function handleClose() {
      isShow.value = false;
      ctx.emit('close', false);
    }

    async function handleSubmit() {
      const action = asyncAction(async () => {
        if (uploadRef.value) {
          await uploadRef.value.upload();
        } else {
          console.log(formValue.value);
          await operation?.value?.callback?.(formValue.value);
        }
        await done?.value?.();
        ctx.emit('done');
      }, t(operation?.value?.message ?? '', formValue.value));
      loading.value = true;
      await action();
      loading.value = false;
      handleClose();
    }

    return () => (
      <>
        {renderConfirmDialog()}
        <Dialog
          class="repo-operation-dialog"
          isShow={isShow.value}
          {...dialogSize}
          title={conf.value?.title}
          onClosed={handleClose}
        >
          {{
            default: () => conf.value?.component?.(),
            footer: () => (
              <>
                <Button onClick={handleClose}>{t('cancel')}</Button>
                <Button
                  class="create-repo-dialog-cancel-btn"
                  theme="primary"
                  disabled={loading.value}
                  loading={loading.value}
                  onClick={handleSubmit}
                >
                  {t('confirm')}
                </Button>
              </>
            ),
          }}
        </Dialog>
      </>
    );
  },
});
