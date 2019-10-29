import Vue from 'vue'
import {
    TEST_ACTION
} from './constants'
const mutations = {
    [TEST_ACTION]: (state, { turbo }) => {
        Vue.set(state, 'turbo', turbo)
    },
    resetRegister: (state) => {
        state.register = {
            taskName: '',
            taskId: '',
            banDistcc: true,
            ccacheEnabled: false
        }
        state.processHead = {
            title: '任务加速',
            current: 0,
            list: ['选择加速方案', '填写配置信息 ', '注册完成'],
            process: 'scheme'
        }
    },
    modifyProcessHead: (state, { process, current }) => {
        Object.assign(state.processHead, { process, current })
    },
    setRegister: (state, register) => {
        Object.assign(state.register, register)
    },
    setDialogOpt: (state, dialogOpt) => {
        Object.assign(state.dialogOpt, dialogOpt)
    },
    setInstall: (state, param) => {
        state.softwareInstallList.push(param)
    },
    setInstallStatus: (state, { param, installIndex }) => {
        Object.assign(state.softwareInstallList[installIndex], param)
    },
    setInstallTimer: (state, { installIndex, timer }) => {
        state.softwareInstallList[installIndex].timer = timer
    },
    clearInstallTimer: (state, { installIndex }) => {
        state.softwareInstallList[installIndex].timer && clearInterval(state.softwareInstallList[installIndex].timer)
    }
}

export default mutations
