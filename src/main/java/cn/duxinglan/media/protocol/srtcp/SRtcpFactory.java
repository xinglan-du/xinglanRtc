package cn.duxinglan.media.protocol.srtcp;

import cn.duxinglan.media.protocol.rtcp.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * 版权所有 (c) 2025 www.duxinglan.cn
 * <p>
 * 项目名称：xinglanRtc
 * <p>
 * 本文件属于 xinglanRtc 项目的一部分。
 * <p>
 * 本软件依据 XinglanRtc 非商业许可证（XNCL）授权，仅限个人非商业使用。
 * 禁止任何形式的商业用途，包括但不限于：收费安装、收费部署、
 * 收费运维、收费技术支持等行为。
 * <p>
 * 详情请参阅项目根目录下的 LICENSE 文件。
 **/
@Slf4j
public class SRtcpFactory {

    public static final int S_RTCP_HEADER_LENGTH = 8;

    public static final int S_RTCP_INDEX_LENGTH = 4;


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
            RtcpPacket rtcpPacket = switch (rtcpPayloadType) {
                case PSFB -> {
                    PsFbRtcpPacket psFbRtcpPacket = new PsFbRtcpPacket();
                    psFbRtcpPacket.setVersion(version);
                    psFbRtcpPacket.setPadding(padding);
                    psFbRtcpPacket.setFmt(countOrFmt);
                    psFbRtcpPacket.setPayloadType(payloadType);
                    psFbRtcpPacket.setLength(length);
                    psFbRtcpPacket.setSenderSsrc(packetBody.readUnsignedInt());
                    psFbRtcpPacket.setMediaSsrc(packetBody.readUnsignedInt());
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
        return rtcpPackets;
    }


    public static SRtcpPacket parseBytebufToSrtcpPacket(ByteBuf byteBuf, int rtcpAuthTagLength) {
        SRtcpPacket sRtcpPacket = new SRtcpPacket(rtcpAuthTagLength);
        sRtcpPacket.setEncryptByteBuf(byteBuf);
        return sRtcpPacket;
    }

    public static SRtcpPacket parseRtcpToSRtcp(List<RtcpPacket> rtcpPackets, int rtcpAuthTagLength) {
        int totalLength = 0;
        for (RtcpPacket rtcpPacket : rtcpPackets) {
            totalLength += rtcpPacket.getTotalLength();
        }

        ByteBuf buffer = Unpooled.buffer(totalLength);
        for (RtcpPacket rtcpPacket : rtcpPackets) {
            if (rtcpPacket instanceof SenderReportRtcpPacket senderReportRtcpPacket) {
                parseSenderReport(senderReportRtcpPacket, buffer);
            } else if (rtcpPacket instanceof SdesRtcpPacket sdesRtcpPacket) {
                parseSdes(sdesRtcpPacket, buffer);
            } else if (rtcpPacket instanceof PsFbRtcpPacket psFbRtcpPacket) {
                parsePsFb(psFbRtcpPacket, buffer);
//                log.info("当前线程id:{},发送申请关键帧：{}", Thread.currentThread().getId(), rtcpPacket);
            } else if (rtcpPacket instanceof ReceiverReportRtcpPacket receiverReportRtcpPacket) {
                parseReceiverReport(receiverReportRtcpPacket, buffer);
//                log.info("当前线程id:{},发送RR：{}", Thread.currentThread().getId(), rtcpPacket);
            }
        }

        SRtcpPacket sRtcpPacket = new SRtcpPacket(rtcpAuthTagLength);
        sRtcpPacket.setDecryptByteBuf(buffer);
        return sRtcpPacket;
    }

    private static void parseReceiverReport(ReceiverReportRtcpPacket receiverReportRtcpPacket, ByteBuf byteBuf) {
        byte one = (byte) ((receiverReportRtcpPacket.getVersion() & 0x03) << 6 | (receiverReportRtcpPacket.getPadding() & 0x01) << 5 | (receiverReportRtcpPacket.getRc() & 0x1F));
        byteBuf.writeByte(one);
        byteBuf.writeByte(receiverReportRtcpPacket.getPayloadType());
        byteBuf.writeShort(receiverReportRtcpPacket.getLength());
        byteBuf.writeInt((int) (receiverReportRtcpPacket.getSsrc() & 0xFFFFFFFFL));
        List<ReceiverReportBlock> receiverReportBlocks = receiverReportRtcpPacket.getReceiverReportBlocks();
        for (ReceiverReportBlock receiverReportBlock : receiverReportBlocks) {
            byteBuf.writeInt((int) (receiverReportBlock.getSourceSsrc() & 0xFFFFFFFFL));
            byteBuf.writeByte(receiverReportBlock.getFractionLost());
            byteBuf.writeMedium(receiverReportBlock.getLost());
            byteBuf.writeInt((int) (receiverReportBlock.getExtHighestSeq() & 0xFFFFFFFFL));
            byteBuf.writeInt((int) (receiverReportBlock.getJitter() & 0xFFFFFFFFL));
            byteBuf.writeInt((int) (receiverReportBlock.getLsr() & 0xFFFFFFFFL));
            byteBuf.writeInt((int) (receiverReportBlock.getDlsr() & 0xFFFFFFFFL));
        }

    }

    private static void parsePsFb(PsFbRtcpPacket psFbRtcpPacket, ByteBuf byteBuf) {
        byte one = (byte) ((psFbRtcpPacket.getVersion() & 0x03) << 6 | (psFbRtcpPacket.getPadding() & 0x01) << 5 | (psFbRtcpPacket.getFmt() & 0x1F));
        byteBuf.writeByte(one);
        byteBuf.writeByte(psFbRtcpPacket.getPayloadType());
        byteBuf.writeShort(psFbRtcpPacket.getLength());
        byteBuf.writeInt((int) (psFbRtcpPacket.getSenderSsrc() & 0xFFFFFFFFL));
        byteBuf.writeInt((int) (psFbRtcpPacket.getMediaSsrc() & 0xFFFFFFFFL));
        if (psFbRtcpPacket.getFirEntry() != null) {
            FirEntry firEntry = psFbRtcpPacket.getFirEntry();
            byteBuf.writeInt((int) (firEntry.getSsrc() & 0xFFFFFFFFL));
            byteBuf.writeByte((int) firEntry.getSeqNr());
            byteBuf.writeMedium(firEntry.getReserved());

        }
    }

    private static void parseSdes(SdesRtcpPacket sdesRtcpPacket, ByteBuf byteBuf) {
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


    private static void parseSenderReport(SenderReportRtcpPacket senderReportRtcpPacket, ByteBuf byteBuf) {
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
