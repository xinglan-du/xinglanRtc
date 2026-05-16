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
 package cn.duxinglan.media.core.stream;


 import org.jspecify.annotations.Nullable;

 public interface MediaSinkFactory {

     //TODO 这里可以限制接受者的编解码 如果不支持可以返回null

     /**
      * 创建一个新的 {@code MediaSink} 实例。
      * <p>
      * 该方法用于生成一个 {@code MediaSink} 对象，提供用于接受和处理
      * RTP 数据包的功能。具体的实现可以根据需求自定义。
      *
      * @return 返回新的 {@code MediaSink} 实例，如果无法创建返回 {@code null}。
      */
     @Nullable MediaSink createMediaSink(MediaStream mediaStream);
 }
