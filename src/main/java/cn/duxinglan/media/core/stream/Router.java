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

 /**
  * 路由器接口，用于实现媒体流的发布与订阅功能。
  * <p>
  * 通过该接口，可以将媒体流发布到路由器，并允许其他对象订阅这些媒体流。
  * 路由器负责管理媒体流与订阅者之间的关系，确保媒体流数据能够分发到所有订阅者。
  */
 public interface Router {

     /**
      * 将媒体流发布到路由器。
      * <p>
      * 该方法将一个媒体流对象发布到路由器，路由器会管理该媒体流及其订阅者之间的关系。
      *
      * @param stream 需要发布的媒体流对象。路由器将接收并管理该媒体流，允许其他对象订阅。
      */
     void publish(String sessionId,MediaStream stream);

     /**
      * 取消发布指定的媒体流。
      * <p>
      * 该方法用于从路由器中移除一个已发布的媒体流。
      * 调用此方法后，路由器将不再管理该媒体流，并停止将其分发给订阅者。
      *
      * @param stream 指定要取消发布的媒体流对象。
      *               该对象应是之前通过 {@code publish} 方法发布到路由器的媒体流。
      */
     void unpublish(String sessionId,MediaStream stream);


     void subscribe(String sessionId,MediaSinkFactory sinkFactory);


     void unSubscribe(String sessionId,MediaSink mediaSink);
 }
