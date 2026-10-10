#!/usr/bin/env python3
"""
Repack SMS Ledger: take a released APK, replace its WebView UI (assets/index.html),
bump versionCode / versionName in the binary AndroidManifest.xml, and write an
UNSIGNED, unaligned APK. The workflow then runs zipalign + apksigner on it.

Only two files inside the APK change; every other entry (code, resources,
Firebase config, native libs) is copied byte-for-byte after decompression.
"""
import argparse, re, struct, sys, zipfile

R_VERSION_CODE = 0x0101021B
R_VERSION_NAME = 0x0101021C
SIG_FILE = re.compile(r"^META-INF/[^/]+\.(SF|RSA|DSA|EC)$|^META-INF/MANIFEST\.MF$", re.I)


def u16(b, o): return struct.unpack_from("<H", b, o)[0]
def u32(b, o): return struct.unpack_from("<I", b, o)[0]


class StringPool:
    def __init__(self, buf, off):
        self.buf, self.off = buf, off
        self.count = u32(buf, off + 8)
        self.flags = u32(buf, off + 16)
        self.strings_start = off + u32(buf, off + 20)
        self.utf8 = bool(self.flags & (1 << 8))
        self.hsize = u16(buf, off + 2)

    def _loc(self, i):
        """Return (byte_offset_of_chars, char_len, byte_len) for string i."""
        p = self.strings_start + u32(self.buf, self.off + self.hsize + i * 4)
        b = self.buf
        if self.utf8:
            n = b[p]; p += 2 if n & 0x80 else 1                  # utf-16 length (skip)
            n = b[p]
            if n & 0x80: n = ((n & 0x7F) << 8) | b[p + 1]; p += 2
            else: p += 1
            return p, None, n
        n = u16(b, p); p += 2
        if n & 0x8000: n = ((n & 0x7FFF) << 16) | u16(b, p); p += 2
        return p, n, n * 2

    def get(self, i):
        p, _, bl = self._loc(i)
        raw = bytes(self.buf[p:p + bl])
        return raw.decode("utf-8" if self.utf8 else "utf-16-le")

    def set_same_length(self, i, new):
        p, _, bl = self._loc(i)
        enc = new.encode("utf-8" if self.utf8 else "utf-16-le")
        if len(enc) != bl:
            raise ValueError("new string must have the same length as the old one")
        self.buf[p:p + bl] = enc


def patch_manifest(data, version_code=None, version_name=None):
    buf = bytearray(data)
    if u16(buf, 0) != 0x0003:
        raise SystemExit("AndroidManifest.xml is not binary XML")
    pool = resmap = None
    off = u16(buf, 2)
    old = {}
    while off < len(buf):
        ctype, hsize, size = u16(buf, off), u16(buf, off + 2), u32(buf, off + 4)
        if ctype == 0x0001:
            pool = StringPool(buf, off)
        elif ctype == 0x0180:
            resmap = [u32(buf, off + 8 + 4 * k) for k in range((size - 8) // 4)]
        elif ctype == 0x0102:                                      # start tag
            ext = off + hsize
            name = pool.get(u32(buf, ext + 4))
            if name == "manifest":
                a_start, a_size, a_count = u16(buf, ext + 8), u16(buf, ext + 10), u16(buf, ext + 12)
                for k in range(a_count):
                    a = ext + a_start + k * a_size
                    nidx = u32(buf, a + 4)
                    rid = resmap[nidx] if resmap and nidx < len(resmap) else 0
                    dtype = buf[a + 15]
                    if rid == R_VERSION_CODE:
                        old["code"] = u32(buf, a + 16)
                        if version_code is not None:
                            if dtype not in (0x10, 0x11): raise SystemExit("unexpected versionCode type")
                            struct.pack_into("<I", buf, a + 16, version_code)
                    elif rid == R_VERSION_NAME:
                        sidx = u32(buf, a + 8)
                        old["name"] = pool.get(sidx)
                        if version_name is not None and version_name != old["name"]:
                            pool.set_same_length(sidx, version_name)
                break
        off += size
    if "code" not in old:
        raise SystemExit("versionCode not found in manifest")
    return bytes(buf), old


def bump_name(name):
    """1.153 -> 1.154, keeping the same number of characters (1.199 -> 1.200)."""
    m = re.match(r"^(.*?)(\d+)$", name)
    if not m: return name
    head, digits = m.groups()
    n = str(int(digits) + 1)
    if len(n) > len(digits):            # e.g. 1.999 -> carry into the major part
        maj = re.match(r"^(.*?)(\d+)\.$", head)
        if maj:
            new = f"{maj.group(1)}{int(maj.group(2)) + 1}.{'0' * len(digits)}"
            return new if len(new) == len(name) else name
        return name
    return head + n.zfill(len(digits))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--base", required=True)
    ap.add_argument("--html", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--version-code", type=int, default=0, help="0 = base + 1")
    ap.add_argument("--version-name", default="", help="empty = bump last number")
    args = ap.parse_args()

    zin = zipfile.ZipFile(args.base)
    _, old = patch_manifest(zin.read("AndroidManifest.xml"))
    code = args.version_code or old["code"] + 1
    name = args.version_name or bump_name(old["name"])
    if len(name) != len(old["name"]):
        print(f"::warning::versionName must keep {len(old['name'])} characters; keeping {old['name']}")
        name = old["name"]
    manifest, _ = patch_manifest(zin.read("AndroidManifest.xml"), code, name)
    html = open(args.html, "rb").read()
    if b"<html" not in html[:2000].lower():
        raise SystemExit("--html does not look like an HTML file")

    with zipfile.ZipFile(args.out, "w") as zout:
        seen = set()
        for info in zin.infolist():
            if SIG_FILE.match(info.filename) or info.filename in seen:
                continue
            seen.add(info.filename)
            data = zin.read(info.filename)
            if info.filename == "AndroidManifest.xml": data = manifest
            elif info.filename == "assets/index.html": data = html
            ni = zipfile.ZipInfo(info.filename, date_time=info.date_time)
            ni.compress_type = info.compress_type
            ni.external_attr = info.external_attr
            ni.create_system = info.create_system
            zout.writestr(ni, data, compress_type=info.compress_type, compresslevel=9)
        if "assets/index.html" not in seen:
            raise SystemExit("base APK has no assets/index.html")

    print(f"versionCode {old['code']} -> {code}")
    print(f"versionName {old['name']} -> {name}")
    gh = __import__("os").environ.get("GITHUB_OUTPUT")
    if gh:
        with open(gh, "a") as f: f.write(f"version_code={code}\nversion_name={name}\n")


if __name__ == "__main__":
    main()
