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


 import cn.duxinglan.media.core.consumer.Consumer;
 import cn.duxinglan.media.core.stream.MediaStreamControl;
 import cn.duxinglan.media.impl.webrtc.MediaLineInfo;
 import cn.duxinglan.media.protocol.rtcp.PsFbRtcpPacket;
 import cn.duxinglan.media.protocol.rtcp.RtcpPacket;
 import cn.duxinglan.media.protocol.rtcp.RtcpPayloadType;
 import cn.duxinglan.media.protocol.rtp.RtpPacket;
 import cn.duxinglan.sdp.entity.type.MediaInfoType;
 import lombok.Getter;
 import lombok.extern.slf4j.Slf4j;

 @Slf4j
 public class WebrtcConsumer implements Consumer {


     @Getter
     private final MediaInfoType mediaInfoType;

     @Getter
     private final String mediaId;

     @Getter
     private final MediaLineInfo.Info info;

     @Getter
     private final long[] ssrcs;

     private final ConsumerEvent consumerEvent;

     private MediaStreamControl mediaStreamControl;

     public WebrtcConsumer(MediaInfoType mediaInfoType,
                           String mediaId,
                           MediaLineInfo.Info info,
                           ConsumerEvent consumerEvent) {
         this.mediaInfoType = mediaInfoType;
         this.mediaId = mediaId;
         this.info = info;
         this.consumerEvent = consumerEvent;
         this.ssrcs = this.info.getSsrcMap().keySet().stream()
                 .mapToLong(Long::longValue)
                 .toArray();
     }

     @Override
     public void onRtpPacket(RtpPacket packet) {
         RtpPacket rtpPacket = new RtpPacket();
         rtpPacket.setVersion(packet.getVersion());
         rtpPacket.setPadding(packet.getPadding());
         rtpPacket.setExtension(packet.getExtension());
         rtpPacket.setCsrcCount(packet.getCsrcCount());
         rtpPacket.setMarker(packet.getMarker());
         rtpPacket.setPayloadType(packet.getPayloadType());
         rtpPacket.setSequenceNumber(packet.getSequenceNumber());
         rtpPacket.setTimestamp(packet.getTimestamp());
         rtpPacket.setSsrc(ssrcs[0]);
         rtpPacket.setPayload(packet.getPayload().slice());
         consumerEvent.onConsumerRtpPacket(rtpPacket);
     }

     @Override
     public void addMediaStreamControl(MediaStreamControl mediaStreamControl) {
         this.mediaStreamControl = mediaStreamControl;
     }

     @Override
     public void onRtcpPacket(RtcpPacket rtcpPacket) {
         if (rtcpPacket instanceof PsFbRtcpPacket psFbRtcpPacket) {
             if (psFbRtcpPacket.getPayloadType() == RtcpPayloadType.PSFB.value
                     && psFbRtcpPacket.getFmt() == 1
                     && this.mediaStreamControl != null) {
                 this.mediaStreamControl.onPLI();
             }
         }

     }
 }
