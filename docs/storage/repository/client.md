### client容器启动方式

```sh
docker run -ti -v /var/run/docker.sock:/var/run/docker.sock -v /usr/bin/docker:/usr/bin/docker bkrepo/client:latest /bin/bash
```
- 需要把母机的docker挂载进去，方便再容器中执行docker命令