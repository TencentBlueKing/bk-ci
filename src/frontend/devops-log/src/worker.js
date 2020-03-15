import ansiParse from './assets/ansiParse'

const colorList = [
    { key: '##[command]', color: 'rgba(146,166,202,1)' },
    { key: '##[info]', color: 'rgba(127,202,84,1)' },
    { key: '##[warning]', color: 'rgba(246,222,84,1)' },
    { key: '##[error]', color: 'rgba(247,49,49,1)' },
    { key: '##[debug]', color: 'rgba(99,176,106,1)' }
]

const reg = (() => {
    const colors = colorList.map((color) => {
        let key = color.key
        key = key.replace(/\[|\]/gi, '\\$&')
        return `(${key})`
    })
    return new RegExp(`${colors.join('|')}`, 'gi')
})()

function handleColor (val) {
    const parseRes = ansiParse(val) || [{ message: '' }]
    const res = { message: '' }
    parseRes.forEach((item) => {
        res.message += item.message
        if (!res.color && item.color) res.color = item.color
    })
    
    const currentColor = colorList.find(color => String(val).includes(color.key))
    if (currentColor) res.color = currentColor.color
    if (res.color) res.fontWeight = 600
    res.message = String(res.message).replace(reg, '')
    return res
}

const allListData = {}
let curListData = []
const allTagList = {}
let curTagList = []
const allMainWidth = {}
const allMainWordNum = {}
const allRepeatLineNum = {}
let curId

onmessage = function (e) {
    const data = e.data
    const type = data.type
    curId = data.id
    curListData = allListData[curId]
    curTagList = allTagList[curId]

    switch (type) {
        case 'initStatus':
            const pluginList = data.pluginList || []
            pluginList.forEach((curId) => {
                allListData[curId] = []
                allTagList[curId] = []
                allMainWidth[curId] = 0
                allMainWordNum[curId] = 0
                allRepeatLineNum[curId] = -1
            })
            break
        case 'initLog':
            allMainWidth[curId] = data.mainWidth - 90
            allMainWordNum[curId] = Math.floor(allMainWidth[curId] / 6.8)
            addListData(data)
            postMessage({ type: 'completeInit', number: curListData.length, id: curId })
            break
        case 'addListData':
            allMainWidth[curId] = data.mainWidth - 90
            allMainWordNum[curId] = Math.floor(allMainWidth[curId] / 6.8)
            addListData(data)
            postMessage({ type: 'completeAdd', number: curListData.length, id: curId })
            break
        case 'wheelGetData':
            getListData(data)
            break
        case 'foldListData':
            foldListData(data)
            postMessage({ type: 'completeFold', number: curListData.length, id: curId })
            break
        case 'search':
            handleSearch(data.val)
            break
        case 'getSearchRes':
            getSearchRes(data.index)
            break
        case 'resetData':
            allListData = {}
            allTagList = {}
            allMainWidth = {}
            allMainWordNum = {}
            allRepeatLineNum = {}
            break
    }
}

let searchRes
function handleSearch (val) {
    searchRes = []
    if (val !== '') {
        const keys = Object.keys(allListData) || []
        keys.forEach((key) => {
            const curList = allListData[key] || []
            curList.forEach(({ message, realIndex }) => {
                const searchData = {
                    index: realIndex,
                    refId: key
                }
                if (message.includes(val)) searchRes.push(searchData)
            })
        })
    }
    postMessage({ type: 'completeSearch', num: searchRes.length })
    getSearchRes(0)
}

// 分页获取搜索结果
function getSearchRes (index) {
    let curSearchRes = []
    let startIndex = index - 500
    let endIndex = index + 500
    if (searchRes.length <= 1500) {
        curSearchRes = searchRes
    } else {
        curSearchRes = [...searchRes.slice(index, endIndex), ...searchRes.slice(startIndex, index)]
        if (startIndex < 0) curSearchRes = [...searchRes.slice(index, endIndex), ...searchRes.slice(startIndex), ...searchRes.slice(0, index)]
        if (endIndex > searchRes.length) curSearchRes = [...searchRes.slice(index), ...searchRes.slice(0, endIndex - searchRes.length), ...searchRes.slice(startIndex, index)]
    }
    postMessage({ type: 'completeGetSearchRes', searchRes: curSearchRes })
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
            allRepeatLineNum[curId] -= subList.length
            currentItem.children = currentItem.children.concat(subList)
        }
    } else {
        for (let index = 0; index < currentItem.children.length;) {
            const someList = currentItem.children.slice(index, index + 10000)
            curListData.splice(realIndex + 1 + index, 0, ...someList)
            allRepeatLineNum[curId] += someList.length
            index += 10000
        }
        currentItem.children = []
    }
}

function addListData ({ list }) {
    list.forEach((item) => {
        const { message, color } = handleColor(item.message || '')
        const newItemArr = message.split(/\r\n|\n/)
        newItemArr.forEach((message) => {
            const splitTextArr = splitText(message)
            splitTextArr.forEach((message, i) => {
                const currentIndex = curListData.length
                const newItem = {
                    message,
                    color,
                    isNewLine: i > 0 ? (allRepeatLineNum[curId]++, true) : false,
                    showIndex: curListData.length - allRepeatLineNum[curId],
                    realIndex: currentIndex,
                    timestamp: item.timestamp
                }
                if (message.includes('##[group]')) {
                    newItem.message = newItem.message.replace('##[group]', '')
                    curTagList.push(newItem)
                }

                if (message.includes('##[endgroup]') && curTagList.length) {
                    newItem.message = newItem.message.replace('##[endgroup]', '')
                    const linkItem = curTagList.pop()
                    linkItem.endIndex = currentIndex
                    linkItem.children = []
                }

                curListData.push(newItem)
            })
        })
    })
}

function splitText (message) {
    let tempMes = ''
    const totalWidth = getTextWidth(message)
    const mesRes = []
    if (totalWidth < allMainWidth[curId]) {
        mesRes.push(message)
    } else {
        const regex = /<a[^>]+?href=["']?([^"']+)["']?[^>]*>([^<]+)<\/a>/gi
        const aList = []
        let tempA = null
        let currentIndex = 0

        while ((tempA = regex.exec(message)) != null) aList.push({
            content: tempA[0],
            href: tempA[1],
            text: tempA[2],
            startIndex: tempA.index
        })
        if (aList.length) message = message.replace(regex, '$2')

        while (message !== '') {
            [tempMes, message] = splitByChar(message)
            // a标签单独处理
            aList.forEach((x) => {
                if (x.startIndex <= currentIndex + tempMes.length && x.startIndex >= currentIndex) {
                    const curStartIndex = x.startIndex - currentIndex
                    const curLength = x.text.length
                    const diffDis = curStartIndex + curLength - tempMes.length
                    if (diffDis > 0) {
                        message = tempMes.slice(curStartIndex) + message
                        tempMes = tempMes.slice(0, curStartIndex)
                    } else {
                        tempMes = (tempMes.slice(0, curStartIndex) + x.content + tempMes.slice(curStartIndex + curLength))
                    }
                }
            })

            currentIndex += tempMes.length
            mesRes.push(tempMes)
        }
    }
    return mesRes
}

function splitByChar (message) {
    let tempMes = message.slice(0, allMainWordNum[curId])
    message = message.slice(allMainWordNum[curId])
    let tempWidth = getTextWidth(tempMes)
    while (tempWidth > allMainWidth[curId] || (allMainWidth[curId] - tempWidth > 15 && message !== '')) {
        if (tempWidth > allMainWidth[curId]) {
            message = tempMes.slice(-1) + message
            tempMes = tempMes.slice(0, -1)
            tempWidth = getTextWidth(tempMes)
        } else {
            tempMes += message.slice(0, 1)
            message = message.slice(1)
            tempWidth = getTextWidth(tempMes)
        }
    }
    return [tempMes, message]
}

const canvas = new OffscreenCanvas(100, 1)
const context = canvas.getContext("2d")
context.font = 'normal 12px Consolas, "Courier New", monospace'
function getTextWidth(text) {
    const metrics = context.measureText(text)
    return metrics.width
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
