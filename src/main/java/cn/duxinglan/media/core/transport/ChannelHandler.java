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
 package cn.duxinglan.media.core.transport;


 import cn.duxinglan.media.core.Channel;
 import cn.duxinglan.media.core.ChannelContext;
 import cn.duxinglan.media.transport.udp.BasePacket;
 import cn.duxinglan.media.transport.udp.ChannelListener;
 import cn.duxinglan.media.transport.udp.InboundPacket;
 import cn.duxinglan.media.transport.udp.OutboundPacket;
 import io.netty.buffer.ByteBuf;
 import lombok.extern.slf4j.Slf4j;

 import java.io.IOException;
 import java.net.InetSocketAddress;
 import java.util.Map;
 import java.util.Queue;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.ConcurrentLinkedQueue;
 import java.util.concurrent.Executor;
 import java.util.concurrent.atomic.AtomicBoolean;

 @Slf4j
 public abstract class ChannelHandler implements ChannelListener {


     private final Channel channel;

     private final Queue<InboundPacket> inQueue = new ConcurrentLinkedQueue<>();

     private final AtomicBoolean inRunning = new AtomicBoolean(false);

     private final Queue<OutboundPacket> outQueue = new ConcurrentLinkedQueue<>();

     private final AtomicBoolean outRunning = new AtomicBoolean(false);

     private final Map<InetSocketAddress, ChannelContext> channelContextMap = new ConcurrentHashMap<>();

     private final Executor executor;

     private final TransportListener transportListener;

     protected ChannelHandler(Channel channel, Executor executor, TransportListener transportListener) {
         this.channel = channel;
         this.channel.setChannelListener(this);
         this.executor = executor;
         this.transportListener = transportListener;

     }


     private ChannelContext createChannelContext(InetSocketAddress socketAddress) {
         return new DefaultChannelContext(socketAddress);
     }


     private void tryInSchedule() {
         if (!inRunning.compareAndSet(false, true)) {
             return;
         }
         executor.execute(this::inRun);
     }

     private void tryOutSchedule() {
         if (!outRunning.compareAndSet(false, true)) {
             return;
         }
         executor.execute(this::outRun);

     }

     private void outRun() {
         try {
             while (true) {
                 OutboundPacket data = outQueue.poll();
                 if (data == null) {
                     break;
                 }
                 ByteBuf write = null;
                 try {
                     write = transportListener.write(data);
                     if (write == null) {
                         continue;
                     }
                     channel.send(data.remoteAddress(), write);
                 } catch (IOException e) {
                     log.error(e.getMessage(), e);
                 } finally {
                     if (write != null) {
                         write.release();
                     }
                 }
             }
         } finally {
             outRunning.set(false);

         }
         if (!outQueue.isEmpty()) {
             tryOutSchedule();
         }
     }


     private void inRun() {
         try {
             while (true) {
                 InboundPacket data = inQueue.poll();
                 if (data == null) {
                     break;
                 }
                 try {
                     transportListener.read(data);
                 } catch (Exception e) {
                     log.error(e.getMessage(), e);
                 } finally {
                     data.release();
                 }
             }
         } finally {
             inRunning.set(false);
         }
         if (!inQueue.isEmpty()) {
             tryInSchedule();
         }
     }

     @Override
     public void receive(InetSocketAddress socketAddress, ByteBuf packet) {
         long arrivalTimeNs = System.nanoTime();
         ChannelContext channelContext = channelContextMap.computeIfAbsent(socketAddress, this::createChannelContext);
         inQueue.offer(new InboundPacket(channelContext, arrivalTimeNs, packet));
         tryInSchedule();
     }

     public class DefaultChannelContext implements ChannelContext {

         private final InetSocketAddress remoteAddress;

         public DefaultChannelContext(InetSocketAddress remoteAddress) {
             this.remoteAddress = remoteAddress;
         }

         @Override
         public InetSocketAddress getRemoteAddress() {
             return remoteAddress;
         }

         @Override
         public void send(BasePacket packet) {
             OutboundPacket outboundPacket = new OutboundPacket(remoteAddress, packet, this);
             outQueue.offer(outboundPacket);
             tryOutSchedule();
         }


         @Override
         public ByteBuf allocateByteBuf(int length) {
             return channel.allocateByteBuf(length);
         }

         @Override
         public ByteBuf allocateByteBuf() {
             return channel.allocateByteBuf();
         }
     }

     public interface TransportListener {

         void read(InboundPacket inboundPacket);


          ByteBuf write(OutboundPacket outboundPacket);
     }
 }
