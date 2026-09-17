#!/usr/bin/env python3
"""Build server pack zip from packwiz metadata."""
import os
import sys
import zipfile
import shutil
import tomllib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODS_DIR = os.path.join(ROOT, "mods")
OUTPUT_FILE = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, "AquaSMP-server.zip")

def get_server_mods():
    """Get list of server-side mod files from .pw.toml files."""
    server_mods = []
    for fname in sorted(os.listdir(MODS_DIR)):
        if fname.endswith(".pw.toml"):
            with open(os.path.join(MODS_DIR, fname), "rb") as f:
                meta = tomllib.load(f)
            side = meta.get("side", "both")
            filename = meta.get("filename", "")
            if side in ("server", "both") and filename:
                server_mods.append(filename)
    return server_mods

def main():
    print("Loading mod metadata...")
    server_mods = get_server_mods()
    print(f"Found {len(server_mods)} server-side mods")
    
    # Create stage directory
    stage = os.path.join(ROOT, "server-stage")
    if os.path.exists(stage):
        shutil.rmtree(stage)
    os.makedirs(stage)
    
    # Copy server files
    print("Copying server configs...")
    for dir_name in ["config", "kubejs", "defaultconfigs"]:
        src = os.path.join(ROOT, dir_name)
        if os.path.exists(src):
            shutil.copytree(src, os.path.join(stage, dir_name))
    
    # Remove client-only configs
    config_dir = os.path.join(stage, "config")
    if os.path.exists(config_dir):
        for root, dirs, files in os.walk(config_dir):
            for f in files:
                if "-client" in f:
                    os.remove(os.path.join(root, f))
    
    # Copy server.properties and icon
    for f in ["server.properties", "server-icon.png"]:
        src = os.path.join(ROOT, f)
        if os.path.exists(src):
            shutil.copy2(src, os.path.join(stage, f))
    
    # Copy server mods
    mods_stage = os.path.join(stage, "mods")
    os.makedirs(mods_stage)
    print("Copying server mods...")
    for mod_file in server_mods:
        src = os.path.join(MODS_DIR, mod_file)
        if os.path.exists(src):
            shutil.copy2(src, mods_stage)
        else:
            print(f"  WARNING: {mod_file} not found")
    
    # Create zip
    print(f"Creating {OUTPUT_FILE}...")
    with zipfile.ZipFile(OUTPUT_FILE, "w", zipfile.ZIP_DEFLATED) as zf:
        for root, dirs, files in os.walk(stage):
            for f in files:
                filepath = os.path.join(root, f)
                arcname = os.path.relpath(filepath, stage)
                zf.write(filepath, arcname)
    
    # Cleanup
    shutil.rmtree(stage)
    
    size = os.path.getsize(OUTPUT_FILE)
    print(f"Done! Server pack: {OUTPUT_FILE} ({size/1024/1024:.1f} MB)")

if __name__ == "__main__":
    main()