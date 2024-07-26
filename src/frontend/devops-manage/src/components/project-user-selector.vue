<template>
  <bk-tag-input
    class="manage-user-selector"
    clearable
    :placeholder="t('输入授权人，选中回车进行校验')"
    :search-key="['id', 'name']"
    save-key="name"
    display-key="displayName"
    :list="userList"
    @input="handleInputUserName"
    @change="handleChange"
    @removeAll="removeAll">
  </bk-tag-input>
</template>

<script setup name="ProjectUserSelector">
import { ref, computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute } from 'vue-router';
import http from '@/http/api';

const { t } = useI18n();
const route = useRoute();
const userList = ref([]);
const projectId = computed(() => route.params?.projectCode);
const emits = defineEmits(['change', 'removeAll']);

function debounce (callBack) {
    window.clearTimeout(debounce.timeId)
    debounce.timeId = window.setTimeout(() => {
        callBack()
    }, 300)
}

async function fetchProjectMembers (query) {
  const res = await http.getProjectMembers(projectId.value, query)
  userList.value = res.records.map(i => {
    return {
      ...i,
      displayName: `${i.id}(${i.name})`,
    }
  })
}
 function handleInputUserName (val) {
  userList.value = []
  if (!val) return
  const query = {
    memberType: 'user',
    page: 1,
    pageSize: 200,
    userName: val
  }
  debounce(() => fetchProjectMembers(query))
}

function handleChange (list) {
    emits('change', { list, userList: userList.value })
}

function removeAll (val) {
    emits('removeAll', val)
}
</script>