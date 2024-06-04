<template>
  <bk-loading class="manage" :loading="isLoading">
    <div class="manage-search">
      <select-group
        :value="serveValue"
        :options="serveList"
        prefix="所属服务"
        @handle-change="value => handleSearchChange(value, 'serve')"
      />
      <select-group
        :value="resourceValue"
        :options="resourceList"
        prefix="资源"
        @handle-change="value => handleSearchChange(value, 'resource')"
      />
      <select-group
        :value="operateValue"
        :options="operateList"
        prefix="操作"
        @handle-change="value => handleSearchChange(value, 'operate')"
      />
      <bk-search-select
        v-model="searchValue"
        :data="searchData"
        unique-select
        class="multi-search"
        placeholder="用户/组织架构/用户组名/用户组 ID/用户组描述"
      />
    </div>
    <div class="manage-article">
      <div class="manage-aside">
        <manage-aside :member-list="memberList" @handle-click="handleAsideClick" />
      </div>
      <div class="manage-content">
        <div class="manage-content-btn">
          <bk-button @click="batchRenewal">批量续期</bk-button>
          <bk-button @click="batchHandover">批量移交</bk-button>
          <bk-button @click="batchRemove">批量移出</bk-button>
        </div>
        <div class="manage-content-project">
          <p class="project-group">项目级用户组</p>
          <div class="project-group-table">
            <bk-collapse>
              <bk-collapse-panel v-model="activeIndex">
                <template #header>
                  <p class="group-title">
                    <i class="permission-icon permission-icon-down-shape"></i>
                    项目（project）
                    <span class="group-num">3</span>
                  </p>
                </template>
                <template #content>
                  <bk-table
                    ref="refTable"
                    checked
                    max-height="200"
                    :data="projectTable"
                    show-overflow-tooltip
                    @select-all="handleSelectAll"
                    @selection-change="handleSelectionChange"
                  >
                    <template #prepend>
                      <div v-if="selectList.length" class="prepend">
                        已选择 {{ selectList.length }} 条数据，
                        <span @click="handleSelectAllData"> 选择全量数据 {{ total }} 条 </span>
                        &nbsp; | &nbsp;
                        <span @click="handleClear">清除选择</span>
                      </div>
                    </template>
                    <!-- <bk-table-column type="selection" :min-width="30" width="30" align="center" /> -->
                    <bk-table-column label="用户组" prop="usergroup">
                    </bk-table-column>
                    <bk-table-column label="用户描述" prop="describe" />
                    <bk-table-column label="有效期" prop="validity" />
                    <bk-table-column label="加入时间" prop="data" />
                    <bk-table-column label="加入方式/操作人" prop="type" />
                    <bk-table-column label="操作">
                      <template #default="{row}">
                        <div class="operation-btn">
                          <span v-bk-tooltips="{ content: '唯一管理员，不可移出。请添加新的管理员后再移出。', disabled: !row.isUninstall }">
                            <bk-button
                              text
                              theme="primary"
                              :disabled="row.isUninstall"
                              @click="handleRenewal(row)"
                            >续期</bk-button>
                          </span>
                          <span v-bk-tooltips="{ content: '通过用户组加入，不可直接移出。如需调整，请编辑用户组。', disabled: !row.isUninstall }">
                            <bk-button
                              text
                              theme="primary"
                              :disabled="row.isUninstall"
                              @click="handleHandover(row)"
                            >移交</bk-button>
                          </span>
                          <span v-bk-tooltips="{ content: '唯一管理员，不可移出。请添加新的管理员后再移出。', disabled: !row.isUninstall }">
                            <bk-button
                              text
                              theme="primary"
                              :disabled="row.isUninstall"
                              @click="handleRemove(row)"
                            >移出</bk-button>
                          </span>
                        </div>
                      </template>
                    </bk-table-column>
                  </bk-table>
                </template>
              </bk-collapse-panel>
            </bk-collapse>
          </div>
        </div>
        <div class="manage-content-resource">
          <p class="project-group">资源级用户组</p>
          <div class="project-group-table">
            <p class="group-title" @click="toggleTable">
              流水线-流水线组
              <span class="group-num">3</span>
            </p>
            <transition name="collapse">
              <div v-if="isTableVisible">
                <bk-table
                  max-height="200"
                  :data="projectTable"
                  show-overflow-tooltip
                >
                  <bk-table-column label="用户组" prop="usergroup" />
                  <bk-table-column label="用户描述" prop="usergroup" />
                  <bk-table-column label="有效期" prop="usergroup" />
                  <bk-table-column label="加入时间" prop="usergroup" />
                  <bk-table-column label="加入方式/操作人" prop="usergroup" />
                  <bk-table-column label="操作">
                    <template #default="{ row }">
                      <div class="operation-btn">
                        <span v-bk-tooltips="{ content: '唯一管理员，不可移出。请添加新的管理员后再移出。', disabled: !row.isUninstall }">
                          <bk-button text theme="primary" :disabled="row.isUninstall">续期</bk-button>
                        </span>
                        <span v-bk-tooltips="{ content: '通过用户组加入，不可直接移出。如需调整，请编辑用户组。', disabled: !row.isUninstall }">
                          <bk-button text theme="primary" :disabled="row.isUninstall">移交</bk-button>
                        </span>
                        <span v-bk-tooltips="{ content: '唯一管理员，不可移出。请添加新的管理员后再移出。', disabled: !row.isUninstall }">
                          <bk-button text theme="primary" :disabled="row.isUninstall">移出</bk-button>
                        </span>
                      </div>
                    </template>
                  </bk-table-column>
                </bk-table>
              </div>
            </transition>
          </div>
          <div class="project-group-table">
            <p class="group-title" @click="toggleTable">
              流水线-流水线组
              <span class="group-num">3</span>
            </p>
            <transition name="collapse">
              <div v-if="isTableVisible">
                <bk-table
                  max-height="200"
                  :data="projectTable"
                  show-overflow-tooltip
                >
                  <bk-table-column label="用户组" prop="usergroup" />
                  <bk-table-column label="用户描述" prop="usergroup" />
                  <bk-table-column label="有效期" prop="usergroup" />
                  <bk-table-column label="加入时间" prop="usergroup" />
                  <bk-table-column label="加入方式/操作人" prop="usergroup" />
                  <bk-table-column label="操作">
                    <template #default="{ row }">
                      <div class="operation-btn">
                        <div class="operation-btn">
                          <span v-bk-tooltips="{ content: '唯一管理员，不可移出。请添加新的管理员后再移出。', disabled: !row.isUninstall }">
                            <bk-button text theme="primary" :disabled="row.isUninstall">续期</bk-button>
                          </span>
                          <span v-bk-tooltips="{ content: '通过用户组加入，不可直接移出。如需调整，请编辑用户组。', disabled: !row.isUninstall }">
                            <bk-button text theme="primary" :disabled="row.isUninstall">移交</bk-button>
                          </span>
                          <span v-bk-tooltips="{ content: '唯一管理员，不可移出。请添加新的管理员后再移出。', disabled: !row.isUninstall }">
                            <bk-button text theme="primary" :disabled="row.isUninstall">移出</bk-button>
                          </span>
                        </div>
                      </div>
                    </template>
                  </bk-table-column>
                </bk-table>
              </div>
            </transition>
          </div>
        </div>
      </div>
    </div>
  </bk-loading>
  <bk-dialog
    :width="450"
    theme="danger"
    cancel-text="关闭"
    confirm-text="确认移出"
    header-align="center"
    footer-align="center"
    class="renewal-dialog"
    :is-show="isShowRenewal"
    @closed="() => isShowRenewal = false"
    @confirm="() => isShowRenewal = false"
  >
    <template #header>
      人员列表
      <span class="dialog-header"> 蓝鲸运营组 </span>
    </template>
    <template #default>
      <p class="remove-text">
        <span>用户组名：</span> 开发人员
      </p>
      <p class="remove-text">
        <span class="is-required">授权期限</span>
        <bk-radio-group
          size="small"
          v-model="radioGroupValue"
        >
          <bk-radio-button label="1个月" />
          <bk-radio-button label="3个月" />
          <bk-radio-button label="6个月" />
          <bk-radio-button label="自定义" />
        </bk-radio-group>
      </p>
      <p class="remove-text">
        <span>到期时间：</span> 已过期
      </p>
    </template>
  </bk-dialog>
  <bk-dialog
    :width="450"
    theme="danger"
    cancel-text="关闭"
    confirm-text="确认移出"
    header-align="center"
    footer-align="center"
    class="remove-dialog"
    :is-show="isShowRemove"
    @closed="() => isShowRemove = false"
    @confirm="() => isShowRemove = false"
  >
    <template #header>
      <span class="dialog-header"> 确认从用户组中移出用户吗？ </span>
    </template>
    <template #default>
      <p class="remove-text">
        <span>待移出用户：</span> fayewang (王玉菊)
      </p>
      <p class="remove-text">
        <span>所在用户组：</span> 开发人员
      </p>
    </template>
  </bk-dialog>
</template>

<script setup name="manageAll">
import { useI18n } from 'vue-i18n';
import { ref, watch  } from 'vue';
import SelectGroup from './select-group.vue';
import ManageAside from './manage-aside.vue';

const { t } = useI18n();
const refTable = ref(null);
const selectList = ref([]);
const isLoading = ref(false);
const isTableVisible = ref(true);
const isShowRenewal = ref(false);
const isShowHandover = ref(false);
const isShowRemove = ref(false);
const radioGroupValue = ref('1个月');

const serveValue = ref([]);
const serveList = ref([
  {
    value: 'climbing',
    label: '爬山',
  },
  {
    value: 'running',
    label: '跑步',
  },
]);

const resourceValue = ref([]);
const resourceList = ref([
  {
    value: 'unknow',
    label: '未知',
  },
  {
    value: 'fitness',
    label: '健身',
  },
]);

const operateValue = ref([]);
const operateList = ref([
  {
    value: 'bike',
    label: '骑车',
  },
  {
    value: 'dancing',
    label: '跳舞',
  },
]);

const searchValue = ref([]);
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

const projectTable = ref([{
  usergroup: 'asdsa',
  describe: 'kjkjkjk',
  validity: '0505',
  data: '08-18',
  type: '加入组',
  isUninstall: true,
},
{
  usergroup: 'asdsa',
  describe: 'kjkjkjk',
  validity: '0505',
  data: '08-18',
  type: '加入组',
  isUninstall: false,
}, {
  usergroup: 'asdsa',
  describe: 'kjkjkjk',
  validity: '0505',
  data: '08-18',
  type: '加入组',
  isUninstall: false,
},
{
  usergroup: 'asdsa',
  describe: 'kjkjkjk',
  validity: '0505',
  data: '08-18',
  type: '加入组',
  isUninstall: false,
}]);

const stateRefs = {
  serve: serveValue,
  resource: resourceValue,
  operate: operateValue,
};

const memberList = ref([
  {
    projectId: 1,
    bgName: 'IEG互动娱乐事业群',
    centerName: 'SRE平台研发中心',
    deptName: '技术运营部',
    approver: 'v_hejieehe',
    subjectScopes: [
      {
        name: '余姣姣',
        full_name: 'v_yjjiaoyu',
        expiredId: 1743602525,
      },
      {
        name: '何福寿',
        full_name: 'terlinhe',
        expiredId: 4102444800,
      },
    ],
  },
]);

const activeIndex =  ref([0]);

watch([serveValue, resourceValue, operateValue, searchValue], () => {
  // 侦听值的变化，调用接口获取筛选数据
  console.log(serveValue.value, resourceValue.value, operateValue.value, searchValue.value, '搜索的数据');
});

function handleSearchChange(value, target) {
  stateRefs[target].value = value;
}

function handleAsideClick(id) {
  console.log(id, '这里根据id展示表格数据');
}
function toggleTable() {
  isTableVisible.value  = !isTableVisible.value;
}

function handleRenewal(row) {
  isShowRenewal.value = true;
}

function handleHandover(row) {
  isShowHandover.value = true;
}

function handleRemove(row) {
  isShowRemove.value = true;
}

/**
 * 当前页全选事件
 */
function handleSelectAll(val) {
  selectList.value = [];
  if (val.checked) {
    projectTable.value.forEach((item) => {
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
  refTable.value.toggleAllSelection();
  // 调用接口获取全部数据后
  selectList.value = projectTable.value.map((item) => item.id);
}
/**
 * 清除选择
 */
function handleClear() {
  refTable.value.clearSelection();
  selectList.value = [];
}
</script>

<style lang="scss" scoped>

.collapse-enter-active, .collapse-leave-active {
  transition: height 0.3s ease-in-out, opacity 0.3s ease-in-out;
}
.collapse-enter-from, .collapse-leave-to {
  opacity: 0;
}
.collapse-enter-to, .collapse-leave-from {
  opacity: 1;
}

.manage{
  width: 100%;
  height: 100%;
  overflow: hidden;

  .manage-search{
    display: flex;
    width: 100%;
    height: 64px;
    background: #FFFFFF;
    padding: 16px 24px;
    box-shadow: 0 2px 4px 0 #1919290d;

    .multi-search{
      flex: 1;
    }
  }

  .manage-article{
    height: calc(100% - 104px);
    display: flex;
    margin: 16px 0 24px 24px;

    .manage-aside{
      width: 230px;
      padding: 8px 0 0;
      background: #FFFFFF;
      box-shadow: 0 2px 4px 0 #1919290d;
    }

    .manage-content{
      flex: 1;
      margin-left: 16px;
      overflow-y: auto;

      &::-webkit-scrollbar-thumb {
        background-color: #c4c6cc !important;
        border-radius: 5px !important;
        &:hover {
          background-color: #979ba5 !important;
        }
      }
      &::-webkit-scrollbar {
        width: 8px !important;
        height: 8px !important;
      }

      .manage-content-btn{
        margin-bottom: 10px;

        .bk-button{
          margin-right: 8px
        }
      }

      .manage-content-project{
        max-height: 300px;
        margin-bottom: 15px;
        background: #FFFFFF;
        padding: 16px 24px;
        box-shadow: 0 2px 4px 0 #1919290d;
      }

      .manage-content-resource{
        background: #FFFFFF;
        padding: 16px 24px;
        box-shadow: 0 2px 4px 0 #1919290d;
      }

      .project-group {
        font-family: MicrosoftYaHei-Bold;
        font-weight: 700;
        font-size: 14px;
        color: #63656E;
        letter-spacing: 0;
        line-height: 22px;
      }

      .project-group-table{
        width: 100%;

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

        ::v-deep .bk-collapse-content {
          padding: 5px 0 !important;
        }

        .group-title {
          width: 100%;
          height: 26px;
          line-height: 26px;
          margin-top: 16px;
          padding-left: 10px;
          margin-bottom: 4px;
          background: #EAEBF0;
          border-radius: 2px;
          font-family: MicrosoftYaHei;
          font-size: 12px;
          color: #313238;
          cursor: pointer;
        }

        .group-num {
          display: inline-block;
          width: 23px;
          height: 16px;
          line-height: 16px;
          background: #F0F1F5;
          border-radius: 2px;
          font-size: 12px;
          color: #979BA5;
          letter-spacing: 0;
          text-align: center;
        }

        .operation-btn {
          display: flex;
          justify-content: space-around;
        }
      }
    }
  }
}

.renewal-dialog {

  .is-required{
    position: relative;
  }
  .is-required:after {
    position: absolute;
    top: 0;
    width: 14px;
    color: #ea3636;
    text-align: center;
    content: "*";
  }

  .remove-text {
    margin: 12px 0;

    span {
      display: inline-block;
      min-width: 68px;
      padding-right: 14px;
    }
  }
}

.remove-dialog{
  .dialog-header {
    display: inline-block;
    padding-left: 17px;
    margin-left: 17px;
    border-left: 1px solid #C4C6CC;
    font-family: MicrosoftYaHei;
    font-size: 12px;
    color: #63656E;
    letter-spacing: 0;
  }

}

.dialog-header-text {
  font-family: MicrosoftYaHei;
  font-size: 20px;
  color: #313238;
  font-weight: 600;
}

.remove-text {
  font-family: MicrosoftYaHei;
  font-size: 12px;
  color: #313238;
  line-height: 20px;
  span {
    color: #63656E;
  }
}
</style>
