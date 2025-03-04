import http from '@/http/api';
import { ref,  } from 'vue';
import { defineStore } from 'pinia';

export default defineStore('userRepoConfigTable', () => {
  const curConfig = ref<repoConfigFromData>({});
  const pagination = ref({ count: 0, current: 1, limit: 20 });
  const isLoading = ref(false);
  const repoConfigList = ref([]);
  const borderConfig = ref(['outer', 'row']);
  const initPagination = () => {
    pagination.value = { count: 0, current: 1, limit: 20 }
  }
  const getRepoConfigList = async () => {
    try {
      isLoading.value = true;
      const res = await http.fetchRepoConfigList({
        page: pagination.value.current,
        pageSize: pagination.value.limit
      });
      repoConfigList.value = res.records;
      pagination.value.count = res.count;
    } catch (e) {
      console.error(e)
    } finally {
      isLoading.value = false;
    }
  }

  const handlePageLimitChange = (limit: number) => {
    pagination.value.current = 1;
    pagination.value.limit = limit;
    getRepoConfigList();
  }

  const handlePageValueChange = (value: number) => {
    pagination.value.current = value;
    getRepoConfigList();
  }

  const handleScrollEnd = () => {
    if (pagination.value.count === repoConfigList.value.length) return
    pagination.value.current += 1;
    getRepoConfigList();
  }

  const setCurConfig = (value) => {
    curConfig.value = value
  }

  return {
    pagination,
    isLoading,
    curConfig,
    repoConfigList,
    borderConfig,
    setCurConfig,
    initPagination,
    handleScrollEnd,
    getRepoConfigList,
    handlePageLimitChange,
    handlePageValueChange
  }
})