"""Sync pack.toml's [index] hash with the current index.toml."""
import hashlib
import re
import sys

h = hashlib.sha256(open('index.toml', 'rb').read()).hexdigest()
p = 'pack.toml'
s = open(p, encoding='utf-8').read()
s2 = re.sub(r'(?m)^hash = "[0-9a-f]{64}"', 'hash = "%s"' % h, s, count=1)
if s2 == s:
    print('pack.toml hash already current:', h)
else:
    open(p, 'w', encoding='utf-8', newline='').write(s2)
    print('pack.toml hash ->', h)