<template>
  <div>
    <div class="group-item">
      <img src="../../../svg/organization.svg?inline" class="group-icon">
      <p>{{ memberList[0].bgName }}</p>
      <bk-popover
        transfer
        offset="15"
        :distance="0"
        :arrow="false"
        trigger="click"
        placement="bottom"
        theme="light dot-menu"
        :popover-delay="[100, 0]"
      >
        <i @click.stop class="more-icon manage-icon manage-icon-more-fill"></i>
        <template #content>
          <div class="menu-content">
            <bk-button
              class="btn"
              text
              @click="handleShowPerson(memberList[0])"
            >
              人员列表
            </bk-button>
            <bk-button
              class="btn"
              text
              @click="handleShowRemove(memberList[0])">
              移出项目
            </bk-button>
          </div>
        </template>
      </bk-popover>
    </div>
    <div
      :class="{'group-active': activeTab == item.expiredId }"
      class="group-item item-hover"
      v-for="item in memberList[0].subjectScopes"
      :key="item.expiredId"
      @click="handleClick(item.expiredId)"
    >
      <img v-if="activeTab != item.expiredId" src="../../../svg/user.svg?inline" class="group-icon">
      <img v-else src="../../../svg/user-active.svg?inline" class="group-icon" />
      <p>{{ item.full_name }}({{ item.name }})</p>
      <bk-popover
        transfer
        offset="15"
        :distance="0"
        :arrow="false"
        trigger="click"
        placement="bottom"
        theme="light dot-menu"
        :popover-delay="[100, 0]"
      >
        <i @click.stop class="more-icon manage-icon manage-icon-more-fill"></i>
        <template #content>
          <div class="menu-content">
            <bk-button
              class="btn"
              text
              @click="handleRemoval(item)">
              {{ t('移出项目') }}
            </bk-button>
          </div>
        </template>
      </bk-popover>
    </div>
  </div>
  <bk-dialog
    :is-show="isShowhandOverDialog"
    :theme="'primary'"
    :width="640"
    :confirm-text="t('移交')"
    :is-loading="handOverDialogLoding"
    @closed="handOverClose"
    @confirm="handOverConfirm"
  >
    <template #header>
      {{ t('移除项目') }}
      <span class="dialog-header"> 移出用户： xxxxxx </span>
    </template>
    <template #default>
      <div class="dialog">
        <p class="text-tag">
          将用户移出项目前，需要指定移交人，平台将自动完成所有权限/授权的移交。
        </p>
        <bk-form
          ref="formRef"
          :model="handOverForm"
        >
          <bk-form-item
            required
            label="移交人"
            property="name"
            labelWidth=""
          >
            <bk-input
              v-model="handOverForm.name"
              placeholder="请输入"
              clearable
            />
          </bk-form-item>
        </bk-form>

        <div v-if="isHandOverfail" class="hand-over-fail">
          <p>
            <img src="@/css/svg/close.svg" class="close-icon">
            以下授权移交失败，请<span>重新指定其他授权人</span>，否则无法将用户移出项目
          </p>
          <!-- 这里需要循环拿数据 -->
          <div class="hand-over-table-group">
            <p class="hand-over-table-item">代码库授权</p>
            <bk-table
              :data="handOverTable"
              :border="['outer', 'row']"
              show-overflow-tooltip
            >
              <bk-table-column label="代码库" prop="code" />
              <bk-table-column label="失败原因" prop="reason" />
              <bk-table-column label="授权人" prop="percent">
                <template #default="{ row }">
                  <bk-input v-model="row.percent"></bk-input>
                </template>
              </bk-table-column>
            </bk-table>
          </div>
        </div>
      </div>
    </template>
  </bk-dialog>
  <bk-dialog
    :width="480"
    :theme="'primary'"
    :dialog-type="'confirm'"
    confirm-text="关闭"
    :is-show="isShowPersonDialog"
    :is-loading="personDialogLoading"
    @closed="() => isShowPersonDialog = false"
    @confirm="() => isShowPersonDialog = false"
  >
    <template #header>
      人员列表
      <span class="dialog-header"> 蓝鲸运营组 </span>
    </template>
    <template #default>
        <bk-table
          max-height="320"
          :data="personList"
          show-overflow-tooltip
          class="person-table"
        >
          <bk-table-column label="用户" prop="person" />
        </bk-table>
    </template>
  </bk-dialog>
  <bk-dialog
    :width="450"
    theme="danger"
    cancel-text="关闭"
    confirm-text="确定移出"
    header-align="center"
    footer-align="center"
    :is-show="isShowRemoveDialog"
    @closed="() => isShowRemoveDialog = false"
    @confirm="handleRemoveConfirm"
  >
    <template #header>
      <h2 class="dialog-header-text"> 确认将组织移出本项目吗？ </h2>
    </template>
    <template #default>
        <p class="remove-text">
          <span>待移出组织：</span> IEG 互动娱乐事业群/技术运营部/蓝鲸产品中心/蓝鲸运营组
        </p>
    </template>
  </bk-dialog>
</template>

<script setup>
import { useI18n } from 'vue-i18n';
import { ref, defineProps, defineEmits } from 'vue';
import { Message } from 'bkui-vue';

const { t } = useI18n();
const activeTab = ref('1743602525');
const isShowhandOverDialog = ref(false);
const formRef = ref(null);
const handOverDialogLoding = ref(false);
const isHandOverfail = ref(false);
const isShowPersonDialog = ref(false);
const personDialogLoading = ref(false);
const isShowRemoveDialog = ref(false);
const handOverForm = ref({
  name: ''
})
const handOverTable = ref([
  {
    id: 1,
    code: "bkdevops-plugins-test/fayenodejstesa",
    reason: "指定用户未操作过 OAuth",
    percent: "",
  },
  {
    id: 2,
    code: "bkdevops-plugins-test/fayenodejstesa",
    reason: "指定用户没有此代码库权限",
    percent: "",
  }
]);
const personList = ref([])
defineProps({
  memberList: {
    type: Array,
    required: true,
  }
});
const emit = defineEmits(['handleChange']);

function handleClick(id) {
  activeTab.value = id;
  emit('handleClick', id);
}

function handleRemoval(item) {
  isShowhandOverDialog.value = true;
}
/**
 *  人员移出项目弹窗关闭
 */
 function handOverClose() {
  isShowhandOverDialog.value = false;
  isHandOverfail.value = false;
}
/**
 *  人员移出项目弹窗提交
 */
function handOverConfirm() {
  handOverDialogLoding.value = true;
  formRef.value?.validate().then( isValid => {
    if (isValid) {
      // 接口判断 是否重置失败
      setTimeout(()=>{
        handOverDialogLoding.value = false;
        if(Math.random() > 0.5){
          isHandOverfail.value = true;
          console.log(handOverTable.value,'移出授权人表格数据');
        }else{
          Message({
            theme: 'success',
            message: 'daisyhong (小芸) 已成功移出本项目。',
          });
          handOverDialogLoding.value = false;
        }
      },1000)
    }
  }).catch(()=>{
    
  }).finally(()=>{
    handOverDialogLoding.value = false;
    isHandOverfail.value = false;
  })
}
/**
 * 人员列表
 */
function handleShowPerson(item) {
  isShowPersonDialog.value = true;
  personDialogLoading.value = true;
  setTimeout(()=>{
    personList.value=[
      {person:'aaaa'},
      {person:'bbb'},
      {person:'ccc'},
      {person:'ddd'},
      {person:'aaaa'},
    ]
    personDialogLoading.value = false;
  },1000)
}
/**
 * 组织移出项目
 */
function handleShowRemove(item) {
  isShowRemoveDialog.value = true;
}
/**
 * 组织移除项目弹窗确定
 */
function handleRemoveConfirm(params) {
  // 调接口
  isShowRemoveDialog.value = false;
  Message({
    theme: 'success',
    message: 'IEG 互动娱乐事业群/技术运营部/蓝鲸产品中心/蓝鲸运营组 已成功移出本项目。',
  });
}
</script>

<style lang="scss" scoped>
.group-item {
  display: flex;
  width: 100%;
  padding: 0 18px;
  height: 40px;
  align-items: center;
  font-size: 14px;
  color: #63656E;
  cursor: pointer;

  p{
    flex: 1;
    font-family: MicrosoftYaHei;
    font-size: 12px;
    color: #63656E;
    letter-spacing: 0;
    line-height: 20px;
  }

  .group-icon {
    width: 15px;
    line-height: 20px;
    margin-right: 8px;
    filter: invert(70%) sepia(8%) saturate(136%) hue-rotate(187deg) brightness(91%) contrast(86%);
  }

  .more-icon {
    border-radius: 50%;
    color: #63656e;
    padding: 1px;
    display: none;
  }

  .more-icon:hover {
    background-color: #DCDEE5;
    color: #3A84FF !important;
  }

  &:hover .more-icon{
    display: block;
  }

  
  &:hover {
    background-color: #eaebf0;
  }
}

.group-active {
  background-color: #E1ECFF !important;
  border-right: 1px solid #3A84FF;

  p{
    color: #3A84FF;
  }

  .group-icon {
    filter: invert(100%) sepia(0%) saturate(90%) hue-rotate(180deg) brightness(90%) contrast(180%);
  }
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

.dialog {
  .text-tag {
    width: 100%;
    height: 32px;
    line-height: 32px;
    padding-left: 10px;
    margin-bottom: 16px;
    background: #F0F8FF;
    border: 1px solid #C5DAFF;
    border-radius: 2px;
    font-family: MicrosoftYaHei;
    font-size: 12px;
    color: #63656E;
  }

  .hand-over-fail {
    border-top: 1px solid #DCDEE5;
    margin-bottom: 25px;

    p {
      margin-top: 8px;
      color: #63656e;
      font-size: 12px;

      span {
        font-weight: 700;
      }
    }

    .close-icon {
      width: 14px;
      height: 14px;
      vertical-align: middle;
    }

    .hand-over-table-item {
      width: 100%;
      height: 32px;
      line-height: 32px;
      margin-top: 12px;
      padding-left: 16px;
      background: #EAEBF0;
      border-radius: 2px;
      font-family: MicrosoftYaHei;
      font-size: 14px;
      color: #313238;
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
  font-weight: 600;
}

.remove-text {
  font-family: MicrosoftYaHei;
  font-size: 12px;
  color: #313238;
  line-height: 20px;
  span {
    color: #63656E;
  }
}
</style>
