# Changelog

- Implemented the `OfflinePlayerCache` in its entirety.
- `internal` Added type safety for values (testing).

## Notice
***This is currently in a beta state. If you have issues with this mod, please open an issue in our GitHub page.
It is advised you do not utilize this in production work. at this time.***

Commands are still a work in progress. 💫

## Migration Guide

- A considerable amount of internal and external changes have happened with the mod.
- `CacheableValue` has been transformed into `CachedPlayerKey`, which better represents its purpose, which is to be a middleman in caching your data into the server.

### Using the Cache API
- In order to utilize the cache, utilize the new `OfflinePlayerCacheAPI`, which will allow you to register `CachedPlayerKeys` to the cache, and permit you to obtain data based on the keys provided.
- You can **call the constructor of the API** in order to use it, as long as you provide the server as an argument.
- > *The cache and its API should **not be used on the client***.

In order to learn about registering and using your key, please consult with the readme or the wiki!