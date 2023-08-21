package main

import (
	"demo/pb"
	"encoding/binary"

	// "encoding/json"
	"fmt"
	"io"
	"net"
	"os"
	"time"

	"google.golang.org/protobuf/proto"
	"gopkg.in/yaml.v2"
)

type Config struct {
	Host string `yaml:"host"`
	Port int    `yaml:"port"`
	Auth string `yaml:"auth"`
}

var config Config
var HEARTBEATTIME = 20 * time.Second // 心跳时间
// 发送消息
func sendMessage(conn net.Conn, actionType pb.ActionType) error {
	certMsg := &pb.CertificationMsg{
		ActionType: &actionType,
	}
	if actionType == pb.ActionType_Login {
		certMsg.Auth = &config.Auth
	}
	msg := pb.Message{
		DataType: pb.Message_CertificationMsgType.Enum(),
		DataBody: &pb.Message_CertificationMsg{
			CertificationMsg: certMsg,
		},
	}
	var readLen int
	sendBytes, err := proto.Marshal(&msg)
	if err != nil {
		fmt.Println("消息序列化失败：", err)
		return err
	}
	// 先获取发送的总字节数
	mesLen := uint32(len(sendBytes))
	// 将总字节数长度进行 Varints 编码成4个字节的byte
	var buf [4]byte
	binary.BigEndian.PutUint32(buf[:4], mesLen)
	// 先写入4个字节
	if readLen, err = conn.Write(buf[:4]); readLen != 4 && err != nil {
		fmt.Println("发送消息失败：", err)
		return err
	}
	_, err = conn.Write(sendBytes)
	if err != nil {
		fmt.Println("发送消息失败：", err)
		return err
	}
	fmt.Println("发送消息：", &msg)
	return nil
}

// 定时发送心跳
func sendHeartbeat(conn net.Conn, auth string) {
	for {
		time.Sleep(time.Duration(HEARTBEATTIME))
		sendMessage(conn, pb.ActionType_Heartbeat)
	}
}

// 接收消息
func receiveMessage(conn net.Conn) {
	for {
		headerBytes := make([]byte, 4)
		_, err := io.ReadFull(conn, headerBytes)
		if err != nil {
			fmt.Println("消息读取失败：", err)
			return
		}
		length := int(binary.BigEndian.Uint32(headerBytes))
		data := make([]byte, length)
		_, err = io.ReadFull(conn, data)
		if err != nil {
			fmt.Println("消息读取失败：", err)
			return
		}
		message := &pb.Message{}
		if err := proto.Unmarshal(data, message); err != nil {
			fmt.Println("消息反序列化失败：", err)
			return
		}
		// 打印消息类型和数据
		fmt.Println("类型：", message.GetDataType(), "数据：", message.GetDataBody(), "时间戳：", message.GetTimestamp(), "延迟：", time.Now().UnixNano()/1e6-*message.Timestamp, "ms")
		// 消息转json
		// json, err := json.Marshal(message)
		// if err != nil {
		// 	fmt.Println("转JSON失败:", err)
		// } else {
		// 	fmt.Println("收到消息：", string(json))
		// }
	}
}

func main() {
	// 读取配置文件
	file, err := os.ReadFile("config.yml")
	if err != nil {
		fmt.Println("无法读取配置文件：", err)
		return
	}
	// 解析YAML
	err = yaml.Unmarshal(file, &config)
	if err != nil {
		fmt.Println("无法解析配置文件：", err)
		return
	}
	// 连接socket
	conn, err := net.Dial("tcp", fmt.Sprintf("%s:%d", config.Host, config.Port))
	if err != nil {
		fmt.Println("socket连接失败：", err)
		return
	}
	defer conn.Close()

	// 开启接收消息
	go receiveMessage(conn)
	// 登入
	sendMessage(conn, pb.ActionType_Login)

	// 开启心跳
	go sendHeartbeat(conn, config.Auth)

	// 防止退出，持续接收
	select {}
}
