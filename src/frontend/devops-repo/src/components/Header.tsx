/*
* Tencent is pleased to support the open source community by making
* 蓝鲸智云PaaS平台社区版 (BlueKing PaaS Community Edition) available.
*
* Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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
import router from '@/router';
import { useStore } from '@/store';
import { IS_CI_MODE } from '@/utils';
import { Dropdown, Select } from 'bkui-vue';
import { computed, defineComponent, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute } from 'vue-router';
import Icon from './Icon';

const { Option } = Select;
const { DropdownMenu, DropdownItem } = Dropdown;
export default defineComponent({
  setup() {
    const store = useStore();
    const route = useRoute();
    const selected = ref(route.params.projectId as string);
    const { t } = useI18n();
    const avatarBgs = ['success', 'warning', 'danger', 'primary'];
    const avatarCls = computed(() => {
      const username = store.state.currentUser?.username ?? 'unknow';
      const randomIndex = username.length % 4;
      return `bk-repo-user-avatar avatar-bg-${avatarBgs[randomIndex]}`;
    });
    const links = [
      {
        name: 'userCenter',
        to: 'userCenter',
      },
      {
        name: 'repoToken',
        to: 'repoToken',
      },
      {
        name: 'logout',
        callback: logout,
      },
    ];

    function handleSelect(val: string) {
      router.push({
        ...route,
        params: {
          ...route.params,
          projectId: val,
        },
      });
    }

    function handleLink(e: MouseEvent, link: any) {
      if (link.to) {
        router.push({
          name: link.to,
        });
      } else {
        link.callback?.();
      }
    }

    function logout() {
      if (IS_CI_MODE) {
        window.postMessage({
          action: 'toggleLoginDialog',
        }, '*');
        // TODO: 后续优化
        // @ts-ignore
        location.href = window.getLoginUrl();
      } else {
        document.cookie = 'bkrepo_ticket=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';

        router.replace({
          name: 'login',
        });
      }
    }

    return () => (
      <div class="bk-repo-top-header">
        <div class="bk-repo-top-header-left">
          <div class="bk-repo-logo">
            <Icon name="repolist" size={16} />
            <span class="bk-repo-title">{t('repoList')}</span>
          </div>
          <div class="bk-repo-project-selector">
            <Select
              modelValue={selected}
              onChange={handleSelect}
            >
              {
                store.state.projectList.map(project => (
                  <Option
                    key={project.id}
                    value={project.id}
                    label={project.displayName as string}
                  />
                ))
              }
            </Select>
          </div>
        </div>
        <Dropdown trigger="click"  placement="bottom-start">
              {{
                default: () => (
                  <div class="bk-repo-user-info">
                    <span class={avatarCls.value}>{store.state.currentUser?.username[0]}</span>
                    <span class="bk-repo-username">{store.state.currentUser?.username}</span>
                    <Icon name="angle-down" size={16}></Icon>
                  </div>
                ),
                content: () => (
                  <DropdownMenu>
                    {
                      links.map(link => (
                        <DropdownItem
                          key={link.name}
                          onClick={(e: MouseEvent) => handleLink(e, link)}
                        >
                            {t(link.name)}
                        </DropdownItem>
                      ))
                    }
                  </DropdownMenu>
                ),
              }}
            </Dropdown>
      </div>
    );
  },
});
