<template>
  <article class="group-manage">
    <div class="content-wrapper">
      <bk-exception
        class="exception-wrap-item exception-part"
        type="403"
        scene="part"
        :title="title"
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
      default: '无该项目用户组管理权限',
    },
  },
};
</script>

<style lang="scss" scoped>
    .group-manage {
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
