<template>
  <div class="permission-wrapper">
    <ul class="aside">
      <li
        class="aside-item"
        :class="activeIndex == index ? 'aside-active' : ''"
        v-for="(item, index) in permissionList"
        :key="index"
        @click="handleAsideClick(item, index)"
      >
        <p>
          {{ item.label }}
        </p>
      </li>
    </ul>
    <div class="content">
      <div class="content-btn">
        <bk-button @click="handleReset">{{ t('批量重置') }}</bk-button>
        <bk-search-select
          v-model="searchValue"
          :data="searchData"
          unique-select
          max-height="32"
          class="content-btn-search"
          :placeholder="filterTips"
          :key="searchName"
        />
      </div>
      <bk-loading class="content-table" :loading="isLoading">
        <bk-table
          ref="refTable"
          checked
          :data="tableData"
          :columns="columns"
          height="100%"
          show-overflow-tooltip
          :scroll-loading="isScrollLoading"
          @select-all="handleSelectAll"
          @selection-change="handleSelectionChange"
          @scroll-bottom="getTableList"
        >
          <template #prepend>
            <div v-if="isSelectAll" class="prepend">
              已选择全量数据 {{ totalCount }} 条，
              <span @click="handleClear">清除选择</span>
            </div>
            <div v-else-if="selectList.length" class="prepend">
              已选择 {{ selectList.length }} 条数据，
              <span @click="handleSelectAllData"> 选择全量数据 {{ totalCount }} 条 </span> 
              &nbsp; | &nbsp;
              <span @click="handleClear">清除选择</span> 
            </div>
          </template>
        </bk-table>
      </bk-loading> 
    </div>
  </div>
  <bk-dialog
    :is-show="showResetDialog"
    :theme="'primary'"
    :width="640"
    :title="t('批量重置')"
    :confirm-text="t('重置')"
    :is-loading="dialogLoading"
    @closed="dialogClose"
    @confirm="confirmReset"
  >
    <div class="dialog">
      <bk-tag radius="20px" class="tag">已选择{{ isSelectAll ? totalCount : selectList.length }}个代码库</bk-tag>
      <bk-form
        ref="formRef"
        :model="resetFormData"
      >
        <bk-form-item
          required
          label="重置授权人"
          property="name"
          labelWidth=""
        >
          <bk-input
            v-model="resetFormData.name"
            placeholder="请输入"
            clearable
          />
        </bk-form-item>
      </bk-form>

      <div v-if="isResetFailure" class="reset-failure">
        <p>
          <img src="@/css/svg/close.svg" class="close-icon">
          以下授权重置失败，请<span>重新指定其他授权人</span>
        </p>
        <p class="reset-table-item">代码库授权</p>
        <bk-table
          ref="resetTable"
          :data="resetTableData"
          :border="['outer', 'row']"
          show-overflow-tooltip
        >
          <bk-table-column label="代码库" prop="code" />
          <bk-table-column label="失败原因" prop="reason" />
          <bk-table-column label="授权人" prop="percent">
            <template #default="{ row, index }">
              <bk-input v-model="row.percent"></bk-input>
            </template>
          </bk-table-column>
        </bk-table>
      </div>
    </div>
  </bk-dialog>
</template>

<script setup name="PermissionManage">
import http from '@/http/api';
import { useI18n } from 'vue-i18n';
import { ref, onMounted, computed, h } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { convertTime } from '@/utils/util'
import { Message } from 'bkui-vue';
import { renderType } from 'bkui-vue/lib/shared';

const { t } = useI18n();
const route = useRoute();
const router = useRouter();

const tableData = ref([]);
const resetTableData = ref([]);
const activeIndex = ref(0);
const formRef = ref(null);
const refTable = ref(null);
const selectList = ref([]);
const searchValue = ref([]);
const isLoading = ref(true);
const showResetDialog = ref(false);
const dialogLoading = ref(false);
const isResetFailure = ref(false);
const isScrollLoading = ref(false);
const page = ref(1);
const pageSize = ref(20);
const projectId = computed(() => route.params?.projectCode);
const resourceType = ref('repertory');
const hasNext = ref(true);
const totalCount = ref(0);
const isSelectAll = ref(false);  // 选择全量数据
const searchName = computed(() => {
  const nameMap = {
    'pipeline': t('流水线'),
    'env_node': t('部署节点'),
    'repertory': t('代码库')
  }
  return nameMap[resourceType.value]
})
const filterTips = computed(() => {
  return searchData.value.map(item => item.name).join(' / ')
})

const resetParams = computed(() => {
  const resourceAuthorizationHandoverList = selectList.value.map(item => {
    return {
      projectCode: projectId.value,
      resourceType: resourceType.value,
      resourceName: item.resourceName,
      resourceCode: item.resourceCode,
      handoverFrom: item.handoverFrom,
      handoverTo: resetFormData.value.name
    }
  })
  const params = {
    projectCode: projectId.value,
    resourceType: resourceType.value,
    fullSelection: isSelectAll.value,
    handoverChannel: 'MANAGER',
    resourceAuthorizationHandoverList: isSelectAll.value ? [] : resourceAuthorizationHandoverList,
  }
  if (isSelectAll.value) {
    params.handoverTo = resetFormData.value.name
  }
  return params
})
const permissionList = ref([
  {
    name: 'codeBase',
    label: t('代码库授权'),
    resourceType: 'repertory'
  },
  {
    name: 'pipeline',
    label: t('流水线执行授权'),
    resourceType: 'pipeline'
  },
  {
    name: 'deployNode',
    label: t('部署节点授权'),
    resourceType: 'env_node'
  },
]);
const searchData = ref([
  {
    name: searchName,
    id: 1, 
  },
  {
    name: t('授权人'),
    id: 1,
  },
]);
const columns = ref([
  {
    type: "selection",
    width: 30,
    align: 'center'
  },
  {
    label: "代码库",
    field: "resourceName",
  },
  {
    label: "授权人",
    field: "handoverFrom",
  },
  {
    label: "授权时间",
    field: "handoverTime",
    render ({ cell, row }) {
      return h(
        'span',
        [
          convertTime(cell)
        ]
      );
    },
  },
])
const resetFormData = ref({
  name: ''
})

onMounted(() => {
  getTableList();
});

function init () {
  page.value = 1;
  tableData.value = [];
  hasNext.value = true;
  searchValue.value = [];
  isSelectAll.value = false;
};
/**
 * 获取列表数据
 */
async function  getTableList () {
  if (!hasNext.value) return;

  if (page.value === 1) {
    isLoading.value = true;
  } else {
    isScrollLoading.value = true;
  }
  try {
    const res = await http.getResourceAuthList(projectId.value, {
      page: page.value,
      pageSize: pageSize.value,
      projectCode: projectId.value,
      resourceType: resourceType.value
    })
    tableData.value = [...tableData.value, ...res.records]
    page.value += 1
    hasNext.value = res.hasNext
    totalCount.value = res.count

    isLoading.value = false
    isScrollLoading.value = false
  } catch (e) {
    isLoading.value = false
    isScrollLoading.value = false
    console.error(e)
  }
}
/**
 * aside点击事件
 */
function handleAsideClick(item, index) {
  if (activeIndex.value === index) return
  activeIndex.value = index;
  resourceType.value = item.resourceType;
  init();
  getTableList();
};
/**
 * 批量重置
 */
function handleReset() {
  if(!selectList.value.length) {
    Message({
      theme: 'error',
      message: '请先选择数据',
    });
    return;
  }
  showResetDialog.value = true;
}
/**
 * 当前页全选事件
 */
function handleSelectAll(val){
  selectList.value = refTable.value.getSelection();
}
/**
 * 多选事件
 * @param val
 */
function handleSelectionChange(val) {
  selectList.value = refTable.value.getSelection();
};
/**
 * 全量数据选择
 */
function handleSelectAllData() {
  if (!isSelectAll.value && selectList.value.length !== tableData.value.length) {
    refTable.value.toggleAllSelection();
  }
  isSelectAll.value = true;

  selectList.value = tableData.value.map((item) => item.id);
}
/**
 * 清除选择
 */
function handleClear() {
  refTable.value.clearSelection();
  isSelectAll.value = false;
  selectList.value = [];
}
/**
 * 弹窗关闭
 */
function dialogClose() {
  showResetDialog.value = false;
  isResetFailure.value = false;
}
/**
 * 弹窗提交
 */
function confirmReset() {
  dialogLoading.value = true;
  formRef.value?.validate().then(async () => {
    try {
      const res = await http.resetAuthorization(projectId.value, resetParams.value, resourceType.value)
      console.log(res, 123123)
    } catch (e) {
      console.error(e)
    }
    // setTimeout(()=>{
    //   dialogLoading.value = false;
    //   if(Math.random() > 0.5){
    //     isResetFailure.value = true;
    //   }else{
    //     Message({
    //       theme: 'success',
    //       message: '代码库授权已成功重置',
    //     });
    //   }
    // },1000)
  }).catch(()=>{
    
  }).finally(()=>{
    dialogLoading.value = false;
    isResetFailure.value = false;
  })
}

</script>

<style scoped lang="scss">
.permission-wrapper {
  width: 100%;
  height: 100%;
  display: flex;

  .aside {
    width: 240px;
    height: 100%;
    padding-top: 8px;
    background: #FAFBFD;
    box-shadow: 1px 0 0 0 #DCDEE5;

    .aside-item {
      height: 40px;
      font-size: 14px;
      color: #63656E;
      line-height: 40px;
      cursor: pointer;

      p {
        margin-left: 22px;
      }
    }

    .aside-active {
      background: #E1ECFF;
      color: #3A84FF;
    }
  }

  .content {
    flex: 1;
    padding: 24px;

    .content-btn {
      display: flex;
      justify-content: space-between;
      margin-bottom: 17px;

      &-search {
        width: 500px;
      }
    }

    .content-table{
      border: 1px solid #DCDEE5;
      height: calc(100vh - 158px);

      .prepend{
        width: 100%;
        height: 32px;
        line-height: 32px;
        background: #F0F1F5;
        text-align: center;
        box-shadow: 0 -1px 0 0 #DCDEE5;

        span{
          font-family: MicrosoftYaHei;
          font-size: 12px;
          color: #3A84FF;
          letter-spacing: 0;
          line-height: 20px;
          cursor: pointer;
        }
      }
    }
  }
}
.dialog{

  .tag{
    padding: 16px;
    font-size: 12px;
    color: #63656E;
    line-height: 20px;
    margin-bottom: 16px;
  }

  .reset-failure{
    border-top: 1px solid #DCDEE5;
    margin-bottom: 25px;

    p{
      margin: 8px 0 18px 0;
      color: #63656e;
      font-size: 12px;

      span{
        font-weight: 700;
      }
    }
    
    .close-icon{
      width: 14px;
      height: 14px;
      vertical-align: middle;
    }
  }
}
</style>