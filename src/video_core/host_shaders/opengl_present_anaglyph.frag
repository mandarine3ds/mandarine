// Copyright 2023 Citra Emulator Project
// Licensed under GPLv2 or any later version
// Refer to the license.txt file included.

//? #version 430 core

// Rendepth: Red/Cyan Anaglyph Filter Optimized for Stereoscopic 3D on LCD Monitors by Andres Hernandez.
// Based on the paper "Producing Anaglyphs from Synthetic Images" by William Sanders, David F. McAllister.
// Using concepts from "Methods for computing color anaglyphs" by David F. McAllister, Ya Zhou, Sophia Sullivan.
// Original research from "Conversion of a Stereo Pair to Anaglyph with the Least-Squares Projection Method" by Eric Dubois

const mat3 l = mat3(
    vec3(0.4561, 0.500484, 0.176381),
    vec3(-0.400822, -0.0378246, -0.0157589),
    vec3(-0.0152161, -0.0205971, -0.00546856));

const mat3 r = mat3(
    vec3(-0.0434706, -0.0879388, -0.00155529),
    vec3(0.378476, 0.73364, -0.0184503),
    vec3(-0.0721527, -0.112961, 1.2264));

const vec3 g = vec3(1.6, 0.8, 1.0);

layout(location = 0) in vec2 frag_tex_coord;
layout(location = 0) out vec4 color;

layout(binding = 0) uniform sampler2D color_texture;
layout(binding = 1) uniform sampler2D color_texture_r;

uniform vec4 resolution;
uniform int layer;

vec3 correct_color(vec3 col) {
    vec3 result;
    result.r = pow(col.r, 1.0 / g.r);
    result.g = pow(col.g, 1.0 / g.g);
    result.b = pow(col.b, 1.0 / g.b);
    return result;
}

void main() {
    vec4 color_tex_l = texture(color_texture, frag_tex_coord);
    vec4 color_tex_r = texture(color_texture_r, frag_tex_coord);
    vec3 color_anaglyph = clamp(color_tex_l.rgb * l, vec3(0.0), vec3(1.0)) + clamp(color_tex_r.rgb * r, vec3(0.0), vec3(1.0));
    vec3 color_corrected = correct_color(color_anaglyph);
    color = vec4(color_corrected, color_tex_l.a);
}
