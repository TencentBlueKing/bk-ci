export default {
    data () {
        return {
            // 缓存的告警详情
            defectCache: {},
            // 缓存的告警详情key
            defectCacheKeyList: [],
            // 预加载定时器
            intervalId: null,
            // 预加载定时器列表
            intervalIdList: []
        }
    },
    watch: {
        intervalId (newVal, oldValue) {
            clearInterval(oldValue)
        }
    },
    beforeDestroy () {
        this.clear()
    },
    methods: {
        initCacheConfig (userConfig) {
            const defaultConfig = {
                // 缓存数据的唯一标识
                cacheKey: 'entityId',
                // 缓存数据所在列表位置
                index: 0,
                // 缓存数据长度
                length: 0,
                // 请求后台定时器间隔（ms）
                interval: 1000,
                // 是否优先加载下一个
                forward: true,
                // 强制每次都更新
                forceUpdate: false
            }
            return Object.assign(defaultConfig, userConfig)
        },
        preloadCache (list = [], userConfig = {}) {
            const { cacheKey, index, length, interval, forward, forceUpdate } = this.initCacheConfig(userConfig)
            const vm = this
            let curForward = forward
            let count = 0
            let step = 0
            let currentIndex = index
            if (length) {
                this.intervalId = setInterval(handleCurrentCache, interval)
                this.intervalIdList.push(this.intervalId)
            }
            function handleCurrentCache () {
                if (count < length && count < list.length) {
                    if (curForward) {
                        curForward = !curForward
                        currentIndex = index + step
                        const defect = list[currentIndex]
                        if (currentIndex < list.length) {
                            step++
                            count++
                            if (vm.defectCache[defect[cacheKey]] && !forceUpdate) {
                                handleCurrentCache()
                            } else {
                                const params = { entityId: list[currentIndex].entityId, defectId: list[currentIndex].defectId }
                                vm.fetchAndCache(params, length)
                            }
                        } else {
                            handleCurrentCache()
                        }
                    } else {
                        curForward = !curForward
                        currentIndex = index - step
                        const defect = list[currentIndex]
                        if (currentIndex < 0) {
                            handleCurrentCache()
                        } else {
                            count++
                            if (!vm.defectCache[defect[cacheKey]] || forceUpdate) {
                                const params = { entityId: list[currentIndex].entityId, defectId: list[currentIndex].defectId }
                                vm.fetchAndCache(params, length)
                            } else {
                                handleCurrentCache()
                            }
                        }
                    }
                } else {
                    vm.clearAllInterval()
                }
            }
        },
        async fetchAndCache (params, length) {
            await this.fetchLintDetail('', params)
            
            // 因为按文件和按告警是两个表格，所以缓存两倍数量
            if (this.defectCacheKeyList.length > 2 * length) {
                const firstCacheKey = this.defectCacheKeyList.shift()
                delete this.defectCache[firstCacheKey]
            }
        },
        updateCache (newKey, newValue) {
            if (!this.defectCacheKeyList.includes(newKey)) this.defectCacheKeyList.push(newKey)
            this.defectCache[newKey] = newValue
        },
        clearAllInterval () {
            this.intervalIdList.map(id => {
                clearInterval(id)
            })
            this.intervalId = null
        },
        clear () {
            this.clearAllInterval()
            this.defectCache = {}
        }
    }
}
