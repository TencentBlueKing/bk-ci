<template>
    <bk-tag-input
      ref="tagInputRef"
      class="manage-user-selector"
      clearable
      :placeholder="t('输入交接人，选中回车进行有效性校验')"
      :search-key="searchKeyArr"
      save-key="id"
      display-key="displayName"
      is-async-list
      allow-auto-match
      :list="userList"
      :pasteFn="pasteFn"
      @input="handleInputUserName"
      @change="handleChange"
      @removeAll="removeAll">
    </bk-tag-input>
  </template>
  
  <script setup name="ProjectUserSelector">
  import { ref, computed, onMounted } from 'vue';
  import { useI18n } from 'vue-i18n';
  import { useRoute } from 'vue-router';
  import http from '@/http/api';
  const props = defineProps({
    projectId: String,
  });

  const { t } = useI18n();
  const route = useRoute();
  const emits = defineEmits(['change', 'removeAll']);
  const userList = ref([]);
  const tagInputRef = ref(null);
  const searchKeyArr = computed(() => ['id', 'name']);
  const searchValue = ref();
  
  async function fetchProjectMembers (query) {
    const res = await http.getProjectMembers(props.projectId, query)
    userList.value = res.records.map(i => {
      return {
        ...i,
        name: i.name || i.id,
        displayName: i.type === 'user' ? (!i.name ? i.id : `${i.id} (${i.name})`) : i.id,
      }
    })
  }
   function handleInputUserName (val) {
    userList.value = []
    searchValue.value = null;
    emits('change', { list: searchValue.value, userList: userList.value })
  
    if (!val) return
    const query = {
      memberType: 'user',
      page: 1,
      pageSize: 400,
      userName: val
    }
    fetchProjectMembers(query)
  }
  
  function handleChange (list) {
    searchValue.value = list;
    emits('change', { list: searchValue.value, userList: userList.value })
  }
  
  function pasteFn (val) {
    tagInputRef.value.curInputValue = val;
    tagInputRef.value?.handleInput();
    return []
  }
  
  function removeAll (val) {
    emits('removeAll', val)
  }
  </script>
