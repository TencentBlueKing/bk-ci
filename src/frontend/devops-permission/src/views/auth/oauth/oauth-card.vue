<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import GitIcon from '@/image/git.png';
import TgitIcon from '@/image/tgit.png';
import GitlabIcon from '@/image/gitlab.png';
import GithubIcon from '@/image/github.png';
import SvnIcon from '@/image/svn.png';
import P4Icon from '@/image/P4.png';
const { t } = useI18n();

const props = defineProps({
  oauth: Object,
});

const getCodeIcon = (type) => {
  const iconMap ={
    'GITLAB': GitlabIcon,
    'GITHUB': GithubIcon,
    'TGIT': TgitIcon,
    'GIT': GitIcon,
    'SVN': SvnIcon,
    'P4': P4Icon,
  };
  return iconMap[type]
}

const  createdTimeAgo = (name, ts) => {
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
  }
}
</script>

<template>
  <div class="oauth-card">
    <div class="code-info">
      <img class="code-icon" :src="getCodeIcon(oauth.type)" />
      <div>
        <p class="code-type">{{ oauth.type }}</p>
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
    <div class="code-operate">
      {{ createdTimeAgo(oauth.username, oauth.createTime) }}
    </div>
  </div>
</template>

<style lang="scss" scoped>
  .oauth-card {
    width: 310px;
    height: 120px;
    flex-shrink: 0;
    background: #FFFFFF;
    box-shadow: 0 2px 4px 0 #1919290d;
    margin-left: 20px;
    margin-bottom: 15px;
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
    height: 40px;
    line-height: 40px;
    color: #979BA5;
    padding: 0 16px;
    background: #FAFBFD;
    box-shadow: 0 -1px 0 0 #EAEBF0;
  }
</style>