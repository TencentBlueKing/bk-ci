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
                max-height="464"
                :fixed-bottom="fixedBottom"
                :data="projectTable"
                show-overflow-tooltip
                :pagination="pagination"
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
                <template #fixedBottom v-if="!pagination">
                  <div class="prepend">
                    剩余{{ 22 }} 条数据，
                    <span @click="handleLoadMore"> 加载更多 </span>
                  </div>
                </template>
                <bk-table-column type="selection" :min-width="30" width="30" align="center" v-if="isShowOperation" />
                <bk-table-column label="用户组" prop="groupName">
                </bk-table-column>
                <bk-table-column label="用户描述" prop="groupDesc" />
                <bk-table-column label="有效期" prop="validityPeriod" />
                <bk-table-column label="加入时间" prop="joinedTime" />
                <bk-table-column label="加入方式/操作人" prop="operateSource">
                  <template #default="{row}">
                    {{ row.operateSource }}/{{ row.operator }}
                  </template>
                </bk-table-column>
                <bk-table-column label="操作" v-if="isShowOperation">
                  <template #default="{row}">
                    <div class="operation-btn">
                      <span
                        v-bk-tooltips="{
                          content: '唯一管理员，不可移出。请添加新的管理员后再移出。',
                          disabled: !row.removeMemberButtonControl
                        }"
                      >
                        <bk-button
                          text
                          theme="primary"
                          :disabled="row.removeMemberButtonControl"
                          @click="handleRenewal(row)"
                        >续期</bk-button>
                      </span>
                      <span
                        v-bk-tooltips="{
                          content: '通过用户组加入，不可直接移出。如需调整，请编辑用户组。',
                          disabled: !row.removeMemberButtonControl
                        }"
                      >
                        <bk-button
                          text
                          theme="primary"
                          :disabled="row.removeMemberButtonControl"
                          @click="handleHandover(row)"
                        >移交</bk-button>
                      </span>
                      <span
                        v-bk-tooltips="{
                          content: '唯一管理员，不可移出。请添加新的管理员后再移出。',
                          disabled: !row.removeMemberButtonControl
                        }"
                      >
                        <bk-button
                          text
                          theme="primary"
                          :disabled="row.removeMemberButtonControl"
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
              max-height="464"
              :data="projectTable"
              :pagination="pagination"
              show-overflow-tooltip
            >
              <template #prepend>
                <div v-if="selectList.length" class="prepend">
                  已选择 {{ selectList.length }} 条数据，
                  <span @click="handleSelectAllData"> 选择全量数据 {{ total }} 条 </span>
                  &nbsp; | &nbsp;
                  <span @click="handleClear">清除选择</span>
                </div>
              </template>
              <template #appendBottom v-if="!pagination">
                <div class="prepend" @click="handleLoadMore">
                  剩余{{ 22 }} 条数据，
                  <span> 加载更多 </span>
                </div>
              </template>
              <bk-table-column type="selection" :min-width="30" width="30" align="center" v-if="isShowOperation" />
              <bk-table-column label="用户组" prop="groupName">
              </bk-table-column>
              <bk-table-column label="用户描述" prop="groupDesc" />
              <bk-table-column label="有效期" prop="validityPeriod" />
              <bk-table-column label="加入时间" prop="joinedTime" />
              <bk-table-column label="加入方式/操作人" prop="operateSource">
                <template #default="{row}">
                  {{ row.operateSource }}/{{ row.operator }}
                </template>
              </bk-table-column>
              <bk-table-column label="操作" v-if="isShowOperation">
                <template #default="{row}">
                  <div class="operation-btn">
                    <span
                      v-bk-tooltips="{
                        content: '唯一管理员，不可移出。请添加新的管理员后再移出。',
                        disabled: !row.removeMemberButtonControl
                      }"
                    >
                      <bk-button
                        text
                        theme="primary"
                        :disabled="row.removeMemberButtonControl"
                        @click="handleRenewal(row)"
                      >续期</bk-button>
                    </span>
                    <span
                      v-bk-tooltips="{
                        content: '通过用户组加入，不可直接移出。如需调整，请编辑用户组。',
                        disabled: !row.removeMemberButtonControl
                      }"
                    >
                      <bk-button
                        text
                        theme="primary"
                        :disabled="row.removeMemberButtonControl"
                        @click="handleHandover(row)"
                      >移交</bk-button>
                    </span>
                    <span
                      v-bk-tooltips="{
                        content: '唯一管理员，不可移出。请添加新的管理员后再移出。',
                        disabled: !row.removeMemberButtonControl
                      }"
                    >
                      <bk-button
                        text
                        theme="primary"
                        :disabled="row.removeMemberButtonControl"
                        @click="handleRemove(row)"
                      >移出</bk-button>
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
              max-height="522"
              :data="projectTable"
              show-overflow-tooltip
            >
              <template #prepend>
                <div v-if="selectList.length" class="prepend">
                  已选择 {{ selectList.length }} 条数据，
                  <span @click="handleSelectAllData"> 选择全量数据 {{ total }} 条 </span>
                  &nbsp; | &nbsp;
                  <span @click="handleClear">清除选择</span>
                </div>
              </template>
              <template #appendBottom v-if="!pagination">
                <div class="prepend" @click="handleLoadMore">
                  剩余{{ 22 }} 条数据，
                  <span> 加载更多 </span>
                </div>
              </template>
              <bk-table-column type="selection" :min-width="30" width="30" align="center" v-if="isShowOperation" />
              <bk-table-column label="用户组" prop="groupName">
              </bk-table-column>
              <bk-table-column label="用户描述" prop="groupDesc" />
              <bk-table-column label="有效期" prop="validityPeriod" />
              <bk-table-column label="加入时间" prop="joinedTime" />
              <bk-table-column label="加入方式/操作人" prop="operateSource">
                <template #default="{row}">
                  {{ row.operateSource }}/{{ row.operator }}
                </template>
              </bk-table-column>
              <bk-table-column label="操作" v-if="isShowOperation">
                <template #default="{row}">
                  <div class="operation-btn">
                    <span
                      v-bk-tooltips="{
                        content: '唯一管理员，不可移出。请添加新的管理员后再移出。',
                        disabled: !row.removeMemberButtonControl
                      }"
                    >
                      <bk-button
                        text
                        theme="primary"
                        :disabled="row.removeMemberButtonControl"
                        @click="handleRenewal(row)"
                      >续期</bk-button>
                    </span>
                    <span
                      v-bk-tooltips="{
                        content: '通过用户组加入，不可直接移出。如需调整，请编辑用户组。',
                        disabled: !row.removeMemberButtonControl
                      }"
                    >
                      <bk-button
                        text
                        theme="primary"
                        :disabled="row.removeMemberButtonControl"
                        @click="handleHandover(row)"
                      >移交</bk-button>
                    </span>
                    <span
                      v-bk-tooltips="{
                        content: '唯一管理员，不可移出。请添加新的管理员后再移出。',
                        disabled: !row.removeMemberButtonControl
                      }"
                    >
                      <bk-button
                        text
                        theme="primary"
                        :disabled="row.removeMemberButtonControl"
                        @click="handleRemove(row)"
                      >移出</bk-button>
                    </span>
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

<script setup> import { ref, defineProps, defineEmits } from 'vue';

const total = ref(0);
const refTable = ref(null);
const selectList = ref([]);
const activeIndex =  ref(true);
const projectTable = ref([{
  groupId: 1,
  groupName: '11',
  groupDesc: 'kjkjkjk',
  validityPeriod: '0505',
  joinedTime: '08-18',
  operateSource: '加入组',
  operator: '张三',
  removeMemberButtonControl: true,
},
{
  groupId: 2,
  groupName: '22',
  groupDesc: 'kjkjkjk',
  validityPeriod: '0505',
  joinedTime: '08-18',
  operateSource: '加入组',
  operator: '张三',
  removeMemberButtonControl: false,
}, {
  groupId: 3,
  groupName: '33',
  groupDesc: 'kjkjkjk',
  validityPeriod: '0505',
  joinedTime: '08-18',
  operateSource: '加入组',
  operator: '张三',
  removeMemberButtonControl: false,
},
{
  groupId: 4,
  groupName: '44',
  groupDesc: 'kjkjkjk',
  validityPeriod: '0505',
  joinedTime: '08-18',
  operateSource: '加入组',
  operator: '张三',
  removeMemberButtonControl: false,
}, {
  groupId: 5,
  groupName: '55',
  groupDesc: 'kjkjkjk',
  validityPeriod: '0505',
  joinedTime: '08-18',
  operateSource: '加入组',
  operator: '张三',
  removeMemberButtonControl: false,
},
{
  groupId: 6,
  groupName: '66',
  groupDesc: 'kjkjkjk',
  validityPeriod: '0505',
  joinedTime: '08-18',
  operateSource: '加入组',
  operator: '张三',
  removeMemberButtonControl: false,
}, {
  groupId: 7,
  groupName: '77',
  groupDesc: 'kjkjkjk',
  validityPeriod: '0505',
  joinedTime: '08-18',
  operateSource: '加入组',
  operator: '张三',
  removeMemberButtonControl: false,
},
{
  groupId: 8,
  groupName: '88',
  groupDesc: 'kjkjkjk',
  validityPeriod: '0505',
  joinedTime: '08-18',
  operateSource: '加入组',
  operator: '张三',
  removeMemberButtonControl: false,
}, {
  groupId: 9,
  groupName: '99',
  groupDesc: 'kjkjkjk',
  validityPeriod: '0505',
  joinedTime: '08-18',
  operateSource: '加入组',
  operator: '张三',
  removeMemberButtonControl: false,
},
{
  groupId: 10,
  groupName: '1010',
  groupDesc: 'kjkjkjk',
  validityPeriod: '0505',
  joinedTime: '08-18',
  operateSource: '加入组',
  operator: '张三',
  removeMemberButtonControl: false,
},
{
  groupId: 11,
  groupName: '1111',
  groupDesc: 'kjkjkjk',
  validityPeriod: '0505',
  joinedTime: '08-18',
  operateSource: '加入组',
  operator: '张三',
  removeMemberButtonControl: false,
}]);
const isTableVisible = ref(false);
const fixedBottom = {
  position: 'relative',
  height: 42,
};
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
      selectList.value.push(item.groupId);
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
    selectList.value.push(val.row.groupId);
  } else {
    selectList.value = selectList.value.filter(item => item !== val.row.groupId);
  }
};
/**
 * 全量数据选择
 */
function handleSelectAllData() {
  refTable.value.toggleAllSelection();
  // 调用接口获取全部数据后
  selectList.value = projectTable.value.map(item => item.groupId);
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
 * 加载更多
 */
function handleLoadMore() {
  console.log('点击加载更多');
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
  max-height: 630px;
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
  height: 100%;
  margin-bottom: 16px;

  .bk-table {
    border: 1px solid #dcdee5;
  }

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
