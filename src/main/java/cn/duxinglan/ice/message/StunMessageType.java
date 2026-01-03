package cn.duxinglan.ice.message;

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
public enum StunMessageType {


    /**
     * 表示STUN协议中用于发送绑定请求的消息类型。
     * 此类型的值为0x0001。
     * 在STUN协议中，绑定请求用于客户端向服务器请求映射关系信息，
     * 以确定其在防火墙或NAT设备之后的公网IP地址和端口。
     */
    BINDING_REQUEST(0x0001),

    /**
     * 表示STUN协议中用于发送绑定请求成功响应的消息类型。
     * 此类型的值为0x0101。
     * 在STUN协议中，绑定成功响应用于服务器向客户端返回映射关系信息，
     * 包括客户端的公网IP地址和端口，以应答绑定请求消息。
     */
    BINDING_SUCCESS_RESPONSE(0x0101),

    /**
     * 表示STUN协议中用于发送绑定请求错误响应的消息类型。
     * 此类型的值为0x0111。
     * 在STUN协议中，绑定错误响应用于服务器向客户端返回绑定请求失败的信息，
     * 通常包含错误代码和错误描述，帮助客户端了解失败的原因。
     */
    BINDING_ERROR_RESPONSE(0x0111),




    NONE(0x0000);

    public final int value;

    StunMessageType(int value) {
        this.value = value;
    }

    public static StunMessageType fromValue(int value) {
        for (StunMessageType type : StunMessageType.values()) {
            if (type.value == value) {
                return type;
            }
        }
        return null;
    }
}
