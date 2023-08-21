# Python

## 编译 .proto

完整linux内核环境下执行，生成文件到当前目录

    protoc xxx.proto --python_out= .

如果报错
    
    This file contains proto3 optional fields, but --experimental_allow_proto3_optional was not set.

执行以下命令

    protoc xxx.proto --python_out=. --experimental_allow_proto3_optional

