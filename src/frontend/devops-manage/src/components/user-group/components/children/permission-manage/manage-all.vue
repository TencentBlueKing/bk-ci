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
        <div v-if="1">
          <GroupTable
            :is-show-operation="true"
            @renewal="handleRenewal"
            @handover="handleHandover"
            @remove="handleRemove"
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
          已选择<span class="desc-primary"> 3 </span>个用户组
          <span>；其中<span class="desc-warn"> 1 </span>个用户组<span class="desc-warn">无法移出</span>，本次操作将忽略</span>
        </p>
        <div>
          <GroupTable :is-show-operation="false" :pagination="pagination" />
        </div>
      </div>
      <div class="slider-footer">
        <div class="footer-main">
          <div v-if="sliderTitle === '批量续期'">
            <div class="main-line">
              <p class="main-label">续期对象</p>
              <span class="main-text">用户： fayewang (王玉菊)</span>
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
                <sapn class="remove-num">2</sapn>
                个用户组中移出
                <span class="remove-person">fayewang (王玉菊)</span>
                吗？
              </p>
            </div>
          </div>
        </div>
        <div class="footer-btn">
          <bk-button :theme="theme" @click="batchConfirm">{{ t(batchBtnText) }}</bk-button>
          <bk-button @click="batchCancel">取消</bk-button>
        </div>
      </div>
    </template>
  </bk-sideslider>
</template>

<script setup name="manageAll">
import { useI18n } from 'vue-i18n';
import { ref, watch } from 'vue';
import SelectGroup from './select-group.vue';
import ManageAside from './manage-aside.vue';
import GroupTable from './group-table.vue';
import TimeLimit from './time-limit.vue';

const { t } = useI18n();

const expiredAt = ref();
const isLoading = ref(false);
const isShowRenewal = ref(false);
const isShowHandover = ref(false);
const isShowRemove = ref(false);
const isShowSlider = ref(false);
const sliderTitle = ref('');
const theme = ref('primary');
const batchBtnText = ref('确定续期');
const pagination = ref({ count: 0, limit: 10, current: 1 });
const handOverForm = ref({
  name: ''
})
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

watch([serveValue, resourceValue, operateValue, searchValue], () => {
  // 侦听值的变化，调用接口获取筛选数据
  console.log(serveValue.value, resourceValue.value, operateValue.value, searchValue.value, '搜索的数据');
});

/**
 * 搜索事件
 * @param value 搜索值
 * @param target 查询标志
 */
function handleSearchChange(value, target) {
  stateRefs[target].value = value;
}
/**
 * 人员组织侧边栏点击事件
 */
function handleAsideClick(id) {
  console.log(id, '这里根据id展示表格数据');
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
  sliderTitle.value = '批量续期';
  batchBtnText.value = '确定续期';
  isShowSlider.value = true;
}
/**
 * 批量移交
 */
function batchHandover() {
  sliderTitle.value = '批量移交';
  batchBtnText.value = '确定移交';
  isShowSlider.value = true;
}
/**
 * 批量移出
 */
function batchRemove() {
  sliderTitle.value = '批量移出';
  batchBtnText.value = '确定移出';
  theme.value = 'danger';
  isShowSlider.value = true;
}
/**
 * sideslider 关闭
 */
function batchCancel() {
  isShowSlider.value = false;
}
/**
 * sideslider 确认
 */
function batchConfirm() {
  console.log(expiredAt.value, '授权的时间');
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
