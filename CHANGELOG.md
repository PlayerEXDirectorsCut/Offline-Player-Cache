# Changelog

- Reimplemented the `OfflinePlayerCache`.
- Added internal type safety for values.
- Implemented more fleshed out command details, which will help provide more context on certain things.

## Changes

- `CacheableValue` has been transformed into `CachedPlayerKey`, which better represents its purpose, which is to be a middleman in caching your data into the server.
- `OfflinePlayerCacheAPI` is the class that will be used to both register and get the cache (via constructor with server).
- > *The cache and its API should **not be used on the client***.
  
In order to learn about registering and using your key, please consult with the readme or the wiki.