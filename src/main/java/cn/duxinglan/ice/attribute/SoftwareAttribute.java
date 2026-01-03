package cn.duxinglan.ice.attribute;

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.nio.charset.StandardCharsets;


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

/**
 * 表示一种特定的 STUN 扩展类型，对应于 {@link AttributeType#SOFTWARE}。
 * 该类用于包含发送方的版本信息或软件标识，通常用作调试、统计或元数据的传递。
 * 在 STUN 协议中，该扩展类型可以被用来标识发送方的软件信息，例如版本号、应用程序标识符等。
 * <p>
 * 特性说明:
 * 1. 继承自抽象类 {@link Attribute}，并扩展了其功能。
 * 2. 包含一个软件属性字段，用于存储软件相关的信息。
 * <p>
 * 方法概述:
 * - 构造方法：初始化类型为 {@link AttributeType#SOFTWARE}。
 * - 静态工厂方法：通过给定软件字符串生成 SoftwareAttribute 实例。
 * - 数据写入及解码：实现了 {@link Attribute} 的抽象方法 write 和 decodeAttribute。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SoftwareAttribute extends Attribute {

    public String software;

    public SoftwareAttribute() {
        super(AttributeType.SOFTWARE);
    }

    public static SoftwareAttribute decode(String software) {
        SoftwareAttribute softwareAttribute = new SoftwareAttribute();
        softwareAttribute.setSoftware(software);
        softwareAttribute.setLeng(software.getBytes(StandardCharsets.UTF_8).length);
        return softwareAttribute;
    }

  /*  @Override
    protected void onValueChange(ByteBuf value) {

    }*/

    @Override
    void write(ByteBuf byteBuf) {
        byteBuf.writeBytes(software.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void decodeAttribute(ByteBuf data, int offset, int length) {
        software = data.toString(offset, length, StandardCharsets.UTF_8);
    }
}
