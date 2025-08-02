# socket-program-implementations
List of all different socket implementations from blocking, nonblocking, nio-blocking, nio-nonblocking and async servers.

All the servers return a reversed version of the input string and most of them shut down on 'exit'/'quit' client message (just to play around)

```bash
$ echo hello there | nc localhost 7000
ereht olleh

$ echo something | nc localhost 7000
gnihtemos
```
