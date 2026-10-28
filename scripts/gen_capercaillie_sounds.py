"""Звуки глухаря: assets/trailcraft/sounds/entity/capercaillie/*.ogg, события — sounds.json (init/ModSounds), субтитры.
Вскрик и смерть пока куриные.

Синтез из шума и тонов (нужны numpy и ffmpeg с libvorbis):
- song — ток: двойные щелчки «тэк-тэк», всё чаще, хлопок «пробки» и «точение» (шипящее скрежетание);
- ambient — низкое квохтанье «кок... кок».
Запуск: python scripts/gen_capercaillie_sounds.py
"""
import json
import os
import subprocess
import tempfile
import wave

import numpy as np

RATE = 44100
ASSETS = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "trailcraft")
OUT = os.path.join(ASSETS, "sounds", "entity", "capercaillie")
rng = np.random.default_rng(11)


def t(seconds):
    return np.arange(int(seconds * RATE)) / RATE


def band(signal, lo, hi):
    """Полосовой фильтр через БПФ (плавные края, чтобы не звенело)."""
    spec = np.fft.rfft(signal)
    f = np.fft.rfftfreq(len(signal), 1 / RATE)
    mask = 1 / (1 + ((f / max(hi, 1)) ** 6)) * (1 - 1 / (1 + (f / max(lo, 1)) ** 6) if lo > 0 else 1)
    return np.fft.irfft(spec * mask, len(signal))


def env(n, attack, decay):
    x = np.arange(n) / RATE
    return np.minimum(1, x / max(attack, 1e-4)) * np.exp(-x / decay)


def place(track, sound, at):
    i = int(at * RATE)
    end = min(len(track), i + len(sound))
    track[i:end] += sound[:end - i]


def click():
    """«Тэк»: сухой щелчок — удар шума и короткий деревянный тон."""
    n = int(0.05 * RATE)
    x = t(0.05)
    knock = band(rng.standard_normal(n), 1500, 4500) * env(n, 0.0005, 0.004)
    body = np.sin(2 * np.pi * 1250 * x) * env(n, 0.0005, 0.010) * 0.6
    return knock * 1.4 + body


def double_click():
    s = np.zeros(int(0.12 * RATE))
    place(s, click(), 0)
    place(s, click() * 0.8, 0.055)
    return s


def pop():
    """Хлопок «пробки»: глухой тон, быстро уходящий вниз."""
    x = t(0.09)
    freq = 900 * np.exp(-x * 18) + 380
    phase = 2 * np.pi * np.cumsum(freq) / RATE
    tone = np.sin(phase) * env(len(x), 0.002, 0.03)
    return tone * 1.2 + band(rng.standard_normal(len(x)), 300, 2000) * env(len(x), 0.001, 0.006) * 0.8


def whetting(seconds=2.6):
    """«Точение»: шипящие скребки по ~0,1 с, как нож о брусок."""
    s = np.zeros(int(seconds * RATE))
    at = 0.0
    while at < seconds - 0.12:
        n = int(0.1 * RATE)
        x = np.arange(n) / RATE
        shape = np.sin(np.pi * x / x[-1]) ** 1.5
        lo = 3000 + rng.uniform(-400, 400)
        stroke = band(rng.standard_normal(n), lo, lo + 4500) * shape
        stroke *= 1 + 0.5 * np.sin(2 * np.pi * 90 * x)  # шершавость
        place(s, stroke * rng.uniform(0.5, 0.8), at)
        at += rng.uniform(0.095, 0.125)
    return s * np.minimum(1, np.arange(len(s)) / (0.4 * RATE)) * np.minimum(1, (len(s) - np.arange(len(s))) / (0.3 * RATE))


def song():
    track = np.zeros(int(6.5 * RATE))
    at, gap = 0.2, 0.85
    while gap > 0.16:
        place(track, double_click(), at)
        at += gap
        gap *= 0.8
    for _ in range(4):  # трель в конце щелчков
        place(track, click(), at)
        at += 0.12
    place(track, pop() * 1.8, at + 0.05)
    place(track, whetting() * 0.35, at + 0.25)  # точение тише щелчков
    return track[:int((at + 2.9) * RATE)]


def croak(freq=150, seconds=0.16):
    """«Кок»: низкий тон с хрипом (частые импульсы) и форманта горла."""
    x = t(seconds)
    pulses = (np.sin(2 * np.pi * 28 * x) > 0.2).astype(float) * 0.6 + 0.4
    voice = sum(np.sin(2 * np.pi * freq * k * x) / k for k in range(1, 8)) * pulses
    voice = band(voice, 120, 1400) + band(voice, 600, 900) * 0.8
    return voice * env(len(x), 0.01, seconds / 2.5)


def ambient():
    track = np.zeros(int(1.1 * RATE))
    place(track, croak(150), 0.05)
    place(track, croak(135, 0.2) * 0.9, 0.55)
    return track


def save(name, signal):
    signal = signal / (np.max(np.abs(signal)) + 1e-9) * 0.85
    pcm = (signal * 32767).astype(np.int16)
    os.makedirs(OUT, exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        wav = os.path.join(tmp, f"{name}.wav")
        with wave.open(wav, "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(RATE)
            w.writeframes(pcm.tobytes())
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "5",
                        os.path.join(OUT, f"{name}.ogg")], check=True)


def write_json(path, data):
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def merge_sounds(events):
    """Дописывает события в sounds.json (там и звуки других животных — gen_animal_sounds.py)."""
    path = os.path.join(ASSETS, "sounds.json")
    data = {}
    if os.path.exists(path):
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
    data.update(events)
    write_json(path, data)


def merge_lang(entries):
    for code, values in entries.items():
        path = os.path.join(ASSETS, "lang", f"{code}.json")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        data.update(values)
        write_json(path, data)


def main():
    sounds = {}
    for name, make in (("song", song), ("ambient", ambient)):
        save(name, make())
        sounds[f"entity.capercaillie.{name}"] = {"subtitle": f"subtitles.trailcraft.entity.capercaillie.{name}",
                                                 "sounds": [f"trailcraft:entity/capercaillie/{name}"]}
    merge_sounds(sounds)
    merge_lang({"ru_ru": {"subtitles.trailcraft.entity.capercaillie.song": "Глухарь токует",
                          "subtitles.trailcraft.entity.capercaillie.ambient": "Глухарь квохчет"},
                "en_us": {"subtitles.trailcraft.entity.capercaillie.song": "Capercaillie displays",
                          "subtitles.trailcraft.entity.capercaillie.ambient": "Capercaillie clucks"}})
    print("ok")


if __name__ == "__main__":
    main()
