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
 import cn.duxinglan.media.impl.dtls.DtlsContext;
 import cn.duxinglan.media.transport.nio.webrtc.DtlsOutputPacket;
 import cn.duxinglan.media.transport.udp.InboundPacket;
 import io.netty.buffer.ByteBuf;
 import lombok.extern.slf4j.Slf4j;
 import org.jspecify.annotations.NonNull;

 import java.io.IOException;

 @Slf4j
 public class DtlsProcessor implements DtlsContext.DtlsEvent {

     private ChannelContext channelContext;

     private volatile DtlsContext dtlsContext;


     public DtlsProcessor() {
     }


     public void receiveProcess(InboundPacket data) {

         if (dtlsContext == null) {
             return;
         }
         ByteBuf buf = data.packet();
         int readable = buf.readableBytes();
         byte[] bytes = new byte[readable];
         buf.getBytes(buf.readerIndex(), bytes);
         //DTLS直接使用bc库
         try {
             dtlsContext.processData(bytes);
         } catch (IOException e) {
             log.error(e.getMessage(), e);
         }
     }

     public void setTransportContext(@NonNull TransportContext transportContext, @NonNull ChannelContext channelContext, DtlsContext.DtlsShakeHandsCallback dtlsShakeHandsCallback) {
         this.channelContext = channelContext;
         this.dtlsContext = new DtlsContext(this, transportContext.getDtlsKeyMaterial(), dtlsShakeHandsCallback);
     }

     @Override
     public void onMessage(DtlsOutputPacket dtlsOutputPacket) {
         channelContext.send(dtlsOutputPacket);
     }


     public ByteBuf sendProcess(ChannelContext channelContext, DtlsOutputPacket dtlsOutputPacket) {
         int totalLength = dtlsOutputPacket.getTotalLength();
         ByteBuf byteBuf = channelContext.allocateByteBuf(totalLength);
         dtlsOutputPacket.writeTo(byteBuf);
         return byteBuf;
     }
 }
