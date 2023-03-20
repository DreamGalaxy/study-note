# MySQL的一些查询

一、一条语句插入或更新（仅针对mysql）

`ON DUPLICATE KEY UPDATE`

eg：id为主键

```mysql
INSERT INTO T_USER (id, password)
VALUES
('1','password1'),
('2','password2')
ON DUPLICATE KEY UPDATE
password = VALUES(password);
```

但此语句可能会产生自增id跳跃的问题，主要原因在于`innodb_autoinc_lock_mode`

innodb_autoinc_lock_mode 有三种设置 。对于“ 传统 ”，“ 连续 ”或 “ 交错 ”锁定模式，设置分别为0,1或2 。

在mysql8.0前`innodb_autoinc_lock_mode`的默认值是1,8.0后的默认值是2

查询innodb_autoinc_lock_mode

```mysql
show variables like '%autoinc%';
```

参考文章：https://blog.51cto.com/fengfeng688/2141772



对于in查询，查询结果并不是按照in传入的顺序排序，而是按照在数据库中的顺序排序，如果希望按照in传入的顺序排序，需要在sql中加上order by  field(字段, 自定义的值1，值2，值3...)

例如查询用户按照所给id顺序查出

```sql
SELECT id,phone,name
FROM t_user
Where id in (5,1)
ORDER BY FIELD(id, 5, 1);
```

