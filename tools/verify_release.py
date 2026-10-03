import hashlib
import json
import os
import re
import tomllib
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
os.chdir(ROOT)

fail = []


def check(cond, msg):
    print(('OK   ' if cond else 'FAIL ') + msg)
    if not cond:
        fail.append(msg)


# --- pack.toml / index.toml consistency -------------------------------
pt = tomllib.load(open('pack.toml', 'rb'))
idx_hash = hashlib.sha256(open('index.toml', 'rb').read()).hexdigest()
check(pt['index']['hash'] == idx_hash, 'pack.toml hash matches index.toml')
VERSION = os.environ.get('AQUA_VERSION', '1.1.8-release')
check(pt['version'] == VERSION,
      'pack.toml version is %s (%s)' % (VERSION, pt['version']))

# --- index.toml must not contain dev output ---------------------------
idx_text = open('index.toml', encoding='utf-8', errors='ignore').read()
idx_files = re.findall(r'^file = "(.+)"$', idx_text, re.M)
dev = [f for f in idx_files if re.match(r'src/[^/]+/(build|run|run2|\.gradle)/', f)]
check(not dev, 'index.toml has no gradle build/run/.gradle output (%d entries total)' % len(idx_files))

# --- every pin matches the jar on disk --------------------------------
def sha512(p):
    h = hashlib.sha512()
    with open(p, 'rb') as f:
        for c in iter(lambda: f.read(4 << 20), b''):
            h.update(c)
    return h.hexdigest()


bad = []
pins = 0
for m in sorted(os.listdir('mods')):
    if not m.endswith('.pw.toml'):
        continue
    meta = tomllib.load(open(os.path.join('mods', m), 'rb'))
    fn = meta.get('filename')
    want = meta.get('download', {}).get('hash')
    if not fn or not want:
        continue
    pins += 1
    dest = os.path.join('mods', fn)
    if not os.path.isfile(dest):
        bad.append('%s: file missing' % fn)
    elif sha512(dest) != want:
        bad.append('%s: sha512 mismatch' % fn)
check(not bad, 'all %d pins match jars on disk %s' % (pins, bad or ''))

# --- required content present in index -------------------------------
required = [
    'mods/meowrelics.jar',
    'mods/meowrelics.pw.toml',
    'mods/lineage_core-2.1.1.jar',
    'mods/lineage_core-2-1-1.pw.toml',
    'mods/create-aeronautics-burner-fuel.pw.toml',
    'mods/structure_pool_api-neoforge-1.2.1+1.21.1.jar',
    'config/meowrelics/balance.json',
    'kubejs/data/create_rns/worldgen/structure_set/deposits.json',
    'kubejs/data/create_rns/worldgen/structure_set/nether_deposits.json',
    'kubejs/data/scguns/worldgen/structure_set/aboveground_medium.json',
    'kubejs/data/minecraft/worldgen/structure_set/strongholds.json',
    'kubejs/data/minecraft/worldgen/structure_set/ocean_monuments.json',
]
missing = [f for f in required if f not in idx_files]
check(not missing, 'required files present in index %s' % (missing or ''))

# --- structure_set overrides must all be empty and parseable ---------
import glob
import json as _json
ss = sorted(glob.glob('kubejs/data/*/worldgen/structure_set/*.json'))
nonempty = []
for f in ss:
    d = _json.load(open(f, encoding='utf-8'))
    if d.get('structures'):
        nonempty.append(f)
check(not nonempty, '%d structure_set overrides, all empty %s' % (len(ss), nonempty or ''))
check(len(ss) == 8, 'expected 8 structure_set overrides, got %d' % len(ss))

# --- mrpack ----------------------------------------------------------
mrp = 'AquaSMP-v%s.mrpack' % VERSION
if os.path.isfile(mrp):
    z = zipfile.ZipFile(mrp)
    names = z.namelist()
    man = _json.loads(z.read('modrinth.index.json'))
    ov = {n[len('overrides/'):] for n in names if n.startswith('overrides/')}
    # Pinned custom/CurseForge-only jars travel as overrides (not manifest downloads),
    # same as the other 12 pins.
    for j in ('mods/meowrelics.jar', 'mods/structure_pool_api-neoforge-1.2.1+1.21.1.jar',
              'mods/meowhex.jar', 'mods/aerofix-1.0.0.jar',
              'mods/lineage_core-2.1.1.jar'):
        check(j in ov, 'mrpack carries pinned jar %s' % j)
    # The clean export must not inline gradle output. Tracked sources ARE expected:
    # CI has always shipped them (v1.1.6 index had 3645 src entries).
    dev = [p for p in ov if re.match(r'src/[^/]+/(build|run|run2|\.gradle)/', p)]
    check(not dev, 'mrpack carries no gradle build/run/.gradle output (%d found)' % len(dev))
    print('     mrpack entries: %d, manifest files: %d, overrides: %d'
          % (len(names), len(man['files']), len(ov)))
else:
    check(False, 'mrpack present')

# --- prism zip -------------------------------------------------------
pz = 'AquaSMP-v%s-prism.zip' % VERSION
if os.path.isfile(pz):
    z = zipfile.ZipFile(pz)
    names = z.namelist()
    for probe in ('minecraft/mods/meowrelics.jar',
                  'minecraft/mods/structure_pool_api-neoforge-1.2.1+1.21.1.jar',
                  'minecraft/mods/colonycard-1.0.0.jar',
                  'minecraft/mods/lineage_core-2.1.1.jar',
                  'minecraft/mods/meowhex.jar',
                  'minecraft/mods/createburnerfuel-1.0.2.jar',
                  'minecraft/mods/meowrelics.jar',
                  'minecraft/kubejs/data/create_rns/worldgen/structure_set/deposits.json',
                  'minecraft/kubejs/data/scguns/worldgen/structure_set/aboveground_medium.json',
                  'minecraft/kubejs/data/minecraft/worldgen/structure_set/strongholds.json',
                  'mmc-pack.json', 'minecraft/icon.png'):
        check(probe in names, 'prism zip contains %s' % probe)
    check(not any(n.endswith('.pw.toml') for n in names), 'prism zip has no .pw.toml')
    jars = [n for n in names if n.startswith('minecraft/mods/') and n.endswith('.jar')]
    on_disk = {f for f in os.listdir('mods') if f.endswith('.jar')}
    check({n.split('/')[-1] for n in jars} == on_disk,
          'prism zip jar set == mods/ (%d jars)' % len(jars))
else:
    check(False, 'prism zip present')

print()
print('FAILURES: %d' % len(fail))
for f in fail:
    print('  -', f)