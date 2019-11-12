package com.tencent.bk.codecc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.bk.codecc.defect.DefectBootApplication;
import com.tencent.bk.codecc.defect.dao.mongorepository.JobInstanceRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.JobTestRepository;
import com.tencent.bk.codecc.defect.model.JobInstanceEntity;
import com.tencent.bk.codecc.defect.model.LintDefectEntity;
import com.tencent.bk.codecc.defect.model.LintFileEntity;
import com.tencent.bk.codecc.defect.vo.LintFileVO;
import com.tencent.devops.common.api.util.UUIDUtil;
import com.tencent.devops.common.auth.AuthExAutoConfiguration;
import com.tencent.devops.common.auth.api.external.BkAuthExPermissionApi;
import com.tencent.devops.common.auth.api.external.BkAuthExRegisterApi;
import com.tencent.devops.common.auth.api.pojo.external.BkAuthExAction;
import com.tencent.devops.common.auth.api.pojo.external.model.BkAuthExResourceActionModel;
import com.tencent.devops.common.service.Profile;
import com.tencent.devops.common.web.WebAutoConfiguration;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {AuthExAutoConfiguration.class, Profile.class, WebAutoConfiguration.class, DefectBootApplication.class})
public class DefectBootApplicationTest {

    @Autowired
    private BkAuthExPermissionApi bkAuthExPermissionApi;

    @Autowired
    private BkAuthExRegisterApi bkAuthExRegisterApi;

    @Autowired
    private JobInstanceRepository jobInstanceRepository;

    @Autowired
    private ObjectMapper objectMapper;


//    @Test
    public void registerResourceTest()
    {
        Boolean result = bkAuthExRegisterApi.registerCodeCCTask(
                "brooklin", "12315", "测试任务", "a68"
        );
        Assert.assertTrue(result);
    }

//    @Test
    public void deleteResourceTest()
    {
        Boolean result = bkAuthExRegisterApi.deleteCodeCCTask(
                "12312", "a72"
        );
        Assert.assertTrue(result);
    }


//    @Test
    /*public void queryUserListTest()
    {
        List<String> userList = bkAuthExPermissionApi.queryUserListForAction(
                "12312", "a68", new ArrayList<BkAuthExAction>(){{
                    add(BkAuthExAction.TASK_MEMBER);
                }});
        Assert.assertEquals("neildwu", userList.get(0));
    }*/


//    @Test
    public void validateBatchPermissionTest()
    {
        List<BkAuthExResourceActionModel> resultList = bkAuthExPermissionApi.validateBatchPermission(
                "neildwu", "12312", "a70", new ArrayList<BkAuthExAction>(){{
//                    add(BkAuthExAction.ADMIN_MEMBER);
                    add(BkAuthExAction.TASK_MEMBER);
                    add(BkAuthExAction.TASK_OWNER);
            }});
        Predicate<List<BkAuthExResourceActionModel>> predicate = (predicateList) ->
        {
            for(BkAuthExResourceActionModel element : predicateList)
            {
                if(!element.isPass())
                {
                    return false;
                }
            }
            return true;
        };
        Assert.assertTrue(predicate.test(resultList));
    }


//    @Test
    public void queryTaskList() throws JsonProcessingException
    {
        Set<Long> taskId = bkAuthExPermissionApi.queryResourceListForUser("yunyaoyang", "a74",
                Arrays.asList(BkAuthExAction.TASK_MEMBER, BkAuthExAction.TASK_OWNER));
        System.out.println(objectMapper.writeValueAsString(taskId));
    }

//    @Test
    public void deserializeTest() throws IOException
    {
        LintFileEntity lintFileInfoEntity = new LintFileEntity();
        lintFileInfoEntity.setRelPath("123");
        LintDefectEntity lintDefectEntity = new LintDefectEntity();
        lintDefectEntity.setAuthor("hihihi");
        lintDefectEntity.setChecker("checker123");
        lintFileInfoEntity.setDefectList(new ArrayList<LintDefectEntity>(){{
            add(lintDefectEntity);
        }});
        String jsonStr = objectMapper.writeValueAsString(lintFileInfoEntity);
        LintFileVO lintFileInfoVO = objectMapper.readValue(jsonStr, LintFileVO.class);
        System.out.println(lintFileInfoVO.getDefectList());
    }



//    @Test
    /*public void insertLintFileEntity()
    {
        LintFileEntity lintFileEntity = new LintFileEntity();
        lintFileEntity.setTaskId(1024L);
        lintFileEntity.setToolName("CPPLINT");
        lintFileEntity.setFilePath("/data/iegci/multi_tool_code_resource_2/CodeCC_Demo_eslint/brook.js");
        lintFileEntity.setFileUpdateTime(System.currentTimeMillis());
        lintFileEntity.setAnalysisVersion("18");
        lintFileEntity.setStatus(1);
        lintFileEntity.setFixedTime(System.currentTimeMillis());
        lintFileEntity.setExcludeTime(System.currentTimeMillis());
        lintFileEntity.setDefectCount(7);
        lintFileEntity.setNewCount(5);
        lintFileEntity.setHistoryCount(2);
        lintFileEntity.setUrl("svn+ssh://brooklin@tc-svn.tencent.com/codecc/test_project_proj/trunk/OpenSource/nw.js-nw26/brook.js");
        lintFileEntity.setRepoId("");
        lintFileEntity.setRevision("18");
        lintFileEntity.setBranch("");
        lintFileEntity.setRelPath("/nw.js-nw26/src/resources/nwapp/background.js");
        lintFileEntity.setSubModule("");
        lintFileEntity.setAuthorList(new HashSet<String>(){{
            add("v_rjliu");
        }});
        lintFileEntity.setCheckerList(new HashSet<String>(){{
            add("space-before-function-paren");
            add("array-bracket-spacing");
            add("curly");
            add("no-redeclare");
            add("no-unused-vars");
            add("object-curly-newline");
            add("one-var");
            add("no-inner-declarations");
            add("semi");
            add("quotes");
            add("no-extra-semi");
            add("nonblock-statement-body-position");
            add("no-undefined");
            add("key-spacing");
            add("eqeqeq");
            add("object-curly-spacing");
            add("no-param-reassign");
        }});

        LintDefectEntity lintDefectEntity1 = new LintDefectEntity();
        lintDefectEntity1.setDefectId(1L);
        lintDefectEntity1.setLineNum(16);
        lintDefectEntity1.setAuthor("v_rjliu");
        lintDefectEntity1.setChecker("eqeqeq");
        lintDefectEntity1.setSeverity(2);
        lintDefectEntity1.setMessage("Expected '!==' and instead saw '!='.");
        lintDefectEntity1.setDefectType(1);
        lintDefectEntity1.setStatus(1);
        lintDefectEntity1.setLineUpdateTime(System.currentTimeMillis());

        LintDefectEntity lintDefectEntity2 = new LintDefectEntity();
        lintDefectEntity2.setDefectId(1L);
        lintDefectEntity2.setLineNum(28);
        lintDefectEntity2.setAuthor("v_rjliu");
        lintDefectEntity2.setChecker("nonblock-statement-body-position");
        lintDefectEntity2.setSeverity(4);
        lintDefectEntity2.setMessage("Expected no linebreak before this statement.");
        lintDefectEntity2.setDefectType(1);
        lintDefectEntity2.setStatus(1);
        lintDefectEntity2.setLineUpdateTime(System.currentTimeMillis());


        LintDefectEntity lintDefectEntity3 = new LintDefectEntity();
        lintDefectEntity3.setDefectId(1L);
        lintDefectEntity3.setLineNum(4);
        lintDefectEntity3.setAuthor("v_rjliu");
        lintDefectEntity3.setChecker("no-unused-vars");
        lintDefectEntity3.setSeverity(2);
        lintDefectEntity3.setMessage("'nwNative' is assigned a value but never used.");
        lintDefectEntity3.setDefectType(1);
        lintDefectEntity3.setStatus(1);
        lintDefectEntity3.setLineUpdateTime(System.currentTimeMillis());
//
//
        LintDefectEntity lintDefectEntity4 = new LintDefectEntity();
        lintDefectEntity4.setDefectId(1L);
        lintDefectEntity4.setLineNum(7);
        lintDefectEntity4.setAuthor("v_rjliu");
        lintDefectEntity4.setChecker("no-unused-vars");
        lintDefectEntity4.setSeverity(2);
        lintDefectEntity4.setMessage("'Event' is assigned a value but never used.");
        lintDefectEntity4.setDefectType(1);
        lintDefectEntity4.setStatus(1);
        lintDefectEntity4.setLineUpdateTime(System.currentTimeMillis());


        LintDefectEntity lintDefectEntity5 = new LintDefectEntity();
        lintDefectEntity5.setDefectId(1L);
        lintDefectEntity5.setLineNum(9);
        lintDefectEntity5.setAuthor("v_rjliu");
        lintDefectEntity5.setChecker("space-before-function-paren");
        lintDefectEntity5.setSeverity(4);
        lintDefectEntity5.setMessage("Unexpected space before function parentheses.");
        lintDefectEntity5.setDefectType(1);
        lintDefectEntity5.setStatus(1);
        lintDefectEntity5.setLineUpdateTime(System.currentTimeMillis());


        lintFileEntity.setDefectList(new ArrayList<LintDefectEntity>(){{
            add(lintDefectEntity1);
            add(lintDefectEntity2);
            add(lintDefectEntity3);
//            add(lintDefectEntity4);
//            add(lintDefectEntity5);
        }});

        lintDefectRepository.save(lintFileEntity);

    }*/


    /*@Test
    public void saveJobInstance(){
        JobInstanceEntity jobInstanceEntity = new JobInstanceEntity();
        String cronExpression = "0/30 * * * * ?";
        String key = UUIDUtil.INSTANCE.generate() + "_" + DigestUtils.md5Hex(cronExpression);
        jobInstanceEntity.setJobName(key);
        jobInstanceEntity.setTriggerName(key);
        jobInstanceEntity.setClassUrl("http://10.123.24.177:9994/downloadFile?fileName=CreateTaskScheduleTask.java");
        jobInstanceEntity.setClassName("CreateTaskScheduleTask");
        jobInstanceEntity.setCronExpression(cronExpression);
        Map<String, Object> jobCustomParam = new HashMap<>();
        jobCustomParam.put("gitPrivateToken", "USKzsIc-tSXHEixK8ENk");
        jobCustomParam.put("gitCodePath", "http://git.code.oa.com");
        jobInstanceEntity.setJobParam(jobCustomParam);
        jobInstanceEntity.setCreatedBy("sysadmin");
        jobInstanceEntity.setCreatedDate(System.currentTimeMillis());
        jobInstanceEntity.setUpdatedBy("sysadmin");
        jobInstanceEntity.setUpdatedDate(System.currentTimeMillis());
        jobInstanceRepository.save(jobInstanceEntity);
    }*/




}