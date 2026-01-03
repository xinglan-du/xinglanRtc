package cn.duxinglan.media.impl.webrtc;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 *
 * 版权所有 (c) 2025 www.duxinglan.cn
 * <p>
 * 项目名称：xinglanRtc
 * <p>
 * 本文件属于 xinglanRtc 项目的一部分。
 * <p>
 * 本软件依据 XinglanRtc 非商业许可证（XNCL）授权，仅限个人非商业使用。
 * 禁止任何形式的商业用途，包括但不限于：收费安装、收费部署、
 * 收费运维、收费技术支持等行为。
 * <p>
 * 详情请参阅项目根目录下的 LICENSE 文件。
 **/
public enum WebrtcNodeDataType {

    /**
     * 节点提议信令类型，表示节点向其他节点发送提议的信令状态。
     * 其对应的整数值为0。
     */
    NODE_OFFER(0),

    /**
     * 节点回复信令类型，表示节点对提议信令进行回复的状态。
     * 其对应的整数值为1。
     */
    NODE_ANSWER(1),

    ;

    /**
     * 表示信令类型的具体整数值。
     * 该值用作信令的唯一标识符，便于在序列化和反序列化时保持一致。
     * 使用@JsonValue注解，确保枚举类型在JSON序列化时以此值表示。
     */
    @JsonValue
    public final int value;

    WebrtcNodeDataType(int value) {
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
    public static WebrtcNodeDataType formValue(int value) {
        for (WebrtcNodeDataType webrtcNodeDataType : values()) {
            if (webrtcNodeDataType.value == value) {
                return webrtcNodeDataType;
            }
        }
        return null;
    }
}
