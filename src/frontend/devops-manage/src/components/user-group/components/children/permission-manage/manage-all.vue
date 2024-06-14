<template>
  <bk-loading class="manage" :loading="isLoading">
    <div class="manage-search">
      <bk-input
        class="multi-search"
        v-model="searchValue"
        placeholder="请输入用户名"
        @blur="() => getProjectMembers()"
      />
    </div>
    <div class="manage-article">
      <div class="manage-aside">
        <manage-aside
          ref="manageAsideRef"
          :member-list="memberList"
          :person-list="personList"
          :over-table="handOverTable"
          @handle-click="handleAsideClick"
          @page-change="handleAsidePageChange"
          @get-person-list="handleShowPerson"
          @remove-confirm="handleAsideRemoveConfirm"
        />
      </div>
      <div class="manage-content">
        <div class="manage-content-btn">
          <bk-button :disabled="!isPermission" @click="batchRenewal">批量续期</bk-button>
          <bk-button :disabled="!isPermission" @click="batchHandover" v-if="asideItem?.type==='USER'">批量移交</bk-button>
          <bk-button :disabled="!isPermission" @click="batchRemove">批量移出</bk-button>
        </div>
        <div v-if="isPermission">
          <GroupTable
            :is-show-operation="true"
            :aside-item="asideItem"
            @renewal="handleRenewal"
            @handover="handleHandover"
            @remove="handleRemove"
            @get-select-list="getSelectList"
          />
        </div>
        <div v-else class="no-permission">
          <bk-exception
            class="exception-wrap-item exception-part"
            type="empty"
            scene="part"
            description="该用户暂无项目权限"
            :class="{'exception-gray': isGray}"
          >
            <p class="empty-text">
              由于该用户仍有部分授权未移交，未能自动移出项目；如有需要，可前往「
              <bk-button text theme="primary">
                授权管理
              </bk-button>
              」处理
            </p>
          </bk-exception>
        </div>
      </div>
    </div>
  </bk-loading>
  <bk-dialog
    :width="640"
    theme="danger"
    confirm-text="提交"
    class="renewal-dialog"
    :is-show="isShowRenewal"
    @closed="() => isShowRenewal = false"
    @confirm="handleRenewalConfirm"
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
        <TimeLimit @change-time="handleChangeTime" />
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
    @confirm="handleRemoveConfirm"
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

  <bk-sideslider
    v-model:isShow="isShowSlider"
    :title="sliderTitle"
    ext-cls="slider"
    width="960"
  >
    <template #default>
      <div class="slider-main">
        <p class="main-desc">
          已选择<span class="desc-primary"> {{ selectList.length }} </span>个用户组
          <span>；其中
            <span class="desc-warn"> {{ unableMove.length }} </span>个用户组<span class="desc-warn">无法移出</span>，本次操作将忽略
          </span>
        </p>
        <div>
          <GroupTable
            :is-show-operation="false"
            :pagination="pagination"
            :aside-item="asideItem"
          />
        </div>
      </div>
      <div class="slider-footer">
        <div class="footer-main">
          <div v-if="sliderTitle === '批量续期'">
            <div class="main-line">
              <p class="main-label">续期对象</p>
              <span class="main-text">用户： {{ asideItem.name }}</span>
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
                <span class="remove-num">{{ selectList.length }}</span>
                个用户组中移出
                <span class="remove-person">{{ asideItem.name }}</span>
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
import { ref, onMounted, computed, nextTick } from 'vue';
import ManageAside from './manage-aside.vue';
import GroupTable from './group-table.vue';
import TimeLimit from './time-limit.vue';
import http from '@/http/api';

const { t } = useI18n();
const route = useRoute();
const formRef = ref('');
const projectId = computed(() => route.params?.projectCode);
const expiredAt = ref();
const isLoading = ref(false);
const isShowRenewal = ref(false);
const isShowHandover = ref(false);
const isShowRemove = ref(false);
const isShowSlider = ref(false);
const sliderTitle = ref('批量续期');
const batchFlag = ref();
const pagination = ref({ limit: 10 });
const memberPagination = ref({ limit: 10, current: 1 });
const handOverForm = ref({
  name: '',
});
const rules = {
  name: [
    { required: true, message: '请输入移交人', trigger: 'blur' },
  ],
};
const searchValue = ref('');
const memberList = ref([]);
const asideItem = ref();
const selectList = ref([]);
const unableMove = ref([]);
const isPermission = ref(true);
const personList = ref([]);
const manageAsideRef = ref(null);
const handOverTable = ref([]);

onMounted(() => {
  getProjectMembers();
});

/**
 * 获取项目下全体成员
 */
async function getProjectMembers() {
  const params = {
    page: memberPagination.value.current,
    pageSize: memberPagination.value.limit,
  };

  if (searchValue.value) {
    params.userName = searchValue.value;
  }
  const res = await http.getProjectMembers(projectId.value, params);
  // memberList.value = res.records
  memberList.value = [
    {
      id: 12345,
      name: 'IEG互动娱乐事业群',
      type: 'DEPARTMENT',
    }, {
      id: 2,
      name: '余姣姣',
      type: 'USER',
    }, {
      id: 3,
      name: '王五',
      type: 'USER',
    },
    {
      id: 4,
      name: 'SRE平台研发中心',
      type: 'DEPARTMENT',
    }, {
      id: 5,
      name: '张三',
      type: 'USER',
    }, {
      id: 6,
      name: '李四',
      type: 'USER',
    },
  ];
}
/**
 * aside页码切换
 */
async function handleAsidePageChange(current) {
  if (memberPagination.value.current !== current) {
    memberPagination.value.current = current;
    getProjectMembers();
  }
}
/**
 * 获取表格选择的数据
 */
function getSelectList(val) {
  selectList.value = val;
  unableMove.value = val.filter(item => item.removeMemberButtonControl);
}
/**
 * 人员组织侧边栏点击事件
 */
function handleAsideClick(item) {
  asideItem.value = item;
}
/**
 * 续期按钮点击
 * @param row 行数据
 */
function handleRenewal(row) {
  isShowRenewal.value = true;
}
/**
 * 续期弹窗提交事件
 */
function handleRenewalConfirm() {
  console.log(expiredAt.value, '授权期限');
  isShowRenewal.value = false;
};
/**
 * 移交按钮点击
 * @param row 行数据
 */
function handleHandover(row) {
  isShowHandover.value = true;
}
/**
 * 移出按钮点击
 * @param row 行数据
 */
function handleRemove(row) {
  isShowRemove.value = true;
}
/**
 * 移出弹窗提交事件
 */
function handleRemoveConfirm() {
  isShowRemove.value = false;
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
  if (!selectList.value.length) {
    Message('请先选择用户组');
    return;
  }
  sliderTitle.value = '批量续期';
  batchFlag.value = 'renewal';
  isShowSlider.value = true;
}
/**
 * 批量移交
 */
function batchHandover() {
  if (!selectList.value.length) {
    Message('请先选择用户组');
    return;
  }
  sliderTitle.value = '批量移交';
  batchFlag.value = 'handover';
  isShowSlider.value = true;
}
/**
 * 批量移出
 */
function batchRemove() {
  if (!selectList.value.length) {
    Message('请先选择用户组');
    return;
  }
  sliderTitle.value = '批量移出';
  batchFlag.value = 'remove';
  isShowSlider.value = true;
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
      member: asideItem.value.name,
      groupId: asideItem.value.id,
      expiredAt: expiredAt.value,
    }];
    const res = await http.batchRenewal(projectId.value, params);
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
  } else if (batchFlag === 'remove') {
    const params = [{
      groupId: asideItem.value.id,
      member: asideItem.value.name,
    }];
    const res = await http.batchRemove(projectId.value, params);
  }

  setTimeout(() => {
    isShowSlider.value = false;
    handOverForm.value.name = '';
    formRef.value?.clearValidate();
  }, 1000);
}
/**
 * 人员列表数据获取
 */
async function handleShowPerson(value) {
  const res = await http.deptUsers(value.id);
  personList.value = res.map(item => ({ person: item }));
}
/**
 * 移出项目
 */
const flag = ref(true);
async function handleAsideRemoveConfirm(value) {
  const res = await http.removeMemberFromProject(value.id, {
    type: value.type,
    member: value.name,
  });
  // 这里根据返回判断移出成功和失败的情况
  if (flag.value) {
    manageAsideRef.value?.handOverfail(true);
    handOverTable.value = [
      {
        id: 1,
        code: 'bkdevops-plugins-test/fayenodejstesa',
        reason: '指定用户未操作过 OAuth',
        percent: '',
      },
      {
        id: 2,
        code: 'bkdevops-plugins-test/fayenodejstesa',
        reason: '指定用户没有此代码库权限',
        percent: '',
      },
    ];
    flag.value = false;
  } else {
    console.log(handOverTable.value, '移交失败表格数据');
    Message({
      theme: 'success',
      message: `${value.name} 已成功移出本项目。`,
    });
  }
}
</script>

<style lang="scss" scoped>
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
    }
  }
}

.renewal-dialog {

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

  .is-required {
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

.remove-dialog {
  .dialog-header {
    font-family: MicrosoftYaHei;
    font-size: 20px;
    color: #313238;
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
  display: flex;
  font-family: MicrosoftYaHei;
  font-size: 12px;
  color: #313238;
  line-height: 20px;

  span {
    color: #63656E;
  }
}

.no-permission {
  width: calc(100% - 24px);
  height: calc(100% - 42px);
  background-color: #fff;
  box-shadow: 0 2px 4px 0 #1919290d;

  ::v-deep .bk-exception-part .bk-exception-img {
    width: 220px;
    margin-top: 120px;

  }
  .empty-text {
    color: #979ba5;
    font-size: 12px;
    line-height: 20px;
  }
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
