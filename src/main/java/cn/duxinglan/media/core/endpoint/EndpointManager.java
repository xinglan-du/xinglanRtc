 /*
  * 版权所有 (c) 2025 www.duxinglan.cn
  *
  * 项目名称：xinglanRtc
  *
  * 本文件属于 xinglanRtc 项目的一部分。
  *
  * 本软件依据 XinglanRtc 非商业许可证（XNCL）授权，仅限个人非商业使用。
  * 禁止任何形式的商业用途，包括但不限于：收费安装、收费部署、
  * 收费运维、收费技术支持等行为。
  *
  * 详情请参阅项目根目录下的 LICENSE 文件。
  */
 package cn.duxinglan.media.core.endpoint;


 import cn.duxinglan.media.core.signaling.ProtocolType;
 import org.jspecify.annotations.NonNull;

 import java.util.Map;
 import java.util.Optional;
 import java.util.concurrent.ConcurrentHashMap;

 /**
  * EndpointManager 类管理和操作信令端点（Endpoint）的生命周期和关联逻辑。
  * 主要负责创建、获取和移除不同协议类型的端点实例。
  * <p>
  * 方法说明：
  * <p>
  * - create：根据指定协议类型创建一个新的端点实例，并将其存储在内部管理的集合中。支持的协议包括 WEBRTC。
  * <p>
  * - get：通过端点 ID 获取对应的端点对象。如果未找到，则返回 null。
  * <p>
  * - remove：根据端点 ID 移除对应的端点对象，并调用端点的关闭方法执行资源清理。
  * <p>
  * 线程安全性：
  * <p>
  * - 使用 `ConcurrentHashMap` 存储端点对象，确保在多线程环境下的线程安全操作。
  */
 public class EndpointManager {

     private final Map<String, Endpoint> endpoints = new ConcurrentHashMap<>();

     /**
      * 根据指定的协议类型创建并返回对应的信令端点实例。
      * 如果端点成功创建，则将其存储在内部维护的集合中。
      *
      * @param protocol 协议类型，用于确定需要创建的具体端点类型。
      * @return 一个 Optional 包装的信令端点实例；
      * 如果端点创建失败，则返回 Optional.empty()。
      */
     public Optional<Endpoint> create(@NonNull ProtocolType protocol) {
         Optional<Endpoint> endpoint = EndpointFactory.getInstance().create(protocol);
         endpoint.ifPresent(ep -> endpoints.put(ep.getId(), ep));
         return endpoint;
     }

     /**
      * 根据指定的端点 ID 获取对应的信令端点实例。
      * 如果集合中不存在对应的端点，则返回 null。
      *
      * @param id 信令端点的唯一标识符，用于查找目标端点对象。
      * @return 对应 ID 的信令端点实例；
      * 如果未找到对应的实例，则返回 null。
      */
     public Endpoint get(String id) {
         return endpoints.get(id);
     }

     /**
      * 根据指定的端点 ID 从管理集合中移除对应的信令端点对象。
      * 如果找到对应的端点，则会调用其关闭方法以释放相关资源。
      *
      * @param id 要移除的信令端点的唯一标识符。
      */
     public void remove(String id) {
         Endpoint ep = endpoints.remove(id);
         if (ep != null) ep.close();
     }


 }
