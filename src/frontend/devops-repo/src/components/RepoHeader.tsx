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
import { Button, Tag } from 'bkui-vue';
import { defineComponent } from 'vue';
import { useI18n } from 'vue-i18n';

import { GENERIC_REPO } from '@/utils/conf';
import Icon from './Icon';

export default defineComponent({
  props: {
    showGuide: {
      type: Boolean,
      default: false,
    },
    repoType: {
      type: String,
      required: true,
    },
    repoName: {
      type: String,
      required: true,
    },
    repoDesc: {
      type: String,
      default: '--',
    },
  },
  setup(props) {
    const { t } = useI18n();
    return () => (
      <header class="repo-header">
        <Icon class="repo-header-icon" size={70} name={props.repoType} />
        <div class="repo-header-content">
          <h2 class="repo-title bold text-overflow">
            {props.repoName}
          </h2>
          <Tag>
            <span class="repo-description text-overflow">
              {props.repoDesc}
            </span>
          </Tag>
        </div>
        {
            props.showGuide && props.repoType !== GENERIC_REPO && (
              <Button
                text
                theme="primary"
              >
                <Icon name="hand-guide" size={16} />
                {t('guide')}
              </Button>
            )
          }
      </header>
    );
  },
});


