<script setup lang="ts">
import Logo from '@/components/Logo';
import http from '@/http/api';
import { Message, InfoBox } from 'bkui-vue';
import { onMounted, computed, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import {
  useRoute,
} from 'vue-router';
const { t } = useI18n();
const route = useRoute();

const isLoading = ref(true);
const filterList = ref([]);
const serviceGroup = ref([]);
const installList = ref([]);
const serviceStatusMap = {
  INIT: t('初始化'),
  COMMITTING: t('提交中'),
  BUILDING: t('构建中'),
  BUILD_FAIL: t('构建失败'),
  TESTING: t('测试中'),
  EDIT: t('填写相关信息中'),
  AUDITING: t('审核中'),
  AUDIT_REJECT: t('审核驳回'),
  RELEASE_DEPLOYING: t('正式发布部署中'),
  RELEASE_DEPLOY_FAIL: t('正式发布部署失败'),
  RELEASED: t('已发布'),
  GROUNDING_SUSPENSION: t('上架中止'),
  UNDERCARRIAGING: t('下架中'),
  UNDERCARRIAGED: t('已下架'),
};

const filterInstallList = computed(() => installList.value.filter((install: any) => filterList.value.length <= 0
    || filterList.value.some(id => install.itemIds.includes(id))));
const projectCode = computed(() => route.params.projectCode);

const initData = () => {
  Promise.all([
    http.requestInstalledServiceList(projectCode.value),
    http.requestServiceItemList(),
  ]).then(([resInstallList, resServiceList]) => {
    const itemIds: any[] = [];
    resInstallList.forEach((x: { itemIds: any; }) => itemIds.push(...x.itemIds));
    serviceGroup.value = (resServiceList || []).map((service: { childItem: any[]; }) => {
      service.childItem = service.childItem.filter((item: { id: any; }) => itemIds.includes(item.id));
      return service;
    });
    installList.value = resInstallList || [];
  })
    .catch((err) => {
      Message({ message: err.message || err, theme: 'error' });
    })
    .finally(() => (isLoading.value = false));
};
const uninstall = (row: { serviceCode: any; serviceName: unknown; }) => {
  const postData = {
    projectCode: projectCode.value,
    serviceCode: row.serviceCode,
  };
  const onConfirm = () => {
    isLoading.value = true;
    http.uninstallService(postData).then(() => {
      initData();
    })
      .finally(() => (isLoading.value = false));
  };

  InfoBox({
    type: 'warning',
    title: t('确定卸载', [row.serviceName]),
    contentAlign: 'center',
    headerAlign: 'center',
    footerAlign: 'center',
    onConfirm,
  });
};

const getIcon = (id: string | number) => {
  const serviceObject = window.serviceObject || {};
  const serviceMap = serviceObject.serviceMap || {};
  const keys = Object.keys(serviceMap);
  let link = '';
  keys.forEach((key) => {
    const cur = serviceMap[key];
    if (+cur.id === +id) link = cur.link_new;
  });
  return link.replace(/\/?(devops\/)?(\w+)\S*$/, '$2');
};

const timeFormatter = (cellValue: string | number) => {
  const date = new Date(+cellValue);
  const year = date.toISOString().slice(0, 10);
  const time = date.toTimeString().split(' ')[0];
  return `${year} ${time}`;
};

const statusFormatter = (cellValue: string | number) => serviceStatusMap[cellValue];

onMounted(() => {
  initData();
});
</script>

<template>
  <bk-loading class="content-wrapper" :loading="isLoading">
    <div class="service-home">
      <header class="service-filter">
        <bk-button
          class="filter-button"
          :disabled="filterList.length <= 0"
          @click="filterList = []"
        >{{ t('全部微扩展') }}（{{installList.length}}）
        </bk-button>
        <bk-checkbox-group v-model="filterList">
          <ul class="fliter-list">
            <li v-for="(group, index) in serviceGroup" :key="index" class="filter-item">
              <template v-if="group.childItem.length">
                <Logo
                  class="service-logo"
                  :name="getIcon(group.extServiceItem.id)"
                  size="18"
                />
                <section class="filter-select">
                  <bk-checkbox
                    v-for="service in group.childItem"
                    :key="service.id"
                    :label="service.id">
                    {{ service.name }}
                  </bk-checkbox>
                </section>
              </template>
            </li>
          </ul>
        </bk-checkbox-group>
      </header>
      <main class="service-table">
        <bk-table
          :empty-text="t('暂时没有微扩展')"
          :data="filterInstallList"
        >
          <bk-table-column :label="t('微扩展名称')" prop="serviceName"></bk-table-column>
          <bk-table-column :label="t('发布者')" prop="publisher"></bk-table-column>
          <bk-table-column :label="t('版本')" prop="version"></bk-table-column>
          <bk-table-column :label="t('状态')" prop="serviceStatus">
            <template #default="{ data }">
              {{ statusFormatter(data.serviceStatus) }}
            </template>
          </bk-table-column>
          <bk-table-column :label="t('操作人')" prop="publisher"></bk-table-column>
          <bk-table-column :label="t('操作时间')" prop="publishTime" width="180">
            <template #default="{ data }">
              {{ timeFormatter(data.publishTime || '') }}
            </template>
          </bk-table-column>
          <bk-table-column :label="t('操作')">
            <template #default="{ data }">
              <span v-bk-tooltips="{ content: t('微扩展初始化项目，不能卸载'), disabled: data.isUninstall }">
                <bk-button text theme="primary" @click="uninstall(data)" :disabled="!data.isUninstall">
                  {{t('卸载')}}
                </bk-button>
              </span>
            </template>
          </bk-table-column>
        </bk-table>
      </main>
    </div>
  </bk-loading>
</template>

<style lang="postcss" scoped>
  .content-wrapper {
    height: 100%;
    overflow: auto;
    &::-webkit-scrollbar-thumb {
      background-color: #c4c6cc !important;
      border-radius: 5px !important;
      &:hover {
        background-color: #979ba5 !important;
      }
    }
    &::-webkit-scrollbar {
      width: 8px !important;
      height: 8px !important;
    }
  }
  .service-home {
    display: flex;
    flex-direction: column;
    &::-webkit-scrollbar-thumb {
      background-color: #c4c6cc !important;
      border-radius: 5px !important;
      &:hover {
        background-color: #979ba5 !important;
      }
    }
    &::-webkit-scrollbar {
      width: 8px !important;
      height: 8px !important;
    }
  }
  .service-filter {
    width: 94%;
    margin: 32px auto 16px;
    background: #ffffff;
    .filter-button {
      margin: 10px 32px 0;
    }
    .fliter-list {
      padding-bottom: 20px;
      .filter-item {
        display: flex;
        line-height: 32px;
        .item-name {
          display: inline-block;
          width: 100px;
          line-height: 18px;
          font-size: 14px;
        }
        .filter-select {
          margin-left: 20px;
          ::v-deep  .bk-form-checkbox {
            margin: 0 18px 20px 0;
            .bk-checkbox-text {
              width: 80px;
            }
          }
        }
      }
    }
  }
  .service-table {
    margin: 0 auto;
    flex: 1;
    width: 94%;
    padding: 15px 32px 24px;
    margin-bottom: 32px;
    background: #ffffff;
  }

  .btn {
    color: #3A84FF;
  }
</style>
