FROM golang:latest

WORKDIR /app

# 将Go程序复制到容器中
COPY . .

# 构建Go程序
RUN go build main.go

# 设置容器的入口点
CMD ["./main"]