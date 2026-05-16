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
 package cn.duxinglan.media.impl;


 import java.util.Optional;

 /**
  * TransportContextLookup 用于查找和管理与 ICE（交互式连接建立）相关的 TransportContext 信息。
  * TransportContext 包括本地和远程的端点信息，以及与媒体会话绑定的上下文数据。
  * <p>
  * 此接口的主要目标是通过指定的 ufrag（用户名片段）定位到对应的 TransportContext 实例。
  * Ufrag 通常是用于区分不同媒体会话和映射传输上下文的关键标识符。
  */
 public interface TransportContextLookup {

     /**
      * 根据指定的用户名片段（ufrag）查找对应的 TransportContext 实例。
      * 用户名片段是识别和关联 ICE 传输上下文的重要标识符。
      *
      * @param ufrag 用户名片段，用于标识需要查找的 TransportContext 实例。
      * @return 如果找到匹配的 TransportContext 实例，则返回包含该实例的 Optional；
      * 如果未找到，则返回一个空的 Optional。
      */
     Optional<TransportContext> findByUfrag(String ufrag);
 }
