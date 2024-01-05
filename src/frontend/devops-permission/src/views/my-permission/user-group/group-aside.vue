<template>
  <article
    class="group-aside"
  >
    <div class="select-project">
      <p class="title">{{ $t('选择项目') }}</p>
      <bk-select
          v-model="projectCode"
          filterable
          :clearable="false"
          :input-search="false"
          :scroll-loading="scrollLoading"
          :remote-method="handleSearchProject"
          @change="handleSelectProject"
        >
          <div v-for="(project, index) in projectList"
            :key="index">
            <bk-option
                :value="project.englishName"
                :disabled="!project.managePermission"
                :label="project.projectName"
            >
              <div
                v-bk-tooltips="{
                  disabled: project.managePermission,
                  content: $t('非项目管理员，无操作权限')
                }"
                class="option-item">
                {{ project.projectName }}
              </div>
            </bk-option>
          </div>
        </bk-select>
    </div>
    <div class="line-split" />
    <span class="group-title">{{ $t('权限角色') }}</span>
    <scroll-load-list
      v-if="projectCode"
      class="group-list"
      ref="loadList"
      :list="groupList"
      :has-load-end="hasLoadEnd"
      :page="page"
      :project-code="projectCode"
      :get-data-method="handleGetData"
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
          v-show="group.groupId !== renameGroupId"
        >
          <span class="group-name" :title="group.name">{{ group.name }}</span>
          <span class="user-num">
            <img src="./svg/user.svg?inline" class="group-icon">
            {{ group.userCount }}
          </span>
          <span class="group-num">
            <img src="./svg/organization.svg?inline" class="group-icon">
            {{ group.departmentCount }}
          </span>
          <bk-popover
            v-if="resourceType === 'project'"
            class="group-more-option"
            placement="bottom"
            theme="light dot-menu"
            :popoverDelay="[100, 0]"
            :arrow="false"
            offset="15"
            :distance="0">
            <i @click.stop class="more-icon permission-icon permission-icon-more-fill"></i>
            <template #content>
              <div class="menu-content">
                <bk-button
                  v-if="!group.defaultGroup"
                  class="btn"
                  text
                  @click="handleShowRename(group)"
                >
                  {{ $t('重命名') }}
                </bk-button>
                <bk-button
                  class="btn"
                  :disabled="group.defaultGroup"
                  text
                  @click="handleShowDeleteGroup(group)">
                  {{ $t('删除') }}
                </bk-button>
              </div>
            </template>
          </bk-popover>
        </div>
      </template>
    </scroll-load-list>
    <div class="line-split" />
    <div
      v-if="projectCode"
      :class="{ 'group-item': true, 'group-active': activeTab === '' }"
      @click="handleCreateGroup">
      <span class="add-group-btn">
        <i class="permission-icon permission-icon-add-fill add-icon"></i>
        {{ $t('新建用户组') }}
      </span>
    </div>
    <bk-dialog
      dialogType="show"
      header-align="center"
      theme="danger"
      extCls="delete-group-dialog"
      :quick-close="false"
      :is-show="deleteObj.isShow"
      :is-loading="deleteObj.isLoading"
      @closed="handleHiddenDeleteGroup"
    >
      <template #header>
        <div class="permission-icon permission-icon-warning-circle-fill title-icon"></div>
        <p class="delete-title">{{ $t('确认删除【】用户组？', [deleteObj.group.name]) }}</p>
      </template>
      <div class="delete-tips">
        <p>{{ $t('删除用户组【】将执行如下操作：', [deleteObj.group.name]) }}</p>
        <p>
          <i class="permission-icon permission-icon-warning-circle-fill warning-icon"></i>
          {{ $t('将用户和组织从组中移除') }}
        </p>
        <p>
          <i class="permission-icon permission-icon-warning-circle-fill warning-icon"></i>
          {{ $t('删除组内用户继承该组的权限') }}
        </p>
        <p>
          <i class="permission-icon permission-icon-warning-circle-fill warning-icon"></i>
          {{ $t('删除组信息和组权限') }}
        </p>
      </div>
      <div class="confirm-delete">
        <i18n-t keypath="此操作提交后将不能恢复，为避免误删除，请再次确认你的操作：" style="color: #737987;font-size: 14px;" tag="div">
          <span style="color: red;">{{$t('不能恢复')}}</span>
        </i18n-t>
        <bk-input
          v-model="keyWords"
          :placeholder="$t('请输入待删除的用户组名')"
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
          {{ $t('删除') }}
        </bk-button>
        <bk-button
          class="btn"
          @click="handleHiddenDeleteGroup"
        >
          {{ $t('取消') }}
        </bk-button>
      </div>
    </bk-dialog>
  </article>
</template>

<script>
import ScrollLoadList from './scroll-load-list';
import ajax from './ajax.js';
import { Message } from 'bkui-vue';
import tools from '@/utils/tools';
export default {
  components: {
    ScrollLoadList,
  },
  props: {
    activeIndex: {
      type: Boolean,
      default: 0,
    },
    showCreateGroup: {
      type: Boolean,
      default: true,
    },
    ajaxPrefix: {
      type: String,
      default: '',
    },
    projectList: {
      type: Array,
      default: () => [],
    },
  },
  emits: ['choose-group', 'create-group', 'close-manage'],
  data() {
    return {
      resourceType: 'project',
      projectCode: '',
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
    };
  },
  computed: {
    disableDeleteBtn() {
      return !(this.keyWords === this.deleteObj.group.name);
    },
  },
  watch: {
    activeIndex(newVal) {
      this.activeTab = this.groupList[newVal]?.groupId || '';
    },
    projectList(list) {
      const project = list.find(i => i.projectCode === this.projectCode);
      if (project && (project.managePermission === false)) {
        this.projectCode = ''
      }
    },
    projectCode(val) {
      this.page = 1;
      if (val) {
        this.groupList = [];
      }
    },
  },
  async created() {
    window.addEventListener('message', this.handleMessage);
    this.projectCode = this.$route.query.project_code || tools.getCookie('X-DEVOPS-PROJECT-ID') || '';
  },

  beforeUnmount() {
    window.removeEventListener('message', this.handleMessage);
  },
  methods: {
    handleGetData(pageSize) {
      return ajax
        .get(`${this.ajaxPrefix}/auth/api/user/auth/resource/${this.projectCode}/${this.resourceType}/${this.projectCode}/listGroup?page=${this.page}&pageSize=${pageSize}`)
        .then(({ data }) => {
          this.hasLoadEnd = !data.hasNext;
          this.groupList.push(...data.records);
          // 首页需要加载
          if (this.page === 1) {
            const chooseGroup = this.groupList.find(group => +group.groupId === +this.$route.query?.groupId) || this.groupList[0];
            this.handleChooseGroup(chooseGroup);
          }
          this.page += 1
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
      return ajax
        .delete(`${this.ajaxPrefix}/auth/api/user/auth/resource/group/${this.projectCode}/${this.resourceType}/${this.deleteObj.group.groupId}`)
        .then(() => {
          this.handleHiddenDeleteGroup();
          this.refreshList();
          Message({
            theme: 'success',
            message: this.$t('删除成功')
          });
        })
        .finally(() => {
          this.deleteObj.isLoading = false;
        });
    },
    handleChooseGroup(group) {
      this.$router.replace({
        query: {
          groupId: group.groupId
        }
      })
      this.activeTab = group.groupId;
      this.curGroupIndex = this.groupList.findIndex(item => item.groupId === group.groupId);
      this.$emit('choose-group', group);
    },
    handleCreateGroup() {
      this.activeTab = '';
      this.$emit('create-group');
    },
    handleCloseManage() {
      this.isClosing = true;
      return ajax
        .put(`${this.ajaxPrefix}/auth/api/user/auth/resource/${this.projectCode}/${this.resourceType}/${this.resourceCode}/disable`)
        .then(() => {
          this.$emit('close-manage');
        })
        .finally(() => {
          this.isClosing = false;
        });
    },
    handleMessage(event) {
      const { data } = event;
      if (data.type === 'IAM') {
        switch (data.code) {
          case 'create_user_group_submit':
            this
              .refreshList()
              .then(() => {
                const group = this.groupList.find(group => group.groupId === data?.data?.id) || this.groupList[0];
                this.handleChooseGroup(group);
              })
            break;
          case 'create_user_group_cancel':
            this.handleChooseGroup(this.groupList[0]);
            break;
          case 'add_user_confirm':
            this.groupList[this.curGroupIndex].departmentCount += data.data.departments.length
            this.groupList[this.curGroupIndex].userCount += data.data.users.length
            break;
          case 'remove_user_confirm':
            const departments = data.data.members.filter(i => i.type === 'department')
            const users = data.data.members.filter(i => i.type === 'user')
            this.groupList[this.curGroupIndex].departmentCount -= departments.length
            this.groupList[this.curGroupIndex].userCount -= users.length
            break;
          case 'change_group_detail_tab':
            this.$emit('change-group-detail-tab', data.data.tab)
        }
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
      if (this.displayGroupName === group && group.name || !group) {
        this.isRename = false;
        this.renameGroupId = 0;
        this.displayGroupName = '';
        return
      }
      return ajax
        .put(`${this.ajaxPrefix}/auth/api/user/auth/resource/group/${this.projectCode}/${this.resourceType}/${this.renameGroupId}/rename`, {
          groupName: this.displayGroupName,
        })
        .then(() => {
          group.name = this.displayGroupName;
          Message({
            theme: 'success',
            message: this.$t('修改成功')
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
      this.$emit('search-project', val);
    },
    handleToProjectManage (project) {
      const { englishName } = project;
      window.open(`/console/perm/my-project?project_code=${englishName}`)
    },
    handleSelectProject (val) {
      this.$emit('changeProject')
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
}
.select-project {
  padding: 10px 24px 0;
  .title {
    font-size: 14px;
    font-weight: 700;
    margin-bottom: 5px;
  }
}
.option-item {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  &:hover {
    .edit-icon {
      display: block;
    }
  }
  .edit-icon {
    display: none;
    color: blue;
    cursor: pointer;
  }
}
.group-list {
  max-height: calc(100% - 210px);
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
    filter: invert(100%) sepia(0%) saturate(1%) hue-rotate(151deg) brightness(104%) contrast(101%);
  }
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
}
.user-num,
.group-num {
  display: flex;
  align-items: center;
  justify-content: space-evenly;
  width: 40px;
  height: 16px;
  background: #F0F1F5;
  border-radius: 2px;
  font-size: 12px;
  line-height: 16px;
  margin-right: 3px;
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
  height: 12px;
  width: 12px;
  filter: invert(89%) sepia(8%) saturate(136%) hue-rotate(187deg) brightness(91%) contrast(86%);
}
.line-split {
  height: 1px;
  background: #dcdee5;
  margin: 10px 15px;
}
.add-group-btn {
  display: flex;
  align-items: center;
}
.add-icon {
  margin-right: 10px;
}
.close-btn {
  margin-bottom: 20px;
  text-align: center;
}
.small-size {
  scale: 0.9;
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
  .bk-dialog-header {
    padding: 15px 0;
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
