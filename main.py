from pathlib import Path

import nbtlib
import typer
from nbtlib import Compound, Int, List, String

DEFAULT_SOURCE = Path(
    "fabric/run/saves/Portals/generated/immersive_gateways/structures"
)
DEFAULT_DEST = Path(
    "common/src/main/resources/data/immersive_gateways/structures"
)
CHEST_ID = "minecraft:chest"
CHEST_LOOT_TABLE = "immersive_gateways:chests/default"
SUSPICIOUS_GRAVEL_ID = "minecraft:suspicious_gravel"
SUSPICIOUS_SAND_ID = "minecraft:suspicious_sand"
ARCHAEOLOGY_LOOT_TABLE = "immersive_gateways:archaeology/default"
AIR_NAME = "minecraft:air"
CAVE_AIR_NAME = "minecraft:cave_air"

app = typer.Typer()


def _palette_index(palette: List[Compound], name: str) -> int:
    for index, entry in enumerate(palette):
        if entry.get("Name") == name:
            return index
    palette.append(Compound({"Name": String(name)}))
    return len(palette) - 1


def _flood_fill(structure: Compound):
    palette = structure["palette"]
    air_index = _palette_index(palette, AIR_NAME)
    cave_air_index = _palette_index(palette, CAVE_AIR_NAME)

    blocks = structure["blocks"]
    for block in blocks:
        if int(block["state"]) == air_index:
            block["state"] = Int(cave_air_index)

    columns: dict[tuple[int, int], list[tuple[int, Compound]]] = {}
    for block in blocks:
        x, y, z = (int(coord) for coord in block["pos"])
        columns.setdefault((x, z), []).append((y, block))

    for entries in columns.values():
        entries.sort(key=lambda item: item[0])
        reached_solid = False
        for _, block in entries:
            state = int(block["state"])
            if state != cave_air_index:
                reached_solid = True
                continue
            if not reached_solid:
                block["state"] = Int(air_index)


def _set_loot_table(nbt: Compound, loot_table: str) -> None:
    nbt["LootTable"] = String(loot_table)


def _apply_loot_tables(structure: Compound) -> None:
    blocks = structure.get("blocks")
    if blocks is None:
        return

    for block in blocks:
        nbt = block.get("nbt")
        if not isinstance(nbt, Compound):
            continue
        block_id = str(nbt.get("id", ""))
        if block_id == CHEST_ID:
            _set_loot_table(nbt, CHEST_LOOT_TABLE)
        elif block_id in {SUSPICIOUS_GRAVEL_ID, SUSPICIOUS_SAND_ID}:
            _set_loot_table(nbt, ARCHAEOLOGY_LOOT_TABLE)


def _transform_file(source_path: Path, dest_path: Path):
    structure = nbtlib.load(source_path)
    _flood_fill(structure)
    _apply_loot_tables(structure)
    dest_path.parent.mkdir(parents=True, exist_ok=True)
    structure.save(dest_path)


def _iter_nbt_files(source_dir: Path) -> list[Path]:
    return [path for path in source_dir.rglob("*.nbt") if path.is_file()]


@app.command()
def run(
        source: Path = typer.Option(
            DEFAULT_SOURCE,
            "--source",
            "-s",
            help="Source directory with structure NBT files.",
        ),
        dest: Path = typer.Option(
            DEFAULT_DEST,
            "--dest",
            "-d",
            help="Destination directory for transformed NBT files.",
        ),
) -> None:
    if not source.exists():
        typer.echo(f"Source directory does not exist: {source}", err=True)
        raise typer.Exit(code=1)

    files = _iter_nbt_files(source)
    if not files:
        typer.echo(f"No .nbt files found under {source}")
        return

    for source_path in files:
        relative_path = source_path.relative_to(source)
        dest_path = dest / relative_path
        _transform_file(source_path, dest_path)

    typer.echo(f"Processed {len(files)} structure NBT files.")


if __name__ == "__main__":
    app()
