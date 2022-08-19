---
name: Bug Report
about: Report a bug encountered while operating bk-ci
title: ''
labels: kind/bug
assignees: ''

---

name: Bug Report
description: Report a bug encountered while operating bk-ci
labels: kind/bug
body:
  - type: textarea
    id: problem
    attributes:
      label: What happened?
      description: |
        Please provide as much info as possible. Not doing so may result in your bug not being addressed in a timely manner.
        If this matter is security related, please disclose it privately via https://github.com/Tencent/bk-ci/blob/master/SECURITY.md
    validations:
      required: true

  - type: textarea
    id: expected
    attributes:
      label: What did you expect to happen?
    validations:
      required: true

  - type: textarea
    id: repro
    attributes:
      label: How can we reproduce it (as minimally and precisely as possible)?
    validations:
      required: true

  - type: textarea
    id: additional
    attributes:
      label: Anything else we need to know?

  - type: textarea
    id: bkciVersion
    attributes:
      label: bk-ci version
      value: |
        <details>


        </details>
    validations:
      required: true

  - type: textarea
    id: cloudProvider
    attributes:
      label: Cloud provider
      value: |
        <details>

        </details>
    validations:
      required: true

  - type: textarea
    id: osVersion
    attributes:
      label: OS version
      value: |
        <details>

        ```console
        # On Linux:
        $ cat /etc/os-release
        # paste output here
        $ uname -a
        # paste output here

        # On Windows:
        C:\> wmic os get Caption, Version, BuildNumber, OSArchitecture
        # paste output here
        ```

        </details>

  - type: textarea
    id: installer
    attributes:
      label: Install tools
      value: |
        <details>

        </details>
