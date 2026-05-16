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
 package cn.duxinglan.media.core.producer;


 import cn.duxinglan.media.core.stream.MediaStream;
 import cn.duxinglan.media.core.stream.MediaStreamControl;
 import cn.duxinglan.media.protocol.ChannelInRtpPacket;
 import cn.duxinglan.media.protocol.rtcp.RtcpPacket;
 import cn.duxinglan.media.transport.udp.BasePacket;

 public interface Producer extends MediaStream, MediaStreamControl {

     //获取当前流要处理的所有ssrc
     long[] getSsrc();


     void onRtpPacket(ChannelInRtpPacket rtpPacket);

     void onRtcpPacket(RtcpPacket rtcpPacket);


     public interface ProducerEvent {

         void onRtcpPacket(BasePacket packet);
     }
 }
