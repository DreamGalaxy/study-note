# linux命令

## SCP 远程复制命令

scp [-R] 本地服务器文件 root@ip:目标服务器地址

其中-R参数是递归复制文件夹



## 查看硬盘空间

df -TH



## java导出堆栈

jmap -dump:live,format=b,file=output.hprof pid

其中live参数是导出存活对象的意思，所以会导致一次fullgc



## java查看gc情况

jstat -gcutil pid 时间间隔（毫秒）



## java查看线程堆栈，建议多抓几次并导出到文件

jstack pid > xxx.log



## 网络抓包

tcpdump -ni eth0 port xxxx -w yyy.cap

-w参数是导出为wireshark可看的cap格式文件



## 硬盘扩容原有目录

参考文章：[linux新增磁盘或增加磁盘容量后，如何进行扩容 - 知乎 (zhihu.com)](https://zhuanlan.zhihu.com/p/602888861)

[linux系统下添加新硬盘、分区及挂载全过程详解 - 知乎 (zhihu.com)](https://zhuanlan.zhihu.com/p/117651379)



### LVM

LVM(Logical Volume Manager)逻辑卷管理，是一种将一个或多个硬盘的分区在逻辑上集合，相当于一个大硬盘来使用，当硬盘的空间不够使用的时候，可以继续将其它的硬盘的分区加入其中，是一种磁盘空间的动态管理，相对于普通的磁盘分区有很大的灵活性。

先介绍下关于LVM中几个关键术语：

* **PE：**物理区域，PV中可以用于分配的最小存储单元，可以在创建PV的时候指定，如1M, 2M, 4M, 8M, 32M, 64M。组成同一VG中所有PV的PE大小都是相同的。

* **PV：**物理卷，处于LVM最底层，可以是物理硬盘或者分区。

* **VG：**卷组，建立在PV之上，可以含有一个到多个PV。

* **LV：**逻辑卷，建立在VG之上，相当于原来分区的概念。不过大小可以动态改变。



