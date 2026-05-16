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

 public enum ProtocolType {

     WEBRTC("webrtc"),
     ;

     public final String value;

     ProtocolType(String value) {
         this.value = value;
     }

     @JsonCreator
     public static @Nullable ProtocolType fromValue(String value) {
         for (ProtocolType type : ProtocolType.values()) {
             if (type.value.equals(value)) {
                 return type;
             }
         }
         return null;
     }
 }
