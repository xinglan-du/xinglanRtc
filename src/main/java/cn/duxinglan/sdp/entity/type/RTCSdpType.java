package cn.duxinglan.sdp.entity.type;

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
public enum RTCSdpType {
    /**
     * 表示 SDP 会话描述协议中用于发起会话的类型。
     * 该枚举常量定义为 "offer"。
     */
    OFFER("offer"),
    /**
     * 表示 SDP 会话描述协议中用于响应会话提议的类型。
     * 该枚举常量定义为 "answer"。
     */
    ANSWER("answer"),
    /**
     * 表示 SDP 会话描述协议中的一种会话响应的中间状态类型。
     * 在协商会话时，"pranswer" 用于指示接收方暂时接收提议，
     * 但尚未确认最终答复。
     * 该枚举常量定义为 "pranswer"。
     */
    PRANSWER("pranswer"),
    /**
     * 表示 SDP 会话描述协议中的撤回会话提议的类型。
     * 该枚举常量定义为 "rollback"。
     */
    ROLLBACK("rollback");

    @JsonValue
    public final String value;

    RTCSdpType(String value) {
        this.value = value;
    }

    public static RTCSdpType fromValue(String value) {
        for (RTCSdpType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}
