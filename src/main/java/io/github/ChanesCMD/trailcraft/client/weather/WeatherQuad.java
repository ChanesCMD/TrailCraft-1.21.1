package io.github.ChanesCMD.trailcraft.client.weather;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Vector3f;

/**
 * Четырёхугольник частицы с произвольными осями (полоса капли вдоль скорости, круг на воде — лёжа). Обход вершин
 * выбирается так, чтобы к камере была лицевая сторона: изнанку частиц видеокарта отсекает.
 */
final class WeatherQuad {
    /**
     * @param cx центр относительно камеры
     * @param a  полуось «вправо» (u)
     * @param b  полуось «вверх» (v, верх текстуры — +b)
     */
    static void render(VertexConsumer buffer, float cx, float cy, float cz, Vector3f a, Vector3f b,
                       float u0, float u1, float v0, float v1, float r, float g, float bl, float alpha, int light) {
        // лицевая грань — обход против часовой стрелки на экране: нормаль (b × a) смотрит на камеру (камера в начале координат)
        if (a.cross(b, new Vector3f()).dot(cx, cy, cz) < 0) {
            a = new Vector3f(a).negate();
            float t = u0;
            u0 = u1;
            u1 = t;
        }
        vertex(buffer, cx - a.x - b.x, cy - a.y - b.y, cz - a.z - b.z, u1, v1, r, g, bl, alpha, light);
        vertex(buffer, cx - a.x + b.x, cy - a.y + b.y, cz - a.z + b.z, u1, v0, r, g, bl, alpha, light);
        vertex(buffer, cx + a.x + b.x, cy + a.y + b.y, cz + a.z + b.z, u0, v0, r, g, bl, alpha, light);
        vertex(buffer, cx + a.x - b.x, cy + a.y - b.y, cz + a.z - b.z, u0, v1, r, g, bl, alpha, light);
    }

    private static void vertex(VertexConsumer buffer, float x, float y, float z, float u, float v,
                               float r, float g, float b, float a, int light) {
        buffer.addVertex(x, y, z).setUv(u, v).setColor(r, g, b, a).setLight(light);
    }

    private WeatherQuad() {}
}
