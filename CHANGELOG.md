# Changelog

## 0.34.0 (2026-01-15)

### New features

- Added `view` command: interactive TUI for exploring WARC files
  - view captures, WARC and HTTP headers
  - filter captures by type, status, method or url
  - save payload to a file, open in browser or external editor

### Fixed

- HttpParser: lenient mode now accepts "0" as a status code for compatibility with Browsertrix WARCs

## 0.33.0 (2025-12-24)

### New features

- CdxRecord: surt(), format(), values() and toString()
- CdxWriter
  - CDXJ output support
  - sort option
- HttpMessage: `Content-Encoding: zstd` support
- HttpRequest: `Content-Encoding: chunked` support
- WarcReader: [Zstandard compressed WARC Files](https://iipc.github.io/warc-specifications/specifications/warc-zstd/) support
- WarcServer: resource record support

### Fixed

- URIs.toNormalizedSurt(): Improved compatibility with Python [surt](https://github.com/internetarchive/surt).

## 0.32.0

### Added

- HeaderValidator with WARC/1.1 standard ruleset
- ExtractTool: can now extract sequential concurrent records (`--concurrent` option)
- DedupeTool
  - In-memory cache for cross-URL digest-based deduplication (`--cache-size` option)
  - Now prints deduplication statistics (`--dry-run` and `--quiet` options)
  - Multi-threaded deduplication (`--threads` option)
- ValidateTool
  - Multi-threaded validation (`--threads` option) 
- ParsingException message is now annotated with the source filename and record offset when available

### Fixed

- RFC5952 canonical form is now used for IPv6 addresses in WARC-IP-Address
- HttpParser in lenient mode now:
  - accepts responses missing version number
  - ignores header lines missing :
  - ignores folded status lines
- WarcParser: treats `alexa/dat` ARC records as not HTTP type
