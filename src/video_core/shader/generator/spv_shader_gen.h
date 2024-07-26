// Copyright 2024 Citra Emulator Project
// Licensed under GPLv2 or any later version
// Refer to the license.txt file included.

#pragma once

#include <sirit/sirit.h>

#include "common/common_types.h"
#include "common/unique_function.h"

namespace Pica {
struct ShaderSetup;
}

namespace Pica::Shader {
struct VSConfig;
struct FSConfig;
struct Profile;
} // namespace Pica::Shader

namespace Pica::Shader::Generator {
struct PicaVSConfig;
} // namespace Pica::Shader::Generator

namespace Pica::Shader::Generator::SPIRV {

using Sirit::Id;

constexpr u32 SPIRV_VERSION_1_3 = 0x00010300;

struct VectorIds {
    /// Returns the type id of the vector with the provided size
    [[nodiscard]] constexpr Id Get(u32 size) const {
        return ids[size - 2];
    }

    std::array<Id, 3> ids;
};

class VertexModule : public Sirit::Module {

public:
    explicit VertexModule();
    ~VertexModule();

private:
    template <bool global = true>
    [[nodiscard]] Id DefineVar(Id type, spv::StorageClass storage_class) {
        const Id pointer_type_id{TypePointer(storage_class, type)};
        return global ? AddGlobalVariable(pointer_type_id, storage_class)
                      : AddLocalVariable(pointer_type_id, storage_class);
    }

    /// Defines an input variable
    [[nodiscard]] Id DefineInput(Id type, u32 location) {
        const Id input_id{DefineVar(type, spv::StorageClass::Input)};
        Decorate(input_id, spv::Decoration::Location, location);
        return input_id;
    }

    /// Defines an output variable
    [[nodiscard]] Id DefineOutput(Id type, u32 location) {
        const Id output_id{DefineVar(type, spv::StorageClass::Output)};
        Decorate(output_id, spv::Decoration::Location, location);
        return output_id;
    }

    void DefineArithmeticTypes();
    void DefineEntryPoint();
    void DefineInterface();

    [[nodiscard]] Id WriteFuncSanitizeVertex();

public:
    struct ModuleIds {
        /// Types
        Id void_{};
        Id bool_{};
        Id f32{};
        Id i32{};
        Id u32{};

        VectorIds vec{};
        VectorIds ivec{};
        VectorIds uvec{};
        VectorIds bvec{};

        /// Input vertex attributes
        Id vert_in_position{};
        Id vert_in_color{};
        Id vert_in_texcoord0{};
        Id vert_in_texcoord1{};
        Id vert_in_texcoord2{};
        Id vert_in_texcoord0_w{};
        Id vert_in_normquat{};
        Id vert_in_view{};

        /// Output vertex attributes
        Id vert_out_color{};
        Id vert_out_texcoord0{};
        Id vert_out_texcoord1{};
        Id vert_out_texcoord2{};
        Id vert_out_texcoord0_w{};
        Id vert_out_normquat{};
        Id vert_out_view{};

        /// Uniforms

        // vs_data
        Id ptr_vs_data;
        Id ptr_enable_clip1;
        Id ptr_clip_coef;

        /// Built-ins
        Id gl_position;
        Id gl_clip_distance;

        /// Functions
        Id sanitize_vertex;
    } ids;

    /// Generate code using the provided SPIRV emitter context
    void Generate(Common::UniqueFunction<void, Sirit::Module&, const ModuleIds&> proc);
};

/**
 * Generates the SPIRV vertex shader program source code that accepts vertices from software shader
 * and directly passes them to the fragment shader.
 * @returns SPIRV shader assembly; empty on failure
 */
std::vector<u32> GenerateTrivialVertexShader(bool use_clip_planes);

} // namespace Pica::Shader::Generator::SPIRV
