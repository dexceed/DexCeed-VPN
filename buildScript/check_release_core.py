"""Verify that R8 retained every generated Go/JNI class used by the pinned native core."""
from io import BytesIO
from pathlib import Path
from zipfile import ZipFile
import sys
from check_release_ssh import classes


def verify(directory, library):
    with ZipFile(library) as archive:
        if library.suffix == ".aar":
            with ZipFile(BytesIO(archive.read("classes.jar"))) as jar:
                names = jar.namelist()
        else:
            names = archive.namelist()
    required = {"L" + name[:-6] + ";" for name in names
                if name.endswith(".class") and name.startswith(("app/rawline/core/", "go/"))}
    assert "Lapp/rawline/core/mobile/Mobile;" in required, "Wrong or missing core bindings"
    apks = sorted(Path(directory).glob("*.apk"))
    assert apks, "No APKs found"
    for apk in apks:
        found = set()
        with ZipFile(apk) as archive:
            for name in archive.namelist():
                if name.startswith("classes") and name.endswith(".dex"):
                    found.update(classes(archive.read(name)))
        missing = required - found
        assert not missing, f"{apk.name}: missing JNI classes: {sorted(missing)}"
        print(f"{apk.name}: all {len(required)} JNI classes retained")


if __name__ == "__main__":
    libraries = [Path("app/libs/RawlineCore.aar"), Path("app/libs/RawlineCore.jar")]
    library = next((p for p in libraries if p.is_file()), None)
    assert library is not None, "Prepare the core inputs first"
    verify(sys.argv[1], library)
