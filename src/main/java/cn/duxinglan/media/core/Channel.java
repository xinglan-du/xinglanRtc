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
 package cn.duxinglan.media.core;


 import cn.duxinglan.media.transport.udp.ChannelListener;
 import io.netty.buffer.ByteBuf;
 import lombok.Getter;
 import lombok.Setter;
 import lombok.extern.slf4j.Slf4j;

 import java.io.IOException;
 import java.net.InetSocketAddress;

 /**
  * 抽象类 Channel 表示一个通用的通道，用于处理键值与通道上下文之间的映射关系。
  * 它主要用于分发和处理入站数据包（InboundPacket）。
  * <p>
  * 通过该类可以实现特定协议的通信功能，负责管理通道上下文对象和数据的处理流程。
  * 子类需实现通道上下文的创建逻辑。
  *
  * @param <K> 通道上下文的标识类型。
  */
 @Slf4j
 public abstract class Channel {


     @Setter
     @Getter
     private ChannelListener channelListener;


     public abstract ByteBuf allocateByteBuf(int length);

     public abstract ByteBuf allocateByteBuf();

     public abstract void send(InetSocketAddress socketAddress, ByteBuf byteBuf) throws IOException;


     public void submit(InetSocketAddress socketAddress, ByteBuf packet) {
         channelListener.receive(socketAddress, packet);
     }

 }
