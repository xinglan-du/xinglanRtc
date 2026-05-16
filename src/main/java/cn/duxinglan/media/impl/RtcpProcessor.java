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
 import cn.duxinglan.media.protocol.rtcp.*;
 import io.netty.buffer.ByteBuf;
 import io.netty.buffer.ByteBufUtil;
 import lombok.extern.slf4j.Slf4j;

 import java.nio.charset.StandardCharsets;
 import java.util.List;

 @Slf4j
 public class RtcpProcessor {

     private final MediaSession session;

     public RtcpProcessor(MediaSession session) {
         this.session = session;
     }

     public void process(ByteBuf decrypt) {
         List<RtcpPacket> rtcpPackets = RtcpFactory.packetsSRtcpToRtcp(decrypt);
         session.onRtcpPacket(rtcpPackets);
     }

     public void sendProcess(RtcpPacket[] rtcpPackets, ByteBuf byteBuf) {
         for (RtcpPacket rtcpPacket : rtcpPackets) {
             if (rtcpPacket instanceof SenderReportRtcpPacket senderReportRtcpPacket) {
                 parseSenderReport(senderReportRtcpPacket, byteBuf);
             } else if (rtcpPacket instanceof SdesRtcpPacket sdesRtcpPacket) {
                 parseSdes(sdesRtcpPacket, byteBuf);
             } else if (rtcpPacket instanceof PsFbRtcpPacket psFbRtcpPacket) {
                 parsePsFb(psFbRtcpPacket, byteBuf);
             } else if (rtcpPacket instanceof ReceiverReportRtcpPacket receiverReportRtcpPacket) {
                 parseReceiverReport(receiverReportRtcpPacket, byteBuf);
//                log.info("当前线程id:{},发送RR：{}", Thread.currentThread().getId(), rtcpPacket);
             }
         }
     }


     private void parseReceiverReport(ReceiverReportRtcpPacket receiverReportRtcpPacket, ByteBuf byteBuf) {
         byte one = (byte) ((receiverReportRtcpPacket.getVersion() & 0x03) << 6 | (receiverReportRtcpPacket.getPadding() & 0x01) << 5 | (receiverReportRtcpPacket.getRc() & 0x1F));
         byteBuf.writeByte(one);
         byteBuf.writeByte(receiverReportRtcpPacket.getPayloadType());
         byteBuf.writeShort(receiverReportRtcpPacket.getLength());
         byteBuf.writeInt((int) (receiverReportRtcpPacket.getSsrc() & 0xFFFFFFFFL));
         List<ReceiverReportBlock> receiverReportBlocks = receiverReportRtcpPacket.getReceiverReportBlocks();
         for (ReceiverReportBlock receiverReportBlock : receiverReportBlocks) {
             byteBuf.writeInt((int) (receiverReportBlock.getSourceSsrc() & 0xFFFFFFFFL));
             byteBuf.writeByte(receiverReportBlock.getFractionLost());
             byteBuf.writeMedium((int) (receiverReportBlock.getLost() & 0x00FFFFFF));
             byteBuf.writeInt((int) (receiverReportBlock.getExtHighestSeq() & 0xFFFFFFFFL));
             byteBuf.writeInt((int) (receiverReportBlock.getJitter() & 0xFFFFFFFFL));
             byteBuf.writeInt((int) (receiverReportBlock.getLsr() & 0xFFFFFFFFL));
             byteBuf.writeInt((int) (receiverReportBlock.getDlsr() & 0xFFFFFFFFL));
         }

     }

     private void parsePsFb(PsFbRtcpPacket psFbRtcpPacket, ByteBuf byteBuf) {
         byte one = (byte) ((psFbRtcpPacket.getVersion() & 0x03) << 6 | (psFbRtcpPacket.getPadding() & 0x01) << 5 | (psFbRtcpPacket.getFmt() & 0x1F));
         byteBuf.writeByte(one);
         byteBuf.writeByte(psFbRtcpPacket.getPayloadType());
         byteBuf.writeShort(psFbRtcpPacket.getLength());
         byteBuf.writeInt((int) (psFbRtcpPacket.getSenderSsrc() & 0xFFFFFFFFL));
         byteBuf.writeInt((int) (psFbRtcpPacket.getMediaSsrc() & 0xFFFFFFFFL));

         //丢包重传
         if (psFbRtcpPacket.getFirEntry() != null) {
             FirEntry firEntry = psFbRtcpPacket.getFirEntry();
             byteBuf.writeInt((int) (firEntry.getSsrc() & 0xFFFFFFFFL));
             byteBuf.writeByte((int) firEntry.getSeqNr());
             byteBuf.writeMedium(firEntry.getReserved());
             return;
         }

         //丢包重传
         ByteBuf fci = psFbRtcpPacket.getFci();
         if (fci != null && fci.isReadable()) {
             byteBuf.writeBytes(fci, fci.readerIndex(), fci.readableBytes());
         }
     }


     private void parseSdes(SdesRtcpPacket sdesRtcpPacket, ByteBuf byteBuf) {
         byte one = (byte) ((sdesRtcpPacket.getVersion() & 0x03) << 6 | (sdesRtcpPacket.getPadding() & 0x01) << 5 | (sdesRtcpPacket.getSc() & 0x1F));
         byteBuf.writeByte(one);
         byteBuf.writeByte(sdesRtcpPacket.getPayloadType());
         byteBuf.writeShort(sdesRtcpPacket.getLength());
         for (SdesChunk sdesChunk : sdesRtcpPacket.getChunks()) {
             int chunkStart = byteBuf.writerIndex();
             byteBuf.writeInt((int) (sdesChunk.getSsrc() & 0xFFFFFFFFL));
             for (SdesItem item : sdesChunk.getItems()) {
                 byteBuf.writeByte(item.getType().value);

                 if (item instanceof CnameSdesItem cnameSdesItem) {
                     int length = cnameSdesItem.getLength();
                     byteBuf.writeByte(length);
                     byte[] bytes = cnameSdesItem.getCname().getBytes(StandardCharsets.UTF_8);
                     byteBuf.writeBytes(bytes);

                 } else if (item.getType() == SdesItemType.END) {

                 }
             }
             int used = byteBuf.writerIndex() - chunkStart;
             int pad = (4 - (used % 4)) % 4;
             byteBuf.writeZero(pad);
         }

     }


     private void parseSenderReport(SenderReportRtcpPacket senderReportRtcpPacket, ByteBuf byteBuf) {
         byte one = (byte) ((senderReportRtcpPacket.getVersion() & 0x03) << 6 | (senderReportRtcpPacket.getPadding() & 0x01) << 5 | (senderReportRtcpPacket.getRc() & 0x1F));
         byteBuf.writeByte(one);
         byteBuf.writeByte(senderReportRtcpPacket.getPayloadType());
         byteBuf.writeShort(senderReportRtcpPacket.getLength());
         byteBuf.writeInt((int) (senderReportRtcpPacket.getSsrc() & 0xFFFFFFFFL));
         byteBuf.writeInt((int) (senderReportRtcpPacket.getNtpSec() & 0xFFFFFFFFL));
         byteBuf.writeInt((int) (senderReportRtcpPacket.getNtpFrac() & 0xFFFFFFFFL));
         byteBuf.writeInt((int) (senderReportRtcpPacket.getRtpTimestamp() & 0xFFFFFFFFL));
         byteBuf.writeInt((int) (senderReportRtcpPacket.getSenderPacketCount() & 0xFFFFFFFFL));
         byteBuf.writeInt((int) (senderReportRtcpPacket.getSenderOctetCount() & 0xFFFFFFFFL));
     }
 }
