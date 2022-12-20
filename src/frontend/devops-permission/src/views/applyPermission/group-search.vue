<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import { Error } from 'bkui-vue/lib/icon'
import GroupDeatil from './group-detail.vue'
import {
  ref,
  computed,
} from 'vue';
const { t } = useI18n();
const showDetail = ref(false);
const data = ref([
  {},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},
]);
const filter = ref([]);

const searchList = computed(() => {
  const datas = [
    {
      name: t('用户组名'),
      id: 'groupName',
      multiple: false,
    },
    {
      name: t('描述'),
      id: 'desc',
      
    },
    {
      name: t('操作'),
      id: 'option',
      multiple: false,
      children: [
        { name: 'test1', id: '1'},
        { name: 'test2', id: '2'},
        { name: 'test3', id: '3'},
        { name: 'test4', id: '4'},
      ],
    },
    {
      name: t('资源实例'),
      id: 'region',
      multiple: true,
      children: [
        { name: 'test1', id: '1'},
        { name: 'test2', id: '2'},
        { name: 'test3', id: '3'},
        { name: 'test4', id: '4'},
      ],
    },
  ];
  return datas.filter((data) => {
    return !filter.value.find(val => val.id === data.id);
  });
});

const pagination = ref({
  count: 0,
  limit: 10,
  current: 1,
});

const handlePageChange = (page) => {
  pagination.value.current = page;
};

const handleLimitChange = (limit) => {
  pagination.value.limit = limit;
};

const handleClickMenu = (item: any) => {
  console.log(item)
}

const hiddenDetail = (payload) => {
  showDetail.value = payload;
};
</script>

<template>
  <article class="group-search">
    {{ filter }}
    <bk-search-select
      class="group-search-filter"
      clearable
      :conditions="[]"
      :show-condition="true"
      :show-popover-tag-change="true"
      :data="searchList"
      v-model="filter"
    >
    <!-- <template #menu={item}>
      <div style="width: 100%; height: 100%; line-height: 32px;" @click.stop="handleClickMenu(item)">
        {{ item.name }}
        <error />
      </div>
    </template> -->
    </bk-search-select>
      <bk-table
        class="group-table"
        :data="data"
        :pagination="pagination"
        :border="['row', 'outer']"
        @page-value-change="handlePageChange"
        @page-limit-change="handleLimitChange"
      >
      <bk-table-column type="selection" width="60"></bk-table-column>
      <bk-table-column :label="t('用户组名')"></bk-table-column>
      <bk-table-column :label="t('描述')"></bk-table-column>
    </bk-table> 
  </article>
  <group-deatil :is-show="showDetail" @hidden-detail="hiddenDetail" />
</template>

<style lang="postcss" scoped>
  .group-search-filter {
    width: 750px;
  }
  .group-table {
    width: 750px !important;
    margin-top: 16px;
  }
  :deep(.bk-pagination-total) {
    padding-left: 15px;
  }
</style>
