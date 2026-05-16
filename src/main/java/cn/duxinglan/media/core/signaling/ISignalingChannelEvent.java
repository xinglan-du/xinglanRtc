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
 package cn.duxinglan.media.core.signaling;


 import cn.duxinglan.media.signaling.data.SignalingData;
 import io.netty.channel.ChannelHandlerContext;

 /**
  * ISignalingChannelEvent 接口用于定义信令通道事件的处理。
  * 通过此接口，可以监听信令通道的行为，如接收动作、处理信令数据以及关闭事件。
  *
  * @param <T> 表示通道上下文的类型。
  */
 public interface ISignalingChannelEvent<T> {

     /**
      * 处理信令通道触发的动作。
      *
      * @param context 表示通道的上下文对象，具体类型由实现者定义。
      */
     void onAction(T context);

     /**
      * 处理信令数据的核心方法。
      * 用于解析、处理和响应特定类型的信令信息。
      *
      * @param signalingData 表示信令数据的对象，包含信令类型和相关信息。
      */
     void handleSignalingData(T context,SignalMessage signalMessage);

     void handleSignalingData(T context, SignalingData signalingData);
     /**
      * 处理信令通道关闭事件的方法。
      * 当信令通道被关闭时触发此方法，用于执行相关的清理或回收操作。
      * 可供实现类定义具体的关闭逻辑，例如资源释放、通知更新等。
      */
     void onClosed(ChannelHandlerContext context);
 }
