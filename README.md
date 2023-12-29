<img src="img/logo.png" alt="Offline Player Cache" />
<hr />

### What is Offline Player Cache?

Offline Player Cache (OPC) is a Minecraft API mod built using the Fabric framework to allow caching player data on the server while the player is offline. This means that mods can use this API to access offline player's data. This was primarily developed as a way to allow global and persistent leaderboards for Minecraft servers that included offline players.

### Specific Capabilities

Mods can register functions that take in a player and output a value/object; this value/object is limited only by what is specified in the key's read/write to/from nbt data. When a player disconnects from a server, all registered functions are called and the respective data is cached on the server before the player disconnects. This data is saved to the server's level data, and can be accessed any time. When the same player connects to the server, their cached data is deleted and instead when a value function is called, it returns the current value/object based on the online player.

### Commands

 - `/opc get uuid|name <uuid|name> <key>` Returns the input uuid/name player's value; if they are online, returns their current value, if they are offline returns their cached value; else returns null. If the value is an `instanceof` `Number` and run from a command block, the redstone output is the absolute modulus of 16.
 - `/opc remove uuid|name <uuid|name> <key>` If the input uuid/name player is offline, removes that player's cached value determined by the input key; if the player is online, does nothing.
 - `/opc remove uuid|name <uuid|name>` If the input uuid/name player is offline, removes all of their cached data; if the player is online, does nothing.
 
### Notes

 - This API mod does not itself register any cacheable values, and hence does not cache any player data, and therefore does nothing by itself. 
 - To use this API in your mod, you can include the following in your gradle build (see [Modrinth Maven](https://docs.modrinth.com/docs/tutorials/maven/) for more details):
 
```gradle
repositories {
    maven {
        name = "Modrinth"
        url = "https://api.modrinth.com/maven"
        content {
            includeGroup "maven.modrinth"
        }
    }
}
```

```gradle
dependencies {
    modImplementation "maven.modrinth:modrinth:opc-directors-cut:<version>"
    include "maven.modrinth:modrinth:opc-directors-cut:<version>"
}
```