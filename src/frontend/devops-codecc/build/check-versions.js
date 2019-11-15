/**
 * @file 检测 node 和 npm 的版本
 * @author blueking
 */

import childProcess from 'child_process'

import chalk from 'chalk'
import semver from 'semver'
import shell from 'shelljs'

import pkg from '../package.json'

/**
 * 执行命令
 *
 * @param {string} cmd 命令语句
 *
 * @return {string} 命令执行结果
 */
const exec = cmd => childProcess.execSync(cmd).toString().trim()

const versionRequirements = [
    {
        name: 'node',
        currentVersion: semver.clean(process.version),
        versionRequirement: pkg.engines.node
    }
]

if (shell.which('npm')) {
    versionRequirements.push({
        name: 'npm',
        currentVersion: exec('npm --version'),
        versionRequirement: pkg.engines.npm
    })
}

export default function () {
    const warnings = []
    for (let i = 0; i < versionRequirements.length; i++) {
        const mod = versionRequirements[i]
        if (!semver.satisfies(mod.currentVersion, mod.versionRequirement)) {
            warnings.push(mod.name
                + ': '
                + chalk.red(mod.currentVersion)
                + ' should be '
                + chalk.green(mod.versionRequirement)
            )
        }
    }

    if (warnings.length) {
        console.log('')
        console.log(chalk.yellow('To use this template, you must update following to modules:'))
        console.log()
        for (let i = 0; i < warnings.length; i++) {
            console.log('  ' + warnings[i])
        }
        console.log()
        process.exit(1)
    }
}
