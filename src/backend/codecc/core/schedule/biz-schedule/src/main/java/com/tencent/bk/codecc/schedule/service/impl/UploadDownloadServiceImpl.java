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

package com.tencent.bk.codecc.schedule.service.impl;

import com.tencent.bk.codecc.schedule.constant.DispatchMessageCode;
import com.tencent.bk.codecc.schedule.constant.ScheduleConstants;
import com.tencent.bk.codecc.schedule.dao.mongorepository.FileIndexRepository;
import com.tencent.bk.codecc.schedule.dao.redis.FileInfoCache;
import com.tencent.bk.codecc.schedule.model.FileIndexEntity;
import com.tencent.bk.codecc.schedule.service.UploadDownloadService;
import com.tencent.bk.codecc.schedule.utils.ChunkUploadUtil;
import com.tencent.bk.codecc.schedule.utils.FileLock;
import com.tencent.bk.codecc.schedule.vo.DownloadVO;
import com.tencent.bk.codecc.schedule.vo.FileChunksMergeVO;
import com.tencent.bk.codecc.schedule.vo.FileIndexVO;
import com.tencent.bk.codecc.schedule.vo.FileInfoModel;
import com.tencent.bk.codecc.schedule.vo.GetFileSizeVO;
import com.tencent.bk.codecc.schedule.vo.UploadVO;
import com.tencent.devops.common.storage.StorageService;
import com.tencent.devops.common.storage.constant.StorageType;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.util.MD5Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

/**
 * 文件上传业务逻辑服务实现
 *
 * @version V1.0
 * @date 2019/10/10
 */
@Service
@Slf4j
public class UploadDownloadServiceImpl implements UploadDownloadService {
    @Autowired
    private FileInfoCache fileInfoCache;

    @Autowired
    private FileIndexRepository fileIndexRepository;

    @Autowired
    private StorageService storageService;

    @Value("${codecc.file.data.path:/data/bkce/codecc/nfs}")
    private String codeccFileDataPath = "";

    private static final Map<String, String> FOLDER_MAP = createMap();

    private static Map<String, String> createMap() {
        Map<String, String> folderMap = new HashMap<>();
        folderMap.put(ScheduleConstants.UploadType.SUCCESS_RESULT.name(),
                "/nfs1/result_upload;"
                        + "/nfs2/result_upload;"
                        + "/nfs3/result_upload;"
                        + "/nfs4/result_upload;"
                        + "/nfs5/result_upload;"
                        + "/nfs6/result_upload;"
                        + "/nfs7/result_upload;"
                        + "/nfs8/result_upload");
        folderMap.put(ScheduleConstants.UploadType.FAIL_RESULT.name(),
                "/nfs1/fail_result_upload;"
                        + "/nfs2/fail_result_upload;"
                        + "/nfs3/fail_result_upload;"
                        + "/nfs4/fail_result_upload;"
                        + "/nfs5/fail_result_upload;"
                        + "/nfs6/fail_result_upload;"
                        + "/nfs7/fail_result_upload;"
                        + "/nfs8/fail_result_upload");
        folderMap.put(ScheduleConstants.UploadType.SCM_JSON.name(),
                "/nfs1/scm;"
                        + "/nfs2/scm;"
                        + "/nfs3/scm;"
                        + "/nfs4/scm;"
                        + "/nfs5/scm;"
                        + "/nfs6/scm;"
                        + "/nfs7/scm;"
                        + "/nfs8/scm");
        folderMap.put(ScheduleConstants.DownloadType.TOOL_CLIENT.name(),
                "/download/tool_client_download");
        folderMap.put(ScheduleConstants.DownloadType.BUILD_SCRIPT.name(),
                "/download/script_download");
        folderMap.put(ScheduleConstants.DownloadType.SCM_TOOL.name(),
                "/download/tool_client_download/scm_tool");
        folderMap.put(ScheduleConstants.DownloadType.P4_TOOL.name(),
                "/download/tool_client_download/p4_tool");
        folderMap.put(ScheduleConstants.UploadType.AGGREGATE.name(),
                "/nfs1/aggregate;"
                        + "/nfs2/aggregate;"
                        + "/nfs3/aggregate;"
                        + "/nfs4/aggregate;"
                        + "/nfs5/aggregate;"
                        + "/nfs6/aggregate;"
                        + "/nfs7/aggregate;"
                        + "/nfs8/aggregate");
        folderMap.put(ScheduleConstants.DownloadType.GATHER.name(), "/download/gather");
        folderMap.put(ScheduleConstants.DownloadType.OP_EXCEL.name(), "/download/op_excel");
        return folderMap;
    }

    @Override
    public Boolean upload(UploadVO uploadVO, InputStream fileInputStream) {
        long beginTime = System.currentTimeMillis();
        log.info("begin upload: {}", uploadVO);

        String fileBaseName = uploadVO.getFileName();

        // 给文件建立索引
        FileIndexVO fileIndexVO = index(fileBaseName, uploadVO.getUploadType());
        String uploadFolder = fileIndexVO.getFileFolder();

        //创建服务器存放上传文件所需的文件夹
        ChunkUploadUtil.createFileFolder(uploadFolder);

        File outFile;
        // 非分片上传
        if (uploadVO.getChunks() == null || uploadVO.getChunks() <= 0) {
            outFile = new File(Paths.get(uploadFolder, fileBaseName).toString());
            log.info("file={}", outFile.getAbsolutePath());
        }
        // 分片上传
        else {
            //为上传的文件准备好对应的目录
            outFile = ChunkUploadUtil.createChunkFileFolder(uploadFolder, fileBaseName, uploadVO.getChunk(),
                    uploadVO.getBuildId());
            log.info("chunk file={}", outFile.getAbsolutePath());
        }

        int index;
        byte[] bytes = new byte[1024];
        try (FileOutputStream fileOutputStream = new FileOutputStream(outFile)) {
            while ((index = fileInputStream.read(bytes)) != -1) {
                fileOutputStream.write(bytes, 0, index);
                fileOutputStream.flush();
            }
            if (uploadVO.getChunks() == null || uploadVO.getChunks() <= 0) {
                //上传至文件存储
                uploadToStorage(uploadVO.getFileName());
            }else {
                uploadChunkToStorage(uploadVO.getFileName(), uploadVO.getChunk(), outFile);
            }
        } catch (IOException e) {
            log.error("upload file {} exception", fileBaseName, e);
            throw new CodeCCException(DispatchMessageCode.UPLOAD_FILE_ERR);
        } finally {
            try {
                fileInputStream.close();
            } catch (IOException e) {
                log.error("close fileInputStream exception", e);
            }
        }

        log.info("end upload cost: {}, {}", System.currentTimeMillis() - beginTime, uploadVO);
        return true;
    }

    @Override
    public Boolean chunksMerge(FileChunksMergeVO fileChunksMergeVO) {
        long beginTime = System.currentTimeMillis();
        log.info("begin chunksMerge: {}", fileChunksMergeVO);
        String fileBaseName = fileChunksMergeVO.getFileName();

        // 获取文件索引
        FileIndexVO fileIndexVO = getFileIndex(fileBaseName, fileChunksMergeVO.getUploadType());
        String uploadFolder = fileIndexVO.getFileFolder();

        //文件上传路径更新为指定文件信息签名后的临时文件夹，用于后期合并
        String fileFolder = ChunkUploadUtil.md5(fileBaseName + fileChunksMergeVO.getBuildId());
        String chunkFileFolder = uploadFolder + File.separator + fileFolder;
        log.info("chunkFileFolder={}", chunkFileFolder);

        int chunks = fileChunksMergeVO.getChunks();

        int chunksNum = this.getChunksNum(chunkFileFolder, chunks);
        //检查是否满足合并条件：分片数量是否足够
        if (chunks == chunksNum) {
            //同步指定合并的对象
            Lock lock = FileLock.getLock(fileFolder);
            try {
                lock.lock();
                //检查是否满足合并条件：分片数量是否足够
                List<File> files = new ArrayList<>(Arrays.asList(this.getChunks(chunkFileFolder)));
                if (chunks == files.size()) {
                    //按照文件分片的最后的块号排序文件
                    files.sort(Comparator.comparing((file) -> getCompareChunkNo(file, fileBaseName)));

                    merge(files, uploadFolder, fileBaseName);

                    finishChunkToStorage(fileIndexVO.getFileName());
                }
            } catch (Exception ex) {
                log.error("数据分片合并失败", ex);
            } finally {
                lock.unlock();

                //清理分片临时文件夹
                this.cleanChunkFileFolder(chunkFileFolder);

                //清理锁对象
                FileLock.removeLock(fileFolder);
            }
        } else {
            //清理分片临时文件夹
            this.cleanChunkFileFolder(chunkFileFolder);
            log.error("数据分片合并失败, 入参分片数chunks={}不等于已上传的分片文件数chunksNum={}, fileName={}",
                    chunks, chunksNum, fileBaseName);
            throw new CodeCCException(DispatchMessageCode.UPLOAD_FILE_ERR);
        }

        log.info("end chunksMerge cost: {}, {}", System.currentTimeMillis() - beginTime, fileChunksMergeVO);
        return true;
    }

    @NotNull
    private int getCompareChunkNo(File file, String fileBaseName) {
        String chunkFileNamePrefix = String.format("%s%s", fileBaseName, ScheduleConstants.CHUNK_FILE_SUFFIX);
        String fileName = file.getName();
        String chunkNo = fileName.substring(chunkFileNamePrefix.length());
        return StringUtils.isEmpty(chunkNo) ? 0 : Integer.valueOf(chunkNo);
    }

    @Override
    public Long getFileSize(GetFileSizeVO getFileSizeVO) {
        long beginTime = System.currentTimeMillis();
        log.info("begin getFileSize: {}", getFileSizeVO);

        String fileName = getFileSizeVO.getFileName();
        String downloadFolder;
        if (ScheduleConstants.DownloadType.LAST_RESULT.name().equals(getFileSizeVO.getDownloadType())) {
            // 获取文件索引
            FileIndexVO fileIndexVO = getFileIndex(fileName, getFileSizeVO.getDownloadType());
            downloadFolder = fileIndexVO.getFileFolder();
        } else {
            downloadFolder = codeccFileDataPath + FOLDER_MAP.get(getFileSizeVO.getDownloadType());
        }

        long size = 0;
        File f = new File(downloadFolder, fileName);
        if (f.exists() && f.isFile()) {
            size = f.length();
            log.info("tool client [{}] size is {}", fileName, size);
        } else {
            log.error("file [{}] doesn't exist or is not a file", fileName);
        }
        log.info("file size: {}", size);
        log.info("end getFileSize cost: {}, {}", System.currentTimeMillis() - beginTime, getFileSizeVO);
        return size;
    }

    @Override
    public Response download(DownloadVO downloadVO) {
        long beginTime = System.currentTimeMillis();
        log.info("begin download: {}", downloadVO);

        String fileName = downloadVO.getFileName();
        String downloadFolder;
        if (ScheduleConstants.DownloadType.LAST_RESULT.name().equals(downloadVO.getDownloadType())
                || ScheduleConstants.DownloadType.GATHER.name().equals(downloadVO.getDownloadType())) {
            // 获取文件索引
            FileIndexVO fileIndexVO = getFileIndex(fileName, downloadVO.getDownloadType());
            downloadFolder = fileIndexVO.getFileFolder();
        } else {
            downloadFolder = codeccFileDataPath + FOLDER_MAP.get(downloadVO.getDownloadType());
        }
        //如果需要的话，下载文件
        downloadFromStorage(Paths.get(downloadFolder, fileName).toString(),fileName);
        //判断文件是否存在
        File target = new File(downloadFolder, fileName);
        if (!target.exists()) {
            log.error("{}不存在", target.getAbsolutePath());
            throw new CodeCCException(DispatchMessageCode.FILE_NOT_EXISTS, new String[]{fileName}, null);
        }

        if (!target.isFile()) {
            log.error("{}不是一个文件", target.getAbsolutePath());
            throw new CodeCCException(DispatchMessageCode.NOT_A_FILE, new String[]{fileName}, null);
        }

        // 一次读40960个字节，如果文件内容不足40960个字节，则读剩下的字节。
        int bufSize = 40960;

        StreamingOutput fileStream = output -> {
            log.info("downloading: {}", target.getAbsoluteFile());
            // 打开一个随机访问文件流，按只读方式
            try (RandomAccessFile randomFile = new RandomAccessFile(target, "r"); OutputStream out = output) {
                // 将读文件的开始位置移到beginIndex位置。
                randomFile.seek(downloadVO.getBeginIndex());
                byte[] bytes = new byte[bufSize];
                // 请求端传入的需要读取的字节数
                long btyeSize = downloadVO.getBtyeSize();
                int len;
                while ((len = randomFile.read(bytes)) != -1 && btyeSize > 0) {
                    out.write(bytes, 0, len);
                    out.flush();
                    btyeSize -= bufSize;
                }
            } catch (Throwable throwable) {
                log.error("下载文件异常: {}", target.getAbsolutePath(), throwable);
                throw new CodeCCException(DispatchMessageCode.DOWNLOAD_FILE_ERR, new String[]{fileName}, null);
            }
            log.info("success download cost: {}, downloadType: {}, fileName: {}",
                    System.currentTimeMillis() - beginTime, downloadVO.getDownloadType(), fileName);
        };
        Response response = Response.ok(fileStream, MediaType.APPLICATION_OCTET_STREAM_TYPE)
                .header("content-disposition", "attachment; filename = " + target.getName())
                .build();

        log.info("end download: {}", downloadVO);
        return response;
    }

    @Override
    public FileInfoModel getFileInfo(GetFileSizeVO getFileSizeVO) {
        long beginTime = System.currentTimeMillis();
        log.info("begin getFileInfo: {}", getFileSizeVO);

        String fileName = getFileSizeVO.getFileName();
        String downloadFolder;
        if (ScheduleConstants.DownloadType.LAST_RESULT.name().equals(getFileSizeVO.getDownloadType())) {
            // 获取文件索引
            FileIndexVO fileIndexVO = getFileIndex(fileName, getFileSizeVO.getDownloadType());
            downloadFolder = fileIndexVO.getFileFolder();
        } else {
            downloadFolder = codeccFileDataPath + FOLDER_MAP.get(getFileSizeVO.getDownloadType());
        }

        String filePath = downloadFolder + File.separator + fileName;
        File file = new File(filePath);
        FileInfoModel fileInfo = null;
        if (file.exists() && file.isFile()) {
            String md5 = null;
            long lastModifiedTime = file.lastModified();
            fileInfo = fileInfoCache.getFileInfo(filePath);
            if (fileInfo == null) {
                md5 = getFileMd5(fileName, file);
                if (StringUtils.isNotEmpty(md5)) {
                    fileInfo = new FileInfoModel();
                    fileInfo.setFileName(fileName);
                    fileInfo.setFilePath(filePath);
                    fileInfo.setContentMd5(md5);
                    fileInfo.setSize(file.length());
                    fileInfo.setLastModifiedTime(lastModifiedTime);
                    fileInfo.setCreatetime(System.currentTimeMillis());
                    fileInfoCache.saveFileInfo(fileInfo);
                }
            } else {
                if (fileInfo.getLastModifiedTime() < lastModifiedTime) {
                    md5 = getFileMd5(fileName, file);
                    if (StringUtils.isNotEmpty(md5)) {
                        fileInfo.setContentMd5(md5);
                        fileInfo.setSize(file.length());
                        fileInfo.setLastModifiedTime(lastModifiedTime);
                        fileInfoCache.saveFileInfo(fileInfo);
                    }
                }
            }
        } else {
            log.error("file [{}] doesn't exist or is not a file", fileName);
        }

        log.info("file info: {}", fileInfo);
        log.info("end getFileInfo cost: {}, {}", System.currentTimeMillis() - beginTime, getFileSizeVO);
        return fileInfo;
    }

    @Override
    public FileIndexVO index(String fileName, String type) {
        long beginTime = System.currentTimeMillis();
        log.info("begin index: {}, {}", fileName, type);

        FileIndexEntity fileIndexEntity = fileIndexRepository.findFirstByFileName(fileName);

        if (fileIndexEntity == null
                || fileIndexEntity.getFileFolder().contains("fail_result_upload")) {
            //获取文件夹
            String fileFolder = getFileFolder(fileName, type);

            ChunkUploadUtil.createFileFolder(fileFolder);

            // 兼容废弃cfs作为文件存储，这里需要把原来缓存的cfs文件路径刷新成新的文件路径
            if (fileIndexEntity == null) {
                fileIndexEntity = new FileIndexEntity();
            }
            fileIndexEntity.setFileName(fileName);
            fileIndexEntity.setFileFolder(fileFolder);
            fileIndexEntity.setUploadType(type);
            fileIndexEntity.setCreatedDate(System.currentTimeMillis());
            fileIndexEntity.setUpdatedDate(System.currentTimeMillis());

            fileIndexRepository.save(fileIndexEntity);
        }
        FileIndexVO fileIndexVO = new FileIndexVO();
        BeanUtils.copyProperties(fileIndexEntity, fileIndexVO);

        log.info("end index cost: {}, {}", System.currentTimeMillis() - beginTime, fileIndexVO);
        return fileIndexVO;
    }

    private String getFileFolder(String fileName, String type){
        String uploadFolders = FOLDER_MAP.get(type);
        if (StringUtils.isEmpty(uploadFolders)) {
            log.error("indexed file {} fail, type [{}] invalid", fileName, type);
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{"type"}, null);
        }
        String[] uploadFolderArr = uploadFolders.split(";");
        int chooseIndex = (int) (System.currentTimeMillis() % uploadFolderArr.length);
        String uploadFolder = codeccFileDataPath + uploadFolderArr[chooseIndex];

        String fileNameMD5 = MD5Utils.getMD5(fileName);
        String dir1 = fileNameMD5.substring(0, 2);
        String dir2 = fileNameMD5.substring(2, 4);
        return String.format("%s/%s/%s", uploadFolder, dir1, dir2);
    }

    @Override
    public FileIndexVO getFileIndex(String fileName, String type) {
        long beginTime = System.currentTimeMillis();
        log.info("begin getFileIndex: {}, {}", fileName, type);

        FileIndexEntity fileIndexEntity = fileIndexRepository.findFirstByFileName(fileName);

        FileIndexVO fileIndexVO = new FileIndexVO();
        if (fileIndexEntity != null) {
            BeanUtils.copyProperties(fileIndexEntity, fileIndexVO);
        }

        log.info("end getFileIndex cost: {}, {}", System.currentTimeMillis() - beginTime, fileIndexVO);
        return fileIndexVO;
    }

    @Override
    public Response download(String downloadType, String fileName) {
        long beginTime = System.currentTimeMillis();
        log.info("begin download, downloadType: {}, fileName: {}", downloadType, fileName);

        String downloadFolder;
        if (ScheduleConstants.DownloadType.LAST_RESULT.name().equals(downloadType)
                || ScheduleConstants.DownloadType.GATHER.name().equals(downloadType)
                || ScheduleConstants.DownloadType.OP_EXCEL.name().equals(downloadType)) {
            // 获取文件索引
            FileIndexVO fileIndexVO = getFileIndex(fileName, downloadType);
            downloadFolder = fileIndexVO.getFileFolder();
        } else {
            downloadFolder = codeccFileDataPath + FOLDER_MAP.get(downloadType);
        }
        //如果需要的话，下载文件
        downloadFromStorage(Paths.get(downloadFolder, fileName).toString(),fileName);
        //判断文件是否存在
        File target = new File(downloadFolder, fileName);
        if (!target.exists()) {
            log.error("{}不存在", target.getAbsolutePath());
            throw new CodeCCException(DispatchMessageCode.FILE_NOT_EXISTS, new String[]{fileName}, null);
        }

        if (!target.isFile()) {
            log.error("{}不是一个文件", target.getAbsolutePath());
            throw new CodeCCException(DispatchMessageCode.NOT_A_FILE, new String[]{fileName}, null);
        }
        StreamingOutput fileStream = output ->
        {
            log.info("downloading: {}", target.getAbsoluteFile());
            try (FileInputStream fileInputStream = new FileInputStream(target); OutputStream out = output) {
                IOUtils.copyLarge(fileInputStream, out);
            } catch (Throwable throwable) {
                log.error("下载文件异常: {}", target.getAbsolutePath(), throwable);
                throw new CodeCCException(DispatchMessageCode.DOWNLOAD_FILE_ERR, new String[]{fileName}, null);
            }
            log.info("success download cost: {}, downloadType: {}, fileName: {}",
                    System.currentTimeMillis() - beginTime, downloadType, fileName);
        };
        Response response = Response.ok(fileStream, MediaType.APPLICATION_OCTET_STREAM_TYPE)
                .header("content-disposition", "attachment; filename = " + target.getName())
                .build();

        log.info("end download. downloadType: {}, fileName: {}", downloadType, fileName);
        return response;
    }

    @Override
    public FileIndexVO updateFileIndex(FileIndexVO fileInfoVo) {
        FileIndexEntity fileIndexEntity = fileIndexRepository.findFirstByFileName(fileInfoVo.getFileName());
        if(fileIndexEntity == null){
            fileIndexEntity = new FileIndexEntity();
            fileIndexEntity.setFileName(fileInfoVo.getFileName());
            fileIndexEntity.setFileFolder(getFileFolder(fileInfoVo.getFileName(), fileInfoVo.getUploadType()));
            fileIndexEntity.setUploadType(fileInfoVo.getUploadType());
            fileIndexEntity.setCreatedDate(System.currentTimeMillis());
            fileIndexEntity.setUpdatedDate(System.currentTimeMillis());
        }
        fileIndexEntity.setStoreType(fileInfoVo.getStoreType());
        fileIndexEntity.setDownloadUrl(fileInfoVo.getDownloadUrl());
        fileIndexRepository.save(fileIndexEntity);
        BeanUtils.copyProperties(fileIndexEntity, fileInfoVo);
        return fileInfoVo;
    }

    private String getFileMd5(String fileName, File file) {
        String md5 = null;
        try (FileInputStream inputStream = new FileInputStream(file)) {
            md5 = DigestUtils.md5Hex(inputStream);
        } catch (IOException e) {
            log.error("get md5 of file [{}] fail", fileName, e);
        }
        log.info("tool fileName [{}] md5 is {}", fileName, md5);

        return md5;
    }

    protected boolean merge(List<File> files, String path, String fileName) throws IOException {
        //创建合并后的文件
        log.info("path={},fileName={}", path, fileName);
        File outputFile = new File(Paths.get(path, fileName).toString());
        if (outputFile.exists()) {
            log.warn("文件[{}]已经存在，删除重新创建", outputFile.getAbsolutePath());
            outputFile.delete();
        }
        outputFile.getParentFile().mkdirs();
        boolean newFile = outputFile.createNewFile();
        if (!newFile) {
            log.error("创建文件失败");
            throw new CodeCCException(DispatchMessageCode.UPLOAD_FILE_ERR);
        }

        try (FileOutputStream out = new FileOutputStream(outputFile); FileChannel outChannel = out.getChannel()) {
            //同步nio方式对分片进行合并, 有效的避免文件过大导致内存溢出
            for (File file : files) {
                try (FileInputStream input = new FileInputStream(file); FileChannel inChannel = input.getChannel()) {
                    inChannel.transferTo(0, inChannel.size(), outChannel);
                } catch (FileNotFoundException ex) {
                    log.error("文件转换失败", ex);
                    throw new CodeCCException(DispatchMessageCode.UPLOAD_FILE_ERR);
                }
                //删除分片
                if (!file.delete()) {
                    log.error("分片[{}]删除失败", file.getName());
                }
            }
            outChannel.force(true);
        } catch (FileNotFoundException e) {
            log.error("文件输出失败", e);
            throw new CodeCCException(DispatchMessageCode.UPLOAD_FILE_ERR);
        }

        return true;
    }

    /**
     * 获取指定文件的分片数量
     *
     * @param folder 文件夹路径
     * @param chunks
     * @return
     */
    private int getChunksNum(String folder, int chunks) {
        int i = 0;
        File[] filesList = this.getChunks(folder);
        int chunksNum = filesList == null ? 0 : filesList.length;
        while (chunks != chunksNum && i < 20) {
            try {
                Thread.sleep(3000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            i++;
            filesList = this.getChunks(folder);
            chunksNum = filesList == null ? 0 : filesList.length;
            log.info("retry to get chunksNum:{}", chunksNum);
        }
        log.info("chunks={}, chunksNum={}", chunks, chunksNum);
        return chunksNum;
    }

    /**
     * 获取指定文件的所有分片
     *
     * @param folder 文件夹路径
     * @return
     */
    private File[] getChunks(String folder) {
        File targetFolder = new File(folder);
        return targetFolder.listFiles((file) ->
        {
            if (file.isDirectory()) {
                return false;
            }
            return true;
        });
    }

    /**
     * 清理分片上传的相关数据
     *
     * @param chunkTmpFolder 文件夹名称
     * @return
     */
    protected boolean cleanChunkFileFolder(String chunkTmpFolder) {
        //删除分片文件夹
        File garbage = new File(chunkTmpFolder);
        if (!FileUtils.deleteQuietly(garbage)) {
            return false;
        }
        return true;
    }

    /**
     * 将文件上传到文件存储引擎
     * NFS挂载本机目录不需要上传
     * @param fileName
     */
    private void uploadToStorage(String fileName){
        FileIndexEntity index = fileIndexRepository.findFirstByFileName(fileName);
        if (index == null || storageService.getStorageType().equals(StorageType.NFS.code())) {
            return;
        }
        String localFilePath = Paths.get(index.getFileFolder(), index.getFileName()).toString();
        try {
            String url = storageService.upload(localFilePath, index.getUploadType(), index.getFileName());
            index.setStoreType(storageService.getStorageType());
            index.setDownloadUrl(url);
            fileIndexRepository.save(index);
        } catch (Exception e) {
            log.error("uploadToStorage filename:" + fileName + " storage:" +
                    storageService.getStorageType() + " fail!", e);
        }
    }

    /**
     * 将分片文件上传到文件存储引擎
     * NFS挂载本机目录不需要上传
     * @param fileName
     */
    private void uploadChunkToStorage(String fileName,Integer chunkNo, File file) {
        FileIndexEntity index = fileIndexRepository.findFirstByFileName(fileName);
        if (index == null || storageService.getStorageType().equals(StorageType.NFS.code())) {
            return;
        }
        try {
            if(chunkNo == 1){
                //初次上传，初始化
                String uploadId = storageService.startChunk(index.getUploadType(), index.getFileName());
                index.setStoreType(storageService.getStorageType());
                index.setUploadId(uploadId);
                fileIndexRepository.save(index);
            }
            storageService.chunkUpload(file.getAbsolutePath(), index.getUploadType(), index.getFileName(),
                    chunkNo, index.getUploadId());
        } catch (Exception e) {
            log.error("uploadToStorage filename:" + fileName + " storage:" +
                    storageService.getStorageType() + " fail!", e);
        }
    }

    /**
     * 完成分配上传
     * @param fileName
     */
    private void finishChunkToStorage(String fileName) {
        FileIndexEntity index = fileIndexRepository.findFirstByFileName(fileName);
        if (index == null || storageService.getStorageType().equals(StorageType.NFS.code())) {
            return;
        }
        try {
            String url = storageService.finishChunk(index.getUploadType(), index.getFileName(), index.getUploadId());
            index.setDownloadUrl(url);
            fileIndexRepository.save(index);
        } catch (Exception e) {
            log.error("finishChunkToStorage filename:" + fileName + " storage:" +
                    storageService.getStorageType() + " fail!", e);
        }
    }

    /**
     * 将文件上传到文件存储引擎
     * NFS挂载本机目录不需要上传
     * @param fileName
     */
    private void downloadFromStorage(String downloadFolder, String fileName) {
        FileIndexEntity index = fileIndexRepository.findFirstByFileName(fileName);
        Path localFilePath = Paths.get(downloadFolder, fileName);
        if(Files.exists(localFilePath) || index == null || index.getStoreType() == null
                || index.getStoreType().equals(StorageType.NFS.code())){
            return;
        }
        try {
            storageService.download(localFilePath.toString(), index.getStoreType(), index.getFileName());
        } catch (Exception e) {
            log.error("downloadFromToStorage filename:" + fileName + " storage:" +
                    storageService.getStorageType() + " fail!", e);
        }
    }

}
