<template>
  <article class="group-aside" v-bkloading="{ isLoading: !groupList.length }">
    <span class="group-title">{{ $t('权限角色') }}</span>
    <scroll-load-list
      class="group-list"
      ref="loadList"
      :list="groupList"
      :has-load-end="hasLoadEnd"
      :page="page"
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
            <img src="../../../svg/user.svg?inline" class="group-icon">
            {{ group.userCount }}
          </span>
          <span class="group-num">
            <img src="../../../svg/organization.svg?inline" class="group-icon">
            {{ group.departmentCount }}
          </span>
          <bk-popover
            v-if="resourceType === 'project'"
            class="group-more-option"
            placement="bottom"
            trigger="click"
            theme="dot-menu light"
            :arrow="false"
            offset="15"
            :distance="0">
            <i class="more-icon manage-icon manage-icon-more-fill"></i>
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
      v-if="showCreateGroup"
      :class="{ 'group-item': true, 'group-active': activeTab === '' }"
      @click="handleCreateGroup">
      <span class="add-group-btn">
        <i class="bk-icon bk-icon-add-fill add-icon"></i>
        {{ $t('新建用户组') }}
      </span>
    </div>
    <div
      v-if="resourceType !== 'project'"
      class="close-btn"
    >
      <bk-button @click="handleCloseManage" :loading="isClosing">{{ $t('关闭权限管理') }}</bk-button>
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
        <div class="manage-icon manage-icon-warning-circle-fill title-icon"></div>
        <p class="delete-title">{{ $t('确认删除【】用户组？', [deleteObj.group.name]) }}</p>
      </template>
      <div class="delete-tips">
        <p>{{ $t('删除用户组【】将执行如下操作：', [deleteObj.group.name]) }}</p>
        <p>
          <i class="manage-icon manage-icon-warning-circle-fill warning-icon"></i>
          {{ $t('将用户和组织从组中移除') }}
        </p>
        <p>
          <i class="manage-icon manage-icon-warning-circle-fill warning-icon"></i>
          {{ $t('删除组内用户继承该组的权限') }}
        </p>
        <p>
          <i class="manage-icon manage-icon-warning-circle-fill warning-icon"></i>
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
import ScrollLoadList from '../../widget-components/scroll-load-list';
import ajax from '../../../ajax/index';
import { Message } from 'bkui-vue';
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
      renameGroupId: '',
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
  },
  async created() {
    window.addEventListener('message', this.handleMessage);
  },

  beforeUnmount() {
    window.removeEventListener('message', this.handleMessage);
  },
  methods: {
    handleGetData(pageSize) {
      return ajax
        .get(`${this.ajaxPrefix}/auth/api/user/auth/resource/${this.projectCode}/${this.resourceType}/${this.resourceCode}/listGroup?page=${this.page}&pageSize=${pageSize}`)
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
            const departments = data.data.members.filters(i => i.type === department)
            const users = data.data.members.filters(i => i.type === user)
            this.groupList[this.curGroupIndex].departmentCount -= departments.length
            this.groupList[this.curGroupIndex].userCount -= users.length
            break;
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
      if (this.displayGroupName === group.name) {
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
        .finally(() => {
          this.isRename = false;
          this.renameGroupId = 0;
          this.displayGroupName = '';
        })
    },
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
.group-list {
  max-height: calc(100% - 62px);
  height: auto;
  overflow-y: auto;
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
    color: #3A84FF;
    background-color: #E1ECFF;
    .group-icon {
      filter: invert(100%) sepia(0%) saturate(1%) hue-rotate(151deg) brightness(104%) contrast(101%);
    }
  }
}
.group-item:hover .user-num,
.group-item:hover .group-num {
  background-color: #A3C5FD;
  color: #fff;
}

.group-active {
  color: #3A84FF;
  background-color: #E1ECFF;
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
.group-icon {
  height: 12px;
  width: 12px;
  filter: invert(89%) sepia(8%) saturate(136%) hue-rotate(187deg) brightness(91%) contrast(86%);
}
.more-icon {
  height: 18px;
  filter: invert(89%) sepia(8%) saturate(136%) hue-rotate(187deg) brightness(91%) contrast(86%);
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
.group-more-option .btn {
  width: 60px;
  height: 32px;
  line-height: 32px;
  text-align: center;
  font-size: 12px;
  margin-top: 0;
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
