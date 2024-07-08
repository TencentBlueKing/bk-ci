<template>
  <div class="aside">
    <div
      :class="{'group-active': activeTab == item.id }"
      class="group-item"
      v-for="item in memberList"
      :key="item.id"
      @click="handleClick(item)"
    >
      <span v-if="item.type === 'DEPARTMENT'">
        <img :src="activeTab === item.id ? organizationActiveIcon : organizationIcon" class="group-icon">
      </span>
      <span v-else>
        <img :src="activeTab === item.id ? userActiveIcon : userIcon" class="group-icon">
      </span>
      <p>{{ item.name }}</p>
      <bk-popover
        :arrow="false"
        placement="bottom"
        theme="light dot-menu"
      >
        <i @click.stop class="more-icon manage-icon manage-icon-more-fill"></i>
        <template #content>
          <div class="menu-content">
            <bk-button
              v-if="item.type === 'DEPARTMENT'"
              class="btn"
              text
              @click="handleShowPerson(item)"
            >
              {{t("人员列表")}}
            </bk-button>
            <bk-button
              class="btn"
              text
              @click="handleRemoval(item)">
              {{t("移出项目")}}
            </bk-button>
          </div>
        </template>
      </bk-popover>
    </div>

    <bk-pagination
      class="pagination"
      v-model="current"
      align="center"
      :count="pageCount"
      small
      :show-limit="false"
      :show-total-count="false"
      @change="pageChange"
    />
  </div>
  <bk-dialog
    :is-show="isShowhandOverDialog"
    :width="640"
    :is-loading="handOverDialogLoding"
    @closed="handOverClose"
  >
    <template #header>
      {{ t("移出项目") }}
      <span class="dialog-header"> {{t("移出用户")}}： {{ removeUser.name }} </span>
    </template>
    <template #default>
      <div class="dialog">
        <p class="text-tag">
          {{t("将用户移出项目前，需要指定移交人，平台将自动完成所有权限/授权的移交")}}。
        </p>
        <bk-form
          ref="formRef"
          :model="handOverForm"
        >
          <bk-form-item
            required
            :label="t('移交人')"
            property="name"
            labelWidth=""
          >
            <bk-input
              v-model="handOverForm.name"
              :placeholder="t('请输入')"
              clearable
              @clear="handOverInputClear"
              @blur="handOverInputBlur"
            />
            <p v-if="verifying" class="verifying">
              {{ authorizationStatus }}
            </p>
          </bk-form-item>
        </bk-form>

        <div v-if="!isAuthorizedSuccess && isHandOverfail" class="hand-over-fail">
          <p class="err-text">
            <p style="display: flex; line-height: 14px;">
              <img src="@/css/svg/close.svg" class="close-icon">
              <i18n-t keypath="检测到以下授权将无法移交给X，请先前往「授权管理」单独处理" tag="div" >
                <span> {{ removeUser.name }} </span>
              </i18n-t>
            </p>
            <p>
              <!-- 一个图标占位 -->
              <span class="blue-text" @click="refreshHandOverfail">{{t("刷新")}}</span>
            </p>
          </p>
          <!-- 这里需要循环拿数据overTable替换成后端返回的东西 -->
          <div class="hand-over-table-group">
            <p class="hand-over-table-item">{{t("代码库授权")}}</p>
            <p class="blue-text">
              <!-- 一个图标占位 -->
              <span @click="goAauthorization">{{t("前往处理")}}</span>
            </p>
          </div>
        </div>
      </div>
    </template>
    <template #footer>
      <bk-button theme="primary" @click="handOverConfirm" :disabled="!isAuthorizedSuccess"> {{t("移交并移出")}} </bk-button>
      <bk-button @click="handOverClose"> {{t("关闭")}} </bk-button>
    </template>
  </bk-dialog>
  <bk-dialog
    :width="480"
    :theme="'primary'"
    :dialog-type="'confirm'"
    :confirm-text="t('关闭')"
    :is-show="isShowPersonDialog"
    :is-loading="personDialogLoading"
    @closed="() => isShowPersonDialog = false"
    @confirm="() => isShowPersonDialog = false"
  >
    <template #header>
      {{t("人员列表")}}
      <span class="dialog-header"> {{ removeUser.name }} </span>
    </template>
    <template #default>
        <bk-table
          max-height="320"
          :data="personList"
          show-overflow-tooltip
          class="person-table"
        >
          <bk-table-column :label="t('用户')" prop="person" />
        </bk-table>
    </template>
  </bk-dialog>
  <bk-dialog
    :width="450"
    confirmButtonTheme="danger"
    :cancel-text="t('关闭')"
    :confirm-text="t('确认移出')"
    header-align="center"
    footer-align="center"
    :is-show="isShowRemoveDialog"
    @closed="() => isShowRemoveDialog = false"
    @confirm="handleRemoveConfirm"
  >
    <template #header>
      <h2 class="dialog-header-text"> {{t("确认将组织移出本项目吗")}}？ </h2>
    </template>
    <template #default>
        <p class="remove-text">
          <span>{{t("待移出组织")}}：</span> {{ removeUser.name }}
        </p>
    </template>
  </bk-dialog>
</template>

<script setup name="ManageAside">
import { useI18n } from 'vue-i18n';
import { ref, defineProps, defineEmits, computed, watch, defineExpose } from 'vue';

const { t } = useI18n();
const current = ref(1);
const pageCount = ref();
const activeTab = ref();
const isShowhandOverDialog = ref(false);
const formRef = ref(null);
const handOverDialogLoding = ref(false);
const isHandOverfail = ref(false);
const isShowPersonDialog = ref(false);
const personDialogLoading = ref(false);
const isShowRemoveDialog = ref(false);
const isAuthorizedSuccess = ref(false);
const verifying = ref(false);
const handOverForm = ref({
  name: ''
});

const organizationIcon = computed(() => require('../../../svg/organization.svg?inline'));
const organizationActiveIcon = computed(() => require('../../../svg/organization-active.svg?inline'));
const userIcon = computed(() => require('../../../svg/user.svg?inline'));
const userActiveIcon = computed(() => require('../../../svg/user-active.svg?inline'));
const removeUser = ref(null);
const authorizationStatus = computed(() => {
  if (verifying.value) {
    if (isAuthorizedSuccess.value) {
      return t("授权校验通过");
    } else if (!isHandOverfail.value){
      return t("正在校验授权");
    }
  }
  return '';
});

const props = defineProps({
  memberList: {
    type: Array,
    default: () => [],
  },
  personList: {
    type: Array,
    default: () => [],
  },
  overTable: {
    type: Array,
    default: () => [],
  },
  projectId: {
    type: String,
  }
});
const emit = defineEmits(['handleClick', 'pageChange', 'getPersonList', 'removeConfirm']);

watch(() => props.memberList, (newData) => {
  activeTab.value = newData[0].id;
  pageCount.value = newData.length;
  emit('handleClick', newData[0]);
});

watch(()=> handOverForm.value.name,(newName,oldName) => {
  handOverInputClear();
})

defineExpose({
  handOverfail,
  handOverClose,
});

function handleClick(item) {
  activeTab.value = item.id;
  emit('handleClick', item);
}
function pageChange(current) {
  emit('pageChange', current, projectId.value);
}
/**
 * 移出项目
 * @param item 
 */
function handleRemoval(item) {
  if(item.type === "DEPARTMENT") {
    isShowRemoveDialog.value = true;
  } else {
    isShowhandOverDialog.value = true;
  }
  removeUser.value = item;
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
      emit('removeConfirm',removeUser.value)
      handOverDialogLoding.value = false;
    }
  }).catch(()=>{
    
  }).finally(()=>{
    handOverDialogLoding.value = false;
    isHandOverfail.value = false;
  })
}
function handOverfail(flag) {
  isHandOverfail.value = flag;
}
/**
 * 移交人输入清空事件
 */
function handOverInputClear(){
  verifying.value = false;
  isAuthorizedSuccess.value = false;
  isHandOverfail.value = false;
}
/**
 * 移交人输入失焦事件
 */
function handOverInputBlur(){
  console.log('失焦时输入框内容',handOverForm.value.name);
  if(!handOverForm.value.name) return;
  verifying.value = true;
  setTimeout(()=>{
    if(Math.random()> 0.5){
      isAuthorizedSuccess.value = true;
    } else {
      isHandOverfail.value = true;
    }
  },2000)
}
/**
 * 刷新
 */
function refreshHandOverfail() {

}
/**
 * 前往授权管理
 */
function goAauthorization() {
  window.open(`${location.origin}/console/manage/xxzza/permission`, '_blank')
}
/**
 * 人员列表
 */
function handleShowPerson(item) {
  isShowPersonDialog.value = true;
  removeUser.value = item;
  personDialogLoading.value = true;
  emit('getPersonList',item)
  personDialogLoading.value = false;
}
/**
 * 组织移除项目弹窗确定
 */
function handleRemoveConfirm() {
  emit('removeConfirm',removeUser.value)
  isShowRemoveDialog.value = false;
}
</script>

<style lang="scss" scoped>
.aside {
  height: calc(100% - 60px);
  overflow-y: scroll;
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
  border-right: 2px solid #3A84FF;

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

  .verifying{
    font-size: 12px;
    color: #63656E;
  }

  .hand-over-fail {
    border-top: 1px solid #DCDEE5;
    margin-bottom: 25px;

    .err-text {
      display: flex;
      justify-content: space-between;
      margin-top: 8px;
      color: #63656e;
      font-size: 12px;

      .close-icon {
        width: 14px;
        height: 14px;
        vertical-align: middle;
      }

      span {
        font-weight: 700;
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
      font-size: 12px;
      color: #3A84FF;
      cursor: pointer;
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

.pagination{
  position: absolute;
  bottom: 16px;
  padding-top: 16px;
  width: 100%;
  border-top: 1px solid #DCDEE5;
}
</style>