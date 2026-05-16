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


 import cn.duxinglan.media.core.ChannelContext;
 import cn.duxinglan.media.core.MediaSession;
 import cn.duxinglan.media.impl.dtls.SrtpProfilesType;
 import cn.duxinglan.media.protocol.rtcp.RtcpGroupPacket;
 import cn.duxinglan.media.protocol.rtcp.RtcpPacket;
 import cn.duxinglan.media.protocol.rtp.RtpPacket;
 import cn.duxinglan.media.protocol.srtcp.SRtcpUtils;
 import cn.duxinglan.media.transport.nio.webrtc.SRtcpContext;
 import cn.duxinglan.media.transport.nio.webrtc.SRtpContext;
 import cn.duxinglan.media.transport.nio.webrtc.SrtpContextFactory;
 import cn.duxinglan.media.transport.nio.webrtc.SrtpUtils;
 import cn.duxinglan.media.transport.udp.InboundPacket;
 import io.netty.buffer.ByteBuf;
 import lombok.extern.slf4j.Slf4j;
 import org.jspecify.annotations.NonNull;

 import javax.crypto.BadPaddingException;
 import javax.crypto.IllegalBlockSizeException;
 import javax.crypto.NoSuchPaddingException;
 import javax.crypto.ShortBufferException;
 import java.security.InvalidAlgorithmParameterException;
 import java.security.InvalidKeyException;
 import java.security.NoSuchAlgorithmException;


 @Slf4j
 public class SrtpProcessor {


     private SrtpContextFactory srtpContextFactory;

     private final RtpProcessor rtpProcessor;

     private final RtcpProcessor rtcpProcessor;

     private final MediaSession session;

     private final ChannelContext channelContext;


     public SrtpProcessor(ChannelContext channelContext, SrtpProfilesType srtpProfilesType, byte[] keyingMaterial, MediaSession session) throws NoSuchPaddingException, NoSuchAlgorithmException {
         this.channelContext = channelContext;
         this.srtpContextFactory = new SrtpContextFactory(srtpProfilesType, keyingMaterial);
         this.session = session;
         rtpProcessor = new RtpProcessor(session);
         rtcpProcessor = new RtcpProcessor(session);
     }

     public void receiveProcess(InboundPacket data) {
         ByteBuf buf = data.packet();
         //判断数据类型，进行解密
         int pt = buf.getByte(1) & 0xFF;
         if (SrtpUtils.isSrtp(pt)) {
             receiveProcessRtpPacket(data);
         } else {
             receiveProcessRtcpPacket(data);
         }
     }

     public ByteBuf sendProcess(ChannelContext channelContext, RtpPacket rtpPacket) {
         ByteBuf decryptByteBuf = channelContext.allocateByteBuf();
         rtpProcessor.sendProcess(rtpPacket, decryptByteBuf);
         //这里要对rtcp进行加密
         long ssrc = rtpPacket.getSsrc();
         SRtpContext srtpContext = srtpContextFactory.getServerSrtpContext(ssrc);

         ByteBuf encryptBytebuf = channelContext.allocateByteBuf(decryptByteBuf.readableBytes() + srtpContext.getAuthTagLength());

         try {
             srtpContext.encrypt(decryptByteBuf, encryptBytebuf);
         } catch (InvalidAlgorithmParameterException | ShortBufferException | IllegalBlockSizeException |
                  BadPaddingException | InvalidKeyException e) {
             log.error(e.getMessage(), e);
         }


         return encryptBytebuf;
     }

     public ByteBuf sendProcess(ChannelContext channelContext, RtcpGroupPacket rtcpGroupPacket) {
         RtcpPacket[] rtcpPackets = rtcpGroupPacket.rtcpPackets();
         int totalLength = 0;
         for (RtcpPacket rtcpPacket : rtcpPackets) {
             totalLength += rtcpPacket.getTotalLength();
         }

         ByteBuf decryptByteBuf = channelContext.allocateByteBuf(totalLength);
         rtcpProcessor.sendProcess(rtcpPackets, decryptByteBuf);

         SRtcpContext sRtcpContext = srtpContextFactory.getServerSRtcpContext(SRtcpUtils.getSsrc(decryptByteBuf));


         ByteBuf encryptByteBuf = channelContext.allocateByteBuf(decryptByteBuf.readableBytes() + SRtcpContext.S_RTCP_INDEX_LENGTH + sRtcpContext.getAuthTagLength());
         try {
             sRtcpContext.encrypt(decryptByteBuf, encryptByteBuf);
         } catch (InvalidAlgorithmParameterException | ShortBufferException | IllegalBlockSizeException |
                  BadPaddingException | InvalidKeyException e) {
             log.error(e.getMessage(), e);
         }


         return encryptByteBuf;
     }


     /**
      * 处理 SRTP（Secure Real-time Transport Protocol）协议的 RTP 数据包。
      *
      * @param data 表示 RTP 数据包的 {@code ByteBuf} 对象，包含加密的音视频数据。
      *             方法会从该数据包中提取 SSRC（Synchronization Source Identifier），通过 SSRC 获取解密上下文，
      *             然后对数据包进行身份验证与解密，并将解密后的数据传递给对应的 RTP 数据处理器。
      */
     private void receiveProcessRtpPacket(@NonNull InboundPacket data) {
         ByteBuf packet = data.packet();
         long ssrc = SrtpUtils.getRtpSsrc(packet);
         SRtpContext srtpContext = srtpContextFactory.getClientSrtpContext(ssrc);
         if (srtpContext.contrastAuthTag(packet)) {
             //认证成功
             try {
                 ByteBuf decrypt = srtpContext.decrypt(packet);
                 rtpProcessor.receiveProcess(data, decrypt);
             } catch (InvalidAlgorithmParameterException | ShortBufferException | IllegalBlockSizeException |
                      BadPaddingException | InvalidKeyException e) {
                 log.error(e.getMessage(), e);
             }


         }


     }

     /**
      * 处理 RTCP（Real-time Transport Control Protocol）协议的数据包。
      *
      * @param data 表示 RTCP 数据包的 {@code ByteBuf} 对象，包含加密的控制信息。
      *             方法会从该数据包中提取 SSRC（Synchronization Source Identifier），
      *             通过 SSRC 获取解密上下文，对数据包进行身份验证与解密，
      *             并将解密后的数据传递给对应的 RTCP 数据处理器进行处理。
      */
     private void receiveProcessRtcpPacket(InboundPacket data) {
         ByteBuf packet = data.packet();
         long ssrc = SRtcpUtils.getSsrc(packet);
         SRtcpContext sRtcpContext = srtpContextFactory.getClientSrtcpContext(ssrc);
         if (sRtcpContext.contrastAuthTag(packet)) {
             try {
                 ByteBuf decrypt = sRtcpContext.decrypt(packet);
                 rtcpProcessor.process(decrypt);
             } catch (InvalidAlgorithmParameterException | ShortBufferException | IllegalBlockSizeException |
                      BadPaddingException | InvalidKeyException e) {
                 log.error(e.getMessage(), e);
             }

         }

     }


 }
