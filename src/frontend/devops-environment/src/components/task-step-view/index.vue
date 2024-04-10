<template>
    <detail-layout
        class="detail-layout-wrapper"
        mode="see"
    >
        <detail-item :label="$t('environment.步骤类型：')">
            {{ stepTypeText }}
        </detail-item>
        <detail-item :label="$t('environment.步骤名称：')">
            {{ data.name }}
        </detail-item>
        <component
            :is="stepCom"
            ref="stepCom"
            :data="data">
            <slot />
        </component>
    </detail-layout>
</template>
  <script>
    import DetailLayout from './components/detail-layout/'
    import DetailItem from './components/detail-layout/item'
  
    import StepDistroFile from './distro-file'
    import StepExecScript from './exec-script'
  
    export default {
        components: {
            StepDistroFile,
            StepExecScript,
            DetailLayout,
            DetailItem
        },
        props: {
            data: {
                type: Object,
                default: () => ({})
            }
        },
        computed: {
            stepTypeText () {
                const typeMap = {
                    1: this.$t('environment.执行脚本'),
                    2: this.$t('environment.分发文件')
                }
                return typeMap[this.data.type] || ''
            },
            stepCom () {
                const taskStepMap = {
                    1: StepExecScript,
                    2: StepDistroFile
                }
                if (!Object.prototype.hasOwnProperty.call(taskStepMap, this.data.type)) {
                    return 'div'
                }
                return taskStepMap[this.data.type]
            }
        }
    }
  </script>
  <style lang="scss" scoped>
    .detail-layout-wrapper {
        .detail-item {
            margin-bottom: 0;
        }
    }
  </style>
