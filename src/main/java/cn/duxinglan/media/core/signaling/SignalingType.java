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
 import com.fasterxml.jackson.annotation.JsonValue;

 /**
  * SignalingType 枚举类表示信令的类型，用于区分不同的信令用途。
  * 该类型主要用于信令系统中的消息分类和处理逻辑。
  * <p>
  * 枚举值包括以下几种：
  * - INIT_MESSAGE：表示初始化信令，用于初始状态的消息，如建立连接时的信令。
  * - ENDPOINT_MESSAGE：表示端点通信信令，包含端点标识及通信数据。
  * <p>
  * 提供特定方法：
  * - {@code formValue(int value)}：根据整数值返回相应的信令类型实例，便于解析和反序列化操作。
  * - {@code value}：通过字段表示信令类型的整数值，具备唯一性。
  * <p>
  * 常见使用场景：
  * - 在信令服务器的消息处理中，通过信令类型判断对应的业务逻辑。
  * - 与 JSON 序列化和反序列化框架结合使用，保证数据的标准化传递。
  */
 public enum SignalingType {

     /**
      * 初始化信令类型，表示初始状态。
      * 其对应的整数值为0。
      */
     INIT_MESSAGE(0),

     /**
      * 表示端点通信信令类型。
      * 该信令类型用于传递与特定端点相关的消息，
      * 包括目标端点的标识及数据内容。
      * <p>
      * 整数值为：1
      */
     ENDPOINT_MESSAGE(1),


     ;

     /**
      * 表示信令类型的具体整数值。
      * 该值用作信令的唯一标识符，便于在序列化和反序列化时保持一致。
      * 使用@JsonValue注解，确保枚举类型在JSON序列化时以此值表示。
      */
     @JsonValue
     public final int value;

     SignalingType(int value) {
         this.value = value;
     }


     /**
      * 根据给定的整数值返回对应的信令类型。
      * 如果未找到匹配的信令类型，则返回null。
      *
      * @param value 整数值，用于标识具体的信令类型。
      * @return 对应的SignalingType枚举实例；如果未找到匹配的枚举实例，则返回null。
      */
     @JsonCreator
     public static SignalingType formValue(int value) {
         for (SignalingType signalingType : values()) {
             if (signalingType.value == value) {
                 return signalingType;
             }
         }
         return null;
     }
 }
