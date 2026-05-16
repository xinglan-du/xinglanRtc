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

 import io.netty.buffer.ByteBuf;
 import io.netty.buffer.Unpooled;
 import lombok.Getter;
 import lombok.Setter;
 import lombok.extern.slf4j.Slf4j;

 import javax.crypto.*;
 import javax.crypto.spec.IvParameterSpec;
 import javax.crypto.spec.SecretKeySpec;
 import java.nio.ByteBuffer;
 import java.security.*;

 /**
  * SRtpContext 类是 Secure Real-time Transport Protocol (SRTP) 上下文的核心实现。
  * 它提供了 SRTP 数据加密与认证的功能，包括密钥派生、加密负载处理以及包序列号相关的处理逻辑。
  * 该类广泛用于实时音视频传输中的保护数据安全。
  * <p>
  * 构造函数:
  * - 提供了上下文初始化所需的主要参数，包括 SSRC、密钥派生函数和操作模式。
  * <p>
  * 功能:
  * - 支持 SRTP 数据包负载的加密处理。
  * - 按照 SRTP 规范生成基于序列号的初始化向量 (IV)。
  * - 根据包序列号推测循环滚动计数器 (ROC) 的当前值。
  */
 @Slf4j
 public class SRtpContext {

     public static final int RTP_HEADER_LENGTH = 12;

     private final long ssrc;

     @Setter
     private int lastSeq = -1;

     private int roc = 0;

     private final Cipher cipher;

     private int opmode;

     private SecretKeySpec spec;

     @Getter
     private final SrtpKeyDerivationFunction kdf;

     private final Mac mac;

     @Getter
     private final int authTagLength;


     public SRtpContext(long ssrc, SrtpKeyDerivationFunction kdf, int opmode, int authTagLength) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
         this.ssrc = ssrc;
         this.opmode = opmode;
         this.kdf = kdf;
         this.authTagLength = authTagLength;
         kdf.calculationSaltKey(SrtpKeyDerivationFunction.LABEL_RTP_SALT);
         kdf.calculationAuthKey(SrtpKeyDerivationFunction.LABEL_RTP_MSG_AUTH);
         kdf.calculationEncKey(SrtpKeyDerivationFunction.LABEL_RTP_ENCRYPTION);
         this.spec = new SecretKeySpec(kdf.getEncKey(), "AES");
         Provider provider = Security.getProvider("SunJCE");
         cipher = Cipher.getInstance("AES/CTR/NoPadding", provider);
         mac = Mac.getInstance("HmacSHA1");
         mac.init(new SecretKeySpec(kdf.getAuthKey(), "HmacSHA1"));
     }

     /**
      * 计算并处理加密负载数据。
      *
      * @param inputBuffer    输入数据缓冲区，包含要处理的数据。
      * @param outputBuffer   输出数据缓冲区，用于存储处理后的数据。
      * @param sequenceNumber 数据包的序列号，用于生成初始化向量（IV）。
      * @return 已处理的输出数据长度。
      * @throws InvalidAlgorithmParameterException 如果加密算法的参数无效。
      * @throws InvalidKeyException                如果使用的密钥无效。
      * @throws ShortBufferException               如果输出缓冲区容量不足以存储处理后的数据。
      * @throws IllegalBlockSizeException          如果块大小非法（当使用分组加密时）。
      * @throws BadPaddingException                如果填充机制检测到错误的数据。
      */
     public int calculationPayload(ByteBuffer inputBuffer, ByteBuffer outputBuffer, int sequenceNumber) throws InvalidAlgorithmParameterException, InvalidKeyException, ShortBufferException, IllegalBlockSizeException, BadPaddingException {
         byte[] iv = calculationIV(sequenceNumber, kdf.getSaltKey());
         cipher.init(this.opmode, this.spec, new IvParameterSpec(iv));
         return cipher.doFinal(inputBuffer, outputBuffer);
     }


     /**
      * 根据序列号和盐值密钥计算初始化向量 (IV)。
      *
      * @param sequenceNumber 数据包的序列号，用于生成初始化向量的一部分。
      * @param saltKey        盐值密钥，用于混淆生成的初始化向量。
      * @return 计算得到的初始化向量 (IV)。
      */
     private byte[] calculationIV(int sequenceNumber, byte[] saltKey) {
         //使用48个bit
         long index = (((long) roc) << 16) | sequenceNumber;
         byte[] iv = new byte[16];
         // byte[] iv = new byte[16];
         iv[0] = saltKey[0];
         iv[1] = saltKey[1];
         iv[2] = saltKey[2];
         iv[3] = saltKey[3];
         int i;
         for (i = 4; i < 8; i++) {
             iv[i] = (byte) ((0xFF & (ssrc >> ((7 - i) * 8))) ^ saltKey[i]);
         }
         for (i = 8; i < 14; i++) {
             iv[i] = (byte) ((0xFF & (byte) (index >> ((13 - i) * 8))) ^ saltKey[i]);
         }
         iv[14] = iv[15] = 0;
         return iv;
     }


     /**
      * 根据当前上下文猜测此包的 roc（RFC/实现的常用做法）
      *
      * @param seq 本包的 16-bit 序列号（0..65535）
      * @return packetRoc 该包应使用的 ROC
      */
     public int guessPacketRoc(int seq) {
         if (lastSeq == -1) {
             // 第一个包，roc 默认为 0
             roc = 0;
             return roc;
         }

         // 差值用无符号处理思想（但用 long 避免溢出）
         if (seq < lastSeq) {
             // 可能发生 wrap（发送端已经回绕）
             if ((lastSeq - seq) > 32768) {
                 // highSeq 很大，seq 很小，超过半个环 -> 认为发送端已回绕
                 return roc += 1;
             } else {
                 return roc;
             }
         } else { // seq >= highestSeq
             if ((seq - lastSeq) > 32768) {
                 // seq 远大于 highestSeq（几乎不可能），说明 seq 应该属于前一轮
                 return Math.max(0, roc - 1);
             } else {
                 return roc;
             }
         }
     }

     public boolean contrastAuthTag(ByteBuf data) {
         int sequenceNumber = data.getUnsignedShort(2);
         int roc = guessPacketRoc(sequenceNumber);
         int length = data.readableBytes() - authTagLength;
         byte[] bytes = SrtpUtils.calculateAuthTag(data.nioBuffer(), length, getKdf().getAuthKey(), roc, authTagLength);
         byte[] authTag = getAuthTag(data);
         return MessageDigest.isEqual(authTag, bytes);
     }


     private byte[] getAuthTag(ByteBuf data) {
         byte[] authTag = new byte[authTagLength];
         data.getBytes(data.readableBytes() - authTagLength, authTag);
         return authTag;
     }

     /**
      * 解密给定的 SRTP 数据包。
      *
      * @param data 要解密的输入数据缓冲区，包含加密的 SRTP 数据包。
      * @return 解密后的数据缓冲区，包含有效负载数据和解密后的内容。
      * @throws InvalidAlgorithmParameterException 如果加密算法的参数无效。
      * @throws ShortBufferException               如果输出缓冲区容量不足以存储解密后的数据。
      * @throws IllegalBlockSizeException          如果块大小非法（当使用分组加密时）。
      * @throws BadPaddingException                如果填充机制检测到错误的数据。
      * @throws InvalidKeyException                如果使用的密钥无效。
      */
     public ByteBuf decrypt(ByteBuf data) throws InvalidAlgorithmParameterException, ShortBufferException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
         int decryptLength = data.readableBytes() - this.authTagLength;
         ByteBuf decryptByteBuf = Unpooled.buffer(decryptLength);
         decryptByteBuf.writeBytes(data, 0, RTP_HEADER_LENGTH);
         int sequenceNumber = data.getUnsignedShort(2);
         int payloadLength = decryptLength - RTP_HEADER_LENGTH;
         int i = calculationPayload(data.nioBuffer(RTP_HEADER_LENGTH, payloadLength), decryptByteBuf.nioBuffer(RTP_HEADER_LENGTH, payloadLength), sequenceNumber);
         decryptByteBuf.writerIndex(decryptByteBuf.writerIndex() + i);
         lastSeq = sequenceNumber;
         return decryptByteBuf;
     }

     public void encrypt(ByteBuf decryptByteBuf, ByteBuf encryptBytebuf) throws InvalidAlgorithmParameterException, ShortBufferException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {

         encryptBytebuf.writeBytes(decryptByteBuf, 0, RTP_HEADER_LENGTH);
         int sequenceNumber = decryptByteBuf.getUnsignedShort(2);
         int payloadLength = decryptByteBuf.readableBytes() - RTP_HEADER_LENGTH;
         int i = calculationPayload(decryptByteBuf.nioBuffer(RTP_HEADER_LENGTH, payloadLength), encryptBytebuf.nioBuffer(RTP_HEADER_LENGTH, payloadLength), sequenceNumber);
         encryptBytebuf.writerIndex(encryptBytebuf.writerIndex() + i);
         int roc = guessPacketRoc(sequenceNumber);
         int length = decryptByteBuf.readableBytes();
         byte[] authTag = SrtpUtils.calculateAuthTag(encryptBytebuf.nioBuffer(), length, getKdf().getAuthKey(), roc, authTagLength);
         encryptBytebuf.writeBytes(authTag);

     }
 }
