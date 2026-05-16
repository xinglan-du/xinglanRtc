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
 import lombok.extern.slf4j.Slf4j;

 import javax.crypto.Mac;
 import javax.crypto.spec.SecretKeySpec;
 import java.nio.ByteBuffer;

 /**
  * SrtpUtils 类提供了与 SRTP（Secure Real-time Transport Protocol）协议相关的工具方法。
  * 它主要包含以下功能：
  * <p>
  * 1. 认证标签（Auth Tag）的计算。
  * 2. Payload 类型判断是否属于 SRTP 范围。
  */
 @Slf4j
 public class SrtpUtils {


     /**
      * 计算 SRTP Auth Tag（支持 DirectByteBuffer）
      *
      * @param buffer      输入数据（可为堆内或直接内存）
      * @param length      参与认证的数据长度（不包含 auth tag）
      * @param authKey     认证密钥（HMAC-SHA1 密钥）
      * @param roc         Roll-over Counter（4字节，大端序）
      * @param authTagSize 输出 AuthTag 的长度（SRTP 通常为 10）
      * @return 截断后的 Auth Tag
      */
     public static byte[] calculateAuthTag(ByteBuffer buffer, int length,
                                           byte[] authKey, long roc, int authTagSize) {
         try {
             // 初始化 HMAC-SHA1
             Mac mac = Mac.getInstance("HmacSHA1");
             mac.init(new SecretKeySpec(authKey, "HmacSHA1"));

             // 计算数据部分
             int oldLimit = buffer.limit();
             int end = buffer.position() + length;
             if (end < oldLimit) {
                 buffer.limit(end);
             }
             mac.update(buffer); // ✅ 直接支持堆内和直接内存 ByteBuffer
             buffer.limit(oldLimit); // 恢复原 limit
             // 加上 ROC（大端序）
             byte[] rocBytes = new byte[]{
                     (byte) (roc >> 24),
                     (byte) (roc >> 16),
                     (byte) (roc >> 8),
                     (byte) roc
             };
             mac.update(rocBytes);

             // 输出完整 MAC（20 字节）
             byte[] fullTag = mac.doFinal();
             // 截断到指定长度（SRTP 默认 10）
             byte[] truncated = new byte[authTagSize];
             System.arraycopy(fullTag, 0, truncated, 0, authTagSize);
             return truncated;

         } catch (Exception e) {
             throw new RuntimeException("计算 SRTP AuthTag 失败", e);
         }
     }

     /**
      * 判断指定的 Payload Type 是否属于 SRTP（Secure Real-time Transport Protocol）的范围。
      *
      * @param payloadType 一个整数，表示 RTP 数据包的 Payload Type。
      * @return 如果该 Payload Type 属于 SRTP 范围，则返回 true；否则返回 false。
      */
     public static boolean isSrtp(int payloadType) {
         return payloadType < 200 || payloadType > 207;
     }


     public static long getRtpSsrc(ByteBuf data) {
         return data.getUnsignedInt(8);
     }
 }
