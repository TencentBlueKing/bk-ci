import actions from './actions'
import getters from './getters'
import mutations from './mutations'
import { CODE_MODE, UI_MODE } from '@/utils/pipelineConst'
import {
    OPERATE_TYPE,
    UPDATE_PUBLIC_VAR_MODE
} from './constants'

const modeList = [UI_MODE, CODE_MODE]
const initPipelineMode = localStorage.getItem(UPDATE_PUBLIC_VAR_MODE)
export default {
    namespaced: true,
    state: {
        publicVarMode: modeList.includes(initPipelineMode) ? initPipelineMode : UI_MODE,
        operateType: OPERATE_TYPE.CREATE,
        modeList: [...modeList],
        isModeSwitching: false,
        publicVarYaml: '',
        groupData: {
            groupName: '',
            desc: '',
            publicVars: []
        }
    },
    mutations,
    actions,
    getters
}
