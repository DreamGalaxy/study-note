1、UNION和UNION ALL

UNION对两个结果集进行并集操作，不包括重复行，相当于distinct，同时进行默认规则的排序

UNION ALL对两个结果集进行并集操作，包括重复行，也不进行排序

在没有去重的前提下，使用UNION ALL的执行效率要比UNION高



2、sql逻辑运算优先级

算数运算符 > 连接符 > 比较符 > IS [NOT] NULL >= LIKE > [NOT] BETWEEN >NOT > AND > OR



3、查看sql执行计划

EXPLAIN



4、Mybatis一级缓存作用域是SQLSession，开启后第一次执行查询操作后会将结果集写入缓存，如果执行了DML操作（指插入、更新、删除）后，会删除一级缓存的相关内容



5、oracle默认隔离级别是READ COMMITTED ，mysql是Repeatable Read



6、in和exists的区别

* in适合外大内小

* exists适合外小内大

* 内外表数据相当时性能差别不大
* in不会处理null，not in遇到null不会返回数据

* not in和not exists:如果查询语句使用了not in，那么内外表都进行全表扫描，没有用到索引；而not extsts的子查询依然能用到表上的索引。所以无论那个表大，用not exists都比not in要快



7、BETWEEN包含两个边界值



8、count(字段名)不会统计null

count(*)、COUNT(字段名)、COUNT(ROWNUM)、COUNT(1)的区别



9、聚合函数中MAX、MIN、COUNT可以对Date类型进行操作



