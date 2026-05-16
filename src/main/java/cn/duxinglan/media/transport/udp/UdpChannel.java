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
 package cn.duxinglan.media.transport.udp;

 import cn.duxinglan.media.core.Channel;
 import io.netty.buffer.ByteBuf;
 import io.netty.buffer.Unpooled;
 import lombok.extern.slf4j.Slf4j;

 import java.io.IOException;
 import java.net.InetSocketAddress;
 import java.nio.ByteBuffer;
 import java.nio.channels.DatagramChannel;
 import java.nio.channels.SelectionKey;
 import java.nio.channels.Selector;
 import java.util.Iterator;

 //这里模型需要修改一下 不要用自己的线程 最好使用一个内部的线程池，这里暂时不改啦
 // 这里的收结构需要改一下  在收到数据后需要同步到一个队列中 队列需要在一个线程里面做接受，不要租塞接受线程
 @Slf4j
 public class UdpChannel extends Channel implements Runnable {

     private Selector selector;

     private DatagramChannel channel;

     private volatile boolean running;

     private final int port;

     private Thread thread;


     public UdpChannel(int port) {
         this.port = port;
     }

     public void start() throws IOException {
         running = true;
         thread = new Thread(this);
         channel = DatagramChannel.open();
         channel.configureBlocking(false);
         channel.bind(new InetSocketAddress(port));
         selector = Selector.open();
         channel.register(selector, SelectionKey.OP_READ);
         thread.start();
     }

     public void stop() {
         running = false;

         if (selector != null) {
             selector.wakeup();
         }

         try {
             if (thread != null) thread.join(2000);
         } catch (InterruptedException ignored) {
         }

         try {
             if (channel != null) channel.close();
             if (selector != null) selector.close();
         } catch (IOException e) {
             log.error("关闭udp资源失败", e);
         }

         thread = null;
     }

     @Override
     public void run() {
         log.info("启动udp接收，监听端口为：{}", port);
         try {
             while (running) {
                 selector.select();
                 Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                 while (it.hasNext()) {
                     SelectionKey key = it.next();
                     it.remove();
                     if (key.isReadable()) {
                         handleRead((DatagramChannel) key.channel());
                     }
                 }
             }
         } catch (IOException e) {
             running = false;
             log.error("udp服务启动失败", e);
             return;
         }
         log.info("udp接收正常关闭:{}", port);
     }

     private void handleRead(DatagramChannel channel) throws IOException {

         ByteBuf buf = Unpooled.directBuffer(1500);
         ByteBuffer nioBuffer = buf.nioBuffer(0, buf.capacity());

         InetSocketAddress remote = (InetSocketAddress) channel.receive(nioBuffer);
         if (remote == null) {
             buf.release();
             return;
         }

         int len = nioBuffer.position();
         buf.writerIndex(len);

         submit(remote, buf);
     }


     @Override
     public ByteBuf allocateByteBuf(int length) {
         return Unpooled.buffer(length);
     }

     @Override
     public ByteBuf allocateByteBuf() {
         return Unpooled.buffer();
     }

     @Override
     public void send(InetSocketAddress remoteAddress, ByteBuf byteBuf) throws IOException {
         this.channel.send(byteBuf.nioBuffer(), remoteAddress);
     }


 }
