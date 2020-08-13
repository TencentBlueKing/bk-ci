# Contributing to BK-CI

The BlueKing team upholds an open attitude and welcomes like-minded developers to contribute to the project. Before you start, please read the following instructions carefully.

## Code License

[MIT LICENSE](LICENSE.txt) is the open source license of BK-CI. Code contributed by anyone is protected by this license. Please make sure that you can accept the license before contributing your code.

## Design Document

Any functionality and feature should have its corresponding design document. Design documents need to be archived in the **docs/features** directory for the team to review and for subsequent developers to learn about the details of the features.

## Contribute Functionality and Feature

If you want to contribute functionalities and features to BK-CI project, please refer to the following steps:

* Contact the BlueKing team for relevant functional requirement.
* Once the team approves of the functionality, an issue is created to track the feature. The issue should at least contain the problem that the feature needs to address, use cases, relevant design, implementation details and problems that may arise.
* Submit detailed design document to the BlueKing team.
* The BlueKing team confirms requirement scheduling, the time to merge the functionality and the feature as well as the version.
* Complete coding, unit testing, use case testing and user documentation. Ensure consistent code style.
* Submit Pull Request/Merge Request which contains the documentation and the code.
* Functionality/Feature review. Merge after it passes.

> Note: To ensure code quality, for big functionalities and features, the BlueKing tends to submit multiple PRs/MRs progressively, so that relevant developers can review the details of the changes. It may take more time to review a one-off and large-scale commit.

## How to Get Started

If you want to contribute your code, it is recommended to refer to existing documentation about features and development environment setup.

## GIT Commit Specifications

Since different projects of different teams have different code commit comments, in order to standardize the commit message of different teams in the open source environment, different marks are used to differentiate committed changes.

```
git commit -m 'mark: comment of the commit issue #123'
```

For example:

```shell
git commit -m 'fix: the bug that the worker-agent.jar process has delayed exit on some third-party agents #29'
```

### Mark Description

| Mark     | Description                                   |
| -------- | -------------------------------------- |
| feature/feat  | Develop a new feature                             |
| bug/fix/bugfix   | Fix a bug                                |
| refactor/perf | Refactor the code/Optimize configurations & parameters/Optimize the logic and the functionality |
| test     | Add unit testing cases                   |
| docs     | Add documents                               |
| info     | Add comment information                         |
| format   | On the premise of not modifying business logic, only format the code  |
| merge    | Only merge and synchronize branches                       |
| depend   | Add, delete or modify the dependencies of the project                 |
| chore    | Relevant code like build scripts and tasks                 |
| del    | Destructive actions like deleting functionalities and APIs that are still in use               |

## Pull Request/Merge Request

If you are already dealing with an existing issue and have a reasonable solution, you are recommended to reply to the issue, so that the BlueKing team or other developers and users know that you are interested in the issue and have made positive progress, which prevents repetition and avoids waste of manpower. The BlueKing team upholds an open attitude and is willing to discussion solutions with you. We look forward to having you submit PR/MR.

Steps to Commit a Fix

* Fork the branch affected by the issue.
* Create your own branch for fixing.
* Fix the issue.
* Add new testing cases. If you try to fix a bug, make sure that the code cannot pass the testing cases when it is not fixed. The testing cases should cover as many scenarios as possible.
* Update the documentation (if necessary).
* Compile successfully and pass unit testing.
* Review. Merge after it passes.

For fixing issues, the BlueKing team hopes that one PR/MR can cover all the relevant content, including but not limited to the code, the documentation and the user guide.

Please refer to [BK-CI Review Process](./docs/specification/review.en.md) for relevant review process.

## Issues

The BlueKing team uses [issues](https://github.com/Tencent/bk-ci/issues) to track bugs, feature, etc.

When submitting a relevant bug, please search for existing or similar issues to ensure that there is no redundancy.

If you confirm that this is a new bug, please include the following information when submitting.

* Information about the operating system you use.
* Information about the current version you use, such as version, commitid.
* Log outputs of relevant modules when the problem occurs.
* Exact steps to reproduce the bug. For example, submitting relevant reproduction scripts/tools is more useful than long description.
