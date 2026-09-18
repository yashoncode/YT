# YT v4.15.23

**Release date:** 2026-09-18

## Fixes and stability

- Fixed Shorts getting stuck on the last video, with nothing new loading and no way to scroll on. The feed only asked for more when you moved to a new short, so if a batch came back empty or a request failed, there was no page left to move to and nothing ever asked again. It now retries, and re-checks whenever the queue grows.
- Lyrics are fetched three sources at a time instead of all at once. Querying every source together made them compete for the connection, so on a weaker network each one was slower and they could all run out of time — which was worse than the version this replaced. The per-source time limit also goes back up from 5 to 8 seconds.
- Lyrics now stop searching shortly after finding something usable, instead of working through every remaining source hoping for a synced version.
