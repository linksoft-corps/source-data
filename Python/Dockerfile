FROM python:3.8
ENV PATH /usr/local/bin:$PATH
ADD . /app
WORKDIR /app
RUN pip3 install -r requirements.txt -i https://mirrors.aliyun.com/pypi/simple/
CMD python main.py