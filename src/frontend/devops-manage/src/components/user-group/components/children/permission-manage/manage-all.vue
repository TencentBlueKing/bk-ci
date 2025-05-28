<template>
  <bk-loading class="manage" :loading="isLoading"  :zIndex="100">
    <div class="manage-search">
      <manage-search
        ref="manageSearchRef"
        @search-init="init"
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
          @page-change="getAsidePageChange"
          @get-person-list="handleShowPerson"
          @remove-confirm="asideRemoveConfirm"
          @handle-select-all="asideSelectAll"
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
          <no-permission
            :user-id="asideItem?.id"
          />
        </div>
      </div>
    </div>
    <bk-exception
      v-else-if="isNotProject"
      :description="t('请先选择项目')"
      scene="part"
      type="empty"
    >
    </bk-exception>
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
    :width="500"
    dialogType="show"
    header-align="center"
    :show-footer="false"
    class="remove-dialog"
    :is-show="showDeptListPermissionDialog"
    @closed="handleRemoveClosed"
  >
    <template #header>
      <img src="@/css/svg/warninfo.svg" class="manage-icon-tips">
      <h2 v-if="!isBatchOperate" class="dialog-header"> {{t('成功移除XX', [`${removeUserDeptListMap.removeUsers[0].id}(${removeUserDeptListMap.removeUsers[0].name})`])}}</h2>
      <h2 v-else class="dialog-header"> {{t('X个组织/用户已成功移出本项目', [removeUserDeptListMap.removeUsers.length])}}</h2>
    </template>
    <template #default>
      <i18n-t
        tag="p"
        keypath="用户XXX已从项目下移除，但其所属的如下组织架构在项目下拥有权限，此用户依然可以进入项目进行操作："
      >
        <span v-for="(item, index) in removeUserDeptListMap.removeUsers">
          {{ `${item.id} (${item.name})` }}
          <span v-if="index < removeUserDeptListMap.removeUsers.length - 1">, </span>
        </span>
      </i18n-t>
      <p v-for="dept in removeUserDeptListMap.list" :key="dept.id">
        - {{ dept.name }}
      </p>
      <p class="mt30">
        {{ t('请评估按照组织架构开通的权限是否合理。') }}
      </p>
      <div class="option-btn">
        <bk-button
          class="btn"
          @click="closeDeptListPermissionDialog"
        >
          {{ t('确定') }}
        </bk-button>
      </div>
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
    :title="title"
    :quick-close="false"
    ext-cls="slider"
    width="960"
    @hidden="batchCancel"
    :before-close="beforeClose"
  >
    <template #default>
      <div v-if="!isDetail" class="slider-content" :style="{height: invalidAuthorizationCount ? 'calc(100vh - 282px)' : 'calc(100vh - 226px)'}">
        <div class="slider-main">
          <p class="main-desc">
            <i18n-t keypath="已选择X个用户组" tag="div">
              <span class="desc-primary">{{ checkData.totalCount }}</span>
            </i18n-t>

            <template v-if="checkData.inoperableCount">
              <i18n-t keypath="；其中X个用户组X，" tag="span">
                <span class="desc-warn">{{ checkData.inoperableCount }}</span><span class="desc-warn">{{ t(unableText[batchFlag]) }}</span>
              </i18n-t>
              <span v-if="batchFlag === 'remove' && checkData.needToHandover">
                {{ t("需先完成交接。") }}
                <span class="remove-num remove-detail" @click="handleDetail">{{ t("查看详情") }}</span>
              </span>
              <span v-else>{{ t("本次操作将忽略。") }}</span>
            </template>
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
        <div class="slider-footer" :style="{height: invalidAuthorizationCount || batchFlag === 'renewal' ? '230px' : '170px'}">
          <div class="footer-main" :class="invalidAuthorizationCount ? '' : 'main-line-handover'">
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
            <div class="main-line">
              <div v-if="batchFlag === 'remove' || batchFlag === 'handover'">
                <p
                  v-if="invalidAuthorizationCount"
                  class="main-text"
                >
                  <span v-if="batchFlag === 'remove'">{{ t('移出以上用户组，将导致') }}</span>
                  <span v-if="batchFlag === 'handover'">{{ t('移交以上用户组，将导致') }}</span>

                  <span v-for="(item, index) in activeItems" :key="item.key">
                    <i18n-t :keypath="item.keypath" tag="span">
                      <span class="remove-num">{{ item.count }}</span>
                    </i18n-t>
                    <span>{{ index === activeItems.length - 1 ? '。' : '，' }}</span>
                  </span>
                  
                  <span class="remove-num remove-detail" @click="handleDetail">{{ t("查看详情") }}</span>
                  <p v-if="batchFlag === 'remove'">{{ t('请填写交接人，完成交接后才能成功移出。') }}</p>
                  <p v-if="batchFlag === 'handover'">{{ t('请确认是否同步移交授权。') }}</p>
                </p>
                <p v-else-if="batchFlag === 'remove'" class="main-label-remove">
                  <i18n-t keypath="确认从以上X个用户组中移出X吗？" tag="span">
                    <span class="remove-num">{{ checkData.operableCount }}</span><span class="remove-person">{{ userName }}</span>
                  </i18n-t>
                </p>
              </div>

              <div v-if="batchFlag === 'handover' || (batchFlag === 'remove' && checkData.needToHandover)"
              >
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
          </div>
          <div class="footer-btn">
            <bk-button
              :disabled="!checkData.canHandoverCount && !checkData.operableCount"
              :theme="batchFlag === 'remove' && !checkData.canHandoverCount && checkData.operableCount ? 'danger' : 'primary'"
              @click="batchConfirm(batchFlag)"
              :loading="batchBtnLoading"
            >
              {{batchFlag === 'remove' && checkData.needToHandover ? t("确认交接") : t(btnTexts[batchFlag])}}
            </bk-button>
            <bk-button @click="batchCancel">{{t("取消")}}</bk-button>
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
      </div>
    </template>
    <template #footer>
      <bk-button v-if="isDetail" class="go-back" @click="goBack">{{t("返回")}}</bk-button>
    </template>
  </bk-sideslider>
</template>

<script setup name="ManageAll">
import { useI18n } from 'vue-i18n';
import { useRoute } from 'vue-router';
import { Message, InfoBox } from 'bkui-vue';
import { ref, onMounted, computed, watch, h } from 'vue';
import ManageAside from './manage-aside.vue';
import GroupTab from './group-tab.vue';
import TimeLimit from './time-limit.vue';
import http from '@/http/api';
import ProjectUserSelector from '@/components/project-user-selector';
import NoPermission from '../no-enable-permission/no-permission.vue';
import userGroupTable from "@/store/userGroupTable";
import useManageAside from "@/store/manageAside";
import manageSearch from "./manage-search.vue";
import { storeToRefs } from 'pinia';
import { batchOperateTypes, btnTexts, batchTitle, batchMassageText } from "@/utils/constants.js";
import userDetailGroupTable from "@/store/userDetailGroupTable";
import DetailGroupTab from "./detail-group-tab.vue";
import { AngleRight  } from 'bkui-vue/lib/icon';

const batchBtnLoading = ref(false);
const { t } = useI18n();
const route = useRoute();
const formRef = ref(null);
const renewalRef = ref(null);
const projectId = computed(() => route.params?.projectCode || route.query?.projectCode);
const isNotProject = computed(() => projectId.value === 'my-project' || !projectId.value);
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
const checkData = ref();
const invalidAuthorizationCount = ref();
const renewalLoading = ref(false);
const handoverLoading = ref(false);
const removerLoading = ref(false);
const loadingMap = {
  renewal: renewalLoading,
  handover: handoverLoading,
  remove: removerLoading
};
const searchGroup = ref();
const manageAsideRef = ref(null);
const manageSearchRef = ref(null);
const groupTableStore = userGroupTable();
const manageAsideStore = useManageAside();
const detailGroupTable = userDetailGroupTable();
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
  remove: t('无法直接移出'),
}
const isBatchOperate = ref(false)
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
  clearPaginations,
} = groupTableStore;

const {
  asideItem,
  memberList,
  personList,
  tableLoading,
  isLoading,
  activeTab,
  memberPagination,
  removeUserDeptListMap,
  showDeptListPermissionDialog,
} = storeToRefs(manageAsideStore);
const {
  handleAsideClick,
  handleAsidePageChange,
  handleShowPerson,
  handleAsideRemoveConfirm,
  getProjectMembers,
  asideSelectAll,
} = manageAsideStore;

const {
  detailSourceList
} = storeToRefs(detailGroupTable);
const {
  fetchDetailList,
  detailCollapseClick,
  detailPageLimitChange,
  detailPageValueChange,
} = detailGroupTable;
const isDetail = ref(false);
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
const activeItems = computed(() => {
  const items = [
    {
      key: 'pipeline',
      keypath: 'X个流水线权限代持失效',
      count: checkData.value.invalidPipelineAuthorizationCount
    },
    {
      key: 'repository',
      keypath: 'X个代码库授权失效',
      count: checkData.value.invalidRepositoryAuthorizationCount
    },
    {
      key: 'manager',
      keypath: 'X个资源没有拥有者',
      count: checkData.value.uniqueManagerCount,
      condition: batchFlag.value === 'remove'
    },
    {
      key: 'envNode',
      keypath: 'X个环境节点授权失效',
      count: checkData.value.invalidEnvNodeAuthorizationCount
    }
  ];

  return items.filter(item => item.count && (item.condition === undefined || item.condition));
})
onMounted(() => {
  init(true);
});
watch(projectId, () => {
  init(true);
});
const value = ref();
function abc(params) {
  console.log("🚀 ~ abc ~ params:", params)
  
}
function ccc(params) {
  console.log("🚀 ~ ccc ~ params:", params)
  
}

function init (flag, searchValue) {
  searchGroup.value = searchValue
  memberPagination.value.current = 1;
  asideItem.value = undefined;
  getProjectMembers(projectId.value, flag, searchValue);
}
function asideClick (item) {
  handleAsideClick(item, projectId.value);
}
function getAsidePageChange (current, projectId) {
  handleAsidePageChange(current, projectId, searchGroup.value)
}
async function refresh () {
  manageSearchRef.value?.clearSearch();
  getProjectMembers(projectId.value, true);
}
/**
 * 移交弹窗打开时
 */
function handOverDialog (row, resourceType, index) {
  formRef.value?.clearValidate();
  handleHandOver(row, resourceType, index);
}
/**
 * 续期弹窗提交事件
 */
async function handleRenewalConfirm () {
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
function handleRenewalClosed () {
  cancelClear('renewal');
  isShowRenewal.value = false;
}
/**
 * 移交弹窗提交事件
 */
async function handleHandoverConfirm () {
  const isValidate = await formRef.value.validate();
  if (!isValidate) return;
  const param = formatSelectParams({
    id: selectedRow.value.groupId,
    memberType: selectedRow.value.memberType
  });
  delete param.renewalDuration;
  if (asideItem.value.id === handOverForm.value.id) {
    showMessage('error', t('目标对象和交接人不允许相同。'));
    return
  }
  try {
    operatorLoading.value = true;
    const res = await http.batchHandover(projectId.value, param);
    if (res) {
      operatorLoading.value = false;
      showMessage('success', t('用户组权限已移交给X。',[`${handOverForm.value.id}(${handOverForm.value.name})`]));
      isShowHandover.value = false;
      handleRemoveRow();
      cancelClear('handover');
    }
  } catch (error) {
    console.error(error)
    operatorLoading.value = false;
  }
};
/**
 * 移交弹窗关闭
 */
 function handleHandoverClosed () {
  cancelClear('handover');
  isShowHandover.value = false;
}
/**
 * 移出弹窗提交事件
 */
async function handleRemoveConfirm () {
  try {
    operatorLoading.value = true;
    const res = await http.getIsDirectRemove(projectId.value, selectedRow.value.groupId, asideItem.value);
    if (res) {
      operatorLoading.value = false;
      showMessage('success', t('X 已移出X用户组。', [`${asideItem.value.id}(${asideItem.value.name})`, selectedRow.value.groupName]));
      handleRemoveRow();
      isShowRemove.value = false;
    }
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
function handleChangeTime (value) {
  expiredAt.value = Number(value);
};
function handleSelectAll(resourceType, asideItem){
  handleSelectAllData(resourceType, asideItem)
}
/**
 * 批量操作
 * @param flag 按钮标识
 */
async function batchOperator (flag) {
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
    checkData.value = res;
    invalidAuthorizationCount.value = res.invalidPipelineAuthorizationCount
      + res.invalidRepositoryAuthorizationCount
      + res.uniqueManagerCount
      + res.invalidEnvNodeAuthorizationCount
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
        title: t('批量移出操作尚未完成，确认放弃操作吗？'),
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

function batchCancel () {
  cancelClear(batchFlag.value);
  isDetail.value = false;
}
/**
 * 批量操作请求参数获取
 * @param batchFlag 按钮标识
 */
function formatSelectParams (rowGroupId) {
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
function cancelClear (batchFlag) {
  isShowSlider.value = false;

  if (batchFlag === 'handover') {
    handOverForm.value && (Object.assign(handOverForm.value, getHandOverForm()));
    formRef.value?.clearValidate()
  } else if (batchFlag === 'renewal') {
    renewalRef.value.initTime();
    expiredAt.value = 30;
  }
  clearPaginations();
}
/**
 * 侧边栏确认事件
 * @param batchFlag 按钮标识
 */
async function batchConfirm (batchFlag) {

  let res = null;
  const params = formatSelectParams();

  try {
    switch (batchFlag) {
      case 'renewal':
        batchBtnLoading.value = true;
        res = await http.batchRenewal(projectId.value, params);
        break;
      case 'handover':
        if (!(await validateFormAndUser())) return;
        batchBtnLoading.value = true;
        res = await http.batchHandover(projectId.value, params);
        break;
      case 'remove':
        if (!(await validateRemoveCondition())) return;
        batchBtnLoading.value = true;
        res = await http.batchRemove(projectId.value, params);
        break;
    }

    if (res) {
      showMessage('success', t(batchMassageText[batchFlag]));
      batchBtnLoading.value = false;
      cancelClear(batchFlag);
      getProjectMembers(projectId.value, true);
    }
  } catch (error) {
    batchBtnLoading.value = false;
  }
}
async function validateFormAndUser() {
  const isValid = await formRef.value.validate();
  if (!isValid) return false;

  if (asideItem.value.id === handOverForm.value.id) {
    showMessage('error', t('目标对象和交接人不允许相同。'));
    return false;
  }

  return true;
}

async function validateRemoveCondition() {
  if (!checkData.value.needToHandover && checkData.value.operableCount) return true;

  return await validateFormAndUser();
}

function showMessage (theme, message) {
  Message({
    theme: theme,
    message: message,
  });
}

function asideRemoveConfirm (isBatch, removeUsers, handOverForm) {
  isBatchOperate.value = isBatch
  handleAsideRemoveConfirm(isBatch, removeUsers, handOverForm, projectId.value, manageAsideRef.value);
}

function handleChangeOverFormName ({list, userList}) {
  if(!list){
    Object.assign(handOverForm.value, getHandOverForm());
    return;
  }
  const val = list.join(',')
  handOverForm.value = userList.find(i => i.id === val)
}

function handleClearOverFormName () {
  Object.assign(handOverForm.value, getHandOverForm());
}

function closeDeptListPermissionDialog () {
  showDeptListPermissionDialog.value = false
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
    previewConditionReq: {
      ...params,
      operateChannel: 'MANAGER'
    },
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

.no-permission {
  height: calc(100% - 42px);
  padding-top: 10%;
  margin-top: 42px;
  background-color: #fff;
  box-shadow: 0 2px 4px 0 #1919290d;
}

.slider{

  .slider-content {
    overflow: auto;

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
    margin: 15px 0 24px;
    overflow: auto;
    height: calc(100vh - 135px);
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

  .go-back {
    position: absolute;
    bottom: 5px;
  }

  ::v-deep .bk-modal-body {
    background-color: #F0F1F5;
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
          margin-bottom: 20px;
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
          margin-top: 24px;

          .remove-person {
            color: #63656e;
            font-size: 14px;
            font-weight: 700;
          }
        }
      }

      .main-line-handover {
        margin-top: 26px;
      }
    }

    .footer-btn {
      position: absolute;
      bottom: 20px;

      .bk-button {
        margin-right: 8px;
      }
    }
  }
}

.text-blue {
  color: #699DF4;
}

.remove-num {
  color: #3a84ff;
  font-size: 14px;
  font-weight: 700;
}

.remove-detail {
  cursor: pointer;
}
</style>
