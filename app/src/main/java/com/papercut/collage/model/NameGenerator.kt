package com.papercut.collage.model

/**
 * Reddit-style two-word names (GrizzlyBear, AppleDoctor) so a new collage
 * arrives with an identity instead of "Untitled". Word lists are bundled — no
 * network, and it stays readable offline.
 */
object NameGenerator {

    private val firsts = listOf(
        "Grizzly", "Apple", "Velvet", "Copper", "Paper", "Amber", "Quiet", "Golden",
        "Cosmic", "Wander", "Rusty", "Silver", "Marble", "Sunny", "Autumn", "Crimson",
        "Hazy", "Clever", "Gentle", "Wild", "Frosted", "Salty", "Dusty", "Lucky",
        "Woven", "Bright", "Curious", "Humble", "Merry", "Neon", "Plush", "Rugged",
    )

    private val seconds = listOf(
        "Bear", "Doctor", "Meadow", "Lantern", "Harbor", "Falcon", "Garden", "Compass",
        "Otter", "Canyon", "Ribbon", "Thicket", "Sparrow", "Anchor", "Willow", "Ember",
        "Pebble", "Cabin", "Comet", "Fern", "Badger", "Trellis", "Puffin", "Alcove",
        "Kettle", "Beacon", "Cricket", "Dune", "Heron", "Juniper", "Mosaic", "Quill",
    )

    fun random(): String = firsts.random() + seconds.random()
}
