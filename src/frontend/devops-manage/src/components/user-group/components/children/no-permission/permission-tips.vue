<template>
  <article class="group-manage">
    <div class="content-wrapper">
      <bk-exception
        class="exception-wrap-item exception-part"
        :type="exceptionObj.type"
        scene="part"
        :title="exceptionObj.title"
        :description="exceptionObj.description"
      >
      </bk-exception>
    </div>
  </article>
</template>

<script>
export default {
  props: {
    title: {
      type: String,
      default: '',
    },
    projectCode: {
      type: String,
    },
    errorCode: {
      type: Number,
    }
  },
  created () {
    this.exceptionObj.type = this.errorCode
    if (this.errorCode === 404)  {
      this.exceptionObj.showBtn = false;
      this.exceptionObj.type = '404';
      this.exceptionObj.title = this.$t('项目不存在');
      this.exceptionObj.description = '';
    } else if (this.errorCode === 2119042) {
      this.exceptionObj.showBtn = false;
      this.exceptionObj.type = '403';
      this.exceptionObj.title = this.$t('项目创建中');
      this.exceptionObj.description = this.$t('项目正在创建审批中，请耐心等待', [this.projectCode]);
    }
  },
  data() {
    return {
      exceptionObj:{
        type: '',
        title: '',
        description: '',
        showBtn: false
      }
    }
  },
};
</script>

<style lang="scss" scoped>
    .group-manage {
        height: 100%;
        flex: 1;
    }
    .content-wrapper {
        display: flex;
        align-items: center;
        flex-direction: column;
        padding-top: 10%;
        width: 100%;
        height: 100%;
        background-color: #fff;
        box-shadow: 0 2px 2px 0 rgba(0,0,0,0.15);
        text-align: center;
        font-size: 14px;
    }
    .btn {
        margin-top: 32px;
    }
    ::v-deep .bk-exception-img {
        height: 240px;
    }
    ::v-deep .bk-exception-title {
        font-size: 24px;
        color: #313238;
        margin-top: 18px;
    }
</style>
