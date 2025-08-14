import actions from './actions'
import getters from './getters'
import mutations from './mutations'

export default {
    namespaced: true,
    state: {
        instanceList: [], // 实例列表选中的实例数据
        initialInstanceList: [],
        useTemplateSettings: false,
        templateVersion: '',
        templateDetail: {},
        isInstanceReleasing: false, // 实例正在发布状态
        releaseBaseId: '', // 实例发布任务ID,
        showTaskDetail: false,
        instanceTaskDetail: {},
        templateRefType: 'ID',
        templateRef: ''
    },
    mutations,
    actions,
    getters
}
