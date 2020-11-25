import actions from './actions'
import mutations from './mutations'

const store = {
    namespaced: true,
    actions,
    mutations,
    state: {
        turbo: 'String turbo',
        processHead: {
            title: '任务加速',
            current: 0,
            list: ['选择加速方案', '填写配置信息 ', '注册完成'],
            process: 'scheme' // scheme buildPublic buildThird buildInstall registSuccess
        },
        register: {
            taskName: '',
            taskId: '',
            banDistcc: 'false',
            ccacheEnabled: 'true',
            machineType: '1'
            // incredibuild: false,
            // crcache: false,
        },
        softwareInstallList: [], // 软件安装列表
        dialogOpt: {
            isShow: false,
            width: '500px',
            padding: 0,
            hasClose: true,
            imgSrc: '',
            imgType: 'distccImg',
            contentType: 'picture', // picture, progress
            progressWidth: '20%',
            restDate: '00:00:00',
            errorMessage: 'Message of failed because of Network',
            currentState: 'error' // error, primary, success
        }
    },
    getters: {
        getRegister: state => state.register,
        getProcessHead: state => state.processHead,
        getDialogOpt: state => state.dialogOpt,
        getSoftwareInstallList: state => state.softwareInstallList
    }
}

export default store
