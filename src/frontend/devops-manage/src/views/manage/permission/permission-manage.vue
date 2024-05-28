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
          placeholder="代码库/授权人/授权时间"
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
          v-bkloading="{ isLoading }"
          :scroll-loading="isScrollLoading"
          @select-all="handleSelectAll"
          @selection-change="handleSelectionChange"
          @scroll-bottom="handleScrollBottom"
        >
          <template #prepend>
            <div v-if="selectList.length" class="prepend">
              已选择 {{ selectList.length }} 条数据，
              <span @click="handleSelectAllData"> 选择全量数据 {{ total }} 条 </span> 
              &nbsp; | &nbsp;
              <span @click="handleClear">清除选择</span> 
            </div>
          </template>
        </bk-table>
      </bk-loading>
    </div>
  </div>
  <bk-dialog
    :is-show="isShowDialog"
    :theme="'primary'"
    :width="640"
    :title="t('批量重置')"
    :confirm-text="t('重置')"
    :is-loading="dialogLoading"
    @closed="dialogClose"
    @confirm="dialogConfirm"
  >
    <div class="dialog">
      <bk-tag radius="20px" class="tag">已选择{{ selectList.length }}个代码库</bk-tag>
      <bk-form
        ref="formRef"
        :model="formData"
      >
        <bk-form-item
          required
          label="重置授权人"
          property="name"
          labelWidth=""
        >
          <bk-input
            v-model="formData.name"
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
        <bk-table
          ref="resetTable"
          height="100%"
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

<script setup name="permission-manage">
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import { ref, onMounted } from 'vue';
import { Message } from 'bkui-vue';

const { t } = useI18n();
const router = useRouter();

const total = ref(0);
const activeNav = ref('');
const tableData = ref([]);
const resetTableData = ref([
  {
    id: 1,
    "code": "bkdevops-plugins-test/fayenodejstesa",
    "reason": "2018-05-25 15:02:0",
    "percent": "",
  },
  {
    id: 2,
    "code": "bkdevops-plugins-test/fayenodejstesa",
    "reason": "2018-05-25 15:02:1",
    "percent": "",
  }
]);
const activeIndex = ref();
const formRef = ref(null);
const refTable = ref(null);
const selectList = ref([]);
const searchValue = ref([]);
const isLoading = ref(false);
const isShowDialog = ref(false);
const dialogLoading = ref(false);
const isResetFailure = ref(false);
const isScrollLoading = ref(false);

const permissionList = ref([
  {
    name: 'codeBase',
    label: t('代码库授权'),
  },
  {
    name: 'pipeline',
    label: t('流水线执行授权'),
  },
  {
    name: 'deployNode',
    label: t('部署节点授权'),
  },
]);
const searchData = ref([
  {
    name: '实例业务',
    id: '2',
    onlyRecommendChildren: true,
    children: [
      {
        name: '王者荣耀',
        id: '2-1',
      },
      {
        name: '刺激战场',
        id: '2-2',
      },
      {
        name: '绝地求生',
        id: '2-3',
      },
    ],
  },
  {
    name: 'IP地址',
    id: '3',
  },
  {
    name: 'testestset',
    id: '4',
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
    field: "code",
  },
  {
    label: "授权人",
    field: "percent",
  },
  {
    label: "授权时间",
    field: "create_time",
  },
])
const formData = ref({
  name: ''
})

onMounted(() => {
  activeNav.value = t('代码库授权');
  activeIndex.value = 0;
  getTableList();
});
/**
 * 获取列表数据
 */
function getTableList(){
 // 调用接口获取表格数据
 tableData.value =[
    {
      id: 1,
      "code": "bkdevops-plugins-test/fayenodejstesa",
      "percent": "OAUTH@  daisyhong",
      "create_time": "2018-05-25 15:02:0"
    },
    {
      id: 2,
      "code": "bkdevops-plugins-test/fayenodejstesa",
      "percent": "OAUTH@  daisyhong",
      "create_time": "2018-05-25 15:02:1"
    },
    {
      id: 3,
      "code": "bkdevops-plugins-test/fayenodejstesa",
      "percent": "OAUTH@  daisyhong",
      "create_time": "2018-05-25 15:02:2"
    },{
      id: 1,
      "code": "bkdevops-plugins-test/fayenodejstesa",
      "percent": "OAUTH@  daisyhong",
      "create_time": "2018-05-25 15:02:0"
    },
    {
      id: 2,
      "code": "bkdevops-plugins-test/fayenodejstesa",
      "percent": "OAUTH@  daisyhong",
      "create_time": "2018-05-25 15:02:1"
    },
    {
      id: 3,
      "code": "bkdevops-plugins-test/fayenodejstesa",
      "percent": "OAUTH@  daisyhong",
      "create_time": "2018-05-25 15:02:2"
    },
 ]
 total.value = tableData.value.length;
}
/**
 * aside点击事件
 */
function handleAsideClick(params, index) {
  activeIndex.value = index;
  activeNav.value = params.label;
  // 模拟
  isLoading.value = true
  setTimeout(()=>{
    getTableList();
    isLoading.value = false
  },2000)
};
/**
 * 批量重置
 */
function handleReset() {
console.log(selectList.value.length);
  if(!selectList.value.length) {
    Message({
      theme: 'error',
      message: '请先选择数据',
    });
    return;
  }
  isShowDialog.value = true;
}
/**
 * 触底加载
 */
function handleScrollBottom(arg) {
  isScrollLoading.value = true;
  // 模拟
  setTimeout(() => {
    isScrollLoading.value = false;
    tableData.value.push({
      id: 1,
      "code": "bkdevops-plugins-test/fayenodejstesa",
      "percent": "OAUTH@  daisyhong",
      "create_time": "2018-05-25 15:02:0"
    },
    {
      id: 2,
      "code": "bkdevops-plugins-test/fayenodejstesa",
      "percent": "OAUTH@  daisyhong",
      "create_time": "2018-05-25 15:02:1"
    },
    {
      id: 3,
      "code": "bkdevops-plugins-test/fayenodejstesa",
      "percent": "OAUTH@  daisyhong",
      "create_time": "2018-05-25 15:02:2"
    },)
    total.value = tableData.value.length;
  }, 1500);
}
/**
 * 当前页全选事件
 */
function handleSelectAll(val){
  selectList.value = [];
  if (val.checked) {
    tableData.value.forEach((item) => {
      selectList.value.push(item.id);
    });
  } else {
    selectList.value = [];
  }
}
/**
 * 多选事件
 * @param val
 */
function handleSelectionChange(val) {
  if (val.checked) {
    selectList.value.push(val.row.id);
  } else {
    selectList.value = selectList.value.filter((item) => item !== val.row.id);
  }
};
/**
 * 全量数据选择
 */
function handleSelectAllData() {
  refTable.value.toggleAllSelection()
  // 调用接口获取全部数据后
  selectList.value = tableData.value.map((item) => item.id);
}
/**
 * 清除选择
 */
function handleClear() {
  refTable.value.clearSelection();
  selectList.value = [];
}
/**
 * 弹窗关闭
 */
function dialogClose() {
  isShowDialog.value = false;
  isResetFailure.value = false;
}
/**
 * 弹窗提交
 */
function dialogConfirm() {
  dialogLoading.value = true;
  formRef.value?.validate().then( isValid => {
    if (isValid) {
      // 接口判断 是否重置失败
      setTimeout(()=>{
        dialogLoading.value = false;
        if(Math.random() > 0.5){
          isResetFailure.value = true;
          console.log(resetTableData.value,'重置授权人表格数据');
        }else{
          Message({
            theme: 'success',
            message: '代码库授权已成功重置',
          });
        }
      },1000)
    }
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