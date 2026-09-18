# YT v4.15.22

**Release date:** 2026-09-18

## New features

- Shorts quality now adapts to your connection. Auto used to mean "largest stream available" on any network; it now picks a height from the measured bandwidth, and both Wi-Fi and mobile data default to it. Fixed qualities are still there in Shorts video quality settings.
- A Short that fails to load offers a Retry button instead of sitting on a black screen.
- The mini music bar gives a small bounce when you play or pause.

## Improvements

- Music artwork shows a blurred preview while it loads, the way video thumbnails already did, so lists, grids and the mini player no longer start as empty boxes.
- The splash screen shows the logo alone, and its loading line is now a lit streak with a glow and a bright leading edge.
- The Shorts comments and description sheets have rounded top corners, and every bottom sheet settles in about half the time.
- Shorts spinners and the lyrics spinner now use the same loading animation as the rest of the app.

## Performance

- Lyrics load much faster. Sources are now queried at the same time instead of one after another, so the wait is no longer the sum of every source that had nothing. Your configured source order still decides which result is used.
- Lyrics already saved on your device appear immediately. Previously anything without word-by-word timings — which most lyrics are — was re-fetched from every source before it could be shown. Pull to refresh still searches for a better version.
- Shorts resolve one video further ahead, so fast scrolling is less likely to wait on the network mid-swipe.

## Fixes and stability

- Fixed a Short freezing on its last frame with no sound when you scrolled back to one you had already watched. This affected the auto-scroll and timed auto-scroll playback modes; looping was unaffected. Play/pause and headphone buttons recover from it too.
