# YT v5.0.0

**Release date:** 2026-09-23

## Recommendations that listen to you

- New **Recommendation mix** setting under Settings → Music recommendations: **Familiar**, **Blend** or **Discover**. Familiar sticks to artists you already play; Discover mixes in more artists you have not heard yet. It applies to Quick Picks, Similar, Daily Discover and endless radio alike. Blend is the default and behaves exactly as before.
- Skipping a song now counts. A skip in the first 30 seconds lowers that artist a little, and a skip later on (but before halfway) lowers them slightly less. Before, a song you skipped after a few seconds taught the app nothing, and one you skipped after a minute still counted in the artist's favour. Only skips you make count: a track that fails to play and moves on by itself is not held against it.
- An artist you skip is pushed to the back of every list for the rest of that listening session, including radio tracks that were already lined up, and comes back once you listen to them properly again.
- When endless radio drifts and you skip two of its songs in a row, it now restarts from the last song you actually listened to. The songs already in your queue stay where they are.
- Songs endless radio picked for you now count half as much as songs you chose yourself, so a long radio session cannot slowly narrow what the app thinks you like.

## Updates

- Update checks now look at this app's own releases. They were still pointed at the project YT was forked from, so no update notice ever appeared for YT releases.
- A new update popup shows what changed and offers **Update** or **Maybe later**. Update downloads the new APK straight from GitHub, and Android asks before installing it.
- **Check for updates** in Settings → About checks straight away and tells you if you are already up to date or if the check failed.

## Fixes and stability

- Fixed songs that appear more than once in the queue: skipping to, or tapping, the second copy no longer jumps back to the first copy and replays the songs in between.
- "Play next" on a song that is already coming up later in the queue now moves it up, instead of adding a second copy that plays twice.
