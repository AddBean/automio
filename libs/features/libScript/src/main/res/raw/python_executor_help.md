Use `action=help` to show this. Use `print()` for output.

## Runtime
- Python 3.10 (Chaquopy), multi-line supported

## Limits (important)
- File system **read-only** (no writing files)
- Timeout **30s**
- Output ~**10000** chars (truncated)
- Keep memory < **10MB** / run; avoid very large loops (e.g. >100k)

## Not supported / limited
- `subprocess`, `os.system` (Android sandbox)
- Third-party packages: `requests`, `numpy`, `pandas`, etc.
- `multiprocessing` (unavailable); `threading` may be restricted

## Common available stdlib
`json`, `re`, `math`, `datetime`, `collections`, `itertools`, `functools`, `csv`, `io`, `typing`, `urllib.request`, `base64`, `hashlib`, `uuid`

## Tips
- Prefer in-memory data; use `io.StringIO` if you need “file-like” behavior
- Use generators to reduce memory
- Wrap risky code with `try/except` and split big tasks into multiple runs

## Mini examples (1-liners)
- JSON: `import json; print(json.dumps({"a": 1}, ensure_ascii=False))`
- HTTP GET: `import urllib.request as u; print(u.urlopen("https://httpbin.org/get", timeout=3).read().decode())`
- StringIO: `from io import StringIO; b=StringIO(); b.write("A\\nB"); print(b.getvalue())`
