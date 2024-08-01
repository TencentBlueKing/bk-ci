<template>
  <bk-loading class="manage" :loading="isLoading"  :zIndex="100">
    <div class="manage-search">
      <bk-search-select
        v-model="searchValue"
        :data="searchData"
        unique-select
        class="multi-search"
        value-behavior="need-key"
        :placeholder="filterTips"
        :get-menu-list="getMenuList"
        @search="handleSearch(searchValue)"
      />
    </div>
    <div class="manage-article" v-if="memberList.length">
      <div class="manage-aside">
        <manage-aside
          ref="manageAsideRef"
          :member-list="memberList"
          :person-list="personList"
          :table-loading="tableLoading"
          :active-tab="activeTab"
          @refresh="refresh"
          @handle-click="asideClick"
          @page-change="handleAsidePageChange"
          @get-person-list="handleShowPerson"
          @remove-confirm="asideRemoveConfirm"
        />
      </div>
      <div class="manage-content">
        <div class="manage-content-btn">
          <bk-button
            :disabled="!isPermission || asideItem?.departed"
            @click="batchOperator('renewal')"
            :loading="renewalLoading"
          >
            {{t("批量续期")}}
          </bk-button>
          <bk-button
            :disabled="!isPermission"
            @click="batchOperator('handover')"
            v-if="asideItem?.type==='user'"
            :loading="handoverLoading"
          >
            {{t("批量移交")}}
          </bk-button>
          <bk-button
            :disabled="!isPermission"
            @click="batchOperator('remove')"
            :loading="removerLoading"
          >
            {{t("批量移出")}}
          </bk-button>

          <i18n-t keypath="已选择X个用户组" tag="div" class="main-desc" v-if="selectedLength">
            <span class="desc-primary">{{ selectedLength }}</span>
          </i18n-t>
        </div>
        <div v-if="isPermission" class="group-tab">
          <GroupTab
            :is-show-operation="true"
            :aside-item="asideItem"
            :source-list="sourceList"
            :selected-data="selectedData"
            :handle-renewal="handleRenewal"
            :handle-hand-over="handOverDialog"
            :handle-remove="handleRemove"
            :get-select-list="getSelectList"
            :handle-select-all-data="handleSelectAll"
            :handle-load-more="handleLoadMore"
            :handle-clear="handleClear"
            @collapse-click="collapseClick"
          />
        </div>
        <div v-else class="no-permission">
          <no-permission />
        </div>
      </div>
    </div>
    <bk-exception
      v-else
      :description="t('没有数据')"
      scene="part"
      type="empty"
    >
      <i18n-t
        tag="div"
        keypath="可以尝试 调整关键词 或 清空筛选条件"
      >
        <button class="text-blue" @click='refresh'>{{t('清空筛选条件')}}</button>
      </i18n-t>
    </bk-exception>
  </bk-loading>
  <bk-dialog
    :width="660"
    class="renewal-dialog"
    :is-show="isShowRenewal"
    @closed="handleRenewalClosed"
  >
    <template #header>
      <h2 class="htext">{{t("续期")}}</h2>
      <span class="dialog-header"> {{userName}} </span>
    </template>
    <template #default>
      <p class="renewal-text">
        <span>{{t("用户组名")}}：</span> {{ selectedRow?.groupName }}
      </p>
      <p class="renewal-text">
        <span class="required">{{t("授权期限")}}</span>
        <TimeLimit ref="renewalRef" @change-time="handleChangeTime" />
      </p>
      <p class="renewal-text">
        <span>{{t("到期时间")}}：</span> 
        <template v-if="selectedRow?.expiredAtDisplay === t('已过期')">
          <span class="text-gray">{{t("已过期")}}</span>
          <span class="text-blue">
            <i class="manage-icon manage-icon-arrows-right"></i> 
            {{ expiredAt }} {{ t("天") }}
          </span>
        </template>
        <template v-else>
          <span class="text-gray">{{ selectedRow?.expiredAtDisplay }}</span class="text-blue">
          <span class="text-blue">
            <i class="manage-icon manage-icon-arrows-right"></i> 
            {{ Number(selectedRow?.expiredAtDisplay.replace(/\D/g, '')) + expiredAt }} {{ t("天") }}
          </span>
        </template>
      </p>
    </template>
    <template #footer>
      <bk-button
        theme="primary"
        :loading="operatorLoading"
        @click="handleRenewalConfirm"
      >
        {{t('续期')}}
      </bk-button>
      <bk-button
        class="btn-margin"
        @click="handleRenewalClosed"
      >
        {{t('取消')}}
      </bk-button>
    </template>
  </bk-dialog>
  <bk-dialog
    :width="640"
    class="handover-dialog"
    :is-show="isShowHandover"
    @closed="handleHandoverClosed"
  >
    <template #header>
      <h2 class="htext">{{t("移交")}}</h2>
      <span class="dialog-header"> {{userName}} </span>
    </template>
    <template #default>
      <p class="handover-text">
        <span>{{t("用户组名")}}：</span> {{ selectedRow?.groupName }}
      </p>
      <p class="handover-text">
        <bk-form
          ref="formRef"
          :rules="rules"
          label-width="100"
          :model="handOverForm"
        >
          <bk-form-item
            required
            property="name"
            label-position="right"
            :label="t('移交给')"
          >
            <project-user-selector
              class="selector-input"
              @change="handleChangeOverFormName"
              @removeAll="handleClearOverFormName"
            >
            </project-user-selector>
          </bk-form-item>
        </bk-form>
      </p>
    </template>
    <template #footer>
      <bk-button
        theme="primary"
        :loading="operatorLoading"
        @click="handleHandoverConfirm"
      >
        {{t('移交')}}
      </bk-button>
      <bk-button
        class="btn-margin"
        @click="handleHandoverClosed"
      >
        {{t('取消')}}
      </bk-button>
    </template>
  </bk-dialog>
  <bk-dialog
    :width="450"
    header-align="center"
    footer-align="center"
    class="remove-dialog"
    :is-show="isShowRemove"
    @closed="handleRemoveClosed"
  >
    <template #header>
      <img src="@/css/svg/warninfo.svg" class="manage-icon-tishi">
      <h2 class="dialog-header"> {{t("确认从用户组中移出用户吗")}}？ </h2>
    </template>
    <template #default>
      <p class="remove-text">
        <span>{{t("待移出用户")}}：</span> {{userName}}
      </p>
      <p class="remove-text">
        <span>{{t("所在用户组")}}：</span> {{ selectedRow?.groupName }}
      </p>
    </template>
    <template #footer>
      <bk-button
        theme="danger"
        :loading="operatorLoading"
        @click="handleRemoveConfirm"
      >
        {{t('确认移出')}}
      </bk-button>
      <bk-button
        class="btn-margin"
        @click="handleRemoveClosed"
      >
        {{t('关闭')}}
      </bk-button>
    </template>
  </bk-dialog>
  <bk-sideslider
    v-model:isShow="isShowSlider"
    :title="sliderTitle"
    :quick-close="false"
    ext-cls="slider"
    width="960"
    @hidden="batchCancel"
  >
    <template #default>
      <div class="slider-main">
        <p class="main-desc">
          <i18n-t keypath="已选择X个用户组" tag="div">
            <span class="desc-primary">{{ totalCount }}</span>
          </i18n-t>
          <i18n-t v-if="inoperableCount" keypath="；其中X个用户组X，本次操作将忽略" tag="div">
            <span class="desc-warn">{{ inoperableCount }}</span>
            <template #op>
              <span class="desc-warn">{{ unableText[batchFlag] }}</span>
            </template>
          </i18n-t>
        </p>
        <div>
          <GroupTab
            :source-list="selectSourceList"
            :is-show-operation="false"
            :aside-item="asideItem"
            :batch-flag="batchFlag"
            :page-limit-change="pageLimitChange"
            :page-value-change="pageValueChange"
          />
        </div>
      </div>
      <div class="slider-footer">
        <div class="footer-main">
          <div v-if="batchFlag === 'renewal'">
            <div class="main-line">
              <p class="main-label">{{t("续期对象")}}</p>
              <span class="main-text">{{t("用户")}}： {{userName}}</span>
            </div>
            <div class="main-line">
              <p class="main-label">{{t("续期时长")}}</p>
              <TimeLimit ref="renewalRef" @change-time="handleChangeTime" />
            </div>
          </div>
          <div v-if="batchFlag === 'handover'">
            <div class="main-line main-line-handover">
              <p class="main-label">{{t("移交给")}}</p>
              <bk-form
                ref="formRef"
                :rules="rules"
                :model="handOverForm"
              >
                <bk-form-item
                  required
                  property="name"
                >
                  <project-user-selector
                    class="selector-input"
                    @change="handleChangeOverFormName"
                    @removeAll="handleClearOverFormName"
                    :key="isShowSlider"
                  >
                  </project-user-selector>
                </bk-form-item>
              </bk-form>
            </div>
          </div>
          <div v-if="batchFlag === 'remove'">
            <div class="main-line main-line-remove">
              <p class="main-label-remove">
                <i18n-t keypath="确认从以上X个用户组中移出X吗？" tag="div">
                  <span class="remove-num">{{ totalCount - inoperableCount }}</span>
                  <span class="remove-person">{{ userName }}</span>
                </i18n-t>
              </p>
            </div>
          </div>
        </div>
        <div class="footer-btn">
          <bk-button
            :disabled="totalCount === inoperableCount"
            :theme="batchFlag === 'remove' ? 'danger' : 'primary'"
            @click="batchConfirm(batchFlag)"
            :loading="batchBtnLoading"
          >
            {{t(btnTexts[batchFlag])}}
          </bk-button>
          <bk-button @click="batchCancel">{{t("取消")}}</bk-button>
        </div>
      </div>
    </template>
  </bk-sideslider>
</template>

<script setup name="ManageAll">
import { useI18n } from 'vue-i18n';
import { useRoute } from 'vue-router';
import { Message } from 'bkui-vue';
import { ref, onMounted, computed, watch } from 'vue';
import ManageAside from './manage-aside.vue';
import GroupTab from './group-tab.vue';
import TimeLimit from './time-limit.vue';
import http from '@/http/api';
import ProjectUserSelector from '@/components/project-user-selector';
import NoPermission from '../no-enable-permission/no-permission.vue';
import userGroupTable from "@/store/userGroupTable";
import useManageAside from "@/store/manageAside";
import { storeToRefs } from 'pinia';
import { batchOperateTypes, btnTexts, batchTitle, batchMassageText } from "@/utils/constants.js";

const batchBtnLoading = ref(false);
const { t } = useI18n();
const route = useRoute();
const formRef = ref(null);
const renewalRef = ref(null);
const projectId = computed(() => route.params?.projectCode);
const expiredAt = ref(30);
const isShowSlider = ref(false);
const sliderTitle = ref();
const batchFlag = ref();
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
    { required: true, message: t('请输入移交人'), trigger: 'blur' },
  ],
};
const searchValue = ref([]);
const inoperableCount = ref();
const totalCount = ref();
const renewalLoading = ref(false);
const handoverLoading = ref(false);
const removerLoading = ref(false);
const loadingMap = {
  renewal: renewalLoading,
  handover: handoverLoading,
  remove: removerLoading
};
const filterTips = computed(() => {
  return searchData.value.map(item => item.name).join(' / ');
});
const searchData = computed(() => {
  const data = [
    {
      name: t('用户'),
      id: 'user',
    },
    {
      name: t('组织架构'),
      id: 'department',
    },
  ]
  return data.filter(data => {
    return !searchValue.value.find(val => val.id === data.id)
  })
});
const manageAsideRef = ref(null);
const groupTableStore = userGroupTable();
const manageAsideStore = useManageAside();
const operatorLoading = ref(false);
const userName = computed(() => {
  if(asideItem.value){
    if (asideItem.value.type === 'user') {
      return `${asideItem.value.id}(${asideItem.value.name})`;
    }
    return asideItem.value.name;
  }
  return ''
})
const unableText = {
  renewal: t('无法续期'),
  handover: t('无法移交'),
  remove: t('无法移出'),
}
const {
  sourceList,
  isShowRenewal,
  isShowHandover,
  isShowRemove,
  selectedData,
  selectedLength,
  selectSourceList,
  selectedRow,
  isPermission,
} = storeToRefs(groupTableStore);
const {
  handleRenewal,
  handleHandOver,
  handleRemove,
  getSelectList,
  getSourceList,
  handleLoadMore,
  handleSelectAllData,
  handleClear,
  collapseClick,
  handleRemoveRow,
  handleUpDateRow,
  pageLimitChange,
  pageValueChange,
} = groupTableStore;

const {
  asideItem,
  memberList,
  personList,
  tableLoading,
  isLoading,
  activeTab,
  memberPagination,
} = storeToRefs(manageAsideStore);
const {
  handleAsideClick,
  handleAsidePageChange,
  handleShowPerson,
  handleAsideRemoveConfirm,
  getProjectMembers,
} = manageAsideStore;

onMounted(() => {
  init(true);
});

watch(searchValue, (newSearchValue) => {
  init(undefined, newSearchValue);
});
function handleSearch(value){
  if(!value.length) return;
  init(undefined, value);
}
function init(flag, searchValue){
  memberPagination.value.current = 1;
  asideItem.value = undefined;
  getProjectMembers(projectId.value, flag, searchValue);
}
function asideClick(item){
  handleAsideClick(item, projectId.value);
}
async function refresh(){
  asideItem.value = undefined;
  searchValue.value = [];
}
/**
 * 移交弹窗打开时
 */
function handOverDialog(row, resourceType, index){
  formRef.value?.clearValidate();
  handleHandOver(row, resourceType, index);
}
/**
 * 续期弹窗提交事件
 */
async function handleRenewalConfirm() {
  try {
    operatorLoading.value = true;
    await handleUpDateRow(expiredAt.value);
    operatorLoading.value = false;
    showMessage('success', t('用户组权限已续期。'));
    cancleClear('renewal');
    isShowRenewal.value = false;
  } catch (error) {
    operatorLoading.value = false;
  }
};
/**
 * 续期弹窗关闭
 */
function handleRenewalClosed() {
  cancleClear('renewal');
  isShowRenewal.value = false;
}
/**
 * 移交弹窗提交事件
 */
async function handleHandoverConfirm() {
  const isValidate = await formRef.value.validate();
  if(!isValidate) return;

  const param = formatSelectParams(selectedRow.value.groupId);
  delete param.renewalDuration;
  try {
    operatorLoading.value = true;
    const res = await http.batchHandover(projectId.value, param);
    if (res) {
      operatorLoading.value = false;
      showMessage('success', t('用户组权限已移交给X。',[`${handOverForm.value.id}(${handOverForm.value.name})`]));
      isShowHandover.value = false;
      handleRemoveRow();
      cancleClear('handover');
    }
  } catch (error) {
    operatorLoading.value = false;
  }
};
/**
 * 移交弹窗关闭
 */
 function handleHandoverClosed() {
  cancleClear('handover');
  isShowHandover.value = false;
}
/**
 * 移出弹窗提交事件
 */
async function handleRemoveConfirm() {
  try {
    operatorLoading.value = true;
    const param = formatSelectParams(selectedRow.value.groupId);
    delete param.renewalDuration;
    await http.batchRemove(projectId.value, param);
    operatorLoading.value = false;
    showMessage('success', t('X 已移出X用户组。', [`${asideItem.value.id}(${asideItem.value.name})`, selectedRow.value.groupName]));
    handleRemoveRow();
    isShowRemove.value = false;
  } catch (error) {
    operatorLoading.value = false;
  }
}
function handleRemoveClosed(){
  isShowRemove.value = false;
}
/**
 * 授权期限选择
 */
function handleChangeTime(value) {
  expiredAt.value = Number(value);
};
function handleSelectAll(resourceType, asideItem){
  handleSelectAllData(resourceType, asideItem)
}
/**
 * 批量操作
 * @param flag 按钮标识
 */
async function batchOperator(flag){
  getSourceList();
  if (!selectedLength.value) {
    Message({
      theme: 'error',
      message: t('请先选择用户组')
    });
    return;
  }

  try {
    loadingMap[flag].value = true;
    const params = formatSelectParams();
    delete params.renewalDuration

    const res = await http.batchOperateCheck(projectId.value, batchOperateTypes[flag], params);
    totalCount.value = res.totalCount;
    inoperableCount.value = res.inoperableCount;
    loadingMap[flag].value = false;

    sliderTitle.value = t(batchTitle[flag]);
    batchFlag.value = flag;
    isShowSlider.value = true;
  } catch (error) {
    loadingMap[flag].value = false;
  }
}

function batchCancel() {
  cancleClear(batchFlag.value);
}
/**
 * 批量操作请求参数获取
 * @param batchFlag 按钮标识
 */
function formatSelectParams(rowGroupId){
  let groupIds = [];
  let resourceTypes = [];
  if(rowGroupId) {
    groupIds.push(rowGroupId);
  } else {
    selectSourceList.value.forEach(item => {
      if (item.isAll && !item.groupIds) {
        resourceTypes.push(item.resourceType);
      } else {
        groupIds.push(...item.groupIds);
      }
    })
  }
  const params = {
    groupIds: groupIds,
    resourceTypes: resourceTypes || [],
    targetMember: asideItem.value,
    ...(expiredAt.value && {renewalDuration: expiredAt.value}),
    ...(handOverForm.value.name && {handoverTo: handOverForm.value}),
  }
  return params;
}
/**
 * 批量操作clear事件
 * @param batchFlag 按钮标识
 */
function cancleClear(batchFlag) {
  isShowSlider.value = false;

  if (batchFlag === 'handover') {
    handOverForm.value && (Object.assign(handOverForm.value, getHandOverForm()));
    formRef.value?.clearValidate()
  } else if (batchFlag === 'renewal') {
    renewalRef.value.initTime();
    expiredAt.value = 30;
  }
}
/**
 * 侧边栏确认事件
 * @param batchFlag 按钮标识
 */
async function batchConfirm(batchFlag) {
  batchBtnLoading.value = true;

  let res = null;
  const params = formatSelectParams();

  try {
    if (batchFlag === 'renewal') {
      res = await http.batchRenewal(projectId.value, params);
    } else if (batchFlag === 'handover') {
      const flag = await formRef.value.validate();
      if (!flag) return;
      res = await http.batchHandover(projectId.value, params);
    } else if (batchFlag === 'remove') {
      res = await http.batchRemove(projectId.value, params);
    }

    if (res) {
      showMessage('success', t(batchMassageText[batchFlag]));
      batchBtnLoading.value = false;
      cancleClear(batchFlag);
      getProjectMembers(projectId.value, true);
    }
  } catch (error) {
    batchBtnLoading.value = false;
  }
}
function showMessage(theme, message) {
  Message({
    theme: theme,
    message: message,
  });
}
function asideRemoveConfirm(removeUser, handOverForm) {
  handleAsideRemoveConfirm(removeUser, handOverForm, projectId.value, manageAsideRef.value);
}

async function getMenuList (item, keyword) {
  const query = {
    memberType: item.id,
    page: 1,
    pageSize: 400
  }
  if (item.id === 'user' && keyword) {
    query.userName = keyword
  } else if (item.id === 'department' && keyword) {
    query.departName = keyword
  }
  const res = await http.getProjectMembers(projectId.value, query)
  return res.records.map(i => {
    return {
      ...i,
      displayName: i.name || i.id,
      name: i.type === 'user' ? (!i.name ? i.id : `${i.id} (${i.name})`) : i.id,
    }
  })
}
function handleChangeOverFormName ({list, userList}) {
  const val = list.join(',')
  handOverForm.value = userList.find(i => i.id === val)
}

function handleClearOverFormName () {
  Object.assign(handOverForm.value, getHandOverForm());
}
</script>

<style lang="less" scoped>
.manage {
  width: calc(100% - 240px);
  height: 100%;
  overflow: hidden;

  .manage-search {
    display: flex;
    width: 100%;
    height: 64px;
    background: #FFFFFF;
    padding: 16px 24px;
    box-shadow: 0 2px 4px 0 #1919290d;

    .multi-search {
      width: 50%;
    }
  }

  .manage-article {
    height: calc(100% - 104px);
    display: flex;
    margin: 16px 0 24px 24px;

    .manage-aside {
      position: relative;
      width: 230px;
      background: #FFFFFF;
      box-shadow: 0 2px 4px 0 #1919290d;
      flex-shrink: 0;
    }
    
    .manage-content {
      width: calc(100% - 246px);
      height: 100%;
      flex: 1;
      margin-left: 16px;

      .manage-content-btn {
        height: 42px;
        display: flex;
        margin-bottom: 10px;
        position: absolute;
        z-index: 9;

        .bk-button {
          margin-right: 8px
        }

        .main-desc {
          display: flex;
          margin-left: 24px;
          color: #63656e;
          font-size: 12px;
          line-height: 32px;

          .desc-primary {
            font-weight: 700;
            color: #3A84FF;
            padding: 0 4px;
          }
        }
      }

      .group-tab {
        height: 100%;
        margin-top: 42px;
      }
    }
  }
}

.dialog-header-common {
  display: inline-block;
  padding-left: 17px;
  margin-left: 17px;
  border-left: 1px solid #C4C6CC;
  font-family: MicrosoftYaHei;
  font-size: 12px;
  color: #63656E;
  letter-spacing: 0;
}

.dialog-text-common {
  font-family: MicrosoftYaHei;
  font-size: 12px;
  color: #313238;
  line-height: 20px;
}

.btn-margin{
  margin-left: 10px
}

.htext{
  display: inline;
}

.renewal-dialog {

  .dialog-header {
    .dialog-header-common();
  }

  .required {
    position: relative;
    margin-right: 16px;
  }

  .required:after {
    position: absolute;
    top: 0;
    width: 14px;
    color: #ea3636;
    text-align: center;
    content: "*";
  }

  .renewal-text {
    display: flex;
    margin: 24px 0 0 40px;
    .dialog-text-common();

    span {
      display: inline-block;
      font-family: MicrosoftYaHei;
      text-align: right;
      font-size: 12px;
      color: #63656E;
    }

    .text-gray{
      color: #979BA5;
    }
    .text-blue{
      color: #699DF4;
    }
    .manage-icon-arrows-right{
      margin: 0 4px;
    }
  }
}

.handover-dialog{

  .dialog-header {
    .dialog-header-common();
  }

  .handover-text {
    margin: 12px 0;
    .dialog-text-common();

    span {
      display: inline-block;
      min-width: 100px;
      text-align: right;
      color: #63656E;
    }

    ::v-deep .bk-form-label {
      font-size: 12px;
      color: #63656E;
    }

  }
}

.remove-dialog {

  .dialog-header {
    font-family: MicrosoftYaHei;
    font-size: 20px;
    color: #313238;
    letter-spacing: 0;
  }

  .remove-text {
    display: flex;
    margin: 12px 0;
    .dialog-text-common();

    span {
      color: #63656E;
    }
  }

  .manage-icon-tishi {
    width: 42px;
    height: 42px;
  }
}

.no-permission {
  height: calc(100% - 42px);
  padding-top: 10%;
  margin-top: 42px;
  background-color: #fff;
  box-shadow: 0 2px 4px 0 #1919290d;
}

.slider{

  ::v-deep .bk-modal-body {
    background-color: #F0F1F5;
  }

  ::v-deep .bk-sideslider-content {
    overflow: auto;
    height: calc(100vh - 282px);

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

  }
  .slider-main {
    margin: 16px 24px;

    .main-desc {
      display: flex;
      margin-bottom: 16px;
      color: #63656e;
      font-size: 12px;
      line-height: 20px;

      .desc-primary {
        color: #3A84FF;
        font-weight: 700;
        padding: 0 4px;
      }

      .desc-warn {
        color: #FF9C01;
        font-weight: 700;
        padding: 0 4px;
      }
    }
  }

  .slider-footer {
    position: fixed;
    bottom: 0;
    z-index: 9;
    width: 100%;
    height: 230px;
    padding: 24px 48px;
    background: #FFFFFF;
    box-shadow: 0 -1px 6px 0 #DCDEE5;

    .footer-main {

      .main-line{
        margin-bottom: 24px;

        .main-label {
          margin-bottom: 6px;
          line-height: 22px;
          font-family: MicrosoftYaHei-Bold;
          font-weight: 700;
          font-size: 14px;
          color: #63656E;
        }

        .main-text {
          font-size: 12px;
          color: #63656E;
        }

        .selector-input {
          width: 480px;
        }

        ::v-deep .bk-form-content {
          margin: 0 !important;
        }

        .main-label-remove {
          color: #63656e;
          font-size: 16px;

          .remove-num {
            color: #3a84ff;
            font-size: 16px;
            font-weight: 700;
          }

          .remove-person {
            color: #63656e;
            font-size: 16px;
            font-weight: 700;
          }
        }
      }

      .main-line-handover {
        margin-top: 26px;
      }
      .main-line-remove {
        margin-top: 40px;
      }
    }

    .footer-btn {
      position: absolute;
      bottom: 24px;
      margin-top: 24px;

      .bk-button {
        margin-right: 8px;
      }
    }
  }
}

.text-blue{
  color: #699DF4;
}
</style>
