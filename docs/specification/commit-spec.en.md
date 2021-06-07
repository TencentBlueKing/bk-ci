# Code Commit Specifications

## Code Review Suggestions

The BK-CI project involves a total of five languages, JavaScript, Golang, Kotlin, Java and Lua. By default, we development team use the following tools for code review. In order to improve the efficiency of merging, please review by yourself before committing.

- ESLint
- Gometalinter
- detekt

## Commit Format

Format of commit messages on the personal branch.

```
type:messsge issue
```

* type Information range
  * feature New feature
  * fix Bug fix
  * docs Document modification
  * style Code formatting, like missing semicolons; no changes in code
  * refactor Code refactoring
  * test Add missing tests, refactor tests; no changes in production code
  * chore Relevant code like build scripts, tasks, etc.
* message Description of this commit
* issue id of the issue this commit is linked to

## Suggestions on Merge Request/Pull Request

Developers may have some simple commit messages on their own forked branches. It is recommended to refine commit messages using git rebase before submitting merge requests. Please refer to the previous section for refining information. Please refer to the following procedure for relevant commands.

```shell
#Use the new branch for feature development
git checkout feature1-pick
#Multiple debugging and commits
git commit -m "xxx"
git commit -m "yyy"
git commit -m "zzz"
#If new third-party dependencies are imported, use dep to manage dependencies 
dep ensure -v -add github.com/org/project

#Rebase operation, merge multiple changes (3 times below) on the feature1-pick branch 
# Complete standard commit information
git rebase -i HEAD~3

#Push to the remote repository
git push origin feature1-pick:feature1-pick
#Submit PR/MR and wait for merging
#......................
#......................

#After the PR/MR is merged, synchronize the local master branch
git fetch upstream
git rebase upstream/master
```
