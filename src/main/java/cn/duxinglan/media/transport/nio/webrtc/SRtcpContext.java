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
 package cn.duxinglan.media.transport.nio.webrtc;

 import cn.duxinglan.media.protocol.srtcp.SRtcpUtils;
 import io.netty.buffer.ByteBuf;
 import io.netty.buffer.Unpooled;
 import lombok.Data;

 import javax.crypto.*;
 import javax.crypto.spec.IvParameterSpec;
 import javax.crypto.spec.SecretKeySpec;
 import java.nio.ByteBuffer;
 import java.security.*;

 /**
  * SRtcpContext 是一个用于处理 SRTP/RTCPS 安全计算和加密解密操作的上下文类。
  * 它包含对 RTCP 数据包的加密、解密、身份验证标签生成和验证等操作的实现。
  * <p>
  * 主要功能包括：
  * 1. 基于密钥派生函数（Key Derivation Function, KDF）生成密钥和盐值。
  * 2. 提供基于 AES/CTR/NoPadding 的加密和解密功能。
  * 3. 对 RTCP 数据包进行身份验证计算和校验。
  */
 @Data
 public class SRtcpContext {

     /**
      * 常量 S_RTCP_HEADER_LENGTH 定义了 RTCP（Real-time Transport Control Protocol）数据包的头部长度。
      * <p>
      * 此长度是根据 RTCP 的协议规范制定的，通常用于解析或生成 RTCP 数据包时，对头部部分进行识别和处理。
      * <p>
      * 值为 8，表示头部固定占用 8 字节。
      */
     public static final int S_RTCP_HEADER_LENGTH = 8;

     /**
      * 表示 RTCP (Real-Time Control Protocol) 中索引字段的长度。
      * <p>
      * 该常量通常用于处理与 RTCP 数据包相关的索引字段，以确保数据包的正确解析和操作。
      * 此长度值定义了 RTCP 索引的固定字节数。
      */
     public static final int S_RTCP_INDEX_LENGTH = 4;

     /**
      * 表示 SRTP (Secure Real-time Transport Protocol) 会话上下文中用于标识源端点的 SSRC (Synchronization Source) 值。
      * <p>
      * SSRC 是 RTP 数据流的唯一标识符，用于区分相同时间同步内的不同数据流。
      * 在 SRTP 中，该字段用来关联加密和认证操作的上下文。
      * <p>
      * 注意：SSRC 值在每个 SRTP 会话中应保持唯一性。
      */
     private final long ssrc;

     private int lastSeq = -1;


     private final Cipher cipher;


     private int opmode;

     private SecretKeySpec spec;

     private SrtpKeyDerivationFunction kdf;

     private int sentIndex = 1;

     private final int authTagLength;


     public SRtcpContext(long ssrc, SrtpKeyDerivationFunction kdf, int opmode, int authTagLength) throws NoSuchPaddingException, NoSuchAlgorithmException {
         this.ssrc = ssrc;
         this.opmode = opmode;
         this.kdf = kdf;
         this.authTagLength = authTagLength;
         kdf.calculationSaltKey(SrtpKeyDerivationFunction.LABEL_RTCP_SALT);
         kdf.calculationAuthKey(SrtpKeyDerivationFunction.LABEL_RTCP_MSG_AUTH);
         kdf.calculationEncKey(SrtpKeyDerivationFunction.LABEL_RTCP_ENCRYPTION);
         this.spec = new SecretKeySpec(kdf.getEncKey(), "AES");
         Provider provider = Security.getProvider("SunJCE");
         cipher = Cipher.getInstance("AES/CTR/NoPadding", provider);
     }


     /**
      * 对输入缓冲区的数据进行加密或解密操作，并将结果存储到输出缓冲区中。
      *
      * @param inputBuffer  包含待处理数据的输入缓冲区。
      * @param outputBuffer 用于存储处理结果的输出缓冲区。
      * @return 处理后的字节数。
      * @throws InvalidAlgorithmParameterException 如果提供的算法参数无效。
      * @throws InvalidKeyException                如果提供的密钥无效。
      * @throws ShortBufferException               如果输出缓冲区的长度不足以存储结果。
      * @throws IllegalBlockSizeException          如果数据块的大小不符合要求。
      * @throws BadPaddingException                如果数据的填充无效或不正确。
      */
     public int calculationPayload(ByteBuffer inputBuffer, ByteBuffer outputBuffer) throws InvalidAlgorithmParameterException, InvalidKeyException, ShortBufferException, IllegalBlockSizeException, BadPaddingException {
         int index = sentIndex;
         byte[] iv = calculationIV(index, kdf.getSaltKey());
         cipher.init(this.opmode, this.spec, new IvParameterSpec(iv));
         return cipher.doFinal(inputBuffer, outputBuffer);
     }


     /**
      * 使用指定的缓冲区加密或解密数据，并返回处理的字节数。
      *
      * @param buffer 输入和输出的共享缓冲区，包含待处理的数据。
      * @param off    数据处理起始位置的偏移量。
      * @param length 要处理的数据长度（字节数）。
      * @return 处理的字节数。
      * @throws InvalidAlgorithmParameterException 如果提供的算法参数无效。
      * @throws InvalidKeyException                如果提供的密钥无效。
      * @throws ShortBufferException               如果输出缓冲区长度不足以存储结果。
      * @throws IllegalBlockSizeException          如果处理的数据块大小无效。
      * @throws BadPaddingException                如果数据未正确填充。
      */
     public int calculationPayload(ByteBuffer buffer, int off, int length) throws InvalidAlgorithmParameterException, InvalidKeyException, ShortBufferException, IllegalBlockSizeException, BadPaddingException {
         int index = sentIndex;
         byte[] iv = calculationIV(index, kdf.getSaltKey());
         cipher.init(this.opmode, this.spec, new IvParameterSpec(iv));

         ByteBuffer input = buffer.duplicate();
         ByteBuffer output = buffer.duplicate();

         input.position(off);
         input.limit(off + length);

         output.position(off);
         output.limit(off + length);
         return cipher.doFinal(input, output);
     }

     public void addSentIndex() {
         sentIndex++;
         sentIndex &= ~0x80000000;
     }

     private byte[] calculationIV(int index, byte[] saltKey) {

         byte[] iv = new byte[16];
         iv[0] = saltKey[0];
         iv[1] = saltKey[1];
         iv[2] = saltKey[2];
         iv[3] = saltKey[3];

         // The shifts transform the ssrc and index into network order
         iv[4] = (byte) (((ssrc >> 24) & 0xff) ^ saltKey[4]);
         iv[5] = (byte) (((ssrc >> 16) & 0xff) ^ saltKey[5]);
         iv[6] = (byte) (((ssrc >> 8) & 0xff) ^ saltKey[6]);
         iv[7] = (byte) ((ssrc & 0xff) ^ saltKey[7]);

         iv[8] = saltKey[8];
         iv[9] = saltKey[9];

         iv[10] = (byte) (((index >> 24) & 0xff) ^ saltKey[10]);
         iv[11] = (byte) (((index >> 16) & 0xff) ^ saltKey[11]);
         iv[12] = (byte) (((index >> 8) & 0xff) ^ saltKey[12]);
         iv[13] = (byte) ((index & 0xff) ^ saltKey[13]);

         iv[14] = iv[15] = 0;
         return iv;
     }

     public boolean contrastAuthTag(ByteBuf data) {
         int sRtcpIndex = SRtcpUtils.getSRtcpIndex(data, authTagLength);
         int index = sRtcpIndex | 0x80000000;
         int length = data.readableBytes() - S_RTCP_INDEX_LENGTH - authTagLength;
         byte[] bytes = SrtpUtils.calculateAuthTag(data.nioBuffer(), length, getKdf().getAuthKey(), index, authTagLength);
         byte[] authTag = getAuthTag(data);
         return MessageDigest.isEqual(authTag, bytes);
     }


     private byte[] getAuthTag(ByteBuf data) {
         byte[] authTag = new byte[authTagLength];
         data.getBytes(data.readableBytes() - authTagLength, authTag);
         return authTag;
     }

     public ByteBuf decrypt(ByteBuf data) throws InvalidAlgorithmParameterException, ShortBufferException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
         int payloadLength = data.readableBytes() - S_RTCP_HEADER_LENGTH - S_RTCP_INDEX_LENGTH - authTagLength;
         ByteBuf decryptByteBuf = Unpooled.buffer(data.readableBytes() - S_RTCP_INDEX_LENGTH - authTagLength);
         decryptByteBuf.writeBytes(data, 0, S_RTCP_HEADER_LENGTH);
         int sRtcpIndex = SRtcpUtils.getSRtcpIndex(data, authTagLength);
         sentIndex = sRtcpIndex & ~0x80000000;
         int i = calculationPayload(data.nioBuffer(S_RTCP_HEADER_LENGTH, payloadLength), decryptByteBuf.nioBuffer(S_RTCP_HEADER_LENGTH, payloadLength));
         decryptByteBuf.writerIndex(decryptByteBuf.writerIndex() + i);
         return decryptByteBuf;

     }

     public void encrypt(ByteBuf decryptByteBuf, ByteBuf encryptByteBuf) throws InvalidAlgorithmParameterException, ShortBufferException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
         encryptByteBuf.writeBytes(decryptByteBuf, 0, S_RTCP_HEADER_LENGTH);
         int e = 1 << 31;
         int sRtcpIndex = getSentIndex() | e;
         int payloadLength = decryptByteBuf.readableBytes() - SRtcpContext.S_RTCP_HEADER_LENGTH;
         int i = calculationPayload(decryptByteBuf.nioBuffer(S_RTCP_HEADER_LENGTH, payloadLength), encryptByteBuf.nioBuffer(S_RTCP_HEADER_LENGTH, payloadLength));
         encryptByteBuf.writerIndex(encryptByteBuf.writerIndex() + i);
         encryptByteBuf.writeInt(sRtcpIndex);
         int index = sRtcpIndex | 0x80000000;
         int length = SRtcpContext.S_RTCP_HEADER_LENGTH + payloadLength;
         byte[] authTag = SrtpUtils.calculateAuthTag(encryptByteBuf.nioBuffer(), length, getKdf().getAuthKey(), index, authTagLength);
         encryptByteBuf.writeBytes(authTag);
         addSentIndex();
     }


 }
