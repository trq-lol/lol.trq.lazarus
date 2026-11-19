/* SPDX-License-Identifier: LGPL-3.0-or-later */
package org.lwjgl.opengl;

import java.lang.reflect.Field;

/**
 * LWJGL 2's {@code ContextCapabilities}: one public flag per OpenGL version and extension. Generated from LWJGL 2.9.4's
 * field list; each flag is copied from the LWJGL 3 {@code GLCapabilities} field of the same name, and is {@code false}
 * when LWJGL 3 does not know it.
 */
public final class ContextCapabilities {
    /** {@code GL_AMD_blend_minmax_factor}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_AMD_blend_minmax_factor;

    /** {@code GL_AMD_conservative_depth}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_AMD_conservative_depth;

    /** {@code GL_AMD_debug_output}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_AMD_debug_output;

    /** {@code GL_AMD_depth_clamp_separate}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_AMD_depth_clamp_separate;

    /** {@code GL_AMD_draw_buffers_blend}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_AMD_draw_buffers_blend;

    /** {@code GL_AMD_interleaved_elements}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_AMD_interleaved_elements;

    /** {@code GL_AMD_multi_draw_indirect}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_AMD_multi_draw_indirect;

    /** {@code GL_AMD_name_gen_delete}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_AMD_name_gen_delete;

    /** {@code GL_AMD_performance_monitor}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_AMD_performance_monitor;

    /** {@code GL_AMD_pinned_memory}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_AMD_pinned_memory;

    /** {@code GL_AMD_query_buffer_object}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_AMD_query_buffer_object;

    /** {@code GL_AMD_sample_positions}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_AMD_sample_positions;

    /** {@code GL_AMD_seamless_cubemap_per_texture}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_AMD_seamless_cubemap_per_texture;

    /** {@code GL_AMD_shader_atomic_counter_ops}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_AMD_shader_atomic_counter_ops;

    /** {@code GL_AMD_shader_stencil_export}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_AMD_shader_stencil_export;

    /** {@code GL_AMD_shader_trinary_minmax}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_AMD_shader_trinary_minmax;

    /** {@code GL_AMD_sparse_texture}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_AMD_sparse_texture;

    /** {@code GL_AMD_stencil_operation_extended}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_AMD_stencil_operation_extended;

    /** {@code GL_AMD_texture_texture4}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_AMD_texture_texture4;

    /** {@code GL_AMD_transform_feedback3_lines_triangles}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_AMD_transform_feedback3_lines_triangles;

    /** {@code GL_AMD_vertex_shader_layer}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_AMD_vertex_shader_layer;

    /** {@code GL_AMD_vertex_shader_tessellator}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_AMD_vertex_shader_tessellator;

    /** {@code GL_AMD_vertex_shader_viewport_index}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_AMD_vertex_shader_viewport_index;

    /** {@code GL_APPLE_aux_depth_stencil}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_APPLE_aux_depth_stencil;

    /** {@code GL_APPLE_client_storage}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_APPLE_client_storage;

    /** {@code GL_APPLE_element_array}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_APPLE_element_array;

    /** {@code GL_APPLE_fence}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_APPLE_fence;

    /** {@code GL_APPLE_float_pixels}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_APPLE_float_pixels;

    /** {@code GL_APPLE_flush_buffer_range}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_APPLE_flush_buffer_range;

    /** {@code GL_APPLE_object_purgeable}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_APPLE_object_purgeable;

    /** {@code GL_APPLE_packed_pixels}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_APPLE_packed_pixels;

    /** {@code GL_APPLE_rgb_422}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_APPLE_rgb_422;

    /** {@code GL_APPLE_row_bytes}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_APPLE_row_bytes;

    /** {@code GL_APPLE_texture_range}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_APPLE_texture_range;

    /** {@code GL_APPLE_vertex_array_object}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_APPLE_vertex_array_object;

    /** {@code GL_APPLE_vertex_array_range}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_APPLE_vertex_array_range;

    /** {@code GL_APPLE_vertex_program_evaluators}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_APPLE_vertex_program_evaluators;

    /** {@code GL_APPLE_ycbcr_422}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_APPLE_ycbcr_422;

    /** {@code GL_ARB_ES2_compatibility}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_ES2_compatibility;

    /** {@code GL_ARB_ES3_1_compatibility}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_ES3_1_compatibility;

    /** {@code GL_ARB_ES3_compatibility}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_ES3_compatibility;

    /** {@code GL_ARB_arrays_of_arrays}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_arrays_of_arrays;

    /** {@code GL_ARB_base_instance}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_base_instance;

    /** {@code GL_ARB_bindless_texture}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_bindless_texture;

    /** {@code GL_ARB_blend_func_extended}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_blend_func_extended;

    /** {@code GL_ARB_buffer_storage}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_buffer_storage;

    /** {@code GL_ARB_cl_event}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_cl_event;

    /** {@code GL_ARB_clear_buffer_object}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_clear_buffer_object;

    /** {@code GL_ARB_clear_texture}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_clear_texture;

    /** {@code GL_ARB_clip_control}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_clip_control;

    /** {@code GL_ARB_color_buffer_float}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_color_buffer_float;

    /** {@code GL_ARB_compatibility}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_compatibility;

    /** {@code GL_ARB_compressed_texture_pixel_storage}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_compressed_texture_pixel_storage;

    /** {@code GL_ARB_compute_shader}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_compute_shader;

    /** {@code GL_ARB_compute_variable_group_size}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_compute_variable_group_size;

    /** {@code GL_ARB_conditional_render_inverted}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_conditional_render_inverted;

    /** {@code GL_ARB_conservative_depth}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_conservative_depth;

    /** {@code GL_ARB_copy_buffer}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_copy_buffer;

    /** {@code GL_ARB_copy_image}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_copy_image;

    /** {@code GL_ARB_cull_distance}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_cull_distance;

    /** {@code GL_ARB_debug_output}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_debug_output;

    /** {@code GL_ARB_depth_buffer_float}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_depth_buffer_float;

    /** {@code GL_ARB_depth_clamp}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_depth_clamp;

    /** {@code GL_ARB_depth_texture}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_depth_texture;

    /** {@code GL_ARB_derivative_control}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_derivative_control;

    /** {@code GL_ARB_direct_state_access}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_direct_state_access;

    /** {@code GL_ARB_draw_buffers}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_draw_buffers;

    /** {@code GL_ARB_draw_buffers_blend}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_draw_buffers_blend;

    /** {@code GL_ARB_draw_elements_base_vertex}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_draw_elements_base_vertex;

    /** {@code GL_ARB_draw_indirect}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_draw_indirect;

    /** {@code GL_ARB_draw_instanced}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_draw_instanced;

    /** {@code GL_ARB_enhanced_layouts}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_enhanced_layouts;

    /** {@code GL_ARB_explicit_attrib_location}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_explicit_attrib_location;

    /** {@code GL_ARB_explicit_uniform_location}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_explicit_uniform_location;

    /** {@code GL_ARB_fragment_coord_conventions}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_fragment_coord_conventions;

    /** {@code GL_ARB_fragment_layer_viewport}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_fragment_layer_viewport;

    /** {@code GL_ARB_fragment_program}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_fragment_program;

    /** {@code GL_ARB_fragment_program_shadow}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_fragment_program_shadow;

    /** {@code GL_ARB_fragment_shader}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_fragment_shader;

    /** {@code GL_ARB_framebuffer_no_attachments}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_framebuffer_no_attachments;

    /** {@code GL_ARB_framebuffer_object}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_framebuffer_object;

    /** {@code GL_ARB_framebuffer_sRGB}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_framebuffer_sRGB;

    /** {@code GL_ARB_geometry_shader4}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_geometry_shader4;

    /** {@code GL_ARB_get_program_binary}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_get_program_binary;

    /** {@code GL_ARB_get_texture_sub_image}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_get_texture_sub_image;

    /** {@code GL_ARB_gpu_shader5}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_gpu_shader5;

    /** {@code GL_ARB_gpu_shader_fp64}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_gpu_shader_fp64;

    /** {@code GL_ARB_half_float_pixel}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_half_float_pixel;

    /** {@code GL_ARB_half_float_vertex}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_half_float_vertex;

    /** {@code GL_ARB_imaging}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_imaging;

    /** {@code GL_ARB_indirect_parameters}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_indirect_parameters;

    /** {@code GL_ARB_instanced_arrays}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_instanced_arrays;

    /** {@code GL_ARB_internalformat_query}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_internalformat_query;

    /** {@code GL_ARB_internalformat_query2}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_internalformat_query2;

    /** {@code GL_ARB_invalidate_subdata}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_invalidate_subdata;

    /** {@code GL_ARB_map_buffer_alignment}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_map_buffer_alignment;

    /** {@code GL_ARB_map_buffer_range}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_map_buffer_range;

    /** {@code GL_ARB_matrix_palette}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_matrix_palette;

    /** {@code GL_ARB_multi_bind}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_multi_bind;

    /** {@code GL_ARB_multi_draw_indirect}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_multi_draw_indirect;

    /** {@code GL_ARB_multisample}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_multisample;

    /** {@code GL_ARB_multitexture}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_multitexture;

    /** {@code GL_ARB_occlusion_query}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_occlusion_query;

    /** {@code GL_ARB_occlusion_query2}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_occlusion_query2;

    /** {@code GL_ARB_pipeline_statistics_query}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_pipeline_statistics_query;

    /** {@code GL_ARB_pixel_buffer_object}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_pixel_buffer_object;

    /** {@code GL_ARB_point_parameters}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_point_parameters;

    /** {@code GL_ARB_point_sprite}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_point_sprite;

    /** {@code GL_ARB_program_interface_query}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_program_interface_query;

    /** {@code GL_ARB_provoking_vertex}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_provoking_vertex;

    /** {@code GL_ARB_query_buffer_object}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_query_buffer_object;

    /** {@code GL_ARB_robust_buffer_access_behavior}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_robust_buffer_access_behavior;

    /** {@code GL_ARB_robustness}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_robustness;

    /** {@code GL_ARB_robustness_isolation}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_robustness_isolation;

    /** {@code GL_ARB_sample_shading}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_sample_shading;

    /** {@code GL_ARB_sampler_objects}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_sampler_objects;

    /** {@code GL_ARB_seamless_cube_map}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_seamless_cube_map;

    /** {@code GL_ARB_seamless_cubemap_per_texture}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_seamless_cubemap_per_texture;

    /** {@code GL_ARB_separate_shader_objects}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_separate_shader_objects;

    /** {@code GL_ARB_shader_atomic_counters}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_shader_atomic_counters;

    /** {@code GL_ARB_shader_bit_encoding}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_shader_bit_encoding;

    /** {@code GL_ARB_shader_draw_parameters}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_shader_draw_parameters;

    /** {@code GL_ARB_shader_group_vote}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_shader_group_vote;

    /** {@code GL_ARB_shader_image_load_store}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_shader_image_load_store;

    /** {@code GL_ARB_shader_image_size}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_shader_image_size;

    /** {@code GL_ARB_shader_objects}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_shader_objects;

    /** {@code GL_ARB_shader_precision}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_shader_precision;

    /** {@code GL_ARB_shader_stencil_export}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_shader_stencil_export;

    /** {@code GL_ARB_shader_storage_buffer_object}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_shader_storage_buffer_object;

    /** {@code GL_ARB_shader_subroutine}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_shader_subroutine;

    /** {@code GL_ARB_shader_texture_image_samples}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_shader_texture_image_samples;

    /** {@code GL_ARB_shader_texture_lod}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_shader_texture_lod;

    /** {@code GL_ARB_shading_language_100}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_shading_language_100;

    /** {@code GL_ARB_shading_language_420pack}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_shading_language_420pack;

    /** {@code GL_ARB_shading_language_include}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_shading_language_include;

    /** {@code GL_ARB_shading_language_packing}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_shading_language_packing;

    /** {@code GL_ARB_shadow}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_shadow;

    /** {@code GL_ARB_shadow_ambient}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_shadow_ambient;

    /** {@code GL_ARB_sparse_buffer}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_sparse_buffer;

    /** {@code GL_ARB_sparse_texture}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_sparse_texture;

    /** {@code GL_ARB_stencil_texturing}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_stencil_texturing;

    /** {@code GL_ARB_sync}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_sync;

    /** {@code GL_ARB_tessellation_shader}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_tessellation_shader;

    /** {@code GL_ARB_texture_barrier}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_texture_barrier;

    /** {@code GL_ARB_texture_border_clamp}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_texture_border_clamp;

    /** {@code GL_ARB_texture_buffer_object}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_texture_buffer_object;

    /** {@code GL_ARB_texture_buffer_object_rgb32}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_texture_buffer_object_rgb32;

    /** {@code GL_ARB_texture_buffer_range}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_texture_buffer_range;

    /** {@code GL_ARB_texture_compression}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_texture_compression;

    /** {@code GL_ARB_texture_compression_bptc}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_texture_compression_bptc;

    /** {@code GL_ARB_texture_compression_rgtc}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_texture_compression_rgtc;

    /** {@code GL_ARB_texture_cube_map}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_texture_cube_map;

    /** {@code GL_ARB_texture_cube_map_array}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_texture_cube_map_array;

    /** {@code GL_ARB_texture_env_add}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_texture_env_add;

    /** {@code GL_ARB_texture_env_combine}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_texture_env_combine;

    /** {@code GL_ARB_texture_env_crossbar}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_texture_env_crossbar;

    /** {@code GL_ARB_texture_env_dot3}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_texture_env_dot3;

    /** {@code GL_ARB_texture_float}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_texture_float;

    /** {@code GL_ARB_texture_gather}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_texture_gather;

    /** {@code GL_ARB_texture_mirror_clamp_to_edge}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_texture_mirror_clamp_to_edge;

    /** {@code GL_ARB_texture_mirrored_repeat}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_texture_mirrored_repeat;

    /** {@code GL_ARB_texture_multisample}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_texture_multisample;

    /** {@code GL_ARB_texture_non_power_of_two}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_texture_non_power_of_two;

    /** {@code GL_ARB_texture_query_levels}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_texture_query_levels;

    /** {@code GL_ARB_texture_query_lod}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_texture_query_lod;

    /** {@code GL_ARB_texture_rectangle}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_texture_rectangle;

    /** {@code GL_ARB_texture_rg}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_texture_rg;

    /** {@code GL_ARB_texture_rgb10_a2ui}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_texture_rgb10_a2ui;

    /** {@code GL_ARB_texture_stencil8}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_texture_stencil8;

    /** {@code GL_ARB_texture_storage}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_texture_storage;

    /** {@code GL_ARB_texture_storage_multisample}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_texture_storage_multisample;

    /** {@code GL_ARB_texture_swizzle}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_texture_swizzle;

    /** {@code GL_ARB_texture_view}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_texture_view;

    /** {@code GL_ARB_timer_query}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_timer_query;

    /** {@code GL_ARB_transform_feedback2}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_transform_feedback2;

    /** {@code GL_ARB_transform_feedback3}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_transform_feedback3;

    /** {@code GL_ARB_transform_feedback_instanced}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_transform_feedback_instanced;

    /** {@code GL_ARB_transform_feedback_overflow_query}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_transform_feedback_overflow_query;

    /** {@code GL_ARB_transpose_matrix}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_transpose_matrix;

    /** {@code GL_ARB_uniform_buffer_object}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_uniform_buffer_object;

    /** {@code GL_ARB_vertex_array_bgra}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_vertex_array_bgra;

    /** {@code GL_ARB_vertex_array_object}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_vertex_array_object;

    /** {@code GL_ARB_vertex_attrib_64bit}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_vertex_attrib_64bit;

    /** {@code GL_ARB_vertex_attrib_binding}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_vertex_attrib_binding;

    /** {@code GL_ARB_vertex_blend}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_vertex_blend;

    /** {@code GL_ARB_vertex_buffer_object}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_vertex_buffer_object;

    /** {@code GL_ARB_vertex_program}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_vertex_program;

    /** {@code GL_ARB_vertex_shader}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_vertex_shader;

    /** {@code GL_ARB_vertex_type_10f_11f_11f_rev}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_vertex_type_10f_11f_11f_rev;

    /** {@code GL_ARB_vertex_type_2_10_10_10_rev}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_vertex_type_2_10_10_10_rev;

    /** {@code GL_ARB_viewport_array}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_viewport_array;

    /** {@code GL_ARB_window_pos}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ARB_window_pos;

    /** {@code GL_ATI_draw_buffers}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ATI_draw_buffers;

    /** {@code GL_ATI_element_array}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ATI_element_array;

    /** {@code GL_ATI_envmap_bumpmap}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ATI_envmap_bumpmap;

    /** {@code GL_ATI_fragment_shader}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ATI_fragment_shader;

    /** {@code GL_ATI_map_object_buffer}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ATI_map_object_buffer;

    /** {@code GL_ATI_meminfo}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ATI_meminfo;

    /** {@code GL_ATI_pn_triangles}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ATI_pn_triangles;

    /** {@code GL_ATI_separate_stencil}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ATI_separate_stencil;

    /** {@code GL_ATI_shader_texture_lod}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ATI_shader_texture_lod;

    /** {@code GL_ATI_text_fragment_shader}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ATI_text_fragment_shader;

    /** {@code GL_ATI_texture_compression_3dc}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ATI_texture_compression_3dc;

    /** {@code GL_ATI_texture_env_combine3}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ATI_texture_env_combine3;

    /** {@code GL_ATI_texture_float}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ATI_texture_float;

    /** {@code GL_ATI_texture_mirror_once}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ATI_texture_mirror_once;

    /** {@code GL_ATI_vertex_array_object}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ATI_vertex_array_object;

    /** {@code GL_ATI_vertex_attrib_array_object}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ATI_vertex_attrib_array_object;

    /** {@code GL_ATI_vertex_streams}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_ATI_vertex_streams;

    /** {@code GL_EXT_Cg_shader}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_Cg_shader;

    /** {@code GL_EXT_abgr}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_abgr;

    /** {@code GL_EXT_bgra}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_bgra;

    /** {@code GL_EXT_bindable_uniform}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_bindable_uniform;

    /** {@code GL_EXT_blend_color}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_blend_color;

    /** {@code GL_EXT_blend_equation_separate}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_blend_equation_separate;

    /** {@code GL_EXT_blend_func_separate}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_blend_func_separate;

    /** {@code GL_EXT_blend_minmax}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_blend_minmax;

    /** {@code GL_EXT_blend_subtract}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_blend_subtract;

    /** {@code GL_EXT_compiled_vertex_array}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_compiled_vertex_array;

    /** {@code GL_EXT_depth_bounds_test}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_depth_bounds_test;

    /** {@code GL_EXT_direct_state_access}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_direct_state_access;

    /** {@code GL_EXT_draw_buffers2}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_draw_buffers2;

    /** {@code GL_EXT_draw_instanced}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_draw_instanced;

    /** {@code GL_EXT_draw_range_elements}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_draw_range_elements;

    /** {@code GL_EXT_fog_coord}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_fog_coord;

    /** {@code GL_EXT_framebuffer_blit}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_framebuffer_blit;

    /** {@code GL_EXT_framebuffer_multisample}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_framebuffer_multisample;

    /** {@code GL_EXT_framebuffer_multisample_blit_scaled}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_framebuffer_multisample_blit_scaled;

    /** {@code GL_EXT_framebuffer_object}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_framebuffer_object;

    /** {@code GL_EXT_framebuffer_sRGB}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_framebuffer_sRGB;

    /** {@code GL_EXT_geometry_shader4}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_geometry_shader4;

    /** {@code GL_EXT_gpu_program_parameters}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_gpu_program_parameters;

    /** {@code GL_EXT_gpu_shader4}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_gpu_shader4;

    /** {@code GL_EXT_multi_draw_arrays}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_multi_draw_arrays;

    /** {@code GL_EXT_packed_depth_stencil}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_packed_depth_stencil;

    /** {@code GL_EXT_packed_float}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_packed_float;

    /** {@code GL_EXT_packed_pixels}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_packed_pixels;

    /** {@code GL_EXT_paletted_texture}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_paletted_texture;

    /** {@code GL_EXT_pixel_buffer_object}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_pixel_buffer_object;

    /** {@code GL_EXT_point_parameters}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_point_parameters;

    /** {@code GL_EXT_provoking_vertex}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_provoking_vertex;

    /** {@code GL_EXT_rescale_normal}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_rescale_normal;

    /** {@code GL_EXT_secondary_color}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_secondary_color;

    /** {@code GL_EXT_separate_shader_objects}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_separate_shader_objects;

    /** {@code GL_EXT_separate_specular_color}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_separate_specular_color;

    /** {@code GL_EXT_shader_image_load_store}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_shader_image_load_store;

    /** {@code GL_EXT_shadow_funcs}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_shadow_funcs;

    /** {@code GL_EXT_shared_texture_palette}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_shared_texture_palette;

    /** {@code GL_EXT_stencil_clear_tag}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_stencil_clear_tag;

    /** {@code GL_EXT_stencil_two_side}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_stencil_two_side;

    /** {@code GL_EXT_stencil_wrap}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_stencil_wrap;

    /** {@code GL_EXT_texture_3d}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_texture_3d;

    /** {@code GL_EXT_texture_array}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_texture_array;

    /** {@code GL_EXT_texture_buffer_object}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_texture_buffer_object;

    /** {@code GL_EXT_texture_compression_latc}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_texture_compression_latc;

    /** {@code GL_EXT_texture_compression_rgtc}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_texture_compression_rgtc;

    /** {@code GL_EXT_texture_compression_s3tc}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_texture_compression_s3tc;

    /** {@code GL_EXT_texture_env_combine}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_texture_env_combine;

    /** {@code GL_EXT_texture_env_dot3}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_texture_env_dot3;

    /** {@code GL_EXT_texture_filter_anisotropic}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_texture_filter_anisotropic;

    /** {@code GL_EXT_texture_integer}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_texture_integer;

    /** {@code GL_EXT_texture_lod_bias}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_texture_lod_bias;

    /** {@code GL_EXT_texture_mirror_clamp}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_texture_mirror_clamp;

    /** {@code GL_EXT_texture_rectangle}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_texture_rectangle;

    /** {@code GL_EXT_texture_sRGB}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_texture_sRGB;

    /** {@code GL_EXT_texture_sRGB_decode}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_texture_sRGB_decode;

    /** {@code GL_EXT_texture_shared_exponent}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_texture_shared_exponent;

    /** {@code GL_EXT_texture_snorm}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_texture_snorm;

    /** {@code GL_EXT_texture_swizzle}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_texture_swizzle;

    /** {@code GL_EXT_timer_query}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_timer_query;

    /** {@code GL_EXT_transform_feedback}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_transform_feedback;

    /** {@code GL_EXT_vertex_array_bgra}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_vertex_array_bgra;

    /** {@code GL_EXT_vertex_attrib_64bit}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_vertex_attrib_64bit;

    /** {@code GL_EXT_vertex_shader}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_vertex_shader;

    /** {@code GL_EXT_vertex_weighting}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_EXT_vertex_weighting;

    /** {@code OpenGL11}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean OpenGL11;

    /** {@code OpenGL12}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean OpenGL12;

    /** {@code OpenGL13}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean OpenGL13;

    /** {@code OpenGL14}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean OpenGL14;

    /** {@code OpenGL15}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean OpenGL15;

    /** {@code OpenGL20}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean OpenGL20;

    /** {@code OpenGL21}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean OpenGL21;

    /** {@code OpenGL30}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean OpenGL30;

    /** {@code OpenGL31}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean OpenGL31;

    /** {@code OpenGL32}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean OpenGL32;

    /** {@code OpenGL33}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean OpenGL33;

    /** {@code OpenGL40}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean OpenGL40;

    /** {@code OpenGL41}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean OpenGL41;

    /** {@code OpenGL42}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean OpenGL42;

    /** {@code OpenGL43}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean OpenGL43;

    /** {@code OpenGL44}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean OpenGL44;

    /** {@code OpenGL45}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean OpenGL45;

    /** {@code GL_GREMEDY_frame_terminator}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_GREMEDY_frame_terminator;

    /** {@code GL_GREMEDY_string_marker}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_GREMEDY_string_marker;

    /** {@code GL_HP_occlusion_test}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_HP_occlusion_test;

    /** {@code GL_IBM_rasterpos_clip}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_IBM_rasterpos_clip;

    /** {@code GL_INTEL_map_texture}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_INTEL_map_texture;

    /** {@code GL_KHR_context_flush_control}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_KHR_context_flush_control;

    /** {@code GL_KHR_debug}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_KHR_debug;

    /** {@code GL_KHR_robust_buffer_access_behavior}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_KHR_robust_buffer_access_behavior;

    /** {@code GL_KHR_robustness}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_KHR_robustness;

    /** {@code GL_KHR_texture_compression_astc_ldr}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_KHR_texture_compression_astc_ldr;

    /** {@code GL_NVX_gpu_memory_info}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NVX_gpu_memory_info;

    /** {@code GL_NV_bindless_multi_draw_indirect}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_bindless_multi_draw_indirect;

    /** {@code GL_NV_bindless_texture}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_bindless_texture;

    /** {@code GL_NV_blend_equation_advanced}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_blend_equation_advanced;

    /** {@code GL_NV_blend_square}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_blend_square;

    /** {@code GL_NV_compute_program5}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_compute_program5;

    /** {@code GL_NV_conditional_render}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_conditional_render;

    /** {@code GL_NV_copy_depth_to_color}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_copy_depth_to_color;

    /** {@code GL_NV_copy_image}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_copy_image;

    /** {@code GL_NV_deep_texture3D}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_deep_texture3D;

    /** {@code GL_NV_depth_buffer_float}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_depth_buffer_float;

    /** {@code GL_NV_depth_clamp}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_depth_clamp;

    /** {@code GL_NV_draw_texture}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_draw_texture;

    /** {@code GL_NV_evaluators}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_evaluators;

    /** {@code GL_NV_explicit_multisample}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_explicit_multisample;

    /** {@code GL_NV_fence}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_fence;

    /** {@code GL_NV_float_buffer}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_float_buffer;

    /** {@code GL_NV_fog_distance}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_fog_distance;

    /** {@code GL_NV_fragment_program}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_fragment_program;

    /** {@code GL_NV_fragment_program2}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_fragment_program2;

    /** {@code GL_NV_fragment_program4}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_fragment_program4;

    /** {@code GL_NV_fragment_program_option}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_fragment_program_option;

    /** {@code GL_NV_framebuffer_multisample_coverage}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_framebuffer_multisample_coverage;

    /** {@code GL_NV_geometry_program4}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_geometry_program4;

    /** {@code GL_NV_geometry_shader4}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_geometry_shader4;

    /** {@code GL_NV_gpu_program4}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_gpu_program4;

    /** {@code GL_NV_gpu_program5}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_gpu_program5;

    /** {@code GL_NV_gpu_program5_mem_extended}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_gpu_program5_mem_extended;

    /** {@code GL_NV_gpu_shader5}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_gpu_shader5;

    /** {@code GL_NV_half_float}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_half_float;

    /** {@code GL_NV_light_max_exponent}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_light_max_exponent;

    /** {@code GL_NV_multisample_coverage}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_multisample_coverage;

    /** {@code GL_NV_multisample_filter_hint}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_multisample_filter_hint;

    /** {@code GL_NV_occlusion_query}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_occlusion_query;

    /** {@code GL_NV_packed_depth_stencil}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_packed_depth_stencil;

    /** {@code GL_NV_parameter_buffer_object}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_parameter_buffer_object;

    /** {@code GL_NV_parameter_buffer_object2}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_parameter_buffer_object2;

    /** {@code GL_NV_path_rendering}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_path_rendering;

    /** {@code GL_NV_pixel_data_range}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_pixel_data_range;

    /** {@code GL_NV_point_sprite}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_point_sprite;

    /** {@code GL_NV_present_video}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_present_video;

    /** {@code GL_NV_primitive_restart}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_primitive_restart;

    /** {@code GL_NV_register_combiners}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_register_combiners;

    /** {@code GL_NV_register_combiners2}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_register_combiners2;

    /** {@code GL_NV_shader_atomic_counters}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_shader_atomic_counters;

    /** {@code GL_NV_shader_atomic_float}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_shader_atomic_float;

    /** {@code GL_NV_shader_buffer_load}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_shader_buffer_load;

    /** {@code GL_NV_shader_buffer_store}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_shader_buffer_store;

    /** {@code GL_NV_shader_storage_buffer_object}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_shader_storage_buffer_object;

    /** {@code GL_NV_tessellation_program5}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_tessellation_program5;

    /** {@code GL_NV_texgen_reflection}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_texgen_reflection;

    /** {@code GL_NV_texture_barrier}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_texture_barrier;

    /** {@code GL_NV_texture_compression_vtc}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_texture_compression_vtc;

    /** {@code GL_NV_texture_env_combine4}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_texture_env_combine4;

    /** {@code GL_NV_texture_expand_normal}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_texture_expand_normal;

    /** {@code GL_NV_texture_multisample}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_texture_multisample;

    /** {@code GL_NV_texture_rectangle}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_texture_rectangle;

    /** {@code GL_NV_texture_shader}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_texture_shader;

    /** {@code GL_NV_texture_shader2}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_texture_shader2;

    /** {@code GL_NV_texture_shader3}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_texture_shader3;

    /** {@code GL_NV_transform_feedback}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_transform_feedback;

    /** {@code GL_NV_transform_feedback2}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_transform_feedback2;

    /** {@code GL_NV_vertex_array_range}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_vertex_array_range;

    /** {@code GL_NV_vertex_array_range2}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_vertex_array_range2;

    /** {@code GL_NV_vertex_attrib_integer_64bit}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_vertex_attrib_integer_64bit;

    /** {@code GL_NV_vertex_buffer_unified_memory}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_vertex_buffer_unified_memory;

    /** {@code GL_NV_vertex_program}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_vertex_program;

    /** {@code GL_NV_vertex_program1_1}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_vertex_program1_1;

    /** {@code GL_NV_vertex_program2}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_vertex_program2;

    /** {@code GL_NV_vertex_program2_option}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_vertex_program2_option;

    /** {@code GL_NV_vertex_program3}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_vertex_program3;

    /** {@code GL_NV_vertex_program4}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_vertex_program4;

    /** {@code GL_NV_video_capture}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_NV_video_capture;

    /** {@code GL_SGIS_generate_mipmap}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_SGIS_generate_mipmap;

    /** {@code GL_SGIS_texture_lod}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_SGIS_texture_lod;

    /** {@code GL_SUN_slice_accum}, copied from LWJGL 3 {@code GLCapabilities} when present. */
    public final boolean GL_SUN_slice_accum;

    /**
     * Copies the flags from LWJGL 3.
     *
     * @param capabilities the current context's LWJGL 3 capabilities
     */
    public ContextCapabilities(GLCapabilities capabilities) {
        int[] version = version();
        GL_AMD_blend_minmax_factor = read(capabilities, "GL_AMD_blend_minmax_factor");
        GL_AMD_conservative_depth = read(capabilities, "GL_AMD_conservative_depth");
        GL_AMD_debug_output = read(capabilities, "GL_AMD_debug_output");
        GL_AMD_depth_clamp_separate = read(capabilities, "GL_AMD_depth_clamp_separate");
        GL_AMD_draw_buffers_blend = read(capabilities, "GL_AMD_draw_buffers_blend");
        GL_AMD_interleaved_elements = read(capabilities, "GL_AMD_interleaved_elements");
        GL_AMD_multi_draw_indirect = read(capabilities, "GL_AMD_multi_draw_indirect");
        GL_AMD_name_gen_delete = read(capabilities, "GL_AMD_name_gen_delete");
        GL_AMD_performance_monitor = read(capabilities, "GL_AMD_performance_monitor");
        GL_AMD_pinned_memory = read(capabilities, "GL_AMD_pinned_memory");
        GL_AMD_query_buffer_object = read(capabilities, "GL_AMD_query_buffer_object");
        GL_AMD_sample_positions = read(capabilities, "GL_AMD_sample_positions");
        GL_AMD_seamless_cubemap_per_texture = read(capabilities, "GL_AMD_seamless_cubemap_per_texture");
        GL_AMD_shader_atomic_counter_ops = read(capabilities, "GL_AMD_shader_atomic_counter_ops");
        GL_AMD_shader_stencil_export = read(capabilities, "GL_AMD_shader_stencil_export");
        GL_AMD_shader_trinary_minmax = read(capabilities, "GL_AMD_shader_trinary_minmax");
        GL_AMD_sparse_texture = read(capabilities, "GL_AMD_sparse_texture");
        GL_AMD_stencil_operation_extended = read(capabilities, "GL_AMD_stencil_operation_extended");
        GL_AMD_texture_texture4 = read(capabilities, "GL_AMD_texture_texture4");
        GL_AMD_transform_feedback3_lines_triangles = read(capabilities, "GL_AMD_transform_feedback3_lines_triangles");
        GL_AMD_vertex_shader_layer = read(capabilities, "GL_AMD_vertex_shader_layer");
        GL_AMD_vertex_shader_tessellator = read(capabilities, "GL_AMD_vertex_shader_tessellator");
        GL_AMD_vertex_shader_viewport_index = read(capabilities, "GL_AMD_vertex_shader_viewport_index");
        GL_APPLE_aux_depth_stencil = read(capabilities, "GL_APPLE_aux_depth_stencil");
        GL_APPLE_client_storage = read(capabilities, "GL_APPLE_client_storage");
        GL_APPLE_element_array = read(capabilities, "GL_APPLE_element_array");
        GL_APPLE_fence = read(capabilities, "GL_APPLE_fence");
        GL_APPLE_float_pixels = read(capabilities, "GL_APPLE_float_pixels");
        GL_APPLE_flush_buffer_range = read(capabilities, "GL_APPLE_flush_buffer_range");
        GL_APPLE_object_purgeable = read(capabilities, "GL_APPLE_object_purgeable");
        GL_APPLE_packed_pixels = read(capabilities, "GL_APPLE_packed_pixels");
        GL_APPLE_rgb_422 = read(capabilities, "GL_APPLE_rgb_422");
        GL_APPLE_row_bytes = read(capabilities, "GL_APPLE_row_bytes");
        GL_APPLE_texture_range = read(capabilities, "GL_APPLE_texture_range");
        GL_APPLE_vertex_array_object = read(capabilities, "GL_APPLE_vertex_array_object");
        GL_APPLE_vertex_array_range = read(capabilities, "GL_APPLE_vertex_array_range");
        GL_APPLE_vertex_program_evaluators = read(capabilities, "GL_APPLE_vertex_program_evaluators");
        GL_APPLE_ycbcr_422 = read(capabilities, "GL_APPLE_ycbcr_422");
        GL_ARB_ES2_compatibility = read(capabilities, "GL_ARB_ES2_compatibility");
        GL_ARB_ES3_1_compatibility = read(capabilities, "GL_ARB_ES3_1_compatibility");
        GL_ARB_ES3_compatibility = read(capabilities, "GL_ARB_ES3_compatibility");
        GL_ARB_arrays_of_arrays = read(capabilities, "GL_ARB_arrays_of_arrays");
        GL_ARB_base_instance = read(capabilities, "GL_ARB_base_instance");
        GL_ARB_bindless_texture = read(capabilities, "GL_ARB_bindless_texture");
        GL_ARB_blend_func_extended = read(capabilities, "GL_ARB_blend_func_extended");
        GL_ARB_buffer_storage = read(capabilities, "GL_ARB_buffer_storage");
        GL_ARB_cl_event = read(capabilities, "GL_ARB_cl_event");
        GL_ARB_clear_buffer_object = read(capabilities, "GL_ARB_clear_buffer_object");
        GL_ARB_clear_texture = read(capabilities, "GL_ARB_clear_texture");
        GL_ARB_clip_control = read(capabilities, "GL_ARB_clip_control");
        GL_ARB_color_buffer_float = read(capabilities, "GL_ARB_color_buffer_float");
        GL_ARB_compatibility = read(capabilities, "GL_ARB_compatibility");
        GL_ARB_compressed_texture_pixel_storage = read(capabilities, "GL_ARB_compressed_texture_pixel_storage");
        GL_ARB_compute_shader = read(capabilities, "GL_ARB_compute_shader");
        GL_ARB_compute_variable_group_size = read(capabilities, "GL_ARB_compute_variable_group_size");
        GL_ARB_conditional_render_inverted = read(capabilities, "GL_ARB_conditional_render_inverted");
        GL_ARB_conservative_depth = read(capabilities, "GL_ARB_conservative_depth");
        GL_ARB_copy_buffer = read(capabilities, "GL_ARB_copy_buffer");
        GL_ARB_copy_image = read(capabilities, "GL_ARB_copy_image");
        GL_ARB_cull_distance = read(capabilities, "GL_ARB_cull_distance");
        GL_ARB_debug_output = read(capabilities, "GL_ARB_debug_output");
        GL_ARB_depth_buffer_float = read(capabilities, "GL_ARB_depth_buffer_float");
        GL_ARB_depth_clamp = read(capabilities, "GL_ARB_depth_clamp");
        GL_ARB_depth_texture = read(capabilities, "GL_ARB_depth_texture");
        GL_ARB_derivative_control = read(capabilities, "GL_ARB_derivative_control");
        GL_ARB_direct_state_access = read(capabilities, "GL_ARB_direct_state_access");
        GL_ARB_draw_buffers = read(capabilities, "GL_ARB_draw_buffers");
        GL_ARB_draw_buffers_blend = read(capabilities, "GL_ARB_draw_buffers_blend");
        GL_ARB_draw_elements_base_vertex = read(capabilities, "GL_ARB_draw_elements_base_vertex");
        GL_ARB_draw_indirect = read(capabilities, "GL_ARB_draw_indirect");
        GL_ARB_draw_instanced = read(capabilities, "GL_ARB_draw_instanced");
        GL_ARB_enhanced_layouts = read(capabilities, "GL_ARB_enhanced_layouts");
        GL_ARB_explicit_attrib_location = read(capabilities, "GL_ARB_explicit_attrib_location");
        GL_ARB_explicit_uniform_location = read(capabilities, "GL_ARB_explicit_uniform_location");
        GL_ARB_fragment_coord_conventions = read(capabilities, "GL_ARB_fragment_coord_conventions");
        GL_ARB_fragment_layer_viewport = read(capabilities, "GL_ARB_fragment_layer_viewport");
        GL_ARB_fragment_program = read(capabilities, "GL_ARB_fragment_program");
        GL_ARB_fragment_program_shadow = read(capabilities, "GL_ARB_fragment_program_shadow");
        GL_ARB_fragment_shader = read(capabilities, "GL_ARB_fragment_shader");
        GL_ARB_framebuffer_no_attachments = read(capabilities, "GL_ARB_framebuffer_no_attachments");
        GL_ARB_framebuffer_object = read(capabilities, "GL_ARB_framebuffer_object");
        GL_ARB_framebuffer_sRGB = read(capabilities, "GL_ARB_framebuffer_sRGB");
        GL_ARB_geometry_shader4 = read(capabilities, "GL_ARB_geometry_shader4");
        GL_ARB_get_program_binary = read(capabilities, "GL_ARB_get_program_binary");
        GL_ARB_get_texture_sub_image = read(capabilities, "GL_ARB_get_texture_sub_image");
        GL_ARB_gpu_shader5 = read(capabilities, "GL_ARB_gpu_shader5");
        GL_ARB_gpu_shader_fp64 = read(capabilities, "GL_ARB_gpu_shader_fp64");
        GL_ARB_half_float_pixel = read(capabilities, "GL_ARB_half_float_pixel");
        GL_ARB_half_float_vertex = read(capabilities, "GL_ARB_half_float_vertex");
        GL_ARB_imaging = read(capabilities, "GL_ARB_imaging");
        GL_ARB_indirect_parameters = read(capabilities, "GL_ARB_indirect_parameters");
        GL_ARB_instanced_arrays = read(capabilities, "GL_ARB_instanced_arrays");
        GL_ARB_internalformat_query = read(capabilities, "GL_ARB_internalformat_query");
        GL_ARB_internalformat_query2 = read(capabilities, "GL_ARB_internalformat_query2");
        GL_ARB_invalidate_subdata = read(capabilities, "GL_ARB_invalidate_subdata");
        GL_ARB_map_buffer_alignment = read(capabilities, "GL_ARB_map_buffer_alignment");
        GL_ARB_map_buffer_range = read(capabilities, "GL_ARB_map_buffer_range");
        GL_ARB_matrix_palette = read(capabilities, "GL_ARB_matrix_palette");
        GL_ARB_multi_bind = read(capabilities, "GL_ARB_multi_bind");
        GL_ARB_multi_draw_indirect = read(capabilities, "GL_ARB_multi_draw_indirect");
        GL_ARB_multisample = read(capabilities, "GL_ARB_multisample");
        GL_ARB_multitexture = read(capabilities, "GL_ARB_multitexture");
        GL_ARB_occlusion_query = read(capabilities, "GL_ARB_occlusion_query");
        GL_ARB_occlusion_query2 = read(capabilities, "GL_ARB_occlusion_query2");
        GL_ARB_pipeline_statistics_query = read(capabilities, "GL_ARB_pipeline_statistics_query");
        GL_ARB_pixel_buffer_object = read(capabilities, "GL_ARB_pixel_buffer_object");
        GL_ARB_point_parameters = read(capabilities, "GL_ARB_point_parameters");
        GL_ARB_point_sprite = read(capabilities, "GL_ARB_point_sprite");
        GL_ARB_program_interface_query = read(capabilities, "GL_ARB_program_interface_query");
        GL_ARB_provoking_vertex = read(capabilities, "GL_ARB_provoking_vertex");
        GL_ARB_query_buffer_object = read(capabilities, "GL_ARB_query_buffer_object");
        GL_ARB_robust_buffer_access_behavior = read(capabilities, "GL_ARB_robust_buffer_access_behavior");
        GL_ARB_robustness = read(capabilities, "GL_ARB_robustness");
        GL_ARB_robustness_isolation = read(capabilities, "GL_ARB_robustness_isolation");
        GL_ARB_sample_shading = read(capabilities, "GL_ARB_sample_shading");
        GL_ARB_sampler_objects = read(capabilities, "GL_ARB_sampler_objects");
        GL_ARB_seamless_cube_map = read(capabilities, "GL_ARB_seamless_cube_map");
        GL_ARB_seamless_cubemap_per_texture = read(capabilities, "GL_ARB_seamless_cubemap_per_texture");
        GL_ARB_separate_shader_objects = read(capabilities, "GL_ARB_separate_shader_objects");
        GL_ARB_shader_atomic_counters = read(capabilities, "GL_ARB_shader_atomic_counters");
        GL_ARB_shader_bit_encoding = read(capabilities, "GL_ARB_shader_bit_encoding");
        GL_ARB_shader_draw_parameters = read(capabilities, "GL_ARB_shader_draw_parameters");
        GL_ARB_shader_group_vote = read(capabilities, "GL_ARB_shader_group_vote");
        GL_ARB_shader_image_load_store = read(capabilities, "GL_ARB_shader_image_load_store");
        GL_ARB_shader_image_size = read(capabilities, "GL_ARB_shader_image_size");
        GL_ARB_shader_objects = read(capabilities, "GL_ARB_shader_objects");
        GL_ARB_shader_precision = read(capabilities, "GL_ARB_shader_precision");
        GL_ARB_shader_stencil_export = read(capabilities, "GL_ARB_shader_stencil_export");
        GL_ARB_shader_storage_buffer_object = read(capabilities, "GL_ARB_shader_storage_buffer_object");
        GL_ARB_shader_subroutine = read(capabilities, "GL_ARB_shader_subroutine");
        GL_ARB_shader_texture_image_samples = read(capabilities, "GL_ARB_shader_texture_image_samples");
        GL_ARB_shader_texture_lod = read(capabilities, "GL_ARB_shader_texture_lod");
        GL_ARB_shading_language_100 = read(capabilities, "GL_ARB_shading_language_100");
        GL_ARB_shading_language_420pack = read(capabilities, "GL_ARB_shading_language_420pack");
        GL_ARB_shading_language_include = read(capabilities, "GL_ARB_shading_language_include");
        GL_ARB_shading_language_packing = read(capabilities, "GL_ARB_shading_language_packing");
        GL_ARB_shadow = read(capabilities, "GL_ARB_shadow");
        GL_ARB_shadow_ambient = read(capabilities, "GL_ARB_shadow_ambient");
        GL_ARB_sparse_buffer = read(capabilities, "GL_ARB_sparse_buffer");
        GL_ARB_sparse_texture = read(capabilities, "GL_ARB_sparse_texture");
        GL_ARB_stencil_texturing = read(capabilities, "GL_ARB_stencil_texturing");
        GL_ARB_sync = read(capabilities, "GL_ARB_sync");
        GL_ARB_tessellation_shader = read(capabilities, "GL_ARB_tessellation_shader");
        GL_ARB_texture_barrier = read(capabilities, "GL_ARB_texture_barrier");
        GL_ARB_texture_border_clamp = read(capabilities, "GL_ARB_texture_border_clamp");
        GL_ARB_texture_buffer_object = read(capabilities, "GL_ARB_texture_buffer_object");
        GL_ARB_texture_buffer_object_rgb32 = read(capabilities, "GL_ARB_texture_buffer_object_rgb32");
        GL_ARB_texture_buffer_range = read(capabilities, "GL_ARB_texture_buffer_range");
        GL_ARB_texture_compression = read(capabilities, "GL_ARB_texture_compression");
        GL_ARB_texture_compression_bptc = read(capabilities, "GL_ARB_texture_compression_bptc");
        GL_ARB_texture_compression_rgtc = read(capabilities, "GL_ARB_texture_compression_rgtc");
        GL_ARB_texture_cube_map = read(capabilities, "GL_ARB_texture_cube_map");
        GL_ARB_texture_cube_map_array = read(capabilities, "GL_ARB_texture_cube_map_array");
        GL_ARB_texture_env_add = read(capabilities, "GL_ARB_texture_env_add");
        GL_ARB_texture_env_combine = read(capabilities, "GL_ARB_texture_env_combine");
        GL_ARB_texture_env_crossbar = read(capabilities, "GL_ARB_texture_env_crossbar");
        GL_ARB_texture_env_dot3 = read(capabilities, "GL_ARB_texture_env_dot3");
        GL_ARB_texture_float = read(capabilities, "GL_ARB_texture_float");
        GL_ARB_texture_gather = read(capabilities, "GL_ARB_texture_gather");
        GL_ARB_texture_mirror_clamp_to_edge = read(capabilities, "GL_ARB_texture_mirror_clamp_to_edge");
        GL_ARB_texture_mirrored_repeat = read(capabilities, "GL_ARB_texture_mirrored_repeat");
        GL_ARB_texture_multisample = read(capabilities, "GL_ARB_texture_multisample");
        GL_ARB_texture_non_power_of_two = read(capabilities, "GL_ARB_texture_non_power_of_two");
        GL_ARB_texture_query_levels = read(capabilities, "GL_ARB_texture_query_levels");
        GL_ARB_texture_query_lod = read(capabilities, "GL_ARB_texture_query_lod");
        GL_ARB_texture_rectangle = read(capabilities, "GL_ARB_texture_rectangle");
        GL_ARB_texture_rg = read(capabilities, "GL_ARB_texture_rg");
        GL_ARB_texture_rgb10_a2ui = read(capabilities, "GL_ARB_texture_rgb10_a2ui");
        GL_ARB_texture_stencil8 = read(capabilities, "GL_ARB_texture_stencil8");
        GL_ARB_texture_storage = read(capabilities, "GL_ARB_texture_storage");
        GL_ARB_texture_storage_multisample = read(capabilities, "GL_ARB_texture_storage_multisample");
        GL_ARB_texture_swizzle = read(capabilities, "GL_ARB_texture_swizzle");
        GL_ARB_texture_view = read(capabilities, "GL_ARB_texture_view");
        GL_ARB_timer_query = read(capabilities, "GL_ARB_timer_query");
        GL_ARB_transform_feedback2 = read(capabilities, "GL_ARB_transform_feedback2");
        GL_ARB_transform_feedback3 = read(capabilities, "GL_ARB_transform_feedback3");
        GL_ARB_transform_feedback_instanced = read(capabilities, "GL_ARB_transform_feedback_instanced");
        GL_ARB_transform_feedback_overflow_query = read(capabilities, "GL_ARB_transform_feedback_overflow_query");
        GL_ARB_transpose_matrix = read(capabilities, "GL_ARB_transpose_matrix");
        GL_ARB_uniform_buffer_object = read(capabilities, "GL_ARB_uniform_buffer_object");
        GL_ARB_vertex_array_bgra = read(capabilities, "GL_ARB_vertex_array_bgra");
        GL_ARB_vertex_array_object = read(capabilities, "GL_ARB_vertex_array_object");
        GL_ARB_vertex_attrib_64bit = read(capabilities, "GL_ARB_vertex_attrib_64bit");
        GL_ARB_vertex_attrib_binding = read(capabilities, "GL_ARB_vertex_attrib_binding");
        GL_ARB_vertex_blend = read(capabilities, "GL_ARB_vertex_blend");
        GL_ARB_vertex_buffer_object = read(capabilities, "GL_ARB_vertex_buffer_object");
        GL_ARB_vertex_program = read(capabilities, "GL_ARB_vertex_program");
        GL_ARB_vertex_shader = read(capabilities, "GL_ARB_vertex_shader");
        GL_ARB_vertex_type_10f_11f_11f_rev = read(capabilities, "GL_ARB_vertex_type_10f_11f_11f_rev");
        GL_ARB_vertex_type_2_10_10_10_rev = read(capabilities, "GL_ARB_vertex_type_2_10_10_10_rev");
        GL_ARB_viewport_array = read(capabilities, "GL_ARB_viewport_array");
        GL_ARB_window_pos = read(capabilities, "GL_ARB_window_pos");
        GL_ATI_draw_buffers = read(capabilities, "GL_ATI_draw_buffers");
        GL_ATI_element_array = read(capabilities, "GL_ATI_element_array");
        GL_ATI_envmap_bumpmap = read(capabilities, "GL_ATI_envmap_bumpmap");
        GL_ATI_fragment_shader = read(capabilities, "GL_ATI_fragment_shader");
        GL_ATI_map_object_buffer = read(capabilities, "GL_ATI_map_object_buffer");
        GL_ATI_meminfo = read(capabilities, "GL_ATI_meminfo");
        GL_ATI_pn_triangles = read(capabilities, "GL_ATI_pn_triangles");
        GL_ATI_separate_stencil = read(capabilities, "GL_ATI_separate_stencil");
        GL_ATI_shader_texture_lod = read(capabilities, "GL_ATI_shader_texture_lod");
        GL_ATI_text_fragment_shader = read(capabilities, "GL_ATI_text_fragment_shader");
        GL_ATI_texture_compression_3dc = read(capabilities, "GL_ATI_texture_compression_3dc");
        GL_ATI_texture_env_combine3 = read(capabilities, "GL_ATI_texture_env_combine3");
        GL_ATI_texture_float = read(capabilities, "GL_ATI_texture_float");
        GL_ATI_texture_mirror_once = read(capabilities, "GL_ATI_texture_mirror_once");
        GL_ATI_vertex_array_object = read(capabilities, "GL_ATI_vertex_array_object");
        GL_ATI_vertex_attrib_array_object = read(capabilities, "GL_ATI_vertex_attrib_array_object");
        GL_ATI_vertex_streams = read(capabilities, "GL_ATI_vertex_streams");
        GL_EXT_Cg_shader = read(capabilities, "GL_EXT_Cg_shader");
        GL_EXT_abgr = read(capabilities, "GL_EXT_abgr");
        GL_EXT_bgra = read(capabilities, "GL_EXT_bgra");
        GL_EXT_bindable_uniform = read(capabilities, "GL_EXT_bindable_uniform");
        GL_EXT_blend_color = read(capabilities, "GL_EXT_blend_color");
        GL_EXT_blend_equation_separate = read(capabilities, "GL_EXT_blend_equation_separate");
        GL_EXT_blend_func_separate = read(capabilities, "GL_EXT_blend_func_separate");
        GL_EXT_blend_minmax = read(capabilities, "GL_EXT_blend_minmax");
        GL_EXT_blend_subtract = read(capabilities, "GL_EXT_blend_subtract");
        GL_EXT_compiled_vertex_array = read(capabilities, "GL_EXT_compiled_vertex_array");
        GL_EXT_depth_bounds_test = read(capabilities, "GL_EXT_depth_bounds_test");
        GL_EXT_direct_state_access = read(capabilities, "GL_EXT_direct_state_access");
        GL_EXT_draw_buffers2 = read(capabilities, "GL_EXT_draw_buffers2");
        GL_EXT_draw_instanced = read(capabilities, "GL_EXT_draw_instanced");
        GL_EXT_draw_range_elements = read(capabilities, "GL_EXT_draw_range_elements");
        GL_EXT_fog_coord = read(capabilities, "GL_EXT_fog_coord");
        GL_EXT_framebuffer_blit = read(capabilities, "GL_EXT_framebuffer_blit");
        GL_EXT_framebuffer_multisample = read(capabilities, "GL_EXT_framebuffer_multisample");
        GL_EXT_framebuffer_multisample_blit_scaled = read(capabilities, "GL_EXT_framebuffer_multisample_blit_scaled");
        GL_EXT_framebuffer_object = read(capabilities, "GL_EXT_framebuffer_object");
        GL_EXT_framebuffer_sRGB = read(capabilities, "GL_EXT_framebuffer_sRGB");
        GL_EXT_geometry_shader4 = read(capabilities, "GL_EXT_geometry_shader4");
        GL_EXT_gpu_program_parameters = read(capabilities, "GL_EXT_gpu_program_parameters");
        GL_EXT_gpu_shader4 = read(capabilities, "GL_EXT_gpu_shader4");
        GL_EXT_multi_draw_arrays = read(capabilities, "GL_EXT_multi_draw_arrays");
        GL_EXT_packed_depth_stencil = read(capabilities, "GL_EXT_packed_depth_stencil");
        GL_EXT_packed_float = read(capabilities, "GL_EXT_packed_float");
        GL_EXT_packed_pixels = read(capabilities, "GL_EXT_packed_pixels");
        GL_EXT_paletted_texture = read(capabilities, "GL_EXT_paletted_texture");
        GL_EXT_pixel_buffer_object = read(capabilities, "GL_EXT_pixel_buffer_object");
        GL_EXT_point_parameters = read(capabilities, "GL_EXT_point_parameters");
        GL_EXT_provoking_vertex = read(capabilities, "GL_EXT_provoking_vertex");
        GL_EXT_rescale_normal = read(capabilities, "GL_EXT_rescale_normal");
        GL_EXT_secondary_color = read(capabilities, "GL_EXT_secondary_color");
        GL_EXT_separate_shader_objects = read(capabilities, "GL_EXT_separate_shader_objects");
        GL_EXT_separate_specular_color = read(capabilities, "GL_EXT_separate_specular_color");
        GL_EXT_shader_image_load_store = read(capabilities, "GL_EXT_shader_image_load_store");
        GL_EXT_shadow_funcs = read(capabilities, "GL_EXT_shadow_funcs");
        GL_EXT_shared_texture_palette = read(capabilities, "GL_EXT_shared_texture_palette");
        GL_EXT_stencil_clear_tag = read(capabilities, "GL_EXT_stencil_clear_tag");
        GL_EXT_stencil_two_side = read(capabilities, "GL_EXT_stencil_two_side");
        GL_EXT_stencil_wrap = read(capabilities, "GL_EXT_stencil_wrap");
        GL_EXT_texture_3d = read(capabilities, "GL_EXT_texture_3d");
        GL_EXT_texture_array = read(capabilities, "GL_EXT_texture_array");
        GL_EXT_texture_buffer_object = read(capabilities, "GL_EXT_texture_buffer_object");
        GL_EXT_texture_compression_latc = read(capabilities, "GL_EXT_texture_compression_latc");
        GL_EXT_texture_compression_rgtc = read(capabilities, "GL_EXT_texture_compression_rgtc");
        GL_EXT_texture_compression_s3tc = read(capabilities, "GL_EXT_texture_compression_s3tc");
        GL_EXT_texture_env_combine = read(capabilities, "GL_EXT_texture_env_combine");
        GL_EXT_texture_env_dot3 = read(capabilities, "GL_EXT_texture_env_dot3");
        GL_EXT_texture_filter_anisotropic = read(capabilities, "GL_EXT_texture_filter_anisotropic");
        GL_EXT_texture_integer = read(capabilities, "GL_EXT_texture_integer");
        GL_EXT_texture_lod_bias = read(capabilities, "GL_EXT_texture_lod_bias");
        GL_EXT_texture_mirror_clamp = read(capabilities, "GL_EXT_texture_mirror_clamp");
        GL_EXT_texture_rectangle = read(capabilities, "GL_EXT_texture_rectangle");
        GL_EXT_texture_sRGB = read(capabilities, "GL_EXT_texture_sRGB");
        GL_EXT_texture_sRGB_decode = read(capabilities, "GL_EXT_texture_sRGB_decode");
        GL_EXT_texture_shared_exponent = read(capabilities, "GL_EXT_texture_shared_exponent");
        GL_EXT_texture_snorm = read(capabilities, "GL_EXT_texture_snorm");
        GL_EXT_texture_swizzle = read(capabilities, "GL_EXT_texture_swizzle");
        GL_EXT_timer_query = read(capabilities, "GL_EXT_timer_query");
        GL_EXT_transform_feedback = read(capabilities, "GL_EXT_transform_feedback");
        GL_EXT_vertex_array_bgra = read(capabilities, "GL_EXT_vertex_array_bgra");
        GL_EXT_vertex_attrib_64bit = read(capabilities, "GL_EXT_vertex_attrib_64bit");
        GL_EXT_vertex_shader = read(capabilities, "GL_EXT_vertex_shader");
        GL_EXT_vertex_weighting = read(capabilities, "GL_EXT_vertex_weighting");
        OpenGL11 = read(capabilities, "OpenGL11") || atLeast(version, 1, 1);
        OpenGL12 = read(capabilities, "OpenGL12") || atLeast(version, 1, 2);
        OpenGL13 = read(capabilities, "OpenGL13") || atLeast(version, 1, 3);
        OpenGL14 = read(capabilities, "OpenGL14") || atLeast(version, 1, 4);
        OpenGL15 = read(capabilities, "OpenGL15") || atLeast(version, 1, 5);
        OpenGL20 = read(capabilities, "OpenGL20") || atLeast(version, 2, 0);
        OpenGL21 = read(capabilities, "OpenGL21") || atLeast(version, 2, 1);
        OpenGL30 = read(capabilities, "OpenGL30") || atLeast(version, 3, 0);
        OpenGL31 = read(capabilities, "OpenGL31") || atLeast(version, 3, 1);
        OpenGL32 = read(capabilities, "OpenGL32") || atLeast(version, 3, 2);
        OpenGL33 = read(capabilities, "OpenGL33") || atLeast(version, 3, 3);
        OpenGL40 = read(capabilities, "OpenGL40") || atLeast(version, 4, 0);
        OpenGL41 = read(capabilities, "OpenGL41") || atLeast(version, 4, 1);
        OpenGL42 = read(capabilities, "OpenGL42") || atLeast(version, 4, 2);
        OpenGL43 = read(capabilities, "OpenGL43") || atLeast(version, 4, 3);
        OpenGL44 = read(capabilities, "OpenGL44") || atLeast(version, 4, 4);
        OpenGL45 = read(capabilities, "OpenGL45") || atLeast(version, 4, 5);
        GL_GREMEDY_frame_terminator = read(capabilities, "GL_GREMEDY_frame_terminator");
        GL_GREMEDY_string_marker = read(capabilities, "GL_GREMEDY_string_marker");
        GL_HP_occlusion_test = read(capabilities, "GL_HP_occlusion_test");
        GL_IBM_rasterpos_clip = read(capabilities, "GL_IBM_rasterpos_clip");
        GL_INTEL_map_texture = read(capabilities, "GL_INTEL_map_texture");
        GL_KHR_context_flush_control = read(capabilities, "GL_KHR_context_flush_control");
        GL_KHR_debug = read(capabilities, "GL_KHR_debug");
        GL_KHR_robust_buffer_access_behavior = read(capabilities, "GL_KHR_robust_buffer_access_behavior");
        GL_KHR_robustness = read(capabilities, "GL_KHR_robustness");
        GL_KHR_texture_compression_astc_ldr = read(capabilities, "GL_KHR_texture_compression_astc_ldr");
        GL_NVX_gpu_memory_info = read(capabilities, "GL_NVX_gpu_memory_info");
        GL_NV_bindless_multi_draw_indirect = read(capabilities, "GL_NV_bindless_multi_draw_indirect");
        GL_NV_bindless_texture = read(capabilities, "GL_NV_bindless_texture");
        GL_NV_blend_equation_advanced = read(capabilities, "GL_NV_blend_equation_advanced");
        GL_NV_blend_square = read(capabilities, "GL_NV_blend_square");
        GL_NV_compute_program5 = read(capabilities, "GL_NV_compute_program5");
        GL_NV_conditional_render = read(capabilities, "GL_NV_conditional_render");
        GL_NV_copy_depth_to_color = read(capabilities, "GL_NV_copy_depth_to_color");
        GL_NV_copy_image = read(capabilities, "GL_NV_copy_image");
        GL_NV_deep_texture3D = read(capabilities, "GL_NV_deep_texture3D");
        GL_NV_depth_buffer_float = read(capabilities, "GL_NV_depth_buffer_float");
        GL_NV_depth_clamp = read(capabilities, "GL_NV_depth_clamp");
        GL_NV_draw_texture = read(capabilities, "GL_NV_draw_texture");
        GL_NV_evaluators = read(capabilities, "GL_NV_evaluators");
        GL_NV_explicit_multisample = read(capabilities, "GL_NV_explicit_multisample");
        GL_NV_fence = read(capabilities, "GL_NV_fence");
        GL_NV_float_buffer = read(capabilities, "GL_NV_float_buffer");
        GL_NV_fog_distance = read(capabilities, "GL_NV_fog_distance");
        GL_NV_fragment_program = read(capabilities, "GL_NV_fragment_program");
        GL_NV_fragment_program2 = read(capabilities, "GL_NV_fragment_program2");
        GL_NV_fragment_program4 = read(capabilities, "GL_NV_fragment_program4");
        GL_NV_fragment_program_option = read(capabilities, "GL_NV_fragment_program_option");
        GL_NV_framebuffer_multisample_coverage = read(capabilities, "GL_NV_framebuffer_multisample_coverage");
        GL_NV_geometry_program4 = read(capabilities, "GL_NV_geometry_program4");
        GL_NV_geometry_shader4 = read(capabilities, "GL_NV_geometry_shader4");
        GL_NV_gpu_program4 = read(capabilities, "GL_NV_gpu_program4");
        GL_NV_gpu_program5 = read(capabilities, "GL_NV_gpu_program5");
        GL_NV_gpu_program5_mem_extended = read(capabilities, "GL_NV_gpu_program5_mem_extended");
        GL_NV_gpu_shader5 = read(capabilities, "GL_NV_gpu_shader5");
        GL_NV_half_float = read(capabilities, "GL_NV_half_float");
        GL_NV_light_max_exponent = read(capabilities, "GL_NV_light_max_exponent");
        GL_NV_multisample_coverage = read(capabilities, "GL_NV_multisample_coverage");
        GL_NV_multisample_filter_hint = read(capabilities, "GL_NV_multisample_filter_hint");
        GL_NV_occlusion_query = read(capabilities, "GL_NV_occlusion_query");
        GL_NV_packed_depth_stencil = read(capabilities, "GL_NV_packed_depth_stencil");
        GL_NV_parameter_buffer_object = read(capabilities, "GL_NV_parameter_buffer_object");
        GL_NV_parameter_buffer_object2 = read(capabilities, "GL_NV_parameter_buffer_object2");
        GL_NV_path_rendering = read(capabilities, "GL_NV_path_rendering");
        GL_NV_pixel_data_range = read(capabilities, "GL_NV_pixel_data_range");
        GL_NV_point_sprite = read(capabilities, "GL_NV_point_sprite");
        GL_NV_present_video = read(capabilities, "GL_NV_present_video");
        GL_NV_primitive_restart = read(capabilities, "GL_NV_primitive_restart");
        GL_NV_register_combiners = read(capabilities, "GL_NV_register_combiners");
        GL_NV_register_combiners2 = read(capabilities, "GL_NV_register_combiners2");
        GL_NV_shader_atomic_counters = read(capabilities, "GL_NV_shader_atomic_counters");
        GL_NV_shader_atomic_float = read(capabilities, "GL_NV_shader_atomic_float");
        GL_NV_shader_buffer_load = read(capabilities, "GL_NV_shader_buffer_load");
        GL_NV_shader_buffer_store = read(capabilities, "GL_NV_shader_buffer_store");
        GL_NV_shader_storage_buffer_object = read(capabilities, "GL_NV_shader_storage_buffer_object");
        GL_NV_tessellation_program5 = read(capabilities, "GL_NV_tessellation_program5");
        GL_NV_texgen_reflection = read(capabilities, "GL_NV_texgen_reflection");
        GL_NV_texture_barrier = read(capabilities, "GL_NV_texture_barrier");
        GL_NV_texture_compression_vtc = read(capabilities, "GL_NV_texture_compression_vtc");
        GL_NV_texture_env_combine4 = read(capabilities, "GL_NV_texture_env_combine4");
        GL_NV_texture_expand_normal = read(capabilities, "GL_NV_texture_expand_normal");
        GL_NV_texture_multisample = read(capabilities, "GL_NV_texture_multisample");
        GL_NV_texture_rectangle = read(capabilities, "GL_NV_texture_rectangle");
        GL_NV_texture_shader = read(capabilities, "GL_NV_texture_shader");
        GL_NV_texture_shader2 = read(capabilities, "GL_NV_texture_shader2");
        GL_NV_texture_shader3 = read(capabilities, "GL_NV_texture_shader3");
        GL_NV_transform_feedback = read(capabilities, "GL_NV_transform_feedback");
        GL_NV_transform_feedback2 = read(capabilities, "GL_NV_transform_feedback2");
        GL_NV_vertex_array_range = read(capabilities, "GL_NV_vertex_array_range");
        GL_NV_vertex_array_range2 = read(capabilities, "GL_NV_vertex_array_range2");
        GL_NV_vertex_attrib_integer_64bit = read(capabilities, "GL_NV_vertex_attrib_integer_64bit");
        GL_NV_vertex_buffer_unified_memory = read(capabilities, "GL_NV_vertex_buffer_unified_memory");
        GL_NV_vertex_program = read(capabilities, "GL_NV_vertex_program");
        GL_NV_vertex_program1_1 = read(capabilities, "GL_NV_vertex_program1_1");
        GL_NV_vertex_program2 = read(capabilities, "GL_NV_vertex_program2");
        GL_NV_vertex_program2_option = read(capabilities, "GL_NV_vertex_program2_option");
        GL_NV_vertex_program3 = read(capabilities, "GL_NV_vertex_program3");
        GL_NV_vertex_program4 = read(capabilities, "GL_NV_vertex_program4");
        GL_NV_video_capture = read(capabilities, "GL_NV_video_capture");
        GL_SGIS_generate_mipmap = read(capabilities, "GL_SGIS_generate_mipmap");
        GL_SGIS_texture_lod = read(capabilities, "GL_SGIS_texture_lod");
        GL_SUN_slice_accum = read(capabilities, "GL_SUN_slice_accum");
    }

    /**
     * The context's version from {@code GL_VERSION}. LWJGL 2 set the version flags from it; LWJGL 3 sets a version
     * flag only when every function of that version is present, which Apple's 2.1 context does not satisfy for 1.3.
     */
    private static int[] version() {
        try {
            String v = GL11.glGetString(GL11.GL_VERSION);
            java.util.regex.Matcher m =
                    java.util.regex.Pattern.compile("(\\d+)\\.(\\d+)").matcher(v == null ? "" : v);
            return m.find() ? new int[] {Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))} : new int[] {1, 1};
        } catch (RuntimeException e) {
            return new int[] {1, 1};
        }
    }

    private static boolean atLeast(int[] version, int major, int minor) {
        return version[0] > major || (version[0] == major && version[1] >= minor);
    }

    /** Extensions the core renderer provides itself, on their core equivalents. */
    private static final java.util.Set<String> CORE_EMULATED = java.util.Set.of(
            "GL_ARB_shader_objects",
            "GL_ARB_vertex_shader",
            "GL_ARB_fragment_shader",
            "GL_ARB_vertex_buffer_object",
            "GL_ARB_framebuffer_object",
            "GL_ARB_multitexture",
            "GL_EXT_framebuffer_object",
            "GL_EXT_blend_func_separate");

    private static boolean read(GLCapabilities capabilities, String name) {
        if (lol.trq.lazarus.lwjgl3.Window.CORE && CORE_EMULATED.contains(name)) {
            return true;
        }
        try {
            Field field = GLCapabilities.class.getField(name);
            return field.getType() == boolean.class && field.getBoolean(capabilities);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }
}
