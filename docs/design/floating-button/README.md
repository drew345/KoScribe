# Spoken K floating button

Andrew selected the second displayed concept, **Spoken K**, on 2026-10-26.
The approved reference is [selected-spoken-k.png](selected-spoken-k.png).
It is original Image Gen artwork, not a third-party logo.

The foreground in `app/src/main/res/drawable-nodpi/ko_spoken_k.png` was extracted
from that reference using Image Gen with a transparent background. Its SHA-256
is `B0CAC4CFA7CBB48457EE66304ABDF7ECA621B9BC6B1BA89AD606A418E936AA33`.
The source PNG is retained unchanged; native drawable sizing handles its padding.

`app/src/main/res/drawable/collapsed_bubble.xml` combines that opaque graphite
mark with an 80% opaque light gray body (`#CCDADDE1`). The existing 56×48 dp native
Button, 24 dp corner radius, click and drag handling remain. A centered 42 dp
bitmap layer with a 4 dp bottom inset places the visible mark at about 24×26 dp.
The accessible label is “Open KoScribe.” The launcher icon is separate.

Visual QA renders only the installed drawable into a transparent bitmap in the
owned smoke fixture. It does not capture a phone screen or change screenshot
protection. Build artifacts and that temporary render stay outside Git.
