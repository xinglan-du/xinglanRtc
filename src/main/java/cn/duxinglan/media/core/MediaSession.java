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
 package cn.duxinglan.media.core;

 import cn.duxinglan.media.core.stream.Router;
 import cn.duxinglan.media.protocol.ChannelInRtpPacket;
 import cn.duxinglan.media.protocol.rtcp.RtcpPacket;

 import java.util.List;

 /**
  * 表示媒体会话的接口。
  * 定义了获取会话标识的方法，用于标识具体的媒体会话实例。
  */
 public interface MediaSession {


     String getSessionId();

     void onRtpPacket(ChannelInRtpPacket rtpPacket);

     void addRouter(Router router);

     void addChannelContext(ChannelContext channelContext);

     void removeChannelContext(ChannelContext channelContext);

     void onRtcpPacket(List<RtcpPacket> rtcpPackets);
 }
