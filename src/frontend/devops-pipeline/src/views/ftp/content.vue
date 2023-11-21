<template>
    <component
        :is="bkComponent"
        class="biz-content"
        ref="pipeline"
        v-bind="$attrs"
        v-on="$listeners"
    />
</template>

<script>
    import { mapState } from 'vuex'
    import RemoteAtom from '../../components/AtomPropertyPanel/RemoteAtom.vue'
    import PipelineEdit from '../subpages/edit.vue'
    import PipelineExecDetail from '../subpages/ExecDetail.vue'
    import PipelinesHistory from '../subpages/history.vue'

    const COMPONENTS = {
        ftpPipelinesEdit: PipelineEdit,
        ftpPipelinesDetail: PipelineExecDetail,
        ftpPipelinesHistory: PipelinesHistory
    }

    // 从外层iframe透传过来的数据
    let outerInfo = {}

    // TOFIX: 兼容现有FIT插件，在页面内修改与iframe通信内容。插件改造完毕后移除
    RemoteAtom.computed = {
        ...RemoteAtom.computed,
        ...mapState('atom', [
            'execDetail',
            'pipeline'
        ])
    }
    RemoteAtom.methods.onLoad = async function () {
        const containerInfo = this.container
        const currentUserInfo = this.$userInfo || {}
        const atomDisabled = false
        const envConf = await this.getEnvConf()
        this.loading = false
        const iframe = document.getElementById('atom-iframe').contentWindow
        const {
            projectId,
            pipelineId
        } = this.$route.params

        let buildId

        if (this.$route.name === 'ftpPipelinesDetail') {
            buildId = this.$route.params.buildNo
        }

        const pipelineInfo = buildId ? this.execDetail.model : this.pipeline
        iframe.postMessage({
            atomPropsValue: this.element.data.input,
            atomPropsModel: this.atomPropsModel.input,
            atomHashId: this.element.id,
            taskId: this.element.id,
            containerInfo,
            stageInfo: this.stage,
            projectId,
            pipelineId,
            buildId,
            templateId: '',
            currentUserInfo,
            envConf,
            elementInfo: this.element,
            pipelineInfo,
            pipelineName: this.pipeline?.name || '',
            atomDisabled,
            hostInfo: {
                ...this.$route.params
            },
            ...outerInfo
        }, '*')

        window.parent.postMessage({
            action: 'atomIframeOnload'
        }, '*')
    }

    window.addEventListener('message', ({ data }) => {
        if (data.action === 'ftpOuterInfo') {
            outerInfo = { ...data.params }
        }
    })

    export default {
        name: 'ftpPipelineContent',
        computed: {
            bkComponent () {
                return COMPONENTS[this.$route.name]
            }
        }
    }
</script>
