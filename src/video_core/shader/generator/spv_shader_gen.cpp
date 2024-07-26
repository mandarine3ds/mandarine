// Copyright 2024 Citra Emulator Project
// Licensed under GPLv2 or any later version
// Refer to the license.txt file included.

#include "video_core/pica/regs_rasterizer.h"
#include "video_core/shader/generator/shader_gen.h"
#include "video_core/shader/generator/spv_shader_gen.h"

using VSOutputAttributes = Pica::RasterizerRegs::VSOutputAttributes;

namespace Pica::Shader::Generator::SPIRV {

VertexModule::VertexModule() : Sirit::Module{SPIRV_VERSION_1_3} {
    DefineArithmeticTypes();
    DefineInterface();

    ids.sanitize_vertex = WriteFuncSanitizeVertex();

    DefineEntryPoint();
}

VertexModule::~VertexModule() = default;

void VertexModule::DefineArithmeticTypes() {
    ids.void_ = Name(TypeVoid(), "void_id");
    ids.bool_ = Name(TypeBool(), "bool_id");
    ids.f32 = Name(TypeFloat(32), "f32_id");
    ids.i32 = Name(TypeSInt(32), "i32_id");
    ids.u32 = Name(TypeUInt(32), "u32_id");

    for (u32 size = 2; size <= 4; size++) {
        const u32 i = size - 2;
        ids.bvec.ids[i] = Name(TypeVector(ids.bool_, size), fmt::format("bvec{}_id", size));
        ids.vec.ids[i] = Name(TypeVector(ids.f32, size), fmt::format("vec{}_id", size));
        ids.ivec.ids[i] = Name(TypeVector(ids.i32, size), fmt::format("ivec{}_id", size));
        ids.uvec.ids[i] = Name(TypeVector(ids.u32, size), fmt::format("uvec{}_id", size));
    }
}

void VertexModule::DefineEntryPoint() {
    AddCapability(spv::Capability::Shader);
    SetMemoryModel(spv::AddressingModel::Logical, spv::MemoryModel::GLSL450);

    const Id main_type{TypeFunction(TypeVoid())};
    const Id main_func{OpFunction(TypeVoid(), spv::FunctionControlMask::MaskNone, main_type)};

    const Id interface_ids[] = {
        // Inputs
        ids.vert_in_position,
        ids.vert_in_color,
        ids.vert_in_texcoord0,
        ids.vert_in_texcoord1,
        ids.vert_in_texcoord2,
        ids.vert_in_texcoord0_w,
        ids.vert_in_normquat,
        ids.vert_in_view,
        // Outputs
        ids.gl_position,
        ids.gl_clip_distance,
        ids.vert_out_color,
        ids.vert_out_texcoord0,
        ids.vert_out_texcoord1,
        ids.vert_out_texcoord2,
        ids.vert_out_texcoord0_w,
        ids.vert_out_normquat,
        ids.vert_out_view,
    };

    AddEntryPoint(spv::ExecutionModel::Vertex, main_func, "main", interface_ids);
}

void VertexModule::DefineInterface() {
    // Define interface block

    /// Inputs
    ids.vert_in_position =
        Name(DefineInput(ids.vec.Get(4), ATTRIBUTE_POSITION), "vert_in_position");
    ids.vert_in_color = Name(DefineInput(ids.vec.Get(4), ATTRIBUTE_COLOR), "vert_in_color");
    ids.vert_in_texcoord0 =
        Name(DefineInput(ids.vec.Get(2), ATTRIBUTE_TEXCOORD0), "vert_in_texcoord0");
    ids.vert_in_texcoord1 =
        Name(DefineInput(ids.vec.Get(2), ATTRIBUTE_TEXCOORD1), "vert_in_texcoord1");
    ids.vert_in_texcoord2 =
        Name(DefineInput(ids.vec.Get(2), ATTRIBUTE_TEXCOORD2), "vert_in_texcoord2");
    ids.vert_in_texcoord0_w =
        Name(DefineInput(ids.f32, ATTRIBUTE_TEXCOORD0_W), "vert_in_texcoord0_w");
    ids.vert_in_normquat =
        Name(DefineInput(ids.vec.Get(4), ATTRIBUTE_NORMQUAT), "vert_in_normquat");
    ids.vert_in_view = Name(DefineInput(ids.vec.Get(3), ATTRIBUTE_VIEW), "vert_in_view");

    /// Outputs
    ids.vert_out_color = Name(DefineOutput(ids.vec.Get(4), ATTRIBUTE_COLOR), "vert_out_color");
    ids.vert_out_texcoord0 =
        Name(DefineOutput(ids.vec.Get(2), ATTRIBUTE_TEXCOORD0), "vert_out_texcoord0");
    ids.vert_out_texcoord1 =
        Name(DefineOutput(ids.vec.Get(2), ATTRIBUTE_TEXCOORD1), "vert_out_texcoord1");
    ids.vert_out_texcoord2 =
        Name(DefineOutput(ids.vec.Get(2), ATTRIBUTE_TEXCOORD2), "vert_out_texcoord2");
    ids.vert_out_texcoord0_w =
        Name(DefineOutput(ids.f32, ATTRIBUTE_TEXCOORD0_W), "vert_out_texcoord0_w");
    ids.vert_out_normquat =
        Name(DefineOutput(ids.vec.Get(4), ATTRIBUTE_NORMQUAT), "vert_out_normquat");
    ids.vert_out_view = Name(DefineOutput(ids.vec.Get(3), ATTRIBUTE_VIEW), "vert_out_view");

    /// Uniforms

    // vs_data
    const Id type_vs_data = Name(TypeStruct(ids.u32, ids.vec.Get(4)), "vs_data");
    Decorate(type_vs_data, spv::Decoration::Block);

    ids.ptr_vs_data = AddGlobalVariable(TypePointer(spv::StorageClass::Uniform, type_vs_data),
                                        spv::StorageClass::Uniform);

    Decorate(ids.ptr_vs_data, spv::Decoration::DescriptorSet, 0);
    Decorate(ids.ptr_vs_data, spv::Decoration::Binding, 1);

    MemberName(type_vs_data, 0, "enable_clip1");
    MemberName(type_vs_data, 1, "clip_coef");

    MemberDecorate(type_vs_data, 0, spv::Decoration::Offset, 0);
    MemberDecorate(type_vs_data, 1, spv::Decoration::Offset, 16);

    /// Built-ins
    ids.gl_position = DefineVar(ids.vec.Get(4), spv::StorageClass::Output);
    Decorate(ids.gl_position, spv::Decoration::BuiltIn, spv::BuiltIn::Position);

    ids.gl_clip_distance =
        DefineVar(TypeArray(ids.f32, Constant(ids.u32, 2)), spv::StorageClass::Output);
    Decorate(ids.gl_clip_distance, spv::Decoration::BuiltIn, spv::BuiltIn::ClipDistance);
}

Id VertexModule::WriteFuncSanitizeVertex() {
    const Id func_type = TypeFunction(ids.vec.Get(4), ids.vec.Get(4));
    const Id func = Name(OpFunction(ids.vec.Get(4), spv::FunctionControlMask::MaskNone, func_type),
                         "SanitizeVertex");
    const Id arg_pos = OpFunctionParameter(ids.vec.Get(4));

    AddLabel(OpLabel());

    const Id result = AddLocalVariable(TypePointer(spv::StorageClass::Function, ids.vec.Get(4)),
                                       spv::StorageClass::Function);
    OpStore(result, arg_pos);

    const Id pos_z = OpCompositeExtract(ids.f32, arg_pos, 2);
    const Id pos_w = OpCompositeExtract(ids.f32, arg_pos, 3);

    const Id ndc_z = OpFDiv(ids.f32, pos_z, pos_w);

    // if (ndc_z > 0.f && ndc_z < 0.000001f)
    const Id test_1 =
        OpLogicalAnd(ids.bool_, OpFOrdGreaterThan(ids.bool_, ndc_z, Constant(ids.f32, 0.0f)),
                     OpFOrdLessThan(ids.bool_, ndc_z, Constant(ids.f32, 0.000001f)));

    {
        const Id true_label = OpLabel();
        const Id end_label = OpLabel();

        OpSelectionMerge(end_label, spv::SelectionControlMask::MaskNone);
        OpBranchConditional(test_1, true_label, end_label);
        AddLabel(true_label);

        // .z = 0.0f;
        OpStore(result, OpCompositeInsert(ids.vec.Get(4), ConstantNull(ids.f32), arg_pos, 2));

        OpBranch(end_label);
        AddLabel(end_label);
    }

    // if (ndc_z < -1.f && ndc_z > -1.00001f)
    const Id test_2 =
        OpLogicalAnd(ids.bool_, OpFOrdLessThan(ids.bool_, ndc_z, Constant(ids.f32, -1.0f)),
                     OpFOrdGreaterThan(ids.bool_, ndc_z, Constant(ids.f32, -1.00001f)));
    {
        const Id true_label = OpLabel();
        const Id end_label = OpLabel();

        OpSelectionMerge(end_label, spv::SelectionControlMask::MaskNone);
        OpBranchConditional(test_2, true_label, end_label);
        AddLabel(true_label);

        // .z = -.w;
        const Id neg_w = OpFNegate(ids.f32, OpCompositeExtract(ids.f32, arg_pos, 3));
        OpStore(result, OpCompositeInsert(ids.vec.Get(4), neg_w, arg_pos, 2));

        OpBranch(end_label);
        AddLabel(end_label);
    }

    OpReturnValue(OpLoad(ids.vec.Get(4), result));
    OpFunctionEnd();
    return func;
}

void VertexModule::Generate(Common::UniqueFunction<void, Sirit::Module&, const ModuleIds&> proc) {
    AddLabel(OpLabel());

    ids.ptr_enable_clip1 = OpAccessChain(TypePointer(spv::StorageClass::Uniform, ids.u32),
                                         ids.ptr_vs_data, Constant(ids.u32, 0));

    ids.ptr_clip_coef = OpAccessChain(TypePointer(spv::StorageClass::Uniform, ids.vec.Get(4)),
                                      ids.ptr_vs_data, Constant(ids.u32, 1));

    proc(*this, ids);
    OpReturn();
    OpFunctionEnd();
}

std::vector<u32> GenerateTrivialVertexShader(bool use_clip_planes) {
    VertexModule module;
    module.Generate([use_clip_planes](Sirit::Module& spv,
                                      const VertexModule::ModuleIds& ids) -> void {
        const Id pos_sanitized = spv.OpFunctionCall(
            ids.vec.Get(4), ids.sanitize_vertex, spv.OpLoad(ids.vec.Get(4), ids.vert_in_position));

        // Negate Z
        const Id neg_z = spv.OpFNegate(ids.f32, spv.OpCompositeExtract(ids.f32, pos_sanitized, 2));
        const Id negated_z = spv.OpCompositeInsert(ids.vec.Get(4), neg_z, pos_sanitized, 2);

        spv.OpStore(ids.gl_position, negated_z);

        // Pass-through
        spv.OpStore(ids.vert_out_color, spv.OpLoad(ids.vec.Get(4), ids.vert_in_color));
        spv.OpStore(ids.vert_out_texcoord0, spv.OpLoad(ids.vec.Get(2), ids.vert_in_texcoord0));
        spv.OpStore(ids.vert_out_texcoord1, spv.OpLoad(ids.vec.Get(2), ids.vert_in_texcoord1));
        spv.OpStore(ids.vert_out_texcoord2, spv.OpLoad(ids.vec.Get(2), ids.vert_in_texcoord2));
        spv.OpStore(ids.vert_out_texcoord0_w, spv.OpLoad(ids.f32, ids.vert_in_texcoord0_w));
        spv.OpStore(ids.vert_out_normquat, spv.OpLoad(ids.vec.Get(4), ids.vert_in_normquat));
        spv.OpStore(ids.vert_out_view, spv.OpLoad(ids.vec.Get(3), ids.vert_in_view));

        if (use_clip_planes) {
            spv.OpStore(spv.OpAccessChain(spv.TypePointer(spv::StorageClass::Output, ids.f32),
                                          ids.gl_clip_distance, spv.Constant(ids.u32, 0)),
                        neg_z);

            const Id enable_clip1 = spv.OpINotEqual(
                ids.bool_, spv.OpLoad(ids.u32, ids.ptr_enable_clip1), spv.Constant(ids.u32, 0));

            {
                const Id true_label = spv.OpLabel();
                const Id false_label = spv.OpLabel();
                const Id end_label = spv.OpLabel();

                spv.OpSelectionMerge(end_label, spv::SelectionControlMask::MaskNone);
                spv.OpBranchConditional(enable_clip1, true_label, false_label);
                {
                    spv.AddLabel(true_label);

                    spv.OpStore(
                        spv.OpAccessChain(spv.TypePointer(spv::StorageClass::Output, ids.f32),
                                          ids.gl_clip_distance, spv.Constant(ids.u32, 1)),
                        spv.OpDot(ids.f32, spv.OpLoad(ids.vec.Get(4), ids.ptr_clip_coef),
                                  pos_sanitized));

                    spv.OpBranch(end_label);
                }
                {
                    spv.AddLabel(false_label);

                    spv.OpStore(
                        spv.OpAccessChain(spv.TypePointer(spv::StorageClass::Output, ids.f32),
                                          ids.gl_clip_distance, spv.Constant(ids.u32, 1)),
                        spv.ConstantNull(ids.f32));

                    spv.OpBranch(end_label);
                }
                spv.AddLabel(end_label);
            }
        }
    });
    return module.Assemble();
}

} // namespace Pica::Shader::Generator::SPIRV