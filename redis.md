# Redis

非关系型数据库NoSQL（Not Only SQL）的一种 ，内存存储，并且不记录关系，是关系型数据库的一种补充，MongoDB、HBase都是NoSQL， MongoDB适合记录文档信息，图片信息则适合用分布式系统文件如FastDFS集群存储，搜索关键字适合用ES、solr、Lucene等存储。



## 1 Redis-cli基本操作

### 1.1 信息添加

* 功能：设置key，value数据

* 命令 

```Redis
set key value
eg: set name test
```

### 1.2 信息查询

* 功能：根据key查询对应的value，如果不存在，则返回空（nil）

* 命令

```redis
get key
eg: get name
```

### 1.3 清屏

* 功能：清除屏幕中的信息

* 命令

```redis
clear
```

### 1.4 退出

```
quit
exit
<ESC>
```

