<template>
  <div class="manage" :zIndex="100">
    <manage-search
        ref="manageSearchRef"
        @search-init="init"
    />
    <div class="manage-content" v-if="isNotProject">
      <div class="manage-content-btn">
        <bk-button
          :disabled="!isPermission"
          @click="batchOperator('handover')"
          :loading="handoverLoading"
          >
          {{t("批量移交")}}
        </bk-button>
        <bk-button
          :disabled="!isPermission"
          @click="batchOperator('remove')"
          :loading="removerLoading"
        >
          {{t("批量退出")}}
        </bk-button>

        <i18n-t keypath="已选择X个用户组" tag="div" class="main-desc" v-if="selectedLength">
          <span class="desc-primary">{{ selectedLength }}</span>
        </i18n-t>
      </div>
      <div v-if="isPermission" class="group-tab">
        <GroupTab
          :is-show-operation="true"
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
        <no-permission
          :user-id="asideItem?.id"
        />
      </div>
    </div>
    <bk-exception
      v-else
      :description="t('没有数据')"
      scene="part"
      type="empty"
      class="empty-content"
    >
      <i18n-t
        tag="div"
        keypath="可以尝试 调整关键词 或 清空筛选条件"
      >
        <button class="text-blue" @click='refresh'>{{t('清空筛选条件')}}</button>
      </i18n-t>
    </bk-exception>
  </div>
  <bk-dialog
    :width="660"
    class="renewal-dialog"
    :is-show="isShowRenewal"
    @closed="handleRenewalClosed"
  >
    <template #header>
      <h2 class="htext">{{t("续期")}}</h2>
      <span class="dialog-header"> {{user.id}} </span>
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
          <span class="text-blue renewal">
            <img src="@/css/svg/arrows-right.svg">
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
      <span class="dialog-header"> {{user.id}} </span>
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
              :projectId="projectId"
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
      <img src="@/css/svg/warninfo.svg" class="manage-icon-tips">
      <h2 class="dialog-header"> {{t("确认从用户组中移出用户吗")}}？ </h2>
    </template>
    <template #default>
      <p class="remove-text">
        <span>{{t("待移出用户")}}：</span> {{user.id}}
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
    :title="title"
    :quick-close="false"
    ext-cls="slider"
    width="960"
    @hidden="batchCancel"
    :before-close="beforeClose"
  >
    <template #default>
      <div v-if="!isDetail" class="slider-content">
        <div class="slider-main">
          <p class="main-desc">
            <i18n-t keypath="已选择X个用户组" tag="div">
              <span class="desc-primary">{{ checkData.totalCount }}</span>
            </i18n-t>
            <i18n-t v-if="checkData.inoperableCount" keypath="；其中X个用户组X，本次操作将忽略" tag="div">
              <span class="desc-warn">{{ checkData.inoperableCount }}</span><span class="desc-warn">{{ t(unableText[batchFlag]) }}</span>
            </i18n-t>
          </p>
          <div>
            <GroupTab
              :source-list="selectSourceList"
              :is-show-operation="false"
              :batch-flag="batchFlag"
              :page-limit-change="pageLimitChange"
              :page-value-change="pageValueChange"
            />
          </div>
        </div>
        <div class="slider-footer">
          <div class="footer-main" :class="authorizationInvalid ? '' : 'main-line-handover'">
            <div class="main-line">
              <p
                v-if="authorizationInvalid && batchFlag === 'handover'"
                class="main-text"
              >
                {{ t('移交以上用户组，将导致') }}
                <i18n-t v-if="checkData.invalidPipelineAuthorizationCount" keypath="X个流水线权限代持失效，" tag="span">
                  <span class="remove-num">{{ checkData.invalidPipelineAuthorizationCount }}</span>
                </i18n-t>

                <i18n-t v-if="checkData.invalidRepositoryAuthorizationCount" keypath="X个代码库授权失效，" tag="span">
                  <span class="remove-num">{{ checkData.invalidRepositoryAuthorizationCount }}</span>
                </i18n-t>
                {{ t('请确认是否同步移交授权。') }}
              </p>
              
              <div v-if="batchFlag === 'remove'">
                <p
                  v-if="authorizationInvalid"
                  class="main-text"
                >
                  {{ t('退出以上用户组，将导致') }}
                  <i18n-t v-if="checkData.invalidPipelineAuthorizationCount" keypath="X个流水线权限代持失效，" tag="span">
                    <span class="remove-num">{{ checkData.invalidPipelineAuthorizationCount }}</span>
                  </i18n-t>

                  <i18n-t v-if="checkData.invalidRepositoryAuthorizationCount" keypath="X个代码库授权失效，" tag="span">
                    <span class="remove-num">{{ checkData.invalidRepositoryAuthorizationCount }}</span>
                  </i18n-t>

                  <i18n-t v-if="checkData.uniqueManagerCount" keypath="X个资源没有拥有者，" tag="span">
                    <span class="remove-num">{{ checkData.uniqueManagerCount }}</span>
                  </i18n-t>

                  <i18n-t keypath="查看详情, 请填写交接人，完成交接后才能成功退出。" tag="span">
                    <span class="remove-num remove-detail" @click="handleDetail">{{ t("查看详情") }}</span>
                  </i18n-t>
                </p>
                <p
                  v-else
                  class="main-label-remove">
                  <i18n-t keypath="确认退出以上X个用户组吗？" tag="div">
                    <span class="remove-num">{{ checkData.totalCount }}</span>
                  </i18n-t>
                </p>
              </div>
  
              <div v-if="authorizationInvalid || batchFlag === 'handover'">
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
                      :projectId="projectId"
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
          </div>
          <div class="footer-btn">
            <bk-button
              :theme="batchFlag === 'remove' && !authorizationInvalid ? 'danger' : 'primary'"
              @click="batchConfirm(batchFlag)"
              :loading="batchBtnLoading"
            >
              {{authorizationInvalid ? t("申请交接") : t(btnTexts[batchFlag])}}
            </bk-button>
            <bk-button @click="batchCancel">{{t("取消")}}</bk-button>
            <p v-if="authorizationInvalid && batchFlag === 'remove'">
              <img src="@/css/svg/info-circle.svg" class="info-circle">
              <span>{{ t("完成交接后，将自动退出用户组") }}</span>
            </p>
          </div>
        </div>
      </div>
      <div class="slider-detail" v-else>
        <DetailGroupTab
          :source-list="detailSourceList"
          :page-limit-change="detailPageLimitChange"
          :page-value-change="detailPageValueChange"
          @collapse-click="detailCollapseClick"
        />
        <bk-button class="go-back" @click="goBack">{{t("返回")}}</bk-button>
      </div>
    </template>
  </bk-sideslider>
</template>

<script setup name="ManageAll">
import http from '@/http/api';
import { useI18n } from 'vue-i18n';
import { storeToRefs } from 'pinia';
import { Message, InfoBox} from 'bkui-vue';
import { ref, onMounted, computed, onUnmounted, h } from 'vue';
import userGroupTable from "@/store/userGroupTable";
import userDetailGroupTable from "@/store/userDetailGroupTable";
import { batchOperateTypes, btnTexts, batchTitle, batchMassageText, unableText } from "@/utils/constants.js";
import GroupTab from '@/components/permission-manage/group-tab';
import TimeLimit from '@/components/permission-manage/time-limit.vue';
import ProjectUserSelector from '@/components/project-user-selector';
import NoPermission from '@/components/permission-manage/no-permission.vue';
import manageSearch from "@/components/permission-manage/manage-search.vue";
import DetailGroupTab from "@/components/permission-manage/detail-group-tab.vue";
import { OPERATE_CHANNEL } from "@/utils/constants";
import { AngleRight  } from 'bkui-vue/lib/icon';
import { useRouter } from 'vue-router';

const user = ref();
const { t } = useI18n();
const formRef = ref(null);
const router = useRouter();
const renewalRef = ref(null);
const expiredAt = ref(30);
const isShowSlider = ref(false);
const sliderTitle = ref();
const batchFlag = ref();
const batchBtnLoading = ref(false);
const handOverForm = ref(getHandOverForm());
const checkData = ref();
const handoverLoading = ref(false);
const removerLoading = ref(false);
const loadingMap = {
  handover: handoverLoading,
  remove: removerLoading
};
const rules = {
  name: [
    { required: true, message: t('请输入移交人'), trigger: 'blur' },
  ],
};
const isDetail = ref(false);
const searchGroup = ref();
const manageSearchRef = ref(null);
const groupTableStore = userGroupTable();
const detailGroupTable = userDetailGroupTable();
const operatorLoading = ref(false);
const isNotProject = computed(() => collapseList.value.length);
const title = computed(() => {
  if (!isDetail.value) {
    return sliderTitle.value
  } else {
    return h('p', { style: { display: 'flex', alignItems: 'center' } },
      [
        h('span', {
          style: { color: '#3A84FF', cursor: 'pointer' },
          onClick () {
            goBack()
          }
        }, sliderTitle.value),
        h(AngleRight, { style: { color: '#C4C6CC', fontSize: '22px', magin: '0 5px' } } ),
        h('span',{ style: { fontSize: '14px', color: '#313238' } },  t('待移交详情'))
      ]
    )
  }
})
// 资源失效个数，是否需要权限交接
const authorizationInvalid = computed(()=> checkData.value.invalidPipelineAuthorizationCount + checkData.value.invalidRepositoryAuthorizationCount)
const {
  projectId,
  sourceList,
  collapseList,
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
  initData,
  fetchUserGroupList,
  handleRenewal,
  handleHandOver,
  handleRemove,
  getSelectList,
  getSourceList,
  handleLoadMore,
  handleSelectAllData,
  handleClear,
  collapseClick,
  handleReplaceRow,
  handleRemoveRow,
  handleUpDateRow,
  pageLimitChange,
  pageValueChange,
  clearPaginations,
} = groupTableStore;

const {
  detailSourceList
} = storeToRefs(detailGroupTable);
const {
  fetchDetailList,
  detailCollapseClick,
  detailPageLimitChange,
  detailPageValueChange,
} = detailGroupTable;

onMounted(() => {
  getUser();
});
onUnmounted(()=>{
  initData()
})
function getHandOverForm(){
  return {
    id: '',
    name: '',
    type: '',
  }
}
/**
 * 获取用户信息
 */
async function getUser() {
  try {
    const res = await http.getUser();
    user.value = {
      id: res.username,
      name:  res.chineseName,
      type: "user"
    }
  } catch (error) {

  }
}

function init(projectId, searchValue) {
  searchGroup.value = searchValue;
  fetchUserGroupList(user.value.id, projectId, searchValue);
}

async function refresh() {
  manageSearchRef.value?.clearSearch();
}
/**
 * 移交弹窗打开时
 */
function handOverDialog(row, resourceType, index) {
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
    cancelClear('renewal');
    isShowRenewal.value = false;
  } catch (error) {
    operatorLoading.value = false;
  }
};
/**
 * 续期弹窗关闭
 */
function handleRenewalClosed() {
  cancelClear('renewal');
  isShowRenewal.value = false;
}
/**
 * 移交弹窗提交事件
 */
async function handleHandoverConfirm() {
  const isValidate = await formRef.value.validate();
  if (!isValidate) return;
  const param = formatSelectParams({
    id: selectedRow.value.groupId,
    memberType: selectedRow.value.memberType
  });
  delete param.renewalDuration;

  if (user.value.id === handOverForm.value.id) {
    showMessage('error', t('目标对象和交接人不允许相同。'));
    return
  }

  try {
    operatorLoading.value = true;
    const res = await http.batchHandover(projectId.value, param);
    if (res) {
      await handleReplaceRow(user.value.id);

      isShowHandover.value = false;
      cancelClear('handover');
      showMessage('success', t('用户组权限已移交给X。',[`${handOverForm.value.id}(${handOverForm.value.name})`]));
    }
  } catch (error) {
    console.log(error)
  } finally {
    operatorLoading.value = false;
  }
};
/**
 * 移交弹窗关闭
 */
 function handleHandoverClosed() {
  cancelClear('handover');
  isShowHandover.value = false;
}
/**
 * 移出弹窗提交事件
 */
async function handleRemoveConfirm() {
  try {
    operatorLoading.value = true;

    const res = await http.getIsDirectRemove(projectId.value, selectedRow.value.groupId, user.value);
    if (res) {
      await handleRemoveRow();

      isShowRemove.value = false;
      showMessage('success', t('X 已移出X用户组。', [`${user.value.id}(${user.value.name})`, selectedRow.value.groupName]));
    }
  } catch (error) {
    console.log(error);
  } finally {
    operatorLoading.value = false;
  }
}
function handleRemoveClosed() {
  isShowRemove.value = false;
}
/**
 * 授权期限选择
 */
function handleChangeTime(value) {
  expiredAt.value = Number(value);
};

function handleSelectAll(resourceType){
  handleSelectAllData(resourceType)
}
/**
 * 批量操作
 * @param flag 按钮标识
 */
async function batchOperator(flag) {
  getSourceList();
  if (!selectedLength.value) {
    showMessage('error', t('请先选择用户组'));
    return;
  }

  try {
    loadingMap[flag].value = true;
    const params = formatSelectParams();
    delete params.renewalDuration
    const res = await http.batchOperateCheck(projectId.value, batchOperateTypes[flag], params);
    checkData.value = res;
    loadingMap[flag].value = false;

    sliderTitle.value = t(batchTitle[flag]);
    batchFlag.value = flag;
    isShowSlider.value = true;
  } catch (error) {
    loadingMap[flag].value = false;
  }
}
function beforeClose() {
  return new Promise((resolve, reject) => {
    if(isDetail.value) {
      InfoBox({
        title: t('批量退出操作尚未完成，确认放弃操作吗？'),
        infoType: 'warning',
        cancelText: t('取消'),
        confirmText: t('确定'),
        onConfirm: () => resolve(true),
        onCancel: () => reject(),
      });
    } else {
      resolve(true);
    }
  });
}

function batchCancel() {
  cancelClear(batchFlag.value);
  batchBtnLoading.value = false;
  isDetail.value = false;
  Object.assign(handOverForm.value, getHandOverForm());
  clearPaginations();
}
/**
 * 批量操作请求参数获取
 * @param batchFlag 按钮标识
 */
function formatSelectParams(rowGroupId) {
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
    operateChannel: OPERATE_CHANNEL,
    resourceTypes: resourceTypes || [],
    targetMember: user.value,
    ...(expiredAt.value && {renewalDuration: expiredAt.value}),
    ...(handOverForm.value.name && {handoverTo: handOverForm.value}),
  }
  return params;
}
/**
 * 批量操作clear事件
 * @param batchFlag 按钮标识
 */
function cancelClear(batchFlag) {
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
  let res = null;
  const params = formatSelectParams();
  delete params.renewalDuration;
  try {
    if (batchFlag === 'handover') {
      if (!(await handleHandoverValidation())) return;
      batchBtnLoading.value = true;
      res = await http.batchHandover(projectId.value, params);
      if (res) {
        showHandoverSuccessInfoBox();
      }
    } else if (batchFlag === 'remove') {
      if (!(await handleRemoveValidation())) return;
      batchBtnLoading.value = true;
      res = await http.batchRemove(projectId.value, params);
      if (res && authorizationInvalid.value) {
        showRemoveSuccessInfoBox();
      }
    }

    if (res) {
      batchCancel();
      fetchUserGroupList(user.value.id, projectId.value, searchGroup.value);
      !authorizationInvalid.value && showMessage('success', t(batchMassageText[batchFlag]));
    }
  } catch (error) {
    console.log(error);
  } finally {
    batchBtnLoading.value = false;
  }
}

async function handleHandoverValidation() {
  const flag = await formRef.value.validate();
  if (!flag) return false;

  if (user.value.id === handOverForm.value.id) {
    showMessage('error', t('目标对象和交接人不允许相同。'));
    batchBtnLoading.value = false;
    return false;
  }
  return true;
}

async function handleRemoveValidation() {
  if (!authorizationInvalid.value) return true;

  const flag = await formRef.value.validate();
  if (!flag) return false;

  return true;
}

function showHandoverSuccessInfoBox() {
  InfoBox({
    type: 'success',
    title: t('移交申请提交成功'),
    confirmText: t('查看进度'),
    cancelText: t('关闭'),
    class: 'info-box',
    content: h(
      'div', { class: 'info-content' },
      [
        h('p', { class: 'info-text' }, t('已成功提交「移交权限」申请，等待交接人确认。')),
        h('p', { class: 'info-text' }, t('可在“我的交接”中查看进度。'))
      ]
    ),
    onConfirm() {
      router.push({
        name: 'my-handover'
      })
    }
  });
}

function showRemoveSuccessInfoBox() {
  InfoBox({
    width: 500,
    type: 'success',
    title: t('交接申请提交成功'),
    confirmText: t('查看进度'),
    cancelText: t('关闭'),
    class: 'info-box',
    content: h(
      'div', { class: 'info-content' },
      [
        checkData.value.operableCount && h('p', { class: 'info-text' }, t('无需交接的X个用户组已成功退出。', [checkData.value.operableCount])),
        h('div', [
          h('p', { class: 'info-text info-tip' }, t('需要交接的X个用户组：', [checkData.value.canHandoverCount])),
          h('p', { class: 'info-text' }, t('1. 已成功提交移交权限申请，等待交接人X确认。', [handOverForm.value.id])),
          h('p', { class: 'info-text' }, t('2. 可在“我的交接”中查看进度。')),
          h('p', { class: 'info-text' }, t('3. 完成交接后，将自动退出用户组')),
        ])
      ]
    ),
    onConfirm() {
      router.push({
        name: 'my-handover'
      })
    }
  });
}

function showMessage(theme, message) {
  Message({
    theme: theme,
    message: message,
  });
}

function handleChangeOverFormName({list, userList}) {
  if(!list){
    handOverForm.value && Object.assign(handOverForm.value, getHandOverForm());
    return;
  }
  const val = list.join(',')
  handOverForm.value = userList.find(i => i.id === val)
}
function handleClearOverFormName () {
  Object.assign(handOverForm.value, getHandOverForm());
}
/**
 * 查看详情
 */
async function handleDetail() {
  const params = formatSelectParams();
  delete params.renewalDuration;
  isDetail.value = true;

  const batchOperateType = batchFlag.value === "handover" ? "HANDOVER": batchFlag.value === "remove" ? "REMOVE" : ''
  const detailParams = {
    projectCode: projectId.value,
    queryChannel: "PREVIEW",
    batchOperateType: batchOperateType,
    previewConditionReq: params,
  }
  fetchDetailList(detailParams);
}
/**
 * 返回
 */
function goBack() {
  isDetail.value = false;
}
</script>

<style lang="less">
.info-box {
  .info-content {
    width: 100%;
    padding: 12px 16px;
    background-color: #F5F6FA;
    border-radius: 2px;
    text-align: left;

    .info-text {
      font-size: 14px;
      color: #4D4F56;
      line-height: 22px;
    }

    .info-tip {
      margin-top: 20px;
    }
  }
}
</style>

<style lang="less" scoped>
.manage {
  width: 100%;
  flex: 1;
  height: 100%;
  overflow: hidden;

  .manage-content {
    height: 100%;
    display: flex;
    flex-direction: column;
    margin: 16px 24px;
    box-sizing: border-box;
    padding-bottom: 122px;

    .manage-content-btn {
      width: 100%;
      display: flex;
      height: 42px;
      margin-bottom: 10px;
      align-items: center;

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
      width: 100%;
      height: 100%;
    }
  }

  .empty-content {
    height: 100%;
    padding-top: 10%;
  }
  
  .no-permission {
    height: calc(100% - 42px);
    padding-top: 10%;
    background-color: #fff;
    box-shadow: 0 2px 4px 0 #1919290d;
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

.btn-margin {
  margin-left: 10px
}

.htext {
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

    .manage-icon-arrows-right {
      margin: 0 4px;
    }
  }
}

.handover-dialog {

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

  .manage-icon-tips {
    width: 42px;
    height: 42px;
  }

  .mt30 {
    margin-top: 30px;
  }

  .option-btn {
    text-align: center;
    margin-top: 20px;

    .btn {
      width: 88px;
      margin-right: 10px;
    }
  }
}

.slider{

  ::v-deep .bk-modal-body {
    background-color: #F0F1F5;
  }

  .slider-content {
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

  .slider-detail {
    margin: 16px 24px;
    overflow: auto;
    height: calc(100vh - 120px);

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

    .go-back {
      margin-top: 18px;
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
    width: 960px;
    height: 230px;
    padding: 24px 48px;
    background: #FFFFFF;
    box-shadow: 0 -1px 6px 0 #DCDEE5;

    .footer-main {

      .main-line {
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
          font-size: 14px;
          margin-bottom: 24px;
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

          .remove-person {
            color: #63656e;
            font-size: 16px;
            font-weight: 700;
          }
        }
      }
      .remove-num {
        color: #3a84ff;
        font-size: 14px;
        font-weight: 700;
      }

      .main-line-remove {
        margin-top: 40px;
      }
    }

    .footer-btn {
      display: flex;
      align-items: center;
      position: absolute;
      bottom: 20px;

      .bk-button {
        margin-right: 8px;
      }

      p {
        display: flex;
        align-items: center;
        margin-left: 12px;
      }

      span {
        color: #4D4F56;
      }

      .info-circle {
        width: 14px;
        height: 14px;
        margin: 5px;
      }
    }
  }
}

.main-line-handover {
  margin-top: 26px;
}

.text-blue {
  color: #699DF4;
}

.remove-detail {
  cursor: pointer;
}
</style>
