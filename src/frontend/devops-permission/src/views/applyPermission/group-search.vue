<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import http from '@/http/api';
import { Error } from 'bkui-vue/lib/icon'
import GroupDeatil from './group-detail.vue'
import SearchSelect from './search-select'
import {
  h,
  ref,
  watch,
  onMounted,
  computed,
} from 'vue';
import { useRoute } from 'vue-router';
import BkCheckbox from 'bkui-vue/lib/checkbox';

const route = useRoute();

const props = defineProps({
  groupList: Array,
  projectCode: String,
});

const { t } = useI18n();
const showDetail = ref(false);
const tableRef = ref();
const resourcesTypeList = ref([]);
const groupInfo = ref([]);
const selections = ref([]);
const filter = ref([]);
const isLoading = ref(false);
const isDetailLoading = ref(false);
const userGroupList = ref([]);
const pagination = ref({
  count: 0,
  limit: 10,
  current: 1,
});
const isRowChecked = ref(false);
const indeterminate = ref(false);
const isSelectedAll = ref(false);

const searchList = computed(() => {
  const datas = [
    {
      name: t('资源实例'),
      id: 'resourceCode',
      multiple: false,
      children: [],
    },
    {
      name: t('用户组名'),
      isDefaultOption: true,
      id: 'name',
      multiple: false,
    },
    {
      name: t('操作'),
      id: 'actionId',
      multiple: false,
      children: [],
    },
    {
      name: t('描述'),
      id: 'description',
    },
    {
      name: 'ID',
      id: 'groupId',
      multiple: false,
    },
  ];
  return datas.filter((data) => {
    return !filter.value.find(val => val.id === data.id);
  });
});

const emits = defineEmits(['handle-change-select-group']);

const handleChangeSelectGroup = (values) => {
  emits('handle-change-select-group', values);
};

// 可选择的用户组 joined -> flase
const optionGroupList = computed(() => userGroupList.value.filter(i => !i.joined));

watch(() => props.projectCode, () => {
  if (props.projectCode && !route.query.resourceType) {
    fetchGroupList();
  };
})

watch(() => selections.value, () => {
  checkSelectedAll();
  checkIndeterminate();
  handleChangeSelectGroup(selections.value);
}, {
  deep: true,
});

watch(() => props.groupList, () => {
  selections.value = props.groupList;
}, {
  immediate: true,
  deep: true,
});

watch(() => userGroupList.value, () => {
  const { groupId } = route?.query;
  if (groupId && filter.value.length) {
    const group = userGroupList.value.find(group => String(group.id) === groupId);
    group && selections.value.push(group);
  }
  checkSelectedAll();
  checkIndeterminate();
});

const handlePageChange = (page) => {
  pagination.value.current = page;
  fetchGroupList(filter.value);
};

const handleLimitChange = (limit) => {
  pagination.value.limit = limit;
  fetchGroupList(filter.value);
};

const handleShowGroupDetail = async (data) => {
  isDetailLoading.value = true;
  showDetail.value = true;
  groupInfo.value = data;
};

const hiddenDetail = (payload) => {
  showDetail.value = payload;
};

const initTable = () => {
  tableRef.value?.clearSelection();
};

const handleChangeSearch = (data) => {
  filter.value = data;
  fetchGroupList(data);
};

const fetchGroupList = async (payload = []) => {
  if (!props.projectCode) return;
  const params = {
    page: pagination.value.current,
    pageSize: pagination.value.limit,
  };
  payload.forEach(i => {
    if (i.id === 'actionId') {
      const values = i.values;
      params[i.id] = values.map(i => i.action).join('');
      params['resourceType'] = values[0].resourceType;
    } else if (i.id === 'resourceCode') {
      const values = i.values;
      params['iamResourceCode'] = values.map(i => i.iamResourceCode).join('');
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

const handleSelectRow = (value, row) => {
  const index = selections.value.findIndex(i => i.id === row.id);
  if (value && index === -1) {
    selections.value.push(row);
  } else if (!value && index > -1) {
    selections.value.splice(index, 1);
  }
};

const checkSelectedAll = () => {
  if (!selections.value.length) {
    isSelectedAll.value = false;
    return false;
  }

  isSelectedAll.value = optionGroupList.value.every(i => {
    return selections.value.some(group => group.id === i.id);
  })
};

const checkIndeterminate = () => {
  if (!selections.value.length) {
    indeterminate.value = false;
    return false;
  }

  if (selections.value.length > optionGroupList.value.length) {
    checkSelectedAll();
  } else {
    indeterminate.value = selections.value.length !== optionGroupList.value.length;
  }
}

const handleSelectAllGroup = (val) => {
  isSelectedAll.value = val;
  if (val) {
    selections.value = userGroupList.value.filter(i => !i.joined);
  } else {
    selections.value = [];
  }
};

const renderSelectionCell = ({ row, column }) => {
  return h(
    BkCheckbox,
    {
      modelValue: row.joined ? row.joined : selections.value.some(item => item.id === row.id),
      disabled: row.joined,
      class: 'label-text',
      title: row.joined ? t('你已获得该权限') : '',
      onChange(val) {
        handleSelectRow(val, row)
      }
    }
  )
};

const renderSelectionHeader = (col: any) => {
  return h(
    BkCheckbox,
    {
      indeterminate: indeterminate.value,
      modelValue: isSelectedAll.value,
      onChange(val) {
        handleSelectAllGroup(val);
      }
    }
  )
};

const columns = [
  {
    label: renderSelectionHeader,
    width: 60,
    render: renderSelectionCell,
  },
  {
    label: t('资源实例'),
    render ({ cell, row }) {
      return h(
        'span',
        {
          title: row.resourceName, 
        },
        [
          cell,
          row.resourceTypeName,
          ': ',
          row.resourceName
        ]
      );
    },
  },
  {
    label: t('用户组名'),
    render ({ cell, row }) {
      return h(
        'span',
        {
          title: row.name, 
          style: {
            cursor: 'pointer',
            color: '#3a84ff',
          },
          onClick() {
            handleShowGroupDetail(row)
          },
        },
        [
          cell,
          row.name
        ]
      );
    },
  },
  {
    label: t('描述'),
    field: 'description',
    render ({ cell, row }) {
      return h(
        'span',
        {
          title: row.description,
        },
        [
          cell,
          row.description
        ]
      );
    },
  }
];
</script>

<template>
  <article class="group-search">
    <search-select
      class="group-search-filter"
      v-model="filter"
      :search-list="searchList"
      :project-code="projectCode"
      @change="handleChangeSearch">
    </search-select>
    <bk-loading
      class="group-table"
      :loading="isLoading">
      <bk-table
        ref="tableRef"
        :data="userGroupList"
        :columns="columns"
        :border="['row', 'outer']"
      >
      </bk-table>
      <bk-pagination
        class="table-pagination"
        v-bind="pagination"
        type="default"
        @change="handlePageChange"
        @limit-change="handleLimitChange"
      />
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
  
  :deep(.bk-table .bk-table-head table thead th),
  :deep(.bk-table .bk-table-body table thead th) {
    text-align: center !important;
  }
  :deep(.bordered-outer) {
    border-bottom: none;
  }
  .table-pagination {
    border: 1px solid #dcdee5;
    border-top: none;
    height: 40px;
  }
</style>
