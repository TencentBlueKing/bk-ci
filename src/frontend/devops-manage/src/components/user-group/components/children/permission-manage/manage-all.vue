<template>
  <bk-loading class="manage" :loading="isLoading">
    <div class="manage-search">
      <bk-search-select
        v-model="searchValue"
        :data="searchData"
        unique-select
        class="multi-search"
        value-behavior="need-key"
        :placeholder="t('用户/组织架构')"
      />
    </div>
    <div class="manage-article">
      <div class="manage-aside">
        <manage-aside
          ref="manageAsideRef"
          :project-id="projectId"
          :member-list="memberList"
          :person-list="personList"
          :over-table="overTable"
          @handle-click="asideClick"
          @page-change="handleAsidePageChange"
          @get-person-list="handleShowPerson"
          @remove-confirm="asideRemoveConfirm"
        />
      </div>
      <div class="manage-content">
        <div class="manage-content-btn">
          <bk-button :disabled="!isPermission" @click="batchRenewal">{{t("批量续期")}}</bk-button>
          <bk-button :disabled="!isPermission" @click="batchHandover" v-if="asideItem?.type==='USER'">{{t("批量移交")}}</bk-button>
          <bk-button :disabled="!isPermission" @click="batchRemove">{{t("批量移出")}}</bk-button>
        </div>
        <div v-if="isPermission" class="group-tab">
          <GroupTab
            :is-show-operation="true"
            :aside-item="asideItem"
            :source-list="sourceList"
            :selected-data="selectedData"
            @collapse-click="collapseClick"
            @handle-renewal="handleRenewal"
            @handle-hand-over="handleHandOver"
            @handle-remove="handleRemove"
            @get-select-list="getSelectList"
            @handle-select-all-data="handleSelectAll"
            @handle-load-more="handleLoadMore"
            @handle-clear="handleClear"
          />
        </div>
        <div v-else class="no-permission">
          <no-permission />
        </div>
      </div>
    </div>
  </bk-loading>
  <bk-dialog
    :width="640"
    theme="danger"
    :confirm-text="t('提交')"
    class="renewal-dialog"
    :is-show="isShowRenewal"
    @closed="handleRenewalClosed"
    @confirm="handleRenewalConfirm"
  >
    <template #header>
      {{t("续期")}}
      <span class="dialog-header"> {{asideItem.name}} </span>
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
        <span>{{t("到期时间")}}：</span> 已过期 ——> {{ selectedRow?.expiredAt }}
      </p>
    </template>
  </bk-dialog>
  <bk-dialog
    :width="640"
    theme="danger"
    :confirm-text="t('移交')"
    class="handover-dialog"
    :is-show="isShowHandover"
    @closed="handleHandoverClosed"
    @confirm="handleHandoverConfirm"
  >
    <template #header>
      {{t("移交")}}
      <span class="dialog-header"> {{asideItem.name}} </span>
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
            :label="t('移交给')"
          >
            <bk-input
              v-model="handOverForm.name"
              :placeholder="t('请输入')"
              clearable
            />
          </bk-form-item>
        </bk-form>
      </p>
    </template>
  </bk-dialog>
  <bk-dialog
    :width="450"
    confirmButtonTheme="danger"
    :cancel-text="t('关闭')"
    :confirm-text="t('确认移出')"
    header-align="center"
    footer-align="center"
    class="remove-dialog"
    :is-show="isShowRemove"
    @closed="() => isShowRemove = false"
    @confirm="handleRemoveConfirm"
  >
    <template #header>
      <span class="dialog-header"> {{t("确认从用户组中移出用户吗")}}？ </span>
    </template>
    <template #default>
      <p class="remove-text">
        <span>{{t("待移出用户")}}：</span> {{asideItem.name}}
      </p>
      <p class="remove-text">
        <span>{{t("所在用户组")}}：</span> {{ selectedRow?.groupName }}
      </p>
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
            <span class="desc-primary"> {{ selectedLength }} </span>
          </i18n-t>
          <i18n-t v-if="unableMoveLength" keypath="；其中X个用户组无法移出，本次操作将忽略" tag="div">
            <span class="desc-warn"> {{ unableMoveLength }} </span>
            <span class="desc-warn">{{t("无法移出")}}</span>
          </i18n-t>
        </p>
        <div>
          <!-- 这个要有分页的点击事件 -->
          <GroupTab
            :source-list="selectSourceList"
            :is-show-operation="false"
            :aside-item="asideItem"
            @page-limit-change="pageLimitChange"
            @page-value-change="pageValueChange"
          />
        </div>
      </div>
      <div class="slider-footer">
        <div class="footer-main">
          <div v-if="sliderTitle === t('批量续期')">
            <div class="main-line">
              <p class="main-label">{{t("续期对象")}}</p>
              <span class="main-text">{{("用户")}}： {{ asideItem.name }}</span>
            </div>
            <div class="main-line">
              <p class="main-label">{{("续期时长")}}</p>
              <TimeLimit ref="renewalRef" @change-time="handleChangeTime" />
            </div>
          </div>
          <div v-if="sliderTitle === t('批量移交')">
            <div class="main-line" style="margin-top: 26px;">
              <p class="main-label">{{("移交给")}}</p>
              <bk-form
                ref="formRef"
                :rules="rules"
                :model="handOverForm"
              >
                <bk-form-item
                  required
                  property="name"
                >
                  <bk-input
                    v-model="handOverForm.name"
                    :placeholder="t('请输入')"
                    clearable
                  />
                </bk-form-item>
              </bk-form>
            </div>
          </div>
          <div v-if="sliderTitle === t('批量移出')">
            <div class="main-line" style="margin-top: 40px;">
              <p class="main-label-remove">
                <i18n-t keypath="确认从以上X个用户组中移出X吗？" tag="div">
                  <span class="remove-num">{{ selectedLength }}</span>
                  <span class="remove-person">{{ asideItem.name }}</span>
                </i18n-t>
              </p>
            </div>
          </div>
        </div>
        <div class="footer-btn">
          <bk-button v-if="batchFlag === 'renewal'" theme="primary" @click="batchConfirm('renewal')">{{t("确定续期")}}</bk-button>
          <bk-button v-if="batchFlag === 'handover'" theme="primary" @click="batchConfirm('handover')">{{t("确定移交")}}</bk-button>
          <bk-button v-if="batchFlag === 'remove'" theme="danger" @click="batchConfirm('remove')">{{t("确定移出")}}</bk-button>
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
import NoPermission from '../no-enable-permission/no-permission.vue';
import userGroupTable from "@/store/userGroupTable";
import useManageAside from "@/store/manageAside";
import { storeToRefs } from 'pinia';

const { t } = useI18n();
const route = useRoute();
const formRef = ref('');
const renewalRef = ref(null);
const projectId = computed(() => route.params?.projectCode);
const expiredAt = ref();
const isLoading = ref(false);
const isShowSlider = ref(false);
const sliderTitle = ref();
const batchFlag = ref();
const handOverForm = ref({
  name: '',
});
const rules = {
  name: [
    { required: true, message: t('请输入移交人'), trigger: 'blur' },
  ],
};
const searchValue = ref([]);
const searchData = ref([
  {
    name: '用户',
    id: 1,
  },
  {
    name: '组织架构',
    id: 2,
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
]);
const isPermission = ref(true);
const manageAsideRef = ref(null);
const groupTableStore = userGroupTable();
const manageAsideStore = useManageAside();

const {
  sourceList,
  isShowRenewal,
  isShowHandover,
  isShowRemove,
  selectedData,
  selectedLength,
  unableMoveLength,
  selectSourceList,
  selectedRow,
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
  overTable,
} = storeToRefs(manageAsideStore);
const {
  handleAsideClick,
  handleAsidePageChange,
  handleShowPerson,
  handleAsideRemoveConfirm,
  getProjectMembers,
} = manageAsideStore;

onMounted(() => {
  getProjectMembers(projectId.value);
});

watch(searchValue, (nv) => {
  manageAsideStore.userName = '';
  nv.forEach((val) => {
    if (val.id === 1) {
      manageAsideStore.userName = val?.values[0]?.name;
    };
  });
  getProjectMembers(projectId.value);
});

function asideClick(item){
  handleAsideClick(item, projectId.value);
}
/**
 * 续期弹窗提交事件
 */
function handleRenewalConfirm() {
  console.log(expiredAt.value, '授权期限');
  // expiredAt 需要处理下
  handleUpDateRow(expiredAt.value);
  renewalRef.value.initTime();
  isShowRenewal.value = false;
};
/**
 * 续期弹窗关闭
 */
function handleRenewalClosed() {
  renewalRef.value.initTime();
  isShowRenewal.value = false;
}
/**
 * 移交弹窗提交事件
 */
async function handleHandoverConfirm() {
  console.log(handOverForm.value, '移交数据');
  const isValidate = await formRef.value.validate();
  if(!isValidate) return;
  handleRemoveRow();
  handOverForm.value.name = '';
  isShowHandover.value = false;
};
/**
 * 移交弹窗关闭
 */
 function handleHandoverClosed() {
   handOverForm.value.name = '';
   formRef.value?.clearValidate();
   isShowHandover.value = false;
}
/**
 * 移出弹窗提交事件
 */
function handleRemoveConfirm() {
  console.log(asideItem,'移出的数据');
  handleRemoveRow();
  isShowRemove.value = false;
}
/**
 * 授权期限选择
 */
function handleChangeTime(value) {
  expiredAt.value = value;
};
function handleSelectAll(resourceType, asideItem){
  handleSelectAllData(resourceType, asideItem)
}
/**
 * 批量续期
 */
function batchRenewal() {
  if (!selectedLength.value) {
    Message(t('请先选择用户组'));
    return;
  }
  sliderTitle.value = t('批量续期');
  batchFlag.value = 'renewal';
  isShowSlider.value = true;
  getSourceList();
}
/**
 * 批量移交
 */
function batchHandover() {
  if (!selectedLength.value) {
    Message(t('请先选择用户组'));
    return;
  }
  sliderTitle.value = t('批量移交');
  batchFlag.value = 'handover';
  isShowSlider.value = true;
  getSourceList();
}
/**
 * 批量移出
 */
function batchRemove() {
  if (!selectedLength.value) {
    Message(t('请先选择用户组'));
    return;
  }
  sliderTitle.value = t('批量移出');
  batchFlag.value = 'remove';
  isShowSlider.value = true;
  getSourceList();
}
/**
 * sideslider 关闭
 */
function batchCancel() {
  if(formRef.value){
    handOverForm.value.name = '';
    formRef.value.clearValidate();
  }
  isShowSlider.value = false;
}
/**
 *  侧边栏确认事件
 */
async function batchConfirm(batchFlag) {
  if (batchFlag === 'renewal') {
    const params = [{
      member: asideItem.value.name,
      groupId: asideItem.value.id,
      expiredAt: expiredAt.value,
    }];
    console.log(params,'参数',asideItem.value);
    const res = await http.batchRenewal(projectId.value, params);
    renewalRef.value.initTime(); 
  } else if (batchFlag === 'handover') {
    const flag = await formRef.value.validate();
    if (flag) {
      const params = [{
        groupId: asideItem.value.id,
        handoverFrom: asideItem.value.name,
        handoverTo: handOverForm.value.name,
      }];
      const res = await http.batchHandover(projectId.value, params);
    }
    handOverForm.value.name = '';
  } else if (batchFlag === 'remove') {
    const params = [{
      groupId: asideItem.value.id,
      member: asideItem.value.name,
    }];
    const res = await http.batchRemove(projectId.value, params);
  }

  setTimeout(() => {
    isShowSlider.value = false;
  }, 1000);
}

function asideRemoveConfirm(value) {
  handleAsideRemoveConfirm(value, manageAsideRef.value);
}
</script>

<style lang="less" scoped>
.manage {
  width: 100%;
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
      // flex: 1;
    }
  }

  .manage-article {
    height: calc(100% - 104px);
    display: flex;
    margin: 16px 0 24px 24px;

    .manage-aside {
      position: relative;
      width: 230px;
      padding: 8px 0 0;
      background: #FFFFFF;
      box-shadow: 0 2px 4px 0 #1919290d;
    }

    .manage-content {
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

      .manage-content-btn {
        margin-bottom: 10px;

        .bk-button {
          margin-right: 8px
        }
      }

      .group-tab {
        width: 100%;
        height: 100%;
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
      text-align: right;
      color: #63656E;
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
      width: 100px;
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
}

.no-permission {
  width: calc(100% - 24px);
  height: calc(100% - 42px);
  padding-top: 120px;
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
      }

      .desc-warn {
        color: #FF9C01;
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

        .bk-input {
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
</style>
