<template>
    <div
        class="distro-file-view"
        :class="{ loading: isLoading }">
        <detail-item :label="$t('environment.超时时长：')">
            {{ stepInfo.timeout }} (s)
        </detail-item>
        <detail-item :label="$t('environment.上传限速：')">
            {{ stepInfo.sourceSpeedLimit ? `${stepInfo.sourceSpeedLimit} (MB/s)` : $t('environment.否')}}
        </detail-item>
        <detail-item :label="$t('environment.下载限速：')">
            {{ stepInfo.destinationSpeedLimit ? `${stepInfo.destinationSpeedLimit} (MB/s)` : $t('environment.否') }}
        </detail-item>
        <detail-item
            :label="$t('environment.文件来源：')"
            layout="vertical">
            <render-source-file
                :data="stepInfo.fileSourceList"
                :variable="variable" />
        </detail-item>
        <detail-item :label="$t('environment.目标路径：')">
            {{ stepInfo.fileDestination.path }}
        </detail-item>
        <detail-item :label="$t('environment.传输模式：')">
            {{ transferModeMap[stepInfo.transferMode] }}
        </detail-item>
        <detail-item :label="$t('environment.执行账号：')">
            {{ stepInfo.fileDestination.account.name || '--' }}
        </detail-item>
        <slot />
    </div>
</template>
  <script>
  
    import DetailItem from './components/detail-layout/item'
    import RenderSourceFile from './components/render-source-file'
  
    export default {
        name: '',
        components: {
            RenderSourceFile,
            DetailItem
        },
        props: {
            data: {
                type: Object,
                default: () => ({})
            },
            variable: {
                type: Array,
                default: () => []
            }
        },
        data () {
            return {
                stepInfo: {}
            }
        },
        computed: {
            transferModeMap () {
                return {
                    1: this.$t('environment.严谨模式'),
                    2: this.$t('environment.强制模式'),
                    3: this.$t('environment.保险模式')
                }
            }
        },
        created () {
            this.stepInfo = Object.freeze(this.data.fileStepInfo)
        }
    }
  </script>
  <style lang="scss" scoped>
    .distro-file-view {
      &.loading {
        height: calc(100vh - 100px);
      }
  
      .detail-item {
        margin-bottom: 0;
      }
    }
  </style>
