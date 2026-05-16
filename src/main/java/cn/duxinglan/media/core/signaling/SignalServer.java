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


 import cn.duxinglan.media.core.endpoint.Endpoint;
 import cn.duxinglan.media.core.endpoint.EndpointManager;
 import cn.duxinglan.media.core.signaling.init.InitRequest;
 import cn.duxinglan.media.core.signaling.init.InitResponse;
 import cn.duxinglan.media.module.CacheModel;
 import com.fasterxml.jackson.databind.ObjectMapper;
 import lombok.extern.slf4j.Slf4j;
 import org.jspecify.annotations.NonNull;

 import java.util.Map;
 import java.util.Optional;
 import java.util.concurrent.ConcurrentHashMap;

 /**
  * SignalServer 类是信令管理的核心组件，用于处理信令消息和管理与端点之间的通信。
  * <p>
  * 该类主要实现了以下功能：
  * 1. 处理传入的信令消息（如初始化信令和端点通信信令）。
  * 2. 管理端点的创建与绑定操作。
  * 3. 转发特定类型的信令到目标端点，以实现灵活的通信机制。
  * <p>
  * 核心组件说明：
  * - {@link EndpointManager}：负责创建、获取和移除端点的管理器。
  * - {@link ObjectMapper}：用于将信令消息的 JSON 数据解析为具体的对象类型。
  * <p>
  * 方法：
  * - {@code onMessage(SignalingConnection signalingConnection, SignalMessage msg)}：
  * 每当接收到新的信令消息时，会根据其类型执行相应处理逻辑。
  * - {@code sendInit(SignalingConnection conn, String id)}：
  * 向指定的连接发送初始化信令消息，该消息通常包含端点 ID 信息。
  * <p>
  * 核心流程：
  * - 当接收到初始化信令时，创建对应的端点，并绑定到信令连接。
  * - 对于端点相关的信令，找到对应端点，并将信令分发至其处理逻辑。
  * <p>
  * 异常处理：
  * - 如果在信令处理中无法找到对应的端点，会输出警告日志。
  * - 不支持的信令类型将被静默忽略。
  * <p>
  * 适用场景：
  * - 实时通信系统，用于管理多个信令通道和端点交互。
  * - 信令服务器的核心控制模块，支持动态扩展的端点类型。
  */
 @Slf4j
 public class SignalServer {

     private final ObjectMapper objectMapper = CacheModel.getObjectMapper();


     /**
      * {@code endpointManager} 是一个用于管理信令端点的核心组件。
      * 它负责信令端点的创建、查找和移除操作。
      * <p>
      * 功能包括但不限于：
      * - 根据协议类型创建新的信令端点实例。
      * - 根据唯一标识符查询已存在的信令端点。
      * - 移除指定标识符对应的信令端点，并执行资源清理。
      * <p>
      * 在信令服务器中，该组件是端点生命周期管理的关键模块。
      * 通过 {@link EndpointManager} 保持信令端点与信令连接的有效交互。
      * <p>
      * 线程安全：
      * - 内部使用线程安全的方式存储和操作端点信息，适用于并发的信令消息处理场景。
      */
     private final EndpointManager endpointManager;

     /**
      * 维护信令连接与唯一标识符之间的映射关系。
      * 该映射主要用于在信令服务器中跟踪每个信令连接，并为其分配一个
      * 唯一的标识符，以便进行状态管理以及消息的发送与接收。
      *
      * <ul>
      * 用途说明：
      * - 键：表示信令连接的实例 {@link SignalingConnection}，用于标识具体的信令会话。
      * - 值：字符串类型，表示与特定信令连接相关联的唯一标识符。
      * </ul>
      * <p>
      * 线程安全性：
      * - 使用 {@link ConcurrentHashMap} 作为内部存储结构，确保在多线程
      *   环境下的并发安全。
      * <p>
      * 注意事项：
      * - 应在信令连接建立时将其与唯一标识符关联，并在连接关闭时及时移除
      *   该映射，以避免资源泄漏。
      */
     private final Map<SignalingConnection, String> connectionId = new ConcurrentHashMap<>();

     /**
      * SignalServer 类的单例实例，确保整个应用程序生命周期中只有一个 SignalServer 实例的存在。
      * <p>
      * INSTANCE 变量是通过静态初始化的方式创建的，这种方式不仅保证了线程安全，
      * 而且确保了实例的全局唯一性。
      * <p>
      * 使用 SignalServer 的 {@code getInstance()} 方法可以直接获取该实例，
      * 从而避免多次创建造成资源浪费或状态不一致等问题。
      */
     private static final SignalServer INSTANCE = new SignalServer();

     /**
      * 获取SignalServer的单例实例。
      * 该方法确保SignalServer在整个应用程序中唯一，不会产生多个实例。
      *
      * @return SignalServer的单例实例
      */
     public static SignalServer getInstance() {
         return INSTANCE;
     }

     /**
      * SignalServer 类的私有构造方法。
      * 该构造方法用于初始化 SignalServer 实例，并设置其所需的依赖组件。
      * 在初始化过程中会完成以下操作：
      * <p>
      * - 创建并初始化 EndpointManager 对象，用于管理信令端点相关的操作。
      * <p>
      * 设计为私有是为了支持单例模式，确保整个应用程序中 SignalServer 只有一个实例。
      * 请通过静态方法 {@code getInstance()} 获取 SignalServer 的单例。
      */
     private SignalServer() {
         this.endpointManager = new EndpointManager();
     }

     /**
      * 处理接收到的信令消息，根据信令类型执行相应的处理逻辑。
      *
      * @param signalingConnection 信令连接实例，用于发送或接收信令消息。
      * @param msg                 接收到的信令消息，包含消息类型和数据。
      */
     public void onMessage(@NonNull SignalingConnection signalingConnection, @NonNull SignalMessage msg) {
         if (msg.type() == SignalingType.INIT_MESSAGE) {
             InitRequest initRequest = objectMapper.convertValue(msg.data(), InitRequest.class);
             //这里是创建逻辑
             Optional<Endpoint> endpointOptional = this.endpointManager.create(initRequest.protocol());
             endpointOptional.ifPresent(endpoint -> {
                 sendInit(signalingConnection, endpoint.getId());
                 connectionId.put(signalingConnection, endpoint.getId());
                 endpoint.bindConnection(signalingConnection);
             });
         } else if (msg.type() == SignalingType.ENDPOINT_MESSAGE) {
             EndpointMessage endpointMessage = objectMapper.convertValue(msg.data(), EndpointMessage.class);
             //这里是转发逻辑
             Endpoint endpoint = endpointManager.get(endpointMessage.id());
             if (endpoint == null) {
                 log.warn("未创建对应的Endpoint:{}", endpointMessage.id());
                 return;
             }
             endpoint.handleSignal(endpointMessage.data());

         }


     }

     /**
      * 向指定的信令连接发送初始化信令消息。
      * 该消息封装了初始化响应数据，通常包含唯一标识符信息。
      *
      * @param conn 信令连接实例，通过该连接发送信令消息。
      * @param id   用于标识初始化响应数据的唯一标识符。
      */
     private void sendInit(@NonNull SignalingConnection conn, String id) {
         conn.sendSignal(new SignalMessage(
                 SignalingType.INIT_MESSAGE,
                 new InitResponse(id)
         ));
     }

     /**
      * 处理信令连接关闭事件。
      * 当某个信令连接关闭时，该方法会执行以下操作：
      * 1. 从当前连接集合中移除该连接对应的标识符。
      * 2. 从端点管理器中移除与该标识符关联的端点，保证资源正确释放。
      *
      * @param signalingConnection 信令连接实例，表示即将关闭的连接。
      */
     public void onClose(SignalingConnection signalingConnection) {
         String id = connectionId.remove(signalingConnection);
         if (id == null) return;
         this.endpointManager.remove(id);
     }
 }
