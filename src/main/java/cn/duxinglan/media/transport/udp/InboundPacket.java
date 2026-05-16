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


 import cn.duxinglan.media.core.ChannelContext;
 import io.netty.buffer.ByteBuf;

 /**
  * 表示一个入站的数据包，包含数据包到达的时间及其内容。
  * 这个类主要用于封装网络通信中接收到的数据包及其相关元信息。
  *
  * @param arrivalTimeNs 表示数据包到达的时间，单位为纳秒。
  *                      此字段用于标识数据包的到达时间戳，以便进行时序分析或延迟计算。
  *                      <p>
  *                      注意：该时间戳通常采用系统的高精度计时器（如 {@link System#nanoTime()}）
  *                      获取，不代表绝对时间，只适用于相对时间的比较。
  * @param packet        表示接收到的原始数据包内容。
  *                      该字段包含通过网络传输接收到的具体字节数据，以 {@link ByteBuf} 形式存储。
  *                      数据包可能是 RTP、RTCP 或其他协议的封装数据。
  *                      <p>
  *                      设计此字段的目的是提供一种高效的、可变长度的字节缓冲区，用于处理网络通信中的数据。
  *                      使用 {@link ByteBuf} 可以避免频繁的内存分配和复制操作。
  *                      <p>
  *                      注意：此字段为只读(final)，确保数据一致性和线程安全。
  */
 public record InboundPacket(ChannelContext channelContext, long arrivalTimeNs, ByteBuf packet) {


     public void release() {
         packet.release();
     }
 }
