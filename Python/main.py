# encoding:utf8
import socket

from config import ConnectConfig
from entity import market_data_pb2 as data_pb2


class SourceData:
    def __init__(self):
        self.host = ConnectConfig.HOST
        self.port = ConnectConfig.PORT
        self.auth = ConnectConfig.AUTH
        self.seconds = ConnectConfig.SECONDS

    def connect(self):
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.connect((self.host, int(self.port)))
        return s

    def requestor(self, req_type):
        message = data_pb2.Message()
        message.data_type = data_pb2.Message().DataType.CertificationMsgType
        message.certificationMsg.actionType = req_type
        # 判断是登录才发送auth
        if req_type == data_pb2.ActionType.Login:
            message.certificationMsg.auth = self.auth
        header = (len(message.SerializeToString())).to_bytes(4, byteorder='big')
        body = message.SerializeToString()
        return header + body

    async def send_heartbeat(self, client):
        while True:
            client.send(self.requestor(data_pb2.ActionType.Heartbeat))
            await asyncio.sleep(self.seconds)

    async def parse_msg(self, client):
        while True:
            response_header = client.recv(4)
            # 判断长度并接收消息体
            response_len = int.from_bytes(response_header, byteorder='big')
            data = client.recv(response_len)
            response_message = data_pb2.Message()
            try:
                response_message.ParseFromString(data)
                print('final_response=', response_message)
            except Exception as e:
                print("parse error", e)
            await asyncio.sleep(0)

    async def start(self):
        # 创建链接
        client = self.connect()
        # 发送认证信息
        client.send(self.requestor(data_pb2.ActionType.Login))
        # 并行携程执行
        await asyncio.gather(
            # 解析数据
            self.parse_msg(client),
            # 定时发送心跳
            self.send_heartbeat(client)
        )


if __name__ == '__main__':
    import asyncio

    app = SourceData()
    asyncio.run(app.start())
