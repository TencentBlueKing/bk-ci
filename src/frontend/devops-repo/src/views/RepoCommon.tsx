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
import { computed, defineComponent } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { Button, Input, Loading, Select } from 'bkui-vue';

import { useConfirm, useEmptyGuide, useRepoInfo, useRouteParams, useSearchArtifact, useToken, useDomain } from '@/hooks';
import CodeBox from '@/components/CodeBox';
import Steps from '@/components/Steps';
import TokenDialog from '@/components/TokenDialog';
import InfiniteScroll from '@/components/InfiniteScroll';
import { Artifact } from '@/utils/vue-ts';
import Icon from '@/components/Icon';
import { SORT_PROPERTY } from '@/utils/conf';
import { DELETE_PACKAGE } from '@/store/constants';
import { asyncAction } from '@/utils';
import RepoHeader from '@/components/RepoHeader';
import PackageItem from '@/components/PackageItem';
import { useStore } from '@/store';

const { Option } = Select;
export default defineComponent({
  setup() {
    const store = useStore();
    const router = useRouter();
    const { t } = useI18n();
    const routeParams = useRouteParams();
    const { renderConfirmDialog, showConfirm, onConfirm } = useConfirm();
    const { isAddTokenDialogShow, toggleAddTokenDialog } = useToken();
    const repoInfo = useRepoInfo();
    const {
      isSearching,
      artifactList,
      searchArtifact,
      searchParams,
      pagination,
      updateRouteQuery,
      toggleDirection,
      handleCurrentChange,
    } = useSearchArtifact();
    const  objectSteps = computed(() => {
      if (!isSearching.value && artifactList.value.length === 0) {
        const emptyGuide = useEmptyGuide();
        const domain = useDomain();
        console.log(domain);
        return [
          {
            index: '01',
            desc: 'step',
            content: () => (
            <>
              <header class="empty-guide-item-title bold">{ t('token') }</header>
              <div class="empty-guide-subtitle">
                <Button
                  text
                  theme="primary"
                  onClick={toggleAddTokenDialog}
                >
                  {t('createToken') }
                </Button>
                <span class="token-subtitle">{t('tokenSubTitle')}</span>
                <RouterLink to={{ name: 'repoToken' }}>
                  <Button text theme='primary'>
                  <span>{t('token')}</span>
                  </Button>
                </RouterLink>
              </div>
            </>
            ),
          },
          ...(
            emptyGuide.map((item: any, index: number) => ({
              index: `0${index + 2}`,
              desc: 'step',
              content: () => (
              <>
                <header class="empty-guide-item-title bold">{ t(item.title) }</header>
                {
                  item.main.map((mainItem: any, index: number) => (
                    <>
                      <h3 class="empty-guide-subtitle">{`${index + 1}、${t(mainItem.subTitle)}`}</h3>
                      <CodeBox codeList={mainItem.codeList}></CodeBox>
                    </>
                  ))
                }
              </>
              ),
            }))
          ),
        ];
      }
      return [];
    });

    async function loadMore(done: Function) {
      const nextCurrent = pagination.current + 1;
      handleCurrentChange(nextCurrent);
      await searchArtifact();
      done();
    }

    function handleDelete(name: string) {
      showConfirm(t('deletePackageTitle', { name }));
      onConfirm(asyncAction(async () => {
        await store.dispatch(DELETE_PACKAGE, {
          ...routeParams.value,
          packageKey: name,
        });
        searchArtifact();
      }, `${name}${t('deleteSuccess')}`));
    }

    function goDetail(artifact: Artifact) {
      router.push({
        name: 'repoPackage',
        query: {
          repoName: routeParams.value.repoName,
          package: artifact.key,
          versionName: artifact.versionName,
          version: artifact.latest,
        },
      });
    }

    return () => (
      <Loading class="repo-common-main" loading={isSearching.value}>
        <RepoHeader
          showGuide={repoInfo.showGuide}
          repoType={repoInfo.type}
          repoName={repoInfo.name}
          repoDesc={repoInfo.desc}
        />
        {
          artifactList.value.length > 0 ? (
            <>
              <header class="bk-repo-search-header">
                <Input
                  v-model={searchParams.packageName}
                  class="search-input"
                  type="search"
                  onClear={updateRouteQuery}
                  onEnter={updateRouteQuery}
                  placeholder={t('artifactSearchPlaceholder')}
                />
                <div class="bk-repo-filter-area">
                  <Select multiple v-model={searchParams.property}>
                    {Object.entries(SORT_PROPERTY).map(([, value]) => (
                        <Option label={t(`${value}Sort`)} value={value} />
                    ))}
                  </Select>
                  <Button hoverTheme='primary' onClick={toggleDirection}>
                    <Icon size={16} name={`order-${searchParams.direction.toLowerCase()}`} />
                  </Button>
                </div>
              </header>
              <InfiniteScroll
                class="common-repo-list"
                onSrollEnd={loadMore}
                hasNext={pagination.hasNext}
              >
                  {
                    artifactList.value.map((item: Artifact) => (
                      <PackageItem
                        isPackage
                        readonly={!store.state.permission.delete}
                        pacakgeInfo={item}
                        onClick={goDetail}
                        onDelete={handleDelete}
                      />
                    ))
                  }
              </InfiniteScroll>
            </>
          ) : !isSearching.value && (
            <>
              <header class="empty-guide-header">
                <h2 class="bold">{t('noArtifact')}</h2>
                <span class="desc">{t('addArtifactGuideTips')}</span>
              </header>
              <section class="empty-guide-content">
                <h3 class="bold">{t('quickSetting')}</h3>
                <Steps steps={objectSteps.value}></Steps>
              </section>
            </>
          )
        }
        <TokenDialog
          isShow={isAddTokenDialogShow.value}
          projectId={routeParams.value.projectId}
          onClose={toggleAddTokenDialog}
        />
        {
          renderConfirmDialog()
        }
      </Loading>
    );
  },
});
