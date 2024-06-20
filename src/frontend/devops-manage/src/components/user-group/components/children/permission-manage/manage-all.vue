<template>
  <bk-loading class="manage" :loading="isLoading">
    <div class="manage-search">
      <bk-search-select
        v-model="searchValue"
        :data="searchData"
        unique-select
        class="multi-search"
        placeholder="用户/组织架构"
      />
    </div>
    <div class="manage-article">
      <div class="manage-aside">
        <manage-aside
          ref="manageAsideRef"
          :project-id="projectId"
          :member-list="manageAsideStore.memberList"
          :person-list="manageAsideStore.personList"
          :over-table="manageAsideStore.overTable"
          @handle-click="manageAsideStore.handleAsideClick"
          @page-change="manageAsideStore.handleAsidePageChange"
          @get-person-list="manageAsideStore.handleShowPerson"
          @remove-confirm="handleAsideRemoveConfirm"
        />
      </div>
      <div class="manage-content">
        <div class="manage-content-btn">
          <bk-button :disabled="!isPermission" @click="batchRenewal">批量续期</bk-button>
          <bk-button :disabled="!isPermission" @click="batchHandover" v-if="manageAsideStore.asideItem?.type==='USER'">批量移交</bk-button>
          <bk-button :disabled="!isPermission" @click="batchRemove">批量移出</bk-button>
        </div>
        <div v-if="isPermission" class="group-tab">
          <GroupTab
            :is-show-operation="true"
            :aside-item="manageAsideStore.asideItem"
            :source-list="groupTableStore.sourceList"
            :selected-data="groupTableStore.selectedData"
            @collapse-click="groupTableStore.collapseClick"
            @handle-renewal="groupTableStore.handleRenewal"
            @handle-hand-over="groupTableStore.handleHandOver"
            @handle-remove="groupTableStore.handleRemove"
            @get-select-list="groupTableStore.getSelectList"
            @handle-select-all-data="groupTableStore.handleSelectAllData"
            @handle-load-more="groupTableStore.handleLoadMore"
            @handle-clear="groupTableStore.handleClear"
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
    confirm-text="提交"
    class="renewal-dialog"
    :is-show="groupTableStore.isShowRenewal"
    @closed="() => groupTableStore.isShowRenewal = false"
    @confirm="handleRenewalConfirm"
  >
    <template #header>
      续期
      <span class="dialog-header"> {{manageAsideStore.asideItem.name}} </span>
    </template>
    <template #default>
      <p class="renewal-text">
        <span>用户组名：</span> 开发人员
      </p>
      <p class="renewal-text">
        <span class="required">授权期限</span>
        <TimeLimit @change-time="handleChangeTime" />
      </p>
      <p class="renewal-text">
        <span>到期时间：</span> 已过期
      </p>
    </template>
  </bk-dialog>
  <bk-dialog
    :width="640"
    theme="danger"
    confirm-text="移交"
    class="handover-dialog"
    :is-show="groupTableStore.isShowHandover"
    @closed="() => groupTableStore.isShowHandover = false"
    @confirm="handleHandoverConfirm"
  >
    <template #header>
      移交
      <span class="dialog-header"> {{manageAsideStore.asideItem.name}} </span>
    </template>
    <template #default>
      <p class="handover-text">
        <span>用户组名：</span> 开发人员
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
            label="移交给"
          >
            <bk-input
              v-model="handOverForm.name"
              placeholder="请输入"
              clearable
            />
          </bk-form-item>
        </bk-form>
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
    :is-show="groupTableStore.isShowRemove"
    @closed="() => groupTableStore.isShowRemove = false"
    @confirm="handleRemoveConfirm"
  >
    <template #header>
      <span class="dialog-header"> 确认从用户组中移出用户吗？ </span>
    </template>
    <template #default>
      <p class="remove-text">
        <span>待移出用户：</span> {{manageAsideStore.asideItem.name}}
      </p>
      <p class="remove-text">
        <span>所在用户组：</span> 开发人员
      </p>
    </template>
  </bk-dialog>

  <bk-sideslider
    v-model:isShow="isShowSlider"
    :title="sliderTitle"
    ext-cls="slider"
    width="960"
  >
    <template #default>
      <div class="slider-main">
        <p class="main-desc">
          已选择<span class="desc-primary"> {{ selectedLength }} </span>个用户组
          <span>；其中
            <span class="desc-warn"> {{ groupTableStore.unableMoveLength }} </span>个用户组<span class="desc-warn">无法移出</span>，本次操作将忽略
          </span>
        </p>
        <div>
          <!-- 这个要有分页的点击事件 -->
          <GroupTab
            :project-table="groupTableStore.selectProjectlist"
            :source-list="groupTableStore.selectSourceList"
            :is-show-operation="false"
            :pagination="groupTableStore.pagination"
            :aside-item="manageAsideStore.asideItem"
          />
        </div>
      </div>
      <div class="slider-footer">
        <div class="footer-main">
          <div v-if="sliderTitle === '批量续期'">
            <div class="main-line">
              <p class="main-label">续期对象</p>
              <span class="main-text">用户： {{ manageAsideStore.asideItem.name }}</span>
            </div>
            <div class="main-line">
              <p class="main-label">续期时长</p>
              <TimeLimit @change-time="handleChangeTime" />
            </div>
          </div>
          <div v-if="sliderTitle === '批量移交'">
            <div class="main-line" style="margin-top: 26px;">
              <p class="main-label">移交给</p>
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
                    placeholder="请输入"
                    clearable
                  />
                </bk-form-item>
              </bk-form>
            </div>
          </div>
          <div v-if="sliderTitle === '批量移出'">
            <div class="main-line" style="margin-top: 40px;">
              <p class="main-label-remove">
                确认从以上
                <span class="remove-num">{{ selectedLength }}</span>
                个用户组中移出
                <span class="remove-person">{{ manageAsideStore.asideItem.name }}</span>
                吗？
              </p>
            </div>
          </div>
        </div>
        <div class="footer-btn">
          <bk-button v-if="batchFlag === 'renewal'" theme="primary" @click="batchConfirm('renewal')">确定续期</bk-button>
          <bk-button v-if="batchFlag === 'handover'" theme="primary" @click="batchConfirm('handover')">确定移交</bk-button>
          <bk-button v-if="batchFlag === 'remove'" theme="danger" @click="batchConfirm('remove')">确定移出</bk-button>
          <bk-button @click="batchCancel">取消</bk-button>
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

const { t } = useI18n();
const route = useRoute();
const formRef = ref('');
const projectId = computed(() => route.params?.projectCode);
const expiredAt = ref();
const isLoading = ref(false);
const isShowSlider = ref(false);
const sliderTitle = ref('批量续期');
const batchFlag = ref();
const handOverForm = ref({
  name: '',
});
const rules = {
  name: [
    { required: true, message: '请输入移交人', trigger: 'blur' },
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
const selectedLength = computed(() => Object.keys(groupTableStore.selectedData).length);
const groupTableStore = userGroupTable();
const manageAsideStore = useManageAside();

onMounted(() => {
  manageAsideStore.getProjectMembers(projectId.value);
});

watch(searchValue, (nv) => {
  manageAsideStore.userName = '';
  nv.forEach((val) => {
    if (val.id === 1) {
      manageAsideStore.userName = val?.values[0]?.name;
    };
  });
  manageAsideStore.getProjectMembers(projectId.value);
});
/**
 * 续期弹窗提交事件
 */
function handleRenewalConfirm() {
  console.log(expiredAt.value, '授权期限');
  groupTableStore.isShowRenewal = false;
};
/**
 * 移交弹窗提交事件
 */
function handleHandoverConfirm() {
  console.log(handOverForm.value,'移交数据');
  groupTableStore.isShowHandover = false;
};
/**
 * 移出弹窗提交事件
 */
function handleRemoveConfirm() {
  console.log(manageAsideStore.asideItem,'移出的数据');
  groupTableStore.isShowRemove = false;
}
/**
 * 授权期限选择
 */
function handleChangeTime(value) {
  expiredAt.value = value;
};
/**
 * 批量续期
 */
function batchRenewal() {
  if (!selectedLength.value) {
    Message('请先选择用户组');
    return;
  }
  sliderTitle.value = '批量续期';
  batchFlag.value = 'renewal';
  isShowSlider.value = true;
  groupTableStore.getSourceList();
}
/**
 * 批量移交
 */
function batchHandover() {
  if (!selectedLength.value) {
    Message('请先选择用户组');
    return;
  }
  sliderTitle.value = '批量移交';
  batchFlag.value = 'handover';
  isShowSlider.value = true;
  groupTableStore.getSourceList();
}
/**
 * 批量移出
 */
function batchRemove() {
  if (!selectedLength.value) {
    Message('请先选择用户组');
    return;
  }
  sliderTitle.value = '批量移出';
  batchFlag.value = 'remove';
  isShowSlider.value = true;
  groupTableStore.getSourceList();
}
/**
 * sideslider 关闭
 */
function batchCancel() {
  handOverForm.value.name = '';
  formRef.value?.clearValidate();
  isShowSlider.value = false;
}
/**
 *  侧边栏确认事件
 */
async function batchConfirm(batchFlag) {
  if (batchFlag === 'renewal') {
    const params = [{
      member: manageAsideStore.asideItem.name,
      groupId: manageAsideStore.asideItem.id,
      expiredAt: expiredAt.value,
    }];
    const res = await http.batchRenewal(projectId.value, params);
  } else if (batchFlag === 'handover') {
    const flag = await formRef.value.validate();
    if (flag) {
      const params = [{
        groupId: manageAsideStore.asideItem.id,
        handoverFrom: manageAsideStore.asideItem.name,
        handoverTo: handOverForm.value.name,
      }];
      const res = await http.batchHandover(projectId.value, params);
    }
  } else if (batchFlag === 'remove') {
    const params = [{
      groupId: manageAsideStore.asideItem.id,
      member: manageAsideStore.asideItem.name,
    }];
    const res = await http.batchRemove(projectId.value, params);
  }

  setTimeout(() => {
    isShowSlider.value = false;
    handOverForm.value.name = '';
    formRef.value?.clearValidate();
  }, 1000);
}

function handleAsideRemoveConfirm(value) {
  manageAsideStore.handleAsideRemoveConfirm(value, manageAsideRef.value);
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
      flex: 1;
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
