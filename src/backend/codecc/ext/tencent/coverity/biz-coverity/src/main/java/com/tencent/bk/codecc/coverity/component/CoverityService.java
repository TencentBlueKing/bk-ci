package com.tencent.bk.codecc.coverity.component;

import com.coverity.ws.v9.*;
import com.tencent.bk.codecc.coverity.constant.CoverityConstants;
import com.tencent.bk.codecc.coverity.constant.CoverityMessageCode;
import com.tencent.bk.codecc.coverity.utils.CoverityDefectAttributeUtils;
import com.tencent.bk.codecc.task.vo.PlatformVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.util.CompressionUtils;
import com.tencent.devops.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Example application that uses Web Services to work with the
 * Coverity Integrity Manager server.  This application gets all the
 * users and prints information about them.
 * <p>
 * Most of the complexity is in arranging WSS authentication for the
 * messages sent to the user.
 */
@Slf4j
public class CoverityService
{
    private static String coverityUserName = "";

    private static String coverityPassword = "";

    private static String coverityPlatformRootUrl = "";

    private static String coverityNamespaceURI = "http://ws.coverity.com/v9";

    private ConfigurationService _configurationService;
    private DefectService _defectService;

    private static Map<String, CoverityService> _mapInst = new HashedMap();

    // 允许注册新项目的实例
    private static Map<String, CoverityService> _mapInst_AllowRegister = new HashedMap();

    // 允许注册开源新项目的实例
    private static Map<String, CoverityService> _mapInst_AllowRegisterOpenSource = new HashedMap();

    /**
     * 根据IP获取对应实例
     *
     * @param ip
     * @return
     */
    public static CoverityService getInst(String ip)
    {
        CoverityService coverityService = _mapInst.get(ip);
        if (coverityService == null)
        {
            log.error("get coverity platform instance by {} is null!", ip);
            throw new CodeCCException(CoverityMessageCode.GET_COV_PLATFORM_INST_FAIL);
        }
        return coverityService;
    }

    public static Map<String, CoverityService> getAllPlatformInst()
    {
        return _mapInst;
    }

    public static Map<String, CoverityService> getAllowRegisterPlatformInst()
    {
        return _mapInst_AllowRegister;
    }

    public static Map<String, CoverityService> getAllowRegisterOpenSourcePlatformInst()
    {
        return _mapInst_AllowRegisterOpenSource;
    }

    public static void initAllPlatform(List<PlatformVO> covPlatformList)
    {
        if (CollectionUtils.isNotEmpty(covPlatformList))
        {
            for (PlatformVO platform : covPlatformList)
            {
                CoverityService coverityServiceInstance = new CoverityService();
                boolean initResult = coverityServiceInstance.init(platform);
                if (initResult)
                {
                    _mapInst.put(platform.getIp(), coverityServiceInstance);

                    if (ComConstants.Status.DISABLE.value() != platform.getStatus())
                    {
                        if (platform.getSupportTaskTypes().contains(ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value()))
                        {
                            _mapInst_AllowRegisterOpenSource.put(platform.getIp(), coverityServiceInstance);
                        }

                        if (platform.getSupportTaskTypes().contains(ComConstants.BsTaskCreateFrom.BS_CODECC.value())
                                || platform.getSupportTaskTypes().contains(ComConstants.BsTaskCreateFrom.BS_PIPELINE.value()))
                        {
                            _mapInst_AllowRegister.put(platform.getIp(), coverityServiceInstance);
                        }
                    }
                }
            }
        }
    }

    public boolean init(PlatformVO platform)
    {
        String ip = platform.getIp();
        String port = platform.getPort();
        String userName = platform.getUserName();
        String password = platform.getPasswd();
        String platformRootUrl = String.format("http://%s:%s/ws/v9/", ip, port);
        String namespaceURI = coverityNamespaceURI;;
        try
        {
            // Create a Web Services port to the server
            DefectServiceService defectServiceService = new DefectServiceService(
                    new URL(platformRootUrl + "defectservice?wsdl"),
                    new QName(namespaceURI, "DefectServiceService"));
            _defectService = defectServiceService.getDefectServicePort();

            // Attach an authentication handler to it
            BindingProvider bindingProvider = (BindingProvider) _defectService;
            setWebServiceAuthentication(bindingProvider, userName, password);
        }
        catch (Exception e)
        {
            log.error("Init defectService failed: {}:{}\n", ip, port, e);
            return false;
        }

        try
        {
            // Create a Web Services port to the server
            ConfigurationServiceService configurationServiceService = new ConfigurationServiceService(
                    new URL(platformRootUrl + "configurationservice?wsdl"),
                    new QName(namespaceURI, "ConfigurationServiceService"));
            _configurationService = configurationServiceService.getConfigurationServicePort();

            // Attach an authentication handler to it
            BindingProvider bindingProvider = (BindingProvider) _configurationService;
            setWebServiceAuthentication(bindingProvider, userName, password);
        }
        catch (Exception e)
        {
            log.error("Init configurationService failed: {}:{}\n", ip, port, e);
            return false;
        }

        log.info("Init Coverity Service succ: {}:{}", ip, port);
        return true;
    }

    /**
     * 设置鉴权信息
     *
     * @param bindingProvider
     * @param userName
     * @param passwd
     */
    private void setWebServiceAuthentication(BindingProvider bindingProvider, String userName, String passwd)
    {
        // 设置超时时间
        Map<String, Object> requestContext = bindingProvider.getRequestContext();

        //建立连接的超时时间为10秒
        requestContext.put("com.sun.xml.internal.ws.connection.timeout", 10 * 1000);

        //指定请求的响应超时时间为30秒
        requestContext.put("com.sun.xml.internal.ws.request.timeout", 30 * 1000);

        // 设置鉴权信息
        bindingProvider.getBinding().setHandlerChain(new ArrayList<>(Arrays.asList(new ClientAuthenticationHandlerWSS(userName, passwd))));
    }

    public boolean isExistStream(String strName)
    {
        StreamFilterSpecDataObj obj = new StreamFilterSpecDataObj();
        obj.setNamePattern(strName);

        List<StreamDataObj> listStreams = null;
        try
        {
            listStreams = _configurationService.getStreams(obj);
        }
        catch (CovRemoteServiceException_Exception e)
        {
            log.error("getStreams fail: {}\n", strName, e);
        }

        if (CollectionUtils.isNotEmpty(listStreams))
        {
            StreamDataObj data = listStreams.get(0);
            if (data.getId().getName().equals(strName))
            {
                log.info("exist stream: {}", strName);
                return true;
            }
        }
        log.info("does not exist stream: {}", strName);
        return false;
    }

    public boolean createComponentMap(String strComponentMapName)
    {
        ComponentMapSpecDataObj mapObj = new ComponentMapSpecDataObj();
        mapObj.setComponentMapName(strComponentMapName);

        ComponentDataObj comObj = new ComponentDataObj();
        ComponentIdDataObj componentId = new ComponentIdDataObj();
        componentId.setName(strComponentMapName + CoverityConstants.IGNORE_COMPONENT_POSTFIX);
        comObj.setComponentId(componentId);
        mapObj.getComponents().add(comObj);

        try
        {
            _configurationService.createComponentMap(mapObj);
        }
        catch (CovRemoteServiceException_Exception e)
        {
            log.error("getStreams fail: {}\n", strComponentMapName, e);
            return false;
        }

        return true;
    }

    private RoleAssignmentDataObj newRoleAssignmentDataObj(String username, String type, String roleId)
    {
        RoleAssignmentDataObj roleAssignmentObj = new RoleAssignmentDataObj();
        roleAssignmentObj.setUsername(username);
        roleAssignmentObj.setType(type);
        RoleIdDataObj roldId = new RoleIdDataObj();
        roldId.setName(roleId);
        roleAssignmentObj.setRoleId(roldId);
        roleAssignmentObj.setRoleAssignmentType("user");

        return roleAssignmentObj;
    }

    public boolean createTriageStore(String strTriageStoreName)
    {
        TriageStoreSpecDataObj specObj = new TriageStoreSpecDataObj();
        specObj.setName(strTriageStoreName);
        specObj.getRoleAssignments().add(newRoleAssignmentDataObj("admin", "triageStore", "triageStoreOwner"));

        try
        {
            _configurationService.createTriageStore(specObj);
        }
        catch (CovRemoteServiceException_Exception e)
        {
            log.error("createTriageStore error: {}\n", strTriageStoreName, e);
            return false;
        }
        return true;
    }

    public boolean updateComponentMap(String streamName, List<String> paths)
    {
        List<ComponentMapDataObj> componentMapObjList = this.getComponentMapObjList(streamName);

        if (CollectionUtils.isEmpty(componentMapObjList))
        {
            log.error("the stream has no component info");
            return false;
        }
        ComponentMapDataObj mapDataObj = componentMapObjList.get(0);

        String ignoreComponentName = streamName + CoverityConstants.IGNORE_COMPONENT_POSTFIX;

        ComponentMapSpecDataObj mapSpecObj = new ComponentMapSpecDataObj();
        mapSpecObj.setComponentMapName(streamName);
        List<ComponentPathRuleDataObj> componentPathRules = mapSpecObj.getComponentPathRules();
        for (String path : paths)
        {
            if (StringUtils.isEmpty(path))
            {
                continue;
            }
            ComponentPathRuleDataObj rule = new ComponentPathRuleDataObj();
            ComponentIdDataObj idDataObj = new ComponentIdDataObj();
            idDataObj.setName(ignoreComponentName);
            rule.setComponentId(idDataObj);
            rule.setPathPattern(path);
            componentPathRules.add(rule);
        }
        mapSpecObj.getComponents().addAll(mapDataObj.getComponents());

        try
        {
            _configurationService.updateComponentMap(mapDataObj.getComponentMapId(), mapSpecObj);
        }
        catch (CovRemoteServiceException_Exception e)
        {
            log.error("Failed to update componentMap: {}\n", streamName, e);
            return false;
        }

        return true;
    }

    private List<ComponentMapDataObj> getComponentMapObjList(String strComponentMapName)
    {
        ComponentMapFilterSpecDataObj filterSpec = new ComponentMapFilterSpecDataObj();
        filterSpec.setNamePattern(strComponentMapName);
        List<ComponentMapDataObj> componentMaps = null;
        try
        {
            componentMaps = _configurationService.getComponentMaps(filterSpec);
        }
        catch (CovRemoteServiceException_Exception e)
        {
            log.error("Failed to getComponentMapObjList: {}\n", strComponentMapName, e);
            return null;
        }
        System.out.println("map size " + componentMaps.size());
        return componentMaps;
    }

    public boolean createProject(String strProjName)
    {
        ProjectSpecDataObj specObj = new ProjectSpecDataObj();
        specObj.setName(strProjName);
        try
        {
            _configurationService.createProject(specObj);
        }
        catch (CovRemoteServiceException_Exception e)
        {
            log.error("Failed to createProject: {}\n", strProjName, e);
            return false;
        }
        return true;
    }

    public boolean createStream(String strProjName, String strStreamName, String strComponentMapName, String strTriageStoreName)
    {
        ProjectIdDataObj projId = new ProjectIdDataObj();
        projId.setName(strProjName);

        StreamSpecDataObj streamSepc = new StreamSpecDataObj();
        streamSepc.setName(strStreamName);

//        ComponentMapIdDataObj comMapObj = new ComponentMapIdDataObj();
//        comMapObj.setName(strComponentMapName);
//        streamSepc.setComponentMapId(comMapObj);

        TriageStoreIdDataObj triageObj = new TriageStoreIdDataObj();
        triageObj.setName(strTriageStoreName);
        streamSepc.setTriageStoreId(triageObj);

        streamSepc.getRoleAssignments().add(newRoleAssignmentDataObj("admin", "stream", "streamOwner"));
//        streamSepc.getRoleAssignments().add(newRoleAssignmentDataObj(strUserName, "stream", "streamOwner"));

        streamSepc.setDescription("Generated by CodeCC Platform");

        try
        {
            _configurationService.createStreamInProject(projId, streamSepc);
        }
        catch (CovRemoteServiceException_Exception e)
        {
            log.error("Failed to createProject: {}\n", strProjName, e);
            return false;
        }

        return true;
    }

    public boolean delStream(String strStreamName)
    {
        StreamIdDataObj streamIdDataObj = new StreamIdDataObj();
        streamIdDataObj.setName(strStreamName);
        try
        {
            _configurationService.deleteStream(streamIdDataObj, false);
        }
        catch (CovRemoteServiceException_Exception e)
        {
            log.error("Failed to createProject: {}\n", strStreamName, e);
            return false;
        }
        return true;
    }

    public boolean delProject(String strStreamName)
    {
        ProjectIdDataObj idDataObj = new ProjectIdDataObj();
        idDataObj.setName(strStreamName);
        try
        {
            _configurationService.deleteProject(idDataObj);
        }
        catch (CovRemoteServiceException_Exception e)
        {
            log.error("Failed to createProject: {}\n", strStreamName, e);
            return false;
        }
        return true;
    }

    public boolean delTriageStore(String strTriageStoreName)
    {
        TriageStoreIdDataObj idDataObj = new TriageStoreIdDataObj();
        idDataObj.setName(strTriageStoreName);
        try
        {
            _configurationService.deleteTriageStore(idDataObj);
        }
        catch (CovRemoteServiceException_Exception e)
        {
            log.error("Failed to createProject: {}\n", strTriageStoreName, e);
            return false;
        }

        return true;
    }

    public boolean delComponentMap(String strComName)
    {
        ComponentMapIdDataObj idDataObj = new ComponentMapIdDataObj();
        idDataObj.setName(strComName);
        try
        {
            _configurationService.deleteComponentMap(idDataObj);
        }
        catch (CovRemoteServiceException_Exception e)
        {
            log.error("Failed to createProject: {}\n", strComName, e);
            return false;
        }

        return true;
    }

    /**
     * 根据CID一次获取所有告警实例
     *
     * @param mergedDefectList
     * @param cidSet
     * @return
     */
    public Map<Long, StreamDefectDataObj> getStreamDefectMap(String streamName,
                                                             List<MergedDefectDataObj> mergedDefectList,
                                                             Set<Long> cidSet) {
        List<MergedDefectIdDataObj> newList = new ArrayList<>();
        List<StreamDefectDataObj> streamDefectList = new ArrayList<>();

        StreamDefectFilterSpecDataObj specDataObj = new StreamDefectFilterSpecDataObj();
        specDataObj.setIncludeDefectInstances(true);
        specDataObj.setIncludeHistory(false);
        specDataObj.setIncludeTotalDefectInstanceCount(true);
        specDataObj.setMaxDefectInstances(10);
        StreamIdDataObj streamIdDataObj = new StreamIdDataObj();
        streamIdDataObj.setName(streamName);
        specDataObj.getStreamIdList().add(streamIdDataObj);

        int count = 0;
        for (MergedDefectDataObj mergedDefect : mergedDefectList) {
            // 如果告警已经存在，则不需要查询告警详情
            Long cid = mergedDefect.getCid();
            if (CollectionUtils.isNotEmpty(cidSet) && cidSet.contains(cid)) {
                continue;
            }

            // 已修复的告警，则不需要查询告警详情
            if (isFixed(mergedDefect)) {
                continue;
            }

            MergedDefectIdDataObj mergedDefectId = new MergedDefectIdDataObj();
            mergedDefectId.setCid(cid);
            mergedDefectId.setMergeKey(mergedDefect.getMergeKey());
            newList.add(mergedDefectId);
            count++;

            // getStreamDefects接口一次最多只能查询100个CID的缺陷实例
            if (count == 100) {
                getStreamDefects(streamName, newList, streamDefectList, specDataObj);
                newList.clear();
                count = 0;
            }
        }

        if (count < 100 && count > 0) {
            getStreamDefects(streamName, newList, streamDefectList, specDataObj);
        }
        log.info("============streamDefectList size: {}", streamDefectList.size());

        Map<Long, StreamDefectDataObj> streamDefectMap = new HashMap<>(streamDefectList.size());
        streamDefectList.forEach(streamDefect -> streamDefectMap.put(streamDefect.getCid(), streamDefect));
        return streamDefectMap;
    }

    public List<StreamDefectDataObj> getStreamDefectList(String streamName, Set<Long> cidSet) {
        List<MergedDefectIdDataObj> newList = new ArrayList<>();
        List<StreamDefectDataObj> streamDefectList = new ArrayList<>();

        StreamDefectFilterSpecDataObj specDataObj = new StreamDefectFilterSpecDataObj();
        specDataObj.setIncludeDefectInstances(true);
        specDataObj.setIncludeHistory(false);
        specDataObj.setIncludeTotalDefectInstanceCount(true);
        specDataObj.setMaxDefectInstances(10);
        StreamIdDataObj streamIdDataObj = new StreamIdDataObj();
        streamIdDataObj.setName(streamName);
        specDataObj.getStreamIdList().add(streamIdDataObj);
        int count = 0;
        for (Long cid : cidSet) {
            MergedDefectIdDataObj mergedDefectId = new MergedDefectIdDataObj();
            mergedDefectId.setCid(cid);
            newList.add(mergedDefectId);
            count++;

            // getStreamDefects接口一次最多只能查询100个CID的缺陷实例
            if (count == 100) {
                getStreamDefects(streamName, newList, streamDefectList, specDataObj);
                newList.clear();
                count = 0;
            }
        }

        if (count < 100 && count > 0) {
            getStreamDefects(streamName, newList, streamDefectList, specDataObj);
        }
        log.info("============streamDefectList size: {}", streamDefectList.size());
        return streamDefectList;
    }

    /**
     * 获取告警详情实例
     *
     * @param streamName
     * @param newList
     * @param streamDefectList
     * @param specDataObj
     */
    private void getStreamDefects(String streamName,
                                  List<MergedDefectIdDataObj> newList,
                                  List<StreamDefectDataObj> streamDefectList,
                                  StreamDefectFilterSpecDataObj specDataObj) {
        log.info("============newList size: {}", newList.size());
        List<StreamDefectDataObj> subStreamDefectList = null;
        try
        {
            subStreamDefectList = _defectService.getStreamDefects(newList, specDataObj);
        }
        catch (CovRemoteServiceException_Exception e)
        {
            log.error("Failed to getStreamDefects: {}, {}\n", streamName, JsonUtil.INSTANCE.toJson(newList), e);
        }
        if (CollectionUtils.isNotEmpty(subStreamDefectList))
        {
            streamDefectList.addAll(subStreamDefectList);
        }
    }

    /**
     * 判断告警是否已经被修复
     *
     * @param mergedDefectDataObj
     * @return
     */
    public boolean isFixed(MergedDefectDataObj mergedDefectDataObj)
    {
        List<DefectStateAttributeValueDataObj> defectList = mergedDefectDataObj.getDefectStateAttributeValues();

        int status = CoverityDefectAttributeUtils.getStatus(defectList);

        log.info("CID: {},  Status:{}", mergedDefectDataObj.getCid(), status);

        if (ComConstants.DefectStatus.FIXED.value() == status)
        {
            return true;
        }

        return false;
    }

    public List<StreamDefectDataObj> getDefectDataObj(long cid, String streamName)
    {
        MergedDefectIdDataObj mergedDefectIdDataObj = new MergedDefectIdDataObj();
        mergedDefectIdDataObj.setCid(cid);

        ArrayList<MergedDefectIdDataObj> listMergedDefectObj = new ArrayList<>();
        listMergedDefectObj.add(mergedDefectIdDataObj);

        StreamDefectFilterSpecDataObj streamDefectFilterSpecDataObj = new StreamDefectFilterSpecDataObj();
        streamDefectFilterSpecDataObj.setIncludeDefectInstances(true);
        streamDefectFilterSpecDataObj.setIncludeHistory(true);

        StreamIdDataObj streamIdDataObj = new StreamIdDataObj();
        streamIdDataObj.setName(streamName);
        streamDefectFilterSpecDataObj.getStreamIdList().add(streamIdDataObj);

        List<StreamDefectDataObj> streamDefects = null;
        try
        {
            streamDefects = _defectService.getStreamDefects(listMergedDefectObj, streamDefectFilterSpecDataObj);
        }
        catch (CovRemoteServiceException_Exception e)
        {
            log.error("Failed to getStreamDefects: {}\n", streamName, e);
        }

        return streamDefects;
    }

    /**
     * 获取文件内容
     * @param streamName
     * @param md5
     * @param filePath
     * @return
     */
    public String getFileContents(String streamName, String md5, String filePath)
    {
        StreamIdDataObj streamIdDataObj = new StreamIdDataObj();
        streamIdDataObj.setName(streamName);

        FileIdDataObj fileIdDataObj = new FileIdDataObj();
        fileIdDataObj.setContentsMD5(md5);
        fileIdDataObj.setFilePathname(filePath);

        try
        {
            FileContentsDataObj fileContents = _defectService.getFileContents(streamIdDataObj, fileIdDataObj);
            byte[] decompress = CompressionUtils.decompress(fileContents.getContents());

            return new String(decompress, StandardCharsets.UTF_8.name());

        }
        catch (CovRemoteServiceException_Exception e)
        {
            log.error("Failed to getFileContents: {}", streamName, e);
        }
        catch (IOException e)
        {
            log.error("Failed to getFileContents: {}", streamName, e);
        }
        return "";
    }

    /**
     * 查询Platform上的告警
     *
     * @param streamName
     * @param filterSpec
     * @return
     * @throws CovRemoteServiceException_Exception
     */
    public List<MergedDefectDataObj> getDefects(String streamName, ProjectScopeDefectFilterSpecDataObj filterSpec)
    {
        ProjectIdDataObj projectId = new ProjectIdDataObj();
        projectId.setName(streamName);

        PageSpecDataObj pageSpec = new PageSpecDataObj();
        pageSpec.setPageSize(100);

        List<MergedDefectDataObj> result = new ArrayList<MergedDefectDataObj>();
        int defectCount = 0;
        MergedDefectsPageDataObj defects = null;

        do
        {
            try
            {
                pageSpec.setStartIndex(defectCount);
                defects = _defectService.getMergedDefectsForProjectScope(projectId, filterSpec, pageSpec);
                result.addAll(defects.getMergedDefects());
                defectCount += defects.getMergedDefects().size();
            }
            catch (CovRemoteServiceException_Exception e)
            {
                log.error("Failed to getMergedDefectsForProjectScope: {}", streamName, e);
            }
        }
        while (defects != null && defectCount < defects.getTotalNumberOfRecords());

        log.info("project [{}] has defects total: {}", streamName, result.size());
        return result;
    }

    /**
     * 获取系统诊断信息
     *
     * @param platformVO
     * @return
     */
    public static String getSystemOverview(PlatformVO platformVO)
    {
        String platformIp = platformVO.getIp();
        String port = platformVO.getPort();
        String userName = platformVO.getUserName();
        String passwd = platformVO.getPasswd();
        String urlStr = String.format("http://%s:%s/diagnostics/overview.json", platformIp, port);
        InputStream stream = null;
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        try
        {
            String author = "Basic " + Base64.getEncoder().encodeToString((userName + ":" + passwd).getBytes());

            URL url = new URL(urlStr);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("Authorization", author);
            urlConnection.connect();

            stream = urlConnection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"), 8);

            StringBuffer rspContent = new StringBuffer();
            String eachLine;
            while ((eachLine = reader.readLine()) != null)
            {
                rspContent.append(eachLine).append("\n");
            }

            System.out.println(rspContent.toString());
            return rspContent.toString();

        }
        catch (IOException e)
        {
            log.error("get coverity system overview exception", e);
            throw new CodeCCException(CoverityMessageCode.GET_COV_PLATFORM_INST_FAIL);
        }
        finally
        {
            if (urlConnection != null)
            {
                urlConnection.disconnect();
            }
            try
            {
                if (stream != null)
                {
                    stream.close();
                }
                if (reader != null)
                {
                    reader.close();
                }
            }
            catch (IOException e)
            {
                log.error("get coverity system overview exception", e);
                throw new CodeCCException(CoverityMessageCode.GET_COV_PLATFORM_INST_FAIL);
            }
        }
    }
}
