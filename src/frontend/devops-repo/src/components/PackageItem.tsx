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
import { useArtifactOperation, useOperation } from '@/hooks';
import { formatDate, formatSize, getIconNameByFileName, getIconNameByRepoType } from '@/utils';
import { Artifact, Operation } from '@/utils/vue-ts';
import { Button, Tag } from 'bkui-vue';
import { computed, defineComponent, PropType } from 'vue';
import { useI18n } from 'vue-i18n';
import Icon from './Icon';
import OperationMenu from './OperationMenu';

/* <template v-if="cardData.type">
<div class="card-metadata" :title="`最新版本：${cardData.latest}`"></div>
<div class="card-metadata" :title="`最后修改：${formatDate(cardData.lastModifiedDate)}`"></div>
<div class="card-metadata" :title="`版本数：${cardData.versions}`"></div>
<div class="card-metadata" :title="`下载统计：${cardData.downloads}`"></div>
</template>
<template v-else>
<div class="card-metadata" :title="`所属仓库：${cardData.repoName}`"></div>
<div class="card-metadata" :title="`文件大小：${convertFileSize(cardData.size)}`"></div>
<div class="card-metadata" :title="`最后修改：${formatDate(cardData.lastModifiedDate)}`"></div>
</template> */

export default defineComponent({
  props: {
    pacakgeInfo: {
      type: Object as PropType<Artifact>,
      required: true,
    },
    isPackage: {
      type: Boolean,
    },
    readonly: {
      type: Boolean,
      default: false,
    },
  },
  emits: ['click', 'delete'],
  setup(props, ctx) {
    const { t } = useI18n();
    const icon = computed(() => (props.isPackage
      ? getIconNameByRepoType(props.pacakgeInfo.type)
      : getIconNameByFileName(props.pacakgeInfo.name)
    ));
    const { activeOperation } = useOperation();
    const operations = useArtifactOperation(props.pacakgeInfo, true);

    const metas = computed(() => (props.isPackage ? [
      t('latestVersion', [props.pacakgeInfo.latest]),
      t('lastModified', [formatDate(props.pacakgeInfo.lastModifiedDate)]),
      t('versionCount', [props.pacakgeInfo.versions]),
      t('downloadCount', [props.pacakgeInfo.downloads]),
    ] : [
      t('parentRepo', [props.pacakgeInfo.repoName]),
      t('fileSize', [formatSize(props.pacakgeInfo.size)]),
      t('lastModified', [formatDate(props.pacakgeInfo.lastModifiedDate)]),
    ]));

    function handleDelete(e: MouseEvent) {
      e.stopPropagation();
      ctx.emit('delete', props.pacakgeInfo.name);
    }

    function handleClick() {
      ctx.emit('click', props.pacakgeInfo);
    }

    return () => (
      <div
        key={props.pacakgeInfo.name}
        class="bk-repo-package-item"
        onClick={handleClick}
      >
        <span class="package-item-type-icon">
          <Icon name={icon.value} size="36"></Icon>
        </span>
        <div class="package-item-info-area">
          <h3 class="package-item-name bold">
            <span>{props.pacakgeInfo.name}</span>
            {
              props.pacakgeInfo.isMaven && (
                <Tag type='stroke'>
                  { props.pacakgeInfo.key.replace(/^.*\/\/(.+):.*$/, '$1') }
                </Tag>
              )
            }
          </h3>
          <p class="package-item-description">
            {props.pacakgeInfo.description}
          </p>
          <p class="package-item-detail-info">
            {
              metas.value.map((meta: string) => (
                <span class="text-overflow">
                  {meta}
                </span>
              ))
            }
          </p>
        </div>
        <div class="package-item-operation">
          {
            !props.readonly && (
              <Button text class="package-item-delete-btn" onClick={handleDelete}>
                <Icon name="delete" size={16}></Icon>
              </Button>
            )
          }
          {
            !props.isPackage && (
              <OperationMenu
                placement='bottom'
                operationList={operations}
                handleOperation={(_: any, operation: Operation) => activeOperation(operation, props.pacakgeInfo)}
              />
            )
          }
        </div>
      </div>
    );
  },
});
