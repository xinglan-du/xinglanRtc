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
 package cn.duxinglan.media.impl.v1;

 import cn.duxinglan.media.impl.webrtc.WebrtcNodeDataType;

 /**
  * 表示WebRTC节点数据的模型类。
  * <p>
  * 该类是一个不可变的记录，用于在WebRTC通信中封装节点数据的信息。
  * 它包含两部分内容：节点数据的类型以及具体的数据内容，用于描述不同类型的信令或消息。
  * <p>
  * 类型字段使用WebrtcNodeDataType枚举表示，用于区分不同的WebRTC信令类型，
  * 如节点的Offer或Answer等。
  * 数据字段为任意类型的对象，表示与特定信令类型相关联的具体内容。
  * <p>
  * 这个类通常作为信令系统的一部分，支持在WebRTC节点之间传递结构化数据，
  * 如通过信令连接发送Offer或Answer，同时携带相关的配置信息或描述。
  * <p>
  * 属性列表：
  * - type: 表示WebRTC节点数据的类型，使用WebrtcNodeDataType枚举定义。
  * - data: 表示与该类型相关联的具体数据，可以是任意类型的对象。
  */
 public record WebrtcNodeData(WebrtcNodeDataType type, Object data) {
 }
