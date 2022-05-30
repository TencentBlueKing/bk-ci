/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.apiquery.hyperlink;

import com.alibaba.excel.metadata.CellData;
import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.write.handler.AbstractCellWriteHandler;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteTableHolder;
import com.tencent.bk.codecc.apiquery.utils.HttpUrlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Hyperlink;

import java.util.List;


/**
 * 任务超链接
 *
 * @version V1.0
 * @date 2021/1/7
 */
@Slf4j
public class TaskHyperlink extends AbstractCellWriteHandler {

    @Override
    public void afterCellDispose(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder,
            List<CellData> cellDataList, Cell cell, Head head, Integer relativeRowIndex, Boolean isHead) {

        String codeccServerApi = HttpUrlUtils.getCodeccServerApi();

        // 在第二行开始之后的每一行进行操作
        if (cell.getRowIndex() >= 1) {
            String hyperLink;
            if (cell.getColumnIndex() == 4) {
                Cell projectIdCell = cell.getRow().getCell(3);
                if (projectIdCell == null) {
                    return;
                }
                String projectId = projectIdCell.toString();

                Cell taskIdCell = cell.getRow().getCell(4);
                if (taskIdCell == null) {
                    return;
                }
                String taskIdCellStr = taskIdCell.toString();

                // 去除小数点
                String taskId = StringUtils.substringBefore(taskIdCellStr, ".");
                hyperLink = codeccServerApi + "/codecc/" + projectId + "/task/" + taskId + "/detail";
            } else {
                return;
            }

            CreationHelper helper = writeSheetHolder.getSheet().getWorkbook().getCreationHelper();
            Hyperlink hyperlink = helper.createHyperlink(HyperlinkType.URL);
            hyperlink.setAddress(hyperLink);
            cell.setHyperlink(hyperlink);
        }
    }

}
