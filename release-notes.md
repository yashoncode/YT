# YT v5.5.0

**Release date:** 2026-10-05

## Playback that keeps going

- Videos and songs no longer stall about 30 seconds to a minute in. When YouTube refuses a stream, YT now tells a refused stream apart from one whose link simply expired. It stops retrying the refused source for 30 minutes and switches to another one, instead of retrying the same refused source again and again.
- Music now picks its stream the same way videos do, so a source YouTube has refused is skipped for songs as well.
- The Diagnostics screen has a new **Reset YouTube session** action for when playback keeps failing. It clears the stored YouTube session so the next video starts a fresh one.
- The preferred audio language now picks the right dub. Before, a language code could match inside another language's name: "en" matched "French" and "hi" matched "Chinese", so a French dub could play instead of the English original.

## Fixes and stability

- Fixed a crash when returning from picture-in-picture, and when opening a link before the app had finished starting.
- Sharing a video to YT from another app now opens it in the YT window you already have, instead of starting a second copy inside the sharing app.
- Very long watch histories no longer crash the app. History is now read in pages.
- The player no longer adds another set of settings listeners every time it is rebuilt. These piled up over a long session.
- Stopping music, and the sleep timer's exit, now stop the music service cleanly. Before, the service could come back to the foreground after it had stopped.
- A download that stops receiving data now fails after 60 seconds instead of staying frozen at the same percentage.
- Shorts you have nearly finished (90% or more) are now hidden from the Shorts feed, matching the watched mark on them.
- The splash screen's loading glow no longer has hard, cut-off edges.
