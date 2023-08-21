const net = require("net");
const protobuf = require('protobufjs');
const config = require("./config.json")

let Message,ActionType, client
// 初始化
const dataPb = protobuf.loadSync('./data.proto');
Message = dataPb.lookupType('data.Message');
ActionType = dataPb.lookupEnum('data.ActionType');
// 发送消息
const sendMessage = (type) => {
  const msg = {
    dataType:Message.DataType.CertificationMsgType,
    certificationMsg:{
      actionType: type,
      auth: config.auth
    }
  }
  type === ActionType.values.Heartbeat && delete msg.certificationMsg.auth //发送心跳不需要auth
  let sendBytes = Message.encode(msg).finish();
  const mesLen = sendBytes.length;
  const buf = Buffer.alloc(4);
  buf.writeUInt32BE(mesLen);
  // // 发送数据到服务器
  client.write(buf);
  client.write(sendBytes);
  console.log('发送消息：',JSON.stringify(msg))
}
// 接收消息
const receiveMessage = () => {
  let buffer = Buffer.alloc(0); // 初始化空缓冲区
  // 监听服务器返回的数据
  client.on("data", (data) => {
    buffer = Buffer.concat([buffer, data]); // 将收到的数据添加到缓冲区
    while (buffer.length >= 4) {
      const messageLength = buffer.readUInt32BE(0); // 读取消息长度
      if (buffer.length >= 4 + messageLength) {
        const messageData = buffer.subarray(4, 4 + messageLength); // 提取完整的消息数据
        // 处理消息数据
        const msg = Message.decode(messageData);
        // console.log('收到消息：',JSON.stringify(msg));//打印json
        console.log('数据类型',msg.dataType,'数据',msg[Message.oneofs.dataBody.oneof[msg.dataType]]);
        buffer = buffer.subarray(4 + messageLength); // 更新缓冲区，去除已处理的消息数据
      } else {
        break; // 缓冲区中的数据不足以解析完整的消息，等待下一次接收数据
      }
    }
  });
}
// 发送心跳
const sendHeartbeat = () => {
  setInterval(() => {
    sendMessage(ActionType.values.Heartbeat)
  }, 10000)
}
const main = async () => {
  // 创建Socket客户端
  client = net.createConnection(config, () => {
    console.log("连接成功!");
    // 接收消息
    receiveMessage()
    // 登入
    sendMessage(ActionType.values.Login)
    // 心跳
    sendHeartbeat()

  });
  // 监听连接关闭事件
  client.on("close", () => {
    console.log("连接关闭!");
  });
}
main()