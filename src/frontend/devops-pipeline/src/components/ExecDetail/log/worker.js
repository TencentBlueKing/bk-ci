const allListData = {}
let curListData = []
const allTagList = {}
const currentSearch = {
    index: 0,
    val: ''
}
let curId
let searchRes
let dataPort

onmessage = function (e) {
    const data = e.data
    const type = data.type
    curId = data.id
    curListData = allListData[curId]

    switch (type) {
        case 'initStatus':
            const pluginList = data.pluginList || []
            pluginList.forEach((curId) => {
                allListData[curId] = []
                allTagList[curId] = []
            })
            break
        case 'initAssistWorker':
            initAssistWorker(data)
            break
        case 'addListData':
            dataPort.postMessage({ type: 'addListData', list: data.list, mainWidth: data.mainWidth, curId })
            break
        case 'wheelGetData':
            getListData(data)
            break
        case 'foldListData':
            foldListData(data)
            postMessage({ type: 'completeFold', number: curListData.length, id: curId })
            handleSearch(currentSearch.val)
            const noScroll = typeof data.index === 'undefined'
            postMessage({
                type: 'completeSearch',
                num: searchRes.length,
                curSearchRes: getSearchRes(data.index || currentSearch.index),
                noScroll
            })
            break
        case 'search':
            handleSearch(data.val)
            postMessage({ type: 'completeSearch', num: searchRes.length, curSearchRes: getSearchRes(0) })
            currentSearch.index = 0
            currentSearch.val = data.val
            break
        case 'getSearchRes':
            postMessage({ type: 'completeGetSearchRes', searchRes: getSearchRes(data.index) })
            break
        case 'changeSearchIndex':
            currentSearch.index = data.index
            break
        case 'resetData':
            const resetList = [
                { data: allListData, default: [] },
                { data: allTagList, default: [] }
            ]
            resetData(resetList)
            dataPort.postMessage({ type: 'resetData' })
            break
    }
}

function initAssistWorker (data) {
    dataPort = data.dataPort
    dataPort.onmessage = (e) => {
        const data = e.data
        const type = data.type

        switch (type) {
            case 'complateHandleData':
                const tempId = data.curId
                data.list.forEach((newItem) => {
                    if (newItem.message.startsWith('##[group]')) {
                        newItem.message = newItem.message.replace('##[group]', '')
                        allTagList[tempId].push(newItem)
                    }
    
                    if (newItem.message.startsWith('##[endgroup]') && allTagList[tempId].length) {
                        newItem.message = newItem.message.replace('##[endgroup]', '')
                        const linkItem = allTagList[tempId].pop()
                        linkItem.endIndex = newItem.realIndex
                        linkItem.children = []
                    }
                    allListData[tempId].push(newItem)
                })
                postMessage({ type: 'completeAdd', number: allListData[tempId].length, id: tempId })
                break
        }
    }
}

function resetData (resetList) {
    resetList.forEach((reset) => {
        const data = reset.data
        const keys = Object.keys(data)
        keys.forEach((key) => {
            data[key] = reset.default
        })
    })
}

function handleSearch (val) {
    searchRes = []
    if (val !== '') {
        const keys = Object.keys(allListData) || []
        val = val.replace(/\*|\.|\?|\+|\$|\^|\[|\]|\(|\)|\{|\}|\||\\|\//g, (str) => `\\${str}`)
        const valReg = new RegExp(val, 'i')
        keys.forEach((key) => {
            const curList = allListData[key] || []
            curList.forEach(({ message, realIndex, children }, index) => {
                const searchData = {
                    index,
                    realIndex,
                    refId: key
                }
                if (valReg.test(message)) searchRes.push(searchData)
    
                if (children && children.length > 0) {
                    children.forEach(({ message, realIndex: searchRealIndex }) => {
                        if (valReg.test(message)) {
                            const foldSearchData = {
                                index,
                                startIndex: realIndex,
                                realIndex: searchRealIndex,
                                refId: key,
                                isInFold: true
                            }
                            searchRes.push(foldSearchData)
                        }
                    })
                }
            })
        })
    }
}

// 分页获取搜索结果
function getSearchRes (index) {
    let curSearchRes = []
    const startIndex = index - 500
    const endIndex = index + 500
    if (searchRes.length <= 1500) {
        curSearchRes = searchRes
    } else {
        curSearchRes = [...searchRes.slice(index, endIndex), ...searchRes.slice(startIndex, index)]
        if (startIndex < 0) curSearchRes = [...searchRes.slice(index, endIndex), ...searchRes.slice(startIndex), ...searchRes.slice(0, index)]
        if (endIndex > searchRes.length) curSearchRes = [...searchRes.slice(index), ...searchRes.slice(0, endIndex - searchRes.length), ...searchRes.slice(startIndex, index)]
    }
    return curSearchRes
}

function foldListData ({ startIndex }) {
    const realIndex = curListData.findIndex(x => x.realIndex === startIndex)
    const currentItem = curListData[realIndex]
    if (!currentItem || !currentItem.children) return

    if (!currentItem.children.length) {
        let totalNum = currentItem.endIndex - startIndex
        while (totalNum > 0) {
            let currentNum
            if (totalNum > 10000) {
                currentNum = 10000
                totalNum -= 10000
            } else {
                currentNum = totalNum
                totalNum = 0
            }
            const subList = curListData.splice(realIndex + 1, currentNum)
            currentItem.children = currentItem.children.concat(subList)
        }
    } else {
        for (let index = 0; index < currentItem.children.length;) {
            const someList = currentItem.children.slice(index, index + 10000)
            curListData.splice(realIndex + 1 + index, 0, ...someList)
            index += 10000
        }
        currentItem.children = []
    }
}

function getListData ({ totalScrollHeight, itemHeight, itemNumber, canvasHeight, minMapTop, totalHeight, mapHeight, type }) {
    const realHeight = minMapTop / ((mapHeight - canvasHeight / 8) || 1) * (totalHeight - canvasHeight)
    let startIndex = Math.floor(realHeight / itemHeight)
    const endIndex = startIndex + itemNumber
    startIndex = startIndex > 0 ? startIndex - 1 : 0

    const listData = []
    const indexList = []
    const nums = Math.floor(startIndex * itemHeight / 500000)
    for (let i = startIndex; i <= endIndex; i++) {
        const top = i * itemHeight - nums * 500000
        const currentItem = curListData[i]
        if (typeof currentItem === 'undefined') continue
        indexList.push({
            top,
            value: currentItem.showIndex,
            isNewLine: currentItem.isNewLine,
            index: currentItem.realIndex,
            listIndex: i,
            isFold: currentItem.endIndex !== undefined,
            hasFolded: (currentItem.children || []).length > 0
        })
        listData.push({
            top,
            isNewLine: currentItem.isNewLine,
            value: currentItem.message,
            color: currentItem.color,
            index: currentItem.realIndex,
            showIndex: currentItem.showIndex,
            fontWeight: currentItem.fontWeight,
            isFold: currentItem.endIndex !== undefined,
            timestamp: currentItem.timestamp
        })
    }

    totalScrollHeight = totalScrollHeight - nums * 500000

    let minMapStartIndex = startIndex - Math.floor(itemNumber * 8 * minMapTop / canvasHeight)
    if (minMapStartIndex < 0) minMapStartIndex = 0
    const minMapEndIndex = minMapStartIndex + itemNumber * 8
    const minMapList = []
    for (let i = minMapStartIndex; i <= minMapEndIndex; i++) {
        const currentItem = curListData[i]
        if (typeof currentItem === 'undefined') continue
        minMapList.push(currentItem)
    }

    postMessage({ type, indexList, listData, totalScrollHeight, minMapList, id: curId })
}
