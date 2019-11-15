#!/usr/bin/env bash

use db_task;
db.t_base_data.insert({"_id":8});
use dba2;
db.a.insert({"_id":8});