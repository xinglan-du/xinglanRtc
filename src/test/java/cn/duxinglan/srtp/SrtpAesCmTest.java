package cn.duxinglan.srtp;

import cn.duxinglan.media.protocol.rtcp.RtcpPacket;
import cn.duxinglan.media.protocol.rtcp.RtcpPayloadType;
import cn.duxinglan.media.protocol.rtcp.SenderReportRtcpPacket;
import cn.duxinglan.media.protocol.rtp.RtpPacket;
import cn.duxinglan.media.protocol.srtcp.SRtcpFactory;
import cn.duxinglan.media.protocol.srtcp.SRtcpPacket;
import cn.duxinglan.media.protocol.srtp.SRtpPacket;
import cn.duxinglan.media.transport.nio.webrtc.SRtcpContext;
import cn.duxinglan.media.transport.nio.webrtc.SRtpContext;
import cn.duxinglan.media.transport.nio.webrtc.SrtpContextFactory;
import cn.duxinglan.media.transport.nio.webrtc.SrtpProfilesType;
import cn.duxinglan.media.util.ByteUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;


@Slf4j
public class SrtpAesCmTest {

    private static final byte[] test_key =
            ByteUtils.hexStringToByteArray("e1f97a0d3e018be0d64fa32c06de4139");
    private static final byte[] test_key_salt =
            ByteUtils.hexStringToByteArray("0ec675ad498afeebb6960b3aabe6");


    private static final byte[] srtp_ciphertext =
            ByteUtils.hexStringToByteArray("800f1234decafbad" +
                                           "cafebabe4e55dc4c" +
                                           "e79978d88ca4d215" +
                                           "949d2402b78d6acc" +
                                           "99ea179b8dbb");
    private static final byte[] rtp_plaintext =
            ByteUtils.hexStringToByteArray("800f1234decafbad" +
                                           "cafebabeabababab" +
                                           "abababababababab" +
                                           "abababab00000000" +
                                           "000000000000");

    private static final byte[] rtp_plaintext_ref =
            ByteUtils.hexStringToByteArray("800f1234decafbad" +
                                           "cafebabeabababab" +
                                           "abababababababab" +
                                           "abababab");

    private static final byte[] rtcp_plaintext =
            ByteUtils.hexStringToByteArray("81c8000bcafebabe" +
                                           "abababababababab" +
                                           "abababababababab" +
                                           "0000000000000000" +
                                           "000000000000");

    private static final byte[] srtcp_ciphertext =
            ByteUtils.hexStringToByteArray("81c8000bcafebabe" +
                                           "7128035be487b9bd" +
                                           "bef89041f977a5a8" +
                                           "80000001993e08cd" +
                                           "54d6c1230798");

    private static final byte[] rtcp_plaintext_ref =
            ByteUtils.hexStringToByteArray("81c8000bcafebabe" +
                                           "abababababababab" +
                                           "abababababababab");

    private SrtpProfilesType srtpProfilesType = SrtpProfilesType.SRTP_AES128_CM_HMAC_SHA1_80;

    private final long ssrc = 0xcafebabeL;


    /**
     * 测试 SRTP (安全实时传输协议) 数据包的加密功能。
     * <p>
     * 此测试方法验证 `encryptRtpPacket()` 方法的正确性，确保加密后的数据包与预期的加密结果一致。
     * 具体流程如下：
     * 1. 调用 `encryptRtpPacket()` 方法对 RTP 数据包进行加密。
     * 2. 使用 `assertArrayEquals` 方法比较生成的密文与预期的密文 `srtp_ciphertext`。
     *
     * @throws NoSuchPaddingException             如果使用的填充方式不可用。
     * @throws NoSuchAlgorithmException           如果加密算法不可用。
     * @throws InvalidAlgorithmParameterException 如果加密算法的参数无效。
     * @throws ShortBufferException               如果目标缓冲区的大小不足以容纳加密输出。
     * @throws IllegalBlockSizeException          如果加密数据块大小无效。
     * @throws BadPaddingException                如果填充模式无效。
     * @throws InvalidKeyException                如果加密密钥无效。
     */
    @Test
    public void srtpEncryptionTest() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, ShortBufferException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        ByteBuf byteBuf = encryptRtpPacket();
        byte[] bytes = new byte[38];
        byteBuf.getBytes(0, bytes);
        assertArrayEquals(srtp_ciphertext, bytes);
        log.info("加密后的数据：{}", ByteBufUtil.hexDump(byteBuf));
    }


    /**
     * 测试 SRTP (安全实时传输协议) 数据包的解密功能。
     * <p>
     * 此测试方法验证 `srtpDecryptTest()` 方法的正确性，确保解密后的数据包内容与原始明文数据一致。
     * 具体流程包括：
     * 1. 调用 `encryptRtpPacket()` 方法模拟生成加密的 RTP 数据包；
     * 2. 使用 `RtspPacket` 和 `SrtpContext` 创建解密需要的上下文和密钥；
     * 3. 解密 RTP 数据包并提取其有效载荷；
     * 4. 验证解密后的数据是否与预期明文 `rtp_plaintext_ref` 一致。
     *
     * @throws NoSuchPaddingException             如果使用的填充方式不可用。
     * @throws NoSuchAlgorithmException           如果解密算法不可用。
     * @throws InvalidAlgorithmParameterException 如果解密算法的参数无效。
     * @throws ShortBufferException               如果目标缓冲区的大小不足以容纳解密输出。
     * @throws IllegalBlockSizeException          如果解密数据块大小无效。
     * @throws BadPaddingException                如果填充模式无效。
     * @throws InvalidKeyException                如果解密密钥无效。
     */
    @Test
    public void srtpDecryptTest() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, ShortBufferException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        ByteBuf byteBuf = encryptRtpPacket();
        SrtpContextFactory srtpContextFactory = new SrtpContextFactory();
        srtpContextFactory.setSrtpProfilesType(srtpProfilesType);
        srtpContextFactory.setClientCipher(test_key, test_key_salt);
        SRtpContext srtpContext = srtpContextFactory.getClientSrtpContext(ssrc);


        SRtpPacket srtpPacket = new SRtpPacket(srtpProfilesType.rtpAuthTagLength);
        srtpPacket.setEncryptByteBuf(byteBuf);

        ByteBuf encrypt = srtpPacket.decrypt(srtpContext);



        assertArrayEquals(rtp_plaintext_ref, encrypt.array());
        log.info("解密后的新数据：{}", ByteBufUtil.hexDump(encrypt));

    }

    @Test
    public void sRtcpEncryptionTest() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, ShortBufferException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        ByteBuf byteBuf = encryptRtcpPacket();
        log.info("加密后的数据：{}", ByteBufUtil.hexDump(byteBuf));
        assertArrayEquals(srtcp_ciphertext, byteBuf.array());
    }

    @Test
    public void sRtcpDecryptTest() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, ShortBufferException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        ByteBuf byteBuf = encryptRtcpPacket();
        SrtpContextFactory srtpContextFactory = new SrtpContextFactory();
        srtpContextFactory.setSrtpProfilesType(srtpProfilesType);
        srtpContextFactory.setClientCipher(test_key, test_key_salt);
        SRtcpContext sRtcpContext = srtpContextFactory.getClientSRtcpContext(ssrc);

        SRtcpPacket sRtcpPacket = SRtcpFactory.parseBytebufToSrtcpPacket(byteBuf, srtpProfilesType.rtpAuthTagLength);
        ByteBuf decrypt = sRtcpPacket.decrypt(sRtcpContext);

        byte[] bytes = new byte[decrypt.readableBytes()];
        decrypt.readBytes(bytes);
        assertArrayEquals(rtcp_plaintext_ref, bytes);
        log.info("解密后的数据：{}", ByteBufUtil.hexDump(bytes));

    }

    /**
     * 对 RTCP 数据包进行加密操作。
     * 该方法采用 SRTP 协议实现 RTCP 数据包的加密，包括密钥上下文初始化、加密处理以及计算认证标签等步骤。
     *
     * @return 加密后的 RTCP 数据包，以 ByteBuf 格式返回。
     * @throws NoSuchPaddingException             如果使用的填充方式不可用。
     * @throws NoSuchAlgorithmException           如果加密算法不可用。
     * @throws InvalidAlgorithmParameterException 如果加密算法的参数无效。
     * @throws ShortBufferException               如果目标缓冲区的大小不足以容纳加密输出。
     * @throws IllegalBlockSizeException          如果加密数据块大小无效。
     * @throws BadPaddingException                如果填充模式无效。
     * @throws InvalidKeyException                如果加密密钥无效。
     */
    public ByteBuf encryptRtcpPacket() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, ShortBufferException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        SrtpContextFactory srtpContextFactory = new SrtpContextFactory();
        srtpContextFactory.setSrtpProfilesType(srtpProfilesType);
        srtpContextFactory.setServerCipher(test_key, test_key_salt);
        SRtcpContext sRtcpContext = srtpContextFactory.getServerSRtcpContext(ssrc);

        ByteBuf byteBuf = Unpooled.wrappedBuffer(rtcp_plaintext_ref);
        SRtcpPacket sRtcpPacket = new SRtcpPacket(srtpProfilesType.rtcpAuthTagLength);
        sRtcpPacket.setDecryptByteBuf(byteBuf);

        sRtcpPacket.encrypt(sRtcpContext);
        sRtcpContext.addSentIndex();
        int totalLength = sRtcpPacket.getTotalLength();
        ByteBuf buffer = Unpooled.buffer(totalLength);
        sRtcpPacket.writeTo(buffer);
        return buffer;
    }

    /**
     * 对 RTP 数据包进行加密操作。
     * 该方法采用 SRTP 协议实现 RTP 数据包的加密，包含密钥导出、加密处理以及认证标签计算等步骤。
     *
     * @return 加密后的 RTP 数据包，以 ByteBuf 格式返回。
     * @throws InvalidAlgorithmParameterException 如果加密算法的参数无效。
     * @throws ShortBufferException               如果目标缓冲区的大小不足以容
     */
    public ByteBuf encryptRtpPacket() throws InvalidAlgorithmParameterException, ShortBufferException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
        //初始化模拟工具
        SrtpContextFactory srtpContextFactory = new SrtpContextFactory();
        srtpContextFactory.setSrtpProfilesType(srtpProfilesType);
        srtpContextFactory.setServerCipher(test_key, test_key_salt);
        SRtpContext srtpContext = srtpContextFactory.getServerSrtpContext(ssrc);
        SRtpPacket srtpPacket = new SRtpPacket(srtpProfilesType.rtcpAuthTagLength);
        srtpPacket.setDecryptByteBuf(Unpooled.wrappedBuffer(rtp_plaintext_ref));
        return srtpPacket.encrypt(srtpContext);
    }


    private RtcpPacket conversionRtcpPacket() {
        ByteBuf byteBuf = Unpooled.wrappedBuffer(rtcp_plaintext);
        byte versionAndPaddingAndRc = byteBuf.readByte();
        byte version = (byte) (versionAndPaddingAndRc >> 6 & 0x03);
        byte padding = (byte) ((versionAndPaddingAndRc >> 5) & 0x01);
        byte countOrFmt = (byte) (versionAndPaddingAndRc & 0x1F);
        int payloadType = byteBuf.readUnsignedByte();
        int length = byteBuf.readShort();
        int packetLength = (length + 1) * 4;
        ByteBuf packetBody = byteBuf.readSlice(packetLength - 4);
        RtcpPayloadType rtcpPayloadType = RtcpPayloadType.fromValue(payloadType);
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


        return senderReportRtcpPacket;
    }

    private RtpPacket conversionRtpPacket() {
        ByteBuf byteBuf = Unpooled.wrappedBuffer(rtp_plaintext);
        RtpPacket rtpPacket = new RtpPacket();
        byte one = byteBuf.readByte();
        rtpPacket.setVersion((byte) ((one >> 6) & 0x03));
        rtpPacket.setPadding((byte) ((one >> 5) & 0x01));
        rtpPacket.setExtension((byte) ((one >> 4) & 0x01));
        rtpPacket.setCsrcCount((byte) (one & 0x0F));
        byte two = byteBuf.readByte();
        rtpPacket.setMarker((byte) ((two >> 7) & 0x01));
        rtpPacket.setPayloadType((byte) (two & 0x7f));
        rtpPacket.setSequenceNumber(byteBuf.readShort());
        rtpPacket.setTimestamp(byteBuf.readUnsignedInt());
        rtpPacket.setSsrc(byteBuf.readInt());
        rtpPacket.setPayload(byteBuf.readSlice(byteBuf.readableBytes() - srtpProfilesType.rtpAuthTagLength));
        return rtpPacket;
    }


}
