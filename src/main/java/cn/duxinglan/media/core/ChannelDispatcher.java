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


 import java.util.Map;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.function.Function;

 public class ChannelDispatcher<T> {

     private final Function<ChannelContext, ChannelExecutor<T>> factory;

     private final Map<ChannelContext, ChannelExecutor<T>> executors = new ConcurrentHashMap<>();

     public ChannelDispatcher(Function<ChannelContext, ChannelExecutor<T>> factory) {
         this.factory = factory;
     }

     public void dispatch(ChannelContext channelContext, T data) {
         ChannelExecutor<T> executor = executors.computeIfAbsent(channelContext, factory);
         executor.submit(data);
     }

 }
