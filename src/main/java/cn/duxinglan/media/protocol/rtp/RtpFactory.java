package cn.duxinglan.media.protocol.rtp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

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
public class RtpFactory {

    public static final int RTP_HEADER_LENGTH = 12;

    public static RtpPacket parseBytebufToRtpPacket(ByteBuf byteBuf) {
        byteBuf.markReaderIndex();

        byte versionAndPaddingAndExtensionAndeCC = byteBuf.readByte();
        byte version = (byte) ((versionAndPaddingAndExtensionAndeCC >> 6) & 0x03);  // 高 2 bit
        byte padding = (byte) ((versionAndPaddingAndExtensionAndeCC >> 5) & 0x01);  // 第 3 bit
        byte extension = (byte) ((versionAndPaddingAndExtensionAndeCC >> 4) & 0x01); // 第 4 bit
        byte csrcCount = (byte) (versionAndPaddingAndExtensionAndeCC & 0x0F);
        byte markerAndPayloadType = byteBuf.readByte();
        byte marker = (byte) ((markerAndPayloadType >> 7) & 0x01);
        int payloadType = (byte) (markerAndPayloadType & 0x7f);
        int sequenceNumber = byteBuf.readUnsignedShort();
        long timestamp = byteBuf.readUnsignedInt();
        long ssrc = byteBuf.readUnsignedInt();

        ByteBuf payload = byteBuf.readSlice(byteBuf.readableBytes());


        RtpPacket rtpPacket = new RtpPacket();
        rtpPacket.setVersion(version);
        rtpPacket.setPadding(padding);
        rtpPacket.setExtension(extension);
        rtpPacket.setCsrcCount(csrcCount);

        rtpPacket.setMarker(marker);
        rtpPacket.setPayloadType(payloadType);
        rtpPacket.setSequenceNumber(sequenceNumber);
        rtpPacket.setSsrc(ssrc);
        rtpPacket.setTimestamp(timestamp);
        rtpPacket.setPayload(payload);

        byteBuf.resetReaderIndex();
        return rtpPacket;
    }

    public static ByteBuf parseRtpPacketToBytebuf(long ssrc, RtpPacket rtpPackage) {

        ByteBuf buffer = Unpooled.buffer();
        byte one = (byte) ((rtpPackage.getVersion() & 0x03) << 6 | (rtpPackage.getPadding() & 0x01) << 5 | (rtpPackage.getExtension() & 0x01) << 4 | (rtpPackage.getCsrcCount() & 0x0F));
        byte two = (byte) (((rtpPackage.getMarker() & 0x01) << 7) | (rtpPackage.getPayloadType() & 0x7F));

        buffer.writeByte(one);
        buffer.writeByte(two);
        buffer.writeShort(rtpPackage.getSequenceNumber());
        buffer.writeInt((int) (rtpPackage.getTimestamp() & 0xFFFFFFFFL));
        buffer.writeInt((int) (ssrc & 0xFFFFFFFFL));
//        buffer.writeInt((int) (rtpPackage.getSsrc() & 0xFFFFFFFFL));
        ByteBuf payload = rtpPackage.getPayload().slice();

        buffer.writeBytes(payload);


        return buffer;
    }
}
