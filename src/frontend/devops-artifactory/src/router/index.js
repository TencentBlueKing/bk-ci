// 版本仓库入口页
const artifactoryEntry = () => import(/* webpackChunkName: 'artifactoryEntry' */'../views/index.vue')

// 版本仓库主页
const artifactoryHome = () => import(/* webpackChunkName: 'artifactoryHome' */'../views/recycle.vue')

// 版本仓库主页
const artifactoryList = () => import(/* webpackChunkName: 'artifactoryList' */'../views/repo.vue')

// 镜像相关页面
const Depot = () => import(/* webpackChunkName: 'depot' */'../views/depot')
const ImageLibrary = () => import(/* webpackChunkName: 'imageLibrary' */'../views/depot/image-library')
const ImageDetail = () => import(/* webpackChunkName: 'imageDetail' */'../views/depot/image-detail')
const ProjectImage = () => import(/* webpackChunkName: 'projectImage' */'../views/depot/project-image')

// 版本仓库最近使用
const artifactoryRecent = () => import(/* webpackChunkName: 'artifactoryRecent' */'../views/recent.vue')

// 版本仓库回收站
const artifactoryRecycle = () => import(/* webpackChunkName: 'artifactoryRecycle' */'../views/recycle.vue')

const meta = {
    title: VERSION_TYPE === 'tencent' ? '版本仓库' : '制品库',
    header: 'artifactory',
    logo: 'artifactory',
    to: 'artifactory'
}

const routes = [
    {
        path: 'artifactory/:projectId?',
        component: artifactoryEntry,
        children: [
            {
                path: '',
                name: 'artifactory',
                meta,
                component: artifactoryHome
            },
            {
                path: 'depot/',
                name: 'depotMain',
                component: Depot,
                children: [
                    {
                        path: 'image-library',
                        component: ImageLibrary,
                        name: 'imageLibrary',
                        alias: ''
                    },
                    {
                        path: 'image-detail',
                        component: ImageDetail,
                        name: 'imageDetail',
                        alias: ''
                    },
                    {
                        path: 'project-image',
                        name: 'projectImage',
                        component: ProjectImage
                    }
                ]
            },
            {
                path: ':type',
                name: 'artifactoryList',
                meta,
                component: artifactoryList
            },
            {
                path: 'recent',
                name: 'artifactoryRecent',
                meta,
                component: artifactoryRecent
            },
            {
                path: 'recycle',
                name: 'artifactoryRecycle',
                meta,
                component: artifactoryRecycle
            }
        ]
    }
]

export default routes
