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


 import cn.duxinglan.media.transport.udp.BasePacket;
 import io.netty.buffer.ByteBuf;

 import java.net.InetSocketAddress;

 public interface ChannelContext {


     InetSocketAddress getRemoteAddress();

     void send(BasePacket packet);


     /**
      * 分配指定长度的 ByteBuf 实例。
      * 该方法用于分配用于网络通信的缓冲区，以便进行数据的发送或接收操作。
      *
      * @param length 要分配的缓冲区长度，单位为字节。必须为非负整数。
      * @return 分配的 ByteBuf 实例，可用于存储或传输数据。
      */
     ByteBuf allocateByteBuf(int length);

     ByteBuf allocateByteBuf();
 }
