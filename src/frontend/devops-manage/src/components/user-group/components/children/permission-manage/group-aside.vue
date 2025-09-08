<template>
  <article :class="['group-aside', {'group-aside-height': showSelectProject}]">
    <template v-if="showSelectProject">
      <div class="select-project">
        <p class="title">{{ t('选择项目') }}</p>
        <bk-select
          v-model="curProjectCode"
          filterable
          :clearable="false"
          :input-search="false"
          :scroll-loading="scrollLoading"
          :remote-method="handleSearchProject"
          @change="handleSelectProject"
          :popoverOptions="popoverOptions"
        >
          <div
            v-for="project in filterProjectList"
            :key="project.projectCode"
          >
            <bk-option
              :value="project.englishName"
              :disabled="!project.managePermission"
              :label="project.projectName"
            >
              <div
                v-bk-tooltips="{
                  disabled: project.managePermission,
                  content: t('非项目管理员，无操作权限'),
                }"
                class="option-item"
              >
                {{ project.projectName }}
              </div>
            </bk-option>
          </div>
        </bk-select>
      </div>
      <div class="line-split" v-if="!isNotProject" />
    </template>
    <bk-loading v-if="dataLoaded" :loading="fetchGroupLoading" class='saide-content'>
      <scroll-load-list
        class="group-list"
        ref="loadList"
        :list="groupList"
        :has-load-end="hasLoadEnd"
        :project-Code="curProjectCode"
        :page="page"
        :get-data-method="handleGetData"
        :is-not-project="isNotProject"
      >
        <template v-slot:default="{ data: group, index }">
          <bk-input
            ref="renameInput"
            v-show="group.groupId === renameGroupId && isRename"
            v-model="displayGroupName"
            class="rename-input"
            @enter="handleRename"
            @blur="handleRename"
          >
          </bk-input>
          <div
            :class="{ 'group-item': true, 'group-active': activeTab === group.groupId }"
            @click="handleChooseGroup(group)"
          >
            <span class="group-name" :title="group.name">{{ group.name }}</span>
            <div class="num-box" v-for="item in groupCountField" :key="item">
              <i
                :class="['group-icon', 'manage-icon', {
                  'manage-icon-user-shape': item === 'userCount',
                  'manage-icon-user-template': item === 'templateCount',
                  'manage-icon-organization': item === 'departmentCount',
                  'active': activeTab === group.groupId
                }]"
              />
              <div class="group-num">{{ group[item] }}</div>
            </div>
            <bk-popover
              v-if="resourceType === 'project' && !group.defaultGroup"
              class="group-more-option"
              placement="bottom"
              theme="light dot-menu"
              :arrow="false"
              trigger="click"
              :offset="15"
              :distance="0">
              <i @click.stop class="more-icon manage-icon manage-icon-more-fill"></i>
              <template #content>
                <div class="menu-content">
                  <bk-button
                    v-if="!group.defaultGroup"
                    class="btn"
                    text
                    @click="handleShowRename(group)"
                  >
                    {{ t('重命名') }}
                  </bk-button>
                  <bk-button
                    class="btn"
                    :disabled="group.defaultGroup"
                    text
                    @click="handleShowDeleteGroup(group)">
                    {{ t('删除') }}
                  </bk-button>
                </div>
              </template>
            </bk-popover>
          </div>
        </template>
      </scroll-load-list>
      <div class="line-split" />
      <div
        v-if="showCreateGroup && projectCode"
        :class="{ 'group-item': true, 'group-active': activeTab === '' }"
        @click="handleCreateGroup">
        <span class="add-group-btn" :title="t('新建用户组')">
          <i class="manage-icon manage-icon-add-fill add-icon"></i>
          {{ t('新建用户组') }}
        </span>
      </div>
      <div
        v-if="resourceType !== 'project'"
        class="close-btn"
      >
        <bk-button @click="handleCloseManage" :loading="isClosing">{{ t('关闭权限管理') }}</bk-button>
      </div>
    </bk-loading>
    <bk-dialog
      dialogType="show"
      header-align="center"
      theme="danger"
      class="delete-group-dialog"
      :quick-close="false"
      :is-show="deleteObj.isShow"
      :is-loading="deleteObj.isLoading"
      @closed="handleHiddenDeleteGroup"
    >
      <template #header>
        <div class="manage-icon manage-icon-warning-circle-fill title-icon"></div>
        <p class="delete-title">{{ t('确认删除【】用户组？', [deleteObj.group.name]) }}</p>
      </template>
      <div class="delete-tips">
        <p>{{ t('删除用户组【】将执行如下操作：', [deleteObj.group.name]) }}</p>
        <p>
          <i class="manage-icon manage-icon-warning-circle-fill warning-icon"></i>
          {{ t('将用户和组织从组中移除') }}
        </p>
        <p>
          <i class="manage-icon manage-icon-warning-circle-fill warning-icon"></i>
          {{ t('删除组内用户继承该组的权限') }}
        </p>
        <p>
          <i class="manage-icon manage-icon-warning-circle-fill warning-icon"></i>
          {{ t('删除组信息和组权限') }}
        </p>
      </div>
      <div class="confirm-delete">
        <i18n-t keypath="此操作提交后将不能恢复，为避免误删除，请再次确认你的操作：" style="color: #737987;font-size: 14px;" tag="div">
          <span style="color: red;">{{t('不能恢复')}}</span>
        </i18n-t>
        <bk-input
          v-model="keyWords"
          :placeholder="t('请输入待删除的用户组名')"
          class="confirm-input"
        ></bk-input>
      </div>
      <div class="option-btns">
        <bk-button
          class="btn"
          theme="danger"
          :disabled="disableDeleteBtn"
          @click="handleDeleteGroup"
        >
          {{ t('删除') }}
        </bk-button>
        <bk-button
          class="btn"
          @click="handleHiddenDeleteGroup"
        >
          {{ t('取消') }}
        </bk-button>
      </div>
    </bk-dialog>
  </article>
</template>

<script>
import http from '@/http/api';
import { validProjectCode } from '@/utils/util';
import { Message } from 'bkui-vue';
import { useI18n } from 'vue-i18n';
import ajax from '../../../ajax/index';
import ScrollLoadList from '../../widget-components/scroll-load-list';
export default {
  components: {
    ScrollLoadList,
  },
  props: {
    activeIndex: {
      type: Boolean,
      default: 0,
    },
    resourceType: {
      type: String,
      default: '',
    },
    resourceCode: {
      type: String,
      default: '',
    },
    projectCode: {
      type: String,
      default: '',
    },
    showCreateGroup: {
      type: Boolean,
      default: true,
    },
    ajaxPrefix: {
      type: String,
      default: '',
    },
  },
  emits: ['choose-group', 'create-group', 'close-manage'],
  data() {
    const { t } = useI18n();
    return {
      page: 1,
      activeTab: '',
      deleteObj: {
        group: {},
        isShow: false,
        isLoading: false,
      },
      groupList: [],
      hasLoadEnd: false,
      isClosing: false,
      isRename: false,
      displayGroupName: '',
      renameGroupId: 0,
      curGroupIndex: -1,
      keyWords: '',
      t,
      projectList: [],
      searchProjectKey: '',
      curProjectCode: this.projectCode,
      fetchGroupLoading: false,
      popoverOptions: {
        zIndex: 2000
      },
      dataLoaded: false
    };
  },
  computed: {
    disableDeleteBtn() {
      return !(this.keyWords === this.deleteObj.group.name);
    },
    groupCountField () {
      if (this.resourceType === 'pipeline') {
        return ['userCount', 'templateCount', 'departmentCount']
      }
      return ['userCount', 'departmentCount']
    },
    showSelectProject () {
      return location.search.includes('showSelectProject=true')
    },
    filterProjectList () {
      return this.projectList.filter(i => i.projectName.includes(this.searchProjectKey));
    },
    isNotProject () {
      return this.curProjectCode === 'my-project' || !this.curProjectCode
    }
  },
  watch: {
    activeIndex(newVal) {
      this.activeTab = this.groupList[newVal]?.groupId || '';
    },
  },
  async created() {
    window.addEventListener('message', this.handleMessage);
    if (this.showSelectProject) {
      await this.getProjectList()
    } else {
      this.dataLoaded = true
    }
  },

  beforeUnmount() {
    window.removeEventListener('message', this.handleMessage);
  },
  methods: {
    handleGetData(pageSize) {
      if (!validProjectCode(this.curProjectCode)) {
        return Promise.resolve();
      }
      this.fetchGroupLoading = true
      return ajax
        .get(`${this.ajaxPrefix}/auth/api/user/auth/resource/${encodeURIComponent(this.curProjectCode)}/${this.resourceType}/${encodeURIComponent(this.curProjectCode)}/listGroup?page=${encodeURIComponent(this.page)}&pageSize=${encodeURIComponent(pageSize)}`)
        .then(({ data }) => {
          this.hasLoadEnd = !data.hasNext;
          this.groupList = [...this.groupList, ...data.records];
          // 首页需要加载
          if (this.page === 1) {
            const chooseGroup = this.groupList.find(group => +group.groupId === +this.$route.query?.groupId) || this.groupList[0];
            this.handleChooseGroup(chooseGroup);
          }
          this.page += 1
        })
        .finally(() => {
          this.fetchGroupLoading = false
        })
    },
    getProjectList() {
      return ajax
        .get(`${this.ajaxPrefix}/project/api/user/projects/?enabled=true`)
        .then((res) => {
          this.projectList = res.data;
          const project = this.projectList.find(i => i.projectCode === this.projectCode);
          if (project?.managePermission === false || !/rbac/.test(project?.routerTag)) {
            this.curProjectCode = '';
          };
          this.dataLoaded = true
          this.$router.push({
            query: {
              ...this.$route.query,
              projectCode: this.curProjectCode
            }
          })
        });
    },
    refreshList() {
      this.groupList = [];
      this.hasLoadEnd = false;
      this.page = 1;
      return this.handleGetData(100)
    },
    handleShowDeleteGroup(group) {
      this.deleteObj.group = group;
      this.deleteObj.isShow = true;
    },
    handleHiddenDeleteGroup() {
      this.deleteObj.isShow = false;
      this.deleteObj.group = {};
    },
    handleDeleteGroup() {
      this.deleteObj.isLoading = true;
      if (!validProjectCode(this.curProjectCode)) {
        return Promise.resolve();
      }
      return ajax
        .delete(`${this.ajaxPrefix}/auth/api/user/auth/resource/group/${encodeURIComponent(this.curProjectCode)}/${this.resourceType}/${encodeURIComponent(this.deleteObj.group.groupId)}`)
        .then(() => {
          this.refreshList();
          this.syncGroupAndMemberIAM();
          this.syncDeleteGroupPermissions(this.deleteObj.group.groupId);
          this.handleHiddenDeleteGroup();
          Message({
            theme: 'success',
            message: this.t('删除成功')
          });
        })
        .finally(() => {
          this.deleteObj.isLoading = false;
        });
    },
    handleChooseGroup(group) {
      this.$router.replace({
        query: {
          ...this.$route.query,
          groupId: group.groupId,
        }
      })
      this.activeTab = group.groupId;
      this.curGroupIndex = this.groupList.findIndex(item => item.groupId === group.groupId);
      this.$emit('choose-group', group);
    },
    async handleCreateGroup() {
      if (this.isNotProject) return
      this.activeTab = '';
      try {
        const res = await http.getResource({
          projectCode: this.curProjectCode,
          resourceType: this.resourceType,
          resourceCode: this.curProjectCode});
          if(res) {
            const role_id = res.iamGradeManagerId;
            this.$emit('create-group', role_id);
          }
      } catch (error) {
        Message({
          theme: 'error',
          message: error.message
        });
      }
      
    },
    handleCloseManage() {
      this.isClosing = true;
      if (!validProjectCode(this.curProjectCode)) {
        return Promise.resolve();
      }
      return ajax
        .put(`${this.ajaxPrefix}/auth/api/user/auth/resource/${encodeURIComponent(this.curProjectCode)}/${this.resourceType}/${encodeURIComponent(this.curProjectCode)}/disable`)
        .then(() => {
          this.$emit('close-manage');
        })
        .finally(() => {
          this.isClosing = false;
        });
    },
    handleMessage(event) {
      const { data } = event;
      if (data?.type === 'IAM') {
        switch (data.code) {
          case 'create_user_group_submit':
            this
              .refreshList()
              .then(() => {
                const group = this.groupList.find(group => group.groupId === data?.data?.id) || this.groupList[0];
                this.handleChooseGroup(group);
              })
              this.syncGroupAndMemberIAM();
              this.syncGroupPermissions(data.data.id)
            break;
          case 'create_user_group_cancel':
            this.handleChooseGroup(this.groupList[0]);
            break;
          case 'add_user_confirm':
          case 'add_template_confirm':
            this.groupList[this.curGroupIndex].departmentCount += data.data.departments.length
            this.groupList[this.curGroupIndex].userCount += data.data.users.length
            this.groupList[this.curGroupIndex].templateCount += data.data.templates.length
            this.syncGroupIAM(this.groupList[this.curGroupIndex].groupId)
            break;
          case 'remove_user_confirm':
          case 'remove_template_confirm': {
            const departments = data.data.members.filter(i => i.type === 'department')
            const users = data.data.members.filter(i => i.type === 'user')
            const templates = data.data.members.filter(i => i.type === 'template')
            this.groupList[this.curGroupIndex].departmentCount -= departments.length
            this.groupList[this.curGroupIndex].userCount -= users.length
            this.groupList[this.curGroupIndex].templateCount -= templates.length
            this.syncGroupIAM(this.groupList[this.curGroupIndex].groupId)
            break;
          }
          case 'change_group_detail_tab':
            this.$emit('change-group-detail-tab', data.data.tab)
            break;
          case 'submit_add_group_perm':
          case 'submit_delete_group_perm':
          case 'submit_edit_group_perm': {
            const groupId = data.data.id;
            this.syncGroupPermissions(groupId)
            break;
          }
          case 'renewal_user_confirm':
          case 'renewal_template_confirm': {
            const groupId = data.data.id;
            this.syncGroupIAM(groupId)
            break;
          }
        }
      }
    },

    async syncDeleteGroupPermissions (groupId) {
      try {
        await http.syncDeleteGroupPermissions(this.curProjectCode, groupId);
      } catch (error) {
        Message({
          theme: 'error',
          message: error.message
        });
      }
    },
    
    async syncGroupPermissions (groupId) {
      if (!groupId) return
      try {
        await http.syncGroupPermissions(this.curProjectCode, groupId);
      } catch (error) {
        Message({
          theme: 'error',
          message: error.message
        });
      }
    },
    
    async syncGroupIAM(groupId){
      if (!groupId) return
      try {
        await http.syncGroupMember(this.curProjectCode, groupId);
      } catch (error) {
        Message({
          theme: 'error',
          message: error.message
        });
      }
    },
    async syncGroupAndMemberIAM(){
      try {
        await http.syncGroupAndMember(this.curProjectCode);
      } catch (error) {
        Message({
          theme: 'error',
          message: error.message
        });
      }
    },
    handleShowRename (group) {
      this.isRename = true;
      this.renameGroupId = group.groupId;
      this.displayGroupName = group.name;
      setTimeout(() => {
        this.$refs.renameInput.focus();
      });
    },

    handleRename () {
      const group = this.groupList.find(i => i.groupId === this.renameGroupId);
      if (this.displayGroupName === group.name || !group) {
        this.isRename = false;
        this.renameGroupId = 0;
        this.displayGroupName = '';
        return
      }
      if (!validProjectCode(this.curProjectCode)) {
        return Promise.resolve();
      }
      return ajax
        .put(`${this.ajaxPrefix}/auth/api/user/auth/resource/group/${encodeURIComponent(this.curProjectCode)}/${this.resourceType}/${encodeURIComponent(this.renameGroupId)}/rename`, {
          groupName: this.displayGroupName,
        })
        .then(() => {
          group.name = this.displayGroupName;
          this.syncGroupAndMemberIAM();
          Message({
            theme: 'success',
            message: this.t('修改成功')
          });
        })
        .catch((err) => {
          Message({
            theme: 'error',
            message: err.message
          });
        })
        .finally(() => {
          this.isRename = false;
          this.renameGroupId = 0;
          this.displayGroupName = '';
        })
    },
    handleSearchProject (val) {
      this.searchProjectKey = val
    },
    handleSelectProject (val) {
      this.page = 1
      this.groupList = []
      this.curProjectCode = val
      this.$router.push({
        query: {
          ...this.$route.query,
          projectCode: this.curProjectCode
        }
      })
    }
  },
};
</script>

<style lang="scss" scoped>
.group-aside {
  min-width: 240px;
  width: 240px;
  height: 100%;
  background-color: #fff;
  border-right: 1px solid #dde0e6;
  padding-top: 10px;
}
.group-aside-height {
  height: calc(100% - 89px);
}
.select-project {
  padding: 10px 24px 0;
  .title {
    font-size: 14px;
    font-weight: 700;
    margin-bottom: 5px;
  }
}
.saide-content {
  height: 100%;
  overflow-y: auto;
  background-color: #fff;
}
.group-list {
  max-height: calc(100% - 70px);
  min-height: 80px;
  height: auto;
  overflow-y: auto;
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
.group-title {
  display: inline-block;
  line-height: 50px;
  padding-left: 24px;
  width: 100%;
  font-size: 14px;
  margin-bottom: 8px;
  font-weight: bold;
}
.group-item {
  display: flex;
  align-items: center;
  width: 100%;
  height: 40px;
  line-height: 40px;
  font-size: 14px;
  padding-left: 24px;
  color: #63656E;
  cursor: pointer;
  &:hover {
    background-color: #eaebf0;
   
  }
}

.group-active {
  color: #3A84FF !important;
  background-color: #E1ECFF !important;
  .user-num, .group-num {
    background-color: #A3C5FD;
    color: #fff;
  }
  .group-icon {
    color: #A3C5FD;
  }
}
.num-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding-right: 10px;
}
.user-num,
.group-num {
  background-color: #A3C5FD;
  color: #fff;
}
.group-name {
  display: inline-block;
  width: 100px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  margin-right: 20px;
}
.user-num,
.group-num {
  display: flex;
  align-items: center;
  justify-content: space-evenly;
  width: 24px;
  height: 12px;
  background: #F0F1F5;
  border-radius: 2px;
  font-size: 12px;
  line-height: 16px;
  text-align: center;
  color: #C4C6CC;
}
.more-icon {
  border-radius: 50%;
  color: #63656e;
  padding: 1px;
}
.more-icon:hover {
  background-color: #DCDEE5;
  color: #3A84FF !important;
}
.group-icon {
  font-size: 12px;
  margin-bottom: 4px;
  color: #C4C6CC;
}
.line-split {
  width: 80%;
  height: 1px;
  background: #ccc;
  margin: 10px auto;
}
.add-group-btn {
  display: flex;
  align-items: center;
  display: inline-block;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.add-icon {
  margin-right: 10px;
}
.close-btn {
  margin-bottom: 20px;
  text-align: center;
}
.rename-input {
  position: relative;
  width: 90%;
  left: 18px;
}
::v-deep .bk-popover-content {
  padding: 0 !important;
  z-index: 100 !important;
}
</style>
<style lang="scss">
.dot-menu {
  padding: 0 !important;
  .btn {
    width: 60px;
    height: 32px;
    line-height: 32px;
    text-align: center;
    font-size: 12px;
    margin-top: 0;
  }
} 
.group-more-option .btn:hover {
  background-color: #F5F7FA;
}
.menu-content {
  display: flex;
  flex-direction: column;
  .is-disable {
    color: #dcdee5;
  }
}

.delete-group-dialog {
  .title-icon {
    font-size: 42px;
    color: #ff9c01;
    margin-bottom: 15px;
  }
  .delete-title {
    white-space: normal !important;
  }
  .delete-tips {
    background-color: #f5f6fa;
    padding: 20px;
  }
  .bk-dialog-title {
    height: 26px !important;
    overflow: initial !important; 
  }
  .confirm-delete {
    margin: 15px 0;
  }
  .confirm-input {
    margin-top: 15px;
  }
  .option-btns {
    text-align: center;
    margin-top: 20px;
    .btn {
      width: 88px;
      margin-right: 10px;
    }
  }
  .warning-icon {
    margin-right: 5px;
    color: #FF9C01;
  }
}
</style>
