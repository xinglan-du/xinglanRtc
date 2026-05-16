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


 import cn.duxinglan.media.core.MediaSession;
 import cn.duxinglan.media.protocol.ChannelInRtpPacket;
 import cn.duxinglan.media.protocol.rtp.RtpFactory;
 import cn.duxinglan.media.protocol.rtp.RtpPacket;
 import cn.duxinglan.media.transport.udp.InboundPacket;
 import io.netty.buffer.ByteBuf;
 import lombok.extern.slf4j.Slf4j;
 import org.jspecify.annotations.NonNull;

 @Slf4j
 public class RtpProcessor {

     private final MediaSession session;

     public RtpProcessor(MediaSession session) {
         this.session = session;
     }

     public void receiveProcess(@NonNull InboundPacket data, ByteBuf decrypt) {
         RtpPacket rtpPacket = RtpFactory.parseBytebufToRtpPacket(decrypt);
         ChannelInRtpPacket channelInRtpPacket = new ChannelInRtpPacket();
         channelInRtpPacket.setRtpPacket(rtpPacket);
         channelInRtpPacket.setInboundPacket(data);
         session.onRtpPacket(channelInRtpPacket);
     }

     public void sendProcess(RtpPacket rtpPacket, ByteBuf buffer) {
         byte one = (byte) ((rtpPacket.getVersion() & 0x03) << 6 | (rtpPacket.getPadding() & 0x01) << 5 | (rtpPacket.getExtension() & 0x01) << 4 | (rtpPacket.getCsrcCount() & 0x0F));
         byte two = (byte) (((rtpPacket.getMarker() & 0x01) << 7) | (rtpPacket.getPayloadType() & 0x7F));
         buffer.writeByte(one);
         buffer.writeByte(two);
         buffer.writeShort(rtpPacket.getSequenceNumber());
         buffer.writeInt((int) (rtpPacket.getTimestamp() & 0xFFFFFFFFL));
         buffer.writeInt((int) (rtpPacket.getSsrc() & 0xFFFFFFFFL));
//        buffer.writeInt((int) (rtpPackage.getSsrc() & 0xFFFFFFFFL));
         ByteBuf payload = rtpPacket.getPayload().slice();
         buffer.writeBytes(payload);

     }
 }
