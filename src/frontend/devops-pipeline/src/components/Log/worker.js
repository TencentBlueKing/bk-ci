import ansiParse from './assets/ansiParse'

const colorList = [
    { key: '##[command]', color: 'rgba(0,112,187,1)' },
    { key: '##[info]', color: 'rgba(31,174,62,1)' },
    { key: '##[warning]', color: 'rgba(187,187,35,1)' },
    { key: '##[error]', color: 'rgba(233,45,61,1)' },
    { key: '##[debug]', color: 'rgba(13,143,97,1)' }
]

const reg = (() => {
    const colors = colorList.map((color) => {
        let key = color.key
        key = key.replace(/\[|\]/gi, '\\$&')
        return `(${key})`
    })
    return new RegExp(`.*${colors.join('|')}.*`, 'gi')
})()

function handleColor (val) {
    const res = ansiParse(val)[0] || { message: '', hasHandle: false }
    const currentColor = colorList.find(color => String(val).includes(color.key))
    if (currentColor) {
        res.color = currentColor.color
        res.fontWeight = 600
    }
    res.message = String(res.message).replace(reg, '')
    res.hasHandle = true
    return res
}

let allListData = []
let offscreenCanvas
let canvasContext

const tagList = []
const foldList = []

onmessage = function (e) {
    const data = e.data
    const type = data.type
    switch (type) {
        case 'initLog':
            addListData(data)
            const foldArr = data.foldIndexs || []
            const arr = foldArr.sort((a, b) => +a - +b)
            arr.forEach(x => foldListData({ startIndex: +x }))
            postMessage({ type: 'completeInit', number: allListData.length })
            break
        case 'addListData':
            addListData(data)
            break
        case 'initLink':
        case 'wheelGetData':
            getListData(data)
            break
        case 'foldListData':
            foldListData(data)
            postMessage({ type: 'completeFold', number: allListData.length })
            break
    }
}

function foldListData ({ startIndex }) {
    const currentItem = allListData[startIndex]
    let changeNum = 0
    if (!currentItem.tagData.list.length) {
        const subList = allListData.splice(startIndex + 1, currentItem.tagData.endIndex - startIndex)
        currentItem.tagData.list = subList
        changeNum = currentItem.tagData.endIndex - startIndex
    } else {
        for (let index = 0; index < currentItem.tagData.list.length;) {
            const someList = currentItem.tagData.list.slice(index, index + 10000)
            allListData.splice(startIndex + 1 + index, 0, ...someList)
            index = index + 10000
        }
        changeNum = -currentItem.tagData.list.length
        currentItem.tagData.list = []
    }
    updateFoldList(startIndex, changeNum)
}

function updateFoldList (startIndex, changeNum) {
    const needChangeList = foldList.filter(x => x.index > startIndex) || []
    needChangeList.forEach((item) => {
        const data = item.data || {}
        item.index -= changeNum
        data.tagData.endIndex -= changeNum
        data.tagData.startIndex -= changeNum
    })
}

function addListData ({ list }) {
    list.forEach((item, index) => {
        const message = item.message
        const currentIndex = allListData.length + index
        if (message.includes('##[group]')) {
            tagList.push({ data: item, index: currentIndex })
        }

        if (message.includes('##[endgroup]') && tagList.length) {
            item.message = item.message.replace('##[endgroup]', '')
            const { data: linkItem, index: startIndex } = tagList.pop()
            linkItem.tagData = {
                endIndex: currentIndex,
                startIndex,
                list: []
            }
            foldList.push({ data: linkItem, index: startIndex })
            linkItem.message = linkItem.message.replace('##[group]', '')
        }
    })
    allListData = allListData.concat(list)
}

function getListData ({ totalScrollHeight, itemHeight, itemNumber, canvasHeight, canvasWidth, minMapTop, totalHeight, mapHeight, isResize, type }) {
    if (!offscreenCanvas || isResize) {
        offscreenCanvas = new OffscreenCanvas(canvasWidth, canvasHeight)
        canvasContext = offscreenCanvas.getContext('2d')
        canvasContext.fillStyle = '#fff'
        canvasContext.font = `normal normal normal ${itemHeight / 8}px Consolas`
    }

    const realHeight = minMapTop / (mapHeight - canvasHeight / 8) * (totalHeight - canvasHeight)
    let startIndex = Math.floor(realHeight / itemHeight)
    const endIndex = startIndex + itemNumber
    startIndex = startIndex > 0 ? startIndex - 1 : 0

    const listData = []
    const indexList = []
    const nums = Math.floor(startIndex * itemHeight / 500000)
    for (let i = startIndex; i <= endIndex; i++) {
        const top = i * itemHeight - nums * 500000
        const currentItem = allListData[i]
        if (typeof currentItem === 'undefined') continue
        if (!currentItem.hasHandle) {
            const handleItem = handleColor(currentItem.message || '')
            Object.assign(currentItem, handleItem)
            allListData[i] = currentItem
        }
        indexList.push({ top, value: i + 1, tagData: currentItem.tagData })
        listData.push({ top, value: currentItem.message, color: currentItem.color, fontWeight: currentItem.fontWeight, tagData: currentItem.tagData, timestamp: currentItem.timestamp })
    }

    totalScrollHeight = totalScrollHeight - nums * 500000

    let minMapStartIndex = startIndex - Math.floor(itemNumber * 8 * minMapTop / canvasHeight)
    if (minMapStartIndex < 0) minMapStartIndex = 0
    const minMapEndIndex = minMapStartIndex + itemNumber * 8
    for (let i = minMapStartIndex; i <= minMapEndIndex; i++) {
        const currentItem = allListData[i]
        if (typeof currentItem === 'undefined') continue
        if (!currentItem.hasHandle) {
            const handleItem = handleColor(currentItem.message || '')
            Object.assign(currentItem, handleItem)
            allListData[i] = currentItem
        }
        const currentColor = currentItem.color || 'rgba(255,255,255,1)'
        if (currentItem.color) canvasContext.font = `normal normal bold ${itemHeight / 8}px Consolas`
        else canvasContext.font = `normal normal normal ${itemHeight / 8}px Consolas`
        canvasContext.fillStyle = currentColor.replace(/rgba\((.+),(.+),(.+),(.)\)/, (rgba, r, g, b, a) => `rgba(${b},${g},${r},${a})`)
        canvasContext.fillText(currentItem.message, 0, ((i - minMapStartIndex + 1) * itemHeight / 8))
    }
    const offscreenBitMap = offscreenCanvas.transferToImageBitmap()

    postMessage({ type, indexList, listData, totalScrollHeight, offscreenBitMap })
}
