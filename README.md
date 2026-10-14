# AE2 Import Card

An upgrade card for Applied Energistics 2 that lets a Pattern Provider collect its own results.

Some machines finish a craft and keep the output in their own inventory instead of pushing it back into the network. Slot an Import Card into the Pattern Provider feeding them and the Provider pulls those results back itself, so the job completes without an extra import bus or pipe.

- Collects only what the Provider is still owed, from the side it sent the pattern to, so ingredients staged for the next step and other Providers' results are left alone.
- Keeps collecting for a short grace period after a job is cancelled, so results already in flight come back to the network.
- Works in AE2's Pattern Provider (block and cable) and in the pattern providers other AE2 addons add.
- Steps aside for Not Enough Patterns, which ships the same card, and keeps installed cards when either mod is added or removed.

See the card's page in the AE2 guide for details.
