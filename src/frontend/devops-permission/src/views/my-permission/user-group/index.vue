<template>
  <section class="permission-manage">
    <group-aside
      :ajax-prefix="ajaxPrefix"
      :project-list="projectList"
      @choose-group="handleChooseGroup"
      @create-group="handleCreateGroup"
      @change-group-detail-tab="handleChangeGroupDetailTab"
      @search-project="handleSearchProject"
      @change-project="handleChangeProject"
    />
    <div v-if="initPage" class="no-data">
      <img src="./images/box.png">
      {{ t('请先选择一个项目') }}
    </div>
    <iam-iframe
      v-else-if="!initPage && path"
      :path="path"
    />
  </section>
</template>

<script>
import ajax from './ajax.js';
import tools from '@/utils/tools';
import GroupAside from './group-aside.vue';
import IamIframe from './iam-Iframe.vue';
import { useI18n } from 'vue-i18n';

export default {
  name: 'permission-manage',

  components: {
    GroupAside,
    IamIframe,
  },

  props: {
    showCreateGroup: {
      type: Boolean,
      default: true,
    },
    ajaxPrefix: {
      type: String,
      default: '',
    },
  },

  emits: ['close-manage'],

  data() {
    const { t } = useI18n();
    return {
      path: '',
      activeIndex: '',
      tabName: '',
      list: [],
      searchProjectKey: '',
      projectCode: '',
      initPage: false,
      t
    };
  },


  computed: {
    projectList() {
      return this.list.filter(i => i.projectName.includes(this.searchProjectKey));
    },
  },

  created() {
    this.getProjectList();
    this.projectCode = this.$route.query.project_code || tools.getCookie('X-DEVOPS-PROJECT-ID') || '';
  },

  methods: {
    getProjectList() {
      return ajax
        .get(`${this.ajaxPrefix}/project/api/user/projects/?enabled=true`)
        .then((res) => {
          this.list = res.data;
          const project = this.list.find(i => i.projectCode === this.projectCode);
          if (project) {
            this.initPage = project.managePermission === false || !/rbac/.test(project.routerTag);
          };
        });
    },
    handleChangeGroupDetailTab(payload) {
      this.tabName = payload;
    },
    handleChooseGroup(payload) {
      this.path = `user-group-detail/${payload.groupId}?role_id=${payload.managerId}&tab=${this.tabName}`;
    },
    handleCreateGroup() {
      this.activeIndex = '';
      this.path = 'create-user-group';
    },
    handleSearchProject(val) {
      this.searchProjectKey = val;
    },
    handleChangeProject() {
      this.initPage = false
    }
  },
};
</script>

<style lang="scss" scoped>
.permission-manage {
    display: flex;
    height: 100%;
    box-shadow: 0 2px 2px 0 #00000026;
}
.no-data {
  width: 100%;
  font-size: 18px;
  display: flex;
  align-items: center;
  flex-direction: column;
  margin-top: 20%;
  img {
    margin-bottom: 20px;
  }
}
</style>
