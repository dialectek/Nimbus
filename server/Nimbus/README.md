Nimbus server
======================
Build:
```
build.sh
```

Run server:
``` 
nimbus_server.sh
```

Run client:
``` 
nimbus_client.sh [<id>] [<server network address> (defaults to localhost)]
```

Client commands:

```
peer_message:<agent name>;<message>
game_invite:<agent_name>
game_accept:<agent_name>
game_action:cooperate|betray
agent_score
game_quit
```



