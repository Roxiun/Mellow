plugins {
    id("dev.kikugie.stonecutter")
}
stonecutter active "1.8.9-ornithe"
stonecutter parameters {
    constants {
        match(node.metadata.project.substringAfterLast('-'), "forge", "ornithe")
    }
}
