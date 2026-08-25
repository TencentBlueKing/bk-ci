<template>
  <article class="group-table">
    <bk-table
      v-bkloading="{ isLoading }"
      :data="memberList"
    >
      <bk-table-column :label="t('用户组')" prop="groupName"></bk-table-column>
      <bk-table-column :label="t('添加时间')" prop="createdTime">
        <template #default="{ row }">
          <span>{{ row.createdTime ? row.createdTime : '--' }} </span>
        </template>
      </bk-table-column>
      <bk-table-column :label="t('有效期')" prop="expiredDisplay">
        <template #default="{ row }">
          <span>{{ row.status === 'EXPIRED' ? row.expiredDisplay : row.expiredDisplay + t('天') }} </span>
        </template>
      </bk-table-column>
      <bk-table-column :label="t('状态')" prop="status">
        <template #default="{ row }">
          <div class="status-content">
            <i :class="{
              'status-icon': true,
              [statusIconClass(row.status)]: true
            }" />
            {{ statusFormatter(row.status) }}
          </div>
        </template>
      </bk-table-column>
      <bk-table-column :label="t('操作')">
        <template #default="{ row }">
          <bk-button
            class="btn"
            theme="primary"
            text
            @click="handleViewDetail(row)"
          >{{ t('权限详情') }}</bk-button>
          <bk-button
            class="btn"
            theme="primary"
            text
            v-if="row.status === 'NOT_JOINED'"
            @click="handleApply(row)"
          >{{ t('申请加入') }}</bk-button>
          <span
            v-bk-tooltips="{
              content: t('通过用户组获得权限，若需续期请联系项目管理员续期用户组'),
              disabled: row.directAdded
            }"
          >
            <bk-button
              class="btn"
              theme="primary"
              text
              :disabled="!row.directAdded"
              v-if="['EXPIRED', 'NORMAL'].includes(row.status)"
              @click="handleRenewal(row)"
            >{{ t('续期') }}</bk-button>
          </span>
          <span
            v-bk-tooltips="{
              content: t('通过用户组获得权限，若需退出先联系项目管理员退出用户组'),
              disabled: row.directAdded
            }"
          >
            <bk-button
              class="btn"
              theme="primary"
              text
              :disabled="!row.directAdded"
              v-if="['EXPIRED', 'NORMAL'].includes(row.status)"
              @click="handleShowLogout(row)"
            >{{ t('退出') }}</bk-button>
          </span>
        </template>
      </bk-table-column>
    </bk-table>
    <bk-sideslider
      quick-close
      :is-show.sync="showDetail"
      :width="640"
    >
      <template v-slot:header>
        <div class="detail-title">
          {{ t('权限详情') }}
          <span class="group-name">{{ groupName }}</span>
        </div>
      </template>
      <template v-slot:content>
        <div class="detail-content" v-bkloading="{ isLoading: isDetailLoading }">
          <div class="title">{{ permissionTitle }}</div>
          <div class="content">
            <bk-checkbox
              v-for="(item, index) in groupPolicies"
              :key="index"
              v-model="item.permission"
              disabled
              class="permission-item"
            >
              {{ item.actionName }}
            </bk-checkbox>
          </div>
        </div>
      </template>
    </bk-sideslider>
    <bk-dialog
      :value="logout.isShow"
      :title="t('确认退出用户组')"
      :loading="logout.loading"
      @confirm="handleLogout"
      @cancel="handleCancelLogout"
    >
      {{ t('退出后，将无法再使用所赋予的权限。', [logout.name]) }}
    </bk-dialog>
    <apply-dialog
      :is-show="apply.isShow"
      :ajax-prefix="ajaxPrefix"
      :project-code="projectCode"
      :resource-type="resourceType"
      v-bind="apply"
      @cancel="() => apply.isShow = false"
    />
  </article>
</template>

<script>
import ajax from '../../../ajax/index';
import ApplyDialog from './apply-dialog.vue';
import { localeMixins } from '../../../utils/locale'

const initFormData = () => ({
  isShow: false,
  groupName: '',
  groupId: '',
  expiredDisplay: '',
  title: '',
  type: '',
});
export default {
  components: {
    ApplyDialog,
  },
  mixins: [localeMixins],
  props: {
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
    ajaxPrefix: {
      type: String,
      default: '',
    },
  },

  data() {
    return {
      showDetail: false,
      logout: {
        loading: false,
        isShow: false,
        groupId: '',
        name: '',
      },
      apply: initFormData(),
      memberList: [],
      isLoading: false,
      isDetailLoading: false,
      groupPolicies: [],
      groupName: '',
    };
  },
  computed: {
    permissionTitle () {
      const titleMap = {
        pipeline: this.t('流水线管理'),
        pipeline_template: this.t('流水线模板管理'),
        pipeline_group: this.t('流水线组管理'),
      }
      return titleMap[this.resourceType];
    }
  },

  mounted() {
    this.getMemberList();
  },

  methods: {
    handleHidden() {
      this.showDetail = false;
    },

    getMemberList() {
      this.isLoading = true;
      return ajax
        .get(`${this.ajaxPrefix}/auth/api/user/auth/resource/${this.projectCode}/${this.resourceType}/${this.resourceCode}/groupMember`)
        .then((res) => {
          this.memberList = res.data;
        })
        .catch((err) => {
          this.$bkMessage({
            theme: 'error',
            message: err.message || err,
          });
        })
        .finally(() => {
          this.isLoading = false;
        });
    },

    handleViewDetail(row) {
      const { groupId, groupName } = row;
      this.groupName = groupName;
      this.showDetail = true;
      this.isDetailLoading = true;
      ajax
        .get(`${this.ajaxPrefix}/auth/api/user/auth/resource/group/${this.projectCode}/${this.resourceType}/${groupId}/groupPolicies`)
        .then(({ data }) => {
          this.groupPolicies = data;
        })
        .catch((err) => {
          this.$bkMessage({
            theme: 'error',
            message: err.message || err,
          });
        })
        .finally(() => {
          this.isDetailLoading = false;
        });
    },

    statusFormatter(status) {
      const map = {
        NOT_JOINED: this.t('未加入'),
        NORMAL: this.t('正常'),
        EXPIRED: this.t('已过期'),
      };
      return map[status];
    },

    statusIconClass (status) {
      const map = {
        NOT_JOINED: 'default',
        NORMAL: 'success',
        EXPIRED: 'failed',
      };
      return map[status];
    },

    handleRenewal(row) {
      this.apply.isShow = true;
      this.apply.groupName = row.groupName;
      this.apply.groupId = row.groupId;
      this.apply.status = row.status;
      this.apply.expiredDisplay = row.expiredDisplay;
      this.apply.title = this.t('续期');
      this.apply.type = 'renewal';
    },

    handleApply(row) {
      this.apply.isShow = true;
      this.apply.groupName = row.groupName;
      this.apply.groupId = row.groupId;
      this.apply.title = this.t('申请加入');
      this.apply.type = 'apply';
    },

    handleShowLogout(row) {
      this.logout.isShow = true;
      this.logout.groupId = row.groupId;
      this.logout.name = row.groupName;
    },

    handleCancelLogout() {
      this.logout.isShow = false;
    },

    handleLogout() {
      this.logout.loading = true;
      return ajax
        .delete(`${this.ajaxPrefix}/auth/api/user/auth/resource/group/${this.projectCode}/${this.resourceType}/${this.logout.groupId}/member`)
        .then(() => {
          this.handleCancelLogout();
          this.getMemberList();
        })
        .catch((err) => {
          this.$bkMessage({
            theme: 'error',
            message: err.message || err,
          });
        })
        .finally(() => {
          this.logout.loading = false;
        });
    },
  },
};
</script>

<style lang="scss" scoped>
    .btn {
        margin-right: 5px;
    }
    .group-name {
        font-size: 12px;
        color: #979BA5;
        margin-left: 10px;
    }
    .status-content {
        display: flex;
        align-items: center;
    }
    .status-icon {
        display: inline-block;
        height: 13px;
        width: 13px;
        border-radius: 50%;
        margin-right: 3px;
        &.default {
          border: 3px solid #c4c6cc;
        }
        &.success {
          border: 3px solid #3fc06d;
        }
        &.failed {
          border: 3px solid #ea3636;
        }
    }
    .detail-content {
        padding: 20px;
        .title {
            font-size: 14px;
            color: #313238;
            margin-left: 14px;
            &::before {
                content: '';
                position: absolute;
                left: 20px;
                top: 22px;
                width: 4px;
                height: 16px;
                background: #699DF4;
                border-radius: 1px;
            }
        }
        .content {
            margin-top: 15px;
        }
        .permission-item {
            min-width: 150px;
            margin-bottom: 10px;
            cursor: default !important;
        }
         .is-disabled {
            .bk-checkbox-text {
                color:#d6d7d7 !important;
            }
        }
        .is-checked {
            .bk-checkbox {
                background-color: #c2daff !important;
                border-color: #c2daff !important;
            }
            .bk-checkbox-text {
                color: #63656e !important;
            }
        }
    }
</style>
