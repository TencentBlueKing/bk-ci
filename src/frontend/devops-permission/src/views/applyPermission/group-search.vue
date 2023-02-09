<script setup lang="ts">
import { h } from 'vue';
import { useI18n } from 'vue-i18n';
import http from '@/http/api';
import { Error } from 'bkui-vue/lib/icon'
import GroupDeatil from './group-detail.vue'
import SearchSelect from './search-select'
import {
  ref,
  watch,
  onMounted,
  computed,
} from 'vue';

const props = defineProps({
  groupList: Array,
  projectCode: String,
});

const { t } = useI18n();
const showDetail = ref(false);
const tableRef = ref();
const resourcesTypeList = ref([]);
const groupInfo = ref([]);
const selectGroupList = ref([]);
const filter = ref([]);
const isLoading = ref(false);
const isDetailLoading = ref(false);
const userGroupList = ref([]);
const pagination = ref({
  count: 0,
  limit: 10,
  current: 1,
});

const emits = defineEmits(['handle-change-select-group']);

// 表格已选中用户组状态
const handleSetTableSelected = () => {
  
};

watch(() => selectGroupList.value, (val) => {
  handleChangeSelectGroup(val)
}, {
  deep: true,
});

watch(() => props.projectCode, () => {
  if (props.projectCode) {
    fetchGroupList([]);
  };
})

watch(() => props.groupList, () => {
  selectGroupList.value = props.groupList;
  if (tableRef.value) {
    const data = tableRef.value.getSelection();
    const selectIdMap = selectGroupList.value.map(i => i.id);
    const list = data.filter(select => !selectIdMap.includes(select.id))
    list.forEach(i => tableRef.value.toggleRowSelection(i, false))
  }
}, {
  immediate: true,
  deep: true,
});

const handleChangeSelectGroup = (values) => {
  emits('handle-change-select-group', values);
};

const searchList = computed(() => {
  const datas = [
    {
      name: t('ID'),
      isDefaultOption: true,
      id: 'groupId',
      multiple: false,
    },
    {
      name: t('用户组名'),
      id: 'name',
      multiple: false,
    },
    {
      name: t('描述'),
      id: 'description',
    },
    {
      name: t('操作'),
      id: 'actionId',
      multiple: false,
      children: [],
    },
    {
      name: t('资源实例'),
      id: 'resourceCode',
      multiple: false,
      children: [],
    },
  ];
  return datas.filter((data) => {
    return !filter.value.find(val => val.id === data.id);
  });
});

const handlePageChange = (page) => {
  pagination.value.current = page;
};

const handleLimitChange = (limit) => {
  pagination.value.limit = limit;
};

const handleShowGroupDetail = async (data) => {
  isDetailLoading.value = true;
  showDetail.value = true;
  groupInfo.value = data;
}

const hiddenDetail = (payload) => {
  showDetail.value = payload;
};

const initTable = () => {
  tableRef.value?.clearSelection();
};

const fetchGroupList = async (payload) => {
  if (!props.projectCode) return;
  const params = {
    page: pagination.value.current,
    pageSize: pagination.value.limit,
  };
  payload.forEach(i => {
    if (i.id === 'actionId') {
      const values = i.values;
      params[i.id] = values.map(i => i.actionId).join('');
      params['resourceType'] = values[0].resourceType;
    } else if (i.id === 'resourceCode') {
      const values = i.values;
      params[i.id] = values.map(i => i.resourceCode).join('');
      params['resourceType'] = values[0].resourceType;
    } else {
      params[i.id] = i.values.join();
    }
  });
  params['projectId'] = props.projectCode;
  isLoading.value = true;
  await http.getUserGroupList(params).then(res => {
    pagination.value.count = res.count;
    userGroupList.value = res.results;
  }).catch(() => {
    isLoading.value = false;
    return [];
  }).finally(() => {
    isLoading.value = false;
  })
};

const handleSelectGroup = ({ row }) => {
  const index = selectGroupList.value.findIndex(i => i.id === row.id)
  if (index === -1) {
    selectGroupList.value.push(row);
  } else {
    selectGroupList.value.splice(index, 1);
  }
};

const handleSelectAllGroup = (selection) => {
  if (selection.checked) {
    selectGroupList.value = [...userGroupList.value];
  } else {
    selectGroupList.value = [];
  }
};

onMounted(() => {
  
});
</script>

<template>
  <article class="group-search">
    <search-select
      class="group-search-filter"
      v-model="filter"
      :search-list="searchList"
      :project-code="projectCode"
      @change="fetchGroupList">
    </search-select>
    <bk-loading
      class="group-table"
      :loading="isLoading">
      <bk-table
        ref="tableRef"
        :data="userGroupList"
        :pagination="pagination"
        :border="['row', 'outer']"
        @page-value-change="handlePageChange"
        @page-limit-change="handleLimitChange"
        @select="handleSelectGroup"
        @select-all="handleSelectAllGroup"
      >
        <bk-table-column type="selection" width="60"></bk-table-column>
        <bk-table-column :label="t('用户组名')" prop="name" show-overflow-tooltip>
          <template #default="{ data }">
            <span class="group-name" @click="handleShowGroupDetail(data)">{{ data.name }}</span>
          </template>
        </bk-table-column>
        <bk-table-column :label="t('描述')" prop="description" show-overflow-tooltip></bk-table-column>
      </bk-table> 
    </bk-loading>
  </article>
  <group-deatil
    :is-show="showDetail"
    :is-detail-loading="isDetailLoading"
    :group-info="groupInfo"
    @hidden-detail="hiddenDetail" />
</template>

<style lang="postcss" scoped>
  .group-search-filter {
    width: 750px;
  }
  .group-table {
    width: 750px !important;
    margin-top: 16px;
  }
  .group-name {
    color: #3A84FF;
    cursor: pointer;
  }
  :deep(.bk-pagination-total) {
    padding-left: 15px;
  }
  :deep(.bordered-outer) {
    border: 1px solid #dcdee5;
  }
</style>
