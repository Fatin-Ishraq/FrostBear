"""FrostBear model generator: the single source of truth for the bear's geometry and texture.

Edit the SPEC section, then run:

    python tools/model/frostbear_model.py [--preview <out.html>]

It writes, relative to the repository root:
  common/src/main/java/com/example/frostbear/client/FrostBearModelLayer.java   (geometry)
  common/src/main/resources/assets/frostbear/textures/entity/frost_bear.png      (texture)
  common/src/main/resources/assets/frostbear/textures/entity/frost_bear_eyes.png (emissive layer)
and, with --preview, a standalone three.js page that renders the model with Minecraft's
exact box-UV mapping from several angles.

Coordinates are Minecraft model space: 1 unit = 1 texture pixel = 1/16 block, Y points
DOWN, the ground is y = 24, and the bear faces -Z. "left" parts sit on +X.
Box UVs use vanilla box mapping, so every box gets its own (2w + 2d) x (d + h) texture strip.
"""
import argparse
import base64
import json
import math
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
JAVA_OUT = ROOT / "common/src/main/java/com/example/frostbear/client/FrostBearModelLayer.java"
TEX_OUT = ROOT / "common/src/main/resources/assets/frostbear/textures/entity/frost_bear.png"
EYES_OUT = ROOT / "common/src/main/resources/assets/frostbear/textures/entity/frost_bear_eyes.png"
CRACKS_OUT = ROOT / "common/src/main/resources/assets/frostbear/textures/entity/frost_bear_cracks.png"
GLOW_ENRAGED_OUT = ROOT / "common/src/main/resources/assets/frostbear/textures/entity/frost_bear_glow_enraged.png"

# Phase 2 (Heart of Winter) layers: fractures over the base texture, and a brighter glow.
CRACK_FUR = (46, 78, 120)
CRACK_FUR_EDGE = (168, 208, 236)
CRACK_ICE = (238, 252, 255)
CRACK_GLOW = (40, 150, 215)
CRYSTAL_GLOW_SCALE = 0.45
# materials that crack in phase 2 (small details like teeth, claws and the nose stay intact)
CRACKING = {"fur", "fur_top", "fur_light", "face", "mane", "brow", "muzzle", "snout", "jaw", "paw", "tuft", "crystal"}


# --------------------------------------------------------------------------------------
# Spec helpers
# --------------------------------------------------------------------------------------

class Box:
    def __init__(self, lo, hi, mat, share=None):
        self.lo = lo          # absolute (x, y, z) min corner, or part-local for local boxes
        self.hi = hi
        self.mat = mat
        self.share = share    # boxes with the same share key reuse one texture strip
        self.uv = None
        self.local = None     # (x, y, z, w, h, d) relative to the owning part's pivot

    @property
    def size(self):
        return tuple(int(round(self.hi[i] - self.lo[i])) for i in range(3))


class Part:
    def __init__(self, name, pivot, rot=(0.0, 0.0, 0.0), boxes=(), children=(), local_boxes=False):
        self.name = name
        self.pivot = pivot            # absolute pivot (parents have no rest rotation)
        self.rot = rot
        self.boxes = list(boxes)
        self.children = list(children)
        self.local_boxes = local_boxes


def B(x0, y0, z0, x1, y1, z1, mat, share=None):
    return Box((x0, y0, z0), (x1, y1, z1), mat, share)


def mirror_box(b):
    return Box((-b.hi[0], b.lo[1], b.lo[2]), (-b.lo[0], b.hi[1], b.hi[2]), b.mat, b.share)


def crystal(name, pivot, kind, rot, mat="crystal"):
    """An ice crystal (or fur tuft): stacked, tapering prisms growing up (-Y) from the pivot."""
    tiers = {
        "tuft":   [(4, 5), (2, 3)],
        "huge":   [(8, 12), (5, 6), (2, 3)],
        "big":    [(6, 9), (3, 4)],
        "medium": [(5, 6), (2, 3)],
        "small":  [(3, 4), (1, 2)],
        "shard":  [(2, 3)],
    }[kind]
    boxes, y = [], 0
    for i, (w, h) in enumerate(tiers):
        lo = (-w / 2, y - h, -w / 2)
        hi = (w / 2, y, w / 2)
        boxes.append(Box(lo, hi, mat, share=f"{mat}_{kind}_{i}"))
        y -= h - 1  # tiers overlap by one pixel so the stack reads as one piece
    return Part(name, pivot, rot, boxes, local_boxes=True)


def leg(side, front):
    """A thick, three-segment leg (upper, lower, paw with claws) on the +X side, mirrored for right."""
    sx = 1 if side == "left" else -1
    z = -15 if front else 15
    depth = 14 if front else 16
    upper = [B(6, -14, z - depth / 2, 20, 10, z + depth / 2, "fur",
               share="front_upper" if front else "hind_upper")]
    lower = [B(7, 10, z - 6, 19, 18, z + 6, "fur", share="lower_leg")]
    paw = [B(6, 18, z - 9, 20, 24, z + 7, "paw", share="paw")]
    for cx in (7, 10, 13, 16):
        paw.append(B(cx, 21, z - 12, cx + 2, 24, z - 9, "claw", share="claw"))
    if sx < 0:
        upper, lower, paw = ([mirror_box(b) for b in bs] for bs in (upper, lower, paw))
    name = f"{'front' if front else 'hind'}_{side}_upper"
    return Part(name, (13 * sx, -10, z), boxes=upper, children=[
        Part("lower", (13 * sx, 10, z), boxes=lower, children=[
            Part("paw", (13 * sx, 18, z), boxes=paw),
        ]),
    ])


def mirrored_crystals(name, pivot, kind, rot, mat="crystal"):
    """A left (+X) crystal and its mirror image on the right.

    In this Y-down space a positive Z rotation leans a left crystal outward and a
    negative X rotation sweeps it back toward the tail."""
    x, y, z = pivot
    return [crystal(f"left_{name}", (x, y, z), kind, rot, mat),
            crystal(f"right_{name}", (-x, y, z), kind, (rot[0], -rot[1], -rot[2]), mat)]


def mirrored(side, box):
    return box if side == "left" else mirror_box(box)


def brow(side):
    """One half of the brow ridge, hinged at its outer end so the inner end can furrow."""
    sx = 1 if side == "left" else -1
    return Part(f"{side}_brow", (10 * sx, -19, -40.5),
                boxes=[mirrored(side, B(1, -21, -42, 10, -17, -39, "brow", share="brow"))],
                children=[crystal(f"{side}_brow_crystal", (7 * sx, -21, -40.5), "shard", (-0.25, 0.0, 0.45 * sx))])


def eyelid(side):
    """A lid hinged at its top edge over the painted eye; the animation scales it down to close."""
    sx = 1 if side == "left" else -1
    return Part(f"{side}_eyelid", (5.5 * sx, -17, -40.5),
                boxes=[mirrored(side, B(3, -17, -41, 8, -14, -40, "eyelid", share="eyelid"))])


def ear(side):
    sx = 1 if side == "left" else -1
    return Part(f"{side}_ear", (8 * sx, -19, -32), boxes=[
        mirrored(side, B(6, -22, -33, 10, -19, -31, "ear", share="ear")),
        mirrored(side, B(7, -23, -33, 9, -22, -31, "ear", share="ear_tip")),
    ])


# --------------------------------------------------------------------------------------
# SPEC: the FrostBear
# --------------------------------------------------------------------------------------

def build_spec():
    head = Part("head", (0, -10, -27), boxes=[
        B(-10, -19, -40, 10, -3, -27, "face"),          # broad skull
        B(-2, -20, -41, 2, -17, -39, "brow"),           # brow bridge between the two brows
        B(-11, -12, -38, 11, -3, -29, "fur_light"),     # cheek fluff
        B(-6, -12, -50, 6, -4, -40, "muzzle"),          # long bear muzzle
        B(-4, -13, -49, 4, -12, -41, "snout"),          # wrinkled snout ridge
        B(-3, -4, -50, 3, -3, -49, "tooth"),            # upper teeth
        B(3, -4, -49, 4, 0, -48, "tooth", share="fang"),
        B(-4, -4, -49, -3, 0, -48, "tooth", share="fang"),
    ], children=[
        Part("jaw", (0, -4, -38), boxes=[
            B(-5, -4, -49, 5, -1, -38, "jaw"),
            B(-6, -1, -46, 6, 1, -38, "fur_light"),      # chin fluff
            B(-3, -5, -49, 3, -4, -48, "tooth"),         # lower teeth
            B(2, -6, -48, 3, -4, -47, "tooth", share="lower_fang"),
            B(-3, -6, -48, -2, -4, -47, "tooth", share="lower_fang"),
        ]),
        Part("nose", (0, -11, -50), boxes=[B(-3, -13, -51, 3, -10, -49, "nose")]),
        brow("left"), brow("right"),
        eyelid("left"), eyelid("right"),
        ear("left"), ear("right"),
        *mirrored_crystals("cheek_tuft_0", (8, -6, -31), "tuft", (-1.45, 0.0, 0.45), mat="tuft"),
        *mirrored_crystals("cheek_tuft_1", (8, -10, -30), "tuft", (-1.25, 0.0, 0.6), mat="tuft"),
    ])

    neck = Part("neck", (0, -10, -23), boxes=[
        B(-13, -21, -29, 13, 0, -22, "mane"),           # thick fur collar
    ], children=[head])

    crystals = [
        # collar: big crystals framing the head
        *mirrored_crystals("collar_crystal_0", (12, -20, -18), "huge", (-0.15, 0.0, 0.55)),
        *mirrored_crystals("collar_crystal_1", (15, -13, -19), "big", (-0.2, 0.0, 1.0)),
        *mirrored_crystals("collar_crystal_2", (15, -6, -19), "small", (-0.1, 0.0, 1.3)),
        # two rows down the spine, swept back and shrinking toward the tail
        *mirrored_crystals("spine_crystal_0", (6, -23, -16), "huge", (-0.2, 0.0, 0.3)),
        *mirrored_crystals("spine_crystal_1", (6, -22, -7), "big", (-0.3, 0.0, 0.35)),
        *mirrored_crystals("spine_crystal_2", (5, -18, 1), "big", (-0.35, 0.0, 0.35)),
        *mirrored_crystals("spine_crystal_3", (5, -17, 9), "medium", (-0.4, 0.0, 0.35)),
        *mirrored_crystals("spine_crystal_4", (4, -17, 15), "small", (-0.45, 0.0, 0.3)),
        # scattered shards on the flanks
        *mirrored_crystals("flank_shard_0", (14, -12, -3), "shard", (0.0, 0.0, 1.35)),
        *mirrored_crystals("flank_shard_1", (14, -7, 8), "shard", (0.2, 0.0, 1.3)),
    ]

    body = Part("body", (0, -4, 12), boxes=[
        B(-15, -20, -24, 15, 2, -6, "fur"),               # massive chest and shoulders
        B(-12, -24, -22, 12, -20, -8, "fur_top"),         # shoulder hump
        B(-13, -17, -7, 13, 0, 6, "fur"),                 # belly
        B(-13, -16, 5, 13, 2, 19, "fur"),                 # hips, lower than the hump
        B(-10, -18, 7, 10, -16, 17, "fur_top"),           # rump
    ], children=[
        neck,
        Part("tail", (0, -12, 19), boxes=[B(-3, -14, 19, 3, -9, 23, "fur")]),
        leg("left", True), leg("right", True), leg("left", False), leg("right", False),
        *crystals,
    ])
    return Part("root", (0, 0, 0), children=[body])


# --------------------------------------------------------------------------------------
# Geometry: local box coordinates and texture packing
# --------------------------------------------------------------------------------------

def walk(part, parent=None):
    yield part, parent
    for child in part.children:
        yield from walk(child, part)


def rotate(v, rot):
    """Apply a ModelPart rotation (Quaternionf.rotationZYX: X first, then Y, then Z) to v."""
    x, y, z = v
    a, b, c = rot
    y, z = y * math.cos(a) - z * math.sin(a), y * math.sin(a) + z * math.cos(a)
    x, z = x * math.cos(b) + z * math.sin(b), -x * math.sin(b) + z * math.cos(b)
    x, y = x * math.cos(c) - y * math.sin(c), x * math.sin(c) + y * math.cos(c)
    return x, y, z


def seat_crystals(root, step=0.25, max_sink=10.0):
    """Push each crystal along its own axis until its base is fully embedded in its parent.

    A tilted crystal planted on a surface lifts one edge of its base off the body, and a
    pivot placed past the edge of a box floats outright. Every corner of the lowest tier's
    base must end up inside one of the parent part's own boxes (not a sibling's, which can
    animate away). Returns how far each moved crystal was sunk.
    """
    eps = 0.01
    moved = {}
    for part, parent in walk(root):
        if not part.local_boxes or parent is None:
            continue
        base = part.boxes[0]
        corners = [(x, base.hi[1], z) for x in (base.lo[0], base.hi[0]) for z in (base.lo[2], base.hi[2])]
        down = rotate((0.0, 1.0, 0.0), part.rot)

        def seated(pivot):
            for cx, cy, cz in corners:
                wx, wy, wz = rotate((cx, cy, cz), part.rot)
                p = (pivot[0] + wx, pivot[1] + wy, pivot[2] + wz)
                if not any(all(b.lo[i] - eps <= p[i] <= b.hi[i] + eps for i in range(3)) for b in parent.boxes):
                    return False
            return True

        sink = 0.0
        while not seated(tuple(part.pivot[i] + down[i] * sink for i in range(3))):
            sink += step
            if sink > max_sink:
                raise SystemExit(f"{part.name}: base never lands inside {parent.name}; move it closer to the body")
        if sink:
            part.pivot = tuple(round(part.pivot[i] + down[i] * sink, 3) for i in range(3))
            moved[part.name] = sink
    return moved


def localize(root):
    for part, _ in walk(root):
        for b in part.boxes:
            w, h, d = b.size
            if part.local_boxes:
                b.local = (b.lo[0], b.lo[1], b.lo[2], w, h, d)
            else:
                b.local = (b.lo[0] - part.pivot[0], b.lo[1] - part.pivot[1], b.lo[2] - part.pivot[2], w, h, d)


def pack(root, width=256):
    strips = {}
    for part, _ in walk(root):
        for b in part.boxes:
            w, h, d = b.size
            key = b.share or id(b)
            strips.setdefault(key, (2 * (w + d), d + h, []))[2].append(b)
    order = sorted(strips.values(), key=lambda s: (-s[1], -s[0]))
    x = y = shelf_h = 0
    for sw, sh, boxes in order:
        if x + sw > width:
            x, y, shelf_h = 0, y + shelf_h, 0
        for b in boxes:
            b.uv = (x, y)
        x += sw
        shelf_h = max(shelf_h, sh)
    used = y + shelf_h
    height = 64
    while height < used:
        height *= 2
    return width, height


# --------------------------------------------------------------------------------------
# Painting
# --------------------------------------------------------------------------------------

def noise(x, y, s):
    h = (x * 374761393 + y * 668265263 + s * 982451653) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((h ^ (h >> 16)) & 0xFFFF) / 65535.0


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def shade(c, k):
    return tuple(max(0, min(255, int(round(v * k)))) for v in c)


FUR = [(247, 250, 252), (238, 244, 248), (228, 237, 244), (217, 229, 238), (204, 219, 231)]
FUR_SHADOW = (150, 176, 202)


def fur_pixel(face, fx, fy, W, H, seed, bright=0.0, coarse=False):
    if face in ("top", "bottom"):
        v = noise(fx, fy, seed)
        idx = min(4, int(v * 4.2 + (0.6 if face == "bottom" else -0.3)))
        c = FUR[max(0, idx - (1 if bright > 0 else 0))]
        k = 1.03 if face == "top" else 0.74
    else:
        offset = int(noise(fx, 0, seed + 3) * 3)
        strand = noise(fx, (fy + offset) // (3 if coarse else 2), seed)
        t = fy / max(1, H - 1)
        idx = int(strand * 3.6 + t * 1.6 - bright * 2)
        c = FUR[max(0, min(4, idx))]
        k = 1.0 - 0.24 * t
        if fy == H - 1 and noise(fx, fy, seed + 11) > 0.45:
            k *= 0.88
    c = shade(c, k)
    return mix(c, FUR_SHADOW, max(0.0, (1.0 - k) * 1.3))


ICE_DEEP, ICE_MID, ICE_LIGHT, ICE_EDGE = (62, 132, 196), (118, 188, 236), (196, 238, 255), (44, 96, 156)
VEIN = (176, 236, 255)
VEIN_GLOW = (60, 170, 235)


def crystal_pixel(face, fx, fy, W, H, seed, veins):
    if face == "top":
        return ICE_LIGHT if noise(fx, fy, seed) > 0.3 else mix(ICE_LIGHT, ICE_MID, 0.4)
    if face == "bottom":
        return ICE_EDGE
    # a faceted prism: lit left edge, mid body, shadowed right edge, lighter toward the tip
    t = fy / max(1, H - 1)
    c = mix(ICE_MID, ICE_DEEP, t * 0.75)
    col = fx / max(1, W - 1)
    if col < 0.34:
        c = mix(c, ICE_LIGHT, 0.45)
    elif col > 0.7:
        c = mix(c, ICE_EDGE, 0.35)
    if noise(fx, fy, seed + 21) > 0.93:
        c = mix(c, (255, 255, 255), 0.7)
    if (fx, fy) in veins:
        c = VEIN
    return c


def vein_pixels(W, H, seed):
    if W < 4 or H < 6:
        return set()
    x = 1 + int(noise(seed, 1, 5) * (W - 2))
    pts = set()
    for y in range(H):
        pts.add((x, y))
        step = noise(x, y, seed + 9)
        x = max(1, min(W - 2, x + (-1 if step < 0.33 else 1 if step > 0.66 else 0)))
    return pts


EYE_GLOW = (70, 214, 255)
EYE_CORE = (170, 246, 255)
SOCKET = (28, 38, 54)
LIP = (38, 46, 62)


def face_front_pixel(fx, fy, W, H, seed):
    """Front of the skull: brow shadow and slanted glowing eyes."""
    base = fur_pixel("front", fx, fy, W, H, seed)
    mx = fx if fx < W / 2 else W - 1 - fx      # mirror so both eyes match; mx grows toward the centre
    # angry eyes: the glow drops toward the nose, under a dark brow line
    eye = {(3, 3): EYE_GLOW, (4, 3): EYE_CORE, (5, 3): SOCKET,
           (3, 4): SOCKET, (4, 4): EYE_GLOW, (5, 4): EYE_CORE, (6, 4): EYE_GLOW,
           (6, 3): SOCKET, (2, 3): SOCKET}
    if (mx, fy) in eye:
        return eye[(mx, fy)], eye[(mx, fy)] != SOCKET
    if fy == 2 and 2 <= mx <= 7:
        return shade(base, 0.66), False
    if 1 <= mx <= 7 and 2 <= fy <= 5:
        return shade(base, 0.88), False  # darker fur mask around the eyes
    return base, False


def crack_pixels(W, H, seed):
    """Jagged fracture lines: mostly straight runs that jog sideways now and then, with short
    branches. A crack keeps its sideways drift for a while, so it never loops back on itself."""
    if W < 4 or H < 4:
        return set()
    pts = set()
    for n in range(max(1, (W * H) // 110)):
        x = int(noise(seed, n, 31) * W)
        y = int(noise(seed, n, 37) * H)
        vertical = noise(seed, n, 41) > 0.4
        drift = 1 if noise(seed, n, 47) > 0.5 else -1
        length = int((H if vertical else W) * (0.5 + noise(seed, n, 43) * 0.5))
        for i in range(length):
            if 0 <= x < W and 0 <= y < H:
                pts.add((x, y))
            r = noise(i, n, seed + 13)
            if r > 0.9:
                drift = -drift  # an occasional change of direction
            jog = drift if r > 0.62 else 0
            if vertical:
                y += 1
                x += jog
            else:
                x += 1
                y += jog
            if noise(i, n, seed + 29) > 0.92:  # a short side branch
                bx, by = x, y
                for _ in range(2):
                    bx, by = (bx + drift, by + 1) if vertical else (bx + 1, by + drift)
                    if 0 <= bx < W and 0 <= by < H:
                        pts.add((bx, by))
    return pts


def paint_box(tex, emi, b, seed, cracks=None, glow2=None):
    w, h, d = b.size
    u, v = b.uv
    faces = {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "west": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "east": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
    }
    for face, (fu, fv, W, H) in faces.items():
        veins = vein_pixels(W, H, seed + len(face)) if b.mat == "crystal" and face not in ("top", "bottom") else set()
        fractures = crack_pixels(W, H, seed * 3 + len(face)) if b.mat in CRACKING and face != "bottom" else set()
        for fy in range(H):
            for fx in range(W):
                glow = None
                m = b.mat
                if m == "crystal":
                    c = crystal_pixel(face, fx, fy, W, H, seed, veins)
                    if (fx, fy) in veins:
                        glow = VEIN_GLOW
                elif m == "face" and face == "front":
                    c, lit = face_front_pixel(fx, fy, W, H, seed)
                    if lit:
                        glow = c
                elif m == "nose":
                    c = (26, 31, 42) if not (fy == 0 and fx < W - 1) else (78, 88, 104)
                    if face == "front" and fy == H - 2 and fx in (1, W - 2):
                        c = (8, 10, 16)  # nostrils
                elif m == "eyelid":
                    c = LIP if fy == H - 1 and face == "front" else shade(fur_pixel(face, fx, fy, W, H, seed), 0.86)
                elif m == "snout":
                    c = fur_pixel(face, fx, fy, W, H, seed, bright=1.0)
                    if face == "top" and fy % 2 == 1:
                        c = shade(c, 0.8)  # snarl wrinkles
                elif m == "tuft":
                    c = fur_pixel(face, fx, fy, W, H, seed, bright=0.6, coarse=True)
                elif m == "claw":
                    c = (30, 40, 66) if face != "top" else (66, 80, 112)
                    if face in ("front", "west", "east") and fy == 0:
                        c = (58, 72, 104)
                elif m == "tooth":
                    c = (240, 232, 208) if fy < H - 1 else (204, 192, 160)
                    if W >= 4 and face in ("front", "back") and fx % 2 == 1:
                        c = (176, 162, 128)  # gaps between teeth
                elif m == "jaw" and face == "top":
                    c = (150, 58, 70) if 1 <= fx < W - 1 else (110, 40, 52)
                elif m == "muzzle" and face == "bottom":
                    c = (92, 34, 46)
                elif m == "muzzle" and face in ("front", "west", "east") and fy == H - 1:
                    c = LIP
                elif m == "jaw" and face in ("front", "west", "east") and fy == 0:
                    c = LIP
                elif m == "paw" and face == "bottom":
                    c = (44, 52, 68) if noise(fx, fy, seed) > 0.25 else (60, 70, 88)
                elif m == "ear" and face == "front":
                    c = (178, 196, 214) if 0 < fx < W - 1 and fy > 0 else fur_pixel(face, fx, fy, W, H, seed)
                elif m in ("fur_light", "muzzle", "jaw", "ear"):
                    c = fur_pixel(face, fx, fy, W, H, seed, bright=1.0)
                elif m == "mane":
                    c = fur_pixel(face, fx, fy, W, H, seed, coarse=True)
                elif m == "brow":
                    c = shade(fur_pixel(face, fx, fy, W, H, seed), 0.94 if face != "bottom" else 0.7)
                else:  # fur, fur_top, face sides, paw sides
                    c = fur_pixel(face, fx, fy, W, H, seed, bright=0.4 if m == "fur_top" else 0.0)
                tex[fv + fy][fu + fx] = (*c, 255)
                if glow:
                    emi[fv + fy][fu + fx] = (*glow, 255)
                if cracks is None:
                    continue
                # phase 2: fractures, glowing crystals, and cold light leaking from the cracks
                bright = glow
                if m == "crystal":
                    bright = glow or shade(c, CRYSTAL_GLOW_SCALE)
                if (fx, fy) in fractures:
                    cracks[fv + fy][fu + fx] = (*(CRACK_ICE if m == "crystal" else CRACK_FUR), 255)
                    bright = CRACK_GLOW if m != "crystal" else (150, 235, 255)
                elif (fx - 1, fy) in fractures and m != "crystal":
                    cracks[fv + fy][fu + fx] = (*CRACK_FUR_EDGE, 255)
                if glow and glow in (EYE_GLOW, EYE_CORE):
                    bright = (200, 252, 255)
                if bright:
                    glow2[fv + fy][fu + fx] = (*bright, 255)


def write_png(path, pixels):
    h, w = len(pixels), len(pixels[0])
    raw = b"".join(b"\x00" + bytes(ch for px in row for ch in px) for row in pixels)

    def chunk(kind, data):
        body = kind + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    data = (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)
    return data


# --------------------------------------------------------------------------------------
# Java output
# --------------------------------------------------------------------------------------

def jf(v):
    return f"{float(v)}F"


def java_source(root, tw, th):
    lines = []
    names = {}

    def var(part, parent_var):
        base = part.name if parent_var in (None, "root") else f"{parent_var}_{part.name}"
        names[id(part)] = base
        return base

    def emit(part, parent_var):
        v = var(part, parent_var)
        rel = part.pivot
        if parent_var is not None:
            pp = PARENT[id(part)].pivot
            rel = tuple(part.pivot[i] - pp[i] for i in range(3))
        cubes = "CubeListBuilder.create()"
        for b in part.boxes:
            x, y, z, w, h, d = b.local
            cubes += (f"\n\t\t\t\t.texOffs({b.uv[0]}, {b.uv[1]})"
                      f".addBox({jf(x)}, {jf(y)}, {jf(z)}, {jf(w)}, {jf(h)}, {jf(d)})")
        if any(part.rot):
            pose = (f"PartPose.offsetAndRotation({jf(rel[0])}, {jf(rel[1])}, {jf(rel[2])}, "
                    f"{jf(part.rot[0])}, {jf(part.rot[1])}, {jf(part.rot[2])})")
        else:
            pose = f"PartPose.offset({jf(rel[0])}, {jf(rel[1])}, {jf(rel[2])})"
        target = "root" if parent_var is None else parent_var
        decl = f"PartDefinition {v} = " if part.children else ""
        lines.append(f"\t\t{decl}{target}.addOrReplaceChild(\"{part.name}\", {cubes},\n\t\t\t\t{pose});")
        for child in part.children:
            emit(child, v)

    PARENT = {}
    for part, parent in walk(root):
        if parent is not None:
            PARENT[id(part)] = parent
    for child in root.children:
        emit(child, None)

    body = "\n".join(lines)
    return f"""package com.example.frostbear.client;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * GENERATED by tools/model/frostbear_model.py — edit the spec there and re-run it,
 * do not edit this file by hand. The texture and emissive layer are generated with it.
 */
public final class FrostBearModelLayer {{
	public static final int TEXTURE_WIDTH = {tw};
	public static final int TEXTURE_HEIGHT = {th};

	private FrostBearModelLayer() {{}}

	public static LayerDefinition create() {{
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

{body}

		return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
	}}
}}
"""


# --------------------------------------------------------------------------------------
# Preview output
# --------------------------------------------------------------------------------------

def preview_tree(part, parent):
    rel = part.pivot if parent is None else tuple(part.pivot[i] - parent.pivot[i] for i in range(3))
    return {
        "name": part.name, "pivot": rel, "rot": part.rot,
        "cubes": [{"pos": b.local[:3], "size": b.local[3:], "uv": b.uv} for b in part.boxes],
        "children": [preview_tree(c, part) for c in part.children],
    }


PREVIEW = r"""<!doctype html><html><head><meta charset="utf-8"><title>FrostBear Model Preview</title>
<style>html,body{margin:0;background:#3a3d42;overflow:hidden;font:13px system-ui;color:#cfd6de}
#l{position:fixed;inset:0;display:grid;grid-template-columns:1fr 1fr;grid-template-rows:1fr 1fr;pointer-events:none}
#l div{padding:6px 10px;opacity:.7}</style></head><body>
<div id="l"><div>front</div><div>side (left)</div><div>three-quarter</div><div>rear three-quarter</div></div>
<script src="https://cdn.jsdelivr.net/npm/three@0.128.0/build/three.min.js"></script>
<script>
const MODEL = __MODEL__, TW = __TW__, TH = __TH__, POSE = __POSE__;
const W = innerWidth, H = innerHeight;
const renderer = new THREE.WebGLRenderer({antialias:true, preserveDrawingBuffer:true});
renderer.setSize(W, H); renderer.setPixelRatio(devicePixelRatio); document.body.appendChild(renderer.domElement);
renderer.setScissorTest(true);
function tex(src){const t=new THREE.TextureLoader().load(src, draw); t.magFilter=THREE.NearestFilter; t.minFilter=THREE.NearestFilter; t.flipY=false; t.encoding=THREE.sRGBEncoding; return t;}
renderer.outputEncoding = THREE.sRGBEncoding;
const base = new THREE.MeshLambertMaterial({map:tex("__TEX__"), alphaTest:0.1});
const ENRAGED = __ENRAGED__;
const cracks = new THREE.MeshLambertMaterial({map:tex("__CRACKS__"), alphaTest:0.1, polygonOffset:true, polygonOffsetFactor:-1});
const glow = new THREE.MeshBasicMaterial({map:tex("__EYES__"), transparent:true, blending:THREE.AdditiveBlending, depthWrite:false});
const scene = new THREE.Scene();
scene.add(new THREE.AmbientLight(0xffffff, 0.62));
const d1 = new THREE.DirectionalLight(0xffffff, 0.55); d1.position.set(0.3, 1, -0.8); scene.add(d1);
const d2 = new THREE.DirectionalLight(0xffffff, 0.25); d2.position.set(-0.5, 0.6, 1); scene.add(d2);
const grid = new THREE.GridHelper(160, 10, 0x55606a, 0x4a5058); scene.add(grid);
// Minecraft box UV mapping (ModelPart.Cube): per face [vertex corners..., u0, v0, u1, v1]
function cubeGeo(c){
  const [x0,y0,z0]=c.pos, [w,h,d]=c.size, x1=x0+w, y1=y0+h, z1=z0+d, [u,v]=c.uv;
  const u0=u, u1=u+d, u2=u+d+w, u22=u+d+w+w, u3=u+d+w+d, u4=u+d+w+d+w, v0=v, v1=v+d, v2=v+d+h;
  const t0=[x0,y0,z0],t1=[x1,y0,z0],t2=[x1,y1,z0],t3=[x0,y1,z0],l0=[x0,y0,z1],l1=[x1,y0,z1],l2=[x1,y1,z1],l3=[x0,y1,z1];
  const faces=[[[l1,l0,t0,t1],u1,v0,u2,v1],[[t2,t3,l3,l2],u2,v1,u22,v0],[[t0,l0,l3,t3],u0,v1,u1,v2],
               [[t1,t0,t3,t2],u1,v1,u2,v2],[[l1,t1,t2,l2],u2,v1,u3,v2],[[l0,l1,l2,l3],u3,v1,u4,v2]];
  const pos=[],uv=[],idx=[];
  faces.forEach(([vs,a,b,cc,dd],i)=>{
    const uvs=[[cc,b],[a,b],[a,dd],[cc,dd]];
    vs.forEach((p,k)=>{pos.push(...p); uv.push(uvs[k][0]/TW, uvs[k][1]/TH);});
    const o=i*4; idx.push(o,o+1,o+2,o,o+2,o+3);  // same counter-clockwise order Minecraft culls by
  });
  const g=new THREE.BufferGeometry(); g.setAttribute('position',new THREE.Float32BufferAttribute(pos,3));
  g.setAttribute('uv',new THREE.Float32BufferAttribute(uv,2)); g.setIndex(idx); g.computeVertexNormals(); return g;
}
function build(p){
  const grp=new THREE.Group(); grp.position.set(...p.pivot);
  const r=(POSE[p.name]||[0,0,0]); grp.rotation.order='ZYX';
  grp.rotation.set(p.rot[0]+r[0], p.rot[1]+r[1], p.rot[2]+r[2]);
  if (r.length >= 6) grp.scale.set(r[3], r[4], r[5]);
  p.cubes.forEach(c=>{const g=cubeGeo(c); grp.add(new THREE.Mesh(g,base)); if (ENRAGED) grp.add(new THREE.Mesh(g,cracks)); grp.add(new THREE.Mesh(g,glow));});
  p.children.forEach(ch=>grp.add(build(ch))); return grp;
}
const model=build(MODEL); const holder=new THREE.Group(); holder.add(model); holder.scale.set(-1,-1,1); holder.position.y=24; scene.add(holder);
const views=[[0,32,-230],[230,32,0],[-165,80,-165],[165,70,165]];
// ?view=N renders one view full-size; ?zoom=K moves the camera K times closer
const q=new URLSearchParams(location.search), single=q.has('view') ? +q.get('view') : -1, zoom=+(q.get('zoom')||1);
if (single >= 0) document.getElementById('l').style.display='none';
function draw(){
  views.forEach((p,i)=>{
    if (single >= 0 && i !== single) return;
    const full=single >= 0, vw=full?W:W/2, vh=full?H:H/2, x=full?0:(i%2)*vw, y=full?0:(i<2?vh:0);
    // ?cam=x,y,z&look=x,y,z override the camera (world space: y up, ground at 0, head toward -z)
    const camPos=q.has('cam') ? q.get('cam').split(',').map(Number) : p.map(c=>c/zoom);
    const look=q.has('look') ? q.get('look').split(',').map(Number) : [0,28,0];
    const cam=new THREE.PerspectiveCamera(30, vw/vh, 1, 2000); cam.position.set(...camPos); cam.lookAt(...look);
    renderer.setViewport(x,y,vw,vh); renderer.setScissor(x,y,vw,vh); renderer.render(scene,cam);
  });
}
draw();
</script></body></html>"""


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--preview", help="write a three.js preview page here")
    ap.add_argument("--pose", default="{}", help='JSON {"part": [x, y, z]} extra rotations for the preview')
    ap.add_argument("--enraged", action="store_true", help="preview phase 2: cracks and the brighter glow")
    args = ap.parse_args()

    root = build_spec()
    for name, sink in seat_crystals(root).items():
        print(f"seated {name}: sunk {sink:g} px into its parent")
    localize(root)
    tw, th = pack(root)
    tex = [[(0, 0, 0, 0)] * tw for _ in range(th)]
    emi = [[(0, 0, 0, 0)] * tw for _ in range(th)]
    cracks = [[(0, 0, 0, 0)] * tw for _ in range(th)]
    glow2 = [[(0, 0, 0, 0)] * tw for _ in range(th)]
    seed = 1
    for part, _ in walk(root):
        for b in part.boxes:
            seed += 7
            # shared strips are painted once per user; a stable seed keeps them identical
            paint_box(tex, emi, b, sum(map(ord, b.share)) if b.share else seed, cracks, glow2)
    tex_png = write_png(TEX_OUT, tex)
    eyes_png = write_png(EYES_OUT, emi)
    cracks_png = write_png(CRACKS_OUT, cracks)
    glow2_png = write_png(GLOW_ENRAGED_OUT, glow2)
    JAVA_OUT.write_text(java_source(root, tw, th), encoding="utf-8", newline="\n")
    boxes = sum(len(p.boxes) for p, _ in walk(root))
    print(f"model: {boxes} boxes, texture {tw}x{th}")

    if args.preview:
        page = (PREVIEW.replace("__MODEL__", json.dumps(preview_tree(root, None)))
                .replace("__TW__", str(tw)).replace("__TH__", str(th)).replace("__POSE__", args.pose)
                .replace("__TEX__", "data:image/png;base64," + base64.b64encode(tex_png).decode())
                .replace("__EYES__", "data:image/png;base64," + base64.b64encode(glow2_png if args.enraged else eyes_png).decode())
                .replace("__CRACKS__", "data:image/png;base64," + base64.b64encode(cracks_png).decode())
                .replace("__ENRAGED__", "true" if args.enraged else "false"))
        Path(args.preview).write_text(page, encoding="utf-8")
        print("preview:", args.preview)


if __name__ == "__main__":
    main()
