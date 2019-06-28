import { GetterTree } from 'vuex'
import { isShallowEqual } from '../utils/util'
import { EMPTY_PROJECT } from './constants'

const getters: GetterTree<RootState, any> = {
    getCollectServices: (state: RootState) => {
        return state.services.reduce((collects: any, service: any) => {
            Array.isArray(service.children) && service.children.map((child: any) => {
                if (child.collected) {
                    collects.push(child)
                }
            })
            return collects
        }, [])
    },

    isEmptyProject: (state: RootState) => (project: Project): boolean => {
        return isShallowEqual(project, EMPTY_PROJECT)
    },

    onlineProjectList: (state: RootState) => {
        return state.projectList.filter((project: ObjectMap) => project.approval_status === 2 && !project.is_offlined)
    },

    approvalingProjectList: (state: RootState) => {
        return state.projectList.filter((project: ObjectMap) => project.approval_status === 1)
    }
    
}

export default getters
