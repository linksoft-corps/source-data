## 更新依赖
`go mod tidy`
## 编译proto生成pb文件
`protoc --go_out=. ./pb/data.proto`
## 运行
`go run main.go`
### 本demo只提供基础功能，请根据实际使用场景补充代码