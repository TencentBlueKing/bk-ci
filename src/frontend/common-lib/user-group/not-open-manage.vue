<template>
    <article class="group-manage">
        <div class="content-wrapper">
            <bk-exception
                class="exception-wrap-item exception-part"
                type="403"
                scene="part"
                :class="{ 'exception-gray': isGray }"
            >
                <div>{{ title }}</div>
                <bk-button
                    class="btn"
                    theme="primary"
                    @click="openManage"
                    :loading="isOpenManageLoading"
                >{{ $t('开启权限管理') }}</bk-button>
            </bk-exception>
        </div>
    </article>
</template>

<script>
    export default {
        props: {
            openManage: {
                type: Function,
                default: () => {}
            },
            resourceType: {
                type: String,
                default: ''
            },
            isOpenManageLoading: {
                type: Boolean
            }
        },

        computed: {
            title () {
                const titleMap = {
                    pipeline: this.$t('尚未开启此流水线权限管理功能'),
                    project: this.$t('尚未开启此项目权限管理功能'),
                    pipeline_group: this.$t('尚未开启此流水线组权限管理功能')
                }
                return titleMap[this.resourceType]
            }
        }
    }
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
</style>
