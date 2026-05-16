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

 import com.fasterxml.jackson.annotation.JsonCreator;
 import org.jspecify.annotations.Nullable;

 /**
  * SignalType 枚举类表示信令操作的类型。
  * 每种类型对应一个字符串值，用于区分不同的信令行为。
  * 可用于信令消息的解析和处理。
  * <p>
  * 枚举成员包括：
  * - JOIN: 表示加入信令。
  * - LEAVE: 表示离开信令。
  * - PUBLISH: 表示发布流信令。
  * - UNPUBLISH: 表示取消发布流信令。
  * - SUBSCRIBE: 表示订阅流信令。
  * - UNSUBSCRIBE: 表示取消订阅流信令。
  * <p>
  * 提供了一个静态方法 fromValue，用于根据字符串值解析对应的 SignalType。
  */
 public enum SignalType {

     JOIN("join"),
     LEAVE("leave"),
     PUBLISH("publish"),
     UNPUBLISH("unpublish"),
     SUBSCRIBE("subscribe"),
     UNSUBSCRIBE("unsubscribe"),

     ;
     public final String value;

     SignalType(String value) {
         this.value = value;
     }


     @JsonCreator
     public static @Nullable SignalType fromValue(String value) {
         for (SignalType type : SignalType.values()) {
             if (type.value.equals(value)) {
                 return type;
             }
         }
         return null;
     }
 }
