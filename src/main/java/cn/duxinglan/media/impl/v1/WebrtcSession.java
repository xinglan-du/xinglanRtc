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


 import cn.duxinglan.media.core.ChannelContext;
 import cn.duxinglan.media.core.MediaSession;
 import cn.duxinglan.media.core.consumer.Consumer;
 import cn.duxinglan.media.core.producer.Producer;
 import cn.duxinglan.media.core.stream.MediaSink;
 import cn.duxinglan.media.core.stream.MediaSinkFactory;
 import cn.duxinglan.media.core.stream.MediaStream;
 import cn.duxinglan.media.core.stream.Router;
 import cn.duxinglan.media.impl.webrtc.MediaLineInfo;
 import cn.duxinglan.media.impl.webrtc.SsrcGenerator;
 import cn.duxinglan.media.protocol.ChannelInRtpPacket;
 import cn.duxinglan.media.protocol.rtcp.PsFbRtcpPacket;
 import cn.duxinglan.media.protocol.rtcp.RtcpPacket;
 import cn.duxinglan.media.protocol.rtcp.RtcpPayloadType;
 import cn.duxinglan.media.protocol.rtcp.SenderReportRtcpPacket;
 import cn.duxinglan.media.transport.udp.BasePacket;
 import cn.duxinglan.media.util.UUIDUtils;
 import cn.duxinglan.sdp.entity.ssrc.SSRC;
 import cn.duxinglan.sdp.entity.ssrc.SsrcGroup;
 import cn.duxinglan.sdp.entity.type.MediaInfoType;
 import lombok.extern.slf4j.Slf4j;
 import org.jspecify.annotations.Nullable;

 import java.util.*;
 import java.util.concurrent.ConcurrentHashMap;

 @Slf4j
 public class WebrtcSession implements MediaSession, MediaSinkFactory, Consumer.ConsumerEvent, Producer.ProducerEvent {

     private final String sessionId = UUIDUtils.createUUID();

     private final Object lock = new Object();

     private final Map<Long, Producer> producers = new ConcurrentHashMap<>();

     private final Map<Long, Consumer> consumers = new ConcurrentHashMap<>();

     private Router router;

     private final SessionEvent sessionEvent;

     private ChannelContext channelContext;

     public WebrtcSession(SessionEvent sessionEvent) {
         this.sessionEvent = sessionEvent;
     }


     @Override
     public String getSessionId() {
         return sessionId;
     }

     @Override
     public void onRtpPacket(ChannelInRtpPacket rtpPacket) {
         long ssrc = rtpPacket.getRtpPacket().getSsrc();
         producers.computeIfPresent(ssrc, (key, producer) -> {
             producer.onRtpPacket(rtpPacket);
             return producer;
         });
     }


     @Override
     public void onRtcpPacket(List<RtcpPacket> rtcpPackets) {
         for (RtcpPacket rtcpPacket : rtcpPackets) {
             long ssrc = 0;
             switch (rtcpPacket) {
                 case SenderReportRtcpPacket senderReportRtcpPacket -> ssrc = senderReportRtcpPacket.getSsrc();
                 case PsFbRtcpPacket psFbRtcpPacket -> {
                     ssrc = psFbRtcpPacket.getMediaSsrc();
                     if (psFbRtcpPacket.getPayloadType() == RtcpPayloadType.PSFB.value
                             && psFbRtcpPacket.getFmt() == 1) {
                         log.debug("接收到申请关键帧{}", psFbRtcpPacket);
                     }
                 }
                 default -> {
                     log.debug("未实现的rtcp处理:{}", rtcpPacket);
                 }
             }
             if (producers.containsKey(ssrc)) {
                 producers.get(ssrc).onRtcpPacket(rtcpPacket);
             } else if (consumers.containsKey(ssrc)) {
                 consumers.get(ssrc).onRtcpPacket(rtcpPacket);
             }
         }
     }


     @Override
     public void addRouter(Router router) {
         this.router = router;
         router.subscribe(sessionId, this);
         synchronized (lock) {
             for (Producer value : producers.values()) {
                 router.publish(sessionId, value);
             }
         }
     }

     @Override
     public void addChannelContext(ChannelContext channelContext) {
         this.channelContext = channelContext;
     }

     @Override
     public void removeChannelContext(ChannelContext channelContext) {

     }


     /**
      * 处理解析后的媒体线路信息，将支持接收媒体的媒体行的相关数据提取并存储。
      * 如果媒体行为只读（仅接收模式），会基于媒体行的接收信息创建媒体接收单元，并将其与对应的 SSRC 关联。
      *
      * @param mediaLines 一个包含媒体线路信息的集合，表示与会话中的多个媒体流相关的信息。
      *                   集合中的每个对象描述了一条媒体线路，包括媒体的类型、接收或发送能力等。
      */
     public void onMediaLinesParsed(Collection<MediaLineInfo> mediaLines) {
         for (MediaLineInfo mediaLine : mediaLines) {
             //当前媒体行是否支持接受媒体，服务器作为接受方
             if (mediaLine.isReadOnly()) {
                 //代表一个媒体数据接受单元
                 MediaLineInfo.Info readInfo = mediaLine.getReadInfo();
                 WebrtcProducer webrtcProducer = new WebrtcProducer(mediaLine.getMediaInfoType(), mediaLine.getMid(), readInfo, this);
                 for (long ssrc : webrtcProducer.getSsrc()) {
                     addProduces(ssrc, webrtcProducer);
                 }
             }
         }
     }


     /**
      * 将一个生产者关联到指定的 SSRC，并在路由器中发布该生产者的媒体流。
      * 如果指定的 SSRC 已经存在于当前会话中，则不执行任何操作。
      *
      * @param ssrc     用于标识生产者的唯一 SSRC。
      * @param producer 要添加的生产者对象，表示媒体流的数据源。
      */
     private void addProduces(long ssrc, Producer producer) {
         if (producers.containsKey(ssrc)) {
             return;
         }
         producers.put(ssrc, producer);
         synchronized (lock) {
             if (router != null) {
                 router.publish(sessionId, producer);
             }
         }

     }

     /**
      * 根据输入的媒体流创建一个 MediaSink 对象。
      * 如果输入的媒体流为 {@link WebrtcProducer} 类型，
      * 则会基于媒体流的接收信息生成对应的 {@link WebrtcConsumer}，
      * 并将其与相应的 SSRC 映射到一起。
      *
      * @param mediaStream 输入的 {@link MediaStream} 对象，用于提供媒体流的数据源。
      *                    如果媒体流是 WebRTC 生产者 ({@link WebrtcProducer})，将基于其信息创建消费者。
      * @return 返回创建的 {@link MediaSink} 对象。
      * 如果输入的媒体流不是 {@link WebrtcProducer} 类型，则返回 null。
      */
     @Override
     public @Nullable MediaSink createMediaSink(MediaStream mediaStream) {
         if (mediaStream instanceof WebrtcProducer webrtcProducer) {
             MediaInfoType mediaInfoType = webrtcProducer.getMediaInfoType();
             MediaLineInfo.Info readInfo = webrtcProducer.getInfo();
             MediaLineInfo mediaLineInfo = sessionEvent.onAddSendMediaLineInfo(mediaInfoType, transformedInfo(readInfo));
             WebrtcConsumer webrtcConsumer = new WebrtcConsumer(mediaLineInfo.getMediaInfoType(), mediaLineInfo.getMid(), mediaLineInfo.getSendInfo(), this);
             long[] ssrcs = webrtcConsumer.getSsrcs();
             for (long ssrc : ssrcs) {
                 consumers.put(ssrc, webrtcConsumer);
             }
             return webrtcConsumer;
         }

         return null;
     }


     /**
      * 将输入的媒体线路信息对象转换为新的媒体线路信息对象，修改其 SSRC 和 SSRC 组信息。
      *
      * @param readInfo 输入的 {@link MediaLineInfo.Info} 对象，表示包含接收端媒体信息的数据结构，
      *                 包括 SSRC 映射、SSRC 组以及 RTP 负载类型等。
      * @return 返回一个新的 {@link MediaLineInfo.Info} 对象，包含经过转换的 SSRC 映射和 SSRC 组信息。
      * 如果输入的 SSRC 映射为空或不存在，返回 null。
      */
     private MediaLineInfo.Info transformedInfo(MediaLineInfo.Info readInfo) {
         List<SsrcGroup> readSsrcGroups = readInfo.getSsrcGroups();
         Map<Long, SSRC> readSsrcMap = readInfo.getSsrcMap();
         if (readSsrcMap == null || readSsrcMap.isEmpty()) {
             return null;
         }

         Map<Long, SSRC> sendInfoSsrc = new LinkedHashMap<>();
         List<SsrcGroup> sendSsrcGroups = new ArrayList<>();


         Map<Long, Long> temporarySsrcMap = new HashMap<>(readSsrcMap.size());
         for (Map.Entry<Long, SSRC> longSSRCEntry : readSsrcMap.entrySet()) {
             long generateSsrc = SsrcGenerator.generateSsrc();
             SSRC readSSrc = longSSRCEntry.getValue();
             SSRC ssrc = new SSRC();
             ssrc.setSsrc(generateSsrc);
             ssrc.setCname(readSSrc.getCname());
             ssrc.setStreamId(readSSrc.getStreamId());
             temporarySsrcMap.put(longSSRCEntry.getKey(), generateSsrc);
             sendInfoSsrc.put(ssrc.getSsrc(), ssrc);
         }


         for (SsrcGroup readSsrcGroup : readSsrcGroups) {
             SsrcGroup ssrcGroup = new SsrcGroup();
             ssrcGroup.setSsrcGroupType(readSsrcGroup.getSsrcGroupType());
             for (Long readSsrc : readSsrcGroup.getSsrcList()) {
                 Long sendSsrc = temporarySsrcMap.get(readSsrc);
                 if (sendSsrc != null) {
                     ssrcGroup.addSsrc(sendSsrc);
                 }

             }
             sendSsrcGroups.add(ssrcGroup);
         }


         MediaLineInfo.Info info = new MediaLineInfo.Info();
         info.setSsrcMap(sendInfoSsrc);
         info.setSsrcGroups(sendSsrcGroups);
         info.setRtpPayloads(readInfo.getRtpPayloads());
         return info;

     }

     @Override
     public void onConsumerRtpPacket(BasePacket packet) {
         if (channelContext != null) {
             channelContext.send(packet);
         }
     }

     @Override
     public void onRtcpPacket(BasePacket packet) {
         if (channelContext != null) {
             channelContext.send(packet);
         }
     }

     public interface SessionEvent {

         MediaLineInfo onAddSendMediaLineInfo(MediaInfoType type, MediaLineInfo.Info info);
     }

 }
