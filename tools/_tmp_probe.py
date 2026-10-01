import hashlib
import re

h = hashlib.sha256(open('index.toml', 'rb').read()).hexdigest()
p = 'pack.toml'
s = open(p, encoding='utf-8').read()
s2 = re.sub(r'(?m)^hash = "[0-9a-f]{64}"', 'hash = "%s"' % h, s, count=1)
open(p, 'w', encoding='utf-8', newline='').write(s2)
print('new index hash:', h)
print('replaced:', s != s2)