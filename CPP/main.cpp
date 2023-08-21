#include <iostream>
#include <unistd.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <thread>
#include <fstream>
#include <google/protobuf/util/json_util.h>
#include <yaml-cpp/yaml.h>
#include "data.pb.h"
int socketClient;
void receiveMessage() {
    while (true) {
        // 读取消息头，确定消息长度
        uint32_t length = 0;
        if (recv(socketClient, &length, sizeof(length), 0) != sizeof(length)) {
            std::cerr << "获取消息长度失败" << std::endl;
            exit(-1);
        }
        length = ntohl(length);

        // 读取消息体
        std::vector<char> buffer(length);
        char* bufferPtr = buffer.data();
        size_t bytesRead = 0;
        while (bytesRead < length) {
            ssize_t result = recv(socketClient, bufferPtr + bytesRead, length - bytesRead, 0);
            if (result == -1) {
                std::cerr << "获取消息失败" << std::endl;
                exit(-1);
            }
            bytesRead += result;
        }

        // 反序列化消息
        data::Message receiveMsg;
        if (!receiveMsg.ParseFromArray(buffer.data(), buffer.size())) {
            std::cerr << "反序列化失败" << std::endl;
            exit(-1);
        }

        // 消息转JSON
        std::string json;
        if (!google::protobuf::util::MessageToJsonString(receiveMsg, &json).ok()) {
            std::cerr << "转json失败" << std::endl;
        } else {
            std::cout << json << std::endl;
        }
    }
}
void sendMessage(const data::Message& message) {
    std::string serializedMsg = message.SerializeAsString();
    // 发送消息长度
    uint32_t length = htonl(serializedMsg.size());
    if (send(socketClient, &length, sizeof(length), 0) == -1) {
        std::cerr << "获取发送消息长度失败" << std::endl;
        close(socketClient);
        exit(-1);
    }

    // 发送消息体
    if (send(socketClient, serializedMsg.c_str(), serializedMsg.size(), 0) == -1) {
        std::cerr << "发送消息失败" << std::endl;
        close(socketClient);
        exit(-1);
    }
    std::string sendMsgJson;
    if (!google::protobuf::util::MessageToJsonString(message, &sendMsgJson).ok()) {
        std::cerr << "转json失败" << std::endl;
    } else {
        std::cout << "发送消息: " << sendMsgJson << std::endl;
    }
}
void sendHeartbeat() {
    while (true) {
        sleep(20); // 20秒发送一次心跳
        data::CertificationMsg certMsg;
        certMsg.set_actiontype(data::Heartbeat);
        data::Message message;
        message.set_data_type(data::Message::CertificationMsgType); 
        data::CertificationMsg* certMsgPtr = message.mutable_certificationmsg();
        certMsgPtr->CopyFrom(certMsg);
        sendMessage(message);
    }
}
int main() {
    // 打开YAML文件
    std::ifstream file("config.yml");
    if (!file) {
        std::cerr << "无法打开配置文件" << std::endl;
        exit(-1);
    }
    // 解析YAML文件
    YAML::Node config = YAML::Load(file);
    // 创建socket
    socketClient = socket(AF_INET, SOCK_STREAM, 0);
    if (socketClient == -1) {
        std::cerr << "创建socket失败" << std::endl;
        exit(-1);
    }

    // 设置服务器地址和端口
    std::string serverIP = config["host"].as<std::string>();
    int serverPort = config["port"].as<int>();
    struct sockaddr_in serverAddress{};
    serverAddress.sin_family = AF_INET;
    serverAddress.sin_addr.s_addr = inet_addr(serverIP.c_str());
    serverAddress.sin_port = htons(serverPort);

    // 连接socket
    if (connect(socketClient, (struct sockaddr*)&serverAddress, sizeof(serverAddress)) == -1) {
        std::cerr << "服务器连接失败" << std::endl;
        close(socketClient);
        exit(-1);
    }
    // 登入
    data::CertificationMsg certMsg;
    certMsg.set_actiontype(data::Login);
    certMsg.set_auth(config["auth"].as<std::string>());
    data::Message message;
    message.set_data_type(data::Message::CertificationMsgType); 
    data::CertificationMsg* certMsgPtr = message.mutable_certificationmsg();
    certMsgPtr->CopyFrom(certMsg);
    sendMessage(message);
    // 创建接收消息线程
    std::thread receiveThread(receiveMessage);
    // 发送心跳消息
    sendHeartbeat();
    // 等待接收消息线程结束
    receiveThread.join();
    close(socketClient);
    return 0;
}