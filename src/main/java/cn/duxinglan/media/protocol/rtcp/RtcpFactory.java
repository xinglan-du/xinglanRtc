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
 package cn.duxinglan.media.protocol.rtcp;


 import io.netty.buffer.ByteBuf;
 import lombok.extern.slf4j.Slf4j;

 import java.nio.charset.StandardCharsets;
 import java.util.ArrayList;
 import java.util.List;

 @Slf4j
 public class RtcpFactory {


     public static List<RtcpPacket> packetsSRtcpToRtcp(ByteBuf byteBuf) {
         List<RtcpPacket> rtcpPackets = new ArrayList<>();
         while (byteBuf.isReadable()) {
             byte versionAndPaddingAndRc = byteBuf.readByte();
             byte version = (byte) (versionAndPaddingAndRc >> 6 & 0x03);
             byte padding = (byte) ((versionAndPaddingAndRc >> 5) & 0x01);
             byte countOrFmt = (byte) (versionAndPaddingAndRc & 0x1F);
             int payloadType = byteBuf.readUnsignedByte();
             int length = byteBuf.readShort();
             int packetLength = (length + 1) * 4;
             ByteBuf packetBody = byteBuf.readSlice(packetLength - 4);
             RtcpPayloadType rtcpPayloadType = RtcpPayloadType.fromValue(payloadType);
//            log.debug("处理rtcp：{}", rtcpPayloadType);
             RtcpPacket rtcpPacket = switch (rtcpPayloadType) {
                 case PSFB, RTPFB -> {
                     PsFbRtcpPacket psFbRtcpPacket = new PsFbRtcpPacket();
                     psFbRtcpPacket.setVersion(version);
                     psFbRtcpPacket.setPadding(padding);
                     psFbRtcpPacket.setFmt(countOrFmt);
                     psFbRtcpPacket.setPayloadType(payloadType);
                     psFbRtcpPacket.setLength(length);
                     psFbRtcpPacket.setSenderSsrc(packetBody.readUnsignedInt());
                     psFbRtcpPacket.setMediaSsrc(packetBody.readUnsignedInt());
                     if (packetBody.isReadable()) {
                         psFbRtcpPacket.setFci(packetBody.readSlice(packetBody.readableBytes()));
                     }

                     yield psFbRtcpPacket;
                 }
                 case SENDER_REPORT -> {
                     long ssrc = packetBody.readUnsignedInt();
                     long ntpSeconds = packetBody.readUnsignedInt();
                     long ntpFraction = packetBody.readUnsignedInt();
                     long rtpTime = packetBody.readUnsignedInt();
                     long senderPacketCount = packetBody.readUnsignedInt();
                     long senderOctetCount = packetBody.readUnsignedInt();

                     SenderReportRtcpPacket senderReportRtcpPacket = new SenderReportRtcpPacket();
                     senderReportRtcpPacket.setVersion(version);
                     senderReportRtcpPacket.setPadding(padding);
                     senderReportRtcpPacket.setRc(countOrFmt);
                     senderReportRtcpPacket.setPayloadType(payloadType);
                     senderReportRtcpPacket.setLength(length);
                     senderReportRtcpPacket.setSsrc(ssrc);
                     senderReportRtcpPacket.setNtpSec(ntpSeconds);
                     senderReportRtcpPacket.setNtpFrac(ntpFraction);
                     senderReportRtcpPacket.setRtpTimestamp(rtpTime);
                     senderReportRtcpPacket.setSenderPacketCount(senderPacketCount);
                     senderReportRtcpPacket.setSenderOctetCount(senderOctetCount);
                     yield senderReportRtcpPacket;
                 }
                 case RECEIVER_REPORT -> {
                     long ssrc = packetBody.readUnsignedInt();

                     ReceiverReportRtcpPacket receiverReportRtcpPacket = new ReceiverReportRtcpPacket();
                     receiverReportRtcpPacket.setVersion(version);
                     receiverReportRtcpPacket.setPadding(padding);
                     receiverReportRtcpPacket.setRc(countOrFmt);
                     receiverReportRtcpPacket.setLength(length);
                     receiverReportRtcpPacket.setPayloadType(payloadType);
                     receiverReportRtcpPacket.setSsrc(ssrc);

                     for (int i = 0; i < countOrFmt; i++) {
                         long sourceSsrc = packetBody.readUnsignedInt();
                         int fractionLost = packetBody.readUnsignedByte();
                         int lost = packetBody.readUnsignedMedium();
                         long extHighestSeq = packetBody.readUnsignedInt();
                         long jitter = packetBody.readUnsignedInt();
                         long lsr = packetBody.readUnsignedInt();
                         long dlsr = packetBody.readUnsignedInt();

                         ReceiverReportBlock receiverReportBlock = new ReceiverReportBlock();
                         receiverReportBlock.setSourceSsrc(sourceSsrc);
                         receiverReportBlock.setFractionLost(fractionLost);
                         receiverReportBlock.setLost(lost);
                         receiverReportBlock.setExtHighestSeq(extHighestSeq);
                         receiverReportBlock.setJitter(jitter);
                         receiverReportBlock.setLsr(lsr);
                         receiverReportBlock.setDlsr(dlsr);
                         receiverReportRtcpPacket.addReceiverReportBlock(receiverReportBlock);
                     }
//                    log.info("这是第{}次循环，解析RR数据包：{}", t, receiverReportRtcpPacket);
                     yield receiverReportRtcpPacket;
                 }
                 case SDES -> {
                     SdesRtcpPacket sdes = new SdesRtcpPacket();
                     sdes.setVersion(version);
                     sdes.setPadding(padding);
                     sdes.setSc(countOrFmt);
                     sdes.setPayloadType(payloadType);
                     sdes.setLength(length);

                     for (int i = 0; i < countOrFmt; i++) {
                         int chunkStart = packetBody.readerIndex();

                         long ssrc = packetBody.readUnsignedInt();
                         SdesChunk chunk = new SdesChunk(ssrc);

                         while (true) {
                             int type = packetBody.readUnsignedByte();
                             SdesItemType sdesItemType = SdesItemType.fromValue(type);

                             if (sdesItemType == SdesItemType.END) { // END
                                 break;
                             }
                             short len = packetBody.readUnsignedByte();
                             ByteBuf value = packetBody.readSlice(len);

                             if (sdesItemType == SdesItemType.CNAME) { // CNAME
                                 chunk.addItem(
                                         new CnameSdesItem(
                                                 value.toString(StandardCharsets.UTF_8)
                                         )
                                 );
                             }
                         }

                         // ===== 关键：padding =====
                         int used = packetBody.readerIndex() - chunkStart;
                         int pad = (4 - (used % 4)) % 4;
                         packetBody.skipBytes(pad);
                         sdes.addChunk(chunk);
                     }

                     yield sdes;
                 }
                 case null -> null;
                 default -> {
                     log.info("当前未处理的payloadType:{}", payloadType);
                     yield null;
                 }
             };
             if (rtcpPacket != null) {
                 rtcpPackets.add(rtcpPacket);
             }
         }
//        log.debug("处理rtcp：{}", rtcpPackets.size());
         return rtcpPackets;
     }

 }
