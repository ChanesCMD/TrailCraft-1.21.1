"""Звуки животных TrailCraft (кроме глухаря — scripts/gen_capercaillie_sounds.py): карканье вороны и ворона.

Пишет assets/trailcraft/sounds/entity/<вид>/*.ogg (копии — docs/sounds/<вид>/), события в sounds.json (init/ModSounds)
и субтитры. Нужны numpy и ffmpeg с libvorbis.
Запуск: python scripts/gen_animal_sounds.py
"""
import os
import shutil

import numpy as np

import gen_capercaillie_sounds as synth
from gen_capercaillie_sounds import RATE, band, env, place, t

DOCS = os.path.join(os.path.dirname(__file__), "..", "docs", "sounds")
rng = np.random.default_rng(23)


def caw(f0, f1, seconds, rough, formants):
    """Хриплый крик: пила с дрожью, шум и форманты клюва и горла."""
    x = t(seconds)
    freq = np.linspace(f0, f1, len(x)) * (1 + 0.03 * np.sin(2 * np.pi * 7 * x))
    phase = 2 * np.pi * np.cumsum(freq) / RATE
    saw = 2 * ((phase / (2 * np.pi)) % 1) - 1
    raw = saw * (1 + rough * np.sin(2 * np.pi * 45 * x)) + band(rng.standard_normal(len(x)), 800, 4000) * 0.6
    voice = sum(band(raw, lo, hi) * k for lo, hi, k in formants)
    shape = np.minimum(1, x / 0.02) * np.minimum(1, (seconds - x) / 0.08)
    return voice * shape


def crow(calls):
    """«Кар-кар»: серая ворона, крик высокий и хриплый."""
    track = np.zeros(int((0.45 * calls + 0.2) * RATE))
    for i in range(calls):
        place(track, caw(560, 470, 0.32, 0.5, [(900, 1500, 1.0), (2200, 3000, 0.7)]) * (1 - 0.15 * i), 0.05 + 0.45 * i)
    return track


def raven(calls):
    """«Крук-крук»: ворон, глубокое гортанное курлыканье."""
    track = np.zeros(int((0.5 * calls + 0.2) * RATE))
    for i in range(calls):
        place(track, caw(300, 270, 0.26, 0.9, [(250, 900, 1.0), (1000, 1600, 0.6)]) * (1 - 0.1 * i), 0.05 + 0.5 * i)
    return track


SOUNDS = {
    "crow": {"ambient": [("caw1", lambda: crow(2)), ("caw2", lambda: crow(3))]},
    "raven": {"ambient": [("croak1", lambda: raven(2)), ("croak2", lambda: raven(1))]},
}
SUBTITLES = {
    "ru_ru": {"crow.ambient": "Ворона каркает", "raven.ambient": "Ворон каркает"},
    "en_us": {"crow.ambient": "Crow caws", "raven.ambient": "Raven croaks"},
}


def main():
    events = {}
    for mob, kinds in SOUNDS.items():
        for kind, files in kinds.items():
            names = []
            for name, make in files:
                synth.OUT = os.path.join(DOCS, mob)
                synth.save(name, make())
                target = os.path.join(synth.ASSETS, "sounds", "entity", mob)
                os.makedirs(target, exist_ok=True)
                shutil.copy(os.path.join(DOCS, mob, f"{name}.ogg"), os.path.join(target, f"{name}.ogg"))
                names.append(f"trailcraft:entity/{mob}/{name}")
            events[f"entity.{mob}.{kind}"] = {"subtitle": f"subtitles.trailcraft.entity.{mob}.{kind}", "sounds": names}
    synth.merge_sounds(events)
    synth.merge_lang({code: {f"subtitles.trailcraft.entity.{k}": v for k, v in names.items()}
                      for code, names in SUBTITLES.items()})
    print("ok")


if __name__ == "__main__":
    main()
