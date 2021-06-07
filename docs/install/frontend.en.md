# bk-ci Frontend Deployment Document

Under BlueKing ci frontend (under the frontend directory), common-lib/ and svg-sprites/ contain static resources of project dependencies. The rest of directories contain SPA projects built with Vue, among which devop-nav is the main entry point. Other subroutines are integrated using the iframe element or the UMD pattern.

## System Requirement

Node.js 8.0.0+

## Installation Guide

1.	Package and deploy the corresponding Vue project, and then navigate to the src/frontend directory.

```
# First, install yarn globally
npm install -g yarn
# Then run install
yarn install
# Next, install the dependencies of each subroutine
yarn start
# Finally, run the packaging command
yarn public
```

After running commands above, a folder named frontend will be created in the src/frontend directory, which contains resource files created from BK-CI frontend packaging.

Each frontend service module and its corresponding folder are as follows.

|   Folder Name   |   Module Name     |
| ------------ | ---------------- |
|  console |   devops-nav
|  pipeline |   devops-pipeline
|  codelib |   devops-codelib
|  environment |   devops-environment
|  store |   devops-atomstore
|  ticket |   devops-ticket
|   common-lib   |  common-lib |
|   svg-sprites   |  svg-sprites |

Finally, copy the frontend folder to the `__INSTALL_PATH__/__MODULE__/` directory.

The final structure of the frontend deployment directory is as follows.

```
__INSTALL_PATH__/__MODULE__/frontend/codelib
__INSTALL_PATH__/__MODULE__/frontend/commom-lib
__INSTALL_PATH__/__MODULE__/frontend/console
__INSTALL_PATH__/__MODULE__/frontend/environment
__INSTALL_PATH__/__MODULE__/frontend/pipeline
__INSTALL_PATH__/__MODULE__/frontend/store
__INSTALL_PATH__/__MODULE__/frontend/svg-sprites
__INSTALL_PATH__/__MODULE__/frontend/ticket
```
