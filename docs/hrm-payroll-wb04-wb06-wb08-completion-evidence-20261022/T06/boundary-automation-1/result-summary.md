# T06 real-exporter boundary fixture test

- Candidate HEAD: `fdc503dca517b4dad4ea136f73bd9fbe92e11c59`
- Test SHA-256: `7626861eff2c8f8d43d6bbe0d081d48eb2eb227be054d4dcdfd36486b35b964a`
- Maven exit code: 0; Surefire: 1 test, 0 failures, 0 errors, 0 skipped; `BUILD SUCCESS`.
- Candidate test, production exporter, and prior governance test SHA values matched the fixed handoff before and after. See `candidate-sha256-*.txt`.
- Only the named standalone JUnit test ran. It emitted two synthetic TEST workbooks with the real exporter; they are not browser downloads, DB snapshots, or formal approvals.
- Exact artifact path and identities are in `fixture-identity.json`. Native Excel visual verification remains pending.
- Requested executor model: gpt-6-luna/max. Actual runtime model: unverified (no runtime evidence available).
