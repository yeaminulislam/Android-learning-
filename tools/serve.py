#!/usr/bin/env python3
"""serve.py — প্রোটোটাইপ + APK ডাউনলোডের সার্ভার (Range/Resume সমর্থনসহ)।

বড় APK (৭৬ মেগাবাইট) মোবাইল নেটে নামাতে গেলে মাঝপথে কাটলে আবার শুরু থেকে
নামতে হয় — তাই আংশিক (Range) অনুরোধ সমর্থন করা হয়, ফলে ডাউনলোড থেমে গেলেও
ব্রাউজার/ডাউনলোড-ম্যানেজার সেখান থেকেই চালিয়ে যেতে পারে।

    python3 tools/serve.py [পোর্ট] [ডিরেক্টরি]
"""
import os
import re
import sys
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer

RANGE = re.compile(r'bytes=(\d*)-(\d*)')


class RangeHandler(SimpleHTTPRequestHandler):
    protocol_version = 'HTTP/1.1'

    def do_GET(self):
        path = self.translate_path(self.path)
        if os.path.isdir(path) or not os.path.exists(path):
            return super().do_GET()

        rng = self.headers.get('Range')
        size = os.path.getsize(path)
        start, end = 0, size - 1
        partial = False
        if rng:
            m = RANGE.search(rng)
            if m:
                g1, g2 = m.group(1), m.group(2)
                if g1:
                    start = int(g1)
                if g2:
                    end = min(int(g2), size - 1)
                if not g1 and g2:            # bytes=-N (শেষের N বাইট)
                    start = max(0, size - int(g2))
                if 0 <= start <= end < size:
                    partial = True

        ctype = self.guess_type(path)
        self.send_response(206 if partial else 200)
        self.send_header('Content-Type', ctype)
        self.send_header('Accept-Ranges', 'bytes')
        self.send_header('Content-Length', str(end - start + 1))
        if partial:
            self.send_header('Content-Range', 'bytes %d-%d/%d' % (start, end, size))
        name = os.path.basename(path)
        if name.endswith('.apk'):
            self.send_header('Content-Disposition',
                             'attachment; filename="%s"' % name)
        self.send_header('Cache-Control', 'no-cache')
        self.end_headers()
        if self.command == 'HEAD':
            return
        with open(path, 'rb') as f:
            f.seek(start)
            left = end - start + 1
            while left > 0:
                chunk = f.read(min(1 << 16, left))
                if not chunk:
                    break
                try:
                    self.wfile.write(chunk)
                except (BrokenPipeError, ConnectionResetError):
                    return
                left -= len(chunk)

    def do_HEAD(self):
        return self.do_GET()

    def log_message(self, fmt, *args):
        sys.stderr.write('%s %s\n' % (self.address_string(), fmt % args))


def main():
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8080
    root = sys.argv[2] if len(sys.argv) > 2 else os.path.join(
        os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'web')
    os.chdir(root)
    httpd = ThreadingHTTPServer(('0.0.0.0', port), RangeHandler)
    print('serving %s on 0.0.0.0:%d (Range/resume সমর্থনসহ)' % (root, port))
    httpd.serve_forever()


if __name__ == '__main__':
    main()
