# YT v4.15.24

**Release date:** 2026-09-18

## Fixes and stability

- Fixed the Shorts tab showing "No shorts found" even though YouTube had returned videos. Shorts you had already watched, or that were shown to you in the past week, were filtered out — and if that removed every video on the page, the result was an empty feed that looked like a failure. Filtering now relaxes step by step rather than leaving nothing: it drops the "shown recently" rule first, then the "already watched" rule. A page of videos you have seen before is shown instead of an empty screen, and when there is anything fresh, that is still what you get.
- An empty Shorts feed now retries once on its own before showing the Retry button.
