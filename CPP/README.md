## 编译proto生成pb文件
`protoc --cpp_out=. ./data.proto`
## 编译程序(ubuntu系统)
`g++ main.cpp data.pb.cc -o main -L/usr/local/lib -lprotobuf -lyaml-cpp`
## 运行
`./main`
### 本demo只提供基础功能，请根据实际使用场景补充代码