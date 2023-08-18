# MySQL

## DQL执行顺序

* 编写顺序

```mysql
SELECT				4
	字段列表
FROM				1
	表名列表
WHERE				2
	条件列表
GROUP BY			3
	分组字段列表
HAVING
	分组后续条件列表
ORDER BY			5
	排序字段列表
LIMIT				6
	分页参数
```



* 执行顺序

```mysql
FROM
	表名列表
WHERE
	条件列表
GROUP BY
	分组字段列表
HAVING
	分组后条件列表
SELECT
	字段列表
ORDER BY
	排序字段列表
LIMIT
	分页参数
```



## UNION和UNION ALL

两个查询结果的列必须相同



UNION对两个结果集进行并集操作，不包括重复行，相当于distinct，同时进行默认规则的排序

UNION ALL对两个结果集进行并集操作，包括重复行，也不进行排序

在没有去重的前提下，使用UNION ALL的执行效率要比UNION高



## 事务

事务是一组操作的集合，它是一个不可分割的工作单位，事务会把所有的操作作为一个整体一起向系统提交或撤销操作请求，即这些操作要么同时成功，要么同时失败。



默认MySQL的事务是自动提交的，也就是说，当执行一条DML语句，MySQL会立即隐式地提交事务。



### 1、事务操作

* 查看/设置事务提交方式（全局）

```mysql
SELECT @@autocommit;
-- 0为手动提交，1为自动提交
SET @@autocommit=0;
```

* 开启事务（局部）

```mysql
START TRANSACTION 或 BEGIN;
```

* 提交事务

```sql
COMMIT;
```

* 回滚事务

```mysql
ROLLBACK;
```



### 2、事务的四大特性（ACID）

* 原子性（<font color="red">A</font>tomicity）：事务是不可分割的做小操作单元，要么全部成功，要么全部失败
* 一致性（<font color="red">C</font>onsistency）：事务完成时，必须使所有的数据都保持一致状态
* 隔离性（<font color="red">I</font>solation）：数据库系统提供的隔离机制，保证事务在不受外部并发操作影响的独立环境下运行
* 持久性（<font color="red">D</font>urability）：事务一旦提交或回滚，它对数据库中的数据的改变就是永久的 



### 3、并发事务问题

* 脏读：一个事务读到另外一个事务还没有提交的数据
* 不可重复读：一个事务先后读取同一条记录，但两次读取的数据不同，称之为不可重复读
* 幻读：一个事务按条件查询数据时，没有对应的数据行，但是在插入数据时，又发现这行数据已经存在，好像出现了“幻影”



### 4、事务的隔离级别

| 隔离级别                     | 脏读 | 不可重复读 | 幻读 |
| ---------------------------- | :--: | :--------: | :--: |
| Read uncommitted             |  √   |     √      |  √   |
| Read committed（Oracle默认） |  ×   |     √      |  √   |
| Repeatable Read（MySQL默认） |  ×   |     ×      |  √   |
| Serializable                 |  ×   |     ×      |  ×   |



查看事务隔离级别

```mysql
SELECT @@TRANSACTION_ISOLATION;
```



设置事务隔离级别

```mysql
SET [SESSION|GLOBAL] TRANSACTION ISOLATION LEVEL {READ UNCOMMITTED | READ COMMITTED | REPEATABLED READ | SERIALIZABLE}
```





