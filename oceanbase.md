# OceanBase

## OceanBase与druid

oceanbase商业版有oracle模式，该模式兼容了大部分oracle的语法、function等特性

druid1.2.3版本后支持oceanbase的oracle模式



## mybatis与mysql、oracle、oceanbase

在druid中com.alibaba.druid.util.JdbcUtils中getDbTypeRaw方法中，对配置文件中的url进行了判断，如果是oceanbase:oracle，则为oceanbase-oracle，如果是oceanbase则是oceanbase，后者为mysql模式。



在mybatis中通过databaseId区别数据库厂商标识时，用的是conn（数据库连接）.getDatabaseMetaData().getDataBaseProductName()方法，Oceanbase在实现jdbc的中conn的接口getDataBaseProductName()接口时，通过连接返回的byte数组中的某一位来判断是否为oracle模式并将此状态进行缓存，最终根据结果返回"MySql"或"Oracle"（和MySql和Oracle的返回值相同）。