# Kubernetes

minikube命令

```shell
# 启动
minikube start --image-mirror-country=cn --hyperv-virtual-switch="minikubaSwitch"

# 停止
minikube stop

# dashboard
minikube dashboard

# 查看状态
minikube status
```



docker命令

```shell
# 查询镜像
docker search mysql

# tag 为版本号，不填默认为 latest
docker pull mysql:tag

# 查看镜像
docker images

# 运行容器，参数见下表
docker run -it -d --name name -p port:port -v path:path -v file:file -e env=value image:tag /bin/bash 

# 查看运行中的容器
docker ps

# 登陆容器
docker exec -it 容器名称或ID /bin/bash
```



| 参数        | 解析                                              |
| ----------- | ------------------------------------------------- |
| -it         | 与容器进行交互式启动                              |
| -d          | 守护式运行                                        |
| --name      | 给容器的别名                                      |
| -p          | 端口映射，[宿主端口:容器端口]                     |
| -v          | 容器挂载点，[挂载的宿主目录或文件:容器目录或文件] |
| -e          | 给容器的环境变量，[变量名=变量值]                 |
| [image:tag] | 要运行的镜像版本                                  |
| [/bin/bash] | 交互路径                                          |



## 一、dockefile

dockerfile中编写了容器初始化时的操作



FROM 指定容器创建时的基础运行环境：即用安装好了相关基础环境的容器，可以从docker官网搜索

```dockerfile
FROM openjdk:8-jdk-slim
```



LABLE 可以指定额外信息，例如maintainer=xxx可以指定作者为xxx

```dockerfile
LABEL maintainer=xxx
```



COPY 将相关文件拷贝至指定目录

```dockerfile
COPY target/xxx.jar /xxx.jar
```



ENTRYPOINT/CMD 指定程序的启动命令，即容器启动完成后要执行哪些命令

```dockerfile
ENTRYPOINT ["java" "-jar" "xxx.jar"]
```

 

ENTRYPOINT和CMD的区别：

* docker run 镜像名 参数，这里的参数会覆盖掉默认的CMD的内容

* 和CMD类似，默认的ENTRYPOINT也在docker run时, 也可以被覆盖. 在运行时, 用--entrypoint覆盖默认的ENTRYPOINT

如果你希望你的docker镜像的功能足够灵活， 建议在Dockerfile里调用CMD命令；

如果你希望docker镜像只执行一个具体程序，不希望用户在执行docker run的时候随意覆盖默认程序，则使用ENTRYPOINT



shell表示法和exec表示法的区别：

ENTRYPOINT和CMD指令都支持2种不同的写法，eg：

```dockerfile
CMD java -jar xxx.jar
CMD ["java" "-jar" "xxx.jar"]
```

* 当使用shell表示法时，命令行程序作为sh程序的子程序运行，<font color="red">PID为1的进程并不是在Dockerfile里面定义的ping命令，而是/bin/sh命令</font>，此外如果从外部发送任何POSIX信号到docker容器，由于/bin/sh命令不会转发消息给实际运行的ping命令, 则不能安全得关闭docker容器（例如ping的例子中，如果用了shell形式的CMD，用户按ctrl-c也不能停止ping命令，因为ctrl-c的信号没有被转发给ping命令）
* 如果build的docker镜像连shell程序都可以没有，shell的表示法没办法满足这个要求，如果镜像里面没有/bin/sh，docker容器就不能运行
* exec表示法会直接运行命令，且命令的PID是1
* 所以<font color="red">强烈建议采用exec表示法</font>



dockerfile样例如下：

```dockerfile
FROM openjdk:8-jdk-slim
LABEL maintainer=xxx

COPY target/xxx.jar /xxx.jar

ENTRYPOINT ["java" "-jar" "xxx.jar"]
```



构建docker镜像：

```shell
# 当dockerfile就叫Dockerfile时，可以省略-f参数，此外最后的.不能丢，代表了当前目录的含义，对于COPY命令的源地址，其实就是从该目录开始的相对目录
docker build -t 镜像标签名 -f 使用的dockerfile .
```



启动镜像：

```shell
# -d参数表示后台启动，-p参数能进行端口映射，否则容器端口不会映射到主机端口
docker run -d -p 端口:映射到主机的端口 镜像标签名
```



## 二、Kubernetes的优势

- **服务发现和负载均衡**

  Kubernetes 可以使用 DNS 名称或自己的 IP 地址来暴露容器。 如果进入容器的流量很大， Kubernetes 可以负载均衡并分配网络流量，从而使部署稳定。

- **存储编排**

  Kubernetes 允许你自动挂载你选择的存储系统，例如本地存储、公共云提供商等。

- **自动部署和回滚**

  你可以使用 Kubernetes 描述已部署容器的所需状态， 它可以以受控的速率将实际状态更改为期望状态。 例如，你可以自动化 Kubernetes 来为你的部署创建新容器， 删除现有容器并将它们的所有资源用于新容器。

- **自动完成装箱计算**

  你为 Kubernetes 提供许多节点组成的集群，在这个集群上运行容器化的任务。 你告诉 Kubernetes 每个容器需要多少 CPU 和内存 (RAM)。 Kubernetes 可以将这些容器按实际情况调度到你的节点上，以最佳方式利用你的资源。

- **自我修复**

  Kubernetes 将重新启动失败的容器、替换容器、杀死不响应用户定义的运行状况检查的容器， 并且在准备好服务之前不将其通告给客户端。

- **密钥与配置管理**

  Kubernetes 允许你存储和管理敏感信息，例如密码、OAuth 令牌和 ssh 密钥。 你可以在不重建容器镜像的情况下部署和更新密钥和应用程序配置，也无需在堆栈配置中暴露密钥。（类似Nacos的配置中心，但nacos的配置中心可以做到热更新，而k8s只能在启动时配置）



## 三、Kubernetes基础概念

### 1、控制平面组件（Control Plane Components）

控制平面的组件对集群做出全局决策(比如调度)，以及检测和响应集群事件（例如，当不满足部署的replicas字段时，启动新的pod)。

控制平面组件可以在集群中的任何节点上运行。然而，为了简单起见，设置脚本通常会在同一个计算机上启动所有控制平面组件，并且不会在此计算机上运行用户容器。



#### 1.1 kube-apiserver

API服务器是Kubernetes控制面的组件，该组件公开了Kubernetes API。API 服务器是Kubernetes控制面的前端

Kubernetes API服务器的主要实现是kube-apiserver。kube-apiserver设计上考虑了水平伸缩，也就是说，他可以通过多个部署实例进行伸缩，并且在这些实例之间平衡流量



#### 1.2 etcd

etcd是兼具一致性和高可用性的分布式键值数据库，可以作为保存Kubernetes所有数据集群的后台数据的数据库。

k8s选择etcd数据库的原因：

1. etcd具有一致性和高可用性
2. 使用内存存储，支持异步复制
3. 与Kubernetes APIserver的数据访问模式非常匹配，支持watch机制，可以高效地进行事件通知，提供实时数据变更通知，而Redis不支持watch操作
4. etcd和Kubernetes都是go语言编写的，可以更好地整合
5. etcd是开源的，可以获得更多的社区支持和贡献



#### 1.3 kube-scheduler

控制平面组件，负责监视新创建的、未指定运行节点（node）的Pods，选择节点让Pod在上面运行。

调度决策考虑的因素包括单个Pod和Pod集合的资源需求、硬件/软件/策略约束、亲和性和反亲和性规范、数据位置、工作负载时间的干扰和最后时限。



#### 1.4 kube-controller-manager

在主节点上运行控制器的组件

从逻辑上讲，每个控制器都是一个单独的进程，但是为了降低复杂性，它们都被编译到同一个可执行文件，并在一个进程中运行。

这些控制器包括：

* 节点控制器（Node Controller）：负责在节点出现故障时进行通知和响应
* 任务控制器（Job Controller）：监测代表一次性任务的Job对象，然后创建Pods来运行这些任务直到完成
* 端点控制器（Endpoints Controller）：填充端点（Endpoints）对象（即加入Service与Pod）
* 服务账户和令牌控制器（Service Account & Token Controllers）：为新的命名空间创建默认账户和API访问令牌



## 四、Kubernetes的使用

### 1、资源创建方式

* 命令行
* yaml：通过kubectl apply -f xxx.yml执行



### 2、Namespace

名称空间，用来对资源集群进行隔离划分。默认只隔离资源，不隔离网络。下图中各个命名空间的应用只能引用到该命名空间内的配置

![image-20230514101954019](image\image-20230514101954019.png)



<font color="red">**注意**</font>：在创建资源时如果没有指定命名空间，默认的命名空间是default



#### 2.1 查看命名空间

ns是namespace的简称

```shell
kubectl get ns
```



#### 2.2 创建和删除命名空间

命令行方式：

```shell
# ns是namespace的简称
# 创建和删除命名空间
kubectl create ns hello
kubectl delete ns hello
```

yml方式：通过kubectl apply -f xxx.yml应用配置

```yaml
apiVerion: v1
kind: Namespace
metadata:
	name: hello
```



### 3、Pod

运行中的**一组**容器，Pod是Kubernetes中应用的最小单位

同一个pod内的容器间共享网络空间和存储



#### 3.1 查看pod信息

pod是pods的简写

```shell
# 查看default命名空间下的pods
kubectl get pods

# 查看所有命名空间下的pods
kubectl get pods -A

# 查看指定命名空间下的pods
kubectl get pods -n 命名空间

# 查看pod的具体信息、状态等
kubectl describe pod pod名

# 查看容器内的打印日志，-f参数表示持续输出
kubectl logs [-f] pod名

# 每个Pod k8s都会分配一个ip，查看ip
kubectl get pod -owide
# 可以通过该pod的ip+对应服务的端口进行访问

# 记录pod的状态变化
kubectl get pods -w

# 展示pod的标签
kubectl get pod --show-labels
```



#### 3.2 创建pod

```shell
kubectl run pod名称 --image=镜像
```



```yaml
apiVersion: v1
kind: Pod
metadata:
	labels:
		run: pod名称
	name: pod名称
spec:
	# 因为一个pod中可以有多个容器，所以这里可以写为数组的形式
	containers:
	- image: 镜像1
	  name: 容器1的名字（可以与pod名不同，使用的时候都是pod名） 
    - image: 镜像2
	  name: 容器2的名字
```



#### 3.3 删除pod

```shell
kubectl delete pod [-n 命名空间] pod名 [pod2名]
```



通过yaml创建也可以通过该yaml删除

```shell
kubectl delete -f yaml名
```



#### 3.4 进入容器中

```shell
kubectl exec -it pod名 -- /bin/bash
```



```shell
# 退出容器
exit
```



### 4、deployment

控制pod，使pod拥有多副本、自愈、扩缩容等能力

* 自愈：当工作负载中的pod down了，k8s会自动拉起一个新的pod

* 故障转移：当某台主机宕机后，会将该主机上的服务在其他可用主机上重新拉起一份（有相关的阈值配置）



#### 4.1 查看deployment

可以用deploy简写deployment

```shell
# 查看default命名空间下的工作负载
kubectl get deploy

# 查看yaml
kubectl get deploy/工作负载名 -oyaml
```



#### 4.2 创建deployment

```shell
kubectl create deployment 工作负载名 --image=镜像名 --repicas=副本数
```



#### 4.3 删除deployment

```shell
kubectl delete deploy 工作负载名
```



#### 4.4 扩缩容

```shell
kubectl scale --replicas=新的副本数量 deploy/工作负载名
```



修改yml方式，进入后修改`replicas`属性

```shell
kubectl edit deploy 工作负载名称
```



#### 4.5 滚动更新

```shell
# --record表示记录该次更新
kubectl set image deployment/工作负载名 镜像名（对应创建时yml中的）=镜像:其版本 --record
kubectl rollout status deployment/工作负载名

# eg:
kubectl set image deployment/my-dep nginx=nginx:1.16.1 --record
```



#### 4.6 版本回退

```shell
# 查看历史记录
kubectl rollout history deploy/工作负载名

# 查看某次历史详情
kubectl rollout history deploy/工作负载名 --revision=记录号

# 回滚（默认回滚到上次）
kubectl rollout undo deploy/工作负载名

# 回滚（回滚到指定版本）
kubectl rollout undo deploy/工作负载名 --to-revision=记录号
```



### 5、其他工作负载

除了deployment，k8s还有`StatefulSet`、`DaemonSet`、`Job`等类型资源，都称为`工作负载`。

有状态应用使用`StatefulSet`部署，无状态应用使用`deployment`部署



![image-20230514122043741](image\image-20230514122043741.png)



### 6、Service

Pod的服务发现与负载均衡

可以通过访问域名的方式访问Service，**域名规则为**：`服务名.所在命名空间.svc`

暴露deploy，集群内使用Service的ip:port就可以负载均衡地访问服务



#### 6.1 查看serveice

命令中service可以简写为svc

```shell
kubectl get svc
```



#### 6.2 创建service

NodePort默认暴露30000-32767之间的端口

```shell
# 默认为--type=ClusterIP（只能在集群内访问）
kubectl expose deployment 工作负载名 --port=对外暴露端口 --target-port=目标服务端口

# 集群外也可以访问（公网）
kubectl expose deployment 工作负载名 --port=对外暴露端口 --target-port=目标服务端口 --type=NodePort

# 使用标签检索pod

```



```yaml
apiVersion: v1
kind: Service
metadata:
	labels:
		app: 标签名
	name: pod名
spec:
	selector:
		app: 要选择的容器的标签名
	ports:
	- port: 对外暴露的端口
	  protocol: TCP
	  targetPort: 目标服务的端口
```



#### 6.3 将端口暴露到主机（win10）

```shell
kubectl port-forward service/服务名 win10的端口:service的端口
```



### 7、Ingress

Ingress：Service的统一网关入口，将流量负载均衡到各个Service，Service再负载均衡到各个工作负载

* 转发
* 路径重写
* 限流



<img src="image\image-20230514184307402.png" alt="image-20230514184307402" style="zoom:50%;" />



官方教程：https://kubernetes.github.io/ingress-nginx/



### 8、存储抽象
