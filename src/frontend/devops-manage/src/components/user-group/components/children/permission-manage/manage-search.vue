<template>
    <bk-select
      class="search-select"
      v-model="serviceValue"
      :prefix="t('所属服务')"
      @change="serviceChange"
    >
      <bk-option
        v-for="(item, index) in serviceList"
        :id="item.resourceType"
        :key="index"
        :name="item.name"
      />
    </bk-select>

    <bk-select
      class="search-select"
      v-model="resourceValue"
      :prefix="t('资源')"
      filterable
      :input-search="false"
      :disabled="isAllowSearch"
      :scroll-loading="resourceScrollLoading"
      @scroll-end="resourceScrollEnd"
      >
      <bk-option
        v-for="(item, index) in resourceList"
        :id="item.resourceCode"
        :key="index"
        :name="item.resourceName"
      />
    </bk-select>

    <bk-select
      class="search-select"
      v-model="actionValue"
      :prefix="t('操作')"
      :disabled="isAllowSearch"
      >
      <bk-option
        v-for="(item, index) in actionList"
        :id="item.action"
        :key="index"
        :name="item.actionName"
      />
    </bk-select>

    <div class="search-expired">
      <p class="search-terms">{{ t('过期时间') }}</p>
      <date-picker
        v-model="searchExpiredAt"
        :commonUseList="commonUseList"
        @update:model-value="handleValueChange"
      />
    </div>
    <bk-search-select
      v-model="searchValue"
      :data="searchData"
      unique-select
      class="multi-search"
      value-behavior="need-key"
      :placeholder="filterTips"
      :get-menu-list="getMenuList"
      @search="handleSearch(searchValue)"
    />
</template>

<script setup>
import { ref, onMounted, computed, watch, defineEmits } from 'vue';
import DatePicker from '@blueking/date-picker';
import '@blueking/date-picker/vue3/vue3.css';
import http from '@/http/api';
import { useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';

const route = useRoute();
const { t } = useI18n();
const projectId = computed(() => route.params?.projectCode || route.query?.projectCode);

const serviceValue = ref('');
const resourceValue = ref('');
const actionValue = ref('');
const serviceList = ref([]);
const resourceList = ref([]);
const actionList = ref([]);
const isAllowSearch = ref(true);
const resourceScrollLoading = ref(false);
const resourcePage = ref(1);
const hasNextPage = ref(false);
const searchExpiredAt = ref([]);
const expiredAtList = ref([])
const searchValue = ref([]);
const filterTips = computed(() => {
  return searchData.value.map(item => item.name).join(' / ');
});
const searchData = computed(() => {
  const data = [
    {
      name: t('用户'),
      id: 'user',
    },
    {
      name: t('组织架构'),
      id: 'department',
    },
    {
      name: t('用户组名称'),
      id: 'groupName',
    },
  ]
  return data.filter(data => {
    return !searchValue.value.find(val => val.id === data.id)
  })
});
const commonUseList = ref([
  {
    id: ['now', 'now+24h'],
    name: t('未来 X 小时', [24]),
  },
  {
    id: ['now', 'now+7d'],
    name: t('未来 X 天', [7]),
  },
  {
    id: ['now', 'now+15d'],
    name: t('未来 X 天', [15]),
  },
  {
    id: ['now', 'now+30d'],
    name: t('未来 X 天', [30]),
  },
  {
    id: ['now', 'now+60d'],
    name: t('未来 X 天', [60]),
  },
  {
    id: ['now-24h', 'now'],
    name: t('过去 X 小时', [24]),
  },
  {
    id: ['now-7d', 'now'],
    name: t('过去 X 天', [7]),
  },
  {
    id: ['now-15d', 'now'],
    name: t('过去 X 天', [15]),
  },
  {
    id: ['now-30d', 'now'],
    name: t('过去 X 天', [30]),
  },
  {
    id: ['now-60d', 'now'],
    name: t('过去 X 天', [60]),
  },
]);
const searchGroup = computed(() => ({
  searchValue: searchValue.value,
  expiredAt: expiredAtList.value,
  relatedResourceType: serviceValue.value,
  relatedResourceCode: resourceValue.value,
  action: actionValue.value,
}));
const emit = defineEmits(['searchInit']);

watch(searchGroup, () => {
  emit('searchInit', undefined, searchGroup.value)
});
watch(serviceValue, (newValue) => {
  if (newValue) {
    isAllowSearch.value = false;
    getListResource();
    getListActions();
  } else {
    isAllowSearch.value = true;
  }
})
onMounted(()=>{
  getListResourceTypes()
})
defineExpose({
  clearSearch,
});
function clearSearch () {
  searchValue.value = [];
  searchExpiredAt.value = [];
  expiredAtList.value = [];
  serviceValue.value = '';
  resourceValue.value = '';
  actionValue.value = '';
}
function handleSearch (value) {
  if(!value.length) return;
  searchValue.value = value;
  emit('searchInit', undefined, searchGroup.value)
}
function handleValueChange (value, info) {
  searchExpiredAt.value = value;
  expiredAtList.value = info;
}
async function getListResourceTypes () {
  try {
    const res = await http.getListResourceTypes();
    serviceList.value = res;
  } catch (error) {
    console.error(error);
  }
}
async function getListResource () {
  try {
    resourceScrollLoading.value = true;
    const query = {
      page: resourcePage.value,
      pageSize: 10,
    };
    const res = await http.getListResource(projectId.value, serviceValue.value, query);
    hasNextPage.value = res.hasNext;
    resourceList.value.push(...res.records);
    resourceScrollLoading.value = false;
  } catch (error) {
    console.error(error);
  }
}
async function resourceScrollEnd () {
  if (!hasNextPage.value) return;
  resourcePage.value ++;
  getListResource();
}
async function getListActions () {
  try {
    const res = await http.getListActions(serviceValue.value);
    actionList.value = res;
  } catch (error) {
    console.error(error);
  }
}
function serviceChange (value) {
  serviceValue.value = value;
  resourcePage.value = 1;
  resourceValue.value = '';
  actionValue.value = '';
  resourceList.value = [];
  actionList.value = [];
}
async function getMenuList (item, keyword) {
  const query = {
    memberType: item.id,
    page: 1,
    pageSize: 400,
    projectCode: projectId.value,
  }
  if (item.id === 'user' && keyword) {
    query.userName = keyword
  } else if (item.id === 'department' && keyword) {
    query.deptName = keyword
  }
  if(item.id === 'groupName') {
    return []
  } else {
    const res = await http.getProjectMembers(projectId.value, query)
    return res.records.map(i => {
      return {
        ...i,
        displayName: i.name || i.id,
        name: i.type === 'user' ? (!i.name ? i.id : `${i.id} (${i.name})`) : i.name,
      }
    })
  }
}
</script>

<style lang="scss" scoped>
.search-select {
  margin-right: 10px;
}

.search-expired {
  display: flex;
  align-items: center;

  .search-terms {
    white-space: nowrap;
    color: #63656e;
    line-height: 30px;
    padding: 0 10px;
    color: #63656e;
    border: 1px solid #c4c6cc;
    border-radius: 2px;
    background-color: #f5f7fa;
  }
}

.multi-search {
  width: 50%;
  margin-left: 10px;
}
</style>