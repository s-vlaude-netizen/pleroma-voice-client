package de.peroma.voice

/** The kinds of attachment a post can carry, as far as speech is concerned. */
enum class MediaKind {
    IMAGE,
    VIDEO,
    AUDIO,
    ANIMATION,
    OTHER
}

/**
 * One attachment of a post.
 *
 * The description is what the author wrote for people who cannot see the file;
 * it is null when there is none. Attachments are kept as data rather than as
 * finished sentences so that the ones without a description can be counted
 * instead of announced one by one.
 */
data class Media(val kind: MediaKind, val description: String?)
