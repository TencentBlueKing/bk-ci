<script setup lang="ts">
import {
  ref,
  watch,
  computed
} from 'vue';
import http from '@/http/api';
import { useI18n } from 'vue-i18n';
import GitIcon from '@/image/git.png';
import TgitIcon from '@/image/tgit.png';
import GitlabIcon from '@/image/gitlab.png';
import GithubIcon from '@/image/github.png';
import SvnIcon from '@/image/svn.png';
import P4Icon from '@/image/P4.png';
import GitExpireIcon from '@/image/git-expire.png';
import TgitExpireIcon from '@/image/tgit-expire.png';
import GitlabExpireIcon from '@/image/gitlab-expire.png';
import GithubExpireIcon from '@/image/github-expire.png';
import SvnExpireIcon from '@/image/svn-expire.png';
import P4ExpireIcon from '@/image/p4-expire.png';
import { Message } from 'bkui-vue'
import { debounce } from 'lodash-es';
const { t } = useI18n();

const props = defineProps({
  oauth: Object,
});

const scmCode = computed(() => props.oauth.scmCode);
const showDeleteDialog = ref(false);
const showRefreshDialog = ref(false);
const showAuthorizeDialog = ref(false);
const page = ref(1);
const pageSize = ref(20);
const relSourceList = ref<any>([]);
const isLoading = ref(false);
const hasLoadEnd = ref(false);
const codelibPageUrl = `${window.location.origin}/console/codelib`

watch(() => showDeleteDialog.value, (val) => {
  if (!val) {
    page.value = 1;
    isLoading.value = false;
    hasLoadEnd.value = false;
    setTimeout(() => {
      relSourceList.value = [];
    }, 500);
  }
})

const getCodeIcon = (type: string, expired: boolean) => {
  const expiredIconMap = {
    'GITLAB': GitlabExpireIcon,
    'GITHUB': GithubExpireIcon,
    'TGIT-CO': TgitExpireIcon,
    'TGIT': GitExpireIcon,
    'SVN': SvnExpireIcon,
    'P4': P4ExpireIcon,
  };
  const iconMap = {
    'GITLAB': GitlabIcon,
    'GITHUB': GithubIcon,
    'TGIT-CO': TgitIcon,
    'TGIT': GitIcon,
    'SVN': SvnIcon,
    'P4': P4Icon,
  };
  return expired ? expiredIconMap[type] : iconMap[type];
};

const  createdTimeAgo = (name: string, ts: any) => {
  const now = Date.now();
  const diffMs = now - ts;

  const msPerMinute = 60 * 1000;
  const msPerHour = 60 * msPerMinute;
  const msPerDay = 24 * msPerHour;
  const daysInYear = 365;

  const days = Math.floor(diffMs / msPerDay);
  const hours = Math.floor(diffMs / msPerHour);
  const minutes = Math.floor(diffMs / msPerMinute);

  if (days >= daysInYear) {
    const years = Math.floor(days / daysInYear);
    return t('xx创建于N年前', [name, years])
  } else if (days >= 1) {
    return t('xx创建于N天前', [name, days])
  } else if (hours >= 1) {
    return t('xx创建于N小时前', [name, hours])
  } else {
    return t('xx创建于N分钟前', [name, minutes ?? 1])
  };
};

// 获取授权代码库列表
const fetchRelSourceList = () => {
  try {
    isLoading.value = true;
    http.getOauthRelSource({
      scmCode: scmCode.value,
      page: page.value,
      pageSize: pageSize.value,
      oauthUserId: props.oauth?.username
    }).then(res => {
      relSourceList.value = [...relSourceList.value, ...res.records];
      hasLoadEnd.value = relSourceList.value.length === res.count;
    })
  } catch (e) {
    console.error(e)
  } finally {
    page.value += 1;
    isLoading.value = false;
  }
}

// 删除oauth授权
const handleShowDeleteDialog = () => {
  if (!!props.oauth.repoCount) {
    fetchRelSourceList();
  }
  showDeleteDialog.value = true;
};
const handleCancelDelete = () => {
  showDeleteDialog.value = false;
};
const handleConfirmDelete = () => {
  try {
    isLoading.value = true;
    http.deleteOauth(scmCode.value, props.oauth?.username).then(res => {
      if (res) {
        Message({
          theme: 'success',
          message: t('删除成功'),
        });
      };
    })
  } catch (e) {
    console.error(e);
  } finally {
    isLoading.value = false;
    showDeleteDialog.value = false;
    fetchRelSourceList();
  }
};

// 重试删除oauth
const handleRetryDelete = () => {
  page.value = 1;
  relSourceList.value = [];
  fetchRelSourceList();
}

// 滚动获取授权代码库
const handleScroll = debounce((event: any)=> {
  const target = event.target
  const bottomDis = target.scrollHeight - target.clientHeight - target.scrollTop
  if (bottomDis <= 1 && !hasLoadEnd.value && !isLoading.value) {
    fetchRelSourceList();
  }
}) 

// 刷新oauth授权
const handleShowRefreshDialog = () => {
  showRefreshDialog.value = true;
};
const handleCancelRefresh = () => {
  showRefreshDialog.value = false;
};
const handleConfirmRefresh = () => {
  try {
    isLoading.value = true;
    const url = encodeURIComponent(window.location.href.replace('com/permission', `com/console/permission`));
    http.refreshOauth(scmCode.value, props.oauth?.username, url).then(res => {
      if (res.url) {
        window.top.open(res.url, '_self')
      }
    })
  } catch (e) {
    console.error(e)
  } finally {
    isLoading.value = false;
  };
};

const handleAuthorize = () => {
  showAuthorizeDialog.value = true;
}
</script>

<template>
  <section>
    <div :class="['oauth-card', { 'expired': oauth.expired }]">
      <div class="code-info">
        <img class="code-icon" :src="getCodeIcon(oauth.scmCode, oauth.expired)" />
        <div>
          <p class="code-type">{{ oauth.name }}</p>
          <div class="code-creator">
            <span class="name" v-bk-tooltips="t('授权账号')">
              <i class="permission-icon permission-icon-user"></i>
              {{ oauth.username ?? '--' }}
            </span>
            <span class="num" v-bk-tooltips="t('授权代码库')">
              <i class="permission-icon permission-icon-codelib"></i>
              {{ oauth.repoCount ?? '--' }}
            </span>
          </div>
        </div>
      </div>
      <div :class="['code-operate', { 'expired': oauth.expired }]">
        <div :class="{ 'create-time': !oauth.authorized }">
          {{ createdTimeAgo(oauth.operator, oauth.createTime) }}
        </div>
        <div class="btn">
          <bk-button
            v-if="!oauth.authorized"
            class="mr5"
            text
            theme="primary"
            @click="handleAuthorize">
            {{ t('OAUTH 授权') }}
          </bk-button>
          <bk-button
            v-if="oauth.authorized && oauth.expired"
            class="mr5"
            text
            theme="primary"
            @click="handleConfirmRefresh">
            {{ t('重置授权') }}
          </bk-button>
          <bk-button
            v-if="oauth.authorized && !oauth.expired"
            class="mr5"
            text
            theme="primary"
            @click="handleShowRefreshDialog">
            {{ t('刷新') }}
          </bk-button>
          <bk-button
            v-if="oauth.authorized"
            text
            theme="primary"
            @click="handleShowDeleteDialog">
            {{ t('删除') }}
          </bk-button>
        </div>
      </div>
      <div v-if="!oauth.authorized || oauth.expired" class="expired-tag">
        {{ !oauth.authorized ? t('未授权') : t('已过期') }}
      </div>
    </div>
    <bk-dialog
      v-model:is-show="showAuthorizeDialog"
      class="authorize-dialog"
    >
      <div class="content">
        <div class="title">{{ t(' OAUTH授权') }}</div>
        <div class="oauth-tips">
          <template>
            <p>{{ t('此授权用于平台和代码库进行交互，涉及如下功能：') }}</p>
            <p>1.{{ t('注册 Webhook 到代码库，用于事件触发场景') }}</p>
            <p>2.{{ t('回写提交检测状态到代码库，用于代码库支持 checker 拦截合并请求场景"') }}</p>
            <p>3.{{ t('流水线中 Checkout 代码') }}</p>
            <p>{{ t('拥有代码库注册 Webhook 权限') }}</p>
          </template>
        </div>
        <bk-button
          theme="primary"
          @click="handleConfirmRefresh"
        >
          {{ t('OAUTH授权') }}
        </bk-button>
      </div>
    </bk-dialog>
    <bk-dialog
      v-model:is-show="showDeleteDialog"
      class="oauth-dialog"
    >
      <template v-if="!oauth.repoCount">
        <div class="title">{{ t('确认删除 OAUTH?') }}</div>
        <div class="content">
          <span>{{ t('OAUTH 授权:') }}</span>
          <span>{{ oauth.name }}</span>
        </div>
      </template>
      <template v-else>
        <i class="warning-icon permission-icon permission-icon-tishi" />
        <div class="title">{{ t('无法删除 OAUTH') }}</div>
        <div class="cannot-delete-content">
          <span>{{ t('OAUTH 授权:') }}</span>
          <span>{{ oauth.name }}</span>
          <div class="tips">
            <p>{{ t('有 X 个代码库正在使用此 OAUTH 授权，无法直接删除。', [oauth.repoCount]) }}</p>
            <p>{{ t('请先修改对应代码库的授权方式，或者请新的负责人重置代码库授权后重试。') }}</p>
          </div>
          <li class="resource-list-header">{{ t('授权代码库') }}</li>
          <ul
            class="resource-list-content"
            @scroll.passive="handleScroll"
          >
            <li
              v-for="item in relSourceList"
              :key="item.name"
            >
            <bk-overflow-title>
              <a
                :href="`${codelibPageUrl}/${item.projectId}?searchName=${item.aliasName}`"
                target="_blank"
              >
                {{ item.aliasName }}
              </a>
            </bk-overflow-title>
            </li>
          </ul>
        </div>
      </template>
      <div class="operate-btn">
        <bk-button
          v-if="!!oauth.repoCount"
          class="btn"
          :loading="isLoading"
          theme="primary"
          @click="handleRetryDelete"
        >
          {{ t('重试') }}
        </bk-button>
        <bk-button
          v-else
          class="btn"
          :loading="isLoading"
          theme="danger"
          @click="handleConfirmDelete"
        >
          {{ t('删除') }}
        </bk-button>
        <bk-button
          class="btn"
          :loading="isLoading"
          @click="handleCancelDelete"
        >
          {{ t('取消') }}
        </bk-button>
      </div>
    </bk-dialog>

    <bk-dialog
      v-model:is-show="showRefreshDialog"
      class="oauth-dialog"
    >
      <div class="title">{{ t('确认刷新 OAUTH?') }}</div>
      <div class="refresh-content">
        <span>{{ t('OAUTH 授权:') }}</span>
        <span>{{ oauth.name }}</span>
        <div class="tips">
          {{ t('刷新过程中可能会导致正在使用此 OAUTH 授权的流水线运行失败。') }}
        </div>
      </div>
      <div class="operate-btn">
        <bk-button
          class="btn"
          :loading="isLoading"
          theme="primary"
          @click="handleConfirmRefresh"
        >
          {{ t('刷新') }}
        </bk-button>
        <bk-button
          class="btn"
          :loading="isLoading"
          @click="handleCancelRefresh"
        >
          {{ t('取消') }}
        </bk-button>
      </div>
    </bk-dialog>
  </section>
</template>

<style lang="scss" scoped>
  .oauth-card {
    position: relative;
    width: 310px;
    height: 120px;
    flex-shrink: 0;
    background: #FFFFFF;
    box-shadow: 0 2px 4px 0 #1919290d;
    margin-left: 20px;
    margin-bottom: 15px;
    &.expired {
      color: #C4C6CC !important;
    }
  }
  .code-info {
    display: flex;
    height: 80px;
    padding: 16px;
    background: #FFFFFF;
    .code-icon {
      margin-right: 8px;
    }
    .code-type {
      font-size: 16px;
      font-weight: 700;
      padding-bottom: 5px;
    }
    .code-creator {
      .name {
        display: inline-flex;
        margin-right: 32px;
      }
      .num {
        display: inline-flex;
      }
      .permission-icon {
        position: relative;
        top: 2px;
        margin-right: 5px;
      }
    }
  }
  .code-operate {
    display: flex;
    justify-content: space-between;
    height: 40px;
    line-height: 40px;
    color: #979BA5;
    padding: 0 16px;
    background: #FAFBFD;
    box-shadow: 0 -1px 0 0 #EAEBF0;
    .create-time {
      opacity: 0;
    }
    &.expired {
      color: #C4C6CC !important;
    }
    .mr5 {
      margin-right: 5px;
    }
  }
  .expired-tag {
    position: absolute;
    top: 0;
    right: 0;
    height: 16px;
    padding: 0 12px;
    background: #C4C6CC;
    border-radius: 0 0 0 16px;
    color: #fff;
  }
</style>

<style lang="scss">
  .authorize-dialog {
    .bk-modal-content {
      margin-bottom: 10px;
    }
    
    .oauth-tips {
      margin-bottom: 26px;
      font-size: 14px;
      text-align: left;
      color: #979BA5;
    }
  }
  .authorize-dialog,
  .oauth-dialog {
    .bk-modal-header,
    .bk-modal-footer {
      display: none;
    }
    .bk-modal-content {
      text-align: center;
      padding-top: 40px !important;
    }
    .title {
      font-size: 24px;
      color: #313238;
      margin-bottom: 15px;
    }
    .content {
      color: #63656E;
    }
    .operate-btn {
      margin-top: 20px;
      .btn {
        min-width: 88px;
        margin-right: 5px;
      }
    }

    .warning-icon {
      color: #FF9C01;
      font-size: 40px;
      display: inline-block;
      margin-bottom: 15px;
    }
    .refresh-content,
    .cannot-delete-content {
      text-align: left;
      .tips {
        margin-top: 10px;
        padding: 12px;
        background: #F5F6FA;
        border-radius: 4px;
        color: #63656E;
      }
    }
    .resource-list-header {
      margin-top: 10px;
      padding: 0 12px;
      height: 32px;
      line-height: 32px;
      border-bottom: 1px solid #EAEBF0;
      background: #F0F1F5;
    }
    .resource-list-content {
      border: 1px solid #EAEBF0;
      height: 210px;
      overflow-y: auto;
      &::-webkit-scrollbar {
        width: 6px;
        height: 6px;
        &-thumb {
          border-radius: 20px;
          background: #a5a5a5;
          box-shadow: inset 0 0 6px hsla(0, 0%, 80%, .3);
        }
      }
      li {
        width: 90%;
        overflow: hidden;
        white-space: nowrap;
        text-overflow: ellipsis;
      }
      li a {
        padding: 0 12px;
        height: 32px;
        line-height: 32px;
        cursor: pointer;
        color: #3A84FF;
        border-bottom: 1px solid #EAEBF0;
        &:last-child {
          border-bottom: none;
        }
      }
      
    }
  }
  
</style>