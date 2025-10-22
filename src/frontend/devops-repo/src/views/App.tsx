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
import { defineComponent, ref, computed } from 'vue';
import { RouterView, useRoute, useRouter } from 'vue-router';
import { Navigation, Menu, Breadcrumb, Button, Sideslider } from 'bkui-vue';
import { useI18n } from 'vue-i18n';

import Icon from '@/components/Icon';
import { useStore } from '@/store';
import OperationDialog from '@/components/OperationDialog';
import Header from '@/components/Header';
import { IS_CI_MODE } from '@/utils';
import { useCiMessage, useOperation, useRouteParams } from '@/hooks';
import ArtifactoryMetaData from '@/components/ArtifactoryMetaData';

export default defineComponent({
  setup() {
    const { t } = useI18n();
    const currentRoute = useRoute();
    const router = useRouter();
    const store = useStore();
    const repoParams = useRouteParams();
    const activeArtifact = computed(() => store.state.operationProps.artifact);
    const { closeOperation } = useOperation();
    const sideNavCollapsed = ref(false);
    const routes = [
      {
        label: 'repoList',
        name: 'repoList',
        icon: 'repolist',
      },
      {
        label: 'repoSearch',
        name: 'repoSearch',
        icon: 'reposearch',
      },
      {
        label: 'repoToken',
        name: 'repoToken',
        icon: 'repotoken',
      },
    ];
    if (IS_CI_MODE) {
      useCiMessage();
    }
    const matchedRoutes = computed(() => {
      const matchedRoutes = currentRoute.matched.filter(matched => matched.meta.breadLabel);
      return matchedRoutes.map((matched, index) => {
        const newQuery = matched.meta.query?.reduce((acc: Record<string, string>, item: string) => {
          if (currentRoute.query[item]) {
            acc[item] = currentRoute.query[item] as string;
          }
          return acc;
        }, {});
        return {
          label: matched.meta.breadLabel!,
          ...(matched.name === currentRoute.name || index === matchedRoutes.length - 1  ? {} : {
            to: {
              name: matched.name ?? 'repoList',
              params: currentRoute.params,
              query: newQuery ?? {},
            },
          }),
        };
      });
    });

    function handleCollapse() {
      sideNavCollapsed.value = !sideNavCollapsed.value;
    }

    function handleMenuClick(route: any) {
      router.push({
        name: route.name,
      });
    }

    function translateVar(varStr: string): string {
      const { params, query } = currentRoute;
      const ctx = { ...params, ...query };
      // @ts-ignore
      return varStr.replace(/\{(.*?)\}/g, (_: string, $1: string) => {
        if (typeof $1 !== 'string') return varStr;
        return $1 in ctx ? ctx[$1] : '';
      });
    }

    return () => (
      <>
        {!IS_CI_MODE && <Header></Header>}
        <Navigation
          class="bk-repo-content"
          sideTitle={t('repoList')}
          headerTitle={t('repoList')}
          showSideNavTitle={IS_CI_MODE}
          defaultOpen={!sideNavCollapsed.value}
          onToggle={handleCollapse}
        >
          {{
            'side-icon': () => <Icon size="16" name="repolist" />,
            header: () => (
              <Breadcrumb class="repo-root-bread-crumb-header bk-repo-bread-crumb">
                {{
                  prefix: () => (
                    <Button
                      text
                      theme="primary"
                      class="bread-crumb-back-icon"
                      onClick={router.back}
                    >
                      <Icon name="arrows-left-shape" />
                    </Button>
                  ),
                  default: () => (
                    matchedRoutes.value.map((item, index) => (
                      <Breadcrumb.Item
                        key={index}
                        class="repo-bread-crumb-item"
                        to={item.to}
                      >
                        {{
                          separator: () => index < matchedRoutes.value.length - 1 && <Icon name="angle-right" size={16} />,
                          default: () => t(translateVar(item.label)),
                        }}
                      </Breadcrumb.Item>
                    ))
                  ),
                }}
            </Breadcrumb>
            ),
            menu: () => (
              <Menu activeKey={currentRoute.meta.activeKey as string}>
                {routes.map(route => (
                  <Menu.Item key={route.name} onClick={() => handleMenuClick(route)}>
                    {{
                      default: () => t(route.label),
                      icon: () => (
                        <Icon name={route.icon} />
                      ),
                    }}
                  </Menu.Item>
                ))}
              </Menu>
            ),
            default: () => (
              <>
                <RouterView />
                <OperationDialog
                  projectId={repoParams.value.projectId}
                  repoName={repoParams.value.repoName}
                  onClose={closeOperation}
                />
                <Sideslider isShow={store.getters.showMetaSlider} onClosed={closeOperation} width={720}>
                {{
                  header: () => activeArtifact?.value?.displayName,
                  default: () => (
                    store.getters.showMetaSlider && <ArtifactoryMetaData
                      projectId={repoParams.value.projectId}
                      repoName={activeArtifact?.value?.repoName}
                      fullPath={activeArtifact?.value?.fullPath}
                      name={activeArtifact?.value?.displayName}
                      isFolder={activeArtifact?.value?.folder}
                    />
                  ),
                }}
              </Sideslider>
              </>
            ),
          }}
        </Navigation>
      </>
    );
  },
});
