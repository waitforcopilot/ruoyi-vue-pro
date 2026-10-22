# Format-only delta provenance

Previous failed-format candidate SHA-256: `ad48036f300e1b6a190f7df52d1b150a54b0961cd11097be7589d7bb1fc96c77` for `WorkbenchPanel.vue`. Re-frozen formatted candidate SHA-256: `f115485d3201beaa23c84e76c561d994a3ccac8ee030ae6bc57487a099d06650`; `issueValidation.ts` and the regression script retain their prior frozen hashes.

Coder handoff states its temporary Prettier diff changed only the layout of two nodes and had no semantic changes. The earlier executor evidence retained the old candidate hash but did not archive its source bytes/diff, so an independent byte-for-byte old/new comparison cannot be reproduced from this evidence package. Current candidate hashes are checked before and after the static commands below.
