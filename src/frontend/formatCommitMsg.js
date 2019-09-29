#! /usr/bin/env node

const fs = require('fs')
const path = require('path')
const { exec } = require('child_process')

const gitPath = path.join(__dirname, '../..', process.env.HUSKY_GIT_PARAMS)
const commitMsg = fs.readFileSync(gitPath, 'utf-8')
const typesEnum = [
    'story',
    'bug',
    'task'
]

const typeLabels = {
    'story': 'feat',
    'bug': 'fix',
    'task': 'task'
}

exec("git branch | grep '*' | sed 's/* //'", (error, stdout, stderr) => {
    if (error) {
        console.log(error)
        process.exit(0)
    }
    
    const branchName = stdout.trim()

    if (['dev', 'develop', 'test', 'master'].includes(branchName)) {
        process.exit(0)
    }
    const [type, id] = branchName.split('_')
    if (!id) {
        console.log('识别不到分支issue ID')
        process.exit(1)
    }
    if (!typesEnum.includes(type)) {
        console.log(`分支issue类型不对，请输入【${typesEnum.join(',')}】中的一种`)
        process.exit(1)
    }

    const sourceCodeKeyword = `【${typeLabels[type]}】`
    const pos = commitMsg.indexOf('# Please enter the commit message for your changes. Lines starting')

    const tempalte = `${sourceCodeKeyword}: ${commitMsg.slice(0, pos)}\n--${type}=${id}`

    fs.writeFileSync(gitPath, tempalte, 'utf-8')
    process.exit(0)
})
