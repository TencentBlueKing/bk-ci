<template>
  <bk-loading class="aside" :loading="manageAsideStore.isLoading" >
    <div class="aside-header">
      {{t("组织/用户")}}
      <span class="refresh" @click="refresh">
        <i class="manage-icon manage-icon-refresh"></i>
        {{t("刷新")}}
      </span>
    </div>
    <div class="group-wrapper">
      <div
        :class="{'group-active': activeTab == item.id }"
        class="group-item"
        v-for="item in memberList"
        :key="item.id"
        @click="handleClick(item)"
      >
        <i 
          :class="{
            'group-icon': true,
            'manage-icon manage-icon-organization': item.type === 'department',
            'manage-icon manage-icon-user-shape': item.type === 'user',
            'active': activeTab === item.id
          }"
        />
        <p class="item" v-if="item.type === 'user'">
          <bk-overflow-title type="tips">
            {{ item.id }} ({{ item.name }})
          </bk-overflow-title>
        </p>
        <p class="item" v-else  v-bk-tooltips="{ content: item.name, placement: 'top', disabled: !truncateMiddleText(item.name).includes(' ... ') }">
          {{truncateMiddleText(item.name)}}
        </p>
        <bk-popover
          :arrow="false"
          placement="bottom"
          trigger="click"
          theme="light dot-menu"
        >
          <i @click.stop class="more-icon manage-icon manage-icon-more-fill"></i>
          <template #content>
            <div class="menu-content">
              <!-- <bk-button
                v-if="item.type === 'department'"
                class="btn"
                text
                @click="handleShowPerson(item)"
              >
                {{t("人员列表")}}
              </bk-button> -->
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
  >
    <template #header>
      {{ t("移出项目") }}
      <span class="dialog-header"> {{t("移出用户")}}： {{ removeUser.id }}({{ removeUser.name }}) </span>
    </template>
    <template #default>
      <div class="dialog">
        <p class="text-tag">
          <i class="manage-icon manage-icon-info-line"></i>
          <span>
            {{t("将用户移出项目前，需要指定移交人，平台将自动完成所有权限/授权的移交")}}。
          </span>
        </p>
        <bk-form
          ref="formRef"
          :rules="rules"
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
              @blur="handOverInput"
              @enter="handOverInput"
            />
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
            <p style="display: flex; line-height: 14px;">
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
            <p class="hand-over-table-item">{{item.name}}({{ item.resourceType }})</p>
            <p class="blue-text" @click="goAauthorization(item.resourceType)">
              <i class="manage-icon manage-icon-jump"></i>
              <span>{{t("前往处理")}}</span>
            </p>
          </div>
        </div>
      </div>
    </template>
    <template #footer>
      <bk-button theme="primary" @click="handConfirm('user')" :loading="loading" :disabled="!isAuthorizedSuccess"> {{t("移交并移出")}} </bk-button>
      <bk-button class="btn-margin" @click="handOverClose"> {{t("关闭")}} </bk-button>
    </template>
  </bk-dialog>
  <bk-dialog
    :width="480"
    :theme="'primary'"
    :dialog-type="'confirm'"
    :confirm-text="t('关闭')"
    :is-show="isShowPersonDialog"
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
      <p class="dialog-header-text"> {{t("确认将组织移出本项目吗")}}？ </p>
    </template>
    <template #default>
        <p class="remove-text">
          <span>{{t("待移出组织")}}：</span> {{ removeUser.name }}
        </p>
    </template>
    <template #footer>
      <bk-button theme="danger" @click="handConfirm('department')" :loading="loading"> {{t("确认移出")}} </bk-button>
      <bk-button class="btn-margin" @click="handOverClose"> {{t("关闭")}} </bk-button>
    </template>
  </bk-dialog>
</template>

<script setup name="ManageAside">
import { useI18n } from 'vue-i18n';
import { useRoute } from 'vue-router';
import { ref, defineProps, defineEmits, computed, watch, defineExpose } from 'vue';
import useManageAside from "@/store/manageAside";
import { storeToRefs } from 'pinia';
import { Success, Spinner } from 'bkui-vue/lib/icon';
import http from '@/http/api';

const { t } = useI18n();
const route = useRoute();
const manageAsideStore = useManageAside();
const current = ref(1);
const isShowHandOverDialog = ref(false);
const formRef = ref(null);
const loading = ref(false);
const isHandOverfail = ref(false);
const isShowPersonDialog = ref(false);
const isShowRemoveDialog = ref(false);
const isAuthorizedSuccess = ref(false);
const handOverForm = ref({
  name: ''
});
const rules = {
  name: [
    { required: true, message: t('请输入移交人'), trigger: 'blur' },
  ],
};
const {
  memberPagination,
} = storeToRefs(manageAsideStore);

const projectId = computed(() => route.params?.projectCode);
const removeUser = ref(null);
const isChecking = ref(false);
const overTable = ref([]);

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
const emit = defineEmits(['handleClick', 'pageChange', 'getPersonList', 'removeConfirm', 'refresh']);

watch(()=> handOverForm.value.name,() => {
  handOverInputClear();
})

defineExpose({
  handOverfail,
  handOverClose,
});

function truncateMiddleText(text) {
  if (text.length <= 15) {
    return text;
  }

  const separator = ' ... ';
  const charsToShow = 15 - separator.length;
  const frontChars = Math.ceil(charsToShow / 2);
  const backChars = Math.floor(charsToShow / 2);

  return text.substr(0, frontChars) + separator + text.substr(text.length - backChars);
}
function handleClick(item) {
  // activeTab.value = item.id;
  emit('handleClick', item);
}
function pageChange(current) {
  emit('pageChange', current, projectId.value);
}
function handleRemoval(item) {
  if(item.type === "department") {
    isShowRemoveDialog.value = true;
  } else {
    isShowHandOverDialog.value = true;
  }
  removeUser.value = item;
}
/**
 *  移出项目弹窗关闭
 */
function handOverClose() {
  if(formRef.value) {
    handOverForm.value.name = '';
    formRef.value.clearValidate()
  }
  isShowHandOverDialog.value = false;
  isShowRemoveDialog.value = false
  isHandOverfail.value = false;
}
/**
 *  移出项目弹窗提交
 */
function handConfirm(flag){
  loading.value = true;
  if(flag === 'user'){
    const isValidate = formRef.value?.validate();
    if(!isValidate) return;
    emit('removeConfirm', removeUser.value, handOverForm.value);
  } else {
    emit('removeConfirm');
  }
  loading.value = false;
  handOverClose();
}

function handOverfail(flag) {
  isHandOverfail.value = flag;
}
function handOverInputClear(){
  isAuthorizedSuccess.value = false;
  isHandOverfail.value = false;
}
async function handOverInput(){
  const isValidate = formRef.value?.validate();
  if(!handOverForm.value.name || !isValidate) {
    handOverInputClear();
    return;
  };
  isChecking.value = true;
  isAuthorizedSuccess.value = false;
  const params = {
    projectCode: projectId.value,
    handoverFrom: removeUser.value.id,
    handoverTo: handOverForm.value.name,
    preCheck: true
  }

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
    console.log(error);
  }
}
function refresh(){
  emit('refresh');
}
/**
 * 移出失败刷新数据
 */
function refreshHandOverfail() {
  handOverInput();
}
function goAauthorization(resourceType) {
  window.open(`${location.origin}/console/manage/${projectId.value}/permission?resourceType=${resourceType}`, '_blank')
}
function handleShowPerson(item) {
  isShowPersonDialog.value = true;
  removeUser.value = item;
  emit('getPersonList',item, projectId.value)
}
/**
  * 校验粘贴数据中合法的值
  * usernames [Array] - 格式化后的用户名数组，可通过paste-formatter配置自定义格式化方法
  */
function pasteValidator (usernames){
  return usernames.filter(username => username.startWith('a'))
}
function onRemoveselected(value){
  console.log(value,'全部清空');
}
</script>

<style lang="scss" scoped>
.aside {
  height: calc(100% - 60px);
  overflow-y: scroll;
  overflow: hidden;
}
.aside-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding: 0 18px;
  height: 40px;
  font-family: MicrosoftYaHei-Bold;
  font-weight: 700;
  font-size: 14px;
  color: #63656E;
  letter-spacing: 0;
  line-height: 22px;

  .refresh{
    display: flex;
    align-items: center;
    font-size: 12px;
    font-weight: 400;
    color: #3A84FF;
    cursor: pointer;
  }
}
.group-wrapper {
  overflow: hidden;
  overflow-y: scroll;
  height: 100%;
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
    width: 150px;
    height: 20px;
    font-family: MicrosoftYaHei;
    font-size: 12px;
    color: #63656E;
    letter-spacing: 0;
    line-height: 20px;
  }

  .group-icon {
    width: 15px;
    margin-right: 8px;
    color: #9ea0a4;
    &.active {
      color: #0b76ff;
    }
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
    padding: 1px;
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
  .bk-form-item{
    margin-bottom: 6px !important;
  }
  .text-tag {
    width: 100%;
    line-height: 30px;
    padding-left: 10px;
    margin-bottom: 16px;
    background: #F0F8FF;
    border: 1px solid #C5DAFF;
    border-radius: 2px;

    .manage-icon-info-line{
      font-size: 14px;
      color: #3A84FF;
      margin-right: 10px;
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