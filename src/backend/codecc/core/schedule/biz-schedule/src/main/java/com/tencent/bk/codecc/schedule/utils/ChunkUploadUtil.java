package com.tencent.bk.codecc.schedule.utils;


import com.tencent.bk.codecc.schedule.constant.ScheduleConstants;
import com.tencent.bk.codecc.schedule.constant.DispatchMessageCode;
import com.tencent.devops.common.api.exception.CodeCCException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 分片上传工具类
 *
 * @author zuihou
 * @date 2019-06-14 11:50
 */
@Service
@Scope("prototype")
@Slf4j
public class ChunkUploadUtil
{
    /**
     * 分片验证
     * 验证对应分片文件是否存在，大小是否吻合
     *
     * @param file 分片文件的路径
     * @param size 分片文件的大小
     * @return
     */
    public static boolean chunkCheck(String file, Long size)
    {
        //检查目标分片是否存在且完整
        File target = new File(file);
        return target.isFile() && size == target.length();
    }

    /**
     * 创建临时分片文件及文件夹
     *
     * @param uploadFolder 上传文件的目录
     * @param fileBaseName 上传文件名（不包含目录）
     * @param chunk        分片号
     * @param buildId
     * @return
     */
    public static File createChunkFileFolder(String uploadFolder, String fileBaseName, Integer chunk, String buildId)
    {
        //分片文件的名称
        String chunkFileName = String.format("%s%s%d", fileBaseName, ScheduleConstants.CHUNK_FILE_SUFFIX, chunk);

        String fileFolder = md5(fileBaseName + buildId);
        log.info("fileFolder={}", fileFolder);

        //文件上传路径更新为指定文件信息签名后的临时文件夹，用于后期合并
        String tmpPath = uploadFolder + File.separator + fileFolder;
        createFileFolder(tmpPath);

        return new File(tmpPath, chunkFileName);
    }

    /**
     * 创建存放上传的文件的文件夹
     *
     * @param path 文件夹路径
     * @return
     */
    public static boolean createFileFolder(String path)
    {
        //创建存放分片文件的临时文件夹
        File tmpFile = new File(path);
        if (!tmpFile.exists())
        {
            tmpFile.mkdirs();
        }
        return true;
    }


    /**
     * MD5签名
     *
     * @param content 要签名的内容
     * @return
     */
    public static String md5(String content)
    {
        StringBuffer sb = new StringBuffer();
        try
        {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(content.getBytes("UTF-8"));
            byte[] tmpFolder = md5.digest();

            for (int i = 0; i < tmpFolder.length; i++)
            {
                sb.append(Integer.toString((tmpFolder[i] & 0xff) + 0x100, 16).substring(1));
            }

            return sb.toString();
        }
        catch (NoSuchAlgorithmException ex)
        {
            log.error("无法生成文件的MD5签名", ex);
            throw new CodeCCException(DispatchMessageCode.UPLOAD_FILE_ERR);
        }
        catch (UnsupportedEncodingException ex)
        {
            log.error("无法生成文件的MD5签名", ex);
            throw new CodeCCException(DispatchMessageCode.UPLOAD_FILE_ERR);
        }
    }

}
