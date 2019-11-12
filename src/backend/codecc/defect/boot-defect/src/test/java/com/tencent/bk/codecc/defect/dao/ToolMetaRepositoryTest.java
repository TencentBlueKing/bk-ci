package com.tencent.bk.codecc.defect.dao;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerPackageRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerRepository;
import com.tencent.bk.codecc.defect.model.CheckerDetailEntity;
import com.tencent.bk.codecc.defect.model.CheckerPackageEntity;
import com.tencent.bk.codecc.defect.model.CovSubcategoryEntity;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.util.JsonUtil;
import org.apache.commons.collections.MapUtils;
import org.json.JSONObject;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//@RunWith(SpringRunner.class)
//SpringBootTest(classes = DefectBootApplication.class)
public class ToolMetaRepositoryTest
{
    //@Autowired
    private CheckerPackageRepository checkerPackageRepository;

    //@Autowired
    private CheckerRepository checkerRepository;

    //@Test
    public void getCovCheckers()
    {
        Jedis jedis177 = new Jedis("9.30.11.178", 6379);
        jedis177.auth("CodeCC_1024@16");

        Map<String, String> sensitivePackage = jedis177.hgetAll("CHECKER_PKG_COVERITY");
        System.out.println("--------------------- sensitive 规则包 ---------------------------");
        List<String> sensitiveList = new ArrayList<>();
        for (String key : sensitivePackage.keySet())
        {
            CheckerPackageEntity entity = JsonUtil.INSTANCE.to(sensitivePackage.get(key), CheckerPackageEntity.class);
            entity.setToolName(ComConstants.Tool.COVERITY.name());
            entity.setPkgId(ComConstants.CheckerPkgKind.getValueByName(entity.getPkgName()));
            sensitiveList.add(JsonUtil.INSTANCE.toJson(entity));
            checkerPackageRepository.save(entity);
        }
        System.out.println(sensitiveList);
    }

    //@Test
    public void uploadCheckers()
    {
        Jedis jedis177 = new Jedis("9.30.11.178", 6379);
        jedis177.auth("CodeCC_1024@16");

        Map<String, String> sensitiveDetail = jedis177.hgetAll("CHECKER_DETAIL_COVERITY");
        System.out.println("---------------------- sensitive 明细 --------------------------");
        List<String> sensitiveLists = new ArrayList<>();
        for(String key : sensitiveDetail.keySet()){
            JSONObject checkerDetailJson = new JSONObject(sensitiveDetail.get(key));
            CheckerDetailEntity entity = JsonUtil.INSTANCE.to(sensitiveDetail.get(key), CheckerDetailEntity.class);
            entity.setToolName(ComConstants.Tool.COVERITY.name());
            entity.setCheckerName(entity.getCheckerKey());
            entity.setPkgKind(ComConstants.CheckerPkgKind.getValueByName(entity.getPkgKind()));
            entity.setLanguage(checkerDetailJson.getInt("codeLang"));
            entity.setNativeChecker(checkerDetailJson.getInt("isNative"));
            if (1 == checkerDetailJson.getInt("covProperty"))
            {
                String subcategoryKey = "CHECKER_SUBCATEGORY:" + key;
                Map<String, String> subCategory = jedis177.hgetAll(subcategoryKey);
                List<CovSubcategoryEntity> covSubcategory = Lists.newArrayList();
                if (MapUtils.isNotEmpty(subCategory))
                {
                    for (Map.Entry<String, String> entry : subCategory.entrySet())
                    {
                        CovSubcategoryEntity covSubcategoryEntity = new CovSubcategoryEntity();
                        JSONObject subcategoryJson = new JSONObject(entry.getValue());
                        covSubcategoryEntity.setCheckerKey(subcategoryJson.getString("checkerName"));
                        covSubcategoryEntity.setCheckerName(subcategoryJson.getString("checkerNameOrig"));
                        covSubcategoryEntity.setLanguage(subcategoryJson.getInt("language"));
                        covSubcategoryEntity.setCheckerSubcategoryDetail(subcategoryJson.getString("checkerSubcategoryDetail"));
                        covSubcategoryEntity.setCheckerSubcategoryName(subcategoryJson.getString("checkerSubcategoryName"));
                        covSubcategoryEntity.setCheckerSubcategoryKey(subcategoryJson.getString("checkerSubcategoryKey"));
                        covSubcategory.add(covSubcategoryEntity);
                    }
                }
                entity.setCovSubcategory(covSubcategory);
            }
            sensitiveLists.add(JsonUtil.INSTANCE.toJson(entity));
            checkerRepository.save(entity);
        }
        System.out.println(sensitiveLists);

    }
}