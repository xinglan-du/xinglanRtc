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


 import cn.duxinglan.ice.message.StunMessage;
 import cn.duxinglan.media.core.ChannelContext;
 import cn.duxinglan.media.core.transport.IMediaProcessor;
 import cn.duxinglan.media.impl.dtls.DtlsContext;
 import cn.duxinglan.media.impl.dtls.SrtpProfilesType;
 import cn.duxinglan.media.protocol.rtcp.RtcpGroupPacket;
 import cn.duxinglan.media.protocol.rtp.RtpPacket;
 import cn.duxinglan.media.transport.nio.webrtc.DtlsOutputPacket;
 import cn.duxinglan.media.transport.udp.BasePacket;
 import cn.duxinglan.media.transport.udp.InboundPacket;
 import cn.duxinglan.media.transport.udp.OutboundPacket;
 import io.netty.buffer.ByteBuf;
 import lombok.extern.slf4j.Slf4j;
 import org.jspecify.annotations.NonNull;

 import javax.crypto.NoSuchPaddingException;
 import java.net.InetSocketAddress;
 import java.security.NoSuchAlgorithmException;
 import java.util.Optional;

 @Slf4j
 public class WebrtcTransportSession implements IMediaProcessor<InboundPacket, OutboundPacket>, TransportContextLookup, IceProcessor.IceProcessorEvent, DtlsContext.DtlsShakeHandsCallback {


     private final IceProcessor iceProcessor;

     private final DtlsProcessor dtlsProcessor;


     private final TransportContextLookup transportContextLookup;

     private final ChannelContext channelContext;

     private TransportContext transportContext;

     private SrtpProcessor srtpProcessor;

     public WebrtcTransportSession(ChannelContext channelContext, TransportContextLookup transportContextLookup) {
         this.channelContext = channelContext;
         this.transportContextLookup = transportContextLookup;
         this.iceProcessor = new IceProcessor(channelContext, this);
         this.dtlsProcessor = new DtlsProcessor();
     }

     @Override
     public void receiveProcess(@NonNull InboundPacket data) {
         ByteBuf buf = data.packet();
         InetSocketAddress remoteAddress = data.channelContext().getRemoteAddress();
         log.debug("接受到数据ip：{},端口号：{}", remoteAddress.getHostString(), remoteAddress.getPort());
         //先判断当前数据是谁需要处理
         byte b = buf.getByte(buf.readerIndex());
         if ((b & 0xC0) == 0x80) {
             srtpProcessor.receiveProcess(data);
             return;
         } else if (b >= 20 && b <= 63) {
             dtlsProcessor.receiveProcess(data);
             return;
         } else if ((b & 0xC0) == 0) {
             iceProcessor.receiveProcess(data);
             return;
         } else {
             log.warn("接收到到未知消息：{}", b);
             return;
         }


     }

     @Override
     public ByteBuf sendProcess(@NonNull OutboundPacket data) {
         BasePacket packet = data.packet();
         if (srtpProcessor != null) {
             if (packet instanceof RtpPacket rtpPacket) {
                 return srtpProcessor.sendProcess(data.channelContext(), rtpPacket);
             } else if (packet instanceof RtcpGroupPacket rtcpGroupPacket) {
                 return srtpProcessor.sendProcess(data.channelContext(), rtcpGroupPacket);
             }
         }

         if (packet instanceof DtlsOutputPacket dtlsOutputPacket) {
             return dtlsProcessor.sendProcess(data.channelContext(), dtlsOutputPacket);
         } else if (packet instanceof StunMessage stunMessage) {
             return iceProcessor.sendProcess(data.channelContext(), stunMessage);
         }

         return null;
     }

     @Override
     public Optional<TransportContext> findByUfrag(String ufrag) {
         return transportContextLookup.findByUfrag(ufrag);
     }

     @Override
     public void onTransportContext(TransportContext transportContext) {
         if (this.transportContext == transportContext) {
             return;
         }
         this.transportContext = transportContext;
         this.transportContext.getSession().addChannelContext(channelContext);
         dtlsProcessor.setTransportContext(transportContext, channelContext, this);
     }

     @Override
     public void notifyHandshakeComplete(SrtpProfilesType srtpProfilesType, byte[] keyingMaterial) {
         try {
             this.srtpProcessor = new SrtpProcessor(this.channelContext, srtpProfilesType, keyingMaterial, this.transportContext.getSession());
             log.debug("创建srtpContextFactory成功");
         } catch (NoSuchPaddingException | NoSuchAlgorithmException e) {
             log.error(e.getMessage(), e);
         }
     }

     @Override
     public void onConnected(DtlsContext.DtlsState state) {
         if (state == DtlsContext.DtlsState.CONNECTED) {
             log.info("媒体通道建立成功");

         }
     }


 }
