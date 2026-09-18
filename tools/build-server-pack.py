#!/usr/bin/env python3
"""Build server pack zip from packwiz metadata."""
import os
import sys
import zipfile
import shutil
import tomllib
import urllib.request
import subprocess
import tempfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODS_DIR = os.path.join(ROOT, "mods")
OUTPUT_FILE = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, "AquaSMP-server.zip")

NEOFORGE_VERSION = "21.1.248"
NEOFORGE_INSTALLER_URL = f"https://maven.neoforged.net/releases/net/neoforged/neoforge/{NEOFORGE_VERSION}/neoforge-{NEOFORGE_VERSION}-installer.jar"

FORCE_SERVER_MODS = {
    "ponderjs-neoforge-1.21.1-2.4.0.jar",
}

EXCLUDE_FROM_SERVER = {
    "UIQuest-neoforge-1.21.1-1.0.4.jar",
}

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

def download_neoforge_installer(dest_path):
    """Download NeoForge installer if not present."""
    if os.path.exists(dest_path):
        print(f"NeoForge installer already exists: {dest_path}")
        return dest_path
    print(f"Downloading NeoForge installer {NEOFORGE_VERSION}...")
    urllib.request.urlretrieve(NEOFORGE_INSTALLER_URL, dest_path)
    print("Download complete")
    return dest_path

def run_neoforge_installer(installer_path, server_dir):
    """Run NeoForge installer to generate server files."""
    print("Running NeoForge installer...")
    result = subprocess.run(
        ["java", "-jar", installer_path, "--installServer", server_dir],
        capture_output=True, text=True, cwd=server_dir
    )
    if result.returncode != 0:
        print(f"NeoForge installer failed: {result.stderr}")
        raise RuntimeError("NeoForge installer failed")
    print("NeoForge server installed")

def main():
    print("Loading mod metadata...")
    server_mods = get_server_mods()
    
    # Force-add PonderJS
    for mod in FORCE_SERVER_MODS:
        if mod not in server_mods:
            server_mods.append(mod)
            print(f"  Force-adding {mod} to server mods")
    
    # Exclude UIQuest
    server_mods = [m for m in server_mods if m not in EXCLUDE_FROM_SERVER]
    if "UIQuest-neoforge-1.21.1-1.0.4.jar" in EXCLUDE_FROM_SERVER:
        print("  Excluding UIQuest from server mods")
    
    print(f"Found {len(server_mods)} server-side mods")
    
    # Create stage directory
    stage = os.path.join(ROOT, "server-stage")
    if os.path.exists(stage):
        shutil.rmtree(stage)
    os.makedirs(stage)
    
    # Download and run NeoForge installer
    with tempfile.TemporaryDirectory() as tmpdir:
        installer_path = os.path.join(tmpdir, f"neoforge-{NEOFORGE_VERSION}-installer.jar")
        download_neoforge_installer(installer_path)
        run_neoforge_installer(installer_path, stage)
    
    # Copy server files
    print("Copying server configs...")
    for dir_name in ["config", "kubejs", "defaultconfigs"]:
        src = os.path.join(ROOT, dir_name)
        if os.path.exists(src):
            shutil.copytree(src, os.path.join(stage, dir_name), dirs_exist_ok=True)
    
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
    os.makedirs(mods_stage, exist_ok=True)
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