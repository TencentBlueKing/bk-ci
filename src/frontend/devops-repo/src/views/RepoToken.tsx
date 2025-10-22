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
import { defineComponent, onBeforeMount, ref } from 'vue';
import { Button, Loading, Table } from 'bkui-vue';
import { useI18n } from 'vue-i18n';
import { useRoute } from 'vue-router';

import Icon from '@/components/Icon';
import { useStore } from '@/store';
import { DELETE_USER_TOKEN, FETCH_USER_TOKEN } from '@/store/constants';
import { asyncAction, formatDate } from '@/utils';
import { Token } from '@/utils/vue-ts';
import TokenDialog from '@/components/TokenDialog';
import { useConfirm, useToken } from '@/hooks';

export default defineComponent({
  setup() {
    const { t } = useI18n();
    const store = useStore();
    const route = useRoute();
    const isLoading = ref(true);
    const { isAddTokenDialogShow, toggleAddTokenDialog } = useToken();
    const { renderConfirmDialog, showConfirm, onConfirm } = useConfirm();
    const tableData = ref([]);
    const columns = [
      {
        label: t('name'),
        field: 'name',
      },
      {
        label: t('createdDate'),
        field: 'createdDate',
      },
      {
        label: t('expires'),
        field: 'expiredDate',
      },
      {
        label: t('operation'),
        field: 'deleteConfirm',
        render: ({ cell }: any) => (
          <Button text onClick={cell}>
            <Icon name="delete" size="14" />
          </Button>
        ),
      },
    ];

    onBeforeMount(() => {
      getTokens();
    });

    async function getTokens() {
      isLoading.value = true;

      const tokenList = await store.dispatch(FETCH_USER_TOKEN, {
        username: store.state.currentUser?.username,
      });
      tableData.value = tokenList.map((token: Token) => ({
        ...token,
        createdDate: formatDate(token.createdAt),
        expiredDate: formatDate(token.expiredAt),
        deleteConfirm: () => {
          showConfirm(t('deleteTokenTitle', [token.name]));
          onConfirm(asyncAction(async () => {
            await store.dispatch(DELETE_USER_TOKEN, {
              username: store.state.currentUser?.username,
              name: token.name,
            });
            getTokens();
          }, `${token.name ?? ''}${t('deleteSuccess')}`));
        },
      }));
      isLoading.value = false;
    }

    function closeTokenDialog() {
      getTokens();
      toggleAddTokenDialog();
    }

    return () => (
      <main class="repo-token-main">
        <header>
          <Button theme='primary' onClick={toggleAddTokenDialog}>
            <Icon name="add" size={18} />
            {t('create')}
          </Button>
        </header>
        <Loading loading={isLoading.value}>
          <Table
            data={tableData.value}
            columns={columns}
          />
        </Loading>
        <TokenDialog
          isShow={isAddTokenDialogShow.value}
          projectId={route.params.projectId as string}
          onClose={closeTokenDialog}
        />
        {renderConfirmDialog?.()}
      </main>
    );
  },
});
