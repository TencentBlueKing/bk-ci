<template>
  <bk-loading class="aside" :loading="manageAsideStore.isLoading">
    <div class="aside-header">
      {{t("组织/用户")}}
      <div class="aside-right">
        <template v-if="isBatchOperate">
          <span class="refresh" @click="goBatchOperate">{{ t("退出批量操作") }}</span>
        </template>
        <template v-else>
          <img class="edit-icon" src="../../../svg/batch-edit.svg" @click="goBatchOperate">
          <span class="refresh" @click="refresh">
            <spinner v-if="syncStatus === 'PENDING'" class="manage-icon" />
            <i v-else class="manage-icon manage-icon-refresh"></i>
            {{ syncStatus === 'PENDING' ? t('同步中') : t('刷新') }}
          </span>
        </template>
      </div>
    </div>
    <div ref="groupWrapperRef" class="group-wrapper">
      <div v-if="isBatchOperate"  class="label-item select-all">
        <bk-checkbox
          class="checkbox"
          v-model="selectAll"
          @change="handleBatchAll"
        >{{ t("全选") }}</bk-checkbox>
        <bk-button
          size="small"
          @click="handleOpenbatchDialog"
        >
          {{ t("批量移出项目(X)", [checkedMembers.length]) }}
        </bk-button>
      </div>
      <div
        :class="{'user-group-active': activeTab == item.id }"
        class="user-group-item"
        v-for="item in memberList"
        :key="item.id"
      >
        <bk-checkbox
          class="checkbox"
          v-if="isBatchOperate"
          v-model="item.checked"
          @change="handleCheckChange(item)"
        />
        <MemberItem
          :member="item"
          :activeTab="activeTab"
          :isBatchOperate="isBatchOperate"
          @handle-removal="handleRemoval"
          @handle-click="handleClick"
        />
      </div>
    </div>

    <bk-pagination
      class="pagination"
      v-model="current"
      align="center"
      :count="memberPagination.count"
      :limit="20"
      small
      :show-limit="false"
      :show-total-count="false"
      @change="pageChange"
    />
  </bk-loading>

  <bk-dialog
    :is-show="isShowHandOverDialog"
    :width="640"
    @closed="handOverClose"
    :style="{ '--dialog-top-translateY': `translateY(${dialogTopOffset}px)` }"
  >
    <template #header>
      <p v-if="!isBatchOperate">
        {{ t("移出项目") }}
        <span class="dialog-header"> {{t("移出用户")}}： {{ removeUser?.id }} ({{ removeUser?.name }}) </span>
      </p>
      <p v-else>{{ t('批量移出项目') }}</p>
    </template>
    <bk-loading :loading="removeCheckLoading">
      <template #default>
        <template v-if="removeMemberChecked">
          <p v-if="!isBatchOperate" class="remove-tips">{{ t('XXX拥有的权限均已过期，无需交接，确定移出用户并清理过期权限吗？', [`${removeUser?.id} (${removeUser?.name})`]) }}</p>
          <p v-else class="remove-tips">{{ t('用户拥有的权限均已过期，无需交接，确定移出用户并清理过期权限吗？') }}</p>
        </template>
        <template v-else>
          <div class="dialog">
            <p class="text-tag">
              <i class="manage-icon manage-icon-info-line"></i>
              <span>
                {{t("将用户移出项目时需指定移交人，确认后将自动移交有效的权限/授权；已过期权限不交接，将自动清理")}}
              </span>
              <span v-if="isBatchOperate">{{ t("组织的权限不交接，将自动清理") }}</span>
            </p>

            <ul
              v-if="isBatchOperate"
              class="select-list"
              :style="{ 'max-height': `${ulMaxHeight}px` }"
            >
              <p class="title">{{ t("已选择以下X个组织/用户", [checkedMembers.length]) }}</p>
              <li v-for="member in checkedMembers">
                <MemberItem
                  :member="member"
                  :isBatchOperate="isBatchOperate"
                />
              </li>
            </ul>

            <bk-form
              ref="formRef"
              :rules="rules"
              :model="handOverForm"
              form-type="vertical"
            >
              <bk-form-item
                required
                :label="t('移交人')"
                property="name"
                labelWidth=""
              >
                <project-user-selector
                  ref="tagInput"
                  @change="handleChangeOverFormName"
                  @removeAll="handOverInputClear"
                >
                </project-user-selector>
              </bk-form-item>
            </bk-form>
    
            <p class="verifying">
              <span v-if="isChecking">
                <Spinner class="check-checking-icon" />
                {{ t("正在校验授权") }}
              </span>
              <span v-if="isAuthorizedSuccess">
                <Success class="check-success-icon" />
                {{t("授权校验通过")}}
              </span>
            </p>
    
            <div v-if="isHandOverfail" class="hand-over-fail">
              <p class="err-text">
                <p class="deal">
                  <i class="manage-icon manage-icon-close"></i>
                  <i18n-t keypath="检测到以下授权将无法移交给X，请先前往「授权管理」单独处理" tag="div" >
                    <span> {{ handOverForm.name }} </span>
                  </i18n-t>
                </p>
                <p class="blue-text" @click="refreshHandOverfail">
                  <i class="manage-icon manage-icon-refresh"></i>
                  <span>{{t("刷新")}}</span>
                </p>
              </p>
              <div class="hand-over-table-group" v-for="item in overTable" :key="item.id">
                <p class="hand-over-table-item">
                  {{item.name}}({{ item.resourceType }})
                </p>
                <p class="blue-text" @click="goAuthorization(item)">
                  <i class="manage-icon manage-icon-jump"></i>
                  <span>{{t("前往处理")}}</span>
                </p>
              </div>
            </div>
          </div>
        </template>
      </template>
    </bk-loading>
    <template #footer>
      <bk-button
        theme="primary"
        @click="handConfirm('user')"
        :loading="manageAsideStore.btnLoading"
        :disabled="!isAuthorizedSuccess && !removeMemberChecked"
      >
        {{ removeMemberChecked ? t('确定') : t("移交并移出")}}
      </bk-button>
      <bk-button
        class="btn-margin"
        @click="handOverClose"
      >
        {{ t('取消') }}
      </bk-button>
    </template>
  </bk-dialog>

  <bk-dialog
    :width="480"
    theme="primary"
    dialog-type="confirm"
    :confirm-text="t('关闭')"
    :is-show="isShowPersonDialog"
    @closed="handlePersonClose"
    @confirm="handlePersonClose"
  >
    <template #header>
      {{t("人员列表")}}
      <span class="dialog-header"> {{ removeUser?.name }} </span>
    </template>
    <template #default>
        <bk-table
          max-height="320"
          :data="personList"
          :loading="tableLoading"
          show-overflow-tooltip
          class="person-table"
        >
          <bk-table-column :label="t('用户')" prop="person" />
        </bk-table>
    </template>
  </bk-dialog>

  <bk-dialog
    :width="450"
    header-align="center"
    footer-align="center"
    :is-show="isShowRemoveDialog"
    @closed="handOverClose"
  >
    <template #header>
      <img src="@/css/svg/warninfo.svg" class="manage-icon-tishi">
      <p class="dialog-header-text">
        <span v-if="!isBatchOperate">{{t("确认将组织移出本项目吗")}}？</span>
        <span v-else>{{t("确认将以下组织移出本项目吗")}}？</span>
        
      </p>
    </template>
    <template #default>
        <p v-if="!isBatchOperate" class="remove-text">
          <span>{{t("待移出组织")}}：</span> {{ removeUser?.name }}
        </p>
        <ul
          v-else
          class="select-list"
          :style="{ 'max-height': `${ulMaxHeight}px` }"
        >
          <p class="title">{{ t("已选择以下X个组织/用户", [checkedMembers.length]) }}</p>
          <li v-for="member in checkedMembers">
            <MemberItem
              :member="member"
              :isBatchOperate="isBatchOperate"
            />
          </li>
        </ul>
    </template>
    <template #footer>
      <bk-button
        theme="danger"
        @click="handConfirm('department')"
        :loading="manageAsideStore.btnLoading"
      >
        {{t("确认移出")}}
      </bk-button>
      <bk-button
        class="btn-margin"
        @click="handOverClose"
      >
        {{t("关闭")}}
      </bk-button>
    </template>
  </bk-dialog>
</template>

<script setup name="ManageAside">
import http from '@/http/api';
import { Message } from 'bkui-vue';
import { useI18n } from 'vue-i18n';
import { storeToRefs } from 'pinia';
import { useRoute } from 'vue-router';
import MemberItem from './MemberItem.vue';
import useManageAside from "@/store/manageAside";
import { Success, Spinner } from 'bkui-vue/lib/icon';
import ProjectUserSelector from '@/components/project-user-selector'
import { ref, defineProps, defineEmits, computed, defineExpose, onMounted, onUnmounted, watch } from 'vue';

const props = defineProps({
  memberList: {
    type: Array,
    default: () => [],
  },
  personList: {
    type: Array,
    default: () => [],
  },
  tableLoading: Boolean,
  activeTab: String,
});
const emit = defineEmits(['handleClick', 'pageChange', 'getPersonList', 'removeConfirm', 'refresh', 'handleSelectAll', 'updateMemberList']);
defineExpose({
  handOverClose,
});

const { t } = useI18n();
const route = useRoute();
const manageAsideStore = useManageAside();
const current = ref(1);
const isShowHandOverDialog = ref(false);
const removeCheckLoading = ref(false);
const formRef = ref(null);
const isHandOverfail = ref(false);
const isShowPersonDialog = ref(false);
const isShowRemoveDialog = ref(false);
const isAuthorizedSuccess = ref(false);
const syncStatus = ref('SUCCESS');
const timer = ref(null);
const groupWrapperRef = ref(null);
const removeMemberChecked = ref(false);
function getHandOverForm(){
  return {
    id: '',
    name: '',
    type: '',
  }
}
const handOverForm = ref(getHandOverForm());
const rules = {
  name: [
    { required: true, message: t('请输入移交人'), trigger: 'change' },
  ],
};
const {
  memberPagination,
} = storeToRefs(manageAsideStore);

const projectId = computed(() => route.params?.projectCode || route.query?.projectCode);
const removeUser = ref(null);
const isChecking = ref(false);
const overTable = ref([]);
const userListData = ref([]);
const isBatchOperate = ref(false);
const dialogTopOffset = ref();
const ulMaxHeight = computed(() => window.innerHeight * 0.8 - 256);
const tagInput = ref(null);
const checkedMembers = ref([]);
const selectAll = ref(false);

watch(
  [
    () => props.memberList,
    () => checkedMembers.value
  ],
  ([memberList, checkedMembers]) => {
    const currentPageIds = new Set(memberList?.map(member => member.id));
    const current = checkedMembers?.filter(item => currentPageIds.has(item.id));

    selectAll.value = memberList.length === current?.length
  },
  {
    immediate: true,
    deep: true
  }
);

watch(isShowHandOverDialog, (newVal) => {
  if (newVal) {
    const ITEM_HEIGHT = 30
    const DIALOG_EXTRA_HEIGHT = 256
    const totalListHeight = checkedMembers.value.length * ITEM_HEIGHT
    const listHeight = Math.min(totalListHeight, ulMaxHeight.value)
    dialogTopOffset.value = -Math.round((listHeight + DIALOG_EXTRA_HEIGHT) / 2)
  }
})

function handleClick(item) {
  emit('handleClick', item);
}
function pageChange(current) {
  groupWrapperRef.value?.scrollTo(0, 0)
  emit('pageChange', current, projectId.value, checkedMembers.value);
}
async function handleRemoval(item) {
  removeUser.value = item;
  if (item.type === 'department') {
    isShowRemoveDialog.value = true;
  } else {
    handOverForm.value && (Object.assign(handOverForm.value, getHandOverForm()));
    isShowHandOverDialog.value = true;
    await removeMemberFromProjectCheck([item]);
    formRef.value?.clearValidate();
  }
}

async function removeMemberFromProjectCheck (payload) {
  const targetMembers = isBatchOperate.value ? checkedMembers.value : payload;
  try {
    removeCheckLoading.value = true;
    removeMemberChecked.value = await http.removeMemberFromProjectCheck(projectId.value, targetMembers);
    removeCheckLoading.value = false;
  } catch (e) {
    removeCheckLoading.value = false;
    console.error(e)
  }
}
/**
 *  移出项目弹窗关闭
 */
function handOverClose() {
  isShowHandOverDialog.value = false;
  isShowRemoveDialog.value = false;
  handOverInputClear();
  tagInput.value?.removeAll();
}
/**
 *  移出项目弹窗提交
 */
async function handConfirm (flag) {
  const targetMembers = isBatchOperate.value ? checkedMembers.value : [removeUser.value]
  try {
    if (flag === 'user') {
      if (removeMemberChecked.value) {
        emit('removeConfirm', isBatchOperate.value, targetMembers, {});
      } else {
        const isValidate = await formRef.value?.validate();
        if(!isValidate) return;
        emit('removeConfirm', isBatchOperate.value, targetMembers, handOverForm.value);
      }
    } else {
      emit('removeConfirm', isBatchOperate.value, targetMembers, {});
    }
  } catch (error) {} finally {
    tagInput.value?.removeAll()
  }
}
function handOverInputClear(){
  isChecking.value = false;
  isAuthorizedSuccess.value = false;
  isHandOverfail.value = false;
}
async function handleChangeOverFormName ({list, userList}){
  if(!list){
    handOverInputClear();
    return;
  }
  userListData.value = userList;
  handOverForm.value = userList?.find(i => i.id === list[0]);
  const checkedMemberFroms = checkedMembers.value.map(item => item.id)
  const handoverFroms = isBatchOperate.value ? checkedMemberFroms : [removeUser.value?.id]

  if(handoverFroms.includes(handOverForm.value?.id)){
    Message({
      theme: 'error',
      message: t('目标对象和交接人不允许相同。')
    });
    return
  }

  const params = {
    projectCode: projectId.value,
    handoverFroms,
    handoverTo: handOverForm.value?.id,
    preCheck: true,
    checkPermission: true
  }
  if (!params.handoverTo) return
  isChecking.value = true;
  isAuthorizedSuccess.value = false;
  isHandOverfail.value = false;
  try {
    const reset = await http.resetAllResourceAuthorization(projectId.value, params)
    if (reset.length) {
      overTable.value = reset;
      isHandOverfail.value = true;
      isChecking.value = false;
      isAuthorizedSuccess.value = false;
    } else {
      isChecking.value = false;
      isAuthorizedSuccess.value = true;
    }
  } catch (error) {
    handOverInputClear();
  }
}
async function refresh () {
  if (syncStatus.value === 'PENDING') return
  try {
    await http.syncGroupAndMember(projectId.value);
    await getSyncStatus();
    
  } catch (error) {
    Message({
      theme: 'error',
      message: err.message
    });
  }
}

async function getSyncStatus () {
  try {
    syncStatus.value = await http.getSyncStatusOfAllMember(projectId.value);
    if (syncStatus.value === 'PENDING') {
      timer.value = setTimeout(() => {
        getSyncStatus()
      }, 10000);
    } else {
      if (timer.value) {
        emit('refresh')
        Message({
          theme: 'success',
          message: t('同步成功')
        })
      }
    }
  } catch (e) {
    console.error(e)
  }
}
/**
 * 移出失败刷新数据
 */
function refreshHandOverfail () {
  const param = {
    list: [handOverForm.value.id],
    userList: userListData.value,
  }
  handleChangeOverFormName(param);
}
function goAuthorization(item) {
  const { resourceType, memberIds} = item
  window.open(`${location.origin}/console/manage/${projectId.value}/permission?resourceType=${resourceType}&userId=${memberIds}`, '_blank')
}
/**
 * 获取人员列表数据
 */
function handleShowPerson (item) {
  isShowPersonDialog.value = true;
  removeUser.value = item;
  emit('getPersonList',item, projectId.value)
}
function handlePersonClose () {
  isShowPersonDialog.value = false;
}

function goBatchOperate() {
  isBatchOperate.value = !isBatchOperate.value;
  checkedMembers.value = [];
  emit('handleSelectAll', false);
}

function handleBatchAll(value) {
  if (value) {
    const memberIds = new Set(checkedMembers.value.map(item => item.id));
    const newMembers = props.memberList.filter(item => !memberIds.has(item.id));
    checkedMembers.value = [...checkedMembers.value, ...newMembers];
  } else {
    const memberIds = props.memberList.map(m => m.id);
    checkedMembers.value = checkedMembers.value.filter(m =>!memberIds.includes(m.id));
  }
  emit('handleSelectAll', value);
}

async function handleOpenbatchDialog() {
  if (!checkedMembers.value.length) {
    Message({
      theme: 'error',
      message: t('请选择组织/用户')
    });
    return
  }
  const allAreGroups = checkedMembers.value.every(member => member.type === 'department');
  if (allAreGroups) {
    isShowRemoveDialog.value = true;
  } else {
    isShowHandOverDialog.value = true;
    await removeMemberFromProjectCheck();
  }
}
function handleCheckChange(item) {
  if (item.checked) {
    if (!checkedMembers.value.find(m => m.id === item.id)) {
      checkedMembers.value.push(item);
    }
  } else {
    checkedMembers.value = checkedMembers.value.filter(m => m.id !== item.id);
  }
  emit('updateMemberList', item)
}

onMounted(() => {
  getSyncStatus()
})
onUnmounted(() => {
  clearTimeout(timer.value);
})
</script>

<style lang="scss" scoped>
.aside {
  position: relative;
  height: 100%;
  overflow-y: scroll;
  overflow: hidden;
}
.aside-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding: 0 10px 0 18px;
  height: 40px;
  font-family: MicrosoftYaHei-Bold;
  font-weight: 700;
  font-size: 14px;
  color: #63656E;
  letter-spacing: 0;
  line-height: 22px;

  .aside-right {
    display: flex;
    align-items: center;

    .edit-icon {
      width: 14px;
      height: 14px;
      cursor: pointer;
    }

    .refresh{
      display: flex;
      align-items: center;
      margin-left: 10px;
      font-size: 12px;
      font-weight: 400;
      color: #3A84FF;
      cursor: pointer;
  
      .manage-icon {
        margin-right: 5px;
      }
    }
  }
}
.group-wrapper {
  overflow: hidden;
  overflow-y: scroll;
  height: calc(100% - 100px);
  &::-webkit-scrollbar-thumb {
    background-color: #c4c6cc !important;
    border-radius: 5px !important;
    &:hover {
      background-color: #979ba5 !important;
    }
  }
  &::-webkit-scrollbar {
    width: 4px !important;
    height: 4px !important;
  }
}
.select-all {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 0 18px 4px;
  font-size: 12px;
  color: #63656E;
  height: 50px;
  border-bottom: 1px solid #e4e5eb;
}
.checkbox {
  margin-right: 10px;
}
.select-list {
  margin-bottom: 24px;
  border: 1px solid #f1f1f5;
  border-radius: 2px;
  overflow-y: auto;

  .title {
    background-color: #EAEBF0;
    font-size: 14px;
    font-weight: 700;
    padding: 0 18px;
    height: 32px;
    line-height: 32px;
  }

  li {
    padding: 5px 18px;
    font-size: 12px;
    // background-color: #fafbfd;
    &:hover {
      background-color: #fafbfd;
    }
  }
}
.user-group-item {
  display: flex;
  width: 100%;
  padding: 0 18px;
  height: 40px;
  align-items: center;flex: 1;
  font-size: 14px;
  color: #63656E;
  cursor: pointer;
}

.user-group-active {
  background-color: #E1ECFF !important;
  border-right: 2px solid #3A84FF;
}

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
.remove-tips {
  padding: 15px 0;
}
.dialog {
  .bk-form-item{
    margin-bottom: 6px !important;
  }
 
  .text-tag {
    width: 100%;
    line-height: 30px;
    padding-left: 5px;
    margin-bottom: 16px;
    background: #F0F8FF;
    border: 1px solid #C5DAFF;
    border-radius: 2px;

    .manage-icon-info-line{
      font-size: 14px;
      color: #3A84FF;
    }

    span {
      vertical-align: middle;
      font-family: MicrosoftYaHei;
      font-size: 12px;
      color: #63656E;
    }
  }

  .verifying{
    font-size: 12px;
    color: #63656E;

    span {
      display: flex;
      vertical-align: middle;
    }
  }

  .hand-over-fail {
    border-top: 1px solid #DCDEE5;
    margin: 25px 0;

    .err-text {
      display: flex;
      justify-content: space-between;
      margin-top: 8px;
      color: #63656e;
      font-size: 12px;

      .manage-icon-close {
        font-size: 14px;
        color: #EA3636;
        margin-right: 5px;
      }

      span {
        font-weight: 700;
        vertical-align: middle;
      }

      .deal {
        display: flex;
        align-items: center;
        line-height: 14px;
      }
    }


    .hand-over-table-group{
      display: flex;
      justify-content: space-between;
      width: 100%;
      height: 32px;
      line-height: 32px;
      margin-top: 12px;
      padding: 0 16px;
      background: #EAEBF0;
      border-radius: 2px;
    }
    
    .hand-over-table-item {
      font-family: MicrosoftYaHei;
      font-size: 14px;
      color: #313238;
    }

    .blue-text {
      display: flex;
      align-items: center;
      cursor: pointer;
      color: #3A84FF;
      font-size: 12px;

      .manage-icon {
        margin-right: 6px;
      }
      
      span{
        vertical-align: middle;
      }
    }
  }
}

.person-table {
  margin-bottom: 15px;
}

.dialog-header-text {
  font-family: MicrosoftYaHei;
  font-size: 20px;
  color: #313238;
  letter-spacing: 0;
  text-align: center;
  line-height: 32px;
  font-weight: 700;
}

.manage-icon-tishi {
  width: 42px;
  height: 42px;
}

.remove-text {
  font-family: MicrosoftYaHei;
  font-size: 12px;
  color: #313238;
  line-height: 20px;
  text-align: center;

  span {
    color: #63656E;
  }
}

.pagination{
  position: absolute;
  bottom: 16px;
  padding-top: 16px;
  width: 100%;
  border-top: 1px solid #DCDEE5;
}

.btn-margin{
  margin-left: 10px
}

.check-success-icon {
  color: #2DCB56;
  margin-right: 5px;
}

.check-checking-icon {
  color: #3A84FF;
  margin-right: 5px;
}
</style>

<style>
.user-group-item {
  &:hover .more-icon{
    display: block;
    padding: 1px;
  }

  &:hover {
    background-color: #eaebf0;
  }
}
.user-group-active {
  background-color: #E1ECFF !important;
  border-right: 2px solid #3A84FF;

  p{
    color: #3A84FF;
  }

  .group-icon {
    filter: invert(100%) sepia(0%) saturate(90%) hue-rotate(180deg) brightness(90%) contrast(180%);
  }
}
</style>