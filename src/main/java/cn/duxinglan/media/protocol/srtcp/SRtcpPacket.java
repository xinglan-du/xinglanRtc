package cn.duxinglan.media.protocol.srtcp;

import cn.duxinglan.media.core.INetworkPacket;
import cn.duxinglan.media.transport.nio.webrtc.SRtcpContext;
import cn.duxinglan.media.transport.nio.webrtc.SrtpUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
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
public class SRtcpPacket implements INetworkPacket {

    public static final int S_RTCP_HEADER_LENGTH = 8;


    /**
     * 表示当前 SRtcp 数据包是否已被成功解密的标志。
     * <p>
     * 字段值为 true 时，表示数据包已解密；
     * 字段值为 false 时，表示数据包尚未解密或解密失败。
     */
//    private boolean decrypted;

    /**
     * 解密后的 ByteBuf 缓冲区实例。
     * <p>
     * 此变量用于存储解密操作的结果，包含解密后的 SRtcp 数据内容。
     * 在进行解密操作时，数据将从源缓冲区中提取并解密后存入该缓冲区。
     * 可通过相关方法读取或操作解密后的数据。
     * <p>
     * 该变量的值可通过 {@link #setDecryptByteBuf(ByteBuf)} 方法设置。
     */
    private ByteBuf decryptByteBuf;

    /**
     * 表示加密后的数据缓冲区。
     * <p>
     * 此字段存储通过加密操作生成的加密数据。加密过程使用特定的 SRtcp 加密上下文，包含必要的密钥和参数。
     * 在加密过程完成后，该缓冲区将被用于后续的数据传输或存储。
     * <p>
     * 该字段通常在调用加密方法时进行填充，例如 {@link #encrypt(SRtcpContext)}。
     */
    private ByteBuf encryptByteBuf;


    @Getter
//    private ByteBuf sourceByteBuf;


    /**
     * 表示 SRtcpPacket 数据包的索引字段。
     * <p>
     * 该变量通常用于标识 SRtcp 数据包的序列号或内部顺序编号，以支持数据包的排序、
     * 去重和完整性检查。索引字段能够确保 SRtcp 数据包在网络传输中被正确解析
     * 和处理。
     * <p>
     * 该值通常是一个 32 位无符号整数，根据协议规范，在数据包构建或解析时被设置或更新。
     * 在实际应用中，该索引需与加密标志位（sRtcpEFlag）和认证标签（authTag）
     * 等字段结合使用，以保障数据包的安全性和正确性。
     */
    private int sRtcpIndex;

    /**
     * 表示 SRtcpPacket 数据包的身份验证标签（Authentication Tag）。
     * <p>
     * 该变量用于存储 SRtcp 数据包的认证标签信息，是一种数据完整性和安全性保护机制。
     * 在 SRtcp 协议中，authTag 通常通过加密算法（例如 HMAC 或 AES-GCM）生成，
     * 用于验证数据包的真实性以及确保其未被篡改。
     * <p>
     * 在发送数据包时，authTag 通常基于数据包内容和密钥计算生成，并附加到数据包末尾。
     * 接收方解析数据包时，会重新计算 authTag，并与接收到的值进行比较。
     * 如果两者匹配，则表明数据包未被篡改且来源可信。
     * <p>
     * 使用注意事项：
     * 1. 生成和验证 authTag 时需要确保双方使用相同的密钥和加密算法。
     * 2. 在解析数据包之前，应正确提取并保留 authTag 内容，避免丢失或数据损坏。
     * 3. authTag 的长度通常由加密算法决定，使用时需与协议规范保持一致。
     */
    private byte[] authTag;

    private final int authTagLength;

    private int payloadLength;

    public SRtcpPacket(int authTagLength) {
        this.authTagLength = authTagLength;
        authTag = new byte[authTagLength];

    }

    /*public void setEncryptionSourceByteBuf(ByteBuf sourceByteBuf) {
        setEncryptionSourceByteBuf(sourceByteBuf, false);
    }*/


    public void setDecryptByteBuf(ByteBuf decryptByteBuf) {
        this.decryptByteBuf = decryptByteBuf;
        payloadLength = decryptByteBuf.readableBytes() - SRtcpFactory.S_RTCP_HEADER_LENGTH;
    }

    public void setEncryptByteBuf(ByteBuf encryptByteBuf) {
        this.encryptByteBuf = encryptByteBuf;
        payloadLength = encryptByteBuf.readableBytes() - SRtcpFactory.S_RTCP_HEADER_LENGTH - SRtcpFactory.S_RTCP_INDEX_LENGTH - authTagLength;
        sRtcpIndex = SRtcpUtils.getSRtcpIndex(encryptByteBuf, authTagLength);
        encryptByteBuf.getBytes(encryptByteBuf.readableBytes() - authTagLength, authTag);
    }


    /**
     * 设置加密的源缓冲区，并初始化相关字段。
     *
     * @param sourceByteBuf 加密的源缓冲区，包含 SRtcp 数据。
     */
   /* public void setEncryptionSourceByteBuf(ByteBuf sourceByteBuf, boolean decrypted) {
        this.sourceByteBuf = sourceByteBuf;
        this.decrypted = decrypted;
        payloadLength = sourceByteBuf.readableBytes() - SRtcpFactory.S_RTCP_HEADER_LENGTH - SRtcpFactory.S_RTCP_INDEX_LENGTH - authTagLength;
        sRtcpIndex = SRtcpUtils.getSRtcpIndex(sourceByteBuf, authTagLength);
        sourceByteBuf.getBytes(sourceByteBuf.readableBytes() - authTagLength, authTag);
    }*/

    /**
     * 比较存储的认证标签与使用给定上下文计算所得的认证标签是否一致。
     *
     * @param sRtcpContext 用于计算认证标签的 SRtcp 上下文，包含必要的密钥和加密参数。
     * @return 如果存储的认证标签与计算出的认证标签一致，则返回 true；否则返回 false。
     */
    public boolean contrastAuthTag(SRtcpContext sRtcpContext) {
        int index = sRtcpIndex | 0x80000000;
        int length = SRtcpFactory.S_RTCP_HEADER_LENGTH + payloadLength;
        byte[] bytes = SrtpUtils.calculateAuthTag(encryptByteBuf.nioBuffer(), length, sRtcpContext.getKdf().getAuthKey(), index, authTagLength);
        return MessageDigest.isEqual(authTag, bytes);
    }


    /**
     * 计算并生成 SRtcp 数据包的认证标签（Auth Tag）。
     *
     * @param sRtcpContext 用于计算认证标签的 SRtcp 上下文，包含密钥派生函数和所需的认证密钥。
     */
    public void calculateAuthTag(SRtcpContext sRtcpContext) {
        int index = sRtcpIndex | 0x80000000;
        int length = SRtcpFactory.S_RTCP_HEADER_LENGTH + payloadLength;
        authTag = SrtpUtils.calculateAuthTag(encryptByteBuf.nioBuffer(), length, sRtcpContext.getKdf().getAuthKey(), index, authTagLength);
        encryptByteBuf.writeBytes(authTag);
    }


    @Override
    public int getTotalLength() {
//        return SRtcpFactory.S_RTCP_HEADER_LENGTH + sourceByteBuf.readableBytes() + SRtcpFactory.S_RTCP_INDEX_LENGTH + authTagLength;
        return encryptByteBuf.readableBytes();
    }

    @Override
    public void writeTo(ByteBuf out) {
        if (encryptByteBuf == null) {
            throw new IllegalArgumentException("encryptByteBuf未设置");
        }
        out.writeBytes(encryptByteBuf);
    }


    /**
     * 解密当前 SRtcp 数据包的方法。
     *
     * @param sRtcpContext SRtcp 上下文，包含加密密钥、加密参数及解密操作所需的信息。
     * @return 解密后的 ByteBuf 缓冲区，包含解密后的数据内容。
     * @throws InvalidAlgorithmParameterException 如果提供的算法参数无效。
     * @throws ShortBufferException               如果输出缓冲区的长度不足以存储解密结果。
     * @throws IllegalBlockSizeException          如果数据块大小不合法。
     * @throws BadPaddingException                如果解密的数据填充无效。
     * @throws InvalidKeyException                如果解密所使用的密钥无效。
     */
    public ByteBuf decrypt(SRtcpContext sRtcpContext) throws InvalidAlgorithmParameterException, ShortBufferException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        if (this.decryptByteBuf != null) {
            return this.decryptByteBuf.slice();
        }

        if (this.encryptByteBuf == null) {
            throw new IllegalArgumentException("encryptByteBuf未设置");
        }
        this.decryptByteBuf = Unpooled.buffer(this.encryptByteBuf.readableBytes() - SRtcpFactory.S_RTCP_INDEX_LENGTH - authTagLength);
        //先写入没有加密的数据
        this.decryptByteBuf.writeBytes(this.encryptByteBuf, 0, S_RTCP_HEADER_LENGTH);

        sRtcpContext.setSentIndex(this.sRtcpIndex & ~0x80000000);

        int i = sRtcpContext.calculationPayload(this.encryptByteBuf.nioBuffer(S_RTCP_HEADER_LENGTH, payloadLength), this.decryptByteBuf.nioBuffer(S_RTCP_HEADER_LENGTH, payloadLength));
        decryptByteBuf.writerIndex(decryptByteBuf.writerIndex() + i);
//        int i = sRtcpContext.calculationPayload(this.encryptByteBuf.nioBuffer(), SRtcpFactory.S_RTCP_HEADER_LENGTH, payloadLength);
        return this.decryptByteBuf.slice();
    }

    public ByteBuf encrypt(SRtcpContext sRtcpContext) throws InvalidAlgorithmParameterException, ShortBufferException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        if (this.encryptByteBuf != null) {
            return this.encryptByteBuf.slice();
        }
        if (this.decryptByteBuf == null) {
            throw new IllegalArgumentException("decryptByteBuf未设置");
        }
        encryptByteBuf = Unpooled.buffer(this.decryptByteBuf.readableBytes() + SRtcpFactory.S_RTCP_INDEX_LENGTH + authTagLength);

        this.encryptByteBuf.writeBytes(this.decryptByteBuf, 0, S_RTCP_HEADER_LENGTH);

        int e = 1 << 31; // WebRTC 一定是 1
        sRtcpIndex = sRtcpContext.getSentIndex() | e;

        int i = sRtcpContext.calculationPayload(this.decryptByteBuf.nioBuffer(S_RTCP_HEADER_LENGTH, payloadLength), this.encryptByteBuf.nioBuffer(S_RTCP_HEADER_LENGTH, payloadLength));
        encryptByteBuf.writerIndex(encryptByteBuf.writerIndex() + i);
        encryptByteBuf.writeInt(sRtcpIndex);

//        int i = sRtcpContext.calculationPayload(this.sourceByteBuf.nioBuffer(), SRtcpFactory.S_RTCP_HEADER_LENGTH, payloadLength);
//        decrypted = false;
        calculateAuthTag(sRtcpContext);
        return this.encryptByteBuf.slice();
    }

    public long getEncryptSsrc() {
        return SRtcpUtils.getSsrc(this.encryptByteBuf);

    }

    public long getDecryptSsrc() {
        return SRtcpUtils.getSsrc(this.decryptByteBuf);

    }


    public ByteBuf getDecrypt() {
        return this.decryptByteBuf;
    }
}
