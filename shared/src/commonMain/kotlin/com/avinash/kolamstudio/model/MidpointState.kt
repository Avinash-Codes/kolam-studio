package com.avinash.kolamstudio.model

/** What the line does at a midpoint between two neighbouring dots. */
enum class MidpointState {
    /** Both strands go straight through and cross. */
    CROSS,

    /** Horizontal wall: a strand's vertical direction flips. */
    MIRROR_H,

    /** Vertical wall: a strand's horizontal direction flips. */
    MIRROR_V,
}
