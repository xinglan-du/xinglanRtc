package cn.duxinglan.media.protocol.srtp;

import cn.duxinglan.media.core.INetworkPacket;
import cn.duxinglan.media.transport.nio.webrtc.SRtpContext;
import cn.duxinglan.media.transport.nio.webrtc.SrtpUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;

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
@Data
public class SRtpPacket implements INetworkPacket {

    public static final int RTP_HEADER_LENGTH = 12;


    /**
     * 表示一个用于存储解密后数据的 {@code ByteBuf} 对象。
     * 该字段在 SRTP 数据包的解密过程中被填充，用于暂存解密后的有效负载数据。
     * 解密后的数据可以通过该字段进行后续的处理或传递。
     * <p>
     * 该字段主要用于 SRTP 数据包的解析流程，并依赖于底层的解密算法及上下文环境。
     */
    private ByteBuf decryptByteBuf;

    /**
     * 加密后的数据缓冲区，用于存储经过 SRTP 加密处理后的数据内容。
     * 在 SRTP 数据包的封装或传输过程中，此缓冲区主要用于临时保存加密后的负载数据。
     * <p>
     * 该字段在初始化时通常为空，需通过相关方法赋值为加密后的 {@code ByteBuf}。
     */
    private ByteBuf encryptByteBuf;

    private final int authTagLength;

    public SRtpPacket(int authTagLength) {
        this.authTagLength = authTagLength;
    }

    @Override
    public int getTotalLength() {
        return encryptByteBuf.readableBytes();
    }

    @Override
    public void writeTo(ByteBuf out) {
        if (encryptByteBuf == null) {
            throw new IllegalArgumentException("encryptByteBuf未设置");
        }
        out.writeBytes(encryptByteBuf);
    }

    public ByteBuf decrypt(SRtpContext srtpContext) throws InvalidAlgorithmParameterException, ShortBufferException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        if (this.decryptByteBuf != null) {
            return this.decryptByteBuf.slice();
        }
        if (this.encryptByteBuf == null) {
            throw new IllegalArgumentException("encryptByteBuf未设置");
        }
        //这里要获取到原始数据长度，就是加密的数据-authTag
        int decryptLength = this.encryptByteBuf.readableBytes() - this.authTagLength;
        this.decryptByteBuf = Unpooled.buffer(decryptLength);
        this.decryptByteBuf.writeBytes(this.encryptByteBuf, 0, RTP_HEADER_LENGTH);
        int sequenceNumber = this.encryptByteBuf.getUnsignedShort(2);

        int payloadLength = decryptLength - RTP_HEADER_LENGTH;

        int i = srtpContext.calculationPayload(this.encryptByteBuf.nioBuffer(RTP_HEADER_LENGTH, payloadLength), decryptByteBuf.nioBuffer(RTP_HEADER_LENGTH, payloadLength), sequenceNumber);
        decryptByteBuf.writerIndex(decryptByteBuf.writerIndex() + i);
        return this.decryptByteBuf.slice();
    }


    public ByteBuf encrypt(SRtpContext srtpContext) throws InvalidAlgorithmParameterException, ShortBufferException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        if (this.encryptByteBuf != null) {
            return this.encryptByteBuf.slice();
        }
        if (this.decryptByteBuf == null) {
            throw new IllegalArgumentException("decryptByteBuf未设置");
        }
        int encryptLength = this.decryptByteBuf.readableBytes() + this.authTagLength;
        this.encryptByteBuf = Unpooled.buffer(encryptLength);
        this.encryptByteBuf.writeBytes(this.decryptByteBuf, 0, RTP_HEADER_LENGTH);
        int sequenceNumber = this.decryptByteBuf.getUnsignedShort(2);
        int payloadLength = this.decryptByteBuf.readableBytes() - RTP_HEADER_LENGTH;

        int i = srtpContext.calculationPayload(this.decryptByteBuf.nioBuffer(RTP_HEADER_LENGTH, payloadLength), encryptByteBuf.nioBuffer(RTP_HEADER_LENGTH, payloadLength), sequenceNumber);
        encryptByteBuf.writerIndex(encryptByteBuf.writerIndex() + i);

        int roc = srtpContext.guessPacketRoc(sequenceNumber);
        int length = encryptLength - authTagLength;
        byte[] authTag = SrtpUtils.calculateAuthTag(encryptByteBuf.nioBuffer(), length, srtpContext.getKdf().getAuthKey(), roc, authTagLength);
        encryptByteBuf.writeBytes(authTag);
        return encryptByteBuf.slice();
    }


    /**
     * 对比 SRTP 数据包中的认证标签（Auth Tag）和通过计算生成的标签，以验证数据包的完整性和认证。
     *
     * @param srtpContext SRTP 上下文对象，包含相关的密钥、加密算法和状态信息。
     * @return 如果认证标签匹配，返回 true；否则返回 false。
     */
    public boolean contrastAuthTag(SRtpContext srtpContext) {
        int sequenceNumber = getEncryptSequenceNumber();
        int roc = srtpContext.guessPacketRoc(sequenceNumber);
        int length = encryptByteBuf.readableBytes() - authTagLength;
        byte[] bytes = SrtpUtils.calculateAuthTag(encryptByteBuf.nioBuffer(), length, srtpContext.getKdf().getAuthKey(), roc, authTagLength);
        byte[] encryptAuthTag = getEncryptAuthTag();
        return MessageDigest.isEqual(encryptAuthTag, bytes);
    }


    /**
     * 获取加密序列号（Sequence Number）。
     * <p>
     * 此方法从缓存中的加密数据中提取无符号的 16 位整数，
     * 从而返回序列号，用于标识 SRTP 数据包的顺序。
     *
     * @return 加密数据包的序列号，表示为一个无符号 16 位整数。
     */
    public int getEncryptSequenceNumber() {
        return encryptByteBuf.getUnsignedShort(2);
    }

    public byte[] getEncryptAuthTag() {
        byte[] authTag = new byte[authTagLength];
        encryptByteBuf.getBytes(encryptByteBuf.readableBytes() - authTagLength, authTag);
        return authTag;
    }

    public int getDecryptSequenceNumber() {
        return decryptByteBuf.getUnsignedShort(2);
    }

    public long getEncryptSsrc() {
        return encryptByteBuf.getUnsignedInt(8);
    }

    public long getDecryptSsrc() {
        return decryptByteBuf.getUnsignedInt(8);
    }
}
