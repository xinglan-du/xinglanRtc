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

 import java.util.Map;
 import java.util.Optional;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.function.Supplier;

 /**
  * EndpointFactory 是一个用于创建和管理各种信令端点（Endpoint）的工厂类。
  * 它采用单例模式，确保全局唯一的实例，并提供注册和创建端点的功能。
  *
  * 功能概述：
  * - 提供工厂模式以支持动态创建不同协议类型的信令端点。
  * - 提供线程安全的方式注册和获取端点创建器。
  *
  * 方法说明：
  * - getInstance:
  *   返回该工厂类的单例实例。通过双重检查锁定（DCL）保证线程安全。
  *
  * - registerEndpointCreator:
  *   注册一个新的端点创建器，将指定的协议类型与创建该类型端点的逻辑关联起来。
  *   参数：
  *     - type: 需要注册的协议类型。
  *     - creator: 负责创建该协议类型端点的函数。
  *
  * - create:
  *   根据协议类型动态创建并返回对应的信令端点实例。
  *   如果未找到匹配的创建器，则返回一个空的 Optional。
  *
  * 线程安全性：
  * - 使用 ConcurrentHashMap 存储端点创建器，确保并发环境下的注册和查询操作是线程安全的。
  *
  * 适用场景：
  * - 可用于基于协议类型生成不同信令行为的端点实例，为多协议支持提供扩展性。
  */
 public class EndpointFactory {

     private static volatile EndpointFactory INSTANCE;

     private final Map<ProtocolType, Supplier<Endpoint>> ENDPOINT_CREATORS = new ConcurrentHashMap<>();


     private EndpointFactory() {
     }

     public static EndpointFactory getInstance() {
         if (INSTANCE == null) {
             synchronized (EndpointFactory.class) {
                 if (INSTANCE == null) {
                     INSTANCE = new EndpointFactory();
                 }
             }
         }
         return INSTANCE;
     }

     /**
      * 注册一个端点创建器，用于为指定的协议类型动态生成端点对象。
      *
      * @param type 指定的协议类型，用于标识针对该协议的端点创建逻辑。
      * @param creator 一个提供端点实例的函数，负责根据协议类型生成对应的端点对象。
      */
     public void registerEndpointCreator(ProtocolType type, Supplier<Endpoint> creator) {
         ENDPOINT_CREATORS.put(type, creator);
     }

     /**
      * 根据指定的协议类型创建并返回对应的信令端点实例。
      * 如果未找到与协议类型匹配的创建器，则返回一个空的 Optional。
      *
      * @param type 协议类型，用于确定需要创建的具体端点类型。
      * @return 一个 Optional 包装的信令端点实例；
      *         如果未找到匹配的创建器，则返回 Optional.empty()。
      */
     public Optional<Endpoint> create(ProtocolType type) {
         Supplier<Endpoint> creator = ENDPOINT_CREATORS.get(type);
         if (creator == null) {
             return Optional.empty();
         }
         return Optional.ofNullable(creator.get());
     }

 }
