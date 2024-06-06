<template>
  <div>
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
                max-height="200"
                :data="projectTable"
                show-overflow-tooltip
                :border="['outer', 'row']"
                :pagination="pagination"
                row-key="id"
                :columns="columns"
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
                <bk-table-column label="操作" v-if="isShowOperation">
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
              :border="['outer', 'row']"
              :pagination="pagination"
              show-overflow-tooltip
            >
              <bk-table-column label="用户组" prop="usergroup" />
              <bk-table-column label="用户描述" prop="usergroup" />
              <bk-table-column label="有效期" prop="usergroup" />
              <bk-table-column label="加入时间" prop="usergroup" />
              <bk-table-column label="加入方式/操作人" prop="usergroup" />
              <bk-table-column label="操作" v-if="isShowOperation">
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
              <bk-table-column label="操作" v-if="isShowOperation">
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
</template>

<script setup>
// import { useI18n } from 'vue-i18n';
import { ref, defineProps, defineEmits } from 'vue';

const total = ref(0);
const refTable = ref(null);
const selectList = ref([]);
const activeIndex =  ref([0]);
const projectTable = ref([{
  id: 1,
  usergroup: 'asdsa',
  describe: 'kjkjkjk',
  validity: '0505',
  data: '08-18',
  type: '加入组',
  isUninstall: true,
},
{
  id: 2,
  usergroup: 'asdsa',
  describe: 'kjkjkjk',
  validity: '0505',
  data: '08-18',
  type: '加入组',
  isUninstall: false,
}, {
  id: 3,
  usergroup: 'asdsa',
  describe: 'kjkjkjk',
  validity: '0505',
  data: '08-18',
  type: '加入组',
  isUninstall: false,
},
{
  id: 4,
  usergroup: 'asdsa',
  describe: 'kjkjkjk',
  validity: '0505',
  data: '08-18',
  type: '加入组',
  isUninstall: false,
}]);
const isTableVisible = ref(true);
// const columns = ref([
//   {
//     type: 'selection',
//     width: 30,
//     align: 'center',
//   },
//   {
//     label: '用户组',
//     field: 'usergroup',
//   },
//   {
//     label: '用户描述',
//     field: 'describe',
//   },
//   {
//     label: '有效期',
//     field: 'validity',
//   },
//   {
//     label: '加入时间',
//     field: 'data',
//   },
//   {
//     label: '加入方式/操作人',
//     field: 'type',
//   },
//   {
//     label: '操作',
//     field: 'action',
//     render: ({ row }) => (
//       <div class="operation-btn">
//         <span v-bk-tooltips="{ content: '唯一管理员，不可移出。请添加新的管理员后再移出。', disabled: !row.isUninstall }">
//           <bk-button
//             text
//             theme="primary"
//             :disabled="row.isUninstall"
//             @click="handleRenewal(row)"
//           >续期</bk-button>
//         </span>
//         <span v-bk-tooltips="{ content: '通过用户组加入，不可直接移出。如需调整，请编辑用户组。', disabled: !row.isUninstall }">
//           <bk-button
//             text
//             theme="primary"
//             :disabled="row.isUninstall"
//             @click="handleHandover(row)"
//           >移交</bk-button>
//         </span>
//         <span v-bk-tooltips="{ content: '唯一管理员，不可移出。请添加新的管理员后再移出。', disabled: !row.isUninstall }">
//           <bk-button
//             text
//             theme="primary"
//             :disabled="row.isUninstall"
//             @click="handleRemove(row)"
//           >移出</bk-button>
//         </span>
//       </div>
//     ),
//   },
// ]);

defineProps({
  isShowOperation: {
    type: Boolean,
    default: true,
    required: true,
  },
  pagination: {
    type: Object,
  },
});
const emit = defineEmits(['renewal', 'handover', 'remove']);
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
    selectList.value = selectList.value.filter(item => item !== val.row.id);
  }
};
/**
 * 全量数据选择
 */
function handleSelectAllData() {
  refTable.value.toggleAllSelection();
  // 调用接口获取全部数据后
  selectList.value = projectTable.value.map(item => item.id);
}
/**
 * 清除选择
 */
function handleClear() {
  refTable.value.clearSelection();
  selectList.value = [];
}
/**
 * 续期按钮点击
 * @param row 行数据
 */
function handleRenewal(row) {
  emit('renewal', row);
}

/**
 * 移交按钮点击
 * @param row 行数据
 */
function handleHandover(row) {
  emit('handover', row);
}
/**
 * 移出按钮点击
 * @param row 行数据
 */
function handleRemove(row) {
  emit('remove', row);
}

/**
 * 动画显隐
 */
function toggleTable() {
  isTableVisible.value  = !isTableVisible.value;
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

.manage-content-project {
  max-height: 300px;
  margin-bottom: 15px;
  background: #FFFFFF;
  padding: 16px 24px;
  box-shadow: 0 2px 4px 0 #1919290d;
}

.manage-content-resource {
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

.project-group-table {
  width: 100%;

  .prepend {
    width: 100%;
    height: 32px;
    line-height: 32px;
    background: #F0F1F5;
    text-align: center;
    box-shadow: 0 -1px 0 0 #DCDEE5;

    span {
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
</style>
