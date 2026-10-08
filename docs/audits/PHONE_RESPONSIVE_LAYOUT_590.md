# Exalted.590 — Phone responsive layout

Base: Exalted.589.
Reference device: Samsung Galaxy A71 screenshot supplied by the user.

Changes are presentation-only; no NPC rules or business logic changed.

- Encounter NPC action buttons adapt to phone width and reserve vertical space, preventing overlap with the NPC title.
- Attributes use two presentation columns below 480dp instead of forcing three narrow columns.
- Skills use two presentation columns below 480dp instead of three.
- Sheet tab titles may use two lines on compact phones, avoiding unnecessary clipping while preserving horizontal navigation and tab order.
- Existing tablet/wide-screen three-column layouts are retained.
- InkButton compact sizing from Exalted.589 is retained.

Regression target: prevent the clipping/compression visible on the Galaxy A71 while keeping readable typography rather than shrinking all text.
