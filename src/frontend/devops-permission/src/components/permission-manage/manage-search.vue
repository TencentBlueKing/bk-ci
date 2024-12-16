<template>
  <div class="search">
    <bk-select
      class="search-select"
      :clearable="false"
      v-model="projectValue"
      :prefix="t('所属项目')"
      @change="handleProjectChange"
    >
      <bk-option
        v-for="item in projectList"
        :key="item.englishName"
        :id="item.englishName"
        :name="item.projectName"
      />
    </bk-select>

    <bk-select
      class="search-select"
      v-model="serviceValue"
      :prefix="t('所属服务')"
      :disabled="!projectValue"
      @change="handleServiceChange"
    >
      <bk-option
        v-for="item in serviceList"
        :key="item.resourceType"
        :id="item.resourceType"
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
      @scroll-end="handleResourceScrollEnd"
      :remote-method="fetchSearchResource"
    >
      <bk-option
        v-for="item in resourceList"
        :key="item.resourceCode"
        :id="item.resourceCode"
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
        v-for="item in actionList"
        :key="item.action"
        :id="item.action"
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
      @search="handleSearch"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch, defineEmits } from 'vue';
import DatePicker from '@blueking/date-picker';
import '@blueking/date-picker/vue3/vue3.css';
import http from '@/http/api';
import tools from '@/utils/tools';
import { useI18n } from 'vue-i18n';
import { useRoute } from 'vue-router';
import { cacheProjectCode } from '@/store/useCacheProjectCode'

const route = useRoute();
const { t } = useI18n();
const projectValue = ref('');
const serviceValue = ref('');
const resourceValue = ref('');
const actionValue = ref('');
const projectList = ref([]);
const serviceList = ref([]);
const resourceList = ref([]);
const actionList = ref([]);
const isAllowSearch = ref(true);
const resourceScrollLoading = ref(false);
const resourcePage = ref(1);
const hasNextPage = ref(false);
const searchExpiredAt = ref([]);
const expiredAtList = ref([]);
const searchValue = ref([]);
const searchResourceName = ref('');
const filterTips = computed(() => searchData.value.map(item => item.name).join(' / '));
const searchData = computed(() => [
  { name: t('用户组名'), id: 'groupName' }
].filter(data => !searchValue.value.find(val => val.id === data.id)));

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
  expiredAt: expiredAtList.value,  // 过期时间
  relatedResourceType: serviceValue.value, // 所属服务
  relatedResourceCode: resourceValue.value, // 资源
  action: actionValue.value,  // 操作
}));
const emit = defineEmits(['searchInit']);

watch([searchGroup, projectValue], () => {
  emit('searchInit', projectValue.value, searchGroup.value);
});

watch(serviceValue, (newValue) => {
  isAllowSearch.value = !newValue;
  if (newValue) {
    fetchResourceList();
    fetchActionList();
  }
});

onMounted(() => {
  fetchResourceTypes();
  console.log(route?.params.projectCode ,route?.query.projectCode ,route?.query.project_code ,cacheProjectCode.get(),tools.getCookie('X-DEVOPS-PROJECT-ID'), '---')
});

defineExpose({
  clearSearch,
});

async function fetchResourceTypes() {
  try {
    const [projects, resourceTypes] = await Promise.all([
      http.fetchProjectList(),
      http.getResourceTypesList()
    ]);
    serviceList.value = resourceTypes;
    projectList.value = projects;
    projectValue.value = route?.params.projectCode || route?.query.projectCode || route?.query.project_code || cacheProjectCode.get() || projects[0].englishName
  } catch (error) {
    console.log(error);
  }
}

function handleProjectChange(value) {
  cacheProjectCode.set(value);
  projectValue.value = value;
  serviceValue.value = '';
  resetSelections();
}

function handleServiceChange(value) {
  serviceValue.value = value;
  resetSelections();
}

function resetSelections() {
  resourcePage.value = 1;
  resourceValue.value = '';
  actionValue.value = '';
  resourceList.value = [];
  actionList.value = [];
}

async function fetchResourceList() {
  try {
    resourceScrollLoading.value = true;
    const query = {
      page: resourcePage.value,
      pageSize: 10,
      ...(searchResourceName.value && { resourceName: searchResourceName.value })
    };
    const res = await http.getListResource(projectValue.value, serviceValue.value, query);
    hasNextPage.value = res.hasNext;
    resourceList.value.push(...res.records);
  } catch (error) {
    console.log(error);
  } finally {
    resourceScrollLoading.value = false;
  }
}

function fetchSearchResource(val) {
  searchResourceName.value = val;
  resourceList.value = [];
  fetchResourceList();
}

async function handleResourceScrollEnd() {
  if (hasNextPage.value) {
    resourcePage.value++;
    fetchResourceList();
  }
}

async function fetchActionList() {
  try {
    const res = await http.getListActions(serviceValue.value);
    actionList.value = res;
  } catch (error) {
    console.log(error);
  }
}

function handleValueChange(value, info) {
  searchExpiredAt.value = value;
  expiredAtList.value = info;
}

function handleSearch(value) {
  if (value.length) {
    searchValue.value = value;
    emit('searchInit', projectValue.value, searchGroup.value);
  }
}

function clearSearch() {
  searchValue.value = [];
  searchExpiredAt.value = [];
  expiredAtList.value = [];
  serviceValue.value = '';
  resetSelections();
}
/**
 * 下拉搜索获取列表数据
 */
// async function getMenuList (item, keyword) {
  // const query = {
  //   memberType: item.id,
  //   page: 1,
  //   pageSize: 400,
  //   projectCode: projectId.value,
  // }
  // if (item.id === 'user' && keyword) {
  //   query.userName = keyword
  // } else if (item.id === 'department' && keyword) {
  //   query.deptName = keyword
  // }
  // if(item.id === 'groupName') {
  //   return []
  // } else {
  //   const res = await http.getProjectMembers(projectId.value, query)
  //   return res.records.map(i => {
  //     return {
  //       ...i,
  //       displayName: i.name || i.id,
  //       name: i.type === 'user' ? (!i.name ? i.id : `${i.id} (${i.name})`) : i.name,
  //     }
  //   })
  // }
// }
</script>
<style lang="less" scoped>
.search{ 
  display: flex;
  background: #FFFFFF;
  align-items: center;
  padding: 0 24px;
  height: 64px;
  box-shadow: 0 2px 4px 0 #1919290d;
}
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
  width: 40%;
  margin-left: 10px;
}
</style>