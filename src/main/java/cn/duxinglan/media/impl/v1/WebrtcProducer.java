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


 import cn.duxinglan.media.core.producer.Producer;
 import cn.duxinglan.media.core.stream.MediaSink;
 import cn.duxinglan.media.impl.webrtc.MediaLineInfo;
 import cn.duxinglan.media.protocol.ChannelInRtpPacket;
 import cn.duxinglan.media.protocol.rtcp.*;
 import cn.duxinglan.media.protocol.rtp.RtpPacket;
 import cn.duxinglan.sdp.entity.rtp.FmtpAttributes;
 import cn.duxinglan.sdp.entity.rtp.RtpPayload;
 import cn.duxinglan.sdp.entity.type.MediaInfoType;
 import lombok.Getter;
 import lombok.extern.slf4j.Slf4j;

 import java.util.List;
 import java.util.concurrent.CopyOnWriteArrayList;

 @Slf4j
 public class WebrtcProducer implements Producer {

     @Getter
     private final MediaInfoType mediaInfoType;

     @Getter
     private final String mediaId;

     @Getter
     private final MediaLineInfo.Info info;

     private final long[] ssrcs;

     private final List<MediaSink> sinks = new CopyOnWriteArrayList<>();

     private final ProducerEvent producerEvent;

     private final InboundRtpStream inboundRtpStream = new InboundRtpStream();

     public WebrtcProducer(MediaInfoType mediaInfoType, String mediaId, MediaLineInfo.Info info, ProducerEvent producerEvent) {
         this.mediaInfoType = mediaInfoType;
         this.mediaId = mediaId;
         this.info = info;
         this.producerEvent = producerEvent;
         this.ssrcs = this.info.getSsrcMap().keySet().stream()
                 .mapToLong(Long::longValue)
                 .toArray();
     }

     @Override
     public long[] getSsrc() {
         return ssrcs;
     }

     @Override
     public void onRtpPacket(ChannelInRtpPacket channelInRtpPacket) {
         RtpPacket rtpPacket = channelInRtpPacket.getRtpPacket();
         rtpPacket = rtcToMainPacket(rtpPacket);
         for (MediaSink sink : sinks) {
             sink.onRtpPacket(rtpPacket);
         }
     }

     @Override
     public void onRtcpPacket(RtcpPacket rtcpPacket) {
        if (rtcpPacket instanceof SenderReportRtcpPacket senderReportRtcpPacket) {
            inboundRtpStream.onSenderReport(senderReportRtcpPacket);
        }
     }


     @Override
     public void addSink(MediaSink sink) {
         sink.addMediaStreamControl(this);
         sinks.add(sink);


     }

     @Override
     public void removeSink(MediaSink sink) {
         sinks.remove(sink);
     }

     private RtpPacket rtcToMainPacket(RtpPacket rtpPacket) {
         int payloadType = rtpPacket.getPayloadType();
         RtpPayload rtpPayload = info.getRtpPayloads().get(payloadType);
         FmtpAttributes fmtp = rtpPayload.getFmtp();
         //这个是重传消息
         if (fmtp != null && fmtp.getAssociatedPayloadType() != null) {
             Integer associatedPayloadType = fmtp.getAssociatedPayloadType();
             rtpPacket.setPayloadType(associatedPayloadType);
             int seq = rtpPacket.getPayload().readUnsignedShort();
             rtpPacket.setSequenceNumber(seq);
             log.info("当前为重传消息：{},序列号为：{}", rtpPayload, rtpPacket.getSequenceNumber());
         }
         return rtpPacket;
     }

     @Override
     public void onPLI() {
         PsFbRtcpPacket psFbRtcpPacket = new PsFbRtcpPacket();
         psFbRtcpPacket.setVersion((byte) 2);
         psFbRtcpPacket.setPadding((byte) 0);
         psFbRtcpPacket.setFmt(1);
         psFbRtcpPacket.setPayloadType(RtcpPayloadType.PSFB.value);
         psFbRtcpPacket.setLength(2);
         psFbRtcpPacket.setSenderSsrc(1);
         psFbRtcpPacket.setMediaSsrc(ssrcs[0]);
         this.producerEvent.onRtcpPacket(new RtcpGroupPacket(psFbRtcpPacket));

     }
 }
