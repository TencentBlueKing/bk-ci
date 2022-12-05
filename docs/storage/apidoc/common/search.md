# 自定义搜索协议

> 自定义搜索协议用于数据搜索接口，支持前端自定义查询条件、筛选字段、排序规则、分页参数等信息。

[toc]

## 请求体格式

```json
{
  "select": ["xxx", "yyy", "xxx"],
  "page": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "sort": {
    "properties": ["name"],
    "direction": "ASC"
  },
  "rule": {
    "rules": [
      {
        "field": "projectId",
        "value": "test",
        "operation": "EQ"
      },
      {
        "field": "repoName",
        "value": ["generic-local1", "generic-local2"],
        "operation": "IN"
      }
    ],
    "relation": "AND"
  }
}
```

### 请求字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|select|list|否|无，代表查询所有字段|筛选字段列表。支持的字段列表请参照对应数据的列表接口响应字段。|select fields list|
|page|PageLimit|否|pageNumber=1, pageSize=20|分页参数|page limit|
|sort|Sort|否|无|排序规则|sort rule|
|rule|Rule|是|无|自定义查询规则|custom query rule|

### PageLimit参数说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|pageNumber|int|否|1|当前页(第1页开始)|page number|
|pageSize|int|否|20|每页数量|page size|

### Sort参数说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|properties|[string]|是|无|排序字段|sort properties|
|direction|string|否|ASC|排序方向。支持ASC升序和DESC降序|sort direction|

### Rule参数说明

> Rule包含两种格式，一种用于表示嵌套规则NestedRule，另一种用于表示条件规则QueryRule

- **嵌套规则NestedRule**

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|rules|[Rule]|是|无|规则列表，可以任意嵌套NestedRule和QueryRule|rule list|
|relation|string|否|AND|规则列表rules的关系。支持AND、OR、NOR|rule relation|

- **条件规则QueryRule**

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|field|string|是|无|查询字段|filed|
|value|any|否|无|查询值。数据类型和查询操作类型相关|value|
|operation|enum|否|EQ|查询操作类型。枚举类型见下文|operation|


### OperationType查询操作类型

|枚举值|对应查询值类型|Description|
|---|---|---|
|EQ|string/boolean/number/date|等于|
|NE|number/date|不等于|
|LTE|number/date|小于或者等于|
|LT|number/date|小于|
|GTE|number/date|大于或者等于|
|GT|number/date|大于|
|BEFORE|date|在某个时间之间，不包含等于|
|AFTER|date|在某个时间之后，不包含等于|
|IN|list|包含于|
|NIN|list|不包含于|
|PREFIX|string|以xxx为前缀|
|SUFFIX|string|以xxx为后缀|
|MATCH|string|通配符匹配，\*表示匹配任意字符。如\*test\*表示包含test的字符串|
|NULL|null|匹配查询字段为空，filed == null|
|NOT_NULL|null|匹配查询字段不为空，filed != null|

## 响应体格式

响应体参考[分页接口响应格式](../common/common.md?id=统一分页接口响应格式)

``` json
{
  "code": 0,
  "message": null,
  "data": {
    "pageNumber": 0,
    "pageSize": 1,
    "totalRecords": 18,
    "totalPages": 18,
    "records": [
      {
        "xxx": "",
        "yyy": ""
      }
    ]
  },
  "traceId": null
}
```

`records`字段根据`select`筛选字段条件决定