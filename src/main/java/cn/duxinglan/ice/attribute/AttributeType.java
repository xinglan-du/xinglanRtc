package cn.duxinglan.ice.attribute;

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
public enum AttributeType {

    /**
     * ICE 用来认证的用户名
     * "user1:pass1"
     */
    USERNAME(0x0006),

    /**
     * GOOG_NETWORK_INFO 是一个扩展类型的枚举常量，其值为 0xc057。
     * <p>
     * 该枚举常量在 STUN 或相关网络协议中可能被用来传递与 Google 网络信息相关的属性数据。
     * 具体用途通常依赖于协议实现以及应用场景，例如在特定的网络系统中交换
     * 与 Google 服务或通信优化相关的元数据信息。
     * <p>
     * 此类型的使用需结合协议规范或上下文环境进行解释。
     */
    GOOG_NETWORK_INFO(0xc057),
    /**
     * 控制端标识
     */
    ICE_CONTROLLING(0x802a),
    /**
     * 优先级
     */
    PRIORITY(0x0024),

    /**
     * MESSAGE_INTEGRITY 是一种 STUN 扩展类型，用于验证消息的完整性。
     * 它通过 HMAC-SHA1 签名机制生成校验值，确保消息在传输过程中未被篡改。
     * 发送方使用共享密钥计算签名，并将计算结果作为此属性的值。
     * 接收方通过相同的共享密钥重新计算并匹配校验值，从而判断消息是否完整且可信。
     * <p>
     * 值：0x0008
     */
    MESSAGE_INTEGRITY(0x0008),

    /**
     * FINGERPRINT 是一个扩展类型，用于在 STUN 消息中添加 CRC32 校验信息。
     * 此校验保证消息在传输过程中未被篡改。具体的实现使用 CRC32 校验算法，
     * 并在校验值末尾执行一个 XOR 操作以满足 STUN 对传输协议的特定要求。
     * <p>
     * 值：0x8028
     */
    FINGERPRINT(0x8028),

    /**
     * XOR_MAPPED_ADDRESS 是一个扩展类型，主要用于表示通过 XOR 操作后的映射地址信息。
     * 该地址通常用于 NAT 穿透，以通过服务器获取客户端的外部 IP 和端口信息。
     * 在 STUN 协议中，它被用作帮助客户端发现其外部地址的一部分。
     */
    XOR_MAPPED_ADDRESS(0x0020),

    /**
     * SOFTWARE 是一个 STUN 扩展类型，主要用于包含发送方的版本信息或软件标识。
     * 该类型通常用于传递机构或应用的特定信息，如用于调试、统计或其他元数据用途。
     * 此值的具体格式根据实现有所不同，通常为字符串表示的版本号或标识符。
     * <p>
     * 值：0x8022
     */
    SOFTWARE(0x8022),

    /**
     * 表示STUN协议中用于传递USE-CANDIDATE属性的消息类型。
     * 此类型的值为0x0025。
     * USE-CANDIDATE属性用于交互式连接建立协议（ICE）。它指示一个候选地址
     * (candidate)被选中，并且需要用于连接的传输路径建立。
     */
    USE_CANDIDATE(0x0025),

    NONE(0x0000);

    public final int value;

    AttributeType(int value) {
        this.value = value;
    }

    public static AttributeType fromValue(int value) {
        for (AttributeType type : AttributeType.values()) {
            if (type.value == value) {
                return type;
            }
        }
        return NONE;
    }

}
